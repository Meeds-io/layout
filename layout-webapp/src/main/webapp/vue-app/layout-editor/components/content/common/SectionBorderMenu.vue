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
    class="absolute-full-size border-radius">
    <v-slide-y-transition>
      <div v-if="open" class="position-relative full-width full-height">
        <div class="absolute-horizontal-center t-0 mt-n4">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                height="32"
                width="32"
                icon
                @click="$root.$emit('layout-add-section-drawer', index)">
                <v-icon size="24">fa-plus</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.addSectionBefore') }}
          </v-tooltip>
        </div>
        <div class="absolute-horizontal-center b-0 mb-n4">
          <v-tooltip top>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                height="32"
                width="32"
                icon
                @click="$root.$emit('layout-add-section-drawer', index + 1)">
                <v-icon size="24">fa-plus</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.addSectionAfter') }}
          </v-tooltip>
        </div>
        <div class="absolute-vertical-center r-0 mr-n5">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                height="32"
                width="32"
                icon
                @click="$root.$emit('layout-edit-section-drawer', index, length)">
                <v-icon size="24">fa-edit</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.editSection') }}
          </v-tooltip>
        </div>
        <div
          v-if="displayMoveButton"
          class="absolute-vertical-center l-0 ml-n4">
          <v-tooltip :disabled="moving" bottom>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                height="32"
                width="32"
                class="draggable"
                icon
                @mousedown="$emit('move-start')">
                <v-icon size="24">fa-arrows-alt</v-icon>
              </v-btn>
            </template>
            {{ $t('layout.moveSection') }}
          </v-tooltip>
        </div>
      </div>
    </v-slide-y-transition>
  </div>
</template>
<script>
export default {
  props: {
    hover: {
      type: Boolean,
      default: false,
    },
    container: {
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
    displayMoveButton() {
      return this.length > 1;
    },
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
  },
};
</script>
