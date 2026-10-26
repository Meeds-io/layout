/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

// Tracks whether getStyle() itself has overridden the site-level --allPagesBackground*
// custom properties on <body>, so it only clears them when reverting its own override,
// never when the site simply never had a background (see getStyle's siteStyle branch).
let siteBackgroundOverridden = false;

export function installApplication(navUri, applicationStorageId, applicationElement, applicationMode, showSite, fullRender) {
  return getApplicationContent(navUri, applicationStorageId, applicationMode, showSite, fullRender)
    .then(applicationContent => handleApplicationContent(applicationContent, applicationElement, applicationMode));
}

const GRADIENT_COLOR_PATTERN = /#[0-9a-fA-F]{3,8}|rgba?\([^)]*\)/;

export function isTopBanner(container) {
  return !!container?.cssClass?.includes?.('layout-banner-top-section');
}

/**
 * Value of a branding theme variable (--allPages…) as the browser computes it at the document root, null when the
 * branding stylesheet left it unset: it then emits 'initial', the guaranteed-invalid value, which computes to an empty
 * string. Lets an editor show the colour a level inherits without storing it.
 *
 * @param {string} name the custom property, e.g. '--allPagesAppIconColor'
 * @returns {string|null} the computed value, null when unset
 */
export function getThemeColor(name) {
  return window.getComputedStyle?.(document.documentElement)?.getPropertyValue?.(name)?.trim?.() || null;
}

export function firstGradientColor(effect) {
  const match = effect?.match?.(GRADIENT_COLOR_PATTERN);
  return match ? match[0] : null;
}

export function isTransparentColor(color) {
  return color === 'transparent' || (color?.length === 9 && color.toUpperCase().endsWith('00'));
}

/**
 * Which Topbar displayed above the page lets what is behind it show through, before or after the page is scrolled.
 * A page may display the site layout's top banner, the shared layout's Topbar, both or none: whether the shared layout
 * is shown depends on the site and the page, not on the layout (UISharedLayout), so a space site's layout renders under
 * the shared Topbar while a site layout may also render without it, and a site layout may have no top banner. The top
 * banner shows its own colours when it has some, the Branding Topbar colours otherwise, as the shared Topbar always
 * does. A stored banner gradient is resolved as the banner renders it (getStyle).
 * The two are told apart because the body paints different layers behind them: the top banner is inside the site
 * scroller, which paints the body-level image, while the shared Topbar is above it, where only the body colour shows.
 *
 * @param {object} layout the displayed layout (site layout or page layout)
 * @returns {{topBanner: boolean, sharedTopBar: boolean}} for each displayed Topbar, true when one of its colours has
 *   an alpha below 1
 */
export function getTopBarTransparency(layout) {
  const topBanner = findTopBanner(layout);
  // The On Scroll colour paints only on a sticky Topbar: the shared layout one always is, a top banner when it has the
  // sticky option of its own or the platform-wide one (body.topbar-sticky)
  return {
    topBanner: !!topBanner
      && hasTransparentColor(getTopBannerColors(topBanner) || getBrandingTopBarColors(),
        isStickySection(topBanner) || document.body.classList.contains('topbar-sticky')),
    sharedTopBar: !!document.getElementById('UITopBarContainerParent')
      && hasTransparentColor(getBrandingTopBarColors(), true),
  };
}

function getBrandingTopBarColors() {
  return [
    getThemeColor('--allPagesTopBarBackgroundColor'),
    getThemeColor('--allPagesTopBarBackgroundScrollColor'),
  ];
}

function hasTransparentColor(colors, sticky) {
  return colors.slice(0, sticky ? 2 : 1).some(hasTransparency);
}

// The background colours getStyle renders for the top banner (sectionStyle), before and after the page is scrolled,
// null when it renders none of its own: the skin then paints the Branding Topbar colours. A banner background with
// margin or radius values is an inset layer (getBackgroundLayerStyle) painted over the banner element, which keeps the
// Branding Topbar colours of the skin: those are the ones that can let the page through.
function getTopBannerColors(topBanner) {
  if (!topBanner || parseBackgroundLayerValues(topBanner.cssClass)) {
    return null;
  } else if (topBanner.backgroundColor) {
    if (topBanner.backgroundEffect) {
      const color = getTopBannerGradientColor(topBanner) || getThemeColor('--allPagesTopBarBackgroundColor');
      return [color, color];
    }
    const [color, scrollColor] = topBanner.backgroundColor.split('@');
    return [color, scrollColor || color];
  } else if (topBanner.backgroundEffect || topBanner.backgroundImage) {
    return ['transparent'];
  }
  return null;
}

// The Topbar background offers no gradient option. A stored gradient is ignored, never rewritten: its first colour is
// shown, otherwise the Topbar colour (its colour at rest, when an On Scroll one is stored with it), null when neither
// is usable (the platform Topbar colour is shown then)
function getTopBannerGradientColor(topBanner) {
  const color = topBanner.backgroundColor?.split?.('@')?.[0];
  return firstGradientColor(topBanner.backgroundEffect)
    || (color && !isTransparentColor(color) && color)
    || null;
}

function isStickySection(container) {
  return !!container?.cssClass?.includes?.('layout-sticky-section');
}

function findTopBanner(container) {
  if (!container || isTopBanner(container)) {
    return container || null;
  }
  for (const child of container.children || []) {
    const topBanner = findTopBanner(child);
    if (topBanner) {
      return topBanner;
    }
  }
  return null;
}

function hasTransparency(color) {
  return getColorAlpha(color) < 1;
}

