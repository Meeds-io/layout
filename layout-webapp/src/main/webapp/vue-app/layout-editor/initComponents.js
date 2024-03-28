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

import LayoutEditor from './components/LayoutEditor.vue';

import Toolbar from './components/toolbar/Toolbar.vue';
import SaveButton from './components/toolbar/actions/SaveButton.vue';
import HistoryButtons from './components/toolbar/actions/HistoryButtons.vue';
import PreviewButton from './components/toolbar/actions/PreviewButton.vue';

import BorderRadiusSelector from './components/form/BorderRadiusSelector.vue';
import BorderInput from './components/form/BorderInput.vue';
import ColorPicker from './components/form/ColorPicker.vue';

import Content from './components/content/Content.vue';

import CellsDropBox from './components/content/base/CellsDropBox.vue';
import CellsSelectionBox from './components/content/base/CellsSelectionBox.vue';
import ContainerExtension from './components/content/base/ContainerExtension.vue';
import ContainerBase from './components/content/base/ContainerBase.vue';

import SectionBorderMenu from './components/content/common/SectionBorderMenu.vue';
import SectionSelectionGrid from './components/content/common/SectionSelectionGrid.vue';
import SectionSelectionGridCell from './components/content/common/SectionSelectionGridCell.vue';
import SectionGridEditor from './components/content/common/SectionGridEditor.vue';
import SectionFlexEditor from './components/content/common/SectionFlexEditor.vue';
import ApplicationCategoryCard from './components/content/common/ApplicationCategoryCard.vue';
import ApplicationCard from './components/content/common/ApplicationCard.vue';
import CellTopMenu from './components/content/common/CellTopMenu.vue';

import Page from './components/content/container/Page.vue';
import Container from './components/content/container/Container.vue';
import Section from './components/content/container/Section.vue';
import Cell from './components/content/container/Cell.vue';
import Application from './components/content/container/Application.vue';

import EditSectionDrawer from './components/drawer/EditSectionDrawer.vue';
import AddSectionDrawer from './components/drawer/AddSectionDrawer.vue';
import AddApplicationDrawer from './components/drawer/AddApplicationDrawer.vue';
import EditApplicationDrawer from './components/drawer/EditApplicationDrawer.vue';

const components = {
  'layout-editor': LayoutEditor,
  'layout-editor-toolbar': Toolbar,
  'layout-editor-toolbar-save-button': SaveButton,
  'layout-editor-toolbar-history-buttons': HistoryButtons,
  'layout-editor-toolbar-preview-button': PreviewButton,
  'layout-editor-content': Content,
  'layout-editor-color-picker': ColorPicker,
  'layout-editor-border-radius-selector': BorderRadiusSelector,
  'layout-editor-border-input': BorderInput,
  'layout-editor-container': Container,
  'layout-editor-container-extension': ContainerExtension,
  'layout-editor-container-base': ContainerBase,
  'layout-editor-container-page': Page,
  'layout-editor-container-section': Section,
  'layout-editor-container-cell': Cell,
  'layout-editor-container-application': Application,
  'layout-editor-section-selection-grid': SectionSelectionGrid,
  'layout-editor-section-selection-grid-cell': SectionSelectionGridCell,
  'layout-editor-section-grid-editor': SectionGridEditor,
  'layout-editor-section-flex-editor': SectionFlexEditor,
  'layout-editor-section-add-drawer': AddSectionDrawer,
  'layout-editor-section-edit-drawer': EditSectionDrawer,
  'layout-editor-section-border-menu': SectionBorderMenu,
  'layout-editor-application-add-drawer': AddApplicationDrawer,
  'layout-editor-application-edit-drawer': EditApplicationDrawer,
  'layout-editor-application-card': ApplicationCard,
  'layout-editor-application-category-card': ApplicationCategoryCard,
  'layout-editor-cell-top-menu': CellTopMenu,
  'layout-editor-cells-selection-box': CellsSelectionBox,
  'layout-editor-cells-drop-box': CellsDropBox,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
