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
package io.meeds.layout.plugin.upgrade;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.dao.WindowDAO;

import io.meeds.layout.model.ApplicationReferenceUpgrade;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class LayoutApplicationReferenceUpgradePluginTest {

  @Mock
  private SettingService              settingService;

  @Mock
  private PortletInstanceService      portletInstanceService;

  @Mock
  private WindowDAO                   windowDAO;

  @Mock
  private InitParams                  initParams;

  @Mock
  private ApplicationReferenceUpgrade applicationModification;

  @Mock
  private PortletInstance             portletInstance;

  private String                      oldContentId = "oldApp/oldContentId";

  private String                      newContentId = "newApp/newContentId";

  @Test
  @SneakyThrows
  public void processUpgradeWithModification() {
    when(initParams.getObjectParamValues(ApplicationReferenceUpgrade.class)).thenReturn(Collections.singletonList(applicationModification));
    when(portletInstanceService.getPortletInstances()).thenReturn(Collections.singletonList(portletInstance));
    when(portletInstance.getContentId()).thenReturn(oldContentId);
    when(applicationModification.isModification()).thenReturn(true);
    when(applicationModification.getOldContentId()).thenReturn(oldContentId);
    when(applicationModification.getNewContentId()).thenReturn(newContentId);

    LayoutApplicationReferenceUpgradePlugin applicationReferenceUpgradePlugin =
                                                                              new LayoutApplicationReferenceUpgradePlugin(settingService,
                                                                                                                          portletInstanceService,
                                                                                                                          windowDAO,
                                                                                                                          initParams);
    assertTrue(applicationReferenceUpgradePlugin.shouldProceedToUpgrade(null, null));

    applicationReferenceUpgradePlugin.processUpgrade(null, null);

    verify(windowDAO).updateContentId(oldContentId, newContentId);
    verify(portletInstanceService).updatePortletInstance(portletInstance);
  }

  @Test
  @SneakyThrows
  public void processUpgradeWithRemoval() {
    when(initParams.getObjectParamValues(ApplicationReferenceUpgrade.class)).thenReturn(Collections.singletonList(applicationModification));
    when(portletInstanceService.getPortletInstances()).thenReturn(Collections.singletonList(portletInstance));
    when(portletInstance.getContentId()).thenReturn(oldContentId);
    when(applicationModification.isRemoval()).thenReturn(true);
    when(applicationModification.getOldContentId()).thenReturn(oldContentId);
    when(applicationModification.getNewContentId()).thenReturn(newContentId);
    when(portletInstance.getId()).thenReturn(2l);

    LayoutApplicationReferenceUpgradePlugin applicationReferenceUpgradePlugin =
                                                                              new LayoutApplicationReferenceUpgradePlugin(settingService,
                                                                                                                          portletInstanceService,
                                                                                                                          windowDAO,
                                                                                                                          initParams);
    assertTrue(applicationReferenceUpgradePlugin.shouldProceedToUpgrade(null, null));

    applicationReferenceUpgradePlugin.processUpgrade(null, null);

    verify(windowDAO).deleteByContentId(oldContentId);
    verify(portletInstanceService).deletePortletInstance(2l);
  }

}
