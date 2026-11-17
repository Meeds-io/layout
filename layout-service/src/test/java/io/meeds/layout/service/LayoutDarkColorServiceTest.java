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
package io.meeds.layout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.mop.page.PageKey;

import io.meeds.layout.model.DarkColorBlock;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.util.CssColorParser;
import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.util.ThemeColorCalculator;

@ExtendWith(MockitoExtension.class)
public class LayoutDarkColorServiceTest {

  private static final String LIGHT_ONLY_PORTLET = "time-tracker/TimeTrackingPortlet";

  @Mock
  private BrandingService     brandingService;

  @InjectMocks
  private LayoutDarkColorService service;

  @BeforeEach
  void setUp() {
    lenient().when(brandingService.isLightOnlyApplication(LIGHT_ONLY_PORTLET)).thenReturn(true);
  }

  @Test
  void flagsALightOnlyApplicationAndComputesNoDarkColorsForIt() {
    LayoutModel application = application("1", LIGHT_ONLY_PORTLET);
    application.setBackgroundColor("#FFFFFF");
    application.setTextColor("#20282C");
    LayoutModel other = application("2", "social-portlet/Other");
    LayoutModel page = page(application, other);

    service.expand(page, null, true);

    assertEquals(Boolean.TRUE, application.getLightOnly());
    assertNull(application.getDarkColors());
    assertNull(other.getLightOnly());
  }

  @Test
  void lightOnlyUsesTheContentIdResolvedFromTheStorage() {
    LayoutModel application = application("1", null);
    LayoutModel page = page(application);

    service.expand(page, Map.of("1", LIGHT_ONLY_PORTLET), true);

    assertEquals(Boolean.TRUE, application.getLightOnly());
  }

  @Test
  void containerGetsItsDarkColors() {
    LayoutModel section = section();
    section.setBackgroundColor("#FFFFFF");
    section.setTextColor("#20282C");
    section.setTextHeaderColor("#FFFFFF");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    assertEquals(Map.of("backgroundColor", "#1E2328", "textColor", ""), section.getDarkColors());
    assertNull(page.getDarkColors());
  }

  @Test
  void pageBackgroundTakesThePageDefault() {
    LayoutModel page = page();
    page.setBackgroundColor("#F0F0F0");

    service.expand(page, null, true);

    assertEquals("#14181B", page.getDarkColors().get("backgroundColor"));
  }

