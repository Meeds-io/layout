/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.layout.plugin.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;

/**
 * Removes the style values a site shipped from what still stores them: a
 * value dropped from a site's definition stays stored on every instance that
 * imported the site before, where it overrides the theme in use. On the named
 * pages, the windows of one application (application.content, as
 * app/portlet) are cleared at any depth, or, without it, the pages' top-level
 * containers; with site.layout, the containers of the site's layout at any
 * depth. Each value of style.values (a style property, as
 * textTitleColor, and its shipped value) is removed where the stored one
 * equals it, ignoring case: it cannot be told apart from the shipped one and
 * is removed whoever set it, and any other value is kept. A page or a site
 * that changes is saved whole through {@link LayoutService}.
 */
public class ShippedStyleUpgradePlugin extends UpgradeProductPlugin {

  public static final String                                     SITE_TYPE_PARAM           = "site.type";

  public static final String                                     SITE_NAME_PARAM           = "site.name";

  public static final String                                     PAGE_NAMES_PARAM          = "page.names";

  public static final String                                     SITE_LAYOUT_PARAM         = "site.layout";

  public static final String                                     APPLICATION_CONTENT_PARAM = "application.content";

  public static final String                                     STYLE_VALUES_PARAM        = "style.values";

  public static final String                                     ENABLED_PARAM             = "enabled";

  private static final Log                                       LOG                       =
                                                                     ExoLogger.getLogger(ShippedStyleUpgradePlugin.class);

  private static final Map<String, Function<ModelStyle, String>> GETTERS                   =
                                                                         Map.of("backgroundColor",
                                                                                ModelStyle::getBackgroundColor,
                                                                                "textTitleColor",
                                                                                ModelStyle::getTextTitleColor,
                                                                                "textHeaderColor",
                                                                                ModelStyle::getTextHeaderColor,
                                                                                "textColor",
                                                                                ModelStyle::getTextColor,
                                                                                "textSubtitleColor",
                                                                                ModelStyle::getTextSubtitleColor);

  private static final Map<String, BiConsumer<ModelStyle, String>> SETTERS                 =
                                                                           Map.of("backgroundColor",
                                                                                  ModelStyle::setBackgroundColor,
                                                                                  "textTitleColor",
                                                                                  ModelStyle::setTextTitleColor,
                                                                                  "textHeaderColor",
                                                                                  ModelStyle::setTextHeaderColor,
                                                                                  "textColor",
                                                                                  ModelStyle::setTextColor,
                                                                                  "textSubtitleColor",
                                                                                  ModelStyle::setTextSubtitleColor);

  private LayoutService                                          layoutService;

  private boolean                                                enabled;

  private String                                                 siteType;

  private String                                                 siteName;

  private List<String>                                           pageNames;

  private boolean                                                siteLayout;

  private String                                                 applicationContent;

  private Map<String, String>                                    styleValues               = new LinkedHashMap<>();

  public ShippedStyleUpgradePlugin(SettingService settingService, LayoutService layoutService, InitParams initParams) {
    super(settingService, initParams);
    this.layoutService = layoutService;
    this.siteType = StringUtils.defaultIfBlank(getValueParam(initParams, SITE_TYPE_PARAM), "portal");
    this.siteName = StringUtils.trim(getValueParam(initParams, SITE_NAME_PARAM));
    ValuesParam pageNamesParam = initParams == null ? null : initParams.getValuesParam(PAGE_NAMES_PARAM);
    this.pageNames = pageNamesParam == null ? Collections.emptyList() : pageNamesParam.getValues();
    this.siteLayout = Boolean.parseBoolean(StringUtils.trim(getValueParam(initParams, SITE_LAYOUT_PARAM)));
    this.applicationContent = StringUtils.trimToNull(getValueParam(initParams, APPLICATION_CONTENT_PARAM));
    PropertiesParam styleValuesParam = initParams == null ? null : initParams.getPropertiesParam(STYLE_VALUES_PARAM);
    if (styleValuesParam != null) {
      styleValuesParam.getProperties().forEach((property, value) -> {
        if (!GETTERS.containsKey(property)) {
          LOG.warn("Style property {} cannot be cleared by this upgrade, it is ignored", property);
        } else if (StringUtils.isNotBlank(value)) {
          styleValues.put(property, value.trim());
        }
      });
    }
    String enabledValue = getValueParam(initParams, ENABLED_PARAM);
    this.enabled = enabledValue == null || Boolean.parseBoolean(StringUtils.trim(enabledValue));
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return enabled && StringUtils.isNotBlank(siteName) && !styleValues.isEmpty() && (siteLayout || !pageNames.isEmpty());
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long start = System.currentTimeMillis();
    LOG.info("Start:: Remove the shipped style values {} from site {}:{}", styleValues, siteType, siteName);
    int upgraded = 0;
    List<String> failed = new ArrayList<>();
    if (siteLayout) {
      try {
        if (upgradeSiteLayout()) {
          upgraded++;
        }
      } catch (RuntimeException e) {
        LOG.warn("| -- Site {}:{}: shipped style values not removed from its layout, "
            + "the upgrade will run again at the next startup of a newer version", siteType, siteName, e);
        failed.add(siteType + "::" + siteName);
      }
    }
    for (String pageName : pageNames) {
      PageKey pageKey = new PageKey(siteType, siteName, StringUtils.trim(pageName));
      try {
        if (upgradePage(pageKey)) {
          upgraded++;
        }
      } catch (RuntimeException e) {
        LOG.warn("| -- Page {}: shipped style values not removed, the upgrade will run again at the next startup of a newer version",
                 pageKey.format(),
                 e);
        failed.add(pageKey.format());
      }
    }
    LOG.info("End:: Removed the shipped style values from {} pages or layouts, {} failed, in {} ms",
             upgraded,
             failed.size(),
             System.currentTimeMillis() - start);
    if (!failed.isEmpty()) {
      // Thrown so that the upgrade is not recorded as done and runs again at
      // the next startup of a newer version; what was already saved holds no
      // shipped value to remove then
      throw new IllegalStateException("Shipped style values not removed from " + failed);
    }
  }

