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
  <v-app>
    <main
      class="application-body position-static">
      <div :class="tabName !== 'instances' && 'application-layout-style'">
        <portlets-toolbar
          ref="toolbar"
          :tab-name="tabName"
          :class="tabName === 'instances' && 'application-layout-style'"
          @portlet-instance-filter="keyword = $event"
          @select-tab="selectTab" />
        <portlets-instance-main
          v-if="tabName === 'instances'"
          :keyword="keyword" />
        <portlets-list
          v-else-if="tabName === 'portlets'"
          :keyword="keyword" />
      </div>
    </main>
    <portlets-item-instances-drawer />
    <portlets-instance-category-drawer />
    <portlets-instance-drawer />
    <layout-image-illustration-preview />
    <layout-analytics application-name="portletsManagement" />
    <serialize-drawer>
      <template #title>{{ $t('portletInstance.label.exportInstance') }}</template>
      <template #content>
        <v-card-text class="pb-0">{{ $t('portletInstance.label.exportInstance.part1') }}</v-card-text>
        <v-card-text class="pb-0">{{ $t('portletInstance.label.exportInstance.part2') }}</v-card-text>
        <v-card-text class="pb-0">{{ $t('portletInstance.label.exportInstance.part3') }}</v-card-text>
        <v-card-text class="pb-0">{{ $t('portletInstance.label.exportInstance.part4') }}</v-card-text>
      </template>
    </serialize-drawer>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    keyword: null,
    tabName: null,
    switching: false,
  }),
  watch: {
    tabName() {
      if (this.tabName && this.switching) {
        window.location.hash = `#${this.tabName}`;
      } else if (!this.tabName) {
        this.switching = true;
      }
    },
  },
  created() {
    this.$root.$on('portlet-instance-created', this.handleInstanceCreated);
    this.$root.$on('portlet-instance-layout-updated', this.handleLayoutUpdated);
    this.tabName = window.location.hash === '#portlets' && 'portlets' || 'instances';
  },
  beforeDestroy() {
    this.$root.$off('portlet-instance-created', this.handleInstanceCreated);
    this.$root.$off('portlet-instance-layout-updated', this.handleLayoutUpdated);
  },
  methods: {
    selectTab(tabName) {
      this.tabName = null;
      this.$nextTick(() => window.setTimeout(() => this.tabName = tabName, 10));
    },
    handleInstanceCreated(instance) {
      const hasEditMode = instance?.supportedModes?.find?.(mode => mode === 'edit');
      const instanceEditorLink = `/portal/${eXo.env.portal.portalName}/portlet-editor?id=${instance.id}&portletMode=${hasEditMode && 'edit' || 'view'}`;
      window.open(instanceEditorLink, '_blank');
    },
    handleLayoutUpdated(instance) {
      this.$root.$emit('portlet-instance-saved', instance);
      this.$root.$emit('alert-message', this.$t('layout.portletInstanceLayoutUpdatedSuccessfully'), 'success');
    },
  },
};
</script>
