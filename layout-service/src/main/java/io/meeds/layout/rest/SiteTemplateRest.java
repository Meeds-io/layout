/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.SiteLayoutUnresolvedException;
import io.meeds.layout.service.SiteTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/site/templates")
@Tag(name = "/layout/rest/site/templates", description = "Managing site templates")
public class SiteTemplateRest {

  @Autowired
  private SiteTemplateService siteTemplateService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieve site templates", method = "GET", description = "This retrieves site templates")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public List<SiteTemplate> getSiteTemplates(HttpServletRequest request) {
    return siteTemplateService.getSiteTemplates(request.getLocale());
  }

  @GetMapping("{id}")
  @Secured("users")
  @Operation(summary = "Retrieve a site template designated by its id", method = "GET", description = "This will retrieve a site template designated by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public SiteTemplate getSiteTemplate(
                                      HttpServletRequest request,
                                      @Parameter(description = "site template identifier")
                                      @PathVariable("id")
                                      long id) {
    try {
      return siteTemplateService.getSiteTemplate(id, request.getLocale());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping
  @Secured("users")
  @Operation(summary = "Create a site template", method = "POST", description = "This creates a new site template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "409", description = "Conflicted"),
  })
  public SiteTemplate createSiteTemplate(
                                         HttpServletRequest request,
                                         @RequestBody
                                         SiteTemplate siteTemplate) {
    try {
      return siteTemplateService.createSiteTemplate(siteTemplate, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (ObjectAlreadyExistsException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @PostMapping("{siteId}")
  @Secured("users")
  @Operation(summary = "Create a site template based on an existing site", method = "POST", description = "This creates a new site template based on an existing site identified by its identifier")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "409", description = "Conflicted"),
  })
  public SiteTemplate saveAsSiteTemplate(
                                         HttpServletRequest request,
                                         @Parameter(description = "Container Storage identifier")
                                         @PathVariable("siteId")
                                         long siteId,
                                         @RequestBody
                                         SiteTemplate siteTemplate) {
    try {
      return siteTemplateService.saveAsSiteTemplate(siteTemplate, siteId, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (ObjectAlreadyExistsException | SiteLayoutUnresolvedException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("users")
  @Operation(summary = "Update a site template", method = "PUT", description = "This updates an existing site template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public SiteTemplate updateSiteTemplate(
                                         HttpServletRequest request,
                                         @Parameter(description = "site template identifier")
                                         @PathVariable("id")
                                         long id,
                                         @RequestBody
                                         SiteTemplate siteTemplate) {
    try {
      siteTemplate.setId(id);
      return siteTemplateService.updateSiteTemplate(siteTemplate, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  @Secured("users")
  @Operation(summary = "Deletes a site template", method = "DELETE", description = "This deletes an existing site template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deleteSiteTemplate(
                                 HttpServletRequest request,
                                 @Parameter(description = "site template identifier")
                                 @PathVariable("id")
                                 long id) {
    try {
      siteTemplateService.deleteSiteTemplate(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
