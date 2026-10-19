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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.ModelObject;
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

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.NavigationItemType;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.ScheduleDescription;
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
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The read tools of the layout: every public method is an MCP tool described
 * in ai-tool-definitions.json. A tool maps its arguments, calls the Services
 * as the current user and translates their refusals into the fixed texts the
 * agent reads: "does not exist: <object>", "not allowed: <object>", "value is
 * not accepted for <option>".
 */
@Service
@Profile("mcp-server")
public class LayoutMcpTool implements McpToolPlugin {

  public static final String        SITE_NOT_FOUND          = "does not exist: site";

  public static final String        SITE_NOT_ALLOWED        = "not allowed: site";

  public static final String        PAGE_NOT_FOUND          = "does not exist: page";

  public static final String        PAGE_NOT_ALLOWED        = "not allowed: page";

  public static final String        DRAFT_NOT_FOUND         = "does not exist: draft";

  public static final String        SITE_ID_NOT_ACCEPTED    = "value is not accepted for site_id";

  public static final String        PAGE_REF_NOT_ACCEPTED   = "value is not accepted for page_ref";

  private static final SiteType[]   LISTED_SITE_TYPES       = { SiteType.PORTAL, SiteType.GROUP };

  @Autowired
  private LayoutService             layoutService;

  @Autowired
  private NavigationService         navigationService;

  @Autowired
  private LayoutAclService          aclService;

  @Autowired
  private SiteLayoutService         siteLayoutService;

  @Autowired
  private PageLayoutService         pageLayoutService;

  @Autowired
  private NavigationLayoutService   navigationLayoutService;

  @Autowired
  private PortletInstanceService    portletInstanceService;

  @Autowired
  private LayoutDescriptionService  layoutDescriptionService;

  @Autowired
  private LayoutDraftService        layoutDraftService;

  /**
   * Lists the portal and group sites the current user can view, in display
   * order for portal sites. Read-only.
   */
  public List<SiteSummary> listSites() {
    String username = getCurrentUserName();
    List<SiteSummary> sites = new ArrayList<>();
    for (SiteType siteType : LISTED_SITE_TYPES) {
      SiteFilter filter = new SiteFilter();
      filter.setSiteType(siteType);
      filter.setSortByDisplayOrder(siteType == SiteType.PORTAL);
      layoutService.getSites(filter)
                   .stream()
                   .filter(site -> aclService.canViewSite(site.getSiteKey(), username))
                   .map(site -> new SiteSummary(site.getId(),
                                                site.getType(),
                                                site.getName(),
                                                StringUtils.defaultIfBlank(site.getLabel(), site.getName()),
                                                site.getDescription(),
                                                site.isDisplayed()))
                   .forEach(sites::add);
    }
    return sites;
  }

