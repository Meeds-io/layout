/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import io.meeds.layout.model.*;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.JsonUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.common.ContainerTransactional;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.space.template.model.SpaceTemplate;

import io.meeds.social.space.template.service.SpaceTemplateService;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

import static io.meeds.layout.util.DatabindUtils.retrieveBackgroundImages;
import static io.meeds.layout.util.DatabindUtils.saveAppBackgroundImages;

@Component
public class SpaceNavigationDatabindPlugin implements DatabindPlugin {

  private static final Log        LOG             = ExoLogger.getLogger(SpaceNavigationDatabindPlugin.class);

  public static final String      OBJECT_TYPE     = "SpaceNavigation";

  public static final String      NAVIGATION_JSON = "navigation.json";

  public static final String      CONFIG_JSON     = "config.json";

  @Autowired
  protected DatabindService       databindService;

  @Autowired
  protected FileService           fileService;

  @Autowired
  protected TranslationService    translationService;

  @Autowired
  protected SpaceTemplateService  spaceTemplateService;

  @Autowired
  protected AttachmentService     attachmentService;

  @Autowired
  LayoutService                   layoutService;

  @Autowired
  private NavigationService       navigationService;

  @Autowired
  DescriptionService              descriptionService;

  @Autowired
  private NavigationLayoutService navigationLayoutService;

  @Autowired
  private PageLayoutService       pageLayoutService;

  @Autowired
  PortletInstanceService          portletInstanceService;

  @Autowired
  protected UserACL               userAcl;

  @Autowired
  protected IdentityManager       identityManager;

  private long                    superUserIdentityId;

  @PostConstruct
  public void init() {
    databindService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canHandleDatabind(String objectType, String objectId) {
    return StringUtils.equals("SpaceTemplate", objectType);
  }

  @SneakyThrows
  @Override
  public void serialize(String objectId, ZipOutputStream zipOutputStream, String username) {
    SpaceTemplate spaceTemplate = spaceTemplateService.getSpaceTemplate(Long.parseLong(objectId),
                                                                        username,
                                                                        Locale.getDefault(),
                                                                        true);

    SiteKey siteKey = SiteKey.groupTemplate(spaceTemplate.getLayout());
    String folderPath = siteKey.getType() + "-" + siteKey.getName();

    List<PageContext> pageContexts = layoutService.findPages(siteKey);
    List<Page> pages = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(pageContexts)) {
      pages = pageContexts.stream().map(this::toPage).toList();
    }
    List<NodeDefinition> nodeDefinitions = buildNodeDefinitions(siteKey, username);

    String navigationJsonData = JsonUtils.toJsonString(nodeDefinitions);

    for (Page page : pages) {
      try {
        LayoutModel layoutModel = new LayoutModel(page, portletInstanceService, new PortletInstanceContext(true, null));
        retrieveBackgroundImages(layoutModel, fileService);
        layoutModel.resetStorage();
        String pageJson = JsonUtils.toJsonString(layoutModel);
        writeToZip(zipOutputStream, folderPath + "/pages/" + page.getName() + ".json", pageJson);
      } catch (Exception e) {
        LOG.warn("Error processing page " + page.getName() + ": " + e.getMessage());
      }
    }

    writeToZip(zipOutputStream, folderPath + "/" + NAVIGATION_JSON, navigationJsonData);
  }

