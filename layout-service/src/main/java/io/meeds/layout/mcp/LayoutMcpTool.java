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

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.LinkTarget;
import io.meeds.layout.mcp.constant.NavigationItemType;
import io.meeds.layout.mcp.model.ElementStyling;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.ScheduleDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteLayoutUpdate;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationItemUpdate;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.NodeSchedule;
import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SitePropertiesUpdate;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.layout.service.SiteLayoutAssembler;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The read tools of the layout, the site tools, the site layout tools and the
 * navigation item tools: every public method is an MCP tool described in ai-tool-definitions.json. A
 * tool maps its arguments, checks them against the vocabulary the editors
 * accept, calls the Services as the current user and translates their
 * refusals into the fixed texts the agent reads: {@code does not exist:
 * <object>}, {@code not allowed: <object>}, {@code value is not accepted for
 * <option>}. A change to a site's properties applies at once, as the sites
 * administration's does, and so does a change to its navigation, as the
 * navigation drawer's; a change to its layout, or to a page's, is the user's
 * draft until publish_layout, as in the layout editors.
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

  public static final String       CREATE_SITE_NOT_ALLOWED = "not allowed: create site";

  public static final String       SITE_ALREADY_EXISTS   = "already exists: site";

  public static final String       SITE_NOT_REMOVABLE    = "cannot be deleted: site";

  public static final String       SITE_NOT_UPDATABLE    = "cannot be updated: site";

  public static final String       SITE_NOT_SAVED        = "could not be saved: site";

  public static final String       SITE_NOT_DELETED      = "could not be deleted: site";

  public static final String       UPLOAD_NOT_FOUND      = "does not exist: upload";

  public static final String       NAME_NOT_ACCEPTED     = "value is not accepted for name";

  public static final String       LABEL_NOT_ACCEPTED    = "value is not accepted for label";

  public static final String       DESCRIPTION_NOT_ACCEPTED = "value is not accepted for description";

  public static final String       TEMPLATE_NOT_ACCEPTED = "value is not accepted for template";

  public static final String       ICON_NOT_ACCEPTED     = "value is not accepted for icon";

  public static final String       ACCESS_PERMISSIONS_NOT_ACCEPTED = "value is not accepted for access_permissions";

  public static final String       EDIT_PERMISSION_NOT_ACCEPTED = "value is not accepted for edit_permission";

  public static final String       REMOVE_BANNER_NOT_ACCEPTED = "value is not accepted for remove_banner";

  public static final String       SITE_LAYOUT_NOT_SAVED = "could not be saved: site layout";

  public static final String       SITE_LAYOUT_NOT_PUBLISHED = "could not be published: site layout";

  public static final String       LAYOUT_OUTDATED       = "layout outdated, re-read it";

  public static final String       PAGE_LAYOUT_NOT_SAVED = "could not be saved: page layout";

  public static final String       PAGE_LAYOUT_NOT_PUBLISHED = "could not be published: page layout";

  public static final String       PAGE_OUTDATED_MESSAGE = "layout.pageOutdatedError";

  public static final String       STYLING_NOT_ACCEPTED  = SiteLayoutAssembler.STYLING_NOT_ACCEPTED;

  public static final String       NODE_NOT_FOUND        = "does not exist: navigation item";

  public static final String       PARENT_NODE_NOT_FOUND = "does not exist: parent navigation item";

  public static final String       NAVIGATION_NOT_ALLOWED = "not allowed: navigation";

  public static final String       NODE_ID_NOT_ACCEPTED  = "value is not accepted for node_id";

  public static final String       PARENT_NODE_ID_NOT_ACCEPTED = "value is not accepted for parent_node_id";

  public static final String       TYPE_NOT_ACCEPTED     = "value is not accepted for type";

  public static final String       URL_NAME_NOT_ACCEPTED = "value is not accepted for url_name";

  public static final String       URL_NAME_NOT_CHANGEABLE = "cannot be changed: url_name";

  public static final String       URL_NOT_ACCEPTED      = "value is not accepted for url";

  public static final String       TARGET_NOT_ACCEPTED   = LinkTarget.TARGET_NOT_ACCEPTED;

  public static final String       PAGE_TEMPLATE_NOT_ACCEPTED = "value is not accepted for page_template_id";

  public static final String       LABELS_NOT_ACCEPTED   = "value is not accepted for labels";

  public static final String       SCHEDULED_NOT_ACCEPTED = "value is not accepted for scheduled";

  public static final String       NODE_NOT_SAVED        = "could not be saved: navigation item";

  public static final String       NODE_NOT_DELETED      = "could not be deleted: navigation item";

  public static final String       BLANK_TEMPLATE_CATEGORY = "blank";

  public static final String       DEFAULT_TEMPLATE      = "basic";

  public static final String       DEFAULT_ICON          = "fa-globe";

  public static final int          LABEL_MAX_LENGTH      = 200;

  public static final int          DESCRIPTION_MAX_LENGTH = 2000;

  private static final Log         LOG                   = ExoLogger.getLogger(LayoutMcpTool.class);

  private static final Pattern     NAME_PATTERN          = Pattern.compile("^[a-zA-Z0-9_-]{1,200}$");

  private static final Pattern     ICON_PATTERN          = Pattern.compile("^[a-z0-9-]+( [a-z0-9-]+)*$");

  private static final Pattern     PERMISSION_PATTERN    = Pattern.compile("^(\\*|[a-zA-Z0-9_-]+):(/[a-zA-Z0-9_./-]+)$");

  private static final Pattern     ABSOLUTE_URL_PATTERN  = Pattern.compile("^(?i:https?)://[^/?#\\s]+(?:[/?#].*)?$");

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

  @Autowired
  private SiteTemplateService      siteTemplateService;

  @Autowired
  private OrganizationService      organizationService;

  @Autowired
  private UploadService            uploadService;

  @Autowired
  private NavigationLayoutService  navigationLayoutService;

  @Autowired
  private PageTemplateService      pageTemplateService;

  @Autowired
  private LocaleConfigService      localeConfigService;

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
    return describeSite(site, draftSite, username);
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
    Long liveNodeId = layoutDraftService.findLiveNodeId(pageKey, username);
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
   * Creates a portal site from a site template, as the sites administration
   * does: the name is the site's key and URL segment and never changes, the
   * label is what users see. Omitted options take the administration's
   * defaults: the basic template, not displayed in the sites menu, the
   * fa-globe icon, administrators as access and edit permissions. Every
   * value is checked before anything is written. Applies at once; not a
   * draft. Returns the site as stored, which the permissions written may no
   * longer let the caller read through get_site.
   */
  public SiteDescription createSite(String name, // NOSONAR
                                    String label,
                                    String description,
                                    String template,
                                    String bannerUploadId,
                                    List<String> accessPermissions,
                                    String editPermission,
                                    Boolean displayed,
                                    String icon) throws ObjectNotFoundException, IllegalAccessException {
    String username = getCurrentUserName();
    if (!aclService.canAddSite(username)) {
      throw new IllegalAccessException(CREATE_SITE_NOT_ALLOWED);
    }
    String siteName = requireName(name);
    String siteLabel = requireLabel(label);
    String siteDescription = description == null ? null : requireDescription(description);
    String siteIcon = icon == null ? DEFAULT_ICON : requireIcon(icon);
    List<String> siteAccessPermissions = requireAccessPermissions(accessPermissions);
    String siteEditPermission = requireEditPermission(editPermission);
    long templateId = resolveTemplateId(template);
    String uploadId = requireUpload(bannerUploadId);

    PortalConfig portalConfig = new PortalConfig(PortalConfig.PORTAL_TYPE, siteName);
    portalConfig.setLabel(siteLabel);
    portalConfig.setDescription(StringUtils.trimToNull(siteDescription));
    portalConfig.setDisplayed(Boolean.TRUE.equals(displayed));
    portalConfig.setDisplayOrder(0);
    portalConfig.setIcon(siteIcon);
    portalConfig.setAccessPermissions(siteAccessPermissions == null ? null : siteAccessPermissions.toArray(new String[0]));
    portalConfig.setEditPermission(siteEditPermission);
    portalConfig.setBannerUploadId(uploadId);
    SiteCreateModel createModel = new SiteCreateModel();
    createModel.setSiteId(templateId);
    createModel.setPortalConfig(portalConfig);
    PortalConfig site;
    try {
      site = siteLayoutService.createSite(createModel, username);
    } catch (IllegalAccessException e) {
      releaseUpload(uploadId);
      throw new IllegalAccessException(CREATE_SITE_NOT_ALLOWED);
    } catch (ObjectAlreadyExistsException e) {
      releaseUpload(uploadId);
      throw new IllegalArgumentException(SITE_ALREADY_EXISTS);
    } catch (Exception e) {
      releaseUpload(uploadId);
      if (e instanceof ObjectNotFoundException) {
        throw new IllegalArgumentException(TEMPLATE_NOT_ACCEPTED);
      }
      LOG.debug("Site {} could not be created by {}", siteName, username, e);
      throw new IllegalStateException(SITE_NOT_SAVED);
    }
    return describeStoredSite(site.getSiteKey(), username);
  }

  /**
   * Updates a site's label, description, icon, display in the sites menu,
   * banner and permissions: an option omitted keeps its stored value. The
   * name is the site's key and cannot change. Only a site the sites
   * administration lists, a portal site that is not a space's public site,
   * can be updated. The permissions are written first, then the properties,
   * and what was written is put back when a later step fails. Applies at
   * once; not a draft. Returns the site as stored, which the permissions
   * written (the edit right handed to a group the caller is not in) may no
   * longer let the caller read through get_site.
   */
  public SiteDescription updateSite(Long siteId, // NOSONAR
                                    String label,
                                    String description,
                                    List<String> accessPermissions,
                                    String editPermission,
                                    Boolean displayed,
                                    String icon,
                                    String bannerUploadId,
                                    Boolean removeBanner) throws ObjectNotFoundException, IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } else if (!isAdministeredSite(site)) {
      throw new IllegalArgumentException(SITE_NOT_UPDATABLE);
    }
    String siteLabel = label == null ? null : requireLabel(label);
    String siteDescription = description == null ? null : requireDescription(description);
    String siteIcon = icon == null ? null : requireIcon(icon);
    List<String> siteAccessPermissions = requireAccessPermissions(accessPermissions);
    String siteEditPermission = requireEditPermission(editPermission);
    if (Boolean.TRUE.equals(removeBanner) && StringUtils.isNotBlank(bannerUploadId)) {
      throw new IllegalArgumentException(REMOVE_BANNER_NOT_ACCEPTED);
    }
    String uploadId = requireUpload(bannerUploadId);
    PermissionUpdateModel permissions = siteAccessPermissions == null && siteEditPermission == null ? null :
                                                                                                     new PermissionUpdateModel(siteKey.getTypeName(),
                                                                                                                               siteKey.getName(),
                                                                                                                               siteAccessPermissions,
                                                                                                                               siteEditPermission);
    SitePropertiesUpdate properties = new SitePropertiesUpdate(siteLabel,
                                                               siteDescription,
                                                               siteIcon,
                                                               displayed,
                                                               uploadId,
                                                               removeBanner);
    try {
      siteLayoutService.updateSiteAndPermissions(siteKey, properties, permissions, username);
    } catch (ObjectNotFoundException e) {
      releaseUpload(uploadId);
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      releaseUpload(uploadId);
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      releaseUpload(uploadId);
      if (StringUtils.equals(e.getMessage(), SiteLayoutService.DISPLAYED_NOT_ACCEPTED)) {
        throw new IllegalArgumentException(SiteLayoutService.DISPLAYED_NOT_ACCEPTED);
      }
      LOG.debug("Site {} could not be updated by {}", siteKey, username, e);
      throw new IllegalStateException(SITE_NOT_SAVED);
    }
    return describeStoredSite(siteKey, username);
  }

  /**
   * Deletes a site with its pages and navigation, at once and for every
   * user, as the sites administration does after its confirmation; a site
   * the administration never offers to delete is refused: one the platform
   * ships as not removable, and one it does not list, a space's site or
   * public site, a user's site, a template.
   */
  public void deleteSite(Long siteId) throws ObjectNotFoundException, IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } else if (!isAdministeredSite(site) || !site.isRemovable()) {
      throw new IllegalArgumentException(SITE_NOT_REMOVABLE);
    }
    try {
      siteLayoutService.deleteSite(siteKey, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      LOG.debug("Site {} could not be deleted by {}", siteKey, username, e);
      throw new IllegalStateException(SITE_NOT_DELETED);
    }
  }

  /**
   * Replaces the structure of the user's site draft: the sidebars, topbars
   * and bottom bars given, with their options, styling and applications, as
   * the site layout editor lays them out; a sidebar or a list omitted is
   * absent from the draft, while an option, a styling block or a display
   * left null on a kept element keeps what it holds. Ids address the layout
   * get_site returned: the
   * user's draft when they have one, the published layout otherwise, in
   * which case the draft is created by this call and every kept application
   * becomes a copy carrying its preferences. Every value is checked before
   * anything is written. Returns the draft as get_site does, with its ids.
   */
  public SiteDescription setSiteLayout(Long siteId,
                                       SiteContainerDescription leftSidebar,
                                       SiteContainerDescription rightSidebar,
                                       List<SiteContainerDescription> topbars,
                                       List<SiteContainerDescription> bottomBars) throws ObjectNotFoundException,
                                                                                  IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readEditableSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    PortalConfig draftSite = readDraftSite(siteId, username);
    boolean createDraft = draftSite == null;
    LayoutModel layout = layoutDescriptionService.toSiteLayout(new SiteLayoutUpdate(leftSidebar, rightSidebar, topbars, bottomBars),
                                                               createDraft ? site : draftSite,
                                                               createDraft,
                                                               username,
                                                               getCurrentUserLocale());
    writeSiteDraft(site, draftSite, layout, username);
    return describeStoredSite(siteKey, username);
  }

  /**
   * Saves a site layout over the user's draft site, created from the live
   * site first when the user has none
   */
  private void writeSiteDraft(PortalConfig site,
                              PortalConfig draftSite,
                              LayoutModel layout,
                              String username) throws ObjectNotFoundException, IllegalAccessException {
    try {
      SiteKey draftSiteKey = draftSite == null ? layoutDraftService.createDraftSite(site, username) : draftSite.getSiteKey();
      siteLayoutService.updateSiteLayout(draftSiteKey, layout.toSite(), false, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(STYLING_NOT_ACCEPTED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, site.getSiteKey(), username, SITE_LAYOUT_NOT_SAVED);
    }
  }

  /**
   * Makes the user's draft the published layout, for every viewer at once,
   * as the editors' publish does: the draft the user's revision names is
   * written over the live site or page, every container and application
   * taking a new id, and the draft ends. Exactly one of site_id and page_ref
   * names what is published. Returns the site as get_site does, or the page
   * as get_page_layout does, as published, with the new ids.
   */
  public Object publishLayout(Long siteId, String pageRef) throws ObjectNotFoundException, IllegalAccessException {
    if (pageRef != null) {
      refuseGiven(siteId, PAGE_REF_NOT_ACCEPTED);
      return publishPageLayout(pageRef);
    }
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readEditableSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    try {
      layoutDraftService.publishSite(site, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, siteKey, username, SITE_LAYOUT_NOT_PUBLISHED);
    }
    return describeStoredSite(siteKey, username);
  }

  /**
   * Drops the user's draft and starts a new one from the published layout,
   * as the editors' "start again" does. Exactly one of site_id and page_ref
   * names the site or the page. Returns the new draft, with its ids.
   */
  public Object discardLayoutDraft(Long siteId, String pageRef) throws ObjectNotFoundException, IllegalAccessException {
    if (pageRef != null) {
      refuseGiven(siteId, PAGE_REF_NOT_ACCEPTED);
      return discardPageDraft(pageRef);
    }
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readEditableSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    try {
      layoutDraftService.discardDraftSite(site, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, siteKey, username, SITE_LAYOUT_NOT_SAVED);
    }
    return describeStoredSite(siteKey, username);
  }

  /**
   * Replaces the whole content of the current user's draft of a page, as the
   * page editor's draft save does: the sections in order, each a dynamic
   * section of 1, 2, 3, 4, 6 or 12 columns holding its applications in
   * order per column, or a grid of up to 12 rows and 12 columns holding one
   * application per cell, with the styling and display of sections and
   * applications. Ids address the layout get_page_layout returned: the
   * user's draft when they have one, the published layout otherwise, in
   * which case the draft is created by this call through the editor's own
   * path and every kept application becomes a copy carrying its
   * preferences. Every value is checked before anything is written; one
   * save. Returns the draft as get_page_layout does, with the ids of every
   * section, cell and application.
   */
  public PageDescription setPageLayout(String pageRef, List<SectionDescription> sections) throws ObjectNotFoundException,
                                                                                            IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    String username = getCurrentUserName();
    readEditablePage(pageKey, username);
    long liveNodeId = requireLiveNodeId(pageKey, username);
    PageKey draftPageKey = layoutDraftService.getDraftPageKey(liveNodeId, username);
    boolean createDraft = draftPageKey == null;
    Page stored = pageLayoutService.getPageLayout(createDraft ? pageKey : draftPageKey);
    if (stored == null) {
      throw new ObjectNotFoundException(createDraft ? PAGE_NOT_FOUND : DRAFT_NOT_FOUND);
    }
    LayoutModel layout = layoutDescriptionService.toPageLayout(sections, stored, createDraft, username, getCurrentUserLocale());
    writePageDraft(pageKey, liveNodeId, draftPageKey, layout, username);
    return describeStoredPage(pageKey, liveNodeId, username);
  }

  /**
   * Sets the styling of sections, applications, topbars, bottom bars and
   * sidebars of the current user's draft of a page or a site, as the editors'
   * styling drawers do, in one draft save: each element addressed by the id
   * the read answered takes the blocks given merged over its stored ones,
   * every other element staying as it is. Exactly one of site_id and page_ref
   * names the layout. Ids address the layout get_page_layout or get_site
   * returned: the user's draft when they have one, the published layout
   * otherwise, in which case the draft is created by this call. Every value
   * is checked before anything is written. Returns the draft as
   * get_page_layout or get_site does, with its ids.
   */
  public Object setLayoutStyles(Long siteId, String pageRef, List<ElementStyling> elements) throws ObjectNotFoundException,
                                                                                              IllegalAccessException {
    if (pageRef != null) {
      refuseGiven(siteId, PAGE_REF_NOT_ACCEPTED);
      return setPageStyles(pageRef, elements);
    }
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readEditableSite(siteId, username);
    PortalConfig draftSite = readDraftSite(siteId, username);
    boolean createDraft = draftSite == null;
    LayoutModel layout = layoutDescriptionService.toStyledSite(elements, createDraft ? site : draftSite, createDraft, username);
    writeSiteDraft(site, draftSite, layout, username);
    return describeStoredSite(site.getSiteKey(), username);
  }

  private PageDescription setPageStyles(String pageRef, List<ElementStyling> elements) throws ObjectNotFoundException,
                                                                                       IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    String username = getCurrentUserName();
    readEditablePage(pageKey, username);
    long liveNodeId = requireLiveNodeId(pageKey, username);
    PageKey draftPageKey = layoutDraftService.getDraftPageKey(liveNodeId, username);
    boolean createDraft = draftPageKey == null;
    Page stored = pageLayoutService.getPageLayout(createDraft ? pageKey : draftPageKey);
    if (stored == null) {
      throw new ObjectNotFoundException(createDraft ? PAGE_NOT_FOUND : DRAFT_NOT_FOUND);
    }
    LayoutModel layout = layoutDescriptionService.toStyledPage(elements, stored, createDraft, username);
    writePageDraft(pageKey, liveNodeId, draftPageKey, layout, username);
    return describeStoredPage(pageKey, liveNodeId, username);
  }

  /**
   * Saves a page layout over the user's draft, the draft being created
   * through the editor's own path first when the user has none
   */
  private void writePageDraft(PageKey pageKey,
                              long liveNodeId,
                              PageKey draftPageKey,
                              LayoutModel layout,
                              String username) throws ObjectNotFoundException, IllegalAccessException {
    PageKey targetPageKey = draftPageKey;
    if (draftPageKey == null) {
      try {
        targetPageKey = layoutDraftService.createDraftPage(liveNodeId, username);
      } catch (ObjectNotFoundException e) {
        throw new ObjectNotFoundException(PAGE_NOT_FOUND);
      } catch (IllegalAccessException e) {
        throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
      } catch (RuntimeException e) {
        throw translateWriteFailure(e, pageKey, username, PAGE_LAYOUT_NOT_SAVED);
      }
    }
    try {
      pageLayoutService.updatePageLayout(targetPageKey.format(), layout.toPage(), false, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(STYLING_NOT_ACCEPTED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, pageKey, username, PAGE_LAYOUT_NOT_SAVED);
    }
  }

  /**
   * Creates a navigation item of a site with the 'Add an item' drawer's
   * inputs: a translatable name, a URL name chosen now and never changed, a
   * type with what it needs (a new page from a page template, an existing
   * page, a link, a group of pages), the tab it opens in, an icon, a visibility and its schedule, and the
   * permissions of a page created here; the item goes last under its parent.
   * An item opens from the menus in the same tab unless its target says
   * otherwise; the storage keeps the same tab on a blank one. Every value is
   * checked before anything is written: the page first, then
   * the item, a page left behind by an item failure being removed. Applies at
   * once; not a draft. Returns the item as list_site_pages describes it, its
   * labels omitted when the permissions written leave the caller no view
   * right on the page, as the navigation drawer cannot read them then either.
   */
  public NavigationItemDescription createPage(Long siteId, // NOSONAR
                                              Long parentNodeId,
                                              String label,
                                              Map<String, String> labels,
                                              String urlName,
                                              String type,
                                              Long pageTemplateId,
                                              String pageRef,
                                              String url,
                                              String target,
                                              String icon,
                                              Boolean visible,
                                              ScheduleDescription scheduled,
                                              List<String> accessPermissions,
                                              String editPermission) throws ObjectNotFoundException, IllegalAccessException {
    requireSiteId(siteId);
    String username = getCurrentUserName();
    PortalConfig site = readSite(siteId, username);
    SiteKey siteKey = site.getSiteKey();
    if (!aclService.canEditNavigation(siteKey, username)) {
      throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
    }
    NavigationItemType itemType = requireType(type);
    String itemLabel = requireLabel(label);
    Map<String, String> itemLabels = requireLabels(labels);
    String itemName = requireUrlName(urlName, itemLabel);
    String itemIcon = icon == null ? null : requireIcon(icon);
    boolean displayed = !Boolean.FALSE.equals(visible);
    NodeSchedule schedule = requireSchedule(scheduled, displayed);
    List<String> pageAccessPermissions = requireAccessPermissions(accessPermissions);
    String pageEditPermission = requireEditPermission(editPermission);
    PageCreateModel pageModel = null;
    String itemPageRef = null;
    String itemTarget = null;
    switch (itemType) {
    case PAGE:
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      refuseGiven(url, URL_NOT_ACCEPTED);
      pageModel = newPageModel(itemName, itemLabel, PageType.PAGE, null, resolvePageTemplateId(pageTemplateId), pageAccessPermissions, pageEditPermission);
      itemTarget = LinkTarget.parse(target).getStateValue();
      break;
    case LINK:
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      pageModel = newPageModel(itemName, itemLabel, PageType.LINK, requireUrl(url), null, pageAccessPermissions, pageEditPermission);
      itemTarget = LinkTarget.parse(target).getStateValue();
      break;
    case EXISTING_PAGE:
      refuseGiven(url, URL_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      refuseGiven(pageAccessPermissions, ACCESS_PERMISSIONS_NOT_ACCEPTED);
      refuseGiven(pageEditPermission, EDIT_PERMISSION_NOT_ACCEPTED);
      itemPageRef = requireExistingPage(pageRef, username).format();
      itemTarget = LinkTarget.parse(target).getStateValue();
      break;
    default:
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      refuseGiven(url, URL_NOT_ACCEPTED);
      refuseGiven(target, TARGET_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      refuseGiven(pageAccessPermissions, ACCESS_PERMISSIONS_NOT_ACCEPTED);
      refuseGiven(pageEditPermission, EDIT_PERMISSION_NOT_ACCEPTED);
      break;
    }
    long parentId = resolveParentNodeId(siteKey, parentNodeId, username);
    boolean temporal = schedule != null && !schedule.isCleared();
    NavigationCreateModel nodeModel = new NavigationCreateModel(parentId,
                                                                null,
                                                                itemLabel,
                                                                itemName,
                                                                displayed,
                                                                temporal,
                                                                false,
                                                                temporal ? schedule.start() : null,
                                                                temporal ? schedule.end() : null,
                                                                itemPageRef,
                                                                itemTarget,
                                                                false,
                                                                itemIcon,
                                                                withDefaultLanguage(itemLabels, itemLabel));
    NodeData node;
    try {
      node = navigationLayoutService.createNodeWithPage(nodeModel, pageModel, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(isParentNotFound(e, parentId) ? PARENT_NODE_NOT_FOUND : PAGE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateNavigationFailure(e, username, NODE_NOT_SAVED);
    }
    return describeStoredItem(siteKey, Long.parseLong(node.getId()), username);
  }

  /**
   * Updates a navigation item's name and translations, type, icon,
   * visibility and schedule and the permissions of its page: an option
   * omitted keeps its stored value, a translation given is merged over the
   * stored ones. The URL name is chosen at creation and cannot change: a
   * different one is refused with that reason. A type given changes the
   * item's page as the drawer's edit does: a group detaches the page, an
   * existing page re-points the item, a link rewrites the item's link page
   * or creates one, a page creates a new one from a template, on an item
   * that already points at a page only when the template is given, so the
   * type read back can be sent back unchanged; the previous page is kept,
   * so a repeated call that replaces a page leaves one behind each time;
   * the tab the item opens in stays as stored unless a target is given, as
   * the drawer's edit keeps it. An item whose page no longer exists
   * keeps its reference: type group drops it, page_ref points it elsewhere,
   * and type page without a template changes nothing on it. The
   * permissions go on the item's page, never in the call that points it at
   * another page. The page steps come first, then the item, and what was
   * written is put back when a later step fails. Applies at once; not a
   * draft. Returns the item as list_site_pages describes it, its labels
   * omitted when the permissions written leave the caller no view right on
   * the page, as the navigation drawer cannot read them then either.
   */
  public NavigationItemDescription updatePage(Long nodeId, // NOSONAR
                                              String label,
                                              Map<String, String> labels,
                                              String urlName,
                                              String type,
                                              Long pageTemplateId,
                                              String pageRef,
                                              String url,
                                              String target,
                                              String icon,
                                              Boolean visible,
                                              ScheduleDescription scheduled,
                                              List<String> accessPermissions,
                                              String editPermission) throws ObjectNotFoundException, IllegalAccessException {
    requireNodeId(nodeId, NODE_ID_NOT_ACCEPTED);
    String username = getCurrentUserName();
    NodeData node = readEditableNode(nodeId, username, NODE_NOT_FOUND);
    if (urlName != null && !StringUtils.equals(StringUtils.trim(urlName), node.getName())) {
      throw new IllegalArgumentException(URL_NAME_NOT_CHANGEABLE);
    }
    NodeState state = node.getState() == null ? NodeState.INITIAL : node.getState();
    PageKey storedPageKey = state.getPageRef();
    PageState storedPage = storedPageKey == null ? null : pageStateOrNull(storedPageKey);
    NavigationItemType storedType = storedPageKey == null ? NavigationItemType.GROUP :
                                  isLinkPage(storedPage) ? NavigationItemType.LINK : NavigationItemType.PAGE;
    NavigationItemType itemType = type == null ? pageRef == null ? storedType : NavigationItemType.EXISTING_PAGE : requireType(type);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setLabel(label == null ? null : requireLabel(label));
    update.setLabels(requireLabels(labels));
    update.setIcon(icon == null ? null : requireIcon(icon));
    update.setVisible(visible);
    boolean displayed = visible == null ? state.getVisibility() != Visibility.HIDDEN && state.getVisibility() != Visibility.SYSTEM
                                        : visible;
    update.setSchedule(requireSchedule(scheduled, displayed));
    update.setAccessPermissions(requireAccessPermissions(accessPermissions));
    update.setEditPermission(requireEditPermission(editPermission));
    switch (itemType) {
    case PAGE:
      refuseGiven(url, URL_NOT_ACCEPTED);
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      if (type != null && (storedPageKey == null || storedType == NavigationItemType.LINK || pageTemplateId != null)) {
        update.setNewPage(newPageModel(node.getName(), itemLabel(update, state), PageType.PAGE, null, resolvePageTemplateId(pageTemplateId), null, null));
      } else {
        refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      }
      break;
    case LINK:
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      if (storedType == NavigationItemType.LINK) {
        update.setLink(url == null ? null : requireUrl(url));
      } else {
        update.setNewPage(newPageModel(node.getName(), itemLabel(update, state), PageType.LINK, requireUrl(url), null, null, null));
      }
      break;
    case EXISTING_PAGE:
      refuseGiven(url, URL_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      refuseGiven(update.getAccessPermissions(), ACCESS_PERMISSIONS_NOT_ACCEPTED);
      refuseGiven(update.getEditPermission(), EDIT_PERMISSION_NOT_ACCEPTED);
      update.setPageRef(requireExistingPage(pageRef, username).format());
      break;
    default:
      refuseGiven(pageRef, PAGE_REF_NOT_ACCEPTED);
      refuseGiven(url, URL_NOT_ACCEPTED);
      refuseGiven(target, TARGET_NOT_ACCEPTED);
      refuseGiven(pageTemplateId, PAGE_TEMPLATE_NOT_ACCEPTED);
      refuseGiven(update.getAccessPermissions(), ACCESS_PERMISSIONS_NOT_ACCEPTED);
      refuseGiven(update.getEditPermission(), EDIT_PERMISSION_NOT_ACCEPTED);
      update.setDetachPage(type != null && storedPageKey != null);
      break;
    }
    if (target != null) {
      update.setTarget(LinkTarget.parse(target).getStateValue());
    }
    if (update.getNewPage() == null
        && (update.getLink() != null || update.getAccessPermissions() != null || update.getEditPermission() != null)) {
      PageKey pageKey = update.getPageRef() == null ? storedPageKey : PageKey.parse(update.getPageRef());
      if (pageKey == null) {
        throw new IllegalArgumentException(update.getLink() == null ? ACCESS_PERMISSIONS_NOT_ACCEPTED : URL_NOT_ACCEPTED);
      } else if (!aclService.canEditPage(pageKey, username)) {
        throw new IllegalAccessException(PAGE_NOT_ALLOWED);
      }
    }
    try {
      navigationLayoutService.updateNodeAndPagePermissions(nodeId, update, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(isNodeNotFound(e, nodeId) ? NODE_NOT_FOUND : PAGE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateNavigationFailure(e, username, NODE_NOT_SAVED);
    }
    return describeStoredItem(node.getSiteKey(), nodeId, username);
  }

  /**
   * Deletes a navigation item with the items under it, at once and for every
   * user, as the navigation drawer does after its confirmation; the page the
   * item pointed to is kept, as the drawer keeps it. An item the platform
   * manages itself (system visibility) is refused.
   */
  public void deletePage(Long nodeId) throws ObjectNotFoundException, IllegalAccessException {
    requireNodeId(nodeId, NODE_ID_NOT_ACCEPTED);
    String username = getCurrentUserName();
    readEditableNode(nodeId, username, NODE_NOT_FOUND);
    try {
      navigationLayoutService.deleteNode(nodeId, 0, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(NODE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
    } catch (RuntimeException e) {
      LOG.debug("Navigation item {} could not be deleted by {}", nodeId, username, e);
      throw new IllegalStateException(NODE_NOT_DELETED);
    }
  }

  /**
   * The live site the user may edit, read as get_site reads it
   */
  private PortalConfig readEditableSite(long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig site = readSite(siteId, username);
    if (!aclService.canEditSite(site.getSiteKey(), username)) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    }
    return site;
  }

  /**
   * A layout write refused by the storage answers a fixed text: the outdated
   * draft its own, every other failure the given one, its cause logged
   */
  private RuntimeException translateWriteFailure(RuntimeException e, Object layoutKey, String username, String refusal) {
    if (StringUtils.equals(e.getMessage(), SiteLayoutService.SITE_OUTDATED_MESSAGE)
        || StringUtils.equals(e.getMessage(), PAGE_OUTDATED_MESSAGE)) {
      return new IllegalStateException(LAYOUT_OUTDATED);
    }
    LOG.debug("Layout {} could not be written by {}", layoutKey, username, e);
    return new IllegalStateException(refusal);
  }

  private PageDescription publishPageLayout(String pageRef) throws ObjectNotFoundException, IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    String username = getCurrentUserName();
    readEditablePage(pageKey, username);
    long liveNodeId = requireLiveNodeId(pageKey, username);
    try {
      layoutDraftService.publishPage(pageKey, liveNodeId, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(STYLING_NOT_ACCEPTED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, pageKey, username, PAGE_LAYOUT_NOT_PUBLISHED);
    }
    return describeStoredPage(pageKey, liveNodeId, username);
  }

  private PageDescription discardPageDraft(String pageRef) throws ObjectNotFoundException, IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    String username = getCurrentUserName();
    readEditablePage(pageKey, username);
    long liveNodeId = requireLiveNodeId(pageKey, username);
    try {
      layoutDraftService.discardDraftPage(pageKey, liveNodeId, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(DRAFT_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, pageKey, username, PAGE_LAYOUT_NOT_SAVED);
    }
    return describeStoredPage(pageKey, liveNodeId, username);
  }

  /**
   * A live page the current user may edit: a draft page addressed by its own
   * reference does not exist for the tools
   */
  private void readEditablePage(PageKey pageKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (layoutDraftService.isDraftNode(pageKey.getName()) || layoutService.getPage(pageKey) == null) {
      throw new ObjectNotFoundException(PAGE_NOT_FOUND);
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
  }

  /**
   * The navigation item of the page's own site a draft of the page hangs on:
   * the item the user's draft was opened from when they have one, else the
   * first item showing the page
   */
  private long requireLiveNodeId(PageKey pageKey, String username) throws ObjectNotFoundException {
    Long liveNodeId = layoutDraftService.findLiveNodeId(pageKey, username);
    if (liveNodeId == null) {
      throw new ObjectNotFoundException(NODE_NOT_FOUND);
    }
    return liveNodeId;
  }

  /**
   * The page as a write of the current user leaves it, read as
   * get_page_layout reads it: their draft when their revision names one, the
   * published layout otherwise
   */
  private PageDescription describeStoredPage(PageKey pageKey, long liveNodeId, String username) throws ObjectNotFoundException,
                                                                                                  IllegalAccessException {
    PageKey draftPageKey = layoutDraftService.getDraftPageKey(liveNodeId, username);
    boolean draft = draftPageKey != null;
    ModelObject tree;
    try {
      tree = pageLayoutService.getPageLayout(draft ? draftPageKey : pageKey, username);
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
   * The site as stored after a write of the current user, read without the
   * view check the permissions written may have taken from them; the layout
   * still shows what they may access, their draft's when they have one
   */
  private SiteDescription describeStoredSite(SiteKey siteKey, String username) throws ObjectNotFoundException {
    PortalConfig site = layoutService.getPortalConfig(siteKey);
    if (site == null) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    }
    return describeSite(site, readDraftSite(site.getId(), username), username);
  }

  /**
   * A portal site's label and description are resolved as the sites
   * administration shows them, through the user's portal, which turns a
   * bundle expression into its text; the stored values stand for the other
   * site types, and when the caller may no longer view the site after a
   * write of their own
   */
  private SiteDescription describeSite(PortalConfig site, PortalConfig draftSite, String username) {
    PortalConfig source = draftSite == null ? site : draftSite;
    ModelObject layout = layoutDescriptionService.filterAccessible(source.getPortalLayout(), username);
    String label = StringUtils.defaultIfBlank(site.getLabel(), site.getName());
    String description = site.getDescription();
    if (StringUtils.equalsIgnoreCase(site.getType(), SiteType.PORTAL.getName())) {
      try {
        label = resolveLabel(siteLayoutService.getSiteLabels(site.getId(), username), label);
        description = resolveLabel(siteLayoutService.getSiteDescriptions(site.getId(), username), description);
      } catch (ObjectNotFoundException | IllegalAccessException e) {
        LOG.debug("Site {} labels are not readable by {} after their write, the stored values stand", site.getId(), username, e);
      }
    }
    return layoutDescriptionService.toSiteDescription(site, layout, label, description, draftSite != null);
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

  private String requireName(String name) {
    String siteName = StringUtils.trim(name);
    if (siteName == null || !NAME_PATTERN.matcher(siteName).matches()) {
      throw new IllegalArgumentException(NAME_NOT_ACCEPTED);
    }
    return siteName;
  }

  private String requireLabel(String label) {
    String siteLabel = StringUtils.trimToNull(label);
    if (siteLabel == null || siteLabel.length() > LABEL_MAX_LENGTH) {
      throw new IllegalArgumentException(LABEL_NOT_ACCEPTED);
    }
    return siteLabel;
  }

  /**
   * @return the description trimmed, an empty string when the caller clears
   *         it with a blank
   */
  private String requireDescription(String description) {
    String siteDescription = StringUtils.trim(description);
    if (siteDescription.length() > DESCRIPTION_MAX_LENGTH) {
      throw new IllegalArgumentException(DESCRIPTION_NOT_ACCEPTED);
    }
    return siteDescription;
  }

  private String requireIcon(String icon) {
    String siteIcon = StringUtils.trim(icon);
    if (siteIcon == null || !ICON_PATTERN.matcher(siteIcon).matches()) {
      throw new IllegalArgumentException(ICON_NOT_ACCEPTED);
    }
    return siteIcon;
  }

  /**
   * @return the permissions as stored, each trimmed, since a one-token
   *         expression is matched untrimmed by UserACL; null when none given
   */
  private List<String> requireAccessPermissions(List<String> accessPermissions) {
    if (accessPermissions == null) {
      return null;
    } else if (accessPermissions.isEmpty()) {
      throw new IllegalArgumentException(ACCESS_PERMISSIONS_NOT_ACCEPTED);
    }
    return accessPermissions.stream()
                            .map(expression -> requirePermission(expression, true, ACCESS_PERMISSIONS_NOT_ACCEPTED))
                            .toList();
  }

  /**
   * @return whether the sites administration lists the site: a portal site
   *         that is not a space's public site
   */
  private boolean isAdministeredSite(PortalConfig site) {
    return StringUtils.equalsIgnoreCase(site.getType(), PortalConfig.PORTAL_TYPE)
           && !Boolean.parseBoolean(site.getProperty(SpaceUtils.IS_PUBLIC_SITE_SPACE));
  }

  private String requireEditPermission(String editPermission) {
    String expression = StringUtils.trimToNull(editPermission);
    if (expression != null) {
      requirePermission(expression, false, EDIT_PERMISSION_NOT_ACCEPTED);
    }
    return expression;
  }

  /**
   * A permission expression as the permission selectors write it: Everyone
   * (for an access permission only), or a membership type and a group,
   * {@code *:/platform/users}, {@code member:/spaces/marketing}, both of
   * which must exist
   */
  private String requirePermission(String expression, boolean everyoneAllowed, String refusal) {
    String permission = StringUtils.trim(expression);
    if (StringUtils.equals(permission, UserACL.EVERYONE)) {
      if (!everyoneAllowed) {
        throw new IllegalArgumentException(refusal);
      }
      return permission;
    }
    var matcher = permission == null ? null : PERMISSION_PATTERN.matcher(permission);
    if (matcher == null || !matcher.matches()) {
      throw new IllegalArgumentException(refusal);
    }
    String membershipType = matcher.group(1);
    String groupId = matcher.group(2);
    try {
      if (organizationService.getGroupHandler().findGroupById(groupId) == null
          || (!StringUtils.equals(membershipType, "*")
              && organizationService.getMembershipTypeHandler().findMembershipType(membershipType) == null)) {
        throw new IllegalArgumentException(refusal);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      LOG.debug("Error looking up permission expression {}", permission, e);
      throw new IllegalArgumentException(refusal);
    }
    return permission;
  }

  /**
   * @return the id of the enabled site template whose layout has the given
   *         name, the basic one when none is given
   */
  private long resolveTemplateId(String template) {
    String layout = StringUtils.defaultIfBlank(StringUtils.trim(template), DEFAULT_TEMPLATE);
    return siteTemplateService.getSiteTemplates(null)
                              .stream()
                              .filter(t -> !t.isDisabled() && StringUtils.equals(t.getLayout(), layout))
                              .map(SiteTemplate::getId)
                              .findFirst()
                              .orElseThrow(() -> new IllegalArgumentException(TEMPLATE_NOT_ACCEPTED));
  }

  /**
   * An upload id is resolved before the first write; the save consumes it,
   * and a failure before the save releases it
   */
  private String requireUpload(String bannerUploadId) throws ObjectNotFoundException {
    String uploadId = StringUtils.trimToNull(bannerUploadId);
    if (uploadId != null && uploadService.getUploadResource(uploadId) == null) {
      throw new ObjectNotFoundException(UPLOAD_NOT_FOUND);
    }
    return uploadId;
  }

  private void releaseUpload(String uploadId) {
    if (uploadId != null) {
      uploadService.removeUploadResource(uploadId);
    }
  }

  /**
   * A node the user may change, read whatever their view right on its page;
   * a draft node is reached through its live node only, never by its own id
   */
  private NodeData readEditableNode(long nodeId, String username, String notFound) throws ObjectNotFoundException,
                                                                                    IllegalAccessException {
    NodeData node;
    try {
      node = navigationLayoutService.getEditableNode(nodeId, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(notFound);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
    }
    if (layoutDraftService.isDraftNode(node.getName())) {
      throw new ObjectNotFoundException(notFound);
    }
    return node;
  }

  /**
   * @return the parent given, which must be an item of the site, or the
   *         site's navigation root when none is given
   */
  private long resolveParentNodeId(SiteKey siteKey, Long parentNodeId, String username) throws ObjectNotFoundException,
                                                                                         IllegalAccessException {
    if (parentNodeId == null) {
      Long rootNodeId = navigationLayoutService.getRootNodeId(siteKey);
      if (rootNodeId == null) {
        throw new ObjectNotFoundException(PARENT_NODE_NOT_FOUND);
      }
      return rootNodeId;
    }
    requireNodeId(parentNodeId, PARENT_NODE_ID_NOT_ACCEPTED);
    NodeData parent = readEditableNode(parentNodeId, username, PARENT_NODE_NOT_FOUND);
    if (!siteKey.equals(parent.getSiteKey())) {
      throw new ObjectNotFoundException(PARENT_NODE_NOT_FOUND);
    }
    return parentNodeId;
  }

  private NavigationItemDescription describeStoredItem(SiteKey siteKey, long nodeId, String username) throws ObjectNotFoundException,
                                                                                                        IllegalAccessException {
    NavigationItemDescription item;
    try {
      item = layoutDescriptionService.describeNavigationItem(siteKey, nodeId, username);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(NAVIGATION_NOT_ALLOWED);
    }
    if (item == null) {
      throw new ObjectNotFoundException(NODE_NOT_FOUND);
    }
    return item;
  }

  /**
   * A navigation write refused by the service answers a fixed text: the name
   * rule and the schedule rules their option's, every other failure the given
   * one, its cause logged
   */
  private RuntimeException translateNavigationFailure(RuntimeException e, String username, String refusal) {
    String message = e.getMessage();
    if (e instanceof IllegalArgumentException) {
      if (StringUtils.equals(message, NavigationLayoutService.NODE_NAME_NOT_ACCEPTED)) {
        return new IllegalArgumentException(URL_NAME_NOT_ACCEPTED);
      } else if (StringUtils.equalsAny(message,
                                       NavigationLayoutService.SCHEDULE_END_BEFORE_START,
                                       NavigationLayoutService.SCHEDULE_START_PAST,
                                       NavigationLayoutService.SCHEDULE_ON_HIDDEN_NODE,
                                       NavigationLayoutService.SCHEDULE_INCOMPLETE)) {
        return new IllegalArgumentException(SCHEDULED_NOT_ACCEPTED);
      }
    }
    LOG.debug("Navigation item could not be written by {}", username, e);
    return new IllegalStateException(refusal);
  }

  /**
   * @return whether a not-found the service answered after the tool's own
   *         checks is the parent's, gone meanwhile, rather than a page's
   */
  private boolean isParentNotFound(ObjectNotFoundException e, long parentId) {
    return StringUtils.equals(e.getMessage(), String.format(NavigationLayoutService.PARENT_NODE_NOT_FOUND_MESSAGE, parentId));
  }

  private boolean isNodeNotFound(ObjectNotFoundException e, long nodeId) {
    return StringUtils.equals(e.getMessage(), String.format(NavigationLayoutService.NODE_NOT_FOUND_MESSAGE, nodeId));
  }

  /**
   * @return the stored page an item points at, null when that page no longer
   *         exists (a page of a deleted site); the item is then a page item
   *         still, as list_site_pages reads it: a target is accepted on it,
   *         permissions are refused as not allowed (no page, no edit
   *         right), and a link needs type link, which creates a link page
   */
  private PageState pageStateOrNull(PageKey pageKey) {
    PageContext pageContext = layoutService.getPageContext(pageKey);
    return pageContext == null ? null : pageContext.getState();
  }

  private boolean isLinkPage(PageState pageState) {
    return pageState != null && PageType.LINK.name().equalsIgnoreCase(pageState.getType());
  }

  /**
   * @return the title of a page created on update: the label given, else the
   *         stored one
   */
  private String itemLabel(NavigationItemUpdate update, NodeState state) {
    return update.getLabel() == null ? state.getLabel() : update.getLabel();
  }

  private PageCreateModel newPageModel(String name,
                                       String title,
                                       PageType pageType,
                                       String link,
                                       Long pageTemplateId,
                                       List<String> accessPermissions,
                                       String editPermission) {
    return new PageCreateModel(name,
                               title,
                               null,
                               null,
                               pageType.name(),
                               link,
                               pageTemplateId,
                               editPermission,
                               accessPermissions == null ? null : accessPermissions.toArray(new String[0]));
  }

  private NavigationItemType requireType(String type) {
    String value = StringUtils.trimToNull(type);
    if (value == null) {
      throw new IllegalArgumentException(TYPE_NOT_ACCEPTED);
    }
    return Arrays.stream(NavigationItemType.values())
                 .filter(t -> t.name().equalsIgnoreCase(value))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException(TYPE_NOT_ACCEPTED));
  }

  /**
   * The URL name as the drawer's input, or derived from the label as the
   * drawer derives it when the name is not customized: accents and every
   * character outside the name's class dropped, lower case
   */
  private String requireUrlName(String urlName, String label) {
    String name = StringUtils.trimToNull(urlName);
    if (name == null) {
      name = Normalizer.normalize(label, Normalizer.Form.NFD)
                       .replaceAll("\\p{M}", "")
                       .replaceAll("[^a-zA-Z0-9_-]", "")
                       .toLowerCase();
    }
    if (!NAME_PATTERN.matcher(name).matches() || name.contains(LayoutDraftService.DRAFT_NAME_MARKER)) {
      throw new IllegalArgumentException(URL_NAME_NOT_ACCEPTED);
    }
    return name;
  }

  /**
   * A link as an agent may write one: an absolute http or https address with
   * a host, or a site-relative path; any other scheme, javascript included,
   * a scheme-relative //host and a control character are refused, where the
   * drawer's own rule is looser
   */
  private String requireUrl(String url) {
    String link = StringUtils.trim(url);
    boolean wellFormed = StringUtils.isNotEmpty(link) && link.chars().noneMatch(c -> c <= 0x20 || c == 0x7f);
    boolean siteRelative = wellFormed && link.startsWith("/") && !(link.length() > 1 && (link.charAt(1) == '/' || link.charAt(1) == '\\'));
    boolean absolute = wellFormed && !link.startsWith("/") && ABSOLUTE_URL_PATTERN.matcher(link).matches();
    if (!siteRelative && !absolute) {
      throw new IllegalArgumentException(URL_NOT_ACCEPTED);
    }
    return link;
  }

  /**
   * @return the schedule's bounds as epoch milliseconds, both null for a
   *         schedule given empty, which drops the stored one; null when none
   *         is given
   */
  private NodeSchedule requireSchedule(ScheduleDescription scheduled, boolean displayed) {
    if (scheduled == null) {
      return null;
    }
    String start = StringUtils.trimToNull(scheduled.start());
    String end = StringUtils.trimToNull(scheduled.end());
    if (start == null && end == null) {
      return new NodeSchedule(null, null);
    } else if (start == null || end == null || !displayed) {
      throw new IllegalArgumentException(SCHEDULED_NOT_ACCEPTED);
    }
    long startTime = parseInstant(start);
    long endTime = parseInstant(end);
    if (startTime >= endTime || startTime <= System.currentTimeMillis()) {
      throw new IllegalArgumentException(SCHEDULED_NOT_ACCEPTED);
    }
    return new NodeSchedule(startTime, endTime);
  }

  private long parseInstant(String value) {
    try {
      return OffsetDateTime.parse(value).toInstant().toEpochMilli();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(SCHEDULED_NOT_ACCEPTED);
    }
  }

  /**
   * @return the translations given, each for a language the platform
   *         supports, null entries dropped; null when none given
   */
  private Map<String, String> requireLabels(Map<String, String> labels) {
    if (labels == null) {
      return null;
    }
    Set<String> supported = localeConfigService.getLocalConfigs()
                                               .stream()
                                               .map(LocaleConfig::getLocale)
                                               .map(Locale::toLanguageTag)
                                               .collect(Collectors.toSet());
    Map<String, String> translations = new LinkedHashMap<>();
    labels.forEach((tag, text) -> {
      if (text != null) {
        String language = StringUtils.trim(tag);
        String translation = StringUtils.trimToNull(text);
        if (!supported.contains(language) || translation == null || translation.length() > LABEL_MAX_LENGTH) {
          throw new IllegalArgumentException(LABELS_NOT_ACCEPTED);
        }
        translations.put(language, translation);
      }
    });
    return translations;
  }

  /**
   * @return the translations with the label as the platform default
   *         language's, as the drawer writes them
   */
  private Map<String, String> withDefaultLanguage(Map<String, String> labels, String label) {
    Map<String, String> translations = new LinkedHashMap<>();
    if (labels != null) {
      translations.putAll(labels);
    }
    translations.put(localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag(), label);
    return translations;
  }

  /**
   * @return the template given, which must be enabled, or the first enabled
   *         blank one the catalog lists when none is given
   */
  private long resolvePageTemplateId(Long pageTemplateId) {
    if (pageTemplateId == null) {
      return pageTemplateService.getPageTemplates()
                                .stream()
                                .filter(t -> !t.isDisabled() && StringUtils.equals(t.getCategory(), BLANK_TEMPLATE_CATEGORY))
                                .map(PageTemplate::getId)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(PAGE_TEMPLATE_NOT_ACCEPTED));
    }
    PageTemplate template = pageTemplateId <= 0 ? null : pageTemplateService.getPageTemplate(pageTemplateId);
    if (template == null || template.isDisabled()) {
      throw new IllegalArgumentException(PAGE_TEMPLATE_NOT_ACCEPTED);
    }
    return pageTemplateId;
  }

  /**
   * @return the key of an existing page the user may view, never a draft
   */
  private PageKey requireExistingPage(String pageRef, String username) throws ObjectNotFoundException, IllegalAccessException {
    PageKey pageKey = parsePageKey(pageRef);
    if (layoutDraftService.isDraftNode(pageKey.getName()) || layoutService.getPageContext(pageKey) == null) {
      throw new ObjectNotFoundException(PAGE_NOT_FOUND);
    } else if (!aclService.canViewPage(pageKey, username)) {
      throw new IllegalAccessException(PAGE_NOT_ALLOWED);
    }
    return pageKey;
  }

  private void refuseGiven(Object value, String refusal) {
    if (value != null) {
      throw new IllegalArgumentException(refusal);
    }
  }

  private void requireNodeId(Long nodeId, String refusal) {
    if (nodeId == null || nodeId <= 0) {
      throw new IllegalArgumentException(refusal);
    }
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
