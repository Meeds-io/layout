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

export function getStyle(container, options) {
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
    if (container.borderColor) {
      style[options.appStyle && '--appBorderColor' || 'border-color'] = container.borderColor;
      if (container.borderSize) {
        style[options.appStyle && '--appBorderSize' || 'border-size'] = `${container.borderSize}px`;
      }
    }
    if (container.boxShadow === 'true') {
      style[options.appStyle && '--appBoxShadow' || 'box-shadow'] = '0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14), 0px 1px 8px 0px rgba(0, 0, 0, 0.12)';
    }
  }
  if (!options.noBackgroundStyle
    && !backgroundLayerValues
    && (container.backgroundImage
      || container.backgroundColor
      || container.backgroundEffect)) {
    if (options.siteStyle) {
      siteBackgroundOverridden = true;
    }
    if (container.backgroundColor) {
      if (options.siteStyle) {
        document.body.style.setProperty('--allPagesBackgroundColor', container.backgroundColor);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundColor'] = container.backgroundColor;
      } else if (options.isApplicationBackground) {
        style['--appBackgroundColor'] = container.backgroundColor;
      } else if (options.sectionStyle) {
        if (container.backgroundColor?.includes?.('@')) {
          const colors = container.backgroundColor.split('@');
          style['background-color'] = colors[0];
          style['--sectionBackgroundColorScroll'] = colors[1];
        } else {
          style['background-color'] = container.backgroundColor;
          if (container.cssClass?.includes?.('layout-sticky-section')) {
            style['--sectionBackgroundColorScroll'] = container.backgroundColor;
          }
        }
      }
    } else if (container.backgroundEffect || container.backgroundImage) {
      if (options.siteStyle) {
        document.body.style.setProperty('--allPagesBackgroundColor', 'transparent');
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundColor'] = 'transparent';
      } else if (options.sectionStyle) {
        style['background-color'] = 'transparent';
      } else {
        style['--appBackgroundColor'] = 'transparent';
      }
    }

    if (container.backgroundEffect && container.backgroundImage) {
      style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage}),${container.backgroundEffect}`;
    } else if (container.backgroundImage) {
      if (options.siteStyle) {
        document.body.style.setProperty('--allPagesBackgroundImage', `url(${container.backgroundImage})`);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = `url(${container.backgroundImage})`;
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage})`;
      }
    } else if (container.backgroundEffect) {
      if (options.siteStyle) {
        document.body.style.setProperty('--allPagesBackgroundImage', container.backgroundEffect);
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = container.backgroundEffect;
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = container.backgroundEffect;
      }
    } else if (container.backgroundColor) {
      if (options.siteStyle) {
        document.body.style.setProperty('--allPagesBackgroundImage', 'none');
        document.body.style.setProperty('--allPagesBackgroundRepeat', 'no-repeat');
        document.body.style.setProperty('--allPagesBackgroundSize', 'unset');
        document.body.style.setProperty('--allPagesBackgroundPosition', 'unset');
      } else if (options.pageStyle) {
        style['--pageBodyBackgroundImage'] = 'none';
        style['--pageBodyBackgroundRepeat'] = 'no-repeat';
        style['--pageBodyBackgroundSize'] = 'unset';
        style['--pageBodyBackgroundPosition'] = 'unset';
      } else {
        style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = 'none';
        style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = 'no-repeat';
        style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = 'unset';
        style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = 'unset';
      }
    }
    if (container.backgroundImage) {
      if (container.backgroundRepeat) {
        if (options.siteStyle) {
          document.body.style.setProperty('--allPagesBackgroundRepeat', container.backgroundRepeat);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundRepeat'] = container.backgroundRepeat;
        } else {
          style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = container.backgroundRepeat;
        }
      }
      if (container.backgroundSize) {
        if (options.siteStyle) {
          document.body.style.setProperty('--allPagesBackgroundSize', container.backgroundSize);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundSize'] = container.backgroundSize;
        } else {
          style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = container.backgroundSize;
        }
      }
      if (container.backgroundPosition) {
        if (options.siteStyle) {
          document.body.style.setProperty('--allPagesBackgroundPosition', container.backgroundPosition);
        } else if (options.pageStyle) {
          style['--pageBodyBackgroundPosition'] = container.backgroundPosition;
        } else {
          style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = container.backgroundPosition;
        }
      }
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
    siteBackgroundOverridden = false;
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

// Background margin/radius (EXIP-88427) can't be stored as dedicated ModelStyle
// fields since ModelStyle is an external org.exoplatform.portal class we can't
// extend from this repo, so the 8 values are opaquely encoded as cssClass tokens
// (same pattern already used to mirror marginTop/radiusTopRight into mt-/brtr-
// classes, under a prefix that can't collide with those).
const BACKGROUND_LAYER_TOKEN_PREFIXES = {
  marginTop: 'layout-bg-margin-top',
  marginRight: 'layout-bg-margin-right',
  marginBottom: 'layout-bg-margin-bottom',
  marginLeft: 'layout-bg-margin-left',
  radiusTopRight: 'layout-bg-radius-tr',
  radiusTopLeft: 'layout-bg-radius-tl',
  radiusBottomRight: 'layout-bg-radius-br',
  radiusBottomLeft: 'layout-bg-radius-bl',
};

export function parseBackgroundLayerValues(cssClass) {
  if (!cssClass) {
    return null;
  }
  const values = {};
  let found = false;
  Object.entries(BACKGROUND_LAYER_TOKEN_PREFIXES).forEach(([key, prefix]) => {
    const match = cssClass.match(new RegExp(`(?:^| )${prefix}-([0-9]+)(?: |$)`));
    if (match) {
      values[key] = parseInt(match[1]);
      found = true;
    }
  });
  return found ? values : null;
}

export function setBackgroundLayerValues(container, partialValues) {
  // Merge with whatever's already encoded so that the margin component's
  // writes never clobber the radius component's tokens (and vice-versa) -
  // each only knows about its own 4 values, not the other's.
  const values = { ...parseBackgroundLayerValues(container.cssClass), ...partialValues };
  const prefixesPattern = Object.values(BACKGROUND_LAYER_TOKEN_PREFIXES).join('|');
  let cssClass = (container.cssClass || '').replace(new RegExp(`(^| )(${prefixesPattern})-[0-9]+`, 'g'), '').replace(/ {2,}/g, ' ').trim();
  Object.entries(BACKGROUND_LAYER_TOKEN_PREFIXES).forEach(([key, prefix]) => {
    if (values[key] || values[key] === 0) {
      cssClass += ` ${prefix}-${values[key]}`;
    }
  });
  container.cssClass = cssClass.trim();
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
