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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.resources.ResourceBundleService;

import io.meeds.layout.mcp.model.NavigationDisplayDescription;
import io.meeds.layout.mcp.model.SidebarDisplayOptions;
import io.meeds.layout.mcp.model.TopbarApplicationOption;
import io.meeds.layout.mcp.model.TopbarDisplayOptions;
import io.meeds.portal.navigation.constant.SidebarItemType;
import io.meeds.portal.navigation.constant.SidebarMode;
import io.meeds.portal.navigation.model.NavigationConfiguration;
import io.meeds.portal.navigation.model.SidebarConfiguration;
import io.meeds.portal.navigation.model.SidebarItem;
import io.meeds.portal.navigation.model.TopbarApplication;
import io.meeds.portal.navigation.model.TopbarConfiguration;
import io.meeds.portal.navigation.service.NavigationConfigurationService;

@ExtendWith(MockitoExtension.class)
public class NavigationDisplayServiceTest {

  private static final String            USER      = "john";

  private static final TopbarDisplayOptions NO_TOPBAR = new TopbarDisplayOptions(null, null, null, null);

  @Mock
  private NavigationConfigurationService navigationConfigurationService;

  @Mock
  private ResourceBundleService          resourceBundleService;

  @Mock
  private LayoutAclService               aclService;

  @InjectMocks
  private NavigationDisplayService       service;

  @Test
  public void nonAdministratorIsRefusedBeforeAnyRead() {
    when(aclService.isAdministrator(USER)).thenReturn(false);

    IllegalAccessException refusal = assertThrows(IllegalAccessException.class,
                                                  () -> service.updateNavigationDisplay(NO_TOPBAR, null, USER, Locale.ENGLISH));
    assertEquals(NavigationDisplayService.ADMINISTRATORS_ONLY, refusal.getMessage());
    verify(navigationConfigurationService, never()).getConfiguration();
    verify(navigationConfigurationService, never()).updateConfiguration(any());
  }

  @Test
  public void bothPartsOmittedIsRefusedAndMissingConfigurationNotFound() {
    administrator();

    assertEquals(NavigationDisplayService.DISPLAY_OPTIONS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.updateNavigationDisplay(null, null, USER, Locale.ENGLISH)).getMessage());

