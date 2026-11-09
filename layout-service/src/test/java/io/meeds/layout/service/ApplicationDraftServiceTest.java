/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.layout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.serialize.model.SiteLayout;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.ApplicationDescription;
import io.meeds.layout.mcp.model.CellDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SiteColumnDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.DraftWindows;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationDraftService.class })
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationDraftServiceTest {

  private static final String        USER      = "john";

  private static final String        PAGE_REF  = "portal::test::home";

  private static final PageKey       PAGE_KEY  = PageKey.parse(PAGE_REF);

  private static final PageKey       DRAFT_KEY = PageKey.parse("portal::test::home_draft_john");

  private static final SiteKey       SITE_KEY  = SiteKey.portal("test");

  private static final SiteKey       DRAFT_SITE_KEY = SiteKey.portal("portal_test_draft_john");

  private static final long          SITE_ID   = 5L;

  private static final long          LIVE_NODE = 11L;

  @MockitoBean
  private LayoutService              layoutService;

  @MockitoBean
  private LayoutAclService           aclService;

  @MockitoBean
  private SiteLayoutService          siteLayoutService;

  @MockitoBean
  private PageLayoutService          pageLayoutService;

  @MockitoBean
  private LayoutDescriptionService   descriptionService;

  @MockitoBean
  private LayoutDraftService         draftService;

  @MockitoBean
  private ApplicationSettingsService settingsService;

  @Autowired
  private ApplicationDraftService    service;

  private Page                       livePage;

  private Page                       draftPage;

  private Application                window;

  private Container                  sectionContainer;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    livePage = mock(Page.class);
    draftPage = mock(Page.class);
    window = window("20", "ide/WidgetPortlet");
    sectionContainer = mock(Container.class);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(livePage);
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(pageLayoutService.getPageLayout(PAGE_KEY, USER)).thenReturn(livePage);
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(livePage);
    when(pageLayoutService.getPageLayout(DRAFT_KEY, USER)).thenReturn(draftPage);
    when(pageLayoutService.getPageLayout(DRAFT_KEY)).thenReturn(draftPage);
    PageDescription pageRead = pageRead("20", "s1");
    when(descriptionService.toPageDescription(any(), eq(PAGE_REF), eq(LayoutVersion.PUBLISHED))).thenReturn(pageRead);
    when(settingsService.pathOf(any(), eq("20"))).thenReturn(List.of(0, 0));
    when(settingsService.atPath(any(), eq(List.of(0, 0)))).thenReturn(window);
  }

  @Test
  public void unknownWriteFailureIsLoggedAsAWarningAndTheOutdatedLayoutIsNot() {
    ListAppender<ILoggingEvent> logged = new ListAppender<>();
    logged.start();
    Logger logger = (Logger) LoggerFactory.getLogger(ApplicationDraftService.class.getName());
    logger.addAppender(logged);
    try {
      RuntimeException outdated = service.translateWriteFailure(new IllegalStateException(ApplicationDraftService.PAGE_OUTDATED_MESSAGE),
                                                                PAGE_KEY,
                                                                USER,
                                                                ApplicationDraftService.PAGE_LAYOUT_NOT_SAVED);
      assertEquals(ApplicationDraftService.LAYOUT_OUTDATED, outdated.getMessage());
      assertTrue(logged.list.isEmpty());

      IllegalStateException failure = new IllegalStateException("engine detail");
      RuntimeException unknown = service.translateWriteFailure(failure, PAGE_KEY, USER, ApplicationDraftService.PAGE_LAYOUT_NOT_SAVED);
      assertEquals(ApplicationDraftService.PAGE_LAYOUT_NOT_SAVED, unknown.getMessage());
      assertEquals(1, logged.list.size());
      assertEquals(Level.WARN, logged.list.get(0).getLevel());
      assertEquals("engine detail", logged.list.get(0).getThrowableProxy().getMessage());
    } finally {
      logger.detachAppender(logged);
    }
  }

  @Test
  @SneakyThrows
  public void aPageTheUserCannotEditOrThatDoesNotExistIsRefusedBeforeAnyRead() {
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(false);
    assertEquals(ApplicationDraftService.PAGE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).getMessage());
    when(layoutService.getPage(PAGE_KEY)).thenReturn(null);
    assertEquals(ApplicationDraftService.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).getMessage());
    when(layoutService.getPage(DRAFT_KEY)).thenReturn(draftPage);
    when(draftService.isDraftNode(DRAFT_KEY.getName())).thenReturn(true);
    assertEquals(ApplicationDraftService.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> service.resolvePage(DRAFT_KEY, DRAFT_KEY.format(), List.of("20"), USER)).getMessage());
    verify(pageLayoutService, never()).getPageLayout(any(), any());
  }

  @Test
  @SneakyThrows
  public void aPageWithNoNavigationItemHasNoDraftToAddress() {
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(null);
    assertEquals(ApplicationDraftService.NODE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void theWindowsAreResolvedOnTheLiveLayoutWhenTheUserHasNoDraft() {
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(settingsService.newContext(window, PAGE_KEY, SITE_KEY, SITE_KEY, PAGE_REF, false, USER)).thenReturn(context);

    DraftWindows resolved = service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);

    assertTrue(resolved.isPage());
    assertTrue(resolved.createDraft());
    assertEquals(LIVE_NODE, resolved.liveNodeId());
    assertNull(resolved.draftPageKey());
    assertSame(livePage, resolved.storedLayout());
    assertEquals(List.of(window), resolved.windows());
    assertEquals(List.of(context), resolved.contexts());
    verify(pageLayoutService, never()).getPageLayout(DRAFT_KEY, USER);
  }

  @Test
  @SneakyThrows
  public void theWindowsAreResolvedOnTheDraftTheRevisionNames() {
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(settingsService.newContext(window, DRAFT_KEY, SITE_KEY, SITE_KEY, PAGE_REF, true, USER)).thenReturn(context);

    DraftWindows resolved = service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);

    assertFalse(resolved.createDraft());
    assertEquals(DRAFT_KEY, resolved.draftPageKey());
    assertSame(draftPage, resolved.storedLayout());
    assertEquals(List.of(context), resolved.contexts());
    verify(pageLayoutService, never()).getPageLayout(PAGE_KEY, USER);

    doThrow(new ObjectNotFoundException("gone")).when(pageLayoutService).getPageLayout(DRAFT_KEY, USER);
    assertEquals(ApplicationDraftService.DRAFT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aContainerIdIsRefusedAndAnIdTheReadDoesNotShowDoesNotExist() {
    assertEquals(ApplicationDraftService.APPLICATION_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("s1"), USER)).getMessage());
    assertEquals(ApplicationDraftService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("99"), USER)).getMessage());
    when(settingsService.pathOf(any(), eq("20"))).thenReturn(null);
    assertEquals(ApplicationDraftService.APPLICATION_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void theDraftIsCreatedAndEachWindowFoundAgainAtItsPlace() {
    DraftWindows resolved = service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    Application draftWindow = window("31", "ide/WidgetPortlet");
    when(settingsService.atPath(draftPage, List.of(0, 0))).thenReturn(draftWindow);
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(settingsService.newContext(draftWindow, DRAFT_KEY, SITE_KEY, SITE_KEY, PAGE_REF, true, USER)).thenReturn(context);

    assertEquals(List.of(context), service.createDraft(resolved, USER));
  }

  @Test
  @SneakyThrows
  public void aLayoutThatChangedBetweenTheReadAndTheCloneIsOutdatedAndNamesTheDraftCreated() {
    DraftWindows resolved = service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    Application movedWindow = window("21", "ide/WidgetPortlet");
    Application draftWindow = window("31", "ide/WidgetPortlet");
    when(settingsService.atPath(livePage, List.of(0, 0))).thenReturn(movedWindow);
    when(settingsService.atPath(draftPage, List.of(0, 0))).thenReturn(draftWindow);

    IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.createDraft(resolved, USER));
    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, ApplicationDraftService.LAYOUT_OUTDATED),
                 failure.getMessage());

    when(settingsService.atPath(livePage, List.of(0, 0))).thenReturn(window);
    Application otherApplication = window("31", "content/Links");
    when(settingsService.atPath(draftPage, List.of(0, 0))).thenReturn(otherApplication);
    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, ApplicationDraftService.LAYOUT_OUTDATED),
                 assertThrows(IllegalStateException.class, () -> service.createDraft(resolved, USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aDraftCreationRefusalIsAnsweredWithTheFixedTexts() {
    DraftWindows resolved = service.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);
    doThrow(new IllegalAccessException("no navigation right")).when(draftService).createDraftPage(LIVE_NODE, USER);
    assertEquals(ApplicationDraftService.NAVIGATION_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> service.createDraft(resolved, USER)).getMessage());
    doThrow(new ObjectNotFoundException("gone")).when(draftService).createDraftPage(LIVE_NODE, USER);
    assertEquals(ApplicationDraftService.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.createDraft(resolved, USER)).getMessage());
    doThrow(new IllegalStateException(ApplicationDraftService.PAGE_OUTDATED_MESSAGE)).when(draftService).createDraftPage(LIVE_NODE, USER);
    assertEquals(ApplicationDraftService.LAYOUT_OUTDATED,
                 assertThrows(IllegalStateException.class, () -> service.createDraft(resolved, USER)).getMessage());
    doThrow(new IllegalStateException("engine text")).when(draftService).createDraftPage(LIVE_NODE, USER);
    assertEquals(ApplicationDraftService.PAGE_LAYOUT_NOT_SAVED,
                 assertThrows(IllegalStateException.class, () -> service.createDraft(resolved, USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aWriteOnTheDraftJustCreatedNamesTheDraftWhenItFails() {
    assertEquals("done", service.onCreatedDraft(() -> "done"));
    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, "not allowed: application"),
                 assertThrows(IllegalStateException.class, () -> service.onCreatedDraft(() -> {
                   throw new IllegalAccessException("not allowed: application");
                 })).getMessage());
    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, "value is not accepted for type"),
                 assertThrows(IllegalStateException.class, () -> service.onCreatedDraft(() -> {
                   throw new IllegalArgumentException("value is not accepted for type");
                 })).getMessage());
    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, ApplicationSettingsService.APPLICATION_NOT_SAVED),
                 assertThrows(IllegalStateException.class, () -> service.onCreatedDraft(() -> {
                   throw new NullPointerException("engine text");
                 })).getMessage());
  }

  @Test
  @SneakyThrows
  public void theWindowsOfASiteAreResolvedOnTheDraftSiteWhenOneExists() {
    PortalConfig site = site(SITE_KEY, PortalConfig.PORTAL_TYPE);
    PortalConfig draftSite = site(DRAFT_SITE_KEY, PortalConfig.DRAFT);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(DRAFT_SITE_KEY);
    when(siteLayoutService.getSite(DRAFT_SITE_KEY, USER)).thenReturn(draftSite);
    when(layoutService.getPortalConfig(DRAFT_SITE_KEY)).thenReturn(draftSite);
    SiteLayout draftLayout = (SiteLayout) draftSite.getPortalLayout();
    when(descriptionService.filterAccessible(draftLayout, USER)).thenReturn(draftLayout);
    SiteDescription siteRead = siteRead("20", "top");
    when(descriptionService.toSiteDescription(site, draftLayout, null, null, false)).thenReturn(siteRead);
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(settingsService.getSiteHomePageReference(SITE_KEY)).thenReturn(PAGE_REF);
    when(settingsService.newContext(window, null, DRAFT_SITE_KEY, SITE_KEY, PAGE_REF, true, USER)).thenReturn(context);

    DraftWindows resolved = service.resolveSite(SITE_ID, List.of("20"), USER);

    assertFalse(resolved.isPage());
    assertFalse(resolved.createDraft());
    assertSame(site, resolved.liveSite());
    assertSame(draftSite, resolved.draftSite());
    assertEquals(List.of(context), resolved.contexts());
    assertEquals(PAGE_REF, resolved.homePageReference());
    verify(settingsService, times(1)).getSiteHomePageReference(SITE_KEY);
    assertEquals(ApplicationDraftService.APPLICATION_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.resolveSite(SITE_ID, List.of("top"), USER)).getMessage());
    assertEquals(ApplicationDraftService.APPLICATION_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> service.resolveSite(SITE_ID, List.of("top-column"), USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aDraftSiteOrASiteTheUserCannotEditIsRefused() {
    PortalConfig draftSite = site(DRAFT_SITE_KEY, PortalConfig.DRAFT);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(draftSite);
    assertEquals(ApplicationDraftService.SITE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> service.resolveSite(SITE_ID, List.of("20"), USER)).getMessage());
    PortalConfig site = site(SITE_KEY, PortalConfig.PORTAL_TYPE);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    assertEquals(ApplicationDraftService.SITE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> service.resolveSite(SITE_ID, List.of("20"), USER)).getMessage());
    doThrow(new IllegalAccessException("hidden")).when(siteLayoutService).getSite(SITE_ID, USER);
    assertEquals(ApplicationDraftService.SITE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> service.resolveSite(SITE_ID, List.of("20"), USER)).getMessage());
  }

  @Test
  @SneakyThrows
  public void theDraftSiteIsCreatedAndTheWindowFoundAgainOnIt() {
    PortalConfig site = site(SITE_KEY, PortalConfig.PORTAL_TYPE);
    PortalConfig draftSite = site(DRAFT_SITE_KEY, PortalConfig.DRAFT);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
    SiteLayout liveLayout = (SiteLayout) site.getPortalLayout();
    when(descriptionService.filterAccessible(liveLayout, USER)).thenReturn(liveLayout);
    SiteDescription siteRead = siteRead("20", "top");
    when(descriptionService.toSiteDescription(site, liveLayout, null, null, false)).thenReturn(siteRead);
    when(settingsService.getSiteHomePageReference(SITE_KEY)).thenReturn(PAGE_REF);
    DraftWindows resolved = service.resolveSite(SITE_ID, List.of("20"), USER);
    assertTrue(resolved.createDraft());

    when(draftService.createDraftSite(site, USER)).thenReturn(DRAFT_SITE_KEY);
    when(layoutService.getPortalConfig(DRAFT_SITE_KEY)).thenReturn(draftSite);
    Application draftWindow = window("31", "ide/WidgetPortlet");
    SiteLayout draftLayout = (SiteLayout) draftSite.getPortalLayout();
    when(settingsService.atPath(draftLayout, List.of(0, 0))).thenReturn(draftWindow);
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(settingsService.newContext(draftWindow, null, DRAFT_SITE_KEY, SITE_KEY, PAGE_REF, true, USER)).thenReturn(context);

    assertEquals(List.of(context), service.createDraft(resolved, USER));
    verify(settingsService, times(1)).getSiteHomePageReference(SITE_KEY);

    doThrow(new IllegalStateException("engine text")).when(draftService).createDraftSite(site, USER);
    assertEquals(ApplicationDraftService.SITE_LAYOUT_NOT_SAVED,
                 assertThrows(IllegalStateException.class, () -> service.createDraft(resolved, USER)).getMessage());
  }

  private Application window(String storageId, String contentId) {
    Application application = mock(Application.class);
    ApplicationState state = mock(ApplicationState.class);
    lenient().when(application.getStorageId()).thenReturn(storageId);
    lenient().when(application.getState()).thenReturn(state);
    lenient().when(layoutService.getId(state)).thenReturn(contentId);
    return application;
  }

  private PortalConfig site(SiteKey siteKey, String type) {
    PortalConfig site = mock(PortalConfig.class);
    SiteLayout layout = mock(SiteLayout.class);
    lenient().when(site.getSiteKey()).thenReturn(siteKey);
    lenient().when(site.getType()).thenReturn(type);
    lenient().when(site.getPortalLayout()).thenReturn(layout);
    return site;
  }

  private PageDescription pageRead(String applicationId, String sectionId) {
    ApplicationDescription application = mock(ApplicationDescription.class);
    when(application.applicationId()).thenReturn(applicationId);
    SectionDescription section = mock(SectionDescription.class);
    when(section.sectionId()).thenReturn(sectionId);
    when(section.columnsContent()).thenReturn(List.of(List.of(application)));
    when(section.cells()).thenReturn(List.<CellDescription> of());
    PageDescription page = mock(PageDescription.class);
    when(page.sections()).thenReturn(List.of(section));
    return page;
  }

  private SiteDescription siteRead(String applicationId, String containerId) {
    ApplicationDescription application = mock(ApplicationDescription.class);
    when(application.applicationId()).thenReturn(applicationId);
    SiteContainerDescription container = new SiteContainerDescription(containerId,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      List.of(new SiteColumnDescription(containerId + "-column", null, List.of(application))));
    SiteDescription site = mock(SiteDescription.class);
    when(site.topbars()).thenReturn(List.of(container));
    when(site.bottomBars()).thenReturn(List.of());
    return site;
  }

}
