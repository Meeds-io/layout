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
  <div v-show="rows && cols">
    <div class="pb-2 text-header">{{ $t('layout.chooseSectionDisplay') }}</div>
    <div class="d-flex align-center my-2">
      <div class="flex-grow-0 flex-shrink-0 align-start pt-1">
        <span class="subtitle-1 text-color">{{ $t('layout.row') }}</span>
      </div>
      <v-card
        class="flex-grow-1 flex-shrink-1 align-end ms-auto mt-4"
        max-width="80%"
        flat>
        <v-slider
          v-model="rows"
          :thumb-size="24"
          :min="1"
          :max="12"
          thumb-label="always">
          <template #prepend>
            <v-btn
              :disabled="rows === 1"
              class="me-n2 mt-n1"
              icon
              fab
              x-small
              @click="rows--">
              <v-icon class="icon-default-color pt-2px">fa-minus</v-icon>
            </v-btn>
          </template>
          <template #append>
            <v-btn
              :disabled="rows === 12"
              class="ms-n2 mt-n1"
              icon
              fab
              x-small
              @click="rows++">
              <v-icon class="icon-default-color pt-2px">fa-plus</v-icon>
            </v-btn>
          </template>
        </v-slider>
      </v-card>
    </div>
    <div class="d-flex align-center my-2">
      <div class="flex-grow-0 flex-shrink-0 align-start pb-3">
        <span class="subtitle-1 text-color">{{ $t('layout.column') }}</span>
      </div>
      <v-card
        class="flex-grow-1 flex-shrink-1 align-end ms-auto"
        max-width="80%"
        flat>
        <v-slider
          v-model="cols"
          :thumb-size="24"
          :min="1"
          :max="12"
          thumb-label="always">
          <template #prepend>
            <v-btn
              :disabled="cols === 1"
              class="me-n2 mt-n1"
              icon
              fab
              x-small
              @click="cols--">
              <v-icon class="icon-default-color pt-2px">fa-minus</v-icon>
            </v-btn>
          </template>
          <template #append>
            <v-btn
              :disabled="cols === 12"
              class="ms-n2 mt-n1"
              fab
              icon
              x-small
              @click="cols++">
              <v-icon class="icon-default-color pt-2px">fa-plus</v-icon>
            </v-btn>
          </template>
        </v-slider>
      </v-card>
    </div>
    <div
      :style="cssStyle"
      class="border-color-thin-grey-opacity2 border-radius mt-2 mb-4 pa-2">
      <div
        v-if="backgroundLayerStyle"
        class="layout-background-layer"
        :style="backgroundLayerStyle"></div>
      <div
        :class="gridClass"
        class="grid-gap-1">
        <div
          v-for="i in length"
          :key="i"
          :id="`grid-cell-${i}`"
          class="grey-background aspect-ratio-1 grid-cell grid-cell-colspan-lg-1 grid-cell-lg-rowspan-1 opacity-5"></div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    rowsCount: {
      type: Number,
      default: null,
    },
    colsCount: {
      type: Number,
      default: null,
    },
    backgroundProperties: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    rows: 0,
    cols: 0,
  }),
  computed: {
    gridClass() {
      return `d-md-grid pb-0 grid-cols-md-${this.cols} grid-rows-md-${this.rows}`;
    },
    cssStyle() {
      return this.backgroundProperties && this.$applicationUtils.getStyle(this.backgroundProperties, {
        onlyBackgroundStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.backgroundProperties && this.$applicationUtils.getBackgroundLayerStyle(this.backgroundProperties, {});
    },
    length() {
      return this.rows * this.cols;
    },
  },
  watch: {
    rows() {
      this.$emit('rows-updated', this.rows);
    },
    cols() {
      this.$emit('cols-updated', this.cols);
    },
  },
  created() {
    this.rows = this.rowsCount;
    this.cols = this.colsCount;
  },
};
</script>