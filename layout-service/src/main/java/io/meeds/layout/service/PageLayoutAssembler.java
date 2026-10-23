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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.CellDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.util.EntityMapper;

/**
 * Rebuilds a page's sections from a set_page_layout call, for one call: the
 * page's ancestors down to the sections parent are kept from the stored tree
 * and the parent's children are written in the order given, as the page
 * editor's newSection and newCell shape them: a dynamic section is a
 * FlexContainer over one CellContainer per column, each spanning 12 / N of
 * the grid; a grid section is a GridContainer over the editor's full matrix
 * of CellContainers in reading order, a described cell at its row and column
 * with its spans and an empty cell at every position no cell covers. A
 * section or a cell addressed by its id keeps its stored element and every
 * styling block the call leaves null, a kept dynamic section its cells when
 * its column count is unchanged; an application addressed by its id keeps
 * its window the same way; a window the caller cannot see, which the call
 * can neither address nor move, stays in its cell at its index when that
 * cell is kept, the call being refused otherwise.
 */
public class PageLayoutAssembler {

  public static final String                                SECTIONS_PARENT_TEMPLATE     = EntityMapper.PAGE_LAYOUT_TEMPLATE;

  public static final String                                FLEX_SECTION_CLASS           =
                                                                               "d-flex flex-column d-md-grid layout-sticky-application grid-cols grid-cols-md-12 grid-cols-lg-12 grid-cols-xl-12";

  public static final String                                GRID_SECTION_CLASS           = "d-flex flex-column d-md-grid";

  public static final String                                FLEX_CELL_TOKEN              = "flex-cell";

  public static final String                                GRID_CELL_TOKEN              = "grid-cell";

  public static final String[]                              BREAKPOINTS                  = { "md", "lg", "xl" };

  public static final Set<Integer>                          DYNAMIC_COLUMNS              = Set.of(1, 2, 3, 4, 6, 12);

  public static final int                                   GRID_MAX                     = 12;

  public static final int                                   GRID_COLUMNS                 = 12;

  public static final int                                   MAX_SECTIONS                 = 20;

  public static final int                                   MAX_ELEMENTS                 = 200;

  public static final int                                   TITLE_MAX_LENGTH             = 200;

  public static final String                                PAGE_LAYOUT_HIDDEN           =
                                                                               "not allowed: page holds elements you cannot see";

  public static final String                                SECTIONS_NOT_ACCEPTED        = "value is not accepted for sections";

  public static final String                                ELEMENTS_NOT_ACCEPTED        =
                                                                                  "value is not accepted for layout: more than 200 elements";

  public static final String                                TYPE_NOT_ACCEPTED            = "value is not accepted for type";

  public static final String                                COLUMNS_NOT_ACCEPTED         = "value is not accepted for columns";

  public static final String                                ROWS_NOT_ACCEPTED            = "value is not accepted for rows";

  public static final String                                COLUMNS_CONTENT_NOT_ACCEPTED = "value is not accepted for columns_content";

  public static final String                                CELLS_NOT_ACCEPTED           = "value is not accepted for cells";

  public static final String                                CELL_TAKEN                   = "cell already taken";

  public static final String                                SECTION_NOT_FOUND            = "does not exist: section";

  public static final String                                SECTION_ID_NOT_ACCEPTED      = "value is not accepted for section_id";

  public static final String                                CELL_NOT_FOUND               = "does not exist: cell";

  public static final String                                CELL_ID_NOT_ACCEPTED         = "value is not accepted for cell_id";

  public static final String                                APPLICATION_NOT_FOUND        = SiteLayoutAssembler.APPLICATION_NOT_FOUND;

  public static final String                                APPLICATION_ID_NOT_ACCEPTED  = SiteLayoutAssembler.APPLICATION_ID_NOT_ACCEPTED;

  public static final String                                APPLICATIONS_NOT_ACCEPTED    = SiteLayoutAssembler.APPLICATIONS_NOT_ACCEPTED;

  public static final String                                TITLE_NOT_ACCEPTED           = SiteLayoutAssembler.TITLE_NOT_ACCEPTED;

  public static final String                                DISPLAY_NOT_ACCEPTED         = "value is not accepted for display";

