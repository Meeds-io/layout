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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SitePropertiesUpdate;
import io.meeds.layout.model.SiteUpdateModel;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
  SiteLayoutService.class,
})
@ExtendWith(MockitoExtension.class)
public class SiteLayoutServiceTest {

  private static final String     TEST_USER = "testuser";

  private static final SiteKey    SITE_KEY  = SiteKey.portal("test");

  private static final String     DRAFT_SITE_NAME = "portal_test_draft_testuser";

  private static final SiteKey    DRAFT_SITE_KEY  = new SiteKey(PortalConfig.DRAFT, DRAFT_SITE_NAME);

  @MockitoBean
  private LayoutService           layoutService;

  @MockitoBean
  private NavigationService       navigationService;

  @MockitoBean
  private UserPortalConfigService portalConfigService;

  @MockitoBean
  private LayoutAclService        aclService;

  @MockitoBean
  private LocaleConfigService     localeConfigService;

  @MockitoBean
  private PortletInstanceService  portletInstanceService;

  @MockitoBean
  private ListenerService         listenerService;

  @MockitoBean
  private NavigationLayoutService navigationLayoutService;

  @MockitoBean
  private TranslationService      translationService;

  @Autowired
  private SiteLayoutService       siteLayoutService;

  @Mock
  private PortalConfig            portalConfig;

