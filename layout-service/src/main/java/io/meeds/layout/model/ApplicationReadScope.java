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
package io.meeds.layout.model;

import java.util.Map;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;

/**
 * What a layout description needs to describe the applications of a tree:
 * their content ids, the windows by storage id, and where they sit and for
 * whom, to read their settings; without a username no settings are read.
 */
public record ApplicationReadScope(Map<String, String> contentIds,
                                   Map<String, Application> windows,
                                   PageKey pageKey,
                                   SiteKey siteKey,
                                   SiteKey liveSiteKey,
                                   String pageReference,
                                   boolean draft,
                                   String username) {
}
