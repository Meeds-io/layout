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
package io.meeds.layout.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.rest.entity.SiteEntity;

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.rest.util.RestEntityBuilder;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { SiteLayoutRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class SiteLayoutRestTest {

  private static final String   SIMPLE_USER                   = "simple";

  private static final String   TEST_PASSWORD                 = "testPassword";

  private static final String   SITE_NAME                     = "testsitename";

  private static final SiteKey  SITE_KEY                      = new SiteKey(PortalConfig.DRAFT, SITE_NAME);

  private static final String   REST_PATH                     = "/sites";                                                   // NOSONAR

  private static final String   LAYOUT_REST_PATH              = REST_PATH + "/layout";

  private static final String   LAYOUT_REST_PATH_WITH_PARAMS  = LAYOUT_REST_PATH + "?siteType=DRAFT&siteName=" + SITE_NAME;

  private static final String   GET_SITE_REST_PATH            = REST_PATH + "?lang=fr&siteType=DRAFT&siteName=" + SITE_NAME;

  private static final String   DELETE_LAYOUT_REST_PATH       = REST_PATH + "?siteType=DRAFT&siteName=" + SITE_NAME;

  private static final String   CREATE_DRAFT_LAYOUT_REST_PATH = REST_PATH + "/draft?siteType=DRAFT&siteName=" + SITE_NAME;

  private static final String   RESTORE_LAYOUT_REST_PATH      = REST_PATH + "/restore";

  private static final String   RESTORE_LAYOUT_PARAMS         = "siteType=DRAFT&siteName=" + SITE_NAME +
      "&importMode=INSERT&siteLayout=true&pagesLayout=true&navigation=true";

  @MockBean
  private SiteLayoutService     siteLayoutService;

  @MockBean
  private LayoutService         layoutService;

  @MockBean
  private UserPortalConfigService portalConfigService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  @Mock
  private NodeLabel             nodeLabel;

  private MockMvc               mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  void getSiteByIdWhenNotFound() throws Exception {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .getSite(2l, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2").with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getSiteByIdWhenIllegalAccess() throws Exception {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .getSite(2l, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getSiteById() throws Exception {
    PortalConfig site = mock(PortalConfig.class);
    when(siteLayoutService.getSite(2l, SIMPLE_USER)).thenReturn(site);
    try (MockedStatic<RestEntityBuilder> restEntityBuilder = mockStatic(RestEntityBuilder.class)) {
      restEntityBuilder.when(() -> RestEntityBuilder.toSiteEntity(any(), any(), any(), any())).thenReturn(mock(SiteEntity.class));
      ResultActions response = mockMvc.perform(get(REST_PATH + "/2").with(testSimpleUser()));
      response.andExpect(status().isOk());
    }
  }

  @Test
  void getSiteWhenNotFound() throws Exception {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .getSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(GET_SITE_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getSiteWhenIllegalAccess() throws Exception {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .getSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(GET_SITE_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getSite() throws Exception {
    PortalConfig site = mock(PortalConfig.class);
    when(siteLayoutService.getSite(SITE_KEY, SIMPLE_USER)).thenReturn(site);
    when(site.getId()).thenReturn(2l);
    when(siteLayoutService.getSite(2l, SIMPLE_USER)).thenReturn(site);
    try (MockedStatic<RestEntityBuilder> restEntityBuilder = mockStatic(RestEntityBuilder.class)) {
      restEntityBuilder.when(() -> RestEntityBuilder.toSiteEntity(any(), any(), any(), any())).thenReturn(mock(SiteEntity.class));
      ResultActions response = mockMvc.perform(get(GET_SITE_REST_PATH).with(testSimpleUser()));
      response.andExpect(status().isOk());
    }
  }

  @Test
  void getSiteLayoutWhenNotFound() throws Exception {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .getSiteLayout(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(LAYOUT_REST_PATH_WITH_PARAMS).with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getSiteLayoutWhenIllegalAccess() throws Exception {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .getSiteLayout(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(get(LAYOUT_REST_PATH_WITH_PARAMS).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getSiteLayout() throws Exception {
    ModelObject modelObject = mock(ModelObject.class);
    when(siteLayoutService.getSiteLayout(SITE_KEY, SIMPLE_USER)).thenReturn(modelObject);
    LayoutModel layoutModel = mock(LayoutModel.class);
    try (MockedStatic<RestEntityBuilder> restEntityBuilder = mockStatic(RestEntityBuilder.class)) {
      restEntityBuilder.when(() -> RestEntityBuilder.toLayoutModel(any(), any(), any())).thenReturn(layoutModel);
      ResultActions response = mockMvc.perform(get(LAYOUT_REST_PATH_WITH_PARAMS).with(testSimpleUser()));
      response.andExpect(status().isOk());
    }
  }

  @Test
  @SneakyThrows
  void deleteSiteWhenNotFound() {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .deleteSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(delete(DELETE_LAYOUT_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void deleteSiteWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .deleteSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(delete(DELETE_LAYOUT_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void deleteSite() {
    ResultActions response = mockMvc.perform(delete(DELETE_LAYOUT_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void updateSiteWhenNotFound() {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .updateSite(any(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(put(REST_PATH).content("{}")
                                                           .contentType(MediaType.APPLICATION_JSON)
                                                           .with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void updateSiteWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .updateSite(any(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(put(REST_PATH).content("{}")
                                                           .contentType(MediaType.APPLICATION_JSON)
                                                           .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void updateSite() {
    ResultActions response = mockMvc.perform(put(REST_PATH).content("{}")
                                                           .contentType(MediaType.APPLICATION_JSON)
                                                           .with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void restoreSiteWhenNotFound() {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .restoreSite(SITE_KEY, ImportMode.INSERT, true, true, true, SIMPLE_USER);
    ResultActions response =
                           mockMvc.perform(put(RESTORE_LAYOUT_REST_PATH).contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                                                        .content(RESTORE_LAYOUT_PARAMS)
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void restoreSiteWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .restoreSite(SITE_KEY, ImportMode.INSERT, true, true, true, SIMPLE_USER);
    ResultActions response =
                           mockMvc.perform(put(RESTORE_LAYOUT_REST_PATH).contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                                                        .content(RESTORE_LAYOUT_PARAMS)
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void restoreSiteWhenIllegalState() {
    doThrow(IllegalStateException.class).when(siteLayoutService)
                                        .restoreSite(SITE_KEY, ImportMode.INSERT, true, true, true, SIMPLE_USER);
    ResultActions response =
                           mockMvc.perform(put(RESTORE_LAYOUT_REST_PATH).contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                                                        .content(RESTORE_LAYOUT_PARAMS)
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void restoreSite() {
    ResultActions response =
                           mockMvc.perform(put(RESTORE_LAYOUT_REST_PATH).contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                                                        .content(RESTORE_LAYOUT_PARAMS)
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void createSiteWhenNotFound() {
    doThrow(ObjectAlreadyExistsException.class).when(siteLayoutService)
                                               .createSite(any(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(post(REST_PATH).content("{}")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .with(testSimpleUser()));
    response.andExpect(status().isConflict());
  }

  @Test
  @SneakyThrows
  void createSiteWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .createSite(any(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(post(REST_PATH).content("{}")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void createSite() {
    ResultActions response = mockMvc.perform(post(REST_PATH).content("{}")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void createDraftSiteWhenNotFound() {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .createDraftSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(post(CREATE_DRAFT_LAYOUT_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void createDraftSiteWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .createDraftSite(SITE_KEY, SIMPLE_USER);
    ResultActions response = mockMvc.perform(post(CREATE_DRAFT_LAYOUT_REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void createDraftSite() {
    SiteKey draftSiteKey = new SiteKey(SiteType.DRAFT, "draftTest");
    PortalConfig draftSite = mock(PortalConfig.class);
    when(draftSite.getId()).thenReturn(3l);
    when(siteLayoutService.getSite(3l, SIMPLE_USER)).thenReturn(draftSite);
    when(siteLayoutService.getSite(draftSiteKey, SIMPLE_USER)).thenReturn(draftSite);
    when(siteLayoutService.createDraftSite(SITE_KEY, SIMPLE_USER)).thenReturn(draftSiteKey);
    try (MockedStatic<RestEntityBuilder> restEntityBuilder = mockStatic(RestEntityBuilder.class)) {
      restEntityBuilder.when(() -> RestEntityBuilder.toSiteEntity(any(), any(), any(), any())).thenReturn(mock(SiteEntity.class));
      ResultActions response = mockMvc.perform(post(CREATE_DRAFT_LAYOUT_REST_PATH).with(testSimpleUser()));
      response.andExpect(status().isOk());
    }
  }

  @Test
  @SneakyThrows
  void updateSiteLayoutWhenNotFound() {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .updateSiteLayout(eq(SITE_KEY), any(), anyBoolean(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(put(LAYOUT_REST_PATH_WITH_PARAMS).content("{}")
                                                                              .contentType(MediaType.APPLICATION_JSON)
                                                                              .with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void updateSiteLayoutWhenIllegalAccess() {
    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .updateSiteLayout(eq(SITE_KEY), any(), anyBoolean(), eq(SIMPLE_USER));
    ResultActions response = mockMvc.perform(put(LAYOUT_REST_PATH_WITH_PARAMS).content("{}")
                                                                              .contentType(MediaType.APPLICATION_JSON)
                                                                              .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  void updateSiteLayout() {
    ModelObject modelObject = mock(ModelObject.class);
    when(siteLayoutService.getSiteLayout(SITE_KEY, SIMPLE_USER)).thenReturn(modelObject);
    LayoutModel layoutModel = mock(LayoutModel.class);
    try (MockedStatic<RestEntityBuilder> restEntityBuilder = mockStatic(RestEntityBuilder.class)) {
      restEntityBuilder.when(() -> RestEntityBuilder.toLayoutModel(any(), any(), any())).thenReturn(layoutModel);
      ResultActions response = mockMvc.perform(put(LAYOUT_REST_PATH_WITH_PARAMS).content("{}")
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .with(testSimpleUser()));
      response.andExpect(status().isOk());
    }
  }

  @Test
  void getSiteLabels() throws Exception {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .getSiteLabels(2l, null);

    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/labels"));
    response.andExpect(status().isNotFound());

    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .getSiteLabels(3l, null);
    response = mockMvc.perform(get(REST_PATH + "/3/labels"));
    response.andExpect(status().isForbidden());

    doReturn(nodeLabel).when(siteLayoutService)
                       .getSiteLabels(4l, null);
    when(nodeLabel.getDefaultLanguage()).thenReturn("en");

    response = mockMvc.perform(get(REST_PATH + "/4/labels"));
    response.andExpect(status().isOk())
            .andExpect(jsonPath("$.defaultLanguage").value("en"));
  }

  @Test
  void getSiteDescriptions() throws Exception {
    doThrow(ObjectNotFoundException.class).when(siteLayoutService)
                                          .getSiteDescriptions(2l, null);

    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/descriptions"));
    response.andExpect(status().isNotFound());

    doThrow(IllegalAccessException.class).when(siteLayoutService)
                                         .getSiteDescriptions(3l, null);
    response = mockMvc.perform(get(REST_PATH + "/3/descriptions"));
    response.andExpect(status().isForbidden());

    doReturn(nodeLabel).when(siteLayoutService)
                       .getSiteDescriptions(4l, null);
    when(nodeLabel.getDefaultLanguage()).thenReturn("en");

    response = mockMvc.perform(get(REST_PATH + "/4/descriptions"));
    response.andExpect(status().isOk())
            .andExpect(jsonPath("$.defaultLanguage").value("en"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
