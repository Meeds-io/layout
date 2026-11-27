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
package io.meeds.layout.plugin.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShippedStyleUpgradePluginTest {

  private static final PageKey             LOGIN    = new PageKey("portal", "global", "login");

  private static final PageKey             REGISTER = new PageKey("portal", "global", "register");

  private static final Map<String, String> SHIPPED  = Map.of("textTitleColor", "#20282C",
                                                             "textHeaderColor", "#707070",
                                                             "textColor", "#20282C",
                                                             "textSubtitleColor", "#707070");

  @Mock
  private SettingService                   settingService;

  @Mock
  private LayoutService                    layoutService;

  @Mock
  private InitParams                       initParams;

  @BeforeEach
  void setUp() {
    when(layoutService.getId(any())).thenAnswer(invocation -> ((TransientApplicationState) invocation.getArgument(0)).getContentId());
  }

  @Test
  void clearsTheShippedValuesOfTheNamedApplicationAtAnyDepthAndKeepsTheOthers() {
    ShippedStyleUpgradePlugin plugin = newPlugin("social/PlatformName", SHIPPED, "login");
    Application platformName = window("social/PlatformName", style("#20282c", "#707070", "#20282C", "#707070"));
    platformName.getCssStyle().setMarginTop(20);
    // the Sidebar Login app ships its own #707070 header: another application, kept
    Application sidebar = window("social/SidebarLogin", style("#FFFFFF", "#707070", "#FFFFFF", "#707070"));
    Container column = container(null, platformName);
    Page page = page(container(null, sidebar, column));
    when(layoutService.getPage(LOGIN)).thenReturn(page);

    assertTrue(plugin.clearPage(LOGIN));

    ModelStyle cleared = platformName.getCssStyle();
    assertNull(cleared.getTextTitleColor());
    assertNull(cleared.getTextHeaderColor());
    assertNull(cleared.getTextColor());
    assertNull(cleared.getTextSubtitleColor());
    assertEquals(20, cleared.getMarginTop());
    assertEquals("#707070", sidebar.getCssStyle().getTextHeaderColor());
    verify(layoutService).save(page);
  }

  @Test
  void removesTheShippedValuesFromTheWindowsStoredPropertiesWhichTheStorageWritesBack() {
    ShippedStyleUpgradePlugin plugin = newPlugin("social/PlatformName", SHIPPED, "login");
    Application platformName = window("social/PlatformName", style("#20282C", "#707070", "#123456", null));
    // as the storage reads a window back: every stored property, its colours
    // included
    platformName.getProperties()
                .putAll(Map.of("text-title-color", "#20282c",
                               "text-header-color", "#707070",
                               "text-color", "#123456",
                               "css-class", "loginTitle"));
    Application sidebar = window("social/SidebarLogin", null);
    sidebar.getProperties().put("text-header-color", "#707070");
    Page page = page(container(null, sidebar, platformName));
    when(layoutService.getPage(LOGIN)).thenReturn(page);

    assertTrue(plugin.clearPage(LOGIN));

    assertEquals(Map.of("text-color", "#123456", "css-class", "loginTitle"), platformName.getProperties());
    assertEquals("#707070", sidebar.getProperties().get("text-header-color"));
    verify(layoutService).save(page);
  }

  @Test
  void keepsAValueChosenSinceAndDoesNotSaveAnUnchangedPage() {
    ShippedStyleUpgradePlugin plugin = newPlugin("social/PlatformName", SHIPPED, "login");
    Application platformName = window("social/PlatformName", style("#123456", null, "#654321", null));
    when(layoutService.getPage(LOGIN)).thenReturn(page(container(null, platformName)));

    assertFalse(plugin.clearPage(LOGIN));

    assertEquals("#123456", platformName.getCssStyle().getTextTitleColor());
    verify(layoutService, never()).save(any(Page.class));
  }

  @Test
  void withoutAnApplicationClearsThePagesTopLevelContainersOnly() {
    ShippedStyleUpgradePlugin plugin = newPlugin(null, SHIPPED, "login");
    Container nested = container(style("#20282C", null, null, null));
    Container topLevel = container(style("#20282C", "#707070", null, null), nested);
    Page page = page(topLevel);
    when(layoutService.getPage(LOGIN)).thenReturn(page);

    assertTrue(plugin.clearPage(LOGIN));

    assertNull(topLevel.getCssStyle().getTextTitleColor());
    assertNull(topLevel.getCssStyle().getTextHeaderColor());
    assertEquals("#20282C", nested.getCssStyle().getTextTitleColor());
    verify(layoutService).save(page);
  }

  @Test
  void clearsTheSiteLayoutsContainersAtAnyDepth() {
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.SITE_LAYOUT_PARAM)).thenReturn(valueParam("true"));
    ShippedStyleUpgradePlugin plugin = newPlugin(null, Map.of("backgroundColor", "#FFFFFFFF"));
    Container banner = container(null);
    banner.setCssStyle(new ModelStyle());
    banner.getCssStyle().setBackgroundColor("#ffffffff");
    Container other = container(null);
    other.setCssStyle(new ModelStyle());
    other.getCssStyle().setBackgroundColor("#F0F0F0");
    SiteLayout layout = new SiteLayout();
    layout.setChildren(new ArrayList<>(Arrays.asList(container(null, banner), other)));
    PortalConfig site = org.mockito.Mockito.mock(PortalConfig.class);
    when(site.getPortalLayout()).thenReturn(layout);
    when(layoutService.getPortalConfig("portal", "global")).thenReturn(site);

    assertTrue(plugin.shouldProceedToUpgrade(null, null));
    assertTrue(plugin.clearSiteLayout());

    assertNull(banner.getCssStyle().getBackgroundColor());
    assertEquals("#F0F0F0", other.getCssStyle().getBackgroundColor());
    verify(layoutService).save(site);
  }

  @Test
  void skipsAMissingPageOrSite() {
    ShippedStyleUpgradePlugin plugin = newPlugin("social/PlatformName", SHIPPED, "login");

    assertFalse(plugin.clearPage(LOGIN));
    assertFalse(plugin.clearSiteLayout());

    verify(layoutService, never()).save(any(Page.class));
    verify(layoutService, never()).save(any(PortalConfig.class));
  }

  @Test
  void upgradesTheOtherPagesAndThrowsWhenAPageFails() {
    ShippedStyleUpgradePlugin plugin = spy(newPlugin("social/PlatformName", SHIPPED, "login", " register "));
    doThrow(new IllegalStateException("save failed")).when(plugin).upgradePage(LOGIN);
    doReturn(true).when(plugin).upgradePage(REGISTER);

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> plugin.processUpgrade(null, null));

    verify(plugin).upgradePage(REGISTER);
    assertTrue(exception.getMessage().contains(LOGIN.format()));
    assertFalse(exception.getMessage().contains(REGISTER.format()));
  }

  @Test
  void proceedsOnlyWhenEnabledWithASiteValuesAndPagesOrTheLayout() {
    assertTrue(newPlugin("social/PlatformName", SHIPPED, "login").shouldProceedToUpgrade(null, null));
    assertFalse(newPlugin("social/PlatformName", SHIPPED).shouldProceedToUpgrade(null, null));
    assertFalse(newPlugin("social/PlatformName", Map.of("borderColor", "#FFFFFF"), "login").shouldProceedToUpgrade(null, null),
                "a property this upgrade cannot clear is ignored");
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.ENABLED_PARAM)).thenReturn(valueParam("false"));
    assertFalse(newPlugin("social/PlatformName", SHIPPED, "login").shouldProceedToUpgrade(null, null));
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.ENABLED_PARAM)).thenReturn(null);
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.SITE_NAME_PARAM)).thenReturn(null);
    assertFalse(new ShippedStyleUpgradePlugin(settingService, layoutService, initParams).shouldProceedToUpgrade(null, null));
  }

  private ShippedStyleUpgradePlugin newPlugin(String application, Map<String, String> values, String... pageNames) {
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.SITE_NAME_PARAM)).thenReturn(valueParam("global"));
    when(initParams.getValueParam(ShippedStyleUpgradePlugin.APPLICATION_CONTENT_PARAM)).thenReturn(valueParam(application));
    PropertiesParam styleValues = new PropertiesParam();
    values.forEach(styleValues::setProperty);
    when(initParams.getPropertiesParam(ShippedStyleUpgradePlugin.STYLE_VALUES_PARAM)).thenReturn(styleValues);
    ValuesParam pageNamesParam = new ValuesParam();
    pageNamesParam.setValues(new ArrayList<>(Arrays.asList(pageNames)));
    when(initParams.getValuesParam(ShippedStyleUpgradePlugin.PAGE_NAMES_PARAM)).thenReturn(pageNamesParam);
    return new ShippedStyleUpgradePlugin(settingService, layoutService, initParams);
  }

  private static ValueParam valueParam(String value) {
    if (value == null) {
      return null;
    }
    ValueParam valueParam = new ValueParam();
    valueParam.setValue(value);
    return valueParam;
  }

  private static ModelStyle style(String title, String header, String text, String subtitle) {
    ModelStyle style = new ModelStyle();
    style.setTextTitleColor(title);
    style.setTextHeaderColor(header);
    style.setTextColor(text);
    style.setTextSubtitleColor(subtitle);
    return style;
  }

  private static Application window(String contentId, ModelStyle style) {
    Application application = new Application();
    application.setState(new TransientApplicationState(contentId));
    application.setCssStyle(style);
    return application;
  }

  private static Container container(ModelStyle style, ModelObject... children) {
    Container container = new Container();
    container.setCssStyle(style);
    container.setChildren(new ArrayList<>(Arrays.asList(children)));
    return container;
  }

  private static Page page(ModelObject... children) {
    Page page = new Page();
    page.setChildren(new ArrayList<>(Arrays.asList(children)));
    return page;
  }

}
