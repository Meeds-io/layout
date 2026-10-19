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
package io.meeds.layout.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public record SiteDescription(@JsonProperty("site_id") long siteId,
                              @JsonProperty("type") String type,
                              @JsonProperty("name") String name,
                              @JsonProperty("label") String label,
                              @JsonProperty("description") String description,
                              @JsonProperty("banner_present") boolean bannerPresent,
                              @JsonProperty("displayed") boolean displayed,
                              @JsonProperty("access_permissions") List<String> accessPermissions,
                              @JsonProperty("edit_permission") String editPermission,
                              @JsonProperty("draft") boolean draft,
                              @JsonProperty("left_sidebar") SiteContainerDescription leftSidebar,
                              @JsonProperty("right_sidebar") SiteContainerDescription rightSidebar,
                              @JsonProperty("topbars") List<SiteContainerDescription> topbars,
                              @JsonProperty("bottom_bars") List<SiteContainerDescription> bottomBars) {
}
