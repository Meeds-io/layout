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

import io.meeds.layout.model.PortletInstanceContext;
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

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.service.PortletInstanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/portlet/instances")
@Tag(name = "/layout/rest/portlet/instances", description = "Managing portlet instances")
public class PortletInstanceRest {

  @Autowired
  private PortletInstanceService portletInstanceService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieve portlet instances", method = "GET", description = "This retrieves portlet instances")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public List<PortletInstance> getPortletInstances(
                                                   HttpServletRequest request,
                                                   @Parameter(description = "Portlet instance category identifier")
                                                   @RequestParam(name = "categoryId", required = false, defaultValue = "0")
                                                   long categoryId) {
    return portletInstanceService.getPortletInstances(categoryId, request.getRemoteUser(), request.getLocale(), true);
  }

  @GetMapping("{id}")
  @Secured("users")
  @Operation(summary = "Retrieve a portlet instance designated by its id", method = "GET",
             description = "This will retrieve a portlet instance designated by its id")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public PortletInstance getPortletInstance(
                                            HttpServletRequest request,
                                            @Parameter(description = "Portlet instance identifier")
                                            @PathVariable("id")
                                            long id) {
    try {
      return portletInstanceService.getPortletInstance(id, request.getRemoteUser(), request.getLocale(), true);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @GetMapping("{id}/preferences")
  @Secured("users")
  @Operation(summary = "Retrieve a portlet instance preferences designated by its id",
             method = "GET",
             description = "This will retrieve a portlet instance preferences designated by its id")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public List<PortletInstancePreference> getPortletInstancePreferences(
                                                                       HttpServletRequest request,
                                                                       @Parameter(description = "Portlet instance identifier")
                                                                       @PathVariable("id")
                                                                       long id) {
    try {
      return portletInstanceService.getPortletInstancePreferences(id, new PortletInstanceContext(false, null), request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @PostMapping
  @Secured("users")
  @Operation(summary = "Create a portlet instance", method = "POST", description = "This creates a new portlet instance")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
  })
  public PortletInstance createPortletInstance(
                                               HttpServletRequest request,
                                               @RequestBody
                                               PortletInstance portletInstance) {
    try {
      return portletInstanceService.createPortletInstance(portletInstance, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("users")
  @Operation(summary = "Update a portlet instance", method = "PUT", description = "This updates an existing portlet instance")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updatePortletInstance(
                                    HttpServletRequest request,
                                    @Parameter(description = "Portlet instance identifier")
                                    @PathVariable("id")
                                    long id,
                                    @RequestBody
                                    PortletInstance portletInstance) {
    try {
      portletInstance.setId(id);
      portletInstanceService.updatePortletInstance(portletInstance, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  @Secured("users")
  @Operation(summary = "Deletes a portlet instance", method = "DELETE", description = "This deletes an existing portlet instance")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deletePortletInstance(
                                    HttpServletRequest request,
                                    @Parameter(description = "Portlet instance identifier")
                                    @PathVariable("id")
                                    long id) {
    try {
      portletInstanceService.deletePortletInstance(id, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
