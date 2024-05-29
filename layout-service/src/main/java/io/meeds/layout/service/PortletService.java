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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.meeds.layout.model.PortletDescriptor;
import io.meeds.layout.storage.PortletStorage;

@Service
public class PortletService {

  @Autowired
  private PortletStorage portletStorage;

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

}
