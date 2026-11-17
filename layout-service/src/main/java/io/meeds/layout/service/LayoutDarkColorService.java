/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.branding.BrandingService;

import io.meeds.layout.model.DarkColorBlock;
import io.meeds.layout.model.ExpandContext;
import io.meeds.layout.model.InheritedStyle;
import io.meeds.layout.model.LayoutModel;
import io.meeds.layout.util.EntityMapper;
import io.meeds.layout.util.LayoutDarkColors;
import io.meeds.layout.util.LayoutStyleValidator;
import io.meeds.portal.theme.constant.BackgroundKind;

/**
 * Calculates, for the layout REST, what is never stored: the dark value of the
 * colours set in the page editor and the light-only flag of an application.
 */
@Service
public class LayoutDarkColorService {

  public static final int     MAX_BLOCKS   = 100;

  public static final String  BLOCKS_MANDATORY = "layout.colors.blocksMandatory";

  public static final String  TOO_MANY_BLOCKS  = "layout.colors.tooManyBlocks";

  private static final String BORDER_COLOR = "borderColor";

  private static final Set<String> PAGE_TEMPLATES = Set.of(EntityMapper.PAGE_LAYOUT_TEMPLATE,
                                                           EntityMapper.PAGE_BODY_TEMPLATE,
                                                           "SiteMiddleCenterBody");

  @Autowired
  private BrandingService brandingService;

  /**
   * Adds to every container of the layout its darkColors and flags
   * lightOnly the applications of a portlet that the platform renders in
   * light whatever the user's mode.
   *
   * @param layout the layout returned by the REST
   * @param contentIds the portlet id ("webapp/portlet") of the applications, by
   *          storage id, for those whose model does not carry its contentId
   * @param lightOnlyEnabled whether {@link #hasLightOnlyApplications()} was
   *          true: no application is checked otherwise
   */
  public void expand(LayoutModel layout, Map<String, String> contentIds, boolean lightOnlyEnabled) {
    if (layout != null) {
      expand(layout,
             new ExpandContext(contentIds == null ? Map.of() : contentIds, lightOnlyEnabled),
             new InheritedStyle(Map.of(), null, false, Map.of()),
             true);
    }
  }

  /**
   * @return true when at least one portlet is rendered in light whatever the
   *         user's display mode: read once per layout, to skip the resolution
   *         of the applications' portlet otherwise
   */
  public boolean hasLightOnlyApplications() {
    return !CollectionUtils.isEmpty(brandingService.getLightOnlyApplications());
  }

  /**
   * @param blocks the blocks whose stored colours are previewed
   * @return their dark values by field, in the order of the blocks
   * @throws IllegalArgumentException when more than {@link #MAX_BLOCKS} blocks
   *           are given or a colour holds a character that a stored colour
   *           cannot
   */
  public List<Map<String, String>> computeDarkColors(List<DarkColorBlock> blocks) {
    if (blocks == null) {
      throw new IllegalArgumentException(BLOCKS_MANDATORY);
    }
    if (blocks.size() > MAX_BLOCKS) {
      throw new IllegalArgumentException(TOO_MANY_BLOCKS);
    }
    List<Map<String, String>> result = new ArrayList<>(blocks.size());
    for (DarkColorBlock block : blocks) {
      result.add(computeDarkColors(block));
    }
    return result;
  }

  private Map<String, String> computeDarkColors(DarkColorBlock block) {
    if (block == null || block.getColors() == null) {
      return new LinkedHashMap<>();
    }
    Map<String, String> colors = new HashMap<>();
    block.getColors().forEach((field, value) -> {
      if (LayoutDarkColors.BACKGROUND_FIELDS.contains(field) || LayoutDarkColors.FOREGROUND_FIELDS.contains(field)) {
        LayoutStyleValidator.validateColor(value);
        colors.put(field, value);
      }
    });
    LayoutStyleValidator.validateColor(block.getBackground());
    String background = StringUtils.isBlank(block.getBackground()) ? colors.get(LayoutDarkColors.BACKGROUND_COLOR) :
                                                                   block.getBackground();
    return LayoutDarkColors.compute(colors,
                                    background,
                                    block.isImage() && StringUtils.isBlank(colors.get(LayoutDarkColors.BACKGROUND_COLOR)),
                                    block.isPage() ? BackgroundKind.PAGE : BackgroundKind.APP);
  }

