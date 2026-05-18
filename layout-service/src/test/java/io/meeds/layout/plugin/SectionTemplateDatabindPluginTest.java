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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.meeds.layout.model.*;
import io.meeds.layout.service.ContainerLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.SectionTemplateService;
import io.meeds.layout.util.JsonUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.core.identity.model.Identity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.service.TranslationService;

@SpringBootTest(classes = { SectionTemplateDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class SectionTemplateDatabindPluginTest {

  @MockitoBean
  private SectionTemplateService        sectionTemplateService;

  @MockitoBean
  private DatabindService               databindService;

  @MockitoBean
  private FileService                   fileService;

  @MockitoBean
  private TranslationService            translationService;

  @MockitoBean
  private AttachmentService             attachmentService;

  @MockitoBean
  private PortletInstanceService        portletInstanceService;

  @MockitoBean
  private LayoutService                 layoutService;

  @MockitoBean
  private PageLayoutService             pageLayoutService;

  @MockitoBean
  private ContainerLayoutService        containerLayoutService;

  @MockitoBean
  private UserACL                       userAcl;

  @MockitoBean
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
    when(sectionTemplateDetail.getContent()).thenReturn(JsonUtils.toJsonString(new LayoutModel()));

    sectionTemplateDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(sectionTemplateService, times(1)).getSectionTemplate(1L, Locale.getDefault());

  }

  @Test
  void deserialize() throws Exception {
    File zipFile = createZipFileWithTwoJsonFiles();

    Identity identity = mock(Identity.class);
    Page page = mock(Page.class);
    PageKey pageKey = mock(PageKey.class);
    when(userAcl.getSuperUser()).thenReturn("root");
    when(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser())).thenReturn(identity);
    lenient().when(identity.getId()).thenReturn("29");
    when(sectionTemplateService.createSectionTemplate(any())).thenReturn(new SectionTemplate());
    when(layoutService.getPage(any(PageKey.class))).thenReturn(page);
    when(page.getPageKey()).thenReturn(pageKey);

    // When
    CompletableFuture<Pair<DatabindReport, File>> futureReport =
                                                               sectionTemplateDatabindPlugin.deserialize(zipFile, null, "admin");

    DatabindReport report = futureReport.thenApply(Pair::getLeft).join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedItems().size());

    verify(sectionTemplateService, times(2)).createSectionTemplate(any());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      SectionTemplateDatabind config1 = new SectionTemplateDatabind();
      config1.setContent(JsonUtils.toJsonString(new LayoutModel()));
      config1.setNames(Map.of("en", "Test Page 1"));
      config1.setDescriptions(Map.of("en", "Desc 1"));

      SectionTemplateDatabind config2 = new SectionTemplateDatabind();
      config2.setContent(JsonUtils.toJsonString(new LayoutModel()));
      config2.setNames(Map.of("en", "Test Page 2"));
      config2.setDescriptions(Map.of("en", "Desc 2"));

      addJsonToZip(zos, "SectionTemplate_1/config.json", JsonUtils.toJsonString(config1));
      addJsonToZip(zos, "SectionTemplate_1/layout.json", JsonUtils.toJsonString(new LayoutModel()));

      addJsonToZip(zos, "SectionTemplate_2/config.json", JsonUtils.toJsonString(config2));
      addJsonToZip(zos, "SectionTemplate_2/layout.json", JsonUtils.toJsonString(new LayoutModel()));
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
