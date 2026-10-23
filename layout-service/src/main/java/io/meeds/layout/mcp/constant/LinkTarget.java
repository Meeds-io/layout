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

/**
 * Where the menus open a navigation item's page (a page, an existing page or
 * a link), as the drawer's "open in the same tab" switch stores it on the
 * node
 */
public enum LinkTarget {
  @JsonProperty("same_tab")
  SAME_TAB("same_tab", "SAME_TAB"),
  @JsonProperty("new_tab")
  NEW_TAB("new_tab", "NEW_TAB");

  public static final String TARGET_NOT_ACCEPTED = "value is not accepted for target";

  private final String       value;

  private final String       stateValue;

  LinkTarget(String value, String stateValue) {
    this.value = value;
    this.stateValue = stateValue;
  }

  /**
   * @param  value the value an agent sent, null for the drawer's default
   * @return the matching target
   * @throws IllegalArgumentException with the tool's fixed message when the
   *                                    value is outside the vocabulary
   */
  public static LinkTarget parse(String value) {
    if (StringUtils.isBlank(value)) {
      return SAME_TAB;
    }
    return Arrays.stream(values())
                 .filter(t -> t.value.equalsIgnoreCase(value.trim()))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException(TARGET_NOT_ACCEPTED));
  }

  /**
   * @param  stateValue the value a node state stores, null included
   * @return            the target the menus apply: the same tab on the
   *                    drawer's value for it, a new tab on anything else
   */
  public static LinkTarget fromStateValue(String stateValue) {
    return SAME_TAB.stateValue.equals(stateValue) ? SAME_TAB : NEW_TAB;
  }

  public String getValue() {
    return value;
  }

  /**
   * @return the value the node state stores
   */
  public String getStateValue() {
    return stateValue;
  }
}
