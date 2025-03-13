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
  <v-hover v-if="childrenSize" :disabled="$root.mobileDisplayMode">
    <template #default="{ hover }">
      <layout-editor-container-base
        ref="container"
        class="position-relative overflow-initial layout-sidebar-section layout-section-content z-index-zero"
        :container="container"
        :index="index"
        :parent-id="parentId"
        section-style
        style="height: inherit;"
        type="sidebar-section"
        @hovered="hoverSection = $event && !drawerOpened">
        <template #footer>
          <site-layout-editor-sidebar-section-menu
            :container="container"
            :hover="!drawerOpened && (hover || hoverSection || movingSection)"
            :index="index"
            :length="length"
            :moving="movingSection"
            :parent-id="parentId"
            @hover-button="hoverSectionMenuButton = $event"
            @move-end="movingSection = false"
            @move-start="movingSection = true" />
        </template>
      </layout-editor-container-base>
    </template>
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
      hoverSectionMenuButton: false,
      movingSection: false,
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
    },
  };
</script>