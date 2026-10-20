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
package io.meeds.layout.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;

import io.meeds.layout.mcp.constant.SidebarDisplayMode;
import io.meeds.layout.mcp.model.NavigationDisplayDescription;
import io.meeds.layout.mcp.model.SidebarDisplayOptions;
import io.meeds.layout.mcp.model.TopbarApplicationOption;
import io.meeds.layout.mcp.model.TopbarDisplayOptions;
import io.meeds.portal.navigation.constant.SidebarMode;
import io.meeds.portal.navigation.model.NavigationConfiguration;
import io.meeds.portal.navigation.model.SidebarConfiguration;
import io.meeds.portal.navigation.model.TopbarApplication;
import io.meeds.portal.navigation.model.TopbarConfiguration;
import io.meeds.portal.navigation.service.NavigationConfigurationService;

/**
 * Updates what the topbar and the sidebar show the way the navigation
 * settings page does: the stored configuration is loaded unresolved, the
 * given options are merged into it, and the whole is submitted with both
 * parts, since the navigation configuration service stores whatever it is
 * given and reads the sidebar after the write. The sidebar's items and the
 * users' own modes are never touched. A topbar application's stored name
 * is the key the settings page translates: the label shown in the user's
 * language is what the tool returns and matches a given name against.
 */
@Service
public class NavigationDisplayService {

  public static final String             ADMINISTRATORS_ONLY          = "not allowed: administrators only";

  public static final String             CONFIGURATION_NOT_FOUND      = "does not exist: navigation configuration";

  public static final String             APPLICATION_NOT_FOUND        = "does not exist: application";

  public static final String             DISPLAY_OPTIONS_NOT_ACCEPTED = "value is not accepted for topbar and sidebar";

  public static final String             APPLICATIONS_NOT_ACCEPTED    = "value is not accepted for applications";

  public static final String             ALLOWED_MODES_NOT_ACCEPTED   = "value is not accepted for allowed_modes";

  public static final String             DEFAULT_MODE_NOT_ACCEPTED    = "value is not accepted for default_mode";

  private static final Log               LOG                          = ExoLogger.getLogger(NavigationDisplayService.class);

  private static final String[]          SETTINGS_BUNDLES             = { "locale.portlet.GeneralSettings", "locale.portlet.Portlets" };

  private static final String            ALLOWED_MODES_OPTION         = "allowed_modes";

  private static final String            DEFAULT_MODE_OPTION          = "default_mode";

  @Autowired
  private NavigationConfigurationService navigationConfigurationService;

  @Autowired
  private ResourceBundleService          resourceBundleService;

  @Autowired
  private LayoutAclService               aclService;

