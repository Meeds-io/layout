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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.addons.AddOnService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.PortletInstancePreference;

import lombok.SneakyThrows;

@SpringBootTest(classes = { PageLayoutService.class })
@ExtendWith(MockitoExtension.class)
public class PageLayoutServiceTest {

  private static final String     ADDON_CONTAINER      = "addonContainer";

  private static final String     TEST_ADDON_CONTAINER = "testAddonContainer";

  private static final String     TEST_USER            = "testuser";

  private static final SiteKey    SITE_KEY             = SiteKey.portal("test");

  private static final PageKey    PAGE_KEY             = PageKey.parse("portal::test::test");

  @MockBean
  private LayoutService           layoutService;

  @MockBean
  private LayoutAclService        aclService;

  @MockBean
  private PageTemplateService     pageTemplateService;

  @MockBean
  private UserPortalConfigService userPortalConfigService;

  @MockBean
  private AddOnService            addOnService;

  @MockBean
  private ContainerLayoutService  containerLayoutService;

  @MockBean
  private PortletInstanceService  portletInstanceService;

  @MockBean
  private ListenerService         listenerService;

  @Autowired
  private PageLayoutService       pageLayoutService;

  @Mock
  private PortalConfig            portalConfig;

  @Mock
  private PageContext             pageContext;

  @Mock
  private PageState               pageState;

  @Mock
  private Page                    page;

  @Mock
  private PortalConfig            site;

  @Test
  @SuppressWarnings("unchecked")
  public void getPages() {
    String siteTypeName = SITE_KEY.getTypeName();
    String siteName = SITE_KEY.getName();
    String pageDisplayName = "pageDisplayName";
    int offset = 0;
    int limit = 10;

    List<PageContext> pageContexts = new ArrayList<>();
    QueryResult<PageContext> queryResult = mock(QueryResult.class);
    when(queryResult.iterator()).thenAnswer(invocation -> pageContexts.iterator());
    when(layoutService.findPages(offset,
                                 limit,
                                 SITE_KEY.getType(),
                                 siteName,
                                 null,
                                 pageDisplayName)).thenReturn(queryResult);
    List<PageContext> result = pageLayoutService.getPages(siteTypeName, siteName, pageDisplayName, offset, limit, TEST_USER);
    assertNotNull(result);
    assertEquals(0, result.size());

    when(pageContext.getKey()).thenReturn(PAGE_KEY);
    pageContexts.add(pageContext);
    result = pageLayoutService.getPages(siteTypeName, siteName, pageDisplayName, offset, limit, TEST_USER);
    assertNotNull(result);
    assertEquals(0, result.size());

    when(aclService.canViewPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    result = pageLayoutService.getPages(siteTypeName, siteName, pageDisplayName, offset, limit, TEST_USER);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  @SneakyThrows
  public void impersonateSite() {
    SiteLayout pageBodyContainer = mock(SiteLayout.class);
    when(site.getPortalLayout()).thenReturn(pageBodyContainer);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
    assertDoesNotThrow(() -> pageLayoutService.impersonateSite(SITE_KEY));

    when(layoutService.findPages(SITE_KEY)).thenReturn(Collections.singletonList(pageContext));
    when(pageContext.getKey()).thenReturn(PAGE_KEY);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    Container container = mock(Container.class);
    Application application = mock(Application.class);
    Portlet preferences = mock(Portlet.class);
    when(container.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(application)));
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(container)));
    when(portletInstanceService.getApplicationPortletPreferences(application)).thenReturn(preferences);
    ApplicationState state = mock(ApplicationState.class);
    when(application.getState()).thenReturn(state);

    pageLayoutService.impersonateSite(SITE_KEY);

    verify(containerLayoutService).impersonateContainer(container, page);
    verify(layoutService, never()).save(state, preferences);

