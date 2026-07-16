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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.portal.config.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.javascript.jscomp.jarjar.com.google.re2j.Pattern;

import org.exoplatform.commons.addons.AddOnService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.util.EntityMapper;
import io.meeds.layout.util.JsonUtils;

import lombok.SneakyThrows;

@Service
public class PageLayoutService {

  /** Broadcast after a page's layout has been persisted (content added/removed/reordered). */
  public static final String     PAGE_UPDATED_EVENT              = "layout.page.updated";

  /** Broadcast after a page's access/edit permissions have been persisted. */
  public static final String     PAGE_PERMISSIONS_UPDATED_EVENT  = "layout.page.permissions.updated";

  private static final String    ADDON_CONTAINER_FACTORY_ID      = "addonContainer";

  private static final Log       LOG                             = ExoLogger.getLogger(PageLayoutService.class);

  private static final Pattern   GENERIC_STYLE_MATCHER_VALIDATOR = Pattern.compile("[#0-9a-zA-Z\\(\\),\\./\"'\\-%_ ]+");

  private static final String    PAGE_NOT_EXISTS_MESSAGE         = "Page with key %s doesn't exist";

  private static final String    PAGE_NOT_ACCESSIBLE_MESSAGE     = "Page with ref %s isn't accessible for user %s";

  private static final String    PAGE_NOT_EDITABLE_MESSAGE       = "Page with ref %s isn't editable for user %s";

  @Autowired
  private LayoutService          layoutService;

  @Autowired
  private LayoutAclService       aclService;

  @Autowired
  private ContainerLayoutService containerLayoutService;

  @Autowired
  private PageTemplateService    pageTemplateService;

  @Autowired
  private PortletInstanceService portletInstanceService;

  @Autowired
  private AddOnService           addOnService;

  @Autowired
  private UserPortalConfigService portalConfigService;

  @Autowired
  private ListenerService        listenerService;

  public List<PageContext> getPages(String siteTypeName,
                                    String siteName,
                                    String pageDisplayName,
                                    int offset,
                                    int limit,
                                    String username) {
    SiteType siteType = null;
    if (StringUtils.isNotBlank(siteTypeName)) {
      siteType = SiteType.valueOf(siteTypeName.toUpperCase());
    }
    QueryResult<PageContext> queryResult = layoutService.findPages(offset, limit, siteType, siteName, null, pageDisplayName);
    List<PageContext> pages = new ArrayList<>();
    queryResult.iterator().forEachRemaining(pages::add);
    return pages.stream()
                .filter(p -> aclService.canViewPage(p.getKey(), username))
                .toList();
  }

