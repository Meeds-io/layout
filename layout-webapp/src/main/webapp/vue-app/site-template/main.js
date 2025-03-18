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

import './initComponents.js';
import '../common-layout/services.js';
import '../common-sites/main.js';
import '../common-illustration/main.js';
import '../common-site-template/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('SiteTemplateManagement');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env.portal.language || 'en';
const urls = [
  `/layout/i18n/locale.portlet.SiteManagement?lang=${lang}`,
  `/layout/i18n/locale.portlet.SiteNavigation?lang=${lang}`,
  `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`
];

const appId = 'siteTemplateManagement';
export function init() {
  exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n =>
      Vue.createApp({
        template: `<site-template-management id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: () => ({
          siteTemplates: [],
          allSiteTemplatesSelected: false,
          selectedSiteTemplates: [],
          siteTemplatesSize: 0,
          processedSiteTemplates: 0,
          isBulkProcessing: false,
          loading: 0,
          collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
        }),
        computed: {
          isMobile() {
            return this.$vuetify.breakpoint.smAndDown;
          },
          systemSelectedSiteTemplates() {
            return this.selectedSiteTemplates.every(template => template.system);
          }
        },
        created() {
          this.$root.$on('site-templates-list-refresh', this.refreshSiteTemplates);
          this.$root.$on('site-template-enabled', this.refreshSiteTemplates);
          this.$root.$on('site-template-disabled', this.refreshSiteTemplates);
          this.$root.$on('site-template-created', this.refreshSiteTemplates);
          this.$root.$on('site-template-updated', this.refreshSiteTemplates);
          this.$root.$on('site-template-deleted', this.refreshSiteTemplates);

          this.refreshSiteTemplates();
        },
        methods: {
          refreshSiteTemplates() {
            this.loading++;
            return this.$siteTemplateService.getSiteTemplates()
              .then(data => this.siteTemplates = data || [])
              .finally(() => this.loading--);
          },

          async applyOperationInBulk(callback, params, onFinish, onCancel) {
            this.processedSiteTemplates = 0;
            this.isBulkProcessing = true;
            this.$emit('site-templates-bulk-operation-status', null, 'disabled');
            try {
              if (this.allSiteTemplatesSelected) {
                let index = 0;
                do {
                  while (index < this.siteTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnSiteTemplate(this.siteTemplates[index++], params, callback);
                  }
                  if (index >= this.siteTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    this.selectedSiteTemplates = this.siteTemplates;
                  }
                } while (index < this.siteTemplates.length && this.isBulkProcessing);
              } else {
                for (const element of this.siteTemplates) {
                  if (!this.isBulkProcessing) {
                    break;
                  }
                  const siteTemplate = element;
                  if (this.selectedSiteTemplates.find(s => s.id === siteTemplate.id)) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnSiteTemplate(siteTemplate, params, callback);
                  }
                }
              }
            } finally {
              this.allSiteTemplatesSelected = false;
              this.selectedSiteTemplates = [];
              this.$emit('site-templates-bulk-operation-status', null, null);
              if (this.isBulkProcessing) {
                this.isBulkProcessing = false;
                await this.$nextTick();
                if (onFinish) {
                  onFinish(params);
                }
              } else if (onCancel) {
                onCancel(params);
              }
            }
          },
          async applyOperationOnSiteTemplate(siteTemplate, params, callback) {
            this.$emit('site-templates-bulk-operation-status', siteTemplate.id, 'processing');
            try {
              await callback(siteTemplate, params);
              this.$emit('site-templates-bulk-operation-status', siteTemplate.id, 'done');
            } catch (e) {
              // eslint-disable-next-line no-console
              console.error('Error processing site template ', siteTemplate.id, '. Error: ', e);
              this.$emit('site-templates-bulk-operation-status', siteTemplate.id, 'error');
            } finally {
              this.processedSiteTemplates++;
            }
          },
        },
      }, `#${appId}`, 'Section Template Management')
    );
}
