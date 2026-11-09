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
package io.meeds.layout.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.layout.constant.Alignment;

/**
 * A column of a topbar or a bottom bar, as the site layout editor shows it:
 * its width in pixels when it has one, or fit_content when it takes the
 * width of its application, the last column taking the remaining width
 * otherwise, where its application sits in it, horizontally and vertically,
 * and the application it holds. A stored column holding several
 * applications is read whole; a write places one application per column,
 * and keeps a column as it is when each application given is sent back by
 * its id from that same column.
 */
@JsonInclude(Include.NON_NULL)
public record SiteColumnDescription(@JsonProperty("column_id") String columnId,
                                    @JsonProperty("width") Integer width,
                                    @JsonProperty("fit_content") Boolean fitContent,
                                    @JsonProperty("h_align") Alignment hAlign,
                                    @JsonProperty("v_align") Alignment vAlign,
                                    @JsonProperty("applications") List<ApplicationDescription> applications) {

  /**
   * A column with a width in pixels, or sharing the remaining width, whose
   * alignment is kept as stored, or left at the editor's default on a new one
   */
  public SiteColumnDescription(String columnId, Integer width, List<ApplicationDescription> applications) {
    this(columnId, width, null, null, null, applications);
  }

  /**
   * A column whose alignment is kept as stored, or left at the editor's
   * default on a new one
   */
  public SiteColumnDescription(String columnId, Integer width, Boolean fitContent, List<ApplicationDescription> applications) {
    this(columnId, width, fitContent, null, null, applications);
  }

}
