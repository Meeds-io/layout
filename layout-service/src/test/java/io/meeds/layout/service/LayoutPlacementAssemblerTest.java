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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.ApplicationPosition;
import io.meeds.layout.model.ApplicationToPlace;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PlacedLayout;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.util.EntityMapper;

@SpringBootTest(classes = { LayoutDescriptionService.class, LayoutStylingService.class })
@ExtendWith(MockitoExtension.class)
public class LayoutPlacementAssemblerTest {

  private static final String        USER          = "john";

  private static final AtomicInteger CONTAINER_IDS = new AtomicInteger(1000);

  private static final String        FLEX_CLASS =
                                                "d-flex flex-column d-md-grid layout-sticky-application grid-cols grid-cols-md-12 grid-cols-lg-12 grid-cols-xl-12";

  private static final String        GRID_CLASS = "d-flex flex-column d-md-grid grid-cols grid-cols-xl-2 grid-rows grid-rows-xl-2";

  @MockitoBean
  private LayoutService              layoutService;

  @MockitoBean
  private PortletInstanceService     portletInstanceService;

  @MockitoBean
  private LayoutAclService           aclService;

  @MockitoBean
  private NavigationService          navigationService;

  @MockitoBean
  private NavigationLayoutService    navigationLayoutService;

  @MockitoBean
  private LayoutDraftService         layoutDraftService;

  @MockitoBean
  private ApplicationCodeService     applicationCodeService;

  @MockitoBean
  private ApplicationSettingsService applicationSettingsService;

  @Autowired
  private LayoutDescriptionService   descriptionService;

  private Container                  flexSection;

  private Container                  firstColumn;

  private Container                  secondColumn;

  private Container                  gridSection;

  private Container                  firstGridCell;

  private Container                  secondGridCell;

  private Container                  wideGridCell;

  private Container                  outerLeftCell;

  @BeforeEach
  public void setUp() {
    lenient().when(aclService.hasAccessPermission(any(), eq(USER))).thenReturn(true);
    lenient().when(layoutService.getId(any())).thenReturn("layout/Test");
  }

  @Test
  public void aDynamicIndexCountsFromOneAndTheResultNamesThePathOfTheMovedWindow() throws Exception {
    Page stored = storedPage();

    PlacedLayout placed = movePage(stored, "3", dynamic(1, 1, null, null));

    assertEquals(List.of("3", "1", "2"), ids(column(placed.layout(), 0)));
    assertEquals(List.of(0, 0, 0, 0), placed.path());
  }

  @Test
  public void beforeAndAfterNameAnApplicationOfTheTargetColumn() throws Exception {
    assertEquals(List.of("2", "3", "1"), ids(column(movePage(storedPage(), "1", dynamic(1, null, null, "3")).layout(), 0)));

    PlacedLayout placed = movePage(storedPage(), "4", dynamic(1, null, "2", null));
    assertEquals(List.of("1", "4", "2", "3"), ids(column(placed.layout(), 0)));
    assertEquals(List.of(), ids(column(placed.layout(), 1)));
    assertEquals(List.of(0, 0, 0, 1), placed.path());
  }

