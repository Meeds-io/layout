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

import SiteTemplateManagement from './components/SiteTemplateManagement.vue';

import SiteTemplateToolbar from './components/header/Toolbar.vue';

import SiteTemplateList from './components/main/List.vue';
import SiteTemplateItem from './components/main/Item.vue';
import SiteTemplateMenu from './components/main/Menu.vue';

import BulkDelete from './components/action/BulkDelete.vue';

import SiteTemplateDeserializeDrawer from './components/drawer/SiteTemplateDeserializeDrawer.vue';

const components = {
  'site-template-management': SiteTemplateManagement,

  'site-template-toolbar': SiteTemplateToolbar,

  'site-template-list': SiteTemplateList,
  'site-template-item': SiteTemplateItem,
  'site-template-menu': SiteTemplateMenu,

  'site-templates-bulk-delete': BulkDelete,

  'site-templates-deserialize-drawer': SiteTemplateDeserializeDrawer,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
