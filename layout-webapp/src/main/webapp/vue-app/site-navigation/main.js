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

import './initComponents.js';
import '../common-sites/main.js';
import '../common-illustration/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('siteNavigation');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

extensionRegistry.registerComponent('manageSpaceActions', 'manage-space-actions', {
  id: 'manage-space-actions',
  vueComponent: Vue.component['site-navigation-button'],
  rank: 20,
});

extensionRegistry.registerComponent('manageSpaceDrawers', 'manage-space-drawers', {
  id: 'manage-space-drawers',
  vueComponent: Vue.component['site-navigation-drawers-actions'],
  rank: 20,
});

extensionRegistry.registerComponent('space-templates', 'space-templates-drawers', {
  id: 'manage-space-drawers',
  vueComponent: Vue.component['site-navigation-drawers-actions'],
  rank: 10,
});

extensionRegistry.registerExtension('space-templates', 'space-templates-item-action', {
  rank: 25,
  name: 'manage-layout',
  componentName: 'site-navigation-space-template-manage-layout',
});

extensionRegistry.registerExtension('space-templates', 'space-templates-main', {
  rank: 10,
  name: 'manage-layout',
  componentName: 'site-navigation-space-template-create-listener',
});

const appId = 'siteNavigation';

//getting language of the PLF
const lang = eXo && eXo.env.portal.language || 'en';

//should expose the locale ressources as REST API
const url = `/layout/i18n/locale.portlet.SiteNavigation?lang=${lang}`;
const i18nPromise = exoi18n.loadLanguageAsync(lang, url);

export function init (canManageSiteNavigation) {
  i18nPromise.then(i18n => {
    // init Vue app when locale ressources are ready
    Vue.createApp({
      template: `<site-navigation id="${appId}" :can-manage-site-navigation="${canManageSiteNavigation}"/>`,
      vuetify: eXo.vuetify,
      i18n,
      data: () => ({
        pageTemplates: null,
      }),
    }, `#${appId}`, 'site-navigation');
  });
}
