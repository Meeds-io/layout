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
package io.meeds.layout.plugin.translation;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortletInstanceCategoryTranslationPlugin extends TranslationPlugin {

  public static final String     OBJECT_TYPE      = "portletInstanceCategory";

  public static final String     TITLE_FIELD_NAME = "title";

  @Autowired
  private LayoutAclService       layoutAclService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private TranslationService     translationService;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasEditPermission(long id, String username) throws ObjectNotFoundException {
    return layoutAclService.isAdministrator(username);
  }

  @Override
  public boolean hasAccessPermission(long id, String username) throws ObjectNotFoundException {
    PortletInstanceCategory category = portletInstanceService.getPortletInstanceCategory(id);
    if (category == null) {
      throw new ObjectNotFoundException("Portlet instance category not found");
    }
    List<String> permissions = category.getPermissions();
    return CollectionUtils.isEmpty(permissions)
           || permissions.stream().anyMatch(p -> layoutAclService.isMemberOf(username, p));
  }

  @Override
  public long getAudienceId(long templateId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(long templateId) throws ObjectNotFoundException {
    return 0;
  }

}
