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
package io.meeds.layout.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.social.cms.plugin.PageUrlResolver;
import io.meeds.social.cms.service.PageUrlResolverService;

import jakarta.annotation.PostConstruct;

/**
 * Resolves a Page's navigation path by walking its site's navigation tree
 * for the first node found pointing to it, and building that node's portal
 * URI. This is the concrete implementation of Social's generic
 * {@link PageUrlResolver} extension point — Layout owns the routing
 * primitives ({@code NavigationLayoutService}/{@code Router}) that Social
 * deliberately doesn't depend on.
 */
@Component
public class LayoutPageUrlResolver implements PageUrlResolver {

  private static final Log        LOGGER = ExoLogger.getExoLogger(LayoutPageUrlResolver.class);

  @Autowired
  private NavigationService       navigationService;

  @Autowired
  private NavigationLayoutService navigationLayoutService;

  @Autowired
  private PageUrlResolverService  pageUrlResolverService;

  @PostConstruct
  public void init() {
    pageUrlResolverService.addPlugin(this);
  }

  @Override
  public String resolvePath(PageKey pageKey) {
    try {
      NodeContext<NodeContext<Object>> root = navigationService.loadNode(pageKey.getSite());
      NodeData node = findNodeByPage(root, pageKey);
      return node == null ? null : "/portal" + navigationLayoutService.getNodeUri(node);
    } catch (Exception e) {
      LOGGER.debug("Cannot resolve a navigation node/path for page {}", pageKey, e);
      return null;
    }
  }

  private NodeData findNodeByPage(NodeContext<NodeContext<Object>> node, PageKey pageKey) {
    if (node == null) {
      return null;
    }
    NodeState state = node.getState();
    if (state != null && pageKey.equals(state.getPageRef())) {
      return node.getData();
    }
    int count = node.getNodeCount();
    for (int i = 0; i < count; i++) {
      NodeData found = findNodeByPage(node.get(i), pageKey);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

}
