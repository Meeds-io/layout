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
package io.meeds.layout.listener;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.layout.service.SiteLayoutService;
import io.meeds.social.security.service.AccountDeletionService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class DraftSiteOwnerAccountDeletedListenerTest {

  @Mock
  private ListenerService                      listenerService;

  @Mock
  private SiteLayoutService                    siteLayoutService;

  @InjectMocks
  private DraftSiteOwnerAccountDeletedListener listener;

  /**
   * Spring calls init: the annotation-removed mutant passes the call made by
   * hand.
   */
  @Test
  @SneakyThrows
  public void init() {
    listener.init();

    verify(listenerService).addListener(AccountDeletionService.ACCOUNT_DELETED_EVENT, listener);
    assertTrue(DraftSiteOwnerAccountDeletedListener.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

  /**
   * The listener service runs a listener outside the broadcasting thread only
   * when its class carries the annotation: the annotation-removed mutant passes
   * every other test.
   */
  @Test
  public void isAsynchronous() {
    assertTrue(DraftSiteOwnerAccountDeletedListener.class.isAnnotationPresent(Asynchronous.class));
  }

  @Test
  public void onEvent() {
    listener.onEvent(new Event<>(AccountDeletionService.ACCOUNT_DELETED_EVENT, "john", null));

    verify(siteLayoutService).deleteUserDraftSites("john");
  }

}
