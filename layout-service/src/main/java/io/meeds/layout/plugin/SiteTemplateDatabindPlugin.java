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
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.*;
import io.meeds.layout.plugin.attachment.PageTemplateAttachmentPlugin;
import io.meeds.layout.plugin.attachment.SiteTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.PageTemplateTranslationPlugin;
import io.meeds.layout.plugin.translation.SiteTemplateTranslationPlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.service.SiteTemplateService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.DescriptionService;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SiteTemplateDatabindPlugin implements DatabindPlugin {

  private static final Random    RANDOM          = new Random();

  public static final String     OBJECT_TYPE     = "SiteTemplate";

  public static final String     CONFIG_JSON     = "config.json";

  public static final String     NAVIGATION_JSON = "Navigation.json";

  @Autowired
  private SiteTemplateService    siteTemplateService;

  @Autowired
  LayoutService                  layoutService;

  @Autowired
  private DatabindService        databindService;

  @Autowired
  private FileService            fileService;

  @Autowired
  private TranslationService     translationService;

  @Autowired
  private AttachmentService      attachmentService;

  @Autowired
  private UserACL                userAcl;

  @Autowired
  private IdentityManager        identityManager;

  private long                   superUserIdentityId;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private NavigationService      navigationService;

  @Autowired
  DescriptionService             descriptionService;

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
    return StringUtils.equals(OBJECT_TYPE, objectType);
  }

  @SneakyThrows
  @Override
  public void serialize(String objectId, ZipOutputStream zipOutputStream, String username) {
    SiteTemplate siteTemplate = siteTemplateService.getSiteTemplate(Long.parseLong(objectId), Locale.getDefault());

    SiteTemplateDatabind databind = new SiteTemplateDatabind();
    databind.setIcon(siteTemplate.getIcon());
    TranslationField translationNameField = translationService.getTranslationField(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                   Long.parseLong(objectId),
                                                                                   PageTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                                                   username);
    if (translationNameField != null) {
      Map<String, String> names = translationNameField.getLabels()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(entry -> entry.getKey().toLanguageTag(),
                                                                                Map.Entry::getValue));
      databind.setNames(names);
    }

    TranslationField translationDescriptionField =
                                                 translationService.getTranslationField(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        PageTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                                                        username);
    if (translationDescriptionField != null) {
      Map<String, String> descriptions = translationDescriptionField.getLabels()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> entry.getKey()
                                                                                                            .toLanguageTag(),
                                                                                              Map.Entry::getValue));
      databind.setDescriptions(descriptions);
    }
    FileItem file = fileService.getFile(siteTemplate.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }

    SiteKey siteKey = SiteKey.portalTemplate(siteTemplate.getLayout());

    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);

    SiteDefinition siteDefinition = new SiteDefinition();

    siteDefinition.setName(portalConfig.getName());
    siteDefinition.setType(portalConfig.getType());
    siteDefinition.setAccessPermissions(portalConfig.getAccessPermissions());
    siteDefinition.setEditPermission(portalConfig.getEditPermission());
    siteDefinition.setProperties(portalConfig.getProperties());
    siteDefinition.setLayout(new LayoutModel(portalConfig.getPortalLayout(), portletInstanceService));

    siteDefinition.getLayout().resetStorage();
    databind.setSiteDefinition(siteDefinition);

    String jsonData = JsonUtils.toJsonString(databind);

    String folderPath = siteKey.getType() + "_" + siteKey.getName();

    List<PageContext> pageContexts = layoutService.findPages(siteKey);
    List<Page> pages = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(pageContexts)) {
      pages = pageContexts.stream().map(this::toPage).toList();
    }
    List<NodeDefinition> nodeDefinitions = buildNodeDefinitions(siteKey);

    String navigationJsonData = JsonUtils.toJsonString(nodeDefinitions);

    for (Page page : pages) {
      page.resetStorage();
      String pageJson = JsonUtils.toJsonString(page);
      writeToZip(zipOutputStream, folderPath + "/pages/" + page.getName() + ".json", pageJson);
    }
    writeToZip(zipOutputStream, folderPath + "/" + CONFIG_JSON, jsonData);
    writeToZip(zipOutputStream, folderPath + "/" + NAVIGATION_JSON, navigationJsonData);
  }

  public CompletableFuture<DatabindReport> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importSiteTemplates(zipFile, username)).thenApply(processedTemplates -> {
      DatabindReport report = new DatabindReport();
      report.setSuccess(!processedTemplates.isEmpty());
      report.setProcessedItems(processedTemplates);
      return report;
    });

  }

  @ContainerTransactional
  public List<String> importSiteTemplates(File zipFile, String username) {
    Map<String, SiteTemplateDatabind> templates = extractTemplates(zipFile);
    List<String> processedPageTemplates = new ArrayList<>();
    for (Map.Entry<String, SiteTemplateDatabind> entry : templates.entrySet()) {
      SiteTemplateDatabind siteTemplate = entry.getValue();
      processSiteTemplate(siteTemplate, username);
      processedPageTemplates.add(siteTemplate.getSiteDefinition().getName());
    }
    return processedPageTemplates;
  }

  private Map<String, SiteTemplateDatabind> extractTemplates(File zipFile) {
    Map<String, SiteTemplateDatabind> templateDatabindMap = new HashMap<>();

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory())
          continue;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zis.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        String jsonContent = baos.toString(StandardCharsets.UTF_8);

        String entryName = entry.getName();

        if (entryName.endsWith(CONFIG_JSON)) {
          SiteTemplateDatabind databind = JsonUtils.fromJsonString(jsonContent, SiteTemplateDatabind.class);
          if (databind != null) {
            String key = entryName.substring(0, entryName.lastIndexOf('/'));
            templateDatabindMap.put(key, databind);
          }
        } else if (entryName.endsWith(NAVIGATION_JSON)) {
          // Deserialize NodeDefinition list from NAVIGATION_JSON
          NodeDefinitionList nodeDefinitionList = JsonUtils.fromJsonString(jsonContent, NodeDefinitionList.class);

          if (nodeDefinitionList != null && org.apache.commons.collections.CollectionUtils.isNotEmpty(nodeDefinitionList.getNodeDefinitions())) {
            String key = entryName.substring(0, entryName.lastIndexOf('/'));
            SiteTemplateDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> new SiteTemplateDatabind());
            databind.setNodeDefinitions(nodeDefinitionList.getNodeDefinitions());
          }
        } else if (entryName.matches(".+/pages/.+\\.json$")) {
          Page page = JsonUtils.fromJsonString(jsonContent, Page.class);
          if (page != null) {
            String key = entryName.substring(0, entryName.indexOf("/pages/"));
            SiteTemplateDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> new SiteTemplateDatabind());
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

  private void saveIllustration(long pageTemplateId, byte[] illustrationBytes) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(illustrationBytes);
      String uploadId = "SiteTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(PageTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(pageTemplateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SiteTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(pageTemplateId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for site template '%s'",
                                                    pageTemplateId),
                                      e);
    } finally {
      if (tempFile != null) {
        try {
          Files.delete(tempFile.toPath());
        } catch (IOException e) {
          tempFile.deleteOnExit();
        }
      }
    }
  }

  @SneakyThrows
  private void saveNames(SiteTemplateDatabind siteTemplateDatabind, SiteTemplate siteTemplate) {
    translationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                             siteTemplate.getId(),
                                             SiteTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                             siteTemplateDatabind.getNames()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                           Map.Entry::getValue)));
  }

  @SneakyThrows
  private void saveDescriptions(SiteTemplateDatabind siteTemplateDatabind, SiteTemplate siteTemplate) {
    translationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                             siteTemplate.getId(),
                                             SiteTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                             siteTemplateDatabind.getDescriptions()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                           Map.Entry::getValue)));
  }

  @SneakyThrows
  private void processSiteTemplate(SiteTemplateDatabind siteTemplateDatabind, String username) {
    SiteTemplate siteTemplate = new SiteTemplate();
    siteTemplate.setName(siteTemplateDatabind.getNames().get("en"));
    siteTemplate.setDescription(siteTemplateDatabind.getDescriptions().get("en"));
    siteTemplate.setLayout(generateLayoutName(siteTemplate.getName()));
    siteTemplate.setIcon(siteTemplateDatabind.getIcon());
    siteTemplate.setSystem(false);
    PortalConfig portalConfig = siteTemplateDatabind.getSiteDefinition().getLayout().toSite();

    portalConfig.setName(siteTemplateDatabind.getSiteDefinition().getName());
    portalConfig.setType(siteTemplateDatabind.getSiteDefinition().getType());
    PortalConfig existSite = layoutService.getPortalConfig(portalConfig.getType(), portalConfig.getName());
    if (existSite == null) {
      layoutService.create(portalConfig);
    } else {
      portalConfig = existSite;
    }

    SiteKey siteKey = portalConfig.getSiteKey();
    if (CollectionUtils.isNotEmpty(siteTemplateDatabind.getPages())) {
      for (Page page : siteTemplateDatabind.getPages()) {
        PageKey k = page.getPageKey();
        layoutService.savePageFromTemplate(k, siteKey, "");
      }
    }

    List<NodeDefinition> nodeDefinitions = siteTemplateDatabind.getNodeDefinitions();

    SiteTemplate createdSiteTemplate = siteTemplateService.createSiteTemplate(siteTemplate,
                                                                              new SiteKey(portalConfig.getType(),
                                                                                          portalConfig.getName()),
                                                                              username,
                                                                              true);
    saveNames(siteTemplateDatabind, createdSiteTemplate);
    saveDescriptions(siteTemplateDatabind, createdSiteTemplate);
    if (siteTemplateDatabind.getIllustration() != null) {
      saveIllustration(createdSiteTemplate.getId(), Base64.decodeBase64(siteTemplateDatabind.getIllustration()));
    }
  }

  @SneakyThrows
  private File getIllustrationFile(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Illustration data is null");
    }
    File tempFile = File.createTempFile("temp", ".png");
    FileUtils.writeByteArrayToFile(tempFile, data);
    return tempFile;
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }

  public static String generateLayoutName(String name) {
    String transformed = name.toLowerCase()
                             .chars()
                             .mapToObj(c -> String.valueOf((char) ((c % 25) + 97)))
                             .collect(Collectors.joining());
    int randomNumber = ThreadLocalRandom.current().nextInt(1000);
    return transformed + randomNumber;
  }

  private void writeToZip(ZipOutputStream zipOutputStream, String filePath, String content) throws IOException {
    ZipEntry entry = new ZipEntry(filePath);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  @SneakyThrows
  private Page toPage(PageContext pageContext) {
    Page page = layoutService.getPage(pageContext.getKey());
    page.resetStorage();
    return page;
  }

  private List<NodeDefinition> buildNodeDefinitions(SiteKey siteKey) {
    NavigationContext navigationContext = navigationService.loadNavigation(siteKey);

    NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);

    if (rootNode == null || rootNode.getNodeCount() == 0) {
      return Collections.emptyList();
    }

    List<NodeDefinition> nodeDefinitions = new ArrayList<>();

    Collection<NodeContext<?>> children = getChildren(rootNode);
    for (NodeContext<?> child : children) {
      nodeDefinitions.add(buildNodeDefinitionRecursively(child));
    }
    return nodeDefinitions;
  }

  private NodeDefinition buildNodeDefinitionRecursively(NodeContext<?> nodeContext) {
    NodeState state = nodeContext.getData().getState();

    NodeDefinition def = new NodeDefinition();
    def.setName(nodeContext.getName());
    def.setIcon(state.getIcon());
    def.setVisibility(state.getVisibility());
    def.setPageReference(state.getPageRef());

    Map<Locale, State> descriptions = descriptionService.getDescriptions(nodeContext.getId());

    Map<String, State> labels = new HashMap<>();
    if (descriptions != null) {
      for (Map.Entry<Locale, State> entry : descriptions.entrySet()) {
        labels.put(entry.getKey().toLanguageTag(), entry.getValue());
      }
    }

    def.setLabels(labels);

    Collection<NodeContext<?>> children = getChildren(nodeContext);
    for (NodeContext<?> child : children) {
      def.getChildren().add(buildNodeDefinitionRecursively(child));
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
}
