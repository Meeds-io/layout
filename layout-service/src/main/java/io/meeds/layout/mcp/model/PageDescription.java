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
package io.meeds.layout.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.layout.mcp.constant.LayoutVersion;

/**
 * The page in the shape set_page_layout takes. editorCompatible is false when
 * the stored page is not made of the editor's sections, in which case the
 * sections are empty: the layout editor refuses such a page too.
 */
@JsonInclude(Include.NON_NULL)
public record PageDescription(@JsonProperty("page_ref") String pageRef,
                              @JsonProperty("version") LayoutVersion version,
                              @JsonProperty("sections") List<SectionDescription> sections,
                              @JsonProperty("editor_compatible") Boolean editorCompatible) {
}
