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
    id="selectApplicationCategoryDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    :loading="$root.loadingPortletInstances"
    right
    @closed="$root.$emit('layout-application-category-drawer-closed')">
    <template #title>
      {{ $t('layout.selectApplicationCategoryTitle') }}
    </template>
    <template v-if="drawer" #content>
      <v-card
        class="d-flex flex-wrap mb-4 ms-4 me-2 overflow-hidden"
        flat
        max-width="100%">
        <layout-editor-application-category-card
          v-for="category in sortedCategories"
          :key="category.id"
          :applications="applications"
          :category="category"
          class="me-2" />
      </v-card>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      portletInstances: [],
    }),
    computed: {
      portletInstanceCategories () {
        const portletInstanceCategories = this.$root.portletInstanceCategories.slice();
        portletInstanceCategories.forEach(c => c.label = this.$te(`layout.${c.name}`) ? this.$t(`layout.${c.name}`) : c.name);
        return portletInstanceCategories;
      },
      applications () {
        return this.$root.portletInstances;
      },
      sortedCategories () {
        const categories = this.portletInstanceCategories?.filter?.(c => c.name) || [];
        categories.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return categories;
      },
    },
    created () {
      this.$root.$on('layout-add-application', this.close);
    },
    beforeUnmount () {
      this.$root.$off('layout-add-application', this.close);
    },
    methods: {
      open () {
        this.$root.$emit('layout-editor-portlet-instances-refresh');
        this.$refs.drawer.open();
      },
      close () {
        this.$refs.drawer.close();
      },
    },
  };
</script>