// Alpha of a colour between 0 and 1, 1 when the colour has none or is not given
function getColorAlpha(color) {
  const value = color?.trim?.()?.toLowerCase?.();
  if (!value) {
    return 1;
  } else if (value === 'transparent') {
    return 0;
  } else if (/^#[0-9a-f]{4}$/.test(value)) {
    return parseInt(value[4], 16) / 15;
  } else if (/^#[0-9a-f]{8}$/.test(value)) {
    return parseInt(value.slice(7), 16) / 255;
  }
  // Only a fourth component is an alpha: rgb(r, g, b), rgba(r, g, b, a), rgb(r g b / a), and the same for hsl
  const alpha = value.match(/^(?:rgb|hsl)a?\(\s*[^,/\s]+[\s,]+[^,/\s]+[\s,]+[^,/\s]+\s*[,/]\s*([\d.]+)(%?)\s*\)$/);
  return alpha ? parseFloat(alpha[1]) / (alpha[2] ? 100 : 1) : 1;
}

// Margins are rendered through --appMargin* custom properties (getStyle); the Vuetify spacing classes that
// older layouts stored for the same margins are dropped at render time so that they do not apply twice

export function getStyle(container, options) {
  if (options.pageBodyBackground && container.backgroundColor && getColorAlpha(container.backgroundColor) === 0) {
    // A fully transparent page colour (stored with a gradient, for instance) is no colour of its own: the body keeps
    // the site or branding colour, which still shows behind the Topbar
    container = {
      ...container,
      backgroundColor: null,
    };
  }
  const style = {};
  const backgroundLayerValues = !options.noBackgroundStyle && parseBackgroundLayerValues(container.cssClass);
  if (backgroundLayerValues) {
    style.position = 'relative';
  }
  if (container.marginTop === 0
    || container.marginTop
    || container.marginBottom === 0
    || container.marginBottom
    || container.marginLeft === 0
    || container.marginLeft
    || container.marginRight === 0
    || container.marginRight) {
    if (options.siteStyle) {
      if (container.marginTop === 0 || container.marginTop) {
        document.body.style.setProperty('--allPagesMarginTop', `${container.marginTop}px`);
        document.body.style.setProperty('--allPagesNoMarginTop', '0px');
      }
      if (container.marginRight === 0 || container.marginRight) {
        document.body.style.setProperty('--allPagesMarginRight', `${container.marginRight}px`);
      }
      if (container.marginBottom === 0 || container.marginBottom) {
        document.body.style.setProperty('--allPagesMarginBottom', `${container.marginBottom}px`);
        document.body.style.setProperty('--allPagesNoMarginBottom', '0px');
      }
      if (container.marginLeft === 0 || container.marginLeft) {
        document.body.style.setProperty('--allPagesMarginLeft', `${container.marginLeft}px`);
      }
    } else if (options.pageStyle) {
      if (container.marginTop === 0 || container.marginTop) {
        style['--allPagesMarginTop'] = `${container.marginTop}px`;
        style['--allPagesNoMarginTop'] = '0px';
      }
      if (container.marginRight === 0 || container.marginRight) {
        style['--allPagesMarginRight'] = `${container.marginRight}px`;
      }
      if (container.marginBottom === 0 || container.marginBottom) {
        style['--allPagesMarginBottom'] = `${container.marginBottom}px`;
        style['--allPagesNoMarginBottom'] = '0px';
      }
      if (container.marginLeft === 0 || container.marginLeft) {
        style['--allPagesMarginLeft'] = `${container.marginLeft}px`;
      }
    } else if (options.sectionStyle && !options.noSectionMargins) {
      const diff = container.template === 'Banner' || container.template === 'BannerCell' || container.template === 'Sidebar' || container.template === 'SidebarCell' ? 0 : 10;
      if (container.marginTop) {
        style['--sectionMarginTop'] = `${container.marginTop + diff}px`;
      }
      if (container.marginRight) {
        style['--sectionMarginRight'] = `${container.marginRight + diff}px`;
      }
      if (container.marginBottom) {
        style['--sectionMarginBottom'] = `${container.marginBottom + diff}px`;
      }
      if (container.marginLeft) {
        style['--sectionMarginLeft'] = `${container.marginLeft + diff}px`;
      }
    }
  }
  if (container.textTitleColor) {
    style['--appTextTitleColor'] = container.textTitleColor;
  }
  if (container.textTitleFontSize) {
    style['--appTextTitleFontSize'] = container.textTitleFontSize;
  }
  if (container.textTitleFontWeight) {
    style['--appTextTitleFontWeight'] = container.textTitleFontWeight;
  }
  if (container.textTitleFontStyle) {
    style['--appTextTitleFontStyle'] = container.textTitleFontStyle;
  }
  if (container.textColor) {
    style['--appTextColor'] = container.textColor;
  }
  if (container.iconColor) {
    style['--appIconColor'] = container.iconColor;
  }
  if (container.textFontSize) {
    style['--appTextFontSize'] = container.textFontSize;
  }
  if (container.textFontWeight) {
    style['--appTextFontWeight'] = container.textFontWeight;
  }
  if (container.textFontStyle) {
    style['--appTextFontStyle'] = container.textFontStyle;
  }
  if (container.textHeaderColor) {
    style['--appTextHeaderColor'] = container.textHeaderColor;
  }
  if (container.textHeaderFontSize) {
    style['--appTextHeaderFontSize'] = container.textHeaderFontSize;
  }
  if (container.textHeaderFontWeight) {
    style['--appTextHeaderFontWeight'] = container.textHeaderFontWeight;
  }
  if (container.textHeaderFontStyle) {
    style['--appTextHeaderFontStyle'] = container.textHeaderFontStyle;
  }
  if (container.textSubtitleColor) {
    style['--appTextSubtitleColor'] = container.textSubtitleColor;
  }
  if (container.textSubtitleFontSize) {
    style['--appTextSubtitleFontSize'] = container.textSubtitleFontSize;
  }
  if (container.textSubtitleFontWeight) {
    style['--appTextSubtitleFontWeight'] = container.textSubtitleFontWeight;
  }
  if (container.textSubtitleFontStyle) {
    style['--appTextSubtitleFontStyle'] = container.textSubtitleFontStyle;
  }
  if (container.textTitleBackgroundColor) {
    style['--appTextTitleBackgroundColor'] = container.textTitleBackgroundColor;
  }
  if (container.textTitleBackgroundImage) {
    style['--appTextTitleBackgroundImage'] = `url(${container.textTitleBackgroundImage})`;
  }
  if (container.textTitleBackgroundEffect) {
    style['--appTextTitleBackgroundImage'] = container.textTitleBackgroundImage
      ? `url(${container.textTitleBackgroundImage}),${container.textTitleBackgroundEffect}`
      : container.textTitleBackgroundEffect;
  }
  if (container.textTitleBackgroundPosition) {
    style['--appTextTitleBackgroundPosition'] = container.textTitleBackgroundPosition;
  }
  if (container.textTitleBackgroundSize) {
    style['--appTextTitleBackgroundSize'] = container.textTitleBackgroundSize;
  }
  if (container.textTitleBackgroundRepeat) {
    style['--appTextTitleBackgroundRepeat'] = container.textTitleBackgroundRepeat;
  }
  if (container.textTitleBackgroundPadding) {
    setTextBackgroundPaddingSides(style, 'appTextTitleBackgroundPadding', container.textTitleBackgroundPadding);
  }
  if (container.textTitleBackgroundRadius) {
    style['--appTextTitleBackgroundRadius'] = container.textTitleBackgroundRadius;
  }
  if (container.textHeaderBackgroundColor) {
    style['--appTextHeaderBackgroundColor'] = container.textHeaderBackgroundColor;
  }
  if (container.textHeaderBackgroundImage) {
    style['--appTextHeaderBackgroundImage'] = `url(${container.textHeaderBackgroundImage})`;
  }
  if (container.textHeaderBackgroundEffect) {
    style['--appTextHeaderBackgroundImage'] = container.textHeaderBackgroundImage
      ? `url(${container.textHeaderBackgroundImage}),${container.textHeaderBackgroundEffect}`
      : container.textHeaderBackgroundEffect;
  }
  if (container.textHeaderBackgroundPosition) {
    style['--appTextHeaderBackgroundPosition'] = container.textHeaderBackgroundPosition;
  }
  if (container.textHeaderBackgroundSize) {
    style['--appTextHeaderBackgroundSize'] = container.textHeaderBackgroundSize;
  }
  if (container.textHeaderBackgroundRepeat) {
    style['--appTextHeaderBackgroundRepeat'] = container.textHeaderBackgroundRepeat;
  }
  if (container.textHeaderBackgroundPadding) {
    setTextBackgroundPaddingSides(style, 'appTextHeaderBackgroundPadding', container.textHeaderBackgroundPadding);
  }
  if (container.textHeaderBackgroundRadius) {
    style['--appTextHeaderBackgroundRadius'] = container.textHeaderBackgroundRadius;
  }
  if (container.textBackgroundColor) {
    style['--appTextBackgroundColor'] = container.textBackgroundColor;
  }
  if (container.textBackgroundImage) {
    style['--appTextBackgroundImage'] = `url(${container.textBackgroundImage})`;
  }
  if (container.textBackgroundEffect) {
    style['--appTextBackgroundImage'] = container.textBackgroundImage
      ? `url(${container.textBackgroundImage}),${container.textBackgroundEffect}`
      : container.textBackgroundEffect;
  }
  if (container.textBackgroundPosition) {
    style['--appTextBackgroundPosition'] = container.textBackgroundPosition;
  }
  if (container.textBackgroundSize) {
    style['--appTextBackgroundSize'] = container.textBackgroundSize;
  }
  if (container.textBackgroundRepeat) {
    style['--appTextBackgroundRepeat'] = container.textBackgroundRepeat;
  }
  if (container.textBackgroundPadding) {
    setTextBackgroundPaddingSides(style, 'appTextBackgroundPadding', container.textBackgroundPadding);
  }
  if (container.textBackgroundRadius) {
    style['--appTextBackgroundRadius'] = container.textBackgroundRadius;
  }
  if (container.textSubtitleBackgroundColor) {
    style['--appTextSubtitleBackgroundColor'] = container.textSubtitleBackgroundColor;
  }
  if (container.textSubtitleBackgroundImage) {
    style['--appTextSubtitleBackgroundImage'] = `url(${container.textSubtitleBackgroundImage})`;
  }
  if (container.textSubtitleBackgroundEffect) {
    style['--appTextSubtitleBackgroundImage'] = container.textSubtitleBackgroundImage
      ? `url(${container.textSubtitleBackgroundImage}),${container.textSubtitleBackgroundEffect}`
      : container.textSubtitleBackgroundEffect;
  }
  if (container.textSubtitleBackgroundPosition) {
    style['--appTextSubtitleBackgroundPosition'] = container.textSubtitleBackgroundPosition;
  }
  if (container.textSubtitleBackgroundSize) {
    style['--appTextSubtitleBackgroundSize'] = container.textSubtitleBackgroundSize;
  }
  if (container.textSubtitleBackgroundRepeat) {
    style['--appTextSubtitleBackgroundRepeat'] = container.textSubtitleBackgroundRepeat;
  }
  if (container.textSubtitleBackgroundPadding) {
    setTextBackgroundPaddingSides(style, 'appTextSubtitleBackgroundPadding', container.textSubtitleBackgroundPadding);
  }
  if (container.textSubtitleBackgroundRadius) {
    style['--appTextSubtitleBackgroundRadius'] = container.textSubtitleBackgroundRadius;
  }
  if (!options.onlyBackgroundStyle) {
    if (container.height) {
      if (options.appStyle) {
        if (options.sectionStyle) {
          style['height'] = hasUnit(container.height) ? container.height : `${container.height}px`;
          style['min-height'] = style['height'];
          style['max-height'] = style['height'];
        }
        if (!options.noApplicationHeight) {
          style['--appHeight'] = hasUnit(container.height) ? container.height : `${container.height}px`;
        }
      } else {
        style['height'] = hasUnit(container.height) ? container.height : `${container.height}px`;
      }
      if (options.isApplicationScroll) {
        style['--appHeightScroll'] = 'auto';
        style['--appWidthScroll'] = 'hidden';
      }
    }
    if (container.width) {
      if (options.siteStyle) {
        if (container.width === 'fullWindow') {
          document.body.style.setProperty('--allPagesWidth', '100%');
        } else if (container.width === 'singlePageApplication') {
          document.body.style.setProperty('--allPagesWidth', '1320px');
        } else {
          document.body.style.setProperty('--allPagesWidth', hasUnit(container.width) ? container.width : `${container.width}px`);
        }
      } else if (options.pageStyle) {
        if (container.width === 'fullWindow') {
          style['--allPagesWidth'] = '100%';
        } else if (container.width === 'singlePageApplication') {
          style['--allPagesWidth'] = '1320px';
        } else {
          style['--allPagesWidth'] = hasUnit(container.width) ? container.width : `${container.width}px`;
        }
      } else if (options.appStyle) {
        if (options.dynamicWidth) {
          style['min-width'] = 'auto';
          style['width'] = hasUnit(container.width) ? container.width : `${container.width}px`;
          style['max-width'] = `min(${options.dynamicWidth}, ${style['width']})`;
          style['--appWidth'] = '100%';
        } else {
          if (options.sectionStyle) {
            style['width'] = hasUnit(container.width) ? container.width : `${container.width}px`;
            style['min-width'] = style['width'];
            style['max-width'] = style['width'];
          }
          if (!options.noApplicationWidth) {
            style['--appWidth'] = hasUnit(container.width) ? container.width : `${container.width}px`;
          }
        }
      } else {
        style['width'] = hasUnit(container.width) ? container.width : `${container.width}px`;
      }
      if (options.isApplicationScroll) {
        style['--appWidthScroll'] = 'auto';
      }
    }
    // Application and page levels write the applications' border variables (a page's "Application styling > Border"
    // is the default of its applications, inherited by custom property); sections paint their own border
    const applicationBorder = options.appStyle || options.pageStyle;
    if (container.borderColor) {
      style[applicationBorder && '--appBorderColor' || 'border-color'] = container.borderColor;
      if (container.borderSize) {
        style[applicationBorder && '--appBorderSize' || 'border-size'] = `${container.borderSize}px`;
      }
    }
    if (container.boxShadow === 'true') {
      style[applicationBorder && '--appBoxShadow' || 'box-shadow'] = '0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14), 0px 1px 8px 0px rgba(0, 0, 0, 0.12)';
    }
  }
  // The site background, and the page background when the page is displayed under a transparent top banner
  // (pageBodyBackground), are written as the body-level --allPagesBackground* properties: they then also paint behind
  // the Topbar and the banners, which sit outside the page body, so the Topbar shows the background of the current page.
  // The page containers render after the site container, so a page background overrides the site one.
  // Under a transparent shared Topbar (pageBodyBackgroundColor), an opaque page colour is written to the body as well:
  // that Topbar is above the site scroller, where the body-level image never paints. A page image or gradient is
  // painted by the body itself then (setSharedTopBarPageImage).
  const bodyBackground = options.siteStyle || options.pageBodyBackground;
  if (!options.noBackgroundStyle
    && !backgroundLayerValues
    && (container.backgroundImage
      || container.backgroundColor
      || container.backgroundEffect)) {
    if (bodyBackground) {
      siteBackgroundOverridden = true;
    }
    if (container.backgroundColor) {
      if (bodyBackground) {
        document.body.style.setProperty('--allPagesBackgroundColor', container.backgroundColor);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundColor'] = container.backgroundColor;
        // Only an opaque colour: the page body paints it again over the body, which would deepen a partly
        // transparent one inside the page width
        if (options.pageBodyBackgroundColor && getColorAlpha(container.backgroundColor) === 1) {
          siteBackgroundOverridden = true;
          document.body.style.setProperty('--allPagesBackgroundColor', container.backgroundColor);
        }
      } else if (options.isApplicationBackground) {
        style['--appBackgroundColor'] = container.backgroundColor;
      } else if (options.sectionStyle) {
        if (container.backgroundColor?.includes?.('@')) {
          const colors = container.backgroundColor.split('@');
          style['background-color'] = colors[0];
          style['--sectionBackgroundColorScroll'] = colors[1];
        } else {
          style['background-color'] = container.backgroundColor;
          // Colour kept once the section is fixed on scroll: the section's own, whether it is fixed by its
          // own option or by the platform-wide Topbar default (body.topbar-sticky); a no-op when nothing is sticky
          style['--sectionBackgroundColorScroll'] = container.backgroundColor;
        }
        if (isTopBanner(container) && container.backgroundEffect) {
          const fallbackColor = getTopBannerGradientColor(container) || 'var(--allPagesTopBarBackgroundColor)';
          style['background-color'] = fallbackColor;
          style['--sectionBackgroundColorScroll'] = fallbackColor;
        }
      }
    } else if (container.backgroundEffect || container.backgroundImage) {
      if (bodyBackground) {
        // Without a colour of its own, a page leaves the site or branding colour, which still shows behind the Topbar
        if (!options.pageBodyBackground) {
          document.body.style.setProperty('--allPagesBackgroundColor', 'transparent');
        }
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundColor'] = 'transparent';
      } else if (options.sectionStyle) {
        style['background-color'] = 'transparent';
      } else {
        style['--appBackgroundColor'] = 'transparent';
      }
    }

    const backgroundEffect = isTopBanner(container) ? null : container.backgroundEffect;
    if (backgroundEffect && container.backgroundImage) {
      // Behind a transparent top banner the site layer is the body, under the page background that overrides it there
      if (options.pageBodyBackground || (options.siteStyle && options.topBannerTransparency)) {
        document.body.style.setProperty('--allPagesBackgroundImage', `url(${container.backgroundImage}),${backgroundEffect}`);
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage}),${backgroundEffect}`;
      }
    } else if (container.backgroundImage) {
      if (bodyBackground) {
        document.body.style.setProperty('--allPagesBackgroundImage', `url(${container.backgroundImage})`);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = `url(${container.backgroundImage})`;
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage})`;
      }
    } else if (backgroundEffect) {
      if (bodyBackground) {
        document.body.style.setProperty('--allPagesBackgroundImage', container.backgroundEffect);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = container.backgroundEffect;
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = container.backgroundEffect;
      }
    } else if (container.backgroundColor) {
      if (bodyBackground) {
        document.body.style.setProperty('--allPagesBackgroundImage', 'none');
        document.body.style.setProperty('--allPagesBackgroundRepeat', 'no-repeat');
        document.body.style.setProperty('--allPagesBackgroundSize', 'unset');
        document.body.style.setProperty('--allPagesBackgroundPosition', 'unset');
        document.body.style.setProperty('--allPagesBackgroundAttachment', 'scroll');
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = 'none';
        style['--pageBodyBackgroundRepeat'] = 'no-repeat';
        style['--pageBodyBackgroundSize'] = 'unset';
        style['--pageBodyBackgroundPosition'] = 'unset';
        style['--pageBodyBackgroundAttachment'] = 'scroll';
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = 'none';
        style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = 'no-repeat';
        style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = 'unset';
        style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = 'unset';
        style[options.isApplicationBackground && '--appBackgroundAttachment' || 'background-attachment'] = 'scroll';
      }
    }
    if (options.pageBodyBackground && (container.backgroundImage || backgroundEffect)) {
      // The body may hold the site's image properties: an unset page property takes the page body defaults
      document.body.style.setProperty('--allPagesBackgroundRepeat', 'no-repeat');
      document.body.style.setProperty('--allPagesBackgroundSize', 'unset');
      document.body.style.setProperty('--allPagesBackgroundPosition', 'unset');
      document.body.style.setProperty('--allPagesBackgroundAttachment', siteBackgroundAttachment('scroll'));
    }
    if (container.backgroundImage) {
      if (container.backgroundRepeat) {
        if (bodyBackground) {
          document.body.style.setProperty('--allPagesBackgroundRepeat', container.backgroundRepeat);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundRepeat'] = container.backgroundRepeat;
        } else {
          style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = container.backgroundRepeat;
        }
      }
      if (container.backgroundSize) {
        if (bodyBackground) {
          document.body.style.setProperty('--allPagesBackgroundSize', container.backgroundSize);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundSize'] = container.backgroundSize;
        } else {
          style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = container.backgroundSize;
        }
      }
      if (container.backgroundPosition) {
        if (bodyBackground) {
          document.body.style.setProperty('--allPagesBackgroundPosition', container.backgroundPosition);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundPosition'] = container.backgroundPosition;
        } else {
          style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = container.backgroundPosition;
        }
      }
      if (container.backgroundAttachment) {
        if (bodyBackground) {
          document.body.style.setProperty('--allPagesBackgroundAttachment', siteBackgroundAttachment(container.backgroundAttachment));
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundAttachment'] = container.backgroundAttachment;
        } else {
          style[options.isApplicationBackground && '--appBackgroundAttachment' || 'background-attachment'] = container.backgroundAttachment;
        }
      }
    }
    if (options.pageStyle && options.pageBodyBackgroundColor) {
      setSharedTopBarPageImage(container, style, options.pageBodyBackground);
    }
  } else if (backgroundLayerValues && options.isApplicationBackground) {
    // The real visible application background paints on the descendant
    // .PORTLET-FRAGMENT element (via these inherited custom properties, per
    // the platform-ui-skin LESS rules), not on this element itself. Leaving
    // them merely unset lets that descendant fall back to its own opaque
    // default background, hiding the new background layer behind it -
    // these must be explicitly cleared instead.
    style['--appBackgroundColor'] = 'transparent';
    style['--appBackgroundImage'] = 'none';
    style['--appBackgroundRepeat'] = 'no-repeat';
    style['--appBackgroundSize'] = 'unset';
    style['--appBackgroundPosition'] = 'unset';
    style['--appBackgroundAttachment'] = 'scroll';
  } else if (!options.noBackgroundStyle && !backgroundLayerValues && options.siteStyle && siteBackgroundOverridden) {
    // Only clear these when a previous call on this same page actually set them:
    // the server (UIPortalApplication.gtmpl) writes them as inline <body> style from
    // the branding admin's default page background, and blindly removing them here
    // on every render with no site-level override would wipe out that branding
    // default instead of letting it show through.
    document.body.style.removeProperty('--allPagesBackgroundColor');
    document.body.style.removeProperty('--allPagesBackgroundImage');
    document.body.style.removeProperty('--allPagesBackgroundRepeat');
    document.body.style.removeProperty('--allPagesBackgroundSize');
    document.body.style.removeProperty('--allPagesBackgroundPosition');
    document.body.style.removeProperty('--allPagesBackgroundAttachment');
    removeSharedTopBarPageImage();
    siteBackgroundOverridden = false;
  }
  // Margins are custom properties at every level, on the platform scale (20 = no extra margin), rendered by one
  // skin rule: calc(var(--appMargin*, var(--allPagesAppMargin*, 20px)) - 20px). The stored attribute travels as is:
  // a legacy application (spacing tokens) is converted once, server-side, by LayoutModel before it reaches here.
  // Page level (pageStyle): default margins of the applications of the page (appMargin*), inherited by every
  // application below. Application level (applicationMargins, set by the application renderers only): its own
  // margins (margin*), on its own element. Sections, cells and the site container carry margin* fields of their
  // own scale and must not write --appMargin*, which their applications would inherit.
  // pageAppMargins: the page editor renders the page container with the site style and passes this option so that
  // the Edit Page margins preview there as they render at view time (pageStyle).
  if (options.applicationMargins || options.pageStyle || options.pageAppMargins) {
    ['Top', 'Right', 'Bottom', 'Left'].forEach(side => {
      const margin = options.applicationMargins ? container[`margin${side}`] : container[`appMargin${side}`];
      if (margin === 0 || margin) {
        style[`--appMargin${side}`] = `${Number(margin)}px`;
      }
    });
  }
  if (container.appBackgroundColor) {
    style['--appBackgroundColor'] = container.appBackgroundColor;
  }
  if (container.appBackgroundEffect && container.appBackgroundImage) {
    style['--appBackgroundImage'] = `url(${container.appBackgroundImage}),${container.appBackgroundEffect}`;
  } else if (container.appBackgroundImage) {
    style['--appBackgroundImage'] = `url(${container.appBackgroundImage})`;
  } else if (container.appBackgroundEffect) {
    style['--appBackgroundImage'] = container.appBackgroundEffect;
  }
  if (container.appBackgroundImage) {
    if (container.appBackgroundRepeat) {
      style['--appBackgroundRepeat'] = container.appBackgroundRepeat;
    }
    if (container.appBackgroundSize) {
      style['--appBackgroundSize'] = container.appBackgroundSize;
    }
    if (container.appBackgroundPosition) {
      style['--appBackgroundPosition'] = container.appBackgroundPosition;
    }
    if (container.appBackgroundAttachment) {
      style['--appBackgroundAttachment'] = container.appBackgroundAttachment;
    }
  }
  if (container.radiusTopRight || container.radiusTopRight === 0) {
    style['--appBorderRadiusTopRight'] = `${container.radiusTopRight}px`;
  }
  if (container.radiusTopLeft || container.radiusTopLeft === 0) {
    style['--appBorderRadiusTopLeft'] = `${container.radiusTopLeft}px`;
  }
  if (container.radiusBottomRight || container.radiusBottomRight === 0) {
    style['--appBorderRadiusBottomRight'] = `${container.radiusBottomRight}px`;
  }
  if (container.radiusBottomLeft || container.radiusBottomLeft === 0) {
    style['--appBorderRadiusBottomLeft'] = `${container.radiusBottomLeft}px`;
  }
  return style;
}

