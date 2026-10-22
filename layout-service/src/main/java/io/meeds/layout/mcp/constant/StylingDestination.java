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
package io.meeds.layout.mcp.constant;

/**
 * The validity matrix of the styling blocks by destination, as the editor
 * offers them: a block offered to no destination of a kind is neither read
 * nor accepted there. A site container hides on mobile only; every other
 * destination with a display choice hides on either device.
 */
public enum StylingDestination {

  DYNAMIC_SECTION(false, false, false, false, false, true, true),
  GRID_SECTION(false, false, false, false, false, false, false),
  APPLICATION(true, true, true, true, true, true, true),
  GRID_CELL_APPLICATION(true, true, true, true, false, true, true),
  SITE_CONTAINER_APPLICATION(true, true, true, true, false, true, true),
  BANNER(false, false, false, true, false, true, false),
  SIDEBAR(false, false, false, true, false, true, false);

  private final boolean application;

  private final boolean border;

  private final boolean corners;

  private final boolean text;

  private final boolean height;

  private final boolean display;

  private final boolean desktopHiding;

  StylingDestination(boolean application, // NOSONAR
                     boolean border,
                     boolean corners,
                     boolean text,
                     boolean height,
                     boolean display,
                     boolean desktopHiding) {
    this.application = application;
    this.border = border;
    this.corners = corners;
    this.text = text;
    this.height = height;
    this.display = display;
    this.desktopHiding = desktopHiding;
  }

  public boolean isApplication() {
    return application;
  }

  public boolean hasBorder() {
    return border;
  }

  public boolean hasCorners() {
    return corners;
  }

  public boolean hasText() {
    return text;
  }

  public boolean hasHeight() {
    return height;
  }

  public boolean hasDisplay() {
    return display;
  }

  public boolean hasDesktopHiding() {
    return desktopHiding;
  }

}
