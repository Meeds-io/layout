/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a caller outside the navigation drawer asks to change on a navigation
 * item: a null field keeps the stored value. The schedule, when given, makes
 * the item visible on its bounds, a cleared one drops a scheduled item's
 * bounds and leaves any other visibility as it is;
 * a new page replaces the item's page by one created from the model, the
 * detach flag by none, a page reference by that page; the link rewrites the
 * item's LINK page; the permissions are written on the item's page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemUpdate {

  private String              label;

  private Map<String, String> labels;

  private String              icon;

  private Boolean             visible;

  private NodeSchedule        schedule;

  private String              pageRef;

  private boolean             detachPage;

  private PageCreateModel     newPage;

  private String              link;

  private String              target;

  private List<String>        accessPermissions;

  private String              editPermission;

}
