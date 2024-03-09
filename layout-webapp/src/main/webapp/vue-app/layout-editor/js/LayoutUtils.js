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

export const breakpoints = ['xs', 'sm', 'md', 'lg', 'xl'];

export const currentBreakpoint = 'xl';
export const simpleTemplate = 'system:/groovy/portal/webui/container/UIContainer.gtmpl';
export const gridTemplate = 'system:/groovy/portal/webui/container/UIVGridContainer.gtmpl';
export const cellTemplate = 'system:/groovy/portal/webui/container/UIVCellContainer.gtmpl';

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
  const parent = newContainer(simpleTemplate, 'v-application v-application--is-ltr v-application--wrap singlePageApplication', vuetifyAppContainer, 0);
  newSection(parent, 0, 3, 4);
}

export function parseSections(layout) {
  const parentContainer = getParentContainer(layout);
  if (parentContainer) {
    if (!parentContainer.children) {
      parentContainer.children = [];
    }
    const compatible = parentContainer.children.every(c => c.template === gridTemplate);
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

export function newSection(parentContainer, index, rows, cols) {
  rows = rows || 1;
  cols = cols || 4;
  const section = newContainer(
    gridTemplate,
    'grid-gap-cols-5 grid-gap-rows-5',
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

export function newApplication(parentContainer, appFromRegistry) {
  const application = JSON.parse(JSON.stringify(applicationModel));
  application.contentId = appFromRegistry.contentId;
  application.title = appFromRegistry.displayName;
  application.showApplicationMode = true;
  if (!parentContainer.children) {
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

export function getX(event) {
  return event.x;
}

export function getY(event) {
  return event.y;
}

function newCell(section, index, rows, cols) {
  const container = newContainer(cellTemplate,
    null,
    (index === 0 || index) && section || null,
    index);
  applyBreakpointValues(container, rows, cols);
  applyGridStyle(container);
  return container;
}

function newContainer(template, cssClass, parentContainer, index) {
  const container = JSON.parse(JSON.stringify(containerModel));
  container.template = template;
  container.cssClass = cssClass || '';
  container.storageId = parseInt(Math.random() * 65536);
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

function parseSection(section) {
  if (section.template !== gridTemplate || !section.children.length) {
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

function parseCell(colContainer) {
  if (colContainer.template !== cellTemplate) {
    throw Error(`Container template '${colContainer.template}' not compatible. Fallback to old editor.`);
  }
  colContainer.colBreakpoints = parseBreakpointClasses(colContainer, 'grid-cell-colspan');
  colContainer.rowBreakpoints = parseBreakpointClasses(colContainer, 'grid-cell-rowspan');
  colContainer.gap = parseGapClasses(colContainer, 'grid-cell-gap');
  colContainer.colsCount = colContainer.colBreakpoints[currentBreakpoint];
  colContainer.rowsCount = colContainer.rowBreakpoints[currentBreakpoint];
}

function applyGridStyle(container) {
  if (container.template === gridTemplate) {
    container.children.forEach(applyGridStyle);
    applyBreakpointClasses(container, 'grid-rows', 'grid-cols');
  } else if (container.template === cellTemplate) {
    applyBreakpointClasses(container, 'grid-cell-rowspan', 'grid-cell-colspan');
  }
}

function applyBreakpointClasses(container, rowClassPrefix, colClassPrefix) {
  let cssClasses = container.cssClass || '';

  const colClasses = cssClasses.match(new RegExp(`(^| )${colClassPrefix}-((sm|md|lg|xl)-)?[0-9]{1,2}`, 'g'));
  // Remove old Classes
  if (colClasses?.length) {
    colClasses.forEach(c => cssClasses = cssClasses.replace(c, ''));
  }
  // Apply new Classes
  cssClasses = cssClasses.replace(/  +/g, ' ');
  if (container.colBreakpoints) {
    breakpoints.forEach(b => cssClasses += ` ${colClassPrefix}${b !== 'xs' && `-${  b}` || ''}-${container.colBreakpoints[b]}`);
  }

  const rowClasses = cssClasses.match(new RegExp(`(^| )${rowClassPrefix}-((sm|md|lg|xl)-)?[0-9]{1,2}`, 'g'));
  // Remove old Classes
  if (rowClasses?.length) {
    rowClasses.forEach(c => cssClasses = cssClasses.replace(c, ''));
  }
  // Apply new Classes
  if (container.rowBreakpoints) {
    breakpoints.forEach(b => cssClasses += ` ${rowClassPrefix}${b !== 'xs' && `-${  b}` || ''}-${container.rowBreakpoints[b]}`);
  }
  container.cssClass = cssClasses;
}

function parseBreakpointClasses(container, classPrefix) {
  const classes = container.cssClass.match(new RegExp(`(^| )${classPrefix}-((sm|md|lg|xl)-)?[0-9]{1,2}`, 'g'));
  const bp = {
    xs: 0,
    sm: 0,
    md: 0,
    lg: 0,
    xl: 0,
  };
  breakpoints.forEach(b => {
    const prefix = b === 'xs' ?  `${classPrefix}-` : `${classPrefix}-${b}-`;
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
  bp.xl = bp.xl || bp.lg || bp.md || bp.sm || bp.xs;
  bp.lg = bp.lg || bp.md || bp.sm || bp.xs;
  bp.md = bp.md || bp.sm || bp.xs;
  bp.sm = bp.sm || bp.xs;
  if (bp.xl) {
    return bp;
  } else {
    throw Error(`CSS classes '${container.cssClass}' not compatible. Fallback to old editor.`);
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
      let exists = false;
      let cell = matrix[row][col];
      if (cell) {
        exists = children.find(c => c.storageId === cell.storageId);
        if (exists
            && ((cell.rowIndex !== row && matrix[row - 1][col].storageId !== cell.storageId)
            || (cell.colIndex !== col && matrix[row][col - 1].storageId !== cell.storageId))) {
          matrix[row][col] = null;
          cell = null;
          exists = false;
        }
      }
      if (!cell) {
        cell = newCell(section, children.length, 1, 1);
        matrix[row][col] = cell;
      }
      if (!exists) {
        cell = JSON.parse(JSON.stringify(cell));
        cell.rowIndex = row;
        cell.colIndex = col;
        children.push(cell);
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
