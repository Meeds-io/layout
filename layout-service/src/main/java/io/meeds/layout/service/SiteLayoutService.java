/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.meeds.layout.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.util.LayoutStyleValidator;

import lombok.SneakyThrows;

@Service
public class SiteLayoutService {

  private static final String     SITE_NOT_FOUND_MESSAGE = "Site with key %s doesn't exist";

  private static final String     EDIT_DENIED_MESSAGE    = "Site with key %s can't be edited by user %s";

  private static final String     SITE_DOESNT_EXIST_MSG  = "Site %s doesn't exist";

  private static final String     DRAFT_NAME_CONFLICT    = "layout.siteDraftNameConflict";

  private static final String     DRAFT_OWNER_SEPARATOR  = "_draft_";

  private static final Log        LOG                    = ExoLogger.getLogger(SiteLayoutService.class);

  @Autowired
  private LayoutService           layoutService;

  @Autowired
  private NavigationService       navigationService;

  @Autowired
  private LocaleConfigService     localeConfigService;

  @Autowired
  private UserPortalConfigService portalConfigService;

  @Autowired
  private PortletInstanceService  portletInstanceService;

  @Autowired
  private LayoutAclService        aclService;

  @Autowired
  private ListenerService         listenerService;

  @Autowired
  private NavigationLayoutService navigationLayoutService;

  private Map<String, String>     supportedLanguages;

  private Locale                  defaultConfiguredLocale;

