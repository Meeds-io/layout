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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.model.SectionTemplateDetail;
import io.meeds.layout.service.SectionTemplateService;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.service.TranslationService;

@SpringBootTest(classes = { SectionTemplateDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class SectionTemplateDatabindPluginTest {

  @Mock
  private Identity                      userIdentity;

  @MockBean
  private SectionTemplateService        sectionTemplateService;

  @MockBean
  private DatabindService               databindService;

  @MockBean
  private FileService                   fileService;

  @MockBean
  private TranslationService            translationService;

  @MockBean
  private AttachmentService             attachmentService;

  @MockBean
  private UserACL                       userAcl;

  @MockBean
  private IdentityManager               identityManager;

  @Autowired
  private SectionTemplateDatabindPlugin sectionTemplateDatabindPlugin;

  @Test
  void getObjectType() {
    assertEquals(SectionTemplateDatabindPlugin.OBJECT_TYPE, sectionTemplateDatabindPlugin.getObjectType());
  }

  @Test
  void canHandleDatabind() {
    assertTrue(sectionTemplateDatabindPlugin.canHandleDatabind("SectionTemplate", "1"));
    assertFalse(sectionTemplateDatabindPlugin.canHandleDatabind("ObjectInstance", "1"));
  }

  @Test
  void serialize() throws ObjectNotFoundException {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    SectionTemplateDetail sectionTemplateDetail = mock(SectionTemplateDetail.class);
    when(sectionTemplateService.getSectionTemplate(anyLong(), any(Locale.class))).thenReturn(sectionTemplateDetail);
    when(sectionTemplateDetail.getContent()).thenReturn("1");

    sectionTemplateDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(sectionTemplateService, times(1)).getSectionTemplate(1L, Locale.getDefault());

  }

  @Test
  void deserialize() throws Exception {
    File zipFile = createZipFileWithTwoJsonFiles();

    when(sectionTemplateService.createSectionTemplate(any())).thenReturn(new SectionTemplate());

    // When
    CompletableFuture<Pair<DatabindReport, File>> futureReport =
                                                               sectionTemplateDatabindPlugin.deserialize(zipFile, null, "admin");

    DatabindReport report = futureReport.thenApply(Pair::getLeft).join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedItems().size());
    assertTrue(report.getProcessedItems().contains("12345"));
    assertTrue(report.getProcessedItems().contains("67890"));

    verify(sectionTemplateService, times(2)).createSectionTemplate(any());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      addJsonToZip(zos,
                   "SectionTemplate_1.json",
                   "{\"content\":\"12345\",\"names\":{\"en\":\"Test Page 1\"},\"descriptions\":{\"en\":\"Desc 1\"}}");
      addJsonToZip(zos,
                   "SectionTemplate_2.json",
                   "{\"content\":\"67890\",\"names\":{\"en\":\"Test Page 2\"},\"descriptions\":{\"en\":\"Desc 2\"}}");
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
