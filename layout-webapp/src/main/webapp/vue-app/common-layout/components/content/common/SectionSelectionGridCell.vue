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
  <div>
    <div
      class="full-width full-height"
      :class="{
        'border-grey-dashed': !$root.isMove || valid,
        'border-error-dashed-1': $root.isMove && !valid && !firstCell,
        'border-error-dashed-2': $root.isMove && !valid && firstCell,
      }">
      <div
        class="full-width full-height"
        :class="{
          'grey lighten-2': selected && (!firstCell || !$root.isMove),
          'success-color-background opacity-5': $root.isMove && selected && firstCell && valid,
          'error-color-background opacity-5': $root.isMove && selected && firstCell && !valid,
        }">
      </div>
    </div>
  </div>
</template>
<script>
  export default {
    props: {
      cellsValidity: {
        type: Object,
        default: null,
      },
      cols: {
        type: Number,
        default: null,
      },
      index: {
        type: Number,
        default: null,
      },
    },
    computed: {
      rowIndex () {
        return this.cols && parseInt((this.index - 1) / this.cols);
      },
      colIndex () {
        return this.cols && (this.index - 1) % this.cols;
      },
      selected () {
        return !!this.$root.selectedCellCoordinates.find(c => c.rowIndex === this.rowIndex && c.colIndex === this.colIndex);
      },
      valid () {
        return this.cellsValidity?.[this.rowIndex]?.[this.colIndex] !== false;
      },
      firstCell () {
        return this.rowIndex === this.$root.selectedFirstRowIndex
          && this.colIndex === this.$root.selectedFirstColIndex;
      },
    },
  };
</script>