  private static final Pattern                              GRAMMAR_TOKENS               =
                                                                           Pattern.compile("(^| )(d-flex|flex-column|d-md-grid|layout-sticky-application|flex-cell|grid-cell|grid-cols(-(md|lg|xl)-\\d+)?|grid-rows(-(md|lg|xl)-\\d+)?|grid-cell-colspan(-(md|lg|xl)-\\d+)?|grid-cell-rowspan(-(md|lg|xl)-\\d+)?)(?= |$)");

  private final LayoutModel                                 root;

  private final Map<String, Integer>                        hiddenWindows;

  private final LayoutStylingService                        stylingService;

  private final BiFunction<LayoutModel, StylingDestination, StylingDescription> storedStyling;

  private final Function<ApplicationDescription, LayoutModel> newApplication;

  private final Map<String, LayoutModel>                    byId                         = new HashMap<>();

  private final Map<String, LayoutModel>                    parentOf                     = new HashMap<>();

  private final Set<String>                                 used                         = new HashSet<>();

  private final Set<String>                                 placedHidden                 = new HashSet<>();

  /**
   * @param root           the stored page, wrapped
   * @param hiddenWindows  the storage id of each window the caller cannot
   *                         see, with its index in its container
   * @param stylingService the styling writer
   * @param storedStyling  reads an element's stored styling as get_page_layout
   *                         answers it, the base a call's description is
   *                         merged over
   * @param newApplication builds the window of an application the call adds,
   *                         from its instance or content id
   */
  public PageLayoutAssembler(LayoutModel root,
                             Map<String, Integer> hiddenWindows,
                             LayoutStylingService stylingService,
                             BiFunction<LayoutModel, StylingDestination, StylingDescription> storedStyling,
                             Function<ApplicationDescription, LayoutModel> newApplication) {
    this.root = root;
    this.hiddenWindows = hiddenWindows;
    this.stylingService = stylingService;
    this.storedStyling = storedStyling;
    this.newApplication = newApplication;
  }

  public LayoutModel assemble(List<SectionDescription> sections) throws ObjectNotFoundException, IllegalAccessException {
    index(root, null);
    List<SectionDescription> list = sections == null ? Collections.emptyList() : sections;
    if (list.size() > MAX_SECTIONS || list.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(SECTIONS_NOT_ACCEPTED);
    }
    int elements = list.size() + list.stream().mapToInt(this::applications).sum();
    if (elements > MAX_ELEMENTS) {
      throw new IllegalArgumentException(ELEMENTS_NOT_ACCEPTED);
    }
    LayoutModel parent = findSectionsParent(root);
    List<LayoutModel> newSections = new ArrayList<>();
    for (SectionDescription description : list) {
      newSections.add(section(description));
    }
    if (placedHidden.size() < hiddenWindows.size()) {
      throw new IllegalAccessException(PAGE_LAYOUT_HIDDEN);
    }
    if (parent == null) {
      parent = newContainer(SECTIONS_PARENT_TEMPLATE);
      root.setChildren(new ArrayList<>(List.of(parent)));
    }
    parent.setChildren(newSections);
    return root;
  }

  private void index(LayoutModel model, LayoutModel parent) {
    if (StringUtils.isNotBlank(model.getStorageId())) {
      byId.put(model.getStorageId(), model);
      if (parent != null) {
        parentOf.put(model.getStorageId(), parent);
      }
    }
    children(model).forEach(child -> index(child, model));
  }

  /**
   * @return the number of applications the description places, a null
   *         column or cell counted later by its own refusal
   */
  private int applications(SectionDescription section) {
    int count = 0;
    if (section.columnsContent() != null) {
      count += section.columnsContent().stream().filter(Objects::nonNull).mapToInt(List::size).sum();
    }
    if (section.cells() != null) {
      count += (int) section.cells().stream().filter(cell -> cell != null && cell.application() != null).count();
    }
    return count;
  }

  private LayoutModel findSectionsParent(LayoutModel layout) {
    List<LayoutModel> children = children(layout);
    if (children.isEmpty()) {
      return null;
    } else if (isTemplate(children.get(0), SECTIONS_PARENT_TEMPLATE)) {
      return children.get(0);
    } else {
      return findSectionsParent(children.get(0));
    }
  }

