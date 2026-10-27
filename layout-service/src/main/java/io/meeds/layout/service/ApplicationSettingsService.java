/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;

/**
 * The registry of the application settings plugins and the rules every
 * configuration of placed applications follows: an application without a
 * plugin is not configurable, every application is validated before the
 * first one is applied, each one is applied through its plugin while its
 * window is locked on this node, and a failure after the first apply names
 * the applications already applied, nothing being undone.
 */
@Service
public class ApplicationSettingsService {

  public static final String                             OWNER_PREFERENCE                   = "mcp.settingsOwner";

  public static final String                             APPLICATION_NOT_FOUND              = "does not exist: application";

  public static final String                             APPLICATION_NOT_ALLOWED            = "not allowed: application";

  public static final String                             NOT_CONFIGURABLE                   = "not configurable through tools: %s";

  public static final String                             NOT_CONFIGURABLE_IN_SITE_CONTAINER =
                                                                                            "not configurable in a site container: %s";

  public static final String                             APPLICATION_NOT_SAVED              = "could not be saved: application";

  public static final String                             LAYOUT_BUSY                        = "layout busy, retry";

  public static final String                             APPLIED_BEFORE_FAILURE             = "applied before the failure: %s; %s";

  public static final String                             VALUE_NOT_ACCEPTED                 = "value is not accepted for %s";

  private static final Log                               LOG                                =
                                                             ExoLogger.getLogger(ApplicationSettingsService.class);

  private static final int                               LOCK_STRIPES                       = 64;

  private static final long                              LOCK_TIMEOUT_SECONDS               = 5;

  private final Map<String, ApplicationSettingsPlugin>   plugins                            = new ConcurrentHashMap<>();

  private final ReentrantLock[]                          locks                              = new ReentrantLock[LOCK_STRIPES];

  @Autowired
  private LayoutService                                  layoutService;

  @Autowired
  private PageLayoutService                              pageLayoutService;

  @Autowired
  private SiteLayoutService                              siteLayoutService;

  @Autowired
  private SpaceService                                   spaceService;

  public ApplicationSettingsService() {
    for (int i = 0; i < LOCK_STRIPES; i++) {
      locks[i] = new ReentrantLock();
    }
  }

  /**
   * Registers a plugin, replacing the one registered for the same portlet
   */
  public void addPlugin(ApplicationSettingsPlugin plugin) {
    if (plugin == null || StringUtils.isBlank(plugin.getPortletName())) {
      LOG.warn("An application settings plugin without a portlet name is ignored: {}", plugin);
      return;
    }
    plugins.put(plugin.getPortletName(), plugin);
  }

  public void removePlugin(String portletName) {
    if (portletName != null) {
      plugins.remove(portletName);
    }
  }

  /**
   * @param  contentId     the application content id, app/portlet
   * @param  siteContainer whether the window sits in a site container
   * @return               whether a plugin configures the application there
   */
  public boolean isConfigurable(String contentId, boolean siteContainer) {
    ApplicationSettingsPlugin plugin = getPlugin(contentId);
    return plugin != null && (!siteContainer || plugin.isConfigurableInSiteContainer());
  }

  /**
   * @param  contentId the application content id, app/portlet
   * @return           the JSON schema of its settings, null when no plugin
   *                   configures it
   */
  public Map<String, Object> getSettingsSchema(String contentId) {
    ApplicationSettingsPlugin plugin = getPlugin(contentId);
    return plugin == null ? null : plugin.getSettingsSchema();
  }

