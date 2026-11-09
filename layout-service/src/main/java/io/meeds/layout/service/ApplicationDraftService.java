/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.CellDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.DraftWindows;
import io.meeds.layout.model.LayoutWrite;

/**
 * Resolves the windows a tool write addresses on the user's layout, the way
 * every draft tool does: the layout read is the user's draft when their
 * co-editing revision names one, the published layout otherwise; an id the
 * read does not show does not exist, a container id is refused; and when no
 * draft exists yet the write creates it first and finds each window again
 * on the draft at the place its live window held. Every refusal is a fixed
 * text, the texts the tools answer.
 */
@Service
public class ApplicationDraftService {

  public static final String       PAGE_NOT_FOUND               = "does not exist: page";

  public static final String       PAGE_NOT_ALLOWED             = "not allowed: page";

  public static final String       DRAFT_NOT_FOUND              = "does not exist: draft";

  public static final String       SITE_NOT_FOUND               = "does not exist: site";

  public static final String       SITE_NOT_ALLOWED             = "not allowed: site";

  public static final String       NODE_NOT_FOUND               = "does not exist: navigation item";

  public static final String       NAVIGATION_NOT_ALLOWED       = "not allowed: navigation";

  public static final String       APPLICATION_NOT_FOUND        = ApplicationSettingsService.APPLICATION_NOT_FOUND;

  public static final String       APPLICATION_ID_NOT_ACCEPTED  = "value is not accepted for application_id";

  public static final String       LAYOUT_OUTDATED              = "layout outdated, re-read it";

  public static final String       DRAFT_CREATED_BEFORE_FAILURE = "draft created, read the layout again for its ids; %s";

  public static final String       PAGE_LAYOUT_NOT_SAVED        = "could not be saved: page layout";

  public static final String       SITE_LAYOUT_NOT_SAVED        = "could not be saved: site layout";

  public static final String       PAGE_OUTDATED_MESSAGE        = "layout.pageOutdatedError";

  private static final Log         LOG                          = ExoLogger.getLogger(ApplicationDraftService.class);

  @Autowired
  private LayoutService            layoutService;

  @Autowired
  private LayoutAclService         aclService;

  @Autowired
  private SiteLayoutService        siteLayoutService;

  @Autowired
  private PageLayoutService        pageLayoutService;

  @Autowired
  private LayoutDescriptionService layoutDescriptionService;

  @Autowired
  private LayoutDraftService       layoutDraftService;

  @Autowired
  private ApplicationSettingsService applicationSettingsService;

