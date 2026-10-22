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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import io.meeds.layout.mcp.constant.BackgroundMode;
import io.meeds.layout.mcp.constant.DisplayMode;
import io.meeds.layout.mcp.constant.HeightPreset;
import io.meeds.layout.mcp.constant.StylingDestination;
import io.meeds.layout.mcp.model.BackgroundBlock;
import io.meeds.layout.mcp.model.BorderBlock;
import io.meeds.layout.mcp.model.CornersBlock;
import io.meeds.layout.mcp.model.HeightBlock;
import io.meeds.layout.mcp.model.MarginsBlock;
import io.meeds.layout.mcp.model.SidesBlock;
import io.meeds.layout.mcp.model.StylingDescription;
import io.meeds.layout.mcp.model.TextBackgroundBlock;
import io.meeds.layout.mcp.model.TextBlock;
import io.meeds.layout.mcp.model.TextRoleBlock;
import io.meeds.layout.model.LayoutModel;

/**
 * Writes a styling description onto a layout element exactly as the editor's
 * styling inputs do, the inverse of LayoutDescriptionService.toStyling: the
 * vocabulary is checked block by block against the destination's matrix
 * before a field is written, a block that is off clears its fields, and a
 * value outside the vocabulary is refused naming its path. Images are not
 * written here: a background or text background carrying an image block is
 * refused.
 */
@Service
public class LayoutStylingService {

  public static final String        REFUSAL_PREFIX                = "value is not accepted for styling.";

  public static final String        GRADIENT_BASE_COLOR           = "#FFFFFF00";

  public static final String        DEFAULT_COLOR                 = "#FFFFFF";

  public static final String        TEXT_BACKGROUND_DEFAULT_COLOR = "#FFFFFFFF";

  public static final String        SCROLL_COLOR_SEPARATOR        = "@";

  public static final String        HIDE_MOBILE_TOKEN             = "hidden-sm-and-down";

  public static final String        HIDE_DESKTOP_TOKEN            = "hidden-md-and-up";

  public static final String        TO_BOTTOM                     = "to_bottom";

  public static final String        TO_RIGHT                      = "to_right";

  public static final String        BOLD                          = "bold";

  public static final String        ITALIC                        = "italic";

  public static final String        NORMAL                        = "normal";

  public static final int           CONTAINER_MARGIN_MIN          = -60;

  public static final int           CONTAINER_MARGIN_MAX          = 60;

  public static final int           APPLICATION_MARGIN_MIN        = -40;

  public static final int           APPLICATION_MARGIN_MAX        = 80;

  public static final int           APPLICATION_MARGIN_NEUTRAL    = 20;

  public static final int           BORDER_SIZE_MAX               = 8;

  public static final int           DEFAULT_BORDER_SIZE           = 1;

  public static final int           CORNER_MAX                    = 20;

  public static final int           TEXT_SIZE_MAX                 = 60;

  public static final int           TEXT_PADDING_MAX              = 400;

  public static final int           TEXT_CORNER_MAX               = 120;

  public static final int           RATIO_MAX                     = 100;

  public static final int           DEFAULT_RATIO                 = 50;

  public static final int           CUSTOM_HEIGHT_MIN             = 10;

  public static final int           CUSTOM_HEIGHT_MAX             = 1000;

  private static final Pattern      HEX_COLOR                     = Pattern.compile("#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?");

  private static final Pattern      GENERATED_TOKENS              =
                                                     Pattern.compile("(^| )((brtr|brtl|brbr|brbl)-\\d+|(mt|mr|mb|ml|ms|me)-((md|lg|xl)-)?n?\\d{1,2})(?= |$)");

  private static final Pattern      DISPLAY_TOKENS                = Pattern.compile("(^| )(hidden-sm-and-down|hidden-md-and-up)(?= |$)");

  private static final Map<String, String> ANGULAR_START_ANGLES   = Map.of("top_left",
                                                                           "90deg",
                                                                           "top_right",
                                                                           "180deg",
                                                                           "bottom_right",
                                                                           "270deg",
                                                                           "bottom_left",
                                                                           "0deg");

