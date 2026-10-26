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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.manager.IdentityManager;

import org.exoplatform.commons.addons.AddOnService;

/**
 * The draft page a clone writes carries no access permission of its own, so
 * the live page's viewers are refused on it while its editors load, save,
 * publish and re-clone it: the real UserACL decides, as it does for a typed
 * address, the layout REST and the tools.
 */
@SpringBootTest(classes = { PageLayoutService.class, LayoutAclService.class, DraftPageAccessTest.AclConfiguration.class })
@ExtendWith(MockitoExtension.class)
public class DraftPageAccessTest {

  private static final String     USERS        = "*:/platform/users";

  private static final String     CONTRIBUTORS = "*:/platform/web-contributors";

  private static final SiteKey    SITE_KEY     = SiteKey.portal("test");

  private static final PageKey    PAGE_KEY     = PageKey.parse("portal::test::home");

  private static final PageKey    DRAFT_KEY    = PageKey.parse("portal::test::home_draft_editor");

  @MockitoBean
  private LayoutService           layoutService;

  @MockitoBean
  private PageTemplateService     pageTemplateService;

  @MockitoBean
  private UserPortalConfigService userPortalConfigService;

  @MockitoBean
  private AddOnService            addOnService;

  @MockitoBean
  private ContainerLayoutService  containerLayoutService;

  @MockitoBean
  private PortletInstanceService  portletInstanceService;

  @MockitoBean
  private ListenerService         listenerService;

  @MockitoBean
  private NavigationService       navigationService;

  @MockitoBean
  private IdentityManager         identityManager;

  @Autowired
  private UserACL                 userAcl;

  @Autowired
  private PageLayoutService       pageLayoutService;

  @Autowired
  private LayoutAclService        aclService;

  @TestConfiguration
  static class AclConfiguration {
    @Bean
    UserACL userAcl() {
      InitParams params = new InitParams();
      ValueParam groups = new ValueParam();
      groups.setName("portal.administrator.groups");
      groups.setValue("/platform/administrators");
      params.addParameter(groups);
      ValueParam membershipType = new ValueParam();
      membershipType.setName("portal.administrator.mstype");
      membershipType.setValue("manager");
      params.addParameter(membershipType);
      return spy(new UserACL(params));
    }
  }

  @BeforeEach
  public void setUp() {
    doReturn(identity("viewer", "/platform/users", "member")).when(userAcl).getUserIdentity("viewer");
    doReturn(identity("editor", "/platform/web-contributors", "member")).when(userAcl).getUserIdentity("editor");
    doReturn(identity("admin", "/platform/administrators", "manager")).when(userAcl).getUserIdentity("admin");
    when(layoutService.getPage(PAGE_KEY)).thenAnswer(invocation -> livePage());
    PortalConfig site = new PortalConfig("portal", "test");
    site.setAccessPermissions(new String[] { UserACL.EVERYONE });
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
  }

  @Test
  public void draftPageIsOpenedByThePagesEditorsOnlyAndStaysSoOnAReclone() throws Exception {
    assertTrue(aclService.canViewPage(PAGE_KEY, "viewer"));
    assertFalse(aclService.canEditPage(PAGE_KEY, "viewer"));

    assertEquals(DRAFT_KEY, pageLayoutService.clonePage(PAGE_KEY, "editor"));
    ArgumentCaptor<Page> saved = ArgumentCaptor.forClass(Page.class);
    verify(layoutService).save(any(PageContext.class), saved.capture());
    Page draft = saved.getValue();
    assertNull(draft.getAccessPermissions());
    assertEquals(CONTRIBUTORS, draft.getEditPermission());
    when(layoutService.getPage(DRAFT_KEY)).thenReturn(draft);

    assertFalse(aclService.canViewPage(DRAFT_KEY, "viewer"));
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageLayout(DRAFT_KEY, "viewer"));
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.updatePageLayout(DRAFT_KEY.format(), livePage(), false, "viewer"));
    assertTrue(aclService.canViewPage(DRAFT_KEY, "editor"));
    assertTrue(aclService.canViewPage(DRAFT_KEY, "admin"));
    assertNotNull(pageLayoutService.getPageLayout(DRAFT_KEY, "editor"));
    pageLayoutService.updatePageLayout(DRAFT_KEY.format(), livePage(), false, "editor");
    pageLayoutService.updatePageLayout(PAGE_KEY.format(), draft, true, "editor");
    verify(layoutService, times(2)).save(any(Page.class));

    assertEquals(DRAFT_KEY, pageLayoutService.clonePage(PAGE_KEY, "editor"));
    verify(layoutService, times(2)).save(any(PageContext.class), saved.capture());
    assertNull(saved.getValue().getAccessPermissions());
    assertEquals(CONTRIBUTORS, saved.getValue().getEditPermission());
  }

  private Identity identity(String username, String group, String membershipType) {
    return new Identity(username, List.of(new MembershipEntry(group, membershipType)));
  }

  private Page livePage() {
    Page page = new Page("portal", "test", "home");
    page.setTitle("Home");
    page.setAccessPermissions(new String[] { USERS });
    page.setEditPermission(CONTRIBUTORS);
    page.setChildren(new ArrayList<>());
    return page;
  }

}