    when(application.getStorageId()).thenReturn("2");
    pageLayoutService.impersonateSite(SITE_KEY);
    verify(layoutService).save(state, preferences);
  }

  @Test
  public void getPage() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPage(PAGE_KEY, TEST_USER));
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPage(PAGE_KEY, TEST_USER));
    when(aclService.canViewPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    assertEquals(pageContext, pageLayoutService.getPage(PAGE_KEY, TEST_USER));
  }

  @Test
  public void getPageLayout() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));
    when(aclService.canViewPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));
    when(aclService.canViewSite(PAGE_KEY.getSite(), TEST_USER)).thenReturn(true);
    assertNull(pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));
    when(aclService.hasAccessPermission(page, TEST_USER)).thenReturn(true);
    assertEquals(page, pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));
  }

  @Test
  public void getPageLayoutWithSite() throws IllegalAccessException, ObjectNotFoundException {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    when(aclService.canViewPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(aclService.canViewSite(PAGE_KEY.getSite(), TEST_USER)).thenReturn(true);
    when(aclService.hasAccessPermission(page, TEST_USER)).thenReturn(true);
    assertEquals(page, pageLayoutService.getPageLayout(PAGE_KEY, TEST_USER));

    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPageLayout(PAGE_KEY, 2l, TEST_USER));
    when(layoutService.getPortalConfig(2l)).thenReturn(site);
    assertNull(pageLayoutService.getPageLayout(PAGE_KEY, 2l, TEST_USER));

    Container container = mock(Container.class);
    Application application = mock(Application.class);
    Portlet preferences = mock(Portlet.class);
    when(container.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(application)));
    when(portletInstanceService.getApplicationPortletPreferences(application)).thenReturn(preferences);

    PageBody pageBody = mock(PageBody.class);
    SiteLayout pageBodyContainer = mock(SiteLayout.class);
    when(site.getPortalLayout()).thenReturn(pageBodyContainer);
    when(pageBodyContainer.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(pageBody)));

    assertNull(pageLayoutService.getPageLayout(PAGE_KEY, 2l, TEST_USER));

    when(aclService.hasAccessPermission(any(), eq(TEST_USER))).thenReturn(true);

    ModelObject pageLayout = pageLayoutService.getPageLayout(PAGE_KEY, 2l, true, TEST_USER);
    assertNotNull(pageLayout);

    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(container)));
    pageLayout = pageLayoutService.getPageLayout(PAGE_KEY, 2l, true, TEST_USER);
    assertNotNull(pageLayout);
    verify(application).setState(argThat(state -> state instanceof TransientApplicationState transientState
                                                  && Objects.equals(transientState.getContentState(), preferences)));
  }

  @Test
  public void getPageApplicationLayout() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
    when(aclService.canViewPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(aclService.canViewSite(PAGE_KEY.getSite(), TEST_USER)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));

    Container container = mock(Container.class);
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(container)));
    Application application = mock(Application.class);
    when(application.getStorageId()).thenReturn("2");
    when(container.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(application)));
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
    when(aclService.hasAccessPermission(page, TEST_USER)).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
    when(aclService.hasAccessPermission(container, TEST_USER)).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
    when(aclService.hasAccessPermission(application, TEST_USER)).thenReturn(true);
    assertNotNull(pageLayoutService.getPageApplicationLayout(PAGE_KEY, 2l, TEST_USER));
  }

  @Test
  public void getPageLayoutWithDynamicContainer() {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    Container dynamicContainer = mock(Container.class);
    when(dynamicContainer.getFactoryId()).thenReturn(ADDON_CONTAINER);
    when(dynamicContainer.getName()).thenReturn(TEST_ADDON_CONTAINER);
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(dynamicContainer)));
    Application application = mock(Application.class);
    when(addOnService.getApplications(TEST_ADDON_CONTAINER)).thenReturn(Collections.singletonList(application));
    pageLayoutService.getPageLayout(PAGE_KEY);
    verify(dynamicContainer).setChildren(argThat(children -> children != null && children.size() == 1
                                                             && children.get(0) == application));
  }

  @Test
  @SneakyThrows
  public void createPage() {
    PageCreateModel pageModel = mock(PageCreateModel.class);
    when(pageModel.getPageSiteType()).thenReturn(SITE_KEY.getTypeName());
    when(pageModel.getPageSiteName()).thenReturn(SITE_KEY.getName());
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.createPage(pageModel, TEST_USER));

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.createPage(pageModel, TEST_USER));

    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);
    when(pageModel.getPageName()).thenReturn(PAGE_KEY.getName());
    when(pageModel.getPageTemplateId()).thenReturn(2l);
    String pageTitle = "pageTitle";
    when(pageModel.getPageTitle()).thenReturn(pageTitle);

    String sitePermission = "site permission";
    when(portalConfig.getAccessPermissions()).thenReturn(new String[] { sitePermission });
    String siteEditPermission = "site edit permission";
    when(portalConfig.getEditPermission()).thenReturn(siteEditPermission);
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.createPage(pageModel, TEST_USER));

    PageTemplate pageTemplate = mock(PageTemplate.class);
    when(pageTemplate.getContent()).thenReturn("{}");
    when(pageTemplateService.getPageTemplate(2l)).thenReturn(pageTemplate);

    pageLayoutService.createPage(pageModel, TEST_USER);
    verify(layoutService).save(any(PageContext.class),
                               argThat(p -> p.getAccessPermissions()[0].equals(sitePermission)
                                            && p.getEditPermission().equals(siteEditPermission)
                                            && p.getTitle().equals(pageTitle)));

    String pagePermission = "page permission";
    when(pageModel.getAccessPermissions()).thenReturn(new String[] { pagePermission });
    String pageEditPermission = "page edit permission";
    when(pageModel.getEditPermission()).thenReturn(pageEditPermission);
    pageLayoutService.createPage(pageModel, TEST_USER);
    verify(layoutService).save(any(PageContext.class),
                               argThat(p -> p.getAccessPermissions()[0].equals(pagePermission)
                                            && p.getEditPermission().equals(pageEditPermission)));
  }

  @Test
  @SneakyThrows
  public void createPageWithLink() {
    PageCreateModel pageModel = mock(PageCreateModel.class);
    when(pageModel.getPageSiteType()).thenReturn(SITE_KEY.getTypeName());
    when(pageModel.getPageSiteName()).thenReturn(SITE_KEY.getName());
    when(pageModel.getPageType()).thenReturn(PageType.LINK.name());
    when(pageModel.getLink()).thenReturn("#link");
    when(pageModel.getPageName()).thenReturn(PAGE_KEY.getName());

    String pageTitle = "pageTitle";
    when(pageModel.getPageTitle()).thenReturn(pageTitle);
    String sitePermission = "site permission";
    when(portalConfig.getAccessPermissions()).thenReturn(new String[] { sitePermission });
    String siteEditPermission = "site edit permission";
    when(portalConfig.getEditPermission()).thenReturn(siteEditPermission);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditNavigation(SITE_KEY, TEST_USER)).thenReturn(true);

    pageLayoutService.createPage(pageModel, TEST_USER);
    verify(layoutService).save(any(PageContext.class),
                               argThat(p -> p.getAccessPermissions()[0].equals(sitePermission)
                                            && p.getEditPermission().equals(siteEditPermission)
                                            && p.getLink().equals(pageModel.getLink())
                                            && p.getType().equals(PageType.LINK.name())
                                            && p.getTitle().equals(pageTitle)));
  }

  @Test
  public void clonePage() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.clonePage(PAGE_KEY, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.clonePage(PAGE_KEY, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);

    when(page.getName()).thenReturn(PAGE_KEY.getName());
    pageLayoutService.clonePage(PAGE_KEY, TEST_USER);
    verify(page).resetStorage();
    verify(page).setName(PAGE_KEY.getName() + "_draft_" + TEST_USER);
    verify(layoutService).save(any(PageContext.class), eq(page));
  }

  @Test
  public void clonePageWithDynamicContainer() throws IllegalAccessException, ObjectNotFoundException {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    Container dynamicContainer = mock(Container.class);
    when(dynamicContainer.getFactoryId()).thenReturn(ADDON_CONTAINER);
    when(dynamicContainer.getName()).thenReturn(TEST_ADDON_CONTAINER);
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(dynamicContainer)));
    Application application = mock(Application.class);
    when(addOnService.getApplications(TEST_ADDON_CONTAINER)).thenReturn(Collections.singletonList(application));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(page.getName()).thenReturn(PAGE_KEY.getName());

    pageLayoutService.clonePage(PAGE_KEY, TEST_USER);
    verify(page).setChildren(argThat(children -> children != null && children.size() == 1 && children.get(0) == application));
  }

  @Test
  public void updatePageApplicationPreferences() throws IllegalAccessException, ObjectNotFoundException {
    PortletInstancePreference portletInstancePreference = mock(PortletInstancePreference.class);
    List<PortletInstancePreference> preferences = Collections.singletonList(portletInstancePreference);
    assertThrows(ObjectNotFoundException.class,
                 () -> pageLayoutService.updatePageApplicationPreferences(PAGE_KEY, 2l, preferences, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertThrows(IllegalAccessException.class,
                 () -> pageLayoutService.updatePageApplicationPreferences(PAGE_KEY, 2l, preferences, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class,
                 () -> pageLayoutService.updatePageApplicationPreferences(PAGE_KEY, 2l, preferences, TEST_USER));

    Container container = mock(Container.class);
    Application application = mock(Application.class);
    when(container.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(application)));
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(container)));
    when(application.getStorageId()).thenReturn("2");
    ApplicationState state = mock(ApplicationState.class);
    when(application.getState()).thenReturn(state);
    when(portletInstancePreference.getName()).thenReturn("name");
    when(portletInstancePreference.getValue()).thenReturn("value");

    pageLayoutService.updatePageApplicationPreferences(PAGE_KEY, 2l, preferences, TEST_USER);
    verify(layoutService).save(eq(state), argThat(prefs -> prefs.getValue("name").equals("value")));
  }

  @Test
  public void updatePageLayout() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);
    assertThrows(IllegalAccessException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);

    when(page.getPageKey()).thenReturn(PAGE_KEY);
    pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, false, TEST_USER);
    verify(page, never()).resetStorage();
    verify(page).setChildren(any());
    verify(layoutService).save(page);

    pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER);
    verify(page).resetStorage();

    String width = "36px";
    String height = "36%";
    String borderColor = "#256977";

    when(page.getWidth()).thenReturn(width);
    when(page.getHeight()).thenReturn(height);
    ModelStyle cssStyle = mock(ModelStyle.class);
    when(page.getCssStyle()).thenReturn(cssStyle);
    when(cssStyle.getBorderColor()).thenReturn(borderColor);
    assertDoesNotThrow(() -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));

    when(page.getWidth()).thenReturn("eval('alert(`XSS in width CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(page.getWidth()).thenReturn(width);

    when(page.getHeight()).thenReturn("eval('alert(`XSS in height CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(page.getHeight()).thenReturn(height);

    when(cssStyle.getBorderColor()).thenReturn("eval('alert(`XSS in background CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(cssStyle.getBorderColor()).thenReturn(borderColor);

    assertDoesNotThrow(() -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));

    Application application = mock(Application.class);
    when(page.getChildren()).thenReturn(new ArrayList<>(Collections.singletonList(application)));

    when(application.getWidth()).thenReturn("eval('alert(`XSS in application width CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(application.getWidth()).thenReturn(width);

    when(application.getHeight()).thenReturn("eval('alert(`XSS in application height CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(application.getHeight()).thenReturn(height);

    ModelStyle applicationCssStyle = mock(ModelStyle.class);
    when(application.getCssStyle()).thenReturn(applicationCssStyle);
    when(applicationCssStyle.getBorderColor()).thenReturn("eval('alert(`XSS in application background CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
    when(applicationCssStyle.getBorderColor()).thenReturn(borderColor);

    assertDoesNotThrow(() -> pageLayoutService.updatePageLayout(PAGE_KEY.format(), page, true, TEST_USER));
  }

  @Test
  public void restorePageLayout() throws IllegalAccessException, ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.restorePageLayout(PAGE_KEY, TEST_USER));
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.restorePageLayout(PAGE_KEY, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(pageContext.getState()).thenReturn(pageState);

    assertThrows(IllegalStateException.class, () -> pageLayoutService.restorePageLayout(PAGE_KEY, TEST_USER));
    verify(userPortalConfigService).restorePage(PAGE_KEY, ImportMode.RESTORE_DEFAULTS);

    when(userPortalConfigService.restorePage(PAGE_KEY, ImportMode.RESTORE_DEFAULTS)).thenReturn(true);
    List<String> accessPermissions = Arrays.asList("access", "permissions");
    String editPermission = "edit permission";
    when(pageState.getAccessPermissions()).thenReturn(accessPermissions);
    when(pageState.getEditPermission()).thenReturn(editPermission);

    PageContext result = pageLayoutService.restorePageLayout(PAGE_KEY, TEST_USER);
    assertEquals(pageContext, result);
    verify(pageState).setAccessPermissions(accessPermissions);
    verify(pageState).setEditPermission(editPermission);
    verify(pageContext).setState(pageState);
    verify(layoutService).save(pageContext);
  }

  @Test
  public void updatePageLink() throws IllegalAccessException, IllegalStateException, ObjectNotFoundException {
    String link = "link";
    assertThrows(ObjectNotFoundException.class, () -> pageLayoutService.updatePageLink(PAGE_KEY, link, TEST_USER));
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    assertThrows(IllegalAccessException.class, () -> pageLayoutService.updatePageLink(PAGE_KEY, link, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(pageContext.getState()).thenReturn(pageState);
    assertThrows(IllegalStateException.class, () -> pageLayoutService.updatePageLink(PAGE_KEY, link, TEST_USER));
    when(pageState.getType()).thenReturn(PageType.LINK.name());
    pageLayoutService.updatePageLink(PAGE_KEY, link, TEST_USER);
    verify(layoutService).save(pageContext);
    verify(pageState).setLink(link);
  }

  @Test
  public void updatePagePermissions() throws IllegalAccessException, ObjectNotFoundException {
    List<String> accessPermissions = Arrays.asList("access", "permissions");
    String editPermission = "edit permission";
    PermissionUpdateModel permissionModel = new PermissionUpdateModel(accessPermissions, editPermission);
    assertThrows(ObjectNotFoundException.class,
                 () -> pageLayoutService.updatePagePermissions(PAGE_KEY, permissionModel, TEST_USER));
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(pageContext);
    assertThrows(IllegalAccessException.class,
                 () -> pageLayoutService.updatePagePermissions(PAGE_KEY, permissionModel, TEST_USER));
    when(aclService.canEditPage(PAGE_KEY, TEST_USER)).thenReturn(true);
    when(pageContext.getState()).thenReturn(pageState);
    pageLayoutService.updatePagePermissions(PAGE_KEY, permissionModel, TEST_USER);
    verify(layoutService).save(pageContext);
    verify(pageState).setEditPermission(permissionModel.getEditPermission());
    verify(pageState).setAccessPermissions(permissionModel.getAccessPermissions());
  }

}
