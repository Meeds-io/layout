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
package io.meeds.layout.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.meeds.layout.mcp.constant.ThemeOptions;

/**
 * The vocabulary of the page fields of the branding, as the Page drawer
 * writes them: a width from 100 to 3000 pixels or the full width, a hex
 * colour, one gradient function, and keywords for the background options.
 * Every page field is
 * written into the style of every page's body, and the branding service
 * validates none of them against a grammar of its own.
 */
public final class BrandingPageStyleValidator {

  public static final int      MIN_WIDTH_PX    = 100;

  public static final int      MAX_WIDTH_PX    = 3000;

  public static final String   FULL_WIDTH      = "100%";

  private static final Pattern WIDTH_PATTERN   = Pattern.compile("^(\\d{3,4})px$");

  private static final Pattern COLOR_PATTERN   = Pattern.compile("^(#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})|transparent)$");

  private static final Pattern EFFECT_PATTERN  = Pattern.compile("^(none|(linear|radial|conic)-gradient\\([#0-9a-zA-Z(),.%\\s-]{1,300}\\))$");

  private static final Pattern KEYWORD_PATTERN = Pattern.compile("^[a-zA-Z -]{1,30}$");

  private BrandingPageStyleValidator() {
  }

  /**
   * @param  field one of the page fields of {@link ThemeOptions#PAGE_FIELDS}
   * @param  value the value to write
   * @return       true when the value is in the drawer's vocabulary for the
   *               field
   */
  public static boolean isAccepted(String field, String value) {
    if (value == null) {
      return false;
    }
    return switch (field) {
      case ThemeOptions.PAGE_WIDTH -> isAcceptedWidth(value);
      case ThemeOptions.PAGE_BACKGROUND_COLOR -> COLOR_PATTERN.matcher(value).matches();
      case ThemeOptions.PAGE_BACKGROUND_EFFECT -> EFFECT_PATTERN.matcher(value).matches() && isOneFunction(value);
      case ThemeOptions.PAGE_BACKGROUND_SIZE,
           ThemeOptions.PAGE_BACKGROUND_REPEAT,
           ThemeOptions.PAGE_BACKGROUND_POSITION,
           ThemeOptions.PAGE_BACKGROUND_ATTACHMENT -> KEYWORD_PATTERN.matcher(value).matches();
      default -> false;
    };
  }

  /**
   * The drawer writes a custom width from 100 to 3000 pixels, or the full
   * width
   */
  private static boolean isAcceptedWidth(String value) {
    if (FULL_WIDTH.equals(value)) {
      return true;
    }
    Matcher matcher = WIDTH_PATTERN.matcher(value);
    if (!matcher.matches()) {
      return false;
    }
    int pixels = Integer.parseInt(matcher.group(1));
    return pixels >= MIN_WIDTH_PX && pixels <= MAX_WIDTH_PX;
  }

  /**
   * The drawer writes one gradient function: its parentheses are balanced
   * and close for the last time on the last character, so that no second
   * layer follows the function
   */
  private static boolean isOneFunction(String value) {
    int depth = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth < 0 || (depth == 0 && i < value.length() - 1)) {
          return false;
        }
      }
    }
    return depth == 0;
  }
}