  /**
   * @param  window        a window of the stored tree
   * @param  pageKey       the page holding it, null in a site container
   * @param  siteKey       the site holding it
   * @param  liveSiteKey   the live site, whose space the setting belongs to
   * @param  pageReference the live page reference, null in a site container
   * @param  draft         whether the window is the user's draft
   * @param  username      the acting user
   * @return               the plugins' context of the window
   */
  public ApplicationSettingsContext newContext(Application window, // NOSONAR
                                               PageKey pageKey,
                                               SiteKey siteKey,
                                               SiteKey liveSiteKey,
                                               String pageReference,
                                               boolean draft,
                                               String username) {
    return new ApplicationSettingsContext(Long.parseLong(window.getStorageId()),
                                          layoutService.getId(window.getState()),
                                          preferencesOf(window),
                                          pageKey == null,
                                          draft,
                                          pageKey,
                                          siteKey,
                                          pageReference,
                                          getSpaceId(liveSiteKey),
                                          username);
  }

  /**
   * @return the settings of a configurable window as its plugin reads them
   *         for the context's user, null when it has none, when the user may
   *         not read them or when the plugin fails
   */
  public Map<String, Object> readSettings(ApplicationSettingsContext context) {
    if (!isConfigurable(context.getContentId(), context.isSiteContainer())) {
      return null;
    }
    try {
      return getPlugin(context.getContentId()).read(context);
    } catch (IllegalAccessException | ObjectNotFoundException e) {
      LOG.debug("Settings of application {} are not readable by {}", context.getApplicationId(), context.getUsername(), e);
      return null;
    } catch (RuntimeException e) {
      LOG.warn("Settings of application {} could not be read for {}", context.getApplicationId(), context.getUsername(), e);
      return null;
    }
  }

  /**
   * Checks every application before anything is written: a plugin
   * configures it where it sits, and its plugin accepts the settings for the
   * context's user
   *
   * @param  contexts                the windows, in the order of the call
   * @param  settings                their settings, null-valued entries
   *                                   already dropped
   * @param  requestedIds            the ids the call named, for the
   *                                   refusals
   * @throws IllegalAccessException  when an application's own rule refuses
   *                                   the user
   * @throws ObjectNotFoundException when a referenced object does not exist
   */
  public void validate(List<ApplicationSettingsContext> contexts,
                       List<Map<String, Object>> settings,
                       List<String> requestedIds) throws IllegalAccessException, ObjectNotFoundException {
    for (int i = 0; i < contexts.size(); i++) {
      ApplicationSettingsContext context = contexts.get(i);
      ApplicationSettingsPlugin plugin = getPlugin(context.getContentId());
      if (plugin == null) {
        throw new IllegalArgumentException(String.format(NOT_CONFIGURABLE, requestedIds.get(i)));
      } else if (context.isSiteContainer() && !plugin.isConfigurableInSiteContainer()) {
        throw new IllegalArgumentException(String.format(NOT_CONFIGURABLE_IN_SITE_CONTAINER, requestedIds.get(i)));
      }
      String optionWithNullItem = findNullListItem(settings.get(i));
      if (optionWithNullItem != null) {
        throw new IllegalArgumentException(String.format(VALUE_NOT_ACCEPTED, optionWithNullItem));
      }
      try {
        plugin.validate(context, settings.get(i));
      } catch (IllegalAccessException e) {
        LOG.debug("Application {} settings refused to {}", context.getApplicationId(), context.getUsername(), e);
        throw new IllegalAccessException(APPLICATION_NOT_ALLOWED);
      }
    }
  }

