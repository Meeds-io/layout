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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.dao.PortletInstanceDAO;
import io.meeds.layout.entity.PortletInstanceEntity;
import io.meeds.layout.model.PortletInstance;

@SpringBootTest(classes = {
                            PortletInstanceStorage.class,
})
@ExtendWith(MockitoExtension.class)
public class PortletInstanceStorageTest {

  private static final String    CONTENT_ID = "test/portlet";

  @MockitoBean
  private PortletInstanceDAO     portletInstanceDAO;

  @MockitoBean
  private PortletStorage         portletStorage;

  @Mock
  private PortletInstanceEntity  portletInstanceEntity;

  @Mock
  private PortletInstance        portletInstance;

  @Autowired
  private PortletInstanceStorage portletInstanceStorage;

  String                         username   = "test";

  @Test
  public void getPortletInstances() {
    when(portletInstanceDAO.findAll()).thenReturn(Collections.singletonList(portletInstanceEntity));
    when(portletInstanceEntity.getId()).thenReturn(2l);
    when(portletInstanceEntity.getContentId()).thenReturn(CONTENT_ID);
    when(portletInstanceEntity.isSystem()).thenReturn(true);
    when(portletInstanceEntity.getPermissions()).thenReturn(Collections.singletonList("permissions"));

    List<PortletInstance> portletInstances = portletInstanceStorage.getPortletInstances();
    assertNotNull(portletInstances);
    assertEquals(1, portletInstances.size());
    assertEquals(portletInstanceEntity.getId(), portletInstances.get(0).getId());
    assertEquals(portletInstanceEntity.getContentId(), portletInstances.get(0).getContentId());
    assertEquals(portletInstanceEntity.getPermissions(), portletInstances.get(0).getPermissions());
    assertEquals(portletInstanceEntity.isSystem(), portletInstances.get(0).isSystem());
  }

  @Test
  public void getPortletInstance() {
    when(portletInstanceDAO.findById(2l)).thenReturn(Optional.of(portletInstanceEntity));
    when(portletInstanceEntity.getId()).thenReturn(2l);
    when(portletInstanceEntity.getContentId()).thenReturn(CONTENT_ID);

    PortletInstance retrievedPortletInstance = portletInstanceStorage.getPortletInstance(2l);
    assertEquals(portletInstanceEntity.getId(), retrievedPortletInstance.getId());
    assertEquals(portletInstanceEntity.getContentId(), retrievedPortletInstance.getContentId());
  }

  @Test
  public void createPortletInstance() {
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);

    when(portletInstanceDAO.save(any(PortletInstanceEntity.class))).thenAnswer(invocation -> {
      PortletInstanceEntity entity = invocation.getArgument(0);
      entity.setId(2l);
      return entity;
    });
    PortletInstance createdPortletInstance =
                                           portletInstanceStorage.createPortletInstance(portletInstance);
    assertNotNull(createdPortletInstance);
    assertEquals(2l, createdPortletInstance.getId());
    assertEquals(portletInstance.getContentId(), createdPortletInstance.getContentId());
  }

  @Test
  public void updatePortletInstance() throws ObjectNotFoundException {
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);

    when(portletInstanceDAO.save(any(PortletInstanceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    assertThrows(ObjectNotFoundException.class,
                 () -> portletInstanceStorage.updatePortletInstance(portletInstance));

    when(portletInstance.getId()).thenReturn(2l);
    when(portletInstance.getContentId()).thenReturn(CONTENT_ID);
    assertThrows(ObjectNotFoundException.class,
                 () -> portletInstanceStorage.updatePortletInstance(portletInstance));

    when(portletInstanceDAO.existsById(portletInstance.getId())).thenReturn(true);
    PortletInstance updatedPortletInstance =
                                           portletInstanceStorage.updatePortletInstance(portletInstance);
    assertNotNull(updatedPortletInstance);
    assertEquals(portletInstance.getId(), updatedPortletInstance.getId());
    assertEquals(portletInstance.getContentId(), updatedPortletInstance.getContentId());
  }

  @Test
  public void deletePortletInstance() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> portletInstanceStorage.deletePortletInstance(2l));
    when(portletInstanceDAO.existsById(2l)).thenReturn(true);
    portletInstanceStorage.deletePortletInstance(2l);
    verify(portletInstanceDAO, times(1)).deleteById(2l);
  }

}
