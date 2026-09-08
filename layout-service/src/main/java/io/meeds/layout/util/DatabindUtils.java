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
import io.meeds.layout.plugin.attachment.LayoutTextBodyBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextHeaderBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextSubtitleBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextTitleBackgroundAttachmentPlugin;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DatabindUtils {

  private DatabindUtils() {
    // Databind Utils Class
  }

  private static final Random RANDOM = new Random();

  private record TextBackgroundImageField(Function<LayoutModel, String> getter,
                                           BiConsumer<LayoutModel, String> setter,
                                           String objectType) {
  }

  private static final List<TextBackgroundImageField> TEXT_BACKGROUND_IMAGE_FIELDS = List.of(
      new TextBackgroundImageField(LayoutModel::getTextTitleBackgroundImage,
                                    LayoutModel::setTextTitleBackgroundImage,
                                    LayoutTextTitleBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(LayoutModel::getTextHeaderBackgroundImage,
                                    LayoutModel::setTextHeaderBackgroundImage,
                                    LayoutTextHeaderBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(LayoutModel::getTextBackgroundImage,
                                    LayoutModel::setTextBackgroundImage,
                                    LayoutTextBodyBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(LayoutModel::getTextSubtitleBackgroundImage,
                                    LayoutModel::setTextSubtitleBackgroundImage,
                                    LayoutTextSubtitleBackgroundAttachmentPlugin.OBJECT_TYPE));

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

    for (TextBackgroundImageField field : TEXT_BACKGROUND_IMAGE_FIELDS) {
      String textBackgroundImage = field.getter().apply(layout);
      if (textBackgroundImage != null && !textBackgroundImage.isEmpty()) {
        String[] parts = textBackgroundImage.split("/");
        String lastPart = parts[parts.length - 1];

        FileItem file = fileService.getFile(Long.parseLong(lastPart));
        if (file != null) {
          field.setter().accept(layout, Base64.encodeBase64String(file.getAsByte()));
        }
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
    attachmentService.deleteAttachments(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                        String.valueOf(pageTemplateId));
    for (TextBackgroundImageField field : TEXT_BACKGROUND_IMAGE_FIELDS) {
      attachmentService.deleteAttachments(field.objectType(), String.valueOf(pageTemplateId));
    }
    saveAppBackgroundImagesInternal(pageTemplateId, layout, attachmentService, userIdentityId);
  }

  private static void saveAppBackgroundImagesInternal(long pageTemplateId,
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
                                                                 LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                                                 attachmentService,
                                                                 userIdentityId);
      if (attachment != null) {
        layout.setAppBackgroundImage(buildBackgroundUrl(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                                         String.valueOf(pageTemplateId),
                                                         attachment));
      }
    }

    String backgroundImage = layout.getBackgroundImage();
    if (backgroundImage != null && !backgroundImage.isEmpty()) {
      ObjectAttachmentDetail attachment = saveAppBackgroundImage(pageTemplateId,
                                                                 Base64.decodeBase64(backgroundImage),
                                                                 LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                                                 attachmentService,
                                                                 userIdentityId);
      if (attachment != null) {
        layout.setBackgroundImage(buildBackgroundUrl(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE,
                                                      String.valueOf(pageTemplateId),
                                                      attachment));
      }
    }

    for (TextBackgroundImageField field : TEXT_BACKGROUND_IMAGE_FIELDS) {
      String textBackgroundImage = field.getter().apply(layout);
      if (textBackgroundImage != null && !textBackgroundImage.isEmpty()) {
        ObjectAttachmentDetail attachment = saveAppBackgroundImage(pageTemplateId,
                                                                   Base64.decodeBase64(textBackgroundImage),
                                                                   field.objectType(),
                                                                   attachmentService,
                                                                   userIdentityId);
        if (attachment != null) {
          field.setter().accept(layout, buildBackgroundUrl(field.objectType(), String.valueOf(pageTemplateId), attachment));
        }
      }
    }
    if (layout.getChildren() != null) {
      for (LayoutModel child : layout.getChildren()) {
        saveAppBackgroundImagesInternal(pageTemplateId, child, attachmentService, userIdentityId);
      }
    }
  }

  public static String buildBackgroundUrl(String objectId, ObjectAttachmentDetail attachment) {
    return buildBackgroundUrl(LayoutBackgroundAttachmentPlugin.OBJECT_TYPE, objectId, attachment);
  }

  public static String buildBackgroundUrl(String objectType, String objectId, ObjectAttachmentDetail attachment) {
    return String.format("/portal/rest/v1/social/attachments/%s/%s/%s",
                         objectType,
                         objectId,
                         attachment.getId());
  }

  private static ObjectAttachmentDetail saveAppBackgroundImage(long templateId,
                                                               byte[] backgroundImageBytes,
                                                               String objectType,
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
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);

      String objectId = String.valueOf(templateId);

      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       objectType,
                                       objectId,
                                       null,
                                       userIdentityId);
      ObjectAttachmentList attachmentList = attachmentService.getAttachments(objectType, objectId);
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
