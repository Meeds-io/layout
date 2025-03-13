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
        flat
        max-width="80%">
        <v-slider
          v-model="rows"
          :max="12"
          :min="1"
          thumb-label="always"
          :thumb-size="24">
          <template #prepend>
            <v-btn
              class="me-n2 mt-n1"
              :disabled="rows === 1"
              fab
              icon
              x-small
              @click="rows--">
              <v-icon class="icon-default-color pt-2px">fa-minus</v-icon>
            </v-btn>
          </template>
          <template #append>
            <v-btn
              class="ms-n2 mt-n1"
              :disabled="rows === 12"
              fab
              icon
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
        flat
        max-width="80%">
        <v-slider
          v-model="cols"
          :max="12"
          :min="1"
          thumb-label="always"
          :thumb-size="24">
          <template #prepend>
            <v-btn
              class="me-n2 mt-n1"
              :disabled="cols === 1"
              fab
              icon
              x-small
              @click="cols--">
              <v-icon class="icon-default-color pt-2px">fa-minus</v-icon>
            </v-btn>
          </template>
          <template #append>
            <v-btn
              class="ms-n2 mt-n1"
              :disabled="cols === 12"
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
      class="border-color-thin-grey-opacity2 border-radius mt-2 mb-4 pa-2"
      :style="cssStyle">
      <div
        class="grid-gap-1"
        :class="gridClass">
        <div
          v-for="i in length"
          :id="`grid-cell-${i}`"
          :key="i"
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
      gridClass () {
        return `d-md-grid pb-0 grid-cols-md-${this.cols} grid-rows-md-${this.rows}`;
      },
      cssStyle () {
        return this.backgroundProperties && eXo.$applicationUtils.getStyle(this.backgroundProperties, {
          onlyBackgroundStyle: true,
        });
      },
      length () {
        return this.rows * this.cols;
      },
    },
    watch: {
      rows () {
        this.$emit('rows-updated', this.rows);
      },
      cols () {
        this.$emit('cols-updated', this.cols);
      },
    },
    created () {
      this.rows = this.rowsCount;
      this.cols = this.colsCount;
    },
  };
</script>