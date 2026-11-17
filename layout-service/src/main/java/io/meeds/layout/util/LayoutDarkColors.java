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
package io.meeds.layout.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.util.ThemeColorCalculator;

/**
 * The dark version of the colours a page, a section or an application stores
 * (set in the page editor): they are treated like theme colours, the dark
 * value being calculated, never stored. Calculation is delegated to
 * {@link ThemeColorCalculator}, the single implementation of the dark rules.
 * <p>
 * The result maps a field to its dark value: a field absent from it keeps its
 * stored value in dark (a colour the calculator cannot parse); a field mapped
 * to the empty string is Auto, i.e. no colour is written and the theme's dark
 * colour applies.
 */
public final class LayoutDarkColors {

  public static final String       BACKGROUND_COLOR        = "backgroundColor";

  public static final String       APP_BACKGROUND_COLOR    = "appBackgroundColor";

  public static final String       BORDER_COLOR            = "borderColor";

  /** Backgrounds: take the Automatic dark background rule. */
  public static final List<String> BACKGROUND_FIELDS       = List.of(BACKGROUND_COLOR,
                                                                     APP_BACKGROUND_COLOR,
                                                                     "textTitleBackgroundColor",
                                                                     "textHeaderBackgroundColor",
                                                                     "textBackgroundColor",
                                                                     "textSubtitleBackgroundColor");

  /** Text, icon and border colours: kept when readable on the dark background, else Auto. */
  public static final List<String> FOREGROUND_FIELDS       = List.of("textTitleColor",
                                                                     "textColor",
                                                                     "textHeaderColor",
                                                                     "textSubtitleColor",
                                                                     "iconColor",
                                                                     BORDER_COLOR);

  public static final String       AUTO                    = "";

  private static final String      SCROLL_COLOR_SEPARATOR  = "@";

  private LayoutDarkColors() {
    // Static utility
  }

  /**
   * @param stored the stored colours by field, the fields outside
   *          {@link #BACKGROUND_FIELDS} and {@link #FOREGROUND_FIELDS} being
   *          ignored
   * @param background the background the foreground colours sit on: the
   *          block's own one, else, for an application, the one it inherits;
   *          null or blank for none. A "colorA@colorB" scroll pair counts by
   *          its first half
   * @param image whether the block has no colour of its own and lies over an
   *          image or a gradient, its own or an inherited one: the image does not change with the mode, so the
   *          text and icon colours are kept
   * @param backgroundKind the kind of theme background whose light default the
   *          block's own background is compared with
   * @return the dark values by field, see the class description
   */
  public static Map<String, String> compute(Map<String, String> stored,
                                            String background,
                                            boolean image,
                                            BackgroundKind backgroundKind) {
    Map<String, String> dark = new LinkedHashMap<>();
    if (stored == null) {
      return dark;
    }
    BACKGROUND_FIELDS.forEach(field -> {
      BackgroundKind kind = BACKGROUND_COLOR.equals(field) ? backgroundKind : BackgroundKind.APP;
      String value = darkBackground(stored.get(field), kind);
      if (value != null) {
        dark.put(field, value);
      }
    });
    boolean ownBackground = StringUtils.equals(firstHalf(background), firstHalf(stored.get(BACKGROUND_COLOR)));
    String darkBackground = darkBackground(firstHalf(background), ownBackground ? backgroundKind : BackgroundKind.APP);
    boolean hasBackground = StringUtils.isNotBlank(background) && !isTransparent(background);
    FOREGROUND_FIELDS.forEach(field -> {
      String value = stored.get(field);
      if (StringUtils.isBlank(value)) {
        return;
      }
      if (image && !BORDER_COLOR.equals(field)) {
        // an image does not change with the mode: the text and icon colours stay, whatever background is behind it
        return;
      }
      if (!hasBackground) {
        dark.put(field, AUTO);
      } else if (darkBackground != null) {
        String hex = CssColorParser.toHex(value);
        if (hex != null && ThemeColorCalculator.contrast(hex, darkBackground) < ThemeColorCalculator.READABLE_RATIO) {
          dark.put(field, AUTO);
        }
      }
    });
    return dark;
  }

  private static String darkBackground(String value, BackgroundKind kind) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    if (value.contains(SCROLL_COLOR_SEPARATOR)) {
      String[] halves = StringUtils.splitPreserveAllTokens(value, SCROLL_COLOR_SEPARATOR);
      if (halves.length != 2) {
        return null;
      }
      String first = darkHalf(halves[0], kind);
      String second = darkHalf(halves[1], kind);
      return first == null && second == null ? null : (first == null ? halves[0] : first) + SCROLL_COLOR_SEPARATOR
          + (second == null ? halves[1] : second);
    }
    return darkHalf(value, kind);
  }

  private static String darkHalf(String value, BackgroundKind kind) {
    String hex = CssColorParser.toHex(value);
    return hex == null || isTransparent(hex) ? null : ThemeColorCalculator.automaticDarkBackground(kind, hex);
  }

  private static String firstHalf(String background) {
    return background == null ? null : StringUtils.substringBefore(background, SCROLL_COLOR_SEPARATOR);
  }

  private static boolean isTransparent(String color) {
    String hex = CssColorParser.toHex(firstHalf(color));
    return hex != null && (hex.length() == 9 && hex.endsWith("00") || hex.length() == 5 && hex.endsWith("0"));
  }

}
