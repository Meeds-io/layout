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
package io.meeds.layout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.BackgroundBlock;
import io.meeds.layout.mcp.model.BackgroundImageBlock;
import io.meeds.layout.mcp.model.BorderBlock;
import io.meeds.layout.mcp.model.CornersBlock;
import io.meeds.layout.mcp.model.HeightBlock;
import io.meeds.layout.mcp.model.MarginsBlock;
import io.meeds.layout.mcp.model.SidesBlock;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.mcp.model.TextBackgroundBlock;
import io.meeds.layout.mcp.model.TextBlock;
import io.meeds.layout.mcp.model.TextRoleBlock;
import io.meeds.layout.model.LayoutModel;

/**
 * The writer stores exactly what the editor's styling inputs store, refuses
 * a value outside their vocabulary naming its path, and never writes an
 * image.
 */
public class LayoutStylingServiceTest {

  private static final String   REFUSED = "value is not accepted for styling.";

  private LayoutStylingService  service = new LayoutStylingService();

  @Test
  public void marginsAreWrittenOnTheDestinationScale() {
    LayoutModel container = new LayoutModel();
    service.applyStyling(container, styling(new MarginsBlock(true, 10, null, -60, null, null), null, null, null, null, null),
                         StylingDestination.BANNER, false, false);
    assertEquals(10, container.getMarginTop());
    assertEquals(-60, container.getMarginRight());
    assertEquals(10, container.getMarginBottom());
    assertEquals(10, container.getMarginLeft());

    LayoutModel application = new LayoutModel();
    service.applyStyling(application, styling(new MarginsBlock(true, null, 80, null, null, null), null, null, null, null, null),
                         StylingDestination.SITE_CONTAINER_APPLICATION, false, false);
    assertEquals(80, application.getMarginTop());
    assertEquals(20, application.getMarginRight());

    assertEquals(REFUSED + "margins.top",
                 refused(() -> service.applyStyling(container,
                                                    styling(new MarginsBlock(true, null, 61, null, null, null), null, null, null, null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "margins.left",
                 refused(() -> service.applyStyling(application,
                                                    styling(new MarginsBlock(true, null, null, null, null, -41), null, null, null, null, null),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));

    service.applyStyling(container, styling(new MarginsBlock(false, 10, 10, 10, 10, 10), null, null, null, null, null), StylingDestination.BANNER, false, false);
    assertNull(container.getMarginTop());
    assertNull(container.getMarginLeft());
    service.applyStyling(application, null, StylingDestination.APPLICATION, false, false);
    assertNull(application.getMarginTop());
  }

  @Test
  public void borderAndCornersAreOfferedToApplicationsOnly() {
    LayoutModel application = new LayoutModel();
    service.applyStyling(application,
                         styling(null, new BorderBlock(true, "#123456", null, true), new CornersBlock(true, 8, null, 20, null, null), null, null, null),
                         StylingDestination.SITE_CONTAINER_APPLICATION,
                         false,
                         false);
    assertEquals("#123456", application.getBorderColor());
    assertEquals("1", application.getBorderSize());
    assertEquals("true", application.getBoxShadow());
    assertEquals(8, application.getRadiusTopLeft());
    assertEquals(20, application.getRadiusTopRight());
    assertEquals(8, application.getRadiusBottomRight());

    service.applyStyling(application, null, StylingDestination.SITE_CONTAINER_APPLICATION, false, false);
    assertNull(application.getBorderColor());
    assertEquals("0", application.getBorderSize());
    assertNull(application.getBoxShadow());
    assertNull(application.getRadiusTopLeft());

    LayoutModel banner = new LayoutModel();
    assertEquals(REFUSED + "border",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, new BorderBlock(true, "#123456", 1, null), null, null, null, null),
                                                    StylingDestination.BANNER,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "corners",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, null, new CornersBlock(true, 4, null, null, null, null), null, null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    service.applyStyling(banner, null, StylingDestination.BANNER, false, false);
    assertNull(banner.getBorderSize());
    assertEquals(REFUSED + "border.size",
                 refused(() -> service.applyStyling(application,
                                                    styling(null, new BorderBlock(true, "#123456", 9, null), null, null, null, null),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "border.color",
                 refused(() -> service.applyStyling(application,
                                                    styling(null, new BorderBlock(true, "red", 1, null), null, null, null, null),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "corners.bottom_left",
                 refused(() -> service.applyStyling(application,
                                                    styling(null, null, new CornersBlock(true, null, null, null, null, 21), null, null, null),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));
  }

  @Test
  public void solidBackgroundCarriesTheScrollColourOfAStickyContainerOnly() {
    LayoutModel banner = new LayoutModel();
    service.applyStyling(banner, styling(null, null, null, background(BackgroundMode.SOLID, "#111111", null, null, null, null, "#222222"), null, null),
                         StylingDestination.BANNER, true, true);
    assertEquals("#111111@#222222", banner.getBackgroundColor());
    assertNull(banner.getBackgroundEffect());

    service.applyStyling(banner, styling(null, null, null, background(null, null, null, null, null, null, null), null, null),
                         StylingDestination.BANNER, true, true);
    assertEquals("#FFFFFF@#FFFFFF", banner.getBackgroundColor());

    service.applyStyling(banner, styling(null, null, null, background(null, "#111111", null, null, null, null, null), null, null),
                         StylingDestination.BANNER, false, true);
    assertEquals("#111111", banner.getBackgroundColor());

    assertEquals(REFUSED + "background.scroll_color",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, null, null, background(null, "#111111", null, null, null, null, "#222222"), null, null),
                                                    StylingDestination.BANNER,
                                                    false,
                                                    true)));
    assertEquals(REFUSED + "background.color",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, null, null, background(null, "white", null, null, null, null, null), null, null),
                                                    StylingDestination.BANNER,
                                                    false,
                                                    true)));
    service.applyStyling(banner, styling(null, null, null, new BackgroundBlock(false, null, "#111111", null, null, null, null, null, null, null), null, null),
                         StylingDestination.BANNER, true, true);
    assertNull(banner.getBackgroundColor());
  }

  @Test
  public void gradientsAreWrittenAsTheEditorWritesThem() {
    LayoutModel cell = new LayoutModel();
    service.applyStyling(cell, styling(null, null, null, background(BackgroundMode.LINEAR, null, "#AAAAAA", "#BBBBBB", 30, null, null), null, null),
                         StylingDestination.SIDEBAR, false, false);
    assertEquals(LayoutStylingService.GRADIENT_BASE_COLOR, cell.getBackgroundColor());
    assertEquals("linear-gradient(#AAAAAA 0%, #AAAAAA 30%, #BBBBBB 100%)", cell.getBackgroundEffect());

    service.applyStyling(cell, styling(null, null, null, background(BackgroundMode.LINEAR, null, "#AAAAAA", "#BBBBBB", null, "to_right", null), null, null),
                         StylingDestination.SIDEBAR, false, false);
    assertEquals("linear-gradient(to right, #AAAAAA 0%, #AAAAAA 50%, #BBBBBB 100%)", cell.getBackgroundEffect());

    service.applyStyling(cell, styling(null, null, null, background(BackgroundMode.RADIAL, null, "#AAAAAA", "#BBBBBB", 70, null, null), null, null),
                         StylingDestination.SIDEBAR, false, false);
    assertEquals("radial-gradient(#AAAAAA 0%, #AAAAAA 70%, #BBBBBB 100%)", cell.getBackgroundEffect());

    service.applyStyling(cell, styling(null, null, null, background(BackgroundMode.ANGULAR, null, "#AAAAAA", "#BBBBBB", 50, "top_left", null), null, null),
                         StylingDestination.SIDEBAR, false, false);
    assertEquals("conic-gradient(from 90deg at top left, #AAAAAA 0deg, #AAAAAA 45.00deg, #BBBBBB 90deg)", cell.getBackgroundEffect());

    service.applyStyling(cell, styling(null, null, null, background(BackgroundMode.ANGULAR, null, "#AAAAAA", "#BBBBBB", 30, "top_right", null), null, null),
                         StylingDestination.SIDEBAR, false, false);
    assertEquals("conic-gradient(from 180deg at top right, #BBBBBB 0deg, #AAAAAA 63.00deg, #AAAAAA 90deg)", cell.getBackgroundEffect());

    assertEquals(REFUSED + "background.mode",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.LINEAR, null, "#AAAAAA", "#BBBBBB", null, null, null), null, null),
                                                    StylingDestination.BANNER,
                                                    false,
                                                    true)));
    assertEquals(REFUSED + "background.ratio",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.RADIAL, null, "#AAAAAA", "#BBBBBB", 101, null, null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.direction",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.ANGULAR, null, "#AAAAAA", "#BBBBBB", null, "left", null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.direction",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.RADIAL, null, "#AAAAAA", "#BBBBBB", null, "to_right", null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.to",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.LINEAR, null, "#AAAAAA", null, null, null, null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.color",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.LINEAR, "#111111", "#AAAAAA", "#BBBBBB", null, null, null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.mode",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, background(BackgroundMode.SOLID, "#111111", "#AAAAAA", null, null, null, null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
  }

