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

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The window an application settings plugin reads or writes, and who acts.
 * The preferences are the window's as stored, client-writable by any editor
 * of the page: they locate a setting, never grant a right on it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingsContext {

  /** The window's storage id, valid for one layout version */
  private long                applicationId;

  private String              contentId;

  private Map<String, String> preferences;

  /** True for a window of a site container, false for a page window */
  private boolean             siteContainer;

  /** True when the window belongs to the user's draft */
  private boolean             draft;

  /** The page holding the window, the draft page on a draft; null in a site container */
  private PageKey             pageKey;

  /** The site holding the window: the page's site, or the (draft) site */
  private SiteKey             siteKey;

  /**
   * The live page reference a CMS setting of the window hangs on, never a
   * draft's; null in a site container
   */
  private String              pageReference;

  /** The space of the live site, 0 for another site */
  private long                spaceId;

  private String              username;

}
