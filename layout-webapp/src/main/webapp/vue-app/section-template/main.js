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
export function init () {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n =>
      Vue.createApp({
        template: `<section-template-management id="${appId}"/>`,
        vuetify: eXo.vuetify,
        i18n,
        data: () => ({
          sectionTemplates: [],
          loading: 0,
          collator: new Intl.Collator(eXo.env.portal.language, { numeric: true, sensitivity: 'base' }),
        }),
        computed: {
          isMobile () {
            return eXo.vuetify.display.smAndDown.value;
          },
        },
        created () {
          this.$root.$on('section-template-enabled', this.refreshSectionTemplates);
          this.$root.$on('section-template-disabled', this.refreshSectionTemplates);
          this.$root.$on('section-template-saved', this.refreshSectionTemplates);
          this.$root.$on('section-template-deleted', this.refreshSectionTemplates);
          window.addEventListener('section-template-layout-updated', this.propagateEventListenerLocally);

          this.refreshSectionTemplates();
        },
        methods: {
          propagateEventListenerLocally (event) {
            this.$root.$emit(event.type, event.detail);
          },
          refreshSectionTemplates () {
            this.loading++;
            return eXo.$sectionTemplateService.getSectionTemplates()
              .then(data => this.sectionTemplates = data || [])
              .finally(() => this.loading--);
          },
        },
      }, `#${appId}`, 'Section Template Management')
    );
}
