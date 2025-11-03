/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.plugin.renderer;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import lombok.SneakyThrows;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SidebarLoginPortletInstancePreferencePlugin extends CMSPortletInstancePreferencePlugin {

  @Override
  public String getPortletName() {
    return "SidebarLogin";

  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    return super.generatePreferences(application, preferences, portletInstanceContext);

  }

}
