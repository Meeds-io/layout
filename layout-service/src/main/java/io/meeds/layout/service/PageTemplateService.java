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

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.plugin.attachment.PageTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.PageTemplateTranslationPlugin;
import io.meeds.layout.storage.PageTemplateStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

@Service
public class PageTemplateService {

  private static final Log    LOG = ExoLogger.getLogger(PageTemplateService.class);

  private LayoutAclService    layoutAclService;

  private TranslationService  translationService;

  private AttachmentService   attachmentService;

  private LocaleConfigService localeConfigService;

  private PageTemplateStorage pageTemplateStorage;

  public PageTemplateService(LayoutAclService layoutAclService,
                             TranslationService translationService,
                             AttachmentService attachmentService,
                             LocaleConfigService localeConfigService,
                             PageTemplateStorage pageTemplateStorage) {
    this.layoutAclService = layoutAclService;
    this.translationService = translationService;
    this.attachmentService = attachmentService;
    this.localeConfigService = localeConfigService;
    this.pageTemplateStorage = pageTemplateStorage;
  }

  public List<PageTemplate> getPageTemplates() {
    return getPageTemplates(null, false);
  }

  public List<PageTemplate> getPageTemplates(boolean expand) {
    return getPageTemplates(null, expand);
  }

  public List<PageTemplate> getPageTemplates(Locale locale, boolean expand) {
    return getPageTemplates(locale, expand, true);
  }

  public List<PageTemplate> getPageTemplates(Locale locale, boolean expand, boolean retrieveContent) {
    List<PageTemplate> pageTemplates = pageTemplateStorage.getPageTemplates();
    if (expand) {
      pageTemplates.forEach(pageTemplate -> computePageTemplateAttributes(locale, pageTemplate, retrieveContent));
    }
    return pageTemplates;
  }

  public PageTemplate getPageTemplate(long id, Locale locale, boolean expand) {
    return getPageTemplate(id, locale, expand, false);
  }

  public PageTemplate getPageTemplate(long id, Locale locale, boolean expand, boolean retrieveContent) {
    PageTemplate pageTemplate = pageTemplateStorage.getPageTemplate(id);
    if (expand) {
      computePageTemplateAttributes(locale, pageTemplate, retrieveContent);
    }
    return pageTemplate;
  }

  public PageTemplate getPageTemplate(long id) {
    return pageTemplateStorage.getPageTemplate(id);
  }

  public PageTemplate createPageTemplate(PageTemplate pageTemplate, String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to create a page template");
    }
    return createPageTemplate(pageTemplate);
  }

  public PageTemplate createPageTemplate(PageTemplate pageTemplate) {
    return pageTemplateStorage.createPageTemplate(pageTemplate);
  }

  public void deletePageTemplate(long templateId, String username) throws IllegalAccessException, ObjectNotFoundException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to create a page template");
    }
    PageTemplate pageTemplate = getPageTemplate(templateId);
    if (pageTemplate == null) {
      throw new ObjectNotFoundException("Page template doesn't exist");
    }
    if (pageTemplate.isSystem()) {
      throw new IllegalAccessException("Can't delete a system page template");
    }
    deletePageTemplate(templateId);
  }

  public void deletePageTemplate(long templateId) throws ObjectNotFoundException {
    try {
      attachmentService.deleteAttachments(PageTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(templateId));
    } catch (Exception e) {
      LOG.debug("Error while deleting attachments of deleted Page Template", e);
    }
    try {
      translationService.deleteTranslationLabels(PageTemplateTranslationPlugin.OBJECT_TYPE, templateId);
    } catch (ObjectNotFoundException e) {
      LOG.debug("Error while deleting translation labels of deleted Page Template", e);
    }
    pageTemplateStorage.deletePageTemplate(templateId);
  }

  public PageTemplate updatePageTemplate(PageTemplate pageTemplate, String username) throws ObjectNotFoundException,
                                                                                     IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to update a page template");
    }
    return updatePageTemplate(pageTemplate);
  }

  public PageTemplate updatePageTemplate(PageTemplate pageTemplate) throws ObjectNotFoundException {
    return pageTemplateStorage.updatePageTemplate(pageTemplate);
  }

  private String getLabel(long templateId, String fieldName, Locale locale) {
    if (locale == null) {
      locale = localeConfigService.getDefaultLocaleConfig().getLocale();
    }
    try {
      TranslationField translationField = translationService.getTranslationField(PageTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                 templateId,
                                                                                 fieldName);
      if (translationField != null && MapUtils.isNotEmpty(translationField.getLabels())) {
        String label = translationField.getLabels().get(locale);
        if (label == null) {
          Locale defaultLocale = localeConfigService.getDefaultLocaleConfig().getLocale();
          label = translationField.getLabels().get(defaultLocale);
        }
        if (label == null) {
          label = translationField.getLabels().values().iterator().next();
        }
        return label;
      } else {
        return null;
      }
    } catch (ObjectNotFoundException e) {
      return null;
    }
  }

  private void computePageTemplateAttributes(Locale locale, PageTemplate pageTemplate, boolean retrieveContent) {
    pageTemplate.setName(getLabel(pageTemplate.getId(), PageTemplateTranslationPlugin.TITLE_FIELD_NAME, locale));
    pageTemplate.setDescription(getLabel(pageTemplate.getId(), PageTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME, locale));
    List<String> attachmentFileIds = attachmentService.getAttachmentFileIds(PageTemplateAttachmentPlugin.OBJECT_TYPE,
                                                                            String.valueOf(pageTemplate.getId()));
    if (CollectionUtils.isNotEmpty(attachmentFileIds)) {
      pageTemplate.setIllustrationId(Long.parseLong(attachmentFileIds.get(0)));
    }
    if (!retrieveContent) {
      pageTemplate.setContent(null);
    }
  }

}
