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
  <v-hover v-model="hoverContainer">
    <layout-editor-container-base
      ref="container"
      :container="container"
      :parent-id="parentId"
      :class="{
        'position-relative': hasApplication,
        'z-index-two': hoverContainer && !$root.drawerOpened,
        'elevation-2 border-color': $root.movingParentId,
      }"
      :style="{
        'min-height': hasApplication ? 'auto' : '25px !important',
      }"
      class="full-height flex-grow-1 flex-shrink-1 layout-banner-cell"
      section-style>
      <template v-if="application" #header>
        <v-hover v-model="hoverMenu">
          <layout-editor-application-menu
            ref="menu"
            :container="application"
            :section="container"
            :parent-id="storageId"
            :application-title="applicationTitle"
            @move-start="moveStart"
            @move-end="moveEnd" />
        </v-hover>
        <div
          v-if="!editablePortlet && !moving"
          v-show="hoverContainer"
          class="full-width full-height overflow-hidden position-absolute z-index-two">
          <v-expand-transition>
            <v-card
              v-if="hoverContainer"
              :class="isDynamicSection && 'mb-5'"
              :height="isDynamicSection && 'calc(100% - 20px)' || '100%'"
              class="d-flex align-center justify-center full-width transition-fast-in-fast-out mask-color darken-2 v-card--reveal white--text">
              <v-icon size="22" class="white--text me-2 mt-1">fab fa-readme</v-icon>
              <span>{{ $t('layout.readonlyPortletContent') }}</span>
            </v-card>
          </v-expand-transition>
        </div>
      </template>
      <template #footer>
        <div
          v-if="displayResizer"
          :class="$vuetify.rtl && 'l-0' || 'r-0'"
          class="position-absolute full-height t-0">
          <layout-editor-cell-resize-button
            :container="container"
            :parent-id="parentId"
            :hover="hoverContainer"
            :moving="moving"
            spacing-class="me-0"
            dynamic-section
            @move-start="moveStart" />
        </div>
        <v-hover v-if="$root.desktopDisplayMode && !hasApplication" v-model="hoverAddApplication">
          <v-card
            v-show="!$root.movingParentId"
            :class="backgroundClass"
            class="full-width full-height rounded-lg layout-add-application-button"
            flat
            @click="$root.$emit('layout-add-application-category-drawer', storageId, container)">
            <v-card
              :class="{
                'invisible': !hoverAddApplication,
              }"
              :aria-label="$t('layout.addApplicationButton')"
              :title="$t('layout.addApplicationButton')"
              min-height="100%"
              height="100%"
              color="transparent"
              class="full-width full-height d-flex flex-wrap align-center justify-center"
              flat>
              <component
                :is="hasBackground && 'v-btn' || 'div'"
                :class="{
                  'btn white': hasBackground,
                  'd-flex flex-wrap align-center justify-center': !hasBackground,
                }"
                class="position-absolute">
                <v-icon class="icon-default-color pe-2">fa-plus</v-icon>
                <span class="text-no-wrap">{{ $t('layout.addApp') }}</span>
              </component>
            </v-card>
          </v-card>
        </v-hover>
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
      default: () => 0,
    },
    length: {
      type: Number,
      default: () => 0,
    },
  },
  data: () => ({
    hoverContainer: false,
    hoverMenu: false,
    hoverAddApplication: false,
    initialWidth: 0,
    movingStartX: 0,
    movingX: 0,
    moving: false,
  }),
  computed: {
    children() {
      return this.container.children;
    },
    childrenSize() {
      return this.children?.length || 0;
    },
    hasApplication() {
      return this.childrenSize > 0;
    },
    storageId() {
      return this.container?.storageId;
    },
    parentContainer() {
      return this.$layoutUtils.getContainerById(this.$root.layout, this.parentId);
    },
    hasBackground() {
      return this.parentContainer.backgroundImage
        || this.parentContainer.backgroundColor
        || this.parentContainer.backgroundGradientFrom;
    },
    opaqueBackground() {
      return !this.hoverAddApplication
        && !this.hasBackground
        && (!this.$root.movingCell || this.$root.selectedSectionId !== this.parentId);
    },
    application() {
      return this.children?.[0];
    },
    applicationId() {
      return this.application?.storageId;
    },
    portlet() {
      return this.$root.portlets?.find?.(p => p.contentId === this.application?.contentId);
    },
    editablePortlet() {
      return this.portlet?.editable || false;
    },
    applicationTitle() {
      return this.application?.title || this.portlet?.name || '';
    },
    backgroundClass() {
      if (this.$root.movingCell && this.$root.selectedSectionId === this.parentId) {
        return 'transparent';
      } else if (!this.opaqueBackground) {
        return 'light-grey-background';
      } else {
        return 'layout-empty-cell grey-lighten1-background';
      }
    },
    width() {
      return this.moving && this.movingStartX && (this.initialWidth + this.movingX - this.movingStartX) || null;
    },
    displayResizer() {
      return this.$root.desktopDisplayMode && this.index < (this.length - 1);
    },
  },
  watch: {
    hoverContainer() {
      if (this.hoverContainer) {
        this.$root.hoveredParentId = this.storageId;
        this.$refs?.menu?.displayMenu?.();
      } else if (!this.hoverMenu) {
        this.$refs?.menu?.hideMenu?.();
      }
      if (!this.hoverContainer && this.$root.hoveredParentId === this.storageId) {
        this.$root.hoveredParentId = null;
      }
    },
    hoverMenu() {
      if (this.hoverMenu) {
        this.$refs?.menu?.displayMenu?.();
      } else if (!this.hoverContainer) {
        this.$refs?.menu?.hideMenu?.();
      }
    },
  },
  methods: {
    moveStart(_event, moveType) {
      this.$root.moveType = moveType;
      if (moveType === 'resize') {
        this.$root.$emit('layout-section-history-add');
        this.$root.hoveredSectionId = this.parentId;
        this.movingStartX = 0;
        this.movingX = 0;
        this.initialWidth = this.$refs.container.$el.offsetWidth;
        document.addEventListener('mousemove', this.updateSize);
        document.addEventListener('mouseup', this.endResizing);
        this.moving = true;
      }
    },
    updateSize(event) {
      this.movingX = event.x;
      if (!this.movingStartX) {
        this.movingStartX = event.x;
      }
      window.setTimeout(() => this.$set(this.container, 'width', this.width), 50);
    },
    endResizing() {
      if (this.moving) {
        document.removeEventListener('mousemove', this.updateSize);
        document.removeEventListener('mouseup', this.endResizing);
        this.moving = false;
        this.movingStartX = 0;
        this.movingX = 0;
      }
    },
    moveEnd() {
      this.$emit('move-end');
    },
  },
};
</script>