  @Test
  void textInheritedByAnApplicationWithItsOwnBackgroundIsResolvedForTheApplication() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setBackgroundColor("#FFFFFF");
    LayoutModel section = section(application);
    section.setTextColor("#20282C");
    section.setIconColor("#FFFFFF");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    // the section has no background: its own text colour is Auto
    assertEquals(Map.of("textColor", "", "iconColor", ""), section.getDarkColors());
    // the application sits on its own background: the inherited dark text is unreadable there. The section has no background, so its own icon is Auto too (the application shows the theme icon color)
    assertEquals("#1E2328", application.getDarkColors().get("backgroundColor"));
    assertEquals("", application.getDarkColors().get("textColor"));
    assertFalse(application.getDarkColors().containsKey("iconColor"));
  }

  @Test
  void siteLayoutRootTakesThePageBackgroundDefault() {
    LayoutModel root = container();
    root.setBackgroundColor("#F0F0F0");
    LayoutModel section = section();
    section.setBackgroundColor("#F0F0F0");
    root.getChildren().add(section);

    service.expand(root, null, true);

    assertEquals("#14181B", root.getDarkColors().get("backgroundColor"));
    assertEquals(ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#F0F0F0"),
                 section.getDarkColors().get("backgroundColor"));
  }

  @Test
  void applicationWithItsOwnImageKeepsItsTextUnderAnInheritedBackground() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setBackgroundImage("/img.png");
    application.setTextColor("#20282C");
    LayoutModel section = section(application);
    section.setAppBackgroundColor("#FFFFFF");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    assertNull(application.getDarkColors());
  }

  @Test
  void imageInheritedFromThePageCountsAsAnImageForABlockWithoutColour() {
    LayoutModel section = section();
    section.setTextColor("#20282C");
    LayoutModel page = page(section);
    page.setBackgroundImage("/img.png");

    service.expand(page, null, true);

    assertNull(section.getDarkColors());
    // the same section over a page without image: its text is Auto
    section.setDarkColors(null);
    page.setBackgroundImage(null);
    service.expand(page, null, true);
    assertEquals(Map.of("textColor", ""), section.getDarkColors());
  }

  @Test
  void imageInheritedThroughSeveralLevelsCountsForABlockWithoutColour() {
    LayoutModel inner = section();
    inner.setTextColor("#20282C");
    LayoutModel outer = section(inner);
    LayoutModel page = page(outer);
    page.setBackgroundImage("/img.png");

    service.expand(page, null, true);

    assertNull(inner.getDarkColors());
  }

  @Test
  void imageInheritedByAnApplicationIsIgnoredWhenItInheritsABackground() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setTextColor("#20282C");
    LayoutModel section = section(application);
    section.setAppBackgroundColor("#FFFFFF");
    LayoutModel page = page(section);
    page.setBackgroundImage("/img.png");

    service.expand(page, null, true);

    assertEquals(Map.of("textColor", ""), application.getDarkColors());
  }

  @Test
  void lightOnlyApplicationGetsTheStoredColorsItInherits() {
    LayoutModel application = application("1", LIGHT_ONLY_PORTLET);
    LayoutModel section = section(application);
    section.setTextColor("#20282C");
    section.setBorderColor("#111111");
    section.setAppBackgroundColor("#FFFFFF");
    section.setAppBackgroundImage("/app.png");
    LayoutModel page = page(section);
    page.setIconColor("#222222");
    page.setBorderColor("#333333");
    page.setBorderSize("2");
    page.setAppBackgroundColor("#EEEEEE");
    page.setTextColor("#444444");

    service.expand(page, null, true);

    assertEquals(Map.of("textColor", "#20282C",
                        "iconColor", "#222222",
                        "borderColor", "#333333",
                        "borderSize", "2",
                        "appBackgroundColor", "#FFFFFF",
                        "appBackgroundImage", "/app.png"),
                 application.getLightColors());
    assertNull(application.getDarkColors());
  }

  @Test
  void applicationInAContainerWithAnImageIsPaintedWithTheApplicationBackground() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setTextColor("#20282C");
    LayoutModel section = section(application);
    section.setBackgroundImage("/img.png");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    assertEquals(Map.of("textColor", ""), application.getDarkColors());
  }

  @Test
  void applicationKeepsItsTextUnderAnInheritedApplicationBackgroundImage() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setTextColor("#20282C");
    LayoutModel section = section(application);
    LayoutModel page = page(section);
    page.setAppBackgroundImage("/app.png");

    service.expand(page, null, true);

    assertNull(application.getDarkColors());
  }

  @Test
  void inheritedApplicationBackgroundColorWinsOverAnInheritedApplicationBackgroundImage() {
    LayoutModel application = application("1", "social-portlet/Other");
    application.setTextColor("#20282C");
    LayoutModel section = section(application);
    section.setAppBackgroundColor("#FFFFFF");
    LayoutModel page = page(section);
    page.setAppBackgroundImage("/app.png");

    service.expand(page, null, true);

    assertEquals(Map.of("textColor", ""), application.getDarkColors());
  }

  @Test
  void pageBackgroundOfThePageLayoutContainerTakesThePageDefault() {
    LayoutModel pageLayout = section();
    pageLayout.setTemplate("system:/groovy/portal/webui/container/UIPageLayout.gtmpl");
    pageLayout.setBackgroundColor("#F0F0F0");
    LayoutModel body = section();
    body.setTemplate("SiteMiddleCenterBody");
    body.setBackgroundColor("#F0F0F0");
    LayoutModel plain = section();
    plain.setBackgroundColor("#F0F0F0");
    LayoutModel page = page(pageLayout, body, plain);

    service.expand(page, null, true);

    assertEquals("#14181B", pageLayout.getDarkColors().get("backgroundColor"));
    assertEquals("#14181B", body.getDarkColors().get("backgroundColor"));
    assertEquals(ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#F0F0F0"), plain.getDarkColors().get("backgroundColor"));
  }

  @Test
  void lightOnlyIsNotCheckedWhenNoPortletIsListed() {
    LayoutModel application = application("1", LIGHT_ONLY_PORTLET);
    service.expand(page(application), null, false);
    assertNull(application.getLightOnly());
  }

  @Test
  void hasLightOnlyApplicationsReadsTheBrandingList() {
    assertFalse(service.hasLightOnlyApplications());
    org.mockito.Mockito.when(brandingService.getLightOnlyApplications()).thenReturn(List.of("TimeTrackingPortlet"));
    assertTrue(service.hasLightOnlyApplications());
  }

  @Test
  void colourLongerThanTheLimitIsRefused() {
    String longValue = "1".repeat(CssColorParser.MAX_LENGTH + 1);
    assertThrows(IllegalArgumentException.class,
                 () -> service.computeDarkColors(List.of(new DarkColorBlock(Map.of("textColor", "rgb(" + longValue + ",0,0)"),
                                                                            null, false, false))));
    assertThrows(IllegalArgumentException.class,
                 () -> service.computeDarkColors(List.of(new DarkColorBlock(Map.of("backgroundColor", "#FFF@" + longValue),
                                                                            null, false, false))));
  }

  @Test
  void textInheritedByAnApplicationWithoutBackgroundGetsNoEntry() {
    LayoutModel application = application("1", "social-portlet/Other");
    LayoutModel section = section(application);
    section.setBackgroundColor("#FFFFFF");
    section.setTextColor("#20282C");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    assertNull(application.getDarkColors());
  }

  @Test
  void applicationBackgroundInheritedFromTheSectionIsTheBackgroundOfItsText() {
    LayoutModel application = application("1", "social-portlet/Other");
    LayoutModel section = section(application);
    section.setAppBackgroundColor("#FFFFFF");
    section.setTextColor("#20282C");
    LayoutModel page = page(section);

    service.expand(page, null, true);

    assertEquals("", application.getDarkColors().get("textColor"));
    assertEquals("#1E2328", section.getDarkColors().get("appBackgroundColor"));
  }

  @Test
  void aNullColorOfABlockIsTreatedAsUnset() {
    Map<String, String> colors = new java.util.HashMap<>();
    colors.put("backgroundColor", null);
    colors.put("textColor", "#20282C");
    Map<String, String> withNull = service.computeDarkColors(List.of(new DarkColorBlock(colors, null, false, false))).get(0);
    Map<String, String> without = service.computeDarkColors(List.of(new DarkColorBlock(Map.of("textColor", "#20282C"),
                                                                                       null,
                                                                                       false,
                                                                                       false)))
                                         .get(0);
    assertEquals(without, withNull);
  }

  @Test
  void computeDarkColorsOfBlocks() {
    DarkColorBlock block = new DarkColorBlock(Map.of("backgroundColor", "#FFFFFF", "textColor", "#20282C", "unknown", "#000000"),
                                              null,
                                              false,
                                              false);
    DarkColorBlock withoutBackground = new DarkColorBlock(Map.of("textColor", "#FFFFFF"), null, false, false);
    DarkColorBlock image = new DarkColorBlock(Map.of("textColor", "#FFFFFF"), null, true, false);
    DarkColorBlock inheritedBackground = new DarkColorBlock(Map.of("textColor", "#20282C"), "#FFFFFF", false, false);
    DarkColorBlock page = new DarkColorBlock(Map.of("backgroundColor", "#F0F0F0"), null, false, true);

    List<Map<String, String>> result = service.computeDarkColors(List.of(block, withoutBackground, image, inheritedBackground, page));

    assertEquals(Map.of("backgroundColor", "#1E2328", "textColor", ""), result.get(0));
    assertEquals(Map.of("textColor", ""), result.get(1));
    assertTrue(result.get(2).isEmpty());
    assertEquals(Map.of("textColor", ""), result.get(3));
    assertEquals(Map.of("backgroundColor", "#14181B"), result.get(4));
  }

  @Test
  void computeDarkColorsAcceptsTheLimitAndRefusesMore() {
    List<DarkColorBlock> blocks = new ArrayList<>();
    for (int i = 0; i < LayoutDarkColorService.MAX_BLOCKS; i++) {
      blocks.add(new DarkColorBlock(Map.of("backgroundColor", "#FFFFFF"), null, false, false));
    }
    assertEquals(LayoutDarkColorService.MAX_BLOCKS, service.computeDarkColors(blocks).size());
    blocks.add(new DarkColorBlock(Map.of("backgroundColor", "#FFFFFF"), null, false, false));
    IllegalArgumentException tooMany = assertThrows(IllegalArgumentException.class, () -> service.computeDarkColors(blocks));
    assertEquals("layout.colors.tooManyBlocks", tooMany.getMessage());
  }

  @Test
  void computeDarkColorsRefusesAMissingListWithItsOwnCode() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.computeDarkColors(null));
    assertEquals("layout.colors.blocksMandatory", e.getMessage());
  }

  @Test
  void computeDarkColorsRefusesAColourOutsideTheValidatorCharacterClass() {
    List<DarkColorBlock> blocks = List.of(new DarkColorBlock(Map.of("textColor", "red;background:url(x)"), null, false, false));
    assertThrows(IllegalArgumentException.class, () -> service.computeDarkColors(blocks));
    List<DarkColorBlock> background = List.of(new DarkColorBlock(Map.of("backgroundColor", "#FFF"), "<script>", false, false));
    assertThrows(IllegalArgumentException.class, () -> service.computeDarkColors(background));
  }

  @Test
  void computeDarkColorsKeepsAnUnparseableColour() {
    DarkColorBlock block = new DarkColorBlock(Map.of("backgroundColor", "var(--x)", "textColor", "#20282C"), null, false, false);
    assertTrue(service.computeDarkColors(List.of(block)).get(0).isEmpty());
  }

  private LayoutModel page(LayoutModel... children) {
    LayoutModel page = container(children);
    page.setPageKey(PageKey.parse("portal::global::page"));
    return page;
  }

  private LayoutModel section(LayoutModel... children) {
    return container(children);
  }

  private LayoutModel container(LayoutModel... children) {
    LayoutModel container = new LayoutModel();
    container.setChildren(new ArrayList<>(List.of(children)));
    return container;
  }

  private LayoutModel application(String storageId, String contentId) {
    LayoutModel application = new LayoutModel();
    application.setStorageId(storageId);
    application.setContentId(contentId);
    return application;
  }

}
