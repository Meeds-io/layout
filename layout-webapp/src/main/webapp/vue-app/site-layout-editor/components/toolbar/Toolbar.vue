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
  <v-card
    class="d-flex align-center no-border-radius border-box-sizing px-4 layout-sticky-top-bar"
    flat
    height="57"
    min-width="100vw"
    width="100vw">
    <v-icon class="icon-default-color">fa-pager</v-icon>
    <span class="px-2">{{ $t('layout.editSiteName', {0: siteLabel}) }}</span>
    <v-spacer />
    <site-layout-editor-toolbar-history-buttons class="me-3" />
    <site-layout-editor-toolbar-edit-sections-button class="me-3" />
    <site-layout-editor-toolbar-properties-button class="me-3" />
    <site-layout-editor-toolbar-mobile-preview-button class="me-3" />
    <site-layout-editor-toolbar-site-preview-button class="me-3" />
    <site-layout-editor-toolbar-save-draft-button />
    <site-layout-editor-toolbar-save-button />
  </v-card>
</template>
<script>
  export default {
    computed: {
      disabledDraft () {
        return !this.$root.sectionHistory?.length && !this.$root.sectionRedo?.length;
      },
      siteLabel () {
        return this.$root.isSiteTemplate ? this.$root.siteTemplate?.name : this.$root.site?.displayName;
      },
    },
    mounted () {
      document.querySelector('#vuetify-apps').append(this.$el);
    },
  };
</script>
