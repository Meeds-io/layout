/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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

import static io.meeds.layout.util.EntityMapper.SITE_ENABLED_PROP;
import static io.meeds.layout.util.EntityMapper.toSiteTemplate;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.plugin.attachment.SiteTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SiteTemplateTranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@Service
public class SiteTemplateService {

  public static final String      SITE_TEMPLATE_BASE     = "basic";

  public static final String      TEMPLATE_CREATED_EVENT = "layout.siteTemplate.created";

  public static final String      TEMPLATE_UPDATED_EVENT = "layout.siteTemplate.updated";

  public static final String      TEMPLATE_DELETED_EVENT = "layout.siteTemplate.deleted";

  @Autowired
  private LayoutService           layoutService;

  @Autowired
  private NavigationService       navigationService;

  @Autowired
  private UserPortalConfigService portalConfigService;

  @Autowired
  private LayoutAclService        aclService;

  @Autowired
  private TranslationService      translationService;

  @Autowired
  private AttachmentService       attachmentService;

  @Autowired
  private ListenerService         listenerService;

  public List<SiteTemplate> getSiteTemplates(Locale locale) {
    SiteFilter siteFilter = new SiteFilter();
    siteFilter.setSiteType(SiteType.PORTAL_TEMPLATE);
    List<PortalConfig> sites = layoutService.getSites(siteFilter);
    return sites.stream()
                .map(s -> toSiteTemplateDetails(s, locale))
                .toList();
  }

  public SiteTemplate getSiteTemplate(long id) throws ObjectNotFoundException {
    return getSiteTemplate(id, null);
  }

  public SiteTemplate getSiteTemplate(long id, Locale locale) throws ObjectNotFoundException {
    PortalConfig portalConfig = layoutService.getPortalConfig(id);
    if (portalConfig == null || !StringUtils.equalsIgnoreCase(portalConfig.getType(), PortalConfig.PORTAL_TEMPLATE)) {
      throw new ObjectNotFoundException(String.format("Site Template with id %s doesn't exists", id));
    }
    return toSiteTemplateDetails(portalConfig, locale);
  }

