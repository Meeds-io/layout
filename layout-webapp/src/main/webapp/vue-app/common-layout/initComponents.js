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

import BorderRadiusSelector from './components/form/BorderRadiusSelector.vue';
import ColorPicker from './components/form/ColorPicker.vue';
import BackgroundImageAttachment from './components/form/BackgroundImageAttachment.vue';
import BackgroundInput from './components/form/BackgroundInput.vue';
import BackgroundMarginInput from './components/form/BackgroundMarginInput.vue';
import BackgroundRadiusInput from './components/form/BackgroundRadiusInput.vue';
import TextInput from './components/form/TextInput.vue';
import MarginInput from './components/form/MarginInput.vue';
import SectionMarginInput from './components/form/SectionMarginInput.vue';
import BorderInput from './components/form/BorderInput.vue';
import BorderRadiusInput from './components/form/BorderRadiusInput.vue';
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

const components = {
  'layout-editor-color-picker': ColorPicker,
  'layout-editor-border-radius-selector': BorderRadiusSelector,
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
  'layout-editor-background-image-attachment': BackgroundImageAttachment,
  'layout-editor-background-input': BackgroundInput,
  'layout-editor-background-margin-input': BackgroundMarginInput,
  'layout-editor-background-radius-input': BackgroundRadiusInput,
  'layout-editor-text-input': TextInput,
  'layout-editor-margin-input': MarginInput,
  'layout-editor-section-margin-input': SectionMarginInput,
  'layout-editor-border-input': BorderInput,
  'layout-editor-border-radius-input': BorderRadiusInput,
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
