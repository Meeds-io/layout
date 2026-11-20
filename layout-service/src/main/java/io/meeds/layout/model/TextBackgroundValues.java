/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.model;

/**
 * What a text background block that is on writes: the colour, the effect, the
 * padding and the radius, then the image and its options; a null image clears
 * the stored one, a stored image repeated keeps its stored options, a new
 * image carries the options the call gave
 */
public record TextBackgroundValues(String color,
                                   String effect,
                                   String padding,
                                   String radius,
                                   String image,
                                   boolean newImage,
                                   String size,
                                   String repeat,
                                   String position) {

  public static TextBackgroundValues off() {
    return new TextBackgroundValues(null, null, null, null, null, false, null, null, null);
  }

}
