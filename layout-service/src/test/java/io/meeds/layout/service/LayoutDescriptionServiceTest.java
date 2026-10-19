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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.commons.exception.ObjectNotFoundException;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.NavigationItemType;
import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.BackgroundBlock;
import io.meeds.layout.mcp.model.CellDescription;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.util.EntityMapper;

import lombok.SneakyThrows;

@SpringBootTest(classes = { LayoutDescriptionService.class })
@ExtendWith(MockitoExtension.class)
public class LayoutDescriptionServiceTest {

  private static final String      USER       = "john";

  private static final String      PAGE_REF   = "portal::test::home";

  private static final JsonMapper  JSON       = JsonMapper.builder().build();

  private static final SiteKey     SITE_KEY   = SiteKey.portal("test");

  private static final String      FLEX_CLASS =
                                              "d-flex flex-column d-md-grid layout-sticky-application grid-cols grid-cols-md-12 grid-cols-lg-12 grid-cols-xl-12";

  @MockitoBean
  private LayoutService            layoutService;

  @MockitoBean
  private PortletInstanceService   portletInstanceService;

  @MockitoBean
  private LayoutAclService         aclService;

  @MockitoBean
  private NavigationService        navigationService;

  @MockitoBean
  private NavigationLayoutService  navigationLayoutService;

  @MockitoBean
  private LayoutDraftService       layoutDraftService;

  @Autowired
  private LayoutDescriptionService descriptionService;

  @BeforeEach
  public void setUp() {
    lenient().when(aclService.hasAccessPermission(any(), eq(USER))).thenReturn(true);
    lenient().when(layoutService.getId(any())).thenReturn("layout/Test");
    lenient().when(portletInstanceService.getApplicationPortletInstanceId(anyLong())).thenReturn(0L);
  }

