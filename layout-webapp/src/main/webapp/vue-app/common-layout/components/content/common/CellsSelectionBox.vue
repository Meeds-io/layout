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
    v-show="multiCellsSelect"
    class="layout-selecting-container border-box-sizing grey opacity-5 elevation-2 position-absolute z-index-modal"
    :style="boxStyle">
  </div>
</template>
<script>
  export default {
    data: () => ({
      interceptEvents: false,
      movingStartX: 0,
      movingStartY: 0,
      movingX: false,
      movingY: false,
      startRowIndex: -1,
      startColIndex: -1,
      endRowIndex: -1,
      endColIndex: -1,
      computingDisplayInterval: null,
      section: null,
      sectionElement: null,
      sectionX: 0,
      sectionY: 0,
      sectionWidth: 0,
      sectionHeight: 0,
      sectionDimensions: null,
    }),
    computed: {
      multiCellsSelect () {
        return this.$root.multiCellsSelect;
      },
      parentAppX () {
        return this.$root.parentAppX;
      },
      parentAppY () {
        return this.$root.parentAppY;
      },
      innerCellWidth () {
        return this.section && this.sectionWidth && (this.sectionWidth - (this.$root.gap * (this.section.colsCount - 1))) / this.section.colsCount || 0;
      },
      innerCellHeight () {
        return this.section && this.sectionHeight && (this.sectionHeight - (this.$root.gap * (this.section.rowsCount - 1))) / this.section.rowsCount || 0;
      },
      boxHeight () {
        return this.movingY - this.movingStartY;
      },
      boxWidth () {
        return this.movingX - this.movingStartX;
      },
      boxStyle () {
        const boxStyle = {};
        if (this.boxHeight > 0) {
          boxStyle.top = `${this.movingStartY}px`;
        } else {
          boxStyle.top = `${this.movingStartY + this.boxHeight}px`;
        }
        if (this.boxWidth > 0) {
          boxStyle.left = `${this.movingStartX}px`;
        } else {
          boxStyle.left = `${this.movingStartX + this.boxWidth}px`;
        }
        boxStyle.height = `${Math.abs(this.boxHeight)}px`;
        boxStyle.width = `${Math.abs(this.boxWidth)}px`;
        return boxStyle;
      },
    },
    watch: {
      interceptEvents (val) {
        if (val) {
          document.addEventListener('mousemove', this.updateSelection);
          document.addEventListener('mouseup', this.endSelection);
          document.querySelector('.site-scroll-parent').addEventListener('scroll', this.updateScrollPosition);
        } else {
          document.removeEventListener('mousemove', this.updateSelection);
          document.removeEventListener('mouseup', this.endSelection);
          document.querySelector('.site-scroll-parent').removeEventListener('scroll', this.updateScrollPosition);
          this.reset();
        }
      },
      movingX () {
        this.updateDisplay();
      },
      movingY () {
        this.updateDisplay();
      },
      parentAppX (newVal, oldVal) {
        if (this.interceptEvents) {
          this.movingX = this.movingX - newVal + oldVal;
        }
      },
      parentAppY (newVal, oldVal) {
        if (this.interceptEvents) {
          this.movingY = this.movingY - newVal + oldVal;
        }
      },
    },
    created () {
      this.$root.$on('layout-section-selection-start', this.startSelection);
    },
    beforeUnmount () {
      this.$root.$off('layout-section-selection-start', this.startSelection);
    },
    methods: {
      reset () {
        this.$root.multiCellsSelect = false;
      },
      startSelection (event, section, sectionElement) {
        this.$root.initCellsSelection();
        this.$root.resetMoving();
        this.$root.initScrollPosition();

        this.$root.moveType = 'multiSelect';
        this.$root.selectedSectionId = section.storageId;
        this.section = section;
        this.sectionElement = sectionElement;

        this.updateSectionDimensions();
        this.sectionX = this.sectionDimensions.x;
        this.sectionWidth = this.sectionDimensions.width;
        this.sectionY = this.sectionDimensions.y;
        this.sectionHeight = this.sectionDimensions.height;

        this.movingX = event.x - this.$root.parentAppDimensions.x;
        this.movingY = event.y - this.$root.parentAppDimensions.y;
        this.movingStartX = this.movingX;
        this.movingStartY = this.movingY;

        this.startRowIndex = parseInt((this.movingStartY - this.sectionY + this.$root.startScrollY) / (this.innerCellHeight + this.$root.gap));
        this.startColIndex = parseInt((this.movingStartX - this.sectionX + this.$root.startScrollX) / (this.innerCellWidth + this.$root.gap));
        this.endRowIndex = this.startRowIndex;
        this.endColIndex = this.startColIndex;

        this.interceptEvents = false;
        this.$nextTick().then(() => this.interceptEvents = true);
      },
      updateDisplay () {
        if (this.interceptEvents) {
          if (!this.computingDisplayInterval && !this.$root.multiCellsSelect) {
            this.computingDisplayInterval = window.setTimeout(() => {
              this.$root.multiCellsSelect =
                Math.abs(this.movingX - this.movingStartX) > 10
                || Math.abs(this.movingY - this.movingStartY) > 10;
              this.computingDisplayInterval = null;
            }, 50);
          }
        } else {
          this.$root.multiCellsSelect = false;
        }
      },
      updateScrollPosition () {
        if (this.interceptEvents) {
          this.$root.updateScrollPosition();
          this.$nextTick(() => this.updateSelectedCellCoordinates());
        }
      },
      updateSectionDimensions () {
        this.sectionDimensions = this.sectionElement.getBoundingClientRect();
      },
      updateSelection (event) {
        if (this.interceptEvents && event) {
          this.movingX = event.x - this.$root.parentAppDimensions.x;
          this.movingY = event.y - this.$root.parentAppDimensions.y;
          this.updateSelectedCellCoordinates();
        }
      },
      updateSelectedCellCoordinates () {
        const endRowIndex = Math.min(
          parseInt((this.movingY - this.sectionY + this.$root.startScrollY - this.$root.gap) / (this.innerCellHeight + this.$root.gap)),
          this.section?.rowsCount || 12 - 1
        );

        const endColIndex = Math.min(
          parseInt((this.movingX - this.sectionX + this.$root.startScrollX) / (this.innerCellWidth + this.$root.gap)),
          this.section?.colsCount || 12 - 1
        );
        if (this.endRowIndex === endRowIndex
          && this.endColIndex === endColIndex) {
          return;
        }
        this.endRowIndex = endRowIndex;
        this.endColIndex = endColIndex;

        const fromRow = Math.min(this.startRowIndex, this.endRowIndex);
        const toRow = Math.max(this.startRowIndex, this.endRowIndex);
        const fromCol = Math.min(this.startColIndex, this.endColIndex);
        const toCol = Math.max(this.startColIndex, this.endColIndex);

        const selectedCellCoordinates = [];
        for (let i = fromRow; i <= toRow; i++) {
          for (let j = fromCol; j <= toCol; j++) {
            selectedCellCoordinates.push({
              rowIndex: i,
              colIndex: j,
            });
          }
        }
        this.$root.selectedCellCoordinates = selectedCellCoordinates;
      },
      endSelection () {
        if (this.interceptEvents) {
          if (this.multiCellsSelect) {
            this.$root.$emit('layout-cells-select', {
              fromRowIndex: Math.min(this.startRowIndex, this.endRowIndex),
              toRowIndex: Math.max(this.startRowIndex, this.endRowIndex),
              fromColIndex: Math.min(this.startColIndex, this.endColIndex),
              toColIndex: Math.max(this.startColIndex, this.endColIndex),
            });
          }
          this.interceptEvents = false;
        }
      },
    },
  };
</script>