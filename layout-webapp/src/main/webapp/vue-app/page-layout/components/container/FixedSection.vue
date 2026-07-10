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
    :style="cssStyle"
    class="layout-section">
    <div
      v-if="backgroundLayerStyle"
      class="layout-background-layer"
      :style="backgroundLayerStyle"></div>
    <page-layout-container-base
      :container="container"
      :parent-id="parentId"
      no-background-style
      class="layout-section-content" />
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    parentId: {
      type: String,
      default: null,
    },
  },
  computed: {
    cssStyle() {
      return this.$applicationUtils.getStyle(this.container, {
        onlyBackgroundStyle: true,
        sectionStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.$applicationUtils.getBackgroundLayerStyle(this.container, {});
    },
  },
};
</script>
