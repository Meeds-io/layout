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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Branding;

import io.meeds.layout.mcp.constant.ThemePart;
import io.meeds.layout.util.ThemeOptions;

/**
 * Updates one part of the theme the way the Branding page's Apply button
 * does: the whole branding is loaded, the part's options and resets are
 * merged into it, and the whole is submitted, so that every field the caller
 * did not name keeps its stored value although the branding service replaces
 * the whole. An untouched theme variable stored at its configured default is
 * submitted absent, so that it keeps following that default. The branding
 * service validates every value before its first write and refuses with its
 * own message code.
 */
@Service
public class ThemeUpdateService {

  public static final String ADMINISTRATORS_ONLY  = "not allowed: administrators only";

  public static final String OPTIONS_NOT_ACCEPTED = "value is not accepted for options";

  public static final String RESET_NOT_ACCEPTED   = "value is not accepted for reset";

  @Autowired
  private BrandingService    brandingService;

  @Autowired
  private LayoutAclService   aclService;

  /**
   * @param  part                   the part of the theme
   * @param  options                the options to set, by name; a null value
   *                                  is ignored
   * @param  reset                  the options returned to their default
   * @param  username               the acting user
   * @return                        the part's options as stored after the
   *                                update
   * @throws IllegalAccessException   when the user is not an administrator
   * @throws IllegalArgumentException when an option is not one of the part,
   *                                    when a value is blank, when nothing is
   *                                    asked, or with the branding service's
   *                                    message code when a value is refused
   */
  public Map<String, String> updateTheme(ThemePart part,
                                         Map<String, String> options,
                                         List<String> reset,
                                         String username) throws IllegalAccessException {
    if (!aclService.isAdministrator(username)) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    }
    Map<String, String> defaults = brandingService.getDefaultThemeStyle();
    Set<String> accepted = acceptedOptions(part, defaults.keySet());
    Map<String, String> changes = acceptedChanges(options, accepted);
    Set<String> resets = acceptedResets(reset, accepted);
    if (changes.isEmpty() && resets.isEmpty()) {
      throw new IllegalArgumentException(OPTIONS_NOT_ACCEPTED);
    }
    Branding branding = brandingService.getBrandingInformation(false);
    Map<String, String> themeStyle = new HashMap<>(branding.getThemeStyle());
    themeStyle.entrySet()
              .removeIf(entry -> !changes.containsKey(entry.getKey())
                                 && Objects.equals(entry.getValue(), defaults.get(entry.getKey())));
    ThemeOptions.PAGE_FIELDS.stream()
                            .filter(field -> !changes.containsKey(field)
                                             && Objects.equals(getBrandingField(branding, field), defaults.get(field)))
                            .forEach(field -> setBrandingField(branding, field, null));
    changes.forEach((option, value) -> {
      if (part.isBrandingField(option)) {
        setBrandingField(branding, option, value);
      } else {
        themeStyle.put(option, value);
      }
    });
    resets.forEach(option -> {
      if (part.isBrandingField(option)) {
        setBrandingField(branding, option, null);
      } else {
        themeStyle.remove(option);
      }
    });
    branding.setThemeStyle(themeStyle);
    brandingService.updateBrandingInformation(branding);
    return describe(part, accepted);
  }

  /**
   * @param  part     the part of the theme
   * @param  accepted the part's accepted options
   * @return          the part's options holding a value, stored or default
   */
  public Map<String, String> describe(ThemePart part, Set<String> accepted) {
    Branding branding = brandingService.getBrandingInformation(false);
    Map<String, String> values = new TreeMap<>();
    accepted.forEach(option -> {
      String value = part.isBrandingField(option) ? getBrandingField(branding, option) : branding.getThemeStyle().get(option);
      if (StringUtils.isNotBlank(value)) {
        values.put(option, value);
      }
    });
    return values;
  }

  /**
   * @param  part     the part of the theme
   * @param  declared the theme variables the platform declares
   * @return          the part's drawer options the platform declares, and the
   *                  part's branding fields
   */
  public Set<String> acceptedOptions(ThemePart part, Set<String> declared) {
    return Stream.concat(part.getThemeOptions().stream().filter(declared::contains), part.getBrandingFields().stream())
                 .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<String, String> acceptedChanges(Map<String, String> options, Set<String> accepted) {
    Map<String, String> changes = new LinkedHashMap<>();
    if (options != null) {
      options.forEach((option, value) -> {
        if (value != null) {
          if (!accepted.contains(option) || StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(OPTIONS_NOT_ACCEPTED + ": " + option);
          }
          changes.put(option, value);
        }
      });
    }
    return changes;
  }

  private Set<String> acceptedResets(List<String> reset, Set<String> accepted) {
    Set<String> resets = new LinkedHashSet<>();
    if (reset != null) {
      reset.forEach(option -> {
        if (!accepted.contains(option)) {
          throw new IllegalArgumentException(RESET_NOT_ACCEPTED + ": " + option);
        }
        resets.add(option);
      });
    }
    return resets;
  }

  private String getBrandingField(Branding branding, String field) {
    return switch (field) {
      case ThemeOptions.PAGE_WIDTH -> branding.getPageWidth();
      case ThemeOptions.PAGE_BACKGROUND_COLOR -> branding.getPageBackgroundColor();
      case ThemeOptions.PAGE_BACKGROUND_EFFECT -> branding.getPageBackgroundEffect();
      case ThemeOptions.PAGE_BACKGROUND_SIZE -> branding.getPageBackgroundSize();
      case ThemeOptions.PAGE_BACKGROUND_REPEAT -> branding.getPageBackgroundRepeat();
      case ThemeOptions.PAGE_BACKGROUND_POSITION -> branding.getPageBackgroundPosition();
      case ThemeOptions.PAGE_BACKGROUND_ATTACHMENT -> branding.getPageBackgroundAttachment();
      default -> throw new IllegalArgumentException(OPTIONS_NOT_ACCEPTED + ": " + field);
    };
  }

  private void setBrandingField(Branding branding, String field, String value) {
    switch (field) {
      case ThemeOptions.PAGE_WIDTH -> branding.setPageWidth(value);
      case ThemeOptions.PAGE_BACKGROUND_COLOR -> branding.setPageBackgroundColor(value);
      case ThemeOptions.PAGE_BACKGROUND_EFFECT -> branding.setPageBackgroundEffect(value);
      case ThemeOptions.PAGE_BACKGROUND_SIZE -> branding.setPageBackgroundSize(value);
      case ThemeOptions.PAGE_BACKGROUND_REPEAT -> branding.setPageBackgroundRepeat(value);
      case ThemeOptions.PAGE_BACKGROUND_POSITION -> branding.setPageBackgroundPosition(value);
      case ThemeOptions.PAGE_BACKGROUND_ATTACHMENT -> branding.setPageBackgroundAttachment(value);
      default -> throw new IllegalArgumentException(OPTIONS_NOT_ACCEPTED + ": " + field);
    }
  }
}
