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
  <v-fade-transition>
    <div
      v-show="menu"
      class="position-sticky t-20 z-index-drawer mx-auto">
      <div
        ref="menu"
        class="layout-no-multi-select absolute-horizontal-center z-index-drawer t-0 mt-n4">
        <v-chip color="white" class="elevation-2 no-border">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-show="$root.desktopDisplayMode"
                v-on="on"
                v-bind="attrs"
                :aria-label="$t('layout.moveCell')"
                :width="iconSize"
                :height="iconSize"
                class="draggable-cell ms-2"
                icon
                @mousedown="dragStart"
                @mouseup="dragEnd">
                <v-icon :size="iconSize" class="icon-default-color">fa-arrows-alt</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.moveCell') }}
          </v-tooltip>
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                :aria-label="$t('layout.editApplication')"
                :width="iconSize"
                :height="iconSize"
                class="mx-4"
                icon
                @click.prevent.stop="$root.$emit('layout-edit-application', sectionId, container, applicationTitle)">
                <v-icon :size="iconSize" class="icon-default-color">fa-edit</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.editApplication') }}
          </v-tooltip>
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                :aria-label="$t('layout.deleteApplication')"
                :width="iconSize"
                :height="iconSize"
                class="me-2"
                icon
                @click.prevent.stop="$root.$emit('layout-delete-application', sectionId, container)">
                <v-icon :size="iconSize" class="icon-default-color">fa-trash</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.deleteApplication') }}
          </v-tooltip>
        </v-chip>
      </div>
    </div>
  </v-fade-transition>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    section: {
      type: Object,
      default: null,
    },
    parentId: {
      type: String,
      default: null,
    },
    applicationTitle: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    iconSize: 20,
    menu: false,
  }),
  computed: {
    sectionId() {
      return this.section?.storageId;
    },
    isDynamicSection() {
      return this.section?.template === this.$layoutUtils.flexTemplate
        || this.section?.template === this.$layoutUtils.bannerCellTemplate
        || this.section?.template === this.$layoutUtils.sidebarCellTemplate;
    },
    drawerOpened() {
      return this.$root.drawerOpened;
    },
  },
  watch: {
    drawerOpened() {
      if (this.drawerOpened) {
        this.hideMenu();
      }
    },
  },
  methods: {
    displayMenu() {
      if (this.$root.movingParentId) {
        return;
      }
      this.$root.hoveredSection = this.section;
      this.$root.hoveredApplication = this.container;
      this.$root.hoveredParentId = this.parentId;
      this.menu = true;
    },
    hideMenu() {
      if (this.$root.movingParentId) {
        return;
      }
      this.$root.hoveredSection = null;
      this.$root.hoveredApplication = null;
      this.$root.hoveredParentId = null;
      this.menu = false;
    },
    dragEnd() {
      if (this.isDynamicSection) {
        this.$emit('move-end');
      }
    },
    dragStart(event) {
      if (event.button !== 0) {
        return;
      }
      if (!this.isDynamicSection) {
        event.preventDefault();
        event.stopPropagation();
      }
      this.$emit('move-start', event, 'drag', this.container);
    },
  },
};
</script>
