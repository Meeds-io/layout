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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.portal.mop.Visibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.router.Router;

import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationUpdateModel;
import io.meeds.layout.model.NodeLabel;

@SpringBootTest(classes = { NavigationLayoutService.class })
@ExtendWith(MockitoExtension.class)
public class NavigationLayoutServiceTest {

  private static final String     TEST_USER = "testuser";

  private static final SiteKey    SITE_KEY  = SiteKey.portal("test");

  private static final PageKey    PAGE_KEY  = PageKey.parse("portal::test::test");

  @MockitoBean
  private NavigationService       navigationService;

  @MockitoBean
  private LayoutService           layoutService;

  @MockitoBean
  private PageLayoutService       pageLayoutService;

  @MockitoBean
  private DescriptionService      descriptionService;

  @MockitoBean
  private LayoutAclService        aclService;

  @MockitoBean
  private WebAppController        webController;

  @MockitoBean
  private ResourceBundleManager   resourceBundleManager;

  @MockitoBean
  private LocaleConfigService     localeConfigService;

  @Mock
  private NodeData                parentNodeData;

  @Mock
  private NodeData                nodeData;

  @Mock
  private NodeState               nodeState;

  @Mock
  private PageContext             pageContext;

  @Mock
  private LocaleConfig            defaultLocaleConfig;

  @Autowired
  private NavigationLayoutService navigationLayoutService;

