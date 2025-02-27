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
  <exo-drawer
    ref="deserializeDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    allow-expand
    right>
    <template #title>
      Import Instance
    </template>
    <template #content>
      <div class="pa-4" flat>
        <div class="text-header">
          Template to import
        </div>
        <v-list-item class="pa-0">
          <v-list-item-content class="pb-3">
            <v-list-item-title>
              {{ fileName }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
              class="dark-grey-color"
              text
              @click="changeFile">
              <v-icon size="18" class="icon-default-color me-2">fa-redo</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
        <div class="text-header">
          Select Destination
        </div>
        <portlets-instance-category-input
          v-model="categoryId"
          class="mt-4" />
        <div class="text-header pt-4">
          Duplicate management rule
        </div>
        <v-card-text class="pb-0 pt-2">
          <v-radio-group v-model="duplicateType" class="ma-0">
            <v-radio
              label="Replace existing item"
              value="REPLACE" />
            <v-radio
              label="Duplicate if already existing"
              value="DUPLICATE" />
          </v-radio-group>
        </v-card-text>
        <div class="d-flex justify-center pt-5">
          <v-btn
            :disabled="disableDeserializeButton"
            class="btn btn-primary"
            @click="deserializePortletInstances">
            Import
          </v-btn>
        </div>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    loading: false,
    fileName: '',
    uploadId: '',
    categoryId: '',
    duplicateType: 'REPLACE'
  }),
  computed: {
    disableDeserializeButton() {
      return !this.fileName || !this.categoryId;
    }
  },
  created() {
    this.$root.$on('deserialize-instance-drawer-open', this.open);
  },
  methods: {
    open(uploadId, fileName) {
      this.fileName = fileName;
      this.uploadId = uploadId;
      this.$refs.deserializeDrawer.open();
    },
    deserializePortletInstances() {
      if (!this.uploadId) {
        return;
      }
      const params = {
        categoryId: this.categoryId
      };
      const databind = {
        objectType: 'PortletInstance',
        uploadId: this.uploadId,
        replaceExisting: true,
        params: params
      };

      return this.$databindService.deserialize(databind);

    },
    changeFile() {
      this.$root.$emit('portlet-instance-file-explorer');
    }
  }
};
</script>