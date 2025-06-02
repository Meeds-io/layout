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

import SiteNavigation from './components/SiteNavigation.vue';
import SiteNavigationButton from './components/SiteNavigationButton.vue';
import SiteNavigationDrawersActions from './components/SiteNavigationDrawersActions.vue';
import SpaceTemplateEditLayoutSpaceMenuItem from './components/SpaceTemplateEditLayoutSpaceMenuItem.vue';
import SpaceTemplateRestoreDefaultPagesSpaceMenuItem from './components/SpaceTemplateRestoreDefaultPagesSpaceMenuItem.vue';
import SpaceTemplateCreateListener from './components/SpaceTemplateCreateListener.vue';

const components = {
  'site-navigation': SiteNavigation,
  'site-navigation-button': SiteNavigationButton,
  'site-navigation-drawers-actions': SiteNavigationDrawersActions,
  'site-navigation-space-template-manage-layout': SpaceTemplateEditLayoutSpaceMenuItem,
  'site-navigation-space-template-restore-default-pages': SpaceTemplateRestoreDefaultPagesSpaceMenuItem,
  'site-navigation-space-template-create-listener': SpaceTemplateCreateListener,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
