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

import static io.meeds.layout.service.PortletInstanceService.*;
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

import io.meeds.layout.model.PortletInstanceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.plugin.attachment.PortletInstanceAttachmentPlugin;
import io.meeds.layout.plugin.translation.PortletInstanceCategoryTranslationPlugin;
import io.meeds.layout.plugin.translation.PortletInstanceTranslationPlugin;
import io.meeds.layout.storage.PortletInstanceCategoryStorage;
import io.meeds.layout.storage.PortletInstanceLayoutStorage;
import io.meeds.layout.storage.PortletInstanceStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { PortletInstanceService.class, })
@ExtendWith(MockitoExtension.class)
public class PortletInstanceServiceTest {

  private static final String             DESCRIPTION = "testDescription";

  private static final String             TITLE       = "testTitle";

  private static final String             CONTENT_ID  = "test/portlet";

  private static final String             USERNAME    = "test";

  @MockitoBean
  private LayoutAclService                layoutAclService;

  @MockitoBean
  private LayoutService                   layoutService;

  @MockitoBean
  private TranslationService              translationService;

  @MockitoBean
  private AttachmentService               attachmentService;

  @MockitoBean
  private LocaleConfigService             localeConfigService;

  @MockitoBean
  private PortletInstanceCategoryStorage  portletInstanceCategoryStorage;

  @MockitoBean
  private PortletInstanceLayoutStorage    portletInstanceLayoutStorage;

  @MockitoBean
  private PortletInstanceStorage          portletInstanceStorage;

  @MockitoBean
  private PortletService                  portletService;

  @MockitoBean
  private ListenerService                 listenerService;

  @Mock
  private PortletInstanceCategory         portletInstanceCategory;

  @Mock
  private PortletInstance                 portletInstance;

  @Mock
  private Application                     application;

  @Mock
  private LocaleConfig                    defaultLocaleConfig;

  @Mock
  private LocaleConfig                    localeConfig;

  @Mock
  private PortletInstancePreferencePlugin plugin;

  @Autowired
  private PortletInstanceService          portletInstanceService;

  private String                          testuser    = "testuser";

  @Test
  public void getPortletInstances() {
    when(portletInstance.getId()).thenReturn(2l);
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);

