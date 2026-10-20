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

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.portal.navigation.constant.SidebarMode;

/**
 * The sidebar modes as the tools name them, one per platform
 * {@link SidebarMode}.
 */
public enum SidebarDisplayMode {
  @JsonProperty("hidden")
  HIDDEN("hidden", SidebarMode.HIDDEN),
  @JsonProperty("icon")
  ICON("icon", SidebarMode.ICON),
  @JsonProperty("sticky")
  STICKY("sticky", SidebarMode.STICKY);

  private static final String VALUE_NOT_ACCEPTED = "value is not accepted for ";

  private final String        value;

  private final SidebarMode   mode;

  SidebarDisplayMode(String value, SidebarMode mode) {
    this.value = value;
    this.mode = mode;
  }

  /**
   * @param  value  the value an agent sent
   * @param  option the name of the option it was sent for, named in the
   *                  refusal
   * @return the matching mode
   * @throws IllegalArgumentException with the tool's fixed message when the
   *                                    value is blank or outside the
   *                                    vocabulary
   */
  public static SidebarDisplayMode parse(String value, String option) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(VALUE_NOT_ACCEPTED + option);
    }
    return Arrays.stream(values())
                 .filter(m -> m.value.equalsIgnoreCase(value.trim()))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException(VALUE_NOT_ACCEPTED + option));
  }

  /**
   * @param  mode a platform mode, null when none is stored
   * @return the tool's name of that mode, null for null
   */
  public static String valueOf(SidebarMode mode) {
    return mode == null ? null : Arrays.stream(values()).filter(m -> m.mode == mode).findFirst().map(m -> m.value).orElse(null);
  }

  public String getValue() {
    return value;
  }

  public SidebarMode getMode() {
    return mode;
  }
}
