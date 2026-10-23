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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.exoplatform.portal.mop.Visibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.router.Router;

import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationItemUpdate;
import io.meeds.layout.model.NodeSchedule;
import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PermissionUpdateModel;
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

  @MockitoBean
  private ListenerService         listenerService;

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

  @SuppressWarnings("unchecked")
  private NodeContext<NodeContext<Object>> newNodeContext() {
    return mock(NodeContext.class);
  }

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

  @SuppressWarnings("unchecked")
  @Test
  public void deleteNodeUnindexesPageWhenNoLongerReachable() throws ObjectNotFoundException, IllegalAccessException {
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    NodeContext<NodeContext<Object>> root = mock(NodeContext.class);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(0);

    Page page = mock(Page.class);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);

    navigationLayoutService.deleteNode(2, 0, TEST_USER);

    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, navigationLayoutService, page);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void deleteNodeUnindexesPagesOfTheDeletedSubtree() throws ObjectNotFoundException, IllegalAccessException {
    // Deleting a node cascades to its descendants, so their pages have to be
    // unindexed too, not just the deleted node's own page.
    PageKey childPageKey = PageKey.parse("portal::test::child");
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    NodeContext<NodeContext<Object>> rootBefore = mock(NodeContext.class);
    NodeContext<NodeContext<Object>> deletedNode = mock(NodeContext.class);
    NodeContext<NodeContext<Object>> childNode = mock(NodeContext.class);
    NodeState childState = mock(NodeState.class);
    when(rootBefore.getNodeCount()).thenReturn(1);
    when(rootBefore.get(0)).thenReturn(deletedNode);
    when(deletedNode.getId()).thenReturn("2");
    when(deletedNode.getState()).thenReturn(nodeState);
    when(deletedNode.getNodeCount()).thenReturn(1);
    when(deletedNode.get(0)).thenReturn(childNode);
    when(childNode.getState()).thenReturn(childState);
    when(childState.getPageRef()).thenReturn(childPageKey);
    when(childNode.getNodeCount()).thenReturn(0);

    // The subtree is collected from the tree as it stands before the deletion;
    // the reachability checks that follow see the tree without it.
    NodeContext<NodeContext<Object>> rootAfter = mock(NodeContext.class);
    when(rootAfter.getState()).thenReturn(null);
    when(rootAfter.getNodeCount()).thenReturn(0);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(rootBefore, rootAfter);

    Page page = mock(Page.class);
    Page childPage = mock(Page.class);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    when(layoutService.getPage(childPageKey)).thenReturn(childPage);

    navigationLayoutService.deleteNode(2, 0, TEST_USER);

    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, navigationLayoutService, page);
    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, navigationLayoutService, childPage);
    // Reachability of every page of the subtree is checked against a single
    // tree load, not one load per page: once to collect the subtree before
    // the deletion, once to check what's still reachable afterwards
    verify(navigationService, times(2)).loadNode(SITE_KEY);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void deleteNodeDoesNotUnindexPageWhenStillReachableFromAnotherNode() throws ObjectNotFoundException, IllegalAccessException {
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    NodeContext<NodeContext<Object>> root = mock(NodeContext.class);
    NodeContext<NodeContext<Object>> otherChild = mock(NodeContext.class);
    NodeState otherState = mock(NodeState.class);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(1);
    when(root.get(0)).thenReturn(otherChild);
    when(otherChild.getState()).thenReturn(otherState);
    when(otherState.getPageRef()).thenReturn(PAGE_KEY);

    navigationLayoutService.deleteNode(2, 0, TEST_USER);

    verify(listenerService, never()).broadcast(any(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getReachablePagesWalksTheWholeNavigationOfTheSite() {
    NodeContext<NodeContext<Object>> root = mock(NodeContext.class);
    NodeContext<NodeContext<Object>> child = mock(NodeContext.class);
    NodeContext<NodeContext<Object>> grandChild = mock(NodeContext.class);
    NodeState childState = mock(NodeState.class);
    NodeState grandChildState = mock(NodeState.class);
    PageKey grandChildPageKey = SITE_KEY.page("grandChild");
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getNodeCount()).thenReturn(1);
    when(root.get(0)).thenReturn(child);
    when(child.getState()).thenReturn(childState);
    when(childState.getPageRef()).thenReturn(PAGE_KEY);
    when(child.getNodeCount()).thenReturn(1);
    when(child.get(0)).thenReturn(grandChild);
    when(grandChild.getState()).thenReturn(grandChildState);
    when(grandChildState.getPageRef()).thenReturn(grandChildPageKey);

    assertEquals(Set.of(PAGE_KEY, grandChildPageKey), navigationLayoutService.getReachablePages(SITE_KEY));
  }

  @Test
  public void createNodeReindexesTheReferencedPage() throws ObjectNotFoundException, IllegalAccessException {
    // A page becomes searchable the moment a node points at it: without this,
    // a page whose blocks were unindexed when their last node was deleted
    // would never come back into the index when a new node is created for it
    NavigationCreateModel nodeModel = mock(NavigationCreateModel.class);
    when(navigationService.getNodeById(nodeModel.getParentNodeId())).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(PAGE_KEY.format());
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(PAGE_KEY);
    when(nodeData.getId()).thenReturn("2");
    when(navigationService.createNode(anyLong(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, nodeData });

    navigationLayoutService.createNode(nodeModel, TEST_USER);

    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, PAGE_KEY.format());
  }

  @Test
  public void createNodeDoesNotReindexADraftNode() throws ObjectNotFoundException, IllegalAccessException {
    // Draft pages are never indexed, so a draft node must not queue any work
    NavigationCreateModel nodeModel = mock(NavigationCreateModel.class);
    when(navigationService.getNodeById(nodeModel.getParentNodeId())).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(PAGE_KEY.format());
    when(nodeModel.isDraft()).thenReturn(true);
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(PAGE_KEY);
    when(nodeData.getId()).thenReturn("2");
    when(navigationService.createNode(anyLong(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, nodeData });

    navigationLayoutService.createNode(nodeModel, TEST_USER);

    verify(listenerService, never()).broadcast(any(), any(), any());
  }

  @Test
  public void updateNodeReindexesTheNewlyReferencedPageAndUnindexesTheAbandonedOne() throws ObjectNotFoundException,
                                                                                    IllegalAccessException {
    // Re-pointing a node to another page makes the new one reachable at this
    // URI, and can leave the previous one with no node leading to it at all —
    // its indexed content block would otherwise keep a pagePath that now
    // serves a different page.
    PageKey newPageKey = PageKey.parse("portal::test::new");
    NavigationUpdateModel nodeModel = mock(NavigationUpdateModel.class);
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getId()).thenReturn("2");
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(newPageKey.format());
    when(layoutService.getPageContext(newPageKey)).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(newPageKey);

    // Nothing points to the abandoned page anymore
    NodeContext<NodeContext<Object>> root = newNodeContext();
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(0);
    Page abandonedPage = mock(Page.class);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(abandonedPage);

    navigationLayoutService.updateNode(2, nodeModel, TEST_USER);

    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, newPageKey.format());
    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT,
                                      navigationLayoutService,
                                      abandonedPage);
  }

  @Test
  public void updateNodeDoesNotReindexAnythingWhenThePageReferenceIsUnchanged() throws ObjectNotFoundException,
                                                                               IllegalAccessException {
    // A node's name is the only part of its URI it owns, and nothing here can
    // change it (NavigationUpdateModel carries no name), so a label/icon edit
    // moves no page and must not re-index the whole subtree
    NavigationUpdateModel nodeModel = mock(NavigationUpdateModel.class);
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getId()).thenReturn("2");
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    when(nodeModel.getPageRef()).thenReturn(PAGE_KEY.format());
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(PAGE_KEY);
    when(nodeModel.getNodeLabel()).thenReturn("A brand new label");

    navigationLayoutService.updateNode(2, nodeModel, TEST_USER);

    verify(listenerService, never()).broadcast(any(), any(), any());
    verify(navigationService, never()).loadNode(SITE_KEY);
  }

  @Test
  public void moveNodeReindexesPagesOfTheMovedSubtree() throws ObjectNotFoundException, IllegalAccessException {
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getParentId()).thenReturn("55");
    when(navigationService.getNodeById(55l)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    NodeContext<NodeContext<Object>> root = newNodeContext();
    NodeContext<NodeContext<Object>> movedNode = newNodeContext();
    NodeState movedState = mock(NodeState.class);
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getNodeCount()).thenReturn(1);
    when(root.get(0)).thenReturn(movedNode);
    when(movedNode.getId()).thenReturn("2");
    when(movedNode.getState()).thenReturn(movedState);
    when(movedState.getPageRef()).thenReturn(PAGE_KEY);
    when(movedNode.getNodeCount()).thenReturn(0);

    navigationLayoutService.moveNode(2l, null, 4l, TEST_USER);

    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, PAGE_KEY.format());
  }

  @Test
  public void moveNodeAcrossSitesLooksTheSubtreeUpInTheDestinationSite() throws ObjectNotFoundException,
                                                                        IllegalAccessException {
    // The destination parent is allowed to belong to another site — that's the
    // site the ACL check runs against — and after the move the node is only
    // found in that site's tree, never in the one it came from
    SiteKey destinationSiteKey = SiteKey.portal("destination");
    when(navigationService.getNodeById(2l)).thenReturn(nodeData);
    when(nodeData.getParentId()).thenReturn("55");
    when(navigationService.getNodeById(3l)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(destinationSiteKey);
    when(aclService.canEditNavigation(destinationSiteKey, TEST_USER)).thenReturn(true);

    NodeContext<NodeContext<Object>> destinationRoot = newNodeContext();
    NodeContext<NodeContext<Object>> movedNode = newNodeContext();
    NodeState movedState = mock(NodeState.class);
    when(navigationService.loadNode(destinationSiteKey)).thenReturn(destinationRoot);
    when(destinationRoot.getNodeCount()).thenReturn(1);
    when(destinationRoot.get(0)).thenReturn(movedNode);
    when(movedNode.getId()).thenReturn("2");
    when(movedNode.getState()).thenReturn(movedState);
    when(movedState.getPageRef()).thenReturn(PAGE_KEY);
    when(movedNode.getNodeCount()).thenReturn(0);

    navigationLayoutService.moveNode(2l, 3l, 4l, TEST_USER);

    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, PAGE_KEY.format());
    verify(navigationService, never()).loadNode(SITE_KEY);
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


  @Test
  public void createNodeWithPageAppliesTheDrawersNameRuleOutsideCreateNode() throws Exception {
    List<NodeContext<NodeContext<Object>>> children = new ArrayList<>(List.of(childContext("10", "home"),
                                                                              childContext("11", "team_draft_john"),
                                                                              childContext("12", "hidden")));
    parentWithChildren(children);
    NavigationCreateModel nodeModel = new NavigationCreateModel();
    nodeModel.setParentNodeId(3L);
    nodeModel.setNodeLabel("Team");

    nodeModel.setNodeId("team");
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createNodeWithPage(nodeModel, null, TEST_USER));
    when(navigationService.getNodeById(3L)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(parentNodeData.getId()).thenReturn("3");
    assertThrows(IllegalAccessException.class, () -> navigationLayoutService.createNodeWithPage(nodeModel, null, TEST_USER));
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    for (String name : Arrays.asList(null, "", "our team", "équipe", "team/1", "x".repeat(201), "home", "hidden", "team_draft_john", "team_draft_x")) {
      nodeModel.setNodeId(name);
      assertEquals(NavigationLayoutService.NODE_NAME_NOT_ACCEPTED,
                   assertThrows(IllegalArgumentException.class,
                                () -> navigationLayoutService.createNodeWithPage(nodeModel, null, TEST_USER)).getMessage(),
                   String.valueOf(name));
    }
    verify(navigationService, never()).createNode(any(), any(), any(), any());
    verify(pageLayoutService, never()).createPage(any(), any());

    nodeModel.setNodeId("team");
    NodeData created = mock(NodeData.class);
    when(created.getId()).thenReturn("99");
    when(navigationService.createNode(any(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, created });
    when(navigationService.getNodeById(99L)).thenReturn(created);
    assertEquals(created, navigationLayoutService.createNodeWithPage(nodeModel, null, TEST_USER));
    verify(navigationService).createNode(eq(3L), eq(12L), eq("team"), any());
    verify(pageLayoutService, never()).createPage(any(), any());

    children.clear();
    nodeModel.setPreviousNodeId(null);
    navigationLayoutService.createNodeWithPage(nodeModel, null, TEST_USER);
    verify(navigationService).createNode(eq(3L), isNull(), eq("team"), any());
  }

  @Test
  public void createNodeWithPageCreatesThePageFirstAndRemovesWhatIsLeftBehindWhenTheNodeFails() throws Exception {
    List<NodeContext<NodeContext<Object>>> children = new ArrayList<>(List.of(childContext("10", "home")));
    parentWithChildren(children);
    when(navigationService.getNodeById(3L)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(parentNodeData.getId()).thenReturn("3");
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    PageKey pageKey = PageKey.parse("portal::test::team_1");
    when(pageContext.getKey()).thenReturn(pageKey);
    when(layoutService.getPageContext(pageKey)).thenReturn(pageContext);
    PageCreateModel pageModel = new PageCreateModel();
    pageModel.setPageName("team");
    when(pageLayoutService.createPage(pageModel, TEST_USER)).thenReturn(pageContext);
    NavigationCreateModel nodeModel = new NavigationCreateModel();
    nodeModel.setParentNodeId(3L);
    nodeModel.setNodeLabel("Team");
    nodeModel.setNodeId("team");

    // a schedule refused is refused before the page is created
    long future = System.currentTimeMillis() + 3_600_000L;
    nodeModel.setVisible(true);
    nodeModel.setScheduled(true);
    nodeModel.setStartScheduleDate(1000L);
    nodeModel.setEndScheduleDate(future);
    assertEquals(NavigationLayoutService.SCHEDULE_START_PAST,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER)).getMessage());
    nodeModel.setStartScheduleDate(future);
    nodeModel.setEndScheduleDate(null);
    assertEquals(NavigationLayoutService.SCHEDULE_INCOMPLETE,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER)).getMessage());
    nodeModel.setVisible(false);
    nodeModel.setEndScheduleDate(future + 1000L);
    assertEquals(NavigationLayoutService.SCHEDULE_ON_HIDDEN_NODE,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER)).getMessage());
    verify(pageLayoutService, never()).createPage(any(), any());
    nodeModel.setScheduled(false);
    nodeModel.setStartScheduleDate(null);
    nodeModel.setEndScheduleDate(null);

    // the created page is gone before the node state is built: the page goes
    when(layoutService.getPageContext(pageKey)).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER));
    verify(layoutService).remove(pageKey);
    verify(navigationService, never()).createNode(any(), any(), any(), any());
    when(layoutService.getPageContext(pageKey)).thenReturn(pageContext);

    // the node write fails before anything is stored: the page goes
    when(navigationService.createNode(any(), any(), any(), any())).thenThrow(new IllegalStateException("storage"));
    assertThrows(IllegalStateException.class, () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER));
    assertEquals(SITE_KEY.getTypeName(), pageModel.getPageSiteType());
    assertEquals(SITE_KEY.getName(), pageModel.getPageSiteName());
    assertEquals(pageKey.format(), nodeModel.getPageRef());
    InOrder order = inOrder(pageLayoutService, navigationService, layoutService);
    order.verify(pageLayoutService).createPage(pageModel, TEST_USER);
    order.verify(navigationService).createNode(eq(3L), eq(10L), eq("team"), any());
    order.verify(layoutService).remove(pageKey);
    verify(navigationService, never()).deleteNode(any());
    verify(layoutService, times(2)).remove(pageKey);

    // the node is written and its labels fail: the node goes, by the id its
    // write returned, then the page; whatever else sits under the parent
    NodeData created = mock(NodeData.class);
    lenient().when(created.getId()).thenReturn("99");
    NodeContext<NodeContext<Object>> someoneElses = childContext("77", "team");
    lenient().when(someoneElses.getState()).thenReturn(new NodeState.Builder().label("Team").pageRef(pageKey).updatedDate(System.currentTimeMillis() + 60_000L).build());
    doAnswer(i -> {
      children.add(someoneElses);
      return new NodeData[] { parentNodeData, created };
    }).when(navigationService).createNode(any(), any(), any(), any());
    when(navigationService.getNodeById(99L)).thenReturn(created);
    lenient().when(navigationService.getNodeById(77L)).thenReturn(mock(NodeData.class));
    doThrow(new IllegalArgumentException("locale")).when(descriptionService).setDescriptions(eq("99"), any());
    assertThrows(IllegalArgumentException.class, () -> navigationLayoutService.createNodeWithPage(nodeModel, pageModel, TEST_USER));
    order = inOrder(navigationService, layoutService);
    order.verify(navigationService).deleteNode(99L);
    order.verify(layoutService).remove(pageKey);
    verify(layoutService, times(3)).remove(pageKey);
    verify(navigationService, never()).deleteNode(77L);
  }

  /**
   * Pins the node-labels pitfall: a writer outside the drawer merges over the
   * raw stored descriptions, never over the labels synthesized from the
   * navigation bundle, so a bundle-keyed node keeps its key on an icon-only
   * update and stored translations stay as they are unless one is given
   */
  @Test
  public void updateNodeAndPagePermissionsKeepsWhatIsNotGiven() throws Exception {
    NodeState stored = new NodeState.Builder().label("#{portal.classic.home}").pageRef(PAGE_KEY).visibility(Visibility.DISPLAYED).build();
    NodeData node = storedNode(2L, stored);
    when(descriptionService.getDescriptions("2")).thenReturn(null);
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    lenient().when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);
    ArgumentCaptor<NodeState> states = ArgumentCaptor.forClass(NodeState.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<Locale, State>> descriptions = ArgumentCaptor.forClass(Map.class);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setIcon("fa-home");
    assertEquals(node, navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER));
    verify(navigationService).updateNode(eq(2L), states.capture());
    assertEquals("#{portal.classic.home}", states.getValue().getLabel());
    assertEquals("fa-home", states.getValue().getIcon());
    assertEquals(PAGE_KEY, states.getValue().getPageRef());
    assertEquals(Visibility.DISPLAYED, states.getValue().getVisibility());
    verify(descriptionService).setDescriptions(eq("2"), descriptions.capture());
    assertTrue(descriptions.getValue().isEmpty());
    verify(pageLayoutService, never()).updatePagePermissions(any(), any(), any());
    verify(pageLayoutService, never()).updatePageLink(any(), any(), any());
    verify(pageLayoutService, never()).createPage(any(), any());
    verify(listenerService, never()).broadcast(any(String.class), any(), any());

    when(descriptionService.getDescriptions("2")).thenReturn(new HashMap<>(Map.of(Locale.ENGLISH,
                                                                                  new State("Home", null),
                                                                                  Locale.FRENCH,
                                                                                  new State("Accueil", null))));
    update = new NavigationItemUpdate();
    update.setLabels(new HashMap<>(Map.of("fr", "Maison")));
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(descriptionService, times(2)).setDescriptions(eq("2"), descriptions.capture());
    assertEquals(Set.of(Locale.ENGLISH, Locale.FRENCH), descriptions.getValue().keySet());
    assertEquals("Home", descriptions.getValue().get(Locale.ENGLISH).getName());
    assertEquals("Maison", descriptions.getValue().get(Locale.FRENCH).getName());
    verify(navigationService, times(2)).updateNode(eq(2L), states.capture());
    assertEquals("#{portal.classic.home}", states.getValue().getLabel());
    assertNull(states.getValue().getIcon());

    update = new NavigationItemUpdate();
    update.setLabel("Start");
    update.setLabels(Map.of("en", "Begin"));
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(descriptionService, times(3)).setDescriptions(eq("2"), descriptions.capture());
    assertEquals("Start", descriptions.getValue().get(Locale.ENGLISH).getName());
    assertEquals("Accueil", descriptions.getValue().get(Locale.FRENCH).getName());
    verify(navigationService, times(3)).updateNode(eq(2L), states.capture());
    assertEquals("Start", states.getValue().getLabel());
  }

  @Test
  public void updateNodeAndPagePermissionsMergesTheVisibility() throws Exception {
    long future = System.currentTimeMillis() + 3_600_000L;
    NodeState stored = new NodeState.Builder().label("Team").visibility(Visibility.TEMPORAL).startPublicationTime(1000L).endPublicationTime(2000L).build();
    NodeData node = storedNode(2L, stored);
    ArgumentCaptor<NodeState> states = ArgumentCaptor.forClass(NodeState.class);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setIcon("fa-users");
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService).updateNode(eq(2L), states.capture());
    assertEquals(Visibility.TEMPORAL, states.getValue().getVisibility());
    assertEquals(1000L, states.getValue().getStartPublicationTime());
    assertEquals(2000L, states.getValue().getEndPublicationTime());

    update = new NavigationItemUpdate();
    update.setVisible(true);
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService, times(2)).updateNode(eq(2L), states.capture());
    assertEquals(Visibility.TEMPORAL, states.getValue().getVisibility());

    update = new NavigationItemUpdate();
    update.setVisible(false);
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService, times(3)).updateNode(eq(2L), states.capture());
    assertEquals(Visibility.HIDDEN, states.getValue().getVisibility());
    assertEquals(-1L, states.getValue().getStartPublicationTime());

    update = new NavigationItemUpdate();
    update.setSchedule(new NodeSchedule(null, null));
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService, times(4)).updateNode(eq(2L), states.capture());
    assertEquals(Visibility.DISPLAYED, states.getValue().getVisibility());
    assertEquals(-1L, states.getValue().getEndPublicationTime());

    update = new NavigationItemUpdate();
    update.setSchedule(new NodeSchedule(future, future + 1000L));
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService, times(5)).updateNode(eq(2L), states.capture());
    assertEquals(Visibility.TEMPORAL, states.getValue().getVisibility());
    assertEquals(future, states.getValue().getStartPublicationTime());
    assertEquals(future + 1000L, states.getValue().getEndPublicationTime());

    NavigationItemUpdate hiddenSchedule = new NavigationItemUpdate();
    hiddenSchedule.setVisible(false);
    hiddenSchedule.setSchedule(new NodeSchedule(future, future + 1000L));
    assertEquals(NavigationLayoutService.SCHEDULE_ON_HIDDEN_NODE,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.updateNodeAndPagePermissions(2L, hiddenSchedule, TEST_USER)).getMessage());
    NavigationItemUpdate pastSchedule = new NavigationItemUpdate();
    pastSchedule.setSchedule(new NodeSchedule(1000L, future));
    assertEquals(NavigationLayoutService.SCHEDULE_START_PAST,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.updateNodeAndPagePermissions(2L, pastSchedule, TEST_USER)).getMessage());
    NavigationItemUpdate reversedSchedule = new NavigationItemUpdate();
    reversedSchedule.setSchedule(new NodeSchedule(future + 1000L, future));
    assertEquals(NavigationLayoutService.SCHEDULE_END_BEFORE_START,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.updateNodeAndPagePermissions(2L, reversedSchedule, TEST_USER)).getMessage());
    NavigationItemUpdate halfSchedule = new NavigationItemUpdate();
    halfSchedule.setSchedule(new NodeSchedule(null, future));
    assertEquals(NavigationLayoutService.SCHEDULE_INCOMPLETE,
                 assertThrows(IllegalArgumentException.class,
                              () -> navigationLayoutService.updateNodeAndPagePermissions(2L, halfSchedule, TEST_USER)).getMessage());
    verify(navigationService, times(5)).updateNode(eq(2L), any());

    storedNode(4L, new NodeState.Builder().label("Docs").visibility(Visibility.HIDDEN).build());
    update = new NavigationItemUpdate();
    update.setSchedule(new NodeSchedule(null, null));
    navigationLayoutService.updateNodeAndPagePermissions(4L, update, TEST_USER);
    verify(navigationService).updateNode(eq(4L), states.capture());
    assertEquals(Visibility.HIDDEN, states.getValue().getVisibility());
    update = new NavigationItemUpdate();
    update.setVisible(true);
    navigationLayoutService.updateNodeAndPagePermissions(4L, update, TEST_USER);
    verify(navigationService, times(2)).updateNode(eq(4L), states.capture());
    assertEquals(Visibility.DISPLAYED, states.getValue().getVisibility());
    storedNode(5L, new NodeState.Builder().label("Admin").visibility(Visibility.SYSTEM).build());
    update = new NavigationItemUpdate();
    update.setSchedule(new NodeSchedule(null, null));
    navigationLayoutService.updateNodeAndPagePermissions(5L, update, TEST_USER);
    verify(navigationService).updateNode(eq(5L), states.capture());
    assertEquals(Visibility.SYSTEM, states.getValue().getVisibility());
    assertNotNull(node);
  }

  @Test
  public void updateNodeAndPagePermissionsWritesThePageStepsFirstAndPutsThemBackOnFailure() throws Exception {
    NodeState stored = new NodeState.Builder().label("Site").pageRef(PAGE_KEY).visibility(Visibility.DISPLAYED).build();
    storedNode(2L, stored);
    PageState previous = new PageState("Site", null, false, false, false, null, null, List.of("Everyone"), "*:/platform/users", "LINK", "https://old.example");
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(new PageContext(PAGE_KEY, previous));
    Map<Locale, State> storedLabels = Map.of(Locale.ENGLISH, new State("Site", null));
    when(descriptionService.getDescriptions("2")).thenReturn(storedLabels);
    ArgumentCaptor<PermissionUpdateModel> permissions = ArgumentCaptor.forClass(PermissionUpdateModel.class);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setLink("https://meeds.io");
    update.setLabels(Map.of("fr", "Site web"));
    update.setAccessPermissions(List.of("*:/platform/administrators"));
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    InOrder order = inOrder(pageLayoutService, navigationService);
    order.verify(pageLayoutService).updatePageLink(PAGE_KEY, "https://meeds.io", TEST_USER);
    order.verify(pageLayoutService).updatePagePermissions(eq(PAGE_KEY), permissions.capture(), eq(TEST_USER));
    order.verify(navigationService).updateNode(eq(2L), any());
    assertEquals(List.of("*:/platform/administrators"), permissions.getValue().getAccessPermissions());
    assertEquals("*:/platform/users", permissions.getValue().getEditPermission());

    doThrow(new IllegalStateException("storage")).when(navigationService).updateNode(eq(2L), any());
    assertThrows(IllegalStateException.class, () -> navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER));
    verify(pageLayoutService, times(2)).updatePageLink(PAGE_KEY, "https://meeds.io", TEST_USER);
    verify(pageLayoutService).updatePageLink(PAGE_KEY, "https://old.example", TEST_USER);
    verify(pageLayoutService, times(3)).updatePagePermissions(eq(PAGE_KEY), permissions.capture(), eq(TEST_USER));
    assertEquals(List.of("Everyone"), permissions.getValue().getAccessPermissions());
    assertEquals("*:/platform/users", permissions.getValue().getEditPermission());
    verify(layoutService, never()).remove(any(PageKey.class));
    InOrder labels = inOrder(descriptionService);
    labels.verify(descriptionService).setDescriptions(eq("2"), argThat(m -> m.containsKey(Locale.FRENCH)));
    labels.verify(descriptionService).setDescriptions("2", storedLabels);
  }

  @Test
  public void updateNodeAndPagePermissionsCreatesThePageAndRemovesItOnFailure() throws Exception {
    NodeState stored = new NodeState.Builder().label("Team").visibility(Visibility.DISPLAYED).build();
    storedNode(2L, stored);
    PageKey createdKey = PageKey.parse("portal::test::team_2");
    PageCreateModel pageModel = new PageCreateModel();
    pageModel.setPageName("team");
    when(pageContext.getKey()).thenReturn(createdKey);
    when(pageLayoutService.createPage(pageModel, TEST_USER)).thenReturn(pageContext);
    NodeContext<NodeContext<Object>> root = newNodeContext();
    lenient().when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    ArgumentCaptor<NodeState> states = ArgumentCaptor.forClass(NodeState.class);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setNewPage(pageModel);
    update.setAccessPermissions(List.of("Everyone"));
    update.setEditPermission("*:/platform/users");
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    assertEquals(SITE_KEY.getTypeName(), pageModel.getPageSiteType());
    assertEquals(SITE_KEY.getName(), pageModel.getPageSiteName());
    assertEquals("*:/platform/users", pageModel.getEditPermission());
    assertEquals("Everyone", pageModel.getAccessPermissions()[0]);
    verify(pageLayoutService, never()).updatePagePermissions(any(), any(), any());
    verify(navigationService).updateNode(eq(2L), states.capture());
    assertEquals(createdKey, states.getValue().getPageRef());
    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, createdKey.format());

    doThrow(new IllegalStateException("storage")).when(navigationService).updateNode(eq(2L), any());
    assertThrows(IllegalStateException.class, () -> navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER));
    verify(layoutService).remove(createdKey);
  }

  @Test
  public void updateNodeAndPagePermissionsRePointsOrDetachesAndBroadcasts() throws Exception {
    PageKey newKey = PageKey.parse("portal::test::new");
    NodeState stored = new NodeState.Builder().label("Team").pageRef(PAGE_KEY).visibility(Visibility.DISPLAYED).build();
    storedNode(2L, stored);
    when(layoutService.getPageContext(newKey)).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(newKey);
    NodeContext<NodeContext<Object>> root = newNodeContext();
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(0);
    Page abandonedPage = mock(Page.class);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(abandonedPage);
    ArgumentCaptor<NodeState> states = ArgumentCaptor.forClass(NodeState.class);

    NavigationItemUpdate update = new NavigationItemUpdate();
    update.setPageRef(newKey.format());
    update.setTarget("NEW_TAB");
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService).updateNode(eq(2L), states.capture());
    assertEquals(newKey, states.getValue().getPageRef());
    assertEquals("NEW_TAB", states.getValue().getTarget());
    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, newKey.format());
    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, navigationLayoutService, abandonedPage);

    update = new NavigationItemUpdate();
    update.setDetachPage(true);
    navigationLayoutService.updateNodeAndPagePermissions(2L, update, TEST_USER);
    verify(navigationService, times(2)).updateNode(eq(2L), states.capture());
    assertNull(states.getValue().getPageRef());
    verify(listenerService, times(2)).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, navigationLayoutService, abandonedPage);

    NavigationItemUpdate missing = new NavigationItemUpdate();
    missing.setPageRef("portal::test::missing");
    assertThrows(ObjectNotFoundException.class, () -> navigationLayoutService.updateNodeAndPagePermissions(2L, missing, TEST_USER));
  }

  /**
   * The name rule of createNodeWithPage leaves createNode and createDraftNode
   * as they are: the platform's own draft name carries the marker and the
   * username, a dot included
   */
  @Test
  public void createDraftNodeStaysOutsideTheNameRule() throws Exception {
    String dottedUser = "john.doe";
    when(nodeData.getParentId()).thenReturn("3");
    when(nodeData.getState()).thenReturn(nodeState);
    when(nodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(nodeData.getName()).thenReturn("home");
    when(nodeState.getPageRef()).thenReturn(PAGE_KEY);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultLocaleConfig);
    when(defaultLocaleConfig.getLocale()).thenReturn(Locale.ENGLISH);
    when(navigationService.getNodeById(2L)).thenReturn(nodeData);
    when(aclService.canViewNavigation(SITE_KEY, PAGE_KEY, dottedUser)).thenReturn(true);
    PageKey clonedPageKey = PageKey.parse("portal::test::home_draft_john.doe");
    when(pageLayoutService.clonePage(PAGE_KEY, dottedUser)).thenReturn(clonedPageKey);
    when(navigationService.getNodeById(3L)).thenReturn(parentNodeData);
    when(parentNodeData.getSiteKey()).thenReturn(SITE_KEY);
    when(aclService.canEditNavigation(SITE_KEY, dottedUser)).thenReturn(true);
    when(layoutService.getPageContext(argThat(k -> k.format().equals(clonedPageKey.format())))).thenReturn(pageContext);
    when(pageContext.getKey()).thenReturn(clonedPageKey);
    NodeData clonedNodeData = mock(NodeData.class);
    when(clonedNodeData.getId()).thenReturn("36");
    when(navigationService.getNodeById(36L)).thenReturn(clonedNodeData);
    when(navigationService.createNode(any(), any(), any(), any())).thenReturn(new NodeData[] { parentNodeData, clonedNodeData });

    navigationLayoutService.createDraftNode(2L, dottedUser);
    verify(navigationService).createNode(eq(3L), isNull(), eq("home_draft_john.doe"), any());
  }

  private NodeData storedNode(long id, NodeState state) {
    NodeData node = mock(NodeData.class);
    lenient().when(node.getId()).thenReturn(String.valueOf(id));
    lenient().when(node.getSiteKey()).thenReturn(SITE_KEY);
    lenient().when(node.getState()).thenReturn(state);
    when(navigationService.getNodeById(id)).thenReturn(node);
    lenient().when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    return node;
  }

  /**
   * The site's navigation: a root holding the parent node 3, whose children
   * are the given list, read at every load
   */
  private void parentWithChildren(List<NodeContext<NodeContext<Object>>> children) {
    NodeContext<NodeContext<Object>> root = newNodeContext();
    NodeContext<NodeContext<Object>> parent = newNodeContext();
    lenient().when(root.getId()).thenReturn("1");
    lenient().when(root.getNodeCount()).thenReturn(1);
    lenient().when(root.get(0)).thenReturn(parent);
    lenient().when(parent.getId()).thenReturn("3");
    lenient().when(parent.getNodeCount()).thenAnswer(i -> children.size());
    lenient().when(parent.get(anyInt())).thenAnswer(i -> children.get(i.getArgument(0)));
    lenient().when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
  }

  private NodeContext<NodeContext<Object>> childContext(String id, String name) {
    NodeContext<NodeContext<Object>> child = newNodeContext();
    lenient().when(child.getId()).thenReturn(id);
    lenient().when(child.getName()).thenReturn(name);
    lenient().when(child.getNodeCount()).thenReturn(0);
    return child;
  }
}
