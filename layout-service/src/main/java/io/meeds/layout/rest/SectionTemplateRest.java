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
import org.springframework.http.MediaType;
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
import org.exoplatform.portal.mop.page.PageKey;

import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.model.SectionTemplateDetail;
import io.meeds.layout.service.SectionTemplateService;
import io.meeds.layout.service.injection.SectionTemplateImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/sections")
@Tag(name = "/layout/rest/sections", description = "Managing section templates")
public class SectionTemplateRest {

  @Autowired
  private SectionTemplateService       sectionTemplateService;

  @Autowired
  private SectionTemplateImportService sectionTemplateImportService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieve section templates", method = "GET", description = "This retrieves section templates")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public List<SectionTemplateDetail> getSectionTemplates(HttpServletRequest request) {
    return sectionTemplateService.getSectionTemplates(request.getLocale());
  }

  @GetMapping("{id}")
  @Secured("users")
  @Operation(summary = "Retrieve a section template designated by its id", method = "GET", description = "This will retrieve a section template designated by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public SectionTemplateDetail getSectionTemplate(
                                                  HttpServletRequest request,
                                                  @Parameter(description = "Section template identifier")
                                                  @PathVariable("id")
                                                  long id) {
    try {
      return sectionTemplateService.getSectionTemplate(id, request.getLocale());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping
  @Secured("users")
  @Operation(summary = "Create a section template", method = "POST", description = "This creates a new section template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
  })
  public SectionTemplate createSectionTemplate(
                                               HttpServletRequest request,
                                               @RequestBody
                                               SectionTemplate sectionTemplate) {
    try {
      return sectionTemplateService.createSectionTemplate(sectionTemplate, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PostMapping(value = "container/{storageId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Secured("users")
  @Operation(summary = "Create a section template based on an existing container", method = "POST", description = "This creates a new section template based on an existing section identified by its container storage identifier")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
  })
  public SectionTemplate saveAsSectionTemplate(
                                               HttpServletRequest request,
                                               @Parameter(description = "Container Storage identifier")
                                               @PathVariable("storageId")
                                               long containerId,
                                               @Parameter(description = "Page reference")
                                               @RequestParam("pageRef")
                                               String pageRef) {
    try {
      return sectionTemplateService.saveAsSectionTemplate(PageKey.parse(pageRef), containerId, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("users")
  @Operation(summary = "Update a section template", method = "PUT", description = "This updates an existing section template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updateSectionTemplate(
                                    HttpServletRequest request,
                                    @Parameter(description = "Section template identifier")
                                    @PathVariable("id")
                                    long id,
                                    @RequestBody
                                    SectionTemplate sectionTemplate) {
    try {
      sectionTemplate.setId(id);
      sectionTemplateService.updateSectionTemplate(sectionTemplate, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}/restore")
  @Secured("users")
  @Operation(summary = "Restores a system Section template", method = "PUT", description = "This restores a system Section template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Bad request"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void restoreSectionTemplate(
                                     HttpServletRequest request,
                                     @Parameter(description = "Section template identifier")
                                     @PathVariable("id")
                                     long id) {
    try {
      sectionTemplateImportService.restoreSectionTemplate(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping("{id}/nodeId")
  @Secured("users")
  @Operation(summary = "Retrieves the Navigation Node identifier in order to edit the section template", method = "GET", description = "Retrieves the Navigation Node identifier in order to edit the section template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public long generateSectionTemplateNodeId(
                                            HttpServletRequest request,
                                            @Parameter(description = "Section template identifier")
                                            @PathVariable("id")
                                            long id) {
    try {
      return sectionTemplateService.generateSectionTemplateNodeId(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping("{id}/content")
  @Secured("users")
  @Operation(summary = "Retrieves the Edited Section Template Page Content", method = "GET", description = "Retrieves the container content of Section template switch page edited using layout editor")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public String generateSectionTemplateContent(
                                               HttpServletRequest request,
                                               @Parameter(description = "Section template identifier")
                                               @PathVariable("id")
                                               long id) {
    try {
      return sectionTemplateService.generateSectionTemplateContent(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  @Secured("users")
  @Operation(summary = "Deletes a section template", method = "DELETE", description = "This deletes an existing section template")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deleteSectionTemplate(
                                    HttpServletRequest request,
                                    @Parameter(description = "Section template identifier")
                                    @PathVariable("id")
                                    long id) {
    try {
      sectionTemplateService.deleteSectionTemplate(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
