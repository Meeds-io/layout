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

import PortletEditor from './components/PortletEditor.vue';

import Toolbar from './components/toolbar/Toolbar.vue';
import EditButton from './components/toolbar/actions/EditButton.vue';
import SaveButton from './components/toolbar/actions/SaveButton.vue';

import Content from './components/content/Content.vue';
import Cell from './components/content/Cell.vue';
import CellResizeButton from './components/content/CellResizeButton.vue';
import Application from './components/content/Application.vue';

const components = {
  'portlet-editor': PortletEditor,
  'portlet-editor-toolbar': Toolbar,
  'portlet-editor-toolbar-edit-button': EditButton,
  'portlet-editor-toolbar-save-button': SaveButton,
  'portlet-editor-content': Content,
  'portlet-editor-cell': Cell,
  'portlet-editor-cell-resize-button': CellResizeButton,
  'portlet-editor-application': Application,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
