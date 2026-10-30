/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.FileAttachmentObject;
import org.exoplatform.social.attachment.model.FileAttachmentResourceList;
import org.exoplatform.social.attachment.model.ObjectAttachmentOperationReport;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.model.AttachedImage;
import io.meeds.layout.model.ImageConsumption;
import io.meeds.layout.model.ImageTarget;
import io.meeds.layout.model.LayoutModel;

/**
 * The images a layout write consumes, as the editor's background inputs save
 * them: each upload id of the call is resolved while the description is
 * applied and before any write, then saved as an attachment of its background
 * keyed on the live page or site, as the user, through the attachment
 * service's edit check; the attachment URL replaces the placeholder in the
 * layout before the draft is written; the upload resources are released
 * whether the write succeeds or fails, and the attachments created are
 * deleted when the write that follows them fails.
 */
@Service
public class LayoutImageService {

  public static final String IMAGE_NOT_SAVED = "could not be saved: image";

  public static final String ATTACHMENT_URL  = "/portal/rest/v1/social/attachments/%s/%s/%s";

  @Autowired
  private UploadService      uploadService;

  @Autowired
  private AttachmentService  attachmentService;

  @Autowired
  private UserACL            userAcl;

  /**
   * @param keyPrefix the live page's or site's storage id, page_&lt;id&gt; or
   *          site_&lt;id&gt;, the first segment of every attachment key the
   *          call creates
   * @return the consumption the call's writers claim their upload ids on
   */
  public ImageConsumption consumption(String keyPrefix) {
    return new ImageConsumption(keyPrefix, id -> uploadService.getUploadResource(id) != null);
  }

  /**
   * Saves each claimed image as an attachment of its target, as the user, and
   * records its URL on the consumption
   *
   * @param images the call's consumption
   * @param username the user writing the layout
   * @throws ObjectNotFoundException when a target no longer exists
   * @throws IllegalAccessException when the user may not edit a target
   */
  public void attach(ImageConsumption images, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (images.isEmpty()) {
      return;
    }
    Identity identity = userAcl.getUserIdentity(username);
    for (Map.Entry<String, ImageTarget> entry : images.getTargets().entrySet()) {
      String uploadId = entry.getKey();
      ImageTarget target = entry.getValue();
      List<String> before = new ArrayList<>(attachmentService.getAttachmentFileIds(target.objectType(), target.objectId()));
      FileAttachmentResourceList list = new FileAttachmentResourceList();
      list.setObjectType(target.objectType());
      list.setObjectId(target.objectId());
      list.setUploadedFiles(List.of(new FileAttachmentObject(null, uploadId, null, null, null, null)));
      list.setAttachedFiles(List.of());
      ObjectAttachmentOperationReport report = attachmentService.saveAttachments(list, identity);
      if (report != null && report.getErrorByUploadId() != null && !report.getErrorByUploadId().isEmpty()) {
        throw new IllegalStateException(IMAGE_NOT_SAVED);
      }
      List<String> created = new ArrayList<>(attachmentService.getAttachmentFileIds(target.objectType(), target.objectId()));
      created.removeAll(before);
      if (created.size() != 1) {
        throw new IllegalStateException(IMAGE_NOT_SAVED);
      }
      String fileId = created.get(0);
      images.addAttached(new AttachedImage(target.objectType(), target.objectId(), fileId));
      images.setUrl(uploadId, String.format(ATTACHMENT_URL, target.objectType(), target.objectId(), fileId));
    }
  }

  /**
   * Replaces every placeholder of the layout tree with the URL of the
   * attachment saved for it
   *
   * @param root the layout to write
   * @param images the call's consumption, its attachments saved
   */
  public void substitute(LayoutModel root, ImageConsumption images) {
    if (root == null) {
      return;
    }
    root.setBackgroundImage(images.resolve(root.getBackgroundImage()));
    root.setTextTitleBackgroundImage(images.resolve(root.getTextTitleBackgroundImage()));
    root.setTextHeaderBackgroundImage(images.resolve(root.getTextHeaderBackgroundImage()));
    if (root.getChildren() != null) {
      root.getChildren().forEach(child -> substitute(child, images));
    }
  }

  /**
   * Deletes the attachments the call created, when the layout write that
   * followed them failed
   *
   * @param images the call's consumption
   */
  public void compensate(ImageConsumption images) {
    for (AttachedImage image : images.getAttached()) {
      attachmentService.deleteAttachment(image.objectType(), image.objectId(), image.fileId());
    }
  }

  /**
   * Releases every upload resource the call claimed: a consumed one is
   * already gone, an unconsumed one goes with the failed call
   *
   * @param images the call's consumption
   */
  public void release(ImageConsumption images) {
    images.getTargets().keySet().forEach(uploadService::removeUploadResource);
  }

}
