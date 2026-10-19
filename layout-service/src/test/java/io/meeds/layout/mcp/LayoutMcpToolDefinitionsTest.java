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

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SectionDescription;
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

  private static final Map<String, String> SAMPLE_INPUTS = Map.of("list_sites",
                                                                  "{\"keyword\": \"marketing\"}",
                                                                  "get_site",
                                                                  "{\"site_id\": 3}",
                                                                  "list_site_pages",
                                                                  "{\"site_id\": 3, \"include_hidden\": true}",
                                                                  "get_page_layout",
                                                                  "{\"page_ref\": \"portal::mysite::home\", \"version\": \"draft\"}",
                                                                  "list_layout_applications",
                                                                  "{\"category_id\": 2, \"keyword\": \"news\"}");

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
  public void everyReadToolIsAnnotatedReadOnly() {
    definitions().values().forEach(definition -> {
      assertNotNull(definition.getAnnotations(), definition.getName());
      assertEquals(Boolean.TRUE, definition.getAnnotations().readOnlyHint(), definition.getName());
      assertEquals(Boolean.FALSE, definition.getAnnotations().destructiveHint(), definition.getName());
    });
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
    return Arrays.stream(LayoutMcpTool.class.getDeclaredMethods())
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
