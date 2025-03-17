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
  <div
    v-show="displayBorder"
    :class="moving && 'layout-section-moving' || 'layout-section-hover'"
    class="layout-no-multi-select border-radius">
    <v-slide-y-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-if="open"
        class="position-relative full-width full-height d-flex flex-column z-index-two">
      </div>
    </v-slide-y-transition>
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    hover: {
      type: Boolean,
      default: false,
    },
    moving: {
      type: Boolean,
      default: false,
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
    open: false,
  }),
  computed: {
    displayBorder() {
      return this.open || this.hover;
    },
  },
  watch: {
    hover() {
      window.setTimeout(() => {
        if (!this.moving) {
          this.open = this.hover;
        }
      }, 200);
    },
    moving() {
      window.setTimeout(() => {
        if (!this.hover) {
          this.open = false;
        }
      }, 200);
    },
    open(newVal, oldVal) {
      if (!oldVal && newVal) {
        this.$root.hoveredSectionId = this.container.storageId;
      } else if (!newVal && this.$root.hoveredSectionId === this.container.storageId) {
        this.$root.hoveredSectionId = null;
      }
    },
  },
};
</script>
