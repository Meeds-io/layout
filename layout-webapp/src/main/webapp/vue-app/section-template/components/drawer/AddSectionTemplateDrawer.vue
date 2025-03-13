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
  <exo-drawer
    id="addSectionDrawer"
    ref="drawer"
    v-model="drawer"
    disable-pull-to-refresh
    right>
    <template #title>
      {{ $t('layout.addSectionTitle') }}
    </template>
    <template v-if="drawer && cols" #content>
      <v-card class="pa-4" flat>
        <div class="text-header mb-2">
          {{ $t('layout.selectSectionType') }}
        </div>
        <v-radio-group v-model="sectionType" class="my-auto text-no-wrap ms-n1">
          <v-radio
            class="mx-0"
            :label="$t('layout.dynamicSectionTypeChoice')"
            value="FlexContainer" />
          <v-radio
            class="mx-0"
            :label="$t('layout.fixedSectionTypeChoice')"
            value="GridContainer" />
        </v-radio-group>

        <section-template-grid-editor
          v-if="sectionType === $layoutUtils.gridTemplate"
          class="mt-4"
          :cols-count="cols"
          :rows-count="rows"
          @cols-updated="cols = $event"
          @rows-updated="rows = $event" />
        <section-template-flex-editor
          v-else-if="sectionType === $layoutUtils.flexTemplate"
          class="mt-4"
          :cols-count="cols"
          @cols-updated="cols = $event" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn"
          :disabled="saving"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
          class="btn btn-primary ms-4"
          :loading="saving"
          @click="next">
          <span class="text-none">{{ $t('layout.next') }}</span>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      sectionType: null,
      drawer: false,
      saving: false,
      rows: 0,
      cols: 0,
    }),
    watch: {
      sectionType () {
        if (this.sectionType === eXo.$layoutUtils.gridTemplate) {
          this.rows = 4;
          this.cols = 12;
        } else if (this.sectionType === eXo.$layoutUtils.flexTemplate) {
          this.rows = 1;
          this.cols = 3;
        }
      },
    },
    created () {
      this.$root.$on('section-template-add', this.open);
    },
    beforeUnmount () {
      this.$root.$off('section-template-add', this.open);
    },
    methods: {
      close () {
        this.$refs.drawer.close();
      },
      async open () {
        this.sectionType = eXo.$layoutUtils.flexTemplate;
        await this.$nextTick();
        this.$refs.drawer.open();
      },
      async next () {
        this.saving = true;
        try {
          const section = eXo.$layoutUtils.newSection(null, null, this.rows, this.cols, this.sectionType);
          const sectionTemplate = await eXo.$sectionTemplateService.createSectionTemplate({
            category: 'custom',
            content: JSON.stringify(section),
          });
          this.$root.$emit('section-template-created', sectionTemplate);
          this.close();
        } finally {
          this.saving = false;
        }
      },
    },
  };
</script>