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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.constant.StylingDestination;
import io.meeds.layout.model.ApplicationPosition;
import io.meeds.layout.model.GridGeometry;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PlacementSlot;
import io.meeds.layout.model.PlacedLayout;
import io.meeds.layout.util.EntityMapper;

/**
 * Places, moves or removes one application of a stored page or site tree,
 * for one move_layout_application or remove_layout_application call, every
 * other element staying where it is stored. The places addressed are the
 * ones the read describes: on a page built with the editor's sections, a
 * column of a dynamic section and a cell of a grid section; on a site, a
 * column of a topbar or a bottom bar by the banner's id and the column's
 * number, and a sidebar by the id of its cell, the outer one when a side
 * holds two. An index counts the
 * applications the caller can see, every number from 1, and an application
 * the caller cannot see keeps its stored place among them. A moved
 * application keeps its window, its preferences and its styling, written
 * for its new place in the editor's matrix: a block the new place does not
 * offer is dropped. A grid cell and a column of a topbar or a bottom bar
 * hold one application: a taken one is refused, never swapped. An
 * application alone in a column of a topbar or a bottom bar moves to
 * another column with its own column, which carries the settings the site
 * layout editor gives it: the two columns swap places. Every value is checked before the tree changes.
 */
public class LayoutPlacementAssembler {

  public static final String             POSITION_NOT_ACCEPTED       = "value is not accepted for position";

  public static final String             CANNOT_BE_PLACED            = "cannot be placed there";

  public static final String             CELL_TAKEN                  = PageLayoutAssembler.CELL_TAKEN;

  public static final String             SECTION_NOT_FOUND           = PageLayoutAssembler.SECTION_NOT_FOUND;

  public static final String             CONTAINER_NOT_FOUND         = "does not exist: container";

  public static final String             APPLICATION_NOT_FOUND       = PageLayoutAssembler.APPLICATION_NOT_FOUND;

  public static final String             APPLICATION_ID_NOT_ACCEPTED = PageLayoutAssembler.APPLICATION_ID_NOT_ACCEPTED;

  private final LayoutModel                     root;

  private final Set<String>                     hiddenIds;

  private final LayoutStylingService            stylingService;

  private final LayoutDescriptionService        descriptionService;

  private final Map<String, LayoutModel>        dynamicSections = new HashMap<>();

  private final Map<String, GridGeometry>       gridSections    = new HashMap<>();

  private final Map<String, List<LayoutModel>>  siteContainers  = new HashMap<>();

  private final Set<String>                     banners         = new HashSet<>();

  private final Map<LayoutModel, LayoutModel>   bannerOf        = new IdentityHashMap<>();

  private final Map<String, LayoutModel>        applications    = new HashMap<>();

  private final Map<String, LayoutModel>        cellOf          = new HashMap<>();

  private final Map<String, StylingDestination> destinationOf   = new HashMap<>();

  private final Set<String>                     containerIds    = new HashSet<>();

  /**
   * @param root               the stored page or site layout, wrapped
   * @param site               whether the root is a site layout
   * @param hiddenIds          the storage id of every element the caller
   *                             cannot see, an element under a container they
   *                             cannot see included
   * @param stylingService     the styling writer
   * @param descriptionService the reader whose numbering and styling the
   *                             placement follows
   */
  public LayoutPlacementAssembler(LayoutModel root,
                                  boolean site,
                                  Set<String> hiddenIds,
                                  LayoutStylingService stylingService,
                                  LayoutDescriptionService descriptionService) {
    this.root = root;
    this.hiddenIds = hiddenIds;
    this.stylingService = stylingService;
    this.descriptionService = descriptionService;
    indexContainerIds(root);
    if (site) {
      indexSite();
    } else {
      indexPage();
    }
  }

  public PlacedLayout move(String applicationId, ApplicationPosition position) throws ObjectNotFoundException {
    LayoutModel application = requireApplication(applicationId);
    PlacementSlot slot = resolve(position, application.getStorageId());
    LayoutModel source = cellOf.get(application.getStorageId());
    if (bannerOf.containsKey(source) && bannerOf.containsKey(slot.cell()) && storedChildren(source).size() == 1) {
      swapColumns(source, slot.cell());
      stripGeneratedTokens();
      return new PlacedLayout(root, pathOf(root, application));
    }
    StylingDestination from = destinationOf.get(application.getStorageId());
    detach(application);
    insert(slot, application);
    if (from != slot.destination()) {
      restyle(application, slot.destination());
    }
    stripGeneratedTokens();
    return new PlacedLayout(root, pathOf(root, application));
  }

