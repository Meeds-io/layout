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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.util.ThemeColorCalculator;

public class LayoutDarkColorsTest {

  private static final String AUTO = LayoutDarkColors.AUTO;

  @Test
  void backgroundTakesTheAutomaticDarkRule() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#3366CC"), "#3366CC", false, BackgroundKind.APP);
    assertEquals(ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#3366CC"), dark.get("backgroundColor"));
    assertFalse(dark.get("backgroundColor").equalsIgnoreCase("#3366CC"));
  }

  @Test
  void lightDefaultBackgroundTakesTheDarkDefaultOfItsKind() {
    assertEquals("#1E2328",
                 LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF"), "#FFFFFF", false, BackgroundKind.APP)
                                 .get("backgroundColor"));
    assertEquals("#14181B",
                 LayoutDarkColors.compute(Map.of("backgroundColor", "#F0F0F0"), "#F0F0F0", false, BackgroundKind.PAGE)
                                 .get("backgroundColor"));
  }

  @Test
  void shippedNearDefaultBackgroundsTakeTheDarkDefaults() {
    assertEquals("#14181B",
                 LayoutDarkColors.compute(Map.of("backgroundColor", "#F2F2F2FF"), "#F2F2F2FF", false, BackgroundKind.PAGE)
                                 .get("backgroundColor"));
    // an application of the shipped My Workspace page: its own translucent white, dark Neutral text kept on the dark default
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFFF0", "textColor", "#E6E9EB"),
                                                        "#FFFFFFF0",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals("#1E2328F0", dark.get("backgroundColor"));
    assertFalse(dark.containsKey("textColor"), "readable on the dark default: " + dark);
  }

  @Test
  void backgroundInRgbFormIsComputedLikeItsHex() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "rgb(51, 102, 204)"),
                                                        "rgb(51, 102, 204)",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals(ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#3366CC"), dark.get("backgroundColor"));
  }

  @Test
  void scrollPairGetsADarkValuePerHalf() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF@#F0F0F0"),
                                                        "#FFFFFF@#F0F0F0",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals("#1E2328@" + ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#F0F0F0"), dark.get("backgroundColor"));
  }

  @Test
  void scrollPairKeepsAnUnparseableHalf() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF@var(--x)"),
                                                        "#FFFFFF@var(--x)",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals("#1E2328@var(--x)", dark.get("backgroundColor"));
  }

  @Test
  void unparseableColourHasNoDarkValue() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "var(--x)", "textColor", "#20282C"),
                                                        "var(--x)",
                                                        false,
                                                        BackgroundKind.APP);
    assertTrue(dark.isEmpty(), "the stored colours are kept: " + dark);
    assertFalse(LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF", "textColor", "var(--y)"),
                                         "#FFFFFF",
                                         false,
                                         BackgroundKind.APP)
                                .containsKey("textColor"));
  }

  @Test
  void textKeptWhenReadableOnTheDarkBackground() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF", "textColor", "#FFFFFF"),
                                                        "#FFFFFF",
                                                        false,
                                                        BackgroundKind.APP);
    assertFalse(dark.containsKey("textColor"));
  }

  @Test
  void textBecomesAutoWhenUnreadableOnTheDarkBackground() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "#FFFFFF",
                                                               "textColor", "#20282C",
                                                               "iconColor", "#20282C",
                                                               "borderColor", "#20282C"),
                                                        "#FFFFFF",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals(AUTO, dark.get("textColor"));
    assertEquals(AUTO, dark.get("iconColor"));
    assertEquals(AUTO, dark.get("borderColor"));
  }

  @Test
  void textOnABlockWithoutBackgroundIsAuto() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("textColor", "#FFFFFF", "textTitleColor", "#20282C"),
                                                        null,
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals(AUTO, dark.get("textColor"));
    assertEquals(AUTO, dark.get("textTitleColor"));
  }

  @Test
  void fullyTransparentBackgroundIsNoBackground() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("backgroundColor", "rgba(0,0,0,0)", "textColor", "#FFFFFF"),
                                                        "rgba(0,0,0,0)",
                                                        false,
                                                        BackgroundKind.APP);
    assertEquals(AUTO, dark.get("textColor"));
    assertFalse(dark.containsKey("backgroundColor"));
  }

  @Test
  void imageWithoutColourKeepsTextAndIconButNotBorder() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("textColor", "#20282C",
                                                               "iconColor", "#20282C",
                                                               "borderColor", "#20282C"),
                                                        null,
                                                        true,
                                                        BackgroundKind.APP);
    assertFalse(dark.containsKey("textColor"));
    assertFalse(dark.containsKey("iconColor"));
    assertEquals(AUTO, dark.get("borderColor"));
  }

  @Test
  void imageKeepsTextAndIconEvenWithAnInheritedBackground() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("textColor", "#20282C", "borderColor", "#20282C"),
                                                        "#FFFFFF",
                                                        true,
                                                        BackgroundKind.APP);
    assertFalse(dark.containsKey("textColor"));
    assertEquals(AUTO, dark.get("borderColor"));
  }

  @Test
  void textBackgroundColoursAreBackgrounds() {
    Map<String, String> dark = LayoutDarkColors.compute(Map.of("textTitleBackgroundColor", "#FFFFFF",
                                                               "appBackgroundColor", "#FFFFFF"),
                                                        null,
                                                        false,
                                                        BackgroundKind.PAGE);
    assertEquals("#1E2328", dark.get("textTitleBackgroundColor"));
    assertEquals("#1E2328", dark.get("appBackgroundColor"));
  }

}
