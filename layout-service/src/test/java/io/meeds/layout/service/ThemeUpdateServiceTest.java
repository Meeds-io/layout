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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Logo;

import io.meeds.layout.mcp.constant.ThemePart;
import io.meeds.layout.util.BrandingPageStyleValidator;
import io.meeds.layout.mcp.constant.ThemeOptions;

@ExtendWith(MockitoExtension.class)
public class ThemeUpdateServiceTest {

  private static final String      USER            = "john";

  /**
   * The theme variables portal's branding-configuration.xml declares
   * (exo.branding.theme.variables), copied from the file at portal
   * feature/meeds-qaui ab916dda65f4: every part's options must be in this
   * list
   */
  private static final Set<String> DECLARED        = new HashSet<>(Arrays.asList(("""
      primaryColor secondaryColor tertiaryColor greyColorLighten1 textColor borderRadius fontFamily
      topBarTextColor topBarTextFontSize topBarTextFontStyle topBarTextFontWeight topBarIconColor
      topBarBackgroundColor topBarBackgroundPosition topBarBackgroundSize topBarBackgroundImage topBarBackgroundRepeat
      sideBarTextColor sideBarTextFontSize sideBarTextFontStyle sideBarTextFontWeight
      sideBarTextSubtitleColor sideBarTextSubtitleFontSize sideBarTextSubtitleFontStyle sideBarTextSubtitleFontWeight
      sideBarIconColor
      sideBarBackgroundColor sideBarBackgroundPosition sideBarBackgroundSize sideBarBackgroundImage sideBarBackgroundRepeat
      drawerTextTitleColor drawerTextTitleFontSize drawerTextTitleFontStyle drawerTextTitleFontWeight
      drawerTextHeaderColor drawerTextHeaderFontSize drawerTextHeaderFontStyle drawerTextHeaderFontWeight
      drawerTextColor drawerTextFontSize drawerTextFontStyle drawerTextFontWeight
      drawerTextSubtitleColor drawerTextSubtitleFontSize drawerTextSubtitleFontStyle drawerTextSubtitleFontWeight
      drawerIconColor
      drawerBackgroundColor drawerBackgroundPosition drawerBackgroundSize drawerBackgroundImage drawerBackgroundRepeat
      pageMarginTop pageMarginRight pageMarginBottom pageMarginLeft
      appMarginTop appMarginRight appMarginBottom appMarginLeft
      appBorderColor appBorderSize appBoxShadow
      appBorderRadiusTopLeft appBorderRadiusTopRight appBorderRadiusBottomLeft appBorderRadiusBottomRight
      appBackgroundColor appBackgroundImage appBackgroundPosition appBackgroundSize appBackgroundRepeat appBackgroundAttachment
      appTextTitleColor appTextTitleFontSize appTextTitleFontStyle appTextTitleFontWeight
      appTextHeaderColor appTextHeaderFontSize appTextHeaderFontStyle appTextHeaderFontWeight
      appTextColor appTextFontSize appTextFontStyle appTextFontWeight
      appTextSubtitleColor appTextSubtitleFontSize appTextSubtitleFontStyle appTextSubtitleFontWeight
      appIconColor
      appTextTitleBackgroundColor appTextTitleBackgroundImage appTextTitleBackgroundPosition appTextTitleBackgroundSize
      appTextTitleBackgroundRepeat appTextTitleBackgroundPaddingTop appTextTitleBackgroundPaddingRight
      appTextTitleBackgroundPaddingBottom appTextTitleBackgroundPaddingLeft appTextTitleBackgroundRadius
      appTextHeaderBackgroundColor appTextHeaderBackgroundImage appTextHeaderBackgroundPosition appTextHeaderBackgroundSize
      appTextHeaderBackgroundRepeat appTextHeaderBackgroundPaddingTop appTextHeaderBackgroundPaddingRight
      appTextHeaderBackgroundPaddingBottom appTextHeaderBackgroundPaddingLeft appTextHeaderBackgroundRadius
      topBarSticky topBarBackgroundScrollColor
      """).trim().split("\\s+")));

  /**
   * Declared variables no drawer of the Branding page writes: an option in
   * no part
   */
  private static final Set<String> NEVER_IN_THE_UI = Set.of("greyColorLighten1", "textColor", "appBackgroundAttachment");

  @Mock
  private BrandingService          brandingService;

  @Mock
  private LayoutAclService         aclService;

  @InjectMocks
  private ThemeUpdateService       service;

