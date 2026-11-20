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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletDescriptor;
import io.meeds.layout.storage.PortletStorage;

/**
 * The protected preferences of a portlet (EXO-93257): only the portlet
 * itself writes them, so a window a client creates through a layout save
 * gets none of them.
 */
@ExtendWith(MockitoExtension.class)
class PortletServiceTest {

  private static final String PEOPLE_LIST = "social/PeopleList";

  @Mock
  private PortletStorage      portletStorage;

  private PortletService      portletService;

  @BeforeEach
  void setUp() {
    portletService = new PortletService(portletStorage);
  }

  @Test
  void protectedPreferencesAreTheDescriptorsOrNone() {
    PortletDescriptor descriptor = new PortletDescriptor();
    descriptor.setProtectedPreferences(List.of("groupIds"));
    when(portletStorage.getPortletDescriptor(PEOPLE_LIST)).thenReturn(descriptor);

    assertEquals(Set.of("groupIds"), portletService.getProtectedPreferences(PEOPLE_LIST));
    assertEquals(Set.of(), portletService.getProtectedPreferences("social/Unknown"));
    assertEquals(Set.of(), portletService.getProtectedPreferences(null));
    descriptor.setProtectedPreferences(null);
    assertEquals(Set.of(), portletService.getProtectedPreferences(PEOPLE_LIST));
  }

  @Test
  void stripRemovesTheProtectedPreferencesOfEveryWindowToCreateOnly() {
    PortletDescriptor descriptor = new PortletDescriptor();
    descriptor.setProtectedPreferences(List.of("groupIds"));
    lenient().when(portletStorage.getPortletDescriptor(anyString())).thenReturn(null);
    lenient().when(portletStorage.getPortletDescriptor(PEOPLE_LIST)).thenReturn(descriptor);

    Application nested = transientWindow(PEOPLE_LIST);
    Container section = new Container();
    section.setChildren(new ArrayList<>(List.of(nested)));
    Application other = transientWindow("social/Other");
    Application stored = new Application("12");
    PersistentApplicationState storedState = new PersistentApplicationState("12");
    stored.setState(storedState);
    Application empty = new Application();
    empty.setState(new TransientApplicationState(PEOPLE_LIST));

    portletService.stripProtectedPreferences(new ArrayList<ModelObject>(List.of(section, other, stored, empty)));

    Portlet stripped = ((TransientApplicationState) nested.getState()).getContentState();
    assertNull(stripped.getPreference("groupIds"));
    assertEquals("20", stripped.getValue("pageSize"));
    // another portlet keeps a preference of the same name, a stored window its state
    assertEquals("[\"/spaces/hidden\"]", ((TransientApplicationState) other.getState()).getContentState().getValue("groupIds"));
    assertSame(storedState, stored.getState());
    assertNull(((TransientApplicationState) empty.getState()).getContentState());
  }

  @Test
  void stripAcceptsNoChildren() {
    assertDoesNotThrow(() -> portletService.stripProtectedPreferences(null));
    assertDoesNotThrow(() -> portletService.stripProtectedPreferences(new ArrayList<>()));
  }

  private static Application transientWindow(String contentId) {
    Portlet preferences = new Portlet();
    preferences.setValue("pageSize", "20");
    preferences.setValue("groupIds", "[\"/spaces/hidden\"]");
    Application application = new Application();
    application.setState(new TransientApplicationState(contentId, preferences));
    return application;
  }
}
