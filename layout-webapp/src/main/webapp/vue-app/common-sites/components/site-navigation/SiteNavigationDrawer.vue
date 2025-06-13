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
  <exo-drawer
    id="siteNavigationDrawer"
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    eager
    allow-expand
    @closed="close">
    <template #title>
      <span>{{ $t('siteNavigation.drawer.title') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div :class="$refs.drawer?.expand ? 'singlePageApplication' : ' ' ">
        <v-alert
          v-if="information"
          class="mx-4 mt-4 mb-0"
          type="info"
          outlined>
          <div class="text-color">
            <span v-sanitized-html="$t(information)"></span>
          </div>
        </v-alert>
        <div v-if="displayEditLayout" class="mt-4 mx-4">
          <div class="d-flex align-center mb-4">
            <div class="text-header me-auto">
              {{ $t('layout.editSiteLayout') }}
            </div>
            <v-btn
              :aria-label="$t('siteManagement.label.editLayout')"
              :href="editSiteLayoutLink"
              target="_blank"
              icon>
              <v-icon size="20">fa-edit</v-icon>
            </v-btn>
          </div>
          <div class="text-header">
            {{ $t('layout.editSiteNavigation') }}
          </div>
        </div>
        <v-toolbar
          color="white"
          flat
          dense>
          <span
            :title="siteName"
            class="font-weight-bold text-truncate">
            {{ label }}
          </span>
          <v-spacer v-if="!$refs.drawer?.expand" />
          <v-btn
            v-if="isMetaSite"
            @click="createNode"
            class="btn btn-primary ms-2">
            {{ $t('siteNavigation.label.btn.createNode') }}
          </v-btn>
          <template v-if="$refs.drawer?.expand">
            <v-spacer />
            <select
              id="siteNavigationDrawerFilterSelect"
              v-model="filter"
              aria-label="hidden"
              class="ignore-vuetify-classes width-auto pa-0 me-5 mb-0">
              <option
                v-for="item in navigationsFilter"
                :key="item.value"
                :value="item.value">
                {{ item.text }}
              </option>
            </select>
          </template>
        </v-toolbar>
        <site-navigation-nodes-list
          :navigation-nodes="navigationNodesToDisplay"
          :expanded="$refs.drawer?.expand"
          :loading="loading"
          :hide-children="hideChildren" />
      </div>
    </template>
    <template v-if="displayCloseFooter" #footer>
      <div class="d-flex justify-end">
        <v-btn
          class="btn"
          @click="drawer = false">
          {{ $t('siteNavigation.label.btn.close') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    navigationNodes: [],
    navigationNodesToDisplay: [],
    displayCloseFooter: false,
    displayEditLayout: false,
    drawer: false,
    siteType: null,
    siteId: null,
    siteName: null,
    siteLabel: null,
    information: null,
    includeGlobal: false,
    loading: false,
    filter: 'ALL',
    site: null,
  }),
  computed: {
    navigationsFilter() {
      return [{
        text: this.$t('siteNavigation.label.all'),
        value: 'ALL',
      },{
        text: this.$t('siteNavigation.label.group'),
        value: 'GROUP',
      },{
        text: this.$t('siteNavigation.label.page'),
        value: 'PAGE',
      },{
        text: this.$t('siteNavigation.label.link'),
        value: 'LINK',
      }];
    },
    hideChildren(){
      return this.filter !== 'ALL';
    },
    isMetaSite() {
      return this.site?.metaSite;
    },
    label() {
      return this.siteLabel || this.siteName;
    },
    editSiteLayoutLink() {
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/site-layout-editor?siteId=${this.siteId}`;
    },
  },
  watch: {
    filter() {
      this.$root.$emit('site-navigation-hide-nodes-tree');
      this.$nextTick().then (() => this.getNavigationNodes());
    },
  },
  created() {
    this.$root.$on('navigation-node-deleted', this.getNavigationNodes);
    this.$root.$on('refresh-navigation-nodes', this.getNavigationNodes);
    this.$root.$on('open-site-navigation-drawer', this.open);
  },
  methods: {
    open(event) {
      this.siteName = event?.siteName || event?.name || eXo.env.portal.spaceDisplayName || eXo.env.portal.siteKeyName;
      this.siteType = event?.siteType || event?.type || eXo.env.portal.siteKeyType;
      this.siteLabel = event?.siteLabel || event?.displayName || eXo.env.portal.siteLabel;
      this.siteId = event?.siteId || event?.id || eXo.env.portal.siteId;
      this.information = event?.information;
      this.includeGlobal = event?.includeGlobal || false;
      this.displayCloseFooter = event?.displayCloseFooter || false;
      this.displayEditLayout = event?.displayEditLayout || false;
      this.getNavigationNodes();
      this.$refs.drawer.open();
      this.$nextTick().then(() =>  this.$root.$emit('site-navigation-drawer-opened'));
    },
    close() {
      this.siteName = null;
      this.siteId = null;
      this.site = null;
      this.includeGlobal = false;
      this.navigationNodes = [];
      this.navigationNodesToDisplay = [];
      this.$root.$emit('site-navigation-hide-nodes-tree');
      this.$refs.drawer.close();
    },
    getNavigationNodes() {
      this.loading = true;
      return this.$siteLayoutService.getSiteById(this.siteId)
        .then(site => {
          this.site = site;
          this.navigationNodes = site.siteNavigations || [];
          this.filterNavigationNodes();
        })
        .finally(() => this.loading = false);
    },
    createNode() {
      this.$root.$emit('open-site-navigation-add-node-drawer', this.site.rootNode);
    },
    filterNavigationNodes(){
      this.navigationNodesToDisplay = [];
      if (this.filter === 'PAGE') {
        this.filterPageNavigationNodes(this.navigationNodes);
      } else if ( this.filter === 'LINK') {
        this.filterLinkNavigationNodes(this.navigationNodes);
      } else if ( this.filter === 'GROUP') {
        this.filterGroupNavigationNodes(this.navigationNodes);
      } else {
        this.navigationNodesToDisplay = this.navigationNodes;
      }
    },
    filterPageNavigationNodes(navigationNodes){
      navigationNodes.forEach(child => {
        if (child.children.length > 0) {
          this.filterPageNavigationNodes(child.children);
        }
        if (child.pageKey && !child.pageLink) {
          this.navigationNodesToDisplay.push(child);
        }
      });
    },
    filterLinkNavigationNodes(navigationNodes){
      navigationNodes.forEach(child => {
        if (child.children.length > 0) {
          this.filterLinkNavigationNodes(child.children);
        }
        if (child.pageKey && child.pageLink) {
          this.navigationNodesToDisplay.push(child);
        }
      });
    },
    filterGroupNavigationNodes(navigationNodes){
      navigationNodes.forEach(child => {
        if (child.children.length > 0) {
          this.filterGroupNavigationNodes(child.children);
        }
        if (!child.pageKey && !child.pageLink) {
          this.navigationNodesToDisplay.push(child);
        }
      });
    },
  }
};
</script>
