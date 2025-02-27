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

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceDatabind;
import io.meeds.layout.plugin.translation.PortletInstanceTranslationPlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.JsonUtils;
import io.meeds.social.databind.plugin.DatabindPlugin;
import io.meeds.social.databind.service.DatabindService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortletInstanceDatabindPlugin implements DatabindPlugin {

  public static final String     OBJECT_TYPE = "PortletInstance";

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private DatabindService        databindService;

  @Autowired
  private FileService            fileService;

  @Autowired
  private TranslationService     translationService;

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
    PortletInstance portletInstance = portletInstanceService.getPortletInstance(Long.parseLong(objectId),
                                                                                username,
                                                                                Locale.getDefault(),
                                                                                true);

    PortletInstanceDatabind databind = new PortletInstanceDatabind();
    databind.setContentId(portletInstance.getContentId());
    TranslationField translationNameField =
                                          translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                                                 Long.parseLong(objectId),
                                                                                 PortletInstanceTranslationPlugin.TITLE_FIELD_NAME,
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
                                                 translationService.getTranslationField(PortletInstanceTranslationPlugin.OBJECT_TYPE,
                                                                                        Long.parseLong(objectId),
                                                                                        PortletInstanceTranslationPlugin.DESCRIPTION_FIELD_NAME,
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
    FileItem file = fileService.getFile(portletInstance.getIllustrationId());
    if (file != null) {
      databind.setIllustrationId(Base64.encodeBase64String(file.getAsByte()));
    }
    databind.setPreferences(portletInstance.getPreferences());
    String jsonData = JsonUtils.toJsonString(databind);
    writeContent(zipOutputStream, objectId, jsonData);
  }

  private void writeContent(ZipOutputStream zipOutputStream, String objectId, String content) throws IOException {
    ZipEntry entry = new ZipEntry(String.format("%s_%s.json", OBJECT_TYPE, objectId));
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }
}
