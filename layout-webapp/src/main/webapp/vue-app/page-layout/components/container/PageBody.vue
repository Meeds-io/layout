<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <div class="d-flex flex-row full-width layout-page-body-row">
    <div
      id="pageBodyLeftPanel"
      :style="panelStyle(placements && placements.left)"
      class="flex-shrink-0 flex-grow-0"></div>
    <page-layout-container-base
      :container="container"
      :parent-id="parentId"
      class="layout-page-parent flex-grow-1 flex-shrink-1"
      page-style />
    <div
      id="pageBodyRightPanel"
      :style="panelStyle(placements && placements.right)"
      class="flex-shrink-0 flex-grow-0"></div>
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    parentId: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    // re-synced from the non-reactive global on each placement change, so the
    // anchors release or reserve their width without a page reload
    placements: window.eXo?.env?.portal?.appPlacements || null,
  }),
  computed: {
    stuckAllowed() {
      return (this.$vuetify?.breakpoint?.width || 0) >= (this.$vuetify?.breakpoint?.thresholds?.lg || 1264);
    },
  },
  created() {
    document.addEventListener('app-placement-changed', this.refreshPlacements);
  },
  mounted() {
    document.dispatchEvent(new CustomEvent('page-layout-rendered'));
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refreshPlacements);
  },
  methods: {
    refreshPlacements() {
      this.placements = window.eXo?.env?.portal?.appPlacements || null;
    },
    panelStyle(placedApplication) {
      // the anchor reserves the panel width from the server-injected payload
      // at its own first render, so the page area narrows in the layout
      // app's first paint instead of when the panel content docks
      if (placedApplication && this.placements?.siteEligible && this.stuckAllowed) {
        return {
          position: 'relative',
          flex: '0 0 420px',
          zIndex: '0',
        };
      }
      return null;
    },
  },
};
</script>