  public PlacedLayout place(Supplier<LayoutModel> newApplication, ApplicationPosition position) throws ObjectNotFoundException {
    PlacementSlot slot = resolve(position, null);
    LayoutModel application = newApplication.get();
    insert(slot, application);
    stylingService.applyStyling(application, null, slot.destination(), false, false);
    stripGeneratedTokens();
    return new PlacedLayout(root, pathOf(root, application));
  }

  public LayoutModel remove(String applicationId) throws ObjectNotFoundException {
    LayoutModel application = requireApplication(applicationId);
    detach(application);
    applications.remove(application.getStorageId());
    stripGeneratedTokens();
    return root;
  }

  private LayoutModel requireApplication(String givenId) throws ObjectNotFoundException {
    String applicationId = StringUtils.trimToNull(givenId);
    if (applicationId == null || containerIds.contains(applicationId)) {
      throw new IllegalArgumentException(APPLICATION_ID_NOT_ACCEPTED);
    }
    LayoutModel application = applications.get(applicationId);
    if (application == null) {
      throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
    }
    return application;
  }

  private PlacementSlot resolve(ApplicationPosition position, String movedId) throws ObjectNotFoundException {
    if (position == null) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    String sectionId = StringUtils.trimToNull(position.sectionId());
    String containerId = StringUtils.trimToNull(position.containerId());
    if (sectionId != null && containerId != null) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    } else if (containerId != null) {
      return containerSlot(containerId, position, movedId);
    } else if (sectionId != null) {
      return sectionSlot(sectionId, position, movedId);
    } else {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
  }

  private PlacementSlot containerSlot(String containerId, ApplicationPosition position, String movedId) throws ObjectNotFoundException {
    List<LayoutModel> cells = siteContainers.get(containerId);
    if (cells == null) {
      throw new ObjectNotFoundException(CONTAINER_NOT_FOUND);
    } else if (banners.contains(containerId)) {
      return bannerSlot(cells, position, movedId);
    } else if (position.column() != null || position.row() != null) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    return orderedSlot(cells, position, movedId, StylingDestination.SITE_CONTAINER_APPLICATION);
  }

  /**
   * A column of a topbar or a bottom bar, numbered from 1 as the read lists
   * them, holds one application, as the site layout editor offers it: a
   * column holding one, seen or not, is taken
   */
  private PlacementSlot bannerSlot(List<LayoutModel> cells, ApplicationPosition position, String movedId) {
    Integer column = position.column();
    if (cells.isEmpty()) {
      throw new IllegalArgumentException(CANNOT_BE_PLACED);
    } else if (position.row() != null || position.index() != null || position.before() != null || position.after() != null
        || column == null || column < 1 || column > cells.size()) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    LayoutModel cell = cells.get(column - 1);
    boolean taken = storedChildren(cell).stream()
                                        .anyMatch(c -> StringUtils.isBlank(c.getTemplate())
                                                       && !StringUtils.equals(c.getStorageId(), movedId));
    if (taken) {
      throw new IllegalArgumentException(CELL_TAKEN);
    }
    return new PlacementSlot(cell, null, StylingDestination.SITE_CONTAINER_APPLICATION);
  }

  private PlacementSlot sectionSlot(String sectionId, ApplicationPosition position, String movedId) throws ObjectNotFoundException {
    LayoutModel dynamicSection = dynamicSections.get(sectionId);
    GridGeometry gridSection = gridSections.get(sectionId);
    if (dynamicSection != null) {
      List<LayoutModel> columns = cells(dynamicSection);
      Integer column = position.column();
      if (position.row() != null || column == null || column < 1 || column > columns.size()) {
        throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
      }
      return orderedSlot(List.of(columns.get(column - 1)), position, movedId, StylingDestination.APPLICATION);
    } else if (gridSection != null) {
      return gridSlot(gridSection, position, movedId);
    } else {
      throw new ObjectNotFoundException(SECTION_NOT_FOUND);
    }
  }

