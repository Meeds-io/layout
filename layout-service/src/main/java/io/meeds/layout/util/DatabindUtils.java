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
package io.meeds.layout.util;

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.plugin.attachment.LayoutBackgroundAttachmentPlugin;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.UUID;

public class DatabindUtils {

  private DatabindUtils() {
    // Databind Utils Class
  }

  private static final Random RANDOM = new Random();

  @SneakyThrows
  public static void retrieveBackgroundImages(LayoutModel layout, FileService fileService) {
    if (layout == null) {
      return;
    }

    String appBackgroundImage = layout.getAppBackgroundImage();
    if (appBackgroundImage != null && !appBackgroundImage.isEmpty()) {
      String[] parts = appBackgroundImage.split("/");
      String lastPart = parts[parts.length - 1];

      FileItem file = fileService.getFile(Long.parseLong(lastPart));
      if (file != null) {
        layout.setAppBackgroundImage(Base64.encodeBase64String(file.getAsByte()));
      }
    }

    String backgroundImage = layout.getBackgroundImage();
    if (backgroundImage != null && !backgroundImage.isEmpty()) {
      String[] parts = backgroundImage.split("/");
      String lastPart = parts[parts.length - 1];

      FileItem file = fileService.getFile(Long.parseLong(lastPart));
      if (file != null) {
        layout.setBackgroundImage(Base64.encodeBase64String(file.getAsByte()));
      }
    }

    if (layout.getChildren() != null) {
      for (LayoutModel child : layout.getChildren()) {
        retrieveBackgroundImages(child, fileService);
      }
    }
  }

  @SneakyThrows
  public static void saveAppBackgroundImages(long pageTemplateId,
                                             LayoutModel layout,
                                             AttachmentService attachmentService,
                                             long userIdentityId) {
    if (layout == null) {
      return;
    }

    String appBackgroundImage = layout.getAppBackgroundImage();
    if (appBackgroundImage != null && !appBackgroundImage.isEmpty()) {
      ObjectAttachmentDetail attachment = saveAppBackgroundImage(pageTemplateId,
                                                                 Base64.decodeBase64(appBackgroundImage),
                                                                 attachmentService,
                                                                 userIdentityId);
      if (attachment != null) {
        layout.setAppBackgroundImage(buildBackgroundUrl(String.valueOf(pageTemplateId), attachment));
      }
    }

    String backgroundImage = layout.getBackgroundImage();
    if (backgroundImage != null && !backgroundImage.isEmpty()) {
      ObjectAttachmentDetail attachment = saveAppBackgroundImage(pageTemplateId,
                                                                 Base64.decodeBase64(backgroundImage),
                                                                 attachmentService,
                                                                 userIdentityId);
      if (attachment != null) {
        layout.setBackgroundImage(buildBackgroundUrl(String.valueOf(pageTemplateId), attachment));
      }
    }
    if (layout.getChildren() != null) {
      for (LayoutModel child : layout.getChildren()) {
        saveAppBackgroundImages(pageTemplateId, child, attachmentService, userIdentityId);
      }
    }
  }

  public static String buildBackgroundUrl(String objectId, ObjectAttachmentDetail attachment) {
    return String.format("/portal/rest/v1/social/attachments/%s/%s/%s",
                         LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                         objectId,
                         attachment.getId());
  }

  private static ObjectAttachmentDetail saveAppBackgroundImage(long templateId,
                                                               byte[] backgroundImageBytes,
                                                               AttachmentService attachmentService,
                                                               long userIdentityId) {

    File tempFile = null;
    try {
      tempFile = getIllustrationFile(backgroundImageBytes);
      String uploadId = "TemplateBackgroundImage" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE, String.valueOf(templateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);

      String objectId = "template_" + templateId + "_" + UUID.randomUUID();

      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                       objectId,
                                       null,
                                       userIdentityId);
      ObjectAttachmentList attachmentList = attachmentService.getAttachments(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                                                             objectId);
      if (attachmentList != null && CollectionUtils.isNotEmpty(attachmentList.getAttachments())) {
        return attachmentList.getAttachments().getFirst();
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving background Image as attachment for template '%s'",
                                                    templateId),
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
  public static File getIllustrationFile(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Illustration data is null");
    }
    File tempFile = File.createTempFile("temp", ".png");
    FileUtils.writeByteArrayToFile(tempFile, data);
    return tempFile;
  }
}
