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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import org.exoplatform.commons.exception.ObjectNotFoundException;
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

  @MockitoBean
  private SiteLayoutService    siteLayoutService;

  @MockitoBean
  private NavigationLayoutService navigationLayoutService;

  @MockitoBean
  private PageLayoutService    pageLayoutService;

  @MockitoBean
  private LayoutAclService     aclService;

  @Autowired
  private LayoutDraftService   layoutDraftService;

  @Test
  public void draftPageIsTheOneTheRevisionNames() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData liveNode = node("home", LIVE_PAGE);
    NodeData draftNode = node("home_draft_john", DRAFT_PAGE);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(draftNode);
    when(layoutService.getPage(DRAFT_PAGE)).thenReturn(new Page());

    assertEquals(DRAFT_PAGE, layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
  }

  @Test
  public void revisionNamingANodeThatIsNotThisUsersDraftOfThisPageReadsAsNoDraft() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData liveNode = node("home", LIVE_PAGE);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);

    NodeData otherUsersDraft = node("home_draft_bob", PageKey.parse("portal::test::home_draft_bob"));
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(otherUsersDraft);
    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));

    NodeData liveNodeOfAnotherPage = node("other", PageKey.parse("portal::test::other"));
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(liveNodeOfAnotherPage);
    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));

    verify(layoutService, never()).getPage(any(PageKey.class));
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
    NodeData liveNode = node("home", LIVE_PAGE);
    NodeData draftNode = node("home_draft_john", DRAFT_PAGE);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);
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
  public void draftSiteIsThisUsersDraftOfThisSite() {
    revision(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID), "77");
    when(layoutService.getPortalConfig(SITE_ID)).thenReturn(new PortalConfig("portal", "test"));
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john");
    when(layoutService.getPortalConfig(77L)).thenReturn(draftSite);

    assertEquals(draftSite.getSiteKey(), layoutDraftService.getDraftSiteKey(SITE_ID, USER));
  }

  @Test
  public void revisionNamingALiveSiteOrAnotherUsersDraftReadsAsNoDraft() {
    revision(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID), "77");
    when(layoutService.getPortalConfig(SITE_ID)).thenReturn(new PortalConfig("portal", "test"));

    when(layoutService.getPortalConfig(77L)).thenReturn(new PortalConfig("portal", "test"));
    assertNull(layoutDraftService.getDraftSiteKey(SITE_ID, USER));

    when(layoutService.getPortalConfig(77L)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_bob"));
    assertNull(layoutDraftService.getDraftSiteKey(SITE_ID, USER));

    when(layoutService.getPortalConfig(77L)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_other_draft_john"));
    assertNull(layoutDraftService.getDraftSiteKey(SITE_ID, USER));
  }

  @Test
  public void draftNamesFollowTheEditors() {
    assertEquals("home_draft_john", layoutDraftService.draftNodeName("home", USER));
    assertEquals("group_/spaces/x_draft_john", layoutDraftService.draftSiteName(new PortalConfig("group", "/spaces/x"), USER));
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
  public void createDraftPageRecordsTheDraftNodeIdAsTheUsersRevision() throws Exception {
    NodeData draftNode = node("home_draft_john", DRAFT_PAGE);
    when(draftNode.getId()).thenReturn(String.valueOf(DRAFT_NODE));
    when(navigationLayoutService.createDraftNode(LIVE_NODE, USER)).thenReturn(draftNode);

    assertEquals(DRAFT_PAGE, layoutDraftService.createDraftPage(LIVE_NODE, USER));

    verify(coeditingService).setRevision(USER, navigationKey(), String.valueOf(DRAFT_NODE));
  }

  /**
   * The draft node's page reference is the user's to re-point through the
   * navigation: a node of the right name pointing at any other page reads
   * as no draft, so nothing of that page is published or answered
   */
  @Test
  public void draftNodeRepointedAtAnotherPageReadsAsNoDraft() throws Exception {
    PageKey otherPage = PageKey.parse("portal::other::secret");
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData liveNode = node("home", LIVE_PAGE);
    NodeData repointed = node("home_draft_john", otherPage);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(repointed);
    lenient().when(layoutService.getPage(otherPage)).thenReturn(new Page());

    assertNull(layoutDraftService.getDraftPageKey(LIVE_NODE, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.publishPage(LIVE_PAGE, LIVE_NODE, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.discardDraftPage(LIVE_PAGE, LIVE_NODE, USER));
    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class));
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());
    verify(navigationLayoutService, never()).createDraftNode(anyLong(), any());
    verify(coeditingService, never()).removeRevision(any(), any());
  }

  @Test
  public void publishPageRefusesADraftTheUserMayNotEdit() throws Exception {
    ownDraftPage();
    when(aclService.canEditPage(DRAFT_PAGE, USER)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> layoutDraftService.publishPage(LIVE_PAGE, LIVE_NODE, USER));

    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class));
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());
    verify(coeditingService, never()).removeRevision(any(), any());
  }

  @Test
  public void publishPageWritesTheRevisionsDraftOverTheLivePageAndRemovesTheRevision() throws Exception {
    ownDraftPage();
    when(aclService.canEditPage(DRAFT_PAGE, USER)).thenReturn(true);
    Page draftPage = new Page("draft");
    when(pageLayoutService.getPageLayout(DRAFT_PAGE)).thenReturn(draftPage);

    layoutDraftService.publishPage(LIVE_PAGE, LIVE_NODE, USER);

    verify(pageLayoutService).updatePageLayout(LIVE_PAGE.format(), draftPage, true, USER);
    verify(coeditingService).removeRevision(USER, navigationKey());
    verify(navigationLayoutService, never()).createDraftNode(anyLong(), any());
  }

  /**
   * A leftover draft page without a revision, or a revision naming another
   * user's draft, reads as no draft: nothing is published or re-cloned
   */
  @Test
  public void publishAndDiscardPageWithoutADraftOfTheUserWriteNothing() throws Exception {
    when(coeditingService.getRevision(any(), any())).thenReturn(null);
    when(layoutService.getPage(DRAFT_PAGE)).thenReturn(new Page());
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.publishPage(LIVE_PAGE, LIVE_NODE, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.discardDraftPage(LIVE_PAGE, LIVE_NODE, USER));

    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData liveNode = node("home", LIVE_PAGE);
    NodeData otherUsersDraft = node("home_draft_bob", DRAFT_PAGE);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(otherUsersDraft);
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.publishPage(LIVE_PAGE, LIVE_NODE, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.discardDraftPage(LIVE_PAGE, LIVE_NODE, USER));

    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());
    verify(navigationLayoutService, never()).createDraftNode(anyLong(), any());
    verify(coeditingService, never()).removeRevision(any(), any());
    verify(coeditingService, never()).setRevision(any(), any(), any());
  }

  @Test
  public void discardDraftPageReclonesTheLivePageAndRecordsTheNewDraft() throws Exception {
    ownDraftPage();
    NodeData recloned = node("home_draft_john", DRAFT_PAGE);
    when(recloned.getId()).thenReturn(String.valueOf(DRAFT_NODE));
    when(navigationLayoutService.createDraftNode(LIVE_NODE, USER)).thenReturn(recloned);

    assertEquals(DRAFT_PAGE, layoutDraftService.discardDraftPage(LIVE_PAGE, LIVE_NODE, USER));

    verify(navigationLayoutService).createDraftNode(LIVE_NODE, USER);
    verify(coeditingService).setRevision(USER, navigationKey(), String.valueOf(DRAFT_NODE));
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());
  }

  @Test
  public void createDraftSiteRecordsTheDraftIdAsTheUsersRevision() throws Exception {
    PortalConfig liveSite = new PortalConfig("portal", "test", "portal_" + SITE_ID);
    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    when(siteLayoutService.createDraftSite(SiteKey.portal("test"), USER)).thenReturn(draftKey);
    when(layoutService.getPortalConfig(draftKey)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9"));

    assertEquals(draftKey, layoutDraftService.createDraftSite(liveSite, USER));
    verify(coeditingService).setRevision(USER, new CoeditionObjectKey(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID)), "9");
  }

  @Test
  public void publishSiteWritesTheRevisionsDraftOverTheLiveSiteAndRemovesTheRevision() throws Exception {
    PortalConfig liveSite = new PortalConfig("portal", "test", "portal_" + SITE_ID);
    PortalConfig draftSite = draftSite("portal_test_draft_john");
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);

    layoutDraftService.publishSite(liveSite, USER);

    verify(siteLayoutService).updateSiteLayout(SiteKey.portal("test"), draftSite, true, USER);
    verify(coeditingService).removeRevision(USER, new CoeditionObjectKey(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID)));
  }

  @Test
  public void publishAndDiscardWithoutADraftOfTheUserWriteNothing() throws Exception {
    PortalConfig liveSite = new PortalConfig("portal", "test", "portal_" + SITE_ID);
    when(coeditingService.getRevision(any(), any())).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.publishSite(liveSite, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.discardDraftSite(liveSite, USER));

    draftSite("portal_test_draft_bob");
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.publishSite(liveSite, USER));
    assertThrows(ObjectNotFoundException.class, () -> layoutDraftService.discardDraftSite(liveSite, USER));

    PortalConfig ownDraft = draftSite("portal_test_draft_john");
    when(siteLayoutService.getSite(ownDraft.getSiteKey(), USER)).thenThrow(new IllegalAccessException("owner only"));
    assertThrows(IllegalAccessException.class, () -> layoutDraftService.publishSite(liveSite, USER));

    verify(siteLayoutService, never()).updateSiteLayout(any(), any(), anyBoolean(), any());
    verify(siteLayoutService, never()).createDraftSite(any(), any());
    verify(coeditingService, never()).removeRevision(any(), any());
    verify(coeditingService, never()).setRevision(any(), any(), any());
  }

  @Test
  public void discardDraftSiteReclonesTheLiveSiteAndRecordsTheNewDraft() throws Exception {
    PortalConfig liveSite = new PortalConfig("portal", "test", "portal_" + SITE_ID);
    draftSite("portal_test_draft_john");
    SiteKey newDraftKey = SiteKey.draft("portal_test_draft_john");
    when(siteLayoutService.createDraftSite(SiteKey.portal("test"), USER)).thenReturn(newDraftKey);
    when(layoutService.getPortalConfig(newDraftKey)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_10"));

    assertEquals(newDraftKey, layoutDraftService.discardDraftSite(liveSite, USER));

    verify(siteLayoutService).createDraftSite(SiteKey.portal("test"), USER);
    verify(coeditingService).setRevision(USER, new CoeditionObjectKey(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID)), "10");
  }

  /**
   * The user's revision on the live site names draft site 9, stored with the
   * given name
   */
  private PortalConfig draftSite(String name) {
    revision(LayoutDraftService.SITE_OBJECT_TYPE, String.valueOf(SITE_ID), "9");
    PortalConfig liveSite = new PortalConfig("portal", "test", "portal_" + SITE_ID);
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, name, "draft_9");
    when(layoutService.getPortalConfig(SITE_ID)).thenReturn(liveSite);
    when(layoutService.getPortalConfig(9L)).thenReturn(draftSite);
    return draftSite;
  }

  @Test
  public void draftNodeIsRecognisedByItsMarker() {
    assertTrue(layoutDraftService.isDraftNode("home_draft_john"));
    assertFalse(layoutDraftService.isDraftNode("home"));
    assertFalse(layoutDraftService.isDraftNode(null));
  }

  private void ownDraftPage() {
    revision(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE), String.valueOf(DRAFT_NODE));
    NodeData liveNode = node("home", LIVE_PAGE);
    NodeData draftNode = node("home_draft_john", DRAFT_PAGE);
    when(navigationService.getNodeById(LIVE_NODE)).thenReturn(liveNode);
    when(navigationService.getNodeById(DRAFT_NODE)).thenReturn(draftNode);
    when(layoutService.getPage(DRAFT_PAGE)).thenReturn(new Page());
  }

  private CoeditionObjectKey navigationKey() {
    return new CoeditionObjectKey(LayoutDraftService.NAVIGATION_OBJECT_TYPE, String.valueOf(LIVE_NODE));
  }

  private void revision(String type, String id, String revision) {
    CoeditionObjectDraft draft = new CoeditionObjectDraft();
    draft.setRevision(revision);
    when(coeditingService.getRevision(USER, new CoeditionObjectKey(type, id))).thenReturn(draft);
  }

  private NodeData node(String name, PageKey pageRef) {
    NodeData nodeData = mock(NodeData.class);
    lenient().when(nodeData.getName()).thenReturn(name);
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
