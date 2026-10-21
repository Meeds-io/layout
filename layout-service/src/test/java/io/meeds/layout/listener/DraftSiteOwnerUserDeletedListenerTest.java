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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import io.meeds.layout.service.SiteLayoutService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class DraftSiteOwnerUserDeletedListenerTest {

  @Mock
  private OrganizationService               organizationService;

  @Mock
  private UserHandler                       userHandler;

  @Mock
  private SiteLayoutService                 siteLayoutService;

  @InjectMocks
  private DraftSiteOwnerUserDeletedListener listener;

  /**
   * Spring calls init: the annotation-removed mutant passes the call made by
   * hand.
   */
  @Test
  @SneakyThrows
  public void init() {
    when(organizationService.getUserHandler()).thenReturn(userHandler);

    listener.init();

    verify(userHandler).addUserEventListener(listener);
    assertTrue(DraftSiteOwnerUserDeletedListener.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

  @Test
  public void postDeleteRemovesTheDraftSitesOutsideTheDeletingThread() {
    AtomicReference<Thread> removingThread = new AtomicReference<>();
    doAnswer(invocation -> {
      removingThread.set(Thread.currentThread());
      return null;
    }).when(siteLayoutService).deleteUserDraftSites("john");
    User user = mock(User.class);
    when(user.getUserName()).thenReturn("john");

    listener.postDelete(user);

    verify(siteLayoutService, timeout(5000)).deleteUserDraftSites("john");
    assertNotEquals(Thread.currentThread(), removingThread.get());
  }

}
