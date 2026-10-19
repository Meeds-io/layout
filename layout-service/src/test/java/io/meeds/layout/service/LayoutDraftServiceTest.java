/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;

import io.meeds.social.coediting.model.CoeditionObjectDraft;
import io.meeds.social.coediting.model.CoeditionObjectKey;
import io.meeds.social.coediting.service.CoeditingService;

@SpringBootTest(classes = { LayoutDraftService.class })
@ExtendWith(MockitoExtension.class)
public class LayoutDraftServiceTest {

  private static final String  USER        = "john";

  private static final long    LIVE_NODE   = 11L;

  private static final long    DRAFT_NODE  = 12L;

  private static final long    SITE_ID     = 5L;

  private static final PageKey LIVE_PAGE   = PageKey.parse("portal::test::home");

  private static final PageKey DRAFT_PAGE  = PageKey.parse("portal::test::home_draft_john");

  @MockitoBean
  private CoeditingService     coeditingService;

  @MockitoBean
  private NavigationService    navigationService;

  @MockitoBean
  private LayoutService        layoutService;

  @Autowired
  private LayoutDraftService   layoutDraftService;

  @Test
  public void draftPageIsTheOneTheRevisionNames() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData draftNode = node(DRAFT_PAGE);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(draftNode);
    when(layoutService.getPage(DRAFT_PAGE)).thenReturn(new Page());

    assertEquals(DRAFT_PAGE, layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
  }

  @Test
  public void leftoverDraftWithoutRevisionIsNeverRead() {
    when(coeditingService.getRevision(any(), any())).thenReturn(null);

    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
    verify(navigationService, never()).getNodeById(anyLong());
    verify(layoutService, never()).getPage(any(PageKey.class));
  }

  @Test
  public void revisionNamingAVanishedDraftReadsAsNoDraft() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData draftNode = node(DRAFT_PAGE);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(draftNode);
    when(layoutService.getPage(DRAFT_PAGE)).thenReturn(null);

    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
  }

  @Test
  public void nonNumericRevisionReadsAsNoDraft() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), "abc");

    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
    verify(navigationService, never()).getNodeById(anyLong());
  }

  @Test
  public void draftSiteIsTheDraftTypedSiteTheRevisionNames() {
    revision(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID), "77");
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john");
    when(layoutService.getPortalConfig(77L)).thenReturn(draftSite);

    assertEquals(draftSite.getSiteKey(), layoutDraftService.getDraftSiteKey(SITE_ID, USER));
  }

  @Test
  public void revisionNamingALiveSiteReadsAsNoDraft() {
    revision(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID), "77");
    when(layoutService.getPortalConfig(77L)).thenReturn(new PortalConfig("portal", "test"));

    assertNull(layoutDraftService.getDraftSiteKey(SITE_ID, USER));
  }

  @Test
  public void liveNodeIsTheFirstNonDraftNodeReferencingThePage() {
    NodeContext<NodeContext<Object>> root = context("default", null, 0L);
    NodeContext<NodeContext<Object>> draft = context("home_draft_john", LIVE_PAGE, DRAFT_NODE);
    NodeContext<NodeContext<Object>> other = context("other", PageKey.parse("portal::test::other"), 13L);
    NodeContext<NodeContext<Object>> live = context("home", LIVE_PAGE, LIVE_NODE);
    children(root, draft, other);
    children(other, live);
    when(navigationService.loadNode(SiteKey.portal("test"))).thenReturn(root);

    assertEquals(LIVE_NODE, layoutDraftService.findLiveNodeId(LIVE_PAGE));
  }

  @Test
  public void pageNoNodeReferencesHasNoLiveNode() {
    NodeContext<NodeContext<Object>> root = context("default", null, 0L);
    children(root, context("other", PageKey.parse("portal::test::other"), 13L));
    when(navigationService.loadNode(SiteKey.portal("test"))).thenReturn(root);

    assertNull(layoutDraftService.findLiveNodeId(LIVE_PAGE));
  }

  @Test
  public void draftNodeIsRecognisedByItsMarker() {
    assertTrue(layoutDraftService.isDraftNode("home_draft_john"));
    assertFalse(layoutDraftService.isDraftNode("home"));
    assertFalse(layoutDraftService.isDraftNode(null));
  }

  private void revision(String type, String id, String revision) {
    CoeditionObjectDraft draft = new CoeditionObjectDraft();
    draft.setRevision(revision);
    when(coeditingService.getRevision(USER, new CoeditionObjectKey(type, id))).thenReturn(draft);
  }

  private NodeData node(PageKey pageRef) {
    NodeData nodeData = mock(NodeData.class);
    lenient().when(nodeData.getState()).thenReturn(new NodeState.Builder().pageRef(pageRef).build());
    return nodeData;
  }

  @SuppressWarnings("unchecked")
  private NodeContext<NodeContext<Object>> context(String name, PageKey pageRef, long id) {
    NodeContext<NodeContext<Object>> node = mock(NodeContext.class);
    lenient().when(node.getName()).thenReturn(name);
    lenient().when(node.getState()).thenReturn(new NodeState.Builder().pageRef(pageRef).build());
    lenient().when(node.getId()).thenReturn(String.valueOf(id));
    return node;
  }

  @SafeVarargs
  private void children(NodeContext<NodeContext<Object>> parent, NodeContext<NodeContext<Object>>... children) {
    lenient().when(parent.getNodeCount()).thenReturn(children.length);
    for (int i = 0; i < children.length; i++) {
      lenient().when(parent.get(i)).thenReturn(children[i]);
    }
  }
}
