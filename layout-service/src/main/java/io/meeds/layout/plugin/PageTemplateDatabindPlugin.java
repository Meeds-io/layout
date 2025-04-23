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

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.*;
import io.meeds.layout.plugin.attachment.PageTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.PageTemplateTranslationPlugin;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.service.PortletInstanceService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.portal.config.model.Page;
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

import static io.meeds.layout.util.DatabindUtils.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PageTemplateDatabindPlugin implements DatabindPlugin {

  private static final Random    RANDOM      = new Random();

  public static final String     OBJECT_TYPE = "PageTemplate";

  public static final String     CONFIG_JSON = "config.json";

  public static final String     LAYOUT_JSON = "layout.json";

  @Autowired
  private PageTemplateService    pageTemplateService;

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
    PageTemplate pageTemplate = pageTemplateService.getPageTemplate(Long.parseLong(objectId), Locale.getDefault(), true, true);

    PageTemplateDatabind databind = new PageTemplateDatabind();
    databind.setContent(pageTemplate.getContent());
    TranslationField translationNameField = translationService.getTranslationField(PageTemplateTranslationPlugin.OBJECT_TYPE,
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
                                                 translationService.getTranslationField(PageTemplateTranslationPlugin.OBJECT_TYPE,
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
    FileItem file = fileService.getFile(pageTemplate.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }

    String jsonData = JsonUtils.toJsonString(databind);

    Page pageLayout = JsonUtils.fromJsonString(pageTemplate.getContent(), LayoutModel.class).toPage();

    LayoutModel layoutModel = new LayoutModel(pageLayout, portletInstanceService, new PortletInstanceContext(true, null));
    retrieveBackgroundImages(layoutModel, fileService);
    layoutModel.resetStorage();
    String layoutData = JsonUtils.toJsonString(layoutModel);
    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + pageTemplate.getId() + "/" + CONFIG_JSON, jsonData);
    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + pageTemplate.getId() + "/" + LAYOUT_JSON, layoutData);
  }

  public CompletableFuture<Pair<DatabindReport, File>> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importPageTemplates(zipFile)).thenApply(processedTemplates -> {
      DatabindReport report = new DatabindReport();
      report.setSuccess(!processedTemplates.isEmpty());
      report.setProcessedItems(processedTemplates);
      return Pair.of(report, zipFile);
    });
  }

  @ContainerTransactional
  public List<String> importPageTemplates(File zipFile) {
    Map<String, PageTemplateDatabind> instances = extractTemplates(zipFile);
    List<String> processedPageTemplates = new ArrayList<>();
    for (Map.Entry<String, PageTemplateDatabind> entry : instances.entrySet()) {
      PageTemplateDatabind pageTemplate = entry.getValue();
      processPageTemplate(pageTemplate);
      processedPageTemplates.add(pageTemplate.getContent());
    }
    return processedPageTemplates;
  }

  private Map<String, PageTemplateDatabind> extractTemplates(File zipFile) {
    Map<String, PageTemplateDatabind> templateDatabindMap = new HashMap<>();

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
          PageTemplateDatabind databind = JsonUtils.fromJsonString(jsonContent, PageTemplateDatabind.class);
          if (databind != null) {
            templateDatabindMap.put(key, databind);
          }
        } else if (entry.getName().endsWith(LAYOUT_JSON)) {
          LayoutModel page = JsonUtils.fromJsonString(jsonContent, LayoutModel.class);
          if (page != null) {
            PageTemplateDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> new PageTemplateDatabind());
            databind.setPage(page);
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
      String uploadId = "PageTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(PageTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(pageTemplateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       PageTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(pageTemplateId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for page template '%s'",
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
  private void saveNames(PageTemplateDatabind pageTemplateDatabind, PageTemplate pageTemplate) {
    translationService.saveTranslationLabels(PageTemplateTranslationPlugin.OBJECT_TYPE,
                                             pageTemplate.getId(),
                                             PageTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                             pageTemplateDatabind.getNames()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                           Map.Entry::getValue)));
  }

  @SneakyThrows
  private void saveDescriptions(PageTemplateDatabind pageTemplateDatabind, PageTemplate pageTemplate) {
    translationService.saveTranslationLabels(PageTemplateTranslationPlugin.OBJECT_TYPE,
                                             pageTemplate.getId(),
                                             PageTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                             pageTemplateDatabind.getDescriptions()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                           Map.Entry::getValue)));
  }

  @SneakyThrows
  private void processPageTemplate(PageTemplateDatabind pageTemplateDatabind) {
    PageTemplate pageTemplate = new PageTemplate();
    pageTemplate.setName(pageTemplateDatabind.getNames().get("en"));
    pageTemplate.setDescription(pageTemplateDatabind.getDescriptions().get("en"));
    LayoutModel page = JsonUtils.fromJsonString(pageTemplateDatabind.getContent(), LayoutModel.class);
    if (page != null) {
      page.setChildren(pageTemplateDatabind.getPage().getChildren());
      pageTemplate.setContent(JsonUtils.toJsonString(page));
    }
    pageTemplate.setSystem(false);
    PageTemplate createdPageTemplate = pageTemplateService.createPageTemplate(pageTemplate);
    saveNames(pageTemplateDatabind, createdPageTemplate);
    saveDescriptions(pageTemplateDatabind, createdPageTemplate);
    if (pageTemplateDatabind.getIllustration() != null) {
      saveIllustration(createdPageTemplate.getId(), Base64.decodeBase64(pageTemplateDatabind.getIllustration()));
    }
    saveAppBackgroundImages(createdPageTemplate.getId(), page, attachmentService, getSuperUserIdentityId());
    createdPageTemplate.setContent(JsonUtils.toJsonString(page));
    pageTemplateService.updatePageTemplate(createdPageTemplate);
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