  public PortalConfig getSite(long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteId);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with id %s doesn't exists", siteId));
    } else if (!aclService.canViewSite(new SiteKey(portalConfig.getType(), portalConfig.getName()), username)) {
      throw new IllegalAccessException();
    }
    return portalConfig;
  }

  public ModelObject getSiteLayout(SiteKey siteKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig site = getSite(siteKey, username);
    SiteLayoutUnresolvedException.checkResolved(site);
    return site.getPortalLayout();
  }

  public PortalConfig getSite(SiteKey siteKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with key %s doesn't exists", siteKey));
    } else if (!aclService.canViewSite(siteKey, username)) {
      throw new IllegalAccessException();
    }
    return portalConfig;
  }

  @SneakyThrows
  public PortalConfig createSite(SiteCreateModel createModel, String username) throws IllegalAccessException,
                                                                               ObjectAlreadyExistsException {
    PortalConfig portalConfig = createModel.getPortalConfig();
    SiteKey siteKey = SiteKey.portal(portalConfig.getName());
    if (!aclService.canAddSite(username)) {
      throw new IllegalAccessException();
    } else if (layoutService.getPortalConfig(siteKey) != null) {
      throw new ObjectAlreadyExistsException(String.format("Site with name %s already exists",
                                                           createModel.getPortalConfig().getName()));
    }
    PortalConfig templatePortalConfig = layoutService.getPortalConfig(createModel.getSiteId());
    if (templatePortalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with id %s doesn't exist",
                                                      createModel.getSiteId()));
    }
    SiteLayoutUnresolvedException.checkResolved(templatePortalConfig);

    String[] accessPermissions = portalConfig.getAccessPermissions() == null ?
                                                                             new String[] {
                                                                               getAdministratorsPermission() } :
                                                                             portalConfig.getAccessPermissions();
    String editPermission = portalConfig.getEditPermission() == null ?
                                                                     getAdministratorsPermission() :
                                                                     portalConfig.getEditPermission();

    portalConfigService.createSiteFromTemplate(new SiteKey(templatePortalConfig.getType(), templatePortalConfig.getName()),
                                               siteKey);

    PortalConfig createdPortalConfig = layoutService.getPortalConfig(siteKey);
    createdPortalConfig.setDescription(portalConfig.getDescription());
    createdPortalConfig.setLabel(portalConfig.getLabel());
    createdPortalConfig.setDisplayed(portalConfig.isDisplayed());
    createdPortalConfig.setIcon(portalConfig.getIcon());
    createdPortalConfig.setDisplayOrder(portalConfig.isDisplayed() ? portalConfig.getDisplayOrder() : 0);
    createdPortalConfig.setAccessPermissions(accessPermissions);
    createdPortalConfig.setEditPermission(editPermission);
    createdPortalConfig.setRemovable(true);
    if (StringUtils.isNotBlank(portalConfig.getBannerUploadId())) {
      createdPortalConfig.setBannerUploadId(portalConfig.getBannerUploadId());
    }
    layoutService.save(createdPortalConfig);
    return createdPortalConfig;
  }

  public SiteKey createDraftSite(SiteKey siteKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format("Not allowed to edit site %s", siteKey));
    }
    PortalConfig site = getSite(siteKey, username);
    SiteLayoutUnresolvedException.checkResolved(site);

    String clonedSiteName = site.getType() + "_" + site.getName() + DRAFT_OWNER_SEPARATOR + username;

    PortalConfig draftSite = site.clone();
    draftSite.setType(PortalConfig.DRAFT);
    draftSite.setName(clonedSiteName);
    if (!aclService.canEditSite(draftSite, username)) {
      // The name could also be another existing user's draft of another site:
      // the user couldn't open it, and replacing it could drop that user's draft
      throw new IllegalArgumentException(DRAFT_NAME_CONFLICT);
    }
    LayoutModel siteLayoutModel = new LayoutModel(site.getPortalLayout(), portletInstanceService, new PortletInstanceContext(false, null));
    siteLayoutModel.resetStorage();
    draftSite.setPortalLayout(siteLayoutModel.toSite().getPortalLayout());
    SiteKey draftSiteKey = new SiteKey(draftSite.getType(), draftSite.getName());
    PortalConfig existingDraftSite = layoutService.getPortalConfig(draftSiteKey);
    if (existingDraftSite != null) {
      layoutService.remove(existingDraftSite);
    }
    layoutService.create(draftSite);
    return draftSiteKey;
  }

  /**
   * Removes the draft sites of a deleted user: every draft site whose name ends
   * with {@code _draft_<username>}, a name another user could also have
   * produced included. A non-administrator couldn't open such a draft while the
   * deleted user existed and could afterwards; an administrator's draft of that
   * name is removed too.
   *
   * @param username deleted user
   */
  @ContainerTransactional
  public void deleteUserDraftSites(String username) {
    removeUserDraftSites(username);
  }

  public void updateSite(SiteUpdateModel updateModel, String username) throws IllegalAccessException,
                                                                       ObjectNotFoundException {
    SiteKey siteKey = new SiteKey(updateModel.getSiteType(), updateModel.getSiteName());
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_NOT_FOUND_MESSAGE, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format(EDIT_DENIED_MESSAGE, siteKey, username));
    }
    portalConfig.setDescription(updateModel.getSiteDescription());
    portalConfig.setLabel(updateModel.getSiteLabel());
    portalConfig.setIcon(updateModel.getSiteIcon());
    portalConfig.setDisplayed(updateModel.isDisplayed());
    portalConfig.setDisplayOrder(updateModel.isDisplayed() ? updateModel.getDisplayOrder() : 0);
    if (updateModel.isBannerRemoved() && portalConfig.getBannerFileId() != 0) {
      layoutService.removeSiteBanner(siteKey.getName());
      portalConfig.setBannerFileId(0);
    } else if (StringUtils.isNotBlank(updateModel.getBannerUploadId())) {
      portalConfig.setBannerUploadId(updateModel.getBannerUploadId());
    }
    layoutService.save(portalConfig);
  }

  public void restoreSite(SiteKey siteKey,
                          ImportMode importMode,
                          boolean restoreSiteConfig,
                          boolean restorePages,
                          boolean restoreNavigationTree,
                          String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_NOT_FOUND_MESSAGE, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format(EDIT_DENIED_MESSAGE, siteKey, username));
    } else if (!portalConfigService.canRestore(siteKey.getTypeName(), siteKey.getName())) {
      throw new IllegalStateException(String.format("Site %s can't be restored since it isn't a default site ", siteKey));
    }
    Set<PageKey> previouslyReachablePages = restorePages || restoreNavigationTree ? getReachablePages(siteKey) : null;
    portalConfigService.restoreSite(siteKey.getTypeName(),
                                    siteKey.getName(),
                                    importMode,
                                    restoreSiteConfig,
                                    restorePages,
                                    restoreNavigationTree);
    if (previouslyReachablePages != null) {
      try {
        broadcastRestoredPages(siteKey, previouslyReachablePages, restorePages, username);
      } catch (RuntimeException e) {
        LOG.warn("Site {} is restored, but the search index of its pages isn't refreshed", siteKey, e);
      }
    }
  }

  public void deleteSite(SiteKey siteKey, String username) throws IllegalAccessException, ObjectNotFoundException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_NOT_FOUND_MESSAGE, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format("Site with key %s can't be deleted by user %s", siteKey, username));
    }
    navigationService.destroyNavigation(siteKey);
    layoutService.removePages(siteKey);
    layoutService.remove(portalConfig);
  }

  public void updateSitePermissions(PermissionUpdateModel permissionUpdateModel,
                                    String username) throws IllegalAccessException, ObjectNotFoundException {
    SiteKey siteKey = new SiteKey(permissionUpdateModel.getSiteType(), permissionUpdateModel.getSiteName());
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_DOESNT_EXIST_MSG, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format("Site permissions with key %s can't be edited by user %s",
                                                     siteKey,
                                                     username));
    }
    if (!StringUtils.isBlank(permissionUpdateModel.getEditPermission())) {
      portalConfig.setEditPermission(permissionUpdateModel.getEditPermission());
    }
    if (permissionUpdateModel.getAccessPermissions() != null) {
      portalConfig.setAccessPermissions(permissionUpdateModel.getAccessPermissions().toArray(new String[0]));
    }
    layoutService.save(portalConfig);
  }

  public void updateSiteLayout(SiteKey siteKey,
                               PortalConfig site,
                               boolean publish,
                               String username) throws IllegalAccessException, ObjectNotFoundException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_DOESNT_EXIST_MSG, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format("Site layout with key %s can't be edited by user %s",
                                                     siteKey,
                                                     username));
    }
    LayoutStyleValidator.validate(site.getPortalLayout());
    portalConfig.setPortalLayout(site.getPortalLayout());
    try {
      if (publish) {
        // Update Site Layout only without resetting preferences
        portalConfig.resetStorage();
      } else {
        // Check Site Layout consistency before saving
        portalConfig.checkStorage();
      }
    } catch (ObjectNotFoundException e) {
      LOG.debug("Error while accessing Site applications storage information", e);
      throw new IllegalStateException("layout.siteOutdatedError");
    }
    layoutService.save(portalConfig);
  }

  public NodeLabel getSiteLabels(Long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return getSiteLabel(siteId, username, true);
  }

  public NodeLabel getSiteDescriptions(Long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return getSiteLabel(siteId, username, false);
  }

  private NodeLabel getSiteLabel(long siteId, String username, boolean isLabel) throws ObjectNotFoundException,
                                                                                IllegalAccessException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteId);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with id %s doesn't exists", siteId));
    } else if (!aclService.canViewSite(new SiteKey(portalConfig.getType(), portalConfig.getName()), username)) {
      throw new IllegalAccessException();
    }
    Locale defaultLocale = getDefaultLocale();

    NodeLabel nodeLabel = new NodeLabel();
    nodeLabel.setDefaultLanguage(defaultLocale.getLanguage());
    nodeLabel.setSupportedLanguages(getSupportedLanguages(defaultLocale));
    Map<String, String> labels = getLabels(portalConfig, defaultLocale, username, isLabel);
    nodeLabel.setLabels(labels);
    return nodeLabel;
  }

  private Map<String, String> getLabels(PortalConfig portalConfig, Locale defaultLocale, String username, boolean isLabel) {
    SiteKey siteKey = new SiteKey(SiteType.valueOf(portalConfig.getType().toUpperCase()), portalConfig.getName());
    UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig(siteKey.getName(), username);
    UserPortal userPortal = userPortalConfig.getUserPortal();

    Map<String, String> labels = new HashMap<>();
    localeConfigService.getLocalConfigs()
                       .stream()
                       .map(LocaleConfig::getLocale)
                       .forEach(locale -> {
                         String translatedLabel = isLabel ? userPortal.getPortalLabel(siteKey, locale) :
                                                          userPortal.getPortalDescription(siteKey, locale);
                         labels.put(LocaleContextInfo.getLocaleAsString(locale), translatedLabel);
                       });
    if (!labels.containsKey(defaultLocale.getLanguage()) && !labels.isEmpty()) {
      labels.put(defaultLocale.getLanguage(), labels.values().iterator().next());
    }
    return labels;
  }

  private Map<String, String> getSupportedLanguages(Locale defaultLocale) {
    if (supportedLanguages == null || !defaultConfiguredLocale.equals(defaultLocale)) {
      supportedLanguages = CollectionUtils.isEmpty(localeConfigService.getLocalConfigs()) ?
                                                                                          Collections.singletonMap(defaultLocale.toLanguageTag(),
                                                                                                                   defaultLocale.getDisplayName()) :
                                                                                          localeConfigService.getLocalConfigs()
                                                                                                             .stream()
                                                                                                             .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocale()
                                                                                                                                                                     .toLanguageTag(),
                                                                                                                                                         "ma"))
                                                                                                             .map(LocaleConfig::getLocale)
                                                                                                             .collect(Collectors.toMap(Locale::toLanguageTag,
                                                                                                                                       Locale::getDisplayName));
      defaultConfiguredLocale = defaultLocale;
    }
    return supportedLanguages;
  }

  private Locale getDefaultLocale() {
    return localeConfigService.getDefaultLocaleConfig() == null ? Locale.ENGLISH :
                                                                localeConfigService.getDefaultLocaleConfig()
                                                                                   .getLocale();
  }

  private String getAdministratorsPermission() {
    return "*:" + aclService.getAdministratorsGroup();
  }

  private Set<PageKey> getReachablePages(SiteKey siteKey) {
    try {
      return navigationLayoutService.getReachablePages(siteKey);
    } catch (RuntimeException e) {
      LOG.warn("The restore of site {} won't refresh the search index of its pages: its navigation can't be read", siteKey, e);
      return null;
    }
  }

  /**
   * A restore saves the pages and rebuilds the navigation of the site without
   * the events of the editors. A page that a node of the site leads to is
   * reindexed, for its content blocks and its path, as after a page save. A
   * page whose last node in the site the restore removed is unindexed, as
   * after a node deletion. A page that no node of the site led to is reached,
   * if at all, from another site: it is reindexed when its content is
   * restored, and left as it is otherwise.
   */
  private void broadcastRestoredPages(SiteKey siteKey,
                                      Set<PageKey> previouslyReachablePages,
                                      boolean pagesRestored,
                                      String username) {
    Set<PageKey> reachablePages = navigationLayoutService.getReachablePages(siteKey);
    for (PageContext pageContext : layoutService.findPages(siteKey)) {
      PageKey pageKey = pageContext.getKey();
      if (reachablePages.contains(pageKey) || (pagesRestored && !previouslyReachablePages.contains(pageKey))) {
        listenerService.broadcast(PageLayoutService.PAGE_UPDATED_EVENT, username, pageKey.format());
      } else if (previouslyReachablePages.contains(pageKey)) {
        Page page = layoutService.getPage(pageKey);
        if (page != null) {
          listenerService.broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, this, page);
        }
      }
    }
  }

  void removeUserDraftSites(String username) {
    if (StringUtils.isBlank(username)) {
      return;
    }
    String draftSiteNameSuffix = DRAFT_OWNER_SEPARATOR + username;
    // Sites are sorted by label, which a draft shares with the site it drafts:
    // pages of that order could skip a draft, so all draft sites are read at once
    SiteFilter siteFilter = new SiteFilter();
    siteFilter.setSiteType(SiteType.DRAFT);
    List<PortalConfig> userDraftSites = layoutService.getSites(siteFilter)
                                                     .stream()
                                                     .filter(draftSite -> StringUtils.endsWith(draftSite.getName(),
                                                                                               draftSiteNameSuffix))
                                                     .toList();
    userDraftSites.forEach(this::removeDraftSite);
  }

  private void removeDraftSite(PortalConfig draftSite) {
    try {
      layoutService.remove(draftSite);
    } catch (Exception e) {
      LOG.warn("Error while removing the draft site {} of a deleted user", draftSite.getName(), e);
    }
  }

}