// The cssClass token helpers of the background layer (EXIP-88427) live in social's shared 'stylingUtils'
// module (Vue.prototype.$stylingUtils), consumed by the styling inputs and by this renderer
export function parseBackgroundLayerValues(cssClass) {
  return Vue.prototype.$stylingUtils.parseBackgroundLayerValues(cssClass);
}

export function setBackgroundLayerValues(container, partialValues) {
  return Vue.prototype.$stylingUtils.setBackgroundLayerValues(container, partialValues);
}

// The site background paints on the scroll container itself (see the
// .site-scroll-parent rule in platform-ui's reset.less), where 'scroll' means
// "fixed with regard to that box" and so looks identical to 'fixed'. 'local' is
// the value that makes the background follow the scrolled content there. Page,
// section and app backgrounds paint on elements *inside* that scroller, which
// physically move, so they keep 'scroll' untranslated.
function siteBackgroundAttachment(attachment) {
  return attachment === 'scroll' ? 'local' : attachment;
}

const SHARED_TOP_BAR_PAGE_IMAGE_PROPERTIES = ['background-image', 'background-repeat', 'background-size', 'background-position'];

// Under a transparent shared Topbar the page image and gradient are painted by the body element itself: the shared
// Topbar is above the site scroller, which paints the body-level image (--allPagesBackgroundImage) only below it.
// The scroller and the page body then paint no image over it, and the page colour moves under it to the body: an
// opaque one as the body colour (getStyle), a partly transparent one as the lowest layer, over the site or Branding
// colour as the page body painted it. The layer is sized and positioned against the viewport, whatever the page's
// attachment: it stays fixed on a desktop, where the body does not scroll, and scrolls with the content on a tablet
// or a phone. A partly transparent page colour without an image or a gradient moves to the body the same way, unless
// the scroller paints a site or Branding image, which would cover it there. Any other page background removes the
// layer an earlier page container wrote.
function setSharedTopBarPageImage(container, style, topBannerTransparency) {
  const layers = getPageImageLayers(container);
  const colorAlpha = !topBannerTransparency && container.backgroundColor && getColorAlpha(container.backgroundColor);
  const colorLayer = colorAlpha > 0 && colorAlpha < 1 && (layers.length > 0 || !hasBodyBackgroundImage());
  if (!layers.length && !colorLayer) {
    removeSharedTopBarPageImage();
    return;
  }
  const repeat = container.backgroundImage && container.backgroundRepeat || 'no-repeat';
  const size = container.backgroundImage && container.backgroundSize || 'auto';
  const position = container.backgroundImage && container.backgroundPosition || '0% 0%';
  const layerValues = layers.map(image => ({image, repeat, size, position}));
  if (!topBannerTransparency) {
    // Under a transparent top banner the colour is already on the body and the page body paints nothing
    style['--pageBodyBackgroundImage'] = 'none';
    style['--pageBodyBackgroundColor'] = 'transparent';
    if (colorLayer) {
      layerValues.push({
        image: `linear-gradient(${container.backgroundColor},${container.backgroundColor})`,
        repeat: 'repeat',
        size: 'auto',
        position: '0% 0%',
      });
    }
  }
  siteBackgroundOverridden = true;
  document.body.style.setProperty('background-image', layerValues.map(layer => layer.image).join(','));
  document.body.style.setProperty('background-repeat', layerValues.map(layer => layer.repeat).join(','));
  document.body.style.setProperty('background-size', layerValues.map(layer => layer.size).join(','));
  document.body.style.setProperty('background-position', layerValues.map(layer => layer.position).join(','));
  document.body.style.setProperty('--allPagesBackgroundImage', 'none');
}

