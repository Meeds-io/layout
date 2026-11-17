/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import java.util.Map;

/**
 * What a block inherits from its ancestors by custom property.
 *
 * @param colors their stored foreground colors
 * @param appBackground the application background color they set
 * @param image whether a container lies over an image or a gradient of an
 *          ancestor container (never read for an application, which paints the
 *          application background)
 * @param appStyle the application background values they set, by field
 */
public record InheritedStyle(Map<String, String> colors, String appBackground, boolean image, Map<String, String> appStyle) {
}