  /**
   * @param target         the element written
   * @param styling        the blocks, null for every block off
   * @param destination    the element's place in the matrix
   * @param sticky         whether the element is a sticky site container,
   *                         the only one with a scroll colour
   * @param plainColorOnly whether the background takes a plain colour only,
   *                         as the editor offers on a topbar
   */
  public void applyStyling(LayoutModel target,
                           StylingDescription styling,
                           StylingDestination destination,
                           boolean sticky,
                           boolean plainColorOnly) {
    StylingDescription blocks = styling == null ? new StylingDescription(null, null, null, null, null, null) : styling;
    applyMargins(target, blocks.margins(), destination);
    applyBorder(target, blocks.border(), destination);
    applyCorners(target, blocks.corners(), destination);
    applyBackground(target, blocks.background(), sticky, plainColorOnly);
    applyText(target, blocks.text(), destination);
    applyHeight(target, blocks.height(), destination);
    if (destination.isApplication()) {
      target.setCssClass(strip(target.getCssClass(), GENERATED_TOKENS));
    }
  }

  /**
   * @param target      the element written
   * @param display     the device visibility, null for both
   * @param destination the element's place in the matrix
   * @param refusal     the text answered when the destination does not offer
   *                      the choice
   */
  public void applyDisplay(LayoutModel target, DisplayMode display, StylingDestination destination, String refusal) {
    DisplayMode mode = display == null ? DisplayMode.BOTH : display;
    if ((mode != DisplayMode.BOTH && !destination.hasDisplay())
        || (mode == DisplayMode.HIDE_DESKTOP && !destination.hasDesktopHiding())) {
      throw new IllegalArgumentException(refusal);
    }
    String cssClass = strip(target.getCssClass(), DISPLAY_TOKENS);
    if (mode == DisplayMode.HIDE_MOBILE) {
      cssClass = append(cssClass, HIDE_MOBILE_TOKEN);
    } else if (mode == DisplayMode.HIDE_DESKTOP) {
      cssClass = append(cssClass, HIDE_DESKTOP_TOKEN);
    }
    target.setCssClass(cssClass);
  }

  public static String strip(String cssClass, Pattern tokens) {
    if (StringUtils.isBlank(cssClass)) {
      return null;
    }
    String stripped = tokens.matcher(cssClass).replaceAll(" ").replaceAll(" {2,}", " ").trim();
    return StringUtils.isBlank(stripped) ? null : stripped;
  }

  public static String append(String cssClass, String token) {
    return StringUtils.isBlank(cssClass) ? token : cssClass.trim() + " " + token;
  }

  private void applyMargins(LayoutModel target, MarginsBlock margins, StylingDestination destination) {
    if (!on(margins == null ? null : margins.enabled())) {
      target.setMarginTop(null);
      target.setMarginRight(null);
      target.setMarginBottom(null);
      target.setMarginLeft(null);
      return;
    }
    boolean application = destination.isApplication();
    int min = application ? APPLICATION_MARGIN_MIN : CONTAINER_MARGIN_MIN;
    int max = application ? APPLICATION_MARGIN_MAX : CONTAINER_MARGIN_MAX;
    int neutral = application ? APPLICATION_MARGIN_NEUTRAL : 0;
    target.setMarginTop(side(margins.top(), margins.all(), neutral, min, max, "margins.top"));
    target.setMarginRight(side(margins.right(), margins.all(), neutral, min, max, "margins.right"));
    target.setMarginBottom(side(margins.bottom(), margins.all(), neutral, min, max, "margins.bottom"));
    target.setMarginLeft(side(margins.left(), margins.all(), neutral, min, max, "margins.left"));
  }

  private void applyBorder(LayoutModel target, BorderBlock border, StylingDestination destination) {
    if (!on(border == null ? null : border.enabled())) {
      if (destination.hasBorder()) {
        target.setBorderColor(null);
        target.setBorderSize("0");
        target.setBoxShadow(null);
      }
      return;
    } else if (!destination.hasBorder()) {
      throw refusal("border");
    }
    target.setBorderColor(hex(border.color(), "border.color"));
    target.setBorderSize(String.valueOf(range(border.size() == null ? DEFAULT_BORDER_SIZE : border.size(),
                                              0,
                                              BORDER_SIZE_MAX,
                                              "border.size")));
    target.setBoxShadow(Boolean.TRUE.equals(border.shadow()) ? "true" : null);
  }

