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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.gatein.common.i18n.LocalizedString;
import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.info.CapabilitiesInfo;
import org.gatein.pc.api.info.MetaInfo;
import org.gatein.pc.api.info.ModeInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.api.info.PreferencesInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.meeds.layout.model.PortletDescriptor;

import lombok.SneakyThrows;

@SpringBootTest(classes = { PortletStorage.class, })
@ExtendWith(MockitoExtension.class)
public class PortletStorageTest {

  private static final String MODE             = "edit";

  private static final String NAME             = "name";

  private static final String APPLICATION_NAME = "test";

  private static final String PORTLET_NAME     = "portlet";

  @MockitoBean
  private PortletInvoker      portletInvoker;

  @Mock
  private PortletInfo         portletInfo;

  @Mock
  private MetaInfo            metaInfo;

  @Mock
  private Portlet             portlet;

  @Mock
  private CapabilitiesInfo    capabilitiesInfo;

  @Mock
  private ModeInfo            modeInfo;

  @Mock
  private PreferencesInfo     preferencesInfo;

  @Mock
  private LocalizedString     nameLS;

  @Mock
  private LocalizedString     descriptionLS;

  @Autowired
  private PortletStorage      portletStorage;

  @BeforeEach
  public void init() {
    portletStorage.setPortletDescriptors(null);
  }

  @Test
  @SneakyThrows
  public void getPortletDescriptors() {
    when(portletInvoker.getPortlets()).thenReturn(Collections.singleton(portlet));
    when(portlet.getInfo()).thenReturn(portletInfo);
    when(portletInfo.getApplicationName()).thenReturn(APPLICATION_NAME);
    when(portletInfo.getName()).thenReturn(PORTLET_NAME);
    when(portletInfo.getMeta()).thenReturn(metaInfo);
    when(portletInfo.getCapabilities()).thenReturn(capabilitiesInfo);
    when(capabilitiesInfo.getModes(org.gatein.common.net.media.MediaType.create("text/html"))).thenReturn(Collections.singleton(modeInfo));
    when(modeInfo.getModeName()).thenReturn(MODE);
    when(metaInfo.getMetaValue(MetaInfo.DISPLAY_NAME)).thenReturn(nameLS);
    when(metaInfo.getMetaValue(MetaInfo.DESCRIPTION)).thenReturn(descriptionLS);
    when(nameLS.getDefaultString()).thenReturn(NAME);

    List<PortletDescriptor> portletDescriptors = portletStorage.getPortletDescriptors();
    assertNotNull(portletDescriptors);
    assertEquals(1, portletDescriptors.size());

    PortletDescriptor portletDescriptor = portletDescriptors.get(0);
    assertEquals(APPLICATION_NAME, portletDescriptor.getApplicationName());
    assertEquals(PORTLET_NAME, portletDescriptor.getPortletName());
    assertEquals(APPLICATION_NAME + "/" + PORTLET_NAME, portletDescriptor.getContentId());
    assertEquals(PORTLET_NAME, portletDescriptor.getDescription());
    assertEquals(NAME, portletDescriptor.getName());
    assertEquals(Collections.singletonList(MODE), portletDescriptor.getSupportedModes());
  }

  @Test
  @SneakyThrows
  public void getPortletDescriptor() {
    when(portletInvoker.getPortlets()).thenReturn(Collections.singleton(portlet));
    when(portlet.getInfo()).thenReturn(portletInfo);
    when(portletInfo.getApplicationName()).thenReturn(APPLICATION_NAME);
    when(portletInfo.getName()).thenReturn(PORTLET_NAME);
    when(portletInfo.getMeta()).thenReturn(metaInfo);
    when(portletInfo.getCapabilities()).thenReturn(capabilitiesInfo);
    when(capabilitiesInfo.getModes(org.gatein.common.net.media.MediaType.create("text/html"))).thenReturn(Collections.singleton(modeInfo));
    when(modeInfo.getModeName()).thenReturn(MODE);
    when(metaInfo.getMetaValue(MetaInfo.DISPLAY_NAME)).thenReturn(nameLS);
    when(metaInfo.getMetaValue(MetaInfo.DESCRIPTION)).thenReturn(descriptionLS);
    when(nameLS.getDefaultString()).thenReturn(NAME);

    PortletDescriptor portletDescriptor = portletStorage.getPortletDescriptor(PORTLET_NAME);
    assertEquals(APPLICATION_NAME, portletDescriptor.getApplicationName());
    assertEquals(PORTLET_NAME, portletDescriptor.getPortletName());
    assertEquals(APPLICATION_NAME + "/" + PORTLET_NAME, portletDescriptor.getContentId());
    assertEquals(PORTLET_NAME, portletDescriptor.getDescription());
    assertEquals(NAME, portletDescriptor.getName());
    assertEquals(Collections.singletonList(MODE), portletDescriptor.getSupportedModes());

    when(portlet.isRemote()).thenReturn(true);
    portletStorage.setPortletDescriptors(null);
    assertNull(portletStorage.getPortletDescriptor(PORTLET_NAME));
  }
}
