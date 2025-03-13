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
    v-show="displayBorder"
    :class="moving && 'layout-section-moving' || 'layout-section-hover'">
    <v-fade-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-show="open"
        @focusin="hoverArea = true"
        @focusout="hoverArea = false"
        @mouseout="hoverArea = false"
        @mouseover="hoverArea = true">
        <v-hover v-if="index > 0" v-model="hoverButton1">
          <div
            class="absolute-horizontal-center d-flex t-0 justify-center mb-auto mt-n4"
            :class="{
              'z-index-floating-button': !hoveredApplication,
            }">
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
                    @click="addSectionBefore">
                    <v-icon class="icon-default-color" size="20">fa-plus</v-icon>
                  </v-btn>
                </div>
              </template>
              {{ $t('layout.addSectionBefore') }}
            </v-tooltip>
          </div>
        </v-hover>
      </div>
    </v-fade-transition>
    <v-fade-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-show="open"
        @focusin="hoverArea = true"
        @focusout="hoverArea = false"
        @mouseout="hoverArea = false"
        @mouseover="hoverArea = true">
        <v-hover
          v-if="displayMoveButton"
          v-model="hoverButton2">
          <div
            class="position-absolute t-10 z-index-two"
            :class="{
              'r-0': $vuetify.rtl,
              'l-0': !$vuetify.rtl,
            }"
            :style="leftButtonStyle">
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
        </v-hover>
      </div>
    </v-fade-transition>
    <v-fade-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-show="open"
        @focusin="hoverArea = true"
        @focusout="hoverArea = false"
        @mouseout="hoverArea = false"
        @mouseover="hoverArea = true">
        <v-hover v-model="hoverButton3">
          <div
            class="position-absolute t-10 z-index-two"
            :class="{
              'l-0': $vuetify.rtl,
              'r-0': !$vuetify.rtl,
            }">
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
                    @click="$root.$emit('layout-site-banner-section-open', container)">
                    <v-icon class="icon-default-color" size="20">fa-edit</v-icon>
                  </v-btn>
                </div>
              </template>
              {{ $t('layout.editSection') }}
            </v-tooltip>
          </div>
        </v-hover>
      </div>
    </v-fade-transition>
    <v-fade-transition>
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <div
        v-show="open"
        @focusin="hoverArea = true"
        @focusout="hoverArea = false"
        @mouseout="hoverArea = false"
        @mouseover="hoverArea = true">
        <v-hover v-if="index < (length - 1)" v-model="hoverButton4">
          <div class="absolute-horizontal-center b-0 z-index-two d-flex justify-center mb-n4">
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
                    @click="addSectionAfter">
                    <v-icon class="icon-default-color" size="20">fa-plus</v-icon>
                  </v-btn>
                </div>
              </template>
              {{ $t('layout.addSectionAfter') }}
            </v-tooltip>
          </div>
        </v-hover>
      </div>
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
        return this.open || this.hover || this.hoverButton;
      },
      hoveredSectionMenu () {
        return this.hoverButton || (!this.hoverArea && !this.hoveredApplication);
      },
      hoveredApplication () {
        return !!this.$root.hoveredApplication;
      },
      rightButtonStyle () {
        return {
          right: this.$root.pageFullWindow && '0' || '-20px',
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
      addSectionAfter () {
        this.addSection(this.index + 1);
      },
      addSectionBefore () {
        this.addSection(this.index);
      },
      addSection (index) {
        this.$root.$emit('layout-section-history-add');
        this.$root.middleContainer.children.splice(index, 0, {
          ...eXo.$layoutUtils.newContainer(eXo.$layoutUtils.bannerTemplate),
          children: [
            eXo.$layoutUtils.newContainer(eXo.$layoutUtils.bannerCellTemplate),
          ],
        });
      },
    },
  };
</script>
