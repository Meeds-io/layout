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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;

import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.rest.util.RestEntityBuilder;
import io.meeds.layout.service.PageLayoutService;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { PageLayoutRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class PageLayoutRestTest {

  private static final String   SIMPLE_USER   = "simple";

  private static final String   TEST_PASSWORD = "testPassword";

  private static final String   PAGE_REF      = "portal::test::home_draft_john";

  private static final PageKey  PAGE_KEY      = PageKey.parse(PAGE_REF);

  private static final String   REST_PATH     = "/pages";                                 // NOSONAR

  private static final String   LAYOUT_PATH   = REST_PATH + "/layout?pageRef=" + PAGE_REF;

  private static final String   BY_REF_PATH   = REST_PATH + "/byRef?pageRef=" + PAGE_REF;

  @MockitoBean
  private PageLayoutService     pageLayoutService;

  @MockitoBean
  private LayoutService         layoutService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  private MockMvc               mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  /**
   * A page read's answer depends on the reader's rights, a draft's on the
   * live page's edit permission since the draft carries no access permission
   * of its own: no shared cache may serve it to another reader, on any status
   */
  @Test
  void pageReadsCarryAPrivateNoStoreCacheControlOnEveryStatus() throws Exception {
    when(pageLayoutService.getPageLayout(PAGE_KEY, 0L, false, SIMPLE_USER)).thenReturn(new Container("draft"));
    LayoutModel model = new LayoutModel();
    try (MockedStatic<RestEntityBuilder> builder = mockStatic(RestEntityBuilder.class)) {
      builder.when(() -> RestEntityBuilder.toLayoutModel(any(), any(), isNull())).thenReturn(model);
      mockMvc.perform(get(LAYOUT_PATH).with(testSimpleUser()))
             .andExpect(status().isOk())
             .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
      mockMvc.perform(get(LAYOUT_PATH).header(HttpHeaders.IF_NONE_MATCH, "\"" + model.hashCode() + "\"").with(testSimpleUser()))
             .andExpect(status().isNotModified())
             .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
    }
    doThrow(ObjectNotFoundException.class).when(pageLayoutService).getPageLayout(PAGE_KEY, 0L, false, SIMPLE_USER);
    mockMvc.perform(get(LAYOUT_PATH).with(testSimpleUser()))
           .andExpect(status().isNotFound())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
    doThrow(IllegalAccessException.class).when(pageLayoutService).getPageLayout(PAGE_KEY, 0L, false, SIMPLE_USER);
    mockMvc.perform(get(LAYOUT_PATH).with(testSimpleUser()))
           .andExpect(status().isForbidden())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));

    mockMvc.perform(get(BY_REF_PATH).with(testSimpleUser()))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
    doThrow(ObjectNotFoundException.class).when(pageLayoutService).getPage(PAGE_KEY, SIMPLE_USER);
    mockMvc.perform(get(BY_REF_PATH).with(testSimpleUser()))
           .andExpect(status().isNotFound())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
    doThrow(IllegalAccessException.class).when(pageLayoutService).getPage(PAGE_KEY, SIMPLE_USER);
    mockMvc.perform(get(BY_REF_PATH).with(testSimpleUser()))
           .andExpect(status().isForbidden())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));

    when(pageLayoutService.getPages(isNull(), isNull(), isNull(), anyInt(), anyInt(), eq(SIMPLE_USER))).thenReturn(List.of());
    mockMvc.perform(get(REST_PATH).with(testSimpleUser()))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PageLayoutRest.PRIVATE_NO_STORE));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
