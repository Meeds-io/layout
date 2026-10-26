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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

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
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.model.BackgroundBlock;
import io.meeds.layout.mcp.model.BorderBlock;
import io.meeds.layout.mcp.model.ElementStyling;
import io.meeds.layout.mcp.model.HeightBlock;
import io.meeds.layout.mcp.model.MarginsBlock;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.mcp.model.TextBlock;
import io.meeds.layout.mcp.model.TextRoleBlock;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.util.EntityMapper;

@SpringBootTest(classes = { LayoutDescriptionService.class, LayoutStylingService.class })
@ExtendWith(MockitoExtension.class)
public class LayoutStylesAssemblerTest {

  private static final String      USER       = "john";

  private static final String      FLEX_CLASS =
                                              "d-flex flex-column d-md-grid layout-sticky-application grid-cols grid-cols-md-12 grid-cols-lg-12 grid-cols-xl-12";

  private static final String      GRID_CLASS = "d-flex flex-column d-md-grid grid-cols grid-cols-xl-2 grid-rows grid-rows-xl-1";

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

  private Container                flexSection;

  private Container                flexCell;

  private Container                gridSection;

  private Container                gridCell;

  @BeforeEach
  public void setUp() {
    lenient().when(aclService.hasAccessPermission(any(), eq(USER))).thenReturn(true);
    lenient().when(layoutService.getId(any())).thenReturn("layout/Test");
    lenient().when(portletInstanceService.getApplicationPortletInstanceId(anyLong())).thenReturn(0L);
  }

  @Test
  public void eachPageElementIsWrittenForItsPlaceInTheEditorsMatrix() throws Exception {
    Page stored = storedPage();
    String flexSectionId = flexSection.getStorageId();
    String gridSectionId = gridSection.getStorageId();

    LayoutModel result = descriptionService.toStyledPage(List.of(element(flexSectionId,
                                                                         styling(new MarginsBlock(true, 10, null, null, null, null),
                                                                                 null,
                                                                                 solid("#123456"),
                                                                                 null,
                                                                                 null)),
                                                                 element(gridSectionId,
                                                                         styling(new MarginsBlock(true, null, -60, null, 60, null),
                                                                                 null,
                                                                                 null,
                                                                                 null,
                                                                                 null)),
                                                                 element("1",
                                                                         styling(new MarginsBlock(true, 30, null, null, null, null),
                                                                                 new BorderBlock(true, "#123456", 2, true),
                                                                                 null,
                                                                                 null,
                                                                                 new HeightBlock(true, HeightPreset.CUSTOM, 420))),
                                                                 element("3",
                                                                         styling(null,
                                                                                 null,
                                                                                 null,
                                                                                 new TextBlock(true,
                                                                                               new TextRoleBlock("#333333", 18, true, null, null),
                                                                                               null,
                                                                                               null,
                                                                                               null,
                                                                                               null),
                                                                                 null))),
                                                         stored,
                                                         false,
                                                         USER);

    LayoutModel section = sections(result).get(0);
    assertEquals(flexSectionId, section.getStorageId());
    assertEquals(10, section.getMarginTop());
    assertEquals(10, section.getMarginLeft());
    assertEquals("#123456", section.getBackgroundColor());
    LayoutModel grid = sections(result).get(1);
    assertEquals(-60, grid.getMarginTop());
    assertEquals(60, grid.getMarginBottom());
    assertEquals(0, grid.getMarginLeft());
    LayoutModel dynamicApplication = section.getChildren().get(0).getChildren().get(0);
    assertEquals(30, dynamicApplication.getMarginTop());
    assertEquals("#123456", dynamicApplication.getBorderColor());
    assertEquals("2", dynamicApplication.getBorderSize());
    assertEquals("true", dynamicApplication.getBoxShadow());
    assertEquals("420", dynamicApplication.getHeight());
    LayoutModel gridApplication = grid.getChildren().get(0).getChildren().get(0);
    assertEquals("#333333", gridApplication.getTextTitleColor());
    assertEquals("18px", gridApplication.getTextTitleFontSize());
    assertEquals("bold", gridApplication.getTextTitleFontWeight());
    assertEquals(FLEX_CLASS, section.getCssClass());
    assertEquals(2, section.getChildren().get(0).getChildren().size());
    assertEquals("2", section.getChildren().get(0).getChildren().get(1).getStorageId());
  }

