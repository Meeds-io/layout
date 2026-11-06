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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PageContainerBackgroundUpgradePluginTest {

  private static final PageKey LOGIN_PAGE_KEY    = new PageKey("portal", "global", "login");

  private static final PageKey REGISTER_PAGE_KEY = new PageKey("portal", "global", "register");

  @Mock
  private SettingService       settingService;

  @Mock
  private LayoutService        layoutService;

  @Mock
  private InitParams           initParams;

  @Test
  void removesTheConfiguredColorWhateverItsCase() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Container container = containerWithBackground("#ffffff");
    Page page = page(container);
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page);

    assertTrue(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    assertNull(container.getCssStyle().getBackgroundColor());
    verify(layoutService).save(page);
  }

  @Test
  void keepsTheOtherStylePropertiesAndTheChildrenOfTheContainer() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Container container = containerWithBackground("#FFFFFF");
    container.getCssStyle().setMarginTop(0);
    container.getCssStyle().setBackgroundImage("/image.png");
    Application window = new Application();
    container.getChildren().add(window);
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page(container));

    assertTrue(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    assertEquals(0, container.getCssStyle().getMarginTop());
    assertEquals("/image.png", container.getCssStyle().getBackgroundImage());
    assertEquals(List.of(window), container.getChildren());
  }

  @Test
  void keepsAColorChosenSinceAndDoesNotSaveThePage() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Container container = containerWithBackground("#F0F0F0");
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page(container));

    assertFalse(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    assertEquals("#F0F0F0", container.getCssStyle().getBackgroundColor());
    verify(layoutService, never()).save(any(Page.class));
  }

  @Test
  void doesNotSaveAPageWhoseContainersHaveNoStyleOrNoColor() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Container withoutStyle = new Container();
    Container withoutColor = containerWithBackground(null);
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page(withoutStyle, withoutColor));

    assertFalse(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    verify(layoutService, never()).save(any(Page.class));
  }

  @Test
  void keepsTheColorOfANestedContainer() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Container nested = containerWithBackground("#FFFFFF");
    Container topLevel = containerWithBackground(null);
    topLevel.getChildren().add(nested);
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page(topLevel));

    assertFalse(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    assertEquals("#FFFFFF", nested.getCssStyle().getBackgroundColor());
    verify(layoutService, never()).save(any(Page.class));
  }

  @Test
  void skipsAMissingPage() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(null);

    assertFalse(plugin.removeContainerBackground(LOGIN_PAGE_KEY));

    verify(layoutService, never()).save(any(Page.class));
  }

  @Test
  void upgradesEveryConfiguredPageOfTheConfiguredSite() {
    PageContainerBackgroundUpgradePlugin plugin = spy(newPlugin("global", "#FFFFFF", "login", " register "));
    doReturn(true).when(plugin).upgradePage(any(PageKey.class));

    plugin.processUpgrade(null, null);

    verify(plugin).upgradePage(LOGIN_PAGE_KEY);
    verify(plugin).upgradePage(REGISTER_PAGE_KEY);
  }

  @Test
  void upgradesTheOtherPagesAndThrowsWhenAPageFails() {
    PageContainerBackgroundUpgradePlugin plugin = spy(newPlugin("global", "#FFFFFF", "login", "register"));
    doThrow(new IllegalStateException("save failed")).when(plugin).upgradePage(LOGIN_PAGE_KEY);
    doReturn(true).when(plugin).upgradePage(REGISTER_PAGE_KEY);

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> plugin.processUpgrade(null, null));

    verify(plugin).upgradePage(REGISTER_PAGE_KEY);
    assertTrue(exception.getMessage().contains(LOGIN_PAGE_KEY.format()));
    assertFalse(exception.getMessage().contains(REGISTER_PAGE_KEY.format()));
  }

  @Test
  void letsAPageSaveFailurePropagate() {
    PageContainerBackgroundUpgradePlugin plugin = newPlugin("global", "#FFFFFF", "login");
    Page page = page(containerWithBackground("#FFFFFF"));
    when(layoutService.getPage(LOGIN_PAGE_KEY)).thenReturn(page);
    doThrow(new IllegalStateException("save failed")).when(layoutService).save(page);

    assertThrows(IllegalStateException.class, () -> plugin.upgradePage(LOGIN_PAGE_KEY));
  }

  @Test
  void proceedsOnlyWithASiteAColorAndPages() {
    assertTrue(newPlugin("global", "#FFFFFF", "login").shouldProceedToUpgrade(null, null));
    assertFalse(newPlugin(null, "#FFFFFF", "login").shouldProceedToUpgrade(null, null));
    assertFalse(newPlugin("global", " ", "login").shouldProceedToUpgrade(null, null));
    assertFalse(newPlugin("global", "#FFFFFF").shouldProceedToUpgrade(null, null));
  }

  @Test
  void proceedsOnlyWhenEnabledOrNotConfigured() {
    when(initParams.getValueParam(PageContainerBackgroundUpgradePlugin.ENABLED_PARAM)).thenReturn(valueParam("true"));
    assertTrue(newPlugin("global", "#FFFFFF", "login").shouldProceedToUpgrade(null, null));
    when(initParams.getValueParam(PageContainerBackgroundUpgradePlugin.ENABLED_PARAM)).thenReturn(valueParam("false"));
    assertFalse(newPlugin("global", "#FFFFFF", "login").shouldProceedToUpgrade(null, null));
    when(initParams.getValueParam(PageContainerBackgroundUpgradePlugin.ENABLED_PARAM)).thenReturn(null);
    assertTrue(newPlugin("global", "#FFFFFF", "login").shouldProceedToUpgrade(null, null));
  }

  private PageContainerBackgroundUpgradePlugin newPlugin(String siteName, String color, String... pageNames) {
    when(initParams.getValueParam(PageContainerBackgroundUpgradePlugin.SITE_NAME_PARAM)).thenReturn(valueParam(siteName));
    when(initParams.getValueParam(PageContainerBackgroundUpgradePlugin.BACKGROUND_COLOR_PARAM)).thenReturn(valueParam(color));
    ValuesParam pageNamesParam = new ValuesParam();
    pageNamesParam.setValues(new ArrayList<>(Arrays.asList(pageNames)));
    when(initParams.getValuesParam(PageContainerBackgroundUpgradePlugin.PAGE_NAMES_PARAM)).thenReturn(pageNamesParam);
    return new PageContainerBackgroundUpgradePlugin(settingService, layoutService, initParams);
  }

  private ValueParam valueParam(String value) {
    if (value == null) {
      return null;
    }
    ValueParam valueParam = new ValueParam();
    valueParam.setValue(value);
    return valueParam;
  }

  private Container containerWithBackground(String color) {
    Container container = new Container();
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor(color);
    container.setCssStyle(style);
    container.setChildren(new ArrayList<>());
    return container;
  }

  private Page page(ModelObject... children) {
    Page page = new Page();
    page.setChildren(new ArrayList<>(Arrays.asList(children)));
    return page;
  }

}
