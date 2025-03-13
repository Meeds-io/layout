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
    class="absolute-full-size layout-no-multi-select border-radius"
    :class="moving && 'layout-section-moving' || 'layout-section-hover'">
    <v-slide-y-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-if="open"
        class="position-relative full-width full-height d-flex flex-column"
        @focusin="hoverArea = true"
        @focusout="hoverArea = false"
        @mouseout="hoverArea = false"
        @mouseover="hoverArea = true">
        <v-hover v-model="hoverButton1">
          <div class="position-sticky d-flex justify-center mb-auto mt-n4" :class="!hoveredApplication && 'z-index-two'">
            <v-tooltip bottom>
              <template #activator="{on, attrs}">
                <div
                  v-bind="attrs"
                  v-on="on">
                  <v-btn
                    v-if="!$root.noSectionAdd"
                    class="white text-color border-color elevation-2"
                    height="32"
                    icon
                    width="32"
                    @click="$root.$emit('layout-add-section-drawer', index)">
                    <v-icon class="icon-default-color" size="20">fa-plus</v-icon>
                  </v-btn>
                </div>
              </template>
              {{ $t('layout.addSectionBefore') }}
            </v-tooltip>
          </div>
        </v-hover>
        <v-hover
          v-if="displayMoveButton"
          v-model="hoverButton2">
          <div
            class="position-absolute t-10 z-index-one full-height"
            :style="leftButtonStyle">
            <div class="position-sticky t-20 z-index-one">
              <v-tooltip bottom :disabled="moving">
                <template #activator="{on, attrs}">
                  <div
                    v-bind="attrs"
                    v-on="on">
                    <v-btn
                      v-if="!$root.noSectionAdd"
                      class="white text-color border-color elevation-2 draggable"
                      height="32"
                      icon
                      width="32"
                      @focusout="$emit('move-end')"
                      @mousedown="$emit('move-start')"
                      @mouseout="$emit('move-end')"
                      @mouseup="$emit('move-end')">
                      <v-icon class="icon-default-color" size="20">fa-arrows-alt</v-icon>
                    </v-btn>
                  </div>
                </template>
                {{ $t('layout.moveSection') }}
              </v-tooltip>
            </div>
          </div>
        </v-hover>
        <v-hover v-model="hoverButton3">
          <div
            class="position-absolute t-10 z-index-one full-height"
            :style="rightButtonStyle">
            <div class="position-sticky t-20 z-index-one">
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
                    v-bind="attrs"
                    v-on="on">
                    <v-btn
                      :aria-label="$t('layout.cloneSection')"
                      class="white text-color border-color elevation-2 mt-2"
                      height="32"
                      icon
                      width="32"
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
                    v-bind="attrs"
                    v-on="on">
                    <v-btn
                      class="white text-color border-color elevation-2 mt-2"
                      height="32"
                      icon
                      :loading="savingAsTemplate"
                      width="32"
                      @click="saveAsTemplate">
                      <v-icon class="icon-default-color" size="20">fa-columns</v-icon>
                    </v-btn>
                  </div>
                </template>
                {{ $t('layout.saveAsSectionTemplate') }}
              </v-tooltip>
            </div>
          </div>
        </v-hover>
        <v-hover v-model="hoverButton4">
          <div class="position-sticky z-index-two d-flex justify-center mb-n4 mt-auto">
            <v-tooltip top>
              <template #activator="{on, attrs}">
                <div
                  v-bind="attrs"
                  v-on="on">
                  <v-btn
                    v-if="!$root.noSectionAdd"
                    class="white text-color border-color elevation-2"
                    height="32"
                    icon
                    width="32"
                    @click="$root.$emit('layout-add-section-drawer', index + 1)">
                    <v-icon class="icon-default-color" size="20">fa-plus</v-icon>
                  </v-btn>
                </div>
              </template>
              {{ $t('layout.addSectionAfter') }}
            </v-tooltip>
          </div>
        </v-hover>
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
      savingAsTemplate: false,
      hoverButton1: false,
      hoverButton2: false,
      hoverButton3: false,
      hoverButton4: false,
      hoverArea: false,
    }),
    computed: {
      hoverButton () {
        return this.hoverButton1 || this.hoverButton2 || this.hoverButton3 || this.hoverButton4;
      },
      displayMoveButton () {
        return this.length > 1;
      },
      displayBorder () {
        return this.open || this.hover;
      },
      hoveredSectionMenu () {
        return this.hoverButton || (!this.hoverArea && !this.hoveredApplication);
      },
      hoveredApplication () {
        return this.$root.hoveredApplication;
      },
      leftButtonStyle () {
        return {
          left: '-20px',
        };
      },
      rightButtonStyle () {
        return {
          right: '-20px',
        };
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
      hoveredSectionMenu () {
        this.$emit('hover-button', this.hoveredSectionMenu);
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
    methods: {
      async saveAsTemplate () {
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
