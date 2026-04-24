<!--
 This file is part of the Meeds project (https://meeds.io/).
 
 Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-app class="siteManagementApplication">
    <v-main class="application-body pb-5">
      <h4 class="text-title px-5 pt-5 ma-0">
        {{ $t('siteManagement.title') }}
      </h4>
      <site-management-toolbar
        ref="toolbar"
        @site-filter="keyword = $event" />
      <site-management-list
        :sites="sites"
        :loading="loading > 0"
        :keyword="keyword" />
    </v-main>
    <exo-confirm-dialog
      ref="deleteSiteConfirmDialog"
      :message="deleteConfirmMessage"
      :title="$t('siteManagement.label.confirmDelete')"
      :ok-label="$t('siteManagement.label.confirm')"
      :cancel-label="$t('siteManagement.label.cancel')"
      @ok="deleteSite" />
    <site-form-drawer
      :sites="sites" />
    <site-navigation-drawer />
    <site-navigation-node-drawer />
    <site-navigation-element-drawer />
    <manage-permissions-drawer />
    <site-template-drawer />
    <layout-analytics application-name="siteManagement" />
    <extension-registry-components
      :name="extensionApp"
      :type="extensionDrawerType" />
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      sites: [],
      loading: 0,
      siteToDelete: null,
      keyword: null,
      deleteConfirmMessage: '',
      extensionApp: 'site-management',
      extensionActionType: 'action',
      extensionDrawerType: 'site-management-drawers'
    };
  },
  created() {
    this.$root.$on('site-created', this.handleSiteCreation);
    this.$root.$on('site-updated', this.refreshSites);
    this.$root.$on('delete-site', this.confirmDelete);
    document.addEventListener(`component-${this.extensionApp}-${this.extensionDrawerType}-updated`, this.refreshActionExtensions);
    document.addEventListener(`extension-${this.extensionApp}-${this.extensionActionType}-updated`, this.refreshActionExtensions);
    this.refreshSites();
    this.refreshActionExtensions();
  },
  beforeDestroy() {
    this.$root.$off('site-created', this.handleSiteCreation);
    this.$root.$off('site-updated', this.refreshSites);
    this.$root.$on('delete-site', this.confirmDelete);
    document.removeEventListener(`component-${this.extensionApp}-${this.extensionDrawerType}-updated`, this.refreshActionExtensions);
    document.removeEventListener(`component-${this.extensionApp}-${this.extensionActionType}-updated`, this.refreshActionExtensions);
  },
  methods: {
    handleSiteCreation(site) {
      this.refreshSites();
      this.$root.$emit('open-site-navigation-drawer', {
        siteName: site.name,
        siteType: site.siteType,
        siteId: site.siteId,
        siteLabel: site.displayName,
        information: 'sites.created.information',
      });
    },
    refreshSites() {
      this.loading++;
      return this.$siteService.getSitesByFilter({
        siteType: 'PORTAL',
        excludeSpaceSites: true,
        expand: 'canRestore',
      })
        .then(sites => this.sites = sites?.filter(s => !s?.properties?.IS_SPACE_PUBLIC_SITE) || [])
        .finally(() => this.loading--);
    },
    refreshActionExtensions() {
      this.$root.siteActionExtensions = extensionRegistry.loadExtensions(this.extensionApp, this.extensionActionType) || [];
    },
    confirmDelete(siteToDelete) {
      this.siteToDelete = siteToDelete;
      this.deleteConfirmMessage = this.$t('siteManagement.label.confirmDelete.message', {0: this.siteToDelete.name});
      this.$refs.deleteSiteConfirmDialog.open();
    },
    deleteSite() {
      this.loading++;
      return this.$siteLayoutService.deleteSite(this.siteToDelete.siteType, this.siteToDelete.name)
        .then(() => this.refreshSites())
        .finally(() => this.loading--);
    },
  }
};
</script>
