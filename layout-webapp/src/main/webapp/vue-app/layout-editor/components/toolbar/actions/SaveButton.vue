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
  <v-tooltip v-if="canSave" bottom>
    <template #activator="{on, attrs}">
      <div
        v-bind="attrs"
        v-on="on">
        <v-btn
          :aria-label="$t('layout.publish')"
          class="btn btn-primary d-flex align-center"
          :disabled="disabled"
          elevation="0"
          :loading="loading"
          @click="savePage">
          <span class="text-none">{{ $t('layout.publish') }}</span>
        </v-btn>
      </div>
    </template>
    <span>{{ $t('layout.publishTooltip') }}</span>
  </v-tooltip>
</template>
<script>
  export default {
    props: {
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      loading: false,
    }),
    computed: {
      canSave () {
        return eXo.env.portal.selectedNodeId !== this.$root.nodeId;
      },
    },
    methods: {
      savePage () {
        if (!this.canSave) {
          return;
        }
        this.loading = true;
        const layoutToUpdate = eXo.$layoutUtils.cleanAttributes(this.$root.layout, false, true);
        return eXo.$pageLayoutService.updatePageLayout(this.$root.pageRef, layoutToUpdate, 'contentId', true)
          .then(() => this.$root.$emit('layout-page-saved'))
          .catch(e => this.$root.$emit('alert-message', this.$te(e.message) ? this.$t(e.message) : this.$t('layout.pageSavingError'), 'error'))
          .finally(() => window.setTimeout(() => this.loading = false));
      },
    },
  };
</script>