  /**
   * Applies validated settings application after application, each window
   * locked on this node while its plugin writes. A failure of the first
   * application is answered as is; a later one names the applications
   * already applied, which stay applied.
   *
   * @param  contexts                the windows, as {@link #validate} checked
   *                                   them, possibly re-pointed at the
   *                                   draft's copies
   * @param  settings                their validated settings
   * @param  requestedIds            the ids the call named
   * @throws IllegalAccessException  when the first application is refused
   * @throws ObjectNotFoundException when the first application's object
   *                                   disappeared
   */
  public void apply(List<ApplicationSettingsContext> contexts,
                    List<Map<String, Object>> settings,
                    List<String> requestedIds) throws IllegalAccessException, ObjectNotFoundException {
    List<String> applied = new ArrayList<>();
    for (int i = 0; i < contexts.size(); i++) {
      ApplicationSettingsContext context = contexts.get(i);
      try {
        applyLocked(getPlugin(context.getContentId()), context, settings.get(i));
        applied.add(requestedIds.get(i));
      } catch (IllegalAccessException e) {
        LOG.debug("Application {} settings refused to {}", context.getApplicationId(), context.getUsername(), e);
        throwFailure(applied, new IllegalAccessException(APPLICATION_NOT_ALLOWED));
      } catch (ObjectNotFoundException e) {
        throwFailure(applied, e);
      } catch (IllegalArgumentException | IllegalStateException e) {
        throwFailure(applied, e);
      } catch (RuntimeException e) {
        LOG.warn("Application {} settings could not be saved by {}", context.getApplicationId(), context.getUsername(), e);
        throwFailure(applied, new IllegalStateException(APPLICATION_NOT_SAVED));
      }
    }
  }

  /**
   * @return whether the window carries the ownership marker naming itself:
   *         the keys it carries were created for it on this layout version
   */
  public boolean isOwner(ApplicationSettingsContext context) {
    return StringUtils.equals(context.getPreferences().get(OWNER_PREFERENCE), String.valueOf(context.getApplicationId()));
  }

