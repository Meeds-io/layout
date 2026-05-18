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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

@SpringBootTest(classes = { ContainerLayoutService.class })
@ExtendWith(MockitoExtension.class)
class ContainerLayoutServiceTest {

  private static final String        MIME_TYPE    = "image/png";

  private static final String        FILE_NAME    = "file.png";

  private static final long          FILE_ID      = 98L;

  private static final long          CONTAINER_ID = 35L;

  private static final long          PAGE_ID      = 58L;

  @MockitoBean
  private AttachmentService          attachmentService;

  @MockitoBean
  private UploadService              uploadService;

  @MockitoBean
  private FileService                fileService;

  @MockitoBean
  private UserACL                    userAcl;

  @MockitoBean
  private IdentityManager            identityManager;

  @MockitoBean
  private LayoutService              layoutService;

  @MockitoBean
  private PortletInstanceService     portletInstanceService;

  @Mock
  private FileItem                   fileItem;

  @Mock
  private FileInfo                   fileInfo;

  @Mock
  private Container                  container;

  @Mock
  private Page                       page;

  @Mock
  private ModelStyle                 modelStyle;

  @Mock
  private ApplicationBackgroundStyle appBackgroundStyle;

  @Mock
  private ObjectAttachmentList       attachmentList;

  @Mock
  private Identity                   identity;

  @Autowired
  private ContainerLayoutService     containerLayoutService;

  @Test
  void testImpersonateContainerWithNullPage() throws Exception {
    containerLayoutService.impersonateContainer(container, null);
    verifyNoInteractions(container);
  }

  @Test
  void testImpersonateContainerWithNullContainer() throws Exception {
    containerLayoutService.impersonateContainer(null, page);
    verifyNoInteractions(page);
  }

  @Test
  void testImpersonateContainerWithCssStyleNoAttachment() throws Exception {
    when(container.getCssStyle()).thenReturn(modelStyle);
    when(modelStyle.getBackgroundImage()).thenReturn("fake/url");
    containerLayoutService.impersonateContainer(container, page);
    verify(modelStyle).setBackgroundImage(null);
  }

  @Test
  void testImpersonateContainerWithAppBackgroundStyleNoAttachment() throws Exception {
    when(container.getAppBackgroundStyle()).thenReturn(appBackgroundStyle);
    when(appBackgroundStyle.getBackgroundImage()).thenReturn("fake/url");
    containerLayoutService.impersonateContainer(container, page);
    verify(appBackgroundStyle).setBackgroundImage(null);
  }

  @Test
  void testImpersonateContainerWithCssStyleValidAttachment() throws Exception {
    String backgroundImageUrl = String.format("/%s/%s_%s/%s", OBJECT_TYPE, PAGE_ID, CONTAINER_ID, FILE_ID);
    when(container.getCssStyle()).thenReturn(modelStyle);
    when(page.getStorageId()).thenReturn(String.format("page_%s", PAGE_ID));
    when(container.getStorageId()).thenReturn(String.valueOf(CONTAINER_ID));
    when(modelStyle.getBackgroundImage()).thenReturn(backgroundImageUrl);

    when(fileService.getFile(FILE_ID)).thenReturn(fileItem);

    // Mock the fileItem and FileInfo
    when(fileItem.getFileInfo()).thenReturn(fileInfo);
    when(fileInfo.getName()).thenReturn(FILE_NAME);
    when(fileInfo.getMimetype()).thenReturn(MIME_TYPE);
    when(fileInfo.getSize()).thenReturn(3l);
    when(fileItem.getAsStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));

    when(userAcl.getSuperUser()).thenReturn("root");
    when(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser())).thenReturn(identity);
    lenient().when(identity.getId()).thenReturn("29");

    long newFileId = 1222l;
    doAnswer(invocation -> {
      when(attachmentList.getAttachments()).thenReturn(Collections.singletonList(new ObjectAttachmentDetail(String.valueOf(newFileId),
                                                                                                            FILE_NAME,
                                                                                                            MIME_TYPE,
                                                                                                            3l,
                                                                                                            0l,
                                                                                                            null)));
      when(attachmentService.getAttachments(OBJECT_TYPE, invocation.getArgument(2))).thenReturn(attachmentList);
      return null;
    }).when(attachmentService)
      .saveAttachment(any(),
                      eq(OBJECT_TYPE),
                      any(),
                      any(),
                      anyLong());
    containerLayoutService.impersonateContainer(container, page);
    verify(modelStyle).setBackgroundImage(String.format("/portal/rest/v1/social/attachments/%s/%s_%s/%s",
                                                        OBJECT_TYPE,
                                                        PAGE_ID,
                                                        CONTAINER_ID,
                                                        newFileId));
  }

  @Test
  void testImpersonateContainerWithAppBackgroundStyleValidAttachment() throws Exception {
    String backgroundImageUrl = String.format("/%s/%s_%s/%s", OBJECT_TYPE, PAGE_ID, CONTAINER_ID, FILE_ID);
    when(container.getAppBackgroundStyle()).thenReturn(appBackgroundStyle);
    when(page.getStorageId()).thenReturn(String.format("page_%s", PAGE_ID));
    when(container.getStorageId()).thenReturn(String.valueOf(CONTAINER_ID));
    when(appBackgroundStyle.getBackgroundImage()).thenReturn(backgroundImageUrl);

    when(fileService.getFile(FILE_ID)).thenReturn(fileItem);

    // Mock the fileItem and FileInfo
    when(fileItem.getFileInfo()).thenReturn(fileInfo);
    when(fileInfo.getName()).thenReturn(FILE_NAME);
    when(fileInfo.getMimetype()).thenReturn(MIME_TYPE);
    when(fileInfo.getSize()).thenReturn(3l);
    when(fileItem.getAsStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));

    when(userAcl.getSuperUser()).thenReturn("root");
    when(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser())).thenReturn(identity);
    lenient().when(identity.getId()).thenReturn("29");

    long newFileId = 1222l;
    doAnswer(invocation -> {
      when(attachmentList.getAttachments()).thenReturn(Collections.singletonList(new ObjectAttachmentDetail(String.valueOf(newFileId),
                                                                                                            FILE_NAME,
                                                                                                            MIME_TYPE,
                                                                                                            3l,
                                                                                                            0l,
                                                                                                            null)));
      when(attachmentService.getAttachments(OBJECT_TYPE, invocation.getArgument(2))).thenReturn(attachmentList);
      return null;
    }).when(attachmentService)
      .saveAttachment(any(),
                      eq(OBJECT_TYPE),
                      any(),
                      any(),
                      anyLong());
    containerLayoutService.impersonateContainer(container, page);
    verify(appBackgroundStyle).setBackgroundImage(String.format("/portal/rest/v1/social/attachments/%s/%s_%s/%s",
                                                                OBJECT_TYPE,
                                                                PAGE_ID,
                                                                CONTAINER_ID,
                                                                newFileId));
  }

}
