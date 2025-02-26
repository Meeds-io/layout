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
import '../common-illustration/main.js';
import '../common-portlets/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('PortletsManagement');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env.portal.language || 'en';
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

const appId = 'portletsManagement';
export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n =>
      Vue.createApp({
        template: `<portlets-management id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: () => ({
          portlets: [],
          portletInstances: [],
          portletInstanceCategories: [],
          selectedCategoryId: null,
          loading: 0,
          collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
          allPortletInstancesSelected: false,
          selectedPortletInstances: [],
          portletInstancesSize: 0
        }),
        computed: {
          isMobile() {
            return this.$vuetify.breakpoint.smAndDown;
          },
          categoriesById() {
            return this.portletInstanceCategories.reduce((a, v) => {
              a[v.id] = v;
              return a;
            }, {});
          },
          portletsById() {
            return this.portlets.reduce((a, v) => {
              a[v.contentId] = v;
              return a;
            }, {});
          },
        },
        created() {
          this.$root.$on('portlet-instance-enabled', this.refreshPortletInstances);
          this.$root.$on('portlet-instance-disabled', this.refreshPortletInstances);
          this.$root.$on('portlet-instance-saved', this.refreshPortletInstances);
          this.$root.$on('portlet-instance-deleted', this.refreshPortletInstances);
          this.$root.$on('portlet-instance-category-saved', this.refreshPortletInstanceCategories);
          this.$root.$on('portlet-instance-category-deleted', this.refreshPortletInstanceCategories);
          this.$root.$on('portlet-instance-category-deleted', this.refreshPortletInstances);
          this.$root.$on('portlet-instance-category-selected', this.selectCategory);
          window.addEventListener('portlet-instance-layout-updated', this.propagateEventListenerLocally);

          this.refreshPortlets();
          this.refreshPortletInstances();
          this.refreshPortletInstanceCategories();
        },
        methods: {
          propagateEventListenerLocally(event) {
            this.$root.$emit(event.type, event.detail);
          },
          selectCategory(categoryId) {
            this.selectedCategoryId = categoryId;
          },
          refreshPortlets() {
            this.loading++;
            return this.$portletService.getPortlets()
              .then(data => this.portlets = data.map(p => ({...p,
                name: this.$te(`layout.portletInstance.${p?.portletName}.name`) ? this.$t(`layout.portletInstance.${p?.portletName}.name`) : p?.name,
                description: this.$te(`layout.portletInstance.${p?.portletName}.description`) ? this.$t(`layout.portletInstance.${p?.portletName}.description`) : p?.description,
              })) || [])
              .finally(() => this.loading--);
          },
          refreshPortletInstances() {
            this.allPortletInstancesSelected = false;
            this.selectedPortletInstances = [];
            this.loading++;
            return this.$portletInstanceService.getPortletInstances()
              .then(data => {
                this.portletInstances = data || [];
                this.portletInstancesSize = this.portletInstances?.length;
              })
              .finally(() => this.loading--);
          },
          refreshPortletInstanceCategories() {
            this.loading++;
            return this.$portletInstanceCategoryService.getPortletInstanceCategories()
              .then(data => this.portletInstanceCategories = data || [])
              .finally(() => this.loading--);
          },
        },
      }, `#${appId}`, 'Portlets Management')
    );
}
