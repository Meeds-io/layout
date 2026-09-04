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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
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

}
