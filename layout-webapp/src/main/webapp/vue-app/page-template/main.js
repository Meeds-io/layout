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
import '../common-page-template/main.js';
import '../common-illustration/initComponents.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('PageTemplatesManagement');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env.portal.language || 'en';
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

const appId = 'pageTemplatesManagement';
export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n =>
      Vue.createApp({
        template: `<page-templates-management id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: () => ({
          pageTemplates: [],
          loading: 0,
          collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
          allPageTemplatesSelected: false,
          selectedPageTemplates: [],
          pageTemplatesSize: 0,
          processedPageTemplates: 0,
          isBulkProcessing: false,
          columnsTemplate: null
        }),
        computed: {
          isMobile() {
            return this.$vuetify.breakpoint.smAndDown;
          },
          systemSelectedPageTemplates() {
            return this.selectedPageTemplates.every(template => template.system);
          }
        },
        created() {
          this.$root.$on('page-templates-list-refresh', this.refreshPageTemplates);
          this.$root.$on('page-templates-deleted', this.refreshPageTemplates);
          this.$root.$on('page-templates-restored', this.refreshPageTemplates);
          this.$root.$on('page-templates-created', this.refreshPageTemplates);
          this.$root.$on('page-templates-updated', this.refreshPageTemplates);
          this.$root.$on('page-templates-enabled', this.refreshPageTemplates);
          this.$root.$on('page-templates-disabled', this.refreshPageTemplates);
          this.$root.$on('page-templates-saved', this.refreshPageTemplates);
          this.refreshPageTemplates();
          this.retrieveColumnsTemplate();
        },
        beforeDestroy() {
          this.$root.$off('page-templates-list-refresh', this.refreshPageTemplates);
          this.$root.$off('page-templates-deleted', this.refreshPageTemplates);
          this.$root.$off('page-templates-restored', this.refreshPageTemplates);
          this.$root.$off('page-templates-created', this.refreshPageTemplates);
          this.$root.$off('page-templates-updated', this.refreshPageTemplates);
          this.$root.$off('page-templates-enabled', this.refreshPageTemplates);
          this.$root.$off('page-templates-disabled', this.refreshPageTemplates);
          this.$root.$off('page-templates-saved', this.refreshPageTemplates);
        },
        methods: {
          refreshPageTemplates() {
            this.loading = true;
            return this.$pageTemplateService.getPageTemplates()
              .then(pageTemplates => this.pageTemplates = pageTemplates || [])
              .finally(() => this.loading = false);
          },
          retrieveColumnsTemplate() {
            return this.$pageTemplateService.getPageTemplates(true)
              .then(pageTemplates => {
                this.columnsTemplate = pageTemplates?.find?.(t => t.system && t.content.includes('FlexContainer'));
              })
              .finally(() => this.contentLoaded = true);
          },
          async createPageTemplate() {
            this.creating = true;
            try {
              const columnsTemplateContent = this.columnsTemplate?.content || '{}';
              const pageTemplate = await this.$pageTemplateService.createPageTemplate(columnsTemplateContent, true);
              window.open(`/portal/administration/layout-editor?pageTemplateId=${pageTemplate.id}`, '_blank');
            } finally {
              this.creating = false;
            }
          },
          async applyOperationInBulk(callback, params, onFinish, onCancel) {
            this.processedPageTemplates = 0;
            this.isBulkProcessing = true;
            this.$emit('page-templates-bulk-operation-status', null, 'disabled');
            try {
              if (this.allPageTemplatesSelected) {
                let index = 0;
                do {
                  while (index < this.pageTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnPageTemplate(this.pageTemplates[index++], params, callback);
                  }
                  if (index >= this.pageTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    this.selectedPageTemplates = this.pageTemplates;
                  }
                } while (index < this.pageTemplates.length && this.isBulkProcessing);
              } else {
                for (const element of this.pageTemplates) {
                  if (!this.isBulkProcessing) {
                    break;
                  }
                  const pageTemplate = element;
                  if (this.selectedPageTemplates.find(s => s.id === pageTemplate.id)) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnPageTemplate(pageTemplate, params, callback);
                  }
                }
              }
            } finally {
              this.allPageTemplatesSelected = false;
              this.selectedPageTemplates = [];
              this.$emit('page-templates-bulk-operation-status', null, null);
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
          async applyOperationOnPageTemplate(pageTemplate, params, callback) {
            this.$emit('page-templates-bulk-operation-status', pageTemplate.id, 'processing');
            try {
              await callback(pageTemplate, params);
              this.$emit('page-templates-bulk-operation-status', pageTemplate.id, 'done');
            } catch (e) {
              // eslint-disable-next-line no-console
              console.error('Error processing page template ', pageTemplate.id, '. Error: ', e);
              this.$emit('page-templates-bulk-operation-status', pageTemplate.id, 'error');
            } finally {
              this.processedPageTemplates++;
            }
          },
        },
      }, `#${appId}`, 'Page Layout')
    );
}
