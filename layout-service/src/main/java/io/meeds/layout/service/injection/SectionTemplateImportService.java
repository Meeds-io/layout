/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.model.SectionTemplateDescriptor;
import io.meeds.layout.model.SectionTemplateDescriptorList;
import io.meeds.layout.plugin.attachment.SectionTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SectionTemplateTranslationPlugin;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.SectionTemplateService;
import io.meeds.layout.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SectionTemplateImportService {

  private static final String             SECTION_TEMPLATE_IMPORT       = "SECTION_TEMPLATE_IMPORT";

  private static final Scope              SECTION_TEMPLATE_IMPORT_SCOPE =
                                                                        Scope.APPLICATION.id(SECTION_TEMPLATE_IMPORT);

  private static final Context            SECTION_TEMPLATE_CONTEXT      = Context.GLOBAL.id("SECTION_TEMPLATE");

  private static final String             SECTION_TEMPLATE_VERSION      = "version";

  private static final Log                LOG                           = ExoLogger.getLogger(SectionTemplateImportService.class);

  private static final Random             RANDOM                        = new Random();

  @Autowired
  private LayoutAclService                layoutAclService;

  @Autowired
  private LayoutTranslationImportService  layoutTranslationService;

  @Autowired
  private AttachmentService               attachmentService;

  @Autowired
  private SectionTemplateService          sectionTemplateService;

  @Autowired
  private SettingService                  settingService;

  @Autowired
  private ConfigurationManager            configurationManager;

  @Value("${meeds.sections.import.override:false}")
  private boolean                         forceReimport;

  @Value("${meeds.sections.import.version:1}")
  private long                            sectionTemplateImportVersion;

  private List<SectionTemplateDescriptor> sectionTemplateDescriptors;

  @PostConstruct
  public void init() {
    CompletableFuture.runAsync(this::importSectionTemplates);
  }

  @ContainerTransactional
  public void importSectionTemplates() {
    if (!forceReimport && getSettingValue(SECTION_TEMPLATE_VERSION) != sectionTemplateImportVersion) {
      forceReimport = true;
    }
    LOG.info("Importing Section Templates with version {}, force reimport = {}", sectionTemplateImportVersion, forceReimport);

    ConversationState.setCurrent(layoutAclService.getSuperUserConversationState());
    try {
      sectionTemplateDescriptors = Collections.list(getClass().getClassLoader()
                                                              .getResources("section-templates.json"))
                                              .stream()
                                              .map(this::parseDescriptors)
                                              .flatMap(List::stream)
                                              .toList();
      sectionTemplateDescriptors.forEach(d -> importDescriptor(d, forceReimport));
      LOG.info("Importing Section Templates finished successfully.");

      LOG.info("Processing Post Section Templates import");
      layoutTranslationService.postImport(SectionTemplateTranslationPlugin.OBJECT_TYPE);
      LOG.info("Processing Post Section Templates import finished");

      setSettingValue(SECTION_TEMPLATE_VERSION, sectionTemplateImportVersion);
    } catch (Exception e) {
      LOG.warn("An error occurred while importing Section Templates", e);
    } finally {
      ConversationState.setCurrent(null);
    }
  }

  public void restoreSectionTemplate(long sectionTemplateId, String username) throws ObjectNotFoundException,
                                                                              IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't authorized to restore a section template");
    }
    SectionTemplate sectionTemplate = sectionTemplateService.getSectionTemplate(sectionTemplateId);
    if (sectionTemplate == null) {
      throw new ObjectNotFoundException(String.format("Section Template with Id %s doesn't exists", sectionTemplateId));
    }
    SectionTemplateDescriptor descriptor = sectionTemplateDescriptors.stream()
                                                                     .filter(d -> getSettingValue(d.getNameId())
                                                                         == sectionTemplateId)
                                                                     .findFirst()
                                                                     .orElse(null);
    if (descriptor == null) {
      if (sectionTemplate.isSystem()) {
        sectionTemplate.setSystem(false);
        sectionTemplateService.updateSectionTemplate(sectionTemplate);
      } else {
        throw new IllegalStateException(String.format("Section Template with Id %s isn't a system Section template",
                                                      sectionTemplateId));
      }
    } else {
      saveSectionTemplate(descriptor, sectionTemplateId);
    }
  }

  protected List<SectionTemplateDescriptor> parseDescriptors(URL url) {
    try (InputStream inputStream = url.openStream()) {
      String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      SectionTemplateDescriptorList list = JsonUtils.fromJsonString(content, SectionTemplateDescriptorList.class);
      return list.getDescriptors();
    } catch (IOException e) {
      LOG.warn("An unkown error happened while parsing Section Templates from url {}", url, e);
      return Collections.emptyList();
    }
  }

  protected void importDescriptor(SectionTemplateDescriptor descriptor, boolean override) {
    String descriptorId = descriptor.getNameId();
    long existingId = getSettingValue(descriptorId);
    if (override || existingId == 0) {
      importSectionTemplate(descriptor, existingId, override);
    } else {
      LOG.debug("Ignore re-importing Section Template {}", descriptorId);
    }
  }

  protected void importSectionTemplate(SectionTemplateDescriptor d, long oldId, boolean override) {
    String descriptorId = d.getNameId();
    LOG.debug("Importing Section Template {}", descriptorId);
    try {
      SectionTemplate sectionTemplate = saveSectionTemplate(d, oldId);
      if (sectionTemplate == null) {
        return;
      }
      if (override || oldId == 0 || sectionTemplate.getId() != oldId) {
        LOG.debug("Importing Section Template {} title translations", descriptorId);
        saveNames(d, sectionTemplate);
        LOG.debug("Importing Section Template {} description translations", descriptorId);
        saveDescriptions(d, sectionTemplate);
        if (StringUtils.isNotBlank(d.getIllustrationPath())) {
          LOG.debug("Importing Section Template {} illustration", descriptorId);
          saveIllustration(sectionTemplate.getId(), d.getIllustrationPath());
        }
        // Mark as imported
        setSettingValue(descriptorId, sectionTemplate.getId());
      }
      LOG.debug("Importing Section Template {} finished successfully", descriptorId);
    } catch (Exception e) {
      LOG.warn("An error occurred while importing Section Template {}", descriptorId, e);
    }
  }

  protected void saveNames(SectionTemplateDescriptor d, SectionTemplate sectionTemplate) {
    layoutTranslationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                   sectionTemplate.getId(),
                                                   SectionTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                   d.getNames());
  }

  protected void saveDescriptions(SectionTemplateDescriptor d, SectionTemplate sectionTemplate) {
    layoutTranslationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                   sectionTemplate.getId(),
                                                   SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                   d.getDescriptions());
  }

  @SneakyThrows
  protected SectionTemplate saveSectionTemplate(SectionTemplateDescriptor d, long oldId) {
    SectionTemplate sectionTemplate = null;
    if (oldId > 0) {
      sectionTemplate = sectionTemplateService.getSectionTemplate(oldId);
    }
    boolean isNew = sectionTemplate == null;
    if (isNew) {
      sectionTemplate = new SectionTemplate();
    }
    sectionTemplate.setContent(d.getContent());
    sectionTemplate.setCategory(d.getCategory());
    sectionTemplate.setSystem(d.isSystem());
    sectionTemplate.setDisabled(d.isDisabled());
    if (isNew) {
      return sectionTemplateService.createSectionTemplate(sectionTemplate);
    } else {
      return sectionTemplateService.updateSectionTemplate(sectionTemplate);
    }
  }

  protected void saveIllustration(long sectionTemplateId, String imagePath) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(imagePath);
      String uploadId = "SectionTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.deleteAttachments(SectionTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(sectionTemplateId));
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SectionTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(sectionTemplateId),
                                       null,
                                       layoutAclService.getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving Image '%s' as attachment for Section Template '%s'",
                                                    imagePath,
                                                    sectionTemplateId),
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

  protected void setSettingValue(String name, long value) {
    settingService.set(SECTION_TEMPLATE_CONTEXT,
                       SECTION_TEMPLATE_IMPORT_SCOPE,
                       name,
                       SettingValue.create(String.valueOf(value)));
  }

  protected long getSettingValue(String name) {
    try {
      SettingValue<?> settingValue = settingService.get(SECTION_TEMPLATE_CONTEXT, SECTION_TEMPLATE_IMPORT_SCOPE, name);
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
