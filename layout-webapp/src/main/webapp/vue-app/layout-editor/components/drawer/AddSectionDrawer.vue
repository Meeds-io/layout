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
    id="addSectionDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    disable-pull-to-refresh
    :loading="loading"
    right>
    <template #title>
      {{ $t('layout.addSectionTitle') }}
    </template>
    <template v-if="drawer && sectionTemplates" #content>
      <v-card class="pa-4" flat>
        <div class="text-header">
          {{ $t('layout.chooseATemplate') }}
        </div>
        <template v-if="blankSectionTemplates?.length">
          <div class="font-weight-bold mt-4">
            {{ $t('layout.section.category.blank') }}
          </div>
          <div class="d-flex flex-wrap me-n4">
            <layout-editor-section-template
              v-for="t in blankSectionTemplates"
              :key="t.id"
              class="col-auto ps-0 pe-4"
              :section-template="t"
              :selected="t.id === selectedSectionTemplate?.id"
              @select="selectedSectionTemplate = t" />
          </div>
        </template>
        <template v-if="defaultSectionTemplates?.length">
          <div class="font-weight-bold mt-4">
            {{ $t('layout.section.category.default') }}
          </div>
          <div class="d-flex flex-wrap me-n4">
            <layout-editor-section-template
              v-for="t in defaultSectionTemplates"
              :key="t.id"
              class="col-auto ps-0 pe-4"
              :section-template="t"
              :selected="t.id === selectedSectionTemplate?.id"
              @select="selectedSectionTemplate = t" />
          </div>
        </template>
        <template v-if="customSectionTemplates?.length">
          <div class="font-weight-bold mt-4">
            {{ $t('layout.section.category.custom') }}
          </div>
          <div class="d-flex flex-wrap me-n4">
            <layout-editor-section-template
              v-for="t in customSectionTemplates"
              :key="t.id"
              class="col-auto ps-0 pe-4"
              :section-template="t"
              :selected="t.id === selectedSectionTemplate?.id"
              @select="selectedSectionTemplate = t" />
          </div>
        </template>
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
          :disabled="!selectedSectionTemplate"
          @click="apply">
          <span class="text-none">{{ $t('layout.create') }}</span>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      selectedSectionTemplate: null,
      parentContainer: null,
      sectionTemplates: null,
      loading: false,
      drawer: false,
      index: null,
    }),
    computed: {
      blankSectionTemplates () {
        return this.sectionTemplates?.filter?.(t => t.category === 'blank' && t.name && !t.disabled);
      },
      defaultSectionTemplates () {
        return this.sectionTemplates?.filter?.(t => t.category === 'default' && t.name && !t.disabled);
      },
      customSectionTemplates () {
        return this.sectionTemplates?.filter?.(t => t.category === 'custom' && t.name && !t.disabled);
      },
    },
    methods: {
      async open (parentContainer, index) {
        this.parentContainer = parentContainer;
        this.index = index;
        this.selectedSectionTemplate = null;
        this.$nextTick().then(() => this.$refs.drawer.open());
        this.loading = true;
        try {
          this.sectionTemplates = await eXo.$sectionTemplateService.getSectionTemplates();
        } finally {
          this.loading = false;
        }
      },
      apply () {
        this.section = JSON.parse(this.selectedSectionTemplate.content);
        this.$root.$emit('layout-add-section', this.section, this.index);
        this.close();
      },
      close () {
        this.$refs.drawer.close();
        this.section = null;
      },
    },
  };
</script>