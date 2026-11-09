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
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;

/**
 * Removes a background color from the top-level containers of named pages,
 * only where the stored color equals the one given in parameter, ignoring
 * case. A page container color is painted over the page background set in
 * Branding and Theme, so a color shipped by default on a page hides that
 * branding setting. A stored color equal to the parameter cannot be told apart
 * from the shipped one and is removed whoever set it; any other color is kept,
 * and nested containers are not read. A page that changes is saved whole
 * through {@link LayoutService}, which updates its existing containers and
 * windows in place, keeping their ids.
 */
public class PageContainerBackgroundUpgradePlugin extends UpgradeProductPlugin {

  public static final String SITE_TYPE_PARAM        = "site.type";

  public static final String SITE_NAME_PARAM        = "site.name";

  public static final String PAGE_NAMES_PARAM       = "page.names";

  public static final String BACKGROUND_COLOR_PARAM = "background.color";

  public static final String ENABLED_PARAM          = "enabled";

  private static final Log   LOG                    = ExoLogger.getLogger(PageContainerBackgroundUpgradePlugin.class);

  private LayoutService      layoutService;

  private boolean            enabled;

  private String             siteType;

  private String             siteName;

  private List<String>       pageNames;

  private String             backgroundColor;

  public PageContainerBackgroundUpgradePlugin(SettingService settingService,
                                              LayoutService layoutService,
                                              InitParams initParams) {
    super(settingService, initParams);
    this.layoutService = layoutService;
    this.siteType = StringUtils.defaultIfBlank(getValueParam(initParams, SITE_TYPE_PARAM), "portal");
    this.siteName = getValueParam(initParams, SITE_NAME_PARAM);
    this.backgroundColor = StringUtils.trim(getValueParam(initParams, BACKGROUND_COLOR_PARAM));
    ValuesParam pageNamesParam = initParams == null ? null : initParams.getValuesParam(PAGE_NAMES_PARAM);
    this.pageNames = pageNamesParam == null ? Collections.emptyList() : pageNamesParam.getValues();
    String enabledValue = getValueParam(initParams, ENABLED_PARAM);
    this.enabled = enabledValue == null || Boolean.parseBoolean(StringUtils.trim(enabledValue));
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return enabled
           && StringUtils.isNotBlank(siteName)
           && StringUtils.isNotBlank(backgroundColor)
           && !pageNames.isEmpty();
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long start = System.currentTimeMillis();
    LOG.info("Start:: Remove page container background color {} from pages {} of site {}:{}",
             backgroundColor,
             pageNames,
             siteType,
             siteName);
    int upgradedPages = 0;
    List<String> failedPages = new ArrayList<>();
    for (String pageName : pageNames) {
      PageKey pageKey = new PageKey(siteType, siteName, StringUtils.trim(pageName));
      try {
        if (upgradePage(pageKey)) {
          upgradedPages++;
        }
      } catch (RuntimeException e) {
        LOG.warn("| -- Page {}: container background color not removed, "
            + "the upgrade will run again at the next startup of a newer version",
                 pageKey.format(),
                 e);
        failedPages.add(pageKey.format());
      }
    }
    LOG.info("End:: Removed page container background color from {} pages, {} failed, in {} ms",
             upgradedPages,
             failedPages.size(),
             System.currentTimeMillis() - start);
    if (!failedPages.isEmpty()) {
      // Thrown so that the upgrade is not recorded as done and runs again at
      // the next startup of a newer version; the pages already saved find no
      // color to remove then
      throw new IllegalStateException("Page container background color not removed from pages " + failedPages);
    }
  }

  /**
   * @param pageKey the page to upgrade
   * @return true when the page was saved without its container background
   *         color
   */
  @ContainerTransactional
  public boolean upgradePage(PageKey pageKey) {
    return removeContainerBackground(pageKey);
  }

  boolean removeContainerBackground(PageKey pageKey) {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      LOG.info("| -- Page {} not found, nothing to upgrade", pageKey.format());
      return false;
    }
    boolean changed = false;
    if (page.getChildren() != null) {
      for (ModelObject child : page.getChildren()) {
        if (child instanceof Container container && removeBackground(container.getCssStyle())) {
          changed = true;
        }
      }
    }
    if (changed) {
      layoutService.save(page);
      LOG.info("| -- Page {}: container background color {} removed", pageKey.format(), backgroundColor);
    }
    return changed;
  }

  private boolean removeBackground(ModelStyle style) {
    if (style != null && StringUtils.equalsIgnoreCase(StringUtils.trim(style.getBackgroundColor()), backgroundColor)) {
      style.setBackgroundColor(null);
      return true;
    }
    return false;
  }

  private static String getValueParam(InitParams initParams, String name) {
    ValueParam valueParam = initParams == null ? null : initParams.getValueParam(name);
    return valueParam == null ? null : valueParam.getValue();
  }

}
