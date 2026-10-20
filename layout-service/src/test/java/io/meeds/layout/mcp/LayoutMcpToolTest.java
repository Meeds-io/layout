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
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.mcp.constant.LayoutVersion;
import io.meeds.layout.mcp.model.LayoutApplicationEntry;
import io.meeds.layout.mcp.model.NavigationItemDescription;
import io.meeds.layout.mcp.model.PageDescription;
import io.meeds.layout.mcp.model.SiteDescription;
import io.meeds.layout.mcp.model.SiteSummary;
import io.meeds.layout.model.NodeLabel;
import io.meeds.layout.model.PermissionUpdateModel;
import io.meeds.layout.model.SiteCreateModel;
import io.meeds.layout.model.SitePropertiesUpdate;
import io.meeds.layout.model.SiteTemplate;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.LayoutDescriptionService;
import io.meeds.layout.service.LayoutDraftService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;
import io.meeds.layout.service.SiteTemplateService;

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

  @Autowired
  private LayoutMcpTool            tool;

  private ConversationState        conversationState;

  @BeforeEach
  public void setUp() {
    Identity identity = mock(Identity.class);
    lenient().when(identity.getUserId()).thenReturn(USER);
    conversationState = mock(ConversationState.class);
    lenient().when(conversationState.getIdentity()).thenReturn(identity);
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
    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
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

    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(DRAFT_KEY);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), "published")));

    when(draftService.getDraftPageKey(LIVE_NODE, USER)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));

    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(null);
    assertEquals(expected, asUser(() -> tool.getPageLayout(PAGE_KEY.format(), null)));
    verify(pageLayoutService, never()).getPageLayout(DRAFT_KEY, USER);
  }

  @Test
  public void explicitDraftWithoutARevisionDoesNotExist() throws Exception {
    viewablePage();
    when(draftService.findLiveNodeId(PAGE_KEY)).thenReturn(LIVE_NODE);
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
