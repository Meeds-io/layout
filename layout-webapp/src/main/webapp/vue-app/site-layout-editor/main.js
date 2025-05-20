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
import './extensions.js';

import '../common-layout/main.js';
import '../common-page-layout/main.js';
import '../common-page-template/main.js';
import '../common-portlets/main.js';
import '../common-illustration/main.js';
import '../common-site-template/main.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('SiteLayoutEditor');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'siteLayoutEditor';

//getting language of the PLF
const lang = eXo?.env.portal.language || 'en';

//should expose the locale ressources as REST API
const url = `/layout/i18n/locale.portlet.LayoutEditor?lang=${lang}`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<site-layout-editor id="${appId}"/>`,
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
          isSiteLayout: true,
          loadingPortletInstances: false,
          branding: null,
          displayMode: 'desktop',
          siteTemplate: null,
          layout: null,
          site: null,
          draftSite: null,
          draftSiteId: null,
          movingParentId: null,
          movingParentDynamic: false,
          drawerOpened: 0,
          selectedSectionId: null,
          nextCellStorageId: null,
          nextCellDiffWidth: null,
          parentAppDimensions: false,
          sectionHistory: null,
          sectionRedo: null,
          movingCell: null,
          moveType: null,
          startScrollX: 0,
          startScrollY: 0,
          diffScrollX: 0,
          diffScrollY: 0,
          gap: 20,
          isAdministrator: eXo.env.portal.isAdministrator,
          middleCenterContainersMinHeight: 0,
        }),
        computed: {
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
          mobileDisplayMode() {
            return this.$root.displayMode === 'mobile';
          },
          desktopDisplayMode() {
            return this.$root.displayMode === 'desktop';
          },
          siteId() {
            return this.$layoutUtils.getQueryParam('siteId');
          },
          siteType() {
            return this.site?.siteType?.toUpperCase?.();
          },
          siteName() {
            return this.site?.name;
          },
          isSiteTemplate() {
            return this.siteType === 'PORTAL_TEMPLATE' || this.siteType === 'GROUP_TEMPLATE';
          },
          siteUri() {
            return this.isSiteTemplate ? `${eXo.env.portal.context}/t/${this.siteId}` : `${eXo.env.portal.context}/${this.siteName}`;
          },
          draftSiteType() {
            return this.draftSite?.siteType;
          },
          draftSiteName() {
            return this.draftSite?.name;
          },
          draftNodeUri() {
            return `/d/${this.draftSiteId}/`;
          },
          leftContainer() {
            return this.layout?.children?.[0];
          },
          middleContainer() {
            return this.layout?.children?.find(c => c.template === this.$layoutUtils.siteBodyMiddleTemplate);
          },
          middleContainerBannersHeight() {
            return this.$root.middleContainer?.children?.map?.(c => (c.height && Number(c.height) || 57) + (c.marginTop || 0) + (c.marginBottom || 0))?.reduce?.((acc, v) => acc + v, 0) || 0;
          },
          rightContainer() {
            return this.layout?.children?.[2];
          },
          middleCenterContainer() {
            return this.middleContainer?.children?.find(c => c.template === this.$layoutUtils.siteBodyMiddleCenterTemplate);
          },
          middleCenterContainerIndex() {
            return this.middleContainer?.children?.findIndex(c => c.template === this.$layoutUtils.siteBodyMiddleCenterTemplate);
          },
          internalLeftContainer() {
            return this.middleCenterContainer?.children?.[0];
          },
          pageBodyContainer() {
            return this.middleCenterContainer?.children?.[1];
          },
          internalRightContainer() {
            return this.middleCenterContainer?.children?.[2];
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
          async siteType() {
            if (this.isSiteTemplate) {
              this.siteTemplate = await this.$siteTemplateService.getSiteTemplate(this.siteId);
            }
          },
        },
        created() {
          document.addEventListener('extension-layout-editor-container-updated', this.refreshContainerTypes);
          this.$on('layout-editor-portlet-instances-refresh', this.refreshPortletInstances);
          document.addEventListener('drawerOpened', this.setDrawerOpened);
          document.addEventListener('drawerClosed', this.setDrawerClosed);
          this.refreshPortletInstances();
          this.$siteLayoutService.getSiteById(this.siteId)
            .then(site => this.site = site);
          this.$brandingService.getBrandingInformation()
            .then(data => this.branding = data);
        },
        mounted() {
          this.$el?.closest?.('.PORTLET-FRAGMENT')?.classList?.remove?.('PORTLET-FRAGMENT');
        },
        methods: {
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
            this.parentAppDimensions = document.querySelector('#siteLayoutEditor').getBoundingClientRect();
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
          },
          resetMoving() {
            this.parentAppDimensions = null;
            this.moveType = null;
          },
        },
      }, `#${appId}`, 'Site Layout Editor');
    })
    .finally(() => {
      Vue.prototype.$utils.includeExtensions('LayoutEditorExtension');
      document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
    });
}
