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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.meeds.layout.model.*;
import io.meeds.layout.plugin.attachment.SectionTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SectionTemplateTranslationPlugin;
import io.meeds.layout.service.*;
import io.meeds.layout.util.EntityMapper;
import lombok.Synchronized;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadResource;

import io.meeds.common.ContainerTransactional;
import io.meeds.social.databind.model.DatabindReport;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

import static io.meeds.layout.util.DatabindUtils.*;
import static io.meeds.layout.util.JsonUtils.fromJsonString;
import static io.meeds.layout.util.JsonUtils.toJsonString;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SectionTemplateDatabindPlugin implements DatabindPlugin {

  public static final String     GLOBAL_SITE_NAME                        = "global";

  private static final PageKey   SECTION_TEMPLATE_EDITOR_SYSTEM_PAGE_KEY = new PageKey(SiteKey.portal(GLOBAL_SITE_NAME),
                                                                                       "_sectionTemplateEditor");

  private static final String[]  EVERYONE_PERMISSIONS                    = new String[] { UserACL.EVERYONE };

  private static final Random    RANDOM                                  = new Random();

  public static final String     OBJECT_TYPE                             = "SectionTemplate";

  public static final String     CONFIG_JSON                             = "config.json";

  public static final String     LAYOUT_JSON                             = "layout.json";

  @Autowired
  private SectionTemplateService sectionTemplateService;

  @Autowired
  private DatabindService        databindService;

  @Autowired
  private FileService            fileService;

  @Autowired
  private TranslationService     translationService;

  @Autowired
  private AttachmentService      attachmentService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private LayoutService          layoutService;

  @Autowired
  private PageLayoutService      pageLayoutService;

  @Autowired
  private ContainerLayoutService containerLayoutService;

  @Autowired
  private UserACL                userAcl;

  @Autowired
  private IdentityManager        identityManager;

  private long                   superUserIdentityId;

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
    SectionTemplateDetail sectionTemplate = sectionTemplateService.getSectionTemplate(Long.parseLong(objectId),
                                                                                      Locale.getDefault());

    SectionTemplateDatabind databind = new SectionTemplateDatabind();
    databind.setContent(sectionTemplate.getContent());
    TranslationField translationNameField =
                                          translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                 Long.parseLong(objectId),
                                                                                 SectionTemplateTranslationPlugin.TITLE_FIELD_NAME,
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
                                                 translationService.getTranslationField(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
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
    FileItem file = fileService.getFile(sectionTemplate.getIllustrationId());
    if (file != null) {
      databind.setIllustration(Base64.encodeBase64String(file.getAsByte()));
    }
    LayoutModel sectionLayoutModel = fromJsonString(sectionTemplate.getContent(), LayoutModel.class);
    retrieveBackgroundImages(sectionLayoutModel, fileService);
    databind.setContent(toJsonString(sectionLayoutModel));
    String jsonData = toJsonString(databind);

    Page pageLayout = fromJsonString(sectionTemplate.getContent(), LayoutModel.class).toPage();
    LayoutModel layoutModel = new LayoutModel(pageLayout, portletInstanceService, new PortletInstanceContext(true, null));
    retrieveBackgroundImages(layoutModel, fileService);
    layoutModel.resetStorage();
    String layoutData = toJsonString(layoutModel);

    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + sectionTemplate.getId() + "/" + CONFIG_JSON, jsonData);
    writeToZip(zipOutputStream, OBJECT_TYPE + "-" + sectionTemplate.getId() + "/" + LAYOUT_JSON, layoutData);
  }

  public CompletableFuture<Pair<DatabindReport, File>> deserialize(File zipFile, Map<String, String> params, String username) {
    return CompletableFuture.supplyAsync(() -> importSectionTemplates(zipFile)).thenApply(processedTemplates -> {
      DatabindReport report = new DatabindReport();
      report.setSuccess(!processedTemplates.isEmpty());
      report.setProcessedItems(processedTemplates);
      return Pair.of(report, zipFile);
    });
  }

  @ContainerTransactional
  public List<String> importSectionTemplates(File zipFile) {
    Map<String, SectionTemplateDatabind> instances = extractTemplates(zipFile);
    List<String> processedSectionTemplates = new ArrayList<>();
    for (Map.Entry<String, SectionTemplateDatabind> entry : instances.entrySet()) {
      SectionTemplateDatabind sectionTemplate = entry.getValue();
      processSectionTemplate(sectionTemplate);
      processedSectionTemplates.add(sectionTemplate.getContent());
    }
    return processedSectionTemplates;
  }

  private Map<String, SectionTemplateDatabind> extractTemplates(File zipFile) {
    Map<String, SectionTemplateDatabind> templateDatabindMap = new HashMap<>();

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
        String key = entryName.split("/")[0];

        if (entry.getName().endsWith(CONFIG_JSON)) {
          SectionTemplateDatabind databind = fromJsonString(jsonContent, SectionTemplateDatabind.class);
          if (databind != null) {
            templateDatabindMap.put(key, databind);
          }
        } else if (entry.getName().endsWith(LAYOUT_JSON)) {
          LayoutModel page = fromJsonString(jsonContent, LayoutModel.class);
          if (page != null) {
            SectionTemplateDatabind databind = templateDatabindMap.computeIfAbsent(key, k -> new SectionTemplateDatabind());
            databind.setPage(page);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error reading zip file", e);
    }
    return templateDatabindMap;
  }

  private void saveIllustration(long sectionTemplateId, byte[] illustrationBytes) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(illustrationBytes);
      String uploadId = "SectionTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      attachmentService.deleteAttachments(SectionTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(sectionTemplateId));
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SectionTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(sectionTemplateId),
                                       null,
                                       getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving illustration as attachment for page template '%s'",
                                                    sectionTemplateId),
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
  private void saveNames(SectionTemplateDatabind sectionTemplateDatabind, SectionTemplate sectionTemplate) {
    translationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                             sectionTemplate.getId(),
                                             SectionTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                             sectionTemplateDatabind.getNames()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                              Map.Entry::getValue)));
  }

  @SneakyThrows
  private void saveDescriptions(SectionTemplateDatabind sectionTemplateDatabind, SectionTemplate sectionTemplate) {
    translationService.saveTranslationLabels(SectionTemplateTranslationPlugin.OBJECT_TYPE,
                                             sectionTemplate.getId() == 0 ? null : String.valueOf(sectionTemplate.getId()),
                                             SectionTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                             sectionTemplateDatabind.getDescriptions()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(entry -> Locale.forLanguageTag(entry.getKey()),
                                                                                              Map.Entry::getValue)),
                                             true);
  }

  @SneakyThrows
  @Synchronized
  private void processSectionTemplate(SectionTemplateDatabind sectionTemplateDatabind) {
    SectionTemplate sectionTemplate = new SectionTemplate();
    LayoutModel page = fromJsonString(sectionTemplateDatabind.getContent(), LayoutModel.class);

    if (page != null) {
      page.setChildren(sectionTemplateDatabind.getPage().getChildren());
      saveAppBackgroundImages(RANDOM.nextLong(), page, attachmentService, getSuperUserIdentityId());
      sectionTemplate.setContent(toJsonString(page));
    }
    sectionTemplate.setSystem(false);
    sectionTemplate.setCategory("custom");

    Page systemPage = createSectionTemplateSystemPage(toJsonString(page));

    if (CollectionUtils.isNotEmpty(systemPage.getChildren())) {
      Container section = (Container) ((Container) systemPage.getChildren().getFirst()).getChildren().getFirst();
      containerLayoutService.exportPortletPreferences(section);
      LayoutModel sectionLayoutModel = new LayoutModel(section);
      sectionLayoutModel.resetStorage();
      sectionTemplate.setContent(toJsonString(sectionLayoutModel));
    }

    SectionTemplate createdSectionTemplate = sectionTemplateService.createSectionTemplate(sectionTemplate);
    saveNames(sectionTemplateDatabind, createdSectionTemplate);
    saveDescriptions(sectionTemplateDatabind, createdSectionTemplate);
    if (sectionTemplateDatabind.getIllustration() != null) {
      saveIllustration(createdSectionTemplate.getId(), Base64.decodeBase64(sectionTemplateDatabind.getIllustration()));
    }
  }

  @SneakyThrows
  private void writeToZip(ZipOutputStream zipOutputStream, String filePath, String content) {
    ZipEntry entry = new ZipEntry(filePath);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  private long getSuperUserIdentityId() {
    if (superUserIdentityId == 0) {
      superUserIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(userAcl.getSuperUser()).getId());
    }
    return superUserIdentityId;
  }

  @SneakyThrows
  private Page createSectionTemplateSystemPage(String sectionTemplateContent) {
    LayoutModel layoutModel = fromJsonString(sectionTemplateContent, LayoutModel.class);
    if (layoutModel != null) {
      layoutModel.resetStorage();
    }
    Container parentContainer = new Container();
    parentContainer.setTemplate(EntityMapper.PAGE_LAYOUT_TEMPLATE);
    parentContainer.setChildren(new ArrayList<>());
    parentContainer.getChildren().add(EntityMapper.toModelObject(layoutModel));
    Page systemPage = layoutService.getPage(SECTION_TEMPLATE_EDITOR_SYSTEM_PAGE_KEY);
    if (systemPage == null) {
      systemPage = new Page();
      systemPage.setAccessPermissions(EVERYONE_PERMISSIONS);
      systemPage.setOwnerType(PortalConfig.PORTAL_TYPE);
      systemPage.setOwnerId(GLOBAL_SITE_NAME);
      systemPage.setName("_sectionTemplateEditor");
      systemPage.setTitle("sectionTemplateSystem");
    }
    systemPage.setChildren(new ArrayList<>());
    systemPage.getChildren().add(parentContainer);
    PageKey clonedPageKey = systemPage.getPageKey();
    layoutService.save(new PageContext(clonedPageKey, Utils.toPageState(systemPage)), systemPage);
    pageLayoutService.updatePageLayout(clonedPageKey.format(), systemPage, true, userAcl.getSuperUser());
    pageLayoutService.impersonatePage(clonedPageKey);
    return layoutService.getPage(clonedPageKey);
  }
}