  private LayoutModel section(SectionDescription description) throws ObjectNotFoundException {
    SectionType type = description.type();
    if (type == null) {
      throw new IllegalArgumentException(TYPE_NOT_ACCEPTED);
    }
    boolean dynamic = type == SectionType.DYNAMIC;
    String template = dynamic ? LayoutDescriptionService.FLEX_TEMPLATE : LayoutDescriptionService.GRID_TEMPLATE;
    boolean kept = description.sectionId() != null;
    LayoutModel section = kept ? container(description.sectionId(), template, SECTION_NOT_FOUND, SECTION_ID_NOT_ACCEPTED)
                               : newContainer(template);
    List<LayoutModel> storedCells = kept ? children(section).stream()
                                                            .filter(c -> isTemplate(c, LayoutDescriptionService.CELL_TEMPLATE))
                                                            .toList()
                                         : Collections.emptyList();
    List<LayoutModel> cells = dynamic ? dynamicCells(description, section, storedCells) : gridCells(description, section);
    StylingDestination destination = dynamic ? StylingDestination.DYNAMIC_SECTION : StylingDestination.GRID_SECTION;
    applyStyling(section, description.styling(), destination);
    stylingService.applyDisplay(section, description.display(), destination, DISPLAY_NOT_ACCEPTED);
    section.setChildren(new ArrayList<>(cells));
    return section;
  }

  /**
   * One cell per column, each spanning 12 / N as the editor's newSection
   * does; a kept section's stored cells are reused in order when the count is
   * unchanged, so their ids and the windows pinned in them stay
   */
  private List<LayoutModel> dynamicCells(SectionDescription description,
                                         LayoutModel section,
                                         List<LayoutModel> storedCells) throws ObjectNotFoundException {
    if (description.rows() != null) {
      throw new IllegalArgumentException(ROWS_NOT_ACCEPTED);
    } else if (description.cells() != null) {
      throw new IllegalArgumentException(CELLS_NOT_ACCEPTED);
    }
    Integer columns = description.columns();
    List<List<ApplicationDescription>> content = description.columnsContent();
    if (columns == null) {
      if (content == null) {
        throw new IllegalArgumentException(COLUMNS_NOT_ACCEPTED);
      }
      columns = content.size();
    }
    if (!DYNAMIC_COLUMNS.contains(columns)) {
      throw new IllegalArgumentException(COLUMNS_NOT_ACCEPTED);
    } else if (content != null && (content.size() != columns || content.stream().anyMatch(Objects::isNull))) {
      throw new IllegalArgumentException(COLUMNS_CONTENT_NOT_ACCEPTED);
    }
    section.setCssClass(grammar(section.getCssClass(), FLEX_SECTION_CLASS));
    boolean reuse = storedCells.size() == columns;
    int span = GRID_COLUMNS / columns;
    List<LayoutModel> cells = new ArrayList<>();
    for (int i = 0; i < columns; i++) {
      LayoutModel cell = reuse ? storedCells.get(i) : newContainer(LayoutDescriptionService.CELL_TEMPLATE);
      if (reuse && cell.getStorageId() != null) {
        used.add(cell.getStorageId());
      }
      cell.setCssClass(grammar(cell.getCssClass(), cellClass(FLEX_CELL_TOKEN, span, 1)));
      cell.setChildren(new ArrayList<>());
      placeApplications(content == null ? Collections.emptyList() : content.get(i), cell, StylingDestination.APPLICATION);
      restoreHidden(cell);
      cells.add(cell);
    }
    return cells;
  }