function removeSharedTopBarPageImage() {
  SHARED_TOP_BAR_PAGE_IMAGE_PROPERTIES.forEach(property => document.body.style.removeProperty(property));
}

function hasBodyBackgroundImage() {
  const image = window.getComputedStyle(document.body).getPropertyValue('--allPagesBackgroundImage').trim();
  return !!image && image !== 'none';
}

function getPageImageLayers(container) {
  return [
    container.backgroundImage && `url(${container.backgroundImage})`,
    container.backgroundEffect,
  ].filter(layer => layer);
}

export function getBackgroundLayerStyle(container, options) {
  if (options?.noBackgroundStyle) {
    return null;
  }
  const values = parseBackgroundLayerValues(container?.cssClass);
  if (!values) {
    return null;
  }
  const style = {
    position: 'absolute',
    top: `${values.marginTop || 0}px`,
    right: `${values.marginRight || 0}px`,
    bottom: `${values.marginBottom || 0}px`,
    left: `${values.marginLeft || 0}px`,
    'border-radius': `${values.radiusTopLeft || 0}px ${values.radiusTopRight || 0}px ${values.radiusBottomRight || 0}px ${values.radiusBottomLeft || 0}px`,
    'z-index': -1,
    'pointer-events': 'none',
  };
  if (container.backgroundColor) {
    style['background-color'] = container.backgroundColor.includes?.('@') ? container.backgroundColor.split('@')[0] : container.backgroundColor;
  } else if (container.backgroundEffect || container.backgroundImage) {
    style['background-color'] = 'transparent';
  }
  if (container.backgroundEffect && container.backgroundImage) {
    style['background-image'] = `url(${container.backgroundImage}),${container.backgroundEffect}`;
  } else if (container.backgroundImage) {
    style['background-image'] = `url(${container.backgroundImage})`;
  } else if (container.backgroundEffect) {
    style['background-image'] = container.backgroundEffect;
  }
  if (container.backgroundImage) {
    if (container.backgroundRepeat) {
      style['background-repeat'] = container.backgroundRepeat;
    }
    if (container.backgroundSize) {
      style['background-size'] = container.backgroundSize;
    }
    if (container.backgroundPosition) {
      style['background-position'] = container.backgroundPosition;
    }
    if (container.backgroundAttachment) {
      style['background-attachment'] = container.backgroundAttachment;
    }
  }
  return style;
}

