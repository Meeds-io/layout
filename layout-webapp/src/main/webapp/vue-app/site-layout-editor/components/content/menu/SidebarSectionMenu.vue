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
  <div
    v-show="open"
    class="layout-section-hover">
    <v-fade-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <v-hover v-model="hoverButton">
        <div class="position-absolute t-10 r-0 me-2 z-index-two">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <div
                v-bind="attrs"
                v-on="on">
                <v-btn
                  class="white text-color border-color elevation-2"
                  height="32"
                  icon
                  width="32"
                  @click="$root.$emit('layout-site-sidebar-section-open', cellContainer, container.storageId)">
                  <v-icon class="icon-default-color" size="20">fa-edit</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.editSection') }}
          </v-tooltip>
        </div>
      </v-hover>
    </v-fade-transition>
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
      hoverButton: false,
    }),
    computed: {
      cellContainer () {
        return this.container?.children?.[0];
      },
    },
    watch: {
      hover () {
        window.setTimeout(() => {
          if (!this.moving) {
            this.open = this.hover;
          }
        }, 200);
      },
      hoverButton () {
        this.$emit('hover-button', this.hoverButton);
      },
      moving () {
        window.setTimeout(() => {
          if (!this.hover) {
            this.open = false;
          }
        }, 200);
      },
      open (newVal, oldVal) {
        if (!oldVal && newVal) {
          this.$root.hoveredSectionId = this.container.storageId;
        } else if (!newVal && this.$root.hoveredSectionId === this.container.storageId) {
          this.$root.hoveredSectionId = null;
        }
      },
    },
  };
</script>
