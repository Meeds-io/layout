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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.layout.mcp.constant.BackgroundMode;

/**
 * mode is solid, linear, radial or angular; from, to, ratio and direction
 * carry a gradient; scrollColor the colour a sticky site container takes once
 * scrolled; effect is the stored CSS when it matches none of the editor's
 * gradient forms.
 */
@JsonInclude(Include.NON_NULL)
public record BackgroundBlock(@JsonProperty("enabled") boolean enabled,
                              @JsonProperty("mode") BackgroundMode mode,
                              @JsonProperty("color") String color,
                              @JsonProperty("from") String from,
                              @JsonProperty("to") String to,
                              @JsonProperty("ratio") Integer ratio,
                              @JsonProperty("direction") String direction,
                              @JsonProperty("scroll_color") String scrollColor,
                              @JsonProperty("effect") String effect,
                              @JsonProperty("image") BackgroundImageBlock image) {
}
