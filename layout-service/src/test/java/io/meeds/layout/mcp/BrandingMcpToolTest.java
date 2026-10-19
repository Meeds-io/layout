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
package io.meeds.layout.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.layout.mcp.constant.ThemePart;
import io.meeds.layout.mcp.model.NavigationDisplayDescription;
import io.meeds.layout.mcp.model.SidebarDisplayOptions;
import io.meeds.layout.mcp.model.ThemeDescription;
import io.meeds.layout.mcp.model.TopbarDisplayOptions;
import io.meeds.layout.service.NavigationDisplayService;
import io.meeds.layout.service.ThemeUpdateService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { BrandingMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
public class BrandingMcpToolTest {

  private static final String      USER = "john";

  @MockitoBean
  private ThemeUpdateService       themeUpdateService;

  @MockitoBean
  private NavigationDisplayService navigationDisplayService;

  @Autowired
  private BrandingMcpTool          tool;

  private ConversationState        conversationState;

  @BeforeEach
  public void setUp() {
    Identity identity = mock(Identity.class);
    lenient().when(identity.getUserId()).thenReturn(USER);
    conversationState = mock(ConversationState.class);
    lenient().when(conversationState.getIdentity()).thenReturn(identity);
  }

  @Test
  public void updateThemeParsesThePartAndActsAsTheCurrentUser() throws Exception {
    Map<String, String> options = Map.of("primaryColor", "#123456");
    List<String> reset = List.of("secondaryColor");
    Map<String, String> stored = Map.of("primaryColor", "#123456", "tertiaryColor", "#e25d5d");
    when(themeUpdateService.updateTheme(ThemePart.COLORS, options, reset, USER)).thenReturn(stored);

    assertEquals(new ThemeDescription(ThemePart.COLORS, stored), asUser(() -> tool.updateTheme("Colors", options, reset)));
  }

  @Test
  public void partOutsideTheVocabularyIsRefusedBeforeTheService() {
    assertEquals(ThemePart.PART_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.updateTheme("logo", Map.of(), null))).getMessage());
    assertEquals(ThemePart.PART_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.updateTheme(null, Map.of(), null))).getMessage());
    verifyNoInteractions(themeUpdateService);
  }

  @Test
  public void updateNavigationDisplayActsAsTheCurrentUser() throws Exception {
    TopbarDisplayOptions topbar = new TopbarDisplayOptions(true, null, null, null);
    SidebarDisplayOptions sidebar = new SidebarDisplayOptions(null, List.of("icon"), "icon");
    NavigationDisplayDescription stored = new NavigationDisplayDescription(topbar, sidebar);
    when(navigationDisplayService.updateNavigationDisplay(topbar, sidebar, USER, Locale.FRENCH)).thenReturn(stored);

    assertEquals(stored, asUser(Locale.FRENCH, () -> tool.updateNavigationDisplay(topbar, sidebar)));
  }

  @Test
  public void serviceRefusalsPassUnchanged() throws Exception {
    when(themeUpdateService.updateTheme(ThemePart.FONT, null, null, USER)).thenThrow(new IllegalAccessException(ThemeUpdateService.ADMINISTRATORS_ONLY));
    when(navigationDisplayService.updateNavigationDisplay(null, null, USER, Locale.ENGLISH)).thenThrow(new ObjectNotFoundException(NavigationDisplayService.CONFIGURATION_NOT_FOUND));

    assertEquals(ThemeUpdateService.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class, () -> asUser(() -> tool.updateTheme("font", null, null))).getMessage());
    assertEquals(NavigationDisplayService.CONFIGURATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> asUser(Locale.ENGLISH, () -> tool.updateNavigationDisplay(null, null))).getMessage());
  }

  @SneakyThrows
  private <T> T asUser(Callable<T> call) {
    try (MockedStatic<ConversationState> conversation = mockStatic(ConversationState.class)) {
      conversation.when(ConversationState::getCurrent).thenReturn(conversationState);
      return call.call();
    }
  }

  @SneakyThrows
  private <T> T asUser(Locale locale, Callable<T> call) {
    try (MockedStatic<LocaleContextInfoUtils> locales = mockStatic(LocaleContextInfoUtils.class)) {
      locales.when(() -> LocaleContextInfoUtils.getUserLocale(USER)).thenReturn(locale);
      return asUser(call);
    }
  }
}
