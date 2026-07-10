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
    id="selectApplicationCategoryDrawer"
    v-model="drawer"
    :loading="$root.loadingPortletInstances"
    allow-expand
    right
    @expand-updated="expanded = $event"
    @closed="$root.$emit('layout-application-category-drawer-closed')">
    <template #title>
      {{ $t('layout.selectApplicationCategoryTitle') }}
    </template>
    <template v-if="drawer" #content>
      <application-toolbar
        id="selectApplicationCategoryToolbar"
        compact
        :left-text="$t('layout.filterApplication.label')"
        :right-text-filter="{
          minCharacters: 1,
          placeholder: $t('layout.filterApplication.placeholder'),
          tooltip: $t('layout.filterApplication.placeholder'),
        }"
        class="border-box-sizing px-1"
        @filter-text-input="keyword = $event" />
      <v-card
        v-if="!keyword"
        max-width="100%"
        class="d-flex flex-wrap mb-4 ms-4 me-2 overflow-hidden"
        flat>
        <layout-editor-application-category-card
          v-for="category in sortedCategories"
          :key="category.id"
          :category="category"
          :applications="applications"
          class="me-2" />
      </v-card>
      <v-card
        v-else
        :class="expanded && 'flex-wrap' || 'flex-column'"
        max-width="100%"
        class="d-flex justify-center ma-4 overflow-hidden"
        flat>
        <layout-editor-application-card
          v-for="application in filteredApplications"
          :key="application.id"
          :application="application"
          :width="expanded && '388px' || '100%'"
          :height="expanded && '210px' || 'auto'"
          :max-image-height="expanded && '100%' || '110px'"
          max-image-width="100%"
          :class="expanded && 'mx-2 content-box-sizing'"
          class="flex-grow-1 mb-4"
          @add="addApplication(application)" />
      </v-card>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    expanded: false,
    keyword: null,
    portletInstances: [],
  }),
  computed: {
    portletInstanceCategories() {
      const portletInstanceCategories = this.$root.portletInstanceCategories.slice();
      portletInstanceCategories.forEach(c => c.label = this.$te(`layout.${c.name}`) ? this.$t(`layout.${c.name}`) : c.name);
      return portletInstanceCategories;
    },
    applications() {
      return this.$root.portletInstances;
    },
    sortedCategories() {
      const categories = this.portletInstanceCategories?.filter?.(c => c.name) || [];
      categories.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return categories;
    },
    sortedApplications() {
      const applications = this.applications?.filter?.(a => a.name) || [];
      applications.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return applications;
    },
    filteredApplications() {
      return this.keyword?.length && this.sortedApplications.filter(a => {
        const name = this.$te(a.name) ? this.$t(a.name) : a.name;
        const description = this.$te(a.description) ? this.$t(a.description) : a.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || [];
    },
  },
  created() {
    this.$root.$on('layout-add-application', this.close);
  },
  beforeDestroy() {
    this.$root.$off('layout-add-application', this.close);
  },
  methods: {
    open() {
      this.keyword = null;
      this.$root.$emit('layout-editor-portlet-instances-refresh');
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    addApplication(application) {
      this.$root.$emit('layout-add-application', application);
    },
  },
};
</script>