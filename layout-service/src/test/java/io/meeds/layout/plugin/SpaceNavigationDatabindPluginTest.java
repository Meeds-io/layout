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

import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.space.template.model.SpaceTemplate;
import io.meeds.social.space.template.service.SpaceTemplateService;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
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

@SpringBootTest(classes = { SpaceNavigationDatabindPlugin.class, })
@ExtendWith(MockitoExtension.class)
class SpaceNavigationDatabindPluginTest {

  @Mock
  private Identity                      userIdentity;

  @MockBean
  protected DatabindService             databindService;

  @MockBean
  protected FileService                 fileService;

  @MockBean
  protected TranslationService          translationService;

  @MockBean
  protected SpaceTemplateService        spaceTemplateService;

  @MockBean
  protected AttachmentService           attachmentService;

  @MockBean
  LayoutService                         layoutService;

  @MockBean
  private NavigationService             navigationService;

  @MockBean
  DescriptionService                    descriptionService;

  @MockBean
  private NavigationLayoutService       navigationLayoutService;

  @MockBean
  private PageLayoutService             pageLayoutService;

  @MockBean
  PortletInstanceService                portletInstanceService;

  @MockBean
  protected UserACL                     userAcl;

  @MockBean
  private IdentityManager               identityManager;

  @Autowired
  private SpaceNavigationDatabindPlugin spaceNavigationDatabindPlugin;

  @Test
  void getObjectType() {
    assertEquals(SpaceNavigationDatabindPlugin.OBJECT_TYPE, spaceNavigationDatabindPlugin.getObjectType());
  }

  @Test
  void canHandleDatabind() {
    assertTrue(spaceNavigationDatabindPlugin.canHandleDatabind("SpaceTemplate", "1"));
    assertFalse(spaceNavigationDatabindPlugin.canHandleDatabind("ObjectInstance", "1"));
  }

  @Test
  void serialize() throws IllegalAccessException {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    SpaceTemplate spaceTemplate = mock(SpaceTemplate.class);
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(spaceTemplate.getLayout()).thenReturn("layout");
    when(spaceTemplateService.getSpaceTemplate(anyLong(),
                                               anyString(),
                                               any(Locale.class),
                                               anyBoolean())).thenReturn(spaceTemplate);
    when(layoutService.getPortalConfig(any(SiteKey.class))).thenReturn(portalConfig);

    spaceNavigationDatabindPlugin.serialize("1", zipOutputStream, "root");

    verify(spaceTemplateService, times(1)).getSpaceTemplate(anyLong(), anyString(), any(Locale.class), anyBoolean());

  }

  @Test
  @SneakyThrows
  @SuppressWarnings("unchecked")
  void deserialize() {
    File zipFile = createZipFileWithTwoJsonFiles();
    NodeContext<NodeContext<Object>> parentNode = mock(NodeContext.class);
    when(parentNode.getId()).thenReturn("85");
    when(navigationService.loadNode(any(SiteKey.class))).thenReturn(null).thenReturn(parentNode);
    doNothing().when(navigationService).saveNavigation(any(NavigationContext.class));

    SpaceTemplate spaceTemplate = mock(SpaceTemplate.class);
    when(spaceTemplate.getLayout()).thenReturn("layout");
    when(spaceTemplateService.getSpaceTemplate(anyLong())).thenReturn(spaceTemplate);

    // When
    CompletableFuture<Pair<DatabindReport, File>> futureReport =
                                                               spaceNavigationDatabindPlugin.deserialize(zipFile, null, "admin");

    DatabindReport report = futureReport.thenApply(Pair::getLeft).join();

    // Then
    assertNotNull(report);
    assertTrue(report.isSuccess());
    assertEquals(2, report.getProcessedItems().size());
  }

  private File createZipFileWithTwoJsonFiles() throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
      addJsonToZip(zos, "space1/config.json", "{\"spaceTemplateId\":\"4\"" + "}");

      // Add navigation JSON for spaceTemplate1
      addJsonToZip(zos,
                   "space1/navigation.json",
                   "[" + "{" + "\"name\":\"overview\"," + "\"icon\":null," + "\"visibility\":\"DISPLAYED\","
                       + "\"pageReference\":\"portal_template::public::overview\"," + "\"labels\":{}," + "\"children\":[" + "{"
                       + "\"name\":\"actions\"," + "\"icon\":null," + "\"visibility\":\"DISPLAYED\","
                       + "\"pageReference\":\"portal_template::public::actions\"," + "\"labels\":{}," + "\"children\":[]" + "}"
                       + "]" + "}" + "]");

      // Add second site template under folder "spaceTemplate2"
      addJsonToZip(zos, "site2/config.json", "{\"spaceTemplateId\":\"4\"" + "}");

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
