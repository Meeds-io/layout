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
      {{ $t('portletInstance.label.importInstance') }}
    </template>
    <template #content>
      <div class="pa-4" flat>
        <div class="text-header">
          {{ header }}
        </div>
        <v-list-item class="pa-0">
          <v-list-item-content class="pb-3">
            <v-list-item-title>
              {{ fileName }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action v-if="!importFinished">
            <v-btn
              class="dark-grey-color"
              text
              @click="changeFile">
              <v-icon size="18" class="icon-default-color me-2">fa-redo</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
        <template v-if="!importFinished">
          <div class="text-header">
            {{ $t('portletInstance.label.selectDestination') }}
          </div>
          <portlets-instance-category-input
            v-model="categoryId"
            class="mt-4" />
        </template>
        <template v-else>
          <v-list-item
            class="px-0"
            dense>
            <v-list-item-avatar
              class="ms-0"
              size="20">
              <v-icon
                size="20"
                class="success--text"
                dark>
                fa-check-circle
              </v-icon>
            </v-list-item-avatar>
            <v-list-item-content>
              <v-list-item-title>{{ $t('portletInstance.label.characteristics') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            class="px-0"
            dense>
            <v-list-item-avatar
              class="ms-0"
              size="20">
              <v-icon
                size="20"
                class="success--text"
                dark>
                fa-check-circle
              </v-icon>
            </v-list-item-avatar>
            <v-list-item-content>
              <v-list-item-title>{{ $t('portletInstance.label.layoutAndPreferences') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </template>
        <div class="d-flex justify-center pt-5">
          <v-btn
            :disabled="disableDeserializeButton || loading"
            class="btn btn-primary"
            @click="handleButtonClick">
            <v-progress-circular
              v-if="loading"
              indeterminate
              size="20"
              class="me-2" />
            {{ buttonLabel }}
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
    importFinished: false

  }),
  computed: {
    disableDeserializeButton() {
      return !this.fileName || !this.categoryId;
    },
    header() {
      return this.importFinished ? this.$t('portletInstance.label.importInProgress') : this.$t('portletInstance.label.templateToImport');
    },
    buttonLabel() {
      return this.importFinished ? this.$t('portletInstance.label.close') : this.$t('portletInstance.label.import');
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
    close() {
      this.importFinished = false;
      this.fileName = '';
      this.uploadId = '';
      this.categoryId = '';
      this.$refs.deserializeDrawer.close();
    },
    async deserializePortletInstances() {
      if (!this.uploadId) {
        return;
      }
      this.loading = true;
      const params = {
        categoryId: this.categoryId
      };
      const databind = {
        objectType: 'PortletInstance',
        uploadId: this.uploadId,
        params: params
      };
      try {
        await this.$databindService.deserialize(databind);
        this.importFinished = true;
        this.$root.$emit('portlet-instance-saved');
      } catch (error) {
        if (String(error).indexOf('databind.notMatchType') >= 0) {
          this.$root.$emit('alert-message', this.$t('portletInstance.label.exception.notMatchType'), 'error');
        } else {
          this.$root.$emit('alert-message', String(error) , 'error');
        }
      } finally {
        this.loading = false;
      }
    },
    changeFile() {
      this.$root.$emit('portlet-instance-file-explorer');
    },
    handleButtonClick() {
      if (this.importFinished) {
        this.close();
      } else {
        this.deserializePortletInstances();
      }
    }
  }
};
</script>