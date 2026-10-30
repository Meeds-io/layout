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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.FileAttachmentResourceList;
import org.exoplatform.social.attachment.model.ObjectAttachmentOperationReport;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.model.AttachedImage;
import io.meeds.layout.model.ImageConsumption;
import io.meeds.layout.model.LayoutModel;

/**
 * The images a layout write consumes: saved as attachments of their target as
 * the user, their URLs put in the layout's place-holders, the resources
 * released, the attachments deleted when the write that followed them failed
 */
@ExtendWith(MockitoExtension.class)
public class LayoutImageServiceTest {

  @Mock
  private UploadService      uploadService;

  @Mock
  private AttachmentService  attachmentService;

  @Mock
  private UserACL            userAcl;

  @Mock
  private Identity           identity;

  @InjectMocks
  private LayoutImageService service;

  @Test
  public void aConsumptionResolvesItsIdsAgainstTheStagedUploads() throws Exception {
    when(uploadService.getUploadResource("u1")).thenReturn(new UploadResource("u1"));
    ImageConsumption images = service.consumption("page_3");
    assertEquals("upload_u1", images.claim("u1", "containerBackground", "12", "background.image"));
    assertThrows(org.exoplatform.commons.exception.ObjectNotFoundException.class, () -> images.claim("u2", "containerBackground", "12", "background.image"));
  }

  @Test
  public void attachSavesEachImageAsTheUserOnItsTargetAndRecordsItsUrl() throws Exception {
    when(userAcl.getUserIdentity("john")).thenReturn(identity);
    when(attachmentService.getAttachmentFileIds("containerBackground", "page_3_12")).thenReturn(List.of("7"), List.of("7", "9"));
    when(attachmentService.saveAttachments(any(), eq(identity))).thenReturn(new ObjectAttachmentOperationReport());
    ImageConsumption images = new ImageConsumption("page_3", id -> true);
    images.claim("u1", "containerBackground", "12", "background.image");

    service.attach(images, "john");

    ArgumentCaptor<FileAttachmentResourceList> saved = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(saved.capture(), eq(identity));
    assertEquals("containerBackground", saved.getValue().getObjectType());
    assertEquals("page_3_12", saved.getValue().getObjectId());
    assertEquals("u1", saved.getValue().getUploadedFiles().get(0).getUploadId());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/page_3_12/9", images.resolve("upload_u1"));
    assertEquals(List.of(new AttachedImage("containerBackground", "page_3_12", "9")), images.getAttached());
  }

  @Test
  public void attachRefusesAnImageTheServiceDidNotSave() throws Exception {
    when(userAcl.getUserIdentity("john")).thenReturn(identity);
    when(attachmentService.getAttachmentFileIds("containerBackground", "page_3_12")).thenReturn(List.of());
    ObjectAttachmentOperationReport report = new ObjectAttachmentOperationReport();
    report.addError("u1", "attachment.uploadIdNotFound");
    when(attachmentService.saveAttachments(any(), eq(identity))).thenReturn(report);
    ImageConsumption images = new ImageConsumption("page_3", id -> true);
    images.claim("u1", "containerBackground", "12", "background.image");

    assertEquals(LayoutImageService.IMAGE_NOT_SAVED, assertThrows(IllegalStateException.class, () -> service.attach(images, "john")).getMessage());
    assertTrue(images.getAttached().isEmpty());
  }

  @Test
  public void attachDoesNothingWithoutAnImage() throws Exception {
    service.attach(ImageConsumption.none(), "john");
    verifyNoInteractions(attachmentService, userAcl);
  }

  @Test
  public void substituteReplacesEveryPlaceholderOfTheTreeWithItsUrl() {
    ImageConsumption images = new ImageConsumption("page_3", id -> true);
    images.setUrl("u1", "/url/1");
    images.setUrl("u2", "/url/2");
    LayoutModel root = new LayoutModel();
    LayoutModel child = new LayoutModel();
    child.setBackgroundImage("upload_u1");
    child.setTextTitleBackgroundImage("upload_u2");
    child.setTextHeaderBackgroundImage("/stored");
    root.setChildren(List.of(child));
    root.setBackgroundImage("upload_u3");

    service.substitute(root, images);

    assertEquals("/url/1", child.getBackgroundImage());
    assertEquals("/url/2", child.getTextTitleBackgroundImage());
    assertEquals("/stored", child.getTextHeaderBackgroundImage());
    assertEquals("upload_u3", root.getBackgroundImage());
  }

  @Test
  public void compensateDeletesTheAttachmentsCreatedAndReleaseRemovesEveryClaimedUpload() throws Exception {
    ImageConsumption images = new ImageConsumption("page_3", id -> true);
    images.claim("u1", "containerBackground", "12", "background.image");
    images.claim("u2", "containerTextTitleBackground", "12", "text.title.background");
    images.addAttached(new AttachedImage("containerBackground", "page_3_12", "9"));

    service.compensate(images);
    service.release(images);

    verify(attachmentService).deleteAttachment("containerBackground", "page_3_12", "9");
    verify(attachmentService, never()).deleteAttachment(eq("containerTextTitleBackground"), any(), any());
    verify(uploadService).removeUploadResource("u1");
    verify(uploadService).removeUploadResource("u2");
  }

}
