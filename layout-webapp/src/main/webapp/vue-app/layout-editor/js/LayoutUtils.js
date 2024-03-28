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

export const breakpoints = ['md', 'lg', 'xl'];

export const currentBreakpoint = 'xl';
export const pageLayoutTemplate = 'system:/groovy/portal/webui/container/UIPageLayout.gtmpl';
export const simpleTemplate = 'system:/groovy/portal/webui/container/UIContainer.gtmpl';
export const gridTemplate = 'GridContainer';
export const flexTemplate = 'FlexContainer';
export const cellTemplate = 'CellContainer';

export const defaultMarginTop = 20;
export const defaultMarginRight = 20;
export const defaultMarginBottom = 20;
export const defaultMarginLeft = 20;
export const defaultBorderRadius = 8;
export const defaultBorderColor = '#FFFFFF';

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
  borderColor: null,
  // Used to specify whether the block should be displayed or not
  // when an addon is present
  profiles: null,
  // Generally kept to be accessible to everyone to make
  // the parent page access permissions applied globally 
  accessPermissions: ['Everyone'],
  // Deprecated, not used for containers
  moveAppsPermissions: ['Everyone'],
  // Deprecated, not used for containers
  moveContainersPermissions: ['Everyone'],
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
  theme: null,
  width: null,
  height: null,
  accessPermissions: ['Everyone'],
};

export const containerModelAttributes = Object.keys(containerModel);

export const applicationModelAttributes = Object.keys(applicationModel);

export const pageJSContextIds = [
  'portalHeadScripts',
  'socialHeadScripts',
  'sitesHeadScripts',
];

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
  if (!layout.children) {
    layout.children = [];
  }
  const parentContainer = layout.children[0]?.children?.[0];
  if (!parentContainer?.cssClass?.includes?.('v-application')) {
    return;
  } else {
    return parentContainer;
  }
}

export function newParentContainer(layout) {
  const vuetifyAppContainer = newContainer(simpleTemplate, 'VuetifyApp', layout, 0);
  const parent = newContainer(pageLayoutTemplate, 'v-application v-application--is-ltr v-application--wrap singlePageApplication layout-sections-parent', vuetifyAppContainer, 0);
  newSection(parent, 0, 12, 12, gridTemplate);
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
    container.children?.forEach(applyGridStyle);
    applyBreakpointClasses(container, container.template === gridTemplate && 'grid-rows' || null, 'grid-cols');
  } else if (container.template === cellTemplate) {
    applyBreakpointClasses(container, 'grid-cell-rowspan', 'grid-cell-colspan');
    applyCellHeightStyle(container);
  } else {
    container.children?.forEach?.(applyGridStyle);
  }
}

export function applyContainerStyle(section, container, containerStyle) {
  if (!container.cssClass) {
    container.cssClass = '';
  }
  container.cssClass = container.cssClass.replace(new RegExp('(^| )(mt|mr|mb|ml|ms|me)-((md|lg|xl)-)?n?[0-9]{1,2}', 'g'), '').replace(/  +/g, ' ');

  // Apply new Classes
  container.marginTop = containerStyle.marginTop || 0;
  container.marginRight = containerStyle.marginRight || 0;
  container.marginBottom = containerStyle.marginBottom || 0;
  container.marginLeft = containerStyle.marginLeft || 0;

  container.cssClass += ` mt-${containerStyle.marginTop >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-20, Math.min(containerStyle.marginTop, 20)) / 4))}`;
  container.cssClass += ` me-${containerStyle.marginRight >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-20, Math.min(containerStyle.marginRight, 20)) / 4))}`;
  container.cssClass += ` mb-${containerStyle.marginBottom >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-20, Math.min(containerStyle.marginBottom, 20)) / 4))}`;
  container.cssClass += ` ms-${containerStyle.marginLeft >= 0 ? '' : 'n'}${Math.abs(parseInt(Math.max(-20, Math.min(containerStyle.marginLeft, 20)) / 4))}`;

  container.cssClass = container.cssClass.replace(new RegExp('(^| )(brtr|brtl|brbr|brbl)-[0-9]', 'g'), '').replace(/  +/g, ' ');
  const borderRadiusEnabled = containerStyle.radiusTopRight === 0 || containerStyle.radiusTopRight;

  Vue.set(container, 'radiusTopRight', borderRadiusEnabled ? containerStyle.radiusTopRight : null);
  Vue.set(container, 'radiusTopLeft', borderRadiusEnabled ? containerStyle.radiusTopLeft : null);
  Vue.set(container, 'radiusBottomRight', borderRadiusEnabled ? containerStyle.radiusBottomRight : null);
  Vue.set(container, 'radiusBottomLeft', borderRadiusEnabled ? containerStyle.radiusBottomLeft : null);

  if (container.radiusTopRight === 0 || container.radiusTopRight) {
    container.cssClass += ` brtr-${parseInt(Math.min(20, Math.max(containerStyle.radiusTopRight, 0)) / 4)}`;
    container.cssClass += ` brtl-${parseInt(Math.min(20, Math.max(containerStyle.radiusTopLeft, 0)) / 4)}`;
    container.cssClass += ` brbr-${parseInt(Math.min(20, Math.max(containerStyle.radiusBottomRight, 0)) / 4)}`;
    container.cssClass += ` brbl-${parseInt(Math.min(20, Math.max(containerStyle.radiusBottomLeft, 0)) / 4)}`;
  }

  Vue.set(container, 'borderColor', containerStyle.borderColor || null);
}

