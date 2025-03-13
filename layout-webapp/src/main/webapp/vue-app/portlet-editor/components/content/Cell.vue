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
  <v-hover v-model="hover" :disabled="!readOnly">
    <div
      class="d-flex position-relative"
      :style="cssStyle">
      <v-card
        v-if="isEmpty"
        class="d-flex align-center card-border-radius justify-center position-absolute full-width full-height"
        flat>
        {{ displayEmptyMessage && $t('portlets.emptyPortletInstanceContent') || '' }}
      </v-card>
      <div
        v-else-if="readOnly"
        v-show="hover && !hoverResizeButton"
        class="full-width full-height overflow-hidden position-absolute z-index-two">
        <v-expand-transition>
          <v-card
            v-if="hover && !hoverResizeButton"
            class="d-flex align-center justify-center full-width transition-fast-in-fast-out mask-color darken-2 v-card--reveal white--text"
            height="100%">
            <v-icon class="white--text me-2 mt-1" size="22">fab fa-readme</v-icon>
            <span>{{ $t('layout.readonlyPortletContent') }}</span>
          </v-card>
        </v-expand-transition>
      </div>
      <portlet-editor-application
        ref="application"
        @empty="isEmpty = $event"
        @empty-message="displayEmptyMessage = true"
        @initialized="initialized = true" />
      <v-hover
        v-if="$root.portletMode === 'view' && !isEmpty"
        v-model="hoverResizeButton"
        :disabled="!readOnly">
        <portlet-editor-cell-resize-button
          ref="resizeButton"
          :moving="moving"
          @move-start="moveStart" />
      </v-hover>
    </div>
  </v-hover>
</template>
<script>
  export default {
    data: () => ({
      initialized: false,
      movingStartX: 0,
      movingStartY: 0,
      movingX: 0,
      movingY: 0,
      diffX: 0,
      diffY: 0,
      updateDisplayInterval: 0,
      moving: false,
      hoverResizeButton: false,
      hover: false,
      isEmpty: null,
      displayEmptyMessage: false,
    }),
    computed: {
      displayResizeButton () {
        return !this.$root.isMobile;
      },
      portletMode () {
        return this.$root.portletMode;
      },
      width () {
        return this.diffX && `calc(100% ${this.diffX > 0 ? '+' : '-'} ${Math.abs(this.diffX) * 2}px)` || '100%';
      },
      height () {
        return this.diffY && `calc(100% ${this.diffY > 0 ? '+' : '-'} ${Math.abs(this.diffY) * 2}px)` || '100%';
      },
      cssStyle () {
        return {
          'max-width': '100%',
          width: this.width,
          height: this.height,
        };
      },
      readOnly () {
        return this.initialized
          && this.$root.portletInstance
          && this.$root.portletMode === 'view'
          && !this.$root.editablePortlet;
      },
    },
    watch: {
      portletMode () {
        this.movingStartX = 0;
        this.movingStartY = 0;
        this.movingX = 0;
        this.movingY = 0;
        this.diffX = 0;
        this.diffY = 0;
      },
      isEmpty () {
        this.$root.portletInstanceEmpty = this.isEmpty;
      },
    },
    methods: {
      moveEnd () {
        this.moving = false;
        document.removeEventListener('mouseup', this.moveEnd);
        document.removeEventListener('mousemove', this.move);
      },
      move (event) {
        this.movingX = event.x;
        this.movingY = event.y;
        this.updateDisplay();
      },
      moveStart (event) {
        this.moving = true;
        if (!this.movingStartX) {
          this.movingStartX = event.x;
          this.movingStartY = event.y;
        }
        this.updateDisplayInterval = 0;
        document.addEventListener('mouseup', this.moveEnd);
        document.addEventListener('mousemove', this.move);
      },
      updateDisplay () {
        if (!this.updateInterval) {
          if (!this.computingDisplayInterval && !this.$root.multiCellsSelect) {
            this.updateDisplayInterval = window.setTimeout(() => {
              this.diffX = this.movingX - this.movingStartX;
              this.diffY = this.movingY - this.movingStartY;
              this.updateDisplayInterval = 0;
            }, 50);
          }
        }
      },
    },
  };
</script>