  @Test
  public void aBlockTheElementsPlaceDoesNotOfferIsRefusedNamingIt() {
    Page stored = storedPage();
    String sectionId = flexSection.getStorageId();

    assertEquals("value is not accepted for styling.border",
                 refused(() -> stylePage(stored, element(sectionId, styling(null, new BorderBlock(true, "#123456", 1, false), null, null, null)))));
    assertEquals("value is not accepted for styling.text",
                 refused(() -> stylePage(stored,
                                         element(sectionId,
                                                 styling(null,
                                                         null,
                                                         null,
                                                         new TextBlock(true, new TextRoleBlock("#333333", null, null, null, null), null, null, null, null),
                                                         null)))));
    assertEquals("value is not accepted for styling.height",
                 refused(() -> stylePage(stored, element("3", styling(null, null, null, null, new HeightBlock(true, HeightPreset.SMALL, null))))));
    assertEquals("value is not accepted for styling.margins.top",
                 refused(() -> stylePage(stored, element(sectionId, styling(new MarginsBlock(true, null, 61, null, null, null), null, null, null, null)))));
  }

  @Test
  public void aBlockTheCallOmitsKeepsWhatTheElementHoldsAndADisabledOneIsCleared() throws Exception {
    Page stored = storedPage();
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#ABCDEF");
    style.setMarginTop(12);
    flexSection.setCssStyle(style);
    String sectionId = flexSection.getStorageId();

    LayoutModel kept = sections(stylePage(stored, element(sectionId, styling(new MarginsBlock(true, null, null, null, 5, null), null, null, null, null)))).get(0);

    assertEquals("#ABCDEF", kept.getBackgroundColor());
    assertEquals(12, kept.getMarginTop());
    assertEquals(5, kept.getMarginBottom());

    LayoutModel cleared = sections(stylePage(stored,
                                             element(sectionId,
                                                     styling(null,
                                                             null,
                                                             new BackgroundBlock(false, null, null, null, null, null, null, null, null, null),
                                                             null,
                                                             null)))).get(0);
    assertNull(cleared.getBackgroundColor());
  }