  @Test
  public void withoutIndexTheApplicationGoesLastAndOneMoreThanTheCountIsTheEnd() throws Exception {
    assertEquals(List.of("4", "1"), ids(column(movePage(storedPage(), "1", dynamic(2, null, null, null)).layout(), 1)));
    assertEquals(List.of("1", "2", "3", "4"), ids(column(movePage(storedPage(), "4", dynamic(1, 4, null, null)).layout(), 0)));

    Page stored = storedPage();
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "4", dynamic(1, 5, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "4", dynamic(1, 0, null, null))));
  }

  @Test
  public void aPositionNotOfTheSectionsKindIsRefused() {
    Page stored = storedPage();
    String flexId = flexSection.getStorageId();
    String gridId = gridSection.getStorageId();

    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", null)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(null, null, 1, null, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(flexId, "12", 1, null, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(flexId, null, 1, 1, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(flexId, null, null, null, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", dynamic(3, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", dynamic(0, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", dynamic(1, 1, "2", null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", dynamic(1, null, "2", "3"))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(gridId, null, 1, 1, 1, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> movePage(stored, "1", new ApplicationPosition(gridId, null, 1, null, null, null, null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", grid(3, 1))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", grid(1, 3))));
    assertEquals(LayoutPlacementAssembler.SECTION_NOT_FOUND,
                 notFound(() -> movePage(stored, "1", new ApplicationPosition("99", null, 1, null, null, null, null))));
    assertEquals(LayoutPlacementAssembler.CONTAINER_NOT_FOUND,
                 notFound(() -> movePage(stored, "1", new ApplicationPosition(null, "99", null, null, null, null, null))));
  }

  @Test
  public void aNeighbourOutsideTheColumnOrTheMovedApplicationItselfCannotBePlacedThere() {
    Page stored = storedPage();

    assertEquals(LayoutPlacementAssembler.CANNOT_BE_PLACED, refused(() -> movePage(stored, "1", dynamic(2, null, "2", null))));
    assertEquals(LayoutPlacementAssembler.CANNOT_BE_PLACED, refused(() -> movePage(stored, "1", dynamic(1, null, "1", null))));
    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND, notFound(() -> movePage(stored, "1", dynamic(1, null, "99", null))));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED, refused(() -> movePage(stored, "1", dynamic(1, null, " ", null))));
  }

  @Test
  public void aGridCellHoldsOneApplicationAndIsNamedByTheRowAndColumnTheReadAnswers() throws Exception {
    Page stored = storedPage();
    ((Application) firstColumn.getChildren().get(0)).setHeight("300px");

    PlacedLayout placed = movePage(stored, "1", grid(1, 2));

    LayoutModel cell = sections(placed.layout()).get(1).getChildren().get(1);
    assertEquals(List.of("1"), ids(cell));
    assertNull(cell.getChildren().get(0).getHeight());
    assertEquals(List.of("2", "3"), ids(column(placed.layout(), 0)));
    assertEquals(List.of(0, 1, 1, 0), placed.path());
    assertEquals(List.of("1"), ids(sections(movePage(stored, "1", grid(2, 1)).layout()).get(1).getChildren().get(2)));
    assertEquals(List.of("5"), ids(sections(movePage(stored, "5", grid(1, 1)).layout()).get(1).getChildren().get(0)));
  }

  @Test
  public void aTakenCellIsRefusedAndAPositionCoveredByASpanCannotBePlaced() {
    Page stored = storedPage();

    assertEquals(PageLayoutAssembler.CELL_TAKEN, refused(() -> movePage(stored, "1", grid(1, 1))));
    assertEquals(LayoutPlacementAssembler.CANNOT_BE_PLACED, refused(() -> movePage(stored, "1", grid(2, 2))));
    assertEquals(List.of("5"), ids(new LayoutModel(firstGridCell)));
  }

  @Test
  public void aWindowTheCallerCannotSeeKeepsItsPlaceAndCannotBeAddressed() throws Exception {
    Page stored = storedPage();
    hide(firstColumn.getChildren().get(1));

    assertEquals(List.of("3", "1", "2"), ids(column(movePage(stored, "3", dynamic(1, 1, null, null)).layout(), 0)));
    assertEquals(List.of("2", "3", "1"), ids(column(movePage(stored, "1", dynamic(1, 2, null, null)).layout(), 0)));
    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND, notFound(() -> movePage(stored, "2", dynamic(2, null, null, null))));
    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND, notFound(() -> removePage(stored, "2")));
    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND, notFound(() -> movePage(stored, "1", dynamic(1, null, "2", null))));
    assertEquals(List.of("2", "3"), ids(column(removePage(stored, "1"), 0)));
  }

  @Test
  public void aGridCellHoldingAWindowTheCallerCannotSeeIsTaken() {
    Page stored = storedPage();
    Application hidden = application("6");
    secondGridCell.getChildren().add(hidden);
    hide(hidden);

    assertEquals(PageLayoutAssembler.CELL_TAKEN, refused(() -> movePage(stored, "1", grid(1, 2))));
  }

  @Test
  public void aContainerIdIsRefusedAsAnApplicationIdAndAnUnknownOneDoesNotExist() {
    Page stored = storedPage();

    assertEquals(LayoutPlacementAssembler.APPLICATION_ID_NOT_ACCEPTED,
                 refused(() -> movePage(stored, flexSection.getStorageId(), dynamic(1, null, null, null))));
    assertEquals(LayoutPlacementAssembler.APPLICATION_ID_NOT_ACCEPTED, refused(() -> removePage(stored, firstGridCell.getStorageId())));
    assertEquals(LayoutPlacementAssembler.APPLICATION_ID_NOT_ACCEPTED, refused(() -> removePage(stored, " ")));
    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND, notFound(() -> removePage(stored, "99")));
  }

  @Test
  public void aRemovalLeavesItsColumnAndCellInPlace() throws Exception {
    LayoutModel page = removePage(storedPage(), "4");
    assertEquals(2, sections(page).get(0).getChildren().size());
    assertEquals(List.of(), ids(column(page, 1)));

    LayoutModel grid = sections(removePage(storedPage(), "5")).get(1);
    assertEquals(3, grid.getChildren().size());
    assertEquals(List.of(), ids(grid.getChildren().get(0)));
  }

  @Test
  public void aPlacedApplicationIsBuiltFromTheCatalogInstance() throws Exception {
    PortletInstance instance = new PortletInstance();
    instance.setId(10L);
    instance.setContentId("layout/Links");
    instance.setName("Links");
    when(portletInstanceService.getPortletInstance(10L, USER, Locale.ENGLISH, true)).thenReturn(instance);

    Page stored = storedPage();
    PlacedLayout placed = descriptionService.toPlacedPage(new ApplicationToPlace(10L, null),
                                                          dynamic(2, 1, null, null),
                                                          stored,
                                                          USER,
                                                          Locale.ENGLISH);

    LayoutModel window = column(placed.layout(), 1).getChildren().get(0);
    assertEquals("layout/Links", window.getContentId());
    assertEquals("Links", window.getTitle());
    assertEquals("4", column(placed.layout(), 1).getChildren().get(1).getStorageId());
    assertEquals(List.of(0, 0, 1, 0), placed.path());
  }

  @Test
  public void aPlacementIsCheckedBeforeTheCatalogIsRead() throws Exception {
    Page stored = storedPage();

    assertEquals(LayoutDescriptionService.APPLICATION_NOT_ACCEPTED, refused(() -> placePage(stored, null, dynamic(1, null, null, null))));
    assertEquals(LayoutDescriptionService.APPLICATION_NOT_ACCEPTED,
                 refused(() -> placePage(stored, new ApplicationToPlace(null, " "), dynamic(1, null, null, null))));
    assertEquals(PageLayoutAssembler.CELL_TAKEN, refused(() -> placePage(stored, new ApplicationToPlace(10L, null), grid(1, 1))));
    verify(portletInstanceService, never()).getPortletInstance(anyLong(), any(), any(), anyBoolean());
  }

  @Test
  public void everyVisibleApplicationIsSavedWithoutItsGeneratedTokens() throws Exception {
    Page stored = storedPage();
    ((Application) firstColumn.getChildren().get(1)).setCssClass("brtr-4 mt-n2 my-class");

    assertEquals("my-class", column(movePage(stored, "4", dynamic(1, null, null, null)).layout(), 0).getChildren().get(1).getCssClass());
  }

  @Test
  public void aSidebarTakesAnIndexOrANeighbour() throws Exception {
    PortalConfig site = storedSite();

    LayoutModel after = descriptionService.toMovedSite("a", container(outerLeftCell.getStorageId(), null, null, "c"), site, false, USER)
                                          .layout();
    assertEquals(List.of("c", "a"), ids(after.getChildren().get(0).getChildren().get(0)));
    assertEquals(List.of(), ids(topCellOf(after, 0)));

    LayoutModel first = descriptionService.toMovedSite("b", container(outerLeftCell.getStorageId(), 1, null, null), site, false, USER)
                                          .layout();
    assertEquals(List.of("b", "c"), ids(first.getChildren().get(0).getChildren().get(0)));
  }

  @Test
  public void aColumnOfATopbarOrABottomBarHoldsOneApplication() throws Exception {
    PortalConfig site = storedSite();
    String topId = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();
    String bottomId = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1).getStorageId();

    PlacedLayout placed = descriptionService.toMovedSite("c", bannerColumn(bottomId, 1), site, false, USER);
    assertEquals(List.of("c"), ids(placed.layout().getChildren().get(1).getChildren().get(2).getChildren().get(0)));
    assertEquals(List.of(), ids(placed.layout().getChildren().get(0).getChildren().get(0)));
    assertEquals(List.of(1, 2, 0, 0), placed.path());

    LayoutModel unchanged = descriptionService.toMovedSite("a", bannerColumn(topId, 1), site, false, USER).layout();
    assertEquals(List.of("a"), ids(topCellOf(unchanged, 0)));
    assertEquals(List.of("b"), ids(topCellOf(unchanged, 1)));

    assertEquals(LayoutPlacementAssembler.CELL_TAKEN, refused(() -> descriptionService.toMovedSite("a", bannerColumn(topId, 2), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.CELL_TAKEN, refused(() -> descriptionService.toMovedSite("c", bannerColumn(topId, 1), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", bannerColumn(topId, 3), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", bannerColumn(topId, 0), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", container(topId, null, null, null), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", new ApplicationPosition(null, bottomId, 1, null, 1, null, null), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", new ApplicationPosition(null, bottomId, 1, null, null, "a", null), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", new ApplicationPosition(null, bottomId, 1, null, null, null, "a"), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", new ApplicationPosition(null, bottomId, 1, 1, null, null, null), site, false, USER)));
  }

  @Test
  public void anApplicationAloneInAColumnMovesWithItsColumn() throws Exception {
    PortalConfig site = storedSite();
    Container top = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    Container bottom = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1);
    Container firstTop = (Container) top.getChildren().get(0);
    Container secondTop = (Container) top.getChildren().get(1);
    Container thirdTop = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    top.getChildren().add(thirdTop);
    firstTop.setCssClass("hidden-sm-and-down");
    firstTop.setWidth("fit-content");
    Container bottomColumn = (Container) bottom.getChildren().get(0);

    PlacedLayout inSection = descriptionService.toMovedSite("a", bannerColumn(top.getStorageId(), 3), site, false, USER);
    List<LayoutModel> columns = inSection.layout().getChildren().get(1).getChildren().get(0).getChildren();
    assertEquals(List.of(thirdTop.getStorageId(), secondTop.getStorageId(), firstTop.getStorageId()),
                 columns.stream().map(LayoutModel::getStorageId).toList());
    assertEquals("hidden-sm-and-down", columns.get(2).getCssClass());
    assertEquals("fit-content", columns.get(2).getWidth());
    assertEquals(List.of("a"), ids(columns.get(2)));
    assertEquals(List.of(1, 0, 2, 0), inSection.path());

    PlacedLayout across = descriptionService.toMovedSite("a", bannerColumn(bottom.getStorageId(), 1), site, false, USER);
    LayoutModel middle = across.layout().getChildren().get(1);
    assertEquals(List.of(bottomColumn.getStorageId(), secondTop.getStorageId(), thirdTop.getStorageId()),
                 middle.getChildren().get(0).getChildren().stream().map(LayoutModel::getStorageId).toList());
    assertEquals(List.of(firstTop.getStorageId()), middle.getChildren().get(2).getChildren().stream().map(LayoutModel::getStorageId).toList());
    assertEquals("hidden-sm-and-down", middle.getChildren().get(2).getChildren().get(0).getCssClass());
    assertEquals(List.of("a"), ids(middle.getChildren().get(2).getChildren().get(0)));
    assertEquals(List.of(1, 2, 0, 0), across.path());
  }

  @Test
  public void anApplicationSharingItsColumnMovesAlone() throws Exception {
    PortalConfig site = storedSite();
    Container top = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    Container bottom = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1);
    Container firstTop = (Container) top.getChildren().get(0);
    firstTop.getChildren().add(application("s"));

    PlacedLayout moved = descriptionService.toMovedSite("a", bannerColumn(bottom.getStorageId(), 1), site, false, USER);

    LayoutModel middle = moved.layout().getChildren().get(1);
    assertEquals(firstTop.getStorageId(), middle.getChildren().get(0).getChildren().get(0).getStorageId());
    assertEquals(List.of("s"), ids(middle.getChildren().get(0).getChildren().get(0)));
    assertEquals(List.of("a"), ids(middle.getChildren().get(2).getChildren().get(0)));
    assertEquals(((Container) bottom.getChildren().get(0)).getStorageId(), middle.getChildren().get(2).getChildren().get(0).getStorageId());
  }

  @Test
  public void aColumnHoldingAnApplicationTheUserCannotSeeIsTaken() throws Exception {
    PortalConfig site = storedSite();
    Container bottom = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1);
    Application hidden = application("h");
    ((Container) bottom.getChildren().get(0)).getChildren().add(hidden);
    hide(hidden);

    assertEquals(LayoutPlacementAssembler.CELL_TAKEN,
                 refused(() -> descriptionService.toMovedSite("c", bannerColumn(bottom.getStorageId(), 1), site, false, USER)));
  }

  @Test
  public void columnsAreNumberedOverTheOnesTheUserCanSee() throws Exception {
    PortalConfig site = storedSite();
    Container bottom = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1);
    Container hiddenColumn = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    Container visibleColumn = (Container) bottom.getChildren().get(0);
    children(bottom, hiddenColumn, visibleColumn);
    hide(hiddenColumn);

    PlacedLayout placed = descriptionService.toMovedSite("c", bannerColumn(bottom.getStorageId(), 1), site, false, USER);

    List<LayoutModel> columns = placed.layout().getChildren().get(1).getChildren().get(2).getChildren();
    assertEquals(List.of(), ids(columns.get(0)));
    assertEquals(List.of("c"), ids(columns.get(1)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("c", bannerColumn(bottom.getStorageId(), 2), site, false, USER)));
  }

  @Test
  public void aCatalogApplicationIsPlacedInASiteContainer() throws Exception {
    PortletInstance instance = new PortletInstance();
    instance.setId(10L);
    instance.setContentId("layout/Links");
    instance.setName("Links");
    when(portletInstanceService.getPortletInstance(10L, USER, Locale.ENGLISH, true)).thenReturn(instance);
    PortalConfig site = storedSite();
    String bottomId = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 1).getStorageId();

    PlacedLayout placed = descriptionService.toPlacedSite(new ApplicationToPlace(10L, null),
                                                          bannerColumn(bottomId, 1),
                                                          site,
                                                          false,
                                                          USER,
                                                          Locale.ENGLISH);

    LayoutModel window = placed.layout().getChildren().get(1).getChildren().get(2).getChildren().get(0).getChildren().get(0);
    assertEquals("layout/Links", window.getContentId());
    assertEquals("Links", window.getTitle());
    assertEquals(List.of(1, 2, 0, 0), placed.path());
    String topId = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();
    assertEquals(LayoutPlacementAssembler.CELL_TAKEN,
                 refused(() -> descriptionService.toPlacedSite(new ApplicationToPlace(10L, null), bannerColumn(topId, 1), site, false, USER, Locale.ENGLISH)));
  }

  @Test
  public void aSitePositionNamesAContainerTheReadAnswers() {
    PortalConfig site = storedSite();
    String topId = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();

    assertEquals(LayoutPlacementAssembler.CONTAINER_NOT_FOUND,
                 notFound(() -> descriptionService.toMovedSite("a", container("99", null, null, null), site, false, USER)));
    assertEquals(LayoutPlacementAssembler.SECTION_NOT_FOUND,
                 notFound(() -> descriptionService.toMovedSite("a",
                                                               new ApplicationPosition("99", null, 1, null, null, null, null),
                                                               site,
                                                               false,
                                                               USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("a",
                                                              new ApplicationPosition(null, outerLeftCell.getStorageId(), 1, null, null, null, null),
                                                              site,
                                                              false,
                                                              USER)));
    assertEquals(LayoutPlacementAssembler.POSITION_NOT_ACCEPTED,
                 refused(() -> descriptionService.toMovedSite("a", container(topId, 4, null, null), site, false, USER)));
  }

  @Test
  public void anInnerSidebarBesideAnOuterOneIsNeitherAddressedNorChanged() throws Exception {
    PortalConfig site = storedSite();
    Container innerCell = findContainer(site.getPortalLayout(), LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 1);
    innerCell.getChildren().add(application("i"));

    assertEquals(LayoutPlacementAssembler.APPLICATION_NOT_FOUND,
                 notFound(() -> descriptionService.toRemovedSite("i", site, false, USER)));
    assertEquals(LayoutPlacementAssembler.CONTAINER_NOT_FOUND,
                 notFound(() -> descriptionService.toMovedSite("a", container(innerCell.getStorageId(), null, null, null), site, false, USER)));

    LayoutModel result = descriptionService.toRemovedSite("c", site, false, USER);
    LayoutModel inner = result.getChildren().get(1).getChildren().get(1).getChildren().get(0).getChildren().get(0);
    assertEquals(List.of("i"), ids(inner));
  }

  private PlacedLayout movePage(Page stored, String applicationId, ApplicationPosition position) throws Exception {
    return descriptionService.toMovedPage(applicationId, position, stored, USER);
  }

  private PlacedLayout placePage(Page stored, ApplicationToPlace application, ApplicationPosition position) throws Exception {
    return descriptionService.toPlacedPage(application, position, stored, USER, Locale.ENGLISH);
  }

  private LayoutModel removePage(Page stored, String applicationId) throws Exception {
    return descriptionService.toRemovedPage(applicationId, stored, USER);
  }

  private ApplicationPosition dynamic(Integer column, Integer index, String before, String after) {
    return new ApplicationPosition(flexSection.getStorageId(), null, column, null, index, before, after);
  }

  private ApplicationPosition grid(int row, int column) {
    return new ApplicationPosition(gridSection.getStorageId(), null, column, row, null, null, null);
  }

  private ApplicationPosition bannerColumn(String containerId, int column) {
    return new ApplicationPosition(null, containerId, column, null, null, null, null);
  }

  private ApplicationPosition container(String containerId, Integer index, String before, String after) {
    return new ApplicationPosition(null, containerId, null, null, index, before, after);
  }

  private void hide(ModelObject object) {
    when(aclService.hasAccessPermission(object, USER)).thenReturn(false);
  }

  private String refused(Executable call) {
    return assertThrows(IllegalArgumentException.class, call).getMessage();
  }

  private String notFound(Executable call) {
    return assertThrows(ObjectNotFoundException.class, call).getMessage();
  }

  private List<String> ids(LayoutModel cell) {
    return cell.getChildren() == null ? List.of() : cell.getChildren().stream().map(LayoutModel::getStorageId).toList();
  }

  private List<LayoutModel> sections(LayoutModel page) {
    return page.getChildren().get(0).getChildren();
  }

  private LayoutModel column(LayoutModel page, int index) {
    return sections(page).get(0).getChildren().get(index);
  }

  private LayoutModel topCellOf(LayoutModel site, int index) {
    return site.getChildren().get(1).getChildren().get(0).getChildren().get(index);
  }

  /**
   * A page of two sections: a dynamic one of two columns holding 1, 2, 3
   * and 4, and a grid of two rows and two columns whose first cell holds 5,
   * whose second is empty and whose third spans the second row
   */
  private Page storedPage() {
    firstColumn = container(LayoutDescriptionService.CELL_TEMPLATE, "flex-cell grid-cell-colspan grid-cell-colspan-xl-6");
    children(firstColumn, application("1"), application("2"), application("3"));
    secondColumn = container(LayoutDescriptionService.CELL_TEMPLATE, "flex-cell grid-cell-colspan grid-cell-colspan-xl-6");
    children(secondColumn, application("4"));
    flexSection = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    children(flexSection, firstColumn, secondColumn);
    firstGridCell = container(LayoutDescriptionService.CELL_TEMPLATE, "grid-cell");
    children(firstGridCell, application("5"));
    secondGridCell = container(LayoutDescriptionService.CELL_TEMPLATE, "grid-cell");
    wideGridCell = container(LayoutDescriptionService.CELL_TEMPLATE, "grid-cell grid-cell-colspan grid-cell-colspan-xl-2");
    gridSection = container(LayoutDescriptionService.GRID_TEMPLATE, GRID_CLASS);
    children(gridSection, firstGridCell, secondGridCell, wideGridCell);
    Container parent = container(EntityMapper.PAGE_LAYOUT_TEMPLATE, null);
    children(parent, flexSection, gridSection);
    Page page = new Page("page");
    children(page, parent);
    return page;
  }

  /**
   * A site: an outer left sidebar holding c, an empty inner left sidebar, a
   * topbar of two cells holding a then b, and an empty bottom bar
   */
  private PortalConfig storedSite() {
    outerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    children(outerLeftCell, application("c"));
    Container outerLeft = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    children(outerLeft, outerLeftCell);
    Container innerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    Container innerLeft = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null);
    children(innerLeft, innerLeftCell);
    Container topCell = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    children(topCell, application("a"));
    Container secondTopCell = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    children(secondTopCell, application("b"));
    Container top = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-top-section");
    children(top, topCell, secondTopCell);
    Container bottom = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-bottom-section");
    children(bottom, container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null));
    Container center = container(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, null);
    children(center, innerLeft, new PageBody(), container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null));
    Container middle = container(LayoutDescriptionService.SITE_MIDDLE_TEMPLATE, null);
    children(middle, top, center, bottom);
    Container root = container(SiteLayoutAssembler.SITE_TEMPLATE, null);
    children(root, outerLeft, middle, container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null));
    PortalConfig site = new PortalConfig("portal", "test", "portal_5");
    site.setPortalLayout(root);
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

  private Container container(String template, String cssClass) {
    Container container = new Container(String.valueOf(CONTAINER_IDS.incrementAndGet()));
    container.setTemplate(template);
    container.setCssClass(cssClass);
    container.setChildren(new ArrayList<>());
    return container;
  }

  private Application application(String storageId) {
    Application application = new Application(storageId);
    application.setState(new PersistentApplicationState(storageId));
    return application;
  }

  private void children(Container container, ModelObject... children) {
    container.setChildren(new ArrayList<>(List.of(children)));
  }
}
