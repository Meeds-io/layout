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
package io.meeds.layout.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationCodePlugin;

/**
 * The registry of the {@link ApplicationCodePlugin}s, keyed by portlet
 * name, and the read the layout descriptions make through them: a window
 * whose portlet has no plugin has no code to describe
 */
@Service
public class ApplicationCodeService {

  private static final Log                         LOG     = ExoLogger.getLogger(ApplicationCodeService.class);

  private final Map<String, ApplicationCodePlugin> plugins = new ConcurrentHashMap<>();

  /**
   * Registers a plugin, replacing the one registered for the same portlet
   *
   * @param plugin the plugin
   */
  public void addPlugin(ApplicationCodePlugin plugin) {
    plugins.put(plugin.getPortletName(), plugin);
  }

  /**
   * @param portletName the portlet name whose plugin leaves
   */
  public void removePlugin(String portletName) {
    plugins.remove(portletName);
  }

  /**
   * @param contentId the window's content id ({@code application/portlet})
   * @return whether a plugin reads the code of that portlet
   */
  public boolean hasPlugin(String contentId) {
    return getPlugin(contentId) != null;
  }

  /**
   * @param contentId the window's content id
   * @param context the window's context, for the plugin
   * @return the code as the plugin reads it for the context's user; null
   *         when the portlet has no plugin, when the window has no code,
   *         when the user may not read it or when the plugin fails
   */
  public ApplicationCode read(String contentId, ApplicationSettingsContext context) {
    ApplicationCodePlugin plugin = getPlugin(contentId);
    if (plugin == null) {
      return null;
    }
    try {
      return plugin.read(context);
    } catch (Exception e) {
      LOG.debug("Code of application {} could not be read for {}", context.getApplicationId(), context.getUsername(), e);
      return null;
    }
  }

  private ApplicationCodePlugin getPlugin(String contentId) {
    String portletName = StringUtils.substringAfter(StringUtils.defaultString(contentId), "/");
    return StringUtils.isBlank(portletName) ? null : plugins.get(portletName);
  }

}
