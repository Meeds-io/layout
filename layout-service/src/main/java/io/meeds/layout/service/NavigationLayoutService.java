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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.portal.application.PortalRequestHandler;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.Visibility;
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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationItemUpdate;
import io.meeds.layout.model.NavigationUpdateModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.NodeSchedule;
import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.portal.web.handler.PortalTemplateRequestHandler;

@Service
public class NavigationLayoutService {

  /**
   * Broadcast when a page stops being reachable through any navigation node,
   * while the page itself still exists. Deliberately NOT
   * {@link LayoutService#PAGE_REMOVED}: that event means the page's own data
   * is gone, and a listener acting on it is entitled to clean up everything
   * attached to the page — which would destroy live data here.
   */
  public static final String             PAGE_UNREACHABLE_EVENT              = "layout.page.unreachable";

  public static final String             NODE_NAME_NOT_ACCEPTED              = "navigation.nodeName.notAccepted";

  public static final String             SCHEDULE_END_BEFORE_START           = "end schedule date must be after start schedule date";

  public static final String             SCHEDULE_START_PAST                 = "start schedule date must be after current date";

  public static final String             SCHEDULE_ON_HIDDEN_NODE             = "navigation.schedule.hiddenNode";

  public static final String             SCHEDULE_INCOMPLETE                 = "navigation.schedule.incomplete";

  public static final String             PARENT_NODE_NOT_FOUND_MESSAGE       = "Parent node with id %s doesn't exist";

  public static final String             NODE_NOT_FOUND_MESSAGE              = "Node with id %s doesn't exist";

  private static final Log               LOG                                 = ExoLogger.getExoLogger(NavigationLayoutService.class);

  private static final Map<Long, String> QUEUE                               = new ConcurrentHashMap<>();

  private static final Pattern           NODE_NAME_PATTERN                   = Pattern.compile("^[a-zA-Z0-9_-]{1,200}$");

  @Autowired
  private NavigationService              navigationService;

  @Autowired
  private LayoutService                  layoutService;

