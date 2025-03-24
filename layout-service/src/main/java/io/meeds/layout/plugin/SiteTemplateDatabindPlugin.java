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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.*;
import io.meeds.layout.plugin.attachment.PageTemplateAttachmentPlugin;
import io.meeds.layout.plugin.attachment.SiteTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.PageTemplateTranslationPlugin;
import io.meeds.layout.plugin.translation.SiteTemplateTranslationPlugin;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.layout.service.injection.LayoutTranslationImportService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SiteTemplateDatabindPlugin implements DatabindPlugin {

  private static final Random            RANDOM      = new Random();

  public static final String             OBJECT_TYPE = "SiteTemplate";

  @Autowired
  private SiteTemplateService            siteTemplateService;

  @Autowired
  private DatabindService                databindService;

  @Autowired
  private FileService                    fileService;

  @Autowired
  private LayoutTranslationImportService layoutTranslationService;

  @Autowired
  private TranslationService             translationService;

  @Autowired
  private AttachmentService              attachmentService;

  @Autowired
  private UserACL                        userAcl;

  @Autowired
  private IdentityManager                identityManager;

  private long                           superUserIdentityId;

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
    SiteTemplate siteTemplate = siteTemplateService.getSiteTemplate(Long.parseLong(objectId), Locale.getDefault());

    SiteTemplateDatabind databind = new SiteTemplateDatabind();
    databind.setLayout(siteTemplate.getLayout());
    databind.setIcon(siteTemplate.getIcon());
    TranslationField translationNameField = translationService.getTranslationField(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                   Long.parseLong(objectId),
                                                                                   PageTemplateTranslationPlugin.TITLE_FIELD_NAME,
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
                                                 translationService.getTranslationField(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        PageTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
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
    FileItem file = fileService.getFile(siteTemplate.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }
    String jsonData = JsonUtils.toJsonString(databind);
    writeContent(zipOutputStream, objectId, jsonData);
  }

  public CompletableFuture<DatabindReport> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importSiteTemplates(zipFile, username))
                            .thenCompose(processedTemplates -> layoutTranslationService.postImport(SiteTemplateTranslationPlugin.OBJECT_TYPE)
                                                                                       .thenApply(v -> {
                                                                                         DatabindReport report =
                                                                                                               new DatabindReport();
                                                                                         report.setSuccess(!processedTemplates.isEmpty());
                                                                                         report.setProcessedItems(processedTemplates);
                                                                                         return report;
                                                                                       }));

  }

  @ContainerTransactional
  public List<String> importSiteTemplates(File zipFile, String username) {
    Map<String, SiteTemplateDatabind> templates = extractTemplates(zipFile);
    List<String> processedPageTemplates = new ArrayList<>();
    for (Map.Entry<String, SiteTemplateDatabind> entry : templates.entrySet()) {
      SiteTemplateDatabind pageTemplate = entry.getValue();
      processSiteTemplate(pageTemplate, username);
      processedPageTemplates.add(pageTemplate.getName());
    }
    return processedPageTemplates;
  }

  private Map<String, SiteTemplateDatabind> extractTemplates(File zipFile) {
    Map<String, SiteTemplateDatabind> templateDatabindMap = new HashMap<>();

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
          }
          String jsonContent = baos.toString(StandardCharsets.UTF_8);

          // Deserialize JSON into a Page templates
          SiteTemplateDatabind databind = JsonUtils.fromJsonString(jsonContent, SiteTemplateDatabind.class);
          if (databind != null) {
            templateDatabindMap.put(entry.getName(), databind);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error reading zip file", e);
    }
    return templateDatabindMap;
  }

  private void saveIllustration(long pageTemplateId, byte[] illustrationBytes) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(illustrationBytes);
      String uploadId = "SiteTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(PageTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(pageTemplateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SiteTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(pageTemplateId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for site template '%s'",
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

  private void saveNames(SiteTemplateDatabind siteTemplateDatabind, SiteTemplate siteTemplate) {
    layoutTranslationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                   siteTemplate.getId(),
                                                   SiteTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                   siteTemplateDatabind.getNames());
  }

  private void saveDescriptions(SiteTemplateDatabind siteTemplateDatabind, SiteTemplate siteTemplate) {
    layoutTranslationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                   siteTemplate.getId(),
                                                   SiteTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                   siteTemplateDatabind.getDescriptions());
  }

  @SneakyThrows
  private void processSiteTemplate(SiteTemplateDatabind siteTemplateDatabind, String username) {
    SiteTemplate siteTemplate = new SiteTemplate();
    siteTemplate.setName(siteTemplateDatabind.getNames().get("en"));
    siteTemplate.setDescription(siteTemplateDatabind.getDescriptions().get("en"));
    siteTemplate.setLayout(generateLayoutName(siteTemplate.getName()));
    siteTemplate.setIcon(siteTemplateDatabind.getIcon());
    siteTemplate.setSystem(false);
    SiteTemplate createdSiteTemplate = siteTemplateService.createSiteTemplate(siteTemplate,
                                                                              new SiteKey(SiteType.PORTAL_TEMPLATE,
                                                                                          siteTemplateDatabind.getLayout()),
                                                                              username,
                                                                              true);
    saveNames(siteTemplateDatabind, createdSiteTemplate);
    saveDescriptions(siteTemplateDatabind, createdSiteTemplate);
    if (siteTemplateDatabind.getIllustration() != null) {
      saveIllustration(createdSiteTemplate.getId(), Base64.decodeBase64(siteTemplateDatabind.getIllustration()));
    }
  }

  @SneakyThrows
  private File getIllustrationFile(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Illustration data is null");
    }
    File tempFile = File.createTempFile("temp", ".png");
    FileUtils.writeByteArrayToFile(tempFile, data);
    return tempFile;
  }

  private void writeContent(ZipOutputStream zipOutputStream, String objectId, String content) throws IOException {
    ZipEntry entry = new ZipEntry(String.format("%s_%s.json", OBJECT_TYPE, objectId));
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

  public static String generateLayoutName(String name) {
    String transformed = name.toLowerCase()
                             .chars()
                             .mapToObj(c -> String.valueOf((char) ((c % 25) + 97)))
                             .collect(Collectors.joining());
    int randomNumber = ThreadLocalRandom.current().nextInt(1000);
    return transformed + randomNumber;
  }

}
