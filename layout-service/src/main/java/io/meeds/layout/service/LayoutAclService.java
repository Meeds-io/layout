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
package io.meeds.layout.service;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

@Service
public class LayoutAclService {

  @Autowired
  private LayoutService   layoutService;

  @Autowired
  private IdentityManager identityManager;

  @Autowired
  private UserACL         userAcl;

  public boolean canAddSite(String username) {
    return userAcl.hasCreatePortalPermission(userAcl.getUserIdentity(username));
  }

  public boolean canEditSite(SiteKey siteKey, String username) {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      return false;
    }
    return canEditSite(portalConfig, username);
  }

  public boolean canEditSite(PortalConfig portalConfig, String username) {
    return userAcl.hasEditPermission(portalConfig, userAcl.getUserIdentity(username));
  }

  public boolean canViewSite(SiteKey siteKey, String username) {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      return false;
    }
    return userAcl.hasAccessPermission(portalConfig, userAcl.getUserIdentity(username));
  }

  public boolean canEditNavigation(SiteKey siteKey, String username) {
    return canEditSite(siteKey, username);
  }

  public boolean canViewNavigation(SiteKey siteKey, PageKey pageKey, String username) {
    return canViewSite(siteKey, username) && (pageKey == null || canViewPage(pageKey, username));
  }

  public boolean canViewPage(PageKey pageKey, String username) {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      return false;
    }
    return userAcl.hasAccessPermission(page, userAcl.getUserIdentity(username));
  }

  public boolean canEditPage(PageKey pageKey, String username) {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      return false;
    }
    return userAcl.hasEditPermission(page, userAcl.getUserIdentity(username));
  }

  public boolean hasAccessPermission(ModelObject modelObject, String username) {
    return switch (modelObject) {
    case Application application:
      yield hasAccessPermission(application.getAccessPermissions(), username);
    case Page page:
      yield canViewPage(new PageKey(page.getOwnerType(), page.getOwnerId(), page.getName()), username);
    case Container container:
      yield hasAccessPermission(container.getAccessPermissions(), username);
    case PortalConfig portalConfig:
      yield canViewSite(new SiteKey(portalConfig.getType(), portalConfig.getName()), username);
    default:
      yield true;
    };
  }

  public boolean isAdministrator(String username) {
    return userAcl.isAdministrator(userAcl.getUserIdentity(username));
  }

  public boolean hasPermission(String username, String expression) {
    return userAcl.hasPermission(userAcl.getUserIdentity(username), expression);
  }

  public String getAdministratorsGroup() {
    return userAcl.getAdminGroups();
  }

  public ConversationState getSuperUserConversationState() {
    return new ConversationState(userAcl.getUserIdentity(userAcl.getSuperUser()));
  }

  public long getSuperUserIdentityId() {
    Identity userIdentity = identityManager.getOrCreateUserIdentity(userAcl.getSuperUser());
    return userIdentity == null ? 0l : Long.parseLong(userIdentity.getId());
  }

  private boolean hasAccessPermission(String[] accessPermissions, String username) {
    return ArrayUtils.isEmpty(accessPermissions)
           || isAdministrator(username)
           || Arrays.stream(accessPermissions)
                    .anyMatch(permission -> hasPermission(username, permission));
  }

}
