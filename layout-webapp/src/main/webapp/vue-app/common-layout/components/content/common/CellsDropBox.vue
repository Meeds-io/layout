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
    ref="movingBox"
    class="layout-selecting-container border-box-sizing d-flex position-absolute elevation-2 z-index-modal"
    :style="boxStyle">
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
      cell: null,
      cellElement: null,
      sectionElement: null,
      sectionDimensions: null,
      interceptEvents: false,
      boxHeight: null,
      boxWidth: null,
      computingSelectionInterval: null,
      sectionX: 0,
      sectionY: 0,
      sectionWidth: 0,
      sectionHeight: 0,
      cellX: 0,
      cellY: 0,
      cellWidth: 0,
      cellHeight: 0,
      movingStartX: 0,
      movingStartY: 0,
      movingX: 0,
      movingY: 0,
      diffMovingX: 0,
      diffMovingY: 0,
      startRowIndex: 0,
      startColIndex: 0,
      rowsCount: 0,
      colsCount: 0,
    }),
    computed: {
      nextCell () {
        if (this.section.template === eXo.$layoutUtils.flexTemplate) {
          const cellIndex = this.section.children.findIndex(c => c.storageId === this.cell?.storageId);
          return this.section.children[cellIndex + 1];
        } else {
          return null;
        }
      },
      nextCellColsCount () {
        return this.nextCell?.colsCount;
      },
      maxWidth () {
        if (this.section.template === eXo.$layoutUtils.flexTemplate) {
          return this.boxWidth + (this.nextCellColsCount - 1) * (this.innerCellWidth + this.$root.gap);
        } else {
          return null;
        }
      },
      innerCellWidth () {
        return this.section && this.sectionWidth && (this.sectionWidth - (this.$root.gap * (this.section.colsCount - 1))) / this.section.colsCount || 0;
      },
      innerCellHeight () {
        return this.section && this.sectionHeight && (this.sectionHeight - (this.$root.gap * (this.section.rowsCount - 1))) / this.section.rowsCount || 0;
      },
      top () {
        return this.$root.isResize ? this.cellY - this.sectionY : this.movingY - this.$root.diffScrollY;
      },
      left () {
        return this.$root.isResize ? this.cellX - this.sectionX : this.movingX - this.$root.diffScrollX;
      },
      height () {
        if (this.section.template === eXo.$layoutUtils.flexTemplate) {
          return this.boxHeight;
        } else {
          return this.$root.isResize ? this.boxHeight + this.diffMovingY - this.$root.diffScrollY + 3 : this.boxHeight + 3;
        }
      },
      width () {
        const width = this.$root.isResize ? this.boxWidth + this.diffMovingX - this.$root.diffScrollX + 3 : this.boxWidth + 3;
        if (this.maxWidth) {
          return Math.max(this.innerCellWidth, Math.min(width, this.maxWidth));
        } else {
          return Math.max(this.innerCellWidth, width);
        }
      },
      boxStyle () {
        return {
          top: `${this.top}px`,
          left: `${this.left}px`,
          height: `${this.height}px`,
          width: `${this.width}px`,
          'max-height': `${this.height}px`,
          'max-width': `${this.width}px`,
        };
      },
    },
    watch: {
      interceptEvents () {
        if (this.interceptEvents) {
          document.addEventListener('mouseup', this.endMoving);
          this.sectionElement.addEventListener('mousemove', this.updateMoving);
          document.querySelector('.site-scroll-parent').addEventListener('scroll', this.updateScrollPosition);
          this.computingSelectionInterval = window.setInterval(() => this.updateSelection(), 50);
        } else {
          document.removeEventListener('mouseup', this.endMoving);
          this.sectionElement.removeEventListener('mousemove', this.updateMoving);
          document.querySelector('.site-scroll-parent').removeEventListener('scroll', this.updateScrollPosition);
          window.clearInterval(this.computingSelectionInterval);
          this.$nextTick().then(() => {
            this.$root.movingCell = null;
            this.$emit('hide');
          });
        }
      },
      cellElement (newVal, oldVal) {
        if (!oldVal && newVal) {
          window.setTimeout(() => {
            this.$refs.movingBox.append(newVal);
          }, 100);
        }
      },
    },
    created () {
      this.$root.$on('layout-cell-moving-start', this.startMoving);
    },
    beforeUnmount () {
      this.$root.$off('layout-cell-moving-start', this.startMoving);
    },
    methods: {
      updateScrollPosition () {
        if (this.interceptEvents) {
          this.$root.updateScrollPosition();
          this.$nextTick(() => this.updateSelection());
        }
      },
      startMoving (event) {
        if (!this.interceptEvents
          && event?.target?.closest?.('#layoutEditor')
          && event?.target?.tagName !== 'BUTTON'
          && event?.target?.tagName !== 'A') {
          this.$root.initCellsSelection();
          this.$root.resetMoving();
          this.$root.initScrollPosition();

          this.$root.moveType = event.moveType;
          this.cell = event.cell;

          const containerDimensions = event.containerElement.getBoundingClientRect();
          this.boxHeight = containerDimensions.height;
          this.boxWidth = containerDimensions.width;
          this.sectionElement = event.containerElement.parentElement;

          this.updateSectionDimensions();
          this.sectionX = this.sectionDimensions.x;
          this.sectionWidth = this.sectionDimensions.width;
          this.sectionY = this.sectionDimensions.y;
          this.sectionHeight = this.sectionDimensions.height;

          this.cellX = containerDimensions.x;
          this.cellWidth = containerDimensions.width;
          this.cellY = containerDimensions.y;
          this.cellHeight = containerDimensions.height;

          this.movingStartX = event.x;
          this.movingStartY = event.y;

          this.updateBoxCoordinates(event);

          this.startRowIndex = -1;
          this.startColIndex = -1;
          this.updateSelectedCellCoordinates(this.cell.rowIndex, this.cell.colIndex);

          this.cellElement = event.containerElement.cloneNode(true);
          this.cellElement.classList.add('full-width');
          this.cellElement.classList.add('full-height');
          this.$root.movingCell = event.cell;

          this.interceptEvents = false;
          this.$nextTick().then(() => this.interceptEvents = true);
        }
      },
      updateSectionDimensions () {
        this.sectionDimensions = this.sectionElement.getBoundingClientRect();
      },
      updateMoving (event) {
        if (this.interceptEvents) {
          this.updateBoxCoordinates(event);
        }
      },
      updateSelection () {
        const startRowIndex = this.$root.isMove ?
          Math.min(
            parseInt((this.movingY - this.$root.diffScrollY + this.$root.gap) / (this.innerCellHeight + this.$root.gap)),
            this.section.rowsCount - this.cell.rowsCount
          ):
          this.cell.rowIndex;
        const startColIndex = this.$root.isMove ?
          Math.min(
            parseInt((this.movingX - this.$root.diffScrollX + this.$root.gap) / (this.innerCellWidth + this.$root.gap)),
            this.section.colsCount - this.cell.colsCount
          ):
          this.cell.colIndex;
        this.updateSelectedCellCoordinates(startRowIndex, startColIndex);
      },
      updateSelectedCellCoordinates (startRowIndex, startColIndex) {
        if (this.$root.isMove
          && this.startRowIndex === startRowIndex
          && this.startColIndex === startColIndex) {
          return;
        }
        this.startRowIndex = startRowIndex;
        this.startColIndex = startColIndex;
        const rowsCount = this.$root.isMove ? this.cell.rowsCount : parseInt((this.height + this.innerCellHeight) / (this.innerCellHeight + this.$root.gap));
        const colsCount = this.$root.isMove ? this.cell.colsCount : parseInt((this.width + this.innerCellWidth) / (this.innerCellWidth + this.$root.gap));
        if (this.$root.isResize
          && this.rowsCount === rowsCount
          && this.colsCount === colsCount) {
          return;
        }

        this.rowsCount = rowsCount;
        this.colsCount = colsCount;
        const endRowIndex = parseInt(startRowIndex + rowsCount);
        const endColIndex = parseInt(startColIndex + colsCount);
        const selectedCellCoordinates = [];
        for (let i = startRowIndex; i < endRowIndex; i++) {
          for (let j = startColIndex; j < endColIndex; j++) {
            selectedCellCoordinates.push({
              rowIndex: i,
              colIndex: j,
            });
          }
        }
        this.$root.selectedCellCoordinates = selectedCellCoordinates;
        if (this.section.template === eXo.$layoutUtils.flexTemplate) {
          this.$root.nextCellStorageId = this.nextCell?.storageId;
          this.$root.nextCellDiffWidth = (this.innerCellWidth + this.$root.gap) * (colsCount - this.cell.colsCount);
        } else {
          this.$root.nextCellStorageId = null;
          this.$root.nextCellDiffWidth = 0;
        }
      },
      updateBoxCoordinates (event) {
        this.diffMovingX = event.x - this.movingStartX;
        this.diffMovingY = event.y - this.movingStartY;

        this.movingX = Math.min(
          Math.max(this.cellX - this.sectionX + this.diffMovingX, 0),
          this.cellX - this.sectionX + this.sectionWidth - this.cellWidth
        );
        this.movingY = Math.min(
          Math.max(this.cellY - this.sectionY + this.diffMovingY, 0),
          this.cellY - this.sectionY + this.sectionHeight - this.cellHeight
        );
      },
      endMoving () {
        if (this.interceptEvents) {
          this.$root.$emit(`layout-cell-${this.$root.moveType}`, { // layout-cell-resize or layout-cell-drag
            sectionId: this.section.storageId,
            cell: this.cell,
            rowIndex: this.startRowIndex,
            colIndex: this.startColIndex,
            rowsCount: this.rowsCount,
            colsCount: this.colsCount,
          });
          this.interceptEvents = false;
        }
      },
    },
  };
</script>