  @Test
  public void createNode() throws IllegalAccessException, IllegalArgumentException, ObjectNotFoundException {
    NavigationCreateModel nodeModel = mock(NavigationCreateModel.class);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));
    when(navigationService.getNodeById(nodeModel.getParentNodeId())).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));
    when(aclService.canEditNavigation(parentNodeData.getSiteKey(), TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(PAGE_KEY.format());
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));

    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    when(nodeModel.isVisible()).thenReturn(true);
    when(nodeModel.isScheduled()).thenReturn(true);
    when(nodeModel.getStartScheduleDate()).thenReturn(3l);
    when(nodeModel.getEndScheduleDate()).thenReturn(2l);
    assertThrows(IllegalArgumentException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));

    when(nodeModel.getEndScheduleDate()).thenReturn(4l);
    assertThrows(IllegalArgumentException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));

    when(nodeModel.isPasteMode()).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> navigationLayoutService.createNode(nodeModel, TEST_USER));

    when(navigationService.createNode(any(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, nodeData });
    when(nodeData.getId()).thenReturn("36");
    when(navigationService.getNodeById(36l)).thenReturn(nodeData);
    assertNotNull(navigationLayoutService.createNode(nodeModel, TEST_USER));
  }

  @Test
  public void createDraftNode() throws IllegalAccessException, ObjectNotFoundException {
    when(nodeData.getId()).thenReturn("2");
    when(nodeData.getParentId()).thenReturn("3");
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.FRENCH);

    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createDraftNode(2l, TEST_USER));

    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.createDraftNode(2l, TEST_USER));

    when(aclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER)).thenReturn(true);
    PageKey clonedPageKey = PageKey.parse("portal::test::test_clone");
    when(pageLayoutService.clonePage(PAGE_KEY, TEST_USER)).thenReturn(clonedPageKey);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createDraftNode(2l, TEST_USER));

    when(navigationService.getNodeById(3l)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.createDraftNode(2l, TEST_USER));

    when(aclService.canEditNavigation(parentNodeData.getSiteKey(), TEST_USER)).thenReturn(true);
    NodeData clonedNodeData = mock(NodeData.class);
    PageContext clonedPageContext = mock(PageContext.class);
    when(layoutService.getPageContext(argThat(k -> k.format().equals(clonedPageKey.format())))).thenReturn(clonedPageContext);
    when(clonedPageContext.getKey()).thenReturn(clonedPageKey);
    when(navigationService.getNodeById(36l)).thenReturn(clonedNodeData);
    when(navigationService.createNode(any(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, nodeData });
    navigationLayoutService.createDraftNode(2l, TEST_USER);
    verify(navigationService).createNode(any(), any(), any(), any());
  }

  @Test
  public void updateNode() {
    NavigationUpdateModel nodeModel = mock(NavigationUpdateModel.class);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));
    when(aclService.canEditNavigation(nodeData.getSiteKey(), TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(PAGE_KEY.format());
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));

    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    when(nodeModel.isVisible()).thenReturn(true);
    when(nodeModel.isScheduled()).thenReturn(true);
    when(nodeModel.getStartScheduleDate()).thenReturn(3l);
    when(nodeModel.getEndScheduleDate()).thenReturn(2l);
    assertThrows(IllegalArgumentException.class, () -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));

    when(nodeModel.getEndScheduleDate()).thenReturn(4l);
    assertThrows(IllegalArgumentException.class, () -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));

    when(nodeModel.getStartScheduleDate()).thenReturn(null);
    when(nodeModel.getEndScheduleDate()).thenReturn(null);

    when(nodeData.getId()).thenReturn("2");
    assertDoesNotThrow(() -> navigationLayoutService.updateNode(2, nodeModel, TEST_USER));
    verify(navigationService).updateNode(any(), any());
  }

  @Test
  public void deleteNode() {
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.deleteNode(2, 0, TEST_USER));
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.deleteNode(2, 0, TEST_USER));
    when(aclService.canEditNavigation(nodeData.getSiteKey(), TEST_USER)).thenReturn(true);
    assertDoesNotThrow(() -> navigationLayoutService.deleteNode(2, 10, TEST_USER));
    verify(navigationService, never()).deleteNode(2l);

    assertDoesNotThrow(() -> navigationLayoutService.deleteNode(2, 0l, TEST_USER));
    verify(navigationService, atLeast(1)).deleteNode(2l);

    when(nodeState.getVisibility()).thenReturn(Visibility.SYSTEM);
    when(nodeData.getState()).thenReturn(nodeState);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.deleteNode(2, 0, TEST_USER));
  }

  @Test
  public void undoDeleteNode() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.deleteNode(2, 0, TEST_USER));
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(nodeData.getSiteKey(), TEST_USER)).thenReturn(true);
    navigationLayoutService.deleteNode(2, 10, TEST_USER);
    verify(navigationService, never()).deleteNode(2l);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.undoDeleteNode(2, "anotherUser"));
    navigationLayoutService.undoDeleteNode(2, TEST_USER);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.undoDeleteNode(2, TEST_USER));
  }

  @Test
  public void moveNode() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.moveNode(2l, 3l, 4l, TEST_USER));

    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getParentId()).thenReturn("55");
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.moveNode(2l, 3l, 4l, TEST_USER));

    when(navigationService.getNodeById(55l)).thenReturn(parentNodeData);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.moveNode(2l, null, 4l, TEST_USER));

    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(parentNodeData.getSiteKey(), TEST_USER)).thenReturn(true);

    navigationLayoutService.moveNode(2l, null, 4l, TEST_USER);
    verify(navigationService).moveNode(2l, 55l, 55l, 4l);

    when(navigationService.getNodeById(3l)).thenReturn(parentNodeData);
    navigationLayoutService.moveNode(2l, 3l, 4l, TEST_USER);
    verify(navigationService).moveNode(2l, 55l, 3l, 4l);
  }

  @Test
  public void getNodeLabels() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.getNodeLabels(2, TEST_USER));
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.FRENCH);

    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.getNodeLabels(2, TEST_USER));
    verify(descriptionService, never()).getDescriptions("2");

    when(aclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER)).thenReturn(true);
    navigationLayoutService.getNodeLabels(2, TEST_USER);
    verify(descriptionService).getDescriptions("2");

    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.FRENCH);

    LocaleConfig otherLocaleConfig = mock(LocaleConfig.class);
    when(otherLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);
    when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(defaultLocaleConfig, otherLocaleConfig));
    String nodeName = "testLabel";
    when(descriptionService.getDescriptions("2")).thenReturn(Collections.singletonMap(Locale.ENGLISH,
                                                                                      new State(nodeName, "testDescription")));

    NodeLabel nodeLabel = navigationLayoutService.getNodeLabels(2, TEST_USER);
    assertNotNull(nodeLabel);
    assertNotNull(nodeLabel.getLabels());
    assertEquals(defaultLocaleConfig.getLocale().toLanguageTag(), nodeLabel.getDefaultLanguage());
    assertEquals(2, nodeLabel.getSupportedLanguages().size());
    assertEquals(2, nodeLabel.getLabels().size());
    assertEquals(nodeName, nodeLabel.getLabels().get(defaultLocaleConfig.getLocale().toLanguageTag()));
    assertEquals(nodeName, nodeLabel.getLabels().get(otherLocaleConfig.getLocale().toLanguageTag()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getNodeUri() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.getNodeUri(2l, TEST_USER));
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.getNodeUri(2l, TEST_USER));

    when(aclService.canViewNavigation(SITE_KEY, PAGE_KEY, TEST_USER)).thenReturn(true);
    Router router = mock(Router.class);
    when(webController.getRouter()).thenReturn(router);
    when(router.render(any(Map.class))).thenReturn("/test/en/uri?lang=en&lang=en");

    String nodeUri = navigationLayoutService.getNodeUri(2l, TEST_USER);
    assertEquals("/test/uri", nodeUri);
  }

}
