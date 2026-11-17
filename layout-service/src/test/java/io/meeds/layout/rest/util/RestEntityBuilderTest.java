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
package io.meeds.layout.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;

import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.service.LayoutDarkColorService;

@ExtendWith(MockitoExtension.class)
public class RestEntityBuilderTest {

  private static final String        STORAGE_ID = "42";

  private static final String        CONTENT_ID = "time-tracker/TimeTrackingPortlet";

  @Mock
  private LayoutService              layoutService;

  @Mock
  private LayoutDarkColorService     darkColorService;

  @Mock
  private BrandingService            brandingService;

  private Container                  container;

  private PersistentApplicationState state;

  @BeforeEach
  void setUp() {
    Application application = new Application(STORAGE_ID);
    state = new PersistentApplicationState(STORAGE_ID);
    application.setState(state);
    container = new Container();
    container.setChildren(new ArrayList<>(List.of(application)));
  }

  @Test
  void contentIdIsNotResolvedWithoutExpandNorLightOnlyPortlet() {
    when(darkColorService.hasLightOnlyApplications()).thenReturn(false);

    LayoutModel model = RestEntityBuilder.toLayoutModel(container, layoutService, null, darkColorService);

    verify(layoutService, never()).getId(any());
    assertNull(model.getChildren().get(0).getContentId());
    verify(darkColorService).expand(model, Map.of(), false);
  }

  @Test
  void contentIdIsResolvedAndPassedToExpandWhenAPortletIsLightOnly() {
    when(darkColorService.hasLightOnlyApplications()).thenReturn(true);
    when(layoutService.getId(state)).thenReturn(CONTENT_ID);

    LayoutModel model = RestEntityBuilder.toLayoutModel(container, layoutService, null, darkColorService);

    verify(darkColorService).expand(model, Map.of(STORAGE_ID, CONTENT_ID), true);
    // not exposed as it was not asked for
    assertNull(model.getChildren().get(0).getContentId());
  }

  @Test
  void contentIdIsExposedWhenExpanded() {
    when(darkColorService.hasLightOnlyApplications()).thenReturn(false);
    when(layoutService.getId(state)).thenReturn(CONTENT_ID);

    LayoutModel model = RestEntityBuilder.toLayoutModel(container, layoutService, "contentId", darkColorService);

    assertEquals(CONTENT_ID, model.getChildren().get(0).getContentId());
    verify(darkColorService).expand(model, Map.of(STORAGE_ID, CONTENT_ID), false);
  }

  @Test
  void applicationIsFlaggedLightOnlyThroughTheService() {
    when(brandingService.getLightOnlyApplications()).thenReturn(List.of("TimeTrackingPortlet"));
    when(brandingService.isLightOnlyApplication(CONTENT_ID)).thenReturn(true);
    LayoutDarkColorService realService = new LayoutDarkColorService();
    ReflectionTestUtils.setField(realService, "brandingService", brandingService);
    when(layoutService.getId(state)).thenReturn(CONTENT_ID);

    LayoutModel model = RestEntityBuilder.toLayoutModel(container, layoutService, null, realService);

    assertEquals(Boolean.TRUE, model.getChildren().get(0).getLightOnly());
  }

  @Test
  void withoutServiceNothingIsAdded() {
    LayoutModel model = RestEntityBuilder.toLayoutModel(container, layoutService, null);
    verify(darkColorService, never()).expand(any(), any(), anyBoolean());
    assertNull(model.getChildren().get(0).getLightOnly());
    assertNull(model.getDarkColors());
  }

}
