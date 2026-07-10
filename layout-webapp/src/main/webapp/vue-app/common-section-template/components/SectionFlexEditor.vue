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
  <div v-show="cols">
    <div class="pb-2 text-header">{{ $t('layout.chooseSectionDisplay') }}</div>
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
              @click="decrementCols">
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
              @click="incrementCols">
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
        <v-card
          v-for="i in cols"
          :key="i"
          :id="`grid-cell-${i}`"
          height="100px"
          class="grey-background grid-cell grid-cell-colspan-lg-1 grid-cell-lg-rowspan-1 opacity-5"
          flat />
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
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
    cols: 0,
    allowedCols: [1, 2, 3, 4, 6, 12],
  }),
  computed: {
    gridClass() {
      return `d-md-grid pb-0 grid-cols-md-${this.cols}`;
    },
    cssStyle() {
      return this.backgroundProperties && this.$applicationUtils.getStyle(this.backgroundProperties, {
        onlyBackgroundStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.backgroundProperties && this.$applicationUtils.getBackgroundLayerStyle(this.backgroundProperties, {});
    },
  },
  watch: {
    cols(newVal, oldVal) {
      const index = this.allowedCols.indexOf(this.cols);
      if (index < 0) {
        this.cols = oldVal;
        if (newVal - oldVal > 0) {
          this.incrementCols();
        } else {
          this.decrementCols();
        }
      } else {
        this.$emit('cols-updated', this.cols);
      }
    },
  },
  created() {
    this.cols = this.colsCount;
  },
  methods: {
    decrementCols() {
      const index = this.allowedCols.indexOf(this.cols);
      this.cols = this.allowedCols[index - 1];
    },
    incrementCols() {
      const index = this.allowedCols.indexOf(this.cols);
      this.cols = this.allowedCols[index + 1];
    },
  },
};
</script>