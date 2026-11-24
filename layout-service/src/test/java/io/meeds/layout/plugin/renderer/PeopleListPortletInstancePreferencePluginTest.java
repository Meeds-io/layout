/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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

import static io.meeds.layout.plugin.renderer.PeopleListPortletInstancePreferencePlugin.GROUPS_TO_SET_PREFERENCE;
import static io.meeds.layout.plugin.renderer.PeopleListPortletInstancePreferencePlugin.GROUP_FILTER_PREFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;

class PeopleListPortletInstancePreferencePluginTest {

  private static final String                               GROUPS     = "[{\"groupId\":\"/spaces/hidden\",\"spaceId\":\"3\"}]";

  private static final PortletInstanceContext                COPY       =
                                                                       new PortletInstanceContext(false,
                                                                                                  Map.of(PortletInstanceContext.COPY_PROPERTY,
                                                                                                         "true"));

  private static final PortletInstanceContext                DRAFT      =
                                                                       new PortletInstanceContext(false,
                                                                                                  Map.of(PortletInstanceContext.DRAFT_PROPERTY,
                                                                                                         "true"));

  private final PeopleListPortletInstancePreferencePlugin plugin = new PeopleListPortletInstancePreferencePlugin();

  /**
   * The literal the plugin is keyed on is the {@code <portlet-name>} social's
   * {@code webapp/src/main/webapp/WEB-INF/portlet.xml} declares for the People
   * Directory: a plugin keyed on another name is never found.
   */
  @Test
  void isKeyedOnThePeopleDirectoryPortletName() {
    assertEquals("PeopleList", plugin.getPortletName());
  }

  @Test
  void aCopyOrADraftKeepsEveryPreferenceTheGroupFilterIncluded() {
    Portlet preferences = preferences("pageSize", "20", GROUP_FILTER_PREFERENCE, GROUPS, GROUPS_TO_SET_PREFERENCE, "true");

    for (PortletInstanceContext context : List.of(COPY, DRAFT)) {
      assertEquals(Map.of("pageSize", "20", GROUP_FILTER_PREFERENCE, GROUPS, GROUPS_TO_SET_PREFERENCE, "true"),
                   generate(preferences, context));
    }
  }

  @Test
  void aTemplateOrAReadGetsTheMarkerInsteadOfTheGroups() {
    Portlet preferences = preferences("pageSize", "20", "attributeFilter", "{\"city\":[\"Tunis\"]}", GROUP_FILTER_PREFERENCE, GROUPS);

    for (PortletInstanceContext context : Arrays.asList(null,
                                                        new PortletInstanceContext(false, null),
                                                        new PortletInstanceContext(true, null),
                                                        new PortletInstanceContext(false,
                                                                                   Map.of(PortletInstanceContext.COPY_PROPERTY,
                                                                                          "false")))) {
      assertEquals(Map.of("pageSize", "20", "attributeFilter", "{\"city\":[\"Tunis\"]}", GROUPS_TO_SET_PREFERENCE, "true"),
                   generate(preferences, context));
    }
  }

  @Test
  void aWindowWithNoGroupFilterGetsNoMarker() {
    for (String noGroups : Arrays.asList("[]", " [ ] ", "", null)) {
      assertEquals(Map.of("pageSize", "20"),
                   generate(preferences("pageSize", "20", GROUP_FILTER_PREFERENCE, noGroups), null),
                   String.valueOf(noGroups));
    }
  }

  @Test
  void anUnreadableGroupFilterListsNobodySoItsTemplateGetsTheMarker() {
    assertEquals(Map.of(GROUPS_TO_SET_PREFERENCE, "true"),
                 generate(preferences(GROUP_FILTER_PREFERENCE, "not json"), null));
  }

  @Test
  void aWindowThatCarriesTheMarkerPassesItOnToItsTemplate() {
    assertEquals(Map.of(GROUPS_TO_SET_PREFERENCE, "true"),
                 generate(preferences(GROUPS_TO_SET_PREFERENCE, "true"), null));
    // any other value is no marker: a page editor can store one
    assertEquals(Map.of(), generate(preferences(GROUPS_TO_SET_PREFERENCE, "false"), null));
  }

  @Test
  void noPreferencesGiveNone() {
    assertTrue(plugin.generatePreferences(null, null, null).isEmpty());
  }

  private Map<String, String> generate(Portlet preferences, PortletInstanceContext context) {
    return plugin.generatePreferences(null, preferences, context)
                 .stream()
                 .collect(Collectors.toMap(PortletInstancePreference::getName, PortletInstancePreference::getValue));
  }

  private static Portlet preferences(String... namesAndValues) {
    Map<String, Preference> map = new HashMap<>();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      map.put(namesAndValues[i], new Preference(namesAndValues[i], namesAndValues[i + 1], false));
    }
    return new Portlet(map);
  }

}
