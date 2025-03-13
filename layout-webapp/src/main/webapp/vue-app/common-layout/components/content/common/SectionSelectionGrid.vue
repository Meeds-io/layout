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
  <div class="border-box-sizing" :class="gridClass">
    <layout-editor-section-selection-grid-cell
      v-for="i in length"
      :id="`grid-cell-${i}`"
      :key="i"
      :cells-validity="cellsValidity"
      :cols="cols"
      :index="i" />
  </div>
</template>
<script>
  export default {
    props: {
      section: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      cellsValidity: {},
    }),
    computed: {
      isMove () {
        return this.$root.isMove;
      },
      rows () {
        return this.section.rowsCount;
      },
      cols () {
        return this.section.colsCount;
      },
      gridClass () {
        return `d-md-grid grid-cols-md-${this.cols} grid-rows-md-${this.rows}`;
      },
      length () {
        return this.rows * this.cols;
      },
    },
    watch: {
      isMove () {
        this.computeCellsValidity();
      },
    },
    created () {
      this.computeCellsValidity();
    },
    methods: {
      computeCellsValidity () {
        if (this.isMove) {
          window.setTimeout(() => {
            const cellsValidity = {};
            for (let i = 0; i < this.section.rowsCount; i++) {
              cellsValidity[i] = {};
              for (let j = 0; j < this.section.colsCount; j++) {
                cellsValidity[i][j] = eXo.$layoutUtils.isValidTargetMovingCell(this.section, this.$root.movingCell, i, j);
              }
            }
            this.cellsValidity = cellsValidity;
          }, 50);
        } else {
          this.cellsValidity = {};
        }
      },
    },
  };
</script>