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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.social.rest.entity.SiteEntity;

import io.meeds.layout.model.LayoutModel;
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

  @Autowired
  private LayoutService     layoutService;

  @Autowired
  private UserPortalConfigService portalConfigService;

  @GetMapping("{siteId}")
  @Operation(summary = "Gets a specific site by its id", description = "Gets site by id", method = "GET")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> getSiteById(
                                                WebRequest webRequest,
                                                HttpServletRequest request,
                                                @Parameter(description = "site id")
                                                @PathVariable("siteId")
                                                long siteId,
                                                @Parameter(description = "Language used to retrieve names", required = false)
                                                @RequestParam(name = "lang", required = false)
                                                String lang) throws Exception {
    try {
      PortalConfig site = siteLayoutService.getSite(siteId,
                                                    request.getRemoteUser());
      Locale locale;
      if (StringUtils.isBlank(lang)) {
        locale = request.getLocale();
      } else {
        locale = Locale.forLanguageTag(lang);
      }
      SiteEntity siteEntity = RestEntityBuilder.toSiteEntity(site,
                                                             request,
                                                             locale,
                                                             portalConfigService);
      String eTag = String.valueOf(siteEntity.hashCode());
      if (webRequest.checkNotModified(eTag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      } else {
        return ResponseEntity.ok()
                             .eTag(eTag)
                             .body(siteEntity);
      }
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
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> getSite(
                                            WebRequest webRequest,
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
      PortalConfig site = siteLayoutService.getSite(new SiteKey(siteType, siteName),
                                                    request.getRemoteUser());
      return getSiteById(webRequest, request, site.getId(), lang);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping("layout")
  @Operation(summary = "Gets a specific site layout by its key", description = "Gets a specific site layout by its key", method = "GET")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<LayoutModel> getSiteLayout(
                                                   WebRequest webRequest,
                                                   HttpServletRequest request,
                                                   @Parameter(description = "site type")
                                                   @RequestParam("siteType")
                                                   String siteType,
                                                   @Parameter(description = "site name")
                                                   @RequestParam("siteName")
                                                   String siteName,
                                                   @Parameter(description = "expand options", required = false)
                                                   @RequestParam(name = "expand", required = false)
                                                   String expand) {
    try {
      ModelObject modelObject = siteLayoutService.getSiteLayout(new SiteKey(siteType, siteName),
                                                                request.getRemoteUser());
      LayoutModel layoutModel = RestEntityBuilder.toLayoutModel(modelObject, layoutService, expand);
      String eTag = String.valueOf(layoutModel.hashCode());
      if (webRequest.checkNotModified(eTag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      } else {
        return ResponseEntity.ok()
                             .eTag(eTag)
                             .body(layoutModel);
      }
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
                         @RequestParam("siteType")
                         String siteType,
                         @Parameter(description = "site name")
                         @RequestParam("siteName")
                         String siteName) {
    try {
      siteLayoutService.deleteSite(new SiteKey(siteType, siteName),
                                   request.getRemoteUser());
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
      siteLayoutService.updateSite(updateModel,
                                   request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping(value = "/restore", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Secured("users")
  @Operation(summary = "Restore a builtin site layout, pages and navigation", method = "PUT", description = "This restores a builtin site layout, pages and navigation from builtin configuration")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void restoreSite(
                          HttpServletRequest request,
                          @RequestParam("siteType")
                          String siteType,
                          @Parameter(description = "site name")
                          @RequestParam("siteName")
                          String siteName,
                          @Parameter(description = "Import Mode")
                          @RequestParam(name = "importMode", defaultValue = "MERGE", required = false)
                          ImportMode importMode,
                          @Parameter(description = "Whether restore the site layout or not", required = false)
                          @RequestParam(name = "siteLayout", defaultValue = "false", required = false)
                          Optional<Boolean> siteLayout,
                          @Parameter(description = "Whether restore site page layouts or not", required = false)
                          @RequestParam(name = "pagesLayout", defaultValue = "false", required = false)
                          Optional<Boolean> pagesLayout,
                          @Parameter(description = "Whether restore site navigation tree or not", required = false)
                          @RequestParam(name = "navigation", defaultValue = "false", required = false)
                          Optional<Boolean> navigationTree) {
    try {
      siteLayoutService.restoreSite(new SiteKey(siteType, siteName),
                                    importMode,
                                    siteLayout.orElse(false).booleanValue(),
                                    pagesLayout.orElse(false).booleanValue(),
                                    navigationTree.orElse(false).booleanValue(),
                                    request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PatchMapping(value = "permissions")
  @Secured("users")
  @Operation(summary = "Update a page access and edit permission", method = "PATCH", description = "This updates the given page access and edit permission")
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
      siteLayoutService.updateSitePermissions(permissionUpdateModel,
                                              request.getRemoteUser());
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
    @ApiResponse(responseCode = "409", description = "Conflict"),
  })
  public ResponseEntity<SiteEntity> createSite(
                                               HttpServletRequest request,
                                               @Parameter(description = "site to create", required = true)
                                               @RequestBody
                                               SiteCreateModel createModel) throws Exception {
    try {
      PortalConfig site = siteLayoutService.createSite(createModel,
                                                       request.getRemoteUser());
      SiteEntity siteEntity = RestEntityBuilder.toSiteEntity(site,
                                                             request,
                                                             request.getLocale(),
                                                             portalConfigService);
      return ResponseEntity.ok()
                           .body(siteEntity);
    } catch (ObjectAlreadyExistsException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PostMapping("draft")
  @Secured("users")
  @Operation(summary = "create a site", method = "POST", description = "This create a new site")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<SiteEntity> createDraftSite(
                                                    WebRequest webRequest,
                                                    HttpServletRequest request,
                                                    @Parameter(description = "site type")
                                                    @RequestParam("siteType")
                                                    String siteType,
                                                    @Parameter(description = "site name")
                                                    @RequestParam("siteName")
                                                    String siteName) throws Exception {
    try {
      SiteKey draftSiteKey = siteLayoutService.createDraftSite(new SiteKey(siteType, siteName), request.getRemoteUser());
      return getSite(webRequest, request, draftSiteKey.getTypeName(), draftSiteKey.getName(), null);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("layout")
  @Secured("users")
  @Operation(summary = "Updates an existing site layout", method = "PUT", description = "This updates the designated site layout")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "400", description = "Invalid request input"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public ResponseEntity<LayoutModel> updateSiteLayout(
                                                      WebRequest webRequest,
                                                      HttpServletRequest request,
                                                      @Parameter(description = "site type")
                                                      @RequestParam("siteType")
                                                      String siteType,
                                                      @Parameter(description = "site name")
                                                      @RequestParam("siteName")
                                                      String siteName,
                                                      @Parameter(description = "Whether the page layout update is a draft page publication or not", required = false)
                                                      @RequestParam(name = "publish", required = false)
                                                      Optional<Boolean> publish,
                                                      @Parameter(description = "expand options", required = false)
                                                      @RequestParam(name = "expand", required = false)
                                                      String expand,
                                                      @RequestBody
                                                      LayoutModel layoutModel) {
    try {
      siteLayoutService.updateSiteLayout(new SiteKey(siteType, siteName),
                                         layoutModel.toSite(),
                                         publish.orElse(false).booleanValue(),
                                         request.getRemoteUser());
      return getSiteLayout(webRequest, request, siteType, siteName, expand);
    } catch (ObjectNotFoundException e) {
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
      return siteLayoutService.getSiteLabels(siteId,
                                             request.getRemoteUser());
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
      return siteLayoutService.getSiteDescriptions(siteId,
                                                   request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
