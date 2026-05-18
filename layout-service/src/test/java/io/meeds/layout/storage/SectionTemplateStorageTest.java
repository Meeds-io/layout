/**
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

import io.meeds.layout.dao.SectionTemplateDAO;
import io.meeds.layout.entity.SectionTemplateEntity;
import io.meeds.layout.model.SectionTemplate;

@SpringBootTest(classes = {
  SectionTemplateStorage.class,
})
@ExtendWith(MockitoExtension.class)
public class SectionTemplateStorageTest {

  private static final String    CONTENT  = "{}";

  @MockitoBean
  private SectionTemplateDAO     sectionTemplateDAO;

  @MockitoBean
  private PortletStorage         portletStorage;

  @Mock
  private SectionTemplateEntity  sectionTemplateEntity;

  @Mock
  private SectionTemplate        sectionTemplate;

  @Autowired
  private SectionTemplateStorage sectionTemplateStorage;

  String                         username = "test";

  @Test
  public void getSectionTemplates() {
    when(sectionTemplateDAO.findAll()).thenReturn(Collections.singletonList(sectionTemplateEntity));
    when(sectionTemplateEntity.getId()).thenReturn(2l);
    when(sectionTemplateEntity.getContent()).thenReturn(CONTENT);
    when(sectionTemplateEntity.isSystem()).thenReturn(true);

    List<SectionTemplate> sectionTemplates = sectionTemplateStorage.getSectionTemplates();
    assertNotNull(sectionTemplates);
    assertEquals(1, sectionTemplates.size());
    assertEquals(sectionTemplateEntity.getId(), sectionTemplates.get(0).getId());
    assertEquals(sectionTemplateEntity.getContent(), sectionTemplates.get(0).getContent());
    assertEquals(sectionTemplateEntity.isSystem(), sectionTemplates.get(0).isSystem());
  }

  @Test
  public void getSectionTemplate() {
    when(sectionTemplateDAO.findById(2l)).thenReturn(Optional.of(sectionTemplateEntity));
    when(sectionTemplateEntity.getId()).thenReturn(2l);
    when(sectionTemplateEntity.getContent()).thenReturn(CONTENT);

    SectionTemplate retrievedSectionTemplate = sectionTemplateStorage.getSectionTemplate(2l);
    assertEquals(sectionTemplateEntity.getId(), retrievedSectionTemplate.getId());
    assertEquals(sectionTemplateEntity.getContent(), retrievedSectionTemplate.getContent());
  }

  @Test
  public void createSectionTemplate() {
    when(sectionTemplate.getContent()).thenReturn(CONTENT);

    when(sectionTemplateDAO.save(any(SectionTemplateEntity.class))).thenAnswer(invocation -> {
      SectionTemplateEntity entity = invocation.getArgument(0);
      entity.setId(2l);
      return entity;
    });
    SectionTemplate createdSectionTemplate = sectionTemplateStorage.createSectionTemplate(sectionTemplate);
    assertNotNull(createdSectionTemplate);
    assertEquals(2l, createdSectionTemplate.getId());
    assertEquals(sectionTemplate.getContent(), createdSectionTemplate.getContent());
  }

  @Test
  public void updateSectionTemplate() throws ObjectNotFoundException {
    when(sectionTemplate.getContent()).thenReturn(CONTENT);

    when(sectionTemplateDAO.save(any(SectionTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    assertThrows(ObjectNotFoundException.class,
                 () -> sectionTemplateStorage.updateSectionTemplate(sectionTemplate));

    when(sectionTemplate.getId()).thenReturn(2l);
    when(sectionTemplate.getContent()).thenReturn(CONTENT);
    assertThrows(ObjectNotFoundException.class,
                 () -> sectionTemplateStorage.updateSectionTemplate(sectionTemplate));

    when(sectionTemplateDAO.existsById(sectionTemplate.getId())).thenReturn(true);
    SectionTemplate updatedSectionTemplate =
                                           sectionTemplateStorage.updateSectionTemplate(sectionTemplate);
    assertNotNull(updatedSectionTemplate);
    assertEquals(sectionTemplate.getId(), updatedSectionTemplate.getId());
    assertEquals(sectionTemplate.getContent(), updatedSectionTemplate.getContent());
  }

  @Test
  public void deleteSectionTemplate() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> sectionTemplateStorage.deleteSectionTemplate(2l));
    when(sectionTemplateDAO.existsById(2l)).thenReturn(true);
    sectionTemplateStorage.deleteSectionTemplate(2l);
    verify(sectionTemplateDAO, times(1)).deleteById(2l);
  }

}
