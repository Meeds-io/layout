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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The option names each drawer of the Branding page writes: the theme
 * variables of the platform's branding configuration, grouped as the page
 * groups them, and the page fields of the branding. A variable the
 * configuration declares but no drawer writes is not an option: the Topbar
 * drawer offers no gradient, so the topbar has no background image option.
 * A background image option takes none or one gradient: the image file
 * itself is uploaded in the drawer, and the branding service drops any url
 * layer a caller sends.
 */
public final class ThemeOptions {

  public static final String      BACKGROUND_IMAGE_SUFFIX    = "BackgroundImage";

  public static final String      URL_LAYER                  = "url(";

  public static final String      PAGE_WIDTH                 = "pageWidth";

  public static final String      PAGE_BACKGROUND_COLOR      = "pageBackgroundColor";

  public static final String      PAGE_BACKGROUND_EFFECT     = "pageBackgroundEffect";

  public static final String      PAGE_BACKGROUND_SIZE       = "pageBackgroundSize";

  public static final String      PAGE_BACKGROUND_REPEAT     = "pageBackgroundRepeat";

  public static final String      PAGE_BACKGROUND_POSITION   = "pageBackgroundPosition";

  public static final String      PAGE_BACKGROUND_ATTACHMENT = "pageBackgroundAttachment";

  public static final Set<String> COLORS                     = Set.of("primaryColor", "secondaryColor", "tertiaryColor");

  public static final Set<String> FONT                       = Set.of("fontFamily");

  public static final Set<String> TOPBAR                     = union(suffixed("topBar",
                                                                              "BackgroundColor",
                                                                              "BackgroundPosition",
                                                                              "BackgroundSize",
                                                                              "BackgroundRepeat"),
                                                                     text("topBarText"),
                                                                     Set.of("topBarIconColor",
                                                                            "topBarSticky",
                                                                            "topBarBackgroundScrollColor"));

  public static final Set<String> SIDEBARS                   = union(background("sideBar"),
                                                                     text("sideBarText"),
                                                                     text("sideBarTextSubtitle"),
                                                                     Set.of("sideBarIconColor"));

  public static final Set<String> DRAWERS                    = union(background("drawer"),
                                                                     text("drawerText"),
                                                                     text("drawerTextSubtitle"),
                                                                     text("drawerTextTitle"),
                                                                     text("drawerTextHeader"),
                                                                     Set.of("drawerIconColor"));

  public static final Set<String> PAGE_FIELDS                = Set.of(PAGE_WIDTH,
                                                                      PAGE_BACKGROUND_COLOR,
                                                                      PAGE_BACKGROUND_EFFECT,
                                                                      PAGE_BACKGROUND_SIZE,
                                                                      PAGE_BACKGROUND_REPEAT,
                                                                      PAGE_BACKGROUND_POSITION,
                                                                      PAGE_BACKGROUND_ATTACHMENT);

  public static final Set<String> PAGE_AND_APPS              = union(sides("pageMargin"),
                                                                     sides("appMargin"),
                                                                     Set.of("appBorderColor", "appBorderSize", "appBoxShadow", "borderRadius"),
                                                                     corners("appBorderRadius"),
                                                                     background("app"),
                                                                     text("appText"),
                                                                     text("appTextTitle"),
                                                                     text("appTextHeader"),
                                                                     text("appTextSubtitle"),
                                                                     Set.of("appIconColor"),
                                                                     textBackground("appTextTitleBackground"),
                                                                     textBackground("appTextHeaderBackground"));

  private ThemeOptions() {
  }

  private static Set<String> background(String prefix) {
    return suffixed(prefix, "BackgroundColor", "BackgroundImage", "BackgroundPosition", "BackgroundSize", "BackgroundRepeat");
  }

  private static Set<String> text(String prefix) {
    return suffixed(prefix, "Color", "FontSize", "FontStyle", "FontWeight");
  }

  private static Set<String> sides(String prefix) {
    return suffixed(prefix, "Top", "Right", "Bottom", "Left");
  }

  private static Set<String> corners(String prefix) {
    return suffixed(prefix, "TopLeft", "TopRight", "BottomLeft", "BottomRight");
  }

  private static Set<String> textBackground(String prefix) {
    return suffixed(prefix,
                    "Color",
                    "Image",
                    "Position",
                    "Size",
                    "Repeat",
                    "PaddingTop",
                    "PaddingRight",
                    "PaddingBottom",
                    "PaddingLeft",
                    "Radius");
  }

  private static Set<String> suffixed(String prefix, String... suffixes) {
    return Arrays.stream(suffixes).map(prefix::concat).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @SafeVarargs
  private static Set<String> union(Set<String>... sets) {
    return Stream.of(sets).flatMap(Set::stream).collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