  /**
   * @param pageKey the page to upgrade
   * @return true when the page was saved without the shipped values
   */
  @ContainerTransactional
  public boolean upgradePage(PageKey pageKey) {
    return clearPage(pageKey);
  }

  /**
   * @return true when the site was saved without the shipped values in its
   *         layout
   */
  @ContainerTransactional
  public boolean upgradeSiteLayout() {
    return clearSiteLayout();
  }

  boolean clearPage(PageKey pageKey) {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      LOG.info("| -- Page {} not found, nothing to upgrade", pageKey.format());
      return false;
    }
    boolean changed = false;
    if (page.getChildren() != null) {
      for (ModelObject child : page.getChildren()) {
        if (applicationContent == null) {
          changed |= child instanceof Container container && clear(container.getCssStyle());
        } else {
          changed |= clearWindows(child);
        }
      }
    }
    if (changed) {
      layoutService.save(page);
      LOG.info("| -- Page {}: shipped style values removed", pageKey.format());
    }
    return changed;
  }

  boolean clearSiteLayout() {
    PortalConfig site = layoutService.getPortalConfig(siteType, siteName);
    if (site == null || site.getPortalLayout() == null) {
      LOG.info("| -- Site {}:{} not found, nothing to upgrade", siteType, siteName);
      return false;
    }
    boolean changed = clearContainers(site.getPortalLayout());
    if (changed) {
      layoutService.save(site);
      LOG.info("| -- Site {}:{}: shipped style values removed from its layout", siteType, siteName);
    }
    return changed;
  }

  private boolean clearWindows(ModelObject object) {
    boolean changed = false;
    if (object instanceof Application application) {
      changed = application.getState() != null && StringUtils.equals(applicationContent, layoutService.getId(application.getState()))
                && clear(application.getCssStyle());
    } else if (object instanceof Container container && container.getChildren() != null) {
      for (ModelObject child : container.getChildren()) {
        changed |= clearWindows(child);
      }
    }
    return changed;
  }

  private boolean clearContainers(Container container) {
    boolean changed = clear(container.getCssStyle());
    if (container.getChildren() != null) {
      for (ModelObject child : container.getChildren()) {
        if (child instanceof Container nested) {
          changed |= clearContainers(nested);
        }
      }
    }
    return changed;
  }

  private boolean clear(ModelStyle style) {
    if (style == null) {
      return false;
    }
    boolean changed = false;
    for (Map.Entry<String, String> shipped : styleValues.entrySet()) {
      if (StringUtils.equalsIgnoreCase(StringUtils.trim(GETTERS.get(shipped.getKey()).apply(style)), shipped.getValue())) {
        SETTERS.get(shipped.getKey()).accept(style, null);
        changed = true;
      }
    }
    return changed;
  }

  private static String getValueParam(InitParams initParams, String name) {
    ValueParam valueParam = initParams == null ? null : initParams.getValueParam(name);
    return valueParam == null ? null : valueParam.getValue();
  }

}
