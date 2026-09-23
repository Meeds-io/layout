/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import SectionTemplate from './components/form/SectionTemplate.vue';

import ContainerExtension from './components/content/base/ContainerExtension.vue';
import ContainerBase from './components/content/base/ContainerBase.vue';

import SectionMenu from './components/content/common/SectionMenu.vue';
import SectionMenuTop from './components/content/common/SectionMenuTop.vue';
import SectionMenuBottom from './components/content/common/SectionMenuBottom.vue';
import SectionMenuLeft from './components/content/common/SectionMenuLeft.vue';
import SectionMenuRight from './components/content/common/SectionMenuRight.vue';
import SectionSelectionGrid from './components/content/common/SectionSelectionGrid.vue';
import SectionSelectionGridCell from './components/content/common/SectionSelectionGridCell.vue';
import ApplicationCategoryCard from './components/content/common/ApplicationCategoryCard.vue';
import ApplicationCard from './components/content/common/ApplicationCard.vue';
import ApplicationMenu from './components/content/common/ApplicationMenu.vue';
import CellResizeButton from './components/content/common/CellResizeButton.vue';
import CellsDropBox from './components/content/common/CellsDropBox.vue';
import CellsSelectionBox from './components/content/common/CellsSelectionBox.vue';

import Container from './components/content/container/Container.vue';
import Section from './components/content/container/Section.vue';
import Cell from './components/content/container/Cell.vue';
import Application from './components/content/container/Application.vue';

import SelectApplicationCategoryDrawer from './components/drawer/SelectApplicationCategoryDrawer.vue';
import AddApplicationDrawer from './components/drawer/AddApplicationDrawer.vue';
import EditApplicationDrawer from './components/drawer/EditApplicationDrawer.vue';
import EditPageDrawer from './components/drawer/EditPageDrawer.vue';
import EditSectionDrawer from './components/drawer/EditSectionDrawer.vue';

import EditPortletDialog from './components/dialog/EditPortletDialog.vue';

// eXIP 7.3.0.30: the styling inputs live in social's shared 'stylingInputs' module (this module depends on it);
// they stay reachable under their historical layout-editor-* tags
function sharedStylingInput(tag) {
  const component = Vue.options.components[tag];
  if (!component) {
    throw new Error(`Shared styling input '${tag}' is not registered: the stylingInputs module must be loaded first`);
  }
  return component;
}

const components = {
  'layout-editor-color-picker': sharedStylingInput('styling-color-picker'),
  'layout-editor-border-radius-selector': sharedStylingInput('styling-border-radius-selector'),
  'layout-editor-container': Container,
  'layout-editor-container-extension': ContainerExtension,
  'layout-editor-container-base': ContainerBase,
  'layout-editor-container-section': Section,
  'layout-editor-section-template': SectionTemplate,
  'layout-editor-container-cell': Cell,
  'layout-editor-container-application': Application,
  'layout-editor-section-selection-grid': SectionSelectionGrid,
  'layout-editor-section-selection-grid-cell': SectionSelectionGridCell,
  'layout-editor-section-menu': SectionMenu,
  'layout-editor-section-menu-top': SectionMenuTop,
  'layout-editor-section-menu-bottom': SectionMenuBottom,
  'layout-editor-section-menu-left': SectionMenuLeft,
  'layout-editor-section-menu-right': SectionMenuRight,
  'layout-editor-application-category-select-drawer': SelectApplicationCategoryDrawer,
  'layout-editor-application-add-drawer': AddApplicationDrawer,
  'layout-editor-application-edit-drawer': EditApplicationDrawer,
  'layout-editor-page-edit-drawer': EditPageDrawer,
  'layout-editor-section-edit-drawer': EditSectionDrawer,
  'layout-editor-portlet-edit-dialog': EditPortletDialog,
  'layout-editor-background-image-attachment': sharedStylingInput('styling-background-image-attachment'),
  'layout-editor-background-input': sharedStylingInput('styling-background-input'),
  'layout-editor-background-margin-input': sharedStylingInput('styling-background-margin-input'),
  'layout-editor-background-radius-input': sharedStylingInput('styling-background-radius-input'),
  'layout-editor-text-input': sharedStylingInput('styling-text-input'),
  'layout-editor-text-background-input': sharedStylingInput('styling-text-background-input'),
  'layout-editor-margin-input': sharedStylingInput('styling-margin-input'),
  'layout-editor-section-margin-input': sharedStylingInput('styling-section-margin-input'),
  'layout-editor-border-input': sharedStylingInput('styling-border-input'),
  'layout-editor-border-radius-input': sharedStylingInput('styling-border-radius-input'),
  'layout-editor-application-card': ApplicationCard,
  'layout-editor-application-category-card': ApplicationCategoryCard,
  'layout-editor-application-menu': ApplicationMenu,
  'layout-editor-cell-resize-button': CellResizeButton,
  'layout-editor-cells-selection-box': CellsSelectionBox,
  'layout-editor-cells-drop-box': CellsDropBox,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
