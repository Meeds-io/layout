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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteContainerOptions;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.mcp.model.TextRoleBlock;
import io.meeds.layout.mcp.model.TextBlock;
import io.meeds.layout.mcp.model.MarginsBlock;
import io.meeds.layout.mcp.model.CornersBlock;
import io.meeds.layout.mcp.model.BorderBlock;
import io.meeds.layout.mcp.model.SiteLayoutUpdate;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.junit.jupiter.api.function.Executable;

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
import java.util.Collections;
import io.meeds.layout.service.PageLayoutAssembler;
import org.exoplatform.portal.config.model.Page;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.util.EntityMapper;

import lombok.SneakyThrows;

@SpringBootTest(classes = { LayoutDescriptionService.class, LayoutStylingService.class })
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
  public void pageLayoutWritesTheEditorsTokensForEachDynamicColumnCount() throws Exception {
    Container shippedCell = (Container) shippedSection("flexSection").getChildren().get(0);
    for (int columns : PageLayoutAssembler.DYNAMIC_COLUMNS) {
      SectionDescription section = new SectionDescription(null, SectionType.DYNAMIC, columns, null, null, null, null, null);
      LayoutModel result = toPageLayout(List.of(section), storedPage(), false);
      LayoutModel parent = result.getChildren().get(0);
      assertEquals(EntityMapper.PAGE_LAYOUT_TEMPLATE, parent.getTemplate());
      LayoutModel written = parent.getChildren().get(0);
      assertEquals(LayoutDescriptionService.FLEX_TEMPLATE, written.getTemplate());
      assertEquals(FLEX_CLASS, written.getCssClass());
      assertEquals(columns, written.getChildren().size());
      written.getChildren().forEach(cell -> {
        assertEquals(LayoutDescriptionService.CELL_TEMPLATE, cell.getTemplate());
        assertTrue(cell.getCssClass().startsWith(PageLayoutAssembler.FLEX_CELL_TOKEN + " "), cell.getCssClass());
        assertEquals(12 / columns, descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_COLSPAN_PREFIX));
        assertEquals(1, descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_ROWSPAN_PREFIX));
      });
      if (columns == 3) {
        assertEquals(shippedCell.getCssClass().trim(), written.getChildren().get(0).getCssClass());
      }
      SectionDescription read = descriptionService.toPageDescription(result.toPage(), PAGE_REF, LayoutVersion.DRAFT).sections().get(0);
      assertEquals(SectionType.DYNAMIC, read.type());
      assertEquals(columns, read.columns());
      assertEquals(DisplayMode.BOTH, read.display());
    }
  }

  @Test
  public void pageLayoutWritesTheGridMatrixInReadingOrderAndReadsBackAtTheSamePositions() throws Exception {
    instance(10L, "content/NewsListView", "News");
    PortletInstance linksInstance = instance(11L, "layout/Links", "Links");
    when(portletInstanceService.getPortletInstances(0, USER, Locale.ENGLISH, true)).thenReturn(List.of(linksInstance));
    Container shippedCell = (Container) shippedSection("gridSection").getChildren().get(0);
    ApplicationDescription news = new ApplicationDescription(null, null, 10L, null, null, null, null, null, null);
    ApplicationDescription links = new ApplicationDescription(null, "layout/Links", null, "Links", null, DisplayMode.HIDE_MOBILE, null, null, null);
    SectionDescription grid = new SectionDescription(null,
                                                     SectionType.GRID,
                                                     3,
                                                     2,
                                                     null,
                                                     List.of(new CellDescription(null, 1, 1, null, 2, news),
                                                             new CellDescription(null, 2, 3, null, null, links)),
                                                     null,
                                                     null);
    LayoutModel result = toPageLayout(List.of(grid), storedPage(), false);
    LayoutModel written = result.getChildren().get(0).getChildren().get(0);
    assertEquals(LayoutDescriptionService.GRID_TEMPLATE, written.getTemplate());
    assertEquals("d-flex flex-column d-md-grid grid-cols grid-cols-md-3 grid-cols-lg-3 grid-cols-xl-3 grid-rows grid-rows-md-2 grid-rows-lg-2 grid-rows-xl-2",
                 written.getCssClass());
    assertEquals(5, written.getChildren().size());
    LayoutModel first = written.getChildren().get(0);
    assertEquals("grid-cell grid-cell-colspan grid-cell-colspan-md-2 grid-cell-colspan-lg-2 grid-cell-colspan-xl-2 grid-cell-rowspan grid-cell-rowspan-md-1 grid-cell-rowspan-lg-1 grid-cell-rowspan-xl-1",
                 first.getCssClass());
    assertEquals("content/NewsListView", first.getChildren().get(0).getContentId());
    assertEquals(shippedCell.getCssClass().trim(), written.getChildren().get(1).getCssClass());
    assertTrue(written.getChildren().get(1).getChildren().isEmpty());
    LayoutModel last = written.getChildren().get(4);
    assertEquals("Links", last.getChildren().get(0).getTitle());
    assertTrue(last.getChildren().get(0).getCssClass().contains(LayoutDescriptionService.HIDE_MOBILE_TOKEN));

    SectionDescription read = descriptionService.toPageDescription(result.toPage(), PAGE_REF, LayoutVersion.DRAFT).sections().get(0);
    assertEquals(SectionType.GRID, read.type());
    assertEquals(3, read.columns());
    assertEquals(2, read.rows());
    assertEquals(5, read.cells().size());
    assertEquals(1, read.cells().get(0).row());
    assertEquals(1, read.cells().get(0).column());
    assertEquals(2, read.cells().get(0).columnSpan());
    assertEquals("content/NewsListView", read.cells().get(0).application().contentId());
    assertNull(read.cells().get(1).application());
    assertEquals(1, read.cells().get(1).row());
    assertEquals(3, read.cells().get(1).column());
    assertEquals(2, read.cells().get(4).row());
    assertEquals(3, read.cells().get(4).column());
    assertEquals("Links", read.cells().get(4).application().title());
    assertEquals(DisplayMode.HIDE_MOBILE, read.cells().get(4).application().display());
  }

  @Test
  public void pageLayoutRefusesEachValueOutsideTheGrammar() throws Exception {
    ApplicationDescription news = new ApplicationDescription(null, null, 10L, null, null, null, null, null, null);
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.SECTIONS_NOT_ACCEPTED, Collections.nCopies(21, dynamic(1, null)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.SECTIONS_NOT_ACCEPTED, Collections.singletonList(null));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.ELEMENTS_NOT_ACCEPTED,
                      Collections.nCopies(20, dynamic(1, List.of(Collections.nCopies(10, news)))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.TYPE_NOT_ACCEPTED,
                      List.of(new SectionDescription(null, null, 1, null, null, null, null, null)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.COLUMNS_NOT_ACCEPTED, List.of(dynamic(5, null)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.COLUMNS_NOT_ACCEPTED, List.of(dynamic(null, null)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.COLUMNS_CONTENT_NOT_ACCEPTED, List.of(dynamic(2, List.of(List.of()))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.ROWS_NOT_ACCEPTED,
                      List.of(new SectionDescription(null, SectionType.DYNAMIC, 1, 1, null, null, null, null)));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.CELLS_NOT_ACCEPTED,
                      List.of(new SectionDescription(null, SectionType.DYNAMIC, 1, null, null, List.of(), null, null)));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.COLUMNS_CONTENT_NOT_ACCEPTED,
                      List.of(new SectionDescription(null, SectionType.GRID, 1, 1, List.of(), null, null, null)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.COLUMNS_NOT_ACCEPTED, List.of(grid(13, 1)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.ROWS_NOT_ACCEPTED, List.of(grid(1, 0)));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.CELLS_NOT_ACCEPTED, List.of(grid(2, 2, new CellDescription(null, 3, 1, null, null, null))));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.CELLS_NOT_ACCEPTED, List.of(grid(2, 2, new CellDescription(null, 1, 2, null, 2, null))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.CELLS_NOT_ACCEPTED,
                      List.of(grid(2, 2, new CellDescription(null, 1, 1, null, 2, null), new CellDescription(null, 1, 2, null, null, null))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.CELL_TAKEN,
                      List.of(grid(2, 2, new CellDescription(null, 1, 1, null, null, null), new CellDescription(null, 1, 1, null, null, news))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.DISPLAY_NOT_ACCEPTED,
                      List.of(new SectionDescription(null, SectionType.GRID, 1, 1, null, null, null, DisplayMode.HIDE_MOBILE)));
    assertPageRefused(ObjectNotFoundException.class,
                      PageLayoutAssembler.SECTION_NOT_FOUND,
                      List.of(new SectionDescription("999", SectionType.DYNAMIC, 1, null, null, null, null, null)));
    assertPageRefused(ObjectNotFoundException.class, PageLayoutAssembler.CELL_NOT_FOUND, List.of(grid(1, 1, new CellDescription("999", 1, 1, null, null, null))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.APPLICATIONS_NOT_ACCEPTED,
                      List.of(dynamic(1, List.of(List.of(new ApplicationDescription(null, null, null, null, null, null, null, null, null))))));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.APPLICATIONS_NOT_ACCEPTED,
                      List.of(dynamic(1, List.of(List.of(new ApplicationDescription(null, null, 10L, null, null, null, Map.of("k", "v"), null, null))))));
    assertPageRefused(ObjectNotFoundException.class,
                      PageLayoutAssembler.APPLICATION_NOT_FOUND,
                      List.of(dynamic(1, List.of(List.of(new ApplicationDescription("999", null, null, null, null, null, null, null, null))))));
    instance(10L, "content/NewsListView", "News");
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.TITLE_NOT_ACCEPTED,
                      List.of(dynamic(1, List.of(List.of(new ApplicationDescription(null, null, 10L, "x".repeat(201), null, null, null, null, null))))));
    verify(portletInstanceService, never()).expandPortletPreferences(any(), any());
  }

  @Test
  public void keptSectionKeepsItsCellsAndHiddenWindowsStayPinnedOrRefuseTheCall() throws Exception {
    Container section = flexSection("7", "8");
    Container hiddenCell = (Container) section.getChildren().get(1);
    Application hidden = (Application) hiddenCell.getChildren().get(0);
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    ApplicationDescription shown = new ApplicationDescription("8", null, null, null, null, null, null, null, null);
    SectionDescription kept = new SectionDescription(section.getStorageId(),
                                                     SectionType.DYNAMIC,
                                                     3,
                                                     null,
                                                     List.of(List.of(), List.of(shown), List.of()),
                                                     null,
                                                     null,
                                                     null);
    LayoutModel result = toPageLayout(List.of(kept), storedPage(section), false);
    LayoutModel written = result.getChildren().get(0).getChildren().get(0);
    assertEquals(section.getStorageId(), written.getStorageId());
    assertEquals(hiddenCell.getStorageId(), written.getChildren().get(1).getStorageId());
    List<LayoutModel> column = written.getChildren().get(1).getChildren();
    assertEquals(2, column.size());
    assertEquals("7", column.get(0).getStorageId());
    assertEquals("8", column.get(1).getStorageId());
    assertTrue(written.getChildren().get(0).getChildren().isEmpty());

    section = flexSection("7", "8");
    hidden = (Application) ((Container) section.getChildren().get(1)).getChildren().get(0);
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    SectionDescription narrowed = new SectionDescription(section.getStorageId(), SectionType.DYNAMIC, 2, null, null, null, null, null);
    assertPageRefused(IllegalAccessException.class, PageLayoutAssembler.PAGE_LAYOUT_HIDDEN, List.of(narrowed), storedPage(section));
    assertPageRefused(IllegalAccessException.class, PageLayoutAssembler.PAGE_LAYOUT_HIDDEN, List.of(), storedPage(section));
    assertPageRefused(IllegalArgumentException.class,
                      PageLayoutAssembler.SECTION_ID_NOT_ACCEPTED,
                      List.of(new SectionDescription(section.getStorageId(), SectionType.GRID, 1, 1, null, null, null, null)),
                      storedPage(section));
    assertPageRefused(ObjectNotFoundException.class,
                      PageLayoutAssembler.APPLICATION_NOT_FOUND,
                      List.of(dynamic(1, List.of(List.of(new ApplicationDescription("7", null, null, null, null, null, null, null, null))))),
                      storedPage(section));
    when(aclService.hasAccessPermission(section.getChildren().get(0), USER)).thenReturn(false);
    assertPageRefused(IllegalAccessException.class, PageLayoutAssembler.PAGE_LAYOUT_HIDDEN, List.of(kept), storedPage(section));

    Container grid = container(LayoutDescriptionService.GRID_TEMPLATE,
                               "d-flex flex-column d-md-grid grid-cols grid-cols-xl-2 grid-rows grid-rows-xl-1");
    Container pinnedCell = cell("grid-cell");
    Application pinned = application("9", null, null);
    children(pinnedCell, pinned);
    children(grid, pinnedCell, cell("grid-cell"));
    when(aclService.hasAccessPermission(pinned, USER)).thenReturn(false);
    SectionDescription taken = grid(2, 1, new CellDescription(pinnedCell.getStorageId(), 1, 1, null, null, shown));
    assertPageRefused(IllegalArgumentException.class, PageLayoutAssembler.CELL_TAKEN, List.of(taken), storedPage(grid));
    SectionDescription keptCell = grid(2, 1, new CellDescription(pinnedCell.getStorageId(), 1, 1, null, null, null));
    LayoutModel gridResult = toPageLayout(List.of(keptCell), storedPage(grid), false);
    LayoutModel writtenCell = gridResult.getChildren().get(0).getChildren().get(0).getChildren().get(0);
    assertEquals(pinnedCell.getStorageId(), writtenCell.getStorageId());
    assertEquals("9", writtenCell.getChildren().get(0).getStorageId());
  }

  /**
   * Two hidden windows at the head of a column come back in their stored
   * order, which rests on the hidden windows being collected in tree order:
   * the ids are chosen so that a hash-ordered walk would reverse them
   */
  @Test
  public void hiddenWindowsOfOneColumnKeepTheirStoredOrder() throws Exception {
    String cellClass = "flex-cell grid-cell-colspan grid-cell-colspan-xl-12 grid-cell-rowspan grid-cell-rowspan-xl-1";
    Container section = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    Container column = cell(cellClass);
    Application firstHidden = application("8", null, null);
    Application secondHidden = application("7", null, null);
    children(column, firstHidden, secondHidden, application("5", null, null), application("6", null, null));
    children(section, column);
    when(aclService.hasAccessPermission(firstHidden, USER)).thenReturn(false);
    when(aclService.hasAccessPermission(secondHidden, USER)).thenReturn(false);
    SectionDescription kept = new SectionDescription(section.getStorageId(),
                                                     SectionType.DYNAMIC,
                                                     1,
                                                     null,
                                                     List.of(List.of(new ApplicationDescription("6", null, null, null, null, null, null, null, null),
                                                                     new ApplicationDescription("5", null, null, null, null, null, null, null, null))),
                                                     null,
                                                     null,
                                                     null);
    LayoutModel result = toPageLayout(List.of(kept), storedPage(section), false);
    List<LayoutModel> written = result.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren();
    assertEquals(List.of("8", "7", "6", "5"), written.stream().map(LayoutModel::getStorageId).toList());
  }

  @Test
  public void firstPageWriteMakesKeptWindowsCopiesWithTheirPreferencesAndALaterWriteKeepsTheirIds() throws Exception {
    doAnswer(invocation -> {
      Application application = invocation.getArgument(0);
      TransientApplicationState state = new TransientApplicationState("social/Menu");
      Portlet portlet = new Portlet();
      portlet.setValue("key", "value");
      state.setContentState(portlet);
      application.setState(state);
      return null;
    }).when(portletInstanceService).expandPortletPreferences(any(), any());
    Container section = flexSection("1", "2");
    SectionDescription kept = new SectionDescription(section.getStorageId(),
                                                     SectionType.DYNAMIC,
                                                     3,
                                                     null,
                                                     List.of(List.of(new ApplicationDescription("1", null, null, null, null, null, null, null, null)),
                                                             List.of(),
                                                             List.of()),
                                                     null,
                                                     null,
                                                     null);
    LayoutModel result = toPageLayout(List.of(kept), storedPage(section), true);
    LayoutModel writtenSection = result.getChildren().get(0).getChildren().get(0);
    assertNull(writtenSection.getStorageId());
    LayoutModel window = writtenSection.getChildren().get(0).getChildren().get(0);
    assertNull(window.getStorageId());
    assertEquals("social/Menu", window.getContentId());
    assertEquals("value", window.getPreferences().get(0).getValue());
    Application copy = (Application) ((Container) ((Container) ((Container) result.toPage().getChildren().get(0)).getChildren().get(0)).getChildren().get(0)).getChildren().get(0);
    assertTrue(copy.getState() instanceof TransientApplicationState);

    section = flexSection("1", "2");
    SectionDescription keptAgain = new SectionDescription(section.getStorageId(),
                                                          kept.type(),
                                                          kept.columns(),
                                                          null,
                                                          kept.columnsContent(),
                                                          null,
                                                          null,
                                                          null);
    LayoutModel later = toPageLayout(List.of(keptAgain), storedPage(section), false);
    LayoutModel keptWindow = later.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0);
    assertEquals("1", keptWindow.getStorageId());
    Application persistent = (Application) ((Container) ((Container) ((Container) later.toPage().getChildren().get(0)).getChildren().get(0)).getChildren().get(0)).getChildren().get(0);
    assertTrue(persistent.getState() instanceof PersistentApplicationState);
  }

  @Test
  public void emptySectionsEmptyThePageAndAPageWithoutTheEditorsParentIsRebuilt() throws Exception {
    Container legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    children(legacy, application("9", null, null));
    Page stored = new Page("page");
    children(stored, legacy);
    LayoutModel result = toPageLayout(List.of(), stored, false);
    assertEquals(1, result.getChildren().size());
    assertEquals(EntityMapper.PAGE_LAYOUT_TEMPLATE, result.getChildren().get(0).getTemplate());
    assertTrue(result.getChildren().get(0).getChildren().isEmpty());

    stored = new Page("page");
    legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    children(legacy, application("9", null, null));
    children(stored, legacy);
    SectionDescription placed = dynamic(1, List.of(List.of(new ApplicationDescription("9", null, null, null, null, null, null, null, null))));
    LayoutModel rebuilt = toPageLayout(List.of(placed), stored, false);
    assertEquals("9", rebuilt.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0).getStorageId());

    stored = new Page("page");
    legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    Application hidden = application("9", null, null);
    children(legacy, hidden);
    children(stored, legacy);
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    assertPageRefused(IllegalAccessException.class, PageLayoutAssembler.PAGE_LAYOUT_HIDDEN, List.of(), stored);
  }

  @Test
  public void sitesAreThePortalSitesWithoutAKeyword() {
    PortalConfig visible = new PortalConfig("portal", "visible", "portal_1");
    visible.setLabel("Visible");
    visible.setDisplayOrder(2);
    PortalConfig forbidden = new PortalConfig("portal", "forbidden", "portal_2");
    PortalConfig first = new PortalConfig("portal", "first", "portal_3");
    first.setDisplayOrder(1);
    PortalConfig alpha = new PortalConfig("portal", "alpha", "portal_4");
    alpha.setLabel("Alpha");
    alpha.setDisplayOrder(2);
    when(layoutService.getSites(argThat((SiteFilter f) -> f != null && f.getSiteType() == SiteType.PORTAL))).thenReturn(List.of(visible,
                                                                                                                                forbidden,
                                                                                                                                alpha,
                                                                                                                                first));
    when(aclService.canViewSite(any(), eq(USER))).thenReturn(true);
    when(aclService.canViewSite(forbidden.getSiteKey(), USER)).thenReturn(false);

    List<SiteSummary> sites = descriptionService.describeSites(USER, " ");

    assertEquals(List.of("first", "alpha", "visible"), sites.stream().map(SiteSummary::name).toList());
    assertEquals("Visible", sites.get(2).label());
    assertEquals("portal", sites.get(0).type());
    verify(layoutService).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.PORTAL && f.isExcludeSpaceSites()));
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
    verify(layoutService, times(2)).getSites(argThat((SiteFilter f) -> f.getSiteType() == SiteType.PORTAL && f.isExcludeSpaceSites()));
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
    NodeState childState = child.getState().builder().target("SAME_TAB").build();
    when(child.getState()).thenReturn(childState);
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
    assertEquals("new_tab", homeItem.target());
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
    assertEquals("new_tab", linkItem.target());
    assertEquals("same_tab", homeItem.children().get(0).target());
    NavigationItemDescription groupItem = items.get(2);
    assertEquals(NavigationItemType.GROUP, groupItem.type());
    assertNull(groupItem.pageRef());
    assertNull(groupItem.target());
    assertTrue(groupItem.children().isEmpty());
  }

  @Test
  public void oneItemIsDescribedToItsWriterWithItsChildrenAndWithoutTheViewFilter() throws Exception {
    PageKey pageKey = PageKey.parse("portal::test::team");
    NodeContext<NodeContext<Object>> root = node(0L, "default", null, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> team = node(5L, "team", pageKey, Visibility.HIDDEN);
    NodeContext<NodeContext<Object>> child = node(6L, "child", null, Visibility.DISPLAYED);
    NodeContext<NodeContext<Object>> draft = node(7L, "team_draft_john", pageKey, Visibility.DRAFT);
    nodes(root, team, draft);
    nodes(team, child);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(layoutDraftService.isDraftNode("team_draft_john")).thenReturn(true);
    lenient().when(navigationLayoutService.getNodeLabels(anyLong(), eq(USER))).thenReturn(nodeLabel("Label"));
    when(navigationLayoutService.getNodeLabels(5L, USER)).thenThrow(new IllegalAccessException("no view right after the write"));
    PageContext teamContext = new PageContext(pageKey, pageState("PAGE", null, "*:/platform/users", "*:/platform/administrators"));
    when(layoutService.getPageContext(pageKey)).thenReturn(teamContext);
    when(aclService.canViewNavigation(eq(SITE_KEY), any(), eq(USER))).thenReturn(true);
    when(aclService.canViewNavigation(SITE_KEY, pageKey, USER)).thenReturn(false);
    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(true);

    NavigationItemDescription item = descriptionService.describeNavigationItem(SITE_KEY, 5L, USER);
    assertNotNull(item);
    assertEquals(5L, item.nodeId());
    assertEquals("team", item.urlName());
    assertEquals("Label", item.name());
    assertNull(item.labels());
    assertEquals("Label", item.children().get(0).name());
    assertFalse(item.visible());
    assertEquals(List.of("*:/platform/administrators"), item.accessPermissions());
    assertEquals(1, item.children().size());
    assertEquals(6L, item.children().get(0).nodeId());

    assertNull(descriptionService.describeNavigationItem(SITE_KEY, 7L, USER));
    assertNull(descriptionService.describeNavigationItem(SITE_KEY, 8L, USER));
    when(aclService.canEditNavigation(SiteKey.portal("empty"), USER)).thenReturn(true);
    assertNull(descriptionService.describeNavigationItem(SiteKey.portal("empty"), 5L, USER));
    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> descriptionService.describeNavigationItem(SITE_KEY, 5L, USER));
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
  public void siteLayoutIsDescribedByItsSidebars() {
    SiteDescription description = describedSite();

    assertEquals(5L, description.siteId());
    assertEquals("Test site", description.label());
    assertTrue(description.bannerPresent());
    assertTrue(description.draft());
    assertEquals(List.of("Everyone"), description.accessPermissions());
    assertEquals(Boolean.TRUE, description.leftSidebar().options().fullHeight());
    assertEquals(310, description.leftSidebar().options().width());
    assertEquals(DisplayMode.HIDE_MOBILE, description.leftSidebar().options().display());
    assertFalse(description.leftSidebar().options().sticky());
    assertNotNull(description.leftSidebar().containerId());
    assertEquals(1, description.leftSidebar().applications().size());
    assertEquals(Boolean.FALSE, description.rightSidebar().options().fullHeight());
    assertEquals(250, description.rightSidebar().options().width());
    assertTrue(description.rightSidebar().options().sticky());
  }

  @Test
  public void siteLayoutIsDescribedByItsBanners() {
    SiteDescription description = describedSite();

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

  private SiteDescription describedSite() {
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

    return descriptionService.toSiteDescription(site, root, "Test site", "A site", true);
  }

  @Test
  public void siteLayoutIsRebuiltInTheEditorsShape() throws Exception {
    PortalConfig site = storedSite();
    Container stored = site.getPortalLayout();
    Container outerLeftCell = findContainer(stored, LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    Container topBanner = findContainer(stored, LayoutDescriptionService.BANNER_TEMPLATE, 0);
    instance(10L, "content/NewsListView", "News");
    SiteContainerDescription left = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                 new SiteContainerOptions(null, 250, true, DisplayMode.HIDE_MOBILE, null),
                                                                 null,
                                                                 List.of(new ApplicationDescription("1", null, null, "Menu", null, null, null, false, null)));
    SiteContainerDescription top = new SiteContainerDescription(topBanner.getStorageId(),
                                                                new SiteContainerOptions(60, null, false, null, null),
                                                                null,
                                                                List.of(new ApplicationDescription("2", null, null, null, null, null, null, false, null),
                                                                        new ApplicationDescription(null, null, 10L, null, null, DisplayMode.HIDE_DESKTOP, null, false, null)));
    SiteContainerDescription bottom = new SiteContainerDescription(null, null, null, null);

    LayoutModel result = descriptionService.toSiteLayout(new SiteLayoutUpdate(left, null, List.of(top), List.of(bottom)), site, false, USER, Locale.ENGLISH);

    List<LayoutModel> rootChildren = result.getChildren();
    assertEquals(3, rootChildren.size());
    LayoutModel outerLeft = rootChildren.get(0);
    assertEquals(LayoutDescriptionService.SIDEBAR_TEMPLATE, outerLeft.getTemplate());
    assertEquals("layout-sticky-section", outerLeft.getCssClass());
    LayoutModel cell = outerLeft.getChildren().get(0);
    assertEquals(outerLeftCell.getStorageId(), cell.getStorageId());
    assertEquals("250", cell.getWidth());
    assertEquals("hidden-sm-and-down", cell.getCssClass());
    assertEquals(1, cell.getChildren().size());
    assertEquals("1", cell.getChildren().get(0).getStorageId());
    assertEquals("Menu", cell.getChildren().get(0).getTitle());
    LayoutModel outerRight = rootChildren.get(2);
    assertEquals(LayoutDescriptionService.SIDEBAR_TEMPLATE, outerRight.getTemplate());
    assertTrue(outerRight.getChildren().isEmpty());
    assertNull(outerRight.getCssClass());

    LayoutModel middle = rootChildren.get(1);
    assertEquals(3, middle.getChildren().size());
    LayoutModel writtenTop = middle.getChildren().get(0);
    assertEquals(topBanner.getStorageId(), writtenTop.getStorageId());
    assertEquals("60", writtenTop.getHeight());
    assertEquals("layout-banner-top-section", writtenTop.getCssClass());
    assertEquals(2, writtenTop.getChildren().size());
    List<LayoutModel> firstCell = writtenTop.getChildren().get(0).getChildren();
    assertEquals(2, firstCell.size());
    assertEquals("2", firstCell.get(0).getStorageId());
    assertNull(firstCell.get(1).getStorageId());
    assertEquals("content/NewsListView", firstCell.get(1).getContentId());
    assertEquals("News", firstCell.get(1).getTitle());
    assertEquals("hidden-md-and-up", firstCell.get(1).getCssClass());
    assertTrue(writtenTop.getChildren().get(1).getChildren().isEmpty());
    LayoutModel center = middle.getChildren().get(1);
    assertEquals(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, center.getTemplate());
    assertEquals(3, center.getChildren().size());
    assertTrue(center.getChildren().get(0).getChildren().isEmpty());
    assertEquals(EntityMapper.PAGE_BODY_TEMPLATE, center.getChildren().get(1).getTemplate());
    assertTrue(center.getChildren().get(2).getChildren().isEmpty());
    LayoutModel writtenBottom = middle.getChildren().get(2);
    assertNull(writtenBottom.getStorageId());
    assertEquals("layout-banner-bottom-section", writtenBottom.getCssClass());
    assertEquals("57", writtenBottom.getHeight());
    assertEquals(1, writtenBottom.getChildren().size());
    assertEquals(LayoutDescriptionService.BANNER_CELL_TEMPLATE, writtenBottom.getChildren().get(0).getTemplate());

    Container saved = result.toSite().getPortalLayout();
    Application kept = (Application) ((Container) ((Container) saved.getChildren().get(0)).getChildren().get(0)).getChildren().get(0);
    assertTrue(kept.getState() instanceof PersistentApplicationState);
  }

  @Test
  public void innerSidebarsSitBetweenTheBanners() throws Exception {
    PortalConfig site = storedSite();
    SiteContainerDescription right = new SiteContainerDescription(null, new SiteContainerOptions(null, null, false, null, false), null, null);

    LayoutModel result = descriptionService.toSiteLayout(new SiteLayoutUpdate(null, right, null, null), site, false, USER, Locale.ENGLISH);

    assertTrue(result.getChildren().get(0).getChildren().isEmpty());
    assertTrue(result.getChildren().get(2).getChildren().isEmpty());
    LayoutModel middle = result.getChildren().get(1);
    assertEquals(1, middle.getChildren().size());
    LayoutModel innerRight = middle.getChildren().get(0).getChildren().get(2);
    assertEquals(1, innerRight.getChildren().size());
    assertEquals("310", innerRight.getChildren().get(0).getWidth());
    assertNull(innerRight.getChildren().get(0).getStorageId());
  }

  @Test
  public void keptWindowsBecomeCopiesWithTheirPreferencesWhenTheDraftIsBuiltFromTheLiveLayout() throws Exception {
    PortalConfig site = storedSite();
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    doAnswer(invocation -> {
      Application application = invocation.getArgument(0);
      TransientApplicationState state = new TransientApplicationState("social/Menu");
      Portlet portlet = new Portlet();
      portlet.setValue("key", "value");
      state.setContentState(portlet);
      application.setState(state);
      return null;
    }).when(portletInstanceService).expandPortletPreferences(any(), any());
    SiteContainerDescription left = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                 null,
                                                                 null,
                                                                 List.of(new ApplicationDescription("1", null, null, null, null, null, null, false, null)));

    LayoutModel result = descriptionService.toSiteLayout(new SiteLayoutUpdate(left, null, null, null), site, true, USER, Locale.ENGLISH);

    LayoutModel cell = result.getChildren().get(0).getChildren().get(0);
    assertNull(cell.getStorageId());
    LayoutModel window = cell.getChildren().get(0);
    assertNull(window.getStorageId());
    assertEquals("social/Menu", window.getContentId());
    assertEquals("value", window.getPreferences().get(0).getValue());
    Container saved = result.toSite().getPortalLayout();
    Application copy = (Application) ((Container) ((Container) saved.getChildren().get(0)).getChildren().get(0)).getChildren().get(0);
    assertTrue(copy.getState() instanceof TransientApplicationState);
    assertEquals("social/Menu", ((TransientApplicationState) copy.getState()).getContentId());
  }

  @Test
  public void hiddenElementsStayInPlaceOrRefuseTheCall() throws Exception {
    PortalConfig site = storedSite();
    Container topBanner = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    Application hidden = (Application) ((Container) topBanner.getChildren().get(0)).getChildren().get(0);
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    instance(10L, "content/NewsListView", "News");
    SiteContainerDescription keptTop = new SiteContainerDescription(topBanner.getStorageId(),
                                                                    null,
                                                                    null,
                                                                    List.of(new ApplicationDescription(null, null, 10L, null, null, null, null, false, null)));

    LayoutModel result = descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, List.of(keptTop), null), site, false, USER, Locale.ENGLISH);
    List<LayoutModel> firstCell = result.getChildren().get(1).getChildren().get(0).getChildren().get(0).getChildren();
    assertEquals(2, firstCell.size());
    assertEquals("2", firstCell.get(0).getStorageId());
    assertEquals("content/NewsListView", firstCell.get(1).getContentId());

    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_HIDDEN,
                 assertThrows(IllegalAccessException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, null, null), site, false, USER, Locale.ENGLISH)).getMessage());
    SiteContainerDescription newTop = new SiteContainerDescription(null, null, null, null);
    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_HIDDEN,
                 assertThrows(IllegalAccessException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, List.of(newTop), null), site, false, USER, Locale.ENGLISH)).getMessage());

    SiteContainerDescription movedHidden = new SiteContainerDescription(topBanner.getStorageId(),
                                                                        null,
                                                                        null,
                                                                        List.of(new ApplicationDescription("2", null, null, null, null, null, null, null, null)));
    assertEquals(SiteLayoutAssembler.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, List.of(movedHidden), null), site, false, USER, Locale.ENGLISH)).getMessage());

    PortalConfig siteWithHiddenContainer = storedSite();
    Container hiddenCell = findContainer(siteWithHiddenContainer.getPortalLayout(), LayoutDescriptionService.BANNER_CELL_TEMPLATE, 1);
    when(aclService.hasAccessPermission(hiddenCell, USER)).thenReturn(false);
    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_HIDDEN,
                 assertThrows(IllegalAccessException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, List.of(keptTop), null), siteWithHiddenContainer, false, USER, Locale.ENGLISH)).getMessage());
  }

  @Test
  public void aKeptContainerKeepsWhatTheCallLeavesNull() throws Exception {
    PortalConfig site = storedSite();
    Container topBanner = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    topBanner.setHeight("auto");
    topBanner.setCssClass("layout-banner-top-section layout-sticky-section hidden-sm-and-down");
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#111111@#222222");
    style.setTextTitleColor("#333333");
    topBanner.setCssStyle(style);
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    Container outerLeft = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_TEMPLATE, 0);
    outerLeft.setCssClass("layout-sticky-section");
    outerLeftCell.setWidth("250");
    Application kept = (Application) outerLeftCell.getChildren().get(0);
    ModelStyle applicationStyle = new ModelStyle();
    applicationStyle.setBorderColor("#444444");
    kept.setCssStyle(applicationStyle);
    kept.setCssClass("hidden-md-and-up");
    SiteContainerDescription top = new SiteContainerDescription(topBanner.getStorageId(), null, null, null);
    SiteContainerDescription left = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                 null,
                                                                 null,
                                                                 List.of(new ApplicationDescription("1", null, null, null, null, null, null, null, null)));

    LayoutModel result = build(site, left, List.of(top), null);

    LayoutModel writtenTop = result.getChildren().get(1).getChildren().get(0);
    assertEquals("auto", writtenTop.getHeight());
    assertEquals(Set.of("layout-banner-top-section", "layout-sticky-section", "hidden-sm-and-down"), Set.of(writtenTop.getCssClass().split(" ")));
    assertEquals("#111111@#222222", writtenTop.getBackgroundColor());
    assertEquals("#333333", writtenTop.getTextTitleColor());
    LayoutModel writtenLeft = result.getChildren().get(0);
    assertEquals("layout-sticky-section", writtenLeft.getCssClass());
    assertEquals("250", writtenLeft.getChildren().get(0).getWidth());
    LayoutModel writtenApplication = writtenLeft.getChildren().get(0).getChildren().get(0);
    assertEquals("#444444", writtenApplication.getBorderColor());
    assertEquals("hidden-md-and-up", writtenApplication.getCssClass());

    SiteContainerDescription unstuck = new SiteContainerDescription(topBanner.getStorageId(), new SiteContainerOptions(null, null, false, DisplayMode.BOTH, null), null, null);
    result = build(site, null, List.of(unstuck), null);
    writtenTop = result.getChildren().get(1).getChildren().get(0);
    assertEquals("layout-banner-top-section", writtenTop.getCssClass());
    assertEquals("#111111", writtenTop.getBackgroundColor());

    style.setBackgroundImage("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7");
    style.setBackgroundSize("cover");
    topBanner.setCssClass("layout-banner-top-section layout-bg-margin-top-4");
    SiteContainerDescription recoloured = new SiteContainerDescription(topBanner.getStorageId(),
                                                                       null,
                                                                       new StylingDescription(null, null, null, new BackgroundBlock(true, null, "#777777", null, null, null, null, null, null, null), null, null),
                                                                       null);
    result = build(site, null, List.of(recoloured), null);
    writtenTop = result.getChildren().get(1).getChildren().get(0);
    assertEquals("#777777", writtenTop.getBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7", writtenTop.getBackgroundImage());
    assertEquals("cover", writtenTop.getBackgroundSize());
    assertEquals(Set.of("layout-banner-top-section", "layout-bg-margin-top-4"), Set.of(writtenTop.getCssClass().split(" ")));
    assertEquals("#333333", writtenTop.getTextTitleColor());
  }

  @Test
  public void aKeptElementTheCallDoesNotStyleIsNeitherRewrittenNorRevalidated() throws Exception {
    PortalConfig site = storedSite();
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    Application kept = (Application) outerLeftCell.getChildren().get(0);
    ModelStyle style = new ModelStyle();
    style.setBackgroundImage("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7");
    style.setMarginTop(99);
    style.setTextTitleBackgroundPadding("8px 8px 8px 8px");
    kept.setCssStyle(style);
    SiteContainerDescription left = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                 null,
                                                                 null,
                                                                 List.of(new ApplicationDescription("1", null, null, null, null, null, null, null, null)));

    LayoutModel written = build(site, left, null, null).getChildren().get(0).getChildren().get(0).getChildren().get(0);

    assertNull(written.getBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7", written.getBackgroundImage());
    assertEquals(99, written.getMarginTop());
    assertEquals("8px 8px 8px 8px", written.getTextTitleBackgroundPadding());
  }

  @Test
  public void aKeptInnerSidebarStaysInItsSlot() throws Exception {
    PortalConfig site = storedSite();
    Container innerRightCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 1);
    SiteContainerDescription right = new SiteContainerDescription(innerRightCell.getStorageId(), null, null, null);

    LayoutModel result = build(site, null, null, null, right);

    assertTrue(result.getChildren().get(2).getChildren().isEmpty());
    LayoutModel innerRight = result.getChildren().get(1).getChildren().get(0).getChildren().get(2);
    assertEquals(innerRightCell.getStorageId(), innerRight.getChildren().get(0).getStorageId());
  }

  @Test
  public void aStickySidebarMovedBetweenTheSlotsStaysSticky() throws Exception {
    PortalConfig site = storedSite();
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_TEMPLATE, 0).setCssClass("layout-sticky-section");
    SiteContainerDescription movedInner = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                       new SiteContainerOptions(null, null, null, null, false),
                                                                       null,
                                                                       List.of(new ApplicationDescription("1", null, null, null, null, null, null, null, null)));

    LayoutModel result = build(site, movedInner, null, null);

    assertTrue(result.getChildren().get(0).getChildren().isEmpty());
    assertNull(result.getChildren().get(0).getCssClass());
    LayoutModel innerLeft = result.getChildren().get(1).getChildren().get(0).getChildren().get(0);
    assertEquals("layout-sticky-section", innerLeft.getCssClass());
    assertEquals(outerLeftCell.getStorageId(), innerLeft.getChildren().get(0).getStorageId());
  }

  @Test
  public void aBlockSentWithoutItsFlagIsOn() throws Exception {
    PortalConfig site = storedSite();
    Container topBanner = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#111111");
    style.setBackgroundImage("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7");
    topBanner.setCssStyle(style);
    SiteContainerDescription recoloured = new SiteContainerDescription(topBanner.getStorageId(),
                                                                       null,
                                                                       new StylingDescription(null, null, null, new BackgroundBlock(null, null, "#777777", null, null, null, null, null, null, null), null, null),
                                                                       null);

    LayoutModel writtenTop = build(site, null, List.of(recoloured), null).getChildren().get(1).getChildren().get(0);

    assertEquals("#777777", writtenTop.getBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/site_5_1/7", writtenTop.getBackgroundImage());
  }

  @Test
  public void onlyATopbarCanBeSticky() throws Exception {
    PortalConfig site = storedSite();
    SiteContainerDescription stickyBar = new SiteContainerDescription(null, new SiteContainerOptions(null, null, true, null, null), null, null);
    assertEquals(SiteLayoutAssembler.STICKY_NOT_ACCEPTED, refused(() -> build(site, null, null, List.of(stickyBar))));

    LayoutModel topbar = build(site, null, List.of(stickyBar), null).getChildren().get(1).getChildren().get(0);
    assertEquals(Set.of("layout-banner-top-section", "layout-sticky-section"), Set.of(topbar.getCssClass().split(" ")));

    Container bottom = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1);
    bottom.setCssClass("layout-banner-bottom-section layout-sticky-section");
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#111111@#222222");
    bottom.setCssStyle(style);
    SiteContainerDescription echoed = new SiteContainerDescription(bottom.getStorageId(),
                                                                   new SiteContainerOptions(null, null, true, null, null),
                                                                   new StylingDescription(null, null, null, new BackgroundBlock(true, null, "#111111", null, null, null, null, "#222222", null, null), null, null),
                                                                   null);
    LayoutModel written = build(site, null, null, List.of(echoed)).getChildren().get(1).getChildren().get(1);
    assertEquals("layout-banner-bottom-section", written.getCssClass());
    assertEquals("#111111", written.getBackgroundColor());
    SiteContainerDescription untouched = new SiteContainerDescription(bottom.getStorageId(), null, null, null);
    written = build(site, null, null, List.of(untouched)).getChildren().get(1).getChildren().get(1);
    assertEquals("layout-banner-bottom-section", written.getCssClass());
    assertEquals("#111111", written.getBackgroundColor());
    SiteContainerDescription scrolled = new SiteContainerDescription(bottom.getStorageId(),
                                                                     null,
                                                                     new StylingDescription(null, null, null, new BackgroundBlock(true, null, "#111111", null, null, null, null, "#333333", null, null), null, null),
                                                                     null);
    assertEquals(LayoutStylingService.REFUSAL_PREFIX + "background.scroll_color", refused(() -> build(site, null, null, List.of(scrolled))));
  }

  @Test
  public void aSideHoldingTwoSidebarsIsRefused() throws Exception {
    PortalConfig site = storedSite();
    Container center = findContainer(site.getPortalLayout(), LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, 0);
    Container innerLeft = (Container) center.getChildren().get(0);
    Container innerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    children(innerLeftCell, application("3", null, null));
    children(innerLeft, innerLeftCell);

    assertEquals(SiteLayoutAssembler.TWO_LEFT_SIDEBARS, refused(() -> build(site, null, null, null)));
    verify(portletInstanceService, never()).getPortletInstances(anyLong(), any(), any(), anyBoolean());
  }

  @Test
  public void aStoredValueTheSaveWouldRefuseIsRefusedBeforeAnyWrite() throws Exception {
    PortalConfig site = storedSite();
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    Application kept = (Application) outerLeftCell.getChildren().get(0);
    kept.setWidth("100px; javascript:alert(1)");
    SiteContainerDescription left = new SiteContainerDescription(outerLeftCell.getStorageId(),
                                                                 null,
                                                                 null,
                                                                 List.of(new ApplicationDescription("1", null, null, null, null, null, null, null, null)));

    assertEquals(SiteLayoutAssembler.STYLING_NOT_ACCEPTED, refused(() -> build(site, left, null, null)));
  }

  @Test
  public void hiddenWindowsComeBackInTheirStoredOrder() throws Exception {
    PortalConfig site = storedSite();
    Container topBanner = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    Container firstCell = (Container) topBanner.getChildren().get(0);
    Application first = (Application) firstCell.getChildren().get(0);
    Application second = application("4", null, null);
    Application visible = application("5", null, null);
    children(firstCell, first, second, visible);
    when(aclService.hasAccessPermission(first, USER)).thenReturn(false);
    when(aclService.hasAccessPermission(second, USER)).thenReturn(false);
    SiteContainerDescription top = new SiteContainerDescription(topBanner.getStorageId(),
                                                                null,
                                                                null,
                                                                List.of(new ApplicationDescription("5", null, null, null, null, null, null, null, null)));

    LayoutModel result = build(site, null, List.of(top), null);

    List<LayoutModel> written = result.getChildren().get(1).getChildren().get(0).getChildren().get(0).getChildren();
    assertEquals(List.of("2", "4", "5"), written.stream().map(LayoutModel::getStorageId).toList());
  }

  @Test
  public void aCallOfMoreThanTwoHundredElementsIsRefused() throws Exception {
    PortalConfig site = storedSite();
    List<ApplicationDescription> fortyOne = java.util.Collections.nCopies(41, new ApplicationDescription(null, null, 10L, null, null, null, null, null, null));
    List<SiteContainerDescription> five = java.util.Collections.nCopies(5, new SiteContainerDescription(null, null, null, fortyOne));

    assertEquals(SiteLayoutAssembler.ELEMENTS_NOT_ACCEPTED, refused(() -> build(site, null, five, null)));
    verify(portletInstanceService, never()).getPortletInstance(anyLong(), any(), any(), anyBoolean());
  }

  @Test
  public void idsAreCheckedBeforeAnythingIsBuilt() throws Exception {
    PortalConfig site = storedSite();
    Container outerLeftCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);
    Container topBanner = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    SiteContainerDescription unknownCell = new SiteContainerDescription("nope", null, null, null);
    assertEquals(SiteLayoutAssembler.CONTAINER_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(unknownCell, null, null, null), site, false, USER, Locale.ENGLISH)).getMessage());
    SiteContainerDescription bannerAsSidebar = new SiteContainerDescription(topBanner.getStorageId(), null, null, null);
    assertEquals(SiteLayoutAssembler.CONTAINER_ID_NOT_ACCEPTED,
                 refused(() -> descriptionService.toSiteLayout(new SiteLayoutUpdate(bannerAsSidebar, null, null, null), site, false, USER, Locale.ENGLISH)));
    SiteContainerDescription cell = new SiteContainerDescription(outerLeftCell.getStorageId(), null, null, null);
    assertEquals(SiteLayoutAssembler.CONTAINER_ID_NOT_ACCEPTED,
                 refused(() -> descriptionService.toSiteLayout(new SiteLayoutUpdate(cell, cell, null, null), site, false, USER, Locale.ENGLISH)));
    SiteContainerDescription unknownApplication = new SiteContainerDescription(null, null, null,
                                                                               List.of(new ApplicationDescription("nope", null, null, null, null, null, null, false, null)));
    assertEquals(SiteLayoutAssembler.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(unknownApplication, null, null, null), site, false, USER, Locale.ENGLISH)).getMessage());
    SiteContainerDescription cellAsApplication = new SiteContainerDescription(null, null, null,
                                                                              List.of(new ApplicationDescription(outerLeftCell.getStorageId(), null, null, null, null, null, null, false, null)));
    assertEquals(SiteLayoutAssembler.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> descriptionService.toSiteLayout(new SiteLayoutUpdate(cellAsApplication, null, null, null), site, false, USER, Locale.ENGLISH)).getMessage());
    ApplicationDescription kept = new ApplicationDescription("1", null, null, null, null, null, null, false, null);
    SiteContainerDescription twice = new SiteContainerDescription(null, null, null, List.of(kept, kept));
    assertEquals(SiteLayoutAssembler.APPLICATION_ID_NOT_ACCEPTED,
                 refused(() -> descriptionService.toSiteLayout(new SiteLayoutUpdate(twice, null, null, null), site, false, USER, Locale.ENGLISH)));
    SiteContainerDescription nothing = new SiteContainerDescription(null, null, null,
                                                                    List.of(new ApplicationDescription(null, null, null, null, null, null, null, false, null)));
    assertEquals(SiteLayoutAssembler.APPLICATIONS_NOT_ACCEPTED,
                 refused(() -> descriptionService.toSiteLayout(new SiteLayoutUpdate(nothing, null, null, null), site, false, USER, Locale.ENGLISH)));
    verify(portletInstanceService, never()).getPortletInstance(anyLong(), any(), any(), anyBoolean());
  }

  @Test
  public void optionsAndBoundsAreChecked() throws Exception {
    PortalConfig site = storedSite();
    assertEquals(SiteLayoutAssembler.WIDTH_NOT_ACCEPTED, refused(() -> build(site, sidebar(new SiteContainerOptions(null, 49, false, null, null)), null, null)));
    assertEquals(SiteLayoutAssembler.WIDTH_NOT_ACCEPTED, refused(() -> build(site, sidebar(new SiteContainerOptions(null, 1001, false, null, null)), null, null)));
    assertEquals(SiteLayoutAssembler.HEIGHT_NOT_ACCEPTED, refused(() -> build(site, sidebar(new SiteContainerOptions(60, null, false, null, null)), null, null)));
    assertEquals(SiteLayoutAssembler.DISPLAY_NOT_ACCEPTED, refused(() -> build(site, sidebar(new SiteContainerOptions(null, null, false, DisplayMode.HIDE_DESKTOP, null)), null, null)));
    assertEquals(SiteLayoutAssembler.HEIGHT_NOT_ACCEPTED, refused(() -> build(site, null, List.of(sidebar(new SiteContainerOptions(1001, null, false, null, null))), null)));
    assertEquals(SiteLayoutAssembler.WIDTH_NOT_ACCEPTED, refused(() -> build(site, null, List.of(sidebar(new SiteContainerOptions(null, 300, false, null, null))), null)));
    assertEquals(SiteLayoutAssembler.FULL_HEIGHT_NOT_ACCEPTED, refused(() -> build(site, null, null, List.of(sidebar(new SiteContainerOptions(null, null, false, null, true))))));
    assertEquals(SiteLayoutAssembler.DISPLAY_NOT_ACCEPTED, refused(() -> build(site, null, List.of(sidebar(new SiteContainerOptions(null, null, false, DisplayMode.HIDE_DESKTOP, null))), null)));
    List<SiteContainerDescription> eleven = java.util.Collections.nCopies(11, sidebar(null));
    assertEquals(SiteLayoutAssembler.TOPBARS_NOT_ACCEPTED, refused(() -> build(site, null, eleven, null)));
    assertEquals(SiteLayoutAssembler.BOTTOM_BARS_NOT_ACCEPTED, refused(() -> build(site, null, null, eleven)));
    List<ApplicationDescription> many = java.util.Collections.nCopies(51, new ApplicationDescription(null, null, 10L, null, null, null, null, false, null));
    assertEquals(SiteLayoutAssembler.APPLICATIONS_NOT_ACCEPTED,
                 refused(() -> build(site, new SiteContainerDescription(null, null, null, many), null, null)));
    instance(10L, "content/NewsListView", "News");
    SiteContainerDescription longTitle = new SiteContainerDescription(null, null, null,
                                                                      List.of(new ApplicationDescription(null, null, 10L, "x".repeat(201), null, null, null, false, null)));
    assertEquals(SiteLayoutAssembler.TITLE_NOT_ACCEPTED, refused(() -> build(site, longTitle, null, null)));
    SiteContainerDescription withSettings = new SiteContainerDescription(null, null, null,
                                                                         List.of(new ApplicationDescription(null, null, 10L, null, null, null, Map.of("a", "b"), false, null)));
    assertEquals(SiteLayoutAssembler.APPLICATIONS_NOT_ACCEPTED, refused(() -> build(site, withSettings, null, null)));
    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_NOT_EDITABLE,
                 refused(() -> build(new PortalConfig("portal", "test", "portal_5"), null, null, null)));
    PortalConfig flat = new PortalConfig("portal", "test", "portal_5");
    flat.setPortalLayout(container(LayoutDescriptionService.SITE_MIDDLE_TEMPLATE, null));
    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_NOT_EDITABLE, refused(() -> build(flat, null, null, null)));
  }

  @Test
  public void anAddedApplicationComesFromTheCatalogTheUserMayPlace() throws Exception {
    PortalConfig site = storedSite();
    when(portletInstanceService.getPortletInstance(10L, USER, Locale.ENGLISH, true)).thenThrow(new IllegalAccessException());
    SiteContainerDescription byInstance = new SiteContainerDescription(null, null, null,
                                                                       List.of(new ApplicationDescription(null, null, 10L, null, null, null, null, false, null)));
    assertEquals(LayoutDescriptionService.INSTANCE_ID_NOT_ACCEPTED, refused(() -> build(site, byInstance, null, null)));
    PortletInstance disabled = instance(11L, "content/NewsListView", "News");
    disabled.setDisabled(true);
    SiteContainerDescription byDisabledInstance = new SiteContainerDescription(null, null, null,
                                                                               List.of(new ApplicationDescription(null, null, 11L, null, null, null, null, false, null)));
    assertEquals(LayoutDescriptionService.INSTANCE_ID_NOT_ACCEPTED, refused(() -> build(site, byDisabledInstance, null, null)));
    instance(12L, "content/NewsListView", "News");
    SiteContainerDescription mismatch = new SiteContainerDescription(null, null, null,
                                                                     List.of(new ApplicationDescription(null, "other/Portlet", 12L, null, null, null, null, false, null)));
    assertEquals(LayoutDescriptionService.CONTENT_ID_NOT_ACCEPTED, refused(() -> build(site, mismatch, null, null)));

    PortletInstance first = instance(13L, "social/Menu", "Menu");
    PortletInstance second = instance(14L, "social/Menu", "Other menu");
    when(portletInstanceService.getPortletInstances(0, USER, Locale.ENGLISH, true)).thenReturn(List.of(first, second));
    SiteContainerDescription byContent = new SiteContainerDescription(null, null, null,
                                                                      List.of(new ApplicationDescription(null, "social/Menu", null, null, null, null, null, false, null)));
    assertEquals(LayoutDescriptionService.CONTENT_ID_NOT_ACCEPTED, refused(() -> build(site, byContent, null, null)));
    second.setDisabled(true);
    LayoutModel result = build(site, byContent, null, null);
    LayoutModel added = result.getChildren().get(0).getChildren().get(0).getChildren().get(0);
    assertEquals("social/Menu", added.getContentId());
    assertEquals("Menu", added.getTitle());
  }

  @Test
  public void writtenSiteReadsBackThroughTheSiteDescription() throws Exception {
    PortalConfig site = storedSite();
    instance(10L, "content/NewsListView", "News");
    StylingDescription bannerStyling = new StylingDescription(new MarginsBlock(true, null, 4, null, null, null),
                                                              null,
                                                              null,
                                                              new BackgroundBlock(true, null, "#111111", null, null, null, null, "#222222", null, null),
                                                              new TextBlock(true, new TextRoleBlock("#333333", 18, true, null, null), null, null, null, null),
                                                              null);
    StylingDescription applicationStyling = new StylingDescription(new MarginsBlock(true, 24, null, null, null, null),
                                                                   new BorderBlock(true, "#444444", 2, false),
                                                                   new CornersBlock(true, 8, null, null, null, null),
                                                                   new BackgroundBlock(true, BackgroundMode.LINEAR, null, "#555555", "#666666", 30, "to_right", null, null, null),
                                                                   null,
                                                                   null);
    SiteContainerDescription top = new SiteContainerDescription(null,
                                                                new SiteContainerOptions(60, null, true, DisplayMode.HIDE_MOBILE, null),
                                                                bannerStyling,
                                                                List.of(new ApplicationDescription(null, null, 10L, null, applicationStyling, DisplayMode.HIDE_DESKTOP, null, false, null)));
    SiteContainerDescription left = new SiteContainerDescription(null, new SiteContainerOptions(null, 200, false, null, false), null, null);

    LayoutModel written = build(site, left, List.of(top), null);
    SiteDescription description = descriptionService.toSiteDescription(site, written.toSite().getPortalLayout(), "Test", null, true);

    assertEquals(200, description.leftSidebar().options().width());
    assertEquals(false, description.leftSidebar().options().fullHeight());
    assertNull(description.rightSidebar());
    assertEquals(1, description.topbars().size());
    SiteContainerDescription readTop = description.topbars().get(0);
    assertEquals(60, readTop.options().height());
    assertTrue(readTop.options().sticky());
    assertEquals(DisplayMode.HIDE_MOBILE, readTop.options().display());
    assertEquals(4, readTop.styling().margins().top());
    assertEquals(0, readTop.styling().margins().right());
    assertEquals("#111111", readTop.styling().background().color());
    assertEquals("#222222", readTop.styling().background().scrollColor());
    assertEquals("#333333", readTop.styling().text().title().color());
    assertEquals(18, readTop.styling().text().title().size());
    assertEquals(true, readTop.styling().text().title().bold());
    ApplicationDescription readApplication = readTop.applications().get(0);
    assertEquals("content/NewsListView", readApplication.contentId());
    assertEquals(DisplayMode.HIDE_DESKTOP, readApplication.display());
    assertEquals(24, readApplication.styling().margins().all());
    assertEquals("#444444", readApplication.styling().border().color());
    assertEquals(2, readApplication.styling().border().size());
    assertEquals(8, readApplication.styling().corners().all());
    assertEquals(BackgroundMode.LINEAR, readApplication.styling().background().mode());
    assertEquals("#555555", readApplication.styling().background().from());
    assertEquals("#666666", readApplication.styling().background().to());
    assertEquals(30, readApplication.styling().background().ratio());
    assertEquals("to_right", readApplication.styling().background().direction());
    assertTrue(description.bottomBars().isEmpty());
  }

  private LayoutModel build(PortalConfig site,
                            SiteContainerDescription left,
                            List<SiteContainerDescription> topbars,
                            List<SiteContainerDescription> bottomBars) throws Exception {
    return build(site, left, topbars, bottomBars, null);
  }

  private LayoutModel build(PortalConfig site,
                            SiteContainerDescription left,
                            List<SiteContainerDescription> topbars,
                            List<SiteContainerDescription> bottomBars,
                            SiteContainerDescription right) throws Exception {
    return descriptionService.toSiteLayout(new SiteLayoutUpdate(left, right, topbars, bottomBars), site, false, USER, Locale.ENGLISH);
  }

  private String refused(Executable call) {
    return assertThrows(IllegalArgumentException.class, call).getMessage();
  }

  private SiteContainerDescription sidebar(SiteContainerOptions options) {
    return new SiteContainerDescription(null, options, null, null);
  }

  private PortletInstance instance(long id, String contentId, String name) throws Exception {
    PortletInstance instance = instance(id, 1L, name, null, false);
    instance.setContentId(contentId);
    lenient().when(portletInstanceService.getPortletInstance(id, USER, Locale.ENGLISH, true)).thenReturn(instance);
    return instance;
  }

  /**
   * The stored site of the read tests, every element visible to the user:
   * an outer left sidebar with one application, a top banner of two cells
   * holding one application, an inner right sidebar, an empty bottom banner
   */
  private PortalConfig storedSite() {
    Container outerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    outerLeftCell.setWidth("310");
    children(outerLeftCell, application("1", null, null));
    Container outerLeft = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    children(outerLeft, outerLeftCell);
    Container outerRight = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    Container topCell = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    children(topCell, application("2", null, null));
    Container top = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-top-section");
    top.setHeight("57");
    children(top, topCell, container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null));
    Container bottom = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-bottom-section");
    Container innerRightCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    Container innerRight = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    children(innerRight, innerRightCell);
    Container center = container(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, null);
    children(center, container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null), new PageBody(), innerRight);
    Container middle = container(LayoutDescriptionService.SITE_MIDDLE_TEMPLATE, null);
    children(middle, top, center, bottom);
    Container root = container(SiteLayoutAssembler.SITE_TEMPLATE, null);
    children(root, outerLeft, middle, outerRight);
    PortalConfig site = new PortalConfig("portal", "test", "portal_5");
    site.setPortalLayout(root);
    lenient().when(aclService.hasAccessPermission(any(), eq(USER))).thenReturn(true);
    return site;
  }

  private Container findContainer(Container parent, String template, int occurrence) {
    List<Container> matches = new ArrayList<>();
    collectContainers(parent, template, matches);
    return matches.get(occurrence);
  }

  private void collectContainers(ModelObject object, String template, List<Container> matches) {
    if (object instanceof Container container) {
      if (template.equals(container.getTemplate())) {
        matches.add(container);
      }
      if (container.getChildren() != null) {
        container.getChildren().forEach(child -> collectContainers(child, template, matches));
      }
    }
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

  private LayoutModel toPageLayout(List<SectionDescription> sections, Page stored, boolean copyWindows) throws Exception {
    return descriptionService.toPageLayout(sections, stored, copyWindows, USER, Locale.ENGLISH);
  }

  private void assertPageRefused(Class<? extends Exception> type, String refusal, List<SectionDescription> sections) {
    assertPageRefused(type, refusal, sections, storedPage());
  }

  private void assertPageRefused(Class<? extends Exception> type, String refusal, List<SectionDescription> sections, Page stored) {
    assertEquals(refusal, assertThrows(type, () -> toPageLayout(sections, stored, false)).getMessage());
  }

  private SectionDescription dynamic(Integer columns, List<List<ApplicationDescription>> content) {
    return new SectionDescription(null, SectionType.DYNAMIC, columns, null, content, null, null, null);
  }

  private SectionDescription grid(int columns, int rows, CellDescription... cells) {
    return new SectionDescription(null, SectionType.GRID, columns, rows, null, List.of(cells), null, null);
  }

  /**
   * A stored three-column dynamic section whose middle column holds the two
   * given windows
   */
  private Container flexSection(String firstWindow, String secondWindow) {
    String cellClass = "flex-cell grid-cell-colspan grid-cell-colspan-xl-4 grid-cell-rowspan grid-cell-rowspan-xl-1";
    Container section = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    Container middle = cell(cellClass);
    children(middle, application(firstWindow, null, null), application(secondWindow, null, null));
    children(section, cell(cellClass), middle, cell(cellClass));
    return section;
  }

  private Page storedPage(Container... sections) {
    Container parent = container(EntityMapper.PAGE_LAYOUT_TEMPLATE, null);
    children(parent, sections);
    Page page = new Page("page");
    children(page, parent);
    return page;
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
    NodeState.Builder builder = new NodeState.Builder().pageRef(pageRef).visibility(visibility).label("Label").target("NEW_TAB");
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