  private void applyCorners(LayoutModel target, CornersBlock corners, StylingDestination destination) {
    if (!on(corners == null ? null : corners.enabled())) {
      target.setRadiusTopLeft(null);
      target.setRadiusTopRight(null);
      target.setRadiusBottomRight(null);
      target.setRadiusBottomLeft(null);
      return;
    } else if (!destination.hasCorners()) {
      throw refusal("corners");
    }
    target.setRadiusTopLeft(side(corners.topLeft(), corners.all(), 0, 0, CORNER_MAX, "corners.top_left"));
    target.setRadiusTopRight(side(corners.topRight(), corners.all(), 0, 0, CORNER_MAX, "corners.top_right"));
    target.setRadiusBottomRight(side(corners.bottomRight(), corners.all(), 0, 0, CORNER_MAX, "corners.bottom_right"));
    target.setRadiusBottomLeft(side(corners.bottomLeft(), corners.all(), 0, 0, CORNER_MAX, "corners.bottom_left"));
  }

  private void applyBackground(LayoutModel target, BackgroundBlock background, boolean sticky, boolean plainColorOnly) {
    target.setBackgroundImage(null);
    target.setBackgroundSize(null);
    target.setBackgroundPosition(null);
    target.setBackgroundRepeat(null);
    target.setBackgroundAttachment(null);
    if (!on(background == null ? null : background.enabled())) {
      target.setBackgroundColor(null);
      target.setBackgroundEffect(null);
      return;
    } else if (background.image() != null) {
      throw refusal("background.image");
    } else if (background.effect() != null) {
      throw refusal("background.effect");
    }
    BackgroundMode mode = background.mode() == null ? BackgroundMode.SOLID : background.mode();
    if (mode == BackgroundMode.SOLID) {
      if (background.from() != null || background.to() != null || background.ratio() != null || background.direction() != null) {
        throw refusal("background.mode");
      }
      String color = hex(background.color() == null ? DEFAULT_COLOR : background.color(), "background.color");
      if (background.scrollColor() != null) {
        if (!sticky) {
          throw refusal("background.scroll_color");
        }
        color = color + SCROLL_COLOR_SEPARATOR + hex(background.scrollColor(), "background.scroll_color");
      } else if (sticky) {
        color = color + SCROLL_COLOR_SEPARATOR + color;
      }
      target.setBackgroundColor(color);
      target.setBackgroundEffect(null);
    } else {
      if (plainColorOnly) {
        throw refusal("background.mode");
      } else if (background.color() != null) {
        throw refusal("background.color");
      } else if (background.scrollColor() != null) {
        throw refusal("background.scroll_color");
      }
      target.setBackgroundColor(GRADIENT_BASE_COLOR);
      target.setBackgroundEffect(gradient(mode,
                                          background.from(),
                                          background.to(),
                                          background.ratio(),
                                          background.direction(),
                                          "background"));
    }
  }

  /**
   * The three gradient strings the editor's background input writes, stop
   * for stop: a plateau of the From colour up to the ratio, then the To
   * colour; on a conic gradient the sweep starts at the corner's own edge, so
   * for top right and bottom left the colours are swapped and the plateau
   * measured from the other end.
   */
  private String gradient(BackgroundMode mode, String fromValue, String toValue, Integer ratioValue, String direction, String path) {
    String from = hex(fromValue, path + ".from");
    String to = hex(toValue, path + ".to");
    int ratio = range(ratioValue == null ? DEFAULT_RATIO : ratioValue, 0, RATIO_MAX, path + ".ratio");
    switch (mode) {
    case LINEAR: {
      String linearDirection = direction == null ? TO_BOTTOM : direction;
      if (!TO_BOTTOM.equals(linearDirection) && !TO_RIGHT.equals(linearDirection)) {
        throw refusal(path + ".direction");
      }
      String stops = from + " 0%, " + from + " " + ratio + "%, " + to + " 100%";
      return TO_RIGHT.equals(linearDirection) ? "linear-gradient(to right, " + stops + ")" : "linear-gradient(" + stops + ")";
    }
    case RADIAL: {
      if (direction != null) {
        throw refusal(path + ".direction");
      }
      return "radial-gradient(" + from + " 0%, " + from + " " + ratio + "%, " + to + " 100%)";
    }
    case ANGULAR: {
      String corner = direction == null ? "top_left" : direction;
      String startAngle = ANGULAR_START_ANGLES.get(corner);
      if (startAngle == null) {
        throw refusal(path + ".direction");
      }
      boolean reversed = "top_right".equals(corner) || "bottom_left".equals(corner);
      String edgeColor = reversed ? to : from;
      String farColor = reversed ? from : to;
      int midPercent = reversed ? RATIO_MAX - ratio : ratio;
      String midAngle = String.format(Locale.ROOT, "%.2f", midPercent / 100d * 90);
      String prefix = "conic-gradient(from " + startAngle + " at " + corner.replace('_', ' ') + ", " + edgeColor + " 0deg, ";
      return reversed ? prefix + farColor + " " + midAngle + "deg, " + farColor + " 90deg)"
                      : prefix + edgeColor + " " + midAngle + "deg, " + farColor + " 90deg)";
    }
    default:
      throw refusal(path + ".mode");
    }
  }

