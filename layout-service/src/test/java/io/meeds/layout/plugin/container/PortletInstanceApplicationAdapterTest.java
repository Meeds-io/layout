/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.layout.plugin.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Properties;

import io.meeds.layout.service.PortletInstanceService;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest(classes = {
                            PortletInstanceApplicationAdapter.class,
})
@ExtendWith(MockitoExtension.class)
public class PortletInstanceApplicationAdapterTest {

  private static final String               USERNAME = "username";

  @MockitoBean
  private PortletInstanceService            portletInstanceService;

  @Mock
  private Application              application;

  @Autowired
  private PortletInstanceApplicationAdapter portletInstanceApplicationAdapter;

  @Test
  void testNullApplication() {
    assertNull(portletInstanceApplicationAdapter.getCssStyle());
    assertNull(portletInstanceApplicationAdapter.getWidth());
    assertNull(portletInstanceApplicationAdapter.getHeight());
    assertNotNull(portletInstanceApplicationAdapter.getId());
    assertNotNull(portletInstanceApplicationAdapter.getState());
    assertFalse(portletInstanceApplicationAdapter.getShowInfoBar());
    assertFalse(portletInstanceApplicationAdapter.getShowApplicationState());
    assertFalse(portletInstanceApplicationAdapter.getShowApplicationMode());
    assertNull(portletInstanceApplicationAdapter.getIcon());
    assertNull(portletInstanceApplicationAdapter.getDescription());
    assertNull(portletInstanceApplicationAdapter.getTitle());
    assertNull(portletInstanceApplicationAdapter.getProperties());
    assertNull(portletInstanceApplicationAdapter.getTheme());
    assertNull(portletInstanceApplicationAdapter.getCssClass());
    assertNull(portletInstanceApplicationAdapter.build());
    assertNull(portletInstanceApplicationAdapter.getStorageName());
    assertNull(portletInstanceApplicationAdapter.getApplication());
  }

  @Test
  void testNoThrowWhenNoApplication() {
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setCssStyle(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setWidth(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setHeight(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setId(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setState(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setShowInfoBar(false));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setShowApplicationState(false));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setShowApplicationMode(false));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setIcon(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setDescription(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setTitle(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setProperties(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setTheme(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setCssClass(null));
    assertDoesNotThrow(() -> portletInstanceApplicationAdapter.setStorageName(null));
  }

  @Test
  void testApplicationDelegation() throws IllegalAccessException, ObjectNotFoundException {
    int portletInstanceId = 2;
    when(portletInstanceService.getPortletInstanceApplication(portletInstanceId, 0, USERNAME)).thenReturn(application);

    try (MockedStatic<PortalRequestContext> portalRequestContextStaticMock = mockStatic(PortalRequestContext.class)) {
      HttpServletRequest request = mock(HttpServletRequest.class);
      PortalRequestContext portalRequestContext = mock(PortalRequestContext.class);
      portalRequestContextStaticMock.when(PortalRequestContext::getCurrentInstance).thenReturn(portalRequestContext);
      when(portalRequestContext.getRequest()).thenReturn(request);
      when(request.getParameter("portletInstanceId")).thenReturn(String.valueOf(portletInstanceId));
      when(request.getRemoteUser()).thenReturn(USERNAME);

      Application portletApplication = portletInstanceApplicationAdapter.getApplication();
      assertNotNull(portletApplication);

      int i = 0;
      ModelStyle cssStyle = mock(ModelStyle.class);
      ApplicationState state = mock(ApplicationState.class);
      Properties properties = mock(Properties.class);
      when(portletApplication.getCssStyle()).thenReturn(cssStyle);
      when(portletApplication.getWidth()).thenReturn(String.valueOf(++i));
      when(portletApplication.getHeight()).thenReturn(String.valueOf(++i));
      when(portletApplication.getId()).thenReturn(String.valueOf(++i));
      when(portletApplication.getState()).thenReturn(state);
      when(portletApplication.getShowInfoBar()).thenReturn(true);
      when(portletApplication.getShowApplicationState()).thenReturn(true);
      when(portletApplication.getShowApplicationMode()).thenReturn(true);
      when(portletApplication.getIcon()).thenReturn(String.valueOf(++i));
      when(portletApplication.getDescription()).thenReturn(String.valueOf(++i));
      when(portletApplication.getTitle()).thenReturn(String.valueOf(++i));
      when(portletApplication.getProperties()).thenReturn(properties);
      when(portletApplication.getTheme()).thenReturn(String.valueOf(++i));
      when(portletApplication.getCssClass()).thenReturn(String.valueOf(++i));
      when(portletApplication.getStorageName()).thenReturn(String.valueOf(++i));

      i = 0;
      assertEquals(cssStyle, portletInstanceApplicationAdapter.getCssStyle());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getWidth());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getHeight());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getId());
      assertEquals(state, portletInstanceApplicationAdapter.getState());
      assertTrue(portletInstanceApplicationAdapter.getShowInfoBar());
      assertTrue(portletInstanceApplicationAdapter.getShowApplicationState());
      assertTrue(portletInstanceApplicationAdapter.getShowApplicationMode());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getIcon());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getDescription());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getTitle());
      assertEquals(properties, portletInstanceApplicationAdapter.getProperties());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getTheme());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getCssClass());
      assertEquals(String.valueOf(++i), portletInstanceApplicationAdapter.getStorageName());
    } finally {
      portletInstanceApplicationAdapter.cleanApplication();
    }
  }

}
