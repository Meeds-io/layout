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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.storage.PortletInstanceLayoutStorage;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
                            PortletInstanceLayoutStorage.class,
})
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PortletInstanceRenderServiceTest {

  private static final String             CONTENT_ID = "test/content";

  @MockitoBean
  private LayoutAclService                layoutAclService;

  @MockitoBean
  private SettingService                  settingService;

  @MockitoBean
  private LayoutService                   layoutService;

  @Autowired
  private PortletInstanceLayoutStorage    portletInstanceLayoutStorage;

  @Mock
  private Application                     application;

  @Mock
  private PortletInstance                 portletInstance;

  @Mock
  private Page                            page;

  @Mock
  private Container                       container;

  @Mock
  private PortletInstancePreferencePlugin plugin;

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationByInstanceId() {
    when(settingService.get(any(), any(), eq("2"))).thenReturn(new SettingValue("3"));
    when(layoutService.getApplicationModel("3")).thenReturn(application);
    when(portletInstance.getId()).thenReturn(2l);

    Application portletInstanceApplication = portletInstanceLayoutStorage.getPortletInstanceApplication(portletInstance, 0);
    assertEquals(application, portletInstanceApplication);
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationPlaceholder() {
    assertNull(portletInstanceLayoutStorage.getPortletInstanceApplication(null, 0));
  }

  @Test
  @SneakyThrows
  public void getPortletInstancePreferencesWhenNoPlugin() {
    when(layoutService.getApplicationModel("3")).thenReturn(application);
    Portlet portlet = new Portlet();
    when(layoutService.load(any())).thenReturn(portlet);
    portlet.setValue("test", "testValue");

    Portlet portletInstancePreferences = portletInstanceLayoutStorage.getApplicationPreferences(3);
    assertNotNull(portletInstancePreferences);
    assertEquals("test", portletInstancePreferences.iterator().next().getName());
    assertEquals("testValue", portletInstancePreferences.iterator().next().getValue());
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationByInstanceIdAndCreateApplication() {
    PortletInstancePreference preference = new PortletInstancePreference();
    preference.setName("prefName");
    preference.setValue("prefValue");
    when(portletInstance.getPreferences()).thenReturn(Collections.singletonList(preference));
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);

    doAnswer(invocation -> {
      when(layoutService.getPage(any(PageKey.class))).thenReturn(page);
      ArrayList list = new ArrayList<>();
      list.add(container);
      when(page.getChildren()).thenReturn(list);

      ArrayList apps = new ArrayList<>();
      apps.add(application);
      when(container.getChildren()).thenReturn(apps);
      return null;
    }).when(layoutService).save(any(PageContext.class), any(Page.class));

    doAnswer(invocation -> {
      ArrayList<ModelObject> children = invocation.getArgument(0);
      children.set(children.size() - 1, application);
      when(container.getChildren()).thenReturn(children);
      when(application.getStorageId()).thenReturn("3");
      return null;
    }).when(container).setChildren(any());

    Application portletInstanceApplication = portletInstanceLayoutStorage.getPortletInstanceApplication(portletInstance, 0);
    assertEquals(application, portletInstanceApplication);
  }

  @Test
  @SneakyThrows
  public void getPortletInstanceApplicationByApplicationId() {
    when(settingService.get(any(), any(), eq("2"))).thenReturn(new SettingValue("3"));

    when(layoutService.getApplicationModel("3")).thenReturn(application);
    Application portletInstanceApplication = portletInstanceLayoutStorage.getPortletInstanceApplication(null, 3);
    assertEquals(application, portletInstanceApplication);
  }

}
