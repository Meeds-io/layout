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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The read tools of the layout: every public method is an MCP tool described
 * in ai-tool-definitions.json. A tool maps its arguments, calls the Services
 * as the current user and translates their refusals into the fixed texts the
 * agent reads: {@code does not exist: <object>}, {@code not allowed: <object>},
 * {@code value is not accepted for <option>}.
 */
@Service
@Profile("mcp-server")
public class LayoutMcpTool implements McpToolPlugin {

  public static final String       SITE_NOT_FOUND        = "does not exist: site";

  public static final String       SITE_NOT_ALLOWED      = "not allowed: site";

  public static final String       PAGE_NOT_FOUND        = "does not exist: page";

  public static final String       PAGE_NOT_ALLOWED      = "not allowed: page";

  public static final String       DRAFT_NOT_FOUND       = "does not exist: draft";

  public static final String       SITE_ID_NOT_ACCEPTED  = "value is not accepted for site_id";

  public static final String       PAGE_REF_NOT_ACCEPTED = "value is not accepted for page_ref";

  @Autowired
  private LayoutService            layoutService;

  @Autowired
  private LayoutAclService         aclService;

  @Autowired
  private SiteLayoutService        siteLayoutService;

  @Autowired
  private PageLayoutService        pageLayoutService;

  @Autowired
  private LayoutDescriptionService layoutDescriptionService;

  @Autowired
  private LayoutDraftService       layoutDraftService;

  /**
   * Lists the sites the current user can view: without a keyword the portal
   * sites, in display order; with one, the portal sites whose name or label
   * contains it and the group sites, a space's site among them, whose own
   * name contains it. Read-only.
   */
  public List<SiteSummary> listSites(String keyword) {
    return layoutDescriptionService.describeSites(getCurrentUserName(), keyword);
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
    PortalConfig draftSite = readDraftSite(siteId, username);
    PortalConfig source = draftSite == null ? site : draftSite;
    ModelObject layout = layoutDescriptionService.filterAccessible(source.getPortalLayout(), username);
    String label = StringUtils.defaultIfBlank(site.getLabel(), site.getName());
    String description = site.getDescription();
    if (StringUtils.equalsIgnoreCase(site.getType(), SiteType.PORTAL.getName())) {
      label = resolveLabel(siteLayoutService.getSiteLabels(siteId, username), label);
      description = resolveLabel(siteLayoutService.getSiteDescriptions(siteId, username), description);
    }
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
    return layoutDescriptionService.describeNavigation(site.getSiteKey(), Boolean.TRUE.equals(includeHidden), username);
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
    if (layoutDraftService.isDraftNode(pageKey.getName()) || layoutService.getPage(pageKey) == null) {
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
    return layoutDescriptionService.describeApplications(categoryId, keyword, getCurrentUserName(), getCurrentUserLocale());
  }

  /**
   * A draft site is reached through its live site and the user's revision
   * only, never by its own id
   */
  private PortalConfig readSite(long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    try {
      PortalConfig site = siteLayoutService.getSite(siteId, username);
      if (StringUtils.equalsIgnoreCase(site.getType(), PortalConfig.DRAFT)) {
        throw new ObjectNotFoundException(SITE_NOT_FOUND);
      }
      return site;
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    }
  }

  /**
   * The user's draft site, read under the owner rule UserACL applies to a
   * draft site; null when the user has none or may not read the one named.
   */
  private PortalConfig readDraftSite(long siteId, String username) {
    SiteKey draftSiteKey = layoutDraftService.getDraftSiteKey(siteId, username);
    if (draftSiteKey == null) {
      return null;
    }
    try {
      return siteLayoutService.getSite(draftSiteKey, username);
    } catch (ObjectNotFoundException | IllegalAccessException e) {
      return null;
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

}
