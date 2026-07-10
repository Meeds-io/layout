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

import { parseBackgroundLayerValues, setBackgroundLayerValues } from '../../common/js/ApplicationUtils.js';

export const breakpoints = ['md', 'lg', 'xl'];

export const currentBreakpoint = 'xl';
export const siteTemplate = 'system:/groovy/portal/webui/container/UISiteLayout.gtmpl';
export const sectionsParentTemplate = 'system:/groovy/portal/webui/container/UIPageLayout.gtmpl';
export const simpleTemplate = 'system:/groovy/portal/webui/container/UIContainer.gtmpl';
export const gridTemplate = 'GridContainer';
export const flexTemplate = 'FlexContainer';
export const cellTemplate = 'CellContainer';
export const siteBodyMiddleTemplate = 'SiteMiddleBody';
export const siteBodyMiddleCenterTemplate = 'SiteMiddleCenterBody';
export const pageBodyTemplate = 'PageBody';
export const sidebarTemplate = 'Sidebar';
export const sidebarCellTemplate = 'SidebarCell';
export const bannerTemplate = 'Banner';
export const bannerCellTemplate = 'BannerCell';

export const defaultMarginTop = 20;
export const defaultMarginRight = 20;
export const defaultMarginBottom = 20;
export const defaultMarginLeft = 20;
export const defaultBorderRadius = 8;
export const defaultBorderColor = '#FFFFFF';
export const defaultBackgroundColor = '#FFFFFF';

export const containerModel = {
  storageId: null,
  storageName: null,
  // Used to add a specific id to the container on display
  // to be able to apply a specific CSS
  // but generally not useful
  id: null,
  // Not used for containers
  name: null,
  // Not used for containers
  icon: null,
  // Used by extensionRegistry mechanism to now which
  // Vue component to use
  template: null,
  // Used for now only for dynamic containers
  factoryId: null,
  // Not used for containers
  title: null,
  // Not used for containers
  description: null,
  // Used to specify the width of the container
  width: null,
  // Used to specify the height of the container
  height: null,
  // Used to specify some special CSS Classes to be used on container parent
  cssClass: null,
  marginTop: null,
  marginRight: null,
  marginBottom: null,
  marginLeft: null,
  borderColor: null,
  borderSize: null,
  boxShadow: null,
  radiusTopRight: null,
  radiusTopLeft: null,
  radiusBottomRight: null,
  radiusBottomLeft: null,
  backgroundColor: null,
  backgroundImage: null,
  backgroundEffect: null,
  backgroundPosition: null,
  backgroundSize: null,
  backgroundRepeat: null,
  appBackgroundColor: null,
  appBackgroundImage: null,
  appBackgroundEffect: null,
  appBackgroundPosition: null,
  appBackgroundSize: null,
  appBackgroundRepeat: null,
  textTitleColor: null,
  textTitleFontSize: null,
  textTitleFontWeight: null,
  textTitleFontStyle: null,
  textHeaderColor: null,
  textHeaderFontSize: null,
  textHeaderFontWeight: null,
  textHeaderFontStyle: null,
  textColor: null,
  textFontSize: null,
  textFontWeight: null,
  textFontStyle: null,
  textSubtitleColor: null,
  textSubtitleFontSize: null,
  textSubtitleFontWeight: null,
  textSubtitleFontStyle: null,
  // Used to specify whether the block should be displayed or not
  // when an addon is present
  profiles: null,
  // Generally kept to be accessible to everyone to make
  // the parent page access permissions applied globally 
  accessPermissions: ['Everyone'],
  // List of children which can be of type:
  // 1. Container
  //   or
  // 2. Application
  children: [],
};

export const applicationModel = {
  storageId: null,
  storageName: null,
  contentId: null,
  id: null,
  title: null,
  icon: null,
  description: null,
  showInfoBar: false,
  showApplicationState: false,
  showApplicationMode: true,
  preferences: null,
  theme: null,
  width: null,
  height: null,
  marginTop: null,
  marginRight: null,
  marginBottom: null,
  marginLeft: null,
  borderColor: null,
  borderSize: null,
  boxShadow: null,
  radiusTopRight: null,
  radiusTopLeft: null,
  radiusBottomRight: null,
  radiusBottomLeft: null,
  backgroundColor: null,
  backgroundImage: null,
  backgroundEffect: null,
  backgroundPosition: null,
  backgroundSize: null,
  backgroundRepeat: null,
  textTitleColor: null,
  textTitleFontSize: null,
  textTitleFontWeight: null,
  textTitleFontStyle: null,
  textHeaderColor: null,
  textHeaderFontSize: null,
  textHeaderFontWeight: null,
  textHeaderFontStyle: null,
  textColor: null,
  textFontSize: null,
  textFontWeight: null,
  textFontStyle: null,
  textSubtitleColor: null,
  textSubtitleFontSize: null,
  textSubtitleFontWeight: null,
  textSubtitleFontStyle: null,
  accessPermissions: ['Everyone'],
};

export const containerModelAttributes = Object.keys(containerModel);

export const applicationModelAttributes = Object.keys(applicationModel);

export const pageJSContextIds = [
  'portalHeadScripts',
  'socialHeadScripts',
  'sitesHeadScripts',
];

export function getQueryParam(paramName) {
  const uri = window.location.search.substring(1);
  const params = new URLSearchParams(uri);
  return params.get(paramName);
}

export function initPageContext(navUri) {
  return fetch(`/portal${navUri}?showMaxWindow=true&hideSharedLayout=true`, {
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
    })
    .then(pageContent => {
      const headContent = pageContent.substring(pageContent.search('<head') + pageContent.match(/<head.*>/g)[0].length, pageContent.search('</head>'));
      installPageContext(headContent);
    })
    .catch(e => console.error('Error navigating to ', navUri, '.', e));
}

