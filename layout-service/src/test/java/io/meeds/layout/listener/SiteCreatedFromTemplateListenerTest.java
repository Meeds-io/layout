/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.listener;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.layout.service.PageLayoutService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class SiteCreatedFromTemplateListenerTest {

  @Mock
  private ListenerService                 listenerService;

  @Mock
  private PageLayoutService               pageLayoutService;

  @InjectMocks
  private SiteCreatedFromTemplateListener listener;

  /**
   * A copy of a live site keeps its windows' own settings; a site made from a
   * template, a template made from a site, or any other site type does not.
   */
  @ParameterizedTest
  @CsvSource({
    "PORTAL, PORTAL, true",
    "SPACE, SPACE, true",
    "GROUP, GROUP, true",
    "USER, PORTAL, true",
    "PORTAL_TEMPLATE, PORTAL, false",
    "GROUP_TEMPLATE, SPACE, false",
    "PORTAL, PORTAL_TEMPLATE, false",
    "SPACE, GROUP_TEMPLATE, false",
    "PORTAL, DRAFT, false",
    "DRAFT, PORTAL, false",
  })
  @SneakyThrows
  void onEventMarksOnlyACopyOfALiveSite(SiteType sourceType, SiteType targetType, boolean copy) {
    SiteKey source = new SiteKey(sourceType, "source");
    SiteKey target = new SiteKey(targetType, "target");

    listener.onEvent(new Event<>("site.template.instantiated", source, target));

    verify(pageLayoutService).impersonateSite(target, copy);
  }

  @Test
  @SneakyThrows
  void onEventWithNoSourceIsNoCopy() {
    SiteKey target = SiteKey.portal("target");

    listener.onEvent(new Event<>("site.template.instantiated", null, target));

    verify(pageLayoutService).impersonateSite(target, false);
  }

  /**
   * Spring calls init: the annotation-removed mutant passes the call made by
   * hand.
   */
  @Test
  @SneakyThrows
  void initListensToTheSiteTemplateInstantiation() {
    listener.init();

    verify(listenerService).addListener("site.template.instantiated", listener);
    assertTrue(SiteCreatedFromTemplateListener.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

}