  public CompletableFuture<Pair<DatabindReport, File>> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importSpaceTemplates(zipFile, username)).thenApply(processedTemplates -> {
      DatabindReport report = new DatabindReport();
      report.setSuccess(!processedTemplates.isEmpty());
      report.setProcessedItems(processedTemplates);
      return Pair.of(report, zipFile);
    });
  }

  @ContainerTransactional
  public List<String> importSpaceTemplates(File zipFile, String username) {
    Map<String, SpaceNavigationDatabind> instances = extractTemplates(zipFile);
    List<String> processedSpaceTemplates = new ArrayList<>();
    for (Map.Entry<String, SpaceNavigationDatabind> entry : instances.entrySet()) {
      SpaceNavigationDatabind spaceTemplate = entry.getValue();
      processSpaceTemplate(spaceTemplate, username);
      processedSpaceTemplates.add(spaceTemplate.getLayout());
    }
    return processedSpaceTemplates;
  }

  private Map<String, SpaceNavigationDatabind> extractTemplates(File zipFile) {
    Map<String, SpaceNavigationDatabind> templateDatabindMap = new HashMap<>();

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zis.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        String jsonContent = baos.toString(StandardCharsets.UTF_8);
        String entryName = entry.getName();

        if (entryName.endsWith(CONFIG_JSON)) {
          SpaceNavigationDatabind databindFromJson = JsonUtils.fromJsonString(jsonContent, SpaceNavigationDatabind.class);
          if (databindFromJson != null) {
            String key = entryName.substring(0, entryName.lastIndexOf('/'));
            templateDatabindMap.put(key, databindFromJson);
          }
        }

        else if (entryName.endsWith(NAVIGATION_JSON)) {
          List<NodeDefinition> nodeDefinitions = JsonUtils.fromJsonString(jsonContent, new TypeReference<>() {
          });
          if (nodeDefinitions != null) {
            String key = entryName.substring(0, entryName.lastIndexOf('/'));
            SpaceNavigationDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> {
              SpaceNavigationDatabind st = new SpaceNavigationDatabind();
              st.setPages(new ArrayList<>());
              return st;
            });
            databind.setNodeDefinitions(nodeDefinitions);
          }
        } else if (entryName.matches(".+/pages/.+\\.json$")) {
          LayoutModel page = JsonUtils.fromJsonString(jsonContent, LayoutModel.class);
          if (page != null) {
            String key = entryName.substring(0, entryName.indexOf("/pages/"));
            SpaceNavigationDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> {
              SpaceNavigationDatabind st = new SpaceNavigationDatabind();
              st.setPages(new ArrayList<>());
              return st;
            });
            if (databind.getPages() == null) {
              databind.setPages(new ArrayList<>());
            }
            databind.getPages().add(page);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error reading zip file", e);
    }
    return templateDatabindMap;
  }

  @SneakyThrows
  private void processSpaceTemplate(SpaceNavigationDatabind spaceTemplateDatabind, String username) {
    if (spaceTemplateDatabind.getSpaceTemplateId() == null) {
      return;
    }
    SpaceTemplate spaceTemplate =
                                spaceTemplateService.getSpaceTemplate(Long.parseLong(spaceTemplateDatabind.getSpaceTemplateId()));
    SiteKey siteKey = SiteKey.groupTemplate(spaceTemplate.getLayout());

    if (CollectionUtils.isNotEmpty(spaceTemplateDatabind.getPages())) {
      for (LayoutModel layoutModel : spaceTemplateDatabind.getPages()) {
        saveAppBackgroundImages(spaceTemplate.getId(), layoutModel, attachmentService, getSuperUserIdentityId());

        Page page = layoutModel.toPage();
        page.setOwnerType(layoutModel.getOwnerType());
        page.setOwnerId(spaceTemplateDatabind.getSpaceTemplateId());
        page.setName(layoutModel.getName());
        page.setType(layoutModel.getType());
        page.setEditPermission(layoutModel.getEditPermission());
        page.setAccessPermissions(layoutModel.getAccessPermissions());
        page.setLink(layoutModel.getLink());
        page.setShowMaxWindow(false);
        page.setHideSharedLayout(false);

        if (page.getPageKey() != null) {
          layoutService.save(new PageContext(new PageKey(siteKey, page.getName()), Utils.toPageState(page)));
          pageLayoutService.updatePageLayout(page.getPageKey().format(), page, true, username);
        }
      }
    }

    NodeContext<NodeContext<Object>> parentNode = navigationService.loadNode(siteKey);
    List<NodeDefinition> nodeDefinitions = spaceTemplateDatabind.getNodeDefinitions();
    if (CollectionUtils.isNotEmpty(nodeDefinitions)) {
      NodeDefinition targetParentNode = nodeDefinitions.getFirst();

      NavigationUpdateModel navigationUpdateModel = new NavigationUpdateModel();
      navigationUpdateModel.setNodeLabel(targetParentNode.getName());
      navigationUpdateModel.setPageRef(getPageKey(siteKey, targetParentNode));
      navigationUpdateModel.setVisible(targetParentNode.getVisibility().equals(Visibility.DISPLAYED));
      navigationUpdateModel.setScheduled(false);
      navigationUpdateModel.setIcon(targetParentNode.getIcon());
      targetParentNode.setLabels(targetParentNode.getLabels());

      navigationLayoutService.updateNode(Long.parseLong(parentNode.getId()), navigationUpdateModel, username);
      parentNode.getNodes().forEach(node -> navigationLayoutService.deleteNode(Long.parseLong(node.getId())));
      createNodesRecursively(nodeDefinitions, parentNode.getId(), siteKey, username);
    }
  }

  private List<NodeDefinition> buildNodeDefinitions(SiteKey siteKey, String username) {
    NavigationContext navigationContext = navigationService.loadNavigation(siteKey);

    NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);

    if (rootNode == null || rootNode.getNodeCount() == 0) {
      return Collections.emptyList();
    }

    List<NodeDefinition> nodeDefinitions = new ArrayList<>();

    Collection<NodeContext<?>> children = getChildren(rootNode);
    for (NodeContext<?> child : children) {
      nodeDefinitions.add(buildNodeDefinitionRecursively(child, username));
    }
    return nodeDefinitions;
  }

  @SneakyThrows
  private NodeDefinition buildNodeDefinitionRecursively(NodeContext<?> nodeContext, String username) {
    NodeState state = nodeContext.getData().getState();

    NodeDefinition def = new NodeDefinition();
    def.setName(nodeContext.getName());
    def.setIcon(state.getIcon());
    def.setVisibility(state.getVisibility());
    def.setPageReference(state.getPageRef() != null ? state.getPageRef().format() : null);

    NodeLabel nodeLabel = navigationLayoutService.getNodeLabels(Long.parseLong(nodeContext.getId()), username);

    def.setLabels(nodeLabel.getLabels());

    Collection<NodeContext<?>> children = getChildren(nodeContext);
    for (NodeContext<?> child : children) {
      def.getChildren().add(buildNodeDefinitionRecursively(child, username));
    }
    return def;
  }

  @SuppressWarnings("unchecked")
  private Collection<NodeContext<?>> getChildren(NodeContext<?> node) {
    Collection<?> rawNodes = node.getNodes();
    if (rawNodes == null) {
      return Collections.emptyList();
    }
    return (Collection<NodeContext<?>>) rawNodes;
  }

  @SneakyThrows
  private Page toPage(PageContext pageContext) {
    Page page = layoutService.getPage(pageContext.getKey());
    page.resetStorage();
    return page;
  }

  private void writeToZip(ZipOutputStream zipOutputStream, String filePath, String content) throws IOException {
    ZipEntry entry = new ZipEntry(filePath);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  @SneakyThrows
  private void createNodesRecursively(List<NodeDefinition> nodeDefinitions, String parentId, SiteKey siteKey, String username) {

    String previousNodeId = null;
    for (NodeDefinition nodeDefinition : nodeDefinitions) {
      NavigationCreateModel model = new NavigationCreateModel(parentId != null ? Long.parseLong(parentId) : null,
                                                              previousNodeId != null ? Long.parseLong(previousNodeId) : null,
                                                              nodeDefinition.getName(),
                                                              nodeDefinition.getName(),
                                                              nodeDefinition.getVisibility().equals(Visibility.DISPLAYED),
                                                              false,
                                                              false,
                                                              null,
                                                              null,
                                                              getPageKey(siteKey, nodeDefinition),
                                                              null,
                                                              false,
                                                              nodeDefinition.getIcon(),
                                                              nodeDefinition.getLabels());
      NodeData nodeData = null;
      if (!StringUtils.contains(nodeDefinition.getName(), "_draft_")) {
        nodeData = navigationLayoutService.createNode(model, username);
      }
      previousNodeId = nodeData != null ? nodeData.getId() : null;

      List<NodeDefinition> children = nodeDefinition.getChildren();
      if (children != null && !children.isEmpty()) {
        createNodesRecursively(children, nodeData != null ? nodeData.getId() : null, siteKey, username);
      }
    }
  }

  private String getPageKey(SiteKey siteKey, NodeDefinition nodeDefinition) {
    String pageRef = nodeDefinition.getPageReference();
    String pageName;

    if (StringUtils.isNotBlank(pageRef)) {
      int lastIndex = pageRef.lastIndexOf("::");
      pageName = lastIndex != -1 ? pageRef.substring(lastIndex + 2) : pageRef;
    } else {
      pageName = nodeDefinition.getName();
    }

    PageKey pageKey = new PageKey(siteKey, pageName);
    String formattedKey = pageKey.format();

    if (StringUtils.isBlank(formattedKey)) {
      return null;
    }

    PageContext pageContext = layoutService.getPageContext(PageKey.parse(formattedKey));
    return pageContext != null ? pageContext.getKey().format() : null;
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }

}
