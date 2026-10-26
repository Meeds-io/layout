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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.ElementStyling;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.util.EntityMapper;

/**
 * Writes the styling of a set_layout_styles call onto a stored page or site
 * tree, for one call, leaving its structure and every element the call does
 * not address as stored. The elements addressed are the ones the read gives
 * an id to and offers styling for: on a page built with the editor's
 * sections, a section and an application of a dynamic column or of a grid
 * cell; on a site, a topbar, a bottom bar, a sidebar by the id of the cell
 * the read answers, and an application of one of them. Each one takes the
 * call's blocks merged over its stored styling, written for its place in the
 * editor's matrix, as the two layout assemblers write them; every other
 * application the caller can see is saved with its generated margin and
 * radius tokens stripped, as the two assemblers save theirs. A grid cell,
 * which the editor styles through its section and its application, is
 * refused, and so is a device visibility; an id the read does not answer, an
 * element the caller cannot see included, does not exist.
 */
public class LayoutStylesAssembler {

  public static final String                                          ELEMENTS_NOT_ACCEPTED   = "value is not accepted for elements";

  public static final String                                          ELEMENT_ID_NOT_ACCEPTED = "value is not accepted for element_id";

  public static final String                                          ELEMENT_NOT_FOUND       = "does not exist: element";

  public static final String                                          DISPLAY_NOT_ACCEPTED    = "value is not accepted for display";

  public static final int                                             MAX_ELEMENTS            = SiteLayoutAssembler.MAX_ELEMENTS;

  private static final Pattern                                        STICKY_TOKEN            =
                                                                                   Pattern.compile("(^| )"
                                                                                       + Pattern.quote(LayoutDescriptionService.STICKY_SECTION_TOKEN)
                                                                                       + "(?= |$)");

  private final LayoutModel                                           root;

  private final Set<String>                                           hiddenIds;

  private final LayoutStylingService                                  stylingService;

  private final BiFunction<LayoutModel, StylingDestination, StylingDescription> storedStyling;

  private final Map<String, LayoutModel>                              targets                 = new HashMap<>();

  private final Map<String, StylingDestination>                       destinations            = new HashMap<>();

  private final Set<String>                                           stickyTargets           = new HashSet<>();

  private final Set<String>                                           plainColorTargets       = new HashSet<>();

  private final Set<String>                                           nonTargets              = new HashSet<>();

  /**
   * @param root           the stored page or site layout, wrapped
   * @param hiddenIds      the storage id of every element the caller cannot
   *                         see, an element under a container they cannot
   *                         see included
   * @param stylingService the styling writer
   * @param storedStyling  reads an element's stored styling as the read
   *                         answers it, the base a call's blocks are merged
   *                         over
   */
  public LayoutStylesAssembler(LayoutModel root,
                               Set<String> hiddenIds,
                               LayoutStylingService stylingService,
                               BiFunction<LayoutModel, StylingDestination, StylingDescription> storedStyling) {
    this.root = root;
    this.hiddenIds = hiddenIds;
    this.stylingService = stylingService;
    this.storedStyling = storedStyling;
  }

  public LayoutModel stylePage(List<ElementStyling> elements) throws ObjectNotFoundException {
    checkBounds(elements);
    indexPage();
    return style(elements);
  }

  public LayoutModel styleSite(List<ElementStyling> elements) throws ObjectNotFoundException {
    checkBounds(elements);
    indexSite();
    return style(elements);
  }

  private void checkBounds(List<ElementStyling> elements) {
    if (elements == null || elements.isEmpty() || elements.size() > MAX_ELEMENTS || elements.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(ELEMENTS_NOT_ACCEPTED);
    }
  }

  private LayoutModel style(List<ElementStyling> elements) throws ObjectNotFoundException {
    Set<String> used = new HashSet<>();
    for (ElementStyling element : elements) {
      String elementId = StringUtils.trimToNull(element.elementId());
      if (elementId == null) {
        throw new IllegalArgumentException(ELEMENT_ID_NOT_ACCEPTED);
      } else if (!targets.containsKey(elementId) && !nonTargets.contains(elementId)) {
        throw new ObjectNotFoundException(ELEMENT_NOT_FOUND);
      } else if (nonTargets.contains(elementId) || !used.add(elementId)) {
        throw new IllegalArgumentException(ELEMENT_ID_NOT_ACCEPTED);
      } else if (element.display() != null) {
        throw new IllegalArgumentException(DISPLAY_NOT_ACCEPTED);
      }
      LayoutModel target = targets.get(elementId);
      StylingDestination destination = destinations.get(elementId);
      boolean sticky = stickyTargets.contains(elementId);
      StylingDescription merged = LayoutStylingService.merge(storedStyling.apply(target, destination), element.styling(), sticky);
      stylingService.applyStyling(target, merged, destination, sticky, plainColorTargets.contains(elementId));
    }
    targets.forEach((storageId, target) -> {
      StylingDestination destination = destinations.get(storageId);
      if (destination.isApplication() && !used.contains(storageId)) {
        stylingService.applyStyling(target, null, destination, false, false);
      }
    });
    return root;
  }

