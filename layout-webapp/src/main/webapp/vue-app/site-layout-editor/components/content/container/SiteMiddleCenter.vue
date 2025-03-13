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
  <layout-editor-container-base
    ref="container"
    class="d-flex flex-row z-index-one site-middle-center-container"
    :container="container"
    :index="index"
    :length="length"
    page-style
    :style="cssStyle"
    type="site-middle-center-container" />
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
    computed: {
      id () {
        return this.container.id || this.storageId;
      },
      storageId () {
        return this.container?.storageId;
      },
      sidebarsContainerMinWidth () {
        return this.$root.layout?.children
          ?.filter?.(c => c.template === eXo.$layoutUtils.sidebarTemplate
            && c.children?.length
            && (
              !this.$root.mobileDisplayMode
              || !c?.children?.[0].cssClass?.includes?.('hidden-sm-and-down')
            )
          )
          ?.map?.(c => c.width
            && Number(c.width)
            || 310
          )
          ?.reduce?.((acc, v) => acc + v, 0) || 0;
      },
      width () {
        return `max(100vw - ${this.sidebarsContainerMinWidth}px, 100%)`;
      },
      maxWidth () {
        return `min(100vw - ${this.sidebarsContainerMinWidth}px, 100%)`;
      },
      cssStyle () {
        return {
          'height': this.$root.middleCenterContainersMinHeight,
          'max-width': this.maxWidth,
        };
      },
    },
    mounted () {
      if (!this.$root.middleCenterContainersMinHeight) {
        this.$root.middleCenterContainersMinHeight = `${document.body.scrollHeight - this.$root.middleContainerBannersHeight + 2}px`;
      }
    },
  };
</script>