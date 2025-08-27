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
package io.meeds.layout.plugin.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
  PortletInstanceTranslationPlugin.class,
})
@ExtendWith(MockitoExtension.class)
public class PortletInstanceTranslationPluginTest {

  @MockBean
  private LayoutAclService                 layoutAclService;

  @MockBean
  private TranslationService               translationService;

  @MockBean
  private PortletInstanceService           portletInstanceService;

  @Autowired
  private PortletInstanceTranslationPlugin translationPlugin;

  private String                           username = "test";

  @Test
  void getObjectType() {
    assertEquals("portletInstance", translationPlugin.getObjectType());
    assertEquals(PortletInstanceTranslationPlugin.OBJECT_TYPE, translationPlugin.getObjectType());
  }

  @Test
  @SneakyThrows
  void hasEditPermission() {
    assertFalse(translationPlugin.hasEditPermission("0", null));
    assertFalse(translationPlugin.hasEditPermission("0", username));
    when(layoutAclService.isAdministrator(username)).thenReturn(true);
    assertTrue(translationPlugin.hasEditPermission("0", username));
  }

  @Test
  @SneakyThrows
  void hasAccessPermission() {
    assertThrows(ObjectNotFoundException.class, () -> translationPlugin.hasAccessPermission("1", null));
    PortletInstance portletInstance = mock(PortletInstance.class);
    when(portletInstanceService.getPortletInstance(1)).thenReturn(portletInstance);
    assertTrue(translationPlugin.hasAccessPermission("1", null));
    assertTrue(translationPlugin.hasAccessPermission("1", username));

    String permissionExpression = "A Permission Expression";
    when(portletInstance.getPermissions()).thenReturn(Collections.singletonList(permissionExpression));
    assertFalse(translationPlugin.hasAccessPermission("1", username));

    when(layoutAclService.hasPermission(username, permissionExpression)).thenReturn(true);
    assertTrue(translationPlugin.hasAccessPermission("1", username));
  }

  @Test
  @SneakyThrows
  void getAudienceId() {
    assertEquals(0l, translationPlugin.getAudienceId("0"));
  }

  @Test
  @SneakyThrows
  void getSpaceId() {
    assertEquals(0l, translationPlugin.getSpaceId("0"));
  }

}
