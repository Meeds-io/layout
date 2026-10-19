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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.PageType;
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

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.NavigationItemType;
import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.BackgroundBlock;
import io.meeds.layout.mcp.model.BackgroundImageBlock;
import io.meeds.layout.mcp.model.BorderBlock;
import io.meeds.layout.mcp.model.CellDescription;
import io.meeds.layout.mcp.model.CornersBlock;
import io.meeds.layout.mcp.model.HeightBlock;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.MarginsBlock;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.ScheduleDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SidesBlock;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteContainerOptions;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.mcp.model.TextBackgroundBlock;
import io.meeds.layout.mcp.model.TextBlock;
import io.meeds.layout.mcp.model.TextRoleBlock;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.util.EntityMapper;

/**
 * Converts a page or site container tree into the description the MCP tools
 * return. The section grammar lives in the editor's cssClass tokens only
 * (FlexContainer/GridContainer over CellContainer, grid-cols/grid-rows and
 * the cell spans, a missing token reading as 1); style values are read from
 * the model's style fields, never parsed back from the generated tokens.
 */
@Service
public class LayoutDescriptionService {

  public static final String     FLEX_TEMPLATE               = "FlexContainer";

  public static final String     GRID_TEMPLATE               = "GridContainer";

  public static final String     CELL_TEMPLATE               = "CellContainer";

  public static final String     SITE_MIDDLE_TEMPLATE        = "SiteMiddleBody";

  public static final String     SITE_MIDDLE_CENTER_TEMPLATE = "SiteMiddleCenterBody";

  public static final String     SIDEBAR_TEMPLATE            = "Sidebar";

  public static final String     SIDEBAR_CELL_TEMPLATE       = "SidebarCell";

  public static final String     BANNER_TEMPLATE             = "Banner";

  public static final String     BANNER_CELL_TEMPLATE        = "BannerCell";

  public static final String     HIDE_MOBILE_TOKEN           = "hidden-sm-and-down";

  public static final String     HIDE_DESKTOP_TOKEN          = "hidden-md-and-up";

  public static final String     STICKY_SECTION_TOKEN        = "layout-sticky-section";

  public static final String     GRID_COLUMNS_PREFIX         = "grid-cols";

  public static final String     GRID_ROWS_PREFIX            = "grid-rows";

  public static final String     CELL_COLSPAN_PREFIX         = "grid-cell-colspan";

  public static final String     CELL_ROWSPAN_PREFIX         = "grid-cell-rowspan";

  public static final String     CURRENT_BREAKPOINT          = "xl";

  private static final String    TO_BOTTOM                   = "to_bottom";

  private static final String    TO_RIGHT                    = "to_right";

  private static final Pattern   BG_MARGIN_TOKEN             =
                                                 Pattern.compile("(^| )layout-bg-margin-(top|right|bottom|left)-(\\d+)(?= |$)");

  private static final Pattern   BG_RADIUS_TOKEN             =
                                                 Pattern.compile("(^| )layout-bg-radius-(tl|tr|br|bl)-(\\d+)(?= |$)");

  private static final Pattern   HEX_COLOR                   = Pattern.compile("#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?");

  private static final Pattern   PERCENT                     = Pattern.compile("(\\d{1,3})%");

  private static final Pattern   CONIC_CORNER                =
                                              Pattern.compile("at (top|bottom) (left|right)");

  private static final Pattern   PIXELS                      = Pattern.compile("-?\\d+");

  public static final int        SITE_MATCH_LIMIT            = 50;

  private static final int       SITE_NAME_PAGE              = 200;

  private static final Pattern   CONIC_STOP                  = Pattern.compile("^(#[0-9a-fA-F]{6,8})\\s+(-?\\d+(?:\\.\\d+)?)deg$");

  private static final Map<String, Pattern> TOKEN_PATTERNS   = new ConcurrentHashMap<>();

  private static final Map<String, Pattern> BREAKPOINT_PATTERNS = new ConcurrentHashMap<>();

  @Autowired
  private LayoutService          layoutService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private LayoutAclService       aclService;

  @Autowired
  private NavigationService      navigationService;

  @Autowired
  private NavigationLayoutService navigationLayoutService;

  @Autowired
  private LayoutDraftService     layoutDraftService;