  public SiteTemplate createSiteTemplate(SiteTemplate siteTemplate, String username) throws IllegalAccessException,
                                                                                     ObjectAlreadyExistsException {
    if (!aclService.isAdministrator(username)) {
      throw new IllegalAccessException();
    } else if (layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout())) != null) {
      throw new ObjectAlreadyExistsException(String.format("Site Template with name %s already exists",
                                                           siteTemplate.getLayout()));
    }
    return createSiteTemplate(siteTemplate, null, username, true);
  }

  public SiteTemplate createSiteTemplate(SiteTemplate siteTemplate) {
    return createSiteTemplate(siteTemplate, null, null, true);
  }

  public SiteTemplate updateSiteTemplate(SiteTemplate siteTemplate, String username) throws IllegalAccessException,
                                                                                     ObjectNotFoundException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteTemplate.getId());
    if (portalConfig == null || !StringUtils.equalsIgnoreCase(portalConfig.getType(), PortalConfig.PORTAL_TEMPLATE)) {
      throw new ObjectNotFoundException(String.format("Site with id %s doesn't exist", siteTemplate.getId()));
    } else if (!aclService.isAdministrator(username)) {
      throw new IllegalAccessException();
    }
    return updateSiteTemplate(siteTemplate, username, true);
  }

  public SiteTemplate updateSiteTemplate(SiteTemplate siteTemplate) {
    return updateSiteTemplate(siteTemplate, null, true);
  }

  public void deleteSiteTemplate(long id, String username) throws IllegalAccessException, ObjectNotFoundException {
    SiteTemplate siteTemplate = getSiteTemplate(id);
    if (!aclService.isAdministrator(username) || siteTemplate.isSystem()) {
      throw new IllegalAccessException(String.format("Site with id %s can't be deleted by user %s", id, username));
    }
    SiteKey siteKey = SiteKey.portalTemplate(siteTemplate.getLayout());
    navigationService.destroyNavigation(siteKey);
    layoutService.removePages(siteKey);

    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    layoutService.remove(portalConfig);
    listenerService.broadcast(TEMPLATE_DELETED_EVENT, siteTemplate, username);
  }

  public SiteTemplate saveAsSiteTemplate(SiteTemplate siteTemplate, long siteId, String username) throws IllegalAccessException,
                                                                                                  ObjectNotFoundException,
                                                                                                  ObjectAlreadyExistsException {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteId);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with id %s doesn't exists", siteId));
    } else if (!aclService.isAdministrator(username)) {
      throw new IllegalAccessException();
    } else if (layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout())) != null) {
      throw new ObjectAlreadyExistsException(String.format("Site Template with name %s already exists",
                                                           siteTemplate.getLayout()));
    }
    return createSiteTemplate(siteTemplate, new SiteKey(portalConfig.getType(), portalConfig.getName()), username, true);
  }

  @SneakyThrows
  public SiteTemplate createSiteTemplate(SiteTemplate siteTemplate,
                                          SiteKey sourceSiteKey,
                                          String username,
                                          boolean broadcast) {
    PortalConfig portalConfig = layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout()));
    if (portalConfig == null) {
      portalConfigService.createSiteFromTemplate(sourceSiteKey == null ? SiteKey.portalTemplate(SITE_TEMPLATE_BASE) :
                                                                       sourceSiteKey,
                                                 SiteKey.portalTemplate(siteTemplate.getLayout()));
      portalConfig = layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout()));
    }
    siteTemplate.setId(portalConfig.getId());
    SiteTemplate createdSiteTemplate = updateSiteTemplate(siteTemplate, portalConfig);
    if (broadcast) {
      listenerService.broadcast(TEMPLATE_CREATED_EVENT, createdSiteTemplate, username);
    }
    return createdSiteTemplate;
  }

  private SiteTemplate updateSiteTemplate(SiteTemplate siteTemplate, String username, boolean broadcast) {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteTemplate.getId());
    SiteTemplate updatedSiteTemplate = updateSiteTemplate(siteTemplate, portalConfig);
    if (broadcast) {
      listenerService.broadcast(TEMPLATE_UPDATED_EVENT, updatedSiteTemplate, username);
    }
    return updatedSiteTemplate;
  }

  private SiteTemplate updateSiteTemplate(SiteTemplate siteTemplate, PortalConfig portalConfig) {
    portalConfig.setLabel(siteTemplate.getName());
    portalConfig.setDescription(siteTemplate.getDescription());
    portalConfig.setRemovable(!siteTemplate.isSystem());
    portalConfig.setIcon(siteTemplate.getIcon());
    portalConfig.setAccessPermissions(new String[] { UserACL.EVERYONE });
    portalConfig.setEditPermission(getAdministratorsPermission());
    portalConfig.setProperty(SITE_ENABLED_PROP, String.valueOf(!siteTemplate.isDisabled()));
    layoutService.save(portalConfig);
    return toSiteTemplateDetails(portalConfig, null);
  }

  private SiteTemplate toSiteTemplateDetails(PortalConfig portalConfig, Locale locale) {
    SiteTemplate siteTemplate = toSiteTemplate(portalConfig);
    if (locale != null) {
      computeSiteTemplateAttributes(locale, portalConfig, siteTemplate);
    }
    return siteTemplate;
  }

  private void computeSiteTemplateAttributes(Locale locale, PortalConfig portalConfig, SiteTemplate siteTemplate) {
    siteTemplate.setName(translationService.getTranslationLabelOrDefault(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                         portalConfig.getId(),
                                                                         SiteTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                                         locale));
    siteTemplate.setDescription(translationService.getTranslationLabelOrDefault(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                portalConfig.getId(),
                                                                                SiteTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                                                locale));
    siteTemplate.setIllustrationId(getSiteTemplateAttributes(portalConfig.getId()));
  }

  private long getSiteTemplateAttributes(long siteTemplateId) {
    List<String> attachmentFileIds = attachmentService.getAttachmentFileIds(SiteTemplateAttachmentPlugin.OBJECT_TYPE,
                                                                            String.valueOf(siteTemplateId));
    if (CollectionUtils.isNotEmpty(attachmentFileIds)) {
      return Long.parseLong(attachmentFileIds.get(0));
    } else {
      return 0l;
    }
  }

  private String getAdministratorsPermission() {
    return "*:" + aclService.getAdministratorsGroup();
  }

}
