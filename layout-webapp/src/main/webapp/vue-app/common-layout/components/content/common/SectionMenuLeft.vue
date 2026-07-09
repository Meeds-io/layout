
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
  <v-hover v-model="hoverButton">
    <div
      v-show="display || hoverButton"
      :style="leftButtonStyle"
      class="position-absolute t-10 full-height justify-center z-index-two">
      <div class="position-relative full-height">
        <div class="position-sticky t-20 z-index-one">
          <v-tooltip :disabled="moving" bottom>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-btn
                  v-if="!$root.noSectionAdd"
                  class="white text-color border-color elevation-2 draggable"
                  height="32"
                  width="32"
                  icon
                  @mousedown="$emit('move-start')"
                  @mouseup="$emit('move-end')"
                  @mouseout="$emit('move-end')"
                  @focusout="$emit('move-end')">
                  <v-icon class="icon-default-color" size="20">fa-arrows-alt</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.moveSection') }}
          </v-tooltip>
        </div>
      </div>
    </div>
  </v-hover>
</template>
<script>
export default {
  props: {
    value: {
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
    display: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    hoverButton: {
      get () {
        return this.value;
      },
      set (value) {
        this.$emit('input', value);
      },
    },
    leftButtonStyle() {
      return {
        left: 'calc(50% - min(50%, var(--allPagesWidth, 1320px) / 2) - 16px)',
      };
    },
  },
};
</script>