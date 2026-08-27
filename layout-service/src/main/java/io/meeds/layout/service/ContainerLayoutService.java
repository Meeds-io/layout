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

import static io.meeds.layout.plugin.attachment.LayoutBackgroundAttachmentPlugin.OBJECT_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.plugin.attachment.LayoutTextBodyBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextHeaderBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextSubtitleBackgroundAttachmentPlugin;
import io.meeds.layout.plugin.attachment.LayoutTextTitleBackgroundAttachmentPlugin;
import io.meeds.layout.util.EntityMapper;

import lombok.SneakyThrows;

@Service
public class ContainerLayoutService {

  private record TextBackgroundImageField(Function<ModelStyle, String> getter,
                                           BiConsumer<ModelStyle, String> setter,
                                           String objectType) {
  }

  private static final List<TextBackgroundImageField> TEXT_BACKGROUND_IMAGE_FIELDS = List.of(
      new TextBackgroundImageField(ModelStyle::getTextTitleBackgroundImage,
                                    ModelStyle::setTextTitleBackgroundImage,
                                    LayoutTextTitleBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(ModelStyle::getTextHeaderBackgroundImage,
                                    ModelStyle::setTextHeaderBackgroundImage,
                                    LayoutTextHeaderBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(ModelStyle::getTextBackgroundImage,
                                    ModelStyle::setTextBackgroundImage,
                                    LayoutTextBodyBackgroundAttachmentPlugin.OBJECT_TYPE),
      new TextBackgroundImageField(ModelStyle::getTextSubtitleBackgroundImage,
                                    ModelStyle::setTextSubtitleBackgroundImage,
                                    LayoutTextSubtitleBackgroundAttachmentPlugin.OBJECT_TYPE));

  @Autowired
  private AttachmentService      attachmentService;

  @Autowired
  private UploadService          uploadService;

  @Autowired
  private FileService            fileService;

  @Autowired
  private UserACL                userAcl;

  @Autowired
  private IdentityManager        identityManager;

  @Autowired
  private LayoutService          layoutService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  private long                   superUserIdentityId;

  public Container cloneContainer(Container container) {
    exportPortletPreferences(container);
    LayoutModel sectionLayoutModel = new LayoutModel(container);
    sectionLayoutModel.resetStorage();
    return (Container) EntityMapper.toModelObject(sectionLayoutModel);
  }

  public void exportPortletPreferences(ModelObject object) {
    if (object instanceof Container container && CollectionUtils.isNotEmpty(container.getChildren())) {
      container.getChildren().forEach(this::exportPortletPreferences);
    } else if (object instanceof Application application) {
      portletInstanceService.exportApplicationPreferences(application);
    }
  }

  public Container findContainer(PageKey pageKey, long containerId) {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      return null;
    } else {
      return findContainer(page, String.valueOf(containerId));
    }
  }

  public void impersonateContainer(Container container, Page page) throws Exception {
    if (page == null || container == null) { // PortalConfig isn't managed yet
                                             // for now (Site Layout)
      return;
    }
    ModelStyle cssStyle = container.getCssStyle();
    if (cssStyle != null && StringUtils.isNotBlank(cssStyle.getBackgroundImage())) {
      String clonedBackgroundImageUrl = null;
      try {
        clonedBackgroundImageUrl = cloneBackgroundUrl(container, page, cssStyle.getBackgroundImage());
      } finally {
        // Even in case of exception thrown, set null
        cssStyle.setBackgroundImage(clonedBackgroundImageUrl);
      }
    }
    ApplicationBackgroundStyle appBackgroundStyle = container.getAppBackgroundStyle();
    if (appBackgroundStyle != null
        && StringUtils.isNotBlank(appBackgroundStyle.getBackgroundImage())) {
      String clonedBackgroundImageUrl = null;
      try {
        clonedBackgroundImageUrl = cloneBackgroundUrl(container, page, appBackgroundStyle.getBackgroundImage());
      } finally {
        // Even in case of exception thrown, set null
        appBackgroundStyle.setBackgroundImage(clonedBackgroundImageUrl);
      }
    }
    if (cssStyle != null) {
      for (TextBackgroundImageField field : TEXT_BACKGROUND_IMAGE_FIELDS) {
        String textBackgroundImage = field.getter().apply(cssStyle);
        if (StringUtils.isNotBlank(textBackgroundImage)) {
          String clonedBackgroundImageUrl = null;
          try {
            clonedBackgroundImageUrl = cloneBackgroundUrl(container, page, textBackgroundImage, field.objectType());
          } finally {
            // Even in case of exception thrown, set null
            field.setter().accept(cssStyle, clonedBackgroundImageUrl);
          }
        }
      }
    }
  }

  public String cloneBackgroundUrl(ModelObject container, Page page, String backgroundImageUrl) throws Exception { // NOSONAR
    return cloneBackgroundUrl(container, page, backgroundImageUrl, OBJECT_TYPE);
  }

  public String cloneBackgroundUrl(ModelObject container, Page page, String backgroundImageUrl, String objectType) throws Exception { // NOSONAR
    String clonedBackgroundImageUrl = null;
    String backgroundImageUri = String.format("/%s/", objectType);
    if (StringUtils.contains(backgroundImageUrl, backgroundImageUri)) {
      String[] backgroundImageUrlParts = backgroundImageUrl.split("/");
      String fileIdString = backgroundImageUrlParts[backgroundImageUrlParts.length - 1];
      if (StringUtils.isNotBlank(fileIdString) && NumberUtils.isCreatable(fileIdString)) {
        FileItem file = fileService.getFile(Long.parseLong(fileIdString));
        if (file != null) {
          String objectId = getObjectId(container, page);
          UploadResource uploadResource = createUploadResource(file);
          ObjectAttachmentDetail attachment = saveAttachment(objectId, uploadResource, objectType);
          if (attachment != null) {
            clonedBackgroundImageUrl = buildBackgroundUrl(objectId, attachment, objectType);
          }
        }
      }
    }
    return clonedBackgroundImageUrl;
  }

  private String getObjectId(ModelObject container, Page page) {
    return String.format("%s_%s",
                         page.getStorageId().replace("page_", ""),
                         container.getStorageId());
  }

  private String buildBackgroundUrl(String objectId, ObjectAttachmentDetail attachment, String objectType) {
    return String.format("/portal/rest/v1/social/attachments/%s/%s/%s",
                         objectType,
                         objectId,
                         attachment.getId());
  }

  @SneakyThrows
  private ObjectAttachmentDetail saveAttachment(String objectId, UploadResource uploadResource) {
    return saveAttachment(objectId, uploadResource, OBJECT_TYPE);
  }

  @SneakyThrows
  private ObjectAttachmentDetail saveAttachment(String objectId, UploadResource uploadResource, String objectType) {
    UploadedAttachmentDetail attachmentDetail = new UploadedAttachmentDetail(uploadResource);
    attachmentService.saveAttachment(attachmentDetail,
                                     objectType,
                                     objectId,
                                     null,
                                     getSuperUserIdentityId());
    ObjectAttachmentList attachmentList = attachmentService.getAttachments(objectType, objectId);
    if (attachmentList != null && CollectionUtils.isNotEmpty(attachmentList.getAttachments())) {
      return attachmentList.getAttachments().get(0);
    } else {
      return null;
    }
  }

  private UploadResource createUploadResource(FileItem fileItem) throws IOException {
    String uploadId = UUID.randomUUID().toString();
    FileInfo fileInfo = fileItem.getFileInfo();
    Path path = Files.createTempFile(fileInfo.getName(), ".png");
    try (InputStream inputStream = fileItem.getAsStream()) {
      File file = path.toFile();
      FileUtils.copyInputStreamToFile(inputStream, file);
      UploadResource uploadResource = new UploadResource(uploadId,
                                                         fileInfo.getName(),
                                                         fileInfo.getMimetype(),
                                                         file.getAbsolutePath(),
                                                         fileInfo.getSize(),
                                                         fileInfo.getSize(),
                                                         UploadResource.UPLOADED_STATUS);
      uploadService.createUploadResource(uploadResource);
      return uploadResource;
    }
  }

  private Container findContainer(ModelObject modelObject, String containerStorageId) {
    return switch (modelObject) {
    case Container container -> {
      if (StringUtils.equals(containerStorageId, container.getStorageId())) {
        yield container;
      } else if (CollectionUtils.isNotEmpty(container.getChildren())) {
        yield container.getChildren()
                       .stream()
                       .map(m -> findContainer(m, containerStorageId))
                       .filter(Objects::nonNull)
                       .findFirst()
                       .orElse(null);
      } else {
        yield null;
      }
    }
    default -> null;
    };
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }
}