  @Test
  @SneakyThrows
  public void getSiteById() {
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.getSite(2l, TEST_USER));
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn(SITE_KEY.getTypeName());
    when(portalConfig.getName()).thenReturn(SITE_KEY.getName());
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.getSite(2l, TEST_USER));
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    assertEquals(portalConfig, siteLayoutService.getSite(2l, TEST_USER));
  }

  @Test
  @SneakyThrows
  public void getSiteByKey() {
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.getSite(SITE_KEY, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.getSite(SITE_KEY, TEST_USER));
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    assertEquals(portalConfig, siteLayoutService.getSite(SITE_KEY, TEST_USER));
  }

  /**
   * The site layout editor publishes its draft back over the same site: the
   * draft's windows are built with the draft property, which a preference
   * plugin that must keep a window's own setting there reads.
   */
  @Test
  @SneakyThrows
  public void createDraftSiteGeneratesPreferencesForADraft() {
    Application application = new Application("12");
    application.setState(new TransientApplicationState("app-center/AppCenterMyApplicationsPortlet"));
    Container portalLayout = new Container();
    portalLayout.setTemplate("system:/groovy/portal/webui/container/UIContainer.gtmpl");
    portalLayout.setChildren(new ArrayList<>(List.of(application)));
    PortalConfig site = new PortalConfig(PortalConfig.PORTAL_TYPE, SITE_KEY.getName());
    site.setPortalLayout(portalLayout);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
    when(aclService.canEditSite(argThat((PortalConfig draftSite) -> DRAFT_SITE_NAME.equals(draftSite.getName())),
                                eq(TEST_USER))).thenReturn(true);

    SiteKey draftSiteKey = siteLayoutService.createDraftSite(SITE_KEY, TEST_USER);

    assertEquals(new SiteKey(PortalConfig.DRAFT, "portal_test_draft_" + TEST_USER), draftSiteKey);
    verify(portletInstanceService).expandPortletPreferences(any(Application.class),
                                                            argThat(context -> context != null
                                                                               && !context.isExport()
                                                                               && context.getProperties() != null
                                                                               && "true".equals(context.getProperties()
                                                                                                       .get(PortletInstanceContext.DRAFT_PROPERTY))));
    verify(layoutService).create(argThat((PortalConfig draftSite) -> draftSite.getName().equals(draftSiteKey.getName())));
  }

  @Test
  @SneakyThrows
  public void createDraftSite() {
    PortalConfig existingDraftSite = mockDraftSiteCreation();
    when(aclService.canEditSite(argThat((PortalConfig draftSite) -> DRAFT_SITE_NAME.equals(draftSite.getName())),
                                eq(TEST_USER))).thenReturn(true);

    assertEquals(DRAFT_SITE_KEY, siteLayoutService.createDraftSite(SITE_KEY, TEST_USER));
    verify(layoutService).remove(existingDraftSite);
    verify(layoutService).create(argThat((PortalConfig draftSite) -> DRAFT_SITE_NAME.equals(draftSite.getName())
                                                                   && PortalConfig.DRAFT.equals(draftSite.getType())));
  }

  /**
   * The draft's name could also be another existing user's draft of another
   * site, so the user can't edit it: a draft of that name is kept, and none is
   * created that the user couldn't open.
   */
  @Test
  @SneakyThrows
  public void createDraftSiteWhenItsNameIsAnotherUsersDraft() {
    mockDraftSiteCreation();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                      () -> siteLayoutService.createDraftSite(SITE_KEY, TEST_USER));
    assertEquals("layout.siteDraftNameConflict", exception.getMessage());
    verify(layoutService, never()).remove(any(PortalConfig.class));
    verify(layoutService, never()).create(any(PortalConfig.class));
  }

  /**
   * The removal runs from a listener's thread, where only the annotation binds
   * the container: the annotation-removed mutant passes every other test, which
   * calls the un-advised seam.
   */
  @Test
  @SneakyThrows
  public void deleteUserDraftSitesBindsTheContainer() {
    assertTrue(SiteLayoutService.class.getMethod("deleteUserDraftSites", String.class)
                                      .isAnnotationPresent(ContainerTransactional.class));
  }

  /**
   * The draft sites are listed unpaged: a page of the label order could skip
   * a draft that shares its label with another one.
   */
  @Test
  public void removeUserDraftSites() {
    PortalConfig otherUserDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_x_draft_doe");
    PortalConfig ownedDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_x_draft_john");
    PortalConfig ambiguousDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_team_draft_draft_john");
    PortalConfig suffixOwnerDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_x_draft_doe_john");
    PortalConfig longerOwnerDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_x_draft_johnny");
    when(layoutService.getSites(argThat(filter -> filter != null && filter.getSiteType() == SiteType.DRAFT
                                                  && filter.getOffset() == 0 && filter.getLimit() == 0)))
                                                                                                         .thenReturn(List.of(otherUserDraftSite,
                                                                                                                             ownedDraftSite,
                                                                                                                             ambiguousDraftSite,
                                                                                                                             suffixOwnerDraftSite,
                                                                                                                             longerOwnerDraftSite));

    siteLayoutService.removeUserDraftSites("john");

    verify(layoutService).remove(ownedDraftSite);
    verify(layoutService).remove(ambiguousDraftSite);
    verify(layoutService, never()).remove(otherUserDraftSite);
    verify(layoutService, never()).remove(suffixOwnerDraftSite);
    verify(layoutService, never()).remove(longerOwnerDraftSite);
    verify(layoutService).getSites(any());
  }

  @Test
  public void removeUserDraftSitesWhenOneRemovalFails() {
    PortalConfig failingDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_x_draft_john");
    PortalConfig otherDraftSite = new PortalConfig(PortalConfig.DRAFT, "portal_y_draft_john");
    when(layoutService.getSites(any())).thenReturn(List.of(failingDraftSite, otherDraftSite));
    doThrow(new IllegalStateException("store down")).when(layoutService).remove(failingDraftSite);

    siteLayoutService.removeUserDraftSites("john");

    verify(layoutService).remove(otherDraftSite);
  }

  @Test
  public void removeUserDraftSitesWithoutUsername() {
    siteLayoutService.removeUserDraftSites(null);
    siteLayoutService.removeUserDraftSites("");

    verify(layoutService, never()).getSites(any());
  }

  @Test
  @SneakyThrows
  public void createSiteWhenAlreadyExists() {
    SiteCreateModel createModel = mock(SiteCreateModel.class);
    when(createModel.getPortalConfig()).thenReturn(portalConfig);
    when(portalConfig.getName()).thenReturn(SITE_KEY.getName());
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.createSite(createModel, TEST_USER));

    when(aclService.canAddSite(TEST_USER)).thenReturn(true);

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(ObjectAlreadyExistsException.class, () -> siteLayoutService.createSite(createModel, TEST_USER));
  }

  @Test
  @SneakyThrows
  public void createSite() {
    SiteCreateModel createModel = mock(SiteCreateModel.class);
    when(createModel.getPortalConfig()).thenReturn(portalConfig);
    when(portalConfig.getName()).thenReturn(SITE_KEY.getName());
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.createSite(createModel, TEST_USER));

    when(aclService.canAddSite(TEST_USER)).thenReturn(true);

    long siteTemplateId = 7l;
    String siteTemplate = "siteTemplate";
    when(createModel.getSiteId()).thenReturn(siteTemplateId);
    PortalConfig templatePortalConfig = mock(PortalConfig.class);
    when(layoutService.getPortalConfig(siteTemplateId)).thenReturn(templatePortalConfig);
    when(templatePortalConfig.getType()).thenReturn(PortalConfig.PORTAL_TEMPLATE);
    when(templatePortalConfig.getName()).thenReturn(siteTemplate);

    String description = "description";
    String uploadId = "56632";
    when(portalConfig.getLabel()).thenReturn(SITE_KEY.getTypeName());
    when(portalConfig.getDescription()).thenReturn(description);
    when(portalConfig.isDisplayed()).thenReturn(true);
    when(portalConfig.getDisplayOrder()).thenReturn(22);
    when(portalConfig.getBannerUploadId()).thenReturn(uploadId);
    when(aclService.getAdministratorsGroup()).thenReturn("administrators");
    doAnswer(invocation -> {
      when(layoutService.getPortalConfig(invocation.getArgument(1, SiteKey.class))).thenReturn(portalConfig);
      return null;
    }).when(portalConfigService)
      .createSiteFromTemplate(SiteKey.portalTemplate(siteTemplate), SITE_KEY);

    siteLayoutService.createSite(createModel, TEST_USER);
    verify(layoutService).save(portalConfig);
    verify(portalConfig).setDescription(description);
    verify(portalConfig).setDisplayed(true);
    verify(portalConfig).setDisplayOrder(22);
    verify(portalConfig).setBannerUploadId(uploadId);
    verify(portalConfig).setEditPermission("*:administrators");
    verify(portalConfig).setAccessPermissions(argThat(p -> p[0].equals("*:administrators")));

    when(portalConfig.isDisplayed()).thenReturn(false);
    when(portalConfig.getBannerUploadId()).thenReturn(null);
    when(portalConfig.getAccessPermissions()).thenReturn(new String[] { "accessPermission" });
    when(portalConfig.getEditPermission()).thenReturn("editPermission");

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(null);
    doAnswer(invocation -> {
      when(layoutService.getPortalConfig(invocation.getArgument(1, SiteKey.class))).thenReturn(portalConfig);
      return null;
    }).when(portalConfigService)
      .createSiteFromTemplate(SiteKey.portalTemplate(siteTemplate), SITE_KEY);
    siteLayoutService.createSite(createModel, TEST_USER);

    verify(portalConfig).setAccessPermissions(argThat(p -> p[0].equals("accessPermission")));
    verify(portalConfig).setEditPermission("editPermission");
    verify(portalConfig).setDisplayed(false);
    verify(portalConfig).setDisplayOrder(0);
  }

  @Test
  @SneakyThrows
  public void createSiteWhenTheTemplateLayoutIsUnresolved() {
    SiteCreateModel createModel = mock(SiteCreateModel.class);
    when(createModel.getPortalConfig()).thenReturn(portalConfig);
    when(portalConfig.getName()).thenReturn(SITE_KEY.getName());
    when(aclService.canAddSite(TEST_USER)).thenReturn(true);
    when(createModel.getSiteId()).thenReturn(7l);
    PortalConfig templatePortalConfig = mock(PortalConfig.class);
    when(layoutService.getPortalConfig(7l)).thenReturn(templatePortalConfig);
    when(templatePortalConfig.isLayoutUnresolved()).thenReturn(true);
    lenient().when(templatePortalConfig.getType()).thenReturn(PortalConfig.PORTAL_TEMPLATE);
    lenient().when(templatePortalConfig.getName()).thenReturn("siteTemplate");

    assertThrows(SiteLayoutUnresolvedException.class, () -> siteLayoutService.createSite(createModel, TEST_USER));

    verify(portalConfigService, never()).createSiteFromTemplate(any(), any());
  }

  @Test
  @SneakyThrows
  public void getSiteLayout() {
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    SiteLayout siteLayout = new SiteLayout();
    when(portalConfig.getPortalLayout()).thenReturn(siteLayout);

    assertEquals(siteLayout, siteLayoutService.getSiteLayout(SITE_KEY, TEST_USER));
  }

  @Test
  @SneakyThrows
  public void getSiteLayoutWhenTheSiteLayoutIsUnresolved() {
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(portalConfig.isLayoutUnresolved()).thenReturn(true);
    lenient().when(portalConfig.getPortalLayout()).thenReturn(new SiteLayout());

    assertThrows(SiteLayoutUnresolvedException.class, () -> siteLayoutService.getSiteLayout(SITE_KEY, TEST_USER));
  }

  @Test
  @SneakyThrows
  public void createDraftSiteWhenTheSiteLayoutIsUnresolved() {
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(portalConfig.isLayoutUnresolved()).thenReturn(true);
    lenient().when(portalConfig.clone()).thenReturn(mock(PortalConfig.class));
    lenient().when(portalConfig.getPortalLayout()).thenReturn(new SiteLayout());

    assertThrows(SiteLayoutUnresolvedException.class, () -> siteLayoutService.createDraftSite(SITE_KEY, TEST_USER));

    verify(layoutService, never()).create(any(PortalConfig.class));
    verify(layoutService, never()).remove(any(PortalConfig.class));
  }

  @Test
  @SneakyThrows
  public void updateSite() {
    SiteUpdateModel updateModel = mock(SiteUpdateModel.class);
    when(updateModel.getSiteType()).thenReturn(SITE_KEY.getTypeName());
    when(updateModel.getSiteName()).thenReturn(SITE_KEY.getName());
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.updateSite(updateModel, TEST_USER));

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.updateSite(updateModel, TEST_USER));

    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);

    when(updateModel.getSiteLabel()).thenReturn("label");
    when(updateModel.getSiteDescription()).thenReturn("description");
    when(updateModel.isDisplayed()).thenReturn(true);
    when(updateModel.getDisplayOrder()).thenReturn(22);
    when(updateModel.getBannerUploadId()).thenReturn("56632");
    siteLayoutService.updateSite(updateModel, TEST_USER);
    verify(layoutService).save(portalConfig);
    verify(layoutService, never()).removeSiteBanner(any());
    verify(portalConfig).setDisplayOrder(22);

    when(updateModel.isDisplayed()).thenReturn(false);
    when(updateModel.isBannerRemoved()).thenReturn(true);
    when(portalConfig.getBannerFileId()).thenReturn(336l);
    siteLayoutService.updateSite(updateModel, TEST_USER);
    verify(layoutService, times(2)).save(portalConfig);
    verify(layoutService).removeSiteBanner(SITE_KEY.getName());
    verify(portalConfig).setDisplayOrder(0);
  }

  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsKeepsEveryPropertyTheCallerDidNotState() {
    PortalConfig stored = storedSite();
    SitePropertiesUpdate labelOnly = new SitePropertiesUpdate("New", null, null, null, null, null);
    assertThrows(ObjectNotFoundException.class,
                 () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY, labelOnly, null, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    assertThrows(IllegalAccessException.class,
                 () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY, labelOnly, null, TEST_USER));
    verify(layoutService, never()).save(any(PortalConfig.class));

    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    defaultLocale(Locale.FRENCH);
    siteLayoutService.updateSiteAndPermissions(SITE_KEY, labelOnly, null, TEST_USER);

    verify(layoutService, times(1)).save(stored);
    assertEquals("New", stored.getLabel());
    assertEquals("Old description", stored.getDescription());
    assertEquals("fa-old", stored.getIcon());
    assertTrue(stored.isDisplayed());
    assertEquals(3, stored.getDisplayOrder());
    assertArrayEquals(new String[] { "*:/platform/users" }, stored.getAccessPermissions());
    assertEquals("*:/platform/administrators", stored.getEditPermission());
    assertEquals(0, stored.getBannerFileId());
    verify(layoutService, never()).removeSiteBanner(any());
    verify(translationService).saveTranslationLabel("site", 3L, "label", Locale.FRENCH, "New");
    verify(translationService, never()).saveTranslationLabel(any(), anyLong(), eq("description"), any(), any());

    siteLayoutService.updateSiteAndPermissions(SITE_KEY,
                                               new SitePropertiesUpdate(null, " ", "fa-new", false, null, true),
                                               null,
                                               TEST_USER);
    assertEquals("New", stored.getLabel());
    assertNull(stored.getDescription());
    assertEquals("fa-new", stored.getIcon());
    assertFalse(stored.isDisplayed());
    assertEquals(0, stored.getDisplayOrder());
    verify(layoutService, never()).removeSiteBanner(any());
    verify(translationService).saveTranslationLabel("site", 3L, "description", Locale.FRENCH, " ");
    verify(translationService, times(1)).saveTranslationLabel(any(), anyLong(), eq("label"), any(), any());
  }

  /**
   * The permissions are written first, then the properties, so a failure on
   * the properties leaves the permissions written: they are put back by a
   * third save carrying the stored values, and nothing is translated.
   */
  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsWritesPermissionsFirstAndPutsThemBackWhenThePropertiesFail() {
    PortalConfig stored = storedSite();
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    defaultLocale(Locale.ENGLISH);
    when(translationService.getTranslationLabel("site", 3L, "label", Locale.ENGLISH)).thenReturn("Ancien");
    List<String> saves = new ArrayList<>();
    boolean[] propertiesSaveFails = { true };
    doAnswer(invocation -> {
      PortalConfig saved = invocation.getArgument(0, PortalConfig.class);
      saves.add(saved.getEditPermission() + "|" + String.join(",", saved.getAccessPermissions()) + "|" + saved.getLabel());
      if (propertiesSaveFails[0] && saves.size() == 2) {
        throw new IllegalStateException("properties save failed");
      }
      return null;
    }).when(layoutService).save(stored);
    PermissionUpdateModel permissions = new PermissionUpdateModel(SITE_KEY.getTypeName(),
                                                                  SITE_KEY.getName(),
                                                                  List.of("Everyone"),
                                                                  "*:/platform/web-contributors");
    SitePropertiesUpdate properties = new SitePropertiesUpdate("New", null, null, null, null, null);

    assertEquals("properties save failed",
                 assertThrows(IllegalStateException.class,
                              () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY, properties, permissions, TEST_USER)).getMessage());
    assertEquals(List.of("*:/platform/web-contributors|Everyone|Old",
                         "*:/platform/web-contributors|Everyone|New",
                         "*:/platform/administrators|*:/platform/users|New"),
                 saves);
    InOrder translations = inOrder(translationService);
    translations.verify(translationService).saveTranslationLabel("site", 3L, "label", Locale.ENGLISH, "New");
    translations.verify(translationService).saveTranslationLabel("site", 3L, "label", Locale.ENGLISH, "Ancien");
    verify(translationService, never()).saveTranslationLabel(any(), anyLong(), eq("description"), any(), any());

    saves.clear();
    propertiesSaveFails[0] = false;
    siteLayoutService.updateSiteAndPermissions(SITE_KEY, properties, permissions, TEST_USER);
    assertEquals(List.of("*:/platform/web-contributors|Everyone|New", "*:/platform/web-contributors|Everyone|New"), saves);
    verify(translationService, times(2)).saveTranslationLabel("site", 3L, "label", Locale.ENGLISH, "New");
    verify(translationService, times(1)).saveTranslationLabel("site", 3L, "label", Locale.ENGLISH, "Ancien");
  }

  /**
   * The edit right is checked once, before the first write: handing it to a
   * group the caller is not in applies, alone or with properties, as the
   * permissions drawer's own request does
   */
  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsHandsTheEditRightToAGroupTheCallerIsNotIn() {
    PortalConfig stored = storedSite();
    stored.setEditPermission("manager:/spaces/marketing");
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenAnswer(i -> "manager:/spaces/marketing".equals(stored.getEditPermission()));
    List<String> saves = new ArrayList<>();
    doAnswer(invocation -> {
      PortalConfig saved = invocation.getArgument(0, PortalConfig.class);
      saves.add(saved.getEditPermission() + "|" + saved.getLabel());
      return null;
    }).when(layoutService).save(stored);
    PermissionUpdateModel handOver = new PermissionUpdateModel(SITE_KEY.getTypeName(), SITE_KEY.getName(), null, "*:/platform/administrators");

    siteLayoutService.updateSiteAndPermissions(SITE_KEY, new SitePropertiesUpdate("New", null, null, null, null, null), handOver, TEST_USER);
    assertEquals(List.of("*:/platform/administrators|Old", "*:/platform/administrators|New"), saves);
    assertEquals("*:/platform/administrators", stored.getEditPermission());

    stored.setEditPermission("manager:/spaces/marketing");
    saves.clear();
    siteLayoutService.updateSiteAndPermissions(SITE_KEY, new SitePropertiesUpdate(null, null, null, null, null, null), handOver, TEST_USER);
    assertEquals(List.of("*:/platform/administrators|New", "*:/platform/administrators|New"), saves);
  }

  /**
   * The meta site is the one the sites administration form never lets out of
   * the sites menu: hiding it is refused, before any write
   */
  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsRefusesToHideTheMetaSite() {
    PortalConfig stored = storedSite();
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(portalConfigService.getMetaPortal()).thenReturn(SITE_KEY.getName());
    PermissionUpdateModel permissions = new PermissionUpdateModel(SITE_KEY.getTypeName(), SITE_KEY.getName(), List.of("Everyone"), null);

    assertEquals(SiteLayoutService.DISPLAYED_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class,
                              () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY,
                                                                               new SitePropertiesUpdate(null, null, null, false, null, null),
                                                                               permissions,
                                                                               TEST_USER)).getMessage());
    verify(layoutService, never()).save(any(PortalConfig.class));
    assertTrue(stored.isDisplayed());

    siteLayoutService.updateSiteAndPermissions(SITE_KEY, new SitePropertiesUpdate(null, null, null, true, null, null), null, TEST_USER);
    assertTrue(stored.isDisplayed());
    assertEquals(3, stored.getDisplayOrder());

    when(portalConfigService.getMetaPortal()).thenReturn("other");
    siteLayoutService.updateSiteAndPermissions(SITE_KEY, new SitePropertiesUpdate(null, null, null, false, null, null), null, TEST_USER);
    assertFalse(stored.isDisplayed());
  }

  /**
   * A creation that fails after the site was instantiated from its template
   * removes that site, so the name is free for a retry: made removable first,
   * since the template's clone is not, and left whole when that save fails
   */
  @Test
  @SneakyThrows
  public void createSiteRemovesTheSiteItCreatedWhenTheFinalSaveFails() {
    SiteCreateModel createModel = mock(SiteCreateModel.class);
    when(createModel.getPortalConfig()).thenReturn(portalConfig);
    when(portalConfig.getName()).thenReturn(SITE_KEY.getName());
    when(aclService.canAddSite(TEST_USER)).thenReturn(true);
    when(createModel.getSiteId()).thenReturn(7L);
    PortalConfig templatePortalConfig = mock(PortalConfig.class);
    when(layoutService.getPortalConfig(7L)).thenReturn(templatePortalConfig);
    when(templatePortalConfig.getType()).thenReturn(PortalConfig.PORTAL_TEMPLATE);
    when(templatePortalConfig.getName()).thenReturn("basic");
    when(aclService.getAdministratorsGroup()).thenReturn("administrators");
    PortalConfig created = mock(PortalConfig.class);
    doAnswer(invocation -> {
      when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(created);
      return null;
    }).when(portalConfigService).createSiteFromTemplate(SiteKey.portalTemplate("basic"), SITE_KEY);
    int[] saves = { 0 };
    doAnswer(invocation -> {
      if (saves[0]++ == 0) {
        throw new IllegalStateException("Error while saving site image file");
      }
      return null;
    }).when(layoutService).save(created);

    assertEquals("Error while saving site image file",
                 assertThrows(IllegalStateException.class, () -> siteLayoutService.createSite(createModel, TEST_USER)).getMessage());
    InOrder removal = inOrder(created, navigationService, layoutService);
    removal.verify(created).setBannerUploadId(null);
    removal.verify(created).setRemovable(true);
    removal.verify(layoutService).save(created);
    removal.verify(navigationService).destroyNavigation(SITE_KEY);
    removal.verify(layoutService).removePages(SITE_KEY);
    removal.verify(layoutService).remove(created);

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(null);
    doThrow(new IllegalStateException("Error while saving site image file")).when(layoutService).save(created);
    assertEquals("Error while saving site image file",
                 assertThrows(IllegalStateException.class, () -> siteLayoutService.createSite(createModel, TEST_USER)).getMessage());
    verify(navigationService, times(1)).destroyNavigation(SITE_KEY);
    verify(layoutService, times(1)).removePages(SITE_KEY);
    verify(layoutService, times(1)).remove(created);
  }

  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsLeavesThePropertiesWhenThePermissionsAreRefused() {
    PortalConfig stored = storedSite();
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    doThrow(new IllegalStateException("permissions save failed")).when(layoutService).save(stored);
    PermissionUpdateModel permissions = new PermissionUpdateModel(SITE_KEY.getTypeName(), SITE_KEY.getName(), List.of("Everyone"), null);

    assertThrows(IllegalStateException.class,
                 () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY,
                                                                  new SitePropertiesUpdate("New", null, null, null, null, null),
                                                                  permissions,
                                                                  TEST_USER));
    verify(layoutService, times(1)).save(stored);
    assertEquals("Old", stored.getLabel());
    verify(translationService, never()).saveTranslationLabel(any(), anyLong(), any(), any(), any());
  }

  /**
   * A translation write that fails sits between the permissions and the
   * properties: the permissions are put back, the properties never written,
   * and the translation returned to what it held, a blank when it held none
   */
  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsPutsThePermissionsBackWhenATranslationWriteFails() {
    PortalConfig stored = storedSite();
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    defaultLocale(Locale.ENGLISH);
    List<String> saves = new ArrayList<>();
    doAnswer(invocation -> {
      PortalConfig saved = invocation.getArgument(0, PortalConfig.class);
      saves.add(saved.getEditPermission() + "|" + String.join(",", saved.getAccessPermissions()) + "|" + saved.getLabel());
      return null;
    }).when(layoutService).save(stored);
    doThrow(new IllegalStateException("translation failed")).when(translationService)
                                                              .saveTranslationLabel("site", 3L, "label", Locale.ENGLISH, "New");
    PermissionUpdateModel permissions = new PermissionUpdateModel(SITE_KEY.getTypeName(), SITE_KEY.getName(), List.of("Everyone"), null);

    assertEquals("translation failed",
                 assertThrows(IllegalStateException.class,
                              () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY,
                                                                               new SitePropertiesUpdate("New", "Desc", null, null, null, null),
                                                                               permissions,
                                                                               TEST_USER)).getMessage());
    assertEquals(List.of("*:/platform/administrators|Everyone|Old", "*:/platform/administrators|*:/platform/users|Old"), saves);
    verify(translationService).deleteTranslationLabel("site", 3L, "label", Locale.ENGLISH);
    verify(translationService, never()).deleteTranslationLabel(any(), anyLong(), eq("description"), any());
    verify(translationService, never()).saveTranslationLabel(any(), anyLong(), eq("description"), any(), any());
  }

  /**
   * A put-back that fails itself neither hides the first failure nor stops
   * the other put-backs: the permissions go first, then each translation
   * attempted, the one whose write failed included, and the exception raised
   * is the one that stopped the update
   */
  @Test
  @SneakyThrows
  public void updateSiteAndPermissionsRaisesTheFirstFailureWhenAPutBackFailsToo() {
    PortalConfig stored = storedSite();
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(stored);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    defaultLocale(Locale.ENGLISH);
    List<String> saves = new ArrayList<>();
    doAnswer(invocation -> {
      PortalConfig saved = invocation.getArgument(0, PortalConfig.class);
      saves.add(saved.getEditPermission() + "|" + String.join(",", saved.getAccessPermissions()) + "|" + saved.getLabel());
      if (saves.size() == 2) {
        throw new IllegalStateException("put-back save failed");
      }
      return null;
    }).when(layoutService).save(stored);
    doThrow(new IllegalStateException("description failed")).when(translationService)
                                                              .saveTranslationLabel("site", 3L, "description", Locale.ENGLISH, "Desc");
    doThrow(new IllegalStateException("put-back delete failed")).when(translationService)
                                                                 .deleteTranslationLabel("site", 3L, "label", Locale.ENGLISH);
    PermissionUpdateModel permissions = new PermissionUpdateModel(SITE_KEY.getTypeName(), SITE_KEY.getName(), List.of("Everyone"), null);

    assertEquals("description failed",
                 assertThrows(IllegalStateException.class,
                              () -> siteLayoutService.updateSiteAndPermissions(SITE_KEY,
                                                                               new SitePropertiesUpdate("New", "Desc", null, null, null, null),
                                                                               permissions,
                                                                               TEST_USER)).getMessage());
    assertEquals(List.of("*:/platform/administrators|Everyone|Old", "*:/platform/administrators|*:/platform/users|Old"), saves);
    InOrder putBacks = inOrder(layoutService, translationService);
    putBacks.verify(layoutService, times(2)).save(stored);
    putBacks.verify(translationService).deleteTranslationLabel("site", 3L, "label", Locale.ENGLISH);
    putBacks.verify(translationService).deleteTranslationLabel("site", 3L, "description", Locale.ENGLISH);
  }

  @Test
  @SneakyThrows
  public void restoreSite() {
    assertThrows(ObjectNotFoundException.class,
                 () -> siteLayoutService.restoreSite(SITE_KEY, ImportMode.CONSERVE, true, true, true, TEST_USER));

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class,
                 () -> siteLayoutService.restoreSite(SITE_KEY, ImportMode.CONSERVE, true, true, true, TEST_USER));
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);

    assertThrows(IllegalStateException.class,
                 () -> siteLayoutService.restoreSite(SITE_KEY, ImportMode.CONSERVE, true, true, true, TEST_USER));
    when(portalConfigService.canRestore(SITE_KEY.getTypeName(), SITE_KEY.getName())).thenReturn(true);

    siteLayoutService.restoreSite(SITE_KEY, ImportMode.OVERWRITE, true, false, true, TEST_USER);
    verify(portalConfigService).restoreSite(SITE_KEY.getTypeName(),
                                            SITE_KEY.getName(),
                                            ImportMode.OVERWRITE,
                                            true,
                                            false,
                                            true);
  }

  @Test
  @SneakyThrows
  public void restoreSiteRefreshesTheSearchIndexOfItsPages() {
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(portalConfigService.canRestore(SITE_KEY.getTypeName(), SITE_KEY.getName())).thenReturn(true);
    PageKey reachablePageKey = SITE_KEY.page("reachable");
    PageKey unreachablePageKey = SITE_KEY.page("unreachable");
    // A page that only a node of another site leads to
    PageKey otherSitePageKey = SITE_KEY.page("otherSite");
    Page unreachablePage = mock(Page.class);
    when(layoutService.findPages(SITE_KEY)).thenReturn(List.of(new PageContext(reachablePageKey, null),
                                                               new PageContext(unreachablePageKey, null),
                                                               new PageContext(otherSitePageKey, null)));
    when(layoutService.getPage(unreachablePageKey)).thenReturn(unreachablePage);

    siteLayoutService.restoreSite(SITE_KEY, ImportMode.OVERWRITE, true, false, false, TEST_USER);
    verify(navigationLayoutService, never()).getReachablePages(SITE_KEY);
    verify(listenerService, never()).broadcast(any(String.class), any(), any());

    // The restore removes the last node of a page of the site
    when(navigationLayoutService.getReachablePages(SITE_KEY)).thenReturn(Set.of(reachablePageKey, unreachablePageKey),
                                                                       Set.of(reachablePageKey));
    siteLayoutService.restoreSite(SITE_KEY, ImportMode.RESTORE_DEFAULTS, false, true, true, TEST_USER);
    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, reachablePageKey.format());
    verify(listenerService).broadcast(NavigationLayoutService.PAGE_UNREACHABLE_EVENT, siteLayoutService, unreachablePage);
    verify(listenerService, never()).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, unreachablePageKey.format());
    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, otherSitePageKey.format());

    // A navigation restore leaves the content of the pages as it is
    clearInvocations(listenerService);
    when(navigationLayoutService.getReachablePages(SITE_KEY)).thenReturn(Set.of(reachablePageKey));
    siteLayoutService.restoreSite(SITE_KEY, ImportMode.RESTORE_DEFAULTS, false, false, true, TEST_USER);
    verify(listenerService).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, reachablePageKey.format());
    verify(listenerService, never()).broadcast(PageLayoutService.PAGE_UPDATED_EVENT, TEST_USER, otherSitePageKey.format());
    verify(listenerService, never()).broadcast(any(String.class), any(), any(Page.class));
  }

  @Test
  @SneakyThrows
  public void restoreSiteIsDoneWhenTheSearchIndexCantBeRefreshed() {
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(portalConfigService.canRestore(SITE_KEY.getTypeName(), SITE_KEY.getName())).thenReturn(true);

    // The navigation can't be read after the restore
    doReturn(Set.of()).doThrow(new IllegalStateException("navigation")).when(navigationLayoutService).getReachablePages(SITE_KEY);
    siteLayoutService.restoreSite(SITE_KEY, ImportMode.RESTORE_DEFAULTS, false, true, false, TEST_USER);
    verify(portalConfigService).restoreSite(SITE_KEY.getTypeName(),
                                            SITE_KEY.getName(),
                                            ImportMode.RESTORE_DEFAULTS,
                                            false,
                                            true,
                                            false);

    // The navigation can't be read before the restore
    doThrow(new IllegalStateException("navigation")).when(navigationLayoutService).getReachablePages(SITE_KEY);
    siteLayoutService.restoreSite(SITE_KEY, ImportMode.RESTORE_DEFAULTS, false, false, true, TEST_USER);
    verify(portalConfigService).restoreSite(SITE_KEY.getTypeName(),
                                            SITE_KEY.getName(),
                                            ImportMode.RESTORE_DEFAULTS,
                                            false,
                                            false,
                                            true);
    verify(listenerService, never()).broadcast(any(String.class), any(), any());
  }

  @Test
  @SneakyThrows
  public void updateSiteLayoutValidatesStyles() {
    // The site layout save path validates its style values like the page one, icon colour hex-only
    PortalConfig site = mock(PortalConfig.class);
    SiteLayout siteLayout = new SiteLayout();
    ModelStyle style = new ModelStyle();
    siteLayout.setCssStyle(style);
    when(site.getPortalLayout()).thenReturn(siteLayout);

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);

    style.setTextColor("eval('alert(`XSS in site text CSS style`)')");
    assertThrows(IllegalArgumentException.class,
                 () -> siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER));
    style.setTextColor("#20282C");

    style.setIconColor("rgb(1, 2, 3)");
    assertThrows(IllegalArgumentException.class,
                 () -> siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER));
    verify(layoutService, never()).save(any(PortalConfig.class));

    style.setIconColor("#AABBCC");
    siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER);
    verify(portalConfig).setPortalLayout(siteLayout);
    verify(layoutService).save(portalConfig);
  }

  @Test
  @SneakyThrows
  public void updateSiteLayoutAcceptsStickySectionScrollColors() {
    // a sticky Topbar or Sidebar stores its two on-scroll colours as one "<top>@<middle>" background value
    PortalConfig site = mock(PortalConfig.class);
    SiteLayout siteLayout = new SiteLayout();
    Container stickySection = new Container();
    ModelStyle style = new ModelStyle();
    stickySection.setCssStyle(style);
    siteLayout.setChildren(new ArrayList<>(List.of(stickySection)));
    when(site.getPortalLayout()).thenReturn(siteLayout);

    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);

    style.setBackgroundColor("#FFF@eval(1)");
    assertThrows(IllegalArgumentException.class,
                 () -> siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER));
    style.setBackgroundColor("#FFFFFF@#20282C@#000000");
    assertThrows(IllegalArgumentException.class,
                 () -> siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER));
    verify(layoutService, never()).save(any(PortalConfig.class));

    style.setBackgroundColor("#FFFFFF@#20282C");
    siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER);
    verify(layoutService).save(portalConfig);
  }

  @Test
  @SneakyThrows
  public void publishingASiteLayoutBroadcastsThePublishedEventAndADraftSaveDoesNot() {
    PortalConfig site = mock(PortalConfig.class);
    when(site.getPortalLayout()).thenReturn(new SiteLayout());
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);

    siteLayoutService.updateSiteLayout(SITE_KEY, site, false, TEST_USER);
    verify(portalConfig).checkStorage();
    verify(listenerService, never()).broadcast(any(String.class), any(), any());

    siteLayoutService.updateSiteLayout(SITE_KEY, site, true, TEST_USER);
    verify(portalConfig).resetStorage();
    verify(listenerService).broadcast(SiteLayoutService.SITE_PUBLISHED_EVENT, TEST_USER, SITE_KEY);
  }

  @Test
  @SneakyThrows
  public void deleteSite() {
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.deleteSite(SITE_KEY, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class, () -> siteLayoutService.deleteSite(SITE_KEY, TEST_USER));
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    siteLayoutService.deleteSite(SITE_KEY, TEST_USER);
    verify(layoutService).remove(portalConfig);
  }

  @Test
  @SneakyThrows
  public void updateSitePermissions() {
    List<String> accessPermissions = Arrays.asList("access", "permissions");
    String editPermission = "edit permission";
    PermissionUpdateModel permissionModel = new PermissionUpdateModel(SITE_KEY.getTypeName(),
                                                                      SITE_KEY.getName(),
                                                                      accessPermissions,
                                                                      editPermission);
    assertThrows(ObjectNotFoundException.class,
                 () -> siteLayoutService.updateSitePermissions(permissionModel, TEST_USER));
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(portalConfig);
    assertThrows(IllegalAccessException.class,
                 () -> siteLayoutService.updateSitePermissions(permissionModel, TEST_USER));
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    siteLayoutService.updateSitePermissions(permissionModel, TEST_USER);
    verify(layoutService).save(portalConfig);
    portalConfig.setAccessPermissions(argThat(p -> Arrays.asList("access", "permissions").equals(Arrays.asList(p))));
    portalConfig.setEditPermission(argThat(editPermission::equals));
  }

  @Test
  @SneakyThrows
  public void getSiteLabels() {
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.getSiteLabels(2l, TEST_USER));
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn("PORTAL");
    when(portalConfig.getName()).thenReturn("test");

    assertThrows(IllegalAccessException.class, () -> siteLayoutService.getSiteLabels(2l, TEST_USER));
    when(aclService.canViewSite(new SiteKey(portalConfig.getType(), portalConfig.getName()),
                                TEST_USER))
                                           .thenReturn(true);

    LocaleConfig enLocaleConfig = new LocaleConfigImpl();
    enLocaleConfig.setLocale(Locale.ENGLISH);
    LocaleConfig frLocaleConfig = new LocaleConfigImpl();
    frLocaleConfig.setLocale(Locale.FRENCH);
    when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(enLocaleConfig, frLocaleConfig));
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortal userPortal = mock(UserPortal.class);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(portalConfigService.getUserPortalConfig("test", TEST_USER)).thenReturn(userPortalConfig);

    SiteKey siteKey = SiteKey.portal("test");
    when(userPortal.getPortalLabel(siteKey, enLocaleConfig.getLocale())).thenReturn("Test Name");
    when(userPortal.getPortalLabel(siteKey, frLocaleConfig.getLocale())).thenReturn("Nom pour le test");

    NodeLabel siteLabels = siteLayoutService.getSiteLabels(2l, TEST_USER);
    assertNotNull(siteLabels);
    assertNotNull(siteLabels.getLabels());
    assertNotNull(siteLabels.getSupportedLanguages());
    assertNotNull(siteLabels.getDefaultLanguage());
    assertEquals("en", siteLabels.getDefaultLanguage());
    assertEquals(2, siteLabels.getSupportedLanguages().size());
    assertTrue(siteLabels.getSupportedLanguages().containsKey(enLocaleConfig.getLocale().getLanguage()));
    assertTrue(siteLabels.getSupportedLanguages().containsKey(frLocaleConfig.getLocale().getLanguage()));
    assertEquals(2, siteLabels.getLabels().size());
    assertEquals("Test Name", siteLabels.getLabels().get(enLocaleConfig.getLocale().getLanguage()));
    assertEquals("Nom pour le test", siteLabels.getLabels().get(frLocaleConfig.getLocale().getLanguage()));
  }

  @Test
  @SneakyThrows
  public void getSiteDescriptions() {
    assertThrows(ObjectNotFoundException.class, () -> siteLayoutService.getSiteDescriptions(2l, TEST_USER));
    when(layoutService.getPortalConfig(2l)).thenReturn(portalConfig);
    when(portalConfig.getType()).thenReturn("PORTAL");
    when(portalConfig.getName()).thenReturn("test");

    assertThrows(IllegalAccessException.class, () -> siteLayoutService.getSiteDescriptions(2l, TEST_USER));
    when(aclService.canViewSite(new SiteKey(portalConfig.getType(), portalConfig.getName()),
                                TEST_USER))
                                           .thenReturn(true);

    LocaleConfig enLocaleConfig = new LocaleConfigImpl();
    enLocaleConfig.setLocale(Locale.ENGLISH);
    LocaleConfig frLocaleConfig = new LocaleConfigImpl();
    frLocaleConfig.setLocale(Locale.FRENCH);
    when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(enLocaleConfig, frLocaleConfig));
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortal userPortal = mock(UserPortal.class);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(portalConfigService.getUserPortalConfig("test", TEST_USER)).thenReturn(userPortalConfig);

    SiteKey siteKey = SiteKey.portal("test");
    when(userPortal.getPortalDescription(siteKey, enLocaleConfig.getLocale())).thenReturn("Test Description");
    when(userPortal.getPortalDescription(siteKey, frLocaleConfig.getLocale())).thenReturn("Description pour le test");

    NodeLabel siteLabels = siteLayoutService.getSiteDescriptions(2l, TEST_USER);
    assertNotNull(siteLabels);
    assertNotNull(siteLabels.getLabels());
    assertNotNull(siteLabels.getSupportedLanguages());
    assertNotNull(siteLabels.getDefaultLanguage());
    assertEquals("en", siteLabels.getDefaultLanguage());
    assertEquals(2, siteLabels.getSupportedLanguages().size());
    assertTrue(siteLabels.getSupportedLanguages().containsKey(enLocaleConfig.getLocale().getLanguage()));
    assertTrue(siteLabels.getSupportedLanguages().containsKey(frLocaleConfig.getLocale().getLanguage()));
    assertEquals(2, siteLabels.getLabels().size());
    assertEquals("Test Description", siteLabels.getLabels().get(enLocaleConfig.getLocale().getLanguage()));
    assertEquals("Description pour le test", siteLabels.getLabels().get(frLocaleConfig.getLocale().getLanguage()));
  }

  private PortalConfig mockDraftSiteCreation() {
    PortalConfig site = new PortalConfig(SITE_KEY.getTypeName(), SITE_KEY.getName());
    Container portalLayout = new Container();
    portalLayout.setTemplate("system:/groovy/portal/webui/container/UIContainer.gtmpl");
    portalLayout.setChildren(new ArrayList<>());
    site.setPortalLayout(portalLayout);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, TEST_USER)).thenReturn(true);
    when(aclService.canViewSite(SITE_KEY, TEST_USER)).thenReturn(true);
    PortalConfig existingDraftSite = new PortalConfig(PortalConfig.DRAFT, DRAFT_SITE_NAME);
    when(layoutService.getPortalConfig(DRAFT_SITE_KEY)).thenReturn(existingDraftSite);
    return existingDraftSite;
  }

  private PortalConfig storedSite() {
    PortalConfig stored = new PortalConfig(SITE_KEY.getTypeName(), SITE_KEY.getName(), "portal_3");
    stored.setLabel("Old");
    stored.setDescription("Old description");
    stored.setIcon("fa-old");
    stored.setDisplayed(true);
    stored.setDisplayOrder(3);
    stored.setAccessPermissions(new String[] { "*:/platform/users" });
    stored.setEditPermission("*:/platform/administrators");
    return stored;
  }

  private void defaultLocale(Locale locale) {
    LocaleConfig localeConfig = mock(LocaleConfig.class);
    when(localeConfig.getLocale()).thenReturn(locale);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(localeConfig);
  }

}
