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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.layout.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.TransientApplicationState;

/**
 * eXIP 7.3.0.30: legacy application margins are read once, server-side, from
 * the Vuetify spacing tokens still stored in the cssClass (value = N x 4 + 20
 * on the platform scale), whatever scale the stored attribute used, and the
 * tokens are stripped from the class exposed to the Vue consumers.
 */
public class LayoutModelTest {

  @Test
  public void shouldReadLegacyMarginsFromXmlImportedApplicationTokens() {
    // page XML: 20-neutral attribute next to the tokens ModelStyle generated at import (mt-n1 = -4px)
    LayoutModel model = new LayoutModel(application(16, 8, 4, 0, "mt-n1 mb-n3 me-n4 ms-n5 TEST-class"));
    assertEquals(16, model.getMarginTop());
    assertEquals(8, model.getMarginBottom());
    assertEquals(4, model.getMarginRight());
    assertEquals(0, model.getMarginLeft());
    assertEquals("TEST-class", model.getCssClass());
  }

  @Test
  public void shouldReadLegacyMarginsFromEditorSavedApplicationTokens() {
    // editor before 7.3.0.30: 0-neutral attribute next to the tokens the editor wrote for the same -4px
    LayoutModel model = new LayoutModel(application(-4, -12, -16, -20, "mt-n1 mb-n3 me-n4 ms-n5"));
    assertEquals(16, model.getMarginTop());
    assertEquals(8, model.getMarginBottom());
    assertEquals(4, model.getMarginRight());
    assertEquals(0, model.getMarginLeft());
    assertNull(model.getCssClass());
  }

  @Test
  public void shouldKeepPlatformScaleMarginsWithoutTokens() {
    LayoutModel model = new LayoutModel(application(32, 20, null, null, "brtr-2 TEST-class"));
    assertEquals(32, model.getMarginTop());
    assertEquals(20, model.getMarginBottom());
    assertNull(model.getMarginRight());
    // Application.getCssClass() prefixes the style tokens with a space (pre-existing): only the content matters here
    assertEquals("brtr-2 TEST-class", model.getCssClass().trim());
  }

  @Test
  public void shouldReadPositiveAndBreakpointTokens() {
    LayoutModel model = new LayoutModel(application(null, null, null, null, "mt-3 mb-0 mr-md-2 ml-1"));
    assertEquals(32, model.getMarginTop());
    assertEquals(20, model.getMarginBottom());
    assertEquals(28, model.getMarginRight());
    assertEquals(24, model.getMarginLeft());
    assertNull(model.getCssClass());
  }

  private Application application(Integer top, Integer bottom, Integer right, Integer left, String cssClass) {
    Application application = new Application("storageId");
    application.setState(new TransientApplicationState("layout/Test"));
    ModelStyle style = new ModelStyle();
    style.setMarginTop(top);
    style.setMarginBottom(bottom);
    style.setMarginRight(right);
    style.setMarginLeft(left);
    application.setCssStyle(style);
    application.setCssClass(cssClass);
    return application;
  }

}
