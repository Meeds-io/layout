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

import static io.meeds.layout.util.JsonUtils.toJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.PortletInstanceCategory;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { PortletInstanceCategoryRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class PortletInstanceCategoryRestTest {

  private static final String REST_PATH     = "/portlet/instance/categories"; // NOSONAR

  private static final String SIMPLE_USER   = "simple";

  private static final String TEST_PASSWORD = "testPassword";

  @MockitoBean
  private PortletInstanceService portletInstanceService;

  @Autowired
  private SecurityFilterChain    filterChain;

  @Autowired
  private WebApplicationContext  context;

  private MockMvc                mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  void getPortletInstanceCategorysAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(portletInstanceService);
  }

  @Test
  void getPortletInstanceCategorysWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(portletInstanceService).getPortletInstanceCategories(any(), any(), anyBoolean());
  }

  @Test
  void getPortletInstanceCategoryAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1"));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(portletInstanceService);
  }

  @Test
  void getPortletInstanceCategoryWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(portletInstanceService).getPortletInstanceCategory(eq(1l), any(), any(), eq(true));
  }

  @Test
  void createPortletInstanceCategoryAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).content(toJsonString(new PortletInstanceCategory()))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(portletInstanceService);
  }

  @Test
  void createPortletInstanceCategoryWithUser() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    ResultActions response = mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                                            .content(toJsonString(portletInstanceCategory))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
    verify(portletInstanceService).createPortletInstanceCategory(portletInstanceCategory, SIMPLE_USER);
  }

  @Test
  void createPortletInstanceCategoryWithUserForbidden() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    when(portletInstanceService.createPortletInstanceCategory(portletInstanceCategory,
                                                              SIMPLE_USER)).thenThrow(IllegalAccessException.class);

    ResultActions response = mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                                            .content(toJsonString(portletInstanceCategory))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void updatePortletInstanceCategoryAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").content(toJsonString(new PortletInstanceCategory()))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(portletInstanceService);
  }

  @Test
  void updatePortletInstanceCategoryWithUser() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(portletInstanceCategory))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());

    portletInstanceCategory.setId(1l);
    verify(portletInstanceService).updatePortletInstanceCategory(portletInstanceCategory, SIMPLE_USER);
  }

  @Test
  void updatePortletInstanceCategoryWithUserForbidden() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    portletInstanceCategory.setId(1l);
    when(portletInstanceService.updatePortletInstanceCategory(portletInstanceCategory,
                                                              SIMPLE_USER)).thenThrow(IllegalAccessException.class);

    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(portletInstanceCategory))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void updatePortletInstanceCategoryWithUserNotFound() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    portletInstanceCategory.setId(1l);
    when(portletInstanceService.updatePortletInstanceCategory(portletInstanceCategory,
                                                              SIMPLE_USER)).thenThrow(ObjectNotFoundException.class);

    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(portletInstanceCategory))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isNotFound());
  }

  @Test
  void deletePortletInstanceCategoryAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1"));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(portletInstanceService);
  }

  @Test
  void deletePortletInstanceCategoryWithUser() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());

    portletInstanceCategory.setId(1l);
    verify(portletInstanceService).deletePortletInstanceCategory(1l, SIMPLE_USER);
  }

  @Test
  void deletePortletInstanceCategoryWithUserForbidden() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    portletInstanceCategory.setId(1l);
    doThrow(IllegalAccessException.class).when(portletInstanceService).deletePortletInstanceCategory(1l, SIMPLE_USER);

    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void deletePortletInstanceCategoryWithUserNotFound() throws Exception {
    PortletInstanceCategory portletInstanceCategory = new PortletInstanceCategory();
    portletInstanceCategory.setId(1l);
    doThrow(ObjectNotFoundException.class).when(portletInstanceService).deletePortletInstanceCategory(1l, SIMPLE_USER);

    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
