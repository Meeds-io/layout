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
  <component
    v-bind="!dynamicSection && {
      width: iconSize,
      height: iconSize,
      icon: true,
    }"
    :is="dynamicSection && 'div' || 'v-btn'"
    v-if="show"
    ref="resizeButton"
    class="position-absolute z-index-two"
    :class="[{
      'l-0': $vuetify.rtl,
      'r-0': !$vuetify.rtl,
      'b-0 mb-n2 me-n2': !dynamicSection,
      'absolute-vertical-center full-height': dynamicSection,
      'fa-rotate-90': !dynamicSection && !$vuetify.rtl,
      'col-resize-cursor grid-gap-width': dynamicSection,
      'layout-column-resize': dynamicSection && hoverSeparator,
    }, spacingClass]"
    :title="dynamicSection && $t('layout.updateWidth') || $t('layout.resizeCell')"
    @focusin="hoverSeparator = true"
    @focusout="hoverSeparator = false"
    @mousedown.prevent.stop="resizeStart"
    @mouseout="hoverSeparator = false"
    @mouseover="hoverSeparator = true">
    <v-icon
      v-if="!dynamicSection"
      class="icon-default-color"
      :size="iconSize">
      {{ dynamicSection && 'fa-arrows-alt-h' || 'fa-expand-alt' }}
    </v-icon>
  </component>
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
      dynamicSection: {
        type: Boolean,
        default: false,
      },
      hover: {
        type: Boolean,
        default: null,
      },
      moving: {
        type: Boolean,
        default: null,
      },
      spacingClass: {
        type: String,
        default: () => 'me-n5',
      },
    },
    data: () => ({
      iconSize: 20,
      hoverSeparator: false,
    }),
    computed: {
      sectionHovered () {
        return this.$root.hoveredSectionId === this.parentId;
      },
      show () {
        return ((!this.dynamicSection && this.hover) || (this.dynamicSection && this.sectionHovered)) && !this.moving && !this.$root.drawerOpened;
      },
    },
    methods: {
      resizeStart (event) {
        this.$emit('move-start', event, 'resize');
      },
    },
  };
</script>
