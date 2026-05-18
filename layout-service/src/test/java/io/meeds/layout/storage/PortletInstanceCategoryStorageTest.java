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

import io.meeds.layout.dao.PortletInstanceCategoryDAO;
import io.meeds.layout.entity.PortletInstanceCategoryEntity;
import io.meeds.layout.model.PortletInstanceCategory;

@SpringBootTest(classes = {
                            PortletInstanceCategoryStorage.class,
})
@ExtendWith(MockitoExtension.class)
public class PortletInstanceCategoryStorageTest {

  private static final String            ICON     = "icon";

  @MockitoBean
  private PortletInstanceCategoryDAO     portletInstanceCategoryDAO;

  @Mock
  private PortletInstanceCategoryEntity  portletInstanceCategoryEntity;

  @Mock
  private PortletInstanceCategory        portletInstanceCategory;

  @Autowired
  private PortletInstanceCategoryStorage portletInstanceCategoryStorage;

  String                                 username = "test";

  @Test
  public void getPortletInstanceCategories() {
    when(portletInstanceCategoryDAO.findAll()).thenReturn(Collections.singletonList(portletInstanceCategoryEntity));
    when(portletInstanceCategoryEntity.getId()).thenReturn(2l);
    when(portletInstanceCategoryEntity.getIcon()).thenReturn(ICON);
    when(portletInstanceCategoryEntity.isSystem()).thenReturn(true);
    when(portletInstanceCategoryEntity.getPermissions()).thenReturn(Collections.singletonList("permissions"));

    List<PortletInstanceCategory> portletInstanceCategories = portletInstanceCategoryStorage.getPortletInstanceCategories();
    assertNotNull(portletInstanceCategories);
    assertEquals(1, portletInstanceCategories.size());
    assertEquals(portletInstanceCategoryEntity.getId(), portletInstanceCategories.get(0).getId());
    assertEquals(portletInstanceCategoryEntity.getIcon(), portletInstanceCategories.get(0).getIcon());
    assertEquals(portletInstanceCategoryEntity.getPermissions(), portletInstanceCategories.get(0).getPermissions());
    assertEquals(portletInstanceCategoryEntity.isSystem(), portletInstanceCategories.get(0).isSystem());
  }

  @Test
  public void getPortletInstanceCategory() {
    when(portletInstanceCategoryDAO.findById(2l)).thenReturn(Optional.of(portletInstanceCategoryEntity));
    when(portletInstanceCategoryEntity.getId()).thenReturn(2l);
    when(portletInstanceCategoryEntity.getIcon()).thenReturn(ICON);

    PortletInstanceCategory retrievedPortletInstanceCategory = portletInstanceCategoryStorage.getPortletInstanceCategory(2l);
    assertEquals(portletInstanceCategoryEntity.getId(), retrievedPortletInstanceCategory.getId());
    assertEquals(portletInstanceCategoryEntity.getIcon(), retrievedPortletInstanceCategory.getIcon());
  }

  @Test
  public void createPortletInstanceCategory() {
    when(portletInstanceCategory.getIcon()).thenReturn(ICON);

    when(portletInstanceCategoryDAO.save(any(PortletInstanceCategoryEntity.class))).thenAnswer(invocation -> {
      PortletInstanceCategoryEntity entity = invocation.getArgument(0);
      entity.setId(2l);
      return entity;
    });
    PortletInstanceCategory createdPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.createPortletInstanceCategory(portletInstanceCategory);
    assertNotNull(createdPortletInstanceCategory);
    assertEquals(2l, createdPortletInstanceCategory.getId());
    assertEquals(portletInstanceCategory.getIcon(), createdPortletInstanceCategory.getIcon());
  }

  @Test
  public void updatePortletInstanceCategory() throws ObjectNotFoundException {
    when(portletInstanceCategory.getIcon()).thenReturn(ICON);

    when(portletInstanceCategoryDAO.save(any(PortletInstanceCategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    assertThrows(ObjectNotFoundException.class,
                 () -> portletInstanceCategoryStorage.updatePortletInstanceCategory(portletInstanceCategory));

    when(portletInstanceCategory.getId()).thenReturn(2l);
    when(portletInstanceCategory.getIcon()).thenReturn(ICON);
    assertThrows(ObjectNotFoundException.class,
                 () -> portletInstanceCategoryStorage.updatePortletInstanceCategory(portletInstanceCategory));

    when(portletInstanceCategoryDAO.existsById(portletInstanceCategory.getId())).thenReturn(true);
    PortletInstanceCategory updatedPortletInstanceCategory =
                                                           portletInstanceCategoryStorage.updatePortletInstanceCategory(portletInstanceCategory);
    assertNotNull(updatedPortletInstanceCategory);
    assertEquals(portletInstanceCategory.getId(), updatedPortletInstanceCategory.getId());
    assertEquals(portletInstanceCategory.getIcon(), updatedPortletInstanceCategory.getIcon());
  }

  @Test
  public void deletePortletInstanceCategory() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> portletInstanceCategoryStorage.deletePortletInstanceCategory(2l));
    when(portletInstanceCategoryDAO.existsById(2l)).thenReturn(true);
    portletInstanceCategoryStorage.deletePortletInstanceCategory(2l);
    verify(portletInstanceCategoryDAO, times(1)).deleteById(2l);
  }

}
