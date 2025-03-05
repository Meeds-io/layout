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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.injection.LayoutTranslationImportService;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.service.TranslationService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SpringBootTest(classes = { PortletInstanceDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class PortletInstanceDatabindPluginTest {

  @Mock
  private Identity                       userIdentity;

  @MockBean
  private PortletInstanceService         portletInstanceService;

  @MockBean
  private DatabindService                databindService;

  @MockBean
  private FileService                    fileService;

  @MockBean
  private LayoutTranslationImportService translationImportService;

  @MockBean
  private TranslationService             translationService;

  @MockBean
  private AttachmentService              attachmentService;

  @MockBean
  private ListenerService                listenerService;

  @MockBean
  private UserACL                        userAcl;

  @MockBean
  private IdentityManager                identityManager;

  @Autowired
  private PortletInstanceDatabindPlugin  portletInstanceDatabindPlugin;

  @Test
  void getObjectType() {
    assertEquals(PortletInstanceDatabindPlugin.OBJECT_TYPE, portletInstanceDatabindPlugin.getObjectType());
  }

  @Test
  void canHandleDatabind() {
    assertTrue(portletInstanceDatabindPlugin.canHandleDatabind("PortletInstance", "1"));
    assertFalse(portletInstanceDatabindPlugin.canHandleDatabind("ObjectInstance", "1"));
  }

  @Test
  void serialize() throws ObjectNotFoundException, IllegalAccessException {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    PortletInstance portletInstance = mock(PortletInstance.class);
    when(portletInstanceService.getPortletInstance(anyLong(),
                                                   anyString(),
                                                   any(Locale.class),
                                                   anyBoolean())).thenReturn(portletInstance);
    when(portletInstance.getContentId()).thenReturn("1");

    portletInstanceDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(portletInstanceService, times(1)).getPortletInstance(1L, "root", Locale.getDefault(), true);

  }

  @Test
  void deserialize() throws Exception {
    File zipFile = createZipFileWithTwoJsonFiles();

    when(portletInstanceService.createPortletInstance(any())).thenReturn(new PortletInstance());

    // When
    CompletableFuture<DatabindReport> futureReport = portletInstanceDatabindPlugin.deserialize(zipFile,
                                                                                               true,
                                                                                               Map.of("categoryId", "1"),
                                                                                               "admin");

    DatabindReport report = futureReport.join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedInstances().size());
    assertTrue(report.getProcessedInstances().contains("12345"));
    assertTrue(report.getProcessedInstances().contains("67890"));

    verify(portletInstanceService, times(2)).createPortletInstance(any());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      addJsonToZip(zos,
                   "instance1.json",
                   "{\"contentId\":\"12345\",\"names\":{\"en\":\"Test Instance 1\"},\"descriptions\":{\"en\":\"Desc 1\"}}");
      addJsonToZip(zos,
                   "instance2.json",
                   "{\"contentId\":\"67890\",\"names\":{\"en\":\"Test Instance 2\"},\"descriptions\":{\"en\":\"Desc 2\"}}");
    }
    return tempFile;
  }

  private void addJsonToZip(ZipOutputStream zos, String fileName, String jsonContent) throws IOException {
    ZipEntry entry = new ZipEntry(fileName);
    zos.putNextEntry(entry);
    zos.write(jsonContent.getBytes());
    zos.closeEntry();
  }
}
