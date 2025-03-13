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
  <KeepAlive v-if="containerType && storageId">
    <component
      :is="containerType"
      :cell-height="cellHeight"
      :cell-width="cellWidth"
      :cols-count="colsCount"
      :container="container"
      :index="index"
      :length="length"
      :parent-id="parentId"
      :rows-count="rowsCount"
      @initialized="$emit('initialized', container)"
      @move-end="moveEnd"
      @move-start="moveStart" />
  </KeepAlive>
  <div v-else-if="!storageId && containerType === 'application'" class="d-flex align-center justify-center full-width full-height">
    <v-progress-circular color="primary" indeterminate />
  </div>
</template>
<script>
  export default {
    props: {
      container: {
        type: Object,
        default: null,
      },
      index: {
        type: Number,
        default: null,
      },
      length: {
        type: Number,
        default: null,
      },
      parentId: {
        type: String,
        default: null,
      },
      cellHeight: {
        type: String,
        default: null,
      },
      cellWidth: {
        type: String,
        default: null,
      },
      rowsCount: {
        type: Number,
        default: null,
      },
      colsCount: {
        type: Number,
        default: null,
      },
    },
    computed: {
      storageId () {
        return this.container?.storageId;
      },
      containerType () {
        const extension = this.container
          && this.$root.containerTypes.find(ext => ext?.isValid?.(this.container))
          || this.$root.defaultContainer;
        return extension?.containerType;
      },
      params () {
        return {
          container: this.container,
        };
      },
    },
    methods: {
      moveStart (event, moveType, container) {
        this.$emit('move-start', event, moveType, container);
      },
      moveEnd () {
        this.$emit('move-end');
      },
    },
  };
</script>