export function getParentContainer(layout) {
  if (!layout) {
    return null;
  }
  if (!layout.children?.length) {
    Vue.set(layout, 'children', []);
  }
  if (layout.children[0]?.template === sectionsParentTemplate) {
    return layout.children[0];
  } else {
    return getParentContainer(layout.children?.[0]);
  }
}

export function newParentContainer(layout) {
  return newContainer(sectionsParentTemplate, '', layout, 0);
}

export function getApplications(container, applications) {
  if (!container) {
    return;
  }
  if (!applications) {
    applications = [];
  }
  if (container.contentId) {
    applications.push(container);
  } else if (container.children?.length) {
    container.children.forEach(c => getApplications(c, applications));
  }
  return applications;
}

export function applyMobileStyle(container) {
  if (container.template === gridTemplate || container.template === flexTemplate) {
    container.children?.forEach(applyMobileStyle);
    cleanBreakpointClasses(container, container.template === gridTemplate && 'grid-rows' || null, 'grid-cols');
  } else if (container.template === cellTemplate) {
    cleanBreakpointClasses(container, 'grid-cell-rowspan', 'grid-cell-colspan');
  } else {
    container.children?.forEach?.(applyMobileStyle);
  }
}

export function applyDesktopStyle(container) {
  applyGridStyle(container);
}

export function applyGridStyle(container) {
  if (container.template === gridTemplate || container.template === flexTemplate) {
    container.children?.forEach(c => applyGridStyle(c, container.template));
    applyBreakpointClasses(container, container.template === gridTemplate && 'grid-rows' || null, 'grid-cols');
  } else if (container.template === cellTemplate) {
    applyBreakpointClasses(container, 'grid-cell-rowspan', 'grid-cell-colspan');
  } else {
    container.children?.forEach?.(applyGridStyle);
  }
}

