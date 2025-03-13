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
  <v-tooltip
    v-if="spaceTemplate.layout"
    bottom
    :disabled="!spaceTemplate.system">
    <template #activator="{ on, attrs }">
      <div
        v-bind="attrs"
        v-on="on">
        <v-list-item
          dense
          :disabled="spaceTemplate.system"
          @click="openSiteNavigationDrawer">
          <v-icon
            :class="spaceTemplate.system && 'disabled--text'"
            size="13">
            fa-columns
          </v-icon>
          <v-list-item-title class="ps-2">
            <span :class="spaceTemplate.system && 'disabled--text'">{{ $t('spaceTemplate.label.editNavigation') }}</span>
          </v-list-item-title>
        </v-list-item>
      </div>
    </template>
    <span>{{ $t('spaceTemplate.label.system.noEditLayout') }}</span>
  </v-tooltip>
</template>
<script>
  export default {
    props: {
      spaceTemplate: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      site: null,
    }),
    beforeUnmount () {
      this.$emit('loading', false);
    },
    methods: {
      async openSiteNavigationDrawer () {
        this.$emit('loading', true);
        try {
          if (!this.site) {
            this.site = await eXo.$siteService.getSite('group_template', this.spaceTemplate?.layout, {
              expandNavigations: false,
            });
          }
          document.dispatchEvent(new CustomEvent('open-site-navigation-drawer',{ detail: {
            siteName: this.site.name,
            siteType: this.site.siteType,
            siteId: this.site.siteId,
            siteLabel: this.spaceTemplate?.name,
            includeGlobal: false,
          } }));
        } finally {
          this.$emit('loading', false);
        }
      },
    },
  };
</script>