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

import static io.meeds.layout.service.SectionTemplateService.TEMPLATE_CREATED_EVENT;
import static io.meeds.layout.service.SectionTemplateService.TEMPLATE_DELETED_EVENT;
import static io.meeds.layout.service.SectionTemplateService.TEMPLATE_UPDATED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.model.SectionTemplateDetail;
import io.meeds.layout.plugin.attachment.SectionTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SectionTemplateTranslationPlugin;
import io.meeds.layout.storage.SectionTemplateLayoutStorage;
import io.meeds.layout.storage.SectionTemplateStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

@SpringBootTest(classes = { SectionTemplateService.class, })
@ExtendWith(MockitoExtension.class)
public class SectionTemplateServiceTest {

  private static final String          CATEGORY    = "category";

  private static final String          DESCRIPTION = "testDescription";

  private static final String          TITLE       = "testTitle";

  private static final String          CONTENT     = "{}";

  @MockitoBean
  private LayoutAclService             layoutAclService;

  @MockitoBean
  private TranslationService           translationService;

  @MockitoBean
  private AttachmentService            attachmentService;

  @MockitoBean
  private LocaleConfigService          localeConfigService;

  @MockitoBean
  private SectionTemplateLayoutStorage sectionTemplateLayoutStorage;

  @MockitoBean
  private ContainerLayoutService       containerLayoutService;

  @MockitoBean
  private SectionTemplateStorage       sectionTemplateStorage;

  @MockitoBean
  private PortletService               portletService;

  @MockitoBean
  private ListenerService              listenerService;

  @Mock
  private SectionTemplate              sectionTemplate;

  @Mock
  private LocaleConfig                 defaultLocaleConfig;

  @Mock
  private LocaleConfig                 localeConfig;

  @Autowired
  private SectionTemplateService       sectionTemplateService;

  private String                       testuser    = "testuser";

  @Test
  public void getSectionTemplates() {
    when(sectionTemplate.getId()).thenReturn(2l);
    when(sectionTemplate.getContent()).thenReturn(CONTENT);

    when(sectionTemplateStorage.getSectionTemplates()).thenReturn(Collections.singletonList(sectionTemplate));
    List<SectionTemplate> sectionTemplates = sectionTemplateService.getSectionTemplates();
    assertNotNull(sectionTemplates);
    assertEquals(1, sectionTemplates.size());
    assertEquals(sectionTemplate.getId(), sectionTemplates.get(0).getId());
    assertEquals(sectionTemplate.getContent(), sectionTemplates.get(0).getContent());
  }

