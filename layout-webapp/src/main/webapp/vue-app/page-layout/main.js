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

import '../common-page-layout/main.js';
import './initComponents.js';
import './extensions.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('PageLayout');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'PageLayout';
export function init (pageRef, siteId) {
  const containerTypes = extensionRegistry.loadExtensions('page-layout', 'container');
  Vue.createApp({
    template: `<page-layout id="${appId}"/>`,
    vuetify: eXo.vuetify,
    data: () => ({
      pageRef,
      siteId,
      containerTypes,
      nodeUri: window.location.pathname.replace('/portal/', '/'),
      page: null,
    }),
    computed: {
      defaultContainer () {
        return this.containerTypes.find(extension => extension.type === 'default');
      },
      layout () {
        if (this.page?.template === 'system:/groovy/portal/webui/container/UISiteLayout.gtmpl') {
          return this.page;
        } else if (this.page?.children?.[0]?.children?.[0]?.template === 'system:/groovy/portal/webui/container/UIPageLayout.gtmpl') {
          return this.page?.children?.[0]?.children?.[0];
        } else {
          return this.page?.children?.[0];
        }
      },
      isMobile () {
        return eXo.vuetify.display.smAndDown.value;
      },
    },
    async created () {
      document.addEventListener('extension-page-layout-container-updated', this.refreshContainerTypes);
      this.$root.page = await eXo.$pageLayoutService.getPageLayout({
        pageRef: this.$root.pageRef,
        siteId: this.$root.siteId,
      });
    },
    mounted () {
      this.$el?.closest?.('.PORTLET-FRAGMENT')?.classList?.remove?.('PORTLET-FRAGMENT');
    },
    methods: {
      refreshContainerTypes () {
        this.containerTypes = extensionRegistry.loadExtensions('page-layout', 'container');
      },
    },
  }, `#${appId}`, 'Page Layout');
  eXo.$utils.includeExtensions('PageLayoutExtension');
}
