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
package io.meeds.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The packages below the MCP tools never depend on them: the tools map the
 * service shapes to their answers, never the reverse
 */
public class PackageLayeringTest {

  private static final Path SOURCES = Path.of("src/main/java/io/meeds/layout");

  @Test
  public void noPackageButTheToolsImportsTheToolsPackage() throws IOException {
    assertTrue(Files.isDirectory(SOURCES.resolve("mcp")));
    try (Stream<Path> files = Files.walk(SOURCES)) {
      List<String> offenders = files.filter(file -> file.toString().endsWith(".java"))
                                    .filter(file -> !file.startsWith(SOURCES.resolve("mcp")))
                                    .filter(PackageLayeringTest::importsTheTools)
                                    .map(file -> SOURCES.relativize(file).toString())
                                    .toList();
      assertEquals(List.of(), offenders);
    }
  }

  private static boolean importsTheTools(Path file) {
    try {
      return Files.readAllLines(file).stream().anyMatch(line -> line.startsWith("import io.meeds.layout.mcp."));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
