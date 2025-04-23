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

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PageTemplateDatabind;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.service.TranslationService;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.social.attachment.AttachmentService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SpringBootTest(classes = { PageTemplateDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class PageTemplateDatabindPluginTest {

  @MockBean
  private PageTemplateService        pageTemplateService;

  @MockBean
  private DatabindService            databindService;

  @MockBean
  private FileService                fileService;

  @MockBean
  private TranslationService         translationService;

  @MockBean
  private AttachmentService          attachmentService;

  @MockBean
  private PortletInstanceService     portletInstanceService;

  @MockBean
  private UserACL                    userAcl;

  @MockBean
  private IdentityManager            identityManager;

  @Autowired
  private PageTemplateDatabindPlugin pageTemplateDatabindPlugin;

  @Test
  void getObjectType() {
    assertEquals(PageTemplateDatabindPlugin.OBJECT_TYPE, pageTemplateDatabindPlugin.getObjectType());
  }

  @Test
  void canHandleDatabind() {
    assertTrue(pageTemplateDatabindPlugin.canHandleDatabind("PageTemplate", "1"));
    assertFalse(pageTemplateDatabindPlugin.canHandleDatabind("ObjectInstance", "1"));
  }

  @Test
  void serialize() {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    PageTemplate pageTemplate = mock(PageTemplate.class);
    when(pageTemplateService.getPageTemplate(anyLong(), any(Locale.class), anyBoolean(), anyBoolean())).thenReturn(pageTemplate);
    when(pageTemplate.getContent()).thenReturn(JsonUtils.toJsonString(new LayoutModel()));

    pageTemplateDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(pageTemplateService, times(1)).getPageTemplate(1L, Locale.getDefault(), true, true);

  }

  @Test
  void deserialize() throws Exception {
    File zipFile = createZipFileWithTwoJsonFiles();

    Identity identity = mock(Identity.class);
    when(userAcl.getSuperUser()).thenReturn("root");
    when(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser())).thenReturn(identity);
    lenient().when(identity.getId()).thenReturn("29");
    when(pageTemplateService.createPageTemplate(any())).thenReturn(new PageTemplate());

    // When
    CompletableFuture<Pair<DatabindReport, File>> futureReport = pageTemplateDatabindPlugin.deserialize(zipFile, null, "admin");

    DatabindReport report = futureReport.thenApply(Pair::getLeft).join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedItems().size());

    verify(pageTemplateService, times(2)).createPageTemplate(any());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      PageTemplateDatabind config1 = new PageTemplateDatabind();
      config1.setContent(JsonUtils.toJsonString(new LayoutModel()));
      config1.setNames(Map.of("en", "Test Page 1"));
      config1.setDescriptions(Map.of("en", "Desc 1"));

      PageTemplateDatabind config2 = new PageTemplateDatabind();
      config2.setContent(JsonUtils.toJsonString(new LayoutModel()));
      config2.setNames(Map.of("en", "Test Page 2"));
      config2.setDescriptions(Map.of("en", "Desc 2"));

      addJsonToZip(zos, "PageTemplate_1/config.json", JsonUtils.toJsonString(config1));
      addJsonToZip(zos, "PageTemplate_1/layout.json", JsonUtils.toJsonString(new LayoutModel()));

      addJsonToZip(zos, "PageTemplate_2/config.json", JsonUtils.toJsonString(config2));
      addJsonToZip(zos, "PageTemplate_2/layout.json", JsonUtils.toJsonString(new LayoutModel()));
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
