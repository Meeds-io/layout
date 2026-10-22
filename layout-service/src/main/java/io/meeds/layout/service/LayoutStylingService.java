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
import io.meeds.layout.mcp.model.BackgroundImageBlock;
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
 * before a field is written; a block, a role or a value that is null leaves
 * what the element holds, a block whose enabled flag is false clears its
 * fields, and a value outside the vocabulary is refused naming its path.
 * Images are not written here: an image block is accepted only when it
 * repeats the image the element already holds, which then stays as stored.
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

  private static final Pattern      BACKGROUND_LAYER_TOKENS       =
                                                            Pattern.compile("(^| )layout-bg-(margin-(top|right|bottom|left)|radius-(tl|tr|br|bl))-\\d+(?= |$)");

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
   * @param styling        the blocks, null to leave every block as it is
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
    if (styling != null) {
      applyMargins(target, styling.margins(), destination);
      applyBorder(target, styling.border(), destination);
      applyCorners(target, styling.corners(), destination);
      applyBackground(target, styling.background(), sticky, plainColorOnly);
      applyText(target, styling.text(), destination);
      applyHeight(target, styling.height(), destination);
    }
    if (destination.isApplication()) {
      target.setCssClass(strip(target.getCssClass(), GENERATED_TOKENS));
    }
  }

  /**
   * @param target      the element written
   * @param display     the device visibility, null to leave it as it is
   * @param destination the element's place in the matrix
   * @param refusal     the text answered when the destination does not offer
   *                      the choice
   */
  public void applyDisplay(LayoutModel target, DisplayMode display, StylingDestination destination, String refusal) {
    if (display == null) {
      return;
    }
    DisplayMode mode = display;
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
    if (margins == null) {
      return;
    } else if (!on(margins.enabled())) {
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
    int top = side(margins.top(), margins.all(), neutral, min, max, "margins.top");
    int right = side(margins.right(), margins.all(), neutral, min, max, "margins.right");
    int bottom = side(margins.bottom(), margins.all(), neutral, min, max, "margins.bottom");
    int left = side(margins.left(), margins.all(), neutral, min, max, "margins.left");
    target.setMarginTop(top);
    target.setMarginRight(right);
    target.setMarginBottom(bottom);
    target.setMarginLeft(left);
  }

  private void applyBorder(LayoutModel target, BorderBlock border, StylingDestination destination) {
    if (border == null) {
      return;
    } else if (!on(border.enabled())) {
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
    if (corners == null) {
      return;
    } else if (!on(corners.enabled())) {
      target.setRadiusTopLeft(null);
      target.setRadiusTopRight(null);
      target.setRadiusBottomRight(null);
      target.setRadiusBottomLeft(null);
      return;
    } else if (!destination.hasCorners()) {
      throw refusal("corners");
    }
    int topLeft = side(corners.topLeft(), corners.all(), 0, 0, CORNER_MAX, "corners.top_left");
    int topRight = side(corners.topRight(), corners.all(), 0, 0, CORNER_MAX, "corners.top_right");
    int bottomRight = side(corners.bottomRight(), corners.all(), 0, 0, CORNER_MAX, "corners.bottom_right");
    int bottomLeft = side(corners.bottomLeft(), corners.all(), 0, 0, CORNER_MAX, "corners.bottom_left");
    target.setRadiusTopLeft(topLeft);
    target.setRadiusTopRight(topRight);
    target.setRadiusBottomRight(bottomRight);
    target.setRadiusBottomLeft(bottomLeft);
  }

  private void applyBackground(LayoutModel target, BackgroundBlock background, boolean sticky, boolean plainColorOnly) {
    if (background == null) {
      return;
    } else if (!on(background.enabled())) {
      target.setBackgroundColor(null);
      target.setBackgroundEffect(null);
      clearImage(target);
      return;
    }
    BackgroundImageBlock image = background.image();
    if (image == null) {
      clearImage(target);
    } else if (image.imageUploadId() != null || image.url() == null || !image.url().equals(target.getBackgroundImage())) {
      throw refusal("background.image");
    }
    if (background.effect() != null) {
      if (!background.effect().equals(target.getBackgroundEffect())) {
        throw refusal("background.effect");
      }
      return;
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
   * Removes the element's background image with the image options and the
   * margin and radius tokens that belong to it
   */
  private void clearImage(LayoutModel target) {
    target.setBackgroundImage(null);
    target.setBackgroundSize(null);
    target.setBackgroundPosition(null);
    target.setBackgroundRepeat(null);
    target.setBackgroundAttachment(null);
    target.setCssClass(strip(target.getCssClass(), BACKGROUND_LAYER_TOKENS));
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
    if (text == null) {
      return;
    } else if (!on(text.enabled())) {
      target.setIconColor(null);
      target.setTextTitleColor(null);
      target.setTextTitleFontSize(null);
      target.setTextTitleFontWeight(NORMAL);
      target.setTextTitleFontStyle(NORMAL);
      target.setTextHeaderColor(null);
      target.setTextHeaderFontSize(null);
      target.setTextHeaderFontWeight(NORMAL);
      target.setTextHeaderFontStyle(NORMAL);
      target.setTextColor(null);
      target.setTextFontSize(null);
      target.setTextFontWeight(NORMAL);
      target.setTextFontStyle(NORMAL);
      target.setTextSubtitleColor(null);
      target.setTextSubtitleFontSize(null);
      target.setTextSubtitleFontWeight(NORMAL);
      target.setTextSubtitleFontStyle(NORMAL);
      clearTitleBackground(target);
      clearHeaderBackground(target);
      return;
    } else if (!destination.hasText()) {
      throw refusal("text");
    }
    TextRoleBlock content = text.content();
    TextRoleBlock subtitle = text.subtitle();
    if (content != null && content.background() != null) {
      throw refusal("text.content.background");
    } else if (subtitle != null && subtitle.background() != null) {
      throw refusal("text.subtitle.background");
    }
    if (text.iconColor() != null) {
      target.setIconColor(hex(text.iconColor(), "text.icon_color"));
    }
    TextRoleBlock title = text.title();
    if (title != null) {
      if (title.color() != null) {
        target.setTextTitleColor(hex(title.color(), "text.title.color"));
      }
      if (title.size() != null) {
        target.setTextTitleFontSize(roleSize(title.size(), "text.title"));
      }
      if (title.bold() != null) {
        target.setTextTitleFontWeight(title.bold() ? BOLD : NORMAL);
      }
      if (title.italic() != null) {
        target.setTextTitleFontStyle(title.italic() ? ITALIC : NORMAL);
      }
      applyTitleBackground(target, title.background());
    }
    TextRoleBlock header = text.header();
    if (header != null) {
      if (header.color() != null) {
        target.setTextHeaderColor(hex(header.color(), "text.header.color"));
      }
      if (header.size() != null) {
        target.setTextHeaderFontSize(roleSize(header.size(), "text.header"));
      }
      if (header.bold() != null) {
        target.setTextHeaderFontWeight(header.bold() ? BOLD : NORMAL);
      }
      if (header.italic() != null) {
        target.setTextHeaderFontStyle(header.italic() ? ITALIC : NORMAL);
      }
      applyHeaderBackground(target, header.background());
    }
    if (content != null) {
      if (content.color() != null) {
        target.setTextColor(hex(content.color(), "text.content.color"));
      }
      if (content.size() != null) {
        target.setTextFontSize(roleSize(content.size(), "text.content"));
      }
      if (content.bold() != null) {
        target.setTextFontWeight(content.bold() ? BOLD : NORMAL);
      }
      if (content.italic() != null) {
        target.setTextFontStyle(content.italic() ? ITALIC : NORMAL);
      }
    }
    if (subtitle != null) {
      if (subtitle.color() != null) {
        target.setTextSubtitleColor(hex(subtitle.color(), "text.subtitle.color"));
      }
      if (subtitle.size() != null) {
        target.setTextSubtitleFontSize(roleSize(subtitle.size(), "text.subtitle"));
      }
      if (subtitle.bold() != null) {
        target.setTextSubtitleFontWeight(subtitle.bold() ? BOLD : NORMAL);
      }
      if (subtitle.italic() != null) {
        target.setTextSubtitleFontStyle(subtitle.italic() ? ITALIC : NORMAL);
      }
    }
  }

  private void applyTitleBackground(LayoutModel target, TextBackgroundBlock background) {
    if (background == null) {
      return;
    } else if (!on(background.enabled())) {
      clearTitleBackground(target);
      return;
    }
    String[] values = textBackground(background, target.getTextTitleBackgroundImage(), target.getTextTitleBackgroundEffect(), "text.title.background");
    if (values[0] != null) {
      target.setTextTitleBackgroundColor(values[0]);
      target.setTextTitleBackgroundEffect(values[1]);
    }
    if (background.image() == null) {
      target.setTextTitleBackgroundImage(null);
      target.setTextTitleBackgroundSize(null);
      target.setTextTitleBackgroundRepeat(null);
      target.setTextTitleBackgroundPosition(null);
    }
    if (values[2] != null) {
      target.setTextTitleBackgroundPadding(values[2]);
    }
    if (values[3] != null) {
      target.setTextTitleBackgroundRadius(values[3]);
    }
  }

  private void applyHeaderBackground(LayoutModel target, TextBackgroundBlock background) {
    if (background == null) {
      return;
    } else if (!on(background.enabled())) {
      clearHeaderBackground(target);
      return;
    }
    String[] values = textBackground(background,
                                     target.getTextHeaderBackgroundImage(),
                                     target.getTextHeaderBackgroundEffect(),
                                     "text.header.background");
    if (values[0] != null) {
      target.setTextHeaderBackgroundColor(values[0]);
      target.setTextHeaderBackgroundEffect(values[1]);
    }
    if (background.image() == null) {
      target.setTextHeaderBackgroundImage(null);
      target.setTextHeaderBackgroundSize(null);
      target.setTextHeaderBackgroundRepeat(null);
      target.setTextHeaderBackgroundPosition(null);
    }
    if (values[2] != null) {
      target.setTextHeaderBackgroundPadding(values[2]);
    }
    if (values[3] != null) {
      target.setTextHeaderBackgroundRadius(values[3]);
    }
  }

  private void clearTitleBackground(LayoutModel target) {
    target.setTextTitleBackgroundColor(null);
    target.setTextTitleBackgroundEffect(null);
    target.setTextTitleBackgroundImage(null);
    target.setTextTitleBackgroundSize(null);
    target.setTextTitleBackgroundRepeat(null);
    target.setTextTitleBackgroundPosition(null);
    target.setTextTitleBackgroundPadding(null);
    target.setTextTitleBackgroundRadius(null);
  }

  private void clearHeaderBackground(LayoutModel target) {
    target.setTextHeaderBackgroundColor(null);
    target.setTextHeaderBackgroundEffect(null);
    target.setTextHeaderBackgroundImage(null);
    target.setTextHeaderBackgroundSize(null);
    target.setTextHeaderBackgroundRepeat(null);
    target.setTextHeaderBackgroundPosition(null);
    target.setTextHeaderBackgroundPadding(null);
    target.setTextHeaderBackgroundRadius(null);
  }

  /**
   * @return the colour and effect a text background stores (both null when
   *         the block repeats the stored effect, which then stays), then its
   *         padding and radius, each null when the block leaves it
   */
  private String[] textBackground(TextBackgroundBlock background, String storedImage, String storedEffect, String path) {
    BackgroundImageBlock image = background.image();
    if (image != null && (image.imageUploadId() != null || image.url() == null || !image.url().equals(storedImage))) {
      throw refusal(path + ".image");
    }
    String padding = background.padding() == null ? null : sides(background.padding(), path + ".padding");
    String radius = background.corners() == null ? null : corners(background.corners(), path + ".corners");
    if (background.effect() != null) {
      if (!background.effect().equals(storedEffect)) {
        throw refusal(path + ".effect");
      }
      return new String[] { null, null, padding, radius };
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
    return new String[] { color, effect, padding, radius };
  }

  private String sides(SidesBlock sides, String path) {
    return pixels(sides.top(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".top") + " "
           + pixels(sides.right(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".right") + " "
           + pixels(sides.bottom(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".bottom") + " "
           + pixels(sides.left(), -TEXT_PADDING_MAX, TEXT_PADDING_MAX, path + ".left");
  }

  private String corners(CornersBlock corners, String path) {
    if (!on(corners.enabled())) {
      return null;
    }
    return side(corners.topLeft(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".top_left") + "px "
           + side(corners.topRight(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".top_right") + "px "
           + side(corners.bottomRight(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".bottom_right") + "px "
           + side(corners.bottomLeft(), corners.all(), 0, 0, TEXT_CORNER_MAX, path + ".bottom_left") + "px";
  }

  private void applyHeight(LayoutModel target, HeightBlock height, StylingDestination destination) {
    if (height == null) {
      return;
    } else if (!on(height.enabled())) {
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

  private String roleSize(Integer size, String path) {
    return range(size, 1, TEXT_SIZE_MAX, path + ".size") + "px";
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
