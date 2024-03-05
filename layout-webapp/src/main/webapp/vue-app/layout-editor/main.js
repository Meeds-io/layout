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
import './extensions.js';
import './services.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('layoutEditor');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'layoutEditor';

//getting language of the PLF
const lang = eXo?.env.portal.language || 'en';

//should expose the locale ressources as REST API
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.LayoutEditor-${lang}.json`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n =>
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<layout-editor id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: {
          containerTypes: extensionRegistry.loadExtensions('layout-editor', 'container'),
          editPage: true,
          layout: null,
          draggedContainer: null,
          draggedSection: null,
          resizeDimensions: null,
          resizeParentId: null,
        },
        computed: {
          resizeMouseX() {
            return this.resizeDimensions && (this.resizeDimensions.x + this.resizeDimensions.width);
          },
          resizeMouseY() {
            return this.resizeDimensions && (this.resizeDimensions.y + this.resizeDimensions.height);
          },
        },
        created() {
          document.addEventListener('extension-layout-editor-container-updated', this.refreshContainerTypes);
        },
        methods: {
          refreshContainerTypes() {
            this.containerTypes = extensionRegistry.loadExtensions('layout-editor', 'container');
          },
        },
      }, `#${appId}`, 'Layout Editor')
    );
}