  private void applyText(LayoutModel target, TextBlock text, StylingDestination destination) { // NOSONAR
    boolean on = on(text == null ? null : text.enabled());
    if (on && !destination.hasText()) {
      throw refusal("text");
    }
    TextRoleBlock title = on ? text.title() : null;
    TextRoleBlock header = on ? text.header() : null;
    TextRoleBlock content = on ? text.content() : null;
    TextRoleBlock subtitle = on ? text.subtitle() : null;
    if (content != null && content.background() != null) {
      throw refusal("text.content.background");
    } else if (subtitle != null && subtitle.background() != null) {
      throw refusal("text.subtitle.background");
    }
    target.setIconColor(on && text.iconColor() != null ? hex(text.iconColor(), "text.icon_color") : null);
    target.setTextTitleColor(roleColor(title, "text.title"));
    target.setTextTitleFontSize(roleSize(title, "text.title"));
    target.setTextTitleFontWeight(roleWeight(title));
    target.setTextTitleFontStyle(roleStyle(title));
    String[] titleBackground = textBackground(title == null ? null : title.background(), "text.title.background");
    target.setTextTitleBackgroundColor(titleBackground[0]);
    target.setTextTitleBackgroundEffect(titleBackground[1]);
    target.setTextTitleBackgroundImage(null);
    target.setTextTitleBackgroundSize(null);
    target.setTextTitleBackgroundRepeat(null);
    target.setTextTitleBackgroundPosition(null);
    target.setTextTitleBackgroundPadding(titleBackground[2]);
    target.setTextTitleBackgroundRadius(titleBackground[3]);
    target.setTextHeaderColor(roleColor(header, "text.header"));
    target.setTextHeaderFontSize(roleSize(header, "text.header"));
    target.setTextHeaderFontWeight(roleWeight(header));
    target.setTextHeaderFontStyle(roleStyle(header));
    String[] headerBackground = textBackground(header == null ? null : header.background(), "text.header.background");
    target.setTextHeaderBackgroundColor(headerBackground[0]);
    target.setTextHeaderBackgroundEffect(headerBackground[1]);
    target.setTextHeaderBackgroundImage(null);
    target.setTextHeaderBackgroundSize(null);
    target.setTextHeaderBackgroundRepeat(null);
    target.setTextHeaderBackgroundPosition(null);
    target.setTextHeaderBackgroundPadding(headerBackground[2]);
    target.setTextHeaderBackgroundRadius(headerBackground[3]);
    target.setTextColor(roleColor(content, "text.content"));
    target.setTextFontSize(roleSize(content, "text.content"));
    target.setTextFontWeight(roleWeight(content));
    target.setTextFontStyle(roleStyle(content));
    target.setTextSubtitleColor(roleColor(subtitle, "text.subtitle"));
    target.setTextSubtitleFontSize(roleSize(subtitle, "text.subtitle"));
    target.setTextSubtitleFontWeight(roleWeight(subtitle));
    target.setTextSubtitleFontStyle(roleStyle(subtitle));
  }