export function applyContainerStyle(container, containerStyle) {
  if (!container.cssClass) {
    container.cssClass = '';
  }

  // Apply new Classes on Section or applications
  if (container.template === flexTemplate
      || container.template === gridTemplate
      || container.template === siteTemplate
      || container.template === bannerTemplate
      || container.template === sidebarTemplate
      || container.template === siteBodyMiddleCenterTemplate
      || container.template === pageBodyTemplate
      || container.template === sectionsParentTemplate) {
    Vue.set(container, 'marginTop', containerStyle.marginTop === 0 || containerStyle.marginTop ? containerStyle.marginTop : null);
    Vue.set(container, 'marginRight', containerStyle.marginRight === 0 || containerStyle.marginRight ? containerStyle.marginRight : null);
    Vue.set(container, 'marginBottom', containerStyle.marginBottom === 0 || containerStyle.marginBottom ? containerStyle.marginBottom : null);
    Vue.set(container, 'marginLeft', containerStyle.marginLeft === 0 || containerStyle.marginLeft ? containerStyle.marginLeft : null);
  } else {
    container.cssClass = container.cssClass.replace(new RegExp('(^| )(mt|mr|mb|ml|ms|me)-((md|lg|xl)-)?n?[0-9]{1,2}', 'g'), '').replace(/  +/g, ' ');
    if (containerStyle.marginTop === 0 || containerStyle.marginTop) {
      container.cssClass += ` mt-${containerStyle.marginTop >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-64, Math.min(containerStyle.marginTop || 0, 64)) / 4))}`;
      container.cssClass += ` me-${containerStyle.marginRight >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-64, Math.min(containerStyle.marginRight || 0, 64)) / 4))}`;
      container.cssClass += ` mb-${containerStyle.marginBottom >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-64, Math.min(containerStyle.marginBottom || 0, 64)) / 4))}`;
      container.cssClass += ` ms-${containerStyle.marginLeft >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-64, Math.min(containerStyle.marginLeft || 0, 64)) / 4))}`;
    }
  }

  container.cssClass = container.cssClass.replace(new RegExp('(^| )(brtr|brtl|brbr|brbl)-[0-9]', 'g'), '').replace(/  +/g, ' ');
  Vue.set(container, 'cssClass', container.cssClass);

  const borderRadiusEnabled = containerStyle.radiusTopRight === 0
    || containerStyle.radiusTopRight
    || containerStyle.radiusTopLeft === 0
    || containerStyle.radiusTopLeft
    || containerStyle.radiusBottomRight === 0
    || containerStyle.radiusBottomRight
    || containerStyle.radiusBottomLeft === 0
    || containerStyle.radiusBottomLeft;

  Vue.set(container, 'radiusTopRight', borderRadiusEnabled ? containerStyle.radiusTopRight : null);
  Vue.set(container, 'radiusTopLeft', borderRadiusEnabled ? containerStyle.radiusTopLeft : null);
  Vue.set(container, 'radiusBottomRight', borderRadiusEnabled ? containerStyle.radiusBottomRight : null);
  Vue.set(container, 'radiusBottomLeft', borderRadiusEnabled ? containerStyle.radiusBottomLeft : null);

  if (container.radiusTopRight === 0
    || container.radiusTopRight
    || container.radiusTopLeft === 0
    || container.radiusTopLeft
    || container.radiusBottomRight === 0
    || container.radiusBottomRight
    || container.radiusBottomLeft === 0
    || container.radiusBottomLeft) {
    container.cssClass += ` brtr-${parseInt(Math.min(20, Math.max(containerStyle.radiusTopRight, 0)) / 4)}`;
    container.cssClass += ` brtl-${parseInt(Math.min(20, Math.max(containerStyle.radiusTopLeft, 0)) / 4)}`;
    container.cssClass += ` brbr-${parseInt(Math.min(20, Math.max(containerStyle.radiusBottomRight, 0)) / 4)}`;
    container.cssClass += ` brbl-${parseInt(Math.min(20, Math.max(containerStyle.radiusBottomLeft, 0)) / 4)}`;
  }

  container.cssClass = container.cssClass.replace(new RegExp('(^| )hidden-sm-and-down', 'g'), '').replace(/  +/g, ' ');
  if (containerStyle.hiddenOnMobile) {
    container.cssClass += ' hidden-sm-and-down';
  }
  Vue.set(container, 'cssClass', container.cssClass);
  if (containerStyle !== container && typeof containerStyle.cssClass === 'string') {
    // Background margin/radius (EXIP-88427) are opaque cssClass tokens, not
    // ModelStyle fields, so they don't flow through the explicit properties
    // above. When containerStyle is a curated stand-in object (e.g. the
    // application drawer's backgroundProperties) rather than the real
    // container itself, its cssClass carries the up-to-date bg-* tokens and
    // must be replicated onto the real container's cssClass here.
    const backgroundLayerValues = parseBackgroundLayerValues(containerStyle.cssClass) || {};
    setBackgroundLayerValues(container, {
      marginTop: backgroundLayerValues.marginTop ?? null,
      marginRight: backgroundLayerValues.marginRight ?? null,
      marginBottom: backgroundLayerValues.marginBottom ?? null,
      marginLeft: backgroundLayerValues.marginLeft ?? null,
      radiusTopRight: backgroundLayerValues.radiusTopRight ?? null,
      radiusTopLeft: backgroundLayerValues.radiusTopLeft ?? null,
      radiusBottomRight: backgroundLayerValues.radiusBottomRight ?? null,
      radiusBottomLeft: backgroundLayerValues.radiusBottomLeft ?? null,
    });
  }
  Vue.set(container, 'height', container.height || null);
  Vue.set(container, 'width', container.width || null);
  Vue.set(container, 'borderColor', containerStyle.borderColor || null);
  Vue.set(container, 'borderSize', containerStyle.borderSize || '0');
  Vue.set(container, 'boxShadow', containerStyle.boxShadow && 'true' || null);
  Vue.set(container, 'appBackgroundColor', containerStyle.appBackgroundColor || null);
  Vue.set(container, 'appBackgroundImage', containerStyle.appBackgroundImage || null);
  Vue.set(container, 'appBackgroundEffect', containerStyle.appBackgroundEffect || null);
  Vue.set(container, 'appBackgroundPosition', containerStyle.appBackgroundPosition || null);
  Vue.set(container, 'appBackgroundSize', containerStyle.appBackgroundSize || null);
  Vue.set(container, 'appBackgroundRepeat', containerStyle.appBackgroundRepeat || null);
  Vue.set(container, 'backgroundColor', containerStyle.backgroundColor || null);
  Vue.set(container, 'backgroundImage', containerStyle.backgroundImage || null);
  Vue.set(container, 'backgroundEffect', containerStyle.backgroundEffect || null);
  Vue.set(container, 'backgroundPosition', containerStyle.backgroundPosition || null);
  Vue.set(container, 'backgroundSize', containerStyle.backgroundSize || null);
  Vue.set(container, 'backgroundRepeat', containerStyle.backgroundRepeat || null);
  Vue.set(container, 'textTitleColor', containerStyle.textTitleColor || null);
  Vue.set(container, 'textTitleFontSize', containerStyle.textTitleFontSize || null);
  Vue.set(container, 'textTitleFontWeight', containerStyle.textTitleFontWeight || null);
  Vue.set(container, 'textTitleFontStyle', containerStyle.textTitleFontStyle || null);
  Vue.set(container, 'textHeaderColor', containerStyle.textHeaderColor || null);
  Vue.set(container, 'textHeaderFontSize', containerStyle.textHeaderFontSize || null);
  Vue.set(container, 'textHeaderFontWeight', containerStyle.textHeaderFontWeight || null);
  Vue.set(container, 'textHeaderFontStyle', containerStyle.textHeaderFontStyle || null);
  Vue.set(container, 'textColor', containerStyle.textColor || null);
  Vue.set(container, 'textFontSize', containerStyle.textFontSize || null);
  Vue.set(container, 'textFontWeight', containerStyle.textFontWeight || null);
  Vue.set(container, 'textFontStyle', containerStyle.textFontStyle || null);
  Vue.set(container, 'textSubtitleColor', containerStyle.textSubtitleColor || null);
  Vue.set(container, 'textSubtitleFontSize', containerStyle.textSubtitleFontSize || null);
  Vue.set(container, 'textSubtitleFontWeight', containerStyle.textSubtitleFontWeight || null);
  Vue.set(container, 'textSubtitleFontStyle', containerStyle.textSubtitleFontStyle || null);
}

export function parseSite(layout) {
  const compatible = isSiteLayoutCompatible(layout);
  if (compatible) {
    parseContainer(layout);
    if (layout.children[0]?.template !== sidebarTemplate) {
      layout.children.unshift(newContainer(sidebarTemplate));
    }
    if (layout.children[2]?.template !== sidebarTemplate) {
      layout.children.push(newContainer(sidebarTemplate));
    }
    const siteBodyMiddleCenterContainer = layout.children[1].children.find(c => c.template === siteBodyMiddleCenterTemplate);
    if (siteBodyMiddleCenterContainer.children[0]?.template !== sidebarTemplate) {
      siteBodyMiddleCenterContainer.children.unshift(newContainer(sidebarTemplate));
    }
    if (siteBodyMiddleCenterContainer.children[2]?.template !== sidebarTemplate) {
      siteBodyMiddleCenterContainer.children.push(newContainer(sidebarTemplate));
    }
    return true;
  } else {
    return newSite(layout);
  }
}

