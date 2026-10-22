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
 * before a field is written; a block, a text role or a text background the
 * call leaves null is not written at all, a block whose enabled flag is
 * false clears its fields, and a value outside the vocabulary is refused
 * naming its path. A caller that keeps an element merges the call's
 * description over the element's stored one first ({@link #merge}), so a
 * value left null inside a block it turns on keeps what the element holds;
 * the one block the merge restates on its own is a solid background with a
 * scroll colour on an element that stops being sticky.
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
   * The description a call means for a kept element: a block the call leaves
   * null stays null, so the element keeps it untouched; a block it turns off
   * is off; a block it sends without the flag is on; and inside a block it
   * turns on every value it leaves null is the stored value, a text role, a
   * padding side or a corner included. A stored
   * gradient's effect is kept only while the call names neither a mode nor a
   * colour, and a stored scroll colour only while the element stays sticky
   * and solid: a solid background left untouched on an element that stops
   * being sticky is restated without its scroll colour, as the editor does.
   *
   * @param  stored the element's stored description, as the read answers it
   * @param  input  the call's description, null for every block untouched
   * @param  sticky whether the element is sticky after the call
   * @return        the description to write
   */
  public static StylingDescription merge(StylingDescription stored, StylingDescription input, boolean sticky) { // NOSONAR
    StylingDescription base = stored == null ? new StylingDescription(null, null, null, null, null, null) : stored;
    if (input == null) {
      return new StylingDescription(null, null, null, mergeBackground(base.background(), null, sticky), null, null);
    }
    return new StylingDescription(mergeMargins(base.margins(), input.margins()),
                                  mergeBorder(base.border(), input.border()),
                                  mergeCorners(base.corners(), input.corners()),
                                  mergeBackground(base.background(), input.background(), sticky),
                                  mergeText(base.text(), input.text()),
                                  input.height() == null || off(input.height().enabled()) ? input.height()
                                                                                          : new HeightBlock(true, input.height().fixed(), input.height().customPx()));
  }

  /**
   * @param target         the element written
   * @param styling        the blocks, null for every block untouched
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
    } else if ((display != DisplayMode.BOTH && !destination.hasDisplay())
               || (display == DisplayMode.HIDE_DESKTOP && !destination.hasDesktopHiding())) {
      throw new IllegalArgumentException(refusal);
    }
    String cssClass = strip(target.getCssClass(), DISPLAY_TOKENS);
    if (display == DisplayMode.HIDE_MOBILE) {
      cssClass = append(cssClass, HIDE_MOBILE_TOKEN);
    } else if (display == DisplayMode.HIDE_DESKTOP) {
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

  private static MarginsBlock mergeMargins(MarginsBlock stored, MarginsBlock input) {
    if (input == null || off(input.enabled())) {
      return input;
    }
    MarginsBlock base = stored != null && on(stored.enabled()) ? stored : new MarginsBlock(true, null, null, null, null, null);
    return new MarginsBlock(true,
                            null,
                            or(input.top(), input.all(), base.top()),
                            or(input.right(), input.all(), base.right()),
                            or(input.bottom(), input.all(), base.bottom()),
                            or(input.left(), input.all(), base.left()));
  }

  private static BorderBlock mergeBorder(BorderBlock stored, BorderBlock input) {
    if (input == null || off(input.enabled())) {
      return input;
    }
    BorderBlock base = stored != null && on(stored.enabled()) ? stored : new BorderBlock(true, null, null, null);
    return new BorderBlock(true, or(input.color(), base.color()), or(input.size(), base.size()), or(input.shadow(), base.shadow()));
  }

  private static CornersBlock mergeCorners(CornersBlock stored, CornersBlock input) {
    if (input == null || off(input.enabled())) {
      return input;
    }
    CornersBlock base = stored != null && on(stored.enabled()) ? stored : new CornersBlock(true, null, null, null, null, null);
    return new CornersBlock(true,
                            null,
                            or(input.topLeft(), input.all(), base.topLeft()),
                            or(input.topRight(), input.all(), base.topRight()),
                            or(input.bottomRight(), input.all(), base.bottomRight()),
                            or(input.bottomLeft(), input.all(), base.bottomLeft()));
  }

  private static BackgroundBlock mergeBackground(BackgroundBlock stored, BackgroundBlock input, boolean sticky) {
    BackgroundBlock base = stored != null && on(stored.enabled()) ? stored
                                                                  : new BackgroundBlock(true, null, null, null, null, null, null, null, null, null);
    if (input == null) {
      return sticky || base.scrollColor() == null ? null
                                                  : new BackgroundBlock(true,
                                                                        base.mode(),
                                                                        base.color(),
                                                                        base.from(),
                                                                        base.to(),
                                                                        base.ratio(),
                                                                        base.direction(),
                                                                        null,
                                                                        base.effect(),
                                                                        base.image());
    } else if (off(input.enabled())) {
      return input;
    }
    boolean restated = input.mode() != null || input.color() != null || input.from() != null || input.to() != null;
    if (!restated && base.effect() != null && input.scrollColor() != null) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + "background.scroll_color");
    }
    BackgroundMode mode = input.mode() != null ? input.mode() : input.color() != null ? BackgroundMode.SOLID : base.mode();
    boolean solid = mode == null || mode == BackgroundMode.SOLID;
    return new BackgroundBlock(true,
                               mode,
                               solid ? or(input.color(), base.color()) : input.color(),
                               solid ? input.from() : or(input.from(), base.from()),
                               solid ? input.to() : or(input.to(), base.to()),
                               solid ? input.ratio() : or(input.ratio(), base.ratio()),
                               solid ? input.direction() : or(input.direction(), base.direction()),
                               sticky && solid ? or(input.scrollColor(), base.scrollColor()) : scrollColorOrEcho(input.scrollColor(), base.scrollColor()),
                               restated ? input.effect() : or(input.effect(), base.effect()),
                               mergeImage(base.image(), input.image(), "background.image"));
  }

  /**
   * An image block the call sends must repeat the stored image: its url when
   * given, and every option it names, a corner or a margin side compared by
   * its value; a value it leaves out is the stored one, since no option of a
   * stored image is rewritten here, and the stored block is what the writer
   * then keeps
   */
  private static BackgroundImageBlock mergeImage(BackgroundImageBlock stored, BackgroundImageBlock input, String path) {
    if (input == null) {
      return stored;
    } else if (input.imageUploadId() != null || stored == null || (input.url() != null && !input.url().equals(stored.url()))) {
      return input;
    } else if (differs(input.size(), stored.size())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".size");
    } else if (differs(input.position(), stored.position())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".position");
    } else if (differs(input.attachment(), stored.attachment())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".attachment");
    } else if (differs(input.repeat(), stored.repeat())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".repeat");
    } else if (sidesDiffer(input.margins(), stored.margins())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".margins");
    } else if (cornersDiffer(input.corners(), stored.corners())) {
      throw new IllegalArgumentException(REFUSAL_PREFIX + path + ".corners");
    }
    return stored;
  }

  private static boolean differs(Object given, Object stored) {
    return given != null && !given.equals(stored);
  }

  private static boolean sidesDiffer(SidesBlock given, SidesBlock stored) {
    if (given == null) {
      return false;
    }
    SidesBlock base = stored == null ? new SidesBlock(null, null, null, null) : stored;
    return differs(given.top(), base.top()) || differs(given.right(), base.right()) || differs(given.bottom(), base.bottom())
           || differs(given.left(), base.left());
  }

  private static boolean cornersDiffer(CornersBlock given, CornersBlock stored) {
    if (given == null) {
      return false;
    }
    CornersBlock base = stored == null ? new CornersBlock(null, null, null, null, null, null) : stored;
    boolean storedRounded = base.topLeft() != null || base.topRight() != null || base.bottomRight() != null || base.bottomLeft() != null;
    return (off(given.enabled()) && storedRounded)
           || differs(or(given.topLeft(), given.all()), base.topLeft())
           || differs(or(given.topRight(), given.all()), base.topRight())
           || differs(or(given.bottomRight(), given.all()), base.bottomRight())
           || differs(or(given.bottomLeft(), given.all()), base.bottomLeft());
  }

  private static TextBlock mergeText(TextBlock stored, TextBlock input) {
    if (input == null || off(input.enabled())) {
      return input;
    }
    TextBlock base = stored != null && on(stored.enabled()) ? stored : new TextBlock(true, null, null, null, null, null);
    return new TextBlock(true,
                         mergeRole(base.title(), input.title()),
                         mergeRole(base.header(), input.header()),
                         mergeRole(base.content(), input.content()),
                         mergeRole(base.subtitle(), input.subtitle()),
                         or(input.iconColor(), base.iconColor()));
  }

  private static TextRoleBlock mergeRole(TextRoleBlock stored, TextRoleBlock input) {
    if (input == null) {
      return null;
    }
    TextRoleBlock base = stored == null ? new TextRoleBlock(null, null, null, null, null) : stored;
    return new TextRoleBlock(or(input.color(), base.color()),
                             or(input.size(), base.size()),
                             or(input.bold(), base.bold()),
                             or(input.italic(), base.italic()),
                             mergeTextBackground(base.background(), input.background()));
  }

  private static TextBackgroundBlock mergeTextBackground(TextBackgroundBlock stored, TextBackgroundBlock input) {
    TextBackgroundBlock base = stored != null && on(stored.enabled()) ? stored
                                                                      : new TextBackgroundBlock(true, null, null, null, null, null, null, null, null, null, null);
    if (input == null || off(input.enabled())) {
      return input;
    }
    boolean restated = input.mode() != null || input.color() != null || input.from() != null || input.to() != null;
    BackgroundMode mode = input.mode() != null ? input.mode() : input.color() != null ? BackgroundMode.SOLID : base.mode();
    boolean solid = mode == null || mode == BackgroundMode.SOLID;
    return new TextBackgroundBlock(true,
                                   mode,
                                   solid ? or(input.color(), base.color()) : input.color(),
                                   solid ? input.from() : or(input.from(), base.from()),
                                   solid ? input.to() : or(input.to(), base.to()),
                                   solid ? input.ratio() : or(input.ratio(), base.ratio()),
                                   solid ? input.direction() : or(input.direction(), base.direction()),
                                   restated ? input.effect() : or(input.effect(), base.effect()),
                                   mergeImage(base.image(), input.image(), "text.background.image"),
                                   mergeSides(base.padding(), input.padding()),
                                   mergeTextCorners(base.corners(), input.corners()));
  }

  /**
   * The padding and the corners of a text background are values inside the
   * block: left null they stay stored, unlike a block of their own
   */
  private static SidesBlock mergeSides(SidesBlock stored, SidesBlock input) {
    if (input == null) {
      return stored;
    }
    SidesBlock base = stored == null ? new SidesBlock(null, null, null, null) : stored;
    return new SidesBlock(or(input.top(), base.top()), or(input.right(), base.right()), or(input.bottom(), base.bottom()), or(input.left(), base.left()));
  }

  private static CornersBlock mergeTextCorners(CornersBlock stored, CornersBlock input) {
    return input == null ? stored : mergeCorners(stored, input);
  }

  /**
   * @return the scroll colour a call sends to an element that takes none:
   *         null when it merely repeats the stored one, which the read
   *         reports and the write drops, the value itself otherwise, for the
   *         writer to refuse
   */
  private static String scrollColorOrEcho(String given, String stored) {
    return given != null && given.equals(stored) ? null : given;
  }

  private static <T> T or(T value, T fallback) {
    return value != null ? value : fallback;
  }

  private static <T> T or(T value, T all, T fallback) {
    return value != null ? value : all != null ? all : fallback;
  }

  private static boolean on(Boolean enabled) {
    return Boolean.TRUE.equals(enabled);
  }

  /**
   * A block the call sends without its enabled flag is on: only an explicit
   * false clears, as the note's null rule states
   */
  private static boolean off(Boolean enabled) {
    return Boolean.FALSE.equals(enabled);
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
    String color = hex(border.color(), "border.color");
    int size = range(border.size() == null ? DEFAULT_BORDER_SIZE : border.size(), 0, BORDER_SIZE_MAX, "border.size");
    target.setBorderColor(color);
    target.setBorderSize(String.valueOf(size));
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
      String effect = gradient(mode, background.from(), background.to(), background.ratio(), background.direction(), "background");
      target.setBackgroundColor(GRADIENT_BASE_COLOR);
      target.setBackgroundEffect(effect);
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
    TextRoleBlock title = text.title();
    TextRoleBlock header = text.header();
    TextRoleBlock content = text.content();
    TextRoleBlock subtitle = text.subtitle();
    if (content != null && content.background() != null) {
      throw refusal("text.content.background");
    } else if (subtitle != null && subtitle.background() != null) {
      throw refusal("text.subtitle.background");
    }
    String iconColor = text.iconColor() == null ? null : hex(text.iconColor(), "text.icon_color");
    String[] titleBackground = textBackground(title == null ? null : title.background(),
                                              target.getTextTitleBackgroundImage(),
                                              target.getTextTitleBackgroundEffect(),
                                              "text.title.background");
    String[] headerBackground = textBackground(header == null ? null : header.background(),
                                               target.getTextHeaderBackgroundImage(),
                                               target.getTextHeaderBackgroundEffect(),
                                               "text.header.background");
    String[] titleValues = roleValues(title, "text.title");
    String[] headerValues = roleValues(header, "text.header");
    String[] contentValues = roleValues(content, "text.content");
    String[] subtitleValues = roleValues(subtitle, "text.subtitle");
    target.setIconColor(iconColor);
    if (title != null) {
      target.setTextTitleColor(titleValues[0]);
      target.setTextTitleFontSize(titleValues[1]);
      target.setTextTitleFontWeight(titleValues[2]);
      target.setTextTitleFontStyle(titleValues[3]);
      if (title.background() != null && !on(title.background().enabled())) {
        clearTitleBackground(target);
      } else if (title.background() != null) {
        target.setTextTitleBackgroundColor(titleBackground[0]);
        target.setTextTitleBackgroundEffect(titleBackground[1]);
        if (titleBackground[4] == null) {
          target.setTextTitleBackgroundImage(null);
          target.setTextTitleBackgroundSize(null);
          target.setTextTitleBackgroundRepeat(null);
          target.setTextTitleBackgroundPosition(null);
        }
        target.setTextTitleBackgroundPadding(titleBackground[2]);
        target.setTextTitleBackgroundRadius(titleBackground[3]);
      }
    }
    if (header != null) {
      target.setTextHeaderColor(headerValues[0]);
      target.setTextHeaderFontSize(headerValues[1]);
      target.setTextHeaderFontWeight(headerValues[2]);
      target.setTextHeaderFontStyle(headerValues[3]);
      if (header.background() != null && !on(header.background().enabled())) {
        clearHeaderBackground(target);
      } else if (header.background() != null) {
        target.setTextHeaderBackgroundColor(headerBackground[0]);
        target.setTextHeaderBackgroundEffect(headerBackground[1]);
        if (headerBackground[4] == null) {
          target.setTextHeaderBackgroundImage(null);
          target.setTextHeaderBackgroundSize(null);
          target.setTextHeaderBackgroundRepeat(null);
          target.setTextHeaderBackgroundPosition(null);
        }
        target.setTextHeaderBackgroundPadding(headerBackground[2]);
        target.setTextHeaderBackgroundRadius(headerBackground[3]);
      }
    }
    if (content != null) {
      target.setTextColor(contentValues[0]);
      target.setTextFontSize(contentValues[1]);
      target.setTextFontWeight(contentValues[2]);
      target.setTextFontStyle(contentValues[3]);
    }
    if (subtitle != null) {
      target.setTextSubtitleColor(subtitleValues[0]);
      target.setTextSubtitleFontSize(subtitleValues[1]);
      target.setTextSubtitleFontWeight(subtitleValues[2]);
      target.setTextSubtitleFontStyle(subtitleValues[3]);
    }
  }

  /**
   * @return the colour, the size in pixels, the weight and the style a role
   *         stores, as the editor's text input writes them: a weight or a
   *         style not chosen is normal
   */
  private String[] roleValues(TextRoleBlock role, String path) {
    if (role == null) {
      return new String[4];
    }
    return new String[] { role.color() == null ? null : hex(role.color(), path + ".color"),
                          role.size() == null ? null : range(role.size(), 1, TEXT_SIZE_MAX, path + ".size") + "px",
                          Boolean.TRUE.equals(role.bold()) ? BOLD : NORMAL,
                          Boolean.TRUE.equals(role.italic()) ? ITALIC : NORMAL };
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
   * @return the colour, effect, padding and radius a text background that is
   *         on stores, then a marker that the stored image stays; a null
   *         colour with the stored effect when the block repeats that effect
   */
  private String[] textBackground(TextBackgroundBlock background, String storedImage, String storedEffect, String path) {
    if (background == null || !on(background.enabled())) {
      return new String[5];
    }
    BackgroundImageBlock image = background.image();
    if (image != null && (image.imageUploadId() != null || image.url() == null || !image.url().equals(storedImage))) {
      throw refusal(path + ".image");
    }
    String keptImage = image == null ? null : storedImage;
    String padding = background.padding() == null ? null : sides(background.padding(), path + ".padding");
    String radius = background.corners() == null || !on(background.corners().enabled()) ? null
                                                                                          : corners(background.corners(), path + ".corners");
    if (background.effect() != null) {
      if (!background.effect().equals(storedEffect)) {
        throw refusal(path + ".effect");
      }
      return new String[] { GRADIENT_BASE_COLOR, storedEffect, padding, radius, keptImage };
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
    return new String[] { color, effect, padding, radius, keptImage };
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
