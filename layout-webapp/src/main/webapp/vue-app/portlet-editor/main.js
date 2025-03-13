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
import '../common-portlets/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('PortletEditor');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'portletEditor';

//getting language of the PLF
const lang = eXo?.env.portal.language || 'en';

//should expose the locale ressources as REST API
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

export function init () {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<portlet-editor id="${appId}"/>`,
        vuetify: eXo.vuetify,
        i18n,
        data: {
          portletInstanceId: null,
          portletInstance: null,
          portletInstanceEmpty: true,
          portletMode: 'view',
        },
        computed: {
          editablePortlet () {
            return this.portletInstance?.editable || false;
          },
        },
        watch: {
          portletInstanceId: {
            immediate: true,
            handler () {
              eXo.env.portal.portletInstanceId = this.portletInstanceId;
            },
          },
          portletMode: {
            immediate: true,
            handler () {
              eXo.env.portal.maximizedPortletMode = this.portletMode;
            },
          },
        },
        created () {
          this.portletInstanceId = this.getQueryParam('id');
          this.portletMode = this.getQueryParam('portletMode') || 'view';
          eXo.$portletInstanceService.getPortletInstance(this.portletInstanceId)
            .then(data => this.portletInstance = data)
            .finally(() => this.aapplicationLoaded());
        },
        methods: {
          getQueryParam (paramName) {
            const uri = window.location.search.substring(1);
            const params = new URLSearchParams(uri);
            return params.get(paramName);
          },
        },
      }, `#${appId}`, 'Portlet Editor');
    });
}
