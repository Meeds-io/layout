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
    :style="cssStyle"
    :class="mobileInColumns && 'layout-section-mobile-pages' || ''"
    class="position-relative layout-section"
    v-on="!isDynamicSection && {
      'mousedown': startSelection,
    }">
    <div
      v-if="backgroundLayerStyle"
      class="layout-background-layer"
      :style="backgroundLayerStyle"></div>
    <layout-editor-section-menu-top
      v-model="hoverButton1"
      :display="hoverSectionMenu"
      :index="index"
      :length="length" />
    <layout-editor-section-menu-left
      v-model="hoverButton2"
      :display="hoverSectionMenu && displayMoveButton"
      :index="index"
      :length="length"
      @mousedown="$emit('move-start')"
      @mouseup="$emit('move-end')"
      @mouseout="$emit('move-end')"
      @focusout="$emit('move-end')" />
    <v-hover v-model="hover" :disabled="$root.mobileDisplayMode">
      <layout-editor-section-menu
        :container="container"
        :hover="hoverSectionMenu"
        :index="index"
        :length="length"
        :moving="movingSection"
        :class="{
          'normal-page-width': !$root.pageFullWindow,
          'full-page-width': $root.pageFullWindow,
        }"
        class="layout-section-border" />
    </v-hover>
    <layout-editor-container-base
      ref="container"
      :container="container"
      :parent-id="parentId"
      :index="index"
      :class="`${mobileInColumns && mobileSectionColumnClass || ''}`"
      :type="isDynamicSection ? 'section-columns' : 'section-grid'"
      class="position-relative overflow-initial layout-section-content"
      no-background-style
      @hovered="hoverSection = $event && !drawerOpened">
      <template v-if="$root.movingParentId === storageId && (!isDynamicSection || $root.moveType === 'resize')" #footer>
        <layout-editor-section-selection-grid
          :section="container"
          class="position-absolute z-index-two full-width full-height px-5 layout-grid-hover-shadow" />
        <layout-editor-cells-drop-box
          :section="container"
          @hide="$root.movingParentId = null" />
      </template>
    </layout-editor-container-base>
    <layout-editor-section-menu-right
      v-model="hoverButton3"
      :display="hoverSectionMenu"
      :container="container"
      :index="index"
      :length="length" />
    <layout-editor-section-menu-bottom
      v-model="hoverButton4"
      :display="hoverSectionMenu"
      :index="index"
      :length="length" />

    <layout-section-mobile-column-menu-drawer
      v-if="mobileInColumns"
      v-model="mobileSectionColumnClass"
      :container="container" />
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    parentId: {
      type: String,
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
    hover: false,
    hoverButton1: false,
    hoverButton2: false,
    hoverButton3: false,
    hoverButton4: false,
    hoverSection: false,
    hoverSectionMenuButton: false,
    movingSection: false,
    mobileSectionColumnClass: null,
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
    isDynamicSection() {
      return this.container.template === this.$layoutUtils.flexTemplate;
    },
    hoverButton() {
      return this.hoverButton1 || this.hoverButton2 || this.hoverButton3 || this.hoverButton4;
    },
    hoverSectionMenu() {
      return !this.drawerOpened && (this.hover || this.hoverButton || this.hoverSection || this.movingSection);
    },
    cssStyle() {
      return this.$applicationUtils.getStyle(this.container, {
        onlyBackgroundStyle: true,
        sectionStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.$applicationUtils.getBackgroundLayerStyle(this.container, {});
    },
    mobileInColumns() {
      return this.isDynamicSection
        && this.$root.mobileDisplayMode
        && this.container?.cssClass?.includes?.('layout-mobile-columns');
    },
    displayMoveButton() {
      return this.length > 1;
    },
  },
  watch: {
    hoverSectionMenu(newVal, oldVal) {
      if (!oldVal && newVal) {
        this.hoverSectionMenuButton = true;
      }
    },
  },
  created() {
    window.addEventListener('resize', this.refreshDimensions);
    document.querySelector('.site-scroll-parent').addEventListener('scroll', this.refreshDimensions);
  },
  mounted() {
    this.refreshDimensions();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.refreshDimensions);
    document.querySelector('.site-scroll-parent').removeEventListener('scroll', this.refreshDimensions);
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