export function newSite(layout) {
  if (!layout) {
    layout = {};
  }
  const applications = getApplications(layout);
  layout.template = siteTemplate;
  layout.children = [
    { // Left
      ...newContainer(sidebarTemplate),
      children: [{
        ...newContainer(sidebarCellTemplate),
        children: applications || [],
        width: 310
      }]
    },
    { // Middle
      ...newContainer(siteBodyMiddleTemplate),
      children: [
        { // Top Banner
          ...newContainer(bannerTemplate),
          children: [
            newContainer(bannerCellTemplate),
            newContainer(bannerCellTemplate),
            newContainer(bannerCellTemplate),
          ]
        },
        { // Middle - Center
          ...newContainer(siteBodyMiddleCenterTemplate),
          children: [
            { // Internal Left
              ...newContainer(sidebarTemplate),
              children: [{
                ...newContainer(sidebarCellTemplate),
                width: 310
              }]
            },
            // Page Body
            newContainer(pageBodyTemplate),
            { // Internal Right
              ...newContainer(sidebarTemplate),
              children: [{
                ...newContainer(sidebarCellTemplate),
                width: 310
              }]
            },
          ]
        },
        { // Bottom Banner
          ...newContainer(bannerTemplate),
          children: [
            newContainer(bannerCellTemplate),
          ]
        }
      ]
    },
    { // Right
      ...newContainer(sidebarTemplate),
      children: [{
        ...newContainer(sidebarCellTemplate),
        width: 310
      }]
    }
  ];
  return !applications?.length;
}

export function isSiteLayoutCompatible(layout) {
  return layout.template === siteTemplate
    && layout.children?.length
    && layout.children.length > 0
    && layout.children.length <= 3
    && layout.children.every(c => c.template === sidebarTemplate || c.template === siteBodyMiddleTemplate)
    && layout.children.find(c => c.template === siteBodyMiddleTemplate)?.children?.
      find?.(c => c.template === siteBodyMiddleCenterTemplate)?.children?.
      find?.(c => c.template === pageBodyTemplate)
    && layout.children.find(c => c.template === siteBodyMiddleTemplate).children.find(c => c.template === siteBodyMiddleCenterTemplate).children.length > 0
    && layout.children.find(c => c.template === siteBodyMiddleTemplate).children.find(c => c.template === siteBodyMiddleCenterTemplate).children.length <= 3;
}

export function parseSections(layout) {
  const parentContainer = getParentContainer(layout);
  if (parentContainer) {
    if (!parentContainer.children) {
      parentContainer.children = [];
    }
    const compatible = parentContainer.children.every(c => c.template === gridTemplate || c.template === flexTemplate);
    if (compatible) {
      Object.assign(parentContainer, Object.assign({...containerModel}, parentContainer));
      try {
        parentContainer.children.forEach(parseSection);
      } catch (e) {
        console.debug(e); // eslint-disable-line no-console
        return false;
      }
    }
    return compatible;
  } else {
    return false;
  }
}

export function getSection(layout, id) {
  const parentContainer = getParentContainer(layout);
  return parentContainer?.children?.find?.(c => c?.storageId === id);
}

export function getSectionByContainer(layout, id, isSiteLayout) {
  if (isSiteLayout) {
    return getContainerById(layout, id);
  } else {
    const parentContainer = getParentContainer(layout);
    return parentContainer?.children?.find?.(c => hasChild(c, id));
  }
}

export function getContainerById(container, id) {
  if (container?.storageId === id) {
    return container;
  } else if (container?.children?.length) {
    return container?.children.map(c => getContainerById(c, id)).find(c => !!c);
  } else {
    return null;
  }
}

export function hasChild(container, id) {
  if (container?.storageId === id) {
    return true;
  } else {
    return container?.children?.find?.(c => hasChild(c, id));
  }
}

export function getCell(container, storageId) {
  if (container.storageId === storageId) {
    return container;
  } else {
    return container?.children?.map?.(c => getCell(c, storageId))?.filter?.(c => c)?.[0];
  }
}

export function newSection(parentContainer, index, rows, cols, sectionType) {
  const defaultSectionStyle = sectionType === flexTemplate ? 'd-flex flex-column d-md-grid layout-sticky-application' : 'd-flex flex-column d-md-grid';
  const section = newContainer(
    sectionType,
    defaultSectionStyle,
    parentContainer,
    index || 0);
  applyBreakpointValues(section,
    sectionType === flexTemplate ? 1 : rows,
    sectionType === flexTemplate ? 12 : cols);
  applyGridStyle(section);

  if (sectionType === flexTemplate) {
    const colSpan = parseInt(12 / cols);
    for (let i = 0; i < cols - 1; i++) {
      newCell(section, 0, 1, colSpan);
    }
    const leftColSpan = colSpan + 12 % cols;
    newCell(section, 0, 1, leftColSpan);
  } else {
    for (let i = 0; i < rows; i++) {
      for (let j = 0; j < cols; j++) {
        newCell(section, 0, 1, 1);
      }
    }
  }
  // Compute cell indexes
  parseMatrix(section);
  return section;
}

export function editGridSection(section, rows, cols) {
  const diffRows = rows - section.rowsCount;
  const diffCols = cols - section.colsCount;
  if (diffCols > 0) {
    addColumns(section, diffCols);
  } else if (diffCols < 0) {
    removeColumns(section, diffCols);
  }
  if (diffRows > 0) {
    addRows(section, diffRows);
  } else if (diffRows < 0) {
    removeRows(section, diffRows);
  }
}

export function editDynamicSection(section, cols) {
  const colSpan = parseInt(section.colsCount / cols);
  section.children.forEach(c => applyBreakpointValues(c, 1, colSpan));

  const diffCols = cols - section.children.length;
  if (diffCols < 0) {
    section.children = section.children.slice(0, cols);
  } else if (diffCols > 0) {
    const lastIndex = section.children.length;
    for (let i = 0; i < diffCols; i++) {
      newCell(section, lastIndex, 1, colSpan);
    }
  }
  parseSectionMatrix(section, parseMatrix(section));
  applyGridStyle(section);
}

export function newApplication(parentContainer, appFromRegistry, append) {
  const application = JSON.parse(JSON.stringify(applicationModel));
  application.contentId = appFromRegistry.contentId;
  application.title = appFromRegistry.displayName || appFromRegistry.name;
  application.description = appFromRegistry.description;
  application.preferences = appFromRegistry.preferences;
  application.showApplicationMode = true;
  if (append && parentContainer.children) {
    parentContainer.children.push(application);
  } else {
    parentContainer.children = [application];
  }
}