  public PageContext getPage(PageKey pageKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    PageContext page = layoutService.getPageContext(pageKey);
    if (page == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_ACCESSIBLE_MESSAGE, pageKey, username));
    } else if (!aclService.canViewPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_ACCESSIBLE_MESSAGE, pageKey, username));
    }
    return page;
  }

  public Application getPageApplicationLayout(PageKey pageKey,
                                              long applicationId,
                                              String username) throws ObjectNotFoundException,
                                                               IllegalAccessException {
    ModelObject page = getPageLayout(pageKey, username);
    Application application = findApplication(page, applicationId);
    if (application == null) {
      throw new ObjectNotFoundException(String.format("Application with id %s wasn't found in page %s",
                                                      applicationId,
                                                      pageKey));
    } else if (!aclService.hasAccessPermission(application, username)) {
      throw new IllegalAccessException(String.format("Application with id %s access denied", applicationId));
    }
    portletInstanceService.expandPortletPreferences(application, null);
    return application;
  }

  public void impersonateSite(SiteKey siteKey) {
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    impersonateModel(portalConfig.getPortalLayout(), null);
    List<PageContext> pages = layoutService.findPages(siteKey);
    if (CollectionUtils.isNotEmpty(pages)) {
      pages.forEach(page -> impersonatePage(page.getKey()));
    }
  }

  public void impersonatePage(PageKey pageKey) {
    Page page = getPageLayout(pageKey);
    impersonateModel(page, page);
  }

  public ModelObject getPageLayout(PageKey pageKey, String username) throws ObjectNotFoundException, IllegalAccessException {
    return getPageLayout(pageKey, 0, false, username);
  }

  public ModelObject getPageLayout(PageKey pageKey,
                                   long siteId,
                                   String username) throws ObjectNotFoundException,
                                                    IllegalAccessException {
    return getPageLayout(pageKey, siteId, false, username);
  }

  public ModelObject getPageLayout(PageKey pageKey,
                                   long siteId,
                                   boolean impersonate,
                                   String username) throws ObjectNotFoundException,
                                                    IllegalAccessException {
    Container page = getPageLayout(pageKey);
    if (page == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_ACCESSIBLE_MESSAGE, pageKey, username));
    } else if (!aclService.canViewPage(pageKey, username)
               || !aclService.canViewSite(pageKey.getSite(), username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_ACCESSIBLE_MESSAGE, pageKey, username));
    }
    if (siteId > 0) {
      PortalConfig site = layoutService.getPortalConfig(siteId);
      if (site == null) {
        throw new ObjectNotFoundException(String.format("Site width id %s not found", siteId));
      }
      Container portalLayout = site.getPortalLayout();
      replacePageBody(portalLayout, page);
      page = portalLayout;
    }
    if (impersonate) {
      impersonateModel(page);
    }
    return filterByPermission(page, username);
  }

  public Page getPageLayout(PageKey pageKey) {
    Page page = layoutService.getPage(pageKey);
    if (page != null) {
      expandAddonContainerChildren(page);
      if (replaceAddonContainerChildren(page)) {
        layoutService.save(page);
        page = layoutService.getPage(pageKey);
      }
    }
    return page;
  }

  @SneakyThrows
  public PageContext createPage(PageCreateModel pageModel, String username) throws ObjectNotFoundException,
                                                                            IllegalAccessException,
                                                                            IllegalArgumentException {
    SiteKey siteKey = new SiteKey(pageModel.getPageSiteType(), pageModel.getPageSiteName());
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site with key %s doesn't exist", siteKey));
    } else if (!aclService.canEditNavigation(siteKey, username)) {
      throw new IllegalAccessException();
    }

    String pageName = pageModel.getPageName() == null ? UUID.randomUUID().toString() :
                                                      pageModel.getPageName() + "_" + UUID.randomUUID();

    Page page = createPageInstance(pageModel.getPageSiteType(),
                                   pageModel.getPageSiteName(),
                                   pageName,
                                   getPageType(pageModel.getPageType()),
                                   pageModel.getPageTemplateId(),
                                   pageModel.getLink());

    page.setName(pageName);
    page.setTitle(pageModel.getPageTitle());
    String[] accessPermissions = pageModel.getAccessPermissions() == null ?
                                                                          portalConfig.getAccessPermissions() :
                                                                          pageModel.getAccessPermissions();
    page.setAccessPermissions(accessPermissions);
    String editPermission = pageModel.getEditPermission() == null ?
                                                                  portalConfig.getEditPermission() :
                                                                  pageModel.getEditPermission();
    page.setEditPermission(editPermission);
    validateCSSInputs(page);
    layoutService.save(new PageContext(page.getPageKey(), Utils.toPageState(page)), page);
    return layoutService.getPageContext(page.getPageKey());
  }

  public PageKey clonePage(PageKey pageKey, String username) throws IllegalAccessException,
                                                             ObjectNotFoundException {
    Page page = getPageLayout(pageKey);
    if (page == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey.format()));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_EDITABLE_MESSAGE, pageKey.format(), username));
    }
    // Copy applications into a dedicated application container
    // to seperate lifecycle of original page and cloned page
    // thus generate a dedicated storage identifier for
    // applications and thus a dedicated portlet preferences
    page.resetStorage();
    page.setName(page.getName() + "_draft_" + username);
    page.setTitle(page.getTitle() + " Draft " + username);

    layoutService.save(new PageContext(page.getPageKey(), Utils.toPageState(page)), page);
    return page.getPageKey();
  }

  public void cloneSection(PageKey pageKey, long containerId, String username) throws IllegalAccessException,
                                                                               ObjectNotFoundException {
    Container container = containerLayoutService.findContainer(pageKey, containerId);
    if (container == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey.format()));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_EDITABLE_MESSAGE, pageKey.format(), username));
    }
    Page page = getPageLayout(pageKey);
    Container parentContainer = findParentContainer(page, String.valueOf(containerId));
    Container clonedContainer = containerLayoutService.cloneContainer(container);
    int containerIndex = parentContainer.getChildren()// NOSONAR
                                        .stream()
                                        .map(ModelObject::getStorageId)
                                        .toList()
                                        .indexOf(container.getStorageId());
    parentContainer.getChildren().add(containerIndex + 1, clonedContainer);
    layoutService.save(new PageContext(page.getPageKey(), Utils.toPageState(page)), page);
  }

  public void updatePageApplicationPreferences(PageKey pageKey,
                                               long applicationId,
                                               List<PortletInstancePreference> preferences,
                                               String username) throws IllegalAccessException, ObjectNotFoundException {
    updatePageApplicationPreferences(pageKey, applicationId, preferences, username, false);

  }
  public void updatePageApplicationPreferences(PageKey pageKey,
                                               long applicationId,
                                               List<PortletInstancePreference> preferences,
                                               String username,
                                               boolean keepExisting) throws IllegalAccessException, ObjectNotFoundException {
    Page page = layoutService.getPage(pageKey);
    if (page == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey.format()));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_EDITABLE_MESSAGE, pageKey.format(), username));
    }
    Application application = findApplication(page, applicationId);
    if (application == null) {
      throw new ObjectNotFoundException(String.format("Application with id %s wasn't found in page %s",
                                                      applicationId,
                                                      pageKey));
    }
    Portlet portletPreferences = new Portlet();
    if (keepExisting) {
      portletPreferences = layoutService.load(application.getState());
      if (portletPreferences == null) {
        portletPreferences = new Portlet();
      }
    }
    for (PortletInstancePreference preference : preferences) {
      portletPreferences.setValue(preference.getName(), preference.getValue());
    }

    layoutService.save(application.getState(), portletPreferences);
  }

  public PageContext updatePageLayout(String pageRef,
                                      Page page,
                                      boolean publish,
                                      String username) throws IllegalAccessException,
                                                       ObjectNotFoundException {
    // Security and existence check
    PageKey pageKey = PageKey.parse(pageRef);
    Page existingPage = layoutService.getPage(pageKey);
    if (existingPage == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey.format()));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_EDITABLE_MESSAGE, pageKey.format(), username));
    }

    try {
      if (publish) {
        // Update Page Layout only without resetting preferences
        page.resetStorage();
      } else {
        // Check Page Layout consistency before saving
        page.checkStorage();
      }
    } catch (ObjectNotFoundException e) {
      LOG.debug("Error while accessing page applications storage information", e);
      throw new IllegalStateException("layout.pageOutdatedError");
    }
    validateCSSInputs(page);
    existingPage.setChildren(page.getChildren());
    layoutService.save(existingPage);
    listenerService.broadcast(PAGE_UPDATED_EVENT, username, pageKey.format());
    return layoutService.getPageContext(existingPage.getPageKey());
  }

  /**
   * Restores a single page's layout to its shipped default, preserving the page's current access
   * and edit permissions (only the layout is reset, not who can see/edit the page).
   *
   * @param pageKey the key of the page to restore
   * @param username the user requesting the restore
   * @return the restored {@link PageContext}
   * @throws ObjectNotFoundException when the page doesn't exist
   * @throws IllegalAccessException when the user can't edit the page's layout
   * @throws IllegalStateException when the page isn't part of any default/product configuration
   */
  public PageContext restorePageLayout(PageKey pageKey, String username) throws ObjectNotFoundException,
                                                                          IllegalAccessException {
    PageContext existingPageContext = layoutService.getPageContext(pageKey);
    if (existingPageContext == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey.format()));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException(String.format(PAGE_NOT_EDITABLE_MESSAGE, pageKey.format(), username));
    }
    PageState previousState = existingPageContext.getState();
    boolean restored = portalConfigService.restorePage(pageKey, ImportMode.RESTORE_DEFAULTS);
    if (!restored) {
      throw new IllegalStateException(String.format("Page %s can't be restored since it isn't a default page",
                                                     pageKey.format()));
    }
    PageContext restoredPageContext = layoutService.getPageContext(pageKey);
    PageState restoredState = restoredPageContext.getState();
    restoredState.setAccessPermissions(previousState.getAccessPermissions());
    restoredState.setEditPermission(previousState.getEditPermission());
    restoredPageContext.setState(restoredState);
    layoutService.save(restoredPageContext);
    return restoredPageContext;
  }

  public void updatePageLink(PageKey pageKey,
                             String link,
                             String username) throws ObjectNotFoundException,
                                              IllegalAccessException,
                                              IllegalStateException {
    PageContext pageContext = layoutService.getPageContext(pageKey);
    if (pageContext == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException();
    }
    PageState pageState = pageContext.getState();
    PageType pageType = getPageType(pageState.getType());
    if (pageType != PageType.LINK) {
      throw new IllegalStateException(String.format("Page %s isn't of type 'LINK'", pageKey));
    }
    pageState.setType(pageState.getType());
    pageState.setLink(link);
    pageContext.setState(pageState);
    layoutService.save(pageContext);
  }

  public void updatePagePermissions(PageKey pageKey,
                                    PermissionUpdateModel permissionUpdateModel,
                                    String username) throws ObjectNotFoundException,
                                                     IllegalAccessException {
    PageContext pageContext = layoutService.getPageContext(pageKey);
    if (pageContext == null) {
      throw new ObjectNotFoundException(String.format(PAGE_NOT_EXISTS_MESSAGE, pageKey));
    } else if (!aclService.canEditPage(pageKey, username)) {
      throw new IllegalAccessException();
    }
    PageState pageState = pageContext.getState();
    pageState.setAccessPermissions(permissionUpdateModel.getAccessPermissions());
    pageState.setEditPermission(permissionUpdateModel.getEditPermission());
    layoutService.save(pageContext);
    listenerService.broadcast(PAGE_PERMISSIONS_UPDATED_EVENT, username, pageKey.format());
  }

  public void impersonateModel(ModelObject object) {
    if (object instanceof Container container) {
      ArrayList<ModelObject> children = container.getChildren();
      if (CollectionUtils.isNotEmpty(children)) {
        children.forEach(this::impersonateModel);
      }
    } else if (object instanceof Application application) {
      Portlet preferences = portletInstanceService.getApplicationPortletPreferences(application);
      application.setState(new TransientApplicationState(layoutService.getId(application.getState()), preferences));
    }
  }

  @SneakyThrows
  private Page createPageInstance(String siteType,
                                  String siteName,
                                  String pageName,
                                  PageType pageType,
                                  Long pageTemplateId,
                                  String pageLink) throws IllegalArgumentException {
    return switch (pageType) {
    case PAGE: {
      if (pageTemplateId == null) {
        throw new IllegalArgumentException("pageTemplateId is mandatory");
      }
      PageTemplate pageTemplate = pageTemplateService.getPageTemplate(pageTemplateId);
      if (pageTemplate == null) {
        throw new ObjectNotFoundException("pageTemplate not found");
      }
      if (pageTemplate.isDisabled()) {
        throw new IllegalArgumentException("pageTemplate with designated Id is disabled");
      }
      Page page = new Page(siteType, siteName, pageName);
      Page pageLayout = JsonUtils.fromJsonString(pageTemplate.getContent(), LayoutModel.class)
                                 .toPage();
      page.setChildren(pageLayout.getChildren());
      page.resetStorage();
      page.setType(pageType.name());
      yield page;
    }
    case LINK: {
      Page page = new Page(siteType, siteName, pageName);
      page.setLink(pageLink);
      page.setType(pageType.name());
      yield page;
    }
    default:
      // Unreachable but kept in case of behavior changes
      throw new IllegalArgumentException("pageType is mandatory");
    };
  }

  private PageType getPageType(String pageType) {
    return StringUtils.isBlank(pageType) ? PageType.PAGE : PageType.valueOf(pageType.toUpperCase());
  }

  private void expandAddonContainerChildren(Container container) {
    if (StringUtils.equals(container.getFactoryId(), ADDON_CONTAINER_FACTORY_ID)) {
      List<Application> applications = addOnService.getApplications(container.getName());
      if (CollectionUtils.isNotEmpty(applications)) {
        container.setChildren(new ArrayList<>(applications));
      }
    } else if (container.getChildren() != null) {
      container.getChildren()
               .stream()
               .filter(Objects::nonNull)
               .filter(Container.class::isInstance)
               .map(Container.class::cast)
               .forEach(this::expandAddonContainerChildren);
    }
  }

  private boolean replaceAddonContainerChildren(Container container) {
    ArrayList<ModelObject> subContainers = container.getChildren();
    if (subContainers == null) {
      return false;
    }
    boolean replaced = false;
    LinkedHashMap<Integer, List<Application>> addonContainerChildren = new LinkedHashMap<>();
    for (int i = subContainers.size() - 1; i >= 0; i--) {
      ModelObject modelObject = subContainers.get(i);
      if (modelObject instanceof Container subContainer) {
        if (StringUtils.equals(subContainer.getFactoryId(), ADDON_CONTAINER_FACTORY_ID)) {
          List<Application> applications = addOnService.getApplications(subContainer.getName());
          if (CollectionUtils.isNotEmpty(applications)) {
            addonContainerChildren.put(i, applications);
          }
        } else {
          if (replaceAddonContainerChildren(subContainer)) {
            replaced = true;
          }
        }
      }
    }
    if (!addonContainerChildren.isEmpty()) {
      addonContainerChildren.forEach((index, applications) -> {
        subContainers.remove(index.intValue());
        subContainers.addAll(index, applications);
      });
      container.setChildren(subContainers);
      replaced = true;
    }
    return replaced;
  }

  private void validateCSSInputs(ModelObject modelObject) { // NOSONAR
    ModelStyle cssStyle = modelObject.getCssStyle();
    Arrays.asList(modelObject.getHeight(),
                  modelObject.getWidth(),
                  cssStyle == null ? null : cssStyle.getBorderColor(),
                  cssStyle == null ? null : cssStyle.getBorderSize(),
                  cssStyle == null ? null : cssStyle.getBoxShadow(),
                  cssStyle == null ? null : cssStyle.getBackgroundColor(),
                  cssStyle == null ? null : cssStyle.getBackgroundImage(),
                  cssStyle == null ? null : cssStyle.getBackgroundEffect(),
                  cssStyle == null ? null : cssStyle.getBackgroundPosition(),
                  cssStyle == null ? null : cssStyle.getBackgroundSize(),
                  cssStyle == null ? null : cssStyle.getBackgroundRepeat(),
                  cssStyle == null ? null : cssStyle.getTextTitleColor(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontSize(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextColor(),
                  cssStyle == null ? null : cssStyle.getTextFontSize(),
                  cssStyle == null ? null : cssStyle.getTextFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextHeaderColor(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontSize(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleColor(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontSize(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontStyle())
          .forEach(this::validateCSSStyleValue);
    if (modelObject instanceof Container container && !CollectionUtils.isEmpty(container.getChildren())) {
      container.getChildren().forEach(this::validateCSSInputs);
    }
  }

  private void validateCSSStyleValue(String value) {
    if (StringUtils.isNotBlank(value)
        && (!GENERIC_STYLE_MATCHER_VALIDATOR.matches(value)
            || value.contains("javascript")
            || value.contains("eval"))) {
      throw new IllegalArgumentException(String.format("Invalid css value input %s",
                                                       value));
    }
  }

  private ModelObject filterByPermission(ModelObject modelObject, String username) {
    if (!aclService.hasAccessPermission(modelObject, username)) {
      return null;
    } else if (modelObject instanceof Container container && CollectionUtils.isNotEmpty(container.getChildren())) {
      container.setChildren(container.getChildren()
                                     .stream()
                                     .map(c -> filterByPermission(c, username))
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toCollection(ArrayList::new)));
    }
    return modelObject;
  }

  @SneakyThrows
  private void impersonateModel(ModelObject object, Page page) {
    if (object instanceof Container container
        // Keep the addonContainer reference instead of storing its children
        && !StringUtils.equals(container.getFactoryId(), ADDON_CONTAINER_FACTORY_ID)) {
      ArrayList<ModelObject> children = container.getChildren();
      try {
        containerLayoutService.impersonateContainer(container, page);
      } catch (Exception e) {
        LOG.warn("Error while impersonating container '{}' in page '{}'. Ignore cloning container background image.",
                 container.getStorageId(),
                 page.getStorageId(),
                 e);
      }
      if (CollectionUtils.isNotEmpty(children)) {
        children.forEach(c -> impersonateModel(c, page));
      }
    } else if (object instanceof Application application) {
      ModelStyle cssStyle = object.getCssStyle();
      if (cssStyle != null && StringUtils.isNotBlank(cssStyle.getBackgroundImage())) {
        String clonedBackgroundImageUrl = null;
        try {
          clonedBackgroundImageUrl = containerLayoutService.cloneBackgroundUrl(object, page, cssStyle.getBackgroundImage());
        } finally {
          cssStyle.setBackgroundImage(clonedBackgroundImageUrl);
        }
      }
      Portlet preferences = portletInstanceService.getApplicationPortletPreferences(application);
      if (preferences != null && StringUtils.isNotBlank(application.getStorageId())) {
        layoutService.save(application.getState(), preferences);
      }
    }
  }

  private void replacePageBody(Container portalLayout, Container page) {
    if (portalLayout == null || CollectionUtils.isEmpty(portalLayout.getChildren())) {
      return;
    }
    int index = -1;
    ArrayList<ModelObject> children = portalLayout.getChildren();
    for (int i = 0; i < children.size(); i++) {
      ModelObject modelObject = children.get(i);
      if (modelObject instanceof PageBody) {
        index = i;
        break;
      } else if (modelObject instanceof Container container) {
        replacePageBody(container, page);
      }
    }
    if (index >= 0) {
      children.remove(index);
      Container pageLayout = getPageBody(page);
      if (pageLayout == null) {
        pageLayout = page;
      }
      if (pageLayout != null) {
        pageLayout.setTemplate(EntityMapper.PAGE_BODY_TEMPLATE);
        children.add(index, pageLayout);
      } else {
        children.add(index, page);
      }
    }
  }

  private Container getPageBody(ModelObject page) {
    if (page == null || !(page instanceof Container container)) {
      return null;
    } else if (StringUtils.equals(container.getTemplate(), EntityMapper.PAGE_LAYOUT_TEMPLATE)) {
      return container;
    } else if (CollectionUtils.isEmpty(container.getChildren())) {
      return null;
    }
    return container.getChildren()
                    .stream()
                    .map(this::getPageBody)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
  }

  private Application findApplication(ModelObject modelObject, long applicationId) {
    if (modelObject == null) {
      return null;
    }
    return switch (modelObject) {
    case Container container -> {
      if (!CollectionUtils.isEmpty(container.getChildren())) {
        yield container.getChildren()
                       .stream()
                       .map(m -> findApplication(m, applicationId))
                       .filter(Objects::nonNull)
                       .findFirst()
                       .orElse(null);
      }
      yield null;
    }
    case Application application -> {
      if (StringUtils.equals(application.getStorageId(), String.valueOf(applicationId))) {
        yield application;
      }
      yield null;
    }
    default -> null;
    };
  }

  private Container findParentContainer(ModelObject modelObject, String containerStorageId) {
    if (modelObject == null) {
      return null;
    }
    return switch (modelObject) {
    case Container container -> {
      if (CollectionUtils.isNotEmpty(container.getChildren())) {
        if (container.getChildren()
                     .stream()
                     .anyMatch(m -> StringUtils.equals(containerStorageId, m.getStorageId()))) {
          yield container;
        } else {
          yield container.getChildren()
                         .stream()
                         .map(m -> findParentContainer(m, containerStorageId))
                         .filter(Objects::nonNull)
                         .findFirst()
                         .orElse(null);
        }
      } else {
        yield null;
      }
    }
    default -> null;
    };
  }

}