  @Test
  public void nonAdministratorIsRefusedBeforeAnyRead() {
    when(aclService.isAdministrator(USER)).thenReturn(false);

    IllegalAccessException refusal = assertThrows(IllegalAccessException.class,
                                                  () -> service.updateTheme(ThemePart.COLORS,
                                                                            Map.of("primaryColor", "#123456"),
                                                                            null,
                                                                            USER));
    assertEquals(ThemeUpdateService.ADMINISTRATORS_ONLY, refusal.getMessage());
    verify(brandingService, never()).getBrandingInformation(anyBoolean());
    verify(brandingService, never()).updateBrandingInformation(any());
  }

  @Test
  public void oneOptionKeepsEveryOtherStoredValueAndDropsThoseAtTheirDefault() throws Exception {
    administrator();
    Branding loaded = loaded();

    service.updateTheme(ThemePart.COLORS, Map.of("primaryColor", "#123456"), null, USER);

    Branding submitted = submitted();
    assertSame(loaded, submitted);
    assertTrue(BrandingPageStyleValidator.isAccepted("pageBackgroundEffect", "none"));
    assertEquals(Map.of("primaryColor", "#123456", "secondaryColor", "#111111", "topBarTextColor", "#222222"),
                 submitted.getThemeStyle());
    assertNull(submitted.getPageWidth());
    assertEquals("#ABCDEF", submitted.getPageBackgroundColor());
    assertSame(loaded.getLogo(), submitted.getLogo());
    assertNull(submitted.getLogo().getUploadId());
    assertEquals(Map.of("en", "Welcome"), submitted.getLoginTitle());
  }

  @Test
  public void resetReturnsTheOptionToItsDefault() throws Exception {
    administrator();
    loaded();

    service.updateTheme(ThemePart.COLORS, null, List.of("secondaryColor"), USER);

    assertEquals(Map.of("topBarTextColor", "#222222"), submitted().getThemeStyle());
  }

