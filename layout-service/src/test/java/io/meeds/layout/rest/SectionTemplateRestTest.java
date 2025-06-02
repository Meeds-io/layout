/**
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
package io.meeds.layout.rest;

import static io.meeds.layout.util.JsonUtils.toJsonString;
import static org.mockito.ArgumentMatchers.any;
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

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.service.SectionTemplateService;
import io.meeds.layout.service.injection.SectionTemplateImportService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { SectionTemplateRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class SectionTemplateRestTest {

  private static final String    REST_PATH     = "/sections";   // NOSONAR

  private static final String    SIMPLE_USER   = "simple";

  private static final String    TEST_PASSWORD = "testPassword";

  @MockBean
  private SectionTemplateService       sectionTemplateService;

  @MockBean
  private SectionTemplateImportService sectionTemplateImportService;

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
  void getSectionTemplatesAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(sectionTemplateService);
  }

  @Test
  void getSectionTemplatesWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(sectionTemplateService).getSectionTemplates(any());
  }

  @Test
  void getSectionTemplateAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1"));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(sectionTemplateService);
  }

  @Test
  void getSectionTemplateWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(sectionTemplateService).getSectionTemplate(eq(1l), any());
  }

  @Test
  void createSectionTemplateAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).content(toJsonString(new SectionTemplate()))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(sectionTemplateService);
  }

  @Test
  void createSectionTemplateWithUser() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    ResultActions response = mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                                            .content(toJsonString(sectionTemplate))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
    verify(sectionTemplateService).createSectionTemplate(sectionTemplate, SIMPLE_USER);
  }

  @Test
  void createSectionTemplateWithUserForbidden() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    when(sectionTemplateService.createSectionTemplate(sectionTemplate,
                                                      SIMPLE_USER)).thenThrow(IllegalAccessException.class);

    ResultActions response = mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                                            .content(toJsonString(sectionTemplate))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void updateSectionTemplateAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").content(toJsonString(new SectionTemplate()))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(sectionTemplateService);
  }

  @Test
  void updateSectionTemplateWithUser() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(sectionTemplate))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());

    sectionTemplate.setId(1l);
    verify(sectionTemplateService).updateSectionTemplate(sectionTemplate, SIMPLE_USER);
  }

  @Test
  void updateSectionTemplateWithUserForbidden() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    sectionTemplate.setId(1l);
    when(sectionTemplateService.updateSectionTemplate(sectionTemplate,
                                                      SIMPLE_USER)).thenThrow(IllegalAccessException.class);

    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(sectionTemplate))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void updateSectionTemplateWithUserNotFound() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    sectionTemplate.setId(1l);
    when(sectionTemplateService.updateSectionTemplate(sectionTemplate,
                                                      SIMPLE_USER)).thenThrow(ObjectNotFoundException.class);

    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(toJsonString(sectionTemplate))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isNotFound());
  }
  
  @Test
  void generateSectionTemplateNodeIdWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1/nodeId").with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(sectionTemplateService).generateSectionTemplateNodeId(1l, SIMPLE_USER);
  }
  
  @Test
  void generateSectionTemplateNodeIdWithUserForbidden() throws Exception {
    when(sectionTemplateService.generateSectionTemplateNodeId(2l, SIMPLE_USER)).thenThrow(IllegalAccessException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/nodeId").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }
  
  @Test
  void generateSectionTemplateNodeIdWithUserNotFound() throws Exception {
    when(sectionTemplateService.generateSectionTemplateNodeId(3l, SIMPLE_USER)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/3/nodeId").with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  void generateSectionTemplateContentWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/1/content").with(testSimpleUser()));
    response.andExpect(status().isOk());
    verify(sectionTemplateService).generateSectionTemplateContent(1l, SIMPLE_USER);
  }

  @Test
  void generateSectionTemplateContentWithUserForbidden() throws Exception {
    when(sectionTemplateService.generateSectionTemplateContent(2l, SIMPLE_USER)).thenThrow(IllegalAccessException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/content").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void generateSectionTemplateContentWithUserNotFound() throws Exception {
    when(sectionTemplateService.generateSectionTemplateContent(3l, SIMPLE_USER)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/3/content").with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  @Test
  void deleteSectionTemplateAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1"));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(sectionTemplateService);
  }

  @Test
  void deleteSectionTemplateWithUser() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());

    sectionTemplate.setId(1l);
    verify(sectionTemplateService).deleteSectionTemplate(1l, SIMPLE_USER);
  }

  @Test
  void deleteSectionTemplateWithUserForbidden() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    sectionTemplate.setId(1l);
    doThrow(IllegalAccessException.class).when(sectionTemplateService).deleteSectionTemplate(1l, SIMPLE_USER);

    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void deleteSectionTemplateWithUserNotFound() throws Exception {
    SectionTemplate sectionTemplate = new SectionTemplate();
    sectionTemplate.setId(1l);
    doThrow(ObjectNotFoundException.class).when(sectionTemplateService).deleteSectionTemplate(1l, SIMPLE_USER);

    ResultActions response = mockMvc.perform(delete(REST_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isNotFound());
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
