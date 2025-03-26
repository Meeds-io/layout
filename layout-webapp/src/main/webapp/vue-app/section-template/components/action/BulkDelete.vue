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
    <v-tooltip :disabled="!$root.systemSelectedSectionTemplates" bottom>
      <template #activator="{ on, attrs }">
        <div
          v-on="on"
          v-bind="attrs">
          <v-btn
            :disabled="$root.isBulkProcessing || $root.systemSelectedSectionTemplates"
            color="error"
            elevation="0"
            outlined
            @click="openConfirmDialog">
            <v-icon size="16" class="me-2">fa-trash</v-icon>
            {{ $t('sectionTemplates.label.delete') }}
          </v-btn>
        </div>
      </template>
      <span>{{ $t('sectionTemplates.label.system.noDelete') }}</span>
    </v-tooltip>
    <confirm-dialog
      ref="dialog"
      :title="$t('sectionTemplates.label.bulk.deleteConfirmTitle')"
      :message="$t('sectionTemplates.label.bulk.deleteConfirmMessage')"
      :ok-label="$t('sectionTemplates.label.confirm')"
      :cancel-label="$t('sectionTemplates.label.cancel')"
      @ok="deleteSectionTemplates"
      @closed="close" />
  </div>
</template>
<script>
export default {
  methods: {
    openConfirmDialog() {
      window.setTimeout(() => this.$refs.dialog.open(), 200);
    },
    deleteSectionTemplates() {
      this.$root.applyOperationInBulk(
        sectionTemplate => {
          if (!sectionTemplate.system) {
            return this.$sectionTemplateService.deleteSectionTemplate(sectionTemplate.id);
          }
          return Promise.resolve();
        },
        null,
        () => {
          this.$root.$emit('alert-message', this.$root.$t('sectionTemplates.label.bulk.delete.success'), 'success');
          this.$root.$emit('section-templates-list-refresh');
        });
    },
    close() {
      window.setTimeout(() => this.dialog = false, 200);
    },
  },
};
</script>