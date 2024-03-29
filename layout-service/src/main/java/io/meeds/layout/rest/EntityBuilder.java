/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.I18N;
import org.exoplatform.portal.mop.State;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.webui.core.model.SelectItemOption;

import io.meeds.layout.rest.model.NodeLabelRestEntity;
import io.meeds.layout.rest.model.PageTemplateRestEntity;
import io.meeds.layout.utils.SiteNavigationUtils;

public class EntityBuilder {

  private EntityBuilder() {
  }

  public static List<PageTemplateRestEntity> toPageTemplateRestEntities(List<SelectItemOption<String>> pageTemplates, Locale userLocal) {
    return pageTemplates.stream().map(pageTemplate -> toPageTemplateRestEntity(pageTemplate, userLocal)).toList();
  }

  public static NodeLabelRestEntity toNodeLabelRestEntity(Map<Locale, State> nodeLabels) {
    LocaleConfigService localeConfigService = CommonsUtils.getService(LocaleConfigService.class);
    Locale defaultLocale = localeConfigService.getDefaultLocaleConfig() == null ? Locale.ENGLISH
                                                                                : localeConfigService.getDefaultLocaleConfig()
                                                                                                     .getLocale();
    String defaultLanguage = defaultLocale.getLanguage();
    Map<String, String> supportedLanguages =
                                           localeConfigService.getLocalConfigs() == null ? Collections.singletonMap(defaultLocale.getLanguage(),
                                                                                                                    defaultLocale.getDisplayName())
                                                                                         : localeConfigService.getLocalConfigs()
                                                                                                              .stream()
                                                                                                              .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocaleName(),
                                                                                                                                                          "ma"))
                                                                                                              .collect(Collectors.toMap(LocaleConfig::getLocaleName,
                                                                                                                                        localeConfig -> localeConfig.getLocale()
                                                                                                                                                                    .getDisplayName()));
    Map<String, String> localized = new HashMap<>();
    NodeLabelRestEntity nodeLabelRestEntity = new NodeLabelRestEntity();
    if (nodeLabels != null && nodeLabels.size() != 0) {
      for (Map.Entry<Locale, State> entry : nodeLabels.entrySet()) {
        Locale locale = entry.getKey();
        State state = entry.getValue();
        localized.put(I18N.toTagIdentifier(locale), state.getName());
      }
      if (!nodeLabels.containsKey(defaultLocale)) {
        localized.put(I18N.toTagIdentifier(defaultLocale), null);
      }
      nodeLabelRestEntity.setLabels(localized);
    }
    nodeLabelRestEntity.setDefaultLanguage(defaultLanguage);
    nodeLabelRestEntity.setSupportedLanguages(supportedLanguages);
    return nodeLabelRestEntity;
  }

  private static PageTemplateRestEntity toPageTemplateRestEntity(SelectItemOption<String> pageTemplate, Locale userLocal) {
    if (pageTemplate == null) {
      return null;
    }
    return new PageTemplateRestEntity(SiteNavigationUtils.getI18NLabel(userLocal, pageTemplate.getLabel()),
                                      pageTemplate.getValue());
  }
}