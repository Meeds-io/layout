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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.meeds.layout.model.PortletInstanceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletInstancePreference;

@SpringBootTest(classes = {
  SpaceDirectoryPortletInstancePreferencePlugin.class,
})
@ExtendWith(MockitoExtension.class)
public class SpaceDirectoryPortletInstancePreferencePluginTest {

  private static final String OTHER_PREF_NAME = "name2";
  private static final String SETTING_NAME = "name";
  @Autowired
  private SpaceDirectoryPortletInstancePreferencePlugin spaceDirectoryPortletInstancePreferencePlugin;

  @Test
  void getPortletName() {
    assertEquals("SpacesList", spaceDirectoryPortletInstancePreferencePlugin.getPortletName());
  }

  @Test
  void generatePreferences() {
    Map<String, Preference> map = new HashMap<>();
    map.put(SETTING_NAME, new Preference(SETTING_NAME, "value", false));
    map.put(OTHER_PREF_NAME, new Preference(OTHER_PREF_NAME, "value", false));
    Portlet preferences = new Portlet(map);
    List<PortletInstancePreference> generatedPreferences = spaceDirectoryPortletInstancePreferencePlugin.generatePreferences(null, preferences, new PortletInstanceContext());
    assertNotNull(generatedPreferences);
    assertEquals(1, generatedPreferences.size());
    assertEquals(OTHER_PREF_NAME, generatedPreferences.get(0).getName());
  }

}