export function addRows(section, diffRows) {
  if (!diffRows) {
    return;
  }
  diffRows = Math.abs(diffRows);
  const matrix = parseMatrix(section);
  section.rowsCount += diffRows;
  parseSectionMatrix(section, matrix);
  parseMatrix(section);
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
}

export function addColumns(section, diffCols) {
  if (!diffCols) {
    return;
  }
  diffCols = Math.abs(diffCols);
  const matrix = parseMatrix(section);
  section.colsCount += diffCols;
  parseSectionMatrix(section, matrix);
  refreshCellIndexes(section);
  filterChildren(section);
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
}

export function removeRows(section, diffRows) {
  if (!diffRows) {
    return;
  }
  diffRows = Math.abs(diffRows);
  const matrix = parseMatrix(section);
  section.rowsCount -= diffRows;
  parseSectionMatrix(section, matrix);
  filterChildren(section);
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
}

export function removeColumns(section, diffCols) {
  diffCols = Math.abs(diffCols);
  if (!diffCols) {
    return;
  }
  diffCols = Math.abs(diffCols);
  const matrix = parseMatrix(section);
  section.colsCount -= diffCols;
  parseSectionMatrix(section, matrix);
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
  filterChildren(section);
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
}

export function refreshCellIndexes(section) {
  parseSectionMatrix(section, parseMatrix(section));
  applyBreakpointValues(section, section.rowsCount, section.colsCount);
  applyGridStyle(section);
}

export function moveCell(section, sourceCell, targetRowIndex, targetColIndex) {
  const sourceRowIndex = sourceCell.rowIndex;
  const sourceColIndex = sourceCell.colIndex;
  if ((sourceRowIndex === targetRowIndex && sourceColIndex === targetColIndex)
    || !isValidTargetMovingCell(section,
      sourceCell,
      targetRowIndex,
      targetColIndex)) {
    return;
  }
  const matrix = parseMatrix(section);

  const rowCollision1 = (targetRowIndex > sourceCell.rowIndex && targetRowIndex < (sourceCell.rowIndex + sourceCell.rowsCount));
  const rowCollision2 = (targetRowIndex < sourceCell.rowIndex && (targetRowIndex + sourceCell.rowsCount) > sourceCell.rowIndex);
  const colCollision1 = (targetColIndex > sourceCell.colIndex && targetColIndex < (sourceCell.colIndex + sourceCell.colsCount));
  const colCollision2 = (targetColIndex < sourceCell.colIndex && (targetColIndex + sourceCell.colsCount) > sourceCell.colIndex);
  if ((colCollision1 || colCollision2) && targetRowIndex === sourceCell.rowIndex) {
    moveCol(matrix, sourceCell, targetColIndex);
    sourceCell.rowIndex = targetRowIndex;
    sourceCell.colIndex = targetColIndex;
  } else if ((rowCollision1 || rowCollision2) && targetColIndex === sourceCell.colIndex) {
    moveRow(matrix, sourceCell, targetRowIndex);
    sourceCell.rowIndex = targetRowIndex;
    sourceCell.colIndex = targetColIndex;
  } else if ((rowCollision1 || rowCollision2) && (colCollision1 || colCollision2)) {
    moveRow(matrix, sourceCell, targetRowIndex);
    sourceCell.rowIndex = targetRowIndex;
    moveCol(matrix, sourceCell, targetColIndex);
    sourceCell.colIndex = targetColIndex;
  } else {
    for (let i = 0; i < sourceCell.rowsCount; i++) {
      for (let j = 0; j < sourceCell.colsCount; j++) {
        const targetCellRowIndex = targetRowIndex + i;
        const targetCellColIndex = targetColIndex + j;
        const sourceCellRowIndex = sourceCell.rowIndex + i;
        const sourceCellColIndex = sourceCell.colIndex + j;

        const target = matrix[targetCellRowIndex][targetCellColIndex];
        matrix[sourceCellRowIndex][sourceCellColIndex] = target;
        matrix[targetCellRowIndex][targetCellColIndex] = sourceCell;
      }
    }
  }
  sourceCell.rowIndex = targetRowIndex;
  sourceCell.colIndex = targetColIndex;

  parseSectionMatrix(section, matrix);
  applyGridStyle(section);
}

export function deleteCell(section, application) {
  const cell = section?.children?.find?.(c => hasChild(c, application.storageId));
  if (section.template === flexTemplate) {
    const index = cell.children.findIndex(c => hasChild(c, application.storageId));
    if (index >= 0) {
      cell.children.splice(index, 1);
    }
    parseSectionMatrix(section, parseMatrix(section));
  } else {
    const matrix = parseMatrix(section);
    for (let row = cell.rowIndex; row < (cell.rowIndex + cell.rowsCount); row++) {
      for (let col = cell.colIndex; col < (cell.colIndex + cell.colsCount); col++) {
        matrix[row][col] = null;
      }
    }
    parseSectionMatrix(section, matrix);
  }
  applyGridStyle(section);
}

export function resizeCell(section, cell, rowIndex, colIndex) {
  if (section.template === flexTemplate) {
    cell = section.children.find(c => c?.storageId === cell.storageId);
    const cellIndex = section.children.findIndex(c => c?.storageId === cell.storageId);
    const nextCell = section.children[cellIndex + 1];

    cell.colsCount = colIndex - cell.colIndex + 1;
    nextCell.colsCount = nextCell.colsCount + nextCell.colIndex - colIndex - 1;
    nextCell.colIndex = colIndex + cell.colsCount;
    applyBreakpointValues(section, section.rowsCount, section.colsCount);
    parseSectionMatrix(section, parseMatrix(section));
  } else {
    const matrix = parseMatrix(section);
    for (let row = cell.rowIndex; row < (cell.rowIndex + cell.rowsCount); row++) {
      for (let col = cell.colIndex; col < (cell.colIndex + cell.colsCount); col++) {
        matrix[row][col] = null;
      }
    }
    for (let row = cell.rowIndex; row <= rowIndex; row++) {
      for (let col = cell.colIndex; col <= colIndex; col++) {
        matrix[row][col] = cell;
      }
    }
    parseSectionMatrix(section, matrix);
  }
  applyGridStyle(section);
}