  /**
   * @return the colour, effect, padding and radius a text background stores,
   *         four nulls when it is off
   */
  private String[] textBackground(TextBackgroundBlock background, String path) {
    if (!on(background == null ? null : background.enabled())) {
      return new String[4];
    } else if (background.image() != null) {
      throw refusal(path + ".image");
    } else if (background.effect() != null) {
      throw refusal(path + ".effect");
    }
    BackgroundMode mode = background.mode() == null ? BackgroundMode.SOLID : background.mode();
    String color;
    String effect = null;
    if (mode == BackgroundMode.SOLID) {
      if (background.from() != null || background.to() != null || background.ratio() != null || background.direction() != null) {
        throw refusal(path + ".mode");
      }
      color = hex(background.color() == null ? TEXT_BACKGROUND_DEFAULT_COLOR : background.color(), path + ".color");
    } else {
      if (background.color() != null) {
        throw refusal(path + ".color");
      }
      color = GRADIENT_BASE_COLOR;
      effect = gradient(mode, background.from(), background.to(), background.ratio(), background.direction(), path);
    }
    String padding = background.padding() == null ? null : sides(background.padding(), path + ".padding");
    String radius = background.corners() == null || !on(background.corners().enabled()) ? null
                                                                                      : corners(background.corners(), path + ".corners");
    return new String[] { color, effect, padding, radius };
  }

  private String sides(SidesBlock sides, String path) {
    return pixels(sides.top(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".top") + " "
           + pixels(sides.right(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".right") + " "
           + pixels(sides.bottom(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".bottom") + " "
           + pixels(sides.left(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".left");
  }

  private String corners(CornersBlock corners, String path) {
    return side(corners.topLeft(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".top_left") + "px "
           + side(corners.topRight(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".top_right") + "px "
           + side(corners.bottomRight(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".bottom_right") + "px "
           + side(corners.bottomLeft(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".bottom_left") + "px";
  }

  private void applyHeight(LayoutModel target, HeightBlock height, StylingDestination destination) {
    if (!on(height == null ? null : height.enabled())) {
      if (destination.hasHeight()) {
        target.setHeight(null);
      }
      return;
    } else if (!destination.hasHeight()) {
      throw refusal("height");
    } else if (height.fixed() == null) {
      throw refusal("height.fixed");
    } else if (height.fixed() == HeightPreset.CUSTOM) {
      target.setHeight(String.valueOf(range(height.customPx(), CUSTOM_HEIGHT_MIN, CUSTOM_HEIGHT_MAX, "height.custom_px")));
    } else if (height.customPx() != null) {
      throw refusal("height.custom_px");
    } else {
      target.setHeight(height.fixed().getStoredValue());
    }
  }

  private boolean on(Boolean enabled) {
    return Boolean.TRUE.equals(enabled);
  }

  private String roleColor(TextRoleBlock role, String path) {
    return role == null || role.color() == null ? null : hex(role.color(), path + ".color");
  }

  private String roleSize(TextRoleBlock role, String path) {
    return role == null || role.size() == null ? null : range(role.size(), 1, TEXT_SIZE_MAX, path + ".size") + "px";
  }

  /**
   * The editor's text input writes a normal weight and style for every role
   * it does not embolden or italicise, the block off included
   */
  private String roleWeight(TextRoleBlock role) {
    return role != null && Boolean.TRUE.equals(role.bold()) ? BOLD : NORMAL;
  }

  private String roleStyle(TextRoleBlock role) {
    return role != null && Boolean.TRUE.equals(role.italic()) ? ITALIC : NORMAL;
  }

  private String pixels(Integer value, int min, int max, String path) {
    return range(value == null ? 0 : value, min, max, path) + "px";
  }

  private int side(Integer value, Integer all, int fallback, int min, int max, String path) {
    int resolved = value != null ? value : all != null ? all : fallback;
    return range(resolved, min, max, path);
  }

  private int range(Integer value, int min, int max, String path) {
    if (value == null || value < min || value > max) {
      throw refusal(path);
    }
    return value;
  }

  private String hex(String value, String path) {
    String color = StringUtils.trim(value);
    if (color == null || !HEX_COLOR.matcher(color).matches()) {
      throw refusal(path);
    }
    return color;
  }

  private IllegalArgumentException refusal(String path) {
    return new IllegalArgumentException(REFUSAL_PREFIX + path);
  }

}
