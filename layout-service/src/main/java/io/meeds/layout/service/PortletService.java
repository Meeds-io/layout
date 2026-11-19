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
package io.meeds.layout.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletDescriptor;
import io.meeds.layout.storage.PortletStorage;

@Service
public class PortletService {

  private PortletStorage portletStorage;

  public PortletService(PortletStorage portletStorage) {
    this.portletStorage = portletStorage;
  }

  /**
   * @return a {@link List} of {@link PortletDescriptor}
   */
  public List<PortletDescriptor> getPortlets() {
    return portletStorage.getPortletDescriptors();
  }

  /**
   * @param id
   * @return {@link PortletDescriptor} corresponding to contentId or portletName
   */
  public PortletDescriptor getPortlet(String id) {
    return portletStorage.getPortletDescriptor(id);
  }

  /**
   * @param id a portlet's contentId or name
   * @return the preference names only the portlet itself writes, empty when
   *         it declares none or is unknown
   */
  public Set<String> getProtectedPreferences(String id) {
    PortletDescriptor portlet = id == null ? null : getPortlet(id);
    return portlet == null || CollectionUtils.isEmpty(portlet.getProtectedPreferences()) ? Collections.emptySet()
                                                                                       : Set.copyOf(portlet.getProtectedPreferences());
  }

  /**
   * Removes the protected preferences from every window of a client-built
   * layout that carries its own preferences, a window to create: the window
   * then gets none of them. A window sent by storage id keeps its stored
   * preferences, whatever the client sends.
   *
   * @param children the layout's children, walked recursively
   */
  public void stripProtectedPreferences(Collection<ModelObject> children) {
    if (CollectionUtils.isEmpty(children)) {
      return;
    }
    for (ModelObject child : children) {
      if (child instanceof Container container) {
        stripProtectedPreferences(container.getChildren());
      } else if (child instanceof Application application
                 && application.getState() instanceof TransientApplicationState state
                 && state.getContentState() instanceof Portlet preferences) {
        Set<String> protectedPreferences = getProtectedPreferences(state.getContentId());
        if (!protectedPreferences.isEmpty()) {
          Map<String, Preference> kept = new LinkedHashMap<>();
          for (Preference preference : preferences) {
            if (!protectedPreferences.contains(preference.getName())) {
              kept.put(preference.getName(), preference);
            }
          }
          state.setContentState(new Portlet(kept));
        }
      }
    }
  }

}
