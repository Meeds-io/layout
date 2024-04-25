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
package io.meeds.layout.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.service.LayoutAclService;
import io.meeds.social.translation.plugin.TranslationPlugin;

@Component
public class PageTemplateTranslationPlugin extends TranslationPlugin {

  public static final String OBJECT_TYPE            = "pageTemplate";

  public static final String DESCRIPTION_FIELD_NAME = "description";

  public static final String TITLE_FIELD_NAME       = "title";

  @Autowired
  private LayoutAclService   layoutAclService;

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasEditPermission(long templateId, String username) throws ObjectNotFoundException {
    return layoutAclService.isAdministrator(username);
  }

  @Override
  public boolean hasAccessPermission(long templateId, String username) throws ObjectNotFoundException {
    return true;
  }

  @Override
  public long getAudienceId(long programId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(long programId) throws ObjectNotFoundException {
    return 0;
  }

}
