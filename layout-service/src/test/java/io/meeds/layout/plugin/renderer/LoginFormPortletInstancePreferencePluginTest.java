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
package io.meeds.layout.plugin.renderer;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.social.translation.service.TranslationService;
import lombok.SneakyThrows;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.social.attachment.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
  LoginFormPortletInstancePreferencePlugin.class,
})
@ExtendWith(MockitoExtension.class)
public class LoginFormPortletInstancePreferencePluginTest {

  private static final String OTHER_PREF_NAME = "name2";
  private static final String SETTING_NAME = "name";
  private static final String    DATA_INIT_PREFERENCE_NAME   = "data.init";

  @Autowired
  private LoginFormPortletInstancePreferencePlugin loginFormPortletInstancePreferencePlugin;

  @MockBean
  private TranslationService translationService;

  @MockBean
  private FileService fileService;

  @MockBean
  private AttachmentService attachmentService;

  @BeforeEach
  @SneakyThrows
  public void setup() {
    lenient().when(translationService.getAllTranslationFields(any(),any())).thenReturn(new HashMap<>());
  }

  @Test
  void getPortletName() {
    assertEquals("LoginForm", loginFormPortletInstancePreferencePlugin.getPortletName());
  }

  @Test
  void generatePreferences() {
    Map<String, Preference> map = new HashMap<>();
    map.put(SETTING_NAME, new Preference(SETTING_NAME, "value", false));
    map.put(OTHER_PREF_NAME, new Preference(OTHER_PREF_NAME, "value", false));
    Portlet preferences = new Portlet(map);
    List<PortletInstancePreference> generatedPreferences = loginFormPortletInstancePreferencePlugin.generatePreferences(null, preferences, new PortletInstanceContext());
    assertNotNull(generatedPreferences);
    assertEquals(2, generatedPreferences.size());
    assertEquals(DATA_INIT_PREFERENCE_NAME, generatedPreferences.get(1).getName());
  }

  @SneakyThrows
  @Test
  void generatePreferencesWithComputeForExport() {
    Map<String, Preference> map = new HashMap<>();
    map.put(SETTING_NAME, new Preference(SETTING_NAME, "value", false));
    map.put(OTHER_PREF_NAME, new Preference(OTHER_PREF_NAME, "value", false));

    PortletInstanceContext portletInstanceContext = mock(PortletInstanceContext.class);
    when(portletInstanceContext.isExport()).thenReturn(true);

    Portlet preferences = new Portlet(map);
    List<PortletInstancePreference> generatedPreferences = loginFormPortletInstancePreferencePlugin.generatePreferences(null, preferences, portletInstanceContext);
    assertNotNull(generatedPreferences);
    assertEquals(1, generatedPreferences.size());
    assertEquals("name2", generatedPreferences.get(0).getName());
  }

}
