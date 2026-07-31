/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.entity.SiteEntity;

import io.meeds.layout.model.LayoutModel;

import jakarta.servlet.http.HttpServletRequest;

public class RestEntityBuilder {

  private RestEntityBuilder() {
  }

  public static SiteEntity toSiteEntity(PortalConfig site,
                                        HttpServletRequest request,
                                        Locale locale,
                                        UserPortalConfigService portalConfigService) throws Exception {
    SiteEntity siteEntity = EntityBuilder.buildSiteEntity(site,
                                                          request,
                                                          true,
                                                          null,
                                                          false,
                                                          false,
                                                          false,
                                                          locale);
    if (siteEntity != null) {
      markRestorableNodes(siteEntity.getSiteNavigations(), portalConfigService);
    }
    return siteEntity;
  }

  /**
   * Flags each navigation node whose page is a default/product page, so the UI can offer to restore
   * the page's layout to its shipped default only where it makes sense.
   */
  private static void markRestorableNodes(List<UserNodeRestEntity> nodes,
                                          UserPortalConfigService portalConfigService) {
    if (CollectionUtils.isEmpty(nodes)) {
      return;
    }
    for (UserNodeRestEntity node : nodes) {
      if (node.getPageKey() != null) {
        node.setCanRestoreLayout(portalConfigService.isDefaultPage(node.getPageKey()));
      }
      markRestorableNodes(node.getChildren(), portalConfigService);
    }
  }

  public static LayoutModel toLayoutModel(ModelObject modelObject,
                                          LayoutService layoutService,
                                          String expand) {
    LayoutModel layoutModel = new LayoutModel(modelObject);
    if (StringUtils.contains(expand, "contentId")
        && modelObject instanceof Container container) {
      Map<String, String> contentIds = new HashMap<>();
      computeApplicationContentId(layoutService, container.getChildren(), contentIds);
      applyApplicationContentId(layoutModel.getChildren(), contentIds);
    }
    return layoutModel;
  }

  private static void computeApplicationContentId(LayoutService layoutService,
                                                  ArrayList<ModelObject> children,
                                                  Map<String, String> contentIds) {
    if (CollectionUtils.isEmpty(children)) {
      return;
    }
    for (ModelObject layoutModel : children) {
      if (layoutModel instanceof Container container) {
        computeApplicationContentId(layoutService, container.getChildren(), contentIds);
      } else if (layoutModel instanceof Application application) { // NOSONAR
        String portletId = application.getStorageId() == null ? application.getId() : application.getStorageId();
        if (StringUtils.isNotBlank(portletId)) {
          String contentId = layoutService.getId(application.getState());
          contentIds.put(portletId, contentId);
        }
      }
    }
  }

  private static void applyApplicationContentId(List<LayoutModel> children,
                                                Map<String, String> contentIds) {
    if (CollectionUtils.isEmpty(children)) {
      return;
    }
    for (LayoutModel layoutModel : children) {
      if (contentIds.containsKey(layoutModel.getStorageId())) {
        layoutModel.setContentId(contentIds.get(layoutModel.getStorageId()));
      } else if (CollectionUtils.isNotEmpty(layoutModel.getChildren())) { // NOSONAR
        applyApplicationContentId(layoutModel.getChildren(), contentIds);
      }
    }
  }

}
