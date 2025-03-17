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
    id="pageTemplatesApplication"
    :right-text-filter="{
      minCharacters: 1,
      placeholder: $t('pageTemplates.filter.placeholder'),
      tooltip: $t('pageTemplates.filter.placeholder'),
    }"
    class="px-1"
    @filter-text-input="$emit('page-templates-filter', $event)">
    <template v-if="!$root.isMobile" #left>
      <div v-if="$root.selectedPageTemplates?.length && !$root.isMobile" class="d-flex">
        <v-btn
          color="primary"
          elevation="0"
          class="me-2"
          outlined
          @click="$root.$emit('serialize-drawer-open', 'PageTemplate', selectedPageTemplatesIds)">
          <v-icon size="16" class="me-2">fa-download</v-icon>
          {{ $t('pageTemplate.label.export') }}
        </v-btn>
        <page-templates-bulk-delete />
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
            :aria-label="$t('pageTemplates.add')"
            :class="$root.isMobile && 'px-0'"
            class="btn btn-primary text-truncate"
            dense>
            <span
              v-if="!$root.isMobile"
              class="text-truncate text-none">
              {{ $t('pageTemplates.add') }}
            </span>
          </v-btn>
        </template>
        <v-list dense>
          <v-list-item
            link
            dense
            @click="$root.$emit('page-templates-create')">
            <v-list-item-icon class="me-3">
              <v-icon size="18">fa-plus</v-icon>
            </v-list-item-icon>
            <v-list-item-content class="d-inline">
              <v-list-item-title>{{ $t('pageTemplates.create') }}</v-list-item-title>
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
              <v-list-item-title>{{ $t('pageTemplates.import') }}</v-list-item-title>
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
  props: {
    creating: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    pageTemplates: null,
  }),
  computed: {
    selectedPageTemplatesIds() {
      return this.$root.selectedPageTemplates.map(item => item.id);
    },
  },
  created() {
    this.$root.$on('page-template-file-explorer', this.openFileExplorer);
  },
  beforeDestroy() {
    this.$root.$off('page-template-file-explorer', this.openFileExplorer);
  },
  methods: {
    openFileExplorer() {
      this.$refs.inputFile.openFileExplorer();
    },
    handelUpload(uploadId, fileName) {
      this.$root.$emit('deserialize-page-template-drawer-open', uploadId, fileName);
    }
  }
};
</script>
