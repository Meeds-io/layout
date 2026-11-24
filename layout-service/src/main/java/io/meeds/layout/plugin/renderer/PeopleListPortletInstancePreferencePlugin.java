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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;

/**
 * Preferences of a People Directory window (social's {@code PeopleList}) when
 * it is read or copied through the layout API. Its group filter
 * ({@code groupIds}) stays with the window's draft and with its copies, a
 * duplicated section or site: those keep every preference unchanged. Any
 * other path (a page, section or site template, a read of the page API) gets
 * no group filter: it gets the marker {@code groupsToSet} instead when the
 * window has groups or carries the marker, so that a directory made from a
 * template lists nobody until an editor allowed to set groups sets them, and a
 * reader learns only that groups are set, never which.
 */
@Service
public class PeopleListPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  public static final String  PORTLET_NAME             = "PeopleList";

  public static final String  GROUP_FILTER_PREFERENCE  = "groupIds";

  public static final String  GROUPS_TO_SET_PREFERENCE = "groupsToSet";

  private static final String NO_GROUP_FILTER          = "[]";

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    if (preferences == null) {
      return Collections.emptyList();
    }
    List<PortletInstancePreference> windowPreferences =
                                                      StreamSupport.stream(preferences.spliterator(), false)
                                                                   .map(p -> new PortletInstancePreference(p.getName(),
                                                                                                           p.getValue()))
                                                                   .toList();
    if (isDraftOrCopy(portletInstanceContext)) {
      return windowPreferences;
    }
    boolean groupsToSet = false;
    List<PortletInstancePreference> result = new ArrayList<>();
    for (PortletInstancePreference preference : windowPreferences) {
      if (StringUtils.equals(GROUP_FILTER_PREFERENCE, preference.getName())) {
        groupsToSet |= hasGroups(preference.getValue());
      } else if (StringUtils.equals(GROUPS_TO_SET_PREFERENCE, preference.getName())) {
        groupsToSet |= Boolean.TRUE.toString().equals(StringUtils.trim(preference.getValue()));
      } else {
        result.add(preference);
      }
    }
    if (groupsToSet) {
      result.add(new PortletInstancePreference(GROUPS_TO_SET_PREFERENCE, Boolean.TRUE.toString()));
    }
    return result;
  }

  /**
   * @return whether the preferences are generated for a draft of the window,
   *         or for its copy in a duplicated section or site: both keep its
   *         group filter
   */
  private static boolean isDraftOrCopy(PortletInstanceContext portletInstanceContext) {
    Map<String, String> properties = portletInstanceContext == null ? null : portletInstanceContext.getProperties();
    return properties != null
           && (Boolean.TRUE.toString().equals(properties.get(PortletInstanceContext.DRAFT_PROPERTY))
               || Boolean.TRUE.toString().equals(properties.get(PortletInstanceContext.COPY_PROPERTY)));
  }

  /**
   * @return whether a stored group filter is set: any value but a blank one or
   *         an empty array, an unreadable one included, which lists nobody on
   *         its window
   */
  private static boolean hasGroups(String value) {
    String trimmed = StringUtils.deleteWhitespace(value);
    return StringUtils.isNotEmpty(trimmed) && !NO_GROUP_FILTER.equals(trimmed);
  }

}
