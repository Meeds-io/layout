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
import '../common-site-template/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('siteManagement');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'siteManagement';

//getting language of the PLF
const lang = eXo?.env.portal.language || 'en';

//should expose the locale ressources as REST API
const urls = [
  `/layout/i18n/locale.portlet.SiteManagement?lang=${lang}`,
  `/layout/i18n/locale.portlet.SiteNavigation?lang=${lang}`,
  `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`,
];

export function init () {
  exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => {
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<site-management id="${appId}"/>`,
        vuetify: eXo.vuetify,
        i18n,
        data: () => ({
          pageTemplates: null,
          collator: new Intl.Collator(eXo.env.portal.language, { numeric: true, sensitivity: 'base' }),
        }),
        computed: {
          isMobile () {
            return eXo.vuetify.display.smAndDown.value;
          },
        },
      }, `#${appId}`, 'site-management');
    }).finally(() => eXo.$utils?.includeExtensions?.('SiteManagementExtension'));

}