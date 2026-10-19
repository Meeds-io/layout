/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.mcp;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.mcp.constant.ThemePart;
import io.meeds.layout.mcp.model.NavigationDisplayDescription;
import io.meeds.layout.mcp.model.SidebarDisplayOptions;
import io.meeds.layout.mcp.model.ThemeDescription;
import io.meeds.layout.mcp.model.TopbarDisplayOptions;
import io.meeds.layout.service.NavigationDisplayService;
import io.meeds.layout.service.ThemeUpdateService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The write tools of the Branding and Theme administration and of the
 * navigation settings: every public method is an MCP tool described in
 * ai-tool-definitions.json, applied as the current user to every user at
 * once, after the platform's own approval step for a caller whose token
 * carries it. A tool maps its arguments and calls the Services, whose
 * refusals carry the fixed texts the agent reads: {@code does not exist:
 * <object>}, {@code not allowed: administrators only}, {@code value is not
 * accepted for <option>}.
 */
@Service
@Profile("mcp-server")
public class BrandingMcpTool implements McpToolPlugin {

  @Autowired
  private ThemeUpdateService       themeUpdateService;

  @Autowired
  private NavigationDisplayService navigationDisplayService;

  /**
   * Updates one part of the theme, colors, font, topbar, sidebars, drawers or
   * page_and_apps, with the options its drawer offers: the named options are
   * set, the reset ones return to their default, every other keeps its
   * value. Applies at once to every user, as the Apply button does; not a
   * draft. A page field is refused outside the Page drawer's vocabulary, a
   * theme variable by the branding service. Returns the part as stored.
   */
  public ThemeDescription updateTheme(String part, Map<String, String> options, List<String> reset) throws IllegalAccessException {
    ThemePart themePart = ThemePart.parse(part);
    return new ThemeDescription(themePart, themeUpdateService.updateTheme(themePart, options, reset, getCurrentUserName()));
  }

  /**
   * Updates what the topbar shows and how the sidebar may be displayed: a
   * field omitted keeps its value, the applications listed are merged by
   * identifier or by the name shown in the user's language, the allowed
   * modes replace the list. Applies at once to every user; not a draft.
   * Returns both parts as stored.
   */
  public NavigationDisplayDescription updateNavigationDisplay(TopbarDisplayOptions topbar,
                                                              SidebarDisplayOptions sidebar) throws ObjectNotFoundException,
                                                                                            IllegalAccessException {
    return navigationDisplayService.updateNavigationDisplay(topbar, sidebar, getCurrentUserName(), getCurrentUserLocale());
  }
}
