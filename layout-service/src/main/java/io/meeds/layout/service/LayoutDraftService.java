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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

/**
 * Resolves the draft a user is editing through the co-editing revision the
 * layout editors record, never through a draft page or a draft site found in
 * storage: the editors re-clone the live layout whenever no revision names a
 * draft, so a leftover draft is a stale copy nobody edits.
 */
@Service
public class LayoutDraftService {

  public static final String NAVIGATION_OBJECT_TYPE = "navigation";

  public static final String SITE_OBJECT_TYPE       = "site";

  public static final String DRAFT_NAME_MARKER      = "_draft_";

  @Autowired
  private CoeditingService   coeditingService;

  @Autowired
  private NavigationService  navigationService;

  @Autowired
  private LayoutService      layoutService;

  /**
   * @param  liveNodeId the navigation node of the live page
   * @param  username   the user whose draft is looked up
   * @return the key of the draft page the user's revision names, null when
   *         the user has no draft of that page
   */
  public PageKey getDraftPageKey(long liveNodeId, String username) {
    String revision = getRevision(username, NAVIGATION_OBJECT_TYPE, String.valueOf(liveNodeId));
    if (!NumberUtils.isDigits(revision)) {
      return null;
    }
    NodeData draftNode = navigationService.getNodeById(Long.parseLong(revision));
    NodeState state = draftNode == null ? null : draftNode.getState();
    PageKey draftPageKey = state == null ? null : state.getPageRef();
    return draftPageKey != null && layoutService.getPage(draftPageKey) != null ? draftPageKey : null;
  }

  /**
   * @param  siteId   the live site
   * @param  username the user whose draft is looked up
   * @return the key of the draft site the user's revision names, null when
   *         the user has no draft of that site
   */
  public SiteKey getDraftSiteKey(long siteId, String username) {
    String revision = getRevision(username, SITE_OBJECT_TYPE, String.valueOf(siteId));
    if (!NumberUtils.isDigits(revision)) {
      return null;
    }
    PortalConfig draftSite = layoutService.getPortalConfig(Long.parseLong(revision));
    return draftSite != null && StringUtils.equals(draftSite.getType(), PortalConfig.DRAFT) ? draftSite.getSiteKey() : null;
  }

  /**
   * @param  pageKey a page
   * @return the id of the first navigation node of the page's own site that
   *         references it, draft nodes excluded, null when no node does
   */
  public Long findLiveNodeId(PageKey pageKey) {
    NodeContext<NodeContext<Object>> root = navigationService.loadNode(pageKey.getSite());
    NodeContext<NodeContext<Object>> node = findNodeByPage(root, pageKey);
    return node == null ? null : Long.parseLong(node.getId());
  }

  public boolean isDraftNode(String nodeName) {
    return StringUtils.contains(nodeName, DRAFT_NAME_MARKER);
  }

  private NodeContext<NodeContext<Object>> findNodeByPage(NodeContext<NodeContext<Object>> node, PageKey pageKey) {
    if (node == null) {
      return null;
    }
    NodeState state = node.getState();
    if (state != null && pageKey.equals(state.getPageRef()) && !isDraftNode(node.getName())) {
      return node;
    }
    for (int i = 0; i < node.getNodeCount(); i++) {
      NodeContext<NodeContext<Object>> result = findNodeByPage(node.get(i), pageKey);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private String getRevision(String username, String objectType, String objectId) {
    CoeditionObjectDraft draft = coeditingService.getRevision(username, new CoeditionObjectKey(objectType, objectId));
    return draft == null ? null : draft.getRevision();
  }

}
