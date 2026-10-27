/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationSettingsService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationSettingsServiceTest {

  private static final String        USER       = "john";

  private static final PageKey       DRAFT_PAGE = PageKey.parse("portal::test::home_draft_john");

  private static final SiteKey       SITE_KEY   = SiteKey.portal("test");

  @MockitoBean
  private LayoutService              layoutService;

  @MockitoBean
  private PageLayoutService          pageLayoutService;

  @MockitoBean
  private SiteLayoutService          siteLayoutService;

  @MockitoBean
  private SpaceService               spaceService;

  @Autowired
  private ApplicationSettingsService service;

  @AfterEach
  public void tearDown() {
    service.removePlugin("Links");
    service.removePlugin("AppCenterMyApplicationsPortlet");
  }

  @Test
  public void anApplicationIsConfigurableWhereItsPluginAcceptsIt() {
    assertFalse(service.isConfigurable("content/Links", false));
    assertNull(service.getSettingsSchema("content/Links"));
    ApplicationSettingsPlugin links = plugin("Links", false);
    ApplicationSettingsPlugin myApplications = plugin("AppCenterMyApplicationsPortlet", true);
    when(links.getSettingsSchema()).thenReturn(Map.of("type", "object"));
    service.addPlugin(links);
    service.addPlugin(myApplications);
    service.addPlugin(plugin(" ", false));

    assertTrue(service.isConfigurable("content/Links", false));
    assertFalse(service.isConfigurable("content/Links", true));
    assertTrue(service.isConfigurable("app-center/AppCenterMyApplicationsPortlet", true));
    assertFalse(service.isConfigurable("content/Image", false));
    assertFalse(service.isConfigurable(null, false));
    assertFalse(service.isConfigurable("Links", false));
    assertEquals(Map.of("type", "object"), service.getSettingsSchema("content/Links"));
  }

  @Test
  @SneakyThrows
  public void validationRefusesAnApplicationWithoutAPluginBeforeAskingAnyOther() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    List<ApplicationSettingsContext> contexts = List.of(context(20, "content/Links", false), context(21, "content/Image", false));

    IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                                                    () -> service.validate(contexts,
                                                                           List.of(Map.of(), Map.of()),
                                                                           List.of("20", "21")));

    assertEquals("not configurable through tools: 21", refusal.getMessage());
    verify(links).validate(contexts.get(0), Map.of());
    verify(links, never()).apply(any(), any());
  }

  @Test
  @SneakyThrows
  public void validationRefusesAPageKeyedApplicationInASiteContainerAndAnswersTheOwnRuleRefusalWithTheFixedText() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    assertEquals("not configurable in a site container: 30",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.validate(List.of(context(30, "content/Links", true)),
                                                     List.of(Map.of()),
                                                     List.of("30"))).getMessage());
    verify(links, never()).validate(any(), any());

    doThrow(new IllegalAccessException("page note-1 is not editable by john")).when(links).validate(any(), any());
    assertEquals(ApplicationSettingsService.APPLICATION_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class,
                              () -> service.validate(List.of(context(20, "content/Links", false)),
                                                     List.of(Map.of()),
                                                     List.of("20"))).getMessage());
  }

  @Test
  @SneakyThrows
  public void aNullListItemIsRefusedNamingItsOptionBeforeThePluginSeesIt() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    Map<String, Object> link = new HashMap<>();
    link.put("tags", Arrays.asList("a", null));
    List<ApplicationSettingsContext> contexts = List.of(context(20, "content/Links", false));

    assertEquals("value is not accepted for links",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.validate(contexts,
                                                     List.of(Map.of("links", Arrays.asList(Map.of("url", "u"), null))),
                                                     List.of("20"))).getMessage());
    assertEquals("value is not accepted for tags",
                 assertThrows(IllegalArgumentException.class,
                              () -> service.validate(contexts,
                                                     List.of(Map.of("links", List.of(link))),
                                                     List.of("20"))).getMessage());
    verify(links, never()).validate(any(), any());
  }

  @Test
  @SneakyThrows
  public void aWriterTakingTheLockReadsTheWindowAsTheWriterBeforeItLeftIt() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    Application stored = new Application("120");
    Portlet portlet = new Portlet();
    portlet.setValue("name", "copy-links");
    portlet.setValue(ApplicationSettingsService.OWNER_PREFERENCE, "120");
    when(layoutService.getApplicationModel("120")).thenReturn(stored);
    when(layoutService.load(stored.getState())).thenReturn(portlet);
    List<Map<String, String>> seen = new ArrayList<>();
    doAnswer(i -> seen.add(new HashMap<>(((ApplicationSettingsContext) i.getArgument(0)).getPreferences()))).when(links)
                                                                                                         .apply(any(), any());
    ApplicationSettingsContext window = context(120, "content/Links", false);
    window.setPreferences(Map.of("name", "live-links"));

    service.apply(List.of(window), List.of(Map.of("type", "ROW")), List.of("20"));

    assertEquals(Map.of("name", "copy-links", ApplicationSettingsService.OWNER_PREFERENCE, "120"), seen.get(0));
    assertTrue(service.isOwner(window));
  }

  @Test
  @SneakyThrows
  public void aFailureOfTheFirstApplicationIsAnsweredAsIsAndALaterOneNamesTheApplicationsAlreadyApplied() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    List<ApplicationSettingsContext> contexts = List.of(context(120, "content/Links", false),
                                                        context(121, "content/Links", false),
                                                        context(122, "content/Links", false));
    List<Map<String, Object>> settings = List.of(Map.of("type", "ROW"), Map.of("type", "CARD"), Map.of("type", "COLUMN"));
    List<String> ids = List.of("20", "21", "22");

    doThrow(new IllegalArgumentException("value is not accepted for type")).when(links).apply(contexts.get(0), settings.get(0));
    assertEquals("value is not accepted for type",
                 assertThrows(IllegalArgumentException.class, () -> service.apply(contexts, settings, ids)).getMessage());
    verify(links, never()).apply(contexts.get(1), settings.get(1));

    doAnswer(i -> null).when(links).apply(contexts.get(0), settings.get(0));
    doThrow(new IllegalAccessException("not editable")).when(links).apply(contexts.get(2), settings.get(2));
    assertEquals("applied before the failure: 20, 21; not allowed: application",
                 assertThrows(IllegalStateException.class, () -> service.apply(contexts, settings, ids)).getMessage());

    doThrow(new ObjectNotFoundException("does not exist: news_target")).when(links).apply(contexts.get(2), settings.get(2));
    assertEquals("applied before the failure: 20, 21; does not exist: news_target",
                 assertThrows(IllegalStateException.class, () -> service.apply(contexts, settings, ids)).getMessage());

    doThrow(new IllegalStateException("database down")).when(links).apply(contexts.get(1), settings.get(1));
    assertEquals("applied before the failure: 20; database down",
                 assertThrows(IllegalStateException.class, () -> service.apply(contexts, settings, ids)).getMessage());

    doThrow(new NullPointerException()).when(links).apply(contexts.get(0), settings.get(0));
    assertEquals(ApplicationSettingsService.APPLICATION_NOT_SAVED,
                 assertThrows(IllegalStateException.class, () -> service.apply(contexts, settings, ids)).getMessage());
    doThrow(new IllegalAccessException("not editable")).when(links).apply(contexts.get(0), settings.get(0));
    assertEquals(ApplicationSettingsService.APPLICATION_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> service.apply(contexts, settings, ids)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aWindowBeingWrittenOnThisNodeLetsASecondWriterGiveUpAfterTheTimeout() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    CountDownLatch writing = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(i -> {
      writing.countDown();
      release.await(20, TimeUnit.SECONDS);
      return null;
    }).when(links).apply(any(), eq(Map.of("type", "ROW")));
    ApplicationSettingsContext window = context(120, "content/Links", false);
    CompletableFuture<Void> first = CompletableFuture.runAsync(() -> {
      try {
        service.apply(List.of(window), List.of(Map.of("type", "ROW")), List.of("20"));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    });
    try {
      assertTrue(writing.await(10, TimeUnit.SECONDS));
      ApplicationSettingsContext sameWindow = context(120, "content/Links", false);
      assertEquals(ApplicationSettingsService.LAYOUT_BUSY,
                   assertThrows(IllegalStateException.class,
                                () -> service.apply(List.of(sameWindow),
                                                    List.of(Map.of("type", "CARD")),
                                                    List.of("20"))).getMessage());
      verify(links, never()).apply(any(), eq(Map.of("type", "CARD")));
    } finally {
      release.countDown();
      first.get(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @SneakyThrows
  public void theOwnerMarkerNamesTheWindowAndIsWrittenWithTheKeysOverTheStoredPreferences() {
    ApplicationSettingsContext window = context(120, "content/Links", false);
    window.setPreferences(Map.of("name", "live-links", ApplicationSettingsService.OWNER_PREFERENCE, "20"));
    assertFalse(service.isOwner(window));

    service.markOwner(window, Map.of("name", "copy-links"));

    ArgumentCaptor<List<PortletInstancePreference>> written = preferencesCaptor();
    verify(pageLayoutService).updatePageApplicationPreferences(eq(DRAFT_PAGE), eq(120L), written.capture(), eq(USER), eq(true));
    Map<String, String> values = new HashMap<>();
    written.getValue().forEach(p -> values.put(p.getName(), p.getValue()));
    assertEquals(Map.of("name", "copy-links", ApplicationSettingsService.OWNER_PREFERENCE, "120"), values);
    assertTrue(service.isOwner(window));
    assertEquals("copy-links", window.getPreferences().get("name"));
    verify(siteLayoutService, never()).updateSiteApplicationPreferences(any(), anyLong(), any(), any(), anyBoolean());
  }

  @Test
  @SneakyThrows
  public void aSiteContainerWindowIsWrittenThroughTheSiteWriter() {
    ApplicationSettingsContext window = context(130, "app-center/AppCenterMyApplicationsPortlet", true);

    service.updatePreferences(window, Map.of("maxAppsToList", "6"));

    verify(siteLayoutService).updateSiteApplicationPreferences(eq(SITE_KEY), eq(130L), any(), eq(USER), eq(true));
    verify(pageLayoutService, never()).updatePageApplicationPreferences(any(), anyLong(), any(), any(), anyBoolean());
    assertEquals("6", window.getPreferences().get("maxAppsToList"));
  }

  @Test
  public void nullEntriesAreDroppedAtEveryDepthOfTheMaps() {
    Map<String, Object> header = new HashMap<>();
    header.put("en", "Links");
    header.put("fr", null);
    Map<String, Object> link = new HashMap<>();
    link.put("url", "https://meeds.io");
    link.put("description", null);
    Map<String, Object> settings = new HashMap<>();
    settings.put("header", header);
    settings.put("see_more", null);
    settings.put("links", Arrays.asList(link, null));

    Map<String, Object> result = service.dropNullEntries(settings);

    assertEquals(Map.of("en", "Links"), result.get("header"));
    assertFalse(result.containsKey("see_more"));
    List<?> links = (List<?>) result.get("links");
    assertEquals(Map.of("url", "https://meeds.io"), links.get(0));
    assertNull(links.get(1));
    assertEquals(Map.of(), service.dropNullEntries(null));
  }

  @Test
  public void aWindowIsFoundAtThePlaceItsLiveWindowHeld() {
    Application live = new Application("20");
    Container liveRoot = container(new Container(), container(new Application("19"), live));
    Application copy = new Application("120");
    Container draftRoot = container(new Container(), container(new Application("119"), copy));

    List<Integer> path = service.pathOf(liveRoot, "20");

    assertEquals(List.of(1, 1), path);
    assertSame(copy, service.atPath(draftRoot, path));
    assertNull(service.pathOf(liveRoot, "99"));
    assertNull(service.atPath(container(new Container()), path));
    assertNull(service.atPath(draftRoot, List.of(0)));
  }

  @Test
  @SneakyThrows
  public void aContextCarriesTheStoredPreferencesAndTheSpaceOfTheLiveGroupSite() {
    Application window = new Application("120");
    Portlet portlet = new Portlet();
    portlet.setValue("name", "links-1");
    when(layoutService.load(window.getState())).thenReturn(portlet);
    when(layoutService.getId(window.getState())).thenReturn("content/Links");
    Space space = new Space();
    space.setId("7");
    SiteKey spaceSite = SiteKey.group("/spaces/marketing");
    when(spaceService.getSpaceByGroupId("/spaces/marketing")).thenReturn(space);

    ApplicationSettingsContext context = service.newContext(window,
                                                            DRAFT_PAGE,
                                                            spaceSite,
                                                            spaceSite,
                                                            "group::/spaces/marketing::home",
                                                            true,
                                                            USER);

    assertEquals(120L, context.getApplicationId());
    assertEquals("content/Links", context.getContentId());
    assertEquals(Map.of("name", "links-1"), context.getPreferences());
    assertFalse(context.isSiteContainer());
    assertTrue(context.isDraft());
    assertEquals(7L, context.getSpaceId());
    assertEquals("group::/spaces/marketing::home", context.getPageReference());
    assertEquals(0L, service.newContext(window, null, SITE_KEY, SITE_KEY, null, false, USER).getSpaceId());
  }

  @Test
  @SneakyThrows
  public void settingsAreReadThroughThePluginAndAFailedReadAnswersNone() {
    ApplicationSettingsPlugin links = plugin("Links", false);
    service.addPlugin(links);
    ApplicationSettingsContext window = context(20, "content/Links", false);
    when(links.read(window)).thenReturn(Map.of("type", "CARD"));
    assertEquals(Map.of("type", "CARD"), service.readSettings(window));

    when(links.read(window)).thenThrow(new IllegalAccessException("not readable"));
    assertNull(service.readSettings(window));
    assertNull(service.readSettings(context(30, "content/Links", true)));
    assertNull(service.readSettings(context(21, "content/Image", false)));
  }

  private ApplicationSettingsPlugin plugin(String portletName, boolean siteContainers) {
    ApplicationSettingsPlugin plugin = mock(ApplicationSettingsPlugin.class, withSettings().strictness(Strictness.LENIENT));
    when(plugin.getPortletName()).thenReturn(portletName);
    when(plugin.isConfigurableInSiteContainer()).thenReturn(siteContainers);
    return plugin;
  }

  private ApplicationSettingsContext context(long applicationId, String contentId, boolean siteContainer) {
    return new ApplicationSettingsContext(applicationId,
                                          contentId,
                                          new HashMap<>(),
                                          siteContainer,
                                          true,
                                          siteContainer ? null : DRAFT_PAGE,
                                          SITE_KEY,
                                          siteContainer ? null : "portal::test::home",
                                          0,
                                          USER);
  }

  private Container container(ModelObject... children) {
    Container container = new Container();
    container.setChildren(new ArrayList<>(List.of(children)));
    return container;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private ArgumentCaptor<List<PortletInstancePreference>> preferencesCaptor() {
    return ArgumentCaptor.forClass((Class) List.class);
  }

}
