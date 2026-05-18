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

import static io.meeds.layout.storage.SectionTemplateLayoutStorage.GLOBAL_SITE_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;

import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.service.ContainerLayoutService;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { SectionTemplateLayoutStorage.class, })
@ExtendWith(MockitoExtension.class)
public class SectionTemplateLayoutStorageTest {

  @MockitoBean
  private LayoutService                layoutService;

  @MockitoBean
  private NavigationService            navigationService;

  @MockitoBean
  private PageLayoutService            pageLayoutService;

  @MockitoBean
  private ContainerLayoutService       containerLayoutService;

  @MockitoBean
  private NavigationLayoutService      navigationLayoutService;

  @Autowired
  private SectionTemplateLayoutStorage sectionTemplateLayoutStorage;

  @Mock
  private SectionTemplate              sectionTemplate;

  private String                       username = "testuser";

  @Test
  @SuppressWarnings("unchecked")
  @SneakyThrows
  public void generateSectionTemplateNodeIdWhenNew() {
    NodeContext<NodeContext<Object>> parentNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    when(parentNode.getId()).thenReturn("85");
    when(navigationService.loadNode(GLOBAL_SITE_KEY)).thenReturn(parentNode);
    when(sectionTemplate.getContent()).thenReturn("{\"template\": \"FlexContainer\"}");
    sectionTemplateLayoutStorage.generateSectionTemplateNodeId(sectionTemplate, username);
    verify(navigationLayoutService).createNode(any(), eq(username));
    verify(pageLayoutService).impersonatePage(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  @SneakyThrows
  public void generateSectionTemplateNodeIdWhenUpdate() {
    NodeContext<NodeContext<Object>> parentNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    NodeContext<NodeContext<Object>> clonedNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    when(navigationService.loadNode(GLOBAL_SITE_KEY)).thenReturn(parentNode);
    when(navigationLayoutService.findNode(any(), anyString())).thenReturn(clonedNode);
    NodeState nodeState = mock(NodeState.class);
    when(clonedNode.getState()).thenReturn(nodeState);
    NodeState.Builder builder = mock(NodeState.Builder.class);
    when(nodeState.builder()).thenReturn(builder);
    when(builder.pageRef(any())).thenReturn(builder);
    when(builder.build()).thenReturn(nodeState);
    when(clonedNode.getId()).thenReturn("21");
    when(sectionTemplate.getContent()).thenReturn("{\"template\": \"FlexContainer\"}");
    sectionTemplateLayoutStorage.generateSectionTemplateNodeId(sectionTemplate, username);
    verify(navigationService).updateNode(21l, nodeState);
  }

  @Test
  @SuppressWarnings("unchecked")
  @SneakyThrows
  public void generateSectionTemplateContent() {
    NodeContext<NodeContext<Object>> parentNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    NodeContext<NodeContext<Object>> clonedNode = (NodeContext<NodeContext<Object>>) mock(NodeContext.class); // NOSONAR
    when(navigationService.loadNode(GLOBAL_SITE_KEY)).thenReturn(parentNode);
    when(navigationLayoutService.findNode(any(), anyString())).thenReturn(clonedNode);
    PageKey pageKey = mock(PageKey.class);
    NodeState nodeState = mock(NodeState.class);
    when(clonedNode.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(pageKey);
    Container section = mockSectionPage(pageKey);

    sectionTemplateLayoutStorage.generateSectionTemplateContent(sectionTemplate, username);
    verify(containerLayoutService).exportPortletPreferences(section);
  }

  @Test
  @SneakyThrows
  public void generateSectionTemplateContentByContainer() {
    Container section = mock(Container.class);
    when(containerLayoutService.cloneContainer(section)).thenReturn(section);
    Page page = mock(Page.class);
    when(layoutService.getPage(any(PageKey.class))).thenReturn(page);

    Container modelObject = mock(Container.class);
    ArrayList<ModelObject> children1 = new ArrayList<>();
    children1.add(modelObject);
    when(page.getChildren()).thenReturn(children1);

    ArrayList<ModelObject> children2 = new ArrayList<>();
    children2.add(section);
    when(modelObject.getChildren()).thenReturn(children2);
    when(section.getTemplate()).thenReturn("FlexContainer");

    sectionTemplateLayoutStorage.generateSectionTemplateContent(sectionTemplate, section, username);
    verify(containerLayoutService).exportPortletPreferences(section);
    verify(pageLayoutService).impersonatePage(any());
  }

  private Container mockSectionPage(PageKey pageKey) {
    Page page = mock(Page.class);
    when(layoutService.getPage(pageKey)).thenReturn(page);

    Container modelObject = mock(Container.class);
    ArrayList<ModelObject> children1 = new ArrayList<>();
    children1.add(modelObject);
    when(page.getChildren()).thenReturn(children1);

    ArrayList<ModelObject> children2 = new ArrayList<>();
    Container section = mock(Container.class);
    children2.add(section);
    when(modelObject.getChildren()).thenReturn(children2);
    return section;
  }

}
