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

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceDatabind;
import io.meeds.layout.plugin.attachment.PortletInstanceAttachmentPlugin;
import io.meeds.layout.plugin.translation.PortletInstanceTranslationPlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.injection.LayoutTranslationImportService;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortletInstanceDatabindPlugin implements DatabindPlugin {

  private static final Random            RANDOM                    = new Random();

  public static final String             OBJECT_TYPE               = "PortletInstance";

  private static final String            PLATFORM_USERS_PERMISSION = "*:/platform/users";

  @Autowired
  private PortletInstanceService         portletInstanceService;

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
    PortletInstance portletInstance = portletInstanceService.getPortletInstance(Long.parseLong(objectId),
                                                                                username,
                                                                                Locale.getDefault(),
                                                                                true);

    PortletInstanceDatabind databind = new PortletInstanceDatabind();
    databind.setContentId(portletInstance.getContentId());
    TranslationField translationNameField =
                                          translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                                                 Long.parseLong(objectId),
                                                                                 PortletInstanceTranslationPlugin.TITLE_FIELD_NAME,
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
                                                 translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME,
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
    FileItem file = fileService.getFile(portletInstance.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }
    databind.setPreferences(portletInstanceService.getPortletInstancePreferences(Long.parseLong(objectId), username));
    String jsonData = JsonUtils.toJsonString(databind);
    writeContent(zipOutputStream, objectId, jsonData);
  }

  @Async
  @ContainerTransactional
  public CompletableFuture<DatabindReport> deserialize(File zipFile,
                                                       boolean replaceExisting,
                                                       Map<String, String> params,
                                                       String username) {
    String categoryId = params.get("categoryId");
    if (categoryId != null) {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, PortletInstanceDatabind> instances = extractInstances(zipFile);
        List<String> processedInstances = new ArrayList<>();
        for (Map.Entry<String, PortletInstanceDatabind> entry : instances.entrySet()) {
          PortletInstanceDatabind instance = entry.getValue();
          processPortletInstance(instance, Long.parseLong(categoryId));
          processedInstances.add(instance.getContentId());
        }
        layoutTranslationService.postImport(PortletInstanceTranslationPlugin.OBJECT_TYPE);
        DatabindReport report = new DatabindReport();
        report.setSuccess(!processedInstances.isEmpty());
        report.setProcessedInstances(processedInstances);
        return report;
      });
    }
    return null;
  }

  private void writeContent(ZipOutputStream zipOutputStream, String objectId, String content) throws IOException {
    ZipEntry entry = new ZipEntry(String.format("%s_%s.json", OBJECT_TYPE, objectId));
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  private Map<String, PortletInstanceDatabind> extractInstances(File zipFile) {
    Map<String, PortletInstanceDatabind> instances = new HashMap<>();

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

          // Deserialize JSON into a PortletInstance object
          PortletInstanceDatabind instance = JsonUtils.fromJsonString(jsonContent, PortletInstanceDatabind.class);
          if (instance != null) {
            instances.put(entry.getName(), instance);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error reading zip file", e);
    }
    return instances;
  }

  private void processPortletInstance(PortletInstanceDatabind instance, long categoryId) {
    PortletInstance portletInstance = new PortletInstance();
    portletInstance.setName(instance.getNames().get("en"));
    portletInstance.setDescription(instance.getDescriptions().get("en"));
    portletInstance.setContentId(instance.getContentId());
    portletInstance.setCategoryId(categoryId);
    portletInstance.setPreferences(instance.getPreferences());
    portletInstance.setPermissions(List.of(PLATFORM_USERS_PERMISSION));
    portletInstance.setSystem(false);
    portletInstance.setSupportedModes(List.of("view"));
    PortletInstance createdPortletInstance = portletInstanceService.createPortletInstance(portletInstance);
    saveNames(instance, createdPortletInstance);
    saveDescriptions(instance, createdPortletInstance);
  }

  protected void saveIllustration(long portletInstanceId, byte[] illustrationBytes) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(illustrationBytes);
      String uploadId = "PortletInstanceIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(PortletInstanceAttachmentPlugin.OBJECT_TYPE, String.valueOf(portletInstanceId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       PortletInstanceAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(portletInstanceId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for portlet instance '%s'",
                                                    portletInstanceId),
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
  private File getIllustrationFile(byte[] data) {
    File tempFile = File.createTempFile("temp", ".png");
    FileUtils.writeByteArrayToFile(tempFile, data);
    return tempFile;
  }

  protected void saveNames(PortletInstanceDatabind portletInstanceDatabind, PortletInstance portletInstance) {
    layoutTranslationService.saveTranslationLabels(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                   portletInstance.getId(),
                                                   PortletInstanceTranslationPlugin.TITLE_FIELD_NAME,
                                                   portletInstanceDatabind.getNames());
  }

  protected void saveDescriptions(PortletInstanceDatabind portletInstanceDatabind, PortletInstance portletInstance) {
    layoutTranslationService.saveTranslationLabels(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                   portletInstance.getId(),
                                                   PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                   portletInstanceDatabind.getDescriptions());
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }
}
