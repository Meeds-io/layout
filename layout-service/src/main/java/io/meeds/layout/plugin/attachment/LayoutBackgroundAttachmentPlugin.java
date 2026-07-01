/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.layout.plugin.attachment;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.service.LayoutAclService;

import jakarta.annotation.PostConstruct;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LayoutBackgroundAttachmentPlugin extends AttachmentPlugin {

  public static final String  OBJECT_TYPE = "containerBackground";

  private static final String PAGE_PREFIX = "page_";

  private static final String SITE_PREFIX = "site_";

  @Autowired
  private LayoutAclService    layoutAclService;

  @Autowired
  private LayoutService       layoutService;

  @Autowired
  private AttachmentService   attachmentService;

  @PostConstruct
  public void init() {
    attachmentService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasEditPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    if (StringUtils.contains(entityId, SITE_PREFIX)) {
      SiteKey siteKey = getSiteKey(entityId);
      return layoutAclService.canEditSite(siteKey, getUsername(userIdentity));
    } else {
      PageKey pageKey = getPageKey(entityId);
      return pageKey != null && layoutAclService.canEditPage(pageKey, getUsername(userIdentity));
    }
  }

  @Override
  public boolean hasAccessPermission(Identity userIdentity, String entityId) throws ObjectNotFoundException {
    if (StringUtils.contains(entityId, SITE_PREFIX)) {
      SiteKey siteKey = getSiteKey(entityId);
      return siteKey.getType() == SiteType.GROUP_TEMPLATE
             || siteKey.getType() == SiteType.PORTAL_TEMPLATE
             || layoutAclService.canViewSite(siteKey, getUsername(userIdentity));
    } else {
      PageKey pageKey = getPageKey(entityId);
      if (pageKey == null) {
        return false;
      } else {
        SiteKey siteKey = pageKey.getSite();
        return siteKey.getType() == SiteType.GROUP_TEMPLATE
               || siteKey.getType() == SiteType.PORTAL_TEMPLATE
               || layoutAclService.canViewPage(pageKey, getUsername(userIdentity));
      }
    }
  }

  @Override
  public long getAudienceId(String objectId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String objectId) throws ObjectNotFoundException {
    return 0;
  }

  private PageKey getPageKey(String entityId) {
    String pageUuid = entityId.split("_")[0].replace(PAGE_PREFIX, "");
    long pageId = StringUtils.isNumeric(pageUuid) ? Long.parseLong(pageUuid) : 0;
    if (pageId == 0) {
      pageUuid = entityId.replace(PAGE_PREFIX, "").split("_")[0];
      pageId = StringUtils.isNumeric(pageUuid) ? Long.parseLong(pageUuid) : 0;
    }
    if (pageId == 0) {
      return null;
    } else {
      Page page = layoutService.getPage(pageId);
      return page == null ? null : page.getPageKey();
    }
  }

  private SiteKey getSiteKey(String entityId) {
    String siteUuid = entityId.replace(SITE_PREFIX, "").split("_")[0];
    long siteId = StringUtils.isNumeric(siteUuid) ? Long.parseLong(siteUuid) : 0;
    PortalConfig site = layoutService.getPortalConfig(siteId);
    return new SiteKey(site.getType(), site.getName());
  }

  private String getUsername(Identity userIdentity) {
    return userIdentity == null ? null : userIdentity.getUserId();
  }

}
