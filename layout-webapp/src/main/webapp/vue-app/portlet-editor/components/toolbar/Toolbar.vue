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
  <v-card
    class="d-flex align-center no-border-radius border-box-sizing px-4 layout-sticky-top-bar"
    flat
    height="57"
    min-width="100vw"
    width="100vw">
    <v-icon class="icon-default-color">fa-pager</v-icon>
    <span v-sanitized-html="title" class="px-2"></span>
    <v-spacer />
    <portlet-editor-toolbar-edit-button
      v-if="canSwitchToEditMode"
      class="mx-2" />
    <extension-registry-component
      v-if="extendedComponentOptions"
      :component="extendedComponentOptions" />
    <portlet-editor-toolbar-save-button
      v-else
      :disabled="$root.portletMode !== 'view'" />
  </v-card>
</template>
<script>
  export default {
    data: () => ({
      saveButtonExtension: null,
    }),
    computed: {
      title () {
        return this.$t('layout.editPortletInstance', { 0: `<strong>${this.$root?.portletInstance?.name}</strong>` });
      },
      canSwitchToEditMode () {
        return this.$root.portletInstance?.supportedModes?.find?.(mode => mode === 'edit');
      },
      extendedComponentOptions () {
        return this.saveButtonExtension
          && (!this.saveButtonExtension.portletMode
            || this.saveButtonExtension.portletMode === this.$root.portletMode)
          && {
            componentName: this.saveButtonExtension.id,
            componentOptions: {
              vueComponent: this.saveButtonExtension.component,
            },
          };
      },
    },
    created () {
      document.addEventListener('extension-Layout-PortletEditorSaveButton-updated', this.refreshSaveButtonExtension);
      this.refreshSaveButtonExtension();
    },
    mounted () {
      document.querySelector('#vuetify-apps').append(this.$el);
    },
    methods: {
      refreshSaveButtonExtension () {
        const extensions = extensionRegistry.loadExtensions('Layout', 'PortletEditorSaveButton');
        this.saveButtonExtension = extensions?.[0];
      },
    },
  };
</script>