  /**
   * Reads a site's properties and its layout: left and right sidebars,
   * topbars and bottom bars with their options, styling and applications. The
   * user's site draft is returned when they have one (draft: true), the
   * published layout otherwise; what the user cannot access is absent.
   * Read-only.
   */
  public SiteDescription getSite(Long siteId) throws ObjectNotFoundException, IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readSite(siteId, username);
    SiteKey draftSiteKey = layoutDraftService.getDraftSiteKey(siteId, username);
    PortalConfig draftSite = draftSiteKey == null ? null : layoutService.getPortalConfig(draftSiteKey);
    PortalConfig source = draftSite == null ? site : draftSite;
    ModelObject layout = layoutDescriptionService.filterAccessible(source.getPortalLayout(), username);
    String label = resolveLabel(siteLayoutService.getSiteLabels(siteId, username),
                                StringUtils.defaultIfBlank(site.getLabel(), site.getName()));
    String description = resolveLabel(siteLayoutService.getSiteDescriptions(siteId, username), site.getDescription());
    return layoutDescriptionService.toSiteDescription(site, layout, label, description, draftSite != null);
  }

  /**
   * Lists the navigation items of a site as a tree, each with its identifier,
   * its name and translations, its URL name, its type, icon, visibility and
   * schedule, and the permissions of its page. Hidden items are listed only
   * when include_hidden is true and the user can edit the site's navigation;
   * draft items are never listed. Read-only.
   */
  public List<NavigationItemDescription> listSitePages(Long siteId, Boolean includeHidden) throws ObjectNotFoundException,
                                                                                            IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    boolean withHidden = Boolean.TRUE.equals(includeHidden) && aclService.canEditNavigation(siteKey, username);
    NodeContext<NodeContext<Object>> root = navigationService.loadNode(siteKey);
    return root == null ? Collections.emptyList() : toNavigationItems(root, siteKey, withHidden, username);
  }

  /**
   * Reads the whole page in the shape set_page_layout takes: its sections,
   * their columns or cells, and the applications they hold with their
   * styling and visibility. The user's draft is returned by default when
   * their co-editing revision names one, the published layout otherwise;
   * version: "draft" or "published" asks for one of them. Reads through the
   * editor's own page read: a page still carrying a legacy addon container is
   * rewritten once by that read, as it is when the editor opens it.
   */
  public PageDescription getPageLayout(String pageRef, String version) throws ObjectNotFoundException,
                                                                        IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    LayoutVersion requested = LayoutVersion.parse(version);
    String username = getCurrentUserName();
    if (layoutService.getPage(pageKey) == null) {
      throw new ObjectNotFoundException(PAGE_NOT_FOUND);
    } else if (!aclService.canViewPage(pageKey, username) || !aclService.canViewSite(pageKey.getSite(), username)) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
    Long liveNodeId = layoutDraftService.findLiveNodeId(pageKey);
    PageKey draftPageKey = liveNodeId == null ? null : layoutDraftService.getDraftPageKey(liveNodeId, username);
    if (requested == LayoutVersion.DRAFT && draftPageKey == null) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    }
    boolean draft = requested != LayoutVersion.PUBLISHED && draftPageKey != null;
    PageKey pageToRead = draft ? draftPageKey : pageKey;
    ModelObject tree;
    try {
      tree = pageLayoutService.getPageLayout(pageToRead, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(draft ? DRAFT_NOT_FOUND : PAGE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
    return layoutDescriptionService.toPageDescription(tree,
                                                      pageKey.format(),
                                                      draft ? LayoutVersion.DRAFT : LayoutVersion.PUBLISHED);
  }

  /**
   * Lists the applications the current user can place on a page or in a site
   * container, with their catalog identifier, content identifier, name,
   * description and category; category_id narrows the list to one category,
   * keyword to the names and descriptions containing it. Read-only.
   */
  public List<LayoutApplicationEntry> listLayoutApplications(Long categoryId, String keyword) {
    String username = getCurrentUserName();
    Locale locale = getCurrentUserLocale();
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

  private List<NavigationItemDescription> toNavigationItems(NodeContext<NodeContext<Object>> parent,
                                                            SiteKey siteKey,
                                                            boolean withHidden,
                                                            String username) {
    List<NavigationItemDescription> items = new ArrayList<>();
    for (int i = 0; i < parent.getNodeCount(); i++) {
      NodeContext<NodeContext<Object>> node = parent.get(i);
      NodeState state = node.getState();
      Visibility visibility = state == null ? Visibility.DISPLAYED : state.getVisibility();
      boolean hidden = visibility == Visibility.HIDDEN || visibility == Visibility.SYSTEM;
      if (layoutDraftService.isDraftNode(node.getName())
          || visibility == Visibility.DRAFT
          || (hidden && !withHidden)
          || state == null
          || !aclService.canViewNavigation(siteKey, state.getPageRef(), username)) {
        continue;
      }
      items.add(toNavigationItem(node, state, hidden, siteKey, withHidden, username));
    }
    return items;
  }

  private NavigationItemDescription toNavigationItem(NodeContext<NodeContext<Object>> node, // NOSONAR
                                                     NodeState state,
                                                     boolean hidden,
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
    ScheduleDescription schedule = visibility(state) == Visibility.TEMPORAL ?
                                                                            new ScheduleDescription(toInstant(state.getStartPublicationTime()),
                                                                                                    toInstant(state.getEndPublicationTime())) :
                                                                            null;
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

  private PortalConfig readSite(long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    try {
      return siteLayoutService.getSite(siteId, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    }
  }

  private String resolveLabel(NodeLabel nodeLabel, String fallback) {
    if (nodeLabel == null || nodeLabel.getLabels() == null || nodeLabel.getLabels().isEmpty()) {
      return fallback;
    }
    String defaultLanguage = nodeLabel.getDefaultLanguage();
    String label = defaultLanguage == null ? null : nodeLabel.getLabels().get(defaultLanguage);
    return StringUtils.defaultIfBlank(label, StringUtils.defaultIfBlank(nodeLabel.getLabels().values().iterator().next(), fallback));
  }

  private void requireSiteId(Long siteId) {
    if (siteId == null || siteId <= 0) {
      throw new IllegalArgumentException(SITE_ID_NOT_ACCEPTED);
    }
  }

  private PageKey parsePageKey(String pageRef) {
    if (StringUtils.isBlank(pageRef)) {
      throw new IllegalArgumentException(PAGE_REF_NOT_ACCEPTED);
    }
    try {
      return PageKey.parse(pageRef.trim());
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(PAGE_REF_NOT_ACCEPTED);
    }
  }

  private Visibility visibility(NodeState state) {
    return state.getVisibility() == null ? Visibility.DISPLAYED : state.getVisibility();
  }

  private String toInstant(long time) {
    return time > 0 ? Instant.ofEpochMilli(time).toString() : null;
  }

  private boolean matches(PortletInstance instance, String needle) {
    return StringUtils.containsIgnoreCase(instance.getName(), needle)
           || StringUtils.containsIgnoreCase(instance.getDescription(), needle);
  }

}
