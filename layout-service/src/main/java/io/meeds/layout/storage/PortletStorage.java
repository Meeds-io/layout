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
package io.meeds.layout.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.i18n.LocalizedString;
import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.info.MetaInfo;
import org.gatein.pc.api.info.ModeInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.portlet.impl.info.ContainerPortletInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;

import io.meeds.layout.model.PortletDescriptor;

import lombok.Setter;
import lombok.SneakyThrows;

@Component
public class PortletStorage {

  /**
   * The portlet.xml init-param naming, comma-separated, the preferences only
   * the portlet itself writes.
   */
  public static final String      PROTECTED_PREFERENCES_PARAM = "layout-protected-preferences";

  // Can't be injected using @Autowired since it's injected by code
  // in org.exoplatform.portal.pc.ExoKernelIntegration
  @Autowired(required = false)
  private PortletInvoker          portletInvoker;

  @Setter
  private List<PortletDescriptor> portletDescriptors;

  public List<PortletDescriptor> getPortletDescriptors() {
    if (portletDescriptors == null) {
      portletDescriptors = Collections.unmodifiableList(getPortlets().stream()
                                                                     .map(this::toPortletDescriptor)
                                                                     .filter(Objects::nonNull)
                                                                     .toList());
    }
    return portletDescriptors;
  }

  public PortletDescriptor getPortletDescriptor(String id) {
    return getPortletDescriptors().stream()
                                  .filter(p -> StringUtils.equals(p.getContentId(), id)
                                               || StringUtils.equals(p.getPortletName(), id))
                                  .findFirst()
                                  .orElse(null);
  }

  private PortletDescriptor toPortletDescriptor(Portlet portlet) {
    if (portlet.isRemote()) {
      return null;
    }
    PortletInfo info = portlet.getInfo();
    MetaInfo metaInfo = info.getMeta();
    Set<ModeInfo> allModes = info.getCapabilities()
                                 .getModes(org.gatein.common.net.media.MediaType.create("text/html"));
    LocalizedString descriptionLS = metaInfo.getMetaValue(MetaInfo.DESCRIPTION);
    LocalizedString nameLS = metaInfo.getMetaValue(MetaInfo.DISPLAY_NAME);

    PortletDescriptor portletDescriptor = new PortletDescriptor();
    portletDescriptor.setApplicationName(info.getApplicationName());
    portletDescriptor.setPortletName(info.getName());
    portletDescriptor.setName(getLocalizedStringOrDefault(nameLS, info.getName()));
    portletDescriptor.setDescription(getLocalizedStringOrDefault(descriptionLS, info.getName()));
    portletDescriptor.setSupportedModes(allModes.stream()
                                        .map(m -> m.getModeName())
                                        .toList());
    if(info instanceof ContainerPortletInfo containerPortletInfo) {
      portletDescriptor.setEditable(StringUtils.equals("true", containerPortletInfo.getInitParameter("layout-content-editable")));
      portletDescriptor.setProtectedPreferences(Arrays.stream(StringUtils.split(StringUtils.defaultString(containerPortletInfo.getInitParameter(PROTECTED_PREFERENCES_PARAM)),
                                                                                ','))
                                                      .map(StringUtils::trim)
                                                      .filter(StringUtils::isNotBlank)
                                                      .distinct()
                                                      .toList());
    }
    return portletDescriptor;
  }

  private String getLocalizedStringOrDefault(LocalizedString localizedString, String portletName) {
    if (localizedString == null || localizedString.getDefaultString() == null) {
      return portletName;
    } else {
      return localizedString.getDefaultString();
    }
  }

  @SneakyThrows
  private Set<Portlet> getPortlets() {
    return getPortletInvoker().getPortlets();
  }

  private PortletInvoker getPortletInvoker() {
    if (portletInvoker == null) {
      portletInvoker = ExoContainerContext.getService(PortletInvoker.class);
    }
    return portletInvoker;
  }

}
