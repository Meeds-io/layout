/*
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
package io.meeds.layout.plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.meeds.layout.model.*;
import io.meeds.layout.plugin.attachment.SectionTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SectionTemplateTranslationPlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.SectionTemplateService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.portal.config.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadResource;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

import static io.meeds.layout.util.DatabindUtils.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SectionTemplateDatabindPlugin implements DatabindPlugin {

  private static final Random    RANDOM      = new Random();

  public static final String     OBJECT_TYPE = "SectionTemplate";

  public static final String     CONFIG_JSON = "config.json";

  public static final String     LAYOUT_JSON = "layout.json";

  @Autowired
  private SectionTemplateService sectionTemplateService;

  @Autowired
  private DatabindService        databindService;

  @Autowired
  private FileService            fileService;

  @Autowired
  private TranslationService     translationService;

  @Autowired
  private AttachmentService      attachmentService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private UserACL                userAcl;

  @Autowired
  private IdentityManager        identityManager;

  private long                   superUserIdentityId;

  @PostConstruct
  public void init() {
    databindService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canHandleDatabind(String objectType, String objectId) {
    return StringUtils.equals(OBJECT_TYPE, objectType);
  }

  @SneakyThrows
  @Override
  public void serialize(String objectId, ZipOutputStream zipOutputStream, String username) {
    SectionTemplateDetail sectionTemplate = sectionTemplateService.getSectionTemplate(Long.parseLong(objectId),
                                                                                      Locale.getDefault());

    SectionTemplateDatabind databind = new SectionTemplateDatabind();
    databind.setContent(sectionTemplate.getContent());
    TranslationField translationNameField =
                                          translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                 Long.parseLong(objectId),
                                                                                 SectionTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                                                 username);
    if (translationNameField != null) {
      Map<String, String> names = translationNameField.getLabels()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(entry -> entry.getKey().toLanguageTag(),
                                                                                Map.Entry::getValue));
      databind.setNames(names);
    }

    TranslationField translationDescriptionField =
                                                 translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                                                        username);
    if (translationDescriptionField != null) {
      Map<String, String> descriptions = translationDescriptionField.getLabels()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> entry.getKey()
                                                                                                            .toLanguageTag(),
                                                                                              Map.Entry::getValue));
      databind.setDescriptions(descriptions);
    }
    FileItem file = fileService.getFile(sectionTemplate.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }
    LayoutModel sectionLayoutModel = JsonUtils.fromJsonString(sectionTemplate.getContent(), LayoutModel.class);
    retrieveBackgroundImages(sectionLayoutModel, fileService);
    databind.setContent(JsonUtils.toJsonString(sectionLayoutModel));
    String jsonData = JsonUtils.toJsonString(databind);

    Page pageLayout = JsonUtils.fromJsonString(sectionTemplate.getContent(), LayoutModel.class).toPage();
    LayoutModel layoutModel = new LayoutModel(pageLayout, portletInstanceService, new PortletInstanceContext(true, null));
    retrieveBackgroundImages(layoutModel, fileService);
    layoutModel.resetStorage();
    String layoutData = JsonUtils.toJsonString(layoutModel);

    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + sectionTemplate.getId() + "/" + CONFIG_JSON, jsonData);
    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + sectionTemplate.getId() + "/" + LAYOUT_JSON, layoutData);
  }

  public CompletableFuture<Pair<DatabindReport, File>> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importSectionTemplates(zipFile)).thenApply(processedTemplates -> {
      DatabindReport report = new DatabindReport();
      report.setSuccess(!processedTemplates.isEmpty());
      report.setProcessedItems(processedTemplates);
      return Pair.of(report, zipFile);
    });
  }

  @ContainerTransactional
  public List<String> importSectionTemplates(File zipFile) {
    Map<String, SectionTemplateDatabind> instances = extractTemplates(zipFile);
    List<String> processedSectionTemplates = new ArrayList<>();
    for (Map.Entry<String, SectionTemplateDatabind> entry : instances.entrySet()) {
      SectionTemplateDatabind sectionTemplate = entry.getValue();
      processSectionTemplate(sectionTemplate);
      processedSectionTemplates.add(sectionTemplate.getContent());
    }
    return processedSectionTemplates;
  }

  private Map<String, SectionTemplateDatabind> extractTemplates(File zipFile) {
    Map<String, SectionTemplateDatabind> templateDatabindMap = new HashMap<>();

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zis.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        String jsonContent = baos.toString(StandardCharsets.UTF_8);
        String entryName = entry.getName();
        String key = entryName.split("/")[0];

        if (entry.getName().endsWith(CONFIG_JSON)) {
          SectionTemplateDatabind databind = JsonUtils.fromJsonString(jsonContent, SectionTemplateDatabind.class);
          if (databind != null) {
            templateDatabindMap.put(key, databind);
          }
        } else if (entry.getName().endsWith(LAYOUT_JSON)) {
          LayoutModel page = JsonUtils.fromJsonString(jsonContent, LayoutModel.class);
          if (page != null) {
            SectionTemplateDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> new SectionTemplateDatabind());
            databind.setPage(page);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error reading zip file", e);
    }
    return templateDatabindMap;
  }

  private void saveIllustration(long sectionTemplateId, byte[] illustrationBytes) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(illustrationBytes);
      String uploadId = "SectionTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(SectionTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(sectionTemplateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SectionTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(sectionTemplateId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for page template '%s'",
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

  @SneakyThrows
  private void saveNames(SectionTemplateDatabind sectionTemplateDatabind, SectionTemplate sectionTemplate) {
    translationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                             sectionTemplate.getId(),
                                             SectionTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                             sectionTemplateDatabind.getNames()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                              Map.Entry::getValue)));
  }

  @SneakyThrows
  private void saveDescriptions(SectionTemplateDatabind sectionTemplateDatabind, SectionTemplate sectionTemplate) {
    translationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                             sectionTemplate.getId(),
                                             SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                             sectionTemplateDatabind.getDescriptions()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                              Map.Entry::getValue)));
  }

  @SneakyThrows
  private void processSectionTemplate(SectionTemplateDatabind sectionTemplateDatabind) {
    SectionTemplate sectionTemplate = new SectionTemplate();
    LayoutModel page = JsonUtils.fromJsonString(sectionTemplateDatabind.getContent(), LayoutModel.class);
    if (page != null) {
      page.setChildren(sectionTemplateDatabind.getPage().getChildren());
      sectionTemplate.setContent(JsonUtils.toJsonString(page));
    }
    sectionTemplate.setSystem(false);
    sectionTemplate.setCategory("custom");
    SectionTemplate createdSectionTemplate = sectionTemplateService.createSectionTemplate(sectionTemplate);
    saveNames(sectionTemplateDatabind, createdSectionTemplate);
    saveDescriptions(sectionTemplateDatabind, createdSectionTemplate);
    if (sectionTemplateDatabind.getIllustration() != null) {
      saveIllustration(createdSectionTemplate.getId(), Base64.decodeBase64(sectionTemplateDatabind.getIllustration()));
    }
    saveAppBackgroundImages(createdSectionTemplate.getId(), page, attachmentService, getSuperUserIdentityId());
    createdSectionTemplate.setContent(JsonUtils.toJsonString(page));
    sectionTemplateService.updateSectionTemplate(createdSectionTemplate);
  }

  @SneakyThrows
  private void writeToZip(ZipOutputStream zipOutputStream, String filePath, String content) {
    ZipEntry entry = new ZipEntry(filePath);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }
}