  /**
   * The editor's full matrix in reading order: each described cell at its row
   * and column with its spans, an empty cell at every position no cell
   * covers, as newSection and parseMatrix place them
   */
  private List<LayoutModel> gridCells(SectionDescription description, LayoutModel section) throws ObjectNotFoundException {
    if (description.columnsContent() != null) {
      throw new IllegalArgumentException(COLUMNS_CONTENT_NOT_ACCEPTED);
    }
    Integer columns = description.columns();
    Integer rows = description.rows();
    if (columns == null || columns < 1 || columns > GRID_MAX) {
      throw new IllegalArgumentException(COLUMNS_NOT_ACCEPTED);
    } else if (rows == null || rows < 1 || rows > GRID_MAX) {
      throw new IllegalArgumentException(ROWS_NOT_ACCEPTED);
    }
    section.setCssClass(grammar(section.getCssClass(), gridSectionClass(columns, rows)));
    List<CellDescription> list = description.cells() == null ? Collections.emptyList() : description.cells();
    if (list.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(CELLS_NOT_ACCEPTED);
    }
    CellDescription[][] starts = new CellDescription[rows][columns];
    boolean[][] covered = new boolean[rows][columns];
    for (CellDescription cell : list) {
      int rowSpan = span(cell.rowSpan());
      int colSpan = span(cell.columnSpan());
      if (cell.row() < 1 || cell.column() < 1 || rowSpan < 1 || colSpan < 1
          || cell.row() + rowSpan - 1 > rows || cell.column() + colSpan - 1 > columns) {
        throw new IllegalArgumentException(CELLS_NOT_ACCEPTED);
      } else if (starts[cell.row() - 1][cell.column() - 1] != null) {
        throw new IllegalArgumentException(CELL_TAKEN);
      }
      for (int row = cell.row() - 1; row < cell.row() - 1 + rowSpan; row++) {
        for (int col = cell.column() - 1; col < cell.column() - 1 + colSpan; col++) {
          if (covered[row][col]) {
            throw new IllegalArgumentException(CELLS_NOT_ACCEPTED);
          }
          covered[row][col] = true;
        }
      }
      starts[cell.row() - 1][cell.column() - 1] = cell;
    }
    boolean[][] emitted = new boolean[rows][columns];
    List<LayoutModel> cells = new ArrayList<>();
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < columns; col++) {
        if (emitted[row][col]) {
          continue;
        }
        CellDescription described = starts[row][col];
        int rowSpan = described == null ? 1 : span(described.rowSpan());
        int colSpan = described == null ? 1 : span(described.columnSpan());
        LayoutModel cell = described != null && described.cellId() != null
                                                                             ? container(described.cellId(),
                                                                                         LayoutDescriptionService.CELL_TEMPLATE,
                                                                                         CELL_NOT_FOUND,
                                                                                         CELL_ID_NOT_ACCEPTED)
                                                                             : newContainer(LayoutDescriptionService.CELL_TEMPLATE);
        cell.setCssClass(grammar(cell.getCssClass(), cellClass(GRID_CELL_TOKEN, colSpan, rowSpan)));
        cell.setChildren(new ArrayList<>());
        ApplicationDescription application = described == null ? null : described.application();
        if (application != null && hasHiddenWindow(cell)) {
          throw new IllegalArgumentException(CELL_TAKEN);
        }
        placeApplications(application == null ? Collections.emptyList() : List.of(application),
                          cell,
                          StylingDestination.GRID_CELL_APPLICATION);
        restoreHidden(cell);
        for (int r = row; r < row + rowSpan; r++) {
          for (int c = col; c < col + colSpan; c++) {
            emitted[r][c] = true;
          }
        }
        cells.add(cell);
      }
    }
    return cells;
  }

  /**
   * Places the applications in the given order: a kept application keeps its
   * window with its settings, a new one is built from the catalog
   */
  private void placeApplications(List<ApplicationDescription> applications,
                                 LayoutModel cell,
                                 StylingDestination destination) throws ObjectNotFoundException {
    if (applications.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
    }
    for (ApplicationDescription description : applications) {
      if (description.settings() != null || description.widgetCode() != null) {
        throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
      }
      LayoutModel application;
      if (description.applicationId() != null) {
        application = byId.get(description.applicationId());
        if (application == null
            || StringUtils.isNotBlank(application.getTemplate())
            || hiddenWindows.containsKey(description.applicationId())) {
          throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
        } else if (!used.add(description.applicationId())) {
          throw new IllegalArgumentException(APPLICATION_ID_NOT_ACCEPTED);
        }
        detach(application);
      } else if (description.instanceId() == null && StringUtils.isBlank(description.contentId())) {
        throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
      } else {
        application = newApplication.apply(description);
      }
      if (description.title() != null) {
        String title = StringUtils.trimToNull(description.title());
        if (title == null || title.length() > TITLE_MAX_LENGTH) {
          throw new IllegalArgumentException(TITLE_NOT_ACCEPTED);
        }
        application.setTitle(title);
      }
      applyStyling(application, description.styling(), destination);
      stylingService.applyDisplay(application, description.display(), destination, DISPLAY_NOT_ACCEPTED);
      cell.getChildren().add(application);
    }
  }

  /**
   * A kept window leaves its stored container, so a window held outside the
   * sections parent, which the call can address by its id, is moved under
   * the section rather than written twice; a rebuilt cell's children are
   * reset before, so there it is a no-op
   */
  private void detach(LayoutModel application) {
    LayoutModel storedParent = parentOf.get(application.getStorageId());
    if (storedParent != null && children(storedParent).contains(application)) {
      storedParent.setChildren(children(storedParent).stream()
                                                     .filter(child -> child != application)
                                                     .collect(Collectors.toCollection(ArrayList::new)));
    }
  }

  /**
   * Puts back, at its stored index, every window the caller cannot see whose
   * stored cell is the given one
   */
  private void restoreHidden(LayoutModel cell) {
    hiddenWindows.forEach((storageId, index) -> {
      if (parentOf.get(storageId) == cell && placedHidden.add(storageId)) {
        List<LayoutModel> children = cell.getChildren();
        children.add(Math.min(index, children.size()), byId.get(storageId));
      }
    });
  }

  private boolean hasHiddenWindow(LayoutModel cell) {
    return hiddenWindows.keySet().stream().anyMatch(storageId -> parentOf.get(storageId) == cell);
  }

  /**
   * Writes the call's styling merged over the element's stored one, so a
   * block or a value the call leaves null keeps what the element holds
   */
  private void applyStyling(LayoutModel element, StylingDescription styling, StylingDestination destination) {
    StylingDescription merged = LayoutStylingService.merge(storedStyling.apply(element, destination), styling, false);
    stylingService.applyStyling(element, merged, destination, false, false);
  }

  private LayoutModel container(String storageId, String template, String notFound, String notAccepted) throws ObjectNotFoundException {
    LayoutModel container = byId.get(storageId);
    if (container == null) {
      throw new ObjectNotFoundException(notFound);
    } else if (!isTemplate(container, template) || !used.add(storageId)) {
      throw new IllegalArgumentException(notAccepted);
    }
    return container;
  }

  private LayoutModel newContainer(String template) {
    LayoutModel container = new LayoutModel();
    container.setTemplate(template);
    container.setChildren(new ArrayList<>());
    return container;
  }

  private int span(Integer value) {
    return value == null ? 1 : value;
  }

  /**
   * @return the grammar tokens followed by whatever else the stored class
   *         holds (its display and style tokens), the stored grammar dropped
   */
  private String grammar(String storedCssClass, String tokens) {
    String rest = LayoutStylingService.strip(storedCssClass, GRAMMAR_TOKENS);
    return rest == null ? tokens : tokens + " " + rest;
  }

  private String gridSectionClass(int columns, int rows) {
    StringBuilder cssClass = new StringBuilder(GRID_SECTION_CLASS);
    appendBreakpoints(cssClass, LayoutDescriptionService.GRID_COLUMNS_PREFIX, columns);
    appendBreakpoints(cssClass, LayoutDescriptionService.GRID_ROWS_PREFIX, rows);
    return cssClass.toString();
  }

  private String cellClass(String token, int colSpan, int rowSpan) {
    StringBuilder cssClass = new StringBuilder(token);
    appendBreakpoints(cssClass, LayoutDescriptionService.CELL_COLSPAN_PREFIX, colSpan);
    appendBreakpoints(cssClass, LayoutDescriptionService.CELL_ROWSPAN_PREFIX, rowSpan);
    return cssClass.toString();
  }

  private void appendBreakpoints(StringBuilder cssClass, String prefix, int value) {
    cssClass.append(' ').append(prefix);
    for (String breakpoint : BREAKPOINTS) {
      cssClass.append(' ').append(prefix).append('-').append(breakpoint).append('-').append(value);
    }
  }

  private boolean isTemplate(LayoutModel model, String template) {
    return model != null && StringUtils.equals(model.getTemplate(), template);
  }

  private List<LayoutModel> children(LayoutModel model) {
    return model == null || model.getChildren() == null ? Collections.emptyList() : model.getChildren();
  }
}
