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
  <div class="d-inline">
    <v-tooltip :disabled="!$root.systemSelectedPortletInstances" bottom>
      <template #activator="{ on, attrs }">
        <div
          v-on="on"
          v-bind="attrs">
          <v-btn
            :disabled="$root.isBulkProcessing || $root.systemSelectedPortletInstances"
            color="error"
            elevation="0"
            outlined
            @click="openConfirmDialog">
            <v-icon size="16" class="me-2">fa-trash</v-icon>
            {{ $t('portlets.label.delete') }}
          </v-btn>
        </div>
      </template>
      <span>{{ $t('portlets.label.system.noDelete') }}</span>
    </v-tooltip>
    <confirm-dialog
      ref="dialog"
      :title="$t('portlets.label.bulk.deleteConfirmTitle')"
      :message="$t('portlets.label.bulk.deleteConfirmMessage')"
      :ok-label="$t('portlets.label.confirm')"
      :cancel-label="$t('portlets.label.cancel')"
      @ok="deletePortletsInstances"
      @closed="close" />
  </div>
</template>
<script>
export default {
  methods: {
    openConfirmDialog() {
      window.setTimeout(() => this.$refs.dialog.open(), 200);
    },
    deletePortletsInstances() {
      this.$root.applyOperationInBulk(
        portletInstance => {
          if (!portletInstance.system) {
            return this.$portletInstanceService.deletePortletInstance(portletInstance.id);
          }
          return Promise.resolve();
        },
        null,
        () => {
          this.$root.$emit('alert-message', this.$root.$t('portlets.label.bulk.delete.success'), 'success');
          this.$root.$emit('portlets-instances-list-refresh');
        });
    },
    close() {
      window.setTimeout(() => this.dialog = false, 200);
    },
  },
};
</script>