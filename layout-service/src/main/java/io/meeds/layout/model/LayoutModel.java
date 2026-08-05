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
package io.meeds.layout.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.EntityMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class LayoutModel {

  protected String                        id;

  protected String                        storageId;

  protected String                        storageName;

  protected String                        name;

  protected String                        icon;

  protected String                        template;

  protected String                        factoryId;

  protected String                        title;

  protected String                        description;

  protected String                        width;

  protected String                        height;

  protected String                        cssClass;

  protected String                        borderColor;

  private String                          borderSize;

  private String                          boxShadow;

  private Integer                         marginTop;

  private Integer                         marginBottom;

  private Integer                         marginRight;

  private Integer                         marginLeft;

  private Integer                         radiusTopRight;

  private Integer                         radiusTopLeft;

  private Integer                         radiusBottomRight;

  private Integer                         radiusBottomLeft;

  private String                          backgroundColor;

  private String                          backgroundImage;

  private String                          backgroundEffect;

  private String                          backgroundPosition;

  private String                          backgroundSize;

  private String                          backgroundRepeat;

  private String                          appBackgroundColor;

  private String                          appBackgroundImage;

  private String                          appBackgroundEffect;

  private String                          appBackgroundPosition;

  private String                          appBackgroundSize;

  private String                          appBackgroundRepeat;

  private String                          textTitleColor;

  private String                          textTitleFontSize;

  private String                          textTitleFontWeight;

  private String                          textTitleFontStyle;

  private String                          textHeaderColor;

  private String                          textHeaderFontSize;

  private String                          textHeaderFontWeight;

  private String                          textHeaderFontStyle;

  private String                          textColor;

  private String                          textFontSize;

  private String                          textFontWeight;

  private String                          textFontStyle;

  private String                          textSubtitleColor;

  private String                          textSubtitleFontSize;

  private String                          textSubtitleFontWeight;

  private String                          textSubtitleFontStyle;

  private String                          textTitleBackgroundColor;

  private String                          textTitleBackgroundImage;

  private String                          textTitleBackgroundEffect;

  private String                          textTitleBackgroundPosition;

  private String                          textTitleBackgroundSize;

  private String                          textTitleBackgroundRepeat;

  private String                          textTitleBackgroundPadding;

  private String                          textTitleBackgroundRadius;

  private String                          textHeaderBackgroundColor;

  private String                          textHeaderBackgroundImage;

  private String                          textHeaderBackgroundEffect;

  private String                          textHeaderBackgroundPosition;

  private String                          textHeaderBackgroundSize;

  private String                          textHeaderBackgroundRepeat;

  private String                          textHeaderBackgroundPadding;

  private String                          textHeaderBackgroundRadius;

  private String                          textBackgroundColor;

  private String                          textBackgroundImage;

  private String                          textBackgroundEffect;

  private String                          textBackgroundPosition;

  private String                          textBackgroundSize;

  private String                          textBackgroundRepeat;

  private String                          textBackgroundPadding;

  private String                          textBackgroundRadius;

  private String                          textSubtitleBackgroundColor;

  private String                          textSubtitleBackgroundImage;

  private String                          textSubtitleBackgroundEffect;

  private String                          textSubtitleBackgroundPosition;

  private String                          textSubtitleBackgroundSize;

  private String                          textSubtitleBackgroundRepeat;

  private String                          textSubtitleBackgroundPadding;

  private String                          textSubtitleBackgroundRadius;

  private String[]                        accessPermissions;

  // Specific to container
  private String                          profiles;

  private List<PortletInstancePreference> preferences;

  private List<LayoutModel>               children;

  // Specific to applications
  private String                          contentId;

  private boolean                         showInfoBar;

  private boolean                         showApplicationState = true;

  private boolean                         showApplicationMode  = true;

  // Specific to page
  private String                          editPermission;

  @JsonProperty(access = Access.READ_ONLY)
  private PageKey                         pageKey;

  private String                          ownerType;

  private String                          ownerId;

  private boolean                         showMaxWindow;

  private boolean                         hideSharedLayout;

  private String                          type;

  private String                          link;

  public LayoutModel(ModelObject model) {
    this(model, null, null);
  }

  public LayoutModel(ModelObject model, PortletInstanceService portletInstanceService, PortletInstanceContext portletInstanceContext) {
    init(model, portletInstanceService, portletInstanceContext);
  }

  private void init(ModelObject model, PortletInstanceService portletInstanceService, PortletInstanceContext portletInstanceContext) { // NOSONAR
    ModelStyle cssStyle = model.getCssStyle();
    if (cssStyle != null) {
      this.borderColor = cssStyle.getBorderColor();
      this.borderSize = cssStyle.getBorderSize();
      this.boxShadow = cssStyle.getBoxShadow();
      this.marginTop = cssStyle.getMarginTop();
      this.marginBottom = cssStyle.getMarginBottom();
      this.marginRight = cssStyle.getMarginRight();
      this.marginLeft = cssStyle.getMarginLeft();
      this.radiusTopLeft = cssStyle.getRadiusTopLeft();
      this.radiusBottomRight = cssStyle.getRadiusBottomRight();
      this.radiusBottomLeft = cssStyle.getRadiusBottomLeft();
      this.radiusTopRight = cssStyle.getRadiusTopRight();
      this.radiusTopLeft = cssStyle.getRadiusTopLeft();
      this.radiusBottomRight = cssStyle.getRadiusBottomRight();
      this.radiusBottomLeft = cssStyle.getRadiusBottomLeft();
      this.backgroundColor = cssStyle.getBackgroundColor();
      this.backgroundImage = cssStyle.getBackgroundImage();
      this.backgroundEffect = cssStyle.getBackgroundEffect();
      this.backgroundPosition = cssStyle.getBackgroundPosition();
      this.backgroundSize = cssStyle.getBackgroundSize();
      this.backgroundRepeat = cssStyle.getBackgroundRepeat();
      this.textTitleColor = cssStyle.getTextTitleColor();
      this.textTitleFontSize = cssStyle.getTextTitleFontSize();
      this.textTitleFontWeight = cssStyle.getTextTitleFontWeight();
      this.textTitleFontStyle = cssStyle.getTextTitleFontStyle();
      this.textHeaderColor = cssStyle.getTextHeaderColor();
      this.textHeaderFontSize = cssStyle.getTextHeaderFontSize();
      this.textHeaderFontWeight = cssStyle.getTextHeaderFontWeight();
      this.textHeaderFontStyle = cssStyle.getTextHeaderFontStyle();
      this.textColor = cssStyle.getTextColor();
      this.textFontSize = cssStyle.getTextFontSize();
      this.textFontWeight = cssStyle.getTextFontWeight();
      this.textFontStyle = cssStyle.getTextFontStyle();
      this.textSubtitleColor = cssStyle.getTextSubtitleColor();
      this.textSubtitleFontSize = cssStyle.getTextSubtitleFontSize();
      this.textSubtitleFontWeight = cssStyle.getTextSubtitleFontWeight();
      this.textSubtitleFontStyle = cssStyle.getTextSubtitleFontStyle();
      this.textTitleBackgroundColor = cssStyle.getTextTitleBackgroundColor();
      this.textTitleBackgroundImage = cssStyle.getTextTitleBackgroundImage();
      this.textTitleBackgroundEffect = cssStyle.getTextTitleBackgroundEffect();
      this.textTitleBackgroundPosition = cssStyle.getTextTitleBackgroundPosition();
      this.textTitleBackgroundSize = cssStyle.getTextTitleBackgroundSize();
      this.textTitleBackgroundRepeat = cssStyle.getTextTitleBackgroundRepeat();
      this.textTitleBackgroundPadding = cssStyle.getTextTitleBackgroundPadding();
      this.textTitleBackgroundRadius = cssStyle.getTextTitleBackgroundRadius();
      this.textHeaderBackgroundColor = cssStyle.getTextHeaderBackgroundColor();
      this.textHeaderBackgroundImage = cssStyle.getTextHeaderBackgroundImage();
      this.textHeaderBackgroundEffect = cssStyle.getTextHeaderBackgroundEffect();
      this.textHeaderBackgroundPosition = cssStyle.getTextHeaderBackgroundPosition();
      this.textHeaderBackgroundSize = cssStyle.getTextHeaderBackgroundSize();
      this.textHeaderBackgroundRepeat = cssStyle.getTextHeaderBackgroundRepeat();
      this.textHeaderBackgroundPadding = cssStyle.getTextHeaderBackgroundPadding();
      this.textHeaderBackgroundRadius = cssStyle.getTextHeaderBackgroundRadius();
      this.textBackgroundColor = cssStyle.getTextBackgroundColor();
      this.textBackgroundImage = cssStyle.getTextBackgroundImage();
      this.textBackgroundEffect = cssStyle.getTextBackgroundEffect();
      this.textBackgroundPosition = cssStyle.getTextBackgroundPosition();
      this.textBackgroundSize = cssStyle.getTextBackgroundSize();
      this.textBackgroundRepeat = cssStyle.getTextBackgroundRepeat();
      this.textBackgroundPadding = cssStyle.getTextBackgroundPadding();
      this.textBackgroundRadius = cssStyle.getTextBackgroundRadius();
      this.textSubtitleBackgroundColor = cssStyle.getTextSubtitleBackgroundColor();
      this.textSubtitleBackgroundImage = cssStyle.getTextSubtitleBackgroundImage();
      this.textSubtitleBackgroundEffect = cssStyle.getTextSubtitleBackgroundEffect();
      this.textSubtitleBackgroundPosition = cssStyle.getTextSubtitleBackgroundPosition();
      this.textSubtitleBackgroundSize = cssStyle.getTextSubtitleBackgroundSize();
      this.textSubtitleBackgroundRepeat = cssStyle.getTextSubtitleBackgroundRepeat();
      this.textSubtitleBackgroundPadding = cssStyle.getTextSubtitleBackgroundPadding();
      this.textSubtitleBackgroundRadius = cssStyle.getTextSubtitleBackgroundRadius();
    }

    if (model instanceof PageBody pageBody) {
      this.storageId = pageBody.getStorageId();
      this.storageName = pageBody.getStorageName();
      this.width = pageBody.getWidth();
      this.height = pageBody.getHeight();
      this.cssClass = pageBody.getCssClass();
      this.template = EntityMapper.PAGE_BODY_TEMPLATE;
    } else if (model instanceof Container container) {
      this.id = container.getId();
      this.storageId = container.getStorageId();
      this.storageName = container.getStorageName();
      this.name = container.getName();
      this.icon = container.getIcon();
      this.template = container.getTemplate();
      this.factoryId = container.getFactoryId();
      this.title = container.getTitle();
      this.description = container.getDescription();
      this.width = container.getWidth();
      this.height = container.getHeight();
      this.cssClass = container.getCssClass();
      this.profiles = container.getProfiles();
      this.accessPermissions = container.getAccessPermissions();
      this.children = container.getChildren().stream().map(c -> new LayoutModel(c, portletInstanceService, portletInstanceContext)).toList();

      ApplicationBackgroundStyle appCssStyle = container.getAppBackgroundStyle();
      if (appCssStyle != null) {
        this.appBackgroundColor = appCssStyle.getBackgroundColor();
        this.appBackgroundImage = appCssStyle.getBackgroundImage();
        this.appBackgroundEffect = appCssStyle.getBackgroundEffect();
        this.appBackgroundPosition = appCssStyle.getBackgroundPosition();
        this.appBackgroundSize = appCssStyle.getBackgroundSize();
        this.appBackgroundRepeat = appCssStyle.getBackgroundRepeat();
      }
      if (model instanceof Page page) {
        this.editPermission = page.getEditPermission();
        this.pageKey = page.getPageKey();
        this.ownerType = page.getOwnerType();
        this.ownerId = page.getOwnerId();
        this.showMaxWindow = page.isShowMaxWindow();
        this.hideSharedLayout = page.isHideSharedLayout();
        this.type = page.getType();
        this.link = page.getLink();
      }
    } else if (model instanceof Application application) { // NOSONAR
      this.id = application.getId();
      this.storageId = application.getStorageId();
      this.storageName = application.getStorageName();
      this.icon = application.getIcon();
      this.title = application.getTitle();
      this.description = application.getDescription();
      this.width = application.getWidth();
      this.height = application.getHeight();
      this.cssClass = application.getCssClass();
      this.showInfoBar = application.getShowInfoBar();
      this.showApplicationState = application.getShowApplicationState();
      this.showApplicationMode = application.getShowApplicationMode();
      this.accessPermissions = application.getAccessPermissions();

      ApplicationState state = application.getState();
      if (portletInstanceService != null) {
        portletInstanceService.expandPortletPreferences(application, portletInstanceContext);
        TransientApplicationState transientState = (TransientApplicationState) application.getState();
        this.contentId = transientState.getContentId();
        Portlet portlet = transientState.getContentState();
        this.preferences = portlet == null ? Collections.emptyList()
                                           : StreamSupport.stream(portlet.spliterator(), false)
                                                          .map(p -> new PortletInstancePreference(p.getName(), p.getValue()))
                                                          .toList();
      } else {
        switch (state) {
        case PersistentApplicationState persistentState -> this.storageId = persistentState.getStorageId();
        case CloneApplicationState persistentState -> this.storageId = persistentState.getStorageId();
        case TransientApplicationState transientState -> {
          this.contentId = transientState.getContentId();
          Portlet portlet = transientState.getContentState();
          this.preferences = portlet == null ? Collections.emptyList()
                                             : StreamSupport.stream(portlet.spliterator(), false)
                                                            .map(p -> new PortletInstancePreference(p.getName(), p.getValue()))
                                                            .toList();
        }
        default -> throw new IllegalStateException("PortletInstance should either has a persistent or transient state");
        }
      }
    }
  }

  public void resetStorage() {
    this.storageId = null;
    this.storageName = null;
    if (getChildren() != null && !getChildren().isEmpty()) {
      for (LayoutModel child : getChildren()) {
        child.resetStorage();
      }
    }
  }

  public Page toPage() {
    Page page = new Page(storageId);
    ArrayList<ModelObject> pageContainers = this.children == null ? new ArrayList<>() :
                                                                  this.children.stream()
                                                                               .map(EntityMapper::toModelObject)
                                                                               .filter(Objects::nonNull)
                                                                               .collect(Collectors.toCollection(ArrayList::new));
    page.setChildren(pageContainers);
    return page;
  }

  public PortalConfig toSite() {
    PortalConfig site = new PortalConfig(storageId);
    ModelObject modelObject = this.children == null ? new PageBody() :
                                                    EntityMapper.toModelObject(this);
    if (modelObject instanceof Container container) {
      site.setPortalLayout(container);
    } else {
      Container container = new Container();
      container.setChildren(new ArrayList<>());
      container.getChildren().add(modelObject);
      site.setPortalLayout(container);
    }
    return site;
  }

}