export function isValidTargetMovingCell(section, movingCell, targetRowIndex, targetColIndex) {
  if (!section || !movingCell || targetRowIndex < 0 || targetColIndex < 0) {
    return false;
  }
  const targetEndRowIndex = movingCell.rowsCount + targetRowIndex - 1;
  const targetEndColIndex = movingCell.colsCount + targetColIndex - 1;

  return (targetEndRowIndex < section.rowsCount)
    && (targetEndColIndex < section.colsCount)
    && section.children.every(c => !c.children?.length
        || c.storageId === movingCell?.storageId
        || (c.colsCount === 1 && c.rowsCount === 1)
        || (c.rowIndex + c.rowsCount - 1) < targetRowIndex
        || c.rowIndex > targetEndRowIndex
        || (c.colIndex + c.colsCount - 1) < targetColIndex
        || c.colIndex > targetEndColIndex
        || (
          isBetween(c.colIndex, targetColIndex, targetEndColIndex)
          && isBetween(c.colIndex + c.colsCount -1, targetColIndex, targetEndColIndex)
          && isBetween(c.rowIndex, targetRowIndex, targetEndRowIndex)
          && isBetween(c.rowIndex + c.rowsCount -1, targetRowIndex, targetEndRowIndex)
        ));
}

export function isBetween(value, b0, b1) {
  return value >= b0 && value <= b1;
}

export function cleanAttributes(container, cleanStorage, cleanStyle) {
  container = JSON.parse(JSON.stringify(container));
  if (cleanStyle) {
    applyDesktopStyle(container);
  }
  if (container.children?.length) {
    container.children = container.children.map(c => cleanAttributes(c, cleanStorage, cleanStyle));
  }
  if (container.randomId || cleanStorage) {
    delete container.storageId;
    delete container.storageName;
    delete container.factoryId;
  }
  Object.keys(container).forEach(key => {
    if (key
      && containerModelAttributes.indexOf(key) < 0
      && applicationModelAttributes.indexOf(key) < 0) {
      delete container[key];
    }
  });
  return container;
}

export function parseContainer(container) {
  if (container.template) {
    containerModelAttributes.forEach(key => {
      if (!Object.hasOwn(container, key)) {
        container[key] = containerModel[key];
      }
    });
    container.children.forEach(parseContainer);
  } else {
    applicationModelAttributes.forEach(key => {
      if (!Object.hasOwn(container, key)) {
        container[key] = applicationModel[key];
      }
    });
  }
  return container;
}

export function newCell(section, index, rows, cols) {
  const container = newContainer(cellTemplate,
    section.template === flexTemplate ? 'flex-cell' : 'grid-cell',
    (index === 0 || index) && section || null,
    index);
  applyBreakpointValues(container, rows, cols);
  applyGridStyle(container);
  return container;
}

export function newContainer(template, cssClass, parentContainer, index) {
  const container = JSON.parse(JSON.stringify(containerModel));
  container.template = template;
  container.cssClass = cssClass || '';
  container.storageId = `${parseInt(Math.random() * 65536)}`;
  container.randomId = true;
  container.name = container.storageId;
  if (parentContainer && (index || index === 0)) {
    container.parentId = parentContainer.storageId;
    if (parentContainer.children) {
      parentContainer.children.splice(index || 0, 0, container);
    } else {
      parentContainer.children = [container];
    }
  }
  return container;
}

function parseSection(section) {
  if ((section.template !== gridTemplate && section.template !== flexTemplate)
    || !section.children.length) {
    return;
  }
  Object.assign(section, Object.assign({...containerModel}, section));
  if (section.children) {
    section.children.forEach(parseCell);
  } else {
    section.children = [];
  }

  section.colBreakpoints = parseBreakpointClasses(section, 'grid-cols');
  section.rowBreakpoints = parseBreakpointClasses(section, 'grid-rows');
  section.gap = parseGapClasses(section, 'grid-gap');
  section.colsCount = section.colBreakpoints[currentBreakpoint];
  section.rowsCount = section.rowBreakpoints[currentBreakpoint];
  if (section.template === gridTemplate || section.template === flexTemplate) {
    // Compute cell indexes
    parseMatrix(section);
  }
}

export function parseContainerStyle(container) {
  if (container.template !== siteTemplate
    && container.template !== flexTemplate
    && container.template !== gridTemplate
    && container.template !== bannerTemplate
    && container.template !== sidebarTemplate
    && container.template !== pageBodyTemplate
    && container.template !== sectionsParentTemplate) {
    const marginMatches = container?.cssClass?.match?.(new RegExp('(^| )(mt|mr|mb|ml|ms|me)-((md|lg|xl)-)?n?[0-9]{1,2}', 'g')) || [];
    if (marginMatches?.length) {
      container.marginTop = parseInt(marginMatches.find(c => c.search(/mt-n?\d+/) >= 0)?.replace?.('mt-n', '-')?.replace?.('mt-', '') || 0) * 4;
      container.marginRight = parseInt(marginMatches.find(c => c.search(/me-n?\d+/) >= 0)?.replace?.('me-n', '-')?.replace?.('me-', '') || 0) * 4;
      container.marginBottom = parseInt(marginMatches.find(c => c.search(/mb-n?\d+/) >= 0)?.replace?.('mb-n', '-')?.replace?.('mb-', '') || 0) * 4;
      container.marginLeft = parseInt(marginMatches.find(c => c.search(/ms-n?\d+/) >= 0)?.replace?.('ms-n', '-')?.replace?.('ms-', '') || 0) * 4;
    } else {
      container.marginTop = null;
      container.marginRight = null;
      container.marginBottom = null;
      container.marginLeft = null;
    }
  }

  const radiusMatches = container?.cssClass?.match?.(new RegExp('(^| )(brtr|brtl|brbr|brbl)-[0-9]', 'g')) || [];
  if (radiusMatches?.length) {
    container.radiusTopRight = parseInt(radiusMatches.find(c => c.indexOf('brtr-') >= 0)?.replace?.('brtr-', '') || 1) * 4;
    container.radiusTopLeft = parseInt(radiusMatches.find(c => c.indexOf('brtl-') >= 0)?.replace?.('brtl-', '') || 1) * 4;
    container.radiusBottomRight = parseInt(radiusMatches.find(c => c.indexOf('brbr-') >= 0)?.replace?.('brbr-', '') || 1) * 4;
    container.radiusBottomLeft = parseInt(radiusMatches.find(c => c.indexOf('brbl-') >= 0)?.replace?.('brbl-', '') || 1) * 4;
  }
}