  private PlacementSlot gridSlot(GridGeometry section, ApplicationPosition position, String movedId) {
    Integer row = position.row();
    Integer column = position.column();
    if (position.index() != null || position.before() != null || position.after() != null || row == null || column == null
        || row < 1 || row > section.rows() || column < 1 || column > section.columns()) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    LayoutModel cell = section.anchors().get(cellKey(row, column));
    if (cell == null) {
      throw new IllegalArgumentException(CANNOT_BE_PLACED);
    }
    boolean taken = storedChildren(cell).stream()
                                  .anyMatch(c -> StringUtils.isBlank(c.getTemplate())
                                                 && !StringUtils.equals(c.getStorageId(), movedId));
    if (taken) {
      throw new IllegalArgumentException(CELL_TAKEN);
    }
    return new PlacementSlot(cell, null, StylingDestination.GRID_CELL_APPLICATION);
  }

  private PlacementSlot orderedSlot(List<LayoutModel> cells,
                                    ApplicationPosition position,
                                    String movedId,
                                    StylingDestination destination) throws ObjectNotFoundException {
    if (cells.isEmpty()) {
      throw new IllegalArgumentException(CANNOT_BE_PLACED);
    }
    long given = Stream.of(position.index(), position.before(), position.after()).filter(Objects::nonNull).count();
    if (given > 1) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    List<LayoutModel> visible = cells.stream()
                                     .flatMap(cell -> visibleApplications(cell).stream())
                                     .filter(a -> !StringUtils.equals(a.getStorageId(), movedId))
                                     .toList();
    LayoutModel next;
    if (position.index() != null) {
      int index = position.index();
      if (index < 1 || index > visible.size() + 1) {
        throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
      }
      next = index <= visible.size() ? visible.get(index - 1) : null;
    } else if (position.before() != null) {
      next = neighbour(position.before(), visible, movedId);
    } else if (position.after() != null) {
      int index = visible.indexOf(neighbour(position.after(), visible, movedId));
      next = index + 1 < visible.size() ? visible.get(index + 1) : null;
    } else {
      next = null;
    }
    LayoutModel cell = next == null ? cells.get(cells.size() - 1) : cellOf.get(next.getStorageId());
    return new PlacementSlot(cell, next, destination);
  }

  private LayoutModel neighbour(String givenId, List<LayoutModel> visible, String movedId) throws ObjectNotFoundException {
    String neighbourId = StringUtils.trimToNull(givenId);
    if (neighbourId == null) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    LayoutModel neighbour = applications.get(neighbourId);
    if (neighbour == null) {
      throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
    } else if (StringUtils.equals(neighbourId, movedId) || !visible.contains(neighbour)) {
      throw new IllegalArgumentException(CANNOT_BE_PLACED);
    }
    return neighbour;
  }

  private void insert(PlacementSlot slot, LayoutModel application) {
    List<LayoutModel> children = new ArrayList<>(storedChildren(slot.cell()));
    int index = slot.next() == null ? children.size() : children.indexOf(slot.next());
    children.add(index, application);
    slot.cell().setChildren(children);
  }

  /**
   * An application alone in a column of a topbar or a bottom bar moves with
   * its column, which carries the settings the site layout editor gives the
   * application: the column takes the empty column's place and the empty
   * column takes its place, the other columns staying where they are
   */
  private void swapColumns(LayoutModel source, LayoutModel target) {
    LayoutModel sourceBanner = bannerOf.get(source);
    LayoutModel targetBanner = bannerOf.get(target);
    List<LayoutModel> sourceColumns = new ArrayList<>(storedChildren(sourceBanner));
    int sourceIndex = indexOf(sourceColumns, source);
    if (sourceBanner == targetBanner) {
      int targetIndex = indexOf(sourceColumns, target);
      sourceColumns.set(sourceIndex, target);
      sourceColumns.set(targetIndex, source);
      sourceBanner.setChildren(sourceColumns);
    } else {
      List<LayoutModel> targetColumns = new ArrayList<>(storedChildren(targetBanner));
      int targetIndex = indexOf(targetColumns, target);
      sourceColumns.set(sourceIndex, target);
      targetColumns.set(targetIndex, source);
      sourceBanner.setChildren(sourceColumns);
      targetBanner.setChildren(targetColumns);
    }
  }

