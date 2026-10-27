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
package io.meeds.layout.mcp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.constant.SectionType;
import io.meeds.layout.mcp.model.ElementDisplay;
import io.meeds.layout.mcp.model.ElementStyling;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.ScheduleDescription;
import io.meeds.layout.mcp.model.SectionDescription;
import io.meeds.layout.mcp.model.SiteContainerDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteLayoutUpdate;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.model.NavigationCreateModel;
import io.meeds.layout.model.NavigationItemUpdate;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PageCreateModel;
import io.meeds.layout.model.PageTemplate;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SitePropertiesUpdate;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.LayoutStylesAssembler;
import io.meeds.layout.service.NavigationLayoutService;
import io.meeds.layout.service.PageLayoutAssembler;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.PageTemplateService;
import io.meeds.layout.service.SiteLayoutAssembler;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.layout.service.SiteTemplateService;
import io.meeds.layout.util.EntityMapper;

import lombok.SneakyThrows;

@SpringBootTest(classes = { LayoutMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
public class LayoutMcpToolTest {

  private static final String      USER      = "john";

  private static final long        SITE_ID   = 5L;

  private static final SiteKey     SITE_KEY  = SiteKey.portal("test");

  private static final PageKey     PAGE_KEY  = PageKey.parse("portal::test::home");

  private static final PageKey     DRAFT_KEY = PageKey.parse("portal::test::home_draft_john");

  private static final long        LIVE_NODE = 11L;

  private static final long        ROOT_NODE = 7L;

  private static final long        NODE_ID   = 12L;

  private static final String      FUTURE    = Instant.now().plusSeconds(3600).toString();

  private static final String      LATER     = Instant.now().plusSeconds(7200).toString();

  @MockitoBean
  private LayoutService            layoutService;

  @MockitoBean
  private LayoutAclService         aclService;

  @MockitoBean
  private SiteLayoutService        siteLayoutService;

  @MockitoBean
  private PageLayoutService        pageLayoutService;

  @MockitoBean
  private LayoutDescriptionService descriptionService;

  @MockitoBean
  private LayoutDraftService       draftService;

  @MockitoBean
  private SiteTemplateService      siteTemplateService;

  @MockitoBean
  private OrganizationService      organizationService;

  @MockitoBean
  private UploadService            uploadService;

  @MockitoBean
  private NavigationLayoutService  navigationLayoutService;

  @MockitoBean
  private PageTemplateService      pageTemplateService;

  @MockitoBean
  private LocaleConfigService      localeConfigService;

  @Autowired
  private LayoutMcpTool            tool;

  private ConversationState        conversationState;

  @BeforeEach
  public void setUp() {
    Identity identity = mock(Identity.class);
    lenient().when(identity.getUserId()).thenReturn(USER);
    conversationState = mock(ConversationState.class);
    lenient().when(conversationState.getIdentity()).thenReturn(identity);
    lenient().when(draftService.isDraftNode(any())).thenAnswer(i -> i.getArgument(0) != null && ((String) i.getArgument(0)).contains("_draft_"));
  }

  @Test
  public void listSitesIsDescribedAsTheCurrentUser() {
    List<SiteSummary> expected = List.of(new SiteSummary(1L, "portal", "test", "Test", null, true));
    when(descriptionService.describeSites(USER, "te")).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.listSites("te")));
  }

  @Test
  public void siteRefusalsCarryTheFixedTexts() throws Exception {
    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    when(siteLayoutService.getSite(2L, USER)).thenThrow(new IllegalAccessException("internal detail"));

    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getSite(1L))).getMessage());
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, assertThrows(IllegalAccessException.class, () -> asUser(() -> tool.getSite(2L))).getMessage());
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.listSitePages(1L, null))).getMessage());
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getSite(null))).getMessage());
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.listSitePages(0L, true))).getMessage());
  }

  @Test
  public void draftSiteIsNotReachableByItsOwnId() throws Exception {
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_bob", "draft_9");
    when(siteLayoutService.getSite(9L, USER)).thenReturn(draftSite);

    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getSite(9L))).getMessage());
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.listSitePages(9L, null))).getMessage());
    verify(descriptionService, never()).toSiteDescription(any(), any(), any(), any(), any(Boolean.class));
  }

  @Test
  public void getSiteReadsTheUserDraftUnderTheOwnerRule() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    site.setPortalLayout(new Container("live"));
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(label("Site label"));
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(label("Site description"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    when(descriptionService.toSiteDescription(site, draftSite.getPortalLayout(), "Site label", "Site description", true)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
    verify(descriptionService).filterAccessible(draftSite.getPortalLayout(), USER);
    verify(layoutService, never()).getPortalConfig(any(SiteKey.class));
  }

  @Test
  public void draftSiteTheUserMayNotReadIsNotADraft() throws Exception {
    PortalConfig site = site("portal", "test", null);
    site.setPortalLayout(new Container("live"));
    SiteKey foreignDraft = SiteKey.draft("portal_test_draft_bob");
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(new NodeLabel());
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(null);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(foreignDraft);
    when(siteLayoutService.getSite(foreignDraft, USER)).thenThrow(new IllegalAccessException("owner only"));
    when(descriptionService.filterAccessible(site.getPortalLayout(), USER)).thenReturn(site.getPortalLayout());
    when(descriptionService.toSiteDescription(site, site.getPortalLayout(), "test", null, false)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
  }

  @Test
  public void groupSiteLabelsAreReadFromTheSiteItself() throws Exception {
    PortalConfig site = site("group", "/spaces/marketing", "Marketing");
    site.setDescription("The marketing space");
    site.setPortalLayout(new Container("live"));
    SiteDescription expected = mock(SiteDescription.class);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    when(descriptionService.filterAccessible(site.getPortalLayout(), USER)).thenReturn(site.getPortalLayout());
    when(descriptionService.toSiteDescription(site, site.getPortalLayout(), "Marketing", "The marketing space", false)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getSite(SITE_ID)));
    verify(siteLayoutService, never()).getSiteLabels(anyLong(), any());
    verify(siteLayoutService, never()).getSiteDescriptions(anyLong(), any());
  }

  @Test
  public void listSitePagesIsDescribedForTheSiteKey() throws Exception {
    PortalConfig site = site("portal", "test", null);
    List<NavigationItemDescription> expected = List.of();
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(descriptionService.describeNavigation(SITE_KEY, true, USER)).thenReturn(expected);
    when(descriptionService.describeNavigation(SITE_KEY, false, USER)).thenReturn(List.of(mock(NavigationItemDescription.class)));

    assertEquals(expected, asUser(() -> tool.listSitePages(SITE_ID, true)));
    assertEquals(1, asUser(() -> tool.listSitePages(SITE_ID, null)).size());
    assertEquals(1, asUser(() -> tool.listSitePages(SITE_ID, false)).size());
  }

  @Test
  public void pageRefusalsCarryTheFixedTexts() throws Exception {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(null);
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null))).getMessage());

    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null))).getMessage());

    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout(" ", null))).getMessage());
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout("not-a-ref", null))).getMessage());
    assertEquals("value is not accepted for version",
                 assertThrows(IllegalArgumentException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "live"))).getMessage());
    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class), any());
  }

  @Test
  public void draftPageAddressedByItsOwnRefDoesNotExist() throws Exception {
    when(draftService.isDraftNode("home_draft_bob")).thenReturn(true);

    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> asUser(() -> tool.getPageLayout("portal::test::home_draft_bob", null))).getMessage());
    verify(layoutService, never()).getPage(any(PageKey.class));
  }

  @Test
  public void draftIsReadByDefaultWhenTheRevisionNamesOne() throws Exception {
    viewablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    ModelObject tree = new Container("draft");
    PageDescription expected = mock(PageDescription.class);
    when(pageLayoutService.getPageLayout(DRAFT_KEY, USER)).thenReturn(tree);
    when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.DRAFT)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "draft")));
  }

  @Test
  public void publishedIsReadWhenAskedForOrWhenNoRevisionNamesADraft() throws Exception {
    viewablePage();
    ModelObject tree = new Container("live");
    PageDescription expected = mock(PageDescription.class);
    when(pageLayoutService.getPageLayout(PAGE_KEY, USER)).thenReturn(tree);
    when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.PUBLISHED)).thenReturn(expected);

    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "published")));

    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));

    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));
    verify(pageLayoutService, never()).getPageLayout(DRAFT_KEY, USER);
  }

  @Test
  public void explicitDraftWithoutARevisionDoesNotExist() throws Exception {
    viewablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);

    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "draft"))).getMessage());
    verify(pageLayoutService, never()).getPageLayout(any(PageKey.class), any());
  }

  @Test
  public void createSiteUsesTheSitesAdministrationDefaults() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false), template(8L, "public", true));
    PortalConfig created = site("portal", "marketing", "Marketing");
    ArgumentCaptor<SiteCreateModel> model = ArgumentCaptor.forClass(SiteCreateModel.class);
    when(siteLayoutService.createSite(model.capture(), eq(USER))).thenReturn(created);
    SiteDescription expected = describedSite(created);

    assertEquals(expected, asUser(() -> tool.createSite(" marketing ", " Marketing ", null, null, null, null, null, null, null)));
    assertEquals(7L, model.getValue().getSiteId());
    PortalConfig config = model.getValue().getPortalConfig();
    assertEquals(PortalConfig.PORTAL_TYPE, config.getType());
    assertEquals("marketing", config.getName());
    assertEquals("Marketing", config.getLabel());
    assertNull(config.getDescription());
    assertFalse(config.isDisplayed());
    assertEquals(0, config.getDisplayOrder());
    assertEquals(LayoutMcpTool.DEFAULT_ICON, config.getIcon());
    assertNull(config.getAccessPermissions());
    assertNull(config.getEditPermission());
    assertNull(config.getBannerUploadId());
    verifyNoInteractions(uploadService, organizationService);
  }

  @Test
  public void createSiteCarriesEveryStatedOption() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false), template(9L, "public", false));
    permissions();
    upload("u1");
    PortalConfig created = site("portal", "marketing", "Marketing");
    ArgumentCaptor<SiteCreateModel> model = ArgumentCaptor.forClass(SiteCreateModel.class);
    when(siteLayoutService.createSite(model.capture(), eq(USER))).thenReturn(created);
    SiteDescription expected = describedSite(created);

    assertEquals(expected,
                 asUser(() -> tool.createSite("marketing",
                                              "Marketing",
                                              " About us ",
                                              "public",
                                              "u1",
                                              List.of(" Everyone ", "*:/platform/users", " member:/spaces/marketing"),
                                              "manager:/spaces/marketing",
                                              true,
                                              "fa-star")));
    assertEquals(9L, model.getValue().getSiteId());
    PortalConfig config = model.getValue().getPortalConfig();
    assertEquals("About us", config.getDescription());
    assertTrue(config.isDisplayed());
    assertEquals(0, config.getDisplayOrder());
    assertEquals("fa-star", config.getIcon());
    assertArrayEquals(new String[] { "Everyone", "*:/platform/users", "member:/spaces/marketing" }, config.getAccessPermissions());
    assertEquals("manager:/spaces/marketing", config.getEditPermission());
    assertEquals("u1", config.getBannerUploadId());
    verify(uploadService, never()).removeUploadResource(any());
  }

  @Test
  public void createSiteIsRefusedBeforeAnyLookupWhenTheUserMayNotAddASite() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(false);

    assertEquals(LayoutMcpTool.CREATE_SITE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.createSite("marketing", "Marketing", null, null, "u1", null, null, null, null)));
    verifyNoInteractions(siteTemplateService, organizationService, uploadService);
    verify(siteLayoutService, never()).createSite(any(), any());
  }

  @Test
  public void createSiteRefusesEachValueOutsideTheVocabularyBeforeAnyWrite() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false), template(8L, "public", true));
    permissions();

    for (String name : new String[] { "bad name", "", "  ", null, "a".repeat(201), "mark/eting" }) {
      assertEquals(LayoutMcpTool.NAME_NOT_ACCEPTED, refusal(() -> tool.createSite(name, "Marketing", null, null, null, null, null, null, null)), name);
    }
    for (String label : new String[] { " ", "", null, "a".repeat(201) }) {
      assertEquals(LayoutMcpTool.LABEL_NOT_ACCEPTED, refusal(() -> tool.createSite("marketing", label, null, null, null, null, null, null, null)), label);
    }
    assertEquals(LayoutMcpTool.DESCRIPTION_NOT_ACCEPTED,
                 refusal(() -> tool.createSite("marketing", "Marketing", "a".repeat(2001), null, null, null, null, null, null)));
    for (String template : new String[] { "public", "nope" }) {
      assertEquals(LayoutMcpTool.TEMPLATE_NOT_ACCEPTED, refusal(() -> tool.createSite("marketing", "Marketing", null, template, null, null, null, null, null)), template);
    }
    for (String icon : new String[] { "fa-globe; color: red", "Fa-Globe", " ", "" }) {
      assertEquals(LayoutMcpTool.ICON_NOT_ACCEPTED, refusal(() -> tool.createSite("marketing", "Marketing", null, null, null, null, null, null, icon)), icon);
    }
    for (List<String> access : List.<List<String>> of(List.of(), List.of("nope"), List.of("*:/nope"), List.of("admin:/platform/users"), List.of("/platform/users"), List.of("Everyone", "*"))) {
      assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED, refusal(() -> tool.createSite("marketing", "Marketing", null, null, null, access, null, null, null)), access.toString());
    }
    for (String edit : new String[] { "Everyone", "*:/nope", "admin:/platform/users", "users" }) {
      assertEquals(LayoutMcpTool.EDIT_PERMISSION_NOT_ACCEPTED, refusal(() -> tool.createSite("marketing", "Marketing", null, null, null, null, edit, null, null)), edit);
    }
    verify(siteLayoutService, never()).createSite(any(), any());
    verifyNoInteractions(uploadService);
  }

  @Test
  public void createSiteAcceptsTheLongestLabelAndDescriptionTheSiteStores() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false));
    PortalConfig created = site("portal", "marketing", "Marketing");
    ArgumentCaptor<SiteCreateModel> model = ArgumentCaptor.forClass(SiteCreateModel.class);
    when(siteLayoutService.createSite(model.capture(), eq(USER))).thenReturn(created);
    describedSite(created);

    asUser(() -> tool.createSite("marketing", "l".repeat(200), "d".repeat(2000), null, null, null, null, null, null));
    assertEquals("l".repeat(200), model.getValue().getPortalConfig().getLabel());
    assertEquals("d".repeat(2000), model.getValue().getPortalConfig().getDescription());
  }

  /**
   * A write may take the view right from the caller, a non-administrator
   * creating a site readable by administrators only, or an editor naming
   * groups they are not in: the answer is still the site as stored
   */
  @Test
  public void writesAnswerTheStoredSiteEvenWhenTheCallerMayNoLongerViewIt() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false));
    PortalConfig created = site("portal", "marketing", "Marketing");
    when(siteLayoutService.createSite(any(), eq(USER))).thenReturn(created);
    SiteDescription createdDescription = describedSite(created);
    when(siteLayoutService.getSite(anyLong(), eq(USER))).thenThrow(new IllegalAccessException("not viewable any more"));
    doThrow(new IllegalAccessException("not viewable any more")).when(siteLayoutService).getSiteLabels(SITE_ID, USER);

    assertEquals(createdDescription, asUser(() -> tool.createSite("marketing", "Marketing", null, null, null, null, null, null, null)));

    PortalConfig site = site("portal", "test", "Test");
    doReturn(site).doThrow(new IllegalAccessException("not viewable any more")).when(siteLayoutService).getSite(SITE_ID, USER);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    SiteDescription updatedDescription = describedSite(site);
    doThrow(new IllegalAccessException("not viewable any more")).when(siteLayoutService).getSiteLabels(SITE_ID, USER);

    assertEquals(updatedDescription, asUser(() -> tool.updateSite(SITE_ID, null, null, null, null, false, null, null, null)));
    verify(siteLayoutService, times(1)).getSite(SITE_ID, USER);
    verify(siteLayoutService, never()).getSiteDescriptions(anyLong(), any());
  }

  /**
   * A write's answer resolves the label and the description as get_site
   * does, through the user's portal, while the caller may still view the site
   */
  @Test
  public void writesResolveTheLabelAsGetSiteDoes() throws Exception {
    PortalConfig site = site("portal", "test", "#{portal.test.label}");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(layoutService.getPortalConfig(SITE_KEY)).thenReturn(site);
    when(siteLayoutService.getSiteLabels(SITE_ID, USER)).thenReturn(label("Test site"));
    when(siteLayoutService.getSiteDescriptions(SITE_ID, USER)).thenReturn(label("The test site"));
    when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    SiteDescription expected = mock(SiteDescription.class);
    when(descriptionService.toSiteDescription(eq(site), any(), eq("Test site"), eq("The test site"), eq(false))).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.updateSite(SITE_ID, null, null, null, null, true, null, null, null)));
  }

  @Test
  public void createSiteResolvesTheBannerBeforeAnyWriteAndReleasesItOnFailure() throws Exception {
    when(aclService.canAddSite(USER)).thenReturn(true);
    templates(template(7L, "basic", false));
    upload("u1");

    assertEquals(LayoutMcpTool.UPLOAD_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.createSite("marketing", "Marketing", null, null, "u0", null, null, null, null)));
    verify(siteLayoutService, never()).createSite(any(), any());
    verify(uploadService, never()).removeUploadResource(any());

    when(siteLayoutService.createSite(any(), eq(USER))).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.CREATE_SITE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.createSite("marketing", "Marketing", null, null, "u1", null, null, null, null)));
    verify(uploadService, times(1)).removeUploadResource("u1");

    when(siteLayoutService.createSite(any(), eq(USER))).thenThrow(new ObjectAlreadyExistsException("internal detail"));
    assertEquals(LayoutMcpTool.SITE_ALREADY_EXISTS,
                 refusal(() -> tool.createSite("marketing", "Marketing", null, null, "u1", null, null, null, null)));
    verify(uploadService, times(2)).removeUploadResource("u1");

    when(siteLayoutService.createSite(any(), eq(USER))).thenAnswer(i -> {
      throw new ObjectNotFoundException("Site with id 7 doesn't exist");
    });
    assertEquals(LayoutMcpTool.TEMPLATE_NOT_ACCEPTED,
                 refusal(() -> tool.createSite("marketing", "Marketing", null, null, "u1", null, null, null, null)));
    verify(uploadService, times(3)).removeUploadResource("u1");

    when(siteLayoutService.createSite(any(), eq(USER))).thenThrow(new IllegalStateException("Error while saving site image file"));
    assertEquals(LayoutMcpTool.SITE_NOT_SAVED,
                 refusal(IllegalStateException.class, () -> tool.createSite("marketing", "Marketing", null, null, "u1", null, null, null, null)));
    verify(uploadService, times(4)).removeUploadResource("u1");
  }

  @Test
  public void updateSiteStatesOnlyTheGivenOptions() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    permissions();
    upload("u1");
    SiteDescription expected = describedSite(site);
    ArgumentCaptor<SitePropertiesUpdate> properties = ArgumentCaptor.forClass(SitePropertiesUpdate.class);
    ArgumentCaptor<PermissionUpdateModel> permissions = ArgumentCaptor.forClass(PermissionUpdateModel.class);

    assertEquals(expected, asUser(() -> tool.updateSite(SITE_ID, null, null, null, null, true, null, null, null)));
    verify(siteLayoutService).updateSiteAndPermissions(eq(SITE_KEY), properties.capture(), permissions.capture(), eq(USER));
    assertEquals(new SitePropertiesUpdate(null, null, null, true, null, null), properties.getValue());
    assertNull(permissions.getValue());

    assertEquals(expected,
                 asUser(() -> tool.updateSite(SITE_ID, " New ", "", List.of("Everyone "), " *:/platform/users ", null, "fa-star", "u1", null)));
    verify(siteLayoutService, times(2)).updateSiteAndPermissions(eq(SITE_KEY), properties.capture(), permissions.capture(), eq(USER));
    assertEquals(new SitePropertiesUpdate("New", "", "fa-star", null, "u1", null), properties.getValue());
    assertEquals(new PermissionUpdateModel("portal", "test", List.of("Everyone"), "*:/platform/users"), permissions.getValue());

    assertEquals(expected, asUser(() -> tool.updateSite(SITE_ID, null, null, null, "member:/spaces/marketing", null, null, null, true)));
    verify(siteLayoutService, times(3)).updateSiteAndPermissions(eq(SITE_KEY), properties.capture(), permissions.capture(), eq(USER));
    assertEquals(new SitePropertiesUpdate(null, null, null, null, null, true), properties.getValue());
    assertEquals(new PermissionUpdateModel("portal", "test", null, "member:/spaces/marketing"), permissions.getValue());
    verify(uploadService, never()).removeUploadResource(any());
  }

  @Test
  public void updateSiteRefusalsCarryTheFixedTextsAndReleaseTheBanner() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSite(9L, USER)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9"));
    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    permissions();
    upload("u1");

    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updateSite(1L, "New", null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updateSite(9L, "New", null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> tool.updateSite(0L, "New", null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.updateSite(SITE_ID, "New", null, null, null, null, null, "u1", null)));
    verifyNoInteractions(uploadService);

    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    assertEquals(LayoutMcpTool.LABEL_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, " ", null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.LABEL_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, "a".repeat(201), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.DESCRIPTION_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, "a".repeat(2001), null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ICON_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, null, null, null, null, "x;y", null, null)));
    assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, null, List.of("*:/nope"), null, null, null, null, null)));
    assertEquals(LayoutMcpTool.EDIT_PERMISSION_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, null, null, "Everyone", null, null, null, null)));
    assertEquals(LayoutMcpTool.REMOVE_BANNER_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, null, null, null, null, null, "u1", true)));
    assertEquals(LayoutMcpTool.UPLOAD_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updateSite(SITE_ID, null, null, null, null, null, null, "u0", null)));
    verify(siteLayoutService, never()).updateSiteAndPermissions(any(), any(), any(), any());
    verify(uploadService, never()).removeUploadResource(any());

    doThrow(new ObjectNotFoundException("internal detail")).when(siteLayoutService).updateSiteAndPermissions(eq(SITE_KEY), any(), any(), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updateSite(SITE_ID, "New", null, null, null, null, null, "u1", null)));
    verify(uploadService, times(1)).removeUploadResource("u1");
    doThrow(new IllegalAccessException("internal detail")).when(siteLayoutService).updateSiteAndPermissions(eq(SITE_KEY), any(), any(), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.updateSite(SITE_ID, "New", null, null, null, null, null, "u1", null)));
    verify(uploadService, times(2)).removeUploadResource("u1");
    doThrow(new IllegalStateException("Can't find uploaded resource with id : u1")).when(siteLayoutService)
                                                                                 .updateSiteAndPermissions(eq(SITE_KEY), any(), any(), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.updateSite(SITE_ID, "New", null, null, null, null, null, "u1", null)));
    verify(uploadService, times(3)).removeUploadResource("u1");
    doThrow(new IllegalArgumentException(SiteLayoutService.DISPLAYED_NOT_ACCEPTED)).when(siteLayoutService)
                                                                                   .updateSiteAndPermissions(eq(SITE_KEY), any(), any(), eq(USER));
    assertEquals(SiteLayoutService.DISPLAYED_NOT_ACCEPTED, refusal(() -> tool.updateSite(SITE_ID, null, null, null, null, false, null, "u1", null)));
    verify(uploadService, times(4)).removeUploadResource("u1");
    doThrow(new IllegalArgumentException("uploadId is mandatory")).when(siteLayoutService)
                                                                  .updateSiteAndPermissions(eq(SITE_KEY), any(), any(), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.updateSite(SITE_ID, "New", null, null, null, null, null, "u1", null)));
    verify(uploadService, times(5)).removeUploadResource("u1");
  }

  /**
   * The sites administration lists portal sites that are not a space's
   * public site, and offers update and delete on those only
   */
  @Test
  public void updateAndDeleteRefuseTheSitesTheAdministrationDoesNotList() throws Exception {
    PortalConfig spaceSite = site("group", "/spaces/marketing", "Marketing");
    PortalConfig userSite = site("user", "john", "John");
    PortalConfig templateSite = site("portal_template", "basic", "Basic");
    PortalConfig publicSite = site("portal", "marketing-public", "Marketing");
    publicSite.setProperty("IS_SPACE_PUBLIC_SITE", "true");
    when(siteLayoutService.getSite(1L, USER)).thenReturn(spaceSite);
    when(siteLayoutService.getSite(2L, USER)).thenReturn(userSite);
    when(siteLayoutService.getSite(3L, USER)).thenReturn(templateSite);
    when(siteLayoutService.getSite(4L, USER)).thenReturn(publicSite);
    when(aclService.canEditSite(any(), eq(USER))).thenReturn(true);

    for (long id : new long[] { 1L, 2L, 3L, 4L }) {
      assertEquals(LayoutMcpTool.SITE_NOT_UPDATABLE, refusal(() -> tool.updateSite(id, "New", null, null, null, null, null, null, null)), String.valueOf(id));
      assertEquals(LayoutMcpTool.SITE_NOT_REMOVABLE, refusal(() -> { tool.deleteSite(id); return null; }), String.valueOf(id));
    }
    verify(siteLayoutService, never()).updateSiteAndPermissions(any(), any(), any(), any());
    verify(siteLayoutService, never()).deleteSite(any(), any());
  }

  @Test
  public void deleteSiteRefusesWhatTheSitesAdministrationNeverOffers() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    PortalConfig platformSite = site("portal", "test", "Test");
    platformSite.setRemovable(false);
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(siteLayoutService.getSite(6L, USER)).thenReturn(platformSite);
    when(siteLayoutService.getSite(9L, USER)).thenReturn(new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9"));
    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    when(siteLayoutService.getSite(2L, USER)).thenThrow(new IllegalAccessException("internal detail"));

    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> { tool.deleteSite(null); return null; }));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> { tool.deleteSite(1L); return null; }));
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> { tool.deleteSite(2L); return null; }));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> { tool.deleteSite(9L); return null; }));
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> { tool.deleteSite(SITE_ID); return null; }));
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    assertEquals(LayoutMcpTool.SITE_NOT_REMOVABLE, refusal(() -> { tool.deleteSite(6L); return null; }));
    verify(siteLayoutService, never()).deleteSite(any(), any());

    asUser(() -> {
      tool.deleteSite(SITE_ID);
      return null;
    });
    verify(siteLayoutService).deleteSite(SITE_KEY, USER);

    doThrow(new IllegalAccessException("internal detail")).when(siteLayoutService).deleteSite(SITE_KEY, USER);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> { tool.deleteSite(SITE_ID); return null; }));
    doThrow(new ObjectNotFoundException("internal detail")).when(siteLayoutService).deleteSite(SITE_KEY, USER);
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> { tool.deleteSite(SITE_ID); return null; }));
    doThrow(new IllegalStateException("engine detail")).when(siteLayoutService).deleteSite(SITE_KEY, USER);
    assertEquals(LayoutMcpTool.SITE_NOT_DELETED, refusal(IllegalStateException.class, () -> { tool.deleteSite(SITE_ID); return null; }));
  }


  @Test
  public void listLayoutApplicationsIsDescribedInTheUserLanguage() {
    List<LayoutApplicationEntry> expected = List.of(new LayoutApplicationEntry(10L, "content/NewsListView", "News", null, "Content", false, null));
    try (MockedStatic<LocaleContextInfoUtils> locales = mockStatic(LocaleContextInfoUtils.class)) {
      locales.when(() -> LocaleContextInfoUtils.getUserLocale(USER)).thenReturn(Locale.FRENCH);
      when(descriptionService.describeApplications(2L, "news", USER, Locale.FRENCH)).thenReturn(expected);

      assertEquals(expected, asUser(() -> tool.listLayoutApplications(2L, "news")));
    }
  }

  @Test
  public void setSiteLayoutBuildsTheDraftFromTheLiveLayoutOnTheFirstWrite() throws Exception {
    PortalConfig site = editableSite();
    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    LayoutModel layout = siteLayout();
    SiteContainerDescription topbar = new SiteContainerDescription(null, null, null, null);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null, draftKey);
    when(descriptionService.toSiteLayout(new SiteLayoutUpdate(null, null, List.of(topbar), null), site, true, USER, Locale.FRENCH)).thenReturn(layout);
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(siteLayoutService.getSite(draftKey, USER)).thenReturn(draftSite);
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUserInFrench(() -> tool.setSiteLayout(SITE_ID, null, null, List.of(topbar), null)));

    ArgumentCaptor<PortalConfig> saved = ArgumentCaptor.forClass(PortalConfig.class);
    verify(siteLayoutService).updateSiteLayout(eq(draftKey), saved.capture(), eq(false), eq(USER));
    assertEquals(SiteLayoutAssembler.SITE_TEMPLATE, ((Container) saved.getValue().getPortalLayout()).getTemplate());
    verify(draftService).createDraftSite(site, USER);
  }

  @Test
  public void setSiteLayoutWritesTheExistingDraftTheRevisionNames() throws Exception {
    PortalConfig site = editableSite();
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    SiteContainerDescription left = new SiteContainerDescription("12", null, null, null);
    when(descriptionService.toSiteLayout(any(), any(), anyBoolean(), any(), any())).thenReturn(siteLayout());
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUserInFrench(() -> tool.setSiteLayout(SITE_ID, left, null, null, null)));

    verify(descriptionService).toSiteLayout(new SiteLayoutUpdate(left, null, null, null), draftSite, false, USER, Locale.FRENCH);
    verify(siteLayoutService).updateSiteLayout(eq(draftSite.getSiteKey()), any(PortalConfig.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftSite(any(), any());
  }

  @Test
  public void setSiteLayoutRefusalsCarryTheFixedTexts() throws Exception {
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> tool.setSiteLayout(0L, null, null, null, null)));
    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setSiteLayout(1L, null, null, null, null)));
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    verify(descriptionService, never()).toSiteLayout(any(), any(), anyBoolean(), any(), any());

    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    SiteLayoutUpdate update = new SiteLayoutUpdate(null, null, null, null);
    when(descriptionService.toSiteLayout(update, site, true, USER, Locale.FRENCH)).thenThrow(new IllegalArgumentException(SiteLayoutAssembler.WIDTH_NOT_ACCEPTED));
    assertEquals(SiteLayoutAssembler.WIDTH_NOT_ACCEPTED, refusalInFrench(IllegalArgumentException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    doThrow(new IllegalAccessException(SiteLayoutAssembler.SITE_LAYOUT_HIDDEN)).when(descriptionService).toSiteLayout(update, site, true, USER, Locale.FRENCH);
    assertEquals(SiteLayoutAssembler.SITE_LAYOUT_HIDDEN, refusalInFrench(IllegalAccessException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    verify(draftService, never()).createDraftSite(any(), any());

    doReturn(siteLayout()).when(descriptionService).toSiteLayout(update, site, true, USER, Locale.FRENCH);
    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    doThrow(new IllegalStateException("Can't find uploaded resource with id : 7d2")).when(siteLayoutService)
                                                                                    .updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.SITE_LAYOUT_NOT_SAVED, refusalInFrench(IllegalStateException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    doThrow(new IllegalStateException(SiteLayoutService.SITE_OUTDATED_MESSAGE)).when(siteLayoutService)
                                                                                .updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED, refusalInFrench(IllegalStateException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    doThrow(new IllegalArgumentException("Invalid css value input javascript:alert(1)")).when(siteLayoutService)
                                                                                         .updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.STYLING_NOT_ACCEPTED, refusalInFrench(IllegalArgumentException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    doThrow(new IllegalAccessException("internal detail")).when(siteLayoutService).updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusalInFrench(IllegalAccessException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
    doThrow(new ObjectNotFoundException("internal detail")).when(siteLayoutService).updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusalInFrench(ObjectNotFoundException.class, () -> tool.setSiteLayout(SITE_ID, null, null, null, null)));
  }

  @Test
  public void publishLayoutPublishesTheDraftAndAnswersThePublishedSite() throws Exception {
    PortalConfig site = editableSite();
    SiteDescription expected = describedSite(site);

    assertEquals(expected, asUser(() -> tool.publishLayout(SITE_ID, null)));
    verify(draftService).publishSite(site, USER);

    doThrow(new ObjectNotFoundException("no draft")).when(draftService).publishSite(site, USER);
    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.publishLayout(SITE_ID, null)));
    doThrow(new IllegalAccessException("internal detail")).when(draftService).publishSite(site, USER);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.publishLayout(SITE_ID, null)));
    doThrow(new IllegalStateException("Application wasn't found with state x")).when(draftService).publishSite(site, USER);
    assertEquals(LayoutMcpTool.SITE_LAYOUT_NOT_PUBLISHED, refusal(IllegalStateException.class, () -> tool.publishLayout(SITE_ID, null)));
    doThrow(new IllegalStateException(SiteLayoutService.SITE_OUTDATED_MESSAGE)).when(draftService).publishSite(site, USER);
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED, refusal(IllegalStateException.class, () -> tool.publishLayout(SITE_ID, null)));

    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.publishLayout(SITE_ID, null)));
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> tool.publishLayout(null, null)));
    verify(draftService, times(5)).publishSite(site, USER);
  }

  @Test
  public void discardLayoutDraftStartsAgainFromThePublishedLayout() throws Exception {
    PortalConfig site = editableSite();
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_10");
    draftSite.setPortalLayout(new Container("draft"));
    when(draftService.discardDraftSite(site, USER)).thenReturn(draftSite.getSiteKey());
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUser(() -> tool.discardLayoutDraft(SITE_ID, null)));

    doThrow(new ObjectNotFoundException("no draft")).when(draftService).discardDraftSite(site, USER);
    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.discardLayoutDraft(SITE_ID, null)));
    doThrow(new IllegalStateException("engine detail")).when(draftService).discardDraftSite(site, USER);
    assertEquals(LayoutMcpTool.SITE_LAYOUT_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.discardLayoutDraft(SITE_ID, null)));
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.discardLayoutDraft(SITE_ID, null)));
    verify(draftService, times(3)).discardDraftSite(site, USER);
  }

  @Test
  public void setPageLayoutBuildsTheDraftFromTheLivePageOnTheFirstWrite() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null, DRAFT_KEY);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    List<SectionDescription> sections = List.of(new SectionDescription(null, SectionType.DYNAMIC, 2, null, null, null, null, null));
    LayoutModel layout = pageLayout();
    when(descriptionService.toPageLayout(sections, stored, true, USER, Locale.FRENCH)).thenReturn(layout);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    PageDescription expected = describedDraft();

    assertEquals(expected, asUserInFrench(() -> tool.setPageLayout(PAGE_KEY.format(), sections)));

    ArgumentCaptor<Page> saved = ArgumentCaptor.forClass(Page.class);
    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), saved.capture(), eq(false), eq(USER));
    assertEquals(EntityMapper.PAGE_LAYOUT_TEMPLATE, ((Container) saved.getValue().getChildren().get(0)).getTemplate());
    verify(draftService).createDraftPage(LIVE_NODE, USER);
  }

  @Test
  public void setPageLayoutWritesTheExistingDraftTheRevisionNames() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    Page stored = new Page("draft");
    when(pageLayoutService.getPageLayout(DRAFT_KEY)).thenReturn(stored);
    List<SectionDescription> sections = List.of();
    when(descriptionService.toPageLayout(sections, stored, false, USER, Locale.FRENCH)).thenReturn(pageLayout());
    PageDescription expected = describedDraft();

    assertEquals(expected, asUserInFrench(() -> tool.setPageLayout(PAGE_KEY.format(), sections)));

    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(Page.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftPage(anyLong(), any());
  }

  @Test
  public void setPageLayoutRefusalsCarryTheFixedTexts() throws Exception {
    List<SectionDescription> sections = List.of();
    when(layoutService.getPage(DRAFT_KEY)).thenReturn(new Page());
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setPageLayout(DRAFT_KEY.format(), sections)));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.publishLayout(null, DRAFT_KEY.format())));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.discardLayoutDraft(null, DRAFT_KEY.format())));
    verify(aclService, never()).canEditPage(DRAFT_KEY, USER);
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(null);
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    verify(descriptionService, never()).toPageLayout(any(), any(), anyBoolean(), any(), any());

    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    when(descriptionService.toPageLayout(sections, stored, true, USER, Locale.FRENCH)).thenThrow(new IllegalArgumentException(PageLayoutAssembler.COLUMNS_NOT_ACCEPTED));
    assertEquals(PageLayoutAssembler.COLUMNS_NOT_ACCEPTED, refusalInFrench(IllegalArgumentException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    verify(draftService, never()).createDraftPage(anyLong(), any());
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());

    doReturn(pageLayout()).when(descriptionService).toPageLayout(sections, stored, true, USER, Locale.FRENCH);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusalInFrench(IllegalAccessException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());

    doReturn(DRAFT_KEY).when(draftService).createDraftPage(LIVE_NODE, USER);
    when(pageLayoutService.updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER))).thenThrow(new IllegalArgumentException("internal detail"));
    assertEquals(LayoutMcpTool.STYLING_NOT_ACCEPTED, refusalInFrench(IllegalArgumentException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    doThrow(new IllegalStateException(LayoutMcpTool.PAGE_OUTDATED_MESSAGE)).when(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED, refusalInFrench(IllegalStateException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    doThrow(new IllegalStateException("engine detail")).when(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.PAGE_LAYOUT_NOT_SAVED, refusalInFrench(IllegalStateException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
    doThrow(new IllegalAccessException("internal detail")).when(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusalInFrench(IllegalAccessException.class, () -> tool.setPageLayout(PAGE_KEY.format(), sections)));
  }

  @Test
  public void setLayoutStylesOnAPageBuildsTheDraftFromTheLivePageOnTheFirstWrite() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null, DRAFT_KEY);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    List<ElementStyling> elements = List.of(new ElementStyling("12", null, null));
    when(descriptionService.toStyledPage(elements, stored, true, USER)).thenReturn(pageLayout());
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    PageDescription expected = describedDraft();

    assertEquals(expected, asUser(() -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));

    ArgumentCaptor<Page> saved = ArgumentCaptor.forClass(Page.class);
    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), saved.capture(), eq(false), eq(USER));
    assertEquals(EntityMapper.PAGE_LAYOUT_TEMPLATE, ((Container) saved.getValue().getChildren().get(0)).getTemplate());
    verify(draftService).createDraftPage(LIVE_NODE, USER);
  }

  @Test
  public void setLayoutStylesOnAPageWritesTheExistingDraftTheRevisionNames() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    Page stored = new Page("draft");
    when(pageLayoutService.getPageLayout(DRAFT_KEY)).thenReturn(stored);
    List<ElementStyling> elements = List.of(new ElementStyling("12", null, null));
    when(descriptionService.toStyledPage(elements, stored, false, USER)).thenReturn(pageLayout());
    PageDescription expected = describedDraft();

    assertEquals(expected, asUser(() -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));

    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(Page.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftPage(anyLong(), any());
  }

  @Test
  public void setLayoutStylesOnASiteBuildsTheDraftFromTheLiveLayoutOnTheFirstWrite() throws Exception {
    PortalConfig site = editableSite();
    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    List<ElementStyling> elements = List.of(new ElementStyling("12", null, null));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null, draftKey);
    when(descriptionService.toStyledSite(elements, site, true, USER)).thenReturn(siteLayout());
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(siteLayoutService.getSite(draftKey, USER)).thenReturn(draftSite);
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUser(() -> tool.setLayoutStyles(SITE_ID, null, elements)));

    verify(siteLayoutService).updateSiteLayout(eq(draftKey), any(PortalConfig.class), eq(false), eq(USER));
    verify(draftService).createDraftSite(site, USER);
  }

  @Test
  public void setLayoutStylesOnASiteWritesTheExistingDraftTheRevisionNames() throws Exception {
    PortalConfig site = editableSite();
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    List<ElementStyling> elements = List.of(new ElementStyling("12", null, null));
    when(descriptionService.toStyledSite(elements, draftSite, false, USER)).thenReturn(siteLayout());
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUser(() -> tool.setLayoutStyles(SITE_ID, null, elements)));

    verify(siteLayoutService).updateSiteLayout(eq(draftSite.getSiteKey()), any(PortalConfig.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftSite(any(), any());
  }

  @Test
  public void setLayoutStylesRefusalsCarryTheFixedTexts() throws Exception {
    List<ElementStyling> elements = List.of(new ElementStyling("12", null, null));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.setLayoutStyles(SITE_ID, PAGE_KEY.format(), elements)));
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> tool.setLayoutStyles(null, null, elements)));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.setLayoutStyles(null, " ", elements)));

    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(null);
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));
    verify(descriptionService, never()).toStyledPage(any(), any(), anyBoolean(), any());

    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    when(descriptionService.toStyledPage(elements, stored, true, USER)).thenThrow(new ObjectNotFoundException(LayoutStylesAssembler.ELEMENT_NOT_FOUND));
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));
    doThrow(new IllegalArgumentException("value is not accepted for styling.border")).when(descriptionService).toStyledPage(elements, stored, true, USER);
    assertEquals("value is not accepted for styling.border",
                 refusal(IllegalArgumentException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));
    verify(draftService, never()).createDraftPage(anyLong(), any());
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());

    doReturn(pageLayout()).when(descriptionService).toStyledPage(elements, stored, true, USER);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));
    doReturn(DRAFT_KEY).when(draftService).createDraftPage(LIVE_NODE, USER);
    doThrow(new IllegalStateException(LayoutMcpTool.PAGE_OUTDATED_MESSAGE)).when(pageLayoutService)
                                                                          .updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED, refusal(IllegalStateException.class, () -> tool.setLayoutStyles(null, PAGE_KEY.format(), elements)));

    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setLayoutStyles(1L, null, elements)));
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.setLayoutStyles(SITE_ID, null, elements)));
    verify(descriptionService, never()).toStyledSite(any(), any(), anyBoolean(), any());

    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    when(descriptionService.toStyledSite(elements, site, true, USER)).thenThrow(new IllegalArgumentException("value is not accepted for styling.border"));
    assertEquals("value is not accepted for styling.border", refusal(IllegalArgumentException.class, () -> tool.setLayoutStyles(SITE_ID, null, elements)));
    verify(draftService, never()).createDraftSite(any(), any());
    verify(siteLayoutService, never()).updateSiteLayout(any(), any(), anyBoolean(), any());

    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    doReturn(siteLayout()).when(descriptionService).toStyledSite(elements, site, true, USER);
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    doThrow(new IllegalArgumentException("Invalid css value input javascript:alert(1)")).when(siteLayoutService)
                                                                                         .updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.STYLING_NOT_ACCEPTED, refusal(IllegalArgumentException.class, () -> tool.setLayoutStyles(SITE_ID, null, elements)));
    doThrow(new IllegalStateException("engine detail")).when(siteLayoutService).updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.SITE_LAYOUT_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.setLayoutStyles(SITE_ID, null, elements)));
  }

  @Test
  public void setLayoutVisibilityOnAPageBuildsTheDraftFromTheLivePageOnTheFirstWrite() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null, DRAFT_KEY);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    List<ElementDisplay> elements = List.of(new ElementDisplay("12", DisplayMode.HIDE_MOBILE, null));
    when(descriptionService.toDisplayedPage(elements, stored, true, USER)).thenReturn(pageLayout());
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    PageDescription expected = describedDraft();

    assertEquals(expected, asUser(() -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));

    ArgumentCaptor<Page> saved = ArgumentCaptor.forClass(Page.class);
    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), saved.capture(), eq(false), eq(USER));
    assertEquals(EntityMapper.PAGE_LAYOUT_TEMPLATE, ((Container) saved.getValue().getChildren().get(0)).getTemplate());
    verify(draftService).createDraftPage(LIVE_NODE, USER);
  }

  @Test
  public void setLayoutVisibilityOnAPageWritesTheExistingDraftTheRevisionNames() throws Exception {
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    Page stored = new Page("draft");
    when(pageLayoutService.getPageLayout(DRAFT_KEY)).thenReturn(stored);
    List<ElementDisplay> elements = List.of(new ElementDisplay("12", DisplayMode.BOTH, null));
    when(descriptionService.toDisplayedPage(elements, stored, false, USER)).thenReturn(pageLayout());
    PageDescription expected = describedDraft();

    assertEquals(expected, asUser(() -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));

    verify(pageLayoutService).updatePageLayout(eq(DRAFT_KEY.format()), any(Page.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftPage(anyLong(), any());
  }

  @Test
  public void setLayoutVisibilityOnASiteBuildsTheDraftFromTheLiveLayoutOnTheFirstWrite() throws Exception {
    PortalConfig site = editableSite();
    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    List<ElementDisplay> elements = List.of(new ElementDisplay("12", DisplayMode.HIDE_MOBILE, null));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null, draftKey);
    when(descriptionService.toDisplayedSite(elements, site, true, USER)).thenReturn(siteLayout());
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(siteLayoutService.getSite(draftKey, USER)).thenReturn(draftSite);
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUser(() -> tool.setLayoutVisibility(SITE_ID, null, elements)));

    verify(siteLayoutService).updateSiteLayout(eq(draftKey), any(PortalConfig.class), eq(false), eq(USER));
    verify(draftService).createDraftSite(site, USER);
  }

  @Test
  public void setLayoutVisibilityOnASiteWritesTheExistingDraftTheRevisionNames() throws Exception {
    PortalConfig site = editableSite();
    PortalConfig draftSite = new PortalConfig(PortalConfig.DRAFT, "portal_test_draft_john", "draft_9");
    draftSite.setPortalLayout(new Container("draft"));
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(draftSite.getSiteKey());
    when(siteLayoutService.getSite(draftSite.getSiteKey(), USER)).thenReturn(draftSite);
    List<ElementDisplay> elements = List.of(new ElementDisplay("12", DisplayMode.BOTH, null));
    when(descriptionService.toDisplayedSite(elements, draftSite, false, USER)).thenReturn(siteLayout());
    SiteDescription expected = describedSite(site, draftSite);

    assertEquals(expected, asUser(() -> tool.setLayoutVisibility(SITE_ID, null, elements)));

    verify(siteLayoutService).updateSiteLayout(eq(draftSite.getSiteKey()), any(PortalConfig.class), eq(false), eq(USER));
    verify(draftService, never()).createDraftSite(any(), any());
  }

  @Test
  public void setLayoutVisibilityRefusalsCarryTheFixedTexts() throws Exception {
    List<ElementDisplay> elements = List.of(new ElementDisplay("12", DisplayMode.HIDE_DESKTOP, null));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.setLayoutVisibility(SITE_ID, PAGE_KEY.format(), elements)));
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> tool.setLayoutVisibility(null, null, elements)));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.setLayoutVisibility(null, " ", elements)));

    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(null);
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));
    verify(descriptionService, never()).toDisplayedPage(any(), any(), anyBoolean(), any());

    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    Page stored = new Page("live");
    when(pageLayoutService.getPageLayout(PAGE_KEY)).thenReturn(stored);
    when(descriptionService.toDisplayedPage(elements, stored, true, USER)).thenThrow(new ObjectNotFoundException(LayoutStylesAssembler.ELEMENT_NOT_FOUND));
    assertEquals(LayoutStylesAssembler.ELEMENT_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));
    doThrow(new IllegalArgumentException(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED)).when(descriptionService)
                                                                                     .toDisplayedPage(elements, stored, true, USER);
    assertEquals(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED,
                 refusal(IllegalArgumentException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));
    verify(draftService, never()).createDraftPage(anyLong(), any());
    verify(pageLayoutService, never()).updatePageLayout(any(), any(), anyBoolean(), any());

    doReturn(pageLayout()).when(descriptionService).toDisplayedPage(elements, stored, true, USER);
    when(draftService.createDraftPage(LIVE_NODE, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));
    doReturn(DRAFT_KEY).when(draftService).createDraftPage(LIVE_NODE, USER);
    doThrow(new IllegalStateException(LayoutMcpTool.PAGE_OUTDATED_MESSAGE)).when(pageLayoutService)
                                                                          .updatePageLayout(eq(DRAFT_KEY.format()), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED,
                 refusal(IllegalStateException.class, () -> tool.setLayoutVisibility(null, PAGE_KEY.format(), elements)));

    when(siteLayoutService.getSite(1L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    assertEquals(LayoutMcpTool.SITE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.setLayoutVisibility(1L, null, elements)));
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.SITE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.setLayoutVisibility(SITE_ID, null, elements)));
    verify(descriptionService, never()).toDisplayedSite(any(), any(), anyBoolean(), any());

    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    when(draftService.getDraftSiteKey(SITE_ID, USER)).thenReturn(null);
    when(descriptionService.toDisplayedSite(elements, site, true, USER)).thenThrow(new IllegalArgumentException(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED));
    assertEquals(LayoutStylesAssembler.DISPLAY_NOT_ACCEPTED,
                 refusal(IllegalArgumentException.class, () -> tool.setLayoutVisibility(SITE_ID, null, elements)));
    verify(draftService, never()).createDraftSite(any(), any());
    verify(siteLayoutService, never()).updateSiteLayout(any(), any(), anyBoolean(), any());

    SiteKey draftKey = SiteKey.draft("portal_test_draft_john");
    doReturn(siteLayout()).when(descriptionService).toDisplayedSite(elements, site, true, USER);
    when(draftService.createDraftSite(site, USER)).thenReturn(draftKey);
    doThrow(new IllegalStateException("engine detail")).when(siteLayoutService).updateSiteLayout(eq(draftKey), any(), eq(false), eq(USER));
    assertEquals(LayoutMcpTool.SITE_LAYOUT_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.setLayoutVisibility(SITE_ID, null, elements)));
  }

  @Test
  public void publishLayoutOnAPagePublishesTheRevisionsDraftAndAnswersThePublishedPage() throws Exception {
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.publishLayout(SITE_ID, PAGE_KEY.format())));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);
    ModelObject tree = new Container("published");
    PageDescription expected = mock(PageDescription.class);
    when(pageLayoutService.getPageLayout(PAGE_KEY, USER)).thenReturn(tree);
    when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.PUBLISHED)).thenReturn(expected);

    assertEquals(expected, asUser(() -> tool.publishLayout(null, PAGE_KEY.format())));

    verify(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    verify(draftService, never()).publishSite(any(), any());
    doThrow(new ObjectNotFoundException("no draft")).when(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    doThrow(new IllegalAccessException("internal detail")).when(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    doThrow(new IllegalArgumentException("internal detail")).when(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.STYLING_NOT_ACCEPTED, refusal(IllegalArgumentException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    doThrow(new IllegalStateException(LayoutMcpTool.PAGE_OUTDATED_MESSAGE)).when(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.LAYOUT_OUTDATED, refusal(IllegalStateException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    doThrow(new IllegalStateException("engine detail")).when(draftService).publishPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.PAGE_LAYOUT_NOT_PUBLISHED, refusal(IllegalStateException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.publishLayout(null, PAGE_KEY.format())));
    verify(draftService, times(6)).publishPage(PAGE_KEY, LIVE_NODE, USER);
  }

  @Test
  public void discardLayoutDraftOnAPageStartsAgainFromThePublishedPage() throws Exception {
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED, refusal(() -> tool.discardLayoutDraft(SITE_ID, PAGE_KEY.format())));
    editablePage();
    when(draftService.findLiveNodeId(PAGE_KEY, USER)).thenReturn(LIVE_NODE);
    when(draftService.discardDraftPage(PAGE_KEY, LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    PageDescription expected = describedDraft();

    assertEquals(expected, asUser(() -> tool.discardLayoutDraft(null, PAGE_KEY.format())));

    verify(draftService, never()).discardDraftSite(any(), any());
    doThrow(new ObjectNotFoundException("no draft")).when(draftService).discardDraftPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.DRAFT_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.discardLayoutDraft(null, PAGE_KEY.format())));
    doThrow(new IllegalAccessException("internal detail")).when(draftService).discardDraftPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.discardLayoutDraft(null, PAGE_KEY.format())));
    doThrow(new IllegalStateException("engine detail")).when(draftService).discardDraftPage(PAGE_KEY, LIVE_NODE, USER);
    assertEquals(LayoutMcpTool.PAGE_LAYOUT_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.discardLayoutDraft(null, PAGE_KEY.format())));
    verify(draftService, times(4)).discardDraftPage(PAGE_KEY, LIVE_NODE, USER);
  }

  private void editablePage() {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
  }

  /**
   * The page as a write answers it: the user's draft, read as get_page_layout
   * reads it
   */
  private PageDescription describedDraft() throws Exception {
    PageDescription expected = mock(PageDescription.class);
    ModelObject tree = new Container("draft");
    lenient().when(pageLayoutService.getPageLayout(DRAFT_KEY, USER)).thenReturn(tree);
    lenient().when(descriptionService.toPageDescription(tree, PAGE_KEY.format(), LayoutVersion.DRAFT)).thenReturn(expected);
    return expected;
  }

  private LayoutModel pageLayout() {
    LayoutModel parent = new LayoutModel();
    parent.setTemplate(EntityMapper.PAGE_LAYOUT_TEMPLATE);
    parent.setChildren(new ArrayList<>());
    LayoutModel page = new LayoutModel();
    page.setChildren(new ArrayList<>(List.of(parent)));
    return page;
  }

  private PortalConfig editableSite() throws Exception {
    PortalConfig site = site("portal", "test", "Test");
    site.setPortalLayout(new Container("live"));
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    when(aclService.canEditSite(SITE_KEY, USER)).thenReturn(true);
    return site;
  }

  /**
   * The site as a write answers it, with the given draft's layout
   */
  private SiteDescription describedSite(PortalConfig site, PortalConfig draftSite) throws Exception {
    SiteDescription expected = mock(SiteDescription.class);
    lenient().when(layoutService.getPortalConfig(site.getSiteKey())).thenReturn(site);
    lenient().doReturn(label(site.getLabel())).when(siteLayoutService).getSiteLabels(site.getId(), USER);
    lenient().doReturn(null).when(siteLayoutService).getSiteDescriptions(site.getId(), USER);
    lenient().when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    lenient().when(descriptionService.toSiteDescription(eq(site), eq(draftSite.getPortalLayout()), eq(site.getLabel()), isNull(), eq(true)))
             .thenReturn(expected);
    return expected;
  }

  private LayoutModel siteLayout() {
    LayoutModel root = new LayoutModel();
    root.setTemplate(SiteLayoutAssembler.SITE_TEMPLATE);
    root.setChildren(new ArrayList<>());
    return root;
  }

  @Test
  public void createPageCreatesEachKindWithTheDrawersDefaults() throws Exception {
    editableNavigation();
    languages();
    templates(pageTemplate(1, "blank", true), pageTemplate(2, "blank", false), pageTemplate(3, "default", false));
    NavigationItemDescription expected = mock(NavigationItemDescription.class);
    when(descriptionService.describeNavigationItem(SITE_KEY, NODE_ID, USER)).thenReturn(expected);
    NodeData createdourteam = node(NODE_ID, "ourteam");
    when(navigationLayoutService.createNodeWithPage(any(), any(), eq(USER))).thenReturn(createdourteam);
    ArgumentCaptor<NavigationCreateModel> nodes = ArgumentCaptor.forClass(NavigationCreateModel.class);
    ArgumentCaptor<PageCreateModel> pages = ArgumentCaptor.forClass(PageCreateModel.class);

    assertEquals(expected,
                 asUser(() -> tool.createPage(SITE_ID, null, "Our Team", null, null, "page", null, null, null, null, null, null, null, null, null)));
    verify(navigationLayoutService).createNodeWithPage(nodes.capture(), pages.capture(), eq(USER));
    NavigationCreateModel node = nodes.getValue();
    assertEquals(ROOT_NODE, node.getParentNodeId());
    assertNull(node.getPreviousNodeId());
    assertEquals("ourteam", node.getNodeId());
    assertEquals("Our Team", node.getNodeLabel());
    assertEquals(Map.of("en", "Our Team"), node.getLabels());
    assertTrue(node.isVisible());
    assertFalse(node.isScheduled());
    assertFalse(node.isDraft());
    assertFalse(node.isPasteMode());
    assertNull(node.getPageRef());
    assertEquals("SAME_TAB", node.getTarget());
    assertNull(node.getIcon());
    PageCreateModel page = pages.getValue();
    assertEquals("ourteam", page.getPageName());
    assertEquals("Our Team", page.getPageTitle());
    assertEquals("PAGE", page.getPageType());
    assertEquals(2L, page.getPageTemplateId());
    assertNull(page.getLink());
    assertNull(page.getAccessPermissions());
    assertNull(page.getEditPermission());

    permissions();
    Map<String, String> labelsWithANull = new HashMap<>();
    labelsWithANull.put("fr", "Équipe");
    labelsWithANull.put("en", "Team");
    labelsWithANull.put("de", null);
    asUser(() -> tool.createPage(SITE_ID,
                                 LIVE_NODE,
                                 "Équipe",
                                 labelsWithANull,
                                 "team",
                                 "LINK",
                                 null,
                                 null,
                                 "https://meeds.io/team?x=1#top",
                                 "new_tab",
                                 "fas fa-users",
                                 false,
                                 null,
                                 List.of("*:/platform/users"),
                                 "member:/spaces/marketing"));
    verify(navigationLayoutService, times(2)).createNodeWithPage(nodes.capture(), pages.capture(), eq(USER));
    node = nodes.getValue();
    assertEquals(LIVE_NODE, node.getParentNodeId());
    assertEquals("team", node.getNodeId());
    assertEquals(Map.of("fr", "Équipe", "en", "Équipe"), node.getLabels());
    assertFalse(node.isVisible());
    assertEquals("NEW_TAB", node.getTarget());
    assertEquals("fas fa-users", node.getIcon());
    page = pages.getValue();
    assertEquals("LINK", page.getPageType());
    assertEquals("https://meeds.io/team?x=1#top", page.getLink());
    assertNull(page.getPageTemplateId());
    assertArrayEquals(new String[] { "*:/platform/users" }, page.getAccessPermissions());
    assertEquals("member:/spaces/marketing", page.getEditPermission());

    page(PAGE_KEY, "PAGE", null);
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(true);
    asUser(() -> tool.createPage(SITE_ID,
                                 null,
                                 "Home again",
                                 null,
                                 null,
                                 "existing_page",
                                 null,
                                 PAGE_KEY.format(),
                                 null,
                                 null,
                                 null,
                                 true,
                                 new ScheduleDescription(FUTURE, LATER),
                                 null,
                                 null));
    verify(navigationLayoutService, times(3)).createNodeWithPage(nodes.capture(), pages.capture(), eq(USER));
    node = nodes.getValue();
    assertNull(pages.getValue());
    assertEquals(PAGE_KEY.format(), node.getPageRef());
    assertEquals("SAME_TAB", node.getTarget());
    assertEquals("homeagain", node.getNodeId());
    assertTrue(node.isScheduled());
    assertEquals(Instant.parse(FUTURE).toEpochMilli(), node.getStartScheduleDate());
    assertEquals(Instant.parse(LATER).toEpochMilli(), node.getEndScheduleDate());

    asUser(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, null, null, null, new ScheduleDescription(null, null), null, null));
    verify(navigationLayoutService, times(4)).createNodeWithPage(nodes.capture(), pages.capture(), eq(USER));
    assertNull(pages.getValue());
    assertNull(nodes.getValue().getPageRef());
    assertNull(nodes.getValue().getTarget());
    assertFalse(nodes.getValue().isScheduled());
    assertTrue(nodes.getValue().isVisible());
  }

  @Test
  public void createPageRefusesEachValueOutsideTheVocabularyBeforeAnyWrite() throws Exception {
    languages();
    permissions();
    templates(pageTemplate(1, "blank", true), pageTemplate(2, "default", false));
    PortalConfig site = site("portal", "test", "Test");
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site);
    assertEquals(LayoutMcpTool.SITE_ID_NOT_ACCEPTED, refusal(() -> createGroup(0L, "Docs", "docs")));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> createGroup(SITE_ID, "Docs", "docs")));

    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(true);
    when(navigationLayoutService.getRootNodeId(SITE_KEY)).thenReturn(ROOT_NODE);
    assertEquals(LayoutMcpTool.TYPE_NOT_ACCEPTED, refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, null, "folder", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.TYPE_NOT_ACCEPTED, refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, null, null, null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.LABEL_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, " ", "docs")));
    assertEquals(LayoutMcpTool.LABEL_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "x".repeat(201), "docs")));
    assertEquals(LayoutMcpTool.URL_NAME_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "Docs", "our docs")));
    assertEquals(LayoutMcpTool.URL_NAME_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "Docs", "docs_draft_john")));
    assertEquals(LayoutMcpTool.URL_NAME_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "Docs", "d".repeat(201))));
    assertEquals(LayoutMcpTool.URL_NAME_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "日本語", null)));
    assertEquals(LayoutMcpTool.LABELS_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", Map.of("de", "Dokumente"), "docs", "group", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.LABELS_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", Map.of("fr", " "), "docs", "group", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ICON_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, null, "<svg>", null, null, null, null)));
    for (ScheduleDescription schedule : List.of(new ScheduleDescription(FUTURE, null),
                                                new ScheduleDescription(null, LATER),
                                                new ScheduleDescription(LATER, FUTURE),
                                                new ScheduleDescription(FUTURE, FUTURE),
                                                new ScheduleDescription("2020-01-01T00:00:00Z", LATER),
                                                new ScheduleDescription("2030-01-01", LATER),
                                                new ScheduleDescription("2030-01-01T10:00:00", LATER))) {
      assertEquals(LayoutMcpTool.SCHEDULED_NOT_ACCEPTED,
                   refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, null, null, null, schedule, null, null)),
                   schedule.toString());
    }
    assertEquals(LayoutMcpTool.SCHEDULED_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, null, null, false, new ScheduleDescription(FUTURE, LATER), null, null)));
    for (String url : List.of("javascript:alert(1)", "//evil.example/x", "www.meeds.io", "ftp://meeds.io/x", "/\\\\evil", "https://", "https://meeds.io/a b", "mailto:x@y.z", "")) {
      assertEquals(LayoutMcpTool.URL_NOT_ACCEPTED,
                   refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "link", null, null, url, null, null, null, null, null, null)),
                   url);
    }
    assertEquals(LayoutMcpTool.TARGET_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "link", null, null, "https://meeds.io", "popup", null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_TEMPLATE_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_TEMPLATE_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", 1L, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_TEMPLATE_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", 9L, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", 2L, null, null, null, null, null, null, List.of("*:/nowhere"), null)));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, "not a ref", null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, DRAFT_KEY.format(), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, PAGE_KEY.format(), null, null, null, null, null, null, null)));
    when(layoutService.getPageContext(PAGE_KEY)).thenReturn(new PageContext(PAGE_KEY, null));
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, PAGE_KEY.format(), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.URL_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", 2L, null, "https://meeds.io", null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "link", null, PAGE_KEY.format(), "https://meeds.io", null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, PAGE_KEY.format(), null, null, null, null, null, List.of("Everyone"), null)));
    assertEquals(LayoutMcpTool.TARGET_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, "new_tab", null, null, null, null, null)));
    assertEquals(LayoutMcpTool.TARGET_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "page", 2L, null, null, "popup", null, null, null, null, null)));
    assertEquals(LayoutMcpTool.EDIT_PERMISSION_NOT_ACCEPTED,
                 refusal(() -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, "*:/platform/users")));

    assertEquals(LayoutMcpTool.PARENT_NODE_ID_NOT_ACCEPTED, refusal(() -> tool.createPage(SITE_ID, 0L, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, null)));
    when(navigationLayoutService.getEditableNode(20L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    NodeData node21L = node(21L, "home_draft_john");
    when(navigationLayoutService.getEditableNode(21L, USER)).thenReturn(node21L);
    NodeData elsewhere = node(22L, "home");
    when(elsewhere.getSiteKey()).thenReturn(SiteKey.portal("other"));
    when(navigationLayoutService.getEditableNode(22L, USER)).thenReturn(elsewhere);
    when(navigationLayoutService.getEditableNode(23L, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, 20L, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, 21L, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, 22L, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.createPage(SITE_ID, 23L, "Docs", null, "docs", "group", null, null, null, null, null, null, null, null, null)));
    when(navigationLayoutService.getRootNodeId(SITE_KEY)).thenReturn(null);
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    verify(navigationLayoutService, never()).createNodeWithPage(any(), any(), any());
  }

  @Test
  public void createPageAnswersTheFixedTextsOnServiceRefusals() throws Exception {
    editableNavigation();
    languages();
    when(navigationLayoutService.createNodeWithPage(any(), any(), eq(USER))).thenThrow(new ObjectNotFoundException(String.format(NavigationLayoutService.PARENT_NODE_NOT_FOUND_MESSAGE, ROOT_NODE)))
                                                                              .thenThrow(new IllegalAccessException("internal detail"))
                                                                              .thenThrow(new IllegalArgumentException(NavigationLayoutService.NODE_NAME_NOT_ACCEPTED))
                                                                              .thenThrow(new IllegalArgumentException(NavigationLayoutService.SCHEDULE_START_PAST))
                                                                              .thenThrow(new IllegalArgumentException("pageTemplate with designated Id is disabled"))
                                                                              .thenThrow(new IllegalStateException("db down"));
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    page(PAGE_KEY, "PAGE", null);
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(true);
    when(navigationLayoutService.createNodeWithPage(any(), any(), eq(USER))).thenThrow(new ObjectNotFoundException(String.format(NavigationLayoutService.PARENT_NODE_NOT_FOUND_MESSAGE, ROOT_NODE)))
                                                                              .thenThrow(new ObjectNotFoundException("Page with key x doesn't exist"))
                                                                              .thenThrow(new IllegalAccessException("internal detail"))
                                                                              .thenThrow(new IllegalArgumentException(NavigationLayoutService.NODE_NAME_NOT_ACCEPTED))
                                                                              .thenThrow(new IllegalArgumentException(NavigationLayoutService.SCHEDULE_INCOMPLETE))
                                                                              .thenThrow(new IllegalArgumentException("pageTemplate with designated Id is disabled"))
                                                                              .thenThrow(new IllegalStateException("db down"));
    assertEquals(LayoutMcpTool.PARENT_NODE_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, PAGE_KEY.format(), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND,
                 refusal(ObjectNotFoundException.class, () -> tool.createPage(SITE_ID, null, "Docs", null, "docs", "existing_page", null, PAGE_KEY.format(), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    assertEquals(LayoutMcpTool.URL_NAME_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "Docs", "docs")));
    assertEquals(LayoutMcpTool.SCHEDULED_NOT_ACCEPTED, refusal(() -> createGroup(SITE_ID, "Docs", "docs")));
    assertEquals(LayoutMcpTool.NODE_NOT_SAVED, refusal(IllegalStateException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    assertEquals(LayoutMcpTool.NODE_NOT_SAVED, refusal(IllegalStateException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    NodeData createddocs = node(NODE_ID, "docs");
    when(navigationLayoutService.createNodeWithPage(any(), any(), eq(USER))).thenReturn(createddocs);
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
    when(descriptionService.describeNavigationItem(SITE_KEY, NODE_ID, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> createGroup(SITE_ID, "Docs", "docs")));
  }

  @Test
  public void updatePageRefusesAChangedUrlNameAndAcceptsTheEcho() throws Exception {
    NodeData node = editableNode(NODE_ID, "team", null, Visibility.DISPLAYED);
    assertEquals(LayoutMcpTool.URL_NAME_NOT_CHANGEABLE,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, "our-team", null, null, null, null, null, null, null, null, null, null)));
    verify(navigationLayoutService, never()).updateNodeAndPagePermissions(anyLong(), any(), any());
    describedItem(node);
    asUser(() -> tool.updatePage(NODE_ID, null, null, " team ", null, null, null, null, null, "fa-users", null, null, null, null));
    verify(navigationLayoutService).updateNodeAndPagePermissions(eq(NODE_ID), any(), eq(USER));
  }

  @Test
  public void updatePageStatesOnlyTheGivenOptions() throws Exception {
    languages();
    NodeData node = editableNode(NODE_ID, "team", PAGE_KEY, Visibility.TEMPORAL);
    page(PAGE_KEY, "PAGE", null);
    NavigationItemDescription expected = describedItem(node);
    ArgumentCaptor<NavigationItemUpdate> updates = ArgumentCaptor.forClass(NavigationItemUpdate.class);

    assertEquals(expected, asUser(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    verify(navigationLayoutService).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    NavigationItemUpdate update = updates.getValue();
    assertEquals("fa-users", update.getIcon());
    assertNull(update.getLabel());
    assertNull(update.getLabels());
    assertNull(update.getVisible());
    assertNull(update.getSchedule());
    assertNull(update.getPageRef());
    assertFalse(update.isDetachPage());
    assertNull(update.getNewPage());
    assertNull(update.getLink());
    assertNull(update.getTarget());
    assertNull(update.getAccessPermissions());
    assertNull(update.getEditPermission());
    verify(aclService, never()).canEditPage(any(), any());

    Map<String, String> labelsWithANull = new HashMap<>();
    labelsWithANull.put("fr", "Équipe");
    labelsWithANull.put("en", null);
    asUser(() -> tool.updatePage(NODE_ID, "Team", labelsWithANull, null, null, null, null, null, "new_tab", null, true, new ScheduleDescription(FUTURE, LATER), null, null));
    verify(navigationLayoutService, times(2)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    update = updates.getValue();
    assertEquals("Team", update.getLabel());
    assertEquals(Map.of("fr", "Équipe"), update.getLabels());
    assertEquals("NEW_TAB", update.getTarget());
    assertTrue(update.getVisible());
    assertEquals(Instant.parse(FUTURE).toEpochMilli(), update.getSchedule().start());
    assertEquals(Instant.parse(LATER).toEpochMilli(), update.getSchedule().end());

    asUser(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, null, null, new ScheduleDescription(null, null), null, null));
    verify(navigationLayoutService, times(3)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertTrue(updates.getValue().getSchedule().isCleared());
    assertEquals(LayoutMcpTool.SCHEDULED_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, null, false, new ScheduleDescription(FUTURE, LATER), null, null)));

    permissions();
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(false);
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, null, null, null, List.of("Everyone"), null)));
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    asUser(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, null, null, null, List.of("Everyone"), "*:/platform/users"));
    verify(navigationLayoutService, times(4)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertEquals(List.of("Everyone"), updates.getValue().getAccessPermissions());
    assertEquals("*:/platform/users", updates.getValue().getEditPermission());
    assertNull(updates.getValue().getNewPage());
  }

  @Test
  public void updatePageChangesTheKindAsTheDrawersEditDoes() throws Exception {
    languages();
    permissions();
    templates(pageTemplate(2, "blank", false));
    NodeData node = editableNode(NODE_ID, "team", PAGE_KEY, Visibility.DISPLAYED);
    describedItem(node);
    page(PAGE_KEY, "PAGE", null);
    ArgumentCaptor<NavigationItemUpdate> updates = ArgumentCaptor.forClass(NavigationItemUpdate.class);

    assertEquals(LayoutMcpTool.URL_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, "https://meeds.io", null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_TEMPLATE_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, null, 2L, null, null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.URL_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "link", null, null, null, null, null, null, null, null, null)));

    asUser(() -> tool.updatePage(NODE_ID, null, null, null, "link", null, null, "/portal/public", "new_tab", null, null, null, List.of("Everyone"), null));
    verify(navigationLayoutService, times(1)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    NavigationItemUpdate update = updates.getValue();
    assertEquals("LINK", update.getNewPage().getPageType());
    assertEquals("/portal/public", update.getNewPage().getLink());
    assertEquals("team", update.getNewPage().getPageName());
    assertEquals("Label", update.getNewPage().getPageTitle());
    assertNull(update.getLink());
    assertEquals("NEW_TAB", update.getTarget());
    assertEquals(List.of("Everyone"), update.getAccessPermissions());
    verify(aclService, never()).canEditPage(any(), any());
    asUser(() -> tool.updatePage(NODE_ID, null, null, null, "link", null, null, "/portal/public", null, null, null, null, null, null));
    verify(navigationLayoutService, times(2)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertNull(updates.getValue().getTarget());

    asUser(() -> tool.updatePage(NODE_ID, "Team 2", null, null, "page", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(3)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertNull(updates.getValue().getNewPage());
    assertFalse(updates.getValue().isDetachPage());
    asUser(() -> tool.updatePage(NODE_ID, "Team 2", null, null, "page", 2L, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(4)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertEquals("PAGE", updates.getValue().getNewPage().getPageType());
    assertEquals(2L, updates.getValue().getNewPage().getPageTemplateId());
    assertEquals("Team 2", updates.getValue().getNewPage().getPageTitle());

    PageKey otherKey = PageKey.parse("portal::test::about");
    page(otherKey, "PAGE", null);
    when(aclService.canViewPage(otherKey, USER)).thenReturn(true);
    asUser(() -> tool.updatePage(NODE_ID, null, null, null, null, null, otherKey.format(), null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(5)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertEquals(otherKey.format(), updates.getValue().getPageRef());
    assertNull(updates.getValue().getNewPage());
    assertFalse(updates.getValue().isDetachPage());
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "existing_page", null, null, null, null, null, null, null, null, null)));
    asUser(() -> tool.updatePage(NODE_ID, null, null, null, "existing_page", null, otherKey.format(), null, "new_tab", null, null, null, null, null));
    verify(navigationLayoutService, times(6)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertEquals(otherKey.format(), updates.getValue().getPageRef());
    assertEquals("NEW_TAB", updates.getValue().getTarget());
    assertEquals(LayoutMcpTool.PAGE_REF_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "page", null, otherKey.format(), null, null, null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, null, null, otherKey.format(), null, null, null, null, null, List.of("Everyone"), null)));
    assertEquals(LayoutMcpTool.EDIT_PERMISSION_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "existing_page", null, otherKey.format(), null, null, null, null, null, null, "*:/platform/users")));

    asUser(() -> tool.updatePage(NODE_ID, null, null, null, "group", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(7)).updateNodeAndPagePermissions(eq(NODE_ID), updates.capture(), eq(USER));
    assertTrue(updates.getValue().isDetachPage());
    assertEquals(LayoutMcpTool.TARGET_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "group", null, null, null, "new_tab", null, null, null, null, null)));
    assertEquals(LayoutMcpTool.ACCESS_PERMISSIONS_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(NODE_ID, null, null, null, "group", null, null, null, null, null, null, null, List.of("Everyone"), null)));

    NodeData linkNode = editableNode(13L, "site", PAGE_KEY, Visibility.DISPLAYED);
    page(PAGE_KEY, "LINK", "https://old.example");
    describedItem(linkNode);
    when(aclService.canEditPage(PAGE_KEY, USER)).thenReturn(true);
    asUser(() -> tool.updatePage(13L, null, null, null, null, null, null, "https://meeds.io", null, null, null, null, null, null));
    verify(navigationLayoutService).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertEquals("https://meeds.io", updates.getValue().getLink());
    assertNull(updates.getValue().getNewPage());
    asUser(() -> tool.updatePage(13L, null, null, null, "link", null, null, null, "same_tab", null, null, null, null, null));
    verify(navigationLayoutService, times(2)).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertNull(updates.getValue().getLink());
    assertEquals("SAME_TAB", updates.getValue().getTarget());
    asUser(() -> tool.updatePage(13L, null, null, null, null, null, null, null, null, "fa-link", null, null, null, null));
    verify(navigationLayoutService, times(3)).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertNull(updates.getValue().getTarget());
    asUser(() -> tool.updatePage(13L, null, null, null, null, null, otherKey.format(), null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(4)).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertNull(updates.getValue().getTarget());
    asUser(() -> tool.updatePage(13L, null, null, null, "page", 2L, null, null, "new_tab", null, null, null, null, null));
    verify(navigationLayoutService, times(5)).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertEquals("NEW_TAB", updates.getValue().getTarget());
    assertEquals("PAGE", updates.getValue().getNewPage().getPageType());
    asUser(() -> tool.updatePage(13L, null, null, null, "page", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(6)).updateNodeAndPagePermissions(eq(13L), updates.capture(), eq(USER));
    assertEquals("PAGE", updates.getValue().getNewPage().getPageType());
    assertEquals(2L, updates.getValue().getNewPage().getPageTemplateId());

    NodeData dangling = editableNode(15L, "gone", PageKey.parse("portal::deleted::home"), Visibility.DISPLAYED);
    describedItem(dangling);
    asUser(() -> tool.updatePage(15L, null, null, null, null, null, null, null, "new_tab", "fa-link", null, null, null, null));
    verify(navigationLayoutService).updateNodeAndPagePermissions(eq(15L), updates.capture(), eq(USER));
    assertFalse(updates.getValue().isDetachPage());
    assertNull(updates.getValue().getNewPage());
    assertEquals("NEW_TAB", updates.getValue().getTarget());
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED,
                 refusal(IllegalAccessException.class, () -> tool.updatePage(15L, null, null, null, null, null, null, null, null, null, null, null, List.of("Everyone"), null)));
    asUser(() -> tool.updatePage(15L, null, null, null, "page", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(2)).updateNodeAndPagePermissions(eq(15L), updates.capture(), eq(USER));
    assertNull(updates.getValue().getNewPage());
    assertFalse(updates.getValue().isDetachPage());
    asUser(() -> tool.updatePage(15L, null, null, null, "group", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(3)).updateNodeAndPagePermissions(eq(15L), updates.capture(), eq(USER));
    assertTrue(updates.getValue().isDetachPage());
    asUser(() -> tool.updatePage(15L, null, null, null, null, null, otherKey.format(), null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(4)).updateNodeAndPagePermissions(eq(15L), updates.capture(), eq(USER));
    assertEquals(otherKey.format(), updates.getValue().getPageRef());

    NodeData groupNode = editableNode(14L, "docs", null, Visibility.DISPLAYED);
    describedItem(groupNode);
    asUser(() -> tool.updatePage(14L, null, null, null, "link", null, null, "https://meeds.io", null, null, null, null, null, null));
    verify(navigationLayoutService).updateNodeAndPagePermissions(eq(14L), updates.capture(), eq(USER));
    assertNull(updates.getValue().getTarget());
    asUser(() -> tool.updatePage(14L, null, null, null, "page", null, null, null, null, null, null, null, null, null));
    verify(navigationLayoutService, times(2)).updateNodeAndPagePermissions(eq(14L), updates.capture(), eq(USER));
    assertEquals("PAGE", updates.getValue().getNewPage().getPageType());
    assertEquals(2L, updates.getValue().getNewPage().getPageTemplateId());
    assertEquals(LayoutMcpTool.EDIT_PERMISSION_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(14L, null, null, null, null, null, null, null, null, null, null, null, null, "*:/platform/users")));
    assertEquals(LayoutMcpTool.URL_NOT_ACCEPTED,
                 refusal(() -> tool.updatePage(14L, null, null, null, null, null, null, "https://meeds.io", null, null, null, null, null, null)));

    when(navigationLayoutService.updateNodeAndPagePermissions(eq(NODE_ID), any(), eq(USER))).thenThrow(new ObjectNotFoundException(String.format(NavigationLayoutService.NODE_NOT_FOUND_MESSAGE, NODE_ID)))
                                                                                             .thenThrow(new ObjectNotFoundException("Page with key x doesn't exist"))
                                                                                             .thenThrow(new IllegalAccessException("internal detail"))
                                                                                             .thenThrow(new IllegalArgumentException(NavigationLayoutService.SCHEDULE_ON_HIDDEN_NODE))
                                                                                             .thenThrow(new IllegalStateException("db down"));
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    assertEquals(LayoutMcpTool.PAGE_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    assertEquals(LayoutMcpTool.SCHEDULED_NOT_ACCEPTED, refusal(() -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    assertEquals(LayoutMcpTool.NODE_NOT_SAVED, refusal(IllegalStateException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
    doReturn(null).when(navigationLayoutService).updateNodeAndPagePermissions(eq(NODE_ID), any(), eq(USER));
    when(descriptionService.describeNavigationItem(SITE_KEY, NODE_ID, USER)).thenThrow(new IllegalAccessException("internal detail"));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> tool.updatePage(NODE_ID, null, null, null, null, null, null, null, null, "fa-users", null, null, null, null)));
  }

  @Test
  public void deletePageRefusalsAndThePlainDelete() throws Exception {
    assertEquals(LayoutMcpTool.NODE_ID_NOT_ACCEPTED, refusal(() -> { tool.deletePage(null); return null; }));
    assertEquals(LayoutMcpTool.NODE_ID_NOT_ACCEPTED, refusal(() -> { tool.deletePage(-1L); return null; }));
    when(navigationLayoutService.getEditableNode(20L, USER)).thenThrow(new ObjectNotFoundException("internal detail"));
    when(navigationLayoutService.getEditableNode(21L, USER)).thenThrow(new IllegalAccessException("internal detail"));
    NodeData node22L = node(22L, "home_draft_john");
    when(navigationLayoutService.getEditableNode(22L, USER)).thenReturn(node22L);
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> { tool.deletePage(20L); return null; }));
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> { tool.deletePage(21L); return null; }));
    assertEquals(LayoutMcpTool.NODE_NOT_FOUND, refusal(ObjectNotFoundException.class, () -> { tool.deletePage(22L); return null; }));
    verify(navigationLayoutService, never()).deleteNode(anyLong(), anyLong(), any());

    NodeData nodeNODE_ID = node(NODE_ID, "team");
    when(navigationLayoutService.getEditableNode(NODE_ID, USER)).thenReturn(nodeNODE_ID);
    doThrow(new IllegalAccessException("system node")).doThrow(new IllegalStateException("db down"))
                                                       .doNothing()
                                                       .when(navigationLayoutService)
                                                       .deleteNode(NODE_ID, 0, USER);
    assertEquals(LayoutMcpTool.NAVIGATION_NOT_ALLOWED, refusal(IllegalAccessException.class, () -> { tool.deletePage(NODE_ID); return null; }));
    assertEquals(LayoutMcpTool.NODE_NOT_DELETED, refusal(IllegalStateException.class, () -> { tool.deletePage(NODE_ID); return null; }));
    asUser(() -> { tool.deletePage(NODE_ID); return null; });
    verify(navigationLayoutService, times(3)).deleteNode(NODE_ID, 0, USER);
  }

  private NavigationItemDescription createGroup(long siteId, String label, String urlName) throws Exception {
    return tool.createPage(siteId, null, label, null, urlName, "group", null, null, null, null, null, null, null, null, null);
  }

  /**
   * The site exists, the user edits its navigation, its root is ROOT_NODE
   * and LIVE_NODE is one of its items
   */
  private void editableNavigation() throws Exception {
    when(siteLayoutService.getSite(SITE_ID, USER)).thenReturn(site("portal", "test", "Test"));
    when(aclService.canEditNavigation(SITE_KEY, USER)).thenReturn(true);
    lenient().when(navigationLayoutService.getRootNodeId(SITE_KEY)).thenReturn(ROOT_NODE);
    NodeData liveNode = node(LIVE_NODE, "home");
    lenient().when(navigationLayoutService.getEditableNode(LIVE_NODE, USER)).thenReturn(liveNode);
  }

  private NodeData editableNode(long nodeId, String name, PageKey pageRef, Visibility visibility) throws Exception {
    NodeData node = node(nodeId, name);
    NodeState.Builder state = new NodeState.Builder().label("Label").pageRef(pageRef).visibility(visibility);
    if (pageRef != null) {
      state.target("NEW_TAB");
    }
    if (visibility == Visibility.TEMPORAL) {
      state.startPublicationTime(1000L).endPublicationTime(2000L);
    }
    lenient().when(node.getState()).thenReturn(state.build());
    when(navigationLayoutService.getEditableNode(nodeId, USER)).thenReturn(node);
    return node;
  }

  @SneakyThrows
  private NavigationItemDescription describedItem(NodeData node) {
    NavigationItemDescription expected = mock(NavigationItemDescription.class);
    lenient().when(descriptionService.describeNavigationItem(SITE_KEY, Long.parseLong(node.getId()), USER)).thenReturn(expected);
    return expected;
  }

  private void page(PageKey pageKey, String type, String link) {
    PageState state = new PageState(pageKey.getName(), null, false, false, false, null, null, List.of("Everyone"), "*:/platform/users", type, link);
    lenient().when(layoutService.getPageContext(pageKey)).thenReturn(new PageContext(pageKey, state));
  }

  private NodeData node(long id, String name) {
    NodeData node = mock(NodeData.class);
    lenient().when(node.getId()).thenReturn(String.valueOf(id));
    lenient().when(node.getName()).thenReturn(name);
    lenient().when(node.getSiteKey()).thenReturn(SITE_KEY);
    return node;
  }

  /**
   * The platform supports English, its default, and French
   */
  private void languages() {
    LocaleConfig english = mock(LocaleConfig.class);
    LocaleConfig french = mock(LocaleConfig.class);
    lenient().when(english.getLocale()).thenReturn(Locale.ENGLISH);
    lenient().when(french.getLocale()).thenReturn(Locale.FRENCH);
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(List.of(english, french));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(english);
  }

  private void templates(PageTemplate... templates) {
    lenient().when(pageTemplateService.getPageTemplates()).thenReturn(List.of(templates));
    for (PageTemplate template : templates) {
      lenient().when(pageTemplateService.getPageTemplate(template.getId())).thenReturn(template);
    }
  }

  private PageTemplate pageTemplate(long id, String category, boolean disabled) {
    return new PageTemplate(id, disabled, true, category, null);
  }

  private String refusalInFrench(Class<? extends Exception> type, Callable<?> call) {
    return assertThrows(type, () -> asUserInFrench(call)).getMessage();
  }

  @SneakyThrows
  private <T> T asUserInFrench(Callable<T> call) {
    try (MockedStatic<LocaleContextInfoUtils> locales = mockStatic(LocaleContextInfoUtils.class)) {
      locales.when(() -> LocaleContextInfoUtils.getUserLocale(USER)).thenReturn(Locale.FRENCH);
      return asUser(call);
    }
  }

  private String refusal(Callable<?> call) {
    return refusal(IllegalArgumentException.class, call);
  }

  private String refusal(Class<? extends Exception> type, Callable<?> call) {
    return assertThrows(type, () -> asUser(call)).getMessage();
  }

  /**
   * The site as a write answers it: read from the stored site, never through
   * the view check
   */
  private SiteDescription describedSite(PortalConfig site) throws Exception {
    SiteDescription expected = mock(SiteDescription.class);
    lenient().when(layoutService.getPortalConfig(site.getSiteKey())).thenReturn(site);
    lenient().doReturn(label(site.getLabel())).when(siteLayoutService).getSiteLabels(site.getId(), USER);
    lenient().doReturn(null).when(siteLayoutService).getSiteDescriptions(site.getId(), USER);
    lenient().when(draftService.getDraftSiteKey(site.getId(), USER)).thenReturn(null);
    lenient().when(descriptionService.filterAccessible(any(), eq(USER))).thenAnswer(i -> i.getArgument(0));
    lenient().when(descriptionService.toSiteDescription(eq(site), any(), eq(site.getLabel()), isNull(), eq(false))).thenReturn(expected);
    return expected;
  }

  private void templates(SiteTemplate... templates) {
    lenient().when(siteTemplateService.getSiteTemplates(null)).thenReturn(List.of(templates));
  }

  private SiteTemplate template(long id, String layout, boolean disabled) {
    SiteTemplate template = new SiteTemplate();
    template.setId(id);
    template.setLayout(layout);
    template.setDisabled(disabled);
    return template;
  }

  /**
   * The groups /platform/users and /spaces/marketing exist, the membership
   * types member and manager exist, nothing else does
   */
  private void permissions() throws Exception {
    GroupHandler groupHandler = mock(GroupHandler.class);
    MembershipTypeHandler membershipTypeHandler = mock(MembershipTypeHandler.class);
    lenient().when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    lenient().when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    lenient().when(groupHandler.findGroupById("/platform/users")).thenReturn(mock(Group.class));
    lenient().when(groupHandler.findGroupById("/spaces/marketing")).thenReturn(mock(Group.class));
    lenient().when(membershipTypeHandler.findMembershipType("member")).thenReturn(mock(MembershipType.class));
    lenient().when(membershipTypeHandler.findMembershipType("manager")).thenReturn(mock(MembershipType.class));
  }

  private void upload(String uploadId) {
    lenient().when(uploadService.getUploadResource(uploadId)).thenReturn(mock(UploadResource.class));
  }

  private void viewablePage() {
    when(layoutService.getPage(PAGE_KEY)).thenReturn(new Page());
    when(aclService.canViewPage(PAGE_KEY, USER)).thenReturn(true);
    when(aclService.canViewSite(SITE_KEY, USER)).thenReturn(true);
  }

  @SneakyThrows
  private <T> T asUser(Callable<T> call) {
    try (MockedStatic<ConversationState> conversation = mockStatic(ConversationState.class)) {
      conversation.when(ConversationState::getCurrent).thenReturn(conversationState);
      return call.call();
    }
  }

  private PortalConfig site(String type, String name, String label) {
    PortalConfig site = new PortalConfig(type, name, type + "_" + SITE_ID);
    site.setLabel(label);
    site.setDisplayed(true);
    return site;
  }

  private NodeLabel label(String english) {
    NodeLabel nodeLabel = new NodeLabel();
    nodeLabel.setDefaultLanguage("en");
    nodeLabel.setLabels(Map.of("en", english));
    return nodeLabel;
  }
}
