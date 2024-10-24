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
    ref="siteNavigationDrawer"
    :right="!$vuetify.rtl"
    eager
    allow-expand
    @closed="close">
    <template slot="title">
      <span>{{ $t('siteNavigation.drawer.title') }}</span>
    </template>
    <template slot="content">
      <div :class="$refs.siteNavigationDrawer?.expand ? 'singlePageApplication' : ' ' ">
        <v-toolbar
          color="white"
          flat
          dense>
          <span
            :title="siteName"
            class="font-weight-bold text-truncate">
            {{ siteName }}
          </span>
          <v-spacer v-if="!$refs.siteNavigationDrawer?.expand" />
          <v-btn
            v-if="isMetaSite"
            @click="createNode"
            class="btn btn-primary ms-2">
            {{ $t('siteNavigation.label.btn.createNode') }}
          </v-btn>
          <v-spacer v-if="$refs.siteNavigationDrawer?.expand" />
          <select
            id="siteNavigationDrawerFilterSelect"
            v-model="filter"
            v-if="$refs.siteNavigationDrawer?.expand"
            class="ignore-vuetify-classes width-auto pa-0 me-5 mb-0">
            <option
              v-for="item in navigationsFilter"
              :key="item.value"
              :value="item.value">
              {{ item.text }}
            </option>
          </select>
        </v-toolbar>
        <site-navigation-nodes-list
          :navigation-nodes="navigationNodesToDisplay"
          :expanded="$refs.siteNavigationDrawer && $refs.siteNavigationDrawer?.expand"
          :loading="loading"
          :hide-children="hideChildren" />
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data() {
    return {
      navigationNodes: [],
      navigationNodesToDisplay: [],
      siteName: null,
      siteType: null,
      siteId: null,
      includeGlobal: false,
      loading: false,
      filter: 'ALL',
      site: null,
    };
  },
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
    }
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
      this.siteName = event?.siteName || eXo.env.portal.spaceDisplayName || eXo.env.portal.siteKeyName;
      this.siteType = event?.siteType || eXo.env.portal.siteKeyType;
      this.siteId = event?.siteId || eXo.env.portal.siteId;
      this.includeGlobal = event?.includeGlobal || false;
      this.getNavigationNodes();
      this.$refs.siteNavigationDrawer.open();
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
      this.$refs.siteNavigationDrawer.close();
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