  /**
   * The windows the ids name on the user's page, with no write
   *
   * @param pageKey the live page
   * @param pageRef the live page reference as the caller gave it
   * @param applicationIds the application ids the read answered
   * @param username the user
   * @return the resolution
   * @throws ObjectNotFoundException when the page, its navigation item, the
   *           draft or an application does not exist
   * @throws IllegalAccessException when the user may not edit the page
   */
  public DraftWindows resolvePage(PageKey pageKey,
                                  String pageRef,
                                  List<String> applicationIds,
                                  String username) throws ObjectNotFoundException, IllegalAccessException {
    readEditablePage(pageKey, username);
    long liveNodeId = requireLiveNodeId(pageKey, username);
    PageKey draftPageKey = layoutDraftService.getDraftPageKey(liveNodeId, username);
    boolean createDraft = draftPageKey == null;
    PageKey readPageKey = createDraft ? pageKey : draftPageKey;
    ModelObject visible;
    try {
      visible = pageLayoutService.getPageLayout(readPageKey, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(createDraft ? PAGE_NOT_FOUND : DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
    Page stored = pageLayoutService.getPageLayout(readPageKey);
    if (stored == null) {
      throw new ObjectNotFoundException(createDraft ? PAGE_NOT_FOUND : DRAFT_NOT_FOUND);
    }
    PageDescription read = layoutDescriptionService.toPageDescription(visible, pageRef, LayoutVersion.PUBLISHED);
    List<Application> windows = resolveWindows(applicationIds, pageApplicationIds(read), pageContainerIds(read), stored);
    String livePageReference = pageKey.format();
    List<ApplicationSettingsContext> contexts = windows.stream()
                                                       .map(w -> applicationSettingsService.newContext(w,
                                                                                                        readPageKey,
                                                                                                        pageKey.getSite(),
                                                                                                        pageKey.getSite(),
                                                                                                        livePageReference,
                                                                                                        !createDraft,
                                                                                                        username))
                                                       .toList();
    return new DraftWindows(pageKey, liveNodeId, draftPageKey, null, null, null, stored, windows, contexts);
  }

  /**
   * The windows the ids name on the user's site, with no write
   *
   * @param siteId the live site
   * @param applicationIds the application ids the read answered
   * @param username the user
   * @return the resolution
   * @throws ObjectNotFoundException when the site or an application does not
   *           exist
   * @throws IllegalAccessException when the user may not edit the site
   */
  public DraftWindows resolveSite(long siteId,
                                  List<String> applicationIds,
                                  String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig site = readEditableSite(siteId, username);
    PortalConfig draftSite = readDraftSite(siteId, username);
    boolean createDraft = draftSite == null;
    PortalConfig stored = createDraft ? site : draftSite;
    ModelObject storedLayout = layoutService.getPortalConfig(stored.getSiteKey()).getPortalLayout();
    ModelObject visible = layoutDescriptionService.filterAccessible(storedLayout, username);
    SiteDescription read = layoutDescriptionService.toSiteDescription(site, visible, null, null, false);
    List<Application> windows = resolveWindows(applicationIds, siteApplicationIds(read), siteContainerIds(read), stored.getPortalLayout());
    String homePageReference = applicationSettingsService.getSiteHomePageReference(site.getSiteKey());
    List<ApplicationSettingsContext> contexts = windows.stream()
                                                       .map(w -> applicationSettingsService.newContext(w,
                                                                                                        null,
                                                                                                        stored.getSiteKey(),
                                                                                                        site.getSiteKey(),
                                                                                                        homePageReference,
                                                                                                        !createDraft,
                                                                                                        username))
                                                       .toList();
    return new DraftWindows(null, null, null, site, draftSite, homePageReference, stored.getPortalLayout(), windows, contexts);
  }

  /**
   * Creates the user's draft of the resolved layout and finds each window
   * again on it, at the place its live window held and of the same
   * application; the live layout read again after the clone still holds each
   * window at its place, no publication came in between (a publication gives
   * every window a new id), else the layout is outdated. A failure of the
   * mapping names the draft just created, as every failure after it does
   *
   * @param resolution the windows resolved on the live layout
   * @param username the user
   * @return the windows' contexts on the draft, in the ids' order
   * @throws ObjectNotFoundException when the page or the site no longer
   *           exists
   * @throws IllegalAccessException when the user may not create the draft
   */
  public List<ApplicationSettingsContext> createDraft(DraftWindows resolution,
                                                      String username) throws ObjectNotFoundException, IllegalAccessException {
    if (resolution.isPage()) {
      PageKey pageKey = resolution.livePageKey();
      PageKey draftPageKey;
      try {
        draftPageKey = layoutDraftService.createDraftPage(resolution.liveNodeId(), username);
      } catch (ObjectNotFoundException e) {
        throw new ObjectNotFoundException(PAGE_NOT_FOUND);
      } catch (IllegalAccessException e) {
        throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
      } catch (RuntimeException e) {
        throw translateWriteFailure(e, pageKey, username, PAGE_LAYOUT_NOT_SAVED);
      }
      String livePageReference = pageKey.format();
      return onCreatedDraft(() -> toDraftContexts(resolution.windows(),
                                                  resolution.storedLayout(),
                                                  pageLayoutService.getPageLayout(pageKey),
                                                  pageLayoutService.getPageLayout(draftPageKey),
                                                  w -> applicationSettingsService.newContext(w,
                                                                                             draftPageKey,
                                                                                             pageKey.getSite(),
                                                                                             pageKey.getSite(),
                                                                                             livePageReference,
                                                                                             true,
                                                                                             username)));
    }
    PortalConfig site = resolution.liveSite();
    SiteKey draftSiteKey;
    try {
      draftSiteKey = layoutDraftService.createDraftSite(site, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, site.getSiteKey(), username, SITE_LAYOUT_NOT_SAVED);
    }
    String homePageReference = resolution.homePageReference();
    return onCreatedDraft(() -> toDraftContexts(resolution.windows(),
                                                resolution.storedLayout(),
                                                layoutService.getPortalConfig(site.getSiteKey()).getPortalLayout(),
                                                layoutService.getPortalConfig(draftSiteKey).getPortalLayout(),
                                                w -> applicationSettingsService.newContext(w,
                                                                                           null,
                                                                                           draftSiteKey,
                                                                                           site.getSiteKey(),
                                                                                           homePageReference,
                                                                                           true,
                                                                                           username)));
  }

  /**
   * Runs a write on the draft this call created: a failure is answered
   * naming the draft, which stays, so the caller reads the layout again for
   * its ids; a failure the write did not name itself is logged
   *
   * @param <T> what the write returns
   * @param write the write
   * @return what the write returns
   */
  public <T> T onCreatedDraft(LayoutWrite<T> write) {
    try {
      return write.run();
    } catch (ObjectNotFoundException | IllegalAccessException | IllegalArgumentException | IllegalStateException e) {
      throw new IllegalStateException(String.format(DRAFT_CREATED_BEFORE_FAILURE, e.getMessage()), e);
    } catch (RuntimeException e) {
      LOG.warn("Write on the draft just created failed", e);
      throw new IllegalStateException(String.format(DRAFT_CREATED_BEFORE_FAILURE, ApplicationSettingsService.APPLICATION_NOT_SAVED),
                                      e);
    }
  }

  /**
   * A layout write refused by the storage answers a fixed text: the outdated
   * draft its own, every other failure the given one, its cause logged
   *
   * @param e the storage's failure
   * @param layoutKey the page or site key, for the log
   * @param username the user, for the log
   * @param refusal the fixed text every other failure answers
   * @return the exception to throw
   */
  public RuntimeException translateWriteFailure(RuntimeException e, Object layoutKey, String username, String refusal) {
    if (StringUtils.equals(e.getMessage(), SiteLayoutService.SITE_OUTDATED_MESSAGE)
        || StringUtils.equals(e.getMessage(), PAGE_OUTDATED_MESSAGE)) {
      return new IllegalStateException(LAYOUT_OUTDATED);
    }
    LOG.warn("Layout {} could not be written by {}", layoutKey, username, e);
    return new IllegalStateException(refusal);
  }

  /**
   * @param pageKey a live page
   * @param username the user
   * @return the stored live page the user may edit, never a draft page
   * @throws ObjectNotFoundException when no live page has that key
   * @throws IllegalAccessException when the user may not edit it
   */
  public Page readEditablePage(PageKey pageKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    Page page = layoutDraftService.isDraftNode(pageKey.getName()) ? null : layoutService.getPage(pageKey);
    if (page == null) {
      throw new ObjectNotFoundException(PAGE_NOT_FOUND);
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
    return page;
  }

  /**
   * @param pageKey a live page
   * @param username the user
   * @return the navigation node of the page, the key its draft is resolved by
   * @throws ObjectNotFoundException when no navigation item references it
   */
  public long requireLiveNodeId(PageKey pageKey, String username) throws ObjectNotFoundException {
    Long liveNodeId = layoutDraftService.findLiveNodeId(pageKey, username);
    if (liveNodeId == null) {
      throw new ObjectNotFoundException(NODE_NOT_FOUND);
    }
    return liveNodeId;
  }

  /**
   * @param siteId a live site
   * @param username the user
   * @return the live site the user may edit, never a draft site
   * @throws ObjectNotFoundException when no live site has that id
   * @throws IllegalAccessException when the user may not edit it
   */
  public PortalConfig readEditableSite(long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig site;
    try {
      site = siteLayoutService.getSite(siteId, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    }
    if (StringUtils.equalsIgnoreCase(site.getType(), PortalConfig.DRAFT)) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    }
    if (!aclService.canEditSite(site.getSiteKey(), username)) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    }
    return site;
  }

  /**
   * The user's draft of a site, as the site layout editor resumes it: the one
   * the user's co-editing revision names, read through the permission-checked
   * service read
   *
   * @param siteId a live site
   * @param username the user
   * @return the draft site, null when the user has none
   */
  public PortalConfig readDraftSite(long siteId, String username) {
    SiteKey draftSiteKey = layoutDraftService.getDraftSiteKey(siteId, username);
    if (draftSiteKey == null) {
      return null;
    }
    try {
      return siteLayoutService.getSite(draftSiteKey, username);
    } catch (ObjectNotFoundException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * The stored windows the ids name: an application the read shows, a
   * section, cell or container id being refused as not an application, any
   * other id, an application the user cannot see included, as not existing
   */
  private List<Application> resolveWindows(List<String> applicationIds,
                                           Set<String> readApplicationIds,
                                           Set<String> readContainerIds,
                                           ModelObject stored) throws ObjectNotFoundException {
    List<Application> windows = new ArrayList<>();
    for (String applicationId : applicationIds) {
      if (readContainerIds.contains(applicationId)) {
        throw new IllegalArgumentException(APPLICATION_ID_NOT_ACCEPTED);
      }
      List<Integer> path = readApplicationIds.contains(applicationId) ? applicationSettingsService.pathOf(stored, applicationId)
                                                                      : null;
      if (path == null) {
        throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
      }
      windows.add(applicationSettingsService.atPath(stored, path));
    }
    return windows;
  }

  private List<ApplicationSettingsContext> toDraftContexts(List<Application> liveWindows,
                                                           ModelObject liveLayout,
                                                           ModelObject liveLayoutAfterClone,
                                                           ModelObject draftLayout,
                                                           Function<Application, ApplicationSettingsContext> context) {
    return liveWindows.stream().map(window -> {
      List<Integer> path = applicationSettingsService.pathOf(liveLayout, window.getStorageId());
      Application liveWindowAfterClone = liveLayoutAfterClone == null ? null
                                                                      : applicationSettingsService.atPath(liveLayoutAfterClone, path);
      Application draftWindow = draftLayout == null ? null : applicationSettingsService.atPath(draftLayout, path);
      if (liveWindowAfterClone == null || !StringUtils.equals(liveWindowAfterClone.getStorageId(), window.getStorageId())
          || draftWindow == null || !StringUtils.isNumeric(draftWindow.getStorageId())
          || !StringUtils.equals(layoutService.getId(draftWindow.getState()), layoutService.getId(window.getState()))) {
        throw new IllegalStateException(LAYOUT_OUTDATED);
      }
      return context.apply(draftWindow);
    }).toList();
  }

  private Set<String> pageApplicationIds(PageDescription page) {
    Set<String> ids = new HashSet<>();
    page.sections().forEach(section -> {
      if (section.columnsContent() != null) {
        section.columnsContent().forEach(column -> column.forEach(a -> ids.add(a.applicationId())));
      }
      if (section.cells() != null) {
        section.cells().stream().map(CellDescription::application).filter(Objects::nonNull).forEach(a -> ids.add(a.applicationId()));
      }
    });
    return ids;
  }

  private Set<String> pageContainerIds(PageDescription page) {
    Set<String> ids = new HashSet<>();
    page.sections().forEach(section -> {
      ids.add(section.sectionId());
      if (section.cells() != null) {
        section.cells().forEach(cell -> ids.add(cell.cellId()));
      }
    });
    return ids;
  }

  private Set<String> siteApplicationIds(SiteDescription site) {
    Set<String> ids = new HashSet<>();
    siteContainers(site).forEach(container -> siteApplications(container).stream()
                                                                         .map(ApplicationDescription::applicationId)
                                                                         .forEach(ids::add));
    return ids;
  }

  private Set<String> siteContainerIds(SiteDescription site) {
    Set<String> ids = new HashSet<>();
    siteContainers(site).forEach(container -> {
      ids.add(container.containerId());
      if (container.columns() != null) {
        container.columns().forEach(column -> ids.add(column.columnId()));
      }
    });
    return ids;
  }

  private List<ApplicationDescription> siteApplications(SiteContainerDescription container) {
    List<ApplicationDescription> applications = new ArrayList<>();
    if (container.applications() != null) {
      applications.addAll(container.applications());
    }
    if (container.columns() != null) {
      container.columns().forEach(column -> applications.addAll(column.applications()));
    }
    return applications;
  }

  private List<SiteContainerDescription> siteContainers(SiteDescription site) {
    List<SiteContainerDescription> containers = new ArrayList<>();
    if (site.leftSidebar() != null) {
      containers.add(site.leftSidebar());
    }
    if (site.rightSidebar() != null) {
      containers.add(site.rightSidebar());
    }
    containers.addAll(site.topbars());
    containers.addAll(site.bottomBars());
    return containers;
  }

}
