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
  <v-dialog
    v-model="dialog"
    :width="width"
    content-class="uiPopup full-width full-height overflow-x-hidden"
    persistent
    max-width="100vw">
    <v-card class="full-height full-width" flat>
      <v-flex class="ms-5 me-0 drawerHeader flex-grow-0">
        <v-list-item class="pe-0 ps-1">
          <v-list-item-content class="drawerTitle align-start text-title text-truncate">
            {{ drawerTitle }}
          </v-list-item-content>
          <v-list-item-action class="drawerIcons align-end d-flex flex-row">
            <v-btn
              :title="$t('label.close')"
              icon>
              <v-icon @click="close()">mdi-close</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-flex>
      <v-divider class="my-0" />
      <v-card
        v-if="dialog"
        :id="applicationId"
        data-mode="EDIT"
        max-width="100%"
        class="ma-4"
        flat>
        <div
          ref="content"
          :id="id"
          class="layout-application"></div>
      </v-card>
    </v-card>
  </v-dialog>
</template>
<script>
export default {
  data: () => ({
    dialog: false,
    expand: false,
    applicationId: null,
    applicationTitle: null,
  }),
  computed: {
    drawerTitle() {
      return this.$t('layout.editPortletTitleNoCategory', {
        0: this.applicationTitle,
      });
    },
    nodeUri() {
      return this.$root.draftNodeUri;
    },
  },
  watch: {
    dialog() {
      if (this.dialog) {
        window.eXo.env.portal.maximizedPortletMode = 'EDIT';
        window.setTimeout(() => this.installApplication(), 200);
      } else {
        window.eXo.env.portal.maximizedPortletMode = null;
        this.$root.$emit('layout-editor-portlet-properties-updated', this.applicationId);
      }
    },
  },
  created() {
    this.$root.$on('layout-editor-portlet-edit', this.open);
  },
  beforeDestroy() {
    this.$root.$off('layout-editor-portlet-edit', this.open);
  },
  methods: {
    open(applicationId, applicationTitle) {
      this.applicationId = applicationId;
      this.applicationTitle = applicationTitle;
      this.$nextTick(() => this.dialog = true);
    },
    close() {
      this.dialog = false;
    },
    installApplication() {
      if (this.$refs.content) {
        this.$applicationUtils.installApplication(this.nodeUri, this.applicationId, this.$refs.content, 'EDIT', this.$root.isSiteLayout, true);
      }
    },
  },
};
</script>