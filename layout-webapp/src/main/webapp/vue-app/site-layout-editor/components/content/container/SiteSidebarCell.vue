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
    class="position-relative display-flex flex-column border-box-sizing layout-sidebar-cell"
    :class="{
      'z-index-two': hover && !$root.drawerOpened,
      'elevation-2 border-color': $root.movingParentId,
    }"
    :container="container"
    draggable
    dynamic-width="20vw"
    :hide-children="moving"
    :parent-id="parentId"
    :style="cssStyle"
    @hovered="hover = $event"
    @move-start="moveStart">
    <template #footer>
      <v-hover v-if="$root.desktopDisplayMode" v-model="hoverAddApplication">
        <v-card
          v-show="!$root.movingParentId"
          class="full-width fill-height layout-add-application-button flex-grow-1 d-flex align-center justify-center"
          :class="backgroundClass"
          flat
          @click="$root.$emit('layout-add-application-category-drawer', storageId, container)">
          <v-card
            :aria-label="$t('layout.addApplicationButton')"
            class="full-width d-flex flex-wrap align-center justify-center"
            :class="{
              'invisible': !hoverAddApplication,
            }"
            color="transparent"
            flat
            :height="hasApplication && 50 || '50%'"
            max-height="200"
            :title="$t('layout.addApplicationButton')">
            <component
              :is="hasBackground && 'v-btn' || 'div'"
              :class="{
                'btn white': hasBackground,
                'd-flex flex-wrap align-center justify-center': !hasBackground,
              }">
              <v-icon class="icon-default-color pe-2">fa-plus</v-icon>
              <span class="text-no-wrap">{{ $t('layout.addApp') }}</span>
            </component>
          </v-card>
        </v-card>
      </v-hover>
    </template>
  </layout-editor-container-base>
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
        default: () => 0,
      },
      length: {
        type: Number,
        default: () => 0,
      },
    },
    data: () => ({
      hover: false,
      hoverAddApplication: false,
      hasContent: true,
    }),
    computed: {
      moving () {
        return this.storageId && this.$root.movingCell?.storageId === this.storageId;
      },
      movingChildren () {
        return this.storageId
          && this.$root.movingParentId
          && this.$root.movingParentDynamic;
      },
      sectionType () {
        return eXo.$layoutUtils.getSection(this.$root.layout, this.parentId)?.template;
      },
      children () {
        return this.container.children;
      },
      childrenSize () {
        return this.children?.length || 0;
      },
      hasApplication () {
        return this.childrenSize > 0;
      },
      storageId () {
        return this.container?.storageId;
      },
      hasBackground () {
        return this.container.backgroundImage
          || this.container.backgroundColor
          || this.container.backgroundGradientFrom;
      },
      opaqueBackground () {
        return !this.hoverAddApplication
          && !this.hasBackground
          && (!this.$root.movingCell || this.$root.selectedSectionId !== this.parentId);
      },
      isInternalSidebar () {
        return this.storageId === this.$root.internalLeftContainer?.children?.[0]?.storageId || this.storageId === this.$root.internalRightContainer?.children?.[0]?.storageId;
      },
      backgroundClass () {
        if (this.$root.movingCell && this.$root.selectedSectionId === this.parentId) {
          return 'transparent';
        } else if (!this.opaqueBackground) {
          return 'light-grey-background';
        } else {
          return `grey-lighten1-background ${this.isInternalSidebar && 'opacity-1' || 'opacity-4'}`;
        }
      },
      width () {
        return this.container.width;
      },
      minHeight () {
        return this.isInternalSidebar ? this.$root.middleCenterContainersMinHeight : 'calc(var(--100vh, 100vh) - 57px)';
      },
      cssStyle () {
        const cssStyle = eXo.$applicationUtils.getStyle(this.container.width ? this.container : {
          ...this.container,
          width: '310px',
        }, {
          sectionStyle: true,
          appStyle: true,
          dynamicWidth: '20vw',
        });
        cssStyle['min-height'] = this.minHeight;
        cssStyle['height'] = '100%';
        return cssStyle;
      },
    },
    watch: {
      hover () {
        if (this.hover) {
          this.$root.hoveredParentId = this.storageId;
        } else if (this.$root.hoveredParentId === this.storageId) {
          this.$root.hoveredParentId = null;
        }
      },
    },
    methods: {
      moveStart (_event, moveType) {
        this.$root.moveType = moveType;
      },
    },
  };
</script>