    when(portletInstanceStorage.getPortletInstances()).thenReturn(Collections.singletonList(portletInstance));
    List<PortletInstance> portletInstances = portletInstanceService.getPortletInstances();
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(portletInstance.getId(), portletInstances.get(0).getId());
    assertEquals(portletInstance.getContentId(), portletInstances.get(0).getContentId());
    assertNull(portletInstances.get(0).getName());
    assertNull(portletInstances.get(0).getDescription());
    assertEquals(0l, portletInstances.get(0).getIllustrationId());
  }

  @Test
  public void getPortletInstancesByCategoryId() {
    when(portletInstance.getId()).thenReturn(2l);
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);

    when(portletInstanceStorage.getPortletInstances(3l)).thenReturn(Collections.singletonList(portletInstance));
    List<PortletInstance> portletInstances = portletInstanceService.getPortletInstances(3l, null, null, false);
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(portletInstance.getId(), portletInstances.get(0).getId());
    assertEquals(portletInstance.getContentId(), portletInstances.get(0).getContentId());
    assertNull(portletInstances.get(0).getName());
    assertNull(portletInstances.get(0).getDescription());
    assertEquals(0l, portletInstances.get(0).getIllustrationId());
  }

  @Test
  public void getPortletInstancesWithExpand() throws ObjectNotFoundException {
    PortletInstance template = newPortletInstance();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(portletInstanceStorage.getPortletInstances()).thenReturn(Collections.singletonList(template));

    List<PortletInstance> portletInstances = portletInstanceService.getPortletInstances();
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(template.getId(), portletInstances.get(0).getId());
    assertEquals(template.getContentId(), portletInstances.get(0).getContentId());
    assertEquals(template.getIllustrationId(), portletInstances.get(0).getIllustrationId());

    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    portletInstances = portletInstanceService.getPortletInstances();
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    portletInstances = portletInstanceService.getPortletInstances(0, null, Locale.ENGLISH, true);
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(template.getId(), portletInstances.get(0).getId());
    assertEquals(template.getContentId(), portletInstances.get(0).getContentId());
    assertNull(portletInstances.get(0).getName());
    assertNull(portletInstances.get(0).getDescription());
    assertEquals(template.getIllustrationId(), portletInstances.get(0).getIllustrationId());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    portletInstances = portletInstanceService.getPortletInstances(0, null, Locale.FRENCH, true);
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(frTitle, portletInstances.get(0).getName());

    TranslationField descriptionTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME)).thenReturn(descriptionTranslationField);
    String enDesc = DESCRIPTION;
    when(descriptionTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.ENGLISH, enDesc));

    portletInstances = portletInstanceService.getPortletInstances(0, null, Locale.ENGLISH, true);
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(enDesc, portletInstances.get(0).getDescription());

    when(attachmentService.getAttachmentFileIds(PortletInstanceAttachmentPlugin.OBJECT_TYPE,
                                                "2")).thenReturn(Collections.singletonList("32"));
    portletInstances = portletInstanceService.getPortletInstances(0, null, Locale.ENGLISH, true);
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(32l, portletInstances.get(0).getIllustrationId());
  }

  @Test
  public void getPortletInstanceWithExpand() throws ObjectNotFoundException, IllegalAccessException {
    PortletInstance template = newPortletInstance();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(portletInstanceStorage.getPortletInstance(template.getId())).thenReturn(template);

    PortletInstance retrievedPortletInstance = portletInstanceService.getPortletInstance(template.getId());
    assertNotNull(retrievedPortletInstance);
    assertEquals(template.getId(), retrievedPortletInstance.getId());
    assertEquals(template.getContentId(), retrievedPortletInstance.getContentId());
    assertEquals(template.getIllustrationId(), retrievedPortletInstance.getIllustrationId());

    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l, null, Locale.FRENCH, true);
    assertNotNull(retrievedPortletInstance);

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l, null, Locale.FRENCH, true);
    assertNotNull(retrievedPortletInstance);
    assertEquals(template.getId(), retrievedPortletInstance.getId());
    assertEquals(template.getContentId(), retrievedPortletInstance.getContentId());
    assertNull(retrievedPortletInstance.getName());
    assertNull(retrievedPortletInstance.getDescription());
    assertEquals(template.getIllustrationId(), retrievedPortletInstance.getIllustrationId());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l, null, Locale.FRENCH, true);
    assertEquals(frTitle, retrievedPortletInstance.getName());

    TranslationField descriptionTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                template.getId(),
                                                PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME)).thenReturn(descriptionTranslationField);
    String enDesc = DESCRIPTION;
    when(descriptionTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.ENGLISH, enDesc));

    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l, null, Locale.ENGLISH, true);
    assertNotNull(retrievedPortletInstance);
    assertEquals(enDesc, retrievedPortletInstance.getDescription());

    when(attachmentService.getAttachmentFileIds(PortletInstanceAttachmentPlugin.OBJECT_TYPE,
                                                "2")).thenReturn(Collections.singletonList("32"));
    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l, null, Locale.GERMAN, true);
    assertNotNull(retrievedPortletInstance);
    assertEquals(32l, retrievedPortletInstance.getIllustrationId());
  }

  @Test
  public void getPortletInstance() {
    when(portletInstanceStorage.getPortletInstance(2l)).thenReturn(portletInstance);
    PortletInstance retrievedPortletInstance = portletInstanceService.getPortletInstance(3l);
    assertNull(retrievedPortletInstance);

    retrievedPortletInstance = portletInstanceService.getPortletInstance(2l);
    assertNotNull(retrievedPortletInstance);
    assertEquals(portletInstance, retrievedPortletInstance);
  }

  @Test
  public void createPortletInstance() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.createPortletInstance(portletInstance, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceStorage.createPortletInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
    portletInstanceService.createPortletInstance(portletInstance, testuser);
    verify(portletInstanceStorage, times(1)).createPortletInstance(portletInstance);
    verify(listenerService).broadcast(INSTANCE_CREATED_EVENT, testuser, portletInstance);
  }

  @Test
  public void deletePortletInstance() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstance(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> portletInstanceService.deletePortletInstance(2l, testuser));

    when(portletInstanceStorage.getPortletInstance(2l)).thenReturn(portletInstance);
    when(portletInstance.isSystem()).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstance(2l, testuser));

    when(portletInstance.isSystem()).thenReturn(false);
    portletInstanceService.deletePortletInstance(2l, testuser);

    verify(attachmentService, times(1)).deleteAttachments(PortletInstanceAttachmentPlugin.OBJECT_TYPE, "2");
    verify(translationService, times(1)).deleteTranslationLabels(PortletInstanceTranslationPlugin.OBJECT_TYPE, 2l);
    verify(portletInstanceStorage, times(1)).deletePortletInstance(2l);
    verify(listenerService).broadcast(INSTANCE_DELETED_EVENT, testuser, portletInstance);
  }

  @Test
  public void deletePortletInstanceWhenException() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstance(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceStorage.getPortletInstance(2l)).thenReturn(portletInstance);
    doThrow(RuntimeException.class).when(attachmentService).deleteAttachments(anyString(), any());
    doThrow(ObjectNotFoundException.class).when(translationService).deleteTranslationLabels(anyString(), anyLong());
    portletInstanceService.deletePortletInstance(2l, testuser);
    verify(portletInstanceStorage, times(1)).deletePortletInstance(2l);
  }

  @Test
  public void updatePortletInstance() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.updatePortletInstance(portletInstance, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceStorage.updatePortletInstance(any())).thenAnswer(invocation -> invocation.getArgument(0));
    portletInstanceService.updatePortletInstance(portletInstance, testuser);
    verify(portletInstanceStorage, times(1)).updatePortletInstance(portletInstance);
    verify(listenerService).broadcast(INSTANCE_UPDATED_EVENT, testuser, portletInstance);
  }

  @Test
  public void getPortletInstanceCategories() {
    when(portletInstanceCategory.getId()).thenReturn(2l);
    when(portletInstanceCategory.getIcon()).thenReturn(CONTENT_ID);

    when(portletInstanceCategoryStorage.getPortletInstanceCategories()).thenReturn(Collections.singletonList(portletInstanceCategory));
    List<PortletInstanceCategory> portletInstanceCategories = portletInstanceService.getPortletInstanceCategories();
    assertNotNull(portletInstanceCategories);
    assertEquals(1, portletInstanceCategories.size());
    assertEquals(portletInstanceCategory.getId(), portletInstanceCategories.get(0).getId());
    assertEquals(portletInstanceCategory.getIcon(), portletInstanceCategories.get(0).getIcon());
    assertNull(portletInstanceCategories.get(0).getName());
  }

  @Test
  public void getPortletInstanceCategoriesWithExpand() throws ObjectNotFoundException {
    PortletInstanceCategory category = newPortletInstanceCategory();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(portletInstanceCategoryStorage.getPortletInstanceCategories()).thenReturn(Collections.singletonList(category));

    List<PortletInstanceCategory> portletInstanceCategorys = portletInstanceService.getPortletInstanceCategories();
    assertNotNull(portletInstanceCategorys);
    assertEquals(1, portletInstanceCategorys.size());
    assertEquals(category.getId(), portletInstanceCategorys.get(0).getId());
    assertEquals(category.getIcon(), portletInstanceCategorys.get(0).getIcon());
    assertNull(portletInstanceCategorys.get(0).getName());

    when(translationService.getTranslationField(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE,
                                                category.getId(),
                                                PortletInstanceCategoryTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    portletInstanceCategorys = portletInstanceService.getPortletInstanceCategories(null, Locale.ENGLISH, true);
    assertNotNull(portletInstanceCategorys);
    assertEquals(1, portletInstanceCategorys.size());

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE,
                                                category.getId(),
                                                PortletInstanceCategoryTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    portletInstanceCategorys = portletInstanceService.getPortletInstanceCategories(null, Locale.FRENCH, true);
    assertNotNull(portletInstanceCategorys);
    assertEquals(1, portletInstanceCategorys.size());
    assertEquals(category.getId(), portletInstanceCategorys.get(0).getId());
    assertEquals(category.getIcon(), portletInstanceCategorys.get(0).getIcon());
    assertNull(portletInstanceCategorys.get(0).getName());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    portletInstanceCategorys = portletInstanceService.getPortletInstanceCategories(null, Locale.FRENCH, true);
    assertNotNull(portletInstanceCategorys);
    assertEquals(1, portletInstanceCategorys.size());
    assertEquals(frTitle, portletInstanceCategorys.get(0).getName());

    portletInstanceCategorys = portletInstanceService.getPortletInstanceCategories(null, Locale.ENGLISH, true);
    assertNotNull(portletInstanceCategorys);
    assertEquals(1, portletInstanceCategorys.size());
  }

  @Test
  public void getPortletInstanceCategoryWithExpand() throws ObjectNotFoundException, IllegalAccessException {
    PortletInstanceCategory category = newPortletInstanceCategory();
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);

    when(portletInstanceCategoryStorage.getPortletInstanceCategory(category.getId())).thenReturn(category);

    PortletInstanceCategory retrievedPortletInstanceCategory =
                                                             portletInstanceService.getPortletInstanceCategory(category.getId());
    assertNotNull(retrievedPortletInstanceCategory);
    assertEquals(category.getId(), retrievedPortletInstanceCategory.getId());
    assertEquals(category.getIcon(), retrievedPortletInstanceCategory.getIcon());

    when(translationService.getTranslationField(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE,
                                                category.getId(),
                                                PortletInstanceCategoryTranslationPlugin.TITLE_FIELD_NAME)).thenThrow(ObjectNotFoundException.class);
    retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(category.getId(),
                                                                                         null,
                                                                                         Locale.FRENCH,
                                                                                         true);
    assertNotNull(retrievedPortletInstanceCategory);

    reset(translationService);

    TranslationField titleTranslationField = mock(TranslationField.class);
    when(translationService.getTranslationField(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE,
                                                category.getId(),
                                                PortletInstanceCategoryTranslationPlugin.TITLE_FIELD_NAME)).thenReturn(titleTranslationField);
    retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(category.getId(),
                                                                                         null,
                                                                                         Locale.FRENCH,
                                                                                         true);
    assertNotNull(retrievedPortletInstanceCategory);
    assertEquals(category.getId(), retrievedPortletInstanceCategory.getId());
    assertEquals(category.getIcon(), retrievedPortletInstanceCategory.getIcon());
    assertNull(retrievedPortletInstanceCategory.getName());

    String frTitle = TITLE;
    when(titleTranslationField.getLabels()).thenReturn(Collections.singletonMap(Locale.FRENCH, frTitle));

    retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(category.getId(),
                                                                                         null,
                                                                                         Locale.FRENCH,
                                                                                         true);
    assertEquals(frTitle, retrievedPortletInstanceCategory.getName());

    retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(category.getId(),
                                                                                         null,
                                                                                         Locale.ENGLISH,
                                                                                         true);
    assertNotNull(retrievedPortletInstanceCategory);
  }

  @Test
  public void getPortletInstanceCategory() {
    when(portletInstanceCategoryStorage.getPortletInstanceCategory(2l)).thenReturn(portletInstanceCategory);
    PortletInstanceCategory retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(3l);
    assertNull(retrievedPortletInstanceCategory);

    retrievedPortletInstanceCategory = portletInstanceService.getPortletInstanceCategory(2l);
    assertNotNull(retrievedPortletInstanceCategory);
    assertEquals(portletInstanceCategory, retrievedPortletInstanceCategory);
  }

  @Test
  public void createPortletInstanceCategory() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class,
                 () -> portletInstanceService.createPortletInstanceCategory(portletInstanceCategory, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceCategoryStorage.createPortletInstanceCategory(any())).thenAnswer(invocation -> invocation.getArgument(0));
    portletInstanceService.createPortletInstanceCategory(portletInstanceCategory, testuser);
    verify(portletInstanceCategoryStorage, times(1)).createPortletInstanceCategory(portletInstanceCategory);
    verify(listenerService).broadcast(CATEGORY_CREATED_EVENT, testuser, portletInstanceCategory);
  }

  @Test
  public void deletePortletInstanceCategory() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstanceCategory(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> portletInstanceService.deletePortletInstanceCategory(2l, testuser));

    when(portletInstanceCategoryStorage.getPortletInstanceCategory(2l)).thenReturn(portletInstanceCategory);
    when(portletInstanceCategory.isSystem()).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstanceCategory(2l, testuser));

    when(portletInstanceCategory.isSystem()).thenReturn(false);
    portletInstanceService.deletePortletInstanceCategory(2l, testuser);

    verify(translationService, times(1)).deleteTranslationLabels(PortletInstanceCategoryTranslationPlugin.OBJECT_TYPE, 2l);
    verify(portletInstanceCategoryStorage, times(1)).deletePortletInstanceCategory(2l);
    verify(listenerService).broadcast(CATEGORY_DELETED_EVENT, testuser, portletInstanceCategory);
  }

  @Test
  public void deletePortletInstanceCategoryWhenException() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> portletInstanceService.deletePortletInstanceCategory(2l, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceCategoryStorage.getPortletInstanceCategory(2l)).thenReturn(portletInstanceCategory);
    doThrow(ObjectNotFoundException.class).when(translationService).deleteTranslationLabels(anyString(), anyLong());
    portletInstanceService.deletePortletInstanceCategory(2l, testuser);
    verify(portletInstanceCategoryStorage, times(1)).deletePortletInstanceCategory(2l);
  }

  @Test
  public void updatePortletInstanceCategory() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class,
                 () -> portletInstanceService.updatePortletInstanceCategory(portletInstanceCategory, testuser));

    when(layoutAclService.isAdministrator(testuser)).thenReturn(true);
    when(portletInstanceCategoryStorage.updatePortletInstanceCategory(any())).thenAnswer(invocation -> invocation.getArgument(0));
    portletInstanceService.updatePortletInstanceCategory(portletInstanceCategory, testuser);
    verify(portletInstanceCategoryStorage, times(1)).updatePortletInstanceCategory(portletInstanceCategory);
    verify(listenerService).broadcast(CATEGORY_UPDATED_EVENT, testuser, portletInstanceCategory);
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplication() {
    assertThrows(ObjectNotFoundException.class, () -> portletInstanceService.getPortletInstanceApplication(2, 0, USERNAME));
    when(portletInstanceStorage.getPortletInstance(2)).thenReturn(portletInstance);
    when(portletInstanceLayoutStorage.getPortletInstanceApplication(portletInstance, 0)).thenReturn(application);
    Application portletInstanceApplication = portletInstanceService.getPortletInstanceApplication(2, 0, USERNAME);
    assertEquals(application, portletInstanceApplication);
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationPlaceholder() {
    assertNull(portletInstanceService.getPortletInstanceApplication(0, 0, USERNAME));
  }

  @Test
  @SneakyThrows
  public void getPortletInstancePreferencesWhenNoPlugin() {
    when(portletInstanceStorage.getPortletInstance(2)).thenReturn(portletInstance);
    when(application.getStorageId()).thenReturn("3");
    when(portletInstanceLayoutStorage.getApplicationPortletName(application)).thenReturn(CONTENT_ID);

    Portlet portlet = new Portlet();
    portlet.setValue("test", "testValue");
    when(portletInstanceLayoutStorage.getOrCreatePortletInstanceApplication(portletInstance)).thenReturn(application);
    when(portletInstanceLayoutStorage.getApplicationPreferences(Long.parseLong(application.getStorageId()))).thenReturn(portlet);

    List<PortletInstancePreference> portletInstancePreferences =
                                                               portletInstanceService.getPortletInstancePreferences(2,
                                                                                                                    new PortletInstanceContext(),
                                                                                                                    USERNAME);
    assertNotNull(portletInstancePreferences);
    assertEquals(1, portletInstancePreferences.size());
    assertEquals("test", portletInstancePreferences.get(0).getName());
    assertEquals("testValue", portletInstancePreferences.get(0).getValue());
  }

  @Test
  @SneakyThrows
  public void getPortletInstancePreferencesWithPlugin() {
    when(portletInstanceStorage.getPortletInstance(2)).thenReturn(portletInstance);
    when(application.getStorageId()).thenReturn("3");
    when(portletInstanceLayoutStorage.getApplicationPortletName(application)).thenReturn(CONTENT_ID.split("/")[1]);
    when(portletInstanceLayoutStorage.getOrCreatePortletInstanceApplication(portletInstance)).thenReturn(application);
    when(plugin.getPortletName()).thenReturn(CONTENT_ID.split("/")[1]);
    portletInstanceService.addPortletInstancePreferencePlugin(plugin);
    try {
      when(plugin.generatePreferences(any(),
                                      any(),
                                      any())).thenReturn(Collections.singletonList(new PortletInstancePreference("test", "value")));

      List<PortletInstancePreference> portletInstancePreferences =
                                                                 portletInstanceService.getPortletInstancePreferences(2,
                                                                                                                      new PortletInstanceContext(),
                                                                                                                      USERNAME);
      assertNotNull(portletInstancePreferences);
      assertEquals(1, portletInstancePreferences.size());
      assertEquals("test", portletInstancePreferences.get(0).getName());
      assertEquals("value", portletInstancePreferences.get(0).getValue());
    } finally {
      portletInstanceService.removePortletInstancePreferencePlugin(plugin.getPortletName());
    }
  }

  @Test
  @SneakyThrows
  public void getPortletInstancePreferencesWhenNoPluginNoPreferences() {
    assertThrows(ObjectNotFoundException.class,
                 () -> portletInstanceService.getPortletInstancePreferences(2, new PortletInstanceContext(), USERNAME));

    when(portletInstanceStorage.getPortletInstance(2)).thenReturn(portletInstance);
    when(portletInstanceLayoutStorage.getApplicationPortletName(application)).thenReturn(CONTENT_ID.split("/")[1]);
    when(portletInstanceLayoutStorage.getOrCreatePortletInstanceApplication(portletInstance)).thenReturn(application);
    when(application.getStorageId()).thenReturn("3");
    assertNotNull(portletInstanceService.getPortletInstancePreferences(2, new PortletInstanceContext(), USERNAME));
    assertEquals(0, portletInstanceService.getPortletInstancePreferences(2, new PortletInstanceContext(), USERNAME).size());
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationByApplicationId() {
    when(portletInstanceLayoutStorage.getPortletInstanceApplication(null, 3)).thenReturn(application);

    Application portletInstanceApplication = portletInstanceService.getPortletInstanceApplication(0, 3, USERNAME);
    assertEquals(application, portletInstanceApplication);
  }

  private PortletInstanceCategory newPortletInstanceCategory() {
    return new PortletInstanceCategory(3l, null, "icon", true, Collections.singletonList("Everyone"));
  }

  private PortletInstance newPortletInstance() {
    return new PortletInstance(2l,
                               "name",
                               "description",
                               5l,
                               CONTENT_ID,
                               0,
                               Collections.singletonList(new PortletInstancePreference("prefName", "prefValue")),
                               7l,
                               Collections.singletonList("Everyone"),
                               Collections.singletonList("edit"),
                               true,
                               false,
                               false);
  }

}