function parseCell(colContainer) {
  if (colContainer.template !== cellTemplate) {
    throw Error(`Container template '${colContainer.template}' not compatible. Fallback to old editor.`);
  }
  colContainer.colBreakpoints = parseBreakpointClasses(colContainer, 'grid-cell-colspan');
  colContainer.rowBreakpoints = parseBreakpointClasses(colContainer, 'grid-cell-rowspan');
  colContainer.gap = parseGapClasses(colContainer, 'grid-cell-gap');
  colContainer.colsCount = colContainer.colBreakpoints[currentBreakpoint];
  colContainer.rowsCount = colContainer.rowBreakpoints[currentBreakpoint];
  parseContainerStyle(colContainer);
}

function applyBreakpointClasses(container, rowClassPrefix, colClassPrefix) {
  if (!container.cssClass) {
    container.cssClass = '';
  }
  cleanBreakpointClasses(container, rowClassPrefix, colClassPrefix);

  let cssClasses = container.cssClass;
  // Apply new Classes
  if (container.colBreakpoints) {
    if (!cssClasses.includes(colClassPrefix)) {
      cssClasses += ` ${colClassPrefix}`;
    }
    breakpoints.forEach(b => cssClasses += ` ${colClassPrefix}-${b}-${container.colBreakpoints[b]}`);
  }
  if (container.rowBreakpoints && rowClassPrefix) {
    if (!cssClasses.includes(rowClassPrefix)) {
      cssClasses += ` ${rowClassPrefix}`;
    }
    breakpoints.forEach(b => cssClasses += ` ${rowClassPrefix}-${b}-${container.rowBreakpoints[b]}`);
  }
  container.cssClass = cssClasses;
}

function cleanBreakpointClasses(container, rowClassPrefix, colClassPrefix) {
  let cssClasses = container.cssClass || '';

  const colClasses = cssClasses.match(new RegExp(`(^| )${colClassPrefix}-((md|lg|xl)-)?[0-9]{1,2}`, 'g'));
  // Remove old Classes
  if (colClasses?.length) {
    colClasses.forEach(c => cssClasses = cssClasses.replace(c, ''));
  }

  if (rowClassPrefix) {
    const rowClasses = cssClasses.match(new RegExp(`(^| )${rowClassPrefix}-((md|lg|xl)-)?[0-9]{1,2}`, 'g'));
    // Remove old Classes
    if (rowClasses?.length) {
      rowClasses.forEach(c => cssClasses = cssClasses.replace(c, ''));
    }
  }

  // Apply new Classes
  container.cssClass = cssClasses.replace(/  +/g, ' ');
}

function parseBreakpointClasses(container, classPrefix) {
  const classes = !container.cssClass && [] || container.cssClass.match(new RegExp(`(^| )${classPrefix}-((md|lg|xl)-)?[0-9]{1,2}`, 'g'));
  const bp = {
    md: 0,
    lg: 0,
    xl: 0,
  };
  breakpoints.forEach(b => {
    const prefix = `${classPrefix}-${b}-`;
    const aClass = classes?.find?.(c => c.match(`${prefix}[0-9]{1,2}`)?.length);
    if (aClass) {
      const span = Number(aClass.replace(prefix, ''));
      if (!Number.isNaN(span)) {
        bp[b] = span;
      }
    } else {
      bp[b] = 1;
    }
  });
  bp.xl = bp.xl || bp.lg || bp.md;
  bp.lg = bp.lg || bp.md;
  if (bp.xl) {
    return bp;
  } else {
    throw Error(`CSS classes '${container.cssClass || ''}' not compatible. Fallback to old editor.`);
  }
}

function parseGapClasses() {
  return {
    h: 20,
    v: 20,
    top: 0,
    right: 20,
    bottom: 20,
    left: 0,
  };
}

export function applyBreakpointValues(container, rows, cols) {
  if (!container.colBreakpoints) {
    container.colBreakpoints = {};
  }
  if (!container.rowBreakpoints) {
    container.rowBreakpoints = {};
  }
  breakpoints.forEach(b => {
    container.colBreakpoints[b] = cols;
    container.rowBreakpoints[b] = rows;
  });
  container.colsCount = container.colBreakpoints[currentBreakpoint];
  container.rowsCount = container.rowBreakpoints[currentBreakpoint];
}

function moveCol(matrix, sourceCell, targetColIndex) {
  for (let i = 0; i < sourceCell.rowsCount; i++) {
    let colIndex = 0;
    for (let j = 0; j < sourceCell.colsCount; j++) {
      const targetCellRowIndex = sourceCell.rowIndex + i;
      const targetCellColIndex = targetColIndex + j;
      const sourceCellRowIndex = sourceCell.rowIndex + i;
      const sourceCellColIndex = sourceCell.colIndex + j - colIndex;

      const target = matrix[targetCellRowIndex][targetCellColIndex];
      if (target.storageId === sourceCell.storageId) { // Then update its col & row indices
        colIndex++;
      } else {
        matrix[targetCellRowIndex][targetCellColIndex] = sourceCell;
        matrix[sourceCellRowIndex][sourceCellColIndex] = target;
        target.rowIndex = sourceCellRowIndex;
        target.colIndex = sourceCellColIndex;
      }
    }
  }
}

