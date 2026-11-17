/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.portal.branding.BrandingService;

import io.meeds.layout.service.LayoutDarkColorService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { LayoutColorRest.class, LayoutDarkColorService.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class LayoutColorRestTest {

  private static final String    REST_PATH     = "/colors/dark"; // NOSONAR

  private static final String    SIMPLE_USER   = "simple";

  private static final String    TEST_PASSWORD = "testPassword";

  @MockitoBean
  private BrandingService    brandingService;

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
  void anonymousIsForbidden() throws Exception {
    mockMvc.perform(post(REST_PATH).contentType(MediaType.APPLICATION_JSON).content("[]"))
           .andExpect(status().isForbidden());
  }

  @Test
  void signedInUserGetsTheDarkColorsOfItsBlocks() throws Exception {
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("[{\"colors\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#20282C\"}},"
                                       + "{\"colors\":{\"textColor\":\"#FFFFFF\"},\"image\":true}]"))
           .andExpect(status().isOk())
           .andExpect(content().json("[{\"backgroundColor\":\"#1E2328\",\"textColor\":\"\"},{}]", true));
  }

  @Test
  void aNullColorInABlockIsAnUnsetColor() throws Exception {
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("[{\"colors\":{\"backgroundColor\":null,\"textColor\":\"#20282C\"}},"
                                       + "{\"colors\":{\"backgroundColor\":\"#FFFFFF\",\"textColor\":null}}]"))
           .andExpect(status().isOk())
           .andExpect(content().json("[{\"textColor\":\"\"},{\"backgroundColor\":\"#1E2328\"}]", true));
  }

  @Test
  void unparseableColorIsNotAnError() throws Exception {
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("[{\"colors\":{\"backgroundColor\":\"var(--x)\"}}]"))
           .andExpect(status().isOk())
           .andExpect(content().json("[{}]", true));
  }

  @Test
  void hundredBlocksAreAcceptedAndMoreIsABadRequest() throws Exception {
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(blocks(100)))
           .andExpect(status().isOk());
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(blocks(101)))
           .andExpect(status().isBadRequest())
           .andExpect(status().reason("layout.colors.tooManyBlocks"));
  }

  @Test
  void colorOutsideTheValidatorCharacterClassIsABadRequest() throws Exception {
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("[{\"colors\":{\"textColor\":\"red;background:url(x)\"}}]"))
           .andExpect(status().isBadRequest())
           .andExpect(status().reason("layout.colors.invalidColor"));
    mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("[{\"colors\":{\"textColor\":\"#" + "a".repeat(200) + "\"}}]"))
           .andExpect(status().isBadRequest())
           .andExpect(status().reason("layout.colors.invalidColor"));
  }

  private String blocks(int count) {
    return "[" + String.join(",", Collections.nCopies(count, "{\"colors\":{\"backgroundColor\":\"#FFFFFF\"}}")) + "]";
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
