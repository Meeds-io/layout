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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A part of the Branding and Theme administration, one per drawer of the
 * Branding page: each part accepts exactly the options its drawer writes,
 * the theme variables as named in the platform's branding configuration and,
 * for the page part, the page fields of the branding itself.
 */
public enum ThemePart {
  @JsonProperty("colors")
  COLORS("colors", ThemeOptions.COLORS, Set.of()),
  @JsonProperty("font")
  FONT("font", ThemeOptions.FONT, Set.of()),
  @JsonProperty("topbar")
  TOPBAR("topbar", ThemeOptions.TOPBAR, Set.of()),
  @JsonProperty("sidebars")
  SIDEBARS("sidebars", ThemeOptions.SIDEBARS, Set.of()),
  @JsonProperty("drawers")
  DRAWERS("drawers", ThemeOptions.DRAWERS, Set.of()),
  @JsonProperty("page_and_apps")
  PAGE_AND_APPS("page_and_apps", ThemeOptions.PAGE_AND_APPS, ThemeOptions.PAGE_FIELDS);

  public static final String  PART_NOT_ACCEPTED = "value is not accepted for part";

  private final String        value;

  private final Set<String>   themeOptions;

  private final Set<String>   brandingFields;

  ThemePart(String value, Set<String> themeOptions, Set<String> brandingFields) {
    this.value = value;
    this.themeOptions = themeOptions;
    this.brandingFields = brandingFields;
  }

  /**
   * @param  value the value an agent sent
   * @return the matching part
   * @throws IllegalArgumentException with the tool's fixed message when the
   *                                    value is omitted or outside the
   *                                    vocabulary
   */
  public static ThemePart parse(String value) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(PART_NOT_ACCEPTED);
    }
    return Arrays.stream(values())
                 .filter(p -> p.value.equalsIgnoreCase(value.trim()))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException(PART_NOT_ACCEPTED));
  }

  public String getValue() {
    return value;
  }

  /**
   * @return the theme variables the part's drawer writes
   */
  public Set<String> getThemeOptions() {
    return themeOptions;
  }

  /**
   * @return the fields of the branding itself the part's drawer writes
   */
  public Set<String> getBrandingFields() {
    return brandingFields;
  }

  public boolean isBrandingField(String option) {
    return brandingFields.contains(option);
  }
}
