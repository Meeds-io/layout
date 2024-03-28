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
    ref="drawer"
    id="defaultSpacesRegistrationDrawer"
    v-model="drawer"
    right
    disable-pull-to-refresh>
    <template #title>
      {{ $t('layout.editSectionTitle', {0: index + 1}) }}
    </template>
    <template v-if="drawer && section" #content>
      <v-card class="pa-4" flat>
        <layout-editor-section-grid-editor
          v-if="sectionType === $layoutUtils.gridTemplate"
          :rows-count="section.rowsCount"
          :cols-count="section.colsCount"
          @rows-updated="rows = $event"
          @cols-updated="cols = $event" />
        <layout-editor-section-flex-editor
          v-else-if="sectionType === $layoutUtils.flexTemplate"
          :cols-count="cols"
          @cols-updated="cols = $event" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          v-if="canRemove"
          color="error"
          outlined
          elevation="0"
          class="ignore-vuetify-classes"
          @click="removeSection">
          <span class="text-none">{{ $t('layout.delete') }}</span>
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
          :disabled="!modified"
          class="btn btn-primary ms-4"
          @click="apply">
          <span class="text-none">{{ $t('layout.apply') }}</span>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    originalSection: null,
    section: null,
    drawer: false,
    index: null,
    rows: 0,
    cols: 0,
    canRemove: false,
  }),
  computed: {
    modified() {
      return this.section?.rowsCount !== this.rows || this.section?.colsCount !== this.cols;
    },
    sectionType() {
      return this.section?.template;
    },
  },
  methods: {
    open(section, index, length) {
      this.section = JSON.parse(JSON.stringify(section));
      this.originalSection = JSON.parse(JSON.stringify(section));
      this.index = index;
      this.rows = this.section?.rowsCount;
      this.cols = this.section?.colsCount;
      this.canRemove = length > 1;
      this.$nextTick().then(() => this.$refs.drawer.open());
    },
    removeSection() {
      this.close();
      this.$root.$emit('layout-remove-section', this.index);
    },
    apply() {
      const diffRows = this.rows - this.originalSection.rowsCount;
      const diffCols = this.cols - this.originalSection.colsCount;
      const section = JSON.parse(JSON.stringify(this.section));
      if (diffCols) {
        if (diffCols > 0) {
          this.$layoutUtils.addColumns(section, diffCols);
        } else {
          this.$layoutUtils.removeColumns(section, diffCols);
        }
      }
      if (diffRows) {
        if (diffRows > 0) {
          this.$layoutUtils.addRows(section, diffRows);
        } else {
          this.$layoutUtils.removeRows(section, diffRows);
        }
      }
      this.$root.$emit('layout-replace-section', this.index, section);
      this.close();
    },
    close() {
      this.$refs.drawer.close();
      this.section = null;
    },
  },
};
</script>