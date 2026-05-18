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
package io.meeds.layout.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

@SpringBootTest(classes = { LayoutAclService.class })
@ExtendWith(MockitoExtension.class)
public class LayoutAclServiceTest {

  private static final String  TEST_USER = "testuser";

  private static final SiteKey SITE_KEY  = SiteKey.portal("test");

  private static final PageKey PAGE_KEY  = PageKey.parse("portal::test::test");

  @MockitoBean
  private LayoutService        layoutService;

  @MockitoBean
  private IdentityManager      identityManager;

  @MockitoBean
  private UserACL              userAcl;

  @Autowired
  private LayoutAclService     layoutAclService;

  @Mock
  private PortalConfig         portalConfig;

  @Mock
  private Page                 page;

  @Mock
  private Identity             aclIdentity;

  @BeforeEach
  public void setup() {
    when(userAcl.getUserIdentity(TEST_USER)).thenReturn(aclIdentity);
  }

  @Test
  public void canAddSite() {
    assertFalse(layoutAclService.canAddSite(TEST_USER));
    when(userAcl.hasCreatePortalPermission(aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canAddSite(TEST_USER));
  }

  @Test
  public void canEditSite() {
    assertFalse(layoutAclService.canEditSite(SITE_KEY, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertFalse(layoutAclService.canEditSite(SITE_KEY, TEST_USER));
    when(userAcl.hasEditPermission(portalConfig, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canEditSite(SITE_KEY, TEST_USER));
  }

  @Test
  public void canViewSite() {
    assertFalse(layoutAclService.canViewSite(SITE_KEY, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertFalse(layoutAclService.canViewSite(SITE_KEY, TEST_USER));
    when(userAcl.hasAccessPermission(portalConfig, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canViewSite(SITE_KEY, TEST_USER));
  }

  @Test
  public void canEditNavigation() {
    assertFalse(layoutAclService.canEditNavigation(SITE_KEY, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertFalse(layoutAclService.canEditNavigation(SITE_KEY, TEST_USER));
    when(userAcl.hasEditPermission(portalConfig, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canEditNavigation(SITE_KEY, TEST_USER));
  }

  @Test
  public void canViewNavigation() {
    assertFalse(layoutAclService.canViewNavigation(SITE_KEY, null, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertFalse(layoutAclService.canViewNavigation(SITE_KEY, null, TEST_USER));
    when(userAcl.hasAccessPermission(portalConfig, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canViewNavigation(SITE_KEY, null, TEST_USER));
  }

  @Test
  public void canViewNavigationWithPage() {
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertFalse(layoutAclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER));
    when(userAcl.hasAccessPermission(page, aclIdentity)).thenReturn(true);
    assertFalse(layoutAclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER));
    when(userAcl.hasAccessPermission(portalConfig, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER));
  }

  @Test
  public void canViewPage() {
    assertFalse(layoutAclService.canViewPage(PAGE_KEY, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertFalse(layoutAclService.canViewPage(PAGE_KEY, TEST_USER));
    when(userAcl.hasAccessPermission(page, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canViewPage(PAGE_KEY, TEST_USER));
  }

  @Test
  public void canEditPage() {
    assertFalse(layoutAclService.canEditPage(PAGE_KEY, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertFalse(layoutAclService.canEditPage(PAGE_KEY, TEST_USER));
    when(userAcl.hasEditPermission(page, aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.canEditPage(PAGE_KEY, TEST_USER));
  }

  @Test
  public void isAdministrator() {
    assertFalse(layoutAclService.isAdministrator(TEST_USER));
    when(userAcl.isAdministrator(aclIdentity)).thenReturn(true);
    assertTrue(layoutAclService.isAdministrator(TEST_USER));
  }

}
