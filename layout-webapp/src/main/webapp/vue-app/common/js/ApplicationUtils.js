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

export function installApplication(navUri, applicationStorageId, applicationElement, applicationMode) {
  return getApplicationContent(navUri, applicationStorageId, applicationMode)
    .then(applicationContent => handleApplicationContent(applicationContent, applicationElement, applicationMode));
}

export function getStyle(container, options) {
  const style = {};
  if (options.sectionStyle && (container.marginTop || container.marginBottom)) {
    if (container.marginTop) {
      style['--sectionMarginTop'] = `${container.marginTop + 10}px`;
    }
    if (container.marginBottom) {
      style['--sectionMarginBottom'] = `${container.marginBottom + 10}px`;
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
      style[options.isApplicationStyle && '--appHeight' || 'height'] = hasUnit(container.height) ? container.height : `${container.height}px`;
      if (options.isApplicationScroll) {
        style['--appHeightScroll'] = 'auto';
        style['--appWidthScroll'] = 'hidden';
      }
    }
    if (container.width === 'fullWindow') {
      style['--allPagesWidth'] = '100%';
    } else if (container.width === 'singlePageApplication') {
      style['--allPagesWidth'] = '1320px';
    } else if (container.width) {
      style[options.isApplicationStyle && '--appWidth' || 'width'] = hasUnit(container.width) ? container.width : `${container.width}px`;
      if (options.isApplicationScroll) {
        style['--appWidthScroll'] = 'auto';
      }
    }
    if (container.borderColor) {
      style[options.isApplicationStyle && '--appBorderColor' || 'border-color'] = container.borderColor;
      if (container.borderSize) {
        style[options.isApplicationStyle && '--appBorderSize' || 'border-size'] = `${container.borderSize}px`;
      }
    }
    if (container.boxShadow === 'true') {
      style[options.isApplicationStyle && '--appBoxShadow' || 'box-shadow'] = '0px 3px 3px -2px rgba(0, 0, 0, 0.2), 0px 3px 4px 0px rgba(0, 0, 0, 0.14), 0px 1px 8px 0px rgba(0, 0, 0, 0.12)';
    }
  }
  if (!options.noBackgroundStyle
    && (container.backgroundImage
      || container.backgroundColor
      || container.backgroundEffect)) {
    if (container.backgroundColor) {
      style[options.isApplicationBackground && '--appBackgroundColor' || 'background-color'] = container.backgroundColor;
    } else if (container.backgroundEffect || container.backgroundImage) {
      style[options.isApplicationBackground && '--appBackgroundColor' || 'background-color'] = 'transparent';
    }

    if (container.backgroundEffect && container.backgroundImage) {
      style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage}),${container.backgroundEffect}`;
    } else if (container.backgroundImage) {
      style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = `url(${container.backgroundImage})`;
    } else if (container.backgroundEffect) {
      style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = container.backgroundEffect;
    } else if (container.backgroundColor) {
      style[options.isApplicationBackground && '--appBackgroundImage' || 'background-image'] = 'none';
      style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = 'no-repeat';
      style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = 'unset';
      style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = 'unset';
    }
    if (container.backgroundImage) {
      if (container.backgroundRepeat) {
        style[options.isApplicationBackground && '--appBackgroundRepeat' || 'background-repeat'] = container.backgroundRepeat;
      }
      if (container.backgroundSize) {
        style[options.isApplicationBackground && '--appBackgroundSize' || 'background-size'] = container.backgroundSize;
      }
      if (container.backgroundPosition) {
        style[options.isApplicationBackground && '--appBackgroundPosition' || 'background-position'] = container.backgroundPosition;
      }
    }
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

export function getApplicationContent(navUri, applicationStorageId, applicationMode) {
  return fetch(`/portal${navUri}?maximizedPortletId=${applicationStorageId}&showMaxWindow=true&hideSharedLayout=true&maximizedPortletMode=${applicationMode || 'VIEW'}`, {
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
  const newHeadContent = applicationContent.substring(applicationContent.search('<head') + applicationContent.match(/<head.*>/g)[0].length, applicationContent.search('</head>'));
  let newBodyContent = applicationContent.substring(applicationContent.search('<body') + applicationContent.match(/<body.*>/g)[0].length, applicationContent.lastIndexOf('</body>'));
  newBodyContent = installNewCSS(newHeadContent, newBodyContent);

  const newHtmlDocument = document.createElement('div');
  newHtmlDocument.innerHTML = newBodyContent;
  const portletContent = newHtmlDocument.querySelector('.UIWorkingWorkspace .PORTLET-FRAGMENT');
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
          const skinElements = document.querySelectorAll(`[skin-type="${skinType}"]`);
          if (skinElements.length) {
            const linkElement = document.createElement('div');
            linkElement.innerHTML = link;
            // Install new CSS in the same skin files categories (portal, portlet or custom)
            const skinElement = skinElements[skinElements.length - 1];
            skinElement.after(linkElement.childNodes[0]);
          }
        }
      }
    });
  } else {
    throw new Error('The application content does not seem to have been loaded correctly with CSS links');
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
