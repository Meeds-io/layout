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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class CssColorParserTest {

  @Test
  void hexIsKept() {
    assertEquals("#AbC", CssColorParser.toHex(" #AbC "));
    assertEquals("#11223344", CssColorParser.toHex("#11223344"));
  }

  @Test
  void rgbAndRgba() {
    assertEquals("#FF8000", CssColorParser.toHex("rgb(255, 128, 0)"));
    assertEquals("#FF8000", CssColorParser.toHex("rgb(255 128 0)"));
    assertEquals("#FF8000", CssColorParser.toHex("RGB(100%, 50.2%, 0%)"));
    assertEquals("#FF800080", CssColorParser.toHex("rgba(255,128,0,0.5)"));
    assertEquals("#FF800080", CssColorParser.toHex("rgb(255 128 0 / 50%)"));
    assertEquals("#FF8000", CssColorParser.toHex("rgba(255,128,0,1)"));
    assertEquals("#000000", CssColorParser.toHex("rgb(-5, 0, 0)"));
  }

  @Test
  void hslAndHsla() {
    assertEquals("#FF0000", CssColorParser.toHex("hsl(0, 100%, 50%)"));
    assertEquals("#00FF00", CssColorParser.toHex("hsl(120deg 100% 50%)"));
    assertEquals("#0000FF80", CssColorParser.toHex("hsla(240, 100%, 50%, 0.5)"));
    assertEquals("#FFFFFF", CssColorParser.toHex("hsl(360, 0%, 100%)"));
  }

  @Test
  void namedColors() {
    assertEquals("#FF0000", CssColorParser.toHex("red"));
    assertEquals("#663399", CssColorParser.toHex("RebeccaPurple"));
    assertEquals("#00000000", CssColorParser.toHex("transparent"));
  }

  @Test
  void numberFormsAreAcceptedOrRejected() {
    assertEquals("#FF0000", CssColorParser.toHex("rgb(255, 0., .0)"));
    assertEquals("#000000", CssColorParser.toHex("rgb(-.5, 0, +0)"));
    assertEquals("#00FF00", CssColorParser.toHex("hsl(+120.DEG 100% 50%)"));
    assertNull(CssColorParser.toHex("rgb(1.2.3, 0, 0)"));
    assertNull(CssColorParser.toHex("rgb(%, 0, 0)"));
    assertNull(CssColorParser.toHex("rgb(a, 0, 0)"));
  }

  @Test
  void longTokenIsRefusedWithoutBacktracking() {
    String digits = "1".repeat(40_000);
    assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
      assertNull(CssColorParser.toHex("rgb(" + digits + "a, 0, 0)"));
      assertNull(CssColorParser.toHex("rgb(" + "1".repeat(CssColorParser.MAX_LENGTH) + "a, 0, 0)"));
    });
  }

  @Test
  void unparseableFormsGiveNull() {
    assertNull(CssColorParser.toHex(null));
    assertNull(CssColorParser.toHex(" "));
    assertNull(CssColorParser.toHex("var(--primary)"));
    assertNull(CssColorParser.toHex("currentcolor"));
    assertNull(CssColorParser.toHex("rgb(1, 2)"));
    assertNull(CssColorParser.toHex("rgb(a, b, c)"));
    assertNull(CssColorParser.toHex("hsl(0, 100, 50)"));
    assertNull(CssColorParser.toHex("#12345"));
  }

}
