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
import java.util.Locale;
import java.util.Map;
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
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.social.core.plugin.SiteTranslationPlugin;

import io.meeds.layout.util.LayoutStyleValidator;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@Service
public class SiteLayoutService {

  public static final String      DISPLAYED_NOT_ACCEPTED = "value is not accepted for displayed";

  /**
   * Broadcast with the acting username and the SiteKey after a publication
   * replaced a site's layout, whatever the site's kind (portal, group, user,
   * template): a listener keyed on pages or spaces checks the kind itself
   */
  public static final String      SITE_PUBLISHED_EVENT   = "layout.site.published";

  public static final String      SITE_OUTDATED_MESSAGE  = "layout.siteOutdatedError";

  private static final String     SITE_NOT_FOUND_MESSAGE = "Site with key %s doesn't exist";

  private static final String     EDIT_DENIED_MESSAGE    = "Site with key %s can't be edited by user %s";

  private static final String     SITE_DOESNT_EXIST_MSG  = "Site %s doesn't exist";

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
  private TranslationService      translationService;

  @Autowired
  private ListenerService         listenerService;

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

    String[] accessPermissions = portalConfig.getAccessPermissions() == null ?
                                                                             new String[] {
                                                                               getAdministratorsPermission() } :
                                                                             portalConfig.getAccessPermissions();
    String editPermission = portalConfig.getEditPermission() == null ?
                                                                     getAdministratorsPermission() :
                                                                     portalConfig.getEditPermission();

