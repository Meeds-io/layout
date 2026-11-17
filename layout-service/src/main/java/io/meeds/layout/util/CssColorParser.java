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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.theme.util.ThemeColorCalculator;

/**
 * Converts the CSS colour forms a page editor stores (hex, rgb() / rgba(),
 * hsl() / hsla(), named colours) to the hex form that
 * {@link ThemeColorCalculator}, which only reads hex, calculates with.
 */
public final class CssColorParser {

  /** Longest colour, or half of a scroll pair, that is parsed: bounds the work done on client input */
  public static final int             MAX_LENGTH       = 64;

  private static final Pattern        FUNCTION_PATTERN = Pattern.compile("^(rgba?|hsla?)\\(([^()]*)\\)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern        SEPARATORS       = Pattern.compile("\\s*[,/]\\s*|\\s+");

  private static final Pattern        NUMBER_PATTERN   = Pattern.compile("[+-]?(?:\\d++(?:\\.\\d*+)?|\\.\\d++)(?:%|deg)?", Pattern.CASE_INSENSITIVE);

  private static final Map<String, String> NAMED_COLORS = new HashMap<>();

  static {
    String table = "aliceblue f0f8ff,antiquewhite faebd7,aqua 00ffff,aquamarine 7fffd4,azure f0ffff,beige f5f5dc,bisque ffe4c4,"
        + "black 000000,blanchedalmond ffebcd,blue 0000ff,blueviolet 8a2be2,brown a52a2a,burlywood deb887,cadetblue 5f9ea0,"
        + "chartreuse 7fff00,chocolate d2691e,coral ff7f50,cornflowerblue 6495ed,cornsilk fff8dc,crimson dc143c,cyan 00ffff,"
        + "darkblue 00008b,darkcyan 008b8b,darkgoldenrod b8860b,darkgray a9a9a9,darkgreen 006400,darkgrey a9a9a9,"
        + "darkkhaki bdb76b,darkmagenta 8b008b,darkolivegreen 556b2f,darkorange ff8c00,darkorchid 9932cc,darkred 8b0000,"
        + "darksalmon e9967a,darkseagreen 8fbc8f,darkslateblue 483d8b,darkslategray 2f4f4f,darkslategrey 2f4f4f,"
        + "darkturquoise 00ced1,darkviolet 9400d3,deeppink ff1493,deepskyblue 00bfff,dimgray 696969,dimgrey 696969,"
        + "dodgerblue 1e90ff,firebrick b22222,floralwhite fffaf0,forestgreen 228b22,fuchsia ff00ff,gainsboro dcdcdc,"
        + "ghostwhite f8f8ff,gold ffd700,goldenrod daa520,gray 808080,green 008000,greenyellow adff2f,grey 808080,"
        + "honeydew f0fff0,hotpink ff69b4,indianred cd5c5c,indigo 4b0082,ivory fffff0,khaki f0e68c,lavender e6e6fa,"
        + "lavenderblush fff0f5,lawngreen 7cfc00,lemonchiffon fffacd,lightblue add8e6,lightcoral f08080,lightcyan e0ffff,"
        + "lightgoldenrodyellow fafad2,lightgray d3d3d3,lightgreen 90ee90,lightgrey d3d3d3,lightpink ffb6c1,"
        + "lightsalmon ffa07a,lightseagreen 20b2aa,lightskyblue 87cefa,lightslategray 778899,lightslategrey 778899,"
        + "lightsteelblue b0c4de,lightyellow ffffe0,lime 00ff00,limegreen 32cd32,linen faf0e6,magenta ff00ff,maroon 800000,"
        + "mediumaquamarine 66cdaa,mediumblue 0000cd,mediumorchid ba55d3,mediumpurple 9370db,mediumseagreen 3cb371,"
        + "mediumslateblue 7b68ee,mediumspringgreen 00fa9a,mediumturquoise 48d1cc,mediumvioletred c71585,"
        + "midnightblue 191970,mintcream f5fffa,mistyrose ffe4e1,moccasin ffe4b5,navajowhite ffdead,navy 000080,"
        + "oldlace fdf5e6,olive 808000,olivedrab 6b8e23,orange ffa500,orangered ff4500,orchid da70d6,palegoldenrod eee8aa,"
        + "palegreen 98fb98,paleturquoise afeeee,palevioletred db7093,papayawhip ffefd5,peachpuff ffdab9,peru cd853f,"
        + "pink ffc0cb,plum dda0dd,powderblue b0e0e6,purple 800080,rebeccapurple 663399,red ff0000,rosybrown bc8f8f,"
        + "royalblue 4169e1,saddlebrown 8b4513,salmon fa8072,sandybrown f4a460,seagreen 2e8b57,seashell fff5ee,"
        + "sienna a0522d,silver c0c0c0,skyblue 87ceeb,slateblue 6a5acd,slategray 708090,slategrey 708090,snow fffafa,"
        + "springgreen 00ff7f,steelblue 4682b4,tan d2b48c,teal 008080,thistle d8bfd8,tomato ff6347,turquoise 40e0d0,"
        + "violet ee82ee,wheat f5deb3,white ffffff,whitesmoke f5f5f5,yellow ffff00,yellowgreen 9acd32";
    for (String entry : table.split(",")) {
      String[] parts = entry.split(" ");
      NAMED_COLORS.put(parts[0], "#" + parts[1].toUpperCase(Locale.ROOT));
    }
    NAMED_COLORS.put("transparent", "#00000000");
  }

  private CssColorParser() {
    // Static utility
  }

  /**
   * @param color a stored CSS colour
   * @return the colour as #RRGGBB or #RRGGBBAA ({@link ThemeColorCalculator}
   *         reads that form), null when the form is not one of hex, rgb(),
   *         rgba(), hsl(), hsla() or a named colour
   */
  public static String toHex(String color) {
    if (StringUtils.isBlank(color) || color.length() > MAX_LENGTH) {
      return null;
    }
    String value = color.trim();
    if (ThemeColorCalculator.isHexColor(value)) {
      return value;
    }
    String named = NAMED_COLORS.get(value.toLowerCase(Locale.ROOT));
    if (named != null) {
      return named;
    }
    Matcher matcher = FUNCTION_PATTERN.matcher(value);
    if (!matcher.matches()) {
      return null;
    }
    String[] tokens = SEPARATORS.split(matcher.group(2).trim());
    if (tokens.length != 3 && tokens.length != 4) {
      return null;
    }
    for (String token : tokens) {
      if (!NUMBER_PATTERN.matcher(token).matches()) {
        return null;
      }
    }
    Double alpha = tokens.length == 4 ? number(tokens[3]) : Double.valueOf(1);
    if (alpha == null) {
      return null;
    }
    double[] rgb = matcher.group(1).toLowerCase(Locale.ROOT).startsWith("rgb") ? rgb(tokens) : hsl(tokens);
    if (rgb == null) {
      return null;
    }
    String hex = String.format(Locale.ROOT, "#%02X%02X%02X", channel(rgb[0]), channel(rgb[1]), channel(rgb[2]));
    return alpha >= 1 ? hex : hex + String.format(Locale.ROOT, "%02X", channel(Math.max(alpha, 0) * 255));
  }

  private static double[] rgb(String[] tokens) {
    double[] rgb = new double[3];
    for (int i = 0; i < 3; i++) {
      Double value = number(tokens[i]);
      if (value == null) {
        return null;
      }
      rgb[i] = tokens[i].endsWith("%") ? value * 255 : value;
    }
    return rgb;
  }

  private static double[] hsl(String[] tokens) {
    Double hue = number(tokens[0]);
    Double saturation = number(tokens[1]);
    Double lightness = number(tokens[2]);
    if (hue == null || saturation == null || lightness == null || !tokens[1].endsWith("%") || !tokens[2].endsWith("%")) {
      return null;
    }
    double h = (((tokens[0].endsWith("%") ? hue * 360 : hue) % 360) + 360) % 360;
    double s = Math.min(Math.max(saturation, 0), 1);
    double l = Math.min(Math.max(lightness, 0), 1);
    double c = (1 - Math.abs(2 * l - 1)) * s;
    double x = c * (1 - Math.abs((h / 60) % 2 - 1));
    double m = l - c / 2;
    double[] rgb;
    if (h < 60) {
      rgb = new double[] { c, x, 0 };
    } else if (h < 120) {
      rgb = new double[] { x, c, 0 };
    } else if (h < 180) {
      rgb = new double[] { 0, c, x };
    } else if (h < 240) {
      rgb = new double[] { 0, x, c };
    } else if (h < 300) {
      rgb = new double[] { x, 0, c };
    } else {
      rgb = new double[] { c, 0, x };
    }
    return new double[] { (rgb[0] + m) * 255, (rgb[1] + m) * 255, (rgb[2] + m) * 255 };
  }

  /**
   * @return the numeric value, a percentage being returned as its fraction of
   *         one (rgb percentages are scaled by the caller), null when
   *         unreadable. A plain number is returned as is.
   */
  private static Double number(String token) {
    try {
      if (token.endsWith("%")) {
        return Double.parseDouble(token.substring(0, token.length() - 1)) / 100;
      }
      if (token.toLowerCase(Locale.ROOT).endsWith("deg")) {
        return Double.parseDouble(token.substring(0, token.length() - 3));
      }
      return Double.parseDouble(token);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static long channel(double value) {
    return Math.round(Math.min(Math.max(value, 0), 255));
  }

}
