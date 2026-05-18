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
package io.meeds.layout.service;

import static io.meeds.layout.util.EntityMapper.SITE_ENABLED_PROP;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.plugin.attachment.SiteTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SiteTemplateTranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { SiteTemplateService.class, })
@ExtendWith(MockitoExtension.class)
public class SiteTemplateServiceTest {

  private static final Locale     LOCALE      = Locale.ENGLISH;

  private static final long       ID          = 5l;

  private static final String     NAME        = "Sample Site Template";

  private static final String     DESCRIPTION = "This is a sample site template.";

  @MockitoBean
  private LayoutService           layoutService;

  @MockitoBean
  private NavigationService       navigationService;

  @MockitoBean
  private UserPortalConfigService portalConfigService;

  @MockitoBean
  private LayoutAclService        aclService;

  @MockitoBean
  private TranslationService      translationService;

  @MockitoBean
  private AttachmentService       attachmentService;

  @MockitoBean
  private ListenerService         listenerService;

  @Autowired
  private SiteTemplateService     siteTemplateService;

  private String                  testuser    = "testuser";

  private SiteTemplate            siteTemplate;

  private PortalConfig            portalConfig;

  @BeforeEach
  void setUp() {
    siteTemplate = new SiteTemplate();
    siteTemplate.setId(ID);
    siteTemplate.setLayout("layout");
    siteTemplate.setName(NAME);
    siteTemplate.setDescription(DESCRIPTION);
    siteTemplate.setIcon("icon.png");
    siteTemplate.setSystem(false);
    siteTemplate.setDisabled(false);

    portalConfig = mock(PortalConfig.class);
    lenient().when(portalConfig.getId()).thenReturn(ID);
    lenient().when(portalConfig.getName()).thenReturn("layout");
    lenient().when(portalConfig.getType()).thenReturn(PortalConfig.PORTAL_TEMPLATE);
    lenient().when(portalConfig.getIcon()).thenReturn(siteTemplate.getIcon());
    lenient().when(portalConfig.isRemovable()).thenReturn(!siteTemplate.isSystem());
    lenient().when(portalConfig.getProperty(SITE_ENABLED_PROP)).thenReturn(String.valueOf(!siteTemplate.isDisabled()));
  }

  @Test
  void testGetSiteTemplates() {
    when(layoutService.getSites(any())).thenReturn(Arrays.asList(portalConfig));

    List<SiteTemplate> templates = siteTemplateService.getSiteTemplates(LOCALE);

    assertNotNull(templates);
    assertEquals(1, templates.size());
  }

  @Test
  @SneakyThrows
  void testGetSiteTemplateById() {
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);

    SiteTemplate template = siteTemplateService.getSiteTemplate(ID);

    assertNotNull(template);
    assertNull(template.getName());
    assertNull(template.getDescription());
    assertEquals(0l, template.getIllustrationId());
  }

  @Test
  @SneakyThrows
  void testGetSiteTemplateByIdWithDetails() {
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    when(translationService.getTranslationLabelOrDefault(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                         portalConfig.getId(),
                                                         SiteTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                         LOCALE)).thenReturn(NAME);
    when(translationService.getTranslationLabelOrDefault(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                         portalConfig.getId(),
                                                         SiteTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                         LOCALE)).thenReturn(DESCRIPTION);
    when(attachmentService.getAttachmentFileIds(SiteTemplateAttachmentPlugin.OBJECT_TYPE,
                                                String.valueOf(ID))).thenReturn(Collections.singletonList("4"));
    SiteTemplate template = siteTemplateService.getSiteTemplate(ID, LOCALE);

    assertNotNull(template);
    assertEquals(NAME, template.getName());
    assertEquals(DESCRIPTION, template.getDescription());
    assertEquals(4l, template.getIllustrationId());
  }

  @Test
  @SneakyThrows
  void testCreateSiteTemplate() {
    when(aclService.isAdministrator(testuser)).thenReturn(true);
    doAnswer(invocation -> {
      when(layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout()))).thenReturn(portalConfig);
      return null;
    }).when(portalConfigService)
      .createSiteFromTemplate(SiteKey.portalTemplate(SiteTemplateService.SITE_TEMPLATE_BASE),
                              SiteKey.portalTemplate(siteTemplate.getLayout()));
    SiteTemplate createdTemplate = siteTemplateService.createSiteTemplate(siteTemplate, testuser);
    assertNotNull(createdTemplate);
    verify(listenerService).broadcast(SiteTemplateService.TEMPLATE_CREATED_EVENT, createdTemplate, testuser);
  }

  @Test
  void testCreateSiteTemplateWithExistingName() {
    when(aclService.isAdministrator(testuser)).thenReturn(true);
    when(layoutService.getPortalConfig(any(SiteKey.class))).thenReturn(portalConfig); // Simulate
    assertThrows(ObjectAlreadyExistsException.class,
                 () -> siteTemplateService.createSiteTemplate(siteTemplate, testuser));
  }

  @Test
  @SneakyThrows
  void testCreateSiteTemplateWhenNotAdmin() {
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> siteTemplateService.createSiteTemplate(siteTemplate, testuser));
  }

  @Test
  @SneakyThrows
  void testUpdateSiteTemplate() {
    when(aclService.isAdministrator(testuser)).thenReturn(true);
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);

    SiteTemplate updatedTemplate = siteTemplateService.updateSiteTemplate(siteTemplate, testuser);

    assertNotNull(updatedTemplate);
    verify(listenerService).broadcast(SiteTemplateService.TEMPLATE_UPDATED_EVENT, updatedTemplate, testuser);
  }

  @Test
  @SneakyThrows
  void testUpdateSiteTemplateWhenNotFound() {
    assertThrows(ObjectNotFoundException.class, () -> siteTemplateService.updateSiteTemplate(siteTemplate, testuser));
  }

  @Test
  @SneakyThrows
  void testUpdateSiteTemplateWhenNotAdmin() {
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> siteTemplateService.updateSiteTemplate(siteTemplate, testuser));
  }

  @Test
  @SneakyThrows
  void testDeleteSiteTemplate() {
    when(aclService.isAdministrator(testuser)).thenReturn(true);
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    when(layoutService.getPortalConfig(SiteKey.portalTemplate(siteTemplate.getLayout()))).thenReturn(portalConfig);

    siteTemplateService.deleteSiteTemplate(ID, testuser);

    verify(layoutService).remove(any(PortalConfig.class));
    verify(listenerService).broadcast(SiteTemplateService.TEMPLATE_DELETED_EVENT, siteTemplate, testuser);
  }

  @Test
  @SneakyThrows
  void testDeleteSiteTemplateWhenSystem() {
    when(aclService.isAdministrator(testuser)).thenReturn(true);
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    when(portalConfig.isRemovable()).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> siteTemplateService.deleteSiteTemplate(ID, testuser));
  }

  @Test
  void testDeleteSiteTemplateWithoutAdminPermission() {
    when(layoutService.getPortalConfig(ID)).thenReturn(portalConfig);
    when(aclService.isAdministrator(testuser)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> siteTemplateService.deleteSiteTemplate(ID, testuser));
  }

  @Test
  void testDeleteSiteTemplateWhenNotFound() {
    assertThrows(ObjectNotFoundException.class, () -> siteTemplateService.deleteSiteTemplate(ID, testuser));
  }

}
