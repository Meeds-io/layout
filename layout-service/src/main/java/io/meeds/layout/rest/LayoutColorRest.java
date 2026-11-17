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
package io.meeds.layout.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.meeds.layout.model.DarkColorBlock;
import io.meeds.layout.service.LayoutDarkColorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("colors")
@Tag(name = "colors", description = "Calculating the dark version of the colors set in the page editor")
public class LayoutColorRest {

  @Autowired
  private LayoutDarkColorService layoutDarkColorService;

  @PostMapping("dark")
  @Secured("users")
  @Operation(summary = "Calculate dark colors",
             method = "POST",
             description = "This calculates the dark value of the colors of at most 100 blocks, by the rules that apply to the colors of the page layout. Nothing is stored.")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled, one map of dark values by field per block"),
    @ApiResponse(responseCode = "400", description = "More than 100 blocks or an invalid color"),
  })
  public List<Map<String, String>> getDarkColors(
                                                 @RequestBody
                                                 List<DarkColorBlock> blocks) {
    try {
      return layoutDarkColorService.computeDarkColors(blocks);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

}
