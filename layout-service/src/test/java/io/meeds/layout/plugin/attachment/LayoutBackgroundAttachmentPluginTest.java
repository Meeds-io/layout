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
package io.meeds.layout.plugin.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.service.LayoutAclService;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
  LayoutBackgroundAttachmentPlugin.class,
})
@ExtendWith(MockitoExtension.class)
public class LayoutBackgroundAttachmentPluginTest {

  private static final String       PAGE_ENTITY_ID = "page_1";

  private static final String       SITE_ENTITY_ID = "site_2";

  @Mock
  private Identity                  userIdentity;

  @Mock
  private Page                      page;

  @Mock
  private PageKey                   pageKey;

  @Mock
  private PortalConfig              portalConfig;

  @MockBean
  private AttachmentService         attachmentService;

  @MockBean
  private LayoutAclService          layoutAclService;

  @MockBean
  private LayoutService             layoutService;

  @Autowired
  private LayoutBackgroundAttachmentPlugin attachmentPlugin;

  @Test
  void getObjectType() {
    assertEquals("containerBackground", attachmentPlugin.getObjectType());
    assertEquals(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE, attachmentPlugin.getObjectType());
  }

  @Test
  @SneakyThrows
  void hasEditPermissionOnPage() {
    when(userIdentity.getUserId()).thenReturn("test");
    when(layoutService.getPage(1l)).thenReturn(page);
    when(page.getPageKey()).thenReturn(pageKey);

    assertFalse(attachmentPlugin.hasEditPermission(userIdentity, PAGE_ENTITY_ID));

    when(layoutAclService.canEditPage(pageKey, "test")).thenReturn(true);
    assertTrue(attachmentPlugin.hasEditPermission(userIdentity, PAGE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasEditPermissionOnPageNotFound() {
    when(layoutService.getPage(1l)).thenReturn(null);

    assertFalse(attachmentPlugin.hasEditPermission(userIdentity, PAGE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasEditPermissionOnSite() {
    when(userIdentity.getUserId()).thenReturn("test");
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn(SiteType.PORTAL.getName());
    when(portalConfig.getName()).thenReturn("site2");

    assertFalse(attachmentPlugin.hasEditPermission(userIdentity, SITE_ENTITY_ID));

    when(layoutAclService.canEditSite(new SiteKey(SiteType.PORTAL.getName(), "site2"), "test")).thenReturn(true);
    assertTrue(attachmentPlugin.hasEditPermission(userIdentity, SITE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasAccessPermissionOnPage() {
    when(layoutService.getPage(1l)).thenReturn(page);
    when(page.getPageKey()).thenReturn(pageKey);
    when(pageKey.getSite()).thenReturn(new SiteKey(SiteType.PORTAL.getName(), "site2"));

    assertFalse(attachmentPlugin.hasAccessPermission(userIdentity, PAGE_ENTITY_ID));

    when(layoutAclService.canViewPage(pageKey, null)).thenReturn(true);
    assertTrue(attachmentPlugin.hasAccessPermission(userIdentity, PAGE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasAccessPermissionOnPageNotFound() {
    when(layoutService.getPage(1l)).thenReturn(null);

    assertFalse(attachmentPlugin.hasAccessPermission(userIdentity, PAGE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasAccessPermissionOnSite() {
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn(SiteType.PORTAL.getName());
    when(portalConfig.getName()).thenReturn("site2");

    assertFalse(attachmentPlugin.hasAccessPermission(userIdentity, SITE_ENTITY_ID));

    when(layoutAclService.canViewSite(new SiteKey(SiteType.PORTAL.getName(), "site2"), null)).thenReturn(true);
    assertTrue(attachmentPlugin.hasAccessPermission(userIdentity, SITE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void hasAccessPermissionOnTemplateSite() {
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn(SiteType.PORTAL_TEMPLATE.getName());
    when(portalConfig.getName()).thenReturn("site2");

    assertTrue(attachmentPlugin.hasAccessPermission(userIdentity, SITE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void getAudienceId() {
    assertEquals(0l, attachmentPlugin.getAudienceId(PAGE_ENTITY_ID));
  }

  @Test
  @SneakyThrows
  void getSpaceId() {
    assertEquals(0l, attachmentPlugin.getSpaceId(PAGE_ENTITY_ID));
  }

}
