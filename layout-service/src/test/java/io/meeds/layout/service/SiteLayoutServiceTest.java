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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;

import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SiteUpdateModel;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
  SiteLayoutService.class,
})
@ExtendWith(MockitoExtension.class)
public class SiteLayoutServiceTest {

  private static final String     TEST_USER = "testuser";

  private static final SiteKey    SITE_KEY  = SiteKey.portal("test");

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

}
