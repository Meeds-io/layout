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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.layout.mcp.constant.NavigationItemType;

@JsonInclude(Include.NON_NULL)
public record NavigationItemDescription(@JsonProperty("node_id") long nodeId,
                                        @JsonProperty("name") String name,
                                        @JsonProperty("labels") Map<String, String> labels,
                                        @JsonProperty("url_name") String urlName,
                                        @JsonProperty("type") NavigationItemType type,
                                        @JsonProperty("page_ref") String pageRef,
                                        @JsonProperty("url") String url,
                                        @JsonProperty("target") String target,
                                        @JsonProperty("icon") String icon,
                                        @JsonProperty("visible") boolean visible,
                                        @JsonProperty("scheduled") ScheduleDescription scheduled,
                                        @JsonProperty("access_permissions") List<String> accessPermissions,
                                        @JsonProperty("edit_permission") String editPermission,
                                        @JsonProperty("children") List<NavigationItemDescription> children) {
}
