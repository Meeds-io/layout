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
package io.meeds.layout.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteContainerOptions;
import io.meeds.layout.mcp.model.SiteLayoutUpdate;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.service.LayoutStylingService;

/**
 * Rebuilds a site layout tree from a set_site_layout call, for one call: the
 * root, the middle body, its center and the page body are kept from the
 * stored tree, the four sidebar slots are always written (empty when unused)
 * and the banners are written in the order given, as the site layout editor's
 * parseSite and newSite shape them. A container addressed by its id keeps its
 * stored element (a banner its cells), an application addressed by its id
 * keeps its window, and a window the caller cannot see, which the call can
 * neither address nor move, stays in its container at its index when that
 * container is kept, the call being refused otherwise.
 */
public class SiteLayoutAssembler {

  public static final String                                SITE_TEMPLATE                   =
                                                                          "system:/groovy/portal/webui/container/UISiteLayout.gtmpl";

  public static final String                                SITE_MIDDLE_TEMPLATE            = "SiteMiddleBody";

  public static final String                                SITE_MIDDLE_CENTER_TEMPLATE     = "SiteMiddleCenterBody";

  public static final String                                SIDEBAR_TEMPLATE                = "Sidebar";

  public static final String                                SIDEBAR_CELL_TEMPLATE           = "SidebarCell";

  public static final String                                BANNER_TEMPLATE                 = "Banner";

  public static final String                                BANNER_CELL_TEMPLATE            = "BannerCell";

  public static final String                                STICKY_SECTION_TOKEN            = "layout-sticky-section";

  public static final String                                BANNER_TOP_TOKEN                = "layout-banner-top-section";

  public static final String                                BANNER_BOTTOM_TOKEN             = "layout-banner-bottom-section";

  public static final String                                SITE_LAYOUT_NOT_EDITABLE        = "cannot be updated: site layout";

  public static final String                                SITE_LAYOUT_HIDDEN              =
                                                                               "not allowed: site layout holds elements you cannot see";

  public static final String                                CONTAINER_NOT_FOUND             = "does not exist: container";

  public static final String                                CONTAINER_ID_NOT_ACCEPTED       = "value is not accepted for container_id";

  public static final String                                APPLICATION_NOT_FOUND           = "does not exist: application";

  public static final String                                APPLICATION_ID_NOT_ACCEPTED     = "value is not accepted for application_id";

  public static final String                                APPLICATIONS_NOT_ACCEPTED       = "value is not accepted for applications";

  public static final String                                TITLE_NOT_ACCEPTED              = "value is not accepted for title";

  public static final String                                TOPBARS_NOT_ACCEPTED            = "value is not accepted for topbars";

  public static final String                                BOTTOM_BARS_NOT_ACCEPTED        = "value is not accepted for bottom_bars";

  public static final String                                HEIGHT_NOT_ACCEPTED             = "value is not accepted for options.height";

  public static final String                                WIDTH_NOT_ACCEPTED              = "value is not accepted for options.width";

  public static final String                                FULL_HEIGHT_NOT_ACCEPTED        =
                                                                                       "value is not accepted for options.full_height";

  public static final String                                DISPLAY_NOT_ACCEPTED            = "value is not accepted for options.display";

  public static final String                                APPLICATION_DISPLAY_NOT_ACCEPTED = "value is not accepted for display";

  public static final int                                   DEFAULT_BANNER_HEIGHT           = 57;

  public static final int                                   BANNER_HEIGHT_MAX               = 1000;

  public static final int                                   DEFAULT_SIDEBAR_WIDTH           = 310;

  public static final int                                   SIDEBAR_WIDTH_MIN               = 50;

  public static final int                                   SIDEBAR_WIDTH_MAX               = 1000;

  public static final int                                   MAX_BANNERS                     = 10;

  public static final int                                   MAX_APPLICATIONS                = 50;

  public static final int                                   TITLE_MAX_LENGTH                = 200;

  private static final Pattern                              BANNER_TOKENS                   =
                                                                          Pattern.compile("(^| )(layout-banner-top-section|layout-banner-bottom-section|layout-sticky-section)(?= |$)");