  @Test
  public void shippedFlexSectionReadsAsAThreeColumnDynamicSection() {
    Container section = shippedSection("flexSection");
    assertEquals(FLEX_CLASS, section.getCssClass().trim());
    assertEquals(3, section.getChildren().size());
    section.getChildren().forEach(cell -> {
      assertEquals(LayoutDescriptionService.CELL_TEMPLATE, ((Container) cell).getTemplate());
      assertEquals(4, descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_COLSPAN_PREFIX));
      assertEquals(1, descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_ROWSPAN_PREFIX));
    });

    PageDescription description = descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED);

    assertNull(description.editorCompatible());
    assertEquals(LayoutVersion.PUBLISHED, description.version());
    assertEquals(1, description.sections().size());
    SectionDescription dynamic = description.sections().get(0);
    assertEquals(SectionType.DYNAMIC, dynamic.type());
    assertEquals(section.getStorageId(), dynamic.sectionId());
    assertEquals(3, dynamic.columns());
    assertEquals(3, dynamic.columnsContent().size());
    assertNull(dynamic.cells());
    assertEquals(DisplayMode.BOTH, dynamic.display());
    assertNotNull(dynamic.styling());
    assertFalse(dynamic.styling().margins().enabled());
    assertNull(dynamic.styling().border());
    assertNull(dynamic.styling().height());
  }

  @Test
  public void shippedGridSectionReadsAsATwelveByFiveGrid() {
    Container section = shippedSection("gridSection");

    PageDescription description = descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.DRAFT);

    SectionDescription grid = description.sections().get(0);
    assertEquals(SectionType.GRID, grid.type());
    assertEquals(12, grid.columns());
    assertEquals(5, grid.rows());
    assertNull(grid.columnsContent());
    assertNull(grid.display());
    assertEquals(60, grid.cells().size());
    CellDescription first = grid.cells().get(0);
    assertEquals(1, first.row());
    assertEquals(1, first.column());
    assertNull(first.rowSpan());
    assertNull(first.columnSpan());
    CellDescription last = grid.cells().get(59);
    assertEquals(5, last.row());
    assertEquals(12, last.column());
  }

  @Test
  public void gridCellApplicationHasNoHeightBlock() {
    Container section = container(LayoutDescriptionService.GRID_TEMPLATE,
                                  "d-flex flex-column d-md-grid grid-cols grid-cols-xl-1 grid-rows grid-rows-xl-1");
    Container cell = cell("grid-cell");
    Application application = application("42", null, null);
    application.setHeight("300px");
    children(cell, application);
    children(section, cell);

    ApplicationDescription description = descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED)
                                                            .sections()
                                                            .get(0)
                                                            .cells()
                                                            .get(0)
                                                            .application();

    assertEquals("42", description.applicationId());
    assertNull(description.styling().height());
    assertNotNull(description.styling().border());
    assertEquals(DisplayMode.BOTH, description.display());
  }

  @Test
  public void sitesAreThePortalSitesWithoutAKeyword() {
    PortalConfig visible = new PortalConfig("portal", "visible", "portal_1");
    visible.setLabel("Visible");
    PortalConfig forbidden = new PortalConfig("portal", "forbidden", "portal_2");
    when(layoutService.getSites(argThat((SiteFilter f) -> f != null && f.getSiteType() == SiteType.PORTAL))).thenReturn(List.of(visible, forbidden));
    when(aclService.canViewSite(visible.getSiteKey(), USER)).thenReturn(true);
    when(aclService.canViewSite(forbidden.getSiteKey(), USER)).thenReturn(false);

    List<SiteSummary> sites = descriptionService.describeSites(USER, " ");

    assertEquals(1, sites.size());
    assertEquals("visible", sites.get(0).name());
    assertEquals("Visible", sites.get(0).label());
    assertEquals("portal", sites.get(0).type());
    verify(layoutService).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.PORTAL && f.isSortByDisplayOrder()
                                                            && f.isExcludeSpaceSites()));
    verify(layoutService, never()).getSiteNames(any(), anyInt(), anyInt());
  }

  @Test
  public void keywordAddsTheMatchingGroupSitesWithoutLoadingTheOthers() {
    PortalConfig portal = new PortalConfig("portal", "intranet", "portal_1");
    portal.setLabel("Marketing portal");
    PortalConfig otherPortal = new PortalConfig("portal", "public", "portal_2");
    when(layoutService.getSites(argThat((SiteFilter f) -> f != null && f.getSiteType() == SiteType.PORTAL))).thenReturn(List.of(portal, otherPortal));
    when(layoutService.getSiteNames(SiteType.SPACE, 0, 200)).thenReturn(List.of("/spaces/marketing", "/spaces/sales", "/spaces/marketing_private"));
    when(layoutService.getSiteNames(SiteType.GROUP, 0, 200)).thenReturn(List.of("/spaces/marketing", "/platform/marketing-team", "/platform/users"));
    PortalConfig marketing = new PortalConfig("group", "/spaces/marketing", "group_3");
    PortalConfig marketingPrivate = new PortalConfig("group", "/spaces/marketing_private", "group_4");
    PortalConfig team = new PortalConfig("group", "/platform/marketing-team", "group_5");
    when(layoutService.getPortalConfig(SiteKey.group("/spaces/marketing"))).thenReturn(marketing);
    when(layoutService.getPortalConfig(SiteKey.group("/spaces/marketing_private"))).thenReturn(marketingPrivate);
    when(layoutService.getPortalConfig(SiteKey.group("/platform/marketing-team"))).thenReturn(team);
    when(aclService.canViewSite(portal.getSiteKey(), USER)).thenReturn(true);
    when(aclService.canViewSite(marketing.getSiteKey(), USER)).thenReturn(true);
    when(aclService.canViewSite(marketingPrivate.getSiteKey(), USER)).thenReturn(false);
    when(aclService.canViewSite(team.getSiteKey(), USER)).thenReturn(true);

    List<SiteSummary> sites = descriptionService.describeSites(USER, "MARKET");

    assertEquals(3, sites.size());
    assertEquals("intranet", sites.get(0).name());
    assertEquals("/spaces/marketing", sites.get(1).name());
    assertEquals("group", sites.get(1).type());
    assertEquals("/platform/marketing-team", sites.get(2).name());
    verify(layoutService, never()).getPortalConfig(SiteKey.group("/spaces/sales"));
    verify(layoutService, never()).getPortalConfig(SiteKey.group("/platform/users"));
    verify(layoutService, times(1)).getPortalConfig(SiteKey.group("/spaces/marketing"));
    assertTrue(descriptionService.describeSites(USER, "spaces").stream().noneMatch(site -> "group".equals(site.type())));
    verify(layoutService, never()).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.GROUP));
    verify(layoutService, times(2)).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.PORTAL && !f.isExcludeSpaceSites()));
  }

  @Test
  public void groupSiteLookupsAreCappedAndPagedWhateverTheUserMayView() {
    when(layoutService.getSites(any())).thenReturn(List.of());
    List<String> firstPage = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      firstPage.add("/spaces/team" + i);
    }
    when(layoutService.getSiteNames(SiteType.SPACE, 0, 200)).thenReturn(firstPage);
    when(layoutService.getSiteNames(SiteType.SPACE, 200, 200)).thenReturn(List.of("/spaces/team200"));
    when(layoutService.getSiteNames(SiteType.GROUP, 0, 200)).thenReturn(List.of("/spaces/team0", "/spaces/team1"));
    when(layoutService.getPortalConfig(any(SiteKey.class))).thenAnswer(i -> new PortalConfig("group", i.getArgument(0, SiteKey.class).getName(), "group_1"));
    when(aclService.canViewSite(any(), eq(USER))).thenReturn(true);

    assertEquals(LayoutDescriptionService.SITE_MATCH_LIMIT, descriptionService.describeSites(USER, "team").size());
    verify(layoutService, times(LayoutDescriptionService.SITE_MATCH_LIMIT)).getPortalConfig(any(SiteKey.class));

    when(aclService.canViewSite(any(), eq(USER))).thenReturn(false);
    assertTrue(descriptionService.describeSites(USER, "team").isEmpty());
    verify(layoutService, times(2 * LayoutDescriptionService.SITE_MATCH_LIMIT)).getPortalConfig(any(SiteKey.class));

    when(aclService.canViewSite(any(), eq(USER))).thenReturn(true);
    assertEquals(11, descriptionService.describeSites(USER, "team19").size());
    verify(layoutService).getSiteNames(SiteType.SPACE, 200, 200);
    verify(layoutService).getSiteNames(SiteType.GROUP, 0, 200);
  }

  @Test
  public void navigationIsDescribedAsTheViewerSeesIt() throws Exception {
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
    nodes(root, home, link, hidden, draft, secret, group);
    nodes(home, child);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(layoutDraftService.isDraftNode(any())).thenAnswer(i -> i.getArgument(0, String.class).contains("_draft_"));
    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(false);
    when(aclService.canViewNavigation(eq(SITE_KEY), any(), eq(USER))).thenReturn(true);
    when(aclService.canViewNavigation(SITE_KEY, secretKey, USER)).thenReturn(false);
    when(navigationLayoutService.getNodeLabels(anyLong(), eq(USER))).thenReturn(nodeLabel("Label"));
    when(navigationLayoutService.getNodeLabels(1L, USER)).thenReturn(nodeLabel("Home"));
    when(navigationLayoutService.getNodeLabels(3L, USER)).thenThrow(new ObjectNotFoundException("gone"));
    PageContext homeContext = new PageContext(homeKey, pageState("PAGE", null, "*:/platform/users", "Everyone"));
    PageContext linkContext = new PageContext(linkKey, pageState("LINK", "https://meeds.io", "*:/platform/administrators"));
    when(layoutService.getPageContext(homeKey)).thenReturn(homeContext);
    when(layoutService.getPageContext(linkKey)).thenReturn(linkContext);
    when(layoutService.getPageContext(childKey)).thenReturn(null);

    List<NavigationItemDescription> items = descriptionService.describeNavigation(SITE_KEY, true, USER);

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
    assertEquals("1970-01-01T00:00:01Z", childItem.scheduled().start());
    assertEquals("1970-01-01T00:00:02Z", childItem.scheduled().end());
    assertNull(childItem.accessPermissions());
    NavigationItemDescription linkItem = items.get(1);
    assertEquals(NavigationItemType.LINK, linkItem.type());
    assertEquals("Label", linkItem.name());
    assertNull(linkItem.labels());
    assertEquals("https://meeds.io", linkItem.url());
    assertEquals("_blank", linkItem.target());
    NavigationItemDescription groupItem = items.get(2);
    assertEquals(NavigationItemType.GROUP, groupItem.type());
    assertNull(groupItem.pageRef());
    assertTrue(groupItem.children().isEmpty());
  }

  @Test
  public void hiddenItemsAreDescribedToNavigationEditorsOnly() throws Exception {
    PageKey pageKey = PageKey.parse("portal::test::home");
    NodeContext<NodeContext<Object>> root = node(0L, "default", null, Visibility.DISPLAYED);
    nodes(root, node(4L, "hidden", pageKey, Visibility.HIDDEN), node(8L, "system", pageKey, Visibility.SYSTEM));
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(aclService.canViewNavigation(eq(SITE_KEY), any(), eq(USER))).thenReturn(true);
    lenient().when(navigationLayoutService.getNodeLabels(anyLong(), eq(USER))).thenReturn(nodeLabel("Label"));
    PageContext pageContext = new PageContext(pageKey, pageState("PAGE", null, null));
    lenient().when(layoutService.getPageContext(pageKey)).thenReturn(pageContext);

    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(false);
    assertTrue(descriptionService.describeNavigation(SITE_KEY, true, USER).isEmpty());

    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(true);
    assertTrue(descriptionService.describeNavigation(SITE_KEY, false, USER).isEmpty());
    List<NavigationItemDescription> items = descriptionService.describeNavigation(SITE_KEY, true, USER);
    assertEquals(2, items.size());
    items.forEach(item -> assertFalse(item.visible()));
    assertTrue(descriptionService.describeNavigation(SiteKey.portal("empty"), true, USER).isEmpty());
  }

  @Test
  public void applicationsAreDescribedByKeywordAndCategory() {
    PortletInstanceCategory category = new PortletInstanceCategory();
    category.setId(1L);
    category.setName("Content");
    PortletInstance news = instance(10L, 1L, "News list", "Latest news", false);
    PortletInstance disabled = instance(11L, 1L, "News archive", null, true);
    PortletInstance links = instance(12L, 2L, "Links", "Useful links", false);
    when(portletInstanceService.getPortletInstanceCategories(USER, Locale.FRENCH, true)).thenReturn(List.of(category));
    when(portletInstanceService.getPortletInstances(0, USER, Locale.FRENCH, true)).thenReturn(List.of(news, disabled, links));
    when(portletInstanceService.getPortletInstances(2L, USER, Locale.FRENCH, true)).thenReturn(List.of(links));

    List<LayoutApplicationEntry> all = descriptionService.describeApplications(null, null, USER, Locale.FRENCH);
    assertEquals(2, all.size());
    assertEquals("Content", all.get(0).category());
    assertNull(all.get(1).category());
    assertFalse(all.get(0).configurable());

    List<LayoutApplicationEntry> filtered = descriptionService.describeApplications(null, " NEWS ", USER, Locale.FRENCH);
    assertEquals(1, filtered.size());
    assertEquals(10L, filtered.get(0).instanceId());
    assertEquals("content/NewsListView", filtered.get(0).contentId());

    assertEquals(12L, descriptionService.describeApplications(2L, null, USER, Locale.FRENCH).get(0).instanceId());
  }

  @Test
  public void gridCellsTakeTheFirstFreePositionTheirSpansLeave() {
    Container section = container(LayoutDescriptionService.GRID_TEMPLATE,
                                  "d-flex flex-column d-md-grid grid-cols grid-cols-md-2 grid-cols-lg-2 grid-cols-xl-2 grid-rows grid-rows-md-2 grid-rows-lg-2 grid-rows-xl-2");
    Container wide = cell("grid-cell grid-cell-colspan grid-cell-colspan-xl-2 grid-cell-rowspan grid-cell-rowspan-xl-1");
    Container second = cell("grid-cell grid-cell-colspan grid-cell-colspan-xl-1 grid-cell-rowspan grid-cell-rowspan-xl-1");
    Container third = cell("grid-cell");
    children(section, wide, second, third);

    List<CellDescription> cells = descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED)
                                                    .sections()
                                                    .get(0)
                                                    .cells();

    assertEquals(1, cells.get(0).row());
    assertEquals(1, cells.get(0).column());
    assertEquals(2, cells.get(0).columnSpan());
    assertEquals(2, cells.get(1).row());
    assertEquals(1, cells.get(1).column());
    assertEquals(2, cells.get(2).row());
    assertEquals(2, cells.get(2).column());
  }

  @Test
  public void missingGridTokenReadsAsOne() {
    Container section = container(LayoutDescriptionService.GRID_TEMPLATE, "d-flex");
    children(section, cell("grid-cell"));

    SectionDescription grid = descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED)
                                                .sections()
                                                .get(0);

    assertEquals(1, grid.columns());
    assertEquals(1, grid.rows());
    assertEquals(1, descriptionService.breakpointValue(null, LayoutDescriptionService.GRID_COLUMNS_PREFIX));
  }

  @Test
  public void pageWithoutEditorSectionsIsNotEditorCompatible() {
    Container legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    Container page = new Container();
    children(page, legacy);

    PageDescription description = descriptionService.toPageDescription(page, PAGE_REF, LayoutVersion.PUBLISHED);

    assertEquals(Boolean.FALSE, description.editorCompatible());
    assertTrue(description.sections().isEmpty());

    Container parent = container(EntityMapper.PAGE_LAYOUT_TEMPLATE, null);
    children(parent, legacy);
    Container other = new Container();
    children(other, parent);
    assertEquals(Boolean.FALSE, descriptionService.toPageDescription(other, PAGE_REF, LayoutVersion.PUBLISHED).editorCompatible());
  }

  @Test
  public void legacyMarginTokensReadAsMargins() {
    Application application = application("42", "mt-3 mb-n2", null);

    ApplicationDescription description = firstApplication(application);

    assertTrue(description.styling().margins().enabled());
    assertEquals(32, description.styling().margins().top());
    assertEquals(12, description.styling().margins().bottom());
    assertNull(description.styling().margins().right());
    assertNull(description.styling().margins().all());
  }

  @Test
  public void displayIsReadFromTheHiddenTokens() {
    assertEquals(DisplayMode.HIDE_MOBILE, firstApplication(application("1", "hidden-sm-and-down", null)).display());
    assertEquals(DisplayMode.HIDE_DESKTOP, firstApplication(application("2", "my-class hidden-md-and-up", null)).display());
    assertEquals(DisplayMode.BOTH, firstApplication(application("3", "my-hidden-sm-and-down-ish", null)).display());

    ModelStyle style = new ModelStyle();
    style.setMobileHidden(true);
    assertEquals(DisplayMode.HIDE_MOBILE, firstApplication(application("4", null, style)).display());

    Container section = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS + " hidden-md-and-up");
    children(section, cell("flex-cell"));
    assertEquals(DisplayMode.HIDE_DESKTOP,
                 descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED).sections().get(0).display());
  }

  @Test
  public void applicationCarriesItsIdentifiersAndStyling() {
    ModelStyle style = new ModelStyle();
    style.setBorderColor("#000000");
    style.setBorderSize("2");
    style.setBoxShadow("true");
    style.setRadiusTopLeft(8);
    style.setRadiusTopRight(8);
    style.setRadiusBottomRight(8);
    style.setRadiusBottomLeft(8);
    style.setBackgroundColor("#FFFFFF");
    style.setTextTitleColor("#123456");
    style.setTextTitleFontSize("18px");
    style.setTextTitleFontWeight("bold");
    style.setTextFontStyle("italic");
    style.setIconColor("#ABCDEF");
    Application application = application("42", null, style);
    application.setHeight("300px");
    application.setTitle("News");
    when(portletInstanceService.getApplicationPortletInstanceId(42L)).thenReturn(7L);
    when(layoutService.getId(application.getState())).thenReturn("content/NewsListView");

    ApplicationDescription description = firstApplication(application);

    assertEquals("42", description.applicationId());
    assertEquals("content/NewsListView", description.contentId());
    assertEquals(7L, description.instanceId());
    assertEquals("News", description.title());
    assertFalse(description.configurable());
    assertNull(description.settings());
    assertNull(description.widgetCode());
    assertTrue(description.styling().border().enabled());
    assertEquals(2, description.styling().border().size());
    assertEquals(Boolean.TRUE, description.styling().border().shadow());
    assertEquals(8, description.styling().corners().all());
    assertEquals(BackgroundMode.SOLID, description.styling().background().mode());
    assertEquals("#FFFFFF", description.styling().background().color());
    assertEquals(HeightPreset.MEDIUM, description.styling().height().fixed());
    assertNull(description.styling().height().customPx());
    assertEquals(18, description.styling().text().title().size());
    assertEquals(Boolean.TRUE, description.styling().text().title().bold());
    assertEquals(Boolean.TRUE, description.styling().text().content().italic());
    assertEquals("#ABCDEF", description.styling().text().iconColor());
    assertNull(description.styling().text().subtitle());
  }

  @Test
  public void customHeightReadsItsPixels() {
    Application application = application("42", null, null);
    application.setHeight("420");

    assertEquals(HeightPreset.CUSTOM, firstApplication(application).styling().height().fixed());
    assertEquals(420, firstApplication(application).styling().height().customPx());
    assertFalse(firstApplication(application("43", null, null)).styling().height().enabled());
  }

  @Test
  public void gradientBackgroundsAreReadBack() {
    BackgroundBlock linear = background("linear-gradient(to right, #FF0000FF 0%, #FF0000FF 30%, #0000FFFF 100%)");
    assertEquals(BackgroundMode.LINEAR, linear.mode());
    assertEquals("to_right", linear.direction());
    assertEquals("#FF0000FF", linear.from());
    assertEquals("#0000FFFF", linear.to());
    assertEquals(30, linear.ratio());
    assertNull(linear.color());

    BackgroundBlock vertical = background("linear-gradient(#FF0000FF 0%, #FF0000FF 50%, #0000FFFF 100%)");
    assertEquals("to_bottom", vertical.direction());

    BackgroundBlock radial = background("radial-gradient(#AAAAAAFF 0%, #AAAAAAFF 50%, #BBBBBBFF 100%)");
    assertEquals(BackgroundMode.RADIAL, radial.mode());
    assertNull(radial.direction());

    BackgroundBlock angular = background("conic-gradient(from 90deg at top left, #AAAAAAFF 0deg, #AAAAAAFF 22.50deg, #BBBBBBFF 90deg)");
    assertEquals(BackgroundMode.ANGULAR, angular.mode());
    assertEquals("top_left", angular.direction());
    assertEquals("#AAAAAAFF", angular.from());
    assertEquals("#BBBBBBFF", angular.to());
    assertEquals(25, angular.ratio());

    BackgroundBlock reversed = background("conic-gradient(from 180deg at top right, #BBBBBBFF 0deg, #AAAAAAFF 67.50deg, #AAAAAAFF 90deg)");
    assertEquals(BackgroundMode.ANGULAR, reversed.mode());
    assertEquals("top_right", reversed.direction());
    assertEquals("#AAAAAAFF", reversed.from());
    assertEquals("#BBBBBBFF", reversed.to());
    assertEquals(25, reversed.ratio());

    BackgroundBlock bottomLeft = background("conic-gradient(from 0deg at bottom left, #BBBBBBFF 0deg, #AAAAAAFF 45.00deg, #AAAAAAFF 90deg)");
    assertEquals("bottom_left", bottomLeft.direction());
    assertEquals("#AAAAAAFF", bottomLeft.from());
    assertEquals(50, bottomLeft.ratio());

    BackgroundBlock malformedConic = background("conic-gradient(from 0deg at bottom left, #BBBBBBFF 0%, #AAAAAAFF 100%)");
    assertNull(malformedConic.mode());
    assertEquals("conic-gradient(from 0deg at bottom left, #BBBBBBFF 0%, #AAAAAAFF 100%)", malformedConic.effect());

    BackgroundBlock unknown = background("url(/image.png)");
    assertTrue(unknown.enabled());
    assertNull(unknown.mode());
    assertEquals("url(/image.png)", unknown.effect());
  }

  @Test
  public void imageBackgroundReadsItsUrlAndTokens() {
    ModelStyle style = new ModelStyle();
    style.setBackgroundImage("/portal/rest/v1/social/attachments/containerBackground/page_1_42/9");
    style.setBackgroundSize("cover");
    style.setBackgroundRepeat("no-repeat");
    Application application = application("42", "layout-bg-margin-top-10 layout-bg-radius-tl-4 layout-bg-radius-tr-4", style);

    BackgroundBlock background = firstApplication(application).styling().background();

    assertTrue(background.enabled());
    assertEquals(BackgroundMode.SOLID, background.mode());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/page_1_42/9", background.image().url());
    assertEquals("cover", background.image().size());
    assertEquals("no-repeat", background.image().repeat());
    assertEquals(10, background.image().margins().top());
    assertNull(background.image().margins().left());
    assertEquals(4, background.image().corners().topLeft());
    assertNull(background.image().corners().bottomLeft());
  }

  @Test
  public void filterDropsWhatTheUserCannotAccess() {
    Application visible = application("1", null, null);
    Application hidden = application("2", null, null);
    hidden.setAccessPermissions(new String[] { "*:/platform/administrators" });
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    Container section = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    Container cell = cell("flex-cell");
    children(cell, visible, hidden);
    children(section, cell);
    Container page = page(section);

    ModelObject filtered = descriptionService.filterAccessible(page, USER);

    List<ApplicationDescription> applications = descriptionService.toPageDescription(filtered, PAGE_REF, LayoutVersion.PUBLISHED)
                                                                  .sections()
                                                                  .get(0)
                                                                  .columnsContent()
                                                                  .get(0);
    assertEquals(1, applications.size());
    assertEquals("1", applications.get(0).applicationId());
    assertNull(descriptionService.filterAccessible(hidden, USER));
  }

  @Test
  public void siteLayoutIsDescribedByItsBannersAndSidebars() {
    Container outerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, "hidden-sm-and-down");
    outerLeftCell.setWidth("310");
    children(outerLeftCell, application("1", null, null));
    Container outerLeft = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    children(outerLeft, outerLeftCell);
    Container outerRight = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);

    Container topCell = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    children(topCell, application("2", null, null));
    Container top = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-top-section layout-sticky-section");
    top.setHeight("60");
    ModelStyle topStyle = new ModelStyle();
    topStyle.setBackgroundColor("#111111@#222222");
    top.setCssStyle(topStyle);
    children(top, topCell, container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null));
    Container bottom = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-bottom-section hidden-sm-and-down");

    Container innerRightCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    innerRightCell.setWidth("250px");
    Container innerRight = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, "layout-sticky-section");
    children(innerRight, innerRightCell);
    Container center = container(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, null);
    children(center, container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null), new PageBody(), innerRight);
    Container middle = container(LayoutDescriptionService.SITE_MIDDLE_TEMPLATE, null);
    children(middle, top, center, bottom);
    Container root = container("system:/groovy/portal/webui/container/UISiteLayout.gtmpl", null);
    children(root, outerLeft, middle, outerRight);

    PortalConfig site = new PortalConfig("portal", "test", "portal_5");
    site.setAccessPermissions(new String[] { "Everyone" });
    site.setEditPermission("*:/platform/administrators");
    site.setBannerFileId(3L);
    site.setDisplayed(true);

    SiteDescription description = descriptionService.toSiteDescription(site, root, "Test site", "A site", true);

    assertEquals(5L, description.siteId());
    assertEquals("Test site", description.label());
    assertTrue(description.bannerPresent());
    assertTrue(description.draft());
    assertEquals(List.of("Everyone"), description.accessPermissions());
    assertEquals(Boolean.TRUE, description.leftSidebar().options().fullHeight());
    assertEquals(310, description.leftSidebar().options().width());
    assertEquals(DisplayMode.HIDE_MOBILE, description.leftSidebar().options().display());
    assertFalse(description.leftSidebar().options().sticky());
    assertEquals(outerLeftCell.getStorageId(), description.leftSidebar().containerId());
    assertEquals(1, description.leftSidebar().applications().size());
    assertEquals(Boolean.FALSE, description.rightSidebar().options().fullHeight());
    assertEquals(250, description.rightSidebar().options().width());
    assertTrue(description.rightSidebar().options().sticky());
    assertEquals(1, description.topbars().size());
    assertEquals(60, description.topbars().get(0).options().height());
    assertTrue(description.topbars().get(0).options().sticky());
    assertEquals(DisplayMode.BOTH, description.topbars().get(0).options().display());
    assertEquals(1, description.topbars().get(0).applications().size());
    assertEquals("#111111", description.topbars().get(0).styling().background().color());
    assertEquals("#222222", description.topbars().get(0).styling().background().scrollColor());
    assertNull(description.topbars().get(0).styling().border());
    assertNotNull(description.topbars().get(0).styling().text());
    assertEquals(1, description.bottomBars().size());
    assertEquals(DisplayMode.HIDE_MOBILE, description.bottomBars().get(0).options().display());
  }

  @Test
  public void siteWithoutSidebarsHasNone() {
    Container middle = container(LayoutDescriptionService.SITE_MIDDLE_TEMPLATE, null);
    Container center = container(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, null);
    children(center, new PageBody());
    children(middle, center);
    Container root = container("system:/groovy/portal/webui/container/UISiteLayout.gtmpl", null);
    children(root, middle);

    SiteDescription description = descriptionService.toSiteDescription(new PortalConfig("portal", "test", "portal_5"),
                                                                       root,
                                                                       null,
                                                                       null,
                                                                       false);

    assertNull(description.leftSidebar());
    assertNull(description.rightSidebar());
    assertTrue(description.topbars().isEmpty());
    assertTrue(description.bottomBars().isEmpty());
    assertFalse(description.draft());
  }

  private BackgroundBlock background(String effect) {
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#FFFFFF00");
    style.setBackgroundEffect(effect);
    return firstApplication(application("42", null, style)).styling().background();
  }

  private ApplicationDescription firstApplication(Application application) {
    Container section = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    Container cell = cell("flex-cell");
    children(cell, application);
    children(section, cell);
    return descriptionService.toPageDescription(page(section), PAGE_REF, LayoutVersion.PUBLISHED)
                             .sections()
                             .get(0)
                             .columnsContent()
                             .get(0)
                             .get(0);
  }

  @SneakyThrows
  private Container shippedSection(String nameId) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("section-templates.json")) {
      JsonNode descriptors = JSON.readTree(inputStream).get("descriptors");
      for (JsonNode descriptor : descriptors) {
        if (nameId.equals(descriptor.get("nameId").asString())) {
          LayoutModel model = JSON.readValue(descriptor.get("content").asString(), LayoutModel.class);
          return (Container) EntityMapper.toModelObject(model);
        }
      }
    }
    throw new IllegalStateException("No shipped section template named " + nameId);
  }

  private Container page(Container section) {
    Container parent = container(EntityMapper.PAGE_LAYOUT_TEMPLATE, null);
    children(parent, section);
    Container page = new Container("page");
    children(page, parent);
    return page;
  }

  private Container cell(String cssClass) {
    return container(LayoutDescriptionService.CELL_TEMPLATE, cssClass);
  }

  private Container container(String template, String cssClass) {
    Container container = new Container(String.valueOf(System.nanoTime()));
    container.setTemplate(template);
    container.setCssClass(cssClass);
    container.setChildren(new ArrayList<>());
    return container;
  }

  private Application application(String storageId, String cssClass, ModelStyle style) {
    Application application = new Application(storageId);
    application.setState(new PersistentApplicationState(storageId));
    application.setCssClass(cssClass);
    application.setCssStyle(style);
    return application;
  }

  private void children(Container container, ModelObject... children) {
    container.setChildren(new ArrayList<>(List.of(children)));
  }

  private NodeLabel nodeLabel(String english) {
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
  private void nodes(NodeContext<NodeContext<Object>> parent, NodeContext<NodeContext<Object>>... children) {
    when(parent.getNodeCount()).thenReturn(children.length);
    for (int i = 0; i < children.length; i++) {
      when(parent.get(i)).thenReturn(children[i]);
    }
  }
}
