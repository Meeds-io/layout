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
      :style="rightButtonStyle"
      class="position-absolute t-10 full-height justify-center z-index-two">
      <div class="position-relative full-height">
        <div class="position-sticky t-20 z-index-one">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-btn
                  class="white text-color border-color elevation-2"
                  height="32"
                  width="32"
                  icon
                  @click="$root.$emit('layout-edit-section-drawer', index, length)">
                  <v-icon class="icon-default-color" size="20">fa-edit</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.editSection') }}
          </v-tooltip>
          <v-tooltip v-if="$root.isAdministrator" bottom>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-btn
                  :aria-label="$t('layout.cloneSection')"
                  class="white text-color border-color elevation-2 mt-2"
                  height="32"
                  width="32"
                  icon
                  @click="$root.$emit('layout-section-clone', container, index)">
                  <v-icon class="icon-default-color" size="20">fa-copy</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.cloneSection') }}
          </v-tooltip>
          <v-tooltip v-if="$root.isAdministrator" bottom>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-btn
                  :loading="savingAsTemplate"
                  class="white text-color border-color elevation-2 mt-2"
                  height="32"
                  width="32"
                  icon
                  @click="saveAsTemplate">
                  <v-icon class="icon-default-color" size="20">fa-columns</v-icon>
                </v-btn>
              </div>
            </template>
            {{ $t('layout.saveAsSectionTemplate') }}
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
    display: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    savingAsTemplate: false,
  }),
  computed: {
    hoverButton: {
      get () {
        return this.value;
      },
      set (value) {
        this.$emit('input', value);
      },
    },
    rightButtonStyle() {
      return {
        right: 'calc(50% - min(50%, var(--allPagesWidth, 1320px) / 2) - 16px)',
      };
    },
  },
  methods: {
    async saveAsTemplate() {
      this.savingAsTemplate = true;
      await this.$nextTick();
      window.setTimeout(() => {
        this.savingAsTemplate = false;
        this.open = false;
        try {
          this.$root.$emit('layout-section-save-as-template', this.container);
        } finally {
          window.setTimeout(() => {
            this.savingAsTemplate = false;
          }, 2000);
        }
      }, 200);
    },
  },
};
</script>