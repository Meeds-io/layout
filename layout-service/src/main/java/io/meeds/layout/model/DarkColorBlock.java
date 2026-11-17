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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A block (page, section or application) whose colours, as set and not yet
 * saved in the page editor, get their dark value calculated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DarkColorBlock {

  /** The block's own colours by field (backgroundColor, textColor, iconColor...) */
  private Map<String, String> colors;

  /**
   * The background the text, icon and border colours sit on, when it is not the
   * block's own backgroundColor (the background an application inherits).
   * Blank: the block's backgroundColor.
   */
  private String              background;

  /** The block's own background is an image or a gradient with no colour */
  private boolean             image;

  /** The block is a page, whose background takes the page background default */
  private boolean             page;

}
