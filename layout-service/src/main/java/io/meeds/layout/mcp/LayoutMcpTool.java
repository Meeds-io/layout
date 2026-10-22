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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteLayoutUpdate;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SitePropertiesUpdate;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The read tools of the layout, the site tools and the site layout tools:
 * every public method is an MCP tool described in ai-tool-definitions.json. A
 * tool maps its arguments, checks them against the vocabulary the editors
 * accept, calls the Services as the current user and translates their
 * refusals into the fixed texts the agent reads: {@code does not exist:
 * <object>}, {@code not allowed: <object>}, {@code value is not accepted for
 * <option>}. A change to a site's properties applies at once, as the sites
 * administration's does; a change to its layout is the user's draft until
 * publish_layout, as in the site layout editor.
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

  public static final String       STYLING_NOT_ACCEPTED  = "value is not accepted for styling";

  public static final String       DEFAULT_TEMPLATE      = "basic";

  public static final String       DEFAULT_ICON          = "fa-globe";

  public static final int          LABEL_MAX_LENGTH      = 200;

  public static final int          DESCRIPTION_MAX_LENGTH = 2000;

  private static final Log         LOG                   = ExoLogger.getLogger(LayoutMcpTool.class);

  private static final Pattern     NAME_PATTERN          = Pattern.compile("^[a-zA-Z0-9_-]{1,200}$");

  private static final Pattern     ICON_PATTERN          = Pattern.compile("^[a-z0-9-]+( [a-z0-9-]+)*$");

  private static final Pattern     PERMISSION_PATTERN    = Pattern.compile("^(\\*|[a-zA-Z0-9_-]+):(/[a-zA-Z0-9_./-]+)$");

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
   * absent from the draft. Ids address the layout get_site returned: the
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
    try {
      SiteKey draftSiteKey = createDraft ? layoutDraftService.createDraftSite(site, username) : draftSite.getSiteKey();
      siteLayoutService.updateSiteLayout(draftSiteKey, layout.toSite(), false, username);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(SITE_NOT_ALLOWED);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(STYLING_NOT_ACCEPTED);
    } catch (RuntimeException e) {
      throw translateWriteFailure(e, siteKey, username, SITE_LAYOUT_NOT_SAVED);
    }
    return describeStoredSite(siteKey, username);
  }

  /**
   * Makes the user's site draft the published layout, for every viewer at
   * once, as the site layout editor's publish does: the draft the user's
   * revision names is written over the live site, every container and
   * application taking a new id, and the draft ends. Returns the site as
   * published, with the new ids.
   */
  public SiteDescription publishLayout(Long siteId) throws ObjectNotFoundException, IllegalAccessException {
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
   * Drops the user's site draft and starts a new one from the published
   * layout, as the site layout editor's "start again" does. Returns the new
   * draft, with its ids.
   */
  public SiteDescription discardLayoutDraft(Long siteId) throws ObjectNotFoundException, IllegalAccessException {
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
  private RuntimeException translateWriteFailure(RuntimeException e, SiteKey siteKey, String username, String refusal) {
    if (StringUtils.equals(e.getMessage(), SiteLayoutService.SITE_OUTDATED_MESSAGE)) {
      return new IllegalStateException(LAYOUT_OUTDATED);
    }
    LOG.debug("Site layout {} could not be written by {}", siteKey, username, e);
    return new IllegalStateException(refusal);
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
