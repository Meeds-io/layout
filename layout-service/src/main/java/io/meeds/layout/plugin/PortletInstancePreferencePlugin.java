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
package io.meeds.layout.plugin;

import java.util.List;

import io.meeds.layout.model.PortletInstanceContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletInstancePreference;

/**
 * A plugin that can be extended in order to inject a specific behavior for some
 * portlets to let export preferences that allow to duplicate behavior from one
 * application instance to another.
 */
public interface PortletInstancePreferencePlugin {

  /**
   * return the portlet name for which the plugin can generate its preferences
   */
  String getPortletName();

  /**
   * Computes the list of preferences to have the same view as the designated
   * application instance
   * 
   * @param application {@link Application} designated to extract its
   *          preferences
   * @param preferences current {@link Portlet} preferences
   * @param portletInstanceContext context preference generate global parm
   * @return {@link List} of {@link PortletInstancePreference}
   */
  List<PortletInstancePreference> generatePreferences(Application application, Portlet preferences, PortletInstanceContext portletInstanceContext);

}
