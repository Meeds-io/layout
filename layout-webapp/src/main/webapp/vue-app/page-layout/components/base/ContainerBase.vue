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
    :id="id"
    :class="cssClass"
    :style="cssStyle">
    <div
      v-if="backgroundLayerStyle"
      class="layout-background-layer"
      :style="backgroundLayerStyle"></div>
    <slot name="header"></slot>
    <template v-if="hasChildren">
      <page-layout-container-extension
        v-for="child in children"
        :key="child.storageId"
        :container="child"
        :parent-id="storageId" />
    </template>
    <slot name="footer"></slot>
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
    noBackgroundStyle: {
      type: Boolean,
      default: false,
    },
    noApplicationStyle: {
      type: Boolean,
      default: false,
    },
    siteStyle: {
      type: Boolean,
      default: false,
    },
    pageStyle: {
      type: Boolean,
      default: false,
    },
    sectionStyle: {
      type: Boolean,
      default: false,
    },
    noSectionMargins: {
      type: Boolean,
      default: false,
    },
    dynamicWidth: {
      type: String,
      default: null,
    },
  },
  computed: {
    id() {
      return this.container?.id || `Container${this.storageId}`;
    },
    storageId() {
      return this.container?.storageId;
    },
    children() {
      return this.container?.children;
    },
    hasChildren() {
      return !!this.children?.length;
    },
    height() {
      return this.container?.height;
    },
    width() {
      return this.container?.width;
    },
    borderColor() {
      return this.container?.borderColor;
    },
    cssStyle() {
      return this.$applicationUtils.getStyle(this.container, {
        isApplicationBackground: this.container.template === 'BannerCell',
        noSectionMargins: this.noSectionMargins,
        noBackgroundStyle: this.noBackgroundStyle,
        siteStyle: this.siteStyle,
        pageStyle: this.pageStyle,
        sectionStyle: this.sectionStyle,
        appStyle: !this.noApplicationStyle,
        dynamicWidth: this.dynamicWidth,
      });
    },
    cssClass() {
      return this.container?.cssClass || '';
    },
    backgroundLayerStyle() {
      return this.$applicationUtils.getBackgroundLayerStyle(this.container, {
        noBackgroundStyle: this.noBackgroundStyle,
      });
    },
  },
  methods: {
    hasUnit(length) {
      return Number.isNaN(Number(length));
    },
  },
};
</script>
