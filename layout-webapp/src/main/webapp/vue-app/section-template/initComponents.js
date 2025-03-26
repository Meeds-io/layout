/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import SectionTemplateManagement from './components/SectionTemplateManagement.vue';

import SectionTemplateToolbar from './components/header/Toolbar.vue';

import SectionTemplateList from './components/main/List.vue';
import SectionTemplateItem from './components/main/Item.vue';
import SectionTemplateMenu from './components/main/Menu.vue';

import AddSectionTemplateDrawer from './components/drawer/AddSectionTemplateDrawer.vue';
import BulkDelete from './components/action/BulkDelete.vue';
import SectionTemplateDeserializeDrawer from './components/drawer/SectionTemplateDeserializeDrawer.vue';

const components = {
  'section-template-management': SectionTemplateManagement,

  'section-template-toolbar': SectionTemplateToolbar,

  'section-template-list': SectionTemplateList,
  'section-template-item': SectionTemplateItem,
  'section-template-menu': SectionTemplateMenu,

  'section-template-add-drawer': AddSectionTemplateDrawer,

  'section-templates-bulk-delete': BulkDelete,

  'section-templates-deserialize-drawer': SectionTemplateDeserializeDrawer,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
