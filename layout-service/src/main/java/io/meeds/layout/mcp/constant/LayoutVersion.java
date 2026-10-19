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

public enum LayoutVersion {
  @JsonProperty("draft")
  DRAFT("draft"),
  @JsonProperty("published")
  PUBLISHED("published");

  private final String value;

  LayoutVersion(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * @param  value the value an agent sent, null or blank when omitted
   * @return the matching version, null when the value is omitted
   * @throws IllegalArgumentException with the tool's fixed message when the
   *                                    value is outside the vocabulary
   */
  public static LayoutVersion parse(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    return Arrays.stream(values())
                 .filter(v -> v.value.equalsIgnoreCase(value.trim()))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException("value is not accepted for version"));
  }
}
