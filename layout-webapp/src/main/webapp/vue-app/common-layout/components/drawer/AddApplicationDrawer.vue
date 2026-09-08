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
    id="addApplicationDrawer"
    v-model="drawer"
    allow-expand
    go-back-button
    right
    @expand-updated="expanded = $event"
    @closed="$root.$emit('layout-application-drawer-closed')">
    <template slot="title">
      <span class="text-truncate">{{ $t('layout.addApplicationFromCategoryTitle', {0: categoryName}) }}</span>
    </template>
    <template v-if="drawer" #content>
      <application-toolbar
        id="addApplicationToolbar"
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
        :class="expanded && 'flex-wrap' || 'flex-column'"
        max-width="100%"
        class="d-flex justify-center ma-4 overflow-hidden"
        flat>
        <layout-editor-application-card
          v-for="application in filteredPortletInstances"
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
    category: null,
  }),
  computed: {
    categoryName() {
      return this.category?.name;
    },
    sortedPortletInstances() {
      const categories = this.portletInstances?.filter?.(c => c.name) || [];
      categories.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return categories;
    },
    filteredPortletInstances() {
      return this.keyword?.length && this.sortedPortletInstances.filter(a => {
        const name = this.$te(a.name) ? this.$t(a.name) : a.name;
        const description = this.$te(a.description) ? this.$t(a.description) : a.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || this.sortedPortletInstances;
    },
  },
  created() {
    this.$root.$on('layout-add-application-drawer', this.open);
  },
  methods: {
    open(portletInstances, category) {
      this.keyword = null;
      this.portletInstances = portletInstances;
      this.category = category;
      this.$refs.drawer.endLoading();
      this.$refs.drawer.open();
    },
    addApplication(application) {
      this.$refs.drawer.startLoading();
      this.$root.$emit('layout-add-application', application);
      window.setTimeout(() => {
        this.close();
      }, 200);
    },
    close() {
      this.$refs.drawer.endLoading();
      this.$refs.drawer.close();
    },
  },
};
</script>