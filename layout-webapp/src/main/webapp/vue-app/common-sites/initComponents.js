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

import SiteNavigationDrawer from './components/site-navigation/SiteNavigationDrawer.vue';
import SiteNavigationNodesList from './components/site-navigation/NodesList.vue';
import SiteNavigationNodeItem from './components/site-navigation/NodeItem.vue';
import SiteNavigationNodeItemMenu from './components/site-navigation/NodeItemMenu.vue';
import SiteNavigationNodeDrawer from './components/site-navigation/SiteNavigationNodeDrawer.vue';
import SiteNavigationElementDrawer from './components/site-navigation/SiteNavigationElementDrawer.vue';
import SiteNavigationPageElement from './components/site-navigation/SiteNavigationPageElement.vue';
import SiteNavigationNewPageElement from './components/site-navigation/SiteNavigationNewPageElement.vue';
import SiteNavigationScheduleDatePickers from './components/site-navigation/SiteNavigationScheduleDatePickers.vue';
import SiteNavigationExistingPageElement from './components/site-navigation/SiteNavigationExistingPageElement.vue';
import SiteNavigationPageSuggester from './components/site-navigation/SiteNavigationPageSuggester.vue';
import SiteNavigationSiteSuggester from './components/site-navigation/SiteNavigationSiteSuggester.vue';
import SiteNavigationNewPageElementItemsList from './components/site-navigation/SiteNavigationNewPageElementItemsList.vue';
import SiteNavigationNewPageElementItem from './components/site-navigation/SiteNavigationNewPageElementItem.vue';
import SiteNavigationIcon from './components/site-navigation/SiteNavigationIcon.vue';
import SiteNavigationIconInput from './components/site-navigation/SiteNavigationIconInput.vue';

import ManagePermissionsDrawer from './components/manage-permissions/ManagePermissionsDrawer.vue';
import AccessPermissions from './components/manage-permissions/AccessPermissions.vue';
import EditPermission from './components/manage-permissions/EditPermission.vue';

const components = {
  'manage-permissions-drawer': ManagePermissionsDrawer,
  'manage-permissions-access': AccessPermissions,
  'manage-permissions-edit': EditPermission,
  'site-navigation-drawer': SiteNavigationDrawer,
  'site-navigation-nodes-list': SiteNavigationNodesList,
  'site-navigation-node-item': SiteNavigationNodeItem,
  'site-navigation-node-item-menu': SiteNavigationNodeItemMenu,
  'site-navigation-node-drawer': SiteNavigationNodeDrawer,
  'site-navigation-element-drawer': SiteNavigationElementDrawer,
  'site-navigation-page-element': SiteNavigationPageElement,
  'site-navigation-new-page-element-item-list': SiteNavigationNewPageElementItemsList,
  'site-navigation-new-page-element': SiteNavigationNewPageElement,
  'site-navigation-new-page-element-item': SiteNavigationNewPageElementItem,
  'site-navigation-schedule-date-pickers': SiteNavigationScheduleDatePickers,
  'site-navigation-existing-page-element': SiteNavigationExistingPageElement,
  'site-navigation-page-suggester': SiteNavigationPageSuggester,
  'site-navigation-site-suggester': SiteNavigationSiteSuggester,
  'site-navigation-icon': SiteNavigationIcon,
  'site-navigation-icon-input': SiteNavigationIconInput,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