    when(navigationConfigurationService.getConfiguration()).thenReturn(null);
    assertEquals(NavigationDisplayService.CONFIGURATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.updateNavigationDisplay(NO_TOPBAR, null, USER, Locale.ENGLISH)).getMessage());
    verify(navigationConfigurationService, never()).updateConfiguration(any());
  }

  @Test
  public void topbarChangeKeepsTheSidebarAndReadsTheStoredConfigurationOnly() throws Exception {
    administrator();
    stored();

    service.updateNavigationDisplay(new TopbarDisplayOptions(null, false, null, null), null, USER, Locale.ENGLISH);

    NavigationConfiguration submitted = submitted();
    assertEquals(stored(false).getSidebar(), submitted.getSidebar());
    assertTrue(submitted.getTopbar().isDisplayCompanyName());
    assertFalse(submitted.getTopbar().isDisplaySiteName());
    assertFalse(submitted.getTopbar().isDisplayMobileCompanyLogo());
    assertEquals(stored(false).getTopbar().getApplications(), submitted.getTopbar().getApplications());
    verify(navigationConfigurationService, never()).getConfiguration(any(), any(), anyBoolean());
  }

  @Test
  public void sidebarChangeKeepsTheTopbarAndTheItems() throws Exception {
    administrator();
    stored();

    service.updateNavigationDisplay(null, new SidebarDisplayOptions(true, List.of("icon", "sticky", "icon"), "Sticky"), USER, Locale.ENGLISH);

    NavigationConfiguration submitted = submitted();
    assertEquals(stored(false).getTopbar(), submitted.getTopbar());
    assertTrue(submitted.getSidebar().isAllowUserCustomHome());
    assertEquals(List.of(SidebarMode.ICON, SidebarMode.STICKY), submitted.getSidebar().getAllowedModes());
    assertEquals(SidebarMode.STICKY, submitted.getSidebar().getDefaultMode());
    assertEquals(stored(false).getSidebar().getItems(), submitted.getSidebar().getItems());
  }

  @Test
  public void applicationsAreMergedByIdentifier() throws Exception {
    administrator();
    stored();

    service.updateNavigationDisplay(new TopbarDisplayOptions(null,
                                                             null,
                                                             null,
                                                             List.of(new TopbarApplicationOption(" notifications ", null, false, null))),
                                    null,
                                    USER,
                                    Locale.ENGLISH);

    List<TopbarApplication> applications = submitted().getTopbar().getApplications();
    assertEquals(List.of("search", "notifications"), applications.stream().map(TopbarApplication::getId).toList());
    assertTrue(applications.get(0).isEnabled());
    assertFalse(applications.get(0).isMobile());
    assertFalse(applications.get(1).isEnabled());
    assertTrue(applications.get(1).isMobile());
    assertEquals("generalSettings.notifications.name", applications.get(1).getName());
  }

  @Test
  public void applicationsAreNamedByNameWhenNoIdentifierIsGiven() throws Exception {
    administrator();
    NavigationConfiguration read = stored();
    read.getTopbar().getApplications().add(application("search-2", "generalSettings.search.name", true, false));

    service.updateNavigationDisplay(new TopbarDisplayOptions(null,
                                                             null,
                                                             null,
                                                             List.of(new TopbarApplicationOption(null, " NOTIFICATION ", null, false))),
                                    null,
                                    USER,
                                    Locale.ENGLISH);
    List<TopbarApplication> applications = submitted().getTopbar().getApplications();
    assertEquals(List.of("search", "notifications", "search-2"), applications.stream().map(TopbarApplication::getId).toList());
    assertTrue(applications.get(1).isEnabled());
    assertFalse(applications.get(1).isMobile());

    TopbarDisplayOptions ambiguous = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption(null, "search", false, null)));
    assertEquals(NavigationDisplayService.APPLICATIONS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.updateNavigationDisplay(ambiguous, null, USER, Locale.ENGLISH)).getMessage());
    TopbarDisplayOptions unknown = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption(null, "Chat", false, null)));
    assertEquals(NavigationDisplayService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.updateNavigationDisplay(unknown, null, USER, Locale.ENGLISH)).getMessage());
    TopbarDisplayOptions byKey = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption(null, "generalSettings.notifications.name", null, true)));
    service.updateNavigationDisplay(byKey, null, USER, Locale.ENGLISH);
    TopbarDisplayOptions noBundle = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption(null, "Notification", null, true)));
    when(resourceBundleService.getResourceBundle(any(String[].class), eq(Locale.ENGLISH))).thenReturn(null);
    assertEquals(NavigationDisplayService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.updateNavigationDisplay(noBundle, null, USER, Locale.ENGLISH)).getMessage());
    when(resourceBundleService.getResourceBundle(any(String[].class), eq(Locale.ENGLISH))).thenThrow(new IllegalStateException("no container"));
    assertEquals(NavigationDisplayService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.updateNavigationDisplay(noBundle, null, USER, Locale.ENGLISH)).getMessage());
    TopbarDisplayOptions idWins = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption("search-2", "Chat", false, null)));
    service.updateNavigationDisplay(idWins, null, USER, Locale.ENGLISH);
    ArgumentCaptor<NavigationConfiguration> captor = ArgumentCaptor.forClass(NavigationConfiguration.class);
    verify(navigationConfigurationService, times(3)).updateConfiguration(captor.capture());
    assertFalse(captor.getAllValues().get(1).getTopbar().getApplications().get(2).isEnabled());
    assertTrue(captor.getAllValues().get(1).getTopbar().getApplications().get(0).isEnabled());
    assertTrue(captor.getAllValues().get(2).getTopbar().getApplications().get(1).isMobile());
  }

  @Test
  public void unknownOrBlankApplicationIsRefusedAndTheReadCopyLeftUntouched() {
    administrator();
    NavigationConfiguration read = stored();
    TopbarDisplayOptions topbar = new TopbarDisplayOptions(null,
                                                           null,
                                                           null,
                                                           List.of(new TopbarApplicationOption("search", null, false, null),
                                                                   new TopbarApplicationOption("missing", null, true, null)));

    assertEquals(NavigationDisplayService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.updateNavigationDisplay(topbar, null, USER, Locale.ENGLISH)).getMessage());
    assertTrue(read.getTopbar().getApplications().get(0).isEnabled());

    TopbarDisplayOptions blank = new TopbarDisplayOptions(null, null, null, List.of(new TopbarApplicationOption(" ", null, false, null)));
    assertEquals(NavigationDisplayService.APPLICATIONS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.updateNavigationDisplay(blank, null, USER, Locale.ENGLISH)).getMessage());
    verify(navigationConfigurationService, never()).updateConfiguration(any());
  }

  @Test
  public void defaultModeMustBeAnAllowedMode() {
    administrator();
    stored();

    assertEquals(NavigationDisplayService.ALLOWED_MODES_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, List.of("icon"), null)));
    assertEquals(NavigationDisplayService.DEFAULT_MODE_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, List.of("icon"), "hidden")));
    assertEquals(NavigationDisplayService.DEFAULT_MODE_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, null, "sticky")));
    assertEquals(NavigationDisplayService.ALLOWED_MODES_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, List.of(), null)));
    assertEquals(NavigationDisplayService.ALLOWED_MODES_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, List.of("hidden", "floating"), null)));
    assertEquals(NavigationDisplayService.DEFAULT_MODE_NOT_ACCEPTED,
                 refusal(new SidebarDisplayOptions(null, null, "floating")));
    verify(navigationConfigurationService, never()).updateConfiguration(any());
  }

  @Test
  public void nullNestedFieldsLeaveEveryStoredValue() throws Exception {
    administrator();
    stored();

    service.updateNavigationDisplay(NO_TOPBAR, new SidebarDisplayOptions(null, null, null), USER, Locale.ENGLISH);

    NavigationConfiguration submitted = submitted();
    assertEquals(stored(false).getTopbar(), submitted.getTopbar());
    assertEquals(stored(false).getSidebar(), submitted.getSidebar());
  }

  @Test
  public void describesTheStoredStateAfterTheWrite() throws Exception {
    administrator();
    NavigationConfiguration before = stored(false);
    NavigationConfiguration after = stored(false);
    after.getTopbar().setDisplaySiteName(false);
    after.getSidebar().setAllowedModes(List.of(SidebarMode.ICON, SidebarMode.STICKY));
    after.getSidebar().setDefaultMode(SidebarMode.ICON);
    when(navigationConfigurationService.getConfiguration()).thenReturn(before, after);

    NavigationDisplayDescription result = service.updateNavigationDisplay(new TopbarDisplayOptions(null, false, null, null),
                                                                          null,
                                                                          USER,
                                                                          Locale.ENGLISH);

    assertEquals(new NavigationDisplayDescription(new TopbarDisplayOptions(true,
                                                                           false,
                                                                           false,
                                                                           List.of(new TopbarApplicationOption("search",
                                                                                                               "Search",
                                                                                                               true,
                                                                                                               false),
                                                                                   new TopbarApplicationOption("notifications",
                                                                                                               "Notification",
                                                                                                               true,
                                                                                                               true))),
                                                  new SidebarDisplayOptions(false, List.of("icon", "sticky"), "icon")),
                 result);
  }

  private String refusal(SidebarDisplayOptions sidebar) {
    return assertThrows(IllegalArgumentException.class, () -> service.updateNavigationDisplay(null, sidebar, USER, Locale.ENGLISH)).getMessage();
  }

  /**
   * The settings page's bundle, as the general-settings app loads it:
   * generalSettings.search.name and generalSettings.notifications.name are
   * the keys the stored applications carry
   */
  private void administrator() {
    when(aclService.isAdministrator(USER)).thenReturn(true);
    ResourceBundle bundle = new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[][] { { "generalSettings.search.name", "Search" }, { "generalSettings.notifications.name", "Notification" } };
      }
    };
    lenient().when(resourceBundleService.getResourceBundle(any(String[].class), eq(Locale.ENGLISH))).thenReturn(bundle);
  }

  private NavigationConfiguration stored() {
    return stored(true);
  }

  /**
   * Company name and site name displayed, two applications, the sidebar in
   * every mode with hidden as the default and one item
   */
  private NavigationConfiguration stored(boolean read) {
    TopbarConfiguration topbar = new TopbarConfiguration(true,
                                                         true,
                                                         false,
                                                         new ArrayList<>(List.of(application("search", "generalSettings.search.name", true, false),
                                                                                 application("notifications", "generalSettings.notifications.name",
                                                                                             true,
                                                                                             true))));
    SidebarItem item = new SidebarItem(SidebarItemType.SITE);
    item.setName("home");
    SidebarConfiguration sidebar = new SidebarConfiguration(false,
                                                            SidebarMode.HIDDEN,
                                                            null,
                                                            new ArrayList<>(List.of(SidebarMode.HIDDEN,
                                                                                    SidebarMode.ICON,
                                                                                    SidebarMode.STICKY)),
                                                            new ArrayList<>(List.of(item)));
    NavigationConfiguration configuration = new NavigationConfiguration(topbar, sidebar);
    if (read) {
      when(navigationConfigurationService.getConfiguration()).thenReturn(configuration);
    }
    return configuration;
  }

  private TopbarApplication application(String id, String name, boolean enabled, boolean mobile) {
    TopbarApplication application = new TopbarApplication();
    application.setId(id);
    application.setName(name);
    application.setEnabled(enabled);
    application.setMobile(mobile);
    return application;
  }

  private NavigationConfiguration submitted() {
    ArgumentCaptor<NavigationConfiguration> captor = ArgumentCaptor.forClass(NavigationConfiguration.class);
    verify(navigationConfigurationService).updateConfiguration(captor.capture());
    return captor.getValue();
  }
}
