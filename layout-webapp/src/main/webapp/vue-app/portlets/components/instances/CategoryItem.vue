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
  <v-hover @input="hover = $event">
    <v-list-item
      :aria-label="category.name"
      role="listitem"
      :value="category.id">
      <v-list-item-icon class="my-auto me-4">
        <v-card
          class="transparent d-flex align-center justify-center"
          flat
          width="30">
          <v-icon class="icon-default-color">{{ category.icon }}</v-icon>
        </v-card>
      </v-list-item-icon>
      <v-list-item-content>
        <v-list-item-title :title="category.name">
          {{ category.name }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action class="my-auto">
        <v-card
          class="transparent d-flex align-center justify-center"
          flat
          width="30">
          <portlets-instance-category-menu
            v-if="hover || menu"
            :category="category"
            @input="menu = $event" />
        </v-card>
      </v-list-item-action>
    </v-list-item>
  </v-hover>
</template>
<script>
  export default {
    props: {
      category: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      hover: false,
      menu: false,
    }),
  };
</script>