export function getApplicationContent(navUri, applicationStorageId, applicationMode, showSite, fullRender) {
  const spaceOption = eXo.env.portal.previewSpaceId && `&previewSpaceId=${eXo.env.portal.previewSpaceId}` || '';
  const fullRenderOption = fullRender && '&fullRender=true' || '';
  return fetch(`/portal${navUri}?maximizedPortletId=${applicationStorageId}&showMaxWindow=${!showSite}&hideSharedLayout=true&maximizedPortletMode=${applicationMode || 'VIEW'}${spaceOption}${fullRenderOption}`, {
    credentials: 'include',
    method: 'GET',
    redirect: 'manual'
  })
    .then(resp => {
      if (resp?.status === 200) {
        return resp.text();
      } else {
        throw new Error('The retrieved page is not a portal page');
      }
    });
}

export function handleApplicationContent(applicationContent, applicationElement) {
  const newHeadContent = applicationContent.search('<head') > -1 && applicationContent.substring(applicationContent.search('<head') + applicationContent.match(/<head.*>/g)[0].length, applicationContent.search('</head>')) || '';
  let newBodyContent = applicationContent.substring(applicationContent.search('<body') + applicationContent.match(/<body.*>/g)[0].length, applicationContent.lastIndexOf('</body>'));
  newBodyContent = installNewCSS(newHeadContent, newBodyContent);

  const newHtmlDocument = document.createElement('div');
  newHtmlDocument.innerHTML = newBodyContent;
  const portletContent = newHtmlDocument.querySelector('.PORTLET-FRAGMENT');
  if (!portletContent) {
    applicationElement.classList.add('hidden');
    return;
  }
  const oldPortletContent = applicationElement.querySelector('.PORTLET-FRAGMENT');
  if (oldPortletContent) {
    oldPortletContent.remove();
  }
  applicationElement.append(portletContent);

  installNewJS(newHeadContent);
  installNewJS(newBodyContent, 'jsManager', true);

  window.setTimeout(() => {
    // The innerHTML change doesn't interpret script element
    // thus force it by replacing nodes and creating them instead
    replaceScriptElements(applicationElement);
  }, 10);
}