  @Test
  public void getSectionTemplatesWithExpand() throws ObjectNotFoundException {
    SectionTemplate template = newSectionTemplate();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(sectionTemplateStorage.getSectionTemplates()).thenReturn(Collections.singletonList(template));

    List<SectionTemplate> sectionTemplates = sectionTemplateService.getSectionTemplates();
    assertNotNull(sectionTemplates);
    assertEquals(1, sectionTemplates.size());
    assertEquals(template.getId(), sectionTemplates.get(0).getId());
    assertEquals(template.getContent(), sectionTemplates.get(0).getContent());

    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    sectionTemplates = sectionTemplateService.getSectionTemplates();
    assertNotNull(sectionTemplates);
    assertEquals(1, sectionTemplates.size());

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    List<SectionTemplateDetail> sectionTemplatesWithDetail = sectionTemplateService.getSectionTemplates(Locale.ENGLISH);
    assertNotNull(sectionTemplatesWithDetail);
    assertEquals(1, sectionTemplatesWithDetail.size());
    assertEquals(template.getId(), sectionTemplatesWithDetail.get(0).getId());
    assertEquals(template.getContent(), sectionTemplatesWithDetail.get(0).getContent());
    assertNull(sectionTemplatesWithDetail.get(0).getName());
    assertNull(sectionTemplatesWithDetail.get(0).getDescription());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    sectionTemplatesWithDetail = sectionTemplateService.getSectionTemplates(Locale.FRENCH);
    assertNotNull(sectionTemplatesWithDetail);
    assertEquals(1, sectionTemplatesWithDetail.size());
    assertEquals(frTitle, sectionTemplatesWithDetail.get(0).getName());

    TranslationField descriptionTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME)).thenReturn(descriptionTranslationField);
    String enDesc = DESCRIPTION;
    when(descriptionTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.ENGLISH, enDesc));

    sectionTemplatesWithDetail = sectionTemplateService.getSectionTemplates(Locale.ENGLISH);
    assertNotNull(sectionTemplatesWithDetail);
    assertEquals(1, sectionTemplatesWithDetail.size());
    assertEquals(enDesc, sectionTemplatesWithDetail.get(0).getDescription());

    when(attachmentService.getAttachmentFileIds(SectionTemplateAttachmentPlugin.OBJECT_TYPE,
                                                "2")).thenReturn(Collections.singletonList("32"));
    sectionTemplatesWithDetail = sectionTemplateService.getSectionTemplates(Locale.ENGLISH);
    assertNotNull(sectionTemplatesWithDetail);
    assertEquals(1, sectionTemplatesWithDetail.size());
    assertEquals(32l, sectionTemplatesWithDetail.get(0).getIllustrationId());
  }

  @Test
  public void getSectionTemplateWithExpand() throws ObjectNotFoundException {
    SectionTemplate template = newSectionTemplate();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(sectionTemplateStorage.getSectionTemplate(template.getId())).thenReturn(template);

    SectionTemplate retrievedSectionTemplate = sectionTemplateService.getSectionTemplate(template.getId());
    assertNotNull(retrievedSectionTemplate);
    assertEquals(template.getId(), retrievedSectionTemplate.getId());
    assertEquals(template.getContent(), retrievedSectionTemplate.getContent());
    assertEquals(template.getCategory(), retrievedSectionTemplate.getCategory());

    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    retrievedSectionTemplate = sectionTemplateService.getSectionTemplate(2l, Locale.FRENCH);
    assertNotNull(retrievedSectionTemplate);

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    SectionTemplateDetail retrievedSectionTemplateWithDetail = sectionTemplateService.getSectionTemplate(2l, Locale.FRENCH);
    assertNotNull(retrievedSectionTemplateWithDetail);
    assertEquals(template.getId(), retrievedSectionTemplateWithDetail.getId());
    assertEquals(template.getContent(), retrievedSectionTemplateWithDetail.getContent());
    assertEquals(template.getCategory(), retrievedSectionTemplateWithDetail.getCategory());
    assertNull(retrievedSectionTemplateWithDetail.getName());
    assertNull(retrievedSectionTemplateWithDetail.getDescription());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    retrievedSectionTemplateWithDetail = sectionTemplateService.getSectionTemplate(2l, Locale.FRENCH);
    assertEquals(frTitle, retrievedSectionTemplateWithDetail.getName());

    TranslationField descriptionTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME)).thenReturn(descriptionTranslationField);
    String enDesc = DESCRIPTION;
    when(descriptionTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.ENGLISH, enDesc));

    retrievedSectionTemplateWithDetail = sectionTemplateService.getSectionTemplate(2l, Locale.ENGLISH);
    assertNotNull(retrievedSectionTemplateWithDetail);
    assertEquals(enDesc, retrievedSectionTemplateWithDetail.getDescription());

    when(attachmentService.getAttachmentFileIds(SectionTemplateAttachmentPlugin.OBJECT_TYPE,
                                                "2")).thenReturn(Collections.singletonList("32"));
    retrievedSectionTemplateWithDetail = sectionTemplateService.getSectionTemplate(2l, Locale.GERMAN);
    assertNotNull(retrievedSectionTemplateWithDetail);
    assertEquals(32l, retrievedSectionTemplateWithDetail.getIllustrationId());
  }

  @Test
  public void getSectionTemplate() {
    when(sectionTemplateStorage.getSectionTemplate(2l)).thenReturn(sectionTemplate);
    SectionTemplate retrievedSectionTemplate = sectionTemplateService.getSectionTemplate(3l);
    assertNull(retrievedSectionTemplate);

    retrievedSectionTemplate = sectionTemplateService.getSectionTemplate(2l);
    assertNotNull(retrievedSectionTemplate);
    assertEquals(sectionTemplate, retrievedSectionTemplate);
  }

  @Test
  public void createSectionTemplate() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.createSectionTemplate(sectionTemplate, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(sectionTemplateStorage.createSectionTemplate(any())).thenAnswer(invocation -> invocation.getArgument(0));
    sectionTemplateService.createSectionTemplate(sectionTemplate, testuser);
    verify(sectionTemplateStorage, times(1)).createSectionTemplate(sectionTemplate);
    verify(listenerService).broadcast(TEMPLATE_CREATED_EVENT, testuser, sectionTemplate);
  }

  @Test
  public void deleteSectionTemplate() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.deleteSectionTemplate(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> sectionTemplateService.deleteSectionTemplate(2l, testuser));

    when(sectionTemplateStorage.getSectionTemplate(2l)).thenReturn(sectionTemplate);
    when(sectionTemplate.isSystem()).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.deleteSectionTemplate(2l, testuser));

    when(sectionTemplate.isSystem()).thenReturn(false);
    sectionTemplateService.deleteSectionTemplate(2l, testuser);

    verify(attachmentService, times(1)).deleteAttachments(SectionTemplateAttachmentPlugin.OBJECT_TYPE, "2");
    verify(translationService, times(1)).deleteTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE, 2l);
    verify(sectionTemplateStorage, times(1)).deleteSectionTemplate(2l);
    verify(listenerService).broadcast(TEMPLATE_DELETED_EVENT, testuser, sectionTemplate);
  }

  @Test
  public void deleteSectionTemplateWhenException() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.deleteSectionTemplate(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(sectionTemplateStorage.getSectionTemplate(2l)).thenReturn(sectionTemplate);
    doThrow(RuntimeException.class).when(attachmentService).deleteAttachments(anyString(), any());
    doThrow(ObjectNotFoundException.class).when(translationService).deleteTranslationLabels(anyString(), anyLong());
    sectionTemplateService.deleteSectionTemplate(2l, testuser);
    verify(sectionTemplateStorage, times(1)).deleteSectionTemplate(2l);
  }

  @Test
  public void generateSectionTemplateNodeId() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.generateSectionTemplateNodeId(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> sectionTemplateService.generateSectionTemplateNodeId(2l, testuser));

    when(sectionTemplateStorage.getSectionTemplate(2l)).thenReturn(sectionTemplate);

    NodeData nodeData = mock(NodeData.class);
    when(nodeData.getId()).thenReturn("35");
    when(sectionTemplateLayoutStorage.generateSectionTemplateNodeId(sectionTemplate, testuser)).thenReturn(nodeData);
    long nodeId = sectionTemplateService.generateSectionTemplateNodeId(2l, testuser);
    assertEquals(35l, nodeId);
  }

  @Test
  public void generateSectionTemplateContent() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.generateSectionTemplateContent(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> sectionTemplateService.generateSectionTemplateContent(2l, testuser));

    when(sectionTemplateStorage.getSectionTemplate(2l)).thenReturn(sectionTemplate);
    String value = "Layout content";
    when(sectionTemplateLayoutStorage.generateSectionTemplateContent(sectionTemplate, testuser)).thenReturn(value);
    String result = sectionTemplateService.generateSectionTemplateContent(2l, testuser);
    assertEquals(value, result);
  }

  @Test
  public void updateSectionTemplate() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> sectionTemplateService.updateSectionTemplate(sectionTemplate, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(sectionTemplateStorage.updateSectionTemplate(any())).thenAnswer(invocation -> invocation.getArgument(0));
    sectionTemplateService.updateSectionTemplate(sectionTemplate, testuser);
    verify(sectionTemplateStorage, times(1)).updateSectionTemplate(sectionTemplate);
    verify(listenerService).broadcast(TEMPLATE_UPDATED_EVENT, testuser, sectionTemplate);
  }

  private SectionTemplate newSectionTemplate() {
    return new SectionTemplate(2l,
                               CATEGORY,
                               CONTENT,
                               true,
                               false);
  }

}
