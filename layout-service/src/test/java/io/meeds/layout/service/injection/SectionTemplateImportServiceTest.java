/**
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
package io.meeds.layout.service.injection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.PortletService;
import io.meeds.layout.service.SectionTemplateService;

@SpringBootTest(classes = { SectionTemplateImportService.class })
@ExtendWith(MockitoExtension.class)
public class SectionTemplateImportServiceTest {

  @MockitoBean
  private LayoutAclService               layoutAclService;

  @MockitoBean
  private LayoutTranslationImportService layoutTranslationService;

  @MockitoBean
  private AttachmentService              attachmentService;

  @MockitoBean
  private SectionTemplateService         sectionTemplateService;

  @MockitoBean
  private PortletService                 portletService;

  @MockitoBean
  private SettingService                 settingService;

  @MockitoBean
  private ConfigurationManager           configurationManager;

  @Autowired
  private SectionTemplateImportService   sectionTemplateImportService;

  @Test
  public void init() {
    assertDoesNotThrow(() -> sectionTemplateImportService.init(),
                       "Shouldn't stop the container initialization if section templates import fails");
    assertDoesNotThrow(() -> sectionTemplateImportService.importSectionTemplates(),
                       "Shouldn't stop the container initialization if section templates import fails");
  }

}