function installNewCSS(newHeadContent, newBodyContent) {
  const headCSSLinks = newHeadContent.match(/<link.*id=".*".*>/g);
  if (headCSSLinks?.length) {
    headCSSLinks.forEach(link => {
      const id = link.match(/id="([^"]*)"/i)[1];
      if (!document.querySelector(`#${id}`)) {
        const skinTypeResult = link.match(/skin-type="([a-z]*-skin)"/);
        if (skinTypeResult && skinTypeResult.length === 2) {
          const skinType = skinTypeResult[1];
          let skinElements = document.querySelectorAll(`[skin-type="${skinType}"]`);
          if (!skinElements.length) {
            skinElements = document.querySelectorAll('[skin-type="portal-skin"]');
          }
          if (!skinElements.length) {
            skinElements = document.querySelectorAll('[skin-type="custom-skin"]');
          }
          // Install new CSS in the same skin files categories (portal, portlet or custom)
          const skinElement = skinElements[skinElements.length - 1];
          const linkElement = document.createElement('div');
          linkElement.innerHTML = link;
          skinElement.after(linkElement.childNodes[0]);
        }
      }
    });
  }
  const bodyCSSLinks = newBodyContent.match(/<link.*id=".*".*>/g);
  if (bodyCSSLinks && bodyCSSLinks.length) {
    bodyCSSLinks.forEach(link => {
      const id = link.match(/id="([^"]*)"/i)[1];
      if (!document.querySelector(`#${id}`)) {
        const linkElement = document.createElement('div');
        linkElement.innerHTML = link;
        // Install new CSS from newly downloaded body
        document.head.append(linkElement.childNodes[0]);
      }
      newBodyContent = newBodyContent.replace(link, '');
    });
  }
  return newBodyContent;
}