  @Test
  public void aGridCellIsRefusedAndAnIdTheReadDoesNotAnswerDoesNotExist() {
    Page stored = storedPage();
    String flexCellId = flexCell.getStorageId();
    String gridCellId = gridCell.getStorageId();
    String sectionsParentId = ((Container) stored.getChildren().get(0)).getStorageId();

    assertEquals(LayoutStylesAssembler.ELEMENT_ID_NOT_ACCEPTED, refused(() -> stylePage(stored, element(gridCellId, null))));
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> stylePage(stored, element(flexCellId, null))).getMessage());
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> stylePage(stored, element("unknown", null))).getMessage());
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> stylePage(stored, element(sectionsParentId, null))).getMessage());
  }

  @Test
  public void theElementsListIsBoundedAndEachElementGivenOnce() {
    Page stored = storedPage();
    List<ElementStyling> tooMany = IntStream.range(0, LayoutStylesAssembler.MAX_ELEMENTS + 1).mapToObj(i -> element("1", null)).toList();
    List<ElementStyling> withNull = new ArrayList<>();
    withNull.add(null);

    assertEquals(LayoutStylesAssembler.ELEMENTS_NOT_ACCEPTED, refused(() -> descriptionService.toStyledPage(null, stored, false, USER)));
    assertEquals(LayoutStylesAssembler.ELEMENTS_NOT_ACCEPTED,
                 refused(() -> descriptionService.toStyledPage(Collections.emptyList(), stored, false, USER)));
    assertEquals(LayoutStylesAssembler.ELEMENTS_NOT_ACCEPTED, refused(() -> descriptionService.toStyledPage(withNull, stored, false, USER)));
    assertEquals(LayoutStylesAssembler.ELEMENTS_NOT_ACCEPTED, refused(() -> descriptionService.toStyledPage(tooMany, stored, false, USER)));
    assertEquals(LayoutStylesAssembler.ELEMENT_ID_NOT_ACCEPTED, refused(() -> stylePage(stored, element(" ", null))));
    assertEquals(LayoutStylesAssembler.ELEMENT_ID_NOT_ACCEPTED, refused(() -> stylePage(stored, element("1", null), element("1", null))));  }

  @Test
  public void aDeviceVisibilityIsRefusedRatherThanDropped() {
    Page stored = storedPage();

    assertEquals(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED,
                 refused(() -> stylePage(stored, new ElementStyling("1", null, DisplayMode.HIDE_MOBILE))));
    assertEquals(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED,
                 refused(() -> stylePage(stored, new ElementStyling(flexSection.getStorageId(), null, DisplayMode.BOTH))));
  }

  @Test
  public void anElementTheCallerCannotSeeDoesNotExistAndStaysAsStored() throws Exception {
    Page stored = storedPage();
    Application hidden = (Application) flexCell.getChildren().get(1);
    when(aclService.hasAccessPermission(hidden, USER)).thenReturn(false);
    when(aclService.hasAccessPermission(gridSection, USER)).thenReturn(false);

    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> stylePage(stored, element("2", null))).getMessage());
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> stylePage(stored, element("3", null))).getMessage());

    LayoutModel result = stylePage(stored, element("1", styling(new MarginsBlock(true, 0, null, null, null, null), null, null, null, null)));

    List<LayoutModel> column = sections(result).get(0).getChildren().get(0).getChildren();
    assertEquals(2, column.size());
    assertEquals("2", column.get(1).getStorageId());
    assertNull(column.get(1).getMarginTop());
    assertEquals(2, sections(result).size());
  }

  @Test
  public void aPageNotBuiltWithTheEditorsSectionsHasNothingToStyle() {
    Container legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    Page stored = storedPage();
    ((Container) stored.getChildren().get(0)).getChildren().add(legacy);

    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> stylePage(stored, element(flexSection.getStorageId(), null))).getMessage());
  }

  @Test
  public void aHiddenContainerBesideTheSectionsLeavesThemStylableAsTheReadAnswersThem() throws Exception {
    Container legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    Page stored = storedPage();
    ((Container) stored.getChildren().get(0)).getChildren().add(legacy);
    when(aclService.hasAccessPermission(legacy, USER)).thenReturn(false);

    LayoutModel result = stylePage(stored, element(flexSection.getStorageId(), styling(null, null, solid("#123456"), null, null)));

    assertEquals("#123456", sections(result).get(0).getBackgroundColor());
    assertEquals(3, sections(result).size());
  }

  @Test
  public void aHiddenContainerBeforeTheSectionsParentLeavesTheSectionsStylableAsTheReadAnswersThem() throws Exception {
    Container legacy = container("system:/groovy/portal/webui/container/UIContainer.gtmpl", null);
    Page stored = storedPage();
    stored.getChildren().add(0, legacy);
    when(aclService.hasAccessPermission(legacy, USER)).thenReturn(false);

    LayoutModel result = stylePage(stored, element("1", styling(new MarginsBlock(true, 30, null, null, null, null), null, null, null, null)));

    assertEquals(30, result.getChildren().get(1).getChildren().get(0).getChildren().get(0).getChildren().get(0).getMarginTop());
  }

  @Test
  public void aHiddenSidebarShiftsTheSiteContainersAsTheReadSeesThem() throws Exception {
    PortalConfig site = storedSite();
    Container root = site.getPortalLayout();
    Container outerLeft = (Container) root.getChildren().get(0);
    Container outerRight = (Container) root.getChildren().get(2);
    Container rightCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    children(outerRight, rightCell);
    when(aclService.hasAccessPermission(outerLeft, USER)).thenReturn(false);
    String topbarId = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();

    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> styleSite(site, element(rightCell.getStorageId(), null))).getMessage());
    LayoutModel result = styleSite(site, element(topbarId, styling(null, null, solid("#121212"), null, null)));
    assertEquals("#121212@#121212", result.getChildren().get(1).getChildren().get(0).getBackgroundColor());
  }

  @Test
  public void aStoredValueTheValidatorRefusesRefusesTheWholeCall() {
    Page stored = storedPage();
    ModelStyle style = new ModelStyle();
    style.setBackgroundImage("url(javascript:alert)");
    gridSection.setCssStyle(style);

    assertEquals(SiteLayoutAssembler.STYLING_NOT_ACCEPTED,
                 refused(() -> stylePage(stored,
                                         element(flexSection.getStorageId(),
                                                 styling(new MarginsBlock(true, 0, null, null, null, null), null, null, null, null)))));
  }

  @Test
  public void aDraftBuiltFromTheLivePageCopiesItsWindowsWithTheirPreferences() throws Exception {
    Page stored = storedPage();
    copyWindowsWithPreferences();

    LayoutModel copied = descriptionService.toStyledPage(List.of(element("1",
                                                                         styling(new MarginsBlock(true, 30, null, null, null, null),
                                                                                 null,
                                                                                 null,
                                                                                 null,
                                                                                 null))),
                                                         stored,
                                                         true,
                                                         USER);

    LayoutModel section = sections(copied).get(0);
    assertNull(section.getStorageId());
    LayoutModel window = section.getChildren().get(0).getChildren().get(0);
    assertNull(window.getStorageId());
    assertEquals(30, window.getMarginTop());
    assertEquals("social/Menu", window.getContentId());
    assertEquals("value", window.getPreferences().get(0).getValue());

    LayoutModel kept = stylePage(stored, element("1", null));
    assertEquals("1", sections(kept).get(0).getChildren().get(0).getChildren().get(0).getStorageId());
  }

  @Test
  public void eachSiteContainerIsWrittenForItsPlaceAndItsStickiness() throws Exception {
    PortalConfig site = storedSite();
    Container root = site.getPortalLayout();
    Container topbar = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 0);
    Container bottomBar = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 1);
    Container sidebarCell = findContainer(root, LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0);

    LayoutModel result = descriptionService.toStyledSite(List.of(element(topbar.getStorageId(),
                                                                         styling(null, null, scrolled("#111111", "#222222"), null, null)),
                                                                 element(bottomBar.getStorageId(), styling(null, null, solid("#333333"), null, null)),
                                                                 element(sidebarCell.getStorageId(),
                                                                         styling(new MarginsBlock(true, null, null, 4, null, null),
                                                                                 null,
                                                                                 scrolled("#444444", null),
                                                                                 new TextBlock(true, null, null, null, null, "#555555"),
                                                                                 null)),
                                                                 element("2",
                                                                         styling(null, new BorderBlock(true, "#666666", 1, false), null, null, null))),
                                                         site,
                                                         false,
                                                         USER);

    LayoutModel middle = result.getChildren().get(1);
    assertEquals("#111111@#222222", middle.getChildren().get(0).getBackgroundColor());
    assertEquals("#333333", middle.getChildren().get(2).getBackgroundColor());
    LayoutModel cell = result.getChildren().get(0).getChildren().get(0);
    assertEquals(4, cell.getMarginRight());
    assertEquals("#444444@#444444", cell.getBackgroundColor());
    assertEquals("#555555", cell.getIconColor());
    assertEquals("#666666", middle.getChildren().get(0).getChildren().get(0).getChildren().get(0).getBorderColor());
    assertEquals("layout-banner-top-section layout-sticky-section", middle.getChildren().get(0).getCssClass());
  }

  @Test
  public void aSiteContainerRefusesWhatItsPlaceDoesNotOffer() {
    PortalConfig site = storedSite();
    Container root = site.getPortalLayout();
    String topbarId = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();
    String bottomBarId = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 1).getStorageId();
    String sidebarId = findContainer(root, LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, 0).getStorageId();
    String bannerCellId = findContainer(root, LayoutDescriptionService.BANNER_CELL_TEMPLATE, 0).getStorageId();
    BackgroundBlock gradient = new BackgroundBlock(true, BackgroundMode.LINEAR, null, "#111111", "#222222", null, null, null, null, null);

    assertEquals("value is not accepted for styling.background.scroll_color",
                 refused(() -> styleSite(site, element(bottomBarId, styling(null, null, scrolled("#111111", "#222222"), null, null)))));
    assertEquals("value is not accepted for styling.background.mode",
                 refused(() -> styleSite(site, element(topbarId, styling(null, null, gradient, null, null)))));
    assertEquals("value is not accepted for styling.border",
                 refused(() -> styleSite(site, element(sidebarId, styling(null, new BorderBlock(true, "#111111", 1, false), null, null, null)))));
    assertEquals("value is not accepted for styling.height",
                 refused(() -> styleSite(site, element("2", styling(null, null, null, null, new HeightBlock(true, HeightPreset.SMALL, null))))));
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> styleSite(site, element(bannerCellId, null))).getMessage());
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> styleSite(site, element(root.getStorageId(), null))).getMessage());
  }

  @Test
  public void aStoredSiteValueTheValidatorRefusesRefusesTheWholeCall() {
    PortalConfig site = storedSite();
    Container root = site.getPortalLayout();
    ModelStyle style = new ModelStyle();
    style.setBackgroundImage("url(javascript:alert)");
    findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 1).setCssStyle(style);
    String topbarId = findContainer(root, LayoutDescriptionService.BANNER_TEMPLATE, 0).getStorageId();

    assertEquals(SiteLayoutAssembler.STYLING_NOT_ACCEPTED,
                 refused(() -> styleSite(site, element(topbarId, styling(null, null, solid("#121212"), null, null)))));
  }

  @Test
  public void aSiteDraftBuiltFromTheLiveSiteResetsItsIds() throws Exception {
    PortalConfig site = storedSite();
    Container topbar = findContainer(site.getPortalLayout(), LayoutDescriptionService.BANNER_TEMPLATE, 0);
    copyWindowsWithPreferences();

    LayoutModel result = descriptionService.toStyledSite(List.of(element(topbar.getStorageId(), styling(null, null, solid("#121212"), null, null))),
                                                         site,
                                                         true,
                                                         USER);

    LayoutModel copiedTopbar = result.getChildren().get(1).getChildren().get(0);
    assertNull(copiedTopbar.getStorageId());
    assertEquals("#121212@#121212", copiedTopbar.getBackgroundColor());
    assertNull(copiedTopbar.getChildren().get(0).getChildren().get(0).getStorageId());
  }

  private void copyWindowsWithPreferences() {
    doAnswer(invocation -> {
      Application application = invocation.getArgument(0);
      TransientApplicationState state = new TransientApplicationState("social/Menu");
      Portlet portlet = new Portlet();
      portlet.setValue("key", "value");
      state.setContentState(portlet);
      application.setState(state);
      return null;
    }).when(portletInstanceService).expandPortletPreferences(any(), any());
  }

  private LayoutModel stylePage(Page stored, ElementStyling... elements) throws Exception {
    return descriptionService.toStyledPage(List.of(elements), stored, false, USER);
  }

  private LayoutModel styleSite(PortalConfig site, ElementStyling... elements) throws Exception {
    return descriptionService.toStyledSite(List.of(elements), site, false, USER);
  }

  private String refused(Executable call) {
    return assertThrows(IllegalArgumentException.class, call).getMessage();
  }

  private ElementStyling element(String elementId, StylingDescription styling) {
    return new ElementStyling(elementId, styling, null);
  }

  private StylingDescription styling(MarginsBlock margins,
                                     BorderBlock border,
                                     BackgroundBlock background,
                                     TextBlock text,
                                     HeightBlock height) {
    return new StylingDescription(margins, border, null, background, text, height);
  }

  private BackgroundBlock solid(String color) {
    return new BackgroundBlock(true, null, color, null, null, null, null, null, null, null);
  }

  private BackgroundBlock scrolled(String color, String scrollColor) {
    return new BackgroundBlock(true, null, color, null, null, null, null, scrollColor, null, null);
  }

  private List<LayoutModel> sections(LayoutModel page) {
    return page.getChildren().get(0).getChildren();
  }

  /**
   * A stored page of two sections: a one-column dynamic section holding the
   * windows 1 and 2, and a one-row grid of two cells, the first holding the
   * window 3
   */
  private Page storedPage() {
    flexCell = container(LayoutDescriptionService.CELL_TEMPLATE, "flex-cell grid-cell-colspan grid-cell-colspan-xl-12");
    children(flexCell, application("1"), application("2"));
    flexSection = container(LayoutDescriptionService.FLEX_TEMPLATE, FLEX_CLASS);
    children(flexSection, flexCell);
    gridCell = container(LayoutDescriptionService.CELL_TEMPLATE, "grid-cell");
    children(gridCell, application("3"));
    gridSection = container(LayoutDescriptionService.GRID_TEMPLATE, GRID_CLASS);
    children(gridSection, gridCell, container(LayoutDescriptionService.CELL_TEMPLATE, "grid-cell"));
    Container parent = container(EntityMapper.PAGE_LAYOUT_TEMPLATE, null);
    children(parent, flexSection, gridSection);
    Page page = new Page("page");
    children(page, parent);
    return page;
  }

  /**
   * A stored site: an outer left sidebar with one application, a sticky
   * topbar of one cell holding one application, a bottom bar holding the
   * sticky token the read reports and the write ignores
   */
  private PortalConfig storedSite() {
    Container outerLeftCell = container(LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE, null);
    children(outerLeftCell, application("1"));
    Container outerLeft = container(LayoutDescriptionService.SIDEBAR_TEMPLATE, "layout-sticky-section");
    children(outerLeft, outerLeftCell);
    Container topCell = container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null);
    children(topCell, application("2"));
    Container top = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-top-section layout-sticky-section");
    children(top, topCell);
    Container bottom = container(LayoutDescriptionService.BANNER_TEMPLATE, "layout-banner-bottom-section layout-sticky-section");
    children(bottom, container(LayoutDescriptionService.BANNER_CELL_TEMPLATE, null));
    Container center = container(LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE, null);
    children(center, container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null), new PageBody(), container(LayoutDescriptionService.SIDEBAR_TEMPLATE, null));
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
    Container container = new Container(String.valueOf(System.nanoTime()));
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
