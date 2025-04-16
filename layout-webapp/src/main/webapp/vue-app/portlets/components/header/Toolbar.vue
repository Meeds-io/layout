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
  <application-toolbar
    id="portletsApplication"
    :center-button-toggle="{
      selected: tabName,
      buttons: [{
        value: 'instances',
        text: $t('portlets.instances'),
        icon: 'fa-braille',
      }, {
        value: 'portlets',
        text: $t('portlets'),
        icon: 'fa-grip-horizontal',
      }]
    }"
    :right-text-filter="{
      minCharacters: 1,
      placeholder: $t('portlets.filter.placeholder'),
      tooltip: $t('portlets.filter.placeholder'),
    }"
    class="border-box-sizing px-1"
    @toggle-select="$emit('select-tab', $event)"
    @filter-text-input="$emit('portlet-instance-filter', $event)">
    <template v-if="!$root.isMobile && tabName === 'instances'" #left>
      <div v-if="$root.selectedPortletInstances?.length && !$root.isMobile" class="d-flex">
        <v-btn
          color="primary"
          elevation="0"
          class="me-2"
          outlined
          @click="$root.$emit('serialize-drawer-open', 'PortletInstance', selectedPortletInstancesIds)">
          <v-icon size="16" class="me-2">fa-download</v-icon>
          {{ $t('portletInstance.label.export') }}
        </v-btn>
        <portlets-instances-bulk-delete />
      </div>
      <v-menu
        v-else
        :right="!$vuetify.rtl"
        :left="$vuetify.rtl"
        content-class="application-menu z-index-modal"
        offset-y>
        <template #activator="{attrs, on}">
          <v-btn
            id="applicationToolbarLeftButton"
            v-bind="attrs"
            v-on="on"
            :disabled="loading"
            :aria-label="$t('layout.portletInstance.add')"
            :class="$root.isMobile && 'px-0'"
            class="btn btn-primary text-truncate"
            dense>
            <v-progress-circular
              v-if="loading"
              indeterminate
              size="20"
              class="me-2" />
            <span
              v-if="!$root.isMobile"
              class="text-truncate text-none">
              {{ $t('layout.portletInstance.add') }}
            </span>
          </v-btn>
        </template>
        <v-list dense>
          <v-list-item
            link
            dense
            @click="$root.$emit('portlet-instance-add')">
            <v-list-item-icon class="me-3">
              <v-icon size="18">fa-plus</v-icon>
            </v-list-item-icon>
            <v-list-item-content class="d-inline">
              <v-list-item-title>{{ $t('layout.portletInstance.create') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            link
            dense
            @click="openFileExplorer">
            <v-list-item-icon class="me-3">
              <v-icon size="18">fas fa-upload</v-icon>
            </v-list-item-icon>
            <v-list-item-content class="d-inline">
              <v-list-item-title>{{ $t('layout.portletInstance.import') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <layout-file-input
            ref="inputFile"
            @uploaded="handelUpload" />
        </v-list>
      </v-menu>
    </template>
  </application-toolbar>
</template>
<script>
export default {
  data: () => ({
    loading: false,
  }),
  props: {
    tabName: {
      type: String,
      default: null,
    }
  },
  computed: {
    selectedPortletInstancesIds() {
      return this.$root.selectedPortletInstances.map(item => item.id);
    },
  },
  created() {
    this.$root.$on('portlet-instance-file-explorer', this.openFileExplorer);
  },
  beforeDestroy() {
    this.$root.$off('portlet-instance-file-explorer', this.openFileExplorer);
  },
  methods: {
    openFileExplorer() {
      this.loading = true;
      this.$refs.inputFile.openFileExplorer();
    },
    handelUpload(uploadId, fileName) {
      this.$root.$emit('deserialize-instance-drawer-open', uploadId, fileName);
      this.loading = false;
    }
  }
};
</script>