  private static final Pattern                              STICKY_TOKEN                    =
                                                                         Pattern.compile("(^| )layout-sticky-section(?= |$)");

  private final LayoutModel                                 root;

  private final Map<String, Integer>                        hiddenWindows;

  private final LayoutStylingService                        stylingService;

  private final Function<ApplicationDescription, LayoutModel> newApplication;

  private final Map<String, LayoutModel>                    byId                            = new HashMap<>();

  private final Map<String, LayoutModel>                    parentOf                        = new HashMap<>();

  private final Set<String>                                 used                            = new HashSet<>();

  private final Set<String>                                 placedHidden                    = new HashSet<>();

  /**
   * @param root           the stored tree, wrapped
   * @param hiddenWindows  the storage id of each window the caller cannot
   *                         see, with its index in its container
   * @param stylingService the styling writer
   * @param newApplication builds the window of an application the call adds,
   *                         from its instance or content id
   */
  public SiteLayoutAssembler(LayoutModel root,
                             Map<String, Integer> hiddenWindows,
                             LayoutStylingService stylingService,
                             Function<ApplicationDescription, LayoutModel> newApplication) {
    this.root = root;
    this.hiddenWindows = hiddenWindows;
    this.stylingService = stylingService;
    this.newApplication = newApplication;
  }

