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
  <v-list-item
    v-if="spaceTemplate?.system"
    dense
    @click="restoreDefaultPages">
    <v-icon size="13">
      fa-redo
    </v-icon>
    <v-list-item-title class="ps-2">
      {{ $t('spaceTemplate.label.restoreDefaultPages') }}
    </v-list-item-title>
  </v-list-item>
</template>
<script>
export default {
  props: {
    spaceTemplate: {
      type: Object,
      default: null,
    },
  },
  beforeDestroy() {
    this.$emit('loading', false);
  },
  methods: {
    async restoreDefaultPages() {
      this.$emit('loading', true);
      try {
        await this.$siteLayoutService.restoreSite({
          siteType: 'group_template',
          siteName: this.spaceTemplate.layout,
          importMode: 'MERGE',
          siteLayout: false,
          sitePages: true,
          siteNavigation: true,
        });
        this.$root.$emit('alert-message', window.vueI18nMessages['siteManagement.label.restoreSitePages.success'], 'success');
      } catch (e) {
        this.$root.$emit('alert-message', window.vueI18nMessages['siteManagement.label.restoreSitePages.error'], 'error');
      } finally {
        this.$emit('loading', false);
      }
    },
  },
};
</script>