/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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

import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.social.rest.entity.SiteEntity;

import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SiteUpdateModel;
import io.meeds.layout.rest.util.RestEntityBuilder;
import io.meeds.layout.service.SiteLayoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("sites")
@Tag(name = "sites", description = "Managing sites")
public class SiteLayoutRest {

  @Autowired
  private SiteLayoutService siteLayoutService;

  @GetMapping("{siteId}")
  @Operation(summary = "Gets a specific site by its id", description = "Gets site by id", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> getSiteById(
                                                HttpServletRequest request,
                                                @Parameter(description = "site id")
                                                @PathVariable("siteId")
                                                long siteId,
                                                @Parameter(description = "Language used to retrieve names", required = false)
                                                @RequestParam(name = "lang", required = false)
                                                String lang) throws Exception {
    try {
      PortalConfig site = siteLayoutService.getSite(siteId, request.getRemoteUser());
      Locale locale;
      if (StringUtils.isBlank(lang)) {
        locale = request.getLocale();
      } else {
        locale = Locale.forLanguageTag(lang);
      }
      SiteEntity siteEntity = RestEntityBuilder.toSiteEntity(site,
                                                             request,
                                                             locale);
      return ResponseEntity.ok()
                           .eTag(String.valueOf(Objects.hash(siteEntity, locale)))
                           .body(siteEntity);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping
  @Operation(summary = "Gets a specific site by its type and name", description = "Gets site its type and name", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> getSite(
                                            HttpServletRequest request,
                                            @Parameter(description = "site type")
                                            @RequestParam("siteType")
                                            String siteType,
                                            @Parameter(description = "site name")
                                            @RequestParam("siteName")
                                            String siteName,
                                            @Parameter(description = "Language used to retrieve names", required = false)
                                            @RequestParam(name = "lang", required = false)
                                            String lang) throws Exception {
    try {
      PortalConfig site = siteLayoutService.getSite(new SiteKey(siteType, siteName), request.getRemoteUser());
      Locale locale;
      if (StringUtils.isBlank(lang)) {
        locale = request.getLocale();
      } else {
        locale = Locale.forLanguageTag(lang);
      }
      SiteEntity siteEntity = RestEntityBuilder.toSiteEntity(site,
                                                             request,
                                                             locale);
      return ResponseEntity.ok()
                           .eTag(String.valueOf(Objects.hash(siteEntity, locale)))
                           .body(siteEntity);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @DeleteMapping
  @Secured("users")
  @Operation(summary = "Delete a site", method = "GET", description = "This deletes the given site")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deleteSite(
                         HttpServletRequest request,
                         @Parameter(description = "site type")
                         @RequestParam("siteType")
                         String siteType,
                         @Parameter(description = "site name")
                         @RequestParam("siteName")
                         String siteName) {
    try {
      siteLayoutService.deleteSite(new SiteKey(siteType, siteName), request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping
  @Secured("users")
  @Operation(summary = "update a site", method = "PUT", description = "This updates the given site")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updateSite(
                         HttpServletRequest request,
                         @RequestBody
                         SiteUpdateModel updateModel) {
    try {
      siteLayoutService.updateSite(updateModel, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PatchMapping(value = "permissions")
  @Secured("users")
  @Operation(summary = "Update a page access and edit permission", method = "PATCH",
             description = "This updates the given page access and edit permission")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updateSitePermissions(
                                    HttpServletRequest request,
                                    @RequestBody
                                    PermissionUpdateModel permissionUpdateModel) {
    try {
      siteLayoutService.updateSitePermissions(permissionUpdateModel, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PostMapping
  @Secured("users")
  @Operation(summary = "create a site", method = "POST", description = "This create a new site")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> createSite(
                                               HttpServletRequest request,
                                               @Parameter(description = "site to create", required = true)
                                               @RequestBody
                                               SiteCreateModel createModel) throws Exception {
    try {
      PortalConfig site = siteLayoutService.createSite(createModel, request.getRemoteUser());
      SiteEntity siteEntity = RestEntityBuilder.toSiteEntity(site,
                                                             request,
                                                             request.getLocale());
      return ResponseEntity.ok()
                           .eTag(String.valueOf(Objects.hash(siteEntity, request.getLocale())))
                           .body(siteEntity);
    } catch (ObjectAlreadyExistsException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping("{siteId}/labels")
  @Operation(summary = "Retrieve site I18N labels", method = "GET", description = "This retrieves site labels")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public NodeLabel getSiteLabels(
                                 HttpServletRequest request,
                                 @Parameter(description = "Site id", required = true)
                                 @PathVariable("siteId")
                                 Long siteId) {
    try {
      return siteLayoutService.getSiteLabels(siteId, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping("{siteId}/descriptions")
  @Operation(summary = "Retrieve site I18N descriptions", method = "GET", description = "This retrieves site descriptions")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public NodeLabel getSiteDescriptions(
                                       HttpServletRequest request,
                                       @Parameter(description = "Site id", required = true)
                                       @PathVariable("siteId")
                                       Long siteId) {
    try {
      return siteLayoutService.getSiteDescriptions(siteId, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
