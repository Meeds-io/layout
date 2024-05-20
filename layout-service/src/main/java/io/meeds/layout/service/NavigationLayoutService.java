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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.portal.application.PortalRequestHandler;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationUpdateModel;
import io.meeds.layout.model.NodeLabel;

@Service
public class NavigationLayoutService {

  private static final String            NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND = "Node with id %s doesn't exist";

  private static final Map<Long, String> QUEUE                               = new ConcurrentHashMap<>();

  @Autowired
  private NavigationService              navigationService;

  @Autowired
  private LayoutService                  layoutService;

  @Autowired
  private PageLayoutService              pageLayoutService;

  @Autowired
  private ResourceBundleManager          resourceBundleManager;

  @Autowired
  private LocaleConfigService            localeConfigService;

  @Autowired
  private DescriptionService             descriptionService;

  @Autowired
  private LayoutAclService               aclService;

  @Autowired
  private WebAppController               webController;

  public NodeData createNode(NavigationCreateModel nodeModel,
                             String username) throws ObjectNotFoundException,
                                              IllegalAccessException,
                                              IllegalArgumentException {
    NodeData parentNodeData = navigationService.getNodeById(nodeModel.getParentNodeId());
    if (parentNodeData == null) {
      throw new ObjectNotFoundException(String.format("Parent node with id %s doesn't exist", nodeModel.getParentNodeId()));
    } else if (!aclService.canEditNavigation(parentNodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }

    NodeState nodeState = buildNodeState(nodeModel.getNodeLabel(),
                                         nodeModel.getIcon(),
                                         getPageKey(nodeModel.getPageRef()),
                                         nodeModel.getTarget(),
                                         nodeModel.isVisible(),
                                         nodeModel.isDraft(),
                                         nodeModel.isScheduled(),
                                         nodeModel.getStartScheduleDate(),
                                         nodeModel.getEndScheduleDate(),
                                         nodeModel.isPasteMode());
    NodeData[] nodeDatas = navigationService.createNode(nodeModel.getParentNodeId(),
                                                        nodeModel.getPreviousNodeId(),
                                                        nodeModel.getNodeId(),
                                                        nodeState);
    if (nodeDatas == null || nodeDatas.length < 2) {
      throw new IllegalStateException("Missing created node");
    } else {
      NodeData nodeData = nodeDatas[1];
      saveNodeLabels(nodeData.getId(), nodeModel.getLabels());

      return navigationService.getNodeById(Long.parseLong(nodeData.getId()));
    }
  }

  public NodeData createDraftNode(Long nodeId, String username) throws ObjectNotFoundException,
                                                                IllegalAccessException {
    NodeData node = getNode(nodeId, username);
    PageKey clonedPageKey = pageLayoutService.clonePage(node.getState().getPageRef(), username);

    String clonedNodeName = node.getName() + "_draft_" + username;
    NodeContext<NodeContext<Object>> parentNode = navigationService.loadNode(node.getSiteKey());
    NodeContext<NodeContext<Object>> clonedNode = findNode(parentNode, clonedNodeName);
    if (clonedNode == null) {
      return createNode(new NavigationCreateModel(Long.parseLong(node.getParentId()),
                                                  null,
                                                  clonedNodeName,
                                                  clonedNodeName,
                                                  false,
                                                  false,
                                                  true,
                                                  null,
                                                  null,
                                                  clonedPageKey.format(),
                                                  null,
                                                  false,
                                                  null,
                                                  getNodeLabels(nodeId, username).getLabels()),
                        username);
    } else {
      NodeState state = clonedNode.getState().builder().pageRef(clonedPageKey).build();
      navigationService.updateNode(Long.parseLong(clonedNode.getId()), state);
      return navigationService.getNodeById(Long.parseLong(clonedNode.getId()));
    }
  }

  public void updateNode(long nodeId,
                         NavigationUpdateModel nodeModel,
                         String username) throws ObjectNotFoundException,
                                          IllegalAccessException,
                                          IllegalArgumentException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, nodeId));
    } else if (!aclService.canEditNavigation(nodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }

    NodeState nodeState = buildNodeState(nodeModel.getNodeLabel(),
                                         nodeModel.getIcon(),
                                         getPageKey(nodeModel.getPageRef()),
                                         nodeModel.getTarget(),
                                         nodeModel.isVisible(),
                                         false,
                                         nodeModel.isScheduled(),
                                         nodeModel.getStartScheduleDate(),
                                         nodeModel.getEndScheduleDate(),
                                         false);
    saveNodeLabels(nodeData.getId(), nodeModel.getLabels());
    navigationService.updateNode(nodeId, nodeState);
  }

  public void deleteNode(long nodeId,
                         long delay,
                         String username) throws ObjectNotFoundException,
                                          IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, nodeId));
    } else if (!aclService.canEditNavigation(nodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }
    if (delay > 0) {
      QUEUE.put(nodeId, username);
      CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS).execute(() -> {
        if (QUEUE.containsKey(nodeId)) {
          try {
            deleteNode(nodeId);
          } finally {
            QUEUE.remove(nodeId);
          }
        }
      });
    } else {
      deleteNode(nodeId);
    }
  }

  public void undoDeleteNode(long nodeId, String username) throws ObjectNotFoundException {
    if (StringUtils.equals(username, QUEUE.get(nodeId))) {
      QUEUE.remove(nodeId);
    } else {
      throw new ObjectNotFoundException(String.format("Node with id %s doesn't exist in queue", nodeId));
    }
  }

  public void moveNode(long nodeId,
                       Long destinationParentId,
                       Long previousNodeId,
                       String username) throws ObjectNotFoundException,
                                        IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, nodeId));
    } else {
      if (destinationParentId == null) {
        destinationParentId = Long.parseLong(nodeData.getParentId());
      }
      NodeData destinationNodeData = navigationService.getNodeById(destinationParentId);
      if (destinationNodeData == null) {
        throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, destinationParentId));
      } else if (!aclService.canEditNavigation(destinationNodeData.getSiteKey(), username)) {
        throw new IllegalAccessException();
      }
    }
    navigationService.moveNode(nodeId, Long.parseLong(nodeData.getParentId()), destinationParentId, previousNodeId);
  }

  public NodeData getNode(long nodeId,
                          String username) throws ObjectNotFoundException,
                                           IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, nodeId));
    } else if (!aclService.canViewNavigation(nodeData.getSiteKey(), nodeData.getState().getPageRef(), username)) {
      throw new IllegalAccessException();
    }
    return nodeData;
  }

  public NodeLabel getNodeLabels(long nodeId, String username) throws ObjectNotFoundException, IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_DATA_WITH_NODE_ID_IS_NOT_FOUND, nodeId));
    } else {
      SiteKey siteKey = nodeData.getSiteKey();
      if (!aclService.canViewNavigation(siteKey, nodeData.getState().getPageRef(), username)) {
        throw new IllegalAccessException();
      }
    }
    Map<Locale, State> nodeLabels = descriptionService.getDescriptions(String.valueOf(nodeId));
    if (MapUtils.isEmpty(nodeLabels)) {
      Map<Locale, State> nodeLocalizedLabels = new HashMap<>();
      localeConfigService.getLocalConfigs().forEach(localeConfig -> {
        Locale locale = localeConfig.getLocale();
        String label = nodeData.getState().getLabel();
        if (ExpressionUtil.isResourceBindingExpression(label)) {
          SiteKey siteKey = nodeData.getSiteKey();
          ResourceBundle nodeLabelResourceBundle = resourceBundleManager.getNavigationResourceBundle(getLocaleName(locale),
                                                                                                     siteKey.getTypeName(),
                                                                                                     siteKey.getName());
          if (nodeLabelResourceBundle != null) {
            label = ExpressionUtil.getExpressionValue(nodeLabelResourceBundle, label);
          }
        }
        nodeLocalizedLabels.put(locale, new State(label, null));
      });
      return toNodeLabel(nodeLocalizedLabels);
    } else {
      return toNodeLabel(nodeLabels);
    }
  }

  public String getNodeUri(long nodeId, String username) throws IllegalAccessException, ObjectNotFoundException {
    NodeData node = getNode(nodeId, username);
    return getNodeUri(node);
  }

  public String getNodeUri(NodeData node) {
    SiteKey siteKey = node.getSiteKey();

    StringBuilder uriBuilder = new StringBuilder();
    buildUri(node, uriBuilder);
    Router router = webController.getRouter();

    Map<QualifiedName, String> params = new HashMap<>();
    params.put(WebAppController.HANDLER_PARAM, "portal"); // PortalRequestHandler.NAME
    params.put(PortalRequestHandler.REQUEST_SITE_NAME, siteKey.getName());
    params.put(PortalRequestHandler.REQUEST_SITE_TYPE, siteKey.getTypeName());
    params.put(PortalRequestHandler.REQUEST_PATH, uriBuilder.toString().replaceFirst("/", ""));
    params.put(PortalRequestHandler.LANG, Locale.ENGLISH.toLanguageTag());
    return router.render(params).replace("/en", "").replace("?lang=en", "").replace("&lang=en", "");
  }

  @ContainerTransactional
  public void deleteNode(long nodeId) {
    navigationService.deleteNode(nodeId);
  }

  private NodeState buildNodeState(String label, // NOSONAR
                                   String icon,
                                   PageKey pageKey,
                                   String target,
                                   boolean visible,
                                   boolean draft,
                                   boolean scheduled,
                                   Long startScheduleDate,
                                   Long endScheduleDate,
                                   boolean pasteMode) {
    if (visible
        && scheduled
        && startScheduleDate != null
        && endScheduleDate != null) {
      if (startScheduleDate > endScheduleDate) {
        throw new IllegalArgumentException("end schedule date must be after start schedule date");
      } else if (System.currentTimeMillis() > startScheduleDate && !pasteMode) {
        throw new IllegalArgumentException("start schedule date must be after current date");
      } else {
        return new NodeState(label,
                             icon,
                             startScheduleDate,
                             endScheduleDate,
                             Visibility.TEMPORAL,
                             pageKey,
                             null,
                             target,
                             System.currentTimeMillis());
      }
    } else {
      Visibility visibility;
      if (draft) {
        visibility = Visibility.DRAFT;
      } else if (visible) {
        visibility = Visibility.DISPLAYED;
      } else {
        visibility = Visibility.HIDDEN;
      }
      return new NodeState(label,
                           icon,
                           -1,
                           -1,
                           visibility,
                           pageKey,
                           null,
                           target,
                           System.currentTimeMillis());
    }
  }

  private void saveNodeLabels(String nodeId, Map<String, String> labels) {
    if (labels != null) {
      Map<Locale, State> nodeLabels = new HashMap<>();
      labels.entrySet().forEach(label -> nodeLabels.put(new Locale(label.getKey()), new State(label.getValue(), null)));
      descriptionService.setDescriptions(nodeId, nodeLabels);
    } else {
      descriptionService.setDescriptions(nodeId, Collections.emptyMap());
    }
  }

  private PageKey getPageKey(String pageRef) throws ObjectNotFoundException, IllegalArgumentException {
    if (StringUtils.isNotBlank(pageRef)) {
      PageContext pageContext = layoutService.getPageContext(PageKey.parse(pageRef));
      if (pageContext == null) {
        throw new ObjectNotFoundException(String.format("Page with key %s doesn't exist", pageRef));
      } else {
        return pageContext.getKey();
      }
    } else {
      return null;
    }
  }

  private NodeContext<NodeContext<Object>> findNode(NodeContext<NodeContext<Object>> node, String name) {
    if (node == null || StringUtils.equals(node.getName(), name)) {
      return node;
    } else if (node.getNodeCount() > 0) {
      int count = node.getNodeCount();
      while (--count >= 0) {
        NodeContext<NodeContext<Object>> next = node.get(count);
        NodeContext<NodeContext<Object>> result = findNode(next, name);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  private void buildUri(NodeData node, StringBuilder uriBuilder) {
    if (StringUtils.isNotBlank(node.getName())
        && !StringUtils.equals(node.getName(), "default")) {
      uriBuilder.insert(0, node.getName());
      if (!uriBuilder.isEmpty()) {
        uriBuilder.insert(0, "/");
      }
    }
    if (StringUtils.isNotBlank(node.getParentId())) {
      NodeData parentNode = navigationService.getNodeById(Long.parseLong(node.getParentId()));
      buildUri(parentNode, uriBuilder);
    }
  }

  public NodeLabel toNodeLabel(Map<Locale, State> nodeLabels) {
    Locale defaultLocale = localeConfigService.getDefaultLocaleConfig() == null ? Locale.ENGLISH :
                                                                                localeConfigService.getDefaultLocaleConfig()
                                                                                                   .getLocale();
    String defaultLanguage = defaultLocale.getLanguage();
    Map<String, String> supportedLanguages =
                                           CollectionUtils.isEmpty(localeConfigService.getLocalConfigs()) ?
                                                                                                          Collections.singletonMap(defaultLocale.toLanguageTag(),
                                                                                                                                   defaultLocale.getDisplayName()) :
                                                                                                          localeConfigService.getLocalConfigs()
                                                                                                                             .stream()
                                                                                                                             .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocale()
                                                                                                                                                                                     .toLanguageTag(),
                                                                                                                                                                         "ma"))
                                                                                                                             .collect(Collectors.toMap(c -> c.getLocale()
                                                                                                                                                             .toLanguageTag(),
                                                                                                                                                       localeConfig -> localeConfig.getLocale()
                                                                                                                                                                                   .getDisplayName()));
    Map<String, String> localized = new HashMap<>();
    NodeLabel nodeLabel = new NodeLabel();
    if (MapUtils.isNotEmpty(nodeLabels)) {
      for (Map.Entry<Locale, State> entry : nodeLabels.entrySet()) {
        Locale locale = entry.getKey();
        State state = entry.getValue();
        localized.put(locale.toLanguageTag(), state.getName());
      }
      if (!nodeLabels.containsKey(defaultLocale) && !localized.isEmpty()) {
        localized.put(defaultLocale.toLanguageTag(), localized.values().iterator().next());
      }
      nodeLabel.setLabels(localized);
    }
    nodeLabel.setDefaultLanguage(defaultLanguage);
    nodeLabel.setSupportedLanguages(supportedLanguages);
    return nodeLabel;
  }

  private String getLocaleName(Locale locale) {
    return locale.toLanguageTag().replace("-", "_"); // Use same name as localeConfigService
  }

}
