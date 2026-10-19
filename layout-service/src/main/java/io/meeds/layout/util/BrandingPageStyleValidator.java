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

import java.util.regex.Pattern;

/**
 * The vocabulary of the page fields of the branding, as the Page drawer
 * writes them: a width in pixels or a percentage, a colour, a gradient
 * effect, and keywords for the background options. Every page field is
 * written into the style of every page's body, and the branding service
 * validates none of them against a grammar of its own.
 */
public final class BrandingPageStyleValidator {

  private static final Pattern WIDTH_PATTERN    = Pattern.compile("^(\\d{1,4}px|\\d{1,3}%|initial)$");

  private static final Pattern COLOR_PATTERN    = Pattern.compile("^(#[0-9a-fA-F]{3,8}|transparent|initial)$");

  private static final Pattern EFFECT_PATTERN   = Pattern.compile("^(initial|none|(linear|radial|conic)-gradient\\([#0-9a-zA-Z(),.%\\s-]{1,300}\\))$");

  private static final Pattern KEYWORD_PATTERN  = Pattern.compile("^[a-zA-Z -]{1,30}$");

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
      case ThemeOptions.PAGE_WIDTH -> WIDTH_PATTERN.matcher(value).matches();
      case ThemeOptions.PAGE_BACKGROUND_COLOR -> COLOR_PATTERN.matcher(value).matches();
      case ThemeOptions.PAGE_BACKGROUND_EFFECT -> EFFECT_PATTERN.matcher(value).matches() && hasBalancedParentheses(value);
      case ThemeOptions.PAGE_BACKGROUND_SIZE,
           ThemeOptions.PAGE_BACKGROUND_REPEAT,
           ThemeOptions.PAGE_BACKGROUND_POSITION,
           ThemeOptions.PAGE_BACKGROUND_ATTACHMENT -> KEYWORD_PATTERN.matcher(value).matches();
      default -> false;
    };
  }

  private static boolean hasBalancedParentheses(String value) {
    int depth = 0;
    for (char c : value.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth < 0) {
          return false;
        }
      }
    }
    return depth == 0;
  }
}
