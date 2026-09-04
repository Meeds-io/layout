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

import io.meeds.layout.dao.PageTemplateDAO;
import io.meeds.layout.entity.PageTemplateEntity;
import io.meeds.layout.model.PageTemplate;

@SpringBootTest(classes = {
  PageTemplateStorage.class,
})
@ExtendWith(MockitoExtension.class)
public class PageTemplateStorageTest {

  private static final String LAYOUT_CONTENT = "...layout...";

  @MockitoBean
  private PageTemplateDAO     pageTemplateDAO;

  @Mock
  private PageTemplateEntity  pageTemplateEntity;

  @Mock
  private PageTemplate        pageTemplate;

  @Autowired
  private PageTemplateStorage pageTemplateStorage;

  String                      username       = "test";

  @Test
  public void getPageTemplates() {
    when(pageTemplateDAO.findAll()).thenReturn(Collections.singletonList(pageTemplateEntity));
    when(pageTemplateEntity.getId()).thenReturn(2l);
    when(pageTemplateEntity.getContent()).thenReturn(LAYOUT_CONTENT);

    List<PageTemplate> pageTemplates = pageTemplateStorage.getPageTemplates();
    assertNotNull(pageTemplates);
    assertEquals(1, pageTemplates.size());
    assertEquals(pageTemplateEntity.getId(), pageTemplates.get(0).getId());
    assertEquals(pageTemplateEntity.getContent(), pageTemplates.get(0).getContent());
  }

  @Test
  public void getPageTemplate() {
    when(pageTemplateDAO.findById(2l)).thenReturn(Optional.of(pageTemplateEntity));
    when(pageTemplateEntity.getId()).thenReturn(2l);
    when(pageTemplateEntity.getContent()).thenReturn(LAYOUT_CONTENT);

    PageTemplate retrievedPageTemplate = pageTemplateStorage.getPageTemplate(2l);
    assertEquals(pageTemplateEntity.getId(), retrievedPageTemplate.getId());
    assertEquals(pageTemplateEntity.getContent(), retrievedPageTemplate.getContent());
  }

  @Test
  public void createPageTemplate() {
    when(pageTemplate.getContent()).thenReturn(LAYOUT_CONTENT);

    when(pageTemplateDAO.save(any(PageTemplateEntity.class))).thenAnswer(invocation -> {
      PageTemplateEntity entity = invocation.getArgument(0);
      entity.setId(2l);
      return entity;
    });
    PageTemplate createdPageTemplate = pageTemplateStorage.createPageTemplate(pageTemplate);
    assertNotNull(createdPageTemplate);
    assertEquals(2l, createdPageTemplate.getId());
    assertEquals(pageTemplate.getContent(), createdPageTemplate.getContent());
  }

  @Test
  public void updatePageTemplate() throws ObjectNotFoundException {
    when(pageTemplate.getContent()).thenReturn(LAYOUT_CONTENT);

    when(pageTemplateDAO.save(any(PageTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    assertThrows(ObjectNotFoundException.class, () -> pageTemplateStorage.updatePageTemplate(pageTemplate));
    
    when(pageTemplate.getId()).thenReturn(2l);
    when(pageTemplate.getContent()).thenReturn(LAYOUT_CONTENT);
    assertThrows(ObjectNotFoundException.class, () -> pageTemplateStorage.updatePageTemplate(pageTemplate));

    when(pageTemplateDAO.existsById(pageTemplate.getId())).thenReturn(true);
    PageTemplate updatedPageTemplate = pageTemplateStorage.updatePageTemplate(pageTemplate);
    assertNotNull(updatedPageTemplate);
    assertEquals(pageTemplate.getId(), updatedPageTemplate.getId());
    assertEquals(pageTemplate.getContent(), updatedPageTemplate.getContent());
  }

  @Test
  public void deletePageTemplate() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageTemplateStorage.deletePageTemplate(2l));
    when(pageTemplateDAO.existsById(2l)).thenReturn(true);
    pageTemplateStorage.deletePageTemplate(2l);
    verify(pageTemplateDAO, times(1)).deleteById(2l);
  }

}
