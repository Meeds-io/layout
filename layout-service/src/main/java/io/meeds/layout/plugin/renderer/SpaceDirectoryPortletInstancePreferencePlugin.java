/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import io.meeds.layout.model.PortletInstanceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;

import lombok.SneakyThrows;

@Service
public class SpaceDirectoryPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String SETTING_PREFERENCE_NAME = "name";

  @Override
  public String getPortletName() {
    return "SpacesList";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application, Portlet preferences, PortletInstanceContext portletInstanceContext) {
    if (preferences == null) {
      return Collections.emptyList();
    }
    return StreamSupport.stream(preferences.spliterator(), false)
                          // remove generated setting name for the original
                          // instance to avoid duplicating instances using the
                          // same setting name
                          .filter(p -> !StringUtils.equals(SETTING_PREFERENCE_NAME, p.getName()))
                          .map(p -> new PortletInstancePreference(p.getName(), p.getValue()))
                          .toList();
    
  }

}
