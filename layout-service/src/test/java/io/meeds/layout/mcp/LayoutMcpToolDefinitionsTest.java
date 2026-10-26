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
package io.meeds.layout.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.ai.util.JsonHelper;

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.model.ElementStyling;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SidebarDisplayOptions;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.TopbarApplicationOption;
import io.meeds.layout.mcp.model.TopbarDisplayOptions;
import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.util.McpToolUtils;

/**
 * The MCP server exposes a public method only when ai-tool-definitions.json
 * names it, binds each argument by the compiled parameter name, and shows the
 * agent the JSON schema alone: the three must agree, and the module must be
 * compiled with -parameters for the names to exist at all.
 */
public class LayoutMcpToolDefinitionsTest {

  private static final JsonHelper JSON_HELPER = new JsonHelper();

  private static final String              NAVIGATION_DISPLAY_SAMPLE = """
      {"topbar": {"display_company_name": true, "display_site_name": false, "display_mobile_company_logo": true,
                  "applications": [{"id": "search", "enabled": false, "mobile": true}]},
       "sidebar": {"allow_user_custom_home": true, "allowed_modes": ["icon", "sticky"], "default_mode": "icon"}}
      """;

  private static final Set<String>         WRITE_TOOLS               = Set.of("update_theme",
                                                                               "update_navigation_display",
                                                                               "create_site",
                                                                               "update_site",
                                                                               "delete_site",
                                                                               "set_site_layout",
                                                                               "publish_layout",
                                                                               "discard_layout_draft",
                                                                               "create_page",
                                                                               "update_page",
                                                                               "delete_page",
                                                                               "set_page_layout",
                                                                               "set_layout_styles");

  private static final Set<String>         APPROVAL_TOOLS            = Set.of("update_theme",
                                                                               "update_navigation_display",
                                                                               "delete_site",
                                                                               "publish_layout",
                                                                               "delete_page");

  private static final String              CREATE_PAGE_SAMPLE        = """
      {"site_id": 3, "parent_node_id": 11, "label": "Our team", "labels": {"fr": "Notre équipe"}, "url_name": "our-team",
       "type": "link", "page_template_id": 4, "page_ref": "portal::mysite::home", "url": "https://meeds.io", "target": "new_tab",
       "icon": "fa-users", "visible": false, "scheduled": {"start": "2030-01-01T09:00:00Z", "end": "2030-01-02T09:00:00Z"},
       "access_permissions": ["Everyone"], "edit_permission": "*:/platform/administrators"}
      """;

  private static final String              UPDATE_PAGE_SAMPLE        = """
      {"node_id": 12, "label": "Our team", "labels": {"fr": "Notre équipe"}, "url_name": "our-team", "type": "existing_page",
       "page_template_id": 4, "page_ref": "portal::mysite::home", "url": "https://meeds.io", "target": "same_tab", "icon": "fa-users",
       "visible": true, "scheduled": {"start": "2030-01-01T09:00:00Z", "end": "2030-01-02T09:00:00Z"},
       "access_permissions": ["*:/platform/users"], "edit_permission": "*:/platform/administrators"}
      """;

  private static final String              SITE_LAYOUT_SAMPLE        = """
      {"site_id": 3,
       "left_sidebar": {"container_id": "12", "options": {"width": 250, "sticky": true, "display": "hide_mobile", "full_height": true},
                        "styling": {"margins": {"enabled": true, "all": 0}, "background": {"enabled": true, "color": "#111111", "scroll_color": "#222222"}},
                        "applications": [{"application_id": "5"}, {"instance_id": 10, "title": "News", "display": "hide_desktop",
                                          "styling": {"border": {"enabled": true, "color": "#123456", "size": 2, "shadow": true},
                                                      "text": {"enabled": true, "title": {"color": "#333333", "size": 18, "bold": true}}}}]},
       "right_sidebar": {"options": {"width": 300}},
       "topbars": [{"options": {"height": 60, "sticky": true}, "applications": [{"content_id": "social/Menu"}]}],
       "bottom_bars": []}
      """;

  private static final String              PAGE_LAYOUT_SAMPLE        = """
      {"page_ref": "portal::mysite::home",
       "sections": [{"section_id": "20", "type": "dynamic", "columns": 2,
                     "columns_content": [[{"application_id": "5", "title": "News"}, {"instance_id": 10, "display": "hide_mobile"}], []],
                     "styling": {"margins": {"enabled": true, "all": 10}}, "display": "both"},
                    {"type": "grid", "rows": 2, "columns": 3,
                     "cells": [{"row": 1, "column": 1, "column_span": 2, "application": {"content_id": "layout/Links",
                                "styling": {"border": {"enabled": true, "color": "#123456", "size": 2, "shadow": true}}}},
                               {"cell_id": "31", "row": 2, "column": 3}]}]}
      """;

