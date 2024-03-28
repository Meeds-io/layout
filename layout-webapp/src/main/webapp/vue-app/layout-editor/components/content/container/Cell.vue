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
  <layout-editor-container-base
    ref="container"
    :container="container"
    :hide-children="moving"
    :class="{
      'z-index-two': hover && !$root.drawerOpened,
      'elevation-1': hasApplication && hover && !$root.movingCell,
    }"
    :style="{
      'min-height': isDynamicSection && !hasApplication && '50px' || 'initial',
    }"
    class="position-relative d-flex flex-column"
    no-draggable
    @hovered="hover = $event"
    @initialized="computeHasContent">
    <template #footer>
      <v-card
        v-if="isDynamicSection"
        :class="{
          'position-relative mb-n5': hasApplication,
          'absolute-all-center': !hasApplication,
        }"
        :min-height="hasApplication && 50"
        class="position-relative d-flex align-center justify-center flex-grow-1 flex-shrink-1 z-index-one"
        color="transparent">
        <v-btn
          :class="{
            'absolute-all-center': hasApplication,
          }"
          :title="$t('layout.addApplicationButton')"
          min-width="50%"
          class="primary"
          outlined
          @click="$root.$emit('layout-cell-add-application', parentId, container)">
          <v-icon color="primary">far fa-plus-square</v-icon>
        </v-btn>
      </v-card>
      <div
        v-else-if="hasApplication && !moving"
        ref="placeholder"
        :class="{
          'linear-gradient-grey-background': hasContent,
          'white': !hasContent,
        }"
        class="position-relative flex-grow-1 flex-shrink-1 overflow-hidden">
        <div
          v-if="!hasContent"
          class="absolute-all-center d-flex flex-column align-center justify-center">
          <v-icon size="40" class="icon-default-color mb-5">far fa-hourglass</v-icon>
          <span class="font-weight-bold">{{ $t('layout.inSearchOfData') }}</span>
          <span v-if="applicationTitle" class="caption">{{ applicationTitle }}</span>
          <span v-if="applicationCategoryTitle" class="caption">({{ applicationCategoryTitle }})</span>
        </div>
      </div>
      <layout-editor-cell-top-menu
        v-if="hasApplication"
        :container="container"
        :dynamic-section="isDynamicSection"
        :parent-id="parentId"
        :hover="hover"
        :moving="moving"
        :application-title="applicationTitle"
        :application-category="applicationCategoryTitle"
        @move-start="moveStart" />
      <v-hover :disabled="$root.mobileDisplayMode" v-else>
        <v-card
          slot-scope="hoverScope"
          :class="{
            'opacity-5': hoverScope.hover && !isSelectedCell,
            'grey-background': !isSelectedCell && (!$root.movingCell || $root.selectedSectionId !== parentId),
            'transparent': $root.movingCell && $root.selectedSectionId === parentId && !isSelectedCell,
            'grey': isSelectedCell,
          }"
          class="full-width full-height"
          flat
          v-on="!multiSelectEnabled && {
            click: () => $root.$emit('layout-cell-add-application', parentId, container)
          }" />
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
  },
  data: () => ({
    hover: false,
    hasContent: true,
  }),
  computed: {
    moving() {
      return this.storageId && this.$root.movingCell?.storageId === this.storageId;
    },
    sectionType() {
      return this.$layoutUtils.getSection(this.$root.layout, this.parentId)?.template;
    },
    isDynamicSection() {
      return this.sectionType === this.$layoutUtils.flexTemplate;
    },
    multiSelectEnabled() {
      return this.$root.isMultiSelect
        && this.$root.multiCellsSelect;
    },
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
    applicationTitle() {
      return this.container?.children?.[0]?.title || '';
    },
    applicationCategory() {
      return this.applicationTitle && this.$root.applicationCategories?.find?.(c => c?.applications?.find?.(a => a?.displayName === this.applicationTitle));
    },
    applicationCategoryTitle() {
      return this.applicationCategory?.displayName || '';
    },
    isSelectedCell() {
      return this.$root.isMultiSelect
        && this.$root.selectedSectionId === this.parentId
        && !!this.$root.selectedCellCoordinates.find(c => c.rowIndex === this.container.rowIndex && c.colIndex === this.container.colIndex);
    },
  },
  watch: {
    hasApplication() {
      this.computeHasContent();
    },
  },
  mounted() {
    this.computeHasContent();
  },
  methods: {
    moveStart(event, moveType) {
      this.$root.movingParentId = this.parentId;
      this.$nextTick().then(() => {
        this.$root.$emit('layout-cell-moving-start', {
          target: event.target,
          x: event.x,
          y: event.y,
          sectionId: this.parentId,
          cell: this.container,
          containerElement: this.$refs.container.$el,
          moveType,
        });
      });
    },
    computeHasContent() {
      this.hasContentCheckCount = 0;
      this.hasContent = true;
      this.computeHasContentAsync();
    },
    computeHasContentAsync() {
      if (this.hasContentCheckCount > 10) {
        this.hasContent = false;
        return;
      }
      this.hasContentCheckCount++;
      window.setTimeout(() => {
        let hasContent = false;
        if (this.$refs.placeholder) {
          const placeholderHeight = this.$refs.placeholder.getBoundingClientRect().height;
          const containerHeight = this.$refs.container.$el.getBoundingClientRect().height;
          hasContent = containerHeight - placeholderHeight > 10;
        }
        this.hasContent = hasContent;
        if (!hasContent) {
          this.computeHasContentAsync();
        }
      }, 200);
    },
  },
};
</script>