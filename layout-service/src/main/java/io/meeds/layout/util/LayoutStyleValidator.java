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
package io.meeds.layout.util;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.ModelStyle;

/**
 * Validates the style values a page or a site layout carries before it is
 * saved, on every editor save path (page layout and site layout alike). A
 * refused value fails the save with an {@link IllegalArgumentException}.
 */
public final class LayoutStyleValidator {

  private static final Pattern GENERIC_STYLE_VALIDATOR = Pattern.compile("[#0-9a-zA-Z\\(\\),\\./\"'\\-%_ ]+");

  /**
   * The page, application and site icon colour is a hex colour only: no
   * keyword, since "not set" at this level is the absent field, and no
   * function, since the value is written by page editors and site editors.
   */
  private static final Pattern ICON_COLOR_VALIDATOR    = Pattern.compile("#[0-9a-fA-F]{3,8}");

  /**
   * A sticky site section (Topbar, Sidebar) stores its two on-scroll colours as
   * one background value, "&lt;top&gt;@&lt;middle&gt;", which the renderer splits
   * back; each half meets the generic check on its own.
   */
  private static final String  SCROLL_COLOR_SEPARATOR  = "@";

  private LayoutStyleValidator() {
    // Static utility
  }

  /**
   * Validates the style values of the given layout object and of every
   * descendant container.
   *
   * @param modelObject the page, site layout or container to validate, null
   *          accepted (nothing to validate)
   * @throws IllegalArgumentException when a style value is refused
   */
  public static void validate(ModelObject modelObject) { // NOSONAR
    if (modelObject == null) {
      return;
    }
    ModelStyle cssStyle = modelObject.getCssStyle();
    Arrays.asList(modelObject.getHeight(),
                  modelObject.getWidth(),
                  cssStyle == null ? null : cssStyle.getBorderColor(),
                  cssStyle == null ? null : cssStyle.getBorderSize(),
                  cssStyle == null ? null : cssStyle.getBoxShadow(),
                  cssStyle == null ? null : cssStyle.getBackgroundImage(),
                  cssStyle == null ? null : cssStyle.getBackgroundEffect(),
                  cssStyle == null ? null : cssStyle.getBackgroundPosition(),
                  cssStyle == null ? null : cssStyle.getBackgroundSize(),
                  cssStyle == null ? null : cssStyle.getBackgroundRepeat(),
                  cssStyle == null ? null : cssStyle.getTextTitleColor(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontSize(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextTitleFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextColor(),
                  cssStyle == null ? null : cssStyle.getTextFontSize(),
                  cssStyle == null ? null : cssStyle.getTextFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextHeaderColor(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontSize(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextHeaderFontStyle(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleColor(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontSize(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontWeight(),
                  cssStyle == null ? null : cssStyle.getTextSubtitleFontStyle())
          .forEach(LayoutStyleValidator::validateStyleValue);
    if (cssStyle != null) {
      validateBackgroundColor(cssStyle.getBackgroundColor());
      validateIconColor(cssStyle.getIconColor());
    }
    if (modelObject instanceof Container container && !CollectionUtils.isEmpty(container.getChildren())) {
      container.getChildren().forEach(LayoutStyleValidator::validate);
    }
  }

  private static void validateStyleValue(String value) {
    if (StringUtils.isNotBlank(value)
        && (!GENERIC_STYLE_VALIDATOR.matcher(value).matches()
            || value.contains("javascript")
            || value.contains("eval"))) {
      throw new IllegalArgumentException(String.format("Invalid css value input %s", value));
    }
  }

  private static void validateBackgroundColor(String value) {
    if (StringUtils.isNotBlank(value)) {
      String[] colors = StringUtils.split(value, SCROLL_COLOR_SEPARATOR);
      if (colors.length > 2) {
        throw new IllegalArgumentException(String.format("Invalid css value input %s", value));
      }
      for (String color : colors) {
        validateStyleValue(color);
      }
    }
  }

  private static void validateIconColor(String value) {
    if (StringUtils.isNotBlank(value) && !ICON_COLOR_VALIDATOR.matcher(value).matches()) {
      throw new IllegalArgumentException(String.format("Invalid icon color input %s", value));
    }
  }

}