  @Test
  public void imagesAreNotWrittenHere() {
    LayoutModel cell = new LayoutModel();
    BackgroundImageBlock image = new BackgroundImageBlock("u1", null, null, null, null, null, null, null);
    assertEquals(REFUSED + "background.image",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, new BackgroundBlock(true, null, "#111111", null, null, null, null, null, null, image), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    TextBackgroundBlock textBackground = new TextBackgroundBlock(true, null, null, null, null, null, null, null, image, null, null);
    assertEquals(REFUSED + "text.title.background.image",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, null, text(new TextRoleBlock(null, null, null, null, textBackground), null, null), null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "background.effect",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, new BackgroundBlock(true, null, null, null, null, null, null, null, "url(x)", null), null, null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
  }

  @Test
  public void textRolesStoreTheirSizeInPixelsAndTheirStyleAsAKeyword() {
    LayoutModel cell = new LayoutModel();
    TextBackgroundBlock titleBackground = new TextBackgroundBlock(true,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  new SidesBlock(4, null, 4, 8),
                                                                  new CornersBlock(true, 6, null, 12, null, null));
    TextBlock text = new TextBlock(true,
                                   new TextRoleBlock("#111111", 20, true, null, titleBackground),
                                   new TextRoleBlock(null, null, false, true, null),
                                   new TextRoleBlock("#333333", 16, null, null, null),
                                   null,
                                   "#444444");
    service.applyStyling(cell, styling(null, null, null, null, text, null), StylingDestination.SIDEBAR, false, false);
    assertEquals("#111111", cell.getTextTitleColor());
    assertEquals("20px", cell.getTextTitleFontSize());
    assertEquals("bold", cell.getTextTitleFontWeight());
    assertEquals("normal", cell.getTextTitleFontStyle());
    assertEquals("#FFFFFFFF", cell.getTextTitleBackgroundColor());
    assertNull(cell.getTextTitleBackgroundEffect());
    assertEquals("4px 0px 4px 8px", cell.getTextTitleBackgroundPadding());
    assertEquals("6px 12px 6px 6px", cell.getTextTitleBackgroundRadius());
    assertNull(cell.getTextHeaderColor());
    assertEquals("normal", cell.getTextHeaderFontWeight());
    assertEquals("italic", cell.getTextHeaderFontStyle());
    assertNull(cell.getTextHeaderBackgroundColor());
    assertEquals("#333333", cell.getTextColor());
    assertEquals("16px", cell.getTextFontSize());
    assertNull(cell.getTextSubtitleColor());
    assertNull(cell.getTextSubtitleFontWeight());
    assertEquals("#444444", cell.getIconColor());

    service.applyStyling(cell, null, StylingDestination.SIDEBAR, false, false);
    assertNull(cell.getTextTitleColor());
    assertNull(cell.getTextTitleFontWeight());
    assertNull(cell.getTextTitleBackgroundPadding());
    assertNull(cell.getIconColor());

    assertEquals(REFUSED + "text",
                 refused(() -> service.applyStyling(cell, styling(null, null, null, null, text, null), StylingDestination.DYNAMIC_SECTION, false, false)));
    assertEquals(REFUSED + "text.content.background",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, null, text(null, null, new TextRoleBlock(null, null, null, null, titleBackground)), null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "text.header.size",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, null, text(null, new TextRoleBlock(null, 61, null, null, null), null), null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "text.title.background.padding.top",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, null,
                                                            text(new TextRoleBlock(null, null, null, null,
                                                                                   new TextBackgroundBlock(true, null, null, null, null, null, null, null, null,
                                                                                                           new SidesBlock(401, null, null, null), null)),
                                                                 null,
                                                                 null),
                                                            null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "text.icon_color",
                 refused(() -> service.applyStyling(cell,
                                                    styling(null, null, null, null, new TextBlock(true, null, null, null, null, "blue"), null),
                                                    StylingDestination.SIDEBAR,
                                                    false,
                                                    false)));
  }

  @Test
  public void heightIsAnApplicationBlockAndNeverTouchesAContainersHeight() {
    LayoutModel application = new LayoutModel();
    service.applyStyling(application, styling(null, null, null, null, null, new HeightBlock(true, HeightPreset.MEDIUM, null)),
                         StylingDestination.APPLICATION, false, false);
    assertEquals("300px", application.getHeight());
    service.applyStyling(application, styling(null, null, null, null, null, new HeightBlock(true, HeightPreset.CUSTOM, 420)),
                         StylingDestination.APPLICATION, false, false);
    assertEquals("420", application.getHeight());
    service.applyStyling(application, null, StylingDestination.APPLICATION, false, false);
    assertNull(application.getHeight());
    assertEquals(REFUSED + "height.custom_px",
                 refused(() -> service.applyStyling(application,
                                                    styling(null, null, null, null, null, new HeightBlock(true, HeightPreset.CUSTOM, 1001)),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "height.fixed",
                 refused(() -> service.applyStyling(application,
                                                    styling(null, null, null, null, null, new HeightBlock(true, null, 100)),
                                                    StylingDestination.APPLICATION,
                                                    false,
                                                    false)));

    LayoutModel banner = new LayoutModel();
    banner.setHeight("60");
    service.applyStyling(banner, null, StylingDestination.BANNER, false, false);
    assertEquals("60", banner.getHeight());
    assertEquals(REFUSED + "height",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, null, null, null, null, new HeightBlock(true, HeightPreset.SMALL, null)),
                                                    StylingDestination.BANNER,
                                                    false,
                                                    false)));
    assertEquals(REFUSED + "height",
                 refused(() -> service.applyStyling(banner,
                                                    styling(null, null, null, null, null, new HeightBlock(true, HeightPreset.SMALL, null)),
                                                    StylingDestination.SITE_CONTAINER_APPLICATION,
                                                    false,
                                                    false)));
  }

  @Test
  public void displayIsAClassTokenAndASiteContainerHidesOnMobileOnly() {
    LayoutModel cell = new LayoutModel();
    cell.setCssClass("custom hidden-md-and-up");
    service.applyDisplay(cell, DisplayMode.HIDE_MOBILE, StylingDestination.SIDEBAR, "refused display");
    assertEquals("custom hidden-sm-and-down", cell.getCssClass());
    service.applyDisplay(cell, null, StylingDestination.SIDEBAR, "refused display");
    assertEquals("custom", cell.getCssClass());
    assertEquals("refused display",
                 refused(() -> service.applyDisplay(cell, DisplayMode.HIDE_DESKTOP, StylingDestination.BANNER, "refused display")));
    assertEquals("refused display",
                 refused(() -> service.applyDisplay(cell, DisplayMode.HIDE_MOBILE, StylingDestination.GRID_SECTION, "refused display")));

    LayoutModel application = new LayoutModel();
    application.setCssClass("mt-n2 brtr-2 custom brbl-0");
    service.applyDisplay(application, DisplayMode.HIDE_DESKTOP, StylingDestination.SITE_CONTAINER_APPLICATION, "refused display");
    service.applyStyling(application, null, StylingDestination.SITE_CONTAINER_APPLICATION, false, false);
    assertEquals("custom hidden-md-and-up", application.getCssClass());
    application.setCssClass(null);
    service.applyDisplay(application, DisplayMode.BOTH, StylingDestination.SITE_CONTAINER_APPLICATION, "refused display");
    assertNull(application.getCssClass());
  }

  private String refused(Runnable call) {
    return assertThrows(IllegalArgumentException.class, call::run).getMessage();
  }

  private StylingDescription styling(MarginsBlock margins,
                                     BorderBlock border,
                                     CornersBlock corners,
                                     BackgroundBlock background,
                                     TextBlock text,
                                     HeightBlock height) {
    return new StylingDescription(margins, border, corners, background, text, height);
  }

  private BackgroundBlock background(BackgroundMode mode, String color, String from, String to, Integer ratio, String direction, String scrollColor) {
    return new BackgroundBlock(true, mode, color, from, to, ratio, direction, scrollColor, null, null);
  }

  private TextBlock text(TextRoleBlock title, TextRoleBlock header, TextRoleBlock content) {
    return new TextBlock(true, title, header, content, null, null);
  }

}
