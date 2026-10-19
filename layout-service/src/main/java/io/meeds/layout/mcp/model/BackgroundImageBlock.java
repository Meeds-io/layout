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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * imageUploadId is an input only (the id upload_layout_image returned); a read
 * carries the stored url instead.
 */
@JsonInclude(Include.NON_NULL)
public record BackgroundImageBlock(@JsonProperty("image_upload_id") String imageUploadId,
                                   @JsonProperty("url") String url,
                                   @JsonProperty("size") String size,
                                   @JsonProperty("position") String position,
                                   @JsonProperty("attachment") String attachment,
                                   @JsonProperty("repeat") String repeat,
                                   @JsonProperty("margins") SidesBlock margins,
                                   @JsonProperty("corners") CornersBlock corners) {
}