  @Autowired
  private ListenerService                listenerService;

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
      throw new ObjectNotFoundException(String.format(PARENT_NODE_NOT_FOUND_MESSAGE, nodeModel.getParentNodeId()));
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
    NodeData nodeData = storeNode(nodeModel, nodeState);
    completeNode(nodeData, nodeModel, nodeState, username);
    return navigationService.getNodeById(Long.parseLong(nodeData.getId()));
  }

  /**
   * The node write itself, without its labels and events
   */
  private NodeData storeNode(NavigationCreateModel nodeModel, NodeState nodeState) {
    NodeData[] nodeDatas = navigationService.createNode(nodeModel.getParentNodeId(),
                                                        nodeModel.getPreviousNodeId(),
                                                        nodeModel.getNodeId(),
                                                        nodeState);
    if (nodeDatas == null || nodeDatas.length < 2) {
      throw new IllegalStateException("Missing created node");
    }
    return nodeDatas[1];
  }

  /**
   * What follows a node write: its labels, and the page it makes reachable
   */
  private void completeNode(NodeData nodeData, NavigationCreateModel nodeModel, NodeState nodeState, String username) {
    saveNodeLabels(nodeData.getId(), nodeModel.getLabels());

    // A page becomes reachable — hence searchable — the moment a node starts
    // pointing at it, and this is the only navigation operation that does so
    // without any other event firing. Without this, a page whose content
    // blocks were unindexed when their last node was deleted would stay out
    // of the search index for good once a new node points at it again, and a
    // page indexed while it had no node at all would keep an empty pagePath.
    // Draft nodes are skipped: the connector doesn't index draft pages.
    if (nodeState.getPageRef() != null && !nodeModel.isDraft()) {
      broadcastPageUpdated(nodeState.getPageRef(), username);
    }
  }

  /**
   * Creates a navigation item for a caller outside the navigation drawer, a
   * tool or an API, with the page it points to when one is to be created.
   * The name is checked here and never in {@link #createNode}, which keeps
   * passing the platform's own draft and imported names through: the
   * drawer's character class, no draft marker, unique among the parent's
   * stored children, hidden and draft ones included. The item is appended
   * after the parent's last child when no previous node is named. The page
   * is created first, in the parent's site, then the node: a node failure
   * removes the page, and a node whose labels then failed is removed, by the
   * id its write returned, before it; a second write each, no atomicity. A
   * node write that throws is taken as having written nothing: this method
   * never looks a node up by name, which cannot tell its own from one
   * another caller wrote under that name meanwhile.
   *
   * @param  nodeModel                the item, its pageRef set here when a
   *                                    page model is given
   * @param  pageModel                the page to create, null for an item
   *                                    pointing at an existing page or none
   * @param  username                 the user creating the item
   * @return                          the created node
   * @throws ObjectNotFoundException  parent, page template or referenced page
   *                                    absent
   * @throws IllegalAccessException   the user cannot edit the site's
   *                                    navigation
   * @throws IllegalArgumentException the name refused
   *                                    ({@link #NODE_NAME_NOT_ACCEPTED}), the
   *                                    schedule refused, both checked before
   *                                    the page is created, or the page model
   */
  public NodeData createNodeWithPage(NavigationCreateModel nodeModel,
                                     PageCreateModel pageModel,
                                     String username) throws ObjectNotFoundException,
                                                      IllegalAccessException {
    NodeData parentNodeData = navigationService.getNodeById(nodeModel.getParentNodeId());
    if (parentNodeData == null) {
      throw new ObjectNotFoundException(String.format(PARENT_NODE_NOT_FOUND_MESSAGE, nodeModel.getParentNodeId()));
    } else if (!aclService.canEditNavigation(parentNodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }
    SiteKey siteKey = parentNodeData.getSiteKey();
    NodeContext<NodeContext<Object>> parent = findNodeById(navigationService.loadNode(siteKey), parentNodeData.getId());
    requireNodeName(parent, nodeModel.getNodeId());
    if (nodeModel.isScheduled()) {
      requireSchedule(nodeModel.getStartScheduleDate(), nodeModel.getEndScheduleDate(), nodeModel.isVisible());
    }
    if (nodeModel.getPreviousNodeId() == null && parent != null && parent.getNodeCount() > 0) {
      nodeModel.setPreviousNodeId(Long.parseLong(parent.get(parent.getNodeCount() - 1).getId()));
    }
    PageKey pageKey = null;
    if (pageModel != null) {
      pageModel.setPageSiteType(siteKey.getTypeName());
      pageModel.setPageSiteName(siteKey.getName());
      pageKey = pageLayoutService.createPage(pageModel, username).getKey();
      nodeModel.setPageRef(pageKey.format());
    }
    NodeData nodeData = null;
    try {
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
      nodeData = storeNode(nodeModel, nodeState);
      completeNode(nodeData, nodeModel, nodeState, username);
      return navigationService.getNodeById(Long.parseLong(nodeData.getId()));
    } catch (Exception e) {
      if (nodeData != null) {
        removeNodeLeftBehind(Long.parseLong(nodeData.getId()));
      }
      if (pageKey != null) {
        removePageLeftBehind(pageKey);
      }
      throw e;
    }
  }

  /**
   * @param  siteKey the site
   * @return         the id of the site's navigation root, null when the site
   *                 has no navigation
   */
  public Long getRootNodeId(SiteKey siteKey) {
    NodeContext<NodeContext<Object>> root = navigationService.loadNode(siteKey);
    return root == null ? null : Long.parseLong(root.getId());
  }

  /**
   * @param  nodeId                  the node
   * @param  username                the user about to change it
   * @return                         the node, whatever the user's view right
   *                                 on its page
   * @throws ObjectNotFoundException the node is absent
   * @throws IllegalAccessException  the user cannot edit the site's navigation
   */
  public NodeData getEditableNode(long nodeId, String username) throws ObjectNotFoundException, IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
    } else if (!aclService.canEditNavigation(nodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }
    return nodeData;
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
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
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
    writeNodeState(nodeData, nodeState, nodeModel.getLabels(), username);
  }

  /**
   * Changes on a navigation item what a caller outside the navigation drawer
   * was given and keeps every other stored value, the schedule a drawer
   * re-picks included: the node state is merged over the stored one and the
   * translations over the raw stored descriptions, never over the labels
   * {@link #getNodeLabels} synthesizes from the navigation bundle, so a
   * bundle-keyed node keeps its key unless a label is given. The item's page
   * steps come first and the node last, each step's inverse run on a later
   * failure: a page created from the model, the LINK page's link, the page's
   * permissions merged over its current ones; a second write each, no
   * atomicity.
   *
   * @param  nodeId                   the item
   * @param  update                   what to change
   * @param  username                 the user changing it
   * @return                          the node as stored
   * @throws ObjectNotFoundException  node, page or template absent
   * @throws IllegalAccessException   the user cannot edit the site's
   *                                    navigation, or the page whose link or
   *                                    permissions change
   * @throws IllegalArgumentException the schedule refused
   */
  public NodeData updateNodeAndPagePermissions(long nodeId, // NOSONAR
                                               NavigationItemUpdate update,
                                               String username) throws ObjectNotFoundException,
                                                                IllegalAccessException {
    NodeData nodeData = getEditableNode(nodeId, username);
    NodeState storedState = nodeData.getState() == null ? NodeState.INITIAL : nodeData.getState();
    Map<Locale, State> storedLabels = descriptionService.getDescriptions(nodeData.getId());
    PageKey pageKey = resolvePageKey(storedState, update);
    NodeSchedule schedule = update.getSchedule();
    if (schedule != null && !schedule.isCleared()) {
      requireSchedule(schedule.start(), schedule.end(), !Boolean.FALSE.equals(update.getVisible()));
    }
    PageKey createdPageKey = null;
    PageState previousPageState = null;
    boolean linkWritten = false;
    boolean permissionsWritten = false;
    boolean nodeStepStarted = false;
    try {
      if (update.getNewPage() != null) {
        createdPageKey = createItemPage(nodeData.getSiteKey(), update, username);
        pageKey = createdPageKey;
      } else if (pageKey != null
                 && (update.getLink() != null || update.getAccessPermissions() != null || update.getEditPermission() != null)) {
        previousPageState = readPageState(pageKey);
        if (update.getLink() != null) {
          pageLayoutService.updatePageLink(pageKey, update.getLink(), username);
          linkWritten = true;
        }
        if (update.getAccessPermissions() != null || update.getEditPermission() != null) {
          List<String> accessPermissions = update.getAccessPermissions() == null ? previousPageState.getAccessPermissions() :
                                                                                  update.getAccessPermissions();
          String editPermission = update.getEditPermission() == null ? previousPageState.getEditPermission() :
                                                                      update.getEditPermission();
          pageLayoutService.updatePagePermissions(pageKey, new PermissionUpdateModel(accessPermissions, editPermission), username);
          permissionsWritten = true;
        }
      }
      nodeStepStarted = true;
      writeNodeState(nodeData, mergeNodeState(storedState, update, pageKey), mergeLabels(storedLabels, update), username);
    } catch (Exception e) {
      if (nodeStepStarted) {
        restoreNodeLabels(nodeData.getId(), storedLabels);
      }
      if (permissionsWritten) {
        restorePagePermissions(pageKey, previousPageState, username);
      }
      if (linkWritten) {
        restorePageLink(pageKey, previousPageState, username);
      }
      if (createdPageKey != null) {
        removePageLeftBehind(createdPageKey);
      }
      throw e;
    }
    return navigationService.getNodeById(nodeId);
  }

  /**
   * Writes a node's labels and state, then what the page change makes
   * searchable: the newly referenced page, and the previously referenced one
   * when this node was its last.
   */
  void writeNodeState(NodeData nodeData, NodeState nodeState, Map<String, String> labels, String username) {
    long nodeId = Long.parseLong(nodeData.getId());
    PageKey previousPageKey = nodeData.getState() == null ? null : nodeData.getState().getPageRef();
    saveNodeLabels(nodeData.getId(), labels);
    navigationService.updateNode(nodeId, nodeState);

    // A node's name — the only part of its URI it owns — can't be changed
    // here (NavigationUpdateModel carries no name, and nothing else renames a
    // node), so neither this node's URI nor any of its descendants' can move.
    // The one thing that does change what is searchable is the page this node
    // points to: the newly referenced page becomes reachable at this URI, and
    // the previously referenced one may have just lost its last node — in
    // which case its indexed content block would keep a pagePath that now
    // leads to a different page entirely.
    PageKey newPageKey = nodeState.getPageRef();
    if (!Objects.equals(previousPageKey, newPageKey)) {
      if (newPageKey != null) {
        broadcastPageUpdated(newPageKey, username);
      }
      if (previousPageKey != null) {
        unindexPagesNoLongerReachable(nodeData.getSiteKey(), Set.of(previousPageKey));
      }
    }
  }

  public void deleteNode(long nodeId,
                         long delay,
                         String username) throws ObjectNotFoundException,
                                          IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
    } else if (!aclService.canEditNavigation(nodeData.getSiteKey(), username) || isSystemVisibility(nodeData)) {
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
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
    }
    if (destinationParentId == null) {
      destinationParentId = Long.parseLong(nodeData.getParentId());
    }
    NodeData destinationNodeData = navigationService.getNodeById(destinationParentId);
    if (destinationNodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, destinationParentId));
    } else if (!aclService.canEditNavigation(destinationNodeData.getSiteKey(), username)) {
      throw new IllegalAccessException();
    }
    navigationService.moveNode(nodeId, Long.parseLong(nodeData.getParentId()), destinationParentId, previousNodeId);

    // The moved subtree has to be looked up in the site it landed in, not the
    // one it came from: the destination parent is allowed to belong to another
    // site (that's the site the ACL check above runs against), and the node
    // wouldn't be found in the source site's tree anymore — leaving every page
    // it carries indexed with the URI it had before the move.
    broadcastPagesUpdated(destinationNodeData.getSiteKey(), nodeId, username);
  }

  public NodeData getNode(long nodeId,
                          String username) throws ObjectNotFoundException,
                                           IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
    } else if (!aclService.canViewNavigation(nodeData.getSiteKey(), nodeData.getState().getPageRef(), username)) {
      throw new IllegalAccessException();
    }
    return nodeData;
  }

  public NodeLabel getNodeLabels(long nodeId, String username) throws ObjectNotFoundException, IllegalAccessException {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    if (nodeData == null) {
      throw new ObjectNotFoundException(String.format(NODE_NOT_FOUND_MESSAGE, nodeId));
    } else {
      SiteKey siteKey = nodeData.getSiteKey();
      if (!aclService.canViewNavigation(siteKey, nodeData.getState().getPageRef(), username)) {
        throw new IllegalAccessException();
      }
    }
    return getNodeLabels(nodeData, nodeId);
  }

  private NodeLabel getNodeLabels(NodeData nodeData, long nodeId) {
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
    if (siteKey.getType() == SiteType.GROUP_TEMPLATE || siteKey.getType() == SiteType.PORTAL_TEMPLATE) {
      PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
      params.put(WebAppController.HANDLER_PARAM, PortalTemplateRequestHandler.HANDLER_NAME);
      params.put(PortalTemplateRequestHandler.REQUEST_SITE_ID, (portalConfig.getStorageId().split("_"))[1]);
      params.put(PortalTemplateRequestHandler.REQUEST_PATH, uriBuilder.toString().replaceFirst("/", ""));
      params.put(PortalTemplateRequestHandler.LANG, Locale.ENGLISH.toLanguageTag());
    } else {
      params.put(WebAppController.HANDLER_PARAM, PortalRequestHandler.HANDLER_NAME);
      params.put(PortalRequestHandler.REQUEST_SITE_NAME, siteKey.getName());
      params.put(PortalRequestHandler.REQUEST_SITE_TYPE, siteKey.getTypeName());
      params.put(PortalRequestHandler.REQUEST_PATH, uriBuilder.toString().replaceFirst("/", ""));
      params.put(PortalRequestHandler.LANG, Locale.ENGLISH.toLanguageTag());
    }
    return router.render(params).replace("/en", "").replace("?lang=en", "").replace("&lang=en", "");
  }

  /**
   * @param  siteKey the site whose navigation is walked
   * @return         the pages that a node of the site's navigation leads to,
   *                 pages of other sites included, whereas the search index
   *                 resolves a page's path in its own site's navigation
   */
  public Set<PageKey> getReachablePages(SiteKey siteKey) {
    Set<PageKey> pageKeys = new HashSet<>();
    collectPages(navigationService.loadNode(siteKey), pageKeys);
    return pageKeys;
  }

  @ContainerTransactional
  public void deleteNode(long nodeId) {
    NodeData nodeData = navigationService.getNodeById(nodeId);
    NodeState state = nodeData == null ? null : nodeData.getState();
    PageKey pageKey = state == null ? null : state.getPageRef();
    SiteKey siteKey = nodeData == null ? null : nodeData.getSiteKey();

    Set<PageKey> pageKeys = new HashSet<>();
    if (pageKey != null) {
      pageKeys.add(pageKey);
    }
    // Deleting a node cascades to its entire subtree (NodeEntity#children is
    // mapped with CascadeType.ALL), so the pages carried by its descendants can
    // become unreachable as well. They have to be collected before the
    // deletion, while the subtree still exists.
    if (siteKey != null) {
      collectSubtreePages(siteKey, String.valueOf(nodeId), pageKeys);
    }

    navigationService.deleteNode(nodeId);

    if (siteKey != null) {
      unindexPagesNoLongerReachable(siteKey, pageKeys);
    }
  }

  /**
   * Adds to {@code pageKeys} every page referenced by the node identified by
   * {@code nodeId} or by any of its descendants.
   *
   * @param siteKey  the site the node belongs to
   * @param nodeId   the node whose subtree to walk
   * @param pageKeys the collected page keys, mutated in place
   */
  private void collectSubtreePages(SiteKey siteKey, String nodeId, Set<PageKey> pageKeys) {
    try {
      NodeContext<NodeContext<Object>> node = findNodeById(navigationService.loadNode(siteKey), nodeId);
      collectPages(node, pageKeys);
    } catch (Exception e) {
      LOG.debug("Cannot collect the pages carried by the subtree of node {}", nodeId, e);
    }
  }

  private NodeContext<NodeContext<Object>> findNodeById(NodeContext<NodeContext<Object>> node, String nodeId) {
    if (node == null) {
      return null;
    }
    if (nodeId.equals(node.getId())) {
      return node;
    }
    int count = node.getNodeCount();
    for (int i = 0; i < count; i++) {
      NodeContext<NodeContext<Object>> found = findNodeById(node.get(i), nodeId);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private void collectPages(NodeContext<NodeContext<Object>> node, Set<PageKey> pageKeys) {
    if (node == null) {
      return;
    }
    NodeState state = node.getState();
    if (state != null && state.getPageRef() != null) {
      pageKeys.add(state.getPageRef());
    }
    int count = node.getNodeCount();
    for (int i = 0; i < count; i++) {
      collectPages(node.get(i), pageKeys);
    }
  }

  /**
   * A page carrying an indexed content block stays searchable only as long
   * as some navigation node still leads to it — deleting the last node
   * pointing to a page doesn't delete the page itself (nothing else does
   * either), so without this check its content block would stay indexed
   * forever with a search result that leads nowhere.
   * <p>
   * The site's navigation tree is loaded (with {@code Scope.ALL}) and walked
   * exactly once for the whole set: deleting a section carrying dozens of
   * descendant pages must not turn into one full tree load per page.
   *
   * @param siteKey the site the deleted node belonged to
   * @param pageKeys the pages the just-deleted subtree used to point to
   */
  private void unindexPagesNoLongerReachable(SiteKey siteKey, Set<PageKey> pageKeys) {
    if (pageKeys.isEmpty()) {
      return;
    }
    try {
      Set<PageKey> reachablePages = getReachablePages(siteKey);
      pageKeys.stream().filter(pageKey -> !reachablePages.contains(pageKey)).forEach(this::broadcastPageUnreachable);
    } catch (Exception e) {
      LOG.debug("Cannot check whether pages {} are still reachable from navigation of site {}", pageKeys, siteKey, e);
    }
  }

  private void broadcastPageUnreachable(PageKey pageKey) {
    try {
      Page page = layoutService.getPage(pageKey);
      if (page != null) {
        listenerService.broadcast(PAGE_UNREACHABLE_EVENT, this, page);
      }
    } catch (Exception e) {
      LOG.debug("Cannot broadcast that page {} is no longer reachable from navigation", pageKey, e);
    }
  }

  /**
   * Renaming or moving a node changes the portal URI of the page it points
   * to, and of every page carried by its descendants. That URI is stored at
   * index time ({@code pagePath}), so the pages have to be re-indexed or
   * their search results would keep pointing at a URL that now 404s.
   *
   * @param siteKey the site the node belongs to
   * @param nodeId the node whose subtree's URIs just changed
   * @param username the user who made the change
   */
  private void broadcastPagesUpdated(SiteKey siteKey, long nodeId, String username) {
    if (siteKey == null) {
      return;
    }
    Set<PageKey> pageKeys = new HashSet<>();
    collectSubtreePages(siteKey, String.valueOf(nodeId), pageKeys);
    pageKeys.forEach(pageKey -> broadcastPageUpdated(pageKey, username));
  }

  private void broadcastPageUpdated(PageKey pageKey, String username) {
    try {
      listenerService.broadcast(PageLayoutService.PAGE_UPDATED_EVENT, username, pageKey.format());
    } catch (Exception e) {
      LOG.debug("Cannot broadcast the update of page {} after its navigation node changed", pageKey, e);
    }
  }

  public NodeContext<NodeContext<Object>> findNode(NodeContext<NodeContext<Object>> node, String name) {
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
      checkSchedule(startScheduleDate, endScheduleDate, pasteMode);
      return new NodeState(label,
                             icon,
                             startScheduleDate,
                             endScheduleDate,
                             Visibility.TEMPORAL,
                             pageKey,
                             null,
                             target,
                             System.currentTimeMillis());
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

  /**
   * A schedule given to a caller outside the drawer: both bounds, a visible
   * node, and the drawer's own rules on the bounds
   */
  private void requireSchedule(Long startScheduleDate, Long endScheduleDate, boolean visible) {
    if (startScheduleDate == null || endScheduleDate == null) {
      throw new IllegalArgumentException(SCHEDULE_INCOMPLETE);
    } else if (!visible) {
      throw new IllegalArgumentException(SCHEDULE_ON_HIDDEN_NODE);
    }
    checkSchedule(startScheduleDate, endScheduleDate, false);
  }

  private void checkSchedule(long startScheduleDate, long endScheduleDate, boolean pasteMode) {
    if (startScheduleDate > endScheduleDate) {
      throw new IllegalArgumentException(SCHEDULE_END_BEFORE_START);
    } else if (System.currentTimeMillis() > startScheduleDate && !pasteMode) {
      throw new IllegalArgumentException(SCHEDULE_START_PAST);
    }
  }

  /**
   * The drawer's name rule, applied to a caller outside it: the character
   * class, no draft marker, unique among the parent's stored children
   */
  private void requireNodeName(NodeContext<NodeContext<Object>> parent, String name) {
    if (name == null
        || !NODE_NAME_PATTERN.matcher(name).matches()
        || name.contains(LayoutDraftService.DRAFT_NAME_MARKER)
        || childNamed(parent, name) != null) {
      throw new IllegalArgumentException(NODE_NAME_NOT_ACCEPTED);
    }
  }

  private NodeContext<NodeContext<Object>> childNamed(NodeContext<NodeContext<Object>> parent, String name) {
    if (parent == null) {
      return null;
    }
    int count = parent.getNodeCount();
    for (int i = 0; i < count; i++) {
      NodeContext<NodeContext<Object>> child = parent.get(i);
      if (StringUtils.equals(child.getName(), name)) {
        return child;
      }
    }
    return null;
  }

  /**
   * A node written by a creation that then failed on its labels is removed
   * by the id its write returned, with what it reaches
   */
  private void removeNodeLeftBehind(long nodeId) {
    try {
      deleteNode(nodeId);
    } catch (Exception e) {
      LOG.warn("Navigation node {} left by a failed creation could not be removed", nodeId, e);
    }
  }

  private void removePageLeftBehind(PageKey pageKey) {
    try {
      layoutService.remove(pageKey);
    } catch (Exception e) {
      LOG.warn("Page {} left by a failed navigation write could not be removed", pageKey, e);
    }
  }

  private void restoreNodeLabels(String nodeId, Map<Locale, State> storedLabels) {
    try {
      descriptionService.setDescriptions(nodeId, storedLabels);
    } catch (Exception e) {
      LOG.warn("Translations of node {} could not be put back after a failed navigation write", nodeId, e);
    }
  }

  private void restorePagePermissions(PageKey pageKey, PageState previousPageState, String username) {
    try {
      pageLayoutService.updatePagePermissions(pageKey,
                                              new PermissionUpdateModel(previousPageState.getAccessPermissions(),
                                                                        previousPageState.getEditPermission()),
                                              username);
    } catch (Exception e) {
      LOG.warn("Permissions of page {} could not be put back after a failed navigation write", pageKey, e);
    }
  }

  private void restorePageLink(PageKey pageKey, PageState previousPageState, String username) {
    try {
      pageLayoutService.updatePageLink(pageKey, previousPageState.getLink(), username);
    } catch (Exception e) {
      LOG.warn("Link of page {} could not be put back after a failed navigation write", pageKey, e);
    }
  }

  private PageKey createItemPage(SiteKey siteKey, NavigationItemUpdate update, String username) throws ObjectNotFoundException,
                                                                                                 IllegalAccessException {
    PageCreateModel pageModel = update.getNewPage();
    pageModel.setPageSiteType(siteKey.getTypeName());
    pageModel.setPageSiteName(siteKey.getName());
    if (pageModel.getAccessPermissions() == null && update.getAccessPermissions() != null) {
      pageModel.setAccessPermissions(update.getAccessPermissions().toArray(new String[0]));
    }
    if (pageModel.getEditPermission() == null) {
      pageModel.setEditPermission(update.getEditPermission());
    }
    return pageLayoutService.createPage(pageModel, username).getKey();
  }

  private PageKey resolvePageKey(NodeState storedState, NavigationItemUpdate update) throws ObjectNotFoundException {
    if (update.isDetachPage()) {
      return null;
    } else if (update.getPageRef() != null) {
      return getPageKey(update.getPageRef());
    } else {
      return storedState.getPageRef();
    }
  }

  private PageState readPageState(PageKey pageKey) throws ObjectNotFoundException {
    PageContext pageContext = layoutService.getPageContext(pageKey);
    if (pageContext == null || pageContext.getState() == null) {
      throw new ObjectNotFoundException(String.format("Page with key %s doesn't exist", pageKey));
    }
    return pageContext.getState();
  }

  /**
   * The stored state with what was given over it: a schedule makes the node
   * temporal on its bounds, a cleared one displayed when the node was
   * temporal and leaves any other visibility as it is, visible false hidden,
   * visible true displayed unless a schedule stands
   */
  private NodeState mergeNodeState(NodeState storedState, NavigationItemUpdate update, PageKey pageKey) {
    NodeState.Builder builder = storedState.builder().pageRef(pageKey).updatedDate(System.currentTimeMillis());
    if (update.getLabel() != null) {
      builder.label(update.getLabel());
    }
    if (update.getIcon() != null) {
      builder.icon(update.getIcon());
    }
    if (update.getTarget() != null) {
      builder.target(update.getTarget());
    }
    NodeSchedule schedule = update.getSchedule();
    if (schedule != null && !schedule.isCleared()) {
      builder.visibility(Visibility.TEMPORAL).startPublicationTime(schedule.start()).endPublicationTime(schedule.end());
    } else if (Boolean.FALSE.equals(update.getVisible())) {
      builder.visibility(Visibility.HIDDEN).startPublicationTime(-1).endPublicationTime(-1);
    } else if ((schedule != null && storedState.getVisibility() == Visibility.TEMPORAL)
               || (Boolean.TRUE.equals(update.getVisible()) && storedState.getVisibility() != Visibility.TEMPORAL)) {
      builder.visibility(Visibility.DISPLAYED).startPublicationTime(-1).endPublicationTime(-1);
    }
    return builder.build();
  }

  /**
   * The stored translations with the given ones over them, then the label as
   * the default language's, as the drawer writes it; null when the node
   * stores none and gets none
   */
  private Map<String, String> mergeLabels(Map<Locale, State> storedLabels, NavigationItemUpdate update) {
    Map<String, String> labels = new HashMap<>();
    if (storedLabels != null) {
      storedLabels.forEach((locale, state) -> labels.put(locale.toLanguageTag(), state.getName()));
    }
    if (update.getLabels() != null) {
      update.getLabels().forEach((tag, text) -> {
        if (text != null) {
          labels.put(tag, text);
        }
      });
    }
    if (update.getLabel() != null) {
      labels.put(localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag(), update.getLabel());
    }
    return labels.isEmpty() ? null : labels;
  }

  private void saveNodeLabels(String nodeId, Map<String, String> labels) {
    if (labels != null) {
      Map<Locale, State> nodeLabels = new HashMap<>();
      labels.entrySet()
            .forEach(label -> nodeLabels.put(Locale.forLanguageTag(label.getKey()), new State(label.getValue(), null)));
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
                                                                                                                             .map(LocaleConfig::getLocale)
                                                                                                                             .collect(Collectors.toMap(Locale::toLanguageTag,
                                                                                                                                                       Locale::getDisplayName));
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
    return locale.toLanguageTag().replace("-", "_"); // Use same name as
                                                     // localeConfigService
  }
  
  private boolean isSystemVisibility(NodeData nodeData) {
    return nodeData.getState() != null && Visibility.SYSTEM.equals(nodeData.getState().getVisibility());
  }

}
