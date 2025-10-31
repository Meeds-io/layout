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
package io.meeds.layout.service.injection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelUnmarshaller;
import org.exoplatform.portal.config.model.UnmarshalledObject;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PageTemplateDescriptor;
import io.meeds.layout.model.PageTemplateDescriptorList;
import io.meeds.layout.plugin.attachment.PageTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.PageTemplateTranslationPlugin;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PageTemplateImportService {

  private static final Scope             PAGE_TEMPLATE_IMPORT_SCOPE   = Scope.APPLICATION.id("PAGE_TEMPLATE_IMPORT");

  private static final Context           PAGE_TEMPLATE_CONTEXT        = Context.GLOBAL.id("PAGE_TEMPLATE");

  private static final String            PAGE_TEMPLATE_VERSION        = "version";

  private static final Log               LOG                          = ExoLogger.getLogger(PageTemplateImportService.class);

  private static final Random            RANDOM                       = new Random();

  @Autowired
  private LayoutTranslationImportService layoutTranslationService;

  @Autowired
  private LayoutAclService               layoutAclService;

  @Autowired
  private AttachmentService              attachmentService;

  @Autowired
  private PageTemplateService            pageTemplateService;

  @Autowired
  private SettingService                 settingService;

  @Autowired
  private ConfigurationManager           configurationManager;

  @Value("${meeds.pages.import.override:false}")
  private boolean                        forceReimportTemplates;

  @Value("${meeds.pages.import.version:3}")
  private long                           pageTemplateImportVersion;

  private List<PageTemplateDescriptor>   pageTemplateDescriptors;

  @PostConstruct
  public void init() {
    CompletableFuture.runAsync(this::importPageTemplates);
  }

  @ContainerTransactional
  public void importPageTemplates() {
    LOG.info("Importing Page Templates");
    if (!forceReimportTemplates
        && getSettingValue(PAGE_TEMPLATE_VERSION) != pageTemplateImportVersion) {
      forceReimportTemplates = true;
    }

    ConversationState.setCurrent(layoutAclService.getSuperUserConversationState());
    try {
      Enumeration<URL> templateFiles = PortalContainer.getInstance()
                                                      .getPortalClassLoader()
                                                      .getResources("page-templates.json");
      pageTemplateDescriptors = Collections.list(templateFiles)
                                           .stream()
                                           .map(this::parseDescriptors)
                                           .flatMap(List::stream)
                                           .sorted((d1, d2) -> {
                                             if (d1.getOrder() == d2.getOrder()) {
                                               return d1.getId().compareTo(d2.getId());
                                             } else {
                                               return d1.getOrder() - d2.getOrder();
                                             }
                                           })
                                           .toList();
      pageTemplateDescriptors.forEach(d -> importDescriptor(d, forceReimportTemplates));
      LOG.info("Importing Page Templates finished successfully");

      LOG.info("Processing Post Page Templates import");
      layoutTranslationService.postImport(PageTemplateTranslationPlugin.OBJECT_TYPE);
      LOG.info("Processing Post Page Templates import finished");

      setSettingValue(PAGE_TEMPLATE_VERSION, pageTemplateImportVersion);
    } catch (Exception e) {
      LOG.warn("An error occurred while importing page templates", e);
    } finally {
      ConversationState.setCurrent(null);
    }
  }

  public void restorePageTemplate(long pageTemplateId, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to restore a page template");
    }
    PageTemplate pageTemplate = pageTemplateService.getPageTemplate(pageTemplateId);
    if (pageTemplate == null) {
      throw new ObjectNotFoundException(String.format("Page Template with Id %s doesn't exists", pageTemplateId));
    }
    PageTemplateDescriptor descriptor = pageTemplateDescriptors.stream()
                                                               .filter(d -> getSettingValue(d.getId()) == pageTemplateId)
                                                               .findFirst()
                                                               .orElse(null);
    if (descriptor == null) {
      if (pageTemplate.isSystem()) {
        pageTemplate.setSystem(false);
        pageTemplateService.updatePageTemplate(pageTemplate);
      } else {
        throw new IllegalStateException(String.format("Page Template with Id %s isn't a system page template", pageTemplateId));
      }
    } else {
      savePageTemplate(descriptor, pageTemplateId);
    }
  }

  protected List<PageTemplateDescriptor> parseDescriptors(URL url) {
    try (InputStream inputStream = url.openStream()) {
      String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      PageTemplateDescriptorList list = JsonUtils.fromJsonString(content, PageTemplateDescriptorList.class);
      return list.getDescriptors();
    } catch (IOException e) {
      LOG.warn("An unkown error happened while parsing page templates from url {}", url, e);
      return Collections.emptyList();
    }
  }

  protected void importDescriptor(PageTemplateDescriptor descriptor, boolean override) {
    String descriptorId = descriptor.getId();
    long existingTemplateId = getSettingValue(descriptorId);
    if (override || existingTemplateId == 0) {
      importPageTemplate(descriptor, existingTemplateId, override);
    } else {
      LOG.debug("Ignore re-importing Page Template {}", descriptorId);
    }
  }

  protected void importPageTemplate(PageTemplateDescriptor d, long oldTemplateId, boolean override) {
    LOG.info("Importing Page Template {}", d.getId());
    try {
      PageTemplate pageTemplate = savePageTemplate(d, oldTemplateId);
      if (override || oldTemplateId == 0 || pageTemplate.getId() != oldTemplateId) {
        LOG.info("Importing Page Template {} title translations", d.getId());
        saveNames(d, pageTemplate);
        LOG.info("Importing Page Template {} description translations", d.getId());
        saveDescriptions(d, pageTemplate);
        LOG.info("Importing Page Template {} illustration", d.getId());
        saveIllustration(pageTemplate.getId(), d.getIllustrationPath());
        // Mark as imported
        setSettingValue(d.getId(), pageTemplate.getId());
      }
      LOG.info("Importing Page Template {} finished successfully", d.getId());
    } catch (Exception e) {
      LOG.warn("An error occurred while importing page template {}", d.getId(), e);
    }
  }

  protected void saveNames(PageTemplateDescriptor d, PageTemplate pageTemplate) {
    layoutTranslationService.saveTranslationLabels(PageTemplateTranslationPlugin.OBJECT_TYPE,
                                                   pageTemplate.getId(),
                                                   PageTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                   d.getNames());
  }

  protected void saveDescriptions(PageTemplateDescriptor d, PageTemplate pageTemplate) {
    layoutTranslationService.saveRichTranslationLabels(PageTemplateTranslationPlugin.OBJECT_TYPE,
                                                       pageTemplate.getId(),
                                                       PageTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                       d.getDescriptions());
  }

  @SneakyThrows
  protected PageTemplate savePageTemplate(PageTemplateDescriptor d, long oldTemplateId) {
    PageTemplate pageTemplate = null;
    if (oldTemplateId > 0) {
      pageTemplate = pageTemplateService.getPageTemplate(oldTemplateId);
    }
    boolean isNew = pageTemplate == null;
    if (isNew) {
      pageTemplate = new PageTemplate();
    }
    pageTemplate.setCategory(d.getCategory());
    pageTemplate.setSystem(d.isSystem());
    try (InputStream is = configurationManager.getInputStream(d.getLayoutPath())) {
      String xml = IOUtil.getStreamContentAsString(is);
      Container layout = fromXML(xml);
      pageTemplate.setContent(JsonUtils.toJsonString(new LayoutModel(layout)));
    }
    if (isNew) {
      return pageTemplateService.createPageTemplate(pageTemplate);
    } else {
      return pageTemplateService.updatePageTemplate(pageTemplate);
    }
  }

  protected void saveIllustration(long pageTemplateId, String imagePath) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(imagePath);
      String uploadId = "PageTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.deleteAttachments(PageTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(pageTemplateId));
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       PageTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(pageTemplateId),
                                       null,
                                       layoutAclService.getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving Image '%s' as attachment for template '%s'",
                                                    imagePath,
                                                    pageTemplateId),
                                      e);
    } finally {
      if (tempFile != null) {
        try {
          Files.delete(tempFile.toPath());
        } catch (IOException e) {
          tempFile.deleteOnExit();
        }
      }
    }
  }

  @SneakyThrows
  protected Container fromXML(String xml) {
    UnmarshalledObject<Container> obj = ModelUnmarshaller.unmarshall(Container.class, xml.getBytes(StandardCharsets.UTF_8));
    return obj.getObject();
  }

  protected void setSettingValue(String name, long id) {
    settingService.set(PAGE_TEMPLATE_CONTEXT,
                       PAGE_TEMPLATE_IMPORT_SCOPE,
                       name,
                       SettingValue.create(String.valueOf(id)));
  }

  protected long getSettingValue(String name) {
    try {
      SettingValue<?> settingValue = settingService.get(PAGE_TEMPLATE_CONTEXT, PAGE_TEMPLATE_IMPORT_SCOPE, name);
      return settingValue == null || settingValue.getValue() == null ? 0l : Long.parseLong(settingValue.getValue().toString());
    } catch (NumberFormatException e) {
      return 0l;
    }
  }

  private File getIllustrationFile(String imagePath) throws Exception {
    try (InputStream inputStream = configurationManager.getInputStream(imagePath)) {
      File tempFile = File.createTempFile("temp", ".png");
      FileUtils.copyInputStreamToFile(inputStream, tempFile);
      return tempFile;
    }
  }

}
