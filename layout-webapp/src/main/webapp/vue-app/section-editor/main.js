/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
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

import '../common-page-layout/main.js';
import '../common-page-template/main.js';
import '../common-portlets/main.js';
import '../common-section-template/main.js';
import '../common-illustration/main.js';
import '../common-layout/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('SectionEditor');
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
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<section-editor v-if="nodeId" id="${appId}"/>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
        data: () => ({
          containerTypes: extensionRegistry.loadExtensions('layout-editor', 'container'),
          collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
          hoveredParentId: null,
          hoveredSectionId: null,
          hoveredSection: null,
          hoveredApplication: null,
          portletInstanceCategories: null,
          portletInstances: null,
          portlets: null,
          loadingPortletInstances: false,
          noSectionAdd: true,
          branding: null,
          displayMode: 'desktop',
          layout: null,
          page: null,
          pageRef: null,
          pageTemplate: null,
          pageTemplateId: null,
          nodeId: null,
          draftPageRef: null,
          draftNode: null,
          draftNodeId: null,
          draftNodeUri: null,
          movingParentId: null,
          movingParentDynamic: false,
          drawerOpened: 0,
          selectedSectionId: null,
          selectedCellCoordinates: [],
          selectedCells: [],
          nextCellStorageId: null,
          nextCellDiffWidth: null,
          parentAppDimensions: false,
          multiCellsSelect: false,
          isSpaceSiteTemplate: false,
          sectionHistory: null,
          sectionRedo: null,
          movingCell: null,
          moveType: null,
          startScrollX: 0,
          startScrollY: 0,
          diffScrollX: 0,
          diffScrollY: 0,
          gap: 20,
          nodeUri: null,
          originalUri: `/portal/${window.location.href.split('/portal/')[1]}`,
          originalHref: window.location.href,
        }),
        computed: {
          sectionTemplateId() {
            return this.$layoutUtils.getQueryParam('id');
          },
          sectionTemplateContainer() {
            return this.sectionTemplate?.content && JSON.parse(this.sectionTemplate.content);
          },
          parentAppX() {
            return this.parentAppDimensions?.x || 0;
          },
          parentAppY() {
            return this.parentAppDimensions?.y || 0;
          },
          defaultContainer() {
            return this.containerTypes.find(extension => extension.type === 'default');
          },
          isResize() {
            return this.moveType === 'resize';
          },
          isMove() {
            return this.moveType === 'drag';
          },
          isMultiSelect() {
            return this.moveType === 'multiSelect';
          },
          selectedFirstRowIndex() {
            return Math.min(...this.selectedCellCoordinates.map(c => c.rowIndex));
          },
          selectedFirstColIndex() {
            return Math.min(...this.selectedCellCoordinates.map(c => c.colIndex));
          },
          mobileDisplayMode() {
            return this.$root.displayMode === 'mobile';
          },
          desktopDisplayMode() {
            return this.$root.displayMode === 'desktop';
          },
          pageId() {
            return this.$root.page?.state?.storageId?.replace?.('page_', '');
          },
          isSpaceSiteTemplate() {
            return this.pageRef?.toLowerCase?.()?.indexOf('group_template::') === 0;
          },
        },
        watch: {
          movingParentId() {
            if (this.movingParentId) {
              this.$root.$emit('layout-editor-moving-start', this.movingParentId);
            } else {
              this.$root.$emit('layout-editor-moving-end', this.movingParentId);
            }
          },
          nodeUri() {
            if (window.opener) {
              eXo.env.portal.webPageUrl = window.opener.location.pathname;
            } else {
              eXo.env.portal.webPageUrl = `/portal${this.nodeUri}`;
            }
          },
          layout(newVal, oldVal) {
            if (!oldVal) {
              window.setTimeout(() => document.dispatchEvent(new CustomEvent('hideTopBarLoading')), 200);
            }
          },
        },
        async created() {
          this.sectionTemplate = await this.$sectionTemplateService.getSectionTemplate(this.sectionTemplateId);
          this.nodeId = await this.$sectionTemplateService.generateSectionTemplateNodeId(this.sectionTemplateId);
          // Some applications will change the window location state when displayed
          // This will ensure to preserve the original Page location URI
          new MutationObserver(this.setOriginalUri).observe(document, { subtree: true, childList: true });
          document.addEventListener('extension-layout-editor-container-updated', this.refreshContainerTypes);
          this.$on('layout-editor-portlet-instances-refresh', this.refreshPortletInstances);
          document.addEventListener('drawerOpened', this.setDrawerOpened);
          document.addEventListener('drawerClosed', this.setDrawerClosed);
          this.refreshPortletInstances();
          this.branding = await this.$brandingService.getBrandingInformation();
        },
        mounted() {
          this.$el?.closest?.('.PORTLET-FRAGMENT')?.classList?.remove?.('PORTLET-FRAGMENT');
          this.$root.$applicationLoaded();
        },
        methods: {
          setOriginalUri() {
            if (window.location.href !== this.originalHref) {
              window.history.replaceState('', window.document.title, this.originalUri);
            }
          },
          setDrawerOpened() {
            this.drawerOpened++;
          },
          setDrawerClosed() {
            this.drawerOpened--;
          },
          async refreshPortletInstances() {
            this.loadingPortletInstances = true;
            try {
              this.portletInstanceCategories =  await this.$portletInstanceCategoryService.getPortletInstanceCategories();

              const applications = await this.$portletInstanceService.getPortletInstances();
              this.portletInstances = applications.filter(a => !a.disabled);

              const portlets = await this.$portletService.getPortlets();
              this.portlets = portlets?.map?.(p => ({
                ...p,
                name: this.$te(`layout.portletInstance.${p?.portletName}.name`) ? this.$t(`layout.portletInstance.${p?.portletName}.name`) : p?.name,
                description: this.$te(`layout.portletInstance.${p?.portletName}.description`) ? this.$t(`layout.portletInstance.${p?.portletName}.description`) : p?.description,
              })) || [];
            } finally {
              this.loadingPortletInstances = false;
            }
          },
          refreshContainerTypes() {
            this.containerTypes = extensionRegistry.loadExtensions('layout-editor', 'container');
          },
          updateParentAppDimensions() {
            this.parentAppDimensions = document.querySelector(`#${appId}`).getBoundingClientRect();
          },
          initScrollPosition() {
            this.updateParentAppDimensions();
            this.startScrollX = this.parentAppDimensions.x;
            this.startScrollY = this.parentAppDimensions.y;
            this.diffScrollX = 0;
            this.diffScrollY = 0;
          },
          updateScrollPosition() {
            this.updateParentAppDimensions();
            this.diffScrollX = this.parentAppDimensions.x - this.startScrollX;
            this.diffScrollY = this.parentAppDimensions.y - this.startScrollY;
          },
          initCellsSelection() {
            this.selectedSectionId = null;
            this.moveType = null;
            this.selectedCells = [];
            this.selectedCellCoordinates = [];
          },
          resetMoving() {
            this.parentAppDimensions = null;
            this.moveType = null;
            this.multiCellsSelect = false;
          },
        },
      }, `#${appId}`, 'Section Editor');
    })
    .finally(() => {
      Vue.prototype.$utils.includeExtensions('LayoutEditorExtension');
      document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
    });
}