  private int indexOf(List<LayoutModel> list, LayoutModel element) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) == element) {
        return i;
      }
    }
    throw new IllegalStateException(CANNOT_BE_PLACED);
  }

  private void detach(LayoutModel application) {
    LayoutModel cell = cellOf.get(application.getStorageId());
    cell.setChildren(storedChildren(cell).stream().filter(child -> child != application).collect(Collectors.toCollection(ArrayList::new)));
  }

  /**
   * Writes the stored styling again for the new place, read as the new place
   * offers it, so a block it does not offer is dropped rather than refused
   */
  private void restyle(LayoutModel application, StylingDestination to) {
    stylingService.applyStyling(application, descriptionService.toStyling(application, to), to, false, false);
  }

  private void stripGeneratedTokens() {
    applications.forEach((storageId, application) -> stylingService.applyStyling(application,
                                                                               null,
                                                                               destinationOf.get(storageId),
                                                                               false,
                                                                               false));
  }

  private void indexContainerIds(LayoutModel model) {
    if (model == null || hiddenIds.contains(model.getStorageId())) {
      return;
    }
    if (StringUtils.isNotBlank(model.getTemplate()) && StringUtils.isNotBlank(model.getStorageId())) {
      containerIds.add(model.getStorageId());
    }
    children(model).forEach(this::indexContainerIds);
  }

  /**
   * The sections the read describes: the children of the sections parent,
   * when every one of them is a section of the editor's kind
   */
  private void indexPage() {
    List<LayoutModel> sections = children(findSectionsParent(root));
    if (sections.stream()
                .anyMatch(s -> !isTemplate(s, LayoutDescriptionService.FLEX_TEMPLATE)
                               && !isTemplate(s, LayoutDescriptionService.GRID_TEMPLATE))) {
      return;
    }
    for (LayoutModel section : sections) {
      if (StringUtils.isBlank(section.getStorageId())) {
        continue;
      }
      if (isTemplate(section, LayoutDescriptionService.FLEX_TEMPLATE)) {
        dynamicSections.put(section.getStorageId(), section);
        cells(section).forEach(cell -> visibleApplications(cell).forEach(a -> index(a, cell, StylingDestination.APPLICATION)));
      } else {
        gridSections.put(section.getStorageId(), gridSection(section));
      }
    }
  }

  /**
   * Places the cells as the read numbers them: in reading order, each cell
   * taking the first free position, its spans marking the positions it
   * covers; only the first application of a cell is read
   */
  private GridGeometry gridSection(LayoutModel section) {
    int rows = descriptionService.breakpointValue(section.getCssClass(), LayoutDescriptionService.GRID_ROWS_PREFIX);
    int columns = descriptionService.breakpointValue(section.getCssClass(), LayoutDescriptionService.GRID_COLUMNS_PREFIX);
    boolean[][] taken = new boolean[rows][columns];
    Map<String, LayoutModel> anchors = new HashMap<>();
    int rowIndex = 0;
    int colIndex = 0;
    for (LayoutModel cell : cells(section)) {
      int colSpan = descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_COLSPAN_PREFIX);
      int rowSpan = descriptionService.breakpointValue(cell.getCssClass(), LayoutDescriptionService.CELL_ROWSPAN_PREFIX);
      while (rowIndex < rows && taken[rowIndex][colIndex]) {
        colIndex++;
        if (colIndex >= columns) {
          colIndex = 0;
          rowIndex++;
        }
      }
      for (int r = rowIndex; r < Math.min(rows, rowIndex + rowSpan); r++) {
        for (int c = colIndex; c < Math.min(columns, colIndex + colSpan); c++) {
          taken[r][c] = true;
        }
      }
      anchors.put(cellKey(rowIndex + 1, colIndex + 1), cell);
      visibleApplications(cell).stream().findFirst().ifPresent(a -> index(a, cell, StylingDestination.GRID_CELL_APPLICATION));
      colIndex += colSpan;
      if (colIndex >= columns) {
        colIndex = 0;
        rowIndex++;
      }
    }
    return new GridGeometry(rows, columns, anchors);
  }

  /**
   * The containers the read describes: the banners of the middle body, and
   * on each side the sidebar holding a cell, the outer one first
   */
  private void indexSite() {
    List<LayoutModel> rootChildren = children(root);
    LayoutModel middle = rootChildren.stream()
                                     .filter(c -> isTemplate(c, LayoutDescriptionService.SITE_MIDDLE_TEMPLATE))
                                     .findFirst()
                                     .orElse(null);
    List<LayoutModel> middleChildren = children(middle);
    LayoutModel center = null;
    for (LayoutModel child : middleChildren) {
      if (isTemplate(child, LayoutDescriptionService.BANNER_TEMPLATE)) {
        List<LayoutModel> cells = children(child).stream()
                                                 .filter(c -> isTemplate(c, LayoutDescriptionService.BANNER_CELL_TEMPLATE))
                                                 .toList();
        siteContainer(child.getStorageId(), cells);
        cells.forEach(cell -> bannerOf.put(cell, child));
        if (StringUtils.isNotBlank(child.getStorageId())) {
          banners.add(child.getStorageId());
        }
      } else if (isTemplate(child, LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE)) {
        center = child;
      }
    }
    List<LayoutModel> centerChildren = children(center);
    sidebar(sidebarAt(rootChildren, 0), sidebarAt(centerChildren, 0));
    sidebar(sidebarAt(rootChildren, 2), sidebarAt(centerChildren, 2));
  }

  private void sidebar(LayoutModel outer, LayoutModel inner) {
    LayoutModel outerCell = sidebarCell(outer);
    LayoutModel cell = outerCell != null ? outerCell : sidebarCell(inner);
    if (cell != null) {
      siteContainer(cell.getStorageId(), List.of(cell));
    }
  }

  private void siteContainer(String containerId, List<LayoutModel> cells) {
    if (StringUtils.isBlank(containerId)) {
      return;
    }
    siteContainers.put(containerId, cells);
    cells.forEach(cell -> visibleApplications(cell).forEach(a -> index(a, cell, StylingDestination.SITE_CONTAINER_APPLICATION)));
  }

  private void index(LayoutModel application, LayoutModel cell, StylingDestination destination) {
    if (StringUtils.isNotBlank(application.getStorageId())) {
      applications.put(application.getStorageId(), application);
      cellOf.put(application.getStorageId(), cell);
      destinationOf.put(application.getStorageId(), destination);
    }
  }

  private LayoutModel findSectionsParent(LayoutModel layout) {
    List<LayoutModel> children = children(layout);
    if (children.isEmpty()) {
      return null;
    } else if (isTemplate(children.get(0), EntityMapper.PAGE_LAYOUT_TEMPLATE)) {
      return children.get(0);
    } else {
      return findSectionsParent(children.get(0));
    }
  }

  private LayoutModel sidebarCell(LayoutModel sidebar) {
    return children(sidebar).stream()
                            .filter(c -> isTemplate(c, LayoutDescriptionService.SIDEBAR_CELL_TEMPLATE))
                            .findFirst()
                            .orElse(null);
  }

  private LayoutModel sidebarAt(List<LayoutModel> children, int index) {
    return children.size() > index && isTemplate(children.get(index), LayoutDescriptionService.SIDEBAR_TEMPLATE) ? children.get(index)
                                                                                                                  : null;
  }

  private List<LayoutModel> cells(LayoutModel section) {
    return children(section).stream().filter(c -> isTemplate(c, LayoutDescriptionService.CELL_TEMPLATE)).toList();
  }

  private List<LayoutModel> visibleApplications(LayoutModel cell) {
    return children(cell).stream().filter(c -> StringUtils.isBlank(c.getTemplate())).toList();
  }

  private boolean isTemplate(LayoutModel model, String template) {
    return model != null && StringUtils.equals(model.getTemplate(), template);
  }

  private String cellKey(int row, int column) {
    return row + ":" + column;
  }

  /**
   * The children the caller can see, which the read walks
   */
  private List<LayoutModel> children(LayoutModel model) {
    return model == null || model.getChildren() == null ? Collections.emptyList()
                                                        : model.getChildren()
                                                               .stream()
                                                               .filter(c -> !hiddenIds.contains(c.getStorageId()))
                                                               .toList();
  }

  /**
   * Every stored child, those the caller cannot see included, which a write
   * keeps
   */
  private List<LayoutModel> storedChildren(LayoutModel model) {
    return model.getChildren() == null ? Collections.emptyList() : model.getChildren();
  }

  /**
   * @return the child indexes leading from the parent to the target, empty
   *         when the parent does not hold it
   */
  private List<Integer> pathOf(LayoutModel parent, LayoutModel target) {
    List<LayoutModel> children = storedChildren(parent);
    for (int i = 0; i < children.size(); i++) {
      LayoutModel child = children.get(i);
      List<Integer> path = child == target ? new ArrayList<>() : pathOf(child, target);
      if (child == target || !path.isEmpty()) {
        path.addFirst(i);
        return path;
      }
    }
    return new ArrayList<>();
  }
}