  /**
   * The sections the read describes: the children of the sections parent,
   * when every one of them is a section of the editor's kind; of their
   * cells, only a grid cell has an id in the read
   */
  private void indexPage() {
    LayoutModel parent = findSectionsParent(root);
    List<LayoutModel> sections = children(parent);
    if (sections.stream()
                .anyMatch(s -> !isTemplate(s, LayoutDescriptionService.FLEX_TEMPLATE)
                               && !isTemplate(s, LayoutDescriptionService.GRID_TEMPLATE))) {
      return;
    }
    for (LayoutModel section : sections) {
      boolean dynamic = isTemplate(section, LayoutDescriptionService.FLEX_TEMPLATE);
      target(section, dynamic ? StylingDestination.DYNAMIC_SECTION : StylingDestination.GRID_SECTION, false, false);
      for (LayoutModel cell : children(section)) {
        if (isTemplate(cell, LayoutDescriptionService.CELL_TEMPLATE)) {
          if (!dynamic) {
            nonTarget(cell);
          }
          applications(cell, dynamic ? StylingDestination.APPLICATION : StylingDestination.GRID_CELL_APPLICATION);
        }
      }
    }
  }

  /**
   * The containers the read describes, found as it finds them: the banners
   * of the middle body, a topbar before the center body and a bottom bar
   * after it, and on each side the sidebar holding a cell, the outer one
   * first; only a topbar holding the sticky token is sticky, and only a
   * topbar takes a plain colour alone, as set_site_layout writes them
   */
  private void indexSite() {
    List<LayoutModel> rootChildren = children(root);
    LayoutModel middle = rootChildren.stream()
                                     .filter(c -> isTemplate(c, LayoutDescriptionService.SITE_MIDDLE_TEMPLATE))
                                     .findFirst()
                                     .orElse(null);
    List<LayoutModel> middleChildren = children(middle);
    int centerIndex = -1;
    for (int i = 0; i < middleChildren.size(); i++) {
      if (isTemplate(middleChildren.get(i), LayoutDescriptionService.SITE_MIDDLE_CENTER_TEMPLATE)) {
        centerIndex = i;
      }
    }
    for (int i = 0; i < middleChildren.size(); i++) {
      LayoutModel banner = middleChildren.get(i);
      if (isTemplate(banner, LayoutDescriptionService.BANNER_TEMPLATE)) {
        boolean top = centerIndex < 0 || i < centerIndex;
        target(banner, StylingDestination.BANNER, top && hasToken(banner.getCssClass(), STICKY_TOKEN), top);
        for (LayoutModel cell : children(banner)) {
          if (isTemplate(cell, LayoutDescriptionService.BANNER_CELL_TEMPLATE)) {
            applications(cell, StylingDestination.SITE_CONTAINER_APPLICATION);
          }
        }
      }
    }
    List<LayoutModel> centerChildren = centerIndex < 0 ? Collections.emptyList() : children(middleChildren.get(centerIndex));
    sidebar(sidebarAt(rootChildren, 0), sidebarAt(centerChildren, 0));
    sidebar(sidebarAt(rootChildren, 2), sidebarAt(centerChildren, 2));
  }

  private void sidebar(LayoutModel outer, LayoutModel inner) {
    LayoutModel outerCell = sidebarCell(outer);
    LayoutModel sidebar = outerCell != null ? outer : inner;
    LayoutModel cell = outerCell != null ? outerCell : sidebarCell(inner);
    if (cell != null) {
      target(cell, StylingDestination.SIDEBAR, hasToken(sidebar.getCssClass(), STICKY_TOKEN), false);
      applications(cell, StylingDestination.SITE_CONTAINER_APPLICATION);
    }
  }

  private void applications(LayoutModel cell, StylingDestination destination) {
    children(cell).stream()
                  .filter(c -> StringUtils.isBlank(c.getTemplate()))
                  .forEach(application -> target(application, destination, false, false));
  }

  private void target(LayoutModel element, StylingDestination destination, boolean sticky, boolean plainColorOnly) {
    String storageId = element.getStorageId();
    if (StringUtils.isBlank(storageId)) {
      return;
    }
    targets.put(storageId, element);
    destinations.put(storageId, destination);
    if (sticky) {
      stickyTargets.add(storageId);
    }
    if (plainColorOnly) {
      plainColorTargets.add(storageId);
    }
  }

  private void nonTarget(LayoutModel element) {
    if (StringUtils.isNotBlank(element.getStorageId())) {
      nonTargets.add(element.getStorageId());
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

  private boolean hasToken(String cssClass, Pattern token) {
    return StringUtils.isNotBlank(cssClass) && token.matcher(cssClass).find();
  }

  private boolean isTemplate(LayoutModel model, String template) {
    return model != null && StringUtils.equals(model.getTemplate(), template);
  }

  /**
   * The children the caller can see: the reads walk the tree filtered by
   * access permission, so the targets, the first child the descent to the
   * sections parent follows and the positions of the site's containers are
   * found among these only
   */
  private List<LayoutModel> children(LayoutModel model) {
    return model == null || model.getChildren() == null ? Collections.emptyList()
                                                        : model.getChildren()
                                                               .stream()
                                                               .filter(c -> !hiddenIds.contains(c.getStorageId()))
                                                               .toList();
  }
}