export function parseSections(layout) {
  const parentContainer = getParentContainer(layout);
  if (parentContainer) {
    if (!parentContainer.children) {
      parentContainer.children = [];
    }
    const compatible = parentContainer.children.every(c => c.template === gridTemplate || c.template === flexTemplate);
    if (compatible) {
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
  return parentContainer?.children?.find?.(c => c.storageId === id);
}

export function getCell(container, storageId) {
  if (container.storageId === storageId) {
    return container;
  } else {
    return container?.children?.map?.(c => getCell(c, storageId))?.filter?.(c => c)?.[0];
  }
}

export function newSection(parentContainer, index, rows, cols, sectionType) {
  rows = rows || 1;
  cols = cols || 4;
  const section = newContainer(
    sectionType,
    'd-flex flex-column d-md-grid',
    parentContainer,
    index || 0);
  applyBreakpointValues(section, rows, cols);
  applyGridStyle(section);

  for (let i = 0; i < rows; i++) {
    for (let j = 0; j < cols; j++) {
      newCell(section, 0, 1, 1);
    }
  }
  // Compute cell indexes
  parseMatrix(section);
  return section;
}

export function newApplication(parentContainer, appFromRegistry, append) {
  const application = JSON.parse(JSON.stringify(applicationModel));
  application.contentId = appFromRegistry.contentId;
  application.title = appFromRegistry.displayName;
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

export function deleteCell(section, cell) {
  const matrix = parseMatrix(section);
  for (let row = cell.rowIndex; row < (cell.rowIndex + cell.rowsCount); row++) {
    for (let col = cell.colIndex; col < (cell.colIndex + cell.colsCount); col++) {
      matrix[row][col] = null;
    }
  }
  parseSectionMatrix(section, matrix);
  applyGridStyle(section);
}

export function resizeCell(section, cell, rowIndex, colIndex) {
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
  
export function cleanAttributes(container) {
  container = JSON.parse(JSON.stringify(container));
  if (container.children?.length) {
    container.children = container.children.map(c => cleanAttributes(c));
  }
  if (container.randomId) {
    container.storageId = null;
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

function newCell(section, index, rows, cols) {
  const container = newContainer(cellTemplate,
    section.template === flexTemplate ? 'flex-cell row-gap-5' : 'grid-cell',
    (index === 0 || index) && section || null,
    index);
  applyBreakpointValues(container, rows, cols);
  applyGridStyle(container);
  return container;
}

function newContainer(template, cssClass, parentContainer, index) {
  const container = JSON.parse(JSON.stringify(containerModel));
  container.template = template;
  container.cssClass = cssClass;
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
  // Compute cell indexes
  parseMatrix(section);
}

export function parseContainerStyle(container) {
  const marginMatches = container?.cssClass?.match?.(new RegExp('(^| )(mt|mr|mb|ml|ms|me)-((md|lg|xl)-)?n?[0-9]{1,2}', 'g')) || [];
  container.marginTop = parseInt(marginMatches.find(c => c.indexOf('mt-') >= 0)?.replace?.('mt-n', '-')?.replace?.('mt-', '') || 0) * 4;
  container.marginRight = parseInt(marginMatches.find(c => c.indexOf('me-') >= 0)?.replace?.('me-n', '-')?.replace?.('me-', '') || 0) * 4;
  container.marginBottom = parseInt(marginMatches.find(c => c.indexOf('mb-') >= 0)?.replace?.('mb-n', '-')?.replace?.('mb-', '') || 0) * 4;
  container.marginLeft = parseInt(marginMatches.find(c => c.indexOf('ms-') >= 0)?.replace?.('ms-n', '-')?.replace?.('ms-', '') || 0) * 4;

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
    breakpoints.forEach(b => cssClasses += ` ${colClassPrefix}-${b}-${container.colBreakpoints[b]}`);
  }
  if (container.rowBreakpoints && rowClassPrefix) {
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
    const aClass = classes.find(c => c.match(`${prefix}[0-9]{1,2}`)?.length);
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

function applyCellHeightStyle(container) {
  container.cssClass = container.cssClass
    .replace(' grid-cell-height-auto', '')
    .replace(' grid-cell-height-scroll', '');
  if (container.height === 'auto') {
    container.cssClass += ' grid-cell-height-auto';
  } else {
    container.cssClass += ' grid-cell-height-scroll';
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

function applyBreakpointValues(container, rows, cols) {
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

// FIXME delete 'export'
export function displayMatrix(matrix) {
  const result = {};
  Object.keys(matrix).forEach(r => {
    result[r] = {};
    Object.keys(matrix[r]).forEach(c => {
      if (matrix[r][c]?.children?.length) {
        result[r][c] = `${matrix[r][c].storageId}_${r}_${c}`;
      } else if (matrix[r][c]?.randomId) {
        result[r][c] = 0;
      } else if (matrix[r][c]?.storageId) {
        result[r][c] = '---------';
      } else {
        result[r][c] = null;
      }
    });
  });
  console.warn(result); // eslint-disable-line no-console
}

// FIXME delete 'export'
export function parseMatrix(section) {
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

// FIXME delete 'export'
export function parseSectionMatrix(section, matrix) {
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