  public LayoutModel assemble(SiteLayoutUpdate update) throws ObjectNotFoundException, IllegalAccessException {
    index(root, null);
    List<LayoutModel> rootChildren = children(root);
    LayoutModel middle = single(rootChildren, SITE_MIDDLE_TEMPLATE, SIDEBAR_TEMPLATE);
    List<LayoutModel> middleChildren = children(middle);
    LayoutModel center = single(middleChildren, SITE_MIDDLE_CENTER_TEMPLATE, BANNER_TEMPLATE);
    List<LayoutModel> centerChildren = children(center);
    LayoutModel pageBody = single(centerChildren, EntityMapper.PAGE_BODY_TEMPLATE, SIDEBAR_TEMPLATE);
    List<SiteContainerDescription> topbars = bounded(update.topbars(), TOPBARS_NOT_ACCEPTED);
    List<SiteContainerDescription> bottomBars = bounded(update.bottomBars(), BOTTOM_BARS_NOT_ACCEPTED);

    boolean leftOuter = outer(update.leftSidebar());
    boolean rightOuter = outer(update.rightSidebar());
    LayoutModel outerLeft = sidebar(slot(rootChildren, middle, true), leftOuter ? update.leftSidebar() : null);
    LayoutModel outerRight = sidebar(slot(rootChildren, middle, false), rightOuter ? update.rightSidebar() : null);
    LayoutModel innerLeft = sidebar(slot(centerChildren, pageBody, true), leftOuter ? null : update.leftSidebar());
    LayoutModel innerRight = sidebar(slot(centerChildren, pageBody, false), rightOuter ? null : update.rightSidebar());

    List<LayoutModel> newMiddleChildren = new ArrayList<>();
    for (SiteContainerDescription topbar : topbars) {
      newMiddleChildren.add(banner(topbar, true));
    }
    newMiddleChildren.add(center);
    for (SiteContainerDescription bottomBar : bottomBars) {
      newMiddleChildren.add(banner(bottomBar, false));
    }
    if (placedHidden.size() < hiddenWindows.size()) {
      throw new IllegalAccessException(SITE_LAYOUT_HIDDEN);
    }
    center.setChildren(new ArrayList<>(List.of(innerLeft, pageBody, innerRight)));
    middle.setChildren(newMiddleChildren);
    root.setChildren(new ArrayList<>(List.of(outerLeft, middle, outerRight)));
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
   * @return the one child of the given template, every other child being of
   *         the sibling template and at most one on each side of it
   */
  private LayoutModel single(List<LayoutModel> children, String template, String siblingTemplate) {
    List<LayoutModel> matches = children.stream().filter(c -> isTemplate(c, template)).toList();
    boolean siblings = children.stream().allMatch(c -> isTemplate(c, template) || isTemplate(c, siblingTemplate));
    if (matches.size() != 1 || !siblings) {
      throw new IllegalArgumentException(SITE_LAYOUT_NOT_EDITABLE);
    }
    int index = children.indexOf(matches.get(0));
    boolean sidebars = SIDEBAR_TEMPLATE.equals(siblingTemplate);
    if (sidebars && (index > 1 || children.size() - index > 2)) {
      throw new IllegalArgumentException(SITE_LAYOUT_NOT_EDITABLE);
    }
    return matches.get(0);
  }

  private LayoutModel slot(List<LayoutModel> children, LayoutModel anchor, boolean before) {
    int index = children.indexOf(anchor) + (before ? -1 : 1);
    return index >= 0 && index < children.size() ? children.get(index) : null;
  }

  private boolean outer(SiteContainerDescription sidebar) {
    return sidebar != null && (sidebar.options() == null || !Boolean.FALSE.equals(sidebar.options().fullHeight()));
  }

  private List<SiteContainerDescription> bounded(List<SiteContainerDescription> banners, String refusal) {
    List<SiteContainerDescription> list = banners == null ? Collections.emptyList() : banners;
    if (list.size() > MAX_BANNERS || list.stream().anyMatch(b -> b == null)) {
      throw new IllegalArgumentException(refusal);
    }
    return list;
  }

  /**
   * A sidebar slot holds its stored Sidebar when it has one, emptied when the
   * call leaves the slot unused, as the editor's remove does; the description's
   * id names the cell, as the read answers it
   */
  private LayoutModel sidebar(LayoutModel placeholder, SiteContainerDescription description) throws ObjectNotFoundException {
    LayoutModel sidebar = placeholder == null ? newContainer(SIDEBAR_TEMPLATE) : placeholder;
    if (description == null) {
      sidebar.setChildren(new ArrayList<>());
      sidebar.setCssClass(null);
      return sidebar;
    }
    LayoutModel cell = description.containerId() == null ? newContainer(SIDEBAR_CELL_TEMPLATE)
                                                         : container(description.containerId(), SIDEBAR_CELL_TEMPLATE);
    SiteContainerOptions options = options(description);
    boolean sticky = Boolean.TRUE.equals(options.sticky());
    if (options.height() != null) {
      throw new IllegalArgumentException(HEIGHT_NOT_ACCEPTED);
    }
    int width = options.width() == null ? DEFAULT_SIDEBAR_WIDTH : options.width();
    if (width < SIDEBAR_WIDTH_MIN || width > SIDEBAR_WIDTH_MAX) {
      throw new IllegalArgumentException(WIDTH_NOT_ACCEPTED);
    }
    cell.setWidth(String.valueOf(width));
    cell.setChildren(new ArrayList<>());
    String sidebarClass = LayoutStylingService.strip(sidebar.getCssClass(), STICKY_TOKEN);
    sidebar.setCssClass(sticky ? LayoutStylingService.append(sidebarClass, STICKY_SECTION_TOKEN) : sidebarClass);
    stylingService.applyStyling(cell, description.styling(), StylingDestination.SIDEBAR, sticky, false);
    stylingService.applyDisplay(cell, options.display(), StylingDestination.SIDEBAR, DISPLAY_NOT_ACCEPTED);
    placeApplications(description.applications(), List.of(cell));
    sidebar.setChildren(new ArrayList<>(List.of(cell)));
    return sidebar;
  }

  /**
   * A banner keeps its stored cells when addressed by id, a new one has one
   * cell; the position token is written from the banner's side
   */
  private LayoutModel banner(SiteContainerDescription description, boolean top) throws ObjectNotFoundException {
    LayoutModel banner;
    List<LayoutModel> cells;
    if (description.containerId() == null) {
      banner = newContainer(BANNER_TEMPLATE);
      cells = List.of(newContainer(BANNER_CELL_TEMPLATE));
    } else {
      banner = container(description.containerId(), BANNER_TEMPLATE);
      cells = children(banner).stream().filter(c -> isTemplate(c, BANNER_CELL_TEMPLATE)).toList();
      cells.forEach(cell -> cell.setChildren(new ArrayList<>()));
      if (cells.isEmpty()) {
        cells = List.of(newContainer(BANNER_CELL_TEMPLATE));
      }
    }
    SiteContainerOptions options = options(description);
    boolean sticky = Boolean.TRUE.equals(options.sticky());
    if (options.width() != null) {
      throw new IllegalArgumentException(WIDTH_NOT_ACCEPTED);
    } else if (options.fullHeight() != null) {
      throw new IllegalArgumentException(FULL_HEIGHT_NOT_ACCEPTED);
    }
    int height = options.height() == null ? DEFAULT_BANNER_HEIGHT : options.height();
    if (height < 0 || height > BANNER_HEIGHT_MAX) {
      throw new IllegalArgumentException(HEIGHT_NOT_ACCEPTED);
    }
    banner.setHeight(String.valueOf(height));
    String bannerClass = LayoutStylingService.append(LayoutStylingService.strip(banner.getCssClass(), BANNER_TOKENS),
                                                     top ? BANNER_TOP_TOKEN : BANNER_BOTTOM_TOKEN);
    banner.setCssClass(sticky ? LayoutStylingService.append(bannerClass, STICKY_SECTION_TOKEN) : bannerClass);
    stylingService.applyStyling(banner, description.styling(), StylingDestination.BANNER, sticky, top);
    stylingService.applyDisplay(banner, options.display(), StylingDestination.BANNER, DISPLAY_NOT_ACCEPTED);
    placeApplications(description.applications(), cells);
    banner.setChildren(new ArrayList<>(cells));
    return banner;
  }

  /**
   * Places the applications in the given order: a kept application goes back
   * to its stored cell when that cell is one of the container's, a new one
   * follows the application before it; the windows the caller cannot see are
   * then put back at their stored index
   */
  private void placeApplications(List<ApplicationDescription> applications, List<LayoutModel> cells) throws ObjectNotFoundException {
    List<ApplicationDescription> list = applications == null ? Collections.emptyList() : applications;
    if (list.size() > MAX_APPLICATIONS || list.stream().anyMatch(a -> a == null)) {
      throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
    }
    LayoutModel current = cells.get(0);
    for (ApplicationDescription description : list) {
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
        LayoutModel storedCell = parentOf.get(description.applicationId());
        if (storedCell != null && cells.contains(storedCell)) {
          current = storedCell;
        }
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
      stylingService.applyStyling(application, description.styling(), StylingDestination.SITE_CONTAINER_APPLICATION, false, false);
      stylingService.applyDisplay(application,
                                  description.display(),
                                  StylingDestination.SITE_CONTAINER_APPLICATION,
                                  APPLICATION_DISPLAY_NOT_ACCEPTED);
      current.getChildren().add(application);
    }
    hiddenWindows.forEach((storageId, index) -> {
      LayoutModel storedCell = parentOf.get(storageId);
      if (storedCell != null && cells.contains(storedCell) && placedHidden.add(storageId)) {
        List<LayoutModel> children = storedCell.getChildren();
        children.add(Math.min(index, children.size()), byId.get(storageId));
      }
    });
  }

  private LayoutModel container(String storageId, String template) throws ObjectNotFoundException {
    LayoutModel container = byId.get(storageId);
    if (container == null) {
      throw new ObjectNotFoundException(CONTAINER_NOT_FOUND);
    } else if (!isTemplate(container, template) || !used.add(storageId)) {
      throw new IllegalArgumentException(CONTAINER_ID_NOT_ACCEPTED);
    }
    return container;
  }

  private SiteContainerOptions options(SiteContainerDescription description) {
    return description.options() == null ? new SiteContainerOptions(null, null, null, null, null) : description.options();
  }

  private LayoutModel newContainer(String template) {
    LayoutModel container = new LayoutModel();
    container.setTemplate(template);
    container.setChildren(new ArrayList<>());
    return container;
  }

  private boolean isTemplate(LayoutModel model, String template) {
    return model != null && StringUtils.equals(model.getTemplate(), template);
  }

  private List<LayoutModel> children(LayoutModel model) {
    return model == null || model.getChildren() == null ? Collections.emptyList() : model.getChildren();
  }

}
