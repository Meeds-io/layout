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
    id="addApplicationDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    go-back-button
    right
    @closed="$root.$emit('layout-application-drawer-closed')"
    @expand-updated="expanded = $event">
    <template #title>
      <span class="text-truncate">{{ $t('layout.addApplicationFromCategoryTitle', {0: categoryName}) }}</span>
    </template>
    <template v-if="drawer" #content>
      <v-card
        class="d-flex justify-center ma-4 overflow-hidden"
        :class="expanded && 'flex-wrap' || 'flex-column'"
        flat
        max-width="100%">
        <layout-editor-application-card
          v-for="application in sortedPortletInstances"
          :key="application.id"
          :application="application"
          class="flex-grow-1 mb-4"
          :class="expanded && 'mx-2 content-box-sizing'"
          :height="expanded && '210px' || 'auto'"
          :max-image-height="expanded && '100%' || '110px'"
          max-image-width="100%"
          :width="expanded && '388px' || '100%'"
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
      portletInstances: [],
      category: null,
    }),
    computed: {
      categoryName () {
        return this.category?.name;
      },
      sortedPortletInstances () {
        const categories = this.portletInstances?.filter?.(c => c.name) || [];
        categories.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return categories;
      },
    },
    created () {
      this.$root.$on('layout-add-application-drawer', this.open);
    },
    methods: {
      open (portletInstances, category) {
        this.portletInstances = portletInstances;
        this.category = category;
        this.$refs.drawer.endLoading();
        this.$refs.drawer.open();
      },
      addApplication (application) {
        this.$refs.drawer.startLoading();
        this.$root.$emit('layout-add-application', application);
        window.setTimeout(() => {
          this.close();
        }, 200);
      },
      close () {
        this.$refs.drawer.endLoading();
        this.$refs.drawer.close();
      },
    },
  };
</script>