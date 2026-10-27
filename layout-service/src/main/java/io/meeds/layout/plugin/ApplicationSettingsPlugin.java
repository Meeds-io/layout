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
package io.meeds.layout.plugin;

import java.util.Map;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.ApplicationSettingsContext;

/**
 * Makes the settings drawer of one portlet configurable by the layout tools:
 * the addon owning the portlet contributes one plugin and registers it from
 * its own {@code @PostConstruct} through
 * {@link io.meeds.layout.service.ApplicationSettingsService#addPlugin}. A
 * plugin exposes exactly the drawer's options, reads and writes them only
 * through the owning Service's permission-checked calls, as the user of the
 * context, and never trusts a window preference as a right: any editor of a
 * page can set any preference of its windows. A setting keyed by a window
 * preference (a CMS setting name, an application id) is the live window's on
 * a draft copy until the plugin re-keys it: it writes in place only when
 * {@link io.meeds.layout.service.ApplicationSettingsService#isOwner} says the
 * window owns its keys, and otherwise creates its own copy first, keyed on the
 * live page reference, then marks the window through
 * {@link io.meeds.layout.service.ApplicationSettingsService#markOwner}.
 * Refusals are the texts the agent reads: IllegalArgumentException
 * {@code value is not accepted for <option>}, ObjectNotFoundException
 * {@code does not exist: <object>}; an IllegalAccessException is answered
 * {@code not allowed: application} whatever its message.
 */
public interface ApplicationSettingsPlugin {

  /**
   * @return the portlet name, the second part of the application content id
   */
  String getPortletName();

  /**
   * @return the JSON schema of the settings object, as the read tools expose
   *         it. A null never reaches the plugin, the service dropping it as
   *         "unchanged", so an option the drawer can clear names its
   *         non-null clearing value in its description: false to hide, an
   *         empty text to remove a link or a translation.
   */
  Map<String, Object> getSettingsSchema();

  /**
   * @return whether a window placed in a site container is configurable: a
   *         setting keyed by a page reference has no page there
   */
  default boolean isConfigurableInSiteContainer() {
    return false;
  }

  /**
   * @param  context                 the window and the reading user
   * @return                         the settings as the drawer shows them,
   *                                 null when none is stored yet
   * @throws IllegalAccessException  when the user may not read them
   * @throws ObjectNotFoundException when the setting the window names does
   *                                   not exist
   */
  Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException;

  /**
   * Checks every given value against the drawer's vocabulary and the user's
   * right to change the application, writing nothing
   *
   * @param  context                 the window and the writing user
   * @param  settings                the settings to change, a key omitted
   *                                   keeping its stored value
   * @throws IllegalAccessException  when the application's own rule refuses
   *                                   the user
   * @throws ObjectNotFoundException when a referenced object does not exist
   */
  void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                   ObjectNotFoundException;

  /**
   * Writes validated settings through the owning Service, as the user
   *
   * @param  context                 the window and the writing user
   * @param  settings                the settings validated by
   *                                   {@link #validate}
   * @throws IllegalAccessException  when the owning Service refuses the user
   * @throws ObjectNotFoundException when a referenced object disappeared
   */
  void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                ObjectNotFoundException;

}
