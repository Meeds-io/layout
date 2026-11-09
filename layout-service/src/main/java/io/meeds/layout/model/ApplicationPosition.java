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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Where move_layout_application puts an application, as the read numbers
 * it, every number counting from 1: in a dynamic section, the section, its
 * column and, at most one of them, the index among the column's applications
 * or the application it goes before or after, the end of the column
 * otherwise; in a grid section, the section and the row and column of the
 * cell; in a site container, the container and, at most one of them, the
 * index, before or after, the end of the container otherwise
 */
public record ApplicationPosition(@JsonProperty("section_id") String sectionId,
                                  @JsonProperty("container_id") String containerId,
                                  @JsonProperty("column") Integer column,
                                  @JsonProperty("row") Integer row,
                                  @JsonProperty("index") Integer index,
                                  @JsonProperty("before") String before,
                                  @JsonProperty("after") String after) {
}
