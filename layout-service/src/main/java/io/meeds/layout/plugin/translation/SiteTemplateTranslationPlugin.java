/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.service.LayoutAclService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SiteTemplateTranslationPlugin extends TranslationPlugin {

  public static final String OBJECT_TYPE            = "siteTemplate";

  public static final String DESCRIPTION_FIELD_NAME = "description";

  public static final String TITLE_FIELD_NAME       = "title";

  @Autowired
  private LayoutAclService   layoutAclService;

  @Autowired
  private TranslationService translationService;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasEditPermission(String id, String username) {
    return layoutAclService.isAdministrator(username);
  }

  @Override
  public boolean hasAccessPermission(String id, String username) {
    return true;
  }

  @Override
  public long getAudienceId(String id) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String id) throws ObjectNotFoundException {
    return 0;
  }

}