function installNewJS(scriptsContent, specificId, forceReload) {
  const replacableScriptsIterator = scriptsContent.matchAll(/<script[^>]*id="[^>]*"[^>]*>/g);
  let scriptIteratorElement = replacableScriptsIterator.next().value;
  while (scriptIteratorElement) {
    const script = scriptIteratorElement[0];
    const id = script.match(/id="([^"]*)"/i)[1];
    let scriptElement = ((specificId && specificId === id) || id) && (document.querySelector(`#${id.trim()}`) || document.querySelector(`[data-id=${id.trim()}]`));
    if (!scriptElement || forceReload) {
      const scriptContent = scriptsContent.substring(scriptIteratorElement.index, scriptsContent.indexOf('</script>', scriptIteratorElement.index));
      scriptElement = document.createElement('script');
      if (id) {
        scriptElement.setAttribute('id', id);
      }
      scriptElement.innerText = scriptContent.substring(scriptContent.indexOf('>') + 1).replace(/(\r)?(\n)?/g, '');
      document.head.append(scriptElement);
    }
    scriptIteratorElement = replacableScriptsIterator.next().value;
  }
}

function replaceScriptElements(node) {
  if (node.tagName === 'SCRIPT') {
    node.parentNode.replaceChild(cloneScriptElement(node), node);
  } else {
    for (let i = 0; i < node.childNodes.length; i++) {
      replaceScriptElements(node.childNodes[i]);
    }
  }
  return node;
}

function cloneScriptElement(node) {
  const scriptElement  = document.createElement('script');
  scriptElement.innerText = node.innerHTML.replace(/<br>/g, '');
  const scriptAttrs = node.attributes;
  for (let i = 0; i < scriptAttrs.length; i++)  {
    scriptElement.setAttribute(scriptAttrs[i].name, scriptAttrs[i].value);
  }
  return scriptElement;
}

function hasUnit(length) {
  return Number.isNaN(Number(length));
}

// The text background "Margin" option insets the background layer away from
// the text edges without moving the text itself (same intent as the
// dedicated app/section background layer from EXIP-88427), so it's painted
// on a ::before positioned behind the text via negative top/right/bottom/left
// offsets - which needs each side split out instead of the single CSS
// padding shorthand string the value is stored/edited as.
function setTextBackgroundPaddingSides(style, cssVarPrefix, padding) {
  const [top, right, bottom, left] = padding.trim().split(/\s+/);
  style[`--${cssVarPrefix}Top`] = top;
  style[`--${cssVarPrefix}Right`] = right;
  style[`--${cssVarPrefix}Bottom`] = bottom;
  style[`--${cssVarPrefix}Left`] = left;
}