  /**
   * @param  topbar                  what the topbar shows, null to leave it
   * @param  sidebar                 how the sidebar may be displayed, null to
   *                                   leave it
   * @param  username                the acting user
   * @param  locale                  the user's locale, in which application
   *                                   names are matched and returned
   * @return                         both parts as stored after the update
   * @throws ObjectNotFoundException  when no configuration is stored, or when
   *                                   an application named is not in the
   *                                   topbar
   * @throws IllegalAccessException   when the user is not an administrator
   * @throws IllegalArgumentException when both parts are omitted, when an
   *                                   application entry names nothing or a
   *                                   name shared by two applications, when
   *                                   a mode is outside the vocabulary, or
   *                                   when the default mode would not be
   *                                   allowed
   */
  public NavigationDisplayDescription updateNavigationDisplay(TopbarDisplayOptions topbar,
                                                              SidebarDisplayOptions sidebar,
                                                              String username,
                                                              Locale locale) throws ObjectNotFoundException,
                                                                             IllegalAccessException {
    if (!aclService.isAdministrator(username)) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    } else if (topbar == null && sidebar == null) {
      throw new IllegalArgumentException(DISPLAY_OPTIONS_NOT_ACCEPTED);
    }
    NavigationConfiguration configuration = navigationConfigurationService.getConfiguration();
    if (configuration == null || configuration.getTopbar() == null || configuration.getSidebar() == null) {
      throw new ObjectNotFoundException(CONFIGURATION_NOT_FOUND);
    }
    mergeTopbar(configuration.getTopbar(), topbar, locale);
    mergeSidebar(configuration.getSidebar(), sidebar);
    navigationConfigurationService.updateConfiguration(configuration);
    return describe(navigationConfigurationService.getConfiguration(), locale);
  }

  /**
   * @param  configuration a stored configuration
   * @param  locale        the locale of the application names
   * @return               its display options, null for null
   */
  public NavigationDisplayDescription describe(NavigationConfiguration configuration, Locale locale) {
    if (configuration == null) {
      return null;
    }
    TopbarConfiguration topbar = configuration.getTopbar();
    SidebarConfiguration sidebar = configuration.getSidebar();
    ResourceBundle bundle = settingsBundle(locale);
    List<TopbarApplicationOption> applications = applications(topbar).stream()
                                                                     .map(app -> new TopbarApplicationOption(app.getId(),
                                                                                                             label(app, bundle),
                                                                                                             app.isEnabled(),
                                                                                                             app.isMobile()))
                                                                     .toList();
    List<String> allowedModes = sidebar.getAllowedModes() == null ? null :
                                                                  sidebar.getAllowedModes()
                                                                         .stream()
                                                                         .map(SidebarDisplayMode::valueOf)
                                                                         .toList();
    return new NavigationDisplayDescription(new TopbarDisplayOptions(topbar.isDisplayCompanyName(),
                                                                     topbar.isDisplaySiteName(),
                                                                     topbar.isDisplayMobileCompanyLogo(),
                                                                     applications),
                                            new SidebarDisplayOptions(sidebar.isAllowUserCustomHome(),
                                                                      allowedModes,
                                                                      SidebarDisplayMode.valueOf(sidebar.getDefaultMode())));
  }

  private void mergeTopbar(TopbarConfiguration stored, TopbarDisplayOptions given, Locale locale) throws ObjectNotFoundException {
    if (given == null) {
      return;
    }
    if (given.displayCompanyName() != null) {
      stored.setDisplayCompanyName(given.displayCompanyName());
    }
    if (given.displaySiteName() != null) {
      stored.setDisplaySiteName(given.displaySiteName());
    }
    if (given.displayMobileCompanyLogo() != null) {
      stored.setDisplayMobileCompanyLogo(given.displayMobileCompanyLogo());
    }
    if (given.applications() != null) {
      stored.setApplications(mergeApplications(applications(stored), given.applications(), locale));
    }
  }

  /**
   * The stored applications are copied before any change, since the
   * configuration service hands out the cached objects themselves: a refusal
   * must leave the cache as it was. An entry names its application by
   * identifier, or by name when it gives no identifier, since the identifiers
   * are only known from this tool's own result.
   */
  private List<TopbarApplication> mergeApplications(List<TopbarApplication> stored,
                                                    List<TopbarApplicationOption> given,
                                                    Locale locale) throws ObjectNotFoundException {
    List<TopbarApplication> copies = stored.stream().map(this::copy).toList();
    ResourceBundle bundle = settingsBundle(locale);
    for (TopbarApplicationOption option : given) {
      TopbarApplication application = findApplication(copies, option, bundle);
      if (option.enabled() != null) {
        application.setEnabled(option.enabled());
      }
      if (option.mobile() != null) {
        application.setMobile(option.mobile());
      }
    }
    return copies;
  }

  /**
   * A name matches the label the settings page shows in the user's
   * language, or the stored key itself, case-insensitive
   */
  private TopbarApplication findApplication(List<TopbarApplication> applications,
                                            TopbarApplicationOption option,
                                            ResourceBundle bundle) throws ObjectNotFoundException {
    if (option == null || (StringUtils.isBlank(option.id()) && StringUtils.isBlank(option.name()))) {
      throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
    }
    List<TopbarApplication> matches;
    if (StringUtils.isNotBlank(option.id())) {
      matches = applications.stream().filter(app -> StringUtils.equals(app.getId(), option.id().trim())).toList();
    } else {
      String name = option.name().trim();
      matches = applications.stream()
                            .filter(app -> StringUtils.equalsIgnoreCase(label(app, bundle), name)
                                           || StringUtils.equalsIgnoreCase(app.getName(), name))
                            .toList();
    }
    if (matches.isEmpty()) {
      throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
    } else if (matches.size() > 1) {
      throw new IllegalArgumentException(APPLICATIONS_NOT_ACCEPTED);
    }
    return matches.get(0);
  }

  private void mergeSidebar(SidebarConfiguration stored, SidebarDisplayOptions given) {
    if (given == null) {
      return;
    }
    if (given.allowUserCustomHome() != null) {
      stored.setAllowUserCustomHome(given.allowUserCustomHome());
    }
    if (given.allowedModes() != null) {
      if (given.allowedModes().isEmpty()) {
        throw new IllegalArgumentException(ALLOWED_MODES_NOT_ACCEPTED);
      }
      stored.setAllowedModes(given.allowedModes()
                                  .stream()
                                  .map(value -> SidebarDisplayMode.parse(value, ALLOWED_MODES_OPTION).getMode())
                                  .distinct()
                                  .toList());
    }
    if (given.defaultMode() != null) {
      stored.setDefaultMode(SidebarDisplayMode.parse(given.defaultMode(), DEFAULT_MODE_OPTION).getMode());
    }
    SidebarMode defaultMode = stored.getDefaultMode();
    List<SidebarMode> allowedModes = stored.getAllowedModes();
    if (defaultMode != null && allowedModes != null && !allowedModes.contains(defaultMode)) {
      throw new IllegalArgumentException(given.defaultMode() != null ? DEFAULT_MODE_NOT_ACCEPTED : ALLOWED_MODES_NOT_ACCEPTED);
    }
  }

  private ResourceBundle settingsBundle(Locale locale) {
    try {
      return resourceBundleService.getResourceBundle(SETTINGS_BUNDLES, locale == null ? Locale.ENGLISH : locale);
    } catch (Exception e) {
      LOG.debug("Settings bundles not readable for locale {}, application names kept as stored", locale, e);
      return null;
    }
  }

  private String label(TopbarApplication application, ResourceBundle bundle) {
    String name = application.getName();
    return bundle != null && name != null && bundle.containsKey(name) ? bundle.getString(name) : name;
  }

  private List<TopbarApplication> applications(TopbarConfiguration topbar) {
    return topbar.getApplications() == null ? new ArrayList<>() : topbar.getApplications();
  }

  private TopbarApplication copy(TopbarApplication application) {
    return new TopbarApplication(application.getId(),
                                 application.getName(),
                                 application.getDescription(),
                                 application.getIcon(),
                                 application.getImageUrl(),
                                 application.getType(),
                                 application.isEnabled(),
                                 application.isMobile(),
                                 application.getProperties());
  }
}