function moveRow(matrix, sourceCell, targetRowIndex) {
  for (let j = 0; j < sourceCell.colsCount; j++) {
    let rowIndex = 0;
    for (let i = 0; i < sourceCell.rowsCount; i++) {
      const targetCellRowIndex = targetRowIndex + i;
      const targetCellColIndex = sourceCell.colIndex + j;
      const sourceCellRowIndex = sourceCell.rowIndex + i - rowIndex;
      const sourceCellColIndex = sourceCell.colIndex + j;

      const target = matrix[targetCellRowIndex][targetCellColIndex];
      if (target.storageId === sourceCell.storageId) { // Then update its col & row indices
        rowIndex++;
      } else {
        matrix[targetCellRowIndex][targetCellColIndex] = sourceCell;
        matrix[sourceCellRowIndex][sourceCellColIndex] = target;
        target.rowIndex = sourceCellRowIndex;
        target.colIndex = sourceCellColIndex;
      }
    }
  }
}

function filterChildren(section) {
  section.children = section.children.filter(c => {
    const colSpan = breakpoints.reduce((sum, b) => sum += c.colBreakpoints[b], 0);
    const rowSpan = breakpoints.reduce((sum, b) => sum += c.rowBreakpoints[b], 0);
    return colSpan && rowSpan;
  });
}

function parseMatrix(section) {
  const matrix = {};
  for (let i = 0; i < section.rowsCount; i++) {
    matrix[i] = {};
    for (let j = 0; j < section.colsCount; j++) {
      matrix[i][j] = null;
    }
  }
  let rowIndex = 0;
  let colIndex = 0;
  section.children.forEach(cell => {
    cell.rowIndex = -1;
    cell.colIndex = -1;
  });
  section.children.forEach(cell => {
    while (matrix[rowIndex][colIndex]) {
      colIndex++;
      if ((colIndex % section.colsCount) === 0) {
        colIndex = 0;
        rowIndex++;
      }
    }
    cell.rowIndex = rowIndex;
    cell.colIndex = colIndex;
    for (let row = 0; row < cell.rowsCount; row++) {
      for (let col = 0; col < cell.colsCount; col++) {
        if (cell.rowIndex + row < section.rowsCount
            && cell.colIndex + col < section.colsCount) {
          matrix[cell.rowIndex + row][cell.colIndex + col] = cell;
        }
      }
    }
    colIndex = cell.colIndex + cell.colsCount;
    if ((colIndex % section.colsCount) === 0) {
      colIndex = 0;
      rowIndex++;
    }
  });
  return matrix;
}

function parseSectionMatrix(section, matrix) {
  const children = [];
  for (let row = 0; row < section.rowsCount; row++) {
    if (!matrix[row]) {
      matrix[row] = {};
    }
    for (let col = 0; col < section.colsCount; col++) {
      let existingCell = null;
      const cell = matrix[row][col];
      if (cell) {
        const storageId = cell.storageId;
        existingCell = children.find(c => c.storageId === storageId);
        if (existingCell
            && ((existingCell.rowIndex !== row && matrix[row - 1]?.[col]?.storageId !== storageId)
            || (existingCell.colIndex !== col && matrix[row]?.[col - 1]?.storageId !== storageId))) {
          matrix[row][col] = null;
          existingCell = null;
        } else if (existingCell) {
          matrix[row][col] = existingCell;
        }
      }
      if (!matrix[row][col]) {
        matrix[row][col] = newCell(section, children.length, 1, 1);
      }
      if (!existingCell) {
        matrix[row][col] = JSON.parse(JSON.stringify(matrix[row][col]));
        matrix[row][col].rowIndex = row;
        matrix[row][col].colIndex = col;
        children.push(matrix[row][col]);
      }
    }
  }
  children.forEach(cell => {
    cell.rowsCount = 0;
    let i = cell.rowIndex;
    while (i < section.rowsCount && matrix[i++]?.[cell.colIndex]?.storageId === cell.storageId) {
      cell.rowsCount++;
    }

    cell.colsCount = 0;
    let j = cell.colIndex;
    while (j < section.colsCount && matrix[cell.rowIndex]?.[j++]?.storageId === cell.storageId) {
      cell.colsCount++;
    }
    breakpoints.forEach(b => {
      cell.rowBreakpoints[b] = cell.rowsCount;
      cell.colBreakpoints[b] = cell.colsCount;
    });
  });
  Object.assign(section, {children});
}

function installPageContext(pageHeadContent) {
  const replacableScriptsIterator = pageHeadContent.matchAll(/<script[^>]*id="[^>]*"[^>]*>/g);
  let scriptIteratorElement = replacableScriptsIterator.next().value;
  while (scriptIteratorElement) {
    const script = scriptIteratorElement[0];
    const id = script.match(/id="([^"]*)"/i)[1];
    if (pageJSContextIds.indexOf(id) >= 0) {
      const scriptContent = pageHeadContent.substring(scriptIteratorElement.index, pageHeadContent.indexOf('</script>', scriptIteratorElement.index));
      let scriptElement = document.querySelector(`#${id.trim()}`) || document.querySelector(`[data-id=${id.trim()}]`);
      if (!scriptElement) {
        scriptElement = document.createElement('script');
        document.head.append(scriptElement);
      }
      scriptElement.innerText = scriptContent.substring(scriptContent.indexOf('>') + 1).replace(/(\r)?(\n)?/g, '');
      replaceScriptElements(scriptElement);
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
  scriptElement.innerText = node.innerHTML;
  const scriptAttrs = node.attributes;
  for (let i = 0; i < scriptAttrs.length; i++)  {
    scriptElement.setAttribute(scriptAttrs[i].name, scriptAttrs[i].value);
  }
  return scriptElement;
}
