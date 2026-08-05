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
package io.meeds.layout.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.entity.PortletInstanceCategoryEntity;
import io.meeds.layout.entity.PortletInstanceEntity;
import io.meeds.layout.entity.SectionTemplateEntity;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PortletDescriptor;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.model.SiteTemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EntityMapper {

  private static final Log   LOG                  = ExoLogger.getLogger(EntityMapper.class);

  public static final String PAGE_BODY_TEMPLATE   = "PageBody";

  public static final String EMPTY_PAGE_TEMPLATE  = "empty";

  public static final String PAGE_LAYOUT_TEMPLATE = "system:/groovy/portal/webui/container/UIPageLayout.gtmpl";

  public static final String SITE_ENABLED_PROP    = "SITE_ENABLED";

  private EntityMapper() {
    // Utils Class
  }

  public static ModelObject toModelObject(LayoutModel layoutModel) { // NOSONAR
    ModelStyle cssStyle = mapToStyle(layoutModel);

    if (StringUtils.equals(layoutModel.getTemplate(), PAGE_BODY_TEMPLATE)) {
      PageBody pageBody = new PageBody();
      pageBody.setStorageId(layoutModel.getStorageId());
      pageBody.setStorageName(layoutModel.getStorageName());
      pageBody.setWidth(layoutModel.getWidth());
      pageBody.setHeight(layoutModel.getHeight());
      pageBody.setCssClass(layoutModel.getCssClass());
      pageBody.setCssStyle(cssStyle);
      return pageBody;
    } else if (StringUtils.isNotBlank(layoutModel.getTemplate())) {
      Container container = new Container(layoutModel.getStorageId());
      container.setId(layoutModel.getId());
      container.setStorageName(layoutModel.getStorageName());
      container.setName(layoutModel.getName());
      container.setIcon(layoutModel.getIcon());
      container.setTemplate(layoutModel.getTemplate());
      container.setFactoryId(layoutModel.getFactoryId());
      container.setTitle(layoutModel.getTitle());
      container.setDescription(layoutModel.getDescription());
      container.setWidth(layoutModel.getWidth());
      container.setHeight(layoutModel.getHeight());
      container.setCssClass(layoutModel.getCssClass());
      container.setProfiles(layoutModel.getProfiles());
      container.setAccessPermissions(layoutModel.getAccessPermissions());
      container.setCssStyle(cssStyle);
      container.setAppBackgroundStyle(mapToAppStyle(layoutModel));
      if (layoutModel.getChildren() != null) {
        container.setChildren(layoutModel.getChildren()
                                         .stream()
                                         .map(EntityMapper::toModelObject)
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toCollection(ArrayList::new)));
      }
      return container;
    } else { // NOSONAR
      Application application = new Application(layoutModel.getStorageId());
      application.setId(layoutModel.getId());
      application.setStorageName(layoutModel.getStorageName());
      application.setIcon(layoutModel.getIcon());
      application.setTitle(layoutModel.getTitle());
      application.setDescription(layoutModel.getDescription());
      application.setWidth(layoutModel.getWidth());
      application.setHeight(layoutModel.getHeight());
      application.setCssClass(layoutModel.getCssClass());
      application.setShowInfoBar(layoutModel.isShowInfoBar());
      application.setShowApplicationState(layoutModel.isShowApplicationState());
      application.setShowApplicationMode(layoutModel.isShowApplicationMode());
      application.setAccessPermissions(layoutModel.getAccessPermissions());
      application.setCssStyle(cssStyle);

      ApplicationState state;
      if (StringUtils.isNotBlank(layoutModel.getStorageId())) {
        state = new PersistentApplicationState(layoutModel.getStorageId());
      } else if (StringUtils.isNotBlank(layoutModel.getContentId())) {
        TransientApplicationState transientState = new TransientApplicationState(layoutModel.getContentId());
        transientState.setOwnerId(layoutModel.getOwnerId());
        transientState.setOwnerType(layoutModel.getOwnerType());
        if (CollectionUtils.isNotEmpty(layoutModel.getPreferences())) {
          Portlet portlet = new Portlet();
          layoutModel.getPreferences()
                     .forEach(p -> portlet.setValue(p.getName(), p.getValue()));
          transientState.setContentState(portlet);
        }
        state = transientState;
      } else {
        LOG.warn("Layout Model {} should have either a ContentId or StorageId. The application will be ignored as child model",
                 layoutModel);
        return null;
      }
      application.setState(state);
      return application;
    }
  }

  private static ModelStyle mapToStyle(LayoutModel layoutModel) {
    ModelStyle cssStyle = null;
    boolean hasStyle = StringUtils.isNotBlank(layoutModel.getBorderColor())
                       || layoutModel.getRadiusTopRight() != null
                       || layoutModel.getRadiusTopLeft() != null
                       || layoutModel.getRadiusBottomLeft() != null
                       || layoutModel.getRadiusBottomRight() != null
                       || layoutModel.getMarginTop() != null
                       || layoutModel.getMarginBottom() != null
                       || layoutModel.getMarginLeft() != null
                       || layoutModel.getMarginRight() != null
                       || StringUtils.isNotBlank(layoutModel.getBorderSize())
                       || StringUtils.isNotBlank(layoutModel.getBoxShadow())
                       || StringUtils.isNotBlank(layoutModel.getBackgroundColor())
                       || StringUtils.isNotBlank(layoutModel.getBackgroundImage())
                       || StringUtils.isNotBlank(layoutModel.getTextTitleColor())
                       || StringUtils.isNotBlank(layoutModel.getTextColor())
                       || StringUtils.isNotBlank(layoutModel.getTextHeaderColor())
                       || StringUtils.isNotBlank(layoutModel.getTextSubtitleColor())
                       || StringUtils.isNotBlank(layoutModel.getTextTitleBackgroundColor())
                       || StringUtils.isNotBlank(layoutModel.getTextTitleBackgroundImage())
                       || StringUtils.isNotBlank(layoutModel.getTextHeaderBackgroundColor())
                       || StringUtils.isNotBlank(layoutModel.getTextHeaderBackgroundImage())
                       || StringUtils.isNotBlank(layoutModel.getTextBackgroundColor())
                       || StringUtils.isNotBlank(layoutModel.getTextBackgroundImage())
                       || StringUtils.isNotBlank(layoutModel.getTextSubtitleBackgroundColor())
                       || StringUtils.isNotBlank(layoutModel.getTextSubtitleBackgroundImage());
    if (hasStyle) {
      cssStyle = new ModelStyle();
      cssStyle.setBorderColor(layoutModel.getBorderColor());
      cssStyle.setBorderSize(layoutModel.getBorderSize());
      cssStyle.setBoxShadow(layoutModel.getBoxShadow());
      cssStyle.setMarginTop(layoutModel.getMarginTop());
      cssStyle.setMarginBottom(layoutModel.getMarginBottom());
      cssStyle.setMarginRight(layoutModel.getMarginRight());
      cssStyle.setMarginLeft(layoutModel.getMarginLeft());
      cssStyle.setRadiusTopRight(layoutModel.getRadiusTopRight());
      cssStyle.setRadiusTopLeft(layoutModel.getRadiusTopLeft());
      cssStyle.setRadiusBottomRight(layoutModel.getRadiusBottomRight());
      cssStyle.setRadiusBottomLeft(layoutModel.getRadiusBottomLeft());
      cssStyle.setBackgroundColor(layoutModel.getBackgroundColor());
      cssStyle.setBackgroundImage(layoutModel.getBackgroundImage());
      cssStyle.setBackgroundEffect(layoutModel.getBackgroundEffect());
      cssStyle.setBackgroundPosition(layoutModel.getBackgroundPosition());
      cssStyle.setBackgroundSize(layoutModel.getBackgroundSize());
      cssStyle.setBackgroundRepeat(layoutModel.getBackgroundRepeat());
      cssStyle.setTextTitleColor(layoutModel.getTextTitleColor());
      cssStyle.setTextTitleFontSize(layoutModel.getTextTitleFontSize());
      cssStyle.setTextTitleFontWeight(layoutModel.getTextTitleFontWeight());
      cssStyle.setTextTitleFontStyle(layoutModel.getTextTitleFontStyle());
      cssStyle.setTextHeaderColor(layoutModel.getTextHeaderColor());
      cssStyle.setTextHeaderFontSize(layoutModel.getTextHeaderFontSize());
      cssStyle.setTextHeaderFontWeight(layoutModel.getTextHeaderFontWeight());
      cssStyle.setTextHeaderFontStyle(layoutModel.getTextHeaderFontStyle());
      cssStyle.setTextColor(layoutModel.getTextColor());
      cssStyle.setTextFontSize(layoutModel.getTextFontSize());
      cssStyle.setTextFontWeight(layoutModel.getTextFontWeight());
      cssStyle.setTextFontStyle(layoutModel.getTextFontStyle());
      cssStyle.setTextSubtitleColor(layoutModel.getTextSubtitleColor());
      cssStyle.setTextSubtitleFontSize(layoutModel.getTextSubtitleFontSize());
      cssStyle.setTextSubtitleFontWeight(layoutModel.getTextSubtitleFontWeight());
      cssStyle.setTextSubtitleFontStyle(layoutModel.getTextSubtitleFontStyle());
      cssStyle.setTextTitleBackgroundColor(layoutModel.getTextTitleBackgroundColor());
      cssStyle.setTextTitleBackgroundImage(layoutModel.getTextTitleBackgroundImage());
      cssStyle.setTextTitleBackgroundEffect(layoutModel.getTextTitleBackgroundEffect());
      cssStyle.setTextTitleBackgroundPosition(layoutModel.getTextTitleBackgroundPosition());
      cssStyle.setTextTitleBackgroundSize(layoutModel.getTextTitleBackgroundSize());
      cssStyle.setTextTitleBackgroundRepeat(layoutModel.getTextTitleBackgroundRepeat());
      cssStyle.setTextTitleBackgroundPadding(layoutModel.getTextTitleBackgroundPadding());
      cssStyle.setTextTitleBackgroundRadius(layoutModel.getTextTitleBackgroundRadius());
      cssStyle.setTextHeaderBackgroundColor(layoutModel.getTextHeaderBackgroundColor());
      cssStyle.setTextHeaderBackgroundImage(layoutModel.getTextHeaderBackgroundImage());
      cssStyle.setTextHeaderBackgroundEffect(layoutModel.getTextHeaderBackgroundEffect());
      cssStyle.setTextHeaderBackgroundPosition(layoutModel.getTextHeaderBackgroundPosition());
      cssStyle.setTextHeaderBackgroundSize(layoutModel.getTextHeaderBackgroundSize());
      cssStyle.setTextHeaderBackgroundRepeat(layoutModel.getTextHeaderBackgroundRepeat());
      cssStyle.setTextHeaderBackgroundPadding(layoutModel.getTextHeaderBackgroundPadding());
      cssStyle.setTextHeaderBackgroundRadius(layoutModel.getTextHeaderBackgroundRadius());
      cssStyle.setTextBackgroundColor(layoutModel.getTextBackgroundColor());
      cssStyle.setTextBackgroundImage(layoutModel.getTextBackgroundImage());
      cssStyle.setTextBackgroundEffect(layoutModel.getTextBackgroundEffect());
      cssStyle.setTextBackgroundPosition(layoutModel.getTextBackgroundPosition());
      cssStyle.setTextBackgroundSize(layoutModel.getTextBackgroundSize());
      cssStyle.setTextBackgroundRepeat(layoutModel.getTextBackgroundRepeat());
      cssStyle.setTextBackgroundPadding(layoutModel.getTextBackgroundPadding());
      cssStyle.setTextBackgroundRadius(layoutModel.getTextBackgroundRadius());
      cssStyle.setTextSubtitleBackgroundColor(layoutModel.getTextSubtitleBackgroundColor());
      cssStyle.setTextSubtitleBackgroundImage(layoutModel.getTextSubtitleBackgroundImage());
      cssStyle.setTextSubtitleBackgroundEffect(layoutModel.getTextSubtitleBackgroundEffect());
      cssStyle.setTextSubtitleBackgroundPosition(layoutModel.getTextSubtitleBackgroundPosition());
      cssStyle.setTextSubtitleBackgroundSize(layoutModel.getTextSubtitleBackgroundSize());
      cssStyle.setTextSubtitleBackgroundRepeat(layoutModel.getTextSubtitleBackgroundRepeat());
      cssStyle.setTextSubtitleBackgroundPadding(layoutModel.getTextSubtitleBackgroundPadding());
      cssStyle.setTextSubtitleBackgroundRadius(layoutModel.getTextSubtitleBackgroundRadius());
    }
    return cssStyle;
  }

  private static ApplicationBackgroundStyle mapToAppStyle(LayoutModel layoutModel) {
    ApplicationBackgroundStyle cssStyle = null;
    if (StringUtils.isNotBlank(layoutModel.getAppBackgroundColor())
        || StringUtils.isNotBlank(layoutModel.getAppBackgroundImage())) {
      cssStyle = new ApplicationBackgroundStyle();
      cssStyle.setBackgroundColor(layoutModel.getAppBackgroundColor());
      cssStyle.setBackgroundImage(layoutModel.getAppBackgroundImage());
      cssStyle.setBackgroundEffect(layoutModel.getAppBackgroundEffect());
      cssStyle.setBackgroundPosition(layoutModel.getAppBackgroundPosition());
      cssStyle.setBackgroundSize(layoutModel.getAppBackgroundSize());
      cssStyle.setBackgroundRepeat(layoutModel.getAppBackgroundRepeat());
    }
    return cssStyle;
  }

  public static PortletInstanceCategory fromEntity(PortletInstanceCategoryEntity entity) {
    return new PortletInstanceCategory(entity.getId(),
                                       null,
                                       entity.getIcon(),
                                       entity.isSystem(),
                                       entity.getPermissions()
                                           == null ? Collections.emptyList() :
                                                   entity.getPermissions().stream().filter(StringUtils::isNotBlank).toList());
  }

  public static PortletInstance fromEntity(PortletInstanceEntity entity, PortletDescriptor portlet) {
    return new PortletInstance(entity.getId(),
                               null,
                               null,
                               entity.getCategoryId(),
                               entity.getContentId(),
                               0,
                               getPreferences(entity),
                               0l,
                               entity.getPermissions()
                                   == null ? Collections.emptyList() :
                                           entity.getPermissions().stream().filter(StringUtils::isNotBlank).toList(),
                               portlet == null ? null : portlet.getSupportedModes(),
                               entity.isSystem(),
                               entity.isDisabled(),
                               false);
  }

  public static PortletInstanceCategoryEntity toEntity(PortletInstanceCategory instance) {
    return new PortletInstanceCategoryEntity(instance.getId() == 0 ? null : instance.getId(),
                                             instance.getPermissions(),
                                             instance.getIcon(),
                                             instance.isSystem());
  }

  public static PortletInstanceEntity toEntity(PortletInstance instance) {
    return new PortletInstanceEntity(instance.getId() == 0 ? null : instance.getId(),
                                     instance.getCategoryId(),
                                     instance.getContentId(),
                                     instance.getPermissions(),
                                     getPreferencesString(instance),
                                     instance.isSystem(),
                                     instance.isDisabled(),
                                     Objects.hash(instance.getCategoryId(),
                                                  getPreferencesString(instance),
                                                  instance.getContentId()));
  }

  public static SectionTemplateEntity toEntity(SectionTemplate sectionTemplate) {
    return new SectionTemplateEntity(sectionTemplate.getId() == 0 ? null : sectionTemplate.getId(),
                                     sectionTemplate.getCategory(),
                                     sectionTemplate.getContent(),
                                     sectionTemplate.isSystem(),
                                     sectionTemplate.isDisabled());
  }

  public static SectionTemplate fromEntity(SectionTemplateEntity entity) {
    return new SectionTemplate(entity.getId(),
                               entity.getCategory(),
                               entity.getContent(),
                               entity.isSystem(),
                               entity.isDisabled());
  }

  public static SiteTemplate toSiteTemplate(PortalConfig portalConfig) {
    SiteTemplate siteTemplate = new SiteTemplate();
    siteTemplate.setId(portalConfig.getId());
    siteTemplate.setLayout(portalConfig.getName());
    siteTemplate.setIcon(portalConfig.getIcon());
    siteTemplate.setSystem(!portalConfig.isRemovable());
    siteTemplate.setDisabled(StringUtils.equals(portalConfig.getProperty(SITE_ENABLED_PROP), "false"));
    return siteTemplate;
  }

  private static String getPreferencesString(PortletInstance instance) {
    if (CollectionUtils.isNotEmpty(instance.getPreferences())) {
      return JsonUtils.toJsonString(new PortletInstancePreferences(instance.getPreferences()));
    }
    return null;
  }

  private static List<PortletInstancePreference> getPreferences(PortletInstanceEntity entity) {
    if (StringUtils.isNotBlank(entity.getPreferences())) {
      return JsonUtils.fromJsonString(entity.getPreferences(), PortletInstancePreferences.class).getPreferences();
    }
    return Collections.emptyList();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PortletInstancePreferences {
    private List<PortletInstancePreference> preferences;
  }

}
