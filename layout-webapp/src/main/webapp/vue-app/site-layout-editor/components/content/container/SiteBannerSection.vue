<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <v-hover v-model="hoverSection" :disabled="$root.mobileDisplayMode">
    <layout-editor-container-base
      ref="container"
      class="position-relative overflow-initial layout-banner-section layout-section-content display-flex flex-row"
      :class="rowIndexClass"
      :container="container"
      draggable
      :index="index"
      :parent-id="parentId"
      section-style
      :style="hoverSection && {
        'z-index': 2
      }"
      type="banner-section"
      @hovered="hoverSection = $event && !drawerOpened">
      <template #footer>
        <site-layout-editor-banner-section-menu
          :container="container"
          :hover="!drawerOpened && (hover || hoverSection || movingSection)"
          :index="index"
          :length="length"
          :moving="movingSection"
          @move-end="movingSection = false"
          @move-start="movingSection = true" />
      </template>
    </layout-editor-container-base>
  </v-hover>
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
      index: {
        type: Number,
        default: null,
      },
      length: {
        type: Number,
        default: null,
      },
    },
    data: () => ({
      hoverSection: false,
      movingSection: false,
      defaultHeight: 57,
      sectionWidth: 0,
    }),
    computed: {
      storageId () {
        return this.container?.storageId;
      },
      zIndexClass () {
        return !this.drawerOpened && 'z-index-one';
      },
      drawerOpened () {
        return this.$root.drawerOpened;
      },
      childrenSize () {
        return this.container?.children?.length;
      },
      isTopContainer () {
        return this.index < this.$root.middleCenterContainerIndex;
      },
      cssClass () {
        return [
          this.isTopContainer ? 'layout-banner-top-section' : 'layout-banner-bottom-section',
          this.container.cssClass?.includes('layout-sticky-section') ? ' layout-sticky-section' : '',
        ];
      },
      rowIndexClass () {
        return this.index % 2 === 0 ? 'layout-banner-section-even' : 'layout-banner-section-odd';
      },
    },
    watch: {
      isTopContainer: {
        immediate: true,
        handler () {
          const container = eXo.$layoutUtils.getContainerById(this.$root.layout, this.storageId);
          if (this.isTopContainer) {
            if (!container.cssClass?.includes?.('layout-banner-top-section')) {
              container.cssClass = container.cssClass?.trim?.()?.length ? `${container.cssClass.trim()} layout-banner-top-section` : 'layout-banner-top-section';
            }
            if (container.cssClass?.includes?.('layout-banner-bottom-section')) {
              container.cssClass = container.cssClass.replace('layout-banner-bottom-section', '');
            }
          } else {
            if (!container.cssClass?.includes?.('layout-banner-bottom-section')) {
              container.cssClass = container.cssClass?.trim?.()?.length ? `${container.cssClass.trim()} layout-banner-bottom-section` : 'layout-banner-bottom-section';
            }
            if (container.cssClass?.includes?.('layout-banner-top-section')) {
              container.cssClass = container.cssClass.replace('layout-banner-top-section', '');
            }
          }
          if (!container.height) {
            container.height = this.defaultHeight;
          }
        },
      },
    },
  };
</script>