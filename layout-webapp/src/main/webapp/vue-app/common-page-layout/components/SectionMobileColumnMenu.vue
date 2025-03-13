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
  <div class="absolute-full-size">
    <div class="position-relative full-width full-height d-flex flex-column">
      <div class="position-sticky t-0 mb-auto"></div>
      <div class="position-sticky t-quarter b-quarter z-index-floating-button d-flex align-center">
        <v-card
          v-if="hasLeftColumn"
          class="position-absolute l-0 d-flex align-center justify-center elevation-2 rounded-lg"
          color="white"
          height="60"
          width="60"
          @click="displayedColumn--">
          <v-icon class="icon-default-color">fa-angle-double-right</v-icon>
        </v-card>
        <v-card
          v-if="hasRightColumn"
          class="position-absolute r-0 d-flex align-center justify-center elevation-2 rounded-lg"
          color="white"
          height="60"
          width="60"
          @click="displayedColumn++">
          <v-icon class="icon-default-color">fa-angle-double-left</v-icon>
        </v-card>
      </div>
      <div class="position-sticky b-0 mt-auto"></div>
    </div>
  </div>
</template>
<script>
  export default {
    props: {
      value: {
        type: String,
        default: null,
      },
      container: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      displayedColumn: 1,
    }),
    computed: {
      isMobileColumns () {
        return this.container?.template === 'FlexContainer'
          && this.container?.cssClass?.includes?.('layout-mobile-columns');
      },
      colsCount () {
        return this.isMobileColumns && this.container?.children?.length || 1;
      },
      hasRightColumn () {
        return this.isMobileColumns && this.displayedColumn < this.colsCount;
      },
      hasLeftColumn () {
        return this.isMobileColumns && this.displayedColumn > 1;
      },
      mobilePageClass () {
        return `mobile-column-display-${(this.displayedColumn - 1)}`;
      },
    },
    watch: {
      mobilePageClass: {
        immediate: true,
        handler (value) {
          this.$emit('input', value);
        },
      },
    },
  };
</script>