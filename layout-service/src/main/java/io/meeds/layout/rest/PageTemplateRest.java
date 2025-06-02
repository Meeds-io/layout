/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.service.injection.PageTemplateImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/page/templates")
@Tag(name = "/layout/rest/page/templates", description = "Managing page templates")
public class PageTemplateRest {

  @Autowired
  private PageTemplateService pageTemplateService;
  @Autowired
  private PageTemplateImportService pageTemplateImportService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieve page templates", method = "GET", description = "This retrieves page templates")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"), })
  public List<PageTemplate> getPageTemplates(HttpServletRequest request,
                                             @Parameter(description = "Whether to retrieve page template content or not", required = false)
                                             @RequestParam(name = "expandContent", required = false, defaultValue = "false")
                                             boolean expandContent) {
    return pageTemplateService.getPageTemplates(request.getLocale(), true, expandContent);
  }

  @GetMapping("{id}")
  @Secured("users")
  @Operation(summary = "Retrieve a page template designated by its id", method = "GET", description = "This will retrieve a page template designated by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public PageTemplate getPageTemplate(
                                      HttpServletRequest request,
                                      @Parameter(description = "Page template identifier")
                                      @PathVariable("id")
                                      long id,
                                      @Parameter(description = "Whether to retrieve page template content or not", required = false)
                                      @RequestParam(name = "expandContent", required = false, defaultValue = "false")
                                      boolean expandContent) {
    return pageTemplateService.getPageTemplate(id, request.getLocale(), true, expandContent);
  }

  @PostMapping
  @Secured("users")
  @Operation(summary = "Create a page template", method = "POST", description = "This creates a new page template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
  })
  public PageTemplate createPageTemplate(
                                         HttpServletRequest request,
                                         @RequestBody
                                         PageTemplate pageTemplate) {
    try {
      return pageTemplateService.createPageTemplate(pageTemplate, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("users")
  @Operation(summary = "Update a page template", method = "PUT", description = "This updates an existing page template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updatePageTemplate(
                                 HttpServletRequest request,
                                 @Parameter(description = "Page template identifier")
                                 @PathVariable("id")
                                 long id,
                                 @RequestBody
                                 PageTemplate pageTemplate) {
    try {
      pageTemplate.setId(id);
      pageTemplateService.updatePageTemplate(pageTemplate, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}/restore")
  @Secured("users")
  @Operation(summary = "Restores a system page template", method = "PUT", description = "This restores a system page template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Bad request"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void restorePageTemplate(
                                  HttpServletRequest request,
                                  @Parameter(description = "Page template identifier")
                                  @PathVariable("id")
                                  long id) {
    try {
      pageTemplateImportService.restorePageTemplate(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  @Secured("users")
  @Operation(summary = "Deletes a page template", method = "DELETE", description = "This deletes an existing page template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deletePageTemplate(
                                 HttpServletRequest request,
                                 @Parameter(description = "Page template identifier")
                                 @PathVariable("id")
                                 long id) {
    try {
      pageTemplateService.deletePageTemplate(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
