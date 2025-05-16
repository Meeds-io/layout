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

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.layout.util.JsonUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
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

import lombok.SneakyThrows;

@SpringBootTest(classes = { SiteTemplateDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class SiteTemplateDatabindPluginTest {

  @Mock
  private Identity                   userIdentity;

  @MockBean
  private SiteTemplateService        siteTemplateService;

  @MockBean
  LayoutService                      layoutService;

  @MockBean
  private DatabindService            databindService;

  @MockBean
  private FileService                fileService;

  @MockBean
  private TranslationService         translationService;

  @MockBean
  private AttachmentService          attachmentService;

  @MockBean
  private NavigationService          navigationService;

  @MockBean
  private PortletInstanceService     portletInstanceService;

  @MockBean
  private NavigationLayoutService    navigationLayoutService;

  @MockBean
  DescriptionService                 descriptionService;

  @MockBean
  PageLayoutService                  pageLayoutService;

  @MockBean
  private UserACL                    userAcl;

  @MockBean
  private IdentityManager            identityManager;

  @Autowired
  private SiteTemplateDatabindPlugin siteTemplateDatabindPlugin;

  @Test
  void getObjectType() {
    assertEquals(SiteTemplateDatabindPlugin.OBJECT_TYPE, siteTemplateDatabindPlugin.getObjectType());
  }

  @Test
  void canHandleDatabind() {
    assertTrue(siteTemplateDatabindPlugin.canHandleDatabind("SiteTemplate", "1"));
    assertFalse(siteTemplateDatabindPlugin.canHandleDatabind("ObjectInstance", "1"));
  }

  @Test
  void serialize() throws ObjectNotFoundException {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    SiteTemplate siteTemplate = mock(SiteTemplate.class);
    PortalConfig portalConfig = mock(PortalConfig.class);
    SiteLayout siteLayout = mock(SiteLayout.class);
    when(siteTemplate.getLayout()).thenReturn("layout");
    when(portalConfig.getPortalLayout()).thenReturn(siteLayout);
    when(siteTemplateService.getSiteTemplate(anyLong(), any(Locale.class))).thenReturn(siteTemplate);
    when(layoutService.getPortalConfig(any(SiteKey.class))).thenReturn(portalConfig);

    siteTemplateDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(siteTemplateService, times(1)).getSiteTemplate(1L, Locale.getDefault());

  }

  @Test
  @SneakyThrows
  @SuppressWarnings("unchecked")
  void deserialize() {
    File zipFile = createZipFileWithTwoJsonFiles();
    SiteTemplate siteTemplate = mock(SiteTemplate.class);
    PortalConfig portalConfig = mock(PortalConfig.class);
    SiteKey siteKey = mock(SiteKey.class);

    when(siteKey.getType()).thenReturn(SiteType.PORTAL);
    when(siteKey.getName()).thenReturn("site1");
    when(portalConfig.getSiteKey()).thenReturn(siteKey);
    when(siteTemplateService.getSiteTemplate(anyLong(), any(Locale.class))).thenReturn(siteTemplate);
    when(layoutService.getPortalConfig(any(SiteKey.class))).thenReturn(portalConfig);
    when(layoutService.getPortalConfig(anyString(), anyString())).thenReturn(portalConfig);
    when(siteTemplateService.createSiteTemplate(any(SiteTemplate.class),
                                                any(SiteKey.class),
                                                anyString(),
                                                anyBoolean())).thenReturn(new SiteTemplate());
    NodeContext<NodeContext<Object>> parentNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    when(parentNode.getId()).thenReturn("85");
    when(navigationService.loadNode(any(SiteKey.class))).thenReturn(parentNode);

    // When
    CompletableFuture<Pair<DatabindReport, File>> futureReport = siteTemplateDatabindPlugin.deserialize(zipFile, null, "admin");

    DatabindReport report = futureReport.thenApply(Pair::getLeft).join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedItems().size());
    assertTrue(report.getProcessedItems().contains("name1"));
    assertTrue(report.getProcessedItems().contains("name2"));

    verify(siteTemplateService, times(2)).createSiteTemplate(any(SiteTemplate.class),
                                                             any(SiteKey.class),
                                                             anyString(),
                                                             anyBoolean());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      addJsonToZip(zos,
                   "site1/config.json",
                   "{\"names\":{\"en\":\"Test Page 2\"}," + "\"descriptions\":{\"en\":\"Desc 2\"}," + "\"siteDefinition\":{"
                       + "\"name\":\"name1\"," + "\"type\":\"PORTAL\"," + "\"layout\":"
                       + JsonUtils.toJsonString(new LayoutModel()) + "}" + "}");

      // Add navigation JSON for site1
      addJsonToZip(zos,
                   "site1/navigation.json",
                   "[" + "{" + "\"name\":\"overview\"," + "\"icon\":null," + "\"visibility\":\"DISPLAYED\","
                       + "\"pageReference\":\"portal_template::public::overview\"," + "\"labels\":{}," + "\"children\":[" + "{"
                       + "\"name\":\"actions\"," + "\"icon\":null," + "\"visibility\":\"DISPLAYED\","
                       + "\"pageReference\":\"portal_template::public::actions\"," + "\"labels\":{}," + "\"children\":[]" + "}"
                       + "]" + "}" + "]");

      // Add second site template under folder "site2"
      addJsonToZip(zos,
                   "site2/config.json",
                   "{\"names\":{\"en\":\"Test Page 2\"}," + "\"descriptions\":{\"en\":\"Desc 2\"}," + "\"siteDefinition\":{"
                       + "\"name\":\"name2\"," + "\"type\":\"PORTAL\"," + "\"layout\":"
                       + JsonUtils.toJsonString(new LayoutModel()) + "}" + "}");
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
