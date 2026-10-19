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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.NavigationItemType;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.SiteLayoutService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { LayoutMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
public class LayoutMcpToolTest {

  private static final String      USER       = "john";

  private static final long        SITE_ID    = 5L;

  private static final SiteKey     SITE_KEY   = SiteKey.portal("test");

  private static final PageKey     PAGE_KEY   = PageKey.parse("portal::test::home");

  private static final PageKey     DRAFT_KEY  = PageKey.parse("portal::test::home_draft_john");

  private static final long        LIVE_NODE  = 11L;

  @MockitoBean
  private LayoutService            layoutService;

  @MockitoBean
  private NavigationService        navigationService;

  @MockitoBean
  private LayoutAclService         aclService;

  @MockitoBean
  private SiteLayoutService        siteLayoutService;

  @MockitoBean
  private PageLayoutService        pageLayoutService;

  @MockitoBean
  private NavigationLayoutService  navigationLayoutService;

  @MockitoBean
  private PortletInstanceService   portletInstanceService;

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
  public void listSitesKeepsTheSitesTheUserCanView() {
    PortalConfig visible = site("visible", "Visible");
    PortalConfig forbidden = site("forbidden", null);
    when(layoutService.getSites(argThat(f -> f != null && f.getSiteType() == SiteType.PORTAL))).thenReturn(List.of(visible,
                                                                                                                  forbidden));
    when(layoutService.getSites(argThat(f -> f != null && f.getSiteType() == SiteType.GROUP))).thenReturn(List.of());
    when(aclService.canViewSite(visible.getSiteKey(), USER)).thenReturn(true);
    when(aclService.canViewSite(forbidden.getSiteKey(), USER)).thenReturn(false);

    List<SiteSummary> sites = asUser(tool::listSites);

    assertEquals(1, sites.size());
    assertEquals("visible", sites.get(0).name());
    assertEquals("Visible", sites.get(0).label());
    assertEquals("portal", sites.get(0).type());
    verify(layoutService).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.PORTAL && f.isSortByDisplayOrder()));
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
  public void getSiteReadsTheUserDraftWhenTheRevisionNamesOne() throws Exception {
    PortalConfig site = site("test", "Test");
    Container liveLayout = new Container("live");
    site.setPortalLayout(liveLayout);
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    Container draftLayout = new Container("draft");
    draftSite.setPortalLayout(draftLayout);
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(label("Site label"));
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(label("Site description"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(layoutService.getPortalConfig(draftSite.getSiteKey())).thenReturn(draftSite);
    when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    when(descriptionService.toSiteDescription(site, draftSite.getPortalLayout(), "Site label", "Site description", true)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
    verify(descriptionService).filterAccessible(draftSite.getPortalLayout(), USER);
  }

  @Test
  public void getSiteReadsThePublishedLayoutWithoutADraft() throws Exception {
    PortalConfig site = site("test", null);
    site.setPortalLayout(new Container("live"));
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(new NodeLabel());
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(null);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    when(descriptionService.filterAccessible(site.getPortalLayout(), USER)).thenReturn(site.getPortalLayout());
    when(descriptionService.toSiteDescription(site, site.getPortalLayout(), "test", null, false)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
  }

  @Test
  public void listSitePagesBuildsTheVisibleTree() throws Exception {
    PortalConfig site = site("test", null);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(false);
    PageKey homeKey = PageKey.parse("portal::test::home");
    PageKey linkKey = PageKey.parse("portal::test::link");
    PageKey secretKey = PageKey.parse("portal::test::secret");
    PageKey childKey = PageKey.parse("portal::test::child");
    NodeContext<NodeContext<Object>> root = node(0L, "default", null, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> home = node(1L, "home", homeKey, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> child = node(2L, "child", childKey, Visibility.TEMPORAL);
    NodeContext<NodeContext<Object>> link = node(3L, "link", linkKey, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> hidden = node(4L, "hidden", homeKey, Visibility.HIDDEN);
    NodeContext<NodeContext<Object>> draft = node(5L, "home_draft_john", homeKey, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> secret = node(6L, "secret", secretKey, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> group = node(7L, "group", null, Visibility.DISPLAYED);
    children(root, home, link, hidden, draft, secret, group);
    children(home, child);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(draftService.isDraftNode(any())).thenAnswer(i -> i.getArgument(0, String.class).contains("_draft_"));
    when(aclService.canViewNavigation(eq(SITE_KEY), any(), eq(USER))).thenReturn(true);
    when(aclService.canViewNavigation(SITE_KEY, secretKey, USER)).thenReturn(false);
    when(navigationLayoutService.getNodeLabels(anyLong(), eq(USER))).thenReturn(label("Label"));
    when(navigationLayoutService.getNodeLabels(1L, USER)).thenReturn(label("Home"));
    PageContext homeContext = new PageContext(homeKey, pageState("PAGE", null, "*:/platform/users", "Everyone"));
    PageContext linkContext = new PageContext(linkKey, pageState("LINK", "https://meeds.io", "*:/platform/administrators"));
    when(layoutService.getPageContext(homeKey)).thenReturn(homeContext);
    when(layoutService.getPageContext(linkKey)).thenReturn(linkContext);
    when(layoutService.getPageContext(childKey)).thenReturn(null);

    List<NavigationItemDescription> items = asUser(() -> tool.listSitePages(SITE_ID, null));

    assertEquals(3, items.size());
    NavigationItemDescription homeItem = items.get(0);
    assertEquals(1L, homeItem.nodeId());
    assertEquals("Home", homeItem.name());
    assertEquals(Map.of("en", "Home"), homeItem.labels());
    assertEquals("home", homeItem.urlName());
    assertEquals(NavigationItemType.PAGE, homeItem.type());
    assertEquals("portal::test::home", homeItem.pageRef());
    assertTrue(homeItem.visible());
    assertNull(homeItem.scheduled());
    assertEquals(List.of("Everyone"), homeItem.accessPermissions());
    assertEquals("*:/platform/users", homeItem.editPermission());
    assertEquals(1, homeItem.children().size());
    NavigationItemDescription childItem = homeItem.children().get(0);
    assertEquals(NavigationItemType.PAGE, childItem.type());
    assertEquals("1970-01-01T00:00:01Z", childItem.scheduled().start());
    assertEquals("1970-01-01T00:00:02Z", childItem.scheduled().end());
    assertNull(childItem.accessPermissions());
    NavigationItemDescription linkItem = items.get(1);
    assertEquals(NavigationItemType.LINK, linkItem.type());
    assertEquals("https://meeds.io", linkItem.url());
    assertEquals("_blank", linkItem.target());
    NavigationItemDescription groupItem = items.get(2);
    assertEquals(NavigationItemType.GROUP, groupItem.type());
    assertNull(groupItem.pageRef());
    assertTrue(groupItem.children().isEmpty());
  }

  @Test
  public void hiddenPagesAreListedToNavigationEditorsOnly() throws Exception {
    PortalConfig site = site("test", null);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    NodeContext<NodeContext<Object>> root = node(0L, "default", null, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> hidden = node(4L, "hidden", PAGE_KEY, Visibility.HIDDEN);
    NodeContext<NodeContext<Object>> system = node(8L, "system", PAGE_KEY, Visibility.SYSTEM);
    children(root, hidden, system);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(aclService.canViewNavigation(eq(SITE_KEY), any(), eq(USER))).thenReturn(true);
    lenient().when(navigationLayoutService.getNodeLabels(anyLong(), eq(USER))).thenReturn(label("Label"));
    PageContext pageContext = new PageContext(PAGE_KEY, pageState("PAGE", null, null));
    lenient().when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);

    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(false);
    assertTrue(asUser(() -> tool.listSitePages(SITE_ID, true)).isEmpty());

    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(true);
    assertTrue(asUser(() -> tool.listSitePages(SITE_ID, false)).isEmpty());
    List<NavigationItemDescription> items = asUser(() -> tool.listSitePages(SITE_ID, true));
    assertEquals(2, items.size());
    items.forEach(item -> assertTrue(!item.visible()));
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
  public void listLayoutApplicationsFiltersByKeywordAndNamesTheCategory() {
    PortletInstanceCategory category = new PortletInstanceCategory();
    category.setId(1L);
    category.setName("Content");
    PortletInstance news = instance(10L, 1L, "News list", "Latest news", false);
    PortletInstance disabled = instance(11L, 1L, "News archive", null, true);
    PortletInstance links = instance(12L, 2L, "Links", "Useful links", false);
    try (MockedStatic<LocaleContextInfoUtils> locales = mockStatic(LocaleContextInfoUtils.class)) {
      locales.when(() -> LocaleContextInfoUtils.getUserLocale(USER)).thenReturn(Locale.FRENCH);
      when(portletInstanceService.getPortletInstanceCategories(USER, Locale.FRENCH, true)).thenReturn(List.of(category));
      when(portletInstanceService.getPortletInstances(0, USER, Locale.FRENCH, true)).thenReturn(List.of(news, disabled, links));

      List<LayoutApplicationEntry> all = asUser(() -> tool.listLayoutApplications(null, null));
      assertEquals(2, all.size());
      assertEquals("Content", all.get(0).category());
      assertNull(all.get(1).category());
      assertTrue(!all.get(0).configurable());

      List<LayoutApplicationEntry> filtered = asUser(() -> tool.listLayoutApplications(null, " NEWS "));
      assertEquals(1, filtered.size());
      assertEquals(10L, filtered.get(0).instanceId());
      assertEquals("content/NewsListView", filtered.get(0).contentId());

      when(portletInstanceService.getPortletInstances(2L, USER, Locale.FRENCH, true)).thenReturn(List.of(links));
      assertEquals(12L, asUser(() -> tool.listLayoutApplications(2L, null)).get(0).instanceId());
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

  private PortalConfig site(String name, String label) {
    PortalConfig site = new PortalConfig("portal", name, "portal_" + SITE_ID);
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

  private PageState pageState(String type, String link, String editPermission, String... accessPermissions) {
    PageState state = mock(PageState.class);
    lenient().when(state.getType()).thenReturn(type);
    lenient().when(state.getLink()).thenReturn(link);
    lenient().when(state.getEditPermission()).thenReturn(editPermission);
    lenient().when(state.getAccessPermissions()).thenReturn(accessPermissions.length == 0 ? null : List.of(accessPermissions));
    return state;
  }

  private PortletInstance instance(long id, long categoryId, String name, String description, boolean disabled) {
    PortletInstance instance = new PortletInstance();
    instance.setId(id);
    instance.setCategoryId(categoryId);
    instance.setName(name);
    instance.setDescription(description);
    instance.setDisabled(disabled);
    instance.setContentId(id == 10L ? "content/NewsListView" : "layout/Links");
    return instance;
  }

  @SuppressWarnings("unchecked")
  private NodeContext<NodeContext<Object>> node(long id, String name, PageKey pageRef, Visibility visibility) {
    NodeContext<NodeContext<Object>> node = mock(NodeContext.class);
    lenient().when(node.getId()).thenReturn(String.valueOf(id));
    lenient().when(node.getName()).thenReturn(name);
    NodeState.Builder builder = new NodeState.Builder().pageRef(pageRef).visibility(visibility).label("Label").target("_blank");
    if (visibility == Visibility.TEMPORAL) {
      builder.startPublicationTime(1000L).endPublicationTime(2000L);
    }
    lenient().when(node.getState()).thenReturn(builder.build());
    lenient().when(node.getNodeCount()).thenReturn(0);
    return node;
  }

  @SafeVarargs
  private void children(NodeContext<NodeContext<Object>> parent, NodeContext<NodeContext<Object>>... children) {
    when(parent.getNodeCount()).thenReturn(children.length);
    for (int i = 0; i < children.length; i++) {
      when(parent.get(i)).thenReturn(children[i]);
    }
  }
}
