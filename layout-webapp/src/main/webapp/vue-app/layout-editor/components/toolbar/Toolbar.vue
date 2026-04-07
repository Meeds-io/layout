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
    height="57"
    min-width="100vw"
    width="100vw"
    flat>
    <v-icon class="icon-default-color">fa-pager</v-icon>
    <span v-if="pageTemplate" class="px-2">{{ $t('layout.editPageTemplate', {0: pageTemplate.name}) }}</span>
    <span v-else-if="!pageTemplateId" class="px-2">{{ $t('layout.editPageName', {0: pageName}) }}</span>
    <v-spacer />
    <layout-editor-toolbar-history-buttons class="me-3" />
    <layout-editor-toolbar-save-as-template-button
      v-if="!pageTemplateId && $root.isAdministrator"
      class="me-3" />
    <layout-editor-toolbar-page-properties-button
      class="me-3" />
    <layout-editor-toolbar-page-space-preview-button
      v-if="$root.isSpaceSiteTemplate"
      class="me-3" />
    <layout-editor-toolbar-page-preview-button
      v-else-if="!pageTemplateId"
      class="me-3" />
    <layout-editor-toolbar-mobile-preview-button
      class="me-3" />
    <layout-editor-toolbar-save-template-button
      v-if="pageTemplateId" />
    <layout-editor-toolbar-save-draft-button
      v-if="!pageTemplateId"
      class="me-3" />
    <layout-editor-toolbar-save-button
      v-if="!pageTemplateId" />
  </v-card>
</template>
<script>
export default {
  props: {
    page: {
      type: Object,
      default: null,
    },
    node: {
      type: Object,
      default: null,
    },
    nodeLabels: {
      type: Object,
      default: null,
    },
  },
  computed: {
    disabledDraft() {
      return !this.$root.sectionHistory?.length && !this.$root.sectionRedo?.length;
    },
    defaultLanguage() {
      return eXo.env.portal.defaultLanguage;
    },
    pageTemplateId() {
      return this.$root.pageTemplateId;
    },
    pageTemplate() {
      return this.$root.pageTemplate;
    },
    pageName() {
      return this.nodeLabels?.labels && (this.nodeLabels.labels[eXo.env.portal.language] || this.nodeLabels.labels[this.defaultLanguage]) || this.page?.title;
    },
  },
  mounted() {
    const container = document.querySelector('#vuetify-apps');
    if (!container.hasChildNodes(this.$el)) {
      container.append(this.$el);
    }
  },
};
</script>
