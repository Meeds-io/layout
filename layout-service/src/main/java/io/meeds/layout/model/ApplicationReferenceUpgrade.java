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
package io.meeds.layout.model;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
public class ApplicationReferenceUpgrade {

  private String  modificationType       = ApplicationReferenceModificationType.UPDATE.name();

  private String  oldContentId;

  private String  newContentId;

  private boolean upgradePages           = true;

  private boolean upgradePortletInstance = true;

  public boolean isModification() {
    return StringUtils.equalsIgnoreCase(ApplicationReferenceModificationType.UPDATE.name(), modificationType);
  }

  public boolean isRemoval() {
    return StringUtils.equalsIgnoreCase(ApplicationReferenceModificationType.REMOVE.name(), modificationType);
  }

  public enum ApplicationReferenceModificationType {
    REMOVE, UPDATE;
  }
}