  /**
   * Stores the keys a plugin created for the window with the ownership
   * marker naming the window, every other preference kept
   */
  public void markOwner(ApplicationSettingsContext context, Map<String, String> keys) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    Map<String, String> preferences = new LinkedHashMap<>(keys);
    preferences.put(OWNER_PREFERENCE, String.valueOf(context.getApplicationId()));
    updatePreferences(context, preferences);
  }

  /**
   * Sets the given preferences of the window, every other preference kept,
   * through the page or the site writer, which check the user's edit right
   * on the layout holding the window
   */
  public void updatePreferences(ApplicationSettingsContext context, Map<String, String> preferences) throws IllegalAccessException,
                                                                                                    ObjectNotFoundException {
    List<PortletInstancePreference> values = preferences.entrySet()
                                                        .stream()
                                                        .map(e -> new PortletInstancePreference(e.getKey(), e.getValue()))
                                                        .toList();
    if (context.isSiteContainer()) {
      siteLayoutService.updateSiteApplicationPreferences(context.getSiteKey(),
                                                         context.getApplicationId(),
                                                         values,
                                                         context.getUsername(),
                                                         true);
    } else {
      pageLayoutService.updatePageApplicationPreferences(context.getPageKey(),
                                                         context.getApplicationId(),
                                                         values,
                                                         context.getUsername(),
                                                         true);
    }
    Map<String, String> stored = new LinkedHashMap<>(context.getPreferences());
    preferences.forEach((name, value) -> {
      if (value == null) {
        stored.remove(name);
      } else {
        stored.put(name, value);
      }
    });
    context.setPreferences(stored);
  }

  /**
   * @return a copy of the settings without their null-valued entries, at
   *         every depth of the maps they hold: a null asks for nothing
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> dropNullEntries(Map<String, Object> settings) {
    if (settings == null) {
      return Collections.emptyMap();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    settings.forEach((key, value) -> {
      if (value instanceof Map<?, ?> map) {
        result.put(key, dropNullEntries((Map<String, Object>) map));
      } else if (value instanceof List<?> list) {
        result.put(key,
                   list.stream()
                       .map(item -> item instanceof Map<?, ?> map ? dropNullEntries((Map<String, Object>) map) : item)
                       .toList());
      } else if (value != null) {
        result.put(key, value);
      }
    });
    return result;
  }

  /**
   * @return the child indexes leading from the root to the window, null when
   *         the root does not hold it
   */
  public List<Integer> pathOf(ModelObject root, String storageId) {
    if (root == null) {
      return null; // NOSONAR
    } else if (root instanceof Application application) {
      return StringUtils.equals(application.getStorageId(), storageId) ? new ArrayList<>() : null;
    } else if (root instanceof Container container && CollectionUtils.isNotEmpty(container.getChildren())) {
      for (int i = 0; i < container.getChildren().size(); i++) {
        List<Integer> path = pathOf(container.getChildren().get(i), storageId);
        if (path != null) {
          path.addFirst(i);
          return path;
        }
      }
    }
    return null; // NOSONAR
  }

  /**
   * @return the window at the given child indexes, null when the tree holds
   *         no window there
   */
  public Application atPath(ModelObject root, List<Integer> path) {
    ModelObject current = root;
    for (int index : path) {
      if (!(current instanceof Container container) || container.getChildren() == null
          || container.getChildren().size() <= index) {
        return null;
      }
      current = container.getChildren().get(index);
    }
    return current instanceof Application application ? application : null;
  }

  private Map<String, String> preferencesOf(Application window) {
    Portlet portlet = layoutService.load(window.getState());
    Map<String, String> preferences = new LinkedHashMap<>();
    if (portlet != null) {
      StreamSupport.stream(portlet.spliterator(), false)
                   .filter(p -> p.getValue() != null)
                   .forEach(p -> preferences.put(p.getName(), p.getValue()));
    }
    return preferences;
  }

  /**
   * @return the option holding a null list item, at any depth, null when
   *         none does
   */
  @SuppressWarnings("unchecked")
  private String findNullListItem(Map<String, Object> settings) {
    for (Map.Entry<String, Object> entry : settings.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> map) {
        String option = findNullListItem((Map<String, Object>) map);
        if (option != null) {
          return option;
        }
      } else if (entry.getValue() instanceof List<?> list) {
        if (list.stream().anyMatch(Objects::isNull)) {
          return entry.getKey();
        }
        for (Object item : list) {
          String option = item instanceof Map<?, ?> map ? findNullListItem((Map<String, Object>) map) : null;
          if (option != null) {
            return option;
          }
        }
      }
    }
    return null;
  }

  private ApplicationSettingsPlugin getPlugin(String contentId) {
    String portletName = StringUtils.substringAfter(StringUtils.defaultString(contentId), "/");
    return StringUtils.isBlank(portletName) ? null : plugins.get(portletName);
  }

  private void applyLocked(ApplicationSettingsPlugin plugin,
                           ApplicationSettingsContext context,
                           Map<String, Object> settings) throws IllegalAccessException, ObjectNotFoundException {
    String layoutKey = context.isSiteContainer() ? String.valueOf(context.getSiteKey()) : context.getPageKey().format();
    ReentrantLock lock = locks[Math.floorMod(Objects.hash(layoutKey, context.getApplicationId()), LOCK_STRIPES)];
    boolean locked;
    try {
      locked = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(LAYOUT_BUSY);
    }
    if (!locked) {
      throw new IllegalStateException(LAYOUT_BUSY);
    }
    try {
      Application window = layoutService.getApplicationModel(String.valueOf(context.getApplicationId()));
      if (window != null) {
        context.setPreferences(preferencesOf(window));
      }
      plugin.apply(context, settings);
    } finally {
      lock.unlock();
    }
  }

  private <E extends Exception> void throwFailure(List<String> applied, E failure) throws E {
    if (applied.isEmpty()) {
      throw failure;
    }
    throw new IllegalStateException(String.format(APPLIED_BEFORE_FAILURE, String.join(", ", applied), failure.getMessage()));
  }

  private long getSpaceId(SiteKey siteKey) {
    if (siteKey == null || siteKey.getType() != SiteType.GROUP) {
      return 0;
    }
    Space space = spaceService.getSpaceByGroupId(siteKey.getName());
    return space == null ? 0 : Long.parseLong(space.getId());
  }

}
