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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { LayoutMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
public class LayoutMcpToolTest {

  private static final String      USER      = "john";

  private static final long        SITE_ID   = 5L;

  private static final SiteKey     SITE_KEY  = SiteKey.portal("test");

  private static final PageKey     PAGE_KEY  = PageKey.parse("portal::test::home");

  private static final PageKey     DRAFT_KEY = PageKey.parse("portal::test::home_draft_john");

  private static final long        LIVE_NODE = 11L;

  @MockitoBean
  private LayoutService            layoutService;

  @MockitoBean
  private LayoutAclService         aclService;

  @MockitoBean
  private SiteLayoutService        siteLayoutService;

  @MockitoBean
  private PageLayoutService        pageLayoutService;

  @MockitoBean
  private LayoutDescriptionService descriptionService;

  @MockitoBean
  private LayoutDraftService       draftService;

  @Autowired
  private LayoutMcpTool            tool;

  private ConversationState        conversationState;

  @BeforeEach
  public void setUp() {
    Identity identity = mock(Identity.class);
    lenient().when(identity.getUserId()).thenReturn(USER);
    conversationState = mock(ConversationState.class);
    lenient().when(conversationState.getIdentity()).thenReturn(identity);
  }

  @Test
  public void listSitesIsDescribedAsTheCurrentUser() {
    List<SiteSummary> expected = List.of(new SiteSummary(1L, "portal", "test", "Test", null, true));
    when(descriptionService.describeSites(USER)).thenReturn(expected);

    assertEquals(expected, asUser(tool::listSites));
  }

  @Test
  public void siteRefusalsCarryTheFixedTexts() throws Exception {
    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    when(siteLayoutService.getSite(2L, USER)).thenThrow(new IllegalAccessException("internal detail"));

    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getSite(1L))).getMessage());
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, assertThrows(IllegalAccessException.class, () -> asUser(() -> tool.getSite(2L))).getMessage());
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.listSitePages(1L, null))).getMessage());
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getSite(null))).getMessage());
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.listSitePages(0L, true))).getMessage());
  }

  @Test
  public void getSiteReadsTheUserDraftUnderTheOwnerRule() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    site.setPortalLayout(new Container("live"));
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(label("Site label"));
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(label("Site description"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    when(descriptionService.toSiteDescription(site, draftSite.getPortalLayout(), "Site label", "Site description", true)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
    verify(descriptionService).filterAccessible(draftSite.getPortalLayout(), USER);
    verify(layoutService, never()).getPortalConfig(any(SiteKey.class));
  }

  @Test
  public void draftSiteTheUserMayNotReadIsNotADraft() throws Exception {
    PortalConfig site = site("portal", "test", null);
    site.setPortalLayout(new Container("live"));
    SiteKey foreignDraft = SiteKey.draft("portal_test_draft_bob");
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(new NodeLabel());
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(null);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(foreignDraft);
    when(siteLayoutService.getSite(foreignDraft, USER)).thenThrow(new IllegalAccessException("owner only"));
    when(descriptionService.filterAccessible(site.getPortalLayout(), USER)).thenReturn(site.getPortalLayout());
    when(descriptionService.toSiteDescription(site, site.getPortalLayout(), "test", null, false)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
  }

  @Test
  public void groupSiteLabelsAreReadFromTheSiteItself() throws Exception {
    PortalConfig site = site("group", "/spaces/marketing", "Marketing");
    site.setDescription("The marketing space");
    site.setPortalLayout(new Container("live"));
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    when(descriptionService.filterAccessible(site.getPortalLayout(), USER)).thenReturn(site.getPortalLayout());
    when(descriptionService.toSiteDescription(site, site.getPortalLayout(), "Marketing", "The marketing space", false)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
    verify(siteLayoutService, never()).getSiteLabels(anyLong(), any());
    verify(siteLayoutService, never()).getSiteDescriptions(anyLong(), any());
  }

  @Test
  public void listSitePagesIsDescribedForTheSiteKey() throws Exception {
    PortalConfig site = site("portal", "test", null);
    List<NavigationItemDescription> expected = List.of();
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(descriptionService.describeNavigation(SITE_KEY, true, USER)).thenReturn(expected);
    when(descriptionService.describeNavigation(SITE_KEY, false, USER)).thenReturn(List.of(mock(NavigationItemDescription.class)));

    assertEquals(expected, asUser(() -> tool.listSitePages(SITE_ID, true)));
    assertEquals(1, asUser(() -> tool.listSitePages(SITE_ID, null)).size());
    assertEquals(1, asUser(() -> tool.listSitePages(SITE_ID, false)).size());
  }

  @Test
  public void pageRefusalsCarryTheFixedTexts() throws Exception {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(null);
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null))).getMessage());

    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null))).getMessage());

    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout(" ", null))).getMessage());
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout("not-a-ref", null))).getMessage());
    assertEquals("value is not accepted for version",
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "live"))).getMessage());
    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class), any());
  }

  @Test
  public void draftPageAddressedByItsOwnRefDoesNotExist() throws Exception {
    when(draftService.isDraftNode("home_draft_bob")).thenReturn(true);

    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> asUser(() -> tool.getPageLayout("portal::test::home_draft_bob", null))).getMessage());
    verify(layoutService, never()).getPage(any(PageKey.class));
  }

  @Test
  public void draftIsReadByDefaultWhenTheRevisionNamesOne() throws Exception {
    viewablePage();
    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    ModelObject tree = new Container("draft");
    PageDescription expected = mock(PageDescription.class);
    when(pageLayoutService.getPageLayout(DRAFT_KEY, USER)).thenReturn(tree);
    when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.DRAFT)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "draft")));
  }

  @Test
  public void publishedIsReadWhenAskedForOrWhenNoRevisionNamesADraft() throws Exception {
    viewablePage();
    ModelObject tree = new Container("live");
    PageDescription expected = mock(PageDescription.class);
    when(pageLayoutService.getPageLayout(PAGE_KEY, USER)).thenReturn(tree);
    when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.PUBLISHED)).thenReturn(expected);

    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "published")));

    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));

    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));
    verify(pageLayoutService, never()).getPageLayout(DRAFT_KEY, USER);
  }

  @Test
  public void explicitDraftWithoutARevisionDoesNotExist() throws Exception {
    viewablePage();
    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);

    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "draft"))).getMessage());
    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class), any());
  }

  @Test
  public void listLayoutApplicationsIsDescribedInTheUserLanguage() {
    List<LayoutApplicationEntry> expected = List.of(new LayoutApplicationEntry(10L, "content/NewsListView", "News", null, "Content", false, null));
    try (MockedStatic<LocaleContextInfoUtils> locales = mockStatic(LocaleContextInfoUtils.class)) {
      locales.when(() -> LocaleContextInfoUtils.getUserLocale(USER)).thenReturn(Locale.FRENCH);
      when(descriptionService.describeApplications(2L, "news", USER, Locale.FRENCH)).thenReturn(expected);

      assertEquals(expected, asUser(() -> tool.listLayoutApplications(2L, "news")));
    }
  }

  private void viewablePage() {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(true);
    when(aclService.canViewSite(SITE_KEY, USER)).thenReturn(true);
  }

  @SneakyThrows
  private <T> T asUser(Callable<T> call) {
    try (MockedStatic<ConversationState> conversation = mockStatic(ConversationState.class)) {
      conversation.when(ConversationState::getCurrent).thenReturn(conversationState);
      return call.call();
    }
  }

  private PortalConfig site(String type, String name, String label) {
    PortalConfig site = new PortalConfig(type, name, type + "_" + SITE_ID);
    site.setLabel(label);
    site.setDisplayed(true);
    return site;
  }

  private NodeLabel label(String english) {
    NodeLabel nodeLabel = new NodeLabel();
    nodeLabel.setDefaultLanguage("en");
    nodeLabel.setLabels(Map.of("en", english));
    return nodeLabel;
  }
}
