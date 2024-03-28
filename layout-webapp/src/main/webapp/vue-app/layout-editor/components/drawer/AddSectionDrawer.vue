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
      {{ $t('layout.addSectionTitle') }}
    </template>
    <template v-if="drawer && cols" #content>
      <v-card class="pa-4" flat>
        <div class="subtitle-1 font-weight-bold">
          {{ $t('layout.selectSectionType') }}
        </div>
        <v-radio-group v-model="sectionType" class="my-auto text-no-wrap ms-n1">
          <v-radio
            :label="$t('layout.fixedSectionTypeChoice')"
            value="GridContainer"
            class="mx-0" />
          <v-radio
            :label="$t('layout.dynamicSectionTypeChoice')"
            value="FlexContainer"
            class="mx-0" />
        </v-radio-group>

        <layout-editor-section-grid-editor
          v-if="sectionType === $layoutUtils.gridTemplate"
          :rows-count="rows"
          :cols-count="cols"
          class="mt-4"
          @rows-updated="rows = $event"
          @cols-updated="cols = $event" />
        <layout-editor-section-flex-editor
          v-else-if="sectionType === $layoutUtils.flexTemplate"
          :cols-count="cols"
          class="mt-4"
          @cols-updated="cols = $event" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
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
    parentContainer: null,
    section: null,
    sectionType: null,
    drawer: false,
    index: null,
    rows: 0,
    cols: 0,
  }),
  watch: {
    sectionType() {
      if (this.sectionType === this.$layoutUtils.gridTemplate) {
        this.rows = 3;
        this.cols = 12;
      } else if (this.sectionType === this.$layoutUtils.flexTemplate) {
        this.rows = 1;
        this.cols = 3;
      }
    },
  },
  methods: {
    open(parentContainer, index) {
      this.sectionType = this.$layoutUtils.gridTemplate;
      this.parentContainer = parentContainer;
      this.index = index;
      this.$nextTick().then(() => this.$refs.drawer.open());
    },
    apply() {
      this.section = this.$layoutUtils.newSection(null, null, this.rows, this.cols, this.sectionType);
      this.$root.$emit('layout-add-section', this.section, this.index);
      this.close();
    },
    close() {
      this.$refs.drawer.close();
      this.section = null;
    },
  },
};
</script>