  @Test
  public void nullEntryIsIgnoredAndBlankValueRefused() throws Exception {
    administrator();
    loaded();
    Map<String, String> options = new HashMap<>();
    options.put("primaryColor", null);
    options.put("secondaryColor", "#333333");

    service.updateTheme(ThemePart.COLORS, options, null, USER);
    assertEquals("#333333", submitted().getThemeStyle().get("secondaryColor"));

    IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                                                    () -> service.updateTheme(ThemePart.COLORS,
                                                                              Map.of("primaryColor", " "),
                                                                              null,
                                                                              USER));
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": primaryColor", refusal.getMessage());
    verify(brandingService).updateBrandingInformation(any());
  }

  @Test
  public void optionOutsideThePartOrUndeclaredIsRefusedBeforeAnyWrite() throws Exception {
    administrator();
    Map<String, String> defaults = new HashMap<>(defaults());
    defaults.remove("topBarSticky");
    when(brandingService.getDefaultThemeStyle()).thenReturn(defaults);

    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": topBarTextColor",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.COLORS, Map.of("topBarTextColor", "#000000"), null, USER)).getMessage());
    assertEquals(ThemeUpdateService.RESET_NOT_ACCEPTED + ": topBarTextColor",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.COLORS, null, List.of("topBarTextColor"), USER)).getMessage());
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": topBarSticky",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.TOPBAR, Map.of("topBarSticky", "true"), null, USER)).getMessage());
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.COLORS, Map.of(), List.of(), USER)).getMessage());
    assertEquals(ThemeUpdateService.RESET_NOT_ACCEPTED + ": primaryColor",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.COLORS,
                                                        Map.of("primaryColor", "#123456"),
                                                        List.of("primaryColor"),
                                                        USER)).getMessage());
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": sideBarBackgroundImage",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.SIDEBARS,
                                                        Map.of("sideBarBackgroundImage", "url(https://cdn/x.png)"),
                                                        null,
                                                        USER)).getMessage());
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": appTextTitleBackgroundImage",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.PAGE_AND_APPS,
                                                        Map.of("appTextTitleBackgroundImage",
                                                               "linear-gradient(#fff, #000), url(https://cdn/x.png)"),
                                                        null,
                                                        USER)).getMessage());
    verify(brandingService, never()).getBrandingInformation(anyBoolean());
    verify(brandingService, never()).updateBrandingInformation(any());
  }

  @Test
  public void pageFieldsAreSetAndResetOnTheBrandingItself() throws Exception {
    administrator();
    loaded();

    service.updateTheme(ThemePart.PAGE_AND_APPS,
                        Map.of("pageWidth", "1200px", "appMarginTop", "24px"),
                        List.of("pageBackgroundColor"),
                        USER);

    Branding submitted = submitted();
    assertEquals("1200px", submitted.getPageWidth());
    assertNull(submitted.getPageBackgroundColor());
    assertEquals("24px", submitted.getThemeStyle().get("appMarginTop"));
    assertEquals("#111111", submitted.getThemeStyle().get("secondaryColor"));
  }

  @Test
  public void pageFieldValuesAreValidatedAgainstThePageDrawerVocabularyBeforeAnyWrite() throws Exception {
    administrator();
    loaded();

    service.updateTheme(ThemePart.PAGE_AND_APPS,
                        Map.of("pageWidth", "100%",
                               "pageBackgroundColor", "#FFFFFFAA",
                               "pageBackgroundEffect", "linear-gradient(90deg, rgba(255, 255, 255, 0.5) 0%, #000000 100%)",
                               "pageBackgroundSize", "cover",
                               "pageBackgroundPosition", "left top",
                               "pageBackgroundRepeat", "no-repeat",
                               "pageBackgroundAttachment", "fixed"),
                        null,
                        USER);
    assertEquals("100%", submitted().getPageWidth());
    assertTrue(BrandingPageStyleValidator.isAccepted("pageWidth", "100px"));
    assertTrue(BrandingPageStyleValidator.isAccepted("pageWidth", "3000px"));
    assertTrue(BrandingPageStyleValidator.isAccepted("pageBackgroundColor", "#FFF"));
    assertTrue(BrandingPageStyleValidator.isAccepted("pageBackgroundColor", "#FFFA"));
    assertTrue(BrandingPageStyleValidator.isAccepted("pageBackgroundEffect", "none"));

    Map<String, String> refused = Map.of("pageWidth", "1320px; background:url(https://x)",
                                         "pageBackgroundColor", "red",
                                         "pageBackgroundEffect", "url(https://x)",
                                         "pageBackgroundSize", "cover; color: red",
                                         "pageBackgroundAttachment", "javascript:alert(1)");
    Map.of("pageWidth", List.of("0px", "99px", "3001px", "9999px", "5%", "1320"),
           "pageBackgroundColor", List.of("#12345", "#1234567", "#GGGGGG"),
           "pageBackgroundEffect", List.of("linear-gradient(#fff 0%, #000 100%), url(evil.png)",
                                           "linear-gradient(#fff, #000)), url(evil.png",
                                           "linear-gradient(90deg, #fff, #000"))
       .forEach((field, values) -> values.forEach(value -> assertFalse(BrandingPageStyleValidator.isAccepted(field, value),
                                                                       field + " " + value)));
    refused.forEach((field, value) -> assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": " + field,
                                                   assertThrows(IllegalArgumentException.class,
                                                                () -> service.updateTheme(ThemePart.PAGE_AND_APPS,
                                                                                          Map.of(field, value),
                                                                                          null,
                                                                                          USER)).getMessage()));
    assertEquals(ThemeUpdateService.OPTIONS_NOT_ACCEPTED + ": pageBackgroundEffect",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.updateTheme(ThemePart.PAGE_AND_APPS,
                                                        Map.of("pageBackgroundEffect", "linear-gradient(90deg, #fff, #000"),
                                                        null,
                                                        USER)).getMessage());
    verify(brandingService).updateBrandingInformation(any());
  }

  @Test
  public void brandingServiceRefusalPassesThroughUnchanged() throws Exception {
    administrator();
    loaded();
    doThrow(new IllegalArgumentException("branding.theme.invalidValue:primaryColor")).when(brandingService)
                                                                                     .updateBrandingInformation(any());

    IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                                                    () -> service.updateTheme(ThemePart.COLORS,
                                                                              Map.of("primaryColor", "red"),
                                                                              null,
                                                                              USER));
    assertEquals("branding.theme.invalidValue:primaryColor", refusal.getMessage());
  }

  @Test
  public void returnsThePartAsStoredAfterTheUpdate() throws Exception {
    administrator();
    Branding before = branding();
    Branding after = branding();
    after.setThemeStyle(Map.of("primaryColor", "#123456", "secondaryColor", "#111111", "tertiaryColor", "#e25d5d", "topBarTextColor", "#222222"));
    when(brandingService.getBrandingInformation(false)).thenReturn(before, after);

    Map<String, String> result = service.updateTheme(ThemePart.COLORS, Map.of("primaryColor", "#123456"), null, USER);

    assertEquals(Map.of("primaryColor", "#123456", "secondaryColor", "#111111", "tertiaryColor", "#e25d5d"), result);
  }

  @Test
  public void everyPartOptionIsADeclaredVariableTheBrandingPageWrites() {
    for (ThemePart part : ThemePart.values()) {
      assertTrue(DECLARED.containsAll(part.getThemeOptions()), part.name());
      assertTrue(part.getThemeOptions().stream().noneMatch(NEVER_IN_THE_UI::contains), part.name());
      assertEquals(part == ThemePart.PAGE_AND_APPS ? ThemeOptions.PAGE_FIELDS : Set.of(), part.getBrandingFields(), part.name());
    }
    assertEquals(Set.of("primaryColor", "secondaryColor", "tertiaryColor"), ThemePart.COLORS.getThemeOptions());
    assertEquals(Set.of("fontFamily"), ThemePart.FONT.getThemeOptions());
    assertTrue(ThemePart.TOPBAR.getThemeOptions().containsAll(Set.of("topBarSticky", "topBarBackgroundScrollColor", "topBarBackgroundColor", "topBarIconColor")));
    assertFalse(ThemePart.TOPBAR.getThemeOptions().contains("topBarBackgroundImage"));
    assertTrue(ThemePart.SIDEBARS.getThemeOptions().contains("sideBarIconColor"));
    assertTrue(ThemePart.DRAWERS.getThemeOptions().contains("drawerIconColor"));
    assertTrue(ThemePart.PAGE_AND_APPS.getThemeOptions().contains("appIconColor"));
    assertTrue(ThemePart.PAGE_AND_APPS.getThemeOptions().containsAll(Set.of("borderRadius", "appTextHeaderBackgroundRadius", "pageMarginLeft")));
  }

  @Test
  public void acceptedOptionsFollowTheDeclaration() {
    Set<String> declared = new HashSet<>(DECLARED);
    declared.remove("topBarSticky");

    Set<String> accepted = service.acceptedOptions(ThemePart.TOPBAR, declared);

    assertFalse(accepted.contains("topBarSticky"));
    assertTrue(accepted.contains("topBarTextColor"));
    assertTrue(service.acceptedOptions(ThemePart.PAGE_AND_APPS, Set.of()).containsAll(ThemeOptions.PAGE_FIELDS));
  }

  private void administrator() {
    when(aclService.isAdministrator(USER)).thenReturn(true);
    when(brandingService.getDefaultThemeStyle()).thenReturn(defaults());
  }

  private Map<String, String> defaults() {
    Map<String, String> defaults = new HashMap<>();
    DECLARED.forEach(variable -> defaults.put(variable, ""));
    defaults.put("primaryColor", "#3f8487");
    defaults.put("secondaryColor", "#e25d5d");
    defaults.put("tertiaryColor", "#e25d5d");
    defaults.put("fontFamily", "Arimo");
    defaults.put("topBarTextColor", "#20282c");
    defaults.put("topBarSticky", "false");
    defaults.put("pageWidth", "1320px");
    defaults.put("pageBackgroundColor", "#F0F0F0");
    return defaults;
  }

  private Branding loaded() {
    Branding branding = branding();
    when(brandingService.getBrandingInformation(false)).thenReturn(branding);
    return branding;
  }

  /**
   * primaryColor and fontFamily stored at their default, secondaryColor and
   * topBarTextColor at a value of their own, pageWidth at its default,
   * pageBackgroundColor at a value of its own
   */
  private Branding branding() {
    Branding branding = new Branding();
    Map<String, String> themeStyle = new HashMap<>();
    themeStyle.put("primaryColor", "#3f8487");
    themeStyle.put("fontFamily", "Arimo");
    themeStyle.put("secondaryColor", "#111111");
    themeStyle.put("topBarTextColor", "#222222");
    branding.setThemeStyle(themeStyle);
    branding.setPageWidth("1320px");
    branding.setPageBackgroundColor("#ABCDEF");
    branding.setLogo(new Logo());
    branding.setLoginTitle(new HashMap<>(Map.of("en", "Welcome")));
    return branding;
  }

  private Branding submitted() {
    ArgumentCaptor<Branding> captor = ArgumentCaptor.forClass(Branding.class);
    verify(brandingService).updateBrandingInformation(captor.capture());
    return captor.getValue();
  }
}
