/**
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
package io.meeds.layout.service.injection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.model.SiteTemplateDescriptor;
import io.meeds.layout.model.SiteTemplateDescriptorList;
import io.meeds.layout.plugin.attachment.SiteTemplateAttachmentPlugin;
import io.meeds.layout.plugin.translation.SiteTemplateTranslationPlugin;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.layout.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SiteTemplateImportService {

  private static final String            SITE_TEMPLATE_IMPORT       = "SITE_TEMPLATE_IMPORT";

  private static final Scope             SITE_TEMPLATE_IMPORT_SCOPE = Scope.APPLICATION.id(SITE_TEMPLATE_IMPORT);

  private static final Context           SITE_TEMPLATE_CONTEXT      = Context.GLOBAL.id("SITE_TEMPLATE");

  private static final String            SITE_TEMPLATE_VERSION      = "version";

  private static final Log               LOG                        =
                                             ExoLogger.getLogger(SiteTemplateImportService.class);

  private static final Random            RANDOM                     = new Random();

  @Autowired
  private LayoutAclService               layoutAclService;

  @Autowired
  private LayoutTranslationImportService layoutTranslationService;

  @Autowired
  private AttachmentService              attachmentService;

  @Autowired
  private SiteTemplateService            siteTemplateService;

  @Autowired
  private SettingService                 settingService;

  @Autowired
  private ConfigurationManager           configurationManager;

  @Value("${meeds.site.templates.import.override:false}")
  private boolean                        forceReimport;

  @Value("${meeds.site.templates.import.version:1}")
  private long                           importVersion;

  @PostConstruct
  public void init() {
    CompletableFuture.runAsync(this::importSiteTemplates);
  }

  @ContainerTransactional
  public void importSiteTemplates() {
    if (!forceReimport && getSettingValue(SITE_TEMPLATE_VERSION) != importVersion) {
      forceReimport = true;
    }
    LOG.info("Importing Site Templates with version {}, force reimport = {}", importVersion, forceReimport);

    ConversationState.setCurrent(layoutAclService.getSuperUserConversationState());
    try {
      Collections.list(getClass().getClassLoader().getResources("site-templates.json"))
                 .stream()
                 .map(this::parseDescriptors)
                 .flatMap(List::stream)
                 .forEach(this::importDescriptor);
      LOG.info("Importing Site Templates finished successfully.");

      LOG.info("Processing Post Site Templates import");
      layoutTranslationService.postImport(SiteTemplateTranslationPlugin.OBJECT_TYPE);
      LOG.info("Processing Post Site Templates import finished");

      setSettingValue(SITE_TEMPLATE_VERSION, importVersion);
    } catch (Exception e) {
      LOG.warn("An error occurred while importing Site Templates", e);
    } finally {
      ConversationState.setCurrent(null);
    }
  }

  protected List<SiteTemplateDescriptor> parseDescriptors(URL url) {
    try (InputStream inputStream = url.openStream()) {
      String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      SiteTemplateDescriptorList list = JsonUtils.fromJsonString(content, SiteTemplateDescriptorList.class);
      return list.getDescriptors();
    } catch (IOException e) {
      LOG.warn("An unkown error happened while parsing Site Templates from url {}", url, e);
      return Collections.emptyList();
    }
  }

  protected void importDescriptor(SiteTemplateDescriptor descriptor) {
    String descriptorId = descriptor.getNameId();
    long existingId = getSettingValue(descriptorId);
    if (forceReimport || existingId == 0) {
      importSiteTemplate(descriptor, existingId);
    } else {
      LOG.debug("Ignore re-importing Site Template {}", descriptorId);
    }
  }

  protected void importSiteTemplate(SiteTemplateDescriptor d, long oldId) {
    String descriptorId = d.getNameId();
    LOG.debug("Importing Site Template {}", descriptorId);
    try {
      SiteTemplate siteTemplate = saveSiteTemplate(d, oldId);
      if (siteTemplate == null) {
        return;
      }
      if (forceReimport || oldId == 0 || siteTemplate.getId() != oldId) {
        LOG.debug("Importing Site Template {} title translations", descriptorId);
        saveNames(d, siteTemplate);
        LOG.debug("Importing Site Template {} description translations", descriptorId);
        saveDescriptions(d, siteTemplate);
        if (StringUtils.isNotBlank(d.getIllustrationPath())) {
          LOG.debug("Importing Site Template {} illustration", descriptorId);
          saveIllustration(siteTemplate.getId(), d.getIllustrationPath());
        }
        // Mark as imported
        setSettingValue(descriptorId, siteTemplate.getId());
      }
      LOG.debug("Importing Site Template {} finished successfully", descriptorId);
    } catch (Exception e) {
      LOG.warn("An error occurred while importing Site Template {}", descriptorId, e);
    }
  }

  protected void saveNames(SiteTemplateDescriptor d, SiteTemplate siteTemplate) {
    layoutTranslationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                   siteTemplate.getId(),
                                                   SiteTemplateTranslationPlugin.TITLE_FIELD_NAME,
                                                   d.getNames(),
                                                   false);
  }

  protected void saveDescriptions(SiteTemplateDescriptor d, SiteTemplate siteTemplate) {
    layoutTranslationService.saveTranslationLabels(SiteTemplateTranslationPlugin.OBJECT_TYPE,
                                                   siteTemplate.getId(),
                                                   SiteTemplateTranslationPlugin.DESCRIPTION_FIELD_NAME,
                                                   d.getDescriptions(),
                                                   true);
  }

  @SneakyThrows
  protected SiteTemplate saveSiteTemplate(SiteTemplateDescriptor d, long oldId) {
    SiteTemplate siteTemplate = null;
    if (oldId > 0) {
      siteTemplate = siteTemplateService.getSiteTemplate(oldId);
    }
    boolean isNew = siteTemplate == null;
    if (isNew) {
      siteTemplate = new SiteTemplate();
    }
    siteTemplate.setIcon(d.getIcon());
    siteTemplate.setLayout(d.getLayout());
    siteTemplate.setSystem(d.isSystem());
    siteTemplate.setDisabled(d.isDisabled());
    if (isNew) {
      return siteTemplateService.createSiteTemplate(siteTemplate);
    } else {
      return siteTemplateService.updateSiteTemplate(siteTemplate);
    }
  }

  protected void saveIllustration(long siteTemplateId, String imagePath) {
    File tempFile = null;
    try {
      tempFile = getIllustrationFile(imagePath);
      String uploadId = "SiteTemplateIllustration" + RANDOM.nextLong();
      UploadResource uploadResource = new UploadResource(uploadId);
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getPath());
      UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
      attachmentService.deleteAttachments(SiteTemplateAttachmentPlugin.OBJECT_TYPE, String.valueOf(siteTemplateId));
      attachmentService.saveAttachment(uploadedAttachmentDetail,
                                       SiteTemplateAttachmentPlugin.OBJECT_TYPE,
                                       String.valueOf(siteTemplateId),
                                       null,
                                       layoutAclService.getSuperUserIdentityId());
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error while saving Image '%s' as attachment for Site Template '%s'",
                                                    imagePath,
                                                    siteTemplateId),
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

  protected void setSettingValue(String name, long value) {
    settingService.set(SITE_TEMPLATE_CONTEXT,
                       SITE_TEMPLATE_IMPORT_SCOPE,
                       name,
                       SettingValue.create(String.valueOf(value)));
  }

  protected long getSettingValue(String name) {
    try {
      SettingValue<?> settingValue = settingService.get(SITE_TEMPLATE_CONTEXT, SITE_TEMPLATE_IMPORT_SCOPE, name);
      return settingValue == null || settingValue.getValue() == null ? 0l : Long.parseLong(settingValue.getValue().toString());
    } catch (NumberFormatException e) {
      return 0l;
    }
  }

  private File getIllustrationFile(String imagePath) throws Exception {
    try (InputStream inputStream = configurationManager.getInputStream(imagePath)) {
      File tempFile = File.createTempFile("temp", ".png");
      FileUtils.copyInputStreamToFile(inputStream, tempFile);
      return tempFile;
    }
  }

}