  private static final String              LAYOUT_STYLES_SAMPLE      = """
      {"page_ref": "portal::mysite::home", "site_id": 3,
       "elements": [{"element_id": "20", "styling": {"margins": {"enabled": true, "top": -10, "bottom": 30},
                                                     "background": {"enabled": true, "mode": "linear", "from": "#FF000080",
                                                                    "to": "#0000FFFF", "ratio": 40, "direction": "to_right"}}},
                    {"element_id": "5", "styling": {"height": {"enabled": true, "fixed": "custom", "custom_px": 420},
                                                    "corners": {"enabled": false}}}]}
      """;

  private static final String              CREATE_SITE_SAMPLE        = """
      {"name": "marketing", "label": "Marketing", "description": "About us", "template": "basic",
       "banner_upload_id": "u1", "access_permissions": ["Everyone"], "edit_permission": "*:/platform/administrators",
       "displayed": true, "icon": "fa-globe"}
      """;

  private static final String              UPDATE_SITE_SAMPLE        = """
      {"site_id": 3, "label": "Marketing", "description": "About us", "access_permissions": ["*:/platform/users"],
       "edit_permission": "*:/platform/administrators", "displayed": false, "icon": "fa-globe",
       "banner_upload_id": "u1", "remove_banner": false}
      """;

  private static final List<Class<?>>      TOOL_CLASSES              = List.of(LayoutMcpTool.class, BrandingMcpTool.class);

  private static final Map<String, String> SAMPLE_INPUTS             = Map.ofEntries(Map.entry("list_sites", "{\"keyword\": \"marketing\"}"),
                                                                                     Map.entry("get_site", "{\"site_id\": 3}"),
                                                                                     Map.entry("list_site_pages",
                                                                                               "{\"site_id\": 3, \"include_hidden\": true}"),
                                                                                     Map.entry("get_page_layout",
                                                                                               "{\"page_ref\": \"portal::mysite::home\", \"version\": \"draft\"}"),
                                                                                     Map.entry("list_layout_applications",
                                                                                               "{\"category_id\": 2, \"keyword\": \"news\"}"),
                                                                                     Map.entry("update_theme",
                                                                                               "{\"part\": \"colors\", \"options\": {\"primaryColor\": \"#123456\"}, \"reset\": [\"secondaryColor\"]}"),
                                                                                     Map.entry("update_navigation_display",
                                                                                               NAVIGATION_DISPLAY_SAMPLE),
                                                                                     Map.entry("create_site", CREATE_SITE_SAMPLE),
                                                                                     Map.entry("update_site", UPDATE_SITE_SAMPLE),
                                                                                     Map.entry("delete_site", "{\"site_id\": 3}"),
                                                                                     Map.entry("set_site_layout", SITE_LAYOUT_SAMPLE),
                                                                                     Map.entry("publish_layout",
                                                                                               "{\"site_id\": 3, \"page_ref\": \"portal::mysite::home\"}"),
                                                                                     Map.entry("discard_layout_draft",
                                                                                               "{\"site_id\": 3, \"page_ref\": \"portal::mysite::home\"}"),
                                                                                     Map.entry("create_page", CREATE_PAGE_SAMPLE),
                                                                                     Map.entry("update_page", UPDATE_PAGE_SAMPLE),
                                                                                     Map.entry("delete_page", "{\"node_id\": 12}"),
                                                                                     Map.entry("set_page_layout", PAGE_LAYOUT_SAMPLE),
                                                                                     Map.entry("set_layout_styles", LAYOUT_STYLES_SAMPLE));

  @Test
  public void everyPublicMethodHasADefinitionAndEveryDefinitionAMethod() {
    Map<String, Method> methods = toolMethods();
    Map<String, SimpleToolDefinition> definitions = definitions();
    assertEquals(methods.keySet(), definitions.keySet());
  }