  /**
   * Drops, recursively, every container and application the user has no
   * access permission on. Only page permissions are considered by the
   * platform; an element-level permission is legacy data this filter still
   * honours, as the page read does.
   */
  public ModelObject filterAccessible(ModelObject modelObject, String username) {
    if (modelObject == null || !aclService.hasAccessPermission(modelObject, username)) {
      return null;
    } else if (modelObject instanceof Container container && CollectionUtils.isNotEmpty(container.getChildren())) {
      container.setChildren(container.getChildren()
                                     .stream()
                                     .map(c -> filterAccessible(c, username))
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toCollection(ArrayList::new)));
    }
    return modelObject;
  }

  public PageDescription toPageDescription(ModelObject pageTree, String pageRef, LayoutVersion version) {
    Map<String, String> contentIds = collectContentIds(pageTree);
    LayoutModel page = new LayoutModel(pageTree);
    LayoutModel sectionsParent = findSectionsParent(page);
    List<LayoutModel> sections = sectionsParent == null ? null : children(sectionsParent);
    boolean compatible = sections != null
                         && sections.stream().allMatch(s -> isTemplate(s, FLEX_TEMPLATE) || isTemplate(s, GRID_TEMPLATE));
    if (!compatible) {
      return new PageDescription(pageRef, version, Collections.emptyList(), false);
    }
    return new PageDescription(pageRef,
                               version,
                               sections.stream().map(s -> toSection(s, contentIds)).toList(),
                               null);
  }

  /**
   * @param  site        the live site, for its properties
   * @param  layout      the site layout to describe, the draft's when the user
   *                       has one, already filtered by access permission
   * @param  label       the site label resolved in the user's language
   * @param  description the site description resolved in the user's language
   * @param  draft       whether the layout is the user's draft
   * @return the site description
   */
  public SiteDescription toSiteDescription(PortalConfig site, // NOSONAR
                                           ModelObject layout,
                                           String label,
                                           String description,
                                           boolean draft) {
    Map<String, String> contentIds = collectContentIds(layout);
    LayoutModel root = layout == null ? new LayoutModel() : new LayoutModel(layout);
    List<LayoutModel> children = children(root);
    LayoutModel outerLeft = sidebarAt(children, 0);
    LayoutModel outerRight = sidebarAt(children, 2);
    LayoutModel middle = children.stream().filter(c -> isTemplate(c, SITE_MIDDLE_TEMPLATE)).findFirst().orElse(null);
    List<LayoutModel> middleChildren = children(middle);
    int centerIndex = -1;
    for (int i = 0; i < middleChildren.size(); i++) {
      if (isTemplate(middleChildren.get(i), SITE_MIDDLE_CENTER_TEMPLATE)) {
        centerIndex = i;
      }
    }
    List<SiteContainerDescription> topbars = new ArrayList<>();
    List<SiteContainerDescription> bottomBars = new ArrayList<>();
    for (int i = 0; i < middleChildren.size(); i++) {
      LayoutModel child = middleChildren.get(i);
      if (isTemplate(child, BANNER_TEMPLATE)) {
        (centerIndex < 0 || i < centerIndex ? topbars : bottomBars).add(toBanner(child, contentIds));
      }
    }
    List<LayoutModel> centerChildren = centerIndex < 0 ? Collections.emptyList() : children(middleChildren.get(centerIndex));
    LayoutModel innerLeft = sidebarAt(centerChildren, 0);
    LayoutModel innerRight = sidebarAt(centerChildren, 2);
    return new SiteDescription(site.getId(),
                               site.getType(),
                               site.getName(),
                               label,
                               description,
                               site.getBannerFileId() != 0,
                               site.isDisplayed(),
                               site.getAccessPermissions() == null ? null : List.of(site.getAccessPermissions()),
                               site.getEditPermission(),
                               draft,
                               toSidebar(outerLeft, innerLeft, contentIds),
                               toSidebar(outerRight, innerRight, contentIds),
                               topbars,
                               bottomBars);
  }

  /**
   * @param  username the reader
   * @param  keyword  null for the portal sites alone, as the sites
   *                    administration lists them; otherwise the portal sites
   *                    whose name or label contains it, then the group sites
   *                    whose own name (for a space, the group name it was
   *                    created with, which a rename leaves as is) contains it
   * @return the sites the user can view, the portal sites by display order
   *         then label, sorted here because the portal's site query is
   *         cached by its predicates alone, the sort of its first caller
   *         serving every later one
   */
  public List<SiteSummary> describeSites(String username, String keyword) {
    String needle = StringUtils.lowerCase(StringUtils.trimToNull(keyword));
    SiteFilter filter = new SiteFilter();
    filter.setSiteType(SiteType.PORTAL);
    filter.setExcludeSpaceSites(true);
    List<SiteSummary> sites = new ArrayList<>(layoutService.getSites(filter)
                                                           .stream()
                                                           .filter(site -> needle == null || matches(site, needle))
                                                           .filter(site -> aclService.canViewSite(site.getSiteKey(), username))
                                                           .sorted(Comparator.comparingInt(PortalConfig::getDisplayOrder)
                                                                             .thenComparing(site -> StringUtils.defaultIfBlank(site.getLabel(),
                                                                                                                             site.getName()),
                                                                                            String.CASE_INSENSITIVE_ORDER))
                                                           .map(this::toSiteSummary)
                                                           .toList());
    if (needle != null) {
      sites.addAll(describeGroupSites(username, needle));
    }
    return sites;
  }

  /**
   * Group sites are matched on the last segment of their name (a space site
   * is named /spaces/&lt;name at creation&gt;, whatever the space is called
   * since) before any is loaded, and at most
   * SITE_MATCH_LIMIT of them are loaded, viewable or not, so that no listing
   * reads every space's layout. The portal's SPACE and GROUP name listings
   * overlap, so a name is considered once.
   */
  private List<SiteSummary> describeGroupSites(String username, String needle) {
    List<SiteSummary> sites = new ArrayList<>();
    Set<String> considered = new HashSet<>();
    int loaded = 0;
    for (SiteType namesType : new SiteType[] { SiteType.SPACE, SiteType.GROUP }) {
      int offset = 0;
      List<String> names;
      do {
        names = layoutService.getSiteNames(namesType, offset, SITE_NAME_PAGE);
        for (String name : names) {
          if (!considered.add(name) || !StringUtils.containsIgnoreCase(StringUtils.substringAfterLast("/" + name, "/"), needle)) {
            continue;
          }
          if (loaded >= SITE_MATCH_LIMIT) {
            return sites;
          }
          loaded++;
          PortalConfig site = layoutService.getPortalConfig(SiteKey.group(name));
          if (site != null && aclService.canViewSite(site.getSiteKey(), username)) {
            sites.add(toSiteSummary(site));
          }
        }
        offset += names.size();
      } while (names.size() == SITE_NAME_PAGE);
    }
    return sites;
  }

  private SiteSummary toSiteSummary(PortalConfig site) {
    return new SiteSummary(site.getId(),
                           site.getType(),
                           site.getName(),
                           StringUtils.defaultIfBlank(site.getLabel(), site.getName()),
                           site.getDescription(),
                           site.isDisplayed());
  }

  private boolean matches(PortalConfig site, String needle) {
    return StringUtils.containsIgnoreCase(site.getName(), needle) || StringUtils.containsIgnoreCase(site.getLabel(), needle);
  }

  /**
   * @param  siteKey       the site whose navigation is read; the caller has
   *                         checked it can be viewed
   * @param  includeHidden whether the hidden and system items are wanted;
   *                         they are returned only to a user who can edit the
   *                         site's navigation
   * @param  username      the reader
   * @return the navigation tree the user can view, draft items excluded
   */
  public List<NavigationItemDescription> describeNavigation(SiteKey siteKey, boolean includeHidden, String username) {
    boolean withHidden = includeHidden && aclService.canEditNavigation(siteKey, username);
    NodeContext<NodeContext<Object>> root = navigationService.loadNode(siteKey);
    return root == null ? Collections.emptyList() : toNavigationItems(root, siteKey, withHidden, username);
  }

  /**
   * @param  categoryId a category, null for every category
   * @param  keyword    a text the name or description must contain, null for
   *                      no filter
   * @param  username   the reader
   * @param  locale     the language of the names
   * @return the enabled catalog instances the user may place
   */
  public List<LayoutApplicationEntry> describeApplications(Long categoryId, String keyword, String username, Locale locale) {
    Map<Long, String> categories = portletInstanceService.getPortletInstanceCategories(username, locale, true)
                                                         .stream()
                                                         .collect(Collectors.toMap(PortletInstanceCategory::getId,
                                                                                   c -> StringUtils.defaultString(c.getName()),
                                                                                   (a, b) -> a));
    String needle = StringUtils.lowerCase(StringUtils.trimToNull(keyword));
    return portletInstanceService.getPortletInstances(categoryId == null ? 0 : categoryId, username, locale, true)
                                 .stream()
                                 .filter(instance -> !instance.isDisabled())
                                 .filter(instance -> needle == null || matches(instance, needle))
                                 .map(instance -> new LayoutApplicationEntry(instance.getId(),
                                                                             instance.getContentId(),
                                                                             instance.getName(),
                                                                             instance.getDescription(),
                                                                             categories.get(instance.getCategoryId()),
                                                                             false,
                                                                             null))
                                 .toList();
  }

  public DisplayMode toDisplay(String cssClass) {
    if (hasToken(cssClass, HIDE_DESKTOP_TOKEN)) {
      return DisplayMode.HIDE_DESKTOP;
    } else if (hasToken(cssClass, HIDE_MOBILE_TOKEN)) {
      return DisplayMode.HIDE_MOBILE;
    } else {
      return DisplayMode.BOTH;
    }
  }

  /**
   * @param  cssClass the container's class tokens
   * @param  prefix   grid-cols, grid-rows, grid-cell-colspan or
   *                    grid-cell-rowspan
   * @return the value of the token at the editor's current breakpoint, 1 when
   *         the token is absent, as the editor's parser reads it
   */
  public int breakpointValue(String cssClass, String prefix) {
    if (StringUtils.isBlank(cssClass)) {
      return 1;
    }
    Pattern pattern = BREAKPOINT_PATTERNS.computeIfAbsent(prefix,
                                                          p -> Pattern.compile("(^| )" + Pattern.quote(p) + "-"
                                                              + CURRENT_BREAKPOINT + "-(\\d{1,2})(?= |$)"));
    Matcher matcher = pattern.matcher(cssClass);
    return matcher.find() ? Integer.parseInt(matcher.group(2)) : 1;
  }

  public StylingDescription toStyling(LayoutModel model, StylingDestination destination) {
    return new StylingDescription(toMargins(model),
                                  destination.hasBorder() ? toBorder(model) : null,
                                  destination.hasCorners() ? toCorners(model) : null,
                                  toBackground(model),
                                  destination.hasText() ? toText(model) : null,
                                  destination.hasHeight() ? toHeight(model) : null);
  }

  private SectionDescription toSection(LayoutModel section, Map<String, String> contentIds) {
    List<LayoutModel> cells = children(section).stream().filter(c -> isTemplate(c, CELL_TEMPLATE)).toList();
    if (isTemplate(section, FLEX_TEMPLATE)) {
      List<List<ApplicationDescription>> columns = cells.stream()
                                                        .map(cell -> toApplications(cell, contentIds, StylingDestination.APPLICATION))
                                                        .toList();
      return new SectionDescription(section.getStorageId(),
                                    SectionType.DYNAMIC,
                                    columns.size(),
                                    null,
                                    columns,
                                    null,
                                    toStyling(section, StylingDestination.DYNAMIC_SECTION),
                                    toDisplay(section.getCssClass()));
    } else {
      int columns = breakpointValue(section.getCssClass(), GRID_COLUMNS_PREFIX);
      int rows = breakpointValue(section.getCssClass(), GRID_ROWS_PREFIX);
      return new SectionDescription(section.getStorageId(),
                                    SectionType.GRID,
                                    columns,
                                    rows,
                                    null,
                                    toGridCells(cells, rows, columns, contentIds),
                                    toStyling(section, StylingDestination.GRID_SECTION),
                                    null);
    }
  }

  /**
   * Places the cells as the editor does: in reading order, each cell taking
   * the first free position, its spans marking the positions it covers.
   */
  private List<CellDescription> toGridCells(List<LayoutModel> cells, int rows, int columns, Map<String, String> contentIds) {
    boolean[][] taken = new boolean[rows][columns];
    List<CellDescription> result = new ArrayList<>();
    int rowIndex = 0;
    int colIndex = 0;
    for (LayoutModel cell : cells) {
      int colSpan = breakpointValue(cell.getCssClass(), CELL_COLSPAN_PREFIX);
      int rowSpan = breakpointValue(cell.getCssClass(), CELL_ROWSPAN_PREFIX);
      while (rowIndex < rows && taken[rowIndex][colIndex]) {
        colIndex++;
        if (colIndex >= columns) {
          colIndex = 0;
          rowIndex++;
        }
      }
      for (int row = rowIndex; row < Math.min(rows, rowIndex + rowSpan); row++) {
        for (int col = colIndex; col < Math.min(columns, colIndex + colSpan); col++) {
          taken[row][col] = true;
        }
      }
      List<ApplicationDescription> applications = toApplications(cell, contentIds, StylingDestination.GRID_CELL_APPLICATION);
      result.add(new CellDescription(cell.getStorageId(),
                                     rowIndex + 1,
                                     colIndex + 1,
                                     rowSpan == 1 ? null : rowSpan,
                                     colSpan == 1 ? null : colSpan,
                                     applications.isEmpty() ? null : applications.get(0)));
      colIndex += colSpan;
      if (colIndex >= columns) {
        colIndex = 0;
        rowIndex++;
      }
    }
    return result;
  }

  private SiteContainerDescription toBanner(LayoutModel banner, Map<String, String> contentIds) {
    List<ApplicationDescription> applications = children(banner).stream()
                                                                .filter(c -> isTemplate(c, BANNER_CELL_TEMPLATE))
                                                                .flatMap(cell -> toApplications(cell,
                                                                                                contentIds,
                                                                                                StylingDestination.SITE_CONTAINER_APPLICATION).stream())
                                                                .toList();
    SiteContainerOptions options = new SiteContainerOptions(parsePixels(banner.getHeight()),
                                                            null,
                                                            hasToken(banner.getCssClass(), STICKY_SECTION_TOKEN),
                                                            hasToken(banner.getCssClass(), HIDE_MOBILE_TOKEN) ? DisplayMode.HIDE_MOBILE
                                                                                                                : DisplayMode.BOTH,
                                                            null);
    return new SiteContainerDescription(banner.getStorageId(),
                                        options,
                                        toStyling(banner, StylingDestination.BANNER),
                                        applications);
  }

  /**
   * A sidebar is in use when it holds its cell; the outer one spans the
   * banners (full height), the inner one sits between them. The cell carries
   * the width, the mobile visibility, the styling and the applications; the
   * sidebar itself carries the sticky token.
   */
  private SiteContainerDescription toSidebar(LayoutModel outer, LayoutModel inner, Map<String, String> contentIds) {
    LayoutModel outerCell = sidebarCell(outer);
    LayoutModel innerCell = sidebarCell(inner);
    LayoutModel sidebar = outerCell != null ? outer : inner;
    LayoutModel cell = outerCell != null ? outerCell : innerCell;
    if (cell == null) {
      return null;
    }
    SiteContainerOptions options = new SiteContainerOptions(null,
                                                            parsePixels(cell.getWidth()),
                                                            hasToken(sidebar.getCssClass(), STICKY_SECTION_TOKEN),
                                                            hasToken(cell.getCssClass(), HIDE_MOBILE_TOKEN) ? DisplayMode.HIDE_MOBILE
                                                                                                              : DisplayMode.BOTH,
                                                            outerCell != null);
    return new SiteContainerDescription(cell.getStorageId(),
                                        options,
                                        toStyling(cell, StylingDestination.SIDEBAR),
                                        toApplications(cell, contentIds, StylingDestination.SITE_CONTAINER_APPLICATION));
  }

  private LayoutModel sidebarCell(LayoutModel sidebar) {
    return sidebar == null ? null :
                           children(sidebar).stream().filter(c -> isTemplate(c, SIDEBAR_CELL_TEMPLATE)).findFirst().orElse(null);
  }

  private LayoutModel sidebarAt(List<LayoutModel> children, int index) {
    return children.size() > index && isTemplate(children.get(index), SIDEBAR_TEMPLATE) ? children.get(index) : null;
  }

  private List<ApplicationDescription> toApplications(LayoutModel cell,
                                                      Map<String, String> contentIds,
                                                      StylingDestination destination) {
    return children(cell).stream()
                         .filter(c -> StringUtils.isBlank(c.getTemplate()))
                         .map(application -> toApplication(application, contentIds, destination))
                         .toList();
  }

  private ApplicationDescription toApplication(LayoutModel application,
                                               Map<String, String> contentIds,
                                               StylingDestination destination) {
    String storageId = application.getStorageId();
    Long instanceId = null;
    if (NumberUtils.isDigits(storageId)) {
      long portletInstanceId = portletInstanceService.getApplicationPortletInstanceId(Long.parseLong(storageId));
      instanceId = portletInstanceId > 0 ? portletInstanceId : null;
    }
    String contentId = contentIds.getOrDefault(storageId, application.getContentId());
    return new ApplicationDescription(storageId,
                                      contentId,
                                      instanceId,
                                      application.getTitle(),
                                      toStyling(application, destination),
                                      toDisplay(application.getCssClass()),
                                      null,
                                      false,
                                      null);
  }

  private List<NavigationItemDescription> toNavigationItems(NodeContext<NodeContext<Object>> parent,
                                                            SiteKey siteKey,
                                                            boolean withHidden,
                                                            String username) {
    List<NavigationItemDescription> items = new ArrayList<>();
    for (int i = 0; i < parent.getNodeCount(); i++) {
      NodeContext<NodeContext<Object>> node = parent.get(i);
      NodeState state = node.getState();
      Visibility visibility = state == null || state.getVisibility() == null ? Visibility.DISPLAYED : state.getVisibility();
      boolean hidden = visibility == Visibility.HIDDEN || visibility == Visibility.SYSTEM;
      if (state == null
          || layoutDraftService.isDraftNode(node.getName())
          || visibility == Visibility.DRAFT
          || (hidden && !withHidden)
          || !aclService.canViewNavigation(siteKey, state.getPageRef(), username)) {
        continue;
      }
      items.add(toNavigationItem(node, state, visibility, siteKey, withHidden, username));
    }
    return items;
  }

  private NavigationItemDescription toNavigationItem(NodeContext<NodeContext<Object>> node, // NOSONAR
                                                     NodeState state,
                                                     Visibility visibility,
                                                     SiteKey siteKey,
                                                     boolean withHidden,
                                                     String username) {
    long nodeId = Long.parseLong(node.getId());
    Map<String, String> labels = readLabels(nodeId, username);
    String name = labels.isEmpty() ? state.getLabel() : labels.values().iterator().next();
    PageKey pageRef = state.getPageRef();
    PageState pageState = null;
    if (pageRef != null) {
      PageContext pageContext = layoutService.getPageContext(pageRef);
      pageState = pageContext == null ? null : pageContext.getState();
    }
    boolean link = pageState != null && PageType.LINK.name().equalsIgnoreCase(pageState.getType());
    NavigationItemType type = pageRef == null ? NavigationItemType.GROUP : link ? NavigationItemType.LINK : NavigationItemType.PAGE;
    ScheduleDescription schedule = visibility == Visibility.TEMPORAL ?
                                                                     new ScheduleDescription(toInstant(state.getStartPublicationTime()),
                                                                                             toInstant(state.getEndPublicationTime())) :
                                                                     null;
    boolean hidden = visibility == Visibility.HIDDEN || visibility == Visibility.SYSTEM;
    return new NavigationItemDescription(nodeId,
                                         name,
                                         labels.isEmpty() ? null : labels,
                                         node.getName(),
                                         type,
                                         pageRef == null ? null : pageRef.format(),
                                         link ? pageState.getLink() : null,
                                         link ? state.getTarget() : null,
                                         state.getIcon(),
                                         !hidden,
                                         schedule,
                                         pageState == null ? null : pageState.getAccessPermissions(),
                                         pageState == null ? null : pageState.getEditPermission(),
                                         toNavigationItems(node, siteKey, withHidden, username));
  }

  private Map<String, String> readLabels(long nodeId, String username) {
    try {
      NodeLabel nodeLabel = navigationLayoutService.getNodeLabels(nodeId, username);
      Map<String, String> labels = nodeLabel == null ? null : nodeLabel.getLabels();
      if (labels == null || labels.isEmpty()) {
        return Collections.emptyMap();
      }
      String defaultLanguage = nodeLabel.getDefaultLanguage();
      if (defaultLanguage != null && labels.containsKey(defaultLanguage)) {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put(defaultLanguage, labels.get(defaultLanguage));
        ordered.putAll(labels);
        return ordered;
      }
      return labels;
    } catch (ObjectNotFoundException | IllegalAccessException e) {
      return Collections.emptyMap();
    }
  }

  private String toInstant(long time) {
    return time > 0 ? Instant.ofEpochMilli(time).toString() : null;
  }

  private boolean matches(PortletInstance instance, String needle) {
    return StringUtils.containsIgnoreCase(instance.getName(), needle)
           || StringUtils.containsIgnoreCase(instance.getDescription(), needle);
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

  private Map<String, String> collectContentIds(ModelObject modelObject) {
    Map<String, String> contentIds = new HashMap<>();
    collectContentIds(modelObject, contentIds);
    return contentIds;
  }

  private void collectContentIds(ModelObject modelObject, Map<String, String> contentIds) {
    if (modelObject instanceof Container container && CollectionUtils.isNotEmpty(container.getChildren())) {
      container.getChildren().forEach(child -> collectContentIds(child, contentIds));
    } else if (modelObject instanceof Application application) {
      String storageId = application.getStorageId() == null ? application.getId() : application.getStorageId();
      if (StringUtils.isNotBlank(storageId)) {
        contentIds.put(storageId, layoutService.getId(application.getState()));
      }
    }
  }

  private MarginsBlock toMargins(LayoutModel model) {
    Integer top = model.getMarginTop();
    Integer right = model.getMarginRight();
    Integer bottom = model.getMarginBottom();
    Integer left = model.getMarginLeft();
    boolean enabled = top != null || right != null || bottom != null || left != null;
    Integer all = top != null && top.equals(right) && top.equals(bottom) && top.equals(left) ? top : null;
    return new MarginsBlock(enabled, all, top, right, bottom, left);
  }

  private BorderBlock toBorder(LayoutModel model) {
    boolean enabled = StringUtils.isNotBlank(model.getBorderColor());
    return new BorderBlock(enabled,
                           model.getBorderColor(),
                           parsePixels(model.getBorderSize()),
                           enabled ? Boolean.valueOf("true".equals(model.getBoxShadow())) : null);
  }

  private CornersBlock toCorners(LayoutModel model) {
    return toCorners(model.getRadiusTopLeft(),
                     model.getRadiusTopRight(),
                     model.getRadiusBottomRight(),
                     model.getRadiusBottomLeft());
  }

  private CornersBlock toCorners(Integer topLeft, Integer topRight, Integer bottomRight, Integer bottomLeft) {
    boolean enabled = topLeft != null || topRight != null || bottomRight != null || bottomLeft != null;
    Integer all = topLeft != null && topLeft.equals(topRight) && topLeft.equals(bottomRight) && topLeft.equals(bottomLeft) ?
                                                                                                                              topLeft :
                                                                                                                              null;
    return new CornersBlock(enabled, all, topLeft, topRight, bottomRight, bottomLeft);
  }

  private BackgroundBlock toBackground(LayoutModel model) {
    String color = model.getBackgroundColor();
    String effect = model.getBackgroundEffect();
    String image = model.getBackgroundImage();
    boolean enabled = StringUtils.isNotBlank(color) || StringUtils.isNotBlank(effect) || StringUtils.isNotBlank(image);
    if (!enabled) {
      return new BackgroundBlock(false, null, null, null, null, null, null, null, null, null);
    }
    String scrollColor = null;
    if (StringUtils.contains(color, "@")) {
      String[] parts = color.split("@", 2);
      color = parts[0];
      scrollColor = parts[1];
    }
    BackgroundImageBlock imageBlock = StringUtils.isBlank(image) ? null :
                                                                 new BackgroundImageBlock(null,
                                                                                          image,
                                                                                          model.getBackgroundSize(),
                                                                                          model.getBackgroundPosition(),
                                                                                          model.getBackgroundAttachment(),
                                                                                          model.getBackgroundRepeat(),
                                                                                          toImageMargins(model.getCssClass()),
                                                                                          toImageCorners(model.getCssClass()));
    if (StringUtils.isBlank(effect)) {
      return new BackgroundBlock(true, BackgroundMode.SOLID, color, null, null, null, null, scrollColor, null, imageBlock);
    }
    BackgroundBlock gradient = parseGradient(effect);
    return new BackgroundBlock(true,
                               gradient.mode(),
                               gradient.mode() == null ? color : null,
                               gradient.from(),
                               gradient.to(),
                               gradient.ratio(),
                               gradient.direction(),
                               scrollColor,
                               gradient.effect(),
                               imageBlock);
  }

  /**
   * Reads the three gradient forms the editor writes (linear-gradient with an
   * optional "to right", radial-gradient, conic-gradient from a corner); any
   * other effect is returned as is in the effect field.
   */
  private BackgroundBlock parseGradient(String effect) {
    BackgroundMode mode = null;
    String direction = null;
    if (effect.startsWith("linear-gradient(")) {
      mode = BackgroundMode.LINEAR;
      direction = effect.contains("to right") ? TO_RIGHT : TO_BOTTOM;
    } else if (effect.startsWith("radial-gradient(")) {
      mode = BackgroundMode.RADIAL;
    } else if (effect.startsWith("conic-gradient(")) {
      mode = BackgroundMode.ANGULAR;
      Matcher corner = CONIC_CORNER.matcher(effect);
      direction = corner.find() ? corner.group(1) + "_" + corner.group(2) : null;
    }
    List<String> colors = new ArrayList<>();
    Matcher colorMatcher = HEX_COLOR.matcher(effect);
    while (colorMatcher.find()) {
      colors.add(colorMatcher.group());
    }
    List<Integer> stops = new ArrayList<>();
    Matcher stopMatcher = PERCENT.matcher(effect);
    while (stopMatcher.find()) {
      stops.add(Integer.parseInt(stopMatcher.group(1)));
    }
    if (mode == BackgroundMode.ANGULAR) {
      return parseConicGradient(effect, direction);
    }
    if (mode == null || colors.size() < 2) {
      return new BackgroundBlock(true, null, null, null, null, null, null, null, effect, null);
    }
    Integer ratio = stops.size() >= 3 ? stops.get(1) : null;
    return new BackgroundBlock(true, mode, null, colors.get(0), colors.get(colors.size() - 1), ratio, direction, null, null, null);
  }

  /**
   * The editor writes conic-gradient(from &lt;angle&gt; at &lt;corner&gt;, E 0deg, X
   * &lt;mid&gt;deg, F 90deg) with degree stops, the plateau next to the From
   * colour's edge: for top right and bottom left the first stop is the To
   * colour and the ratio is the complement of the middle angle's share.
   */
  private BackgroundBlock parseConicGradient(String effect, String direction) {
    String inner = effect.substring("conic-gradient(".length()).replaceAll("\\)$", "");
    int firstComma = inner.indexOf(',');
    if (firstComma < 0) {
      return new BackgroundBlock(true, null, null, null, null, null, null, null, effect, null);
    }
    List<Matcher> stops = new ArrayList<>();
    for (String stop : inner.substring(firstComma + 1).split(",")) {
      Matcher matcher = CONIC_STOP.matcher(stop.trim());
      if (!matcher.matches()) {
        return new BackgroundBlock(true, null, null, null, null, null, null, null, effect, null);
      }
      stops.add(matcher);
    }
    if (stops.size() < 2) {
      return new BackgroundBlock(true, null, null, null, null, null, null, null, effect, null);
    }
    boolean reversed = "top_right".equals(direction) || "bottom_left".equals(direction);
    String firstColor = stops.get(0).group(1);
    String lastColor = stops.get(stops.size() - 1).group(1);
    Integer ratio = null;
    if (stops.size() == 3) {
      int midPercent = (int) Math.round(Double.parseDouble(stops.get(1).group(2)) / 90 * 100);
      ratio = reversed ? 100 - midPercent : midPercent;
    }
    return new BackgroundBlock(true,
                               BackgroundMode.ANGULAR,
                               null,
                               reversed ? lastColor : firstColor,
                               reversed ? firstColor : lastColor,
                               ratio,
                               direction,
                               null,
                               null,
                               null);
  }

  private SidesBlock toImageMargins(String cssClass) {
    if (StringUtils.isBlank(cssClass)) {
      return null;
    }
    Map<String, Integer> values = new HashMap<>();
    Matcher matcher = BG_MARGIN_TOKEN.matcher(cssClass);
    while (matcher.find()) {
      values.put(matcher.group(2), Integer.valueOf(matcher.group(3)));
    }
    return values.isEmpty() ? null : new SidesBlock(values.get("top"), values.get("right"), values.get("bottom"), values.get("left"));
  }

  private CornersBlock toImageCorners(String cssClass) {
    if (StringUtils.isBlank(cssClass)) {
      return null;
    }
    Map<String, Integer> values = new HashMap<>();
    Matcher matcher = BG_RADIUS_TOKEN.matcher(cssClass);
    while (matcher.find()) {
      values.put(matcher.group(2), Integer.valueOf(matcher.group(3)));
    }
    return values.isEmpty() ? null : toCorners(values.get("tl"), values.get("tr"), values.get("br"), values.get("bl"));
  }

  private TextBlock toText(LayoutModel model) { // NOSONAR
    TextRoleBlock title = toTextRole(model.getTextTitleColor(),
                                     model.getTextTitleFontSize(),
                                     model.getTextTitleFontWeight(),
                                     model.getTextTitleFontStyle(),
                                     toTextBackground(model.getTextTitleBackgroundColor(),
                                                      model.getTextTitleBackgroundEffect(),
                                                      model.getTextTitleBackgroundImage(),
                                                      model.getTextTitleBackgroundSize(),
                                                      model.getTextTitleBackgroundRepeat(),
                                                      model.getTextTitleBackgroundPosition(),
                                                      model.getTextTitleBackgroundPadding(),
                                                      model.getTextTitleBackgroundRadius()));
    TextRoleBlock header = toTextRole(model.getTextHeaderColor(),
                                      model.getTextHeaderFontSize(),
                                      model.getTextHeaderFontWeight(),
                                      model.getTextHeaderFontStyle(),
                                      toTextBackground(model.getTextHeaderBackgroundColor(),
                                                       model.getTextHeaderBackgroundEffect(),
                                                       model.getTextHeaderBackgroundImage(),
                                                       model.getTextHeaderBackgroundSize(),
                                                       model.getTextHeaderBackgroundRepeat(),
                                                       model.getTextHeaderBackgroundPosition(),
                                                       model.getTextHeaderBackgroundPadding(),
                                                       model.getTextHeaderBackgroundRadius()));
    TextRoleBlock content = toTextRole(model.getTextColor(),
                                       model.getTextFontSize(),
                                       model.getTextFontWeight(),
                                       model.getTextFontStyle(),
                                       null);
    TextRoleBlock subtitle = toTextRole(model.getTextSubtitleColor(),
                                        model.getTextSubtitleFontSize(),
                                        model.getTextSubtitleFontWeight(),
                                        model.getTextSubtitleFontStyle(),
                                        null);
    boolean enabled = title != null || header != null || content != null || subtitle != null
                      || StringUtils.isNotBlank(model.getIconColor());
    return new TextBlock(enabled, title, header, content, subtitle, model.getIconColor());
  }

  private TextRoleBlock toTextRole(String color, String fontSize, String fontWeight, String fontStyle, TextBackgroundBlock background) {
    boolean bold = "bold".equals(fontWeight);
    boolean italic = "italic".equals(fontStyle);
    if (StringUtils.isBlank(color) && StringUtils.isBlank(fontSize) && !bold && !italic && background == null) {
      return null;
    }
    return new TextRoleBlock(color, parsePixels(fontSize), bold, italic, background);
  }

  private TextBackgroundBlock toTextBackground(String color, // NOSONAR
                                               String effect,
                                               String image,
                                               String size,
                                               String repeat,
                                               String position,
                                               String padding,
                                               String radius) {
    if (StringUtils.isBlank(color) && StringUtils.isBlank(effect) && StringUtils.isBlank(image)) {
      return null;
    }
    BackgroundImageBlock imageBlock = StringUtils.isBlank(image) ? null :
                                                                 new BackgroundImageBlock(null,
                                                                                          image,
                                                                                          size,
                                                                                          position,
                                                                                          null,
                                                                                          repeat,
                                                                                          null,
                                                                                          null);
    BackgroundBlock gradient = StringUtils.isBlank(effect) ? null : parseGradient(effect);
    BackgroundMode mode = gradient == null ? BackgroundMode.SOLID : gradient.mode();
    return new TextBackgroundBlock(true,
                                   mode,
                                   gradient == null || gradient.mode() == null ? color : null,
                                   gradient == null ? null : gradient.from(),
                                   gradient == null ? null : gradient.to(),
                                   gradient == null ? null : gradient.ratio(),
                                   gradient == null ? null : gradient.direction(),
                                   gradient == null ? null : gradient.effect(),
                                   imageBlock,
                                   toSides(padding),
                                   toSpacedCorners(radius));
  }

  private HeightBlock toHeight(LayoutModel model) {
    String height = model.getHeight();
    if (StringUtils.isBlank(height)) {
      return new HeightBlock(false, null, null);
    }
    HeightPreset preset = HeightPreset.fromStoredValue(height);
    return new HeightBlock(true, preset, preset == HeightPreset.CUSTOM ? parsePixels(height) : null);
  }

  /**
   * @param  value a "T R B L" list of pixel values, as the text background
   *                 padding is stored
   * @return the four sides, null when the value is blank or malformed
   */
  private SidesBlock toSides(String value) {
    List<Integer> parts = parsePixelList(value);
    return parts.size() == 4 ? new SidesBlock(parts.get(0), parts.get(1), parts.get(2), parts.get(3)) : null;
  }

  private CornersBlock toSpacedCorners(String value) {
    List<Integer> parts = parsePixelList(value);
    return parts.size() == 4 ? toCorners(parts.get(0), parts.get(1), parts.get(2), parts.get(3)) : null;
  }

  private List<Integer> parsePixelList(String value) {
    if (StringUtils.isBlank(value)) {
      return Collections.emptyList();
    }
    List<Integer> parts = new ArrayList<>();
    Matcher matcher = PIXELS.matcher(value);
    while (matcher.find()) {
      parts.add(Integer.valueOf(matcher.group()));
    }
    return parts;
  }

  private Integer parsePixels(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    Matcher matcher = PIXELS.matcher(value);
    return matcher.find() ? Integer.valueOf(matcher.group()) : null;
  }

  private boolean hasToken(String cssClass, String token) {
    return StringUtils.isNotBlank(cssClass)
           && TOKEN_PATTERNS.computeIfAbsent(token, t -> Pattern.compile("(^| )" + Pattern.quote(t) + "(?= |$)"))
                            .matcher(cssClass)
                            .find();
  }

  private boolean isTemplate(LayoutModel model, String template) {
    return model != null && StringUtils.equals(model.getTemplate(), template);
  }

  private List<LayoutModel> children(LayoutModel model) {
    return model == null || model.getChildren() == null ? Collections.emptyList() : model.getChildren();
  }

}
