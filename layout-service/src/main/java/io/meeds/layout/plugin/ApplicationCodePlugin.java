/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.plugin;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.ApplicationCodeService;

/**
 * The read of the code a placed application runs in the visitor's browser,
 * contributed by the add-on that owns the portlet: the layout reads answer
 * it as the window's widget code, where the add-on writes it through its
 * own tool. The owning add-on registers its plugin from its own
 * {@code @PostConstruct} through {@link ApplicationCodeService#addPlugin},
 * as the application settings plugins do; the plugin reads through the
 * owning Service, as the context's user, and never from a window preference
 * taken as a right.
 */
public interface ApplicationCodePlugin {

  /**
   * @return the portlet name the plugin reads for, as the portlet
   *         descriptor declares it ({@code WidgetPortlet})
   */
  String getPortletName();

  /**
   * @param context the window: its storage id, content id, preferences,
   *          layout and user
   * @return the code the window runs, null when the window has none yet
   * @throws ObjectNotFoundException when the code the window names no
   *           longer exists
   * @throws IllegalAccessException when the context's user may not read it
   */
  ApplicationCode read(ApplicationSettingsContext context) throws ObjectNotFoundException, IllegalAccessException;

  /**
   * @return a new window of the application, placed when no catalog instance
   *         carries its content id: its content id, name, description and
   *         preferences, a new object on each call, each window placed from
   *         it running code of its own; null when the application is placed
   *         from the catalog only
   */
  default PortletInstance newApplication() {
    return null;
  }

}
