/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.plugin.DatabindPreferencePlugin;
import io.meeds.social.databind.service.DatabindService;
import jakarta.annotation.PostConstruct;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortletInstanceDatabindPreferencePlugin implements DatabindPreferencePlugin {

  private static final Log       LOG = ExoLogger.getExoLogger(PortletInstanceDatabindPreferencePlugin.class);

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private DatabindService        databindService;

  @Override
  public String getDataType() {
    return "PortletInstance";
  }

  @PostConstruct
  public void init() {
    databindService.addDataPreferencePlugin(this);
  }

  @Override
  public void serialize(String objectId, File zipFile, String username) throws ObjectNotFoundException, IllegalAccessException {
    List<PortletInstancePreference> preferences = portletInstanceService.getPortletInstancePreferences(Long.parseLong(objectId),
                                                                                                       username);
    String jsonData = JsonUtils.toJsonString(preferences);
    try (FileWriter writer = new FileWriter(zipFile)) {
      writer.write(jsonData);
    } catch (IOException e) {
      LOG.warn("Fail to serialize portlet instance with id", objectId, e);
    }
  }
}
