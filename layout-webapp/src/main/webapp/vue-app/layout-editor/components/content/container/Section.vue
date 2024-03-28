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
  <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
  <div
    ref="section"
    class="position-relative layout-section pb-5"
    @mousedown="startSelection">
    <layout-editor-container-base
      ref="container"
      :container="container"
      :index="index"
      :class="zIndexClass"
      class="position-relative overflow-initial"
      type="section"
      no-draggable
      @hovered="hoverSection = $event && !drawerOpened">
      <template v-if="$root.movingParentId === storageId" #footer>
        <layout-editor-section-selection-grid
          :section="container"
          class="position-absolute z-index-two full-width full-height" />
        <layout-editor-cells-drop-box
          :section="container"
          @hide="$root.movingParentId = null" />
      </template>
    </layout-editor-container-base>
    <v-hover :disabled="$root.mobileDisplayMode">
      <div
        slot-scope="{ hover }"
        class="layout-section-border">
        <div class="position-relative full-height full-width">
          <layout-editor-section-border-menu
            :hover="!drawerOpened && (hover || hoverSection || movingSection)"
            :index="index"
            :length="length"
            :moving="movingSection"
            @move-start="movingSection = true" />
        </div>
      </div>
    </v-hover>
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    index: {
      type: Number,
      default: null,
    },
    length: {
      type: Number,
      default: null,
    },
  },
  data: () => ({
    hoverSection: false,
    movingSection: false,
    sectionWidth: 0,
  }),
  computed: {
    storageId() {
      return this.container?.storageId;
    },
    zIndexClass() {
      return !this.drawerOpened && 'z-index-one';
    },
    drawerOpened() {
      return this.$root.drawerOpened;
    },
  },
  created() {
    window.addEventListener('resize', this.refreshDimensions);
    document.querySelector('.page-scroll-content').addEventListener('scroll', this.refreshDimensions);
  },
  mounted() {
    this.refreshDimensions();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.refreshDimensions);
    document.querySelector('.page-scroll-content').removeEventListener('scroll', this.refreshDimensions);
  },
  methods: {
    refreshDimensions() {
      window.setTimeout(() => {
        if (!this.$refs.section) {
          return;
        }
        const dimensions = this.$refs.section.getBoundingClientRect();
        this.sectionWidth = dimensions.width;
      }, 300);
    },
    startSelection(event) {
      if (event.button !== 0) {
        return;
      }
      if (!event?.target?.closest?.('.layout-no-multi-select')
          && event?.target?.tagName !== 'BUTTON'
          && event?.target?.tagName !== 'A') {
        this.$root.$emit('layout-section-selection-start', event, this.container, this.$refs.container.$el);
      }
    },
  },
};
</script>