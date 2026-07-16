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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.NavigationService;

import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.social.cms.service.PageUrlResolverService;

@ExtendWith(MockitoExtension.class)
class LayoutPageUrlResolverTest {

  private static final PageKey          PAGE_KEY = PageKey.parse("portal::site::page");

  private static final SiteKey          SITE_KEY = PAGE_KEY.getSite();

  @Mock
  private NavigationService             navigationService;

  @Mock
  private NavigationLayoutService       navigationLayoutService;

  @Mock
  private PageUrlResolverService        pageUrlResolverService;

  @InjectMocks
  private LayoutPageUrlResolver         resolver;

  @SuppressWarnings("unchecked")
  private NodeContext<NodeContext<Object>> newNode() {
    return mock(NodeContext.class);
  }

  @Test
  void shouldRegisterItselfOnInit() {
    resolver.init();

    verify(pageUrlResolverService).addPlugin(resolver);
  }

  @Test
  void shouldReturnNullWhenNoRootNodeFound() {
    when(navigationService.loadNode(SITE_KEY)).thenReturn(null);

    assertNull(resolver.resolvePath(PAGE_KEY));
  }

  @Test
  void shouldReturnNullWhenNoNodePointsToPage() {
    NodeContext<NodeContext<Object>> root = newNode();
    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(0);

    assertNull(resolver.resolvePath(PAGE_KEY));
  }

  @Test
  void shouldReturnPortalPrefixedUriWhenChildNodePointsToPage() {
    NodeContext<NodeContext<Object>> root = newNode();
    NodeContext<NodeContext<Object>> child = newNode();
    NodeState childState = mock(NodeState.class);
    NodeData childData = mock(NodeData.class);

    when(navigationService.loadNode(SITE_KEY)).thenReturn(root);
    when(root.getState()).thenReturn(null);
    when(root.getNodeCount()).thenReturn(1);
    when(root.get(0)).thenReturn(child);
    when(child.getState()).thenReturn(childState);
    when(childState.getPageRef()).thenReturn(PAGE_KEY);
    when(child.getData()).thenReturn(childData);
    when(navigationLayoutService.getNodeUri(childData)).thenReturn("/site/page-uri");

    assertEquals("/portal/site/page-uri", resolver.resolvePath(PAGE_KEY));
  }

  @Test
  void shouldReturnNullWhenNavigationServiceThrows() {
    when(navigationService.loadNode(SITE_KEY)).thenThrow(new RuntimeException("boom"));

    assertNull(resolver.resolvePath(PAGE_KEY));
  }

}
