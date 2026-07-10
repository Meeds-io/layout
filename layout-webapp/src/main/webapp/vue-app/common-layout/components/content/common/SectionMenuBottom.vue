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
    <div>
      <v-slide-y-transition>
        <div
          v-if="display || hoverButton"
          :class="lastSection && 'mb-n2' || 'mb-n4'"
          class="absolute-horizontal-center b-0 d-flex justify-center z-index-two">
          <v-tooltip top>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-btn
                  v-if="!$root.noSectionAdd"
                  class="white text-color border-color elevation-2"
                  height="32"
                  width="32"
                  icon
                  @click="$root.$emit('layout-add-section-drawer', index + 1)">
                  <v-icon class="icon-default-color" size="20">fa-plus</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.addSectionAfter') }}
          </v-tooltip>
        </div>
      </v-slide-y-transition>
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
    lastSection() {
      return this.index === this.length -1;
    },
    hoverButton: {
      get () {
        return this.value;
      },
      set (value) {
        this.$emit('input', value);
      },
    },
  }
};
</script>