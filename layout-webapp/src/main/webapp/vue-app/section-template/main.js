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
import '../common-layout/main.js';
import '../common-illustration/main.js';
import '../common-section-template/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('SectionTemplateManagement');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env.portal.language || 'en';
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

const appId = 'sectionTemplateManagement';
export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n =>
      Vue.createApp({
        template: `<section-template-management id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: () => ({
          sectionTemplates: [],
          allSectionTemplatesSelected: false,
          selectedSectionTemplates: [],
          sectionTemplatesSize: 0,
          processedSectionTemplates: 0,
          isBulkProcessing: false,
          loading: 0,
          collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
        }),
        computed: {
          isMobile() {
            return this.$vuetify.breakpoint.smAndDown;
          },
          systemSelectedSectionTemplates() {
            return this.selectedSectionTemplates.every(template => template.system);
          }
        },
        created() {
          this.$root.$on('section-templates-list-refresh', this.refreshSectionTemplates);
          this.$root.$on('section-template-enabled', this.refreshSectionTemplates);
          this.$root.$on('section-template-disabled', this.refreshSectionTemplates);
          this.$root.$on('section-template-saved', this.refreshSectionTemplates);
          this.$root.$on('section-template-deleted', this.refreshSectionTemplates);
          this.$root.$on('section-template-restored', this.refreshSectionTemplates);
          window.addEventListener('section-template-layout-updated', this.propagateEventListenerLocally);

          this.refreshSectionTemplates();
        },
        beforeDestroy() {
          this.$root.$off('section-templates-list-refresh', this.refreshSectionTemplates);
          this.$root.$off('section-template-enabled', this.refreshSectionTemplates);
          this.$root.$off('section-template-disabled', this.refreshSectionTemplates);
          this.$root.$off('section-template-saved', this.refreshSectionTemplates);
          this.$root.$off('section-template-deleted', this.refreshSectionTemplates);
          this.$root.$off('section-template-restored', this.refreshSectionTemplates);
          window.removeEventListener('section-template-layout-updated', this.propagateEventListenerLocally);
        },
        methods: {
          propagateEventListenerLocally(event) {
            this.$root.$emit(event.type, event.detail);
          },
          refreshSectionTemplates() {
            this.loading++;
            return this.$sectionTemplateService.getSectionTemplates()
              .then(data => this.sectionTemplates = data || [])
              .finally(() => this.loading--);
          },
          async applyOperationInBulk(callback, params, onFinish, onCancel) {
            this.processedSectionTemplates = 0;
            this.isBulkProcessing = true;
            this.$emit('section-templates-bulk-operation-status', null, 'disabled');
            try {
              if (this.allSectionTemplatesSelected) {
                let index = 0;
                do {
                  while (index < this.sectionTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnSectionTemplate(this.sectionTemplates[index++], params, callback);
                  }
                  if (index >= this.sectionTemplates.length && this.isBulkProcessing) {
                    // eslint-disable-next-line no-await-in-loop
                    this.selectedSectionTemplates = this.sectionTemplates;
                  }
                } while (index < this.sectionTemplates.length && this.isBulkProcessing);
              } else {
                for (const element of this.sectionTemplates) {
                  if (!this.isBulkProcessing) {
                    break;
                  }
                  const sectionTemplate = element;
                  if (this.selectedSectionTemplates.find(s => s.id === sectionTemplate.id)) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.applyOperationOnSectionTemplate(sectionTemplate, params, callback);
                  }
                }
              }
            } finally {
              this.allSectionTemplatesSelected = false;
              this.selectedSectionTemplates = [];
              this.$emit('section-templates-bulk-operation-status', null, null);
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
          async applyOperationOnSectionTemplate(sectionTemplate, params, callback) {
            this.$emit('section-templates-bulk-operation-status', sectionTemplate.id, 'processing');
            try {
              await callback(sectionTemplate, params);
              this.$emit('section-templates-bulk-operation-status', sectionTemplate.id, 'done');
            } catch (e) {
              // eslint-disable-next-line no-console
              console.error('Error processing section template ', sectionTemplate.id, '. Error: ', e);
              this.$emit('section-templates-bulk-operation-status', sectionTemplate.id, 'error');
            } finally {
              this.processedSectionTemplates++;
            }
          },
        },
      }, `#${appId}`, 'Section Template Management')
    );
}
