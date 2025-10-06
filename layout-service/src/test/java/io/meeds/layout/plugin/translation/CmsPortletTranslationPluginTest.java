/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import io.meeds.layout.service.LayoutAclService;
import io.meeds.social.translation.service.TranslationService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
  CmsPortletTranslationPlugin.class,
})
@ExtendWith(MockitoExtension.class)
class CmsPortletTranslationPluginTest {

  @MockBean
  private LayoutAclService                 layoutAclService;

  @MockBean
  private TranslationService               translationService;

  @Autowired
  private CmsPortletTranslationPlugin translationPlugin;

  private String                           username = "test";

  @Test
  void getObjectType() {
    assertEquals("cmsPortlet", translationPlugin.getObjectType());
    assertEquals(CmsPortletTranslationPlugin.OBJECT_TYPE, translationPlugin.getObjectType());
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