  private void expand(LayoutModel model, ExpandContext context, InheritedStyle inherited, boolean root) {
    if (isApplication(model)) {
      if (context.lightOnlyEnabled()) {
        String contentId = context.contentIds().getOrDefault(model.getStorageId(), model.getContentId());
        if (contentId != null && brandingService.isLightOnlyApplication(contentId)) {
          model.setLightOnly(true);
          setLightColors(model, inherited);
          return;
        }
      }
      setDarkColors(model, inherited, false);
      return;
    }
    setDarkColors(model, new InheritedStyle(Map.of(), null, inherited.image(), Map.of()), root);
    if (model.getChildren() != null) {
      Map<String, String> childColors = new HashMap<>(inherited.colors());
      Map<String, String> ownColors = storedColors(model);
      LayoutDarkColors.FOREGROUND_FIELDS.forEach(field -> {
        // Only the page writes its border color as the one of its applications: a section paints its own border
        if (StringUtils.isNotBlank(ownColors.get(field)) && (root || !BORDER_COLOR.equals(field))) {
          childColors.put(field, ownColors.get(field));
        }
      });
      if (root && childColors.containsKey(BORDER_COLOR) && StringUtils.isNotBlank(model.getBorderColor())) {
        // the size goes with the color it is the size of
        putIfNotBlank(childColors, "borderSize", model.getBorderSize());
      }
      Map<String, String> childAppStyle = new HashMap<>(inherited.appStyle());
      putIfNotBlank(childAppStyle, "appBackgroundColor", model.getAppBackgroundColor());
      putIfNotBlank(childAppStyle, "appBackgroundImage", model.getAppBackgroundImage());
      putIfNotBlank(childAppStyle, "appBackgroundEffect", model.getAppBackgroundEffect());
      putIfNotBlank(childAppStyle, "appBackgroundRepeat", model.getAppBackgroundRepeat());
      putIfNotBlank(childAppStyle, "appBackgroundSize", model.getAppBackgroundSize());
      putIfNotBlank(childAppStyle, "appBackgroundPosition", model.getAppBackgroundPosition());
      putIfNotBlank(childAppStyle, "appBackgroundAttachment", model.getAppBackgroundAttachment());
      String childAppBackground = StringUtils.isBlank(model.getAppBackgroundColor()) ? inherited.appBackground() :
                                                                                     model.getAppBackgroundColor();
      boolean childImage = StringUtils.isNotBlank(model.getBackgroundColor()) ? false :
                                                                              hasImage(model) || inherited.image();
      InheritedStyle childInherited = new InheritedStyle(childColors, childAppBackground, childImage, childAppStyle);
      model.getChildren().forEach(child -> expand(child, context, childInherited, false));
    }
  }

  private void setDarkColors(LayoutModel model, InheritedStyle inherited, boolean root) {
    Map<String, String> colors = storedColors(model);
    boolean application = isApplication(model);
    boolean ownBackground = StringUtils.isNotBlank(colors.get(LayoutDarkColors.BACKGROUND_COLOR));
    String background = ownBackground ? colors.get(LayoutDarkColors.BACKGROUND_COLOR) :
                                      application ? inherited.appBackground() : null;
    if (application && StringUtils.isNotBlank(background)) {
      // A text colour inherited from a section or the page is resolved against the background of this application
      inherited.colors().forEach(colors::putIfAbsent);
    }
    // A container lies over the image of its ancestor containers. An application is painted with the application
    // background (the theme's, else the inherited one): only an inherited application background image counts, and
    // only when no application background color is inherited
    boolean inheritedImage = application ? StringUtils.isBlank(inherited.appBackground()) && hasAppImage(inherited.appStyle()) :
                                         inherited.image();
    boolean image = !ownBackground && (hasImage(model) || inheritedImage);
    Map<String, String> dark = LayoutDarkColors.compute(colors,
                                                        background,
                                                        image,
                                                        isPage(model, root) ? BackgroundKind.PAGE : BackgroundKind.APP);
    model.setDarkColors(dark.isEmpty() ? null : dark);
  }

  /**
   * A light only application is light whatever its ancestors: they write their
   * colors, in dark their dark ones, as custom properties that it inherits, and
   * the platform resets them on the outermost light scope. It is given the
   * stored (light) values to write on its own block.
   */
  private void setLightColors(LayoutModel model, InheritedStyle inherited) {
    Map<String, String> lightColors = new HashMap<>(inherited.colors());
    lightColors.putAll(inherited.appStyle());
    model.setLightColors(lightColors.isEmpty() ? null : lightColors);
  }

  private boolean hasAppImage(Map<String, String> appStyle) {
    return appStyle.containsKey("appBackgroundImage") || appStyle.containsKey("appBackgroundEffect");
  }

  /**
   * @return true for the container that holds the page background: the page layout
   *         of a page, the page body attached in a site, the middle center body of
   *         the site editor, and the root of the layout
   */
  private boolean isPage(LayoutModel model, boolean root) {
    return root || model.getTemplate() != null && PAGE_TEMPLATES.contains(model.getTemplate());
  }

  private boolean hasImage(LayoutModel model) {
    return StringUtils.isNotBlank(model.getBackgroundImage()) || StringUtils.isNotBlank(model.getBackgroundEffect());
  }

  private boolean isApplication(LayoutModel model) {
    return model.getChildren() == null && !StringUtils.equals(model.getTemplate(), EntityMapper.PAGE_BODY_TEMPLATE);
  }

  private Map<String, String> storedColors(LayoutModel model) {
    Map<String, String> colors = new HashMap<>();
    putIfNotBlank(colors, LayoutDarkColors.BACKGROUND_COLOR, model.getBackgroundColor());
    putIfNotBlank(colors, LayoutDarkColors.APP_BACKGROUND_COLOR, model.getAppBackgroundColor());
    putIfNotBlank(colors, "textTitleBackgroundColor", model.getTextTitleBackgroundColor());
    putIfNotBlank(colors, "textHeaderBackgroundColor", model.getTextHeaderBackgroundColor());
    putIfNotBlank(colors, "textBackgroundColor", model.getTextBackgroundColor());
    putIfNotBlank(colors, "textSubtitleBackgroundColor", model.getTextSubtitleBackgroundColor());
    putIfNotBlank(colors, "textTitleColor", model.getTextTitleColor());
    putIfNotBlank(colors, "textColor", model.getTextColor());
    putIfNotBlank(colors, "textHeaderColor", model.getTextHeaderColor());
    putIfNotBlank(colors, "textSubtitleColor", model.getTextSubtitleColor());
    putIfNotBlank(colors, "iconColor", model.getIconColor());
    putIfNotBlank(colors, BORDER_COLOR, model.getBorderColor());
    return colors;
  }

  private void putIfNotBlank(Map<String, String> colors, String field, String value) {
    if (StringUtils.isNotBlank(value)) {
      colors.put(field, value);
    }
  }

}
