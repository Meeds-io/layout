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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.plugin.ApplicationCodePlugin;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class ApplicationCodeServiceTest {

  private static final String    WIDGET  = "ide/WidgetPortlet";

  private ApplicationCodeService service = new ApplicationCodeService();

  @Test
  @SneakyThrows
  public void aPortletWithoutAPluginHasNoCodeAndNoRead() {
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    assertFalse(service.hasPlugin(WIDGET));
    assertFalse(service.hasPlugin(null));
    assertFalse(service.hasPlugin("WidgetPortlet"));
    assertNull(service.read(WIDGET, context));
  }

  @Test
  @SneakyThrows
  public void thePluginOfThePortletReadsTheCodeForTheContext() {
    ApplicationCodePlugin plugin = plugin("WidgetPortlet");
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    ApplicationCode code = new ApplicationCode("<b/>", "b{}", "c()");
    when(plugin.read(context)).thenReturn(code);
    service.addPlugin(plugin);

    assertTrue(service.hasPlugin(WIDGET));
    assertFalse(service.hasPlugin("content/Links"));
    assertEquals(code, service.read(WIDGET, context));
    assertNull(service.read("content/Links", context));
    verify(plugin).read(context);

    service.removePlugin("WidgetPortlet");
    assertFalse(service.hasPlugin(WIDGET));
  }

  @Test
  @SneakyThrows
  public void aPluginThatRefusesOrFailsAnswersNoCode() {
    ApplicationCodePlugin plugin = plugin("WidgetPortlet");
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    service.addPlugin(plugin);

    doThrow(new IllegalAccessException("not allowed")).when(plugin).read(context);
    assertNull(service.read(WIDGET, context));
    doThrow(new ObjectNotFoundException("gone")).when(plugin).read(context);
    assertNull(service.read(WIDGET, context));
    doThrow(new IllegalStateException("engine")).when(plugin).read(context);
    assertNull(service.read(WIDGET, context));
  }

  @Test
  @SneakyThrows
  public void aSecondPluginForThePortletReplacesTheFirst() {
    ApplicationCodePlugin first = plugin("WidgetPortlet");
    ApplicationCodePlugin second = plugin("WidgetPortlet");
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    when(second.read(context)).thenReturn(new ApplicationCode(null, null, "x()"));
    service.addPlugin(first);
    service.addPlugin(second);

    assertEquals("x()", service.read(WIDGET, context).js());
    verify(first, never()).read(context);
  }

  @Test
  public void anApplicationIsPlacedWithoutCatalogOnlyByItsPluginsExactContentId() {
    ApplicationCodePlugin plugin = plugin("WidgetPortlet");
    when(plugin.newApplication()).thenAnswer(invocation -> application(WIDGET));
    service.addPlugin(plugin);

    PortletInstance first = service.newApplication(WIDGET);
    assertEquals(WIDGET, first.getContentId());
    assertFalse(first == service.newApplication(WIDGET));
    assertNull(service.newApplication("other/WidgetPortlet"));
    assertNull(service.newApplication("content/Links"));
    assertNull(service.newApplication(null));
    assertEquals(1, service.newApplications().size());
  }

  @Test
  public void aPluginPlacingFromTheCatalogOnlyOffersNoApplication() {
    ApplicationCodePlugin catalogOnly = plugin("WidgetPortlet");
    ApplicationCodePlugin blankContentId = plugin("OtherPortlet");
    when(blankContentId.newApplication()).thenReturn(application(" "));
    service.addPlugin(catalogOnly);
    service.addPlugin(blankContentId);

    assertNull(service.newApplication(WIDGET));
    assertTrue(service.newApplications().isEmpty());
  }

  @Test
  public void aPluginFailingToOfferAnApplicationIsSkipped() {
    ApplicationCodePlugin failing = plugin("WidgetPortlet");
    ApplicationCodePlugin offering = plugin("OtherPortlet");
    when(failing.newApplication()).thenThrow(new IllegalStateException("engine"));
    when(offering.newApplication()).thenAnswer(invocation -> application("other/OtherPortlet"));
    service.addPlugin(failing);
    service.addPlugin(offering);

    assertNull(service.newApplication(WIDGET));
    assertEquals(1, service.newApplications().size());
    assertEquals("other/OtherPortlet", service.newApplications().get(0).getContentId());
  }

  private PortletInstance application(String contentId) {
    PortletInstance application = new PortletInstance();
    application.setContentId(contentId);
    application.setName("Widget");
    return application;
  }

  private ApplicationCodePlugin plugin(String portletName) {
    ApplicationCodePlugin plugin = mock(ApplicationCodePlugin.class);
    when(plugin.getPortletName()).thenReturn(portletName);
    return plugin;
  }

}