    portalConfigService.createSiteFromTemplate(new SiteKey(templatePortalConfig.getType(), templatePortalConfig.getName()),
                                               siteKey);
    try {
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
    } catch (RuntimeException e) {
      removeCreatedSite(siteKey);
      throw e;
    }
  }

  public SiteKey createDraftSite(SiteKey siteKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format("Not allowed to edit site %s", siteKey));
    }
    PortalConfig site = getSite(siteKey, username);

    String clonedSiteName = site.getType() + "_" + site.getName() + "_draft_" + username;

    PortalConfig draftSite = site.clone();
    draftSite.setType(PortalConfig.DRAFT);
    draftSite.setName(clonedSiteName);
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

  public void updateSite(SiteUpdateModel updateModel, String username) throws IllegalAccessException,
                                                                       ObjectNotFoundException {
    SiteKey siteKey = new SiteKey(updateModel.getSiteType(), updateModel.getSiteName());
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_NOT_FOUND_MESSAGE, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format(EDIT_DENIED_MESSAGE, siteKey, username));
    }
    applySiteProperties(portalConfig, updateModel);
  }

  /**
   * Updates a site's permissions, then its properties, the way the sites
   * administration does it in two requests: the permissions first, so a
   * refusal on them leaves the properties untouched; then the properties,
   * every one the caller did not state kept from the stored site. A stated
   * label or description is also written as the site's translation in the
   * platform's default language, the one the sites list shows ahead of the
   * stored property, as the form's main field does; the other languages keep
   * their translation. The edit right is checked once, before the first
   * write, so handing the edit right to a group the caller is not in applies,
   * with or without properties, as the permissions drawer's own request does.
   * The properties save, which consumes the banner upload, comes last: when
   * it fails, or when a translation write fails, what was written before it
   * is put back, the permissions first, then each default-language
   * translation attempted, to the value it held or to none; a put-back that
   * fails itself is logged and the first failure is the one raised.
   *
   * @param  siteKey                 the site to update
   * @param  properties              the properties the caller states, null
   *                                   fields kept from the stored site
   * @param  permissions             the permissions to write first, null to
   *                                   leave them
   * @param  username                the acting user
   * @throws ObjectNotFoundException when the site does not exist
   * @throws IllegalAccessException  when the user may not edit the site
   */
  public void updateSiteAndPermissions(SiteKey siteKey,
                                       SitePropertiesUpdate properties,
                                       PermissionUpdateModel permissions,
                                       String username) throws ObjectNotFoundException, IllegalAccessException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format(SITE_NOT_FOUND_MESSAGE, siteKey));
    } else if (!aclService.canEditSite(siteKey, username)) {
      throw new IllegalAccessException(String.format(EDIT_DENIED_MESSAGE, siteKey, username));
    }
    long siteId = portalConfig.getId();
    String[] storedAccessPermissions = portalConfig.getAccessPermissions() == null ? null :
                                                                                   portalConfig.getAccessPermissions().clone();
    String storedEditPermission = portalConfig.getEditPermission();
    SiteUpdateModel updateModel = toUpdateModel(portalConfig, properties);
    Locale defaultLocale = getDefaultLocale();
    String storedLabel = storedTranslation(siteId, SiteTranslationPlugin.SITE_LABEL_FIELD_NAME, defaultLocale, properties.label());
    String storedDescription = storedTranslation(siteId,
                                                 SiteTranslationPlugin.SITE_DESCRIPTION_FIELD_NAME,
                                                 defaultLocale,
                                                 properties.description());
    if (permissions != null) {
      updateSitePermissions(permissions, username);
    }
    boolean labelWritten = false;
    boolean descriptionWritten = false;
    try {
      if (properties.label() != null) {
        labelWritten = true;
        saveTranslation(siteId, SiteTranslationPlugin.SITE_LABEL_FIELD_NAME, defaultLocale, properties.label());
      }
      if (properties.description() != null) {
        descriptionWritten = true;
        saveTranslation(siteId, SiteTranslationPlugin.SITE_DESCRIPTION_FIELD_NAME, defaultLocale, properties.description());
      }
      applySiteProperties(layoutService.getPortalConfig(siteKey), updateModel);
    } catch (Exception e) {
      if (permissions != null) {
        restorePermissions(siteKey, storedAccessPermissions, storedEditPermission);
      }
      if (labelWritten) {
        restoreTranslation(siteId, SiteTranslationPlugin.SITE_LABEL_FIELD_NAME, defaultLocale, storedLabel);
      }
      if (descriptionWritten) {
        restoreTranslation(siteId, SiteTranslationPlugin.SITE_DESCRIPTION_FIELD_NAME, defaultLocale, storedDescription);
      }
      throw e;
    }
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
    portalConfigService.restoreSite(siteKey.getTypeName(),
                                    siteKey.getName(),
                                    importMode,
                                    restoreSiteConfig,
                                    restorePages,
                                    restoreNavigationTree);
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

  /**
   * Replaces a site's layout with the given one: a draft save checks that
   * every window the tree names still exists, a publication recreates every
   * window with the preferences it carries and broadcasts the site
   * publication event, as the site layout editor's publish does
   */
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
      throw new IllegalStateException(SITE_OUTDATED_MESSAGE);
    }
    layoutService.save(portalConfig);
    if (publish) {
      listenerService.broadcast(SITE_PUBLISHED_EVENT, username, siteKey);
    }
  }

  public NodeLabel getSiteLabels(Long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return getSiteLabel(siteId, username, true);
  }

  public NodeLabel getSiteDescriptions(Long siteId, String username) throws ObjectNotFoundException, IllegalAccessException {
    return getSiteLabel(siteId, username, false);
  }

  private SiteUpdateModel toUpdateModel(PortalConfig portalConfig, SitePropertiesUpdate properties) {
    SiteUpdateModel updateModel = new SiteUpdateModel();
    updateModel.setSiteType(portalConfig.getType());
    updateModel.setSiteName(portalConfig.getName());
    updateModel.setSiteLabel(properties.label() == null ? portalConfig.getLabel() : properties.label());
    updateModel.setSiteDescription(properties.description() == null ? portalConfig.getDescription() :
                                                                       StringUtils.trimToNull(properties.description()));
    updateModel.setSiteIcon(properties.icon() == null ? portalConfig.getIcon() : properties.icon());
    if (Boolean.FALSE.equals(properties.displayed()) && isMetaSite(portalConfig)) {
      throw new IllegalArgumentException(DISPLAYED_NOT_ACCEPTED);
    }
    updateModel.setDisplayed(properties.displayed() == null ? portalConfig.isDisplayed() : properties.displayed());
    updateModel.setDisplayOrder(portalConfig.getDisplayOrder());
    updateModel.setBannerUploadId(properties.bannerUploadId());
    updateModel.setBannerRemoved(Boolean.TRUE.equals(properties.removeBanner()));
    return updateModel;
  }

  /**
   * @return the translation a field holds in the default language before an
   *         update writes the given value over it, a blank when it holds
   *         none; null when the update writes nothing there
   */
  private String storedTranslation(long siteId, String fieldName, Locale locale, String value) {
    if (value == null) {
      return null;
    }
    String stored = translationService.getTranslationLabel(SiteTranslationPlugin.SITE_OBJECT_TYPE, siteId, fieldName, locale);
    return StringUtils.defaultString(stored);
  }

  private void saveTranslation(long siteId, String fieldName, Locale locale, String value) throws ObjectNotFoundException {
    translationService.saveTranslationLabel(SiteTranslationPlugin.SITE_OBJECT_TYPE, siteId, fieldName, locale, value);
  }

  private void restoreTranslation(long siteId, String fieldName, Locale locale, String storedValue) {
    try {
      if (StringUtils.isBlank(storedValue)) {
        translationService.deleteTranslationLabel(SiteTranslationPlugin.SITE_OBJECT_TYPE, siteId, fieldName, locale);
      } else {
        translationService.saveTranslationLabel(SiteTranslationPlugin.SITE_OBJECT_TYPE, siteId, fieldName, locale, storedValue);
      }
    } catch (Exception e) {
      LOG.warn("The {} translation of site {} could not be put back after a failed update", fieldName, siteId, e);
    }
  }

  private void applySiteProperties(PortalConfig portalConfig, SiteUpdateModel updateModel) throws ObjectNotFoundException {
    portalConfig.setDescription(updateModel.getSiteDescription());
    portalConfig.setLabel(updateModel.getSiteLabel());
    portalConfig.setIcon(updateModel.getSiteIcon());
    portalConfig.setDisplayed(updateModel.isDisplayed());
    portalConfig.setDisplayOrder(updateModel.isDisplayed() ? updateModel.getDisplayOrder() : 0);
    if (updateModel.isBannerRemoved() && portalConfig.getBannerFileId() != 0) {
      layoutService.removeSiteBanner(updateModel.getSiteName());
      portalConfig.setBannerFileId(0);
    } else if (StringUtils.isNotBlank(updateModel.getBannerUploadId())) {
      portalConfig.setBannerUploadId(updateModel.getBannerUploadId());
    }
    layoutService.save(portalConfig);
  }

  /**
   * The meta site is the one the sites administration form never lets out of
   * the sites menu
   */
  private boolean isMetaSite(PortalConfig portalConfig) {
    return StringUtils.equalsIgnoreCase(portalConfig.getType(), PortalConfig.PORTAL_TYPE)
           && StringUtils.equals(portalConfig.getName(), portalConfigService.getMetaPortal());
  }

  /**
   * Removes the site a failed creation left behind, instantiated from its
   * template but without the stated properties, so a retry can create it.
   * The clone carries the template's removable flag, false on a system
   * template, and the site storage refuses to remove such a site: the flag
   * is saved first, without the banner upload the failed save consumed, and
   * nothing is destroyed when that save fails, which leaves the site whole.
   */
  private void removeCreatedSite(SiteKey siteKey) {
    try {
      PortalConfig createdPortalConfig = layoutService.getPortalConfig(siteKey);
      if (createdPortalConfig != null) {
        createdPortalConfig.setBannerUploadId(null);
        createdPortalConfig.setRemovable(true);
        layoutService.save(createdPortalConfig);
        navigationService.destroyNavigation(siteKey);
        layoutService.removePages(siteKey);
        layoutService.remove(layoutService.getPortalConfig(siteKey));
      }
    } catch (Exception e) {
      LOG.warn("Site {} could not be removed after its creation failed", siteKey, e);
    }
  }

  private void restorePermissions(SiteKey siteKey, String[] accessPermissions, String editPermission) {
    try {
      PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
      if (portalConfig != null) {
        portalConfig.setAccessPermissions(accessPermissions);
        portalConfig.setEditPermission(editPermission);
        layoutService.save(portalConfig);
      }
    } catch (RuntimeException e) {
      LOG.warn("The permissions of site {} could not be put back after a failed update", siteKey, e);
    }
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

}