  @Test
  public void everySchemaPropertyNamesACompiledParameter() {
    Map<String, Method> methods = toolMethods();
    definitions().forEach((name, definition) -> {
      Method method = methods.get(name);
      Set<String> parameterNames = Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.toSet());
      assertTrue(parameterNames.stream().noneMatch(p -> p.matches("arg\\d+")),
                 "module compiled without -parameters, " + name + " would receive no argument");
      Map<String, Object> schema = JSON_HELPER.fromJsonToMap(definition.getInputSchema());
      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
      Set<String> schemaNames = properties.keySet().stream().map(McpToolUtils::toCamelCase).collect(Collectors.toSet());
      assertEquals(parameterNames, schemaNames, name);
      @SuppressWarnings("unchecked")
      List<String> required = (List<String>) schema.get("required");
      assertTrue(properties.keySet().containsAll(required), name + " requires a property it does not declare");
    });
  }

  @Test
  public void everyToolIsAnnotatedByItsNature() {
    definitions().values().forEach(definition -> {
      boolean write = WRITE_TOOLS.contains(definition.getName());
      boolean approval = APPROVAL_TOOLS.contains(definition.getName());
      assertNotNull(definition.getAnnotations(), definition.getName());
      assertEquals(!write, definition.getAnnotations().readOnlyHint(), definition.getName());
      assertEquals(approval, definition.getAnnotations().destructiveHint(), definition.getName());
      assertEquals(approval, definition.isRequireApproval(), definition.getName());
    });
  }

  @Test
  public void nestedArgumentsBindEveryFieldThroughTheServerConverter() {
    Map<String, Object> arguments = JSON_HELPER.fromJsonToMap(NAVIGATION_DISPLAY_SAMPLE);
    TopbarDisplayOptions topbar = (TopbarDisplayOptions) JSON_HELPER.convertToTypedObject(arguments.get("topbar"),
                                                                                          TopbarDisplayOptions.class);
    SidebarDisplayOptions sidebar = (SidebarDisplayOptions) JSON_HELPER.convertToTypedObject(arguments.get("sidebar"),
                                                                                             SidebarDisplayOptions.class);

    assertEquals(new TopbarDisplayOptions(true, false, true, List.of(new TopbarApplicationOption("search", null, false, true))), topbar);
    assertEquals(new SidebarDisplayOptions(true, List.of("icon", "sticky"), "icon"), sidebar);
    assertEquals(new TopbarDisplayOptions(null, null, null, null),
                 JSON_HELPER.convertToTypedObject(JSON_HELPER.fromJsonToMap("{}"), TopbarDisplayOptions.class));
  }

  /**
   * Spring AI binds a list argument through the method's generic parameter
   * type, so the containers and their nested blocks arrive as records
   */
  @Test
  public void siteLayoutListsBindTheirRecordsThroughTheParameterizedType() throws Exception {
    Method method = toolMethods().get("set_site_layout");
    Map<String, Object> arguments = JSON_HELPER.fromJsonToMap(SITE_LAYOUT_SAMPLE);
    Parameter topbars = Arrays.stream(method.getParameters()).filter(p -> p.getName().equals("topbars")).findFirst().orElseThrow();
    @SuppressWarnings("unchecked")
    List<SiteContainerDescription> typed = (List<SiteContainerDescription>) JSON_HELPER.fromJson(JSON_HELPER.toJson(arguments.get("topbars")),
                                                                                                topbars.getParameterizedType());
    assertEquals(60, typed.get(0).options().height());
    assertTrue(typed.get(0).options().sticky());
    assertEquals("social/Menu", typed.get(0).applications().get(0).contentId());

    SiteContainerDescription left = (SiteContainerDescription) JSON_HELPER.convertToTypedObject(arguments.get("left_sidebar"),
                                                                                                SiteContainerDescription.class);
    assertEquals("12", left.containerId());
    assertEquals(DisplayMode.HIDE_MOBILE, left.options().display());
    assertEquals(0, left.styling().margins().all());
    assertEquals("#222222", left.styling().background().scrollColor());
    assertEquals("5", left.applications().get(0).applicationId());
    assertEquals(10L, left.applications().get(1).instanceId());
    assertEquals(DisplayMode.HIDE_DESKTOP, left.applications().get(1).display());
    assertEquals(2, left.applications().get(1).styling().border().size());
    assertEquals(true, left.applications().get(1).styling().text().title().bold());
  }

  /**
   * The sections list binds through the method's generic parameter type, so
   * each section, its columns' applications and its cells arrive as records
   */
  @Test
  public void pageLayoutSectionsBindTheirRecordsThroughTheParameterizedType() throws Exception {
    Method method = toolMethods().get("set_page_layout");
    Map<String, Object> arguments = JSON_HELPER.fromJsonToMap(PAGE_LAYOUT_SAMPLE);
    Parameter sections = Arrays.stream(method.getParameters()).filter(p -> p.getName().equals("sections")).findFirst().orElseThrow();
    @SuppressWarnings("unchecked")
    List<SectionDescription> typed = (List<SectionDescription>) JSON_HELPER.fromJson(JSON_HELPER.toJson(arguments.get("sections")),
                                                                                    sections.getParameterizedType());
    SectionDescription dynamic = typed.get(0);
    assertEquals("20", dynamic.sectionId());
    assertEquals(SectionType.DYNAMIC, dynamic.type());
    assertEquals(2, dynamic.columns());
    assertEquals(2, dynamic.columnsContent().size());
    assertEquals("5", dynamic.columnsContent().get(0).get(0).applicationId());
    assertEquals("News", dynamic.columnsContent().get(0).get(0).title());
    assertEquals(10L, dynamic.columnsContent().get(0).get(1).instanceId());
    assertEquals(DisplayMode.HIDE_MOBILE, dynamic.columnsContent().get(0).get(1).display());
    assertEquals(10, dynamic.styling().margins().all());
    assertEquals(DisplayMode.BOTH, dynamic.display());
    SectionDescription grid = typed.get(1);
    assertEquals(SectionType.GRID, grid.type());
    assertEquals(2, grid.rows());
    assertEquals(3, grid.columns());
    assertEquals(2, grid.cells().get(0).columnSpan());
    assertEquals("layout/Links", grid.cells().get(0).application().contentId());
    assertEquals(2, grid.cells().get(0).application().styling().border().size());
    assertEquals("31", grid.cells().get(1).cellId());
    assertEquals(2, grid.cells().get(1).row());
    assertEquals(3, grid.cells().get(1).column());
    assertNull(grid.cells().get(1).application());
  }

  /**
   * The elements list binds through the method's generic parameter type, so
   * each element and its styling blocks arrive as records
   */
  @Test
  public void layoutStylesElementsBindTheirRecordsThroughTheParameterizedType() throws Exception {
    Method method = toolMethods().get("set_layout_styles");
    Map<String, Object> arguments = JSON_HELPER.fromJsonToMap(LAYOUT_STYLES_SAMPLE);
    Parameter elements = Arrays.stream(method.getParameters()).filter(p -> p.getName().equals("elements")).findFirst().orElseThrow();
    @SuppressWarnings("unchecked")
    List<ElementStyling> typed = (List<ElementStyling>) JSON_HELPER.fromJson(JSON_HELPER.toJson(arguments.get("elements")),
                                                                            elements.getParameterizedType());
    assertEquals("20", typed.get(0).elementId());
    assertEquals(-10, typed.get(0).styling().margins().top());
    assertEquals(30, typed.get(0).styling().margins().bottom());
    assertEquals(BackgroundMode.LINEAR, typed.get(0).styling().background().mode());
    assertEquals("#FF000080", typed.get(0).styling().background().from());
    assertEquals(40, typed.get(0).styling().background().ratio());
    assertEquals("to_right", typed.get(0).styling().background().direction());
    assertEquals("5", typed.get(1).elementId());
    assertEquals(HeightPreset.CUSTOM, typed.get(1).styling().height().fixed());
    assertEquals(420, typed.get(1).styling().height().customPx());
    assertEquals(false, typed.get(1).styling().corners().enabled());
  }

  @Test
  public void everySampleInputBindsThroughTheServerConverter() {
    Map<String, Method> methods = toolMethods();
    assertEquals(methods.keySet(), SAMPLE_INPUTS.keySet());
    SAMPLE_INPUTS.forEach((name, json) -> {
      Map<String, Object> arguments = JSON_HELPER.fromJsonToMap(json)
                                                 .entrySet()
                                                 .stream()
                                                 .collect(Collectors.toMap(e -> McpToolUtils.toCamelCase(e.getKey()),
                                                                           Map.Entry::getValue));
      for (Parameter parameter : methods.get(name).getParameters()) {
        Object raw = arguments.get(parameter.getName());
        assertNotNull(raw, name + " sample lacks " + parameter.getName());
        Object typed = JSON_HELPER.convertToTypedObject(raw, parameter.getType());
        assertNotNull(typed, name + "." + parameter.getName());
        assertTrue(parameter.getType().isInstance(typed), name + "." + parameter.getName() + " is " + typed.getClass());
      }
    });
  }

  @Test
  public void outputIsSerializedInSnakeCase() {
    SectionDescription section = new SectionDescription("12", SectionType.DYNAMIC, 3, null, List.of(), null, null, null);
    String json = JSON_HELPER.toJson(new PageDescription("portal::a::b", LayoutVersion.DRAFT, List.of(section), null));
    assertTrue(json.contains("\"page_ref\""), json);
    assertTrue(json.contains("\"section_id\""), json);
    assertTrue(json.contains("\"columns_content\""), json);
    assertTrue(json.contains("\"dynamic\""), json);
    assertTrue(json.contains("\"draft\""), json);
    assertTrue(!json.contains("editor_compatible"), json);
  }

  private Map<String, Method> toolMethods() {
    return TOOL_CLASSES.stream()
                 .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                 .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                 .collect(Collectors.toMap(m -> McpToolUtils.toSnakeCase(m.getName()), m -> m));
  }

  private Map<String, SimpleToolDefinition> definitions() {
    URL url = getClass().getClassLoader().getResource("ai-tool-definitions.json");
    assertNotNull(url);
    return McpToolUtils.parseToolDefinitions(url)
                       .stream()
                       .collect(Collectors.toMap(SimpleToolDefinition::getName, d -> d));
  }
}
