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
  <v-hover v-if="hasApplications" v-model="hover">
    <v-card
      :aria-label="$utils.htmlToText(categoryLabel)"
      class="d-flex flex-column border-color border-radius overflow-hidden mt-4 align-center justify-center aspect-ratio-1"
      :elevation="hover && 2 || 0"
      max-width="120px"
      min-width="110px"
      :title="$utils.htmlToText(categoryLabel)"
      width="calc(33% - 8px)"
      @click="$root.$emit('layout-add-application-drawer', categoryApplications, category)">
      <v-icon class="mt-auto mb-2">{{ categoryIcon }}</v-icon>
      <div
        v-sanitized-html="categoryLabel"
        class="text-truncate-2 full-width mb-auto mt-2 px-1"></div>
    </v-card>
  </v-hover>
</template>
<script>
  export default {
    props: {
      expanded: {
        type: Boolean,
        default: false,
      },
      category: {
        type: Object,
        default: null,
      },
      parentId: {
        type: Object,
        default: null,
      },
      container: {
        type: Object,
        default: null,
      },
      applications: {
        type: Array,
        default: null,
      },
    },
    data: () => ({
      hover: false,
    }),
    computed: {
      categoryName () {
        return this.category?.name;
      },
      categoryLabel () {
        return this.category?.label || this.category?.name;
      },
      categoryIcon () {
        return this.category?.icon;
      },
      categoryId () {
        return this.category?.id;
      },
      categoryApplications () {
        return this.applications?.filter?.(a => (a.categoryId && a.categoryId === this.categoryId) || (!a.categoryId && a.applicationName === this.categoryName)) || [];
      },
      hasApplications () {
        return this.categoryApplications.length > 0;
      },
    },
  };
</script>

