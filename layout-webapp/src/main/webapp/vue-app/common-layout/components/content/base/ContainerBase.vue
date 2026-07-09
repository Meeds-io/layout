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
  <v-hover v-model="hover" :disabled="$root.mobileDisplayMode">
    <component
      v-model="children"
      v-bind="draggable && {
        'options': dragOptions,
      }"
      v-on="draggable && {
        start: startMoving,
        end: endMoving,
      }"
      :is="draggable && 'draggable' || 'div'"
      :id="id"
      :class="cssClass"
      :style="cssStyle"
      :data-storage-id="storageId">
      <!-- Added in a template on purpose to workaround a 'draggable' component bug -->
      <template v-if="$slots.header">
        <slot name="header"></slot>
        <template v-if="$slots.content">
          <slot name="content"></slot>
        </template>
        <template v-else-if="hasChildren">
          <layout-editor-container-extension
            v-for="(child, i) in children"
            :key="child.storageId"
            :container="child"
            :parent-id="storageId"
            :index="i"
            :length="childrenSize"
            :class="{
              'invisible': hideChildren,
              'draggable-container-flex': draggable,
            }"
            @initialized="$emit('initialized', child)"
            @move-start="moveStart"
            @move-end="moveEnd" />
        </template>
      </template>
      <!-- Added in a template on purpose to workaround a 'draggable' component bug -->
      <template v-else>
        <template v-if="$slots.content">
          <slot name="content"></slot>
        </template>
        <template v-else-if="hasChildren">
          <layout-editor-container-extension
            v-for="(child, i) in children"
            :key="child.storageId"
            :container="child"
            :parent-id="storageId"
            :index="i"
            :length="childrenSize"
            :class="{
              'invisible': hideChildren,
              'draggable-container-flex': draggable,
            }"
            @initialized="$emit('initialized', child)"
            @move-start="moveStart"
            @move-end="moveEnd" />
        </template>
      </template>
      <template v-if="$slots.footer">
        <slot name="footer"></slot>
      </template>
    </component>
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
    type: {
      type: String,
      default: () => 'default',
    },
    index: {
      type: Number,
      default: () => 0,
    },
    length: {
      type: Number,
      default: () => 0,
    },
    draggable: {
      type: Boolean,
      default: false,
    },
    hideChildren: {
      type: Boolean,
      default: false,
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
  data: () => ({
    hover: false,
    dragged: false,
    movingOrder: null,
  }),
  computed: {
    children: {
      get() {
        return this.container.children?.slice?.() || [];
      },
      set(value) {
        if (!this.isCell && JSON.stringify(this.container.children) !== JSON.stringify(value)) {
          this.$set(this.container, 'children', value?.filter?.(c => c));
        }
      }
    },
    storageId() {
      return this.container.storageId;
    },
    id() {
      return this.container.id || this.storageId;
    },
    containerChildren() {
      return this.container.children;
    },
    childrenSize() {
      return this.children.length;
    },
    hasChildren() {
      return !!this.childrenSize;
    },
    noChildren() {
      return !this.childrenSize;
    },
    height() {
      return this.container.height === 'unset' ? null : this.container.height;
    },
    width() {
      return this.container.width === 'unset' ? null : this.container.width;
    },
    cssStyle() {
      return this.$applicationUtils.getStyle(this.container, {
        isApplicationBackground: this.container.template === this.$layoutUtils.bannerCellTemplate,
        noSectionMargins: this.noSectionMargins,
        noBackgroundStyle: this.noBackgroundStyle,
        siteStyle: this.siteStyle,
        pageStyle: this.pageStyle,
        sectionStyle: this.sectionStyle,
        appStyle: !this.noApplicationStyle,
        dynamicWidth: this.dynamicWidth,
      });
    },
    containerCssClass() {
      return this.$root.mobileDisplayMode ? this.container.cssClass?.replace?.('d-md-grid', '') : this.container.cssClass;
    },
    cssClass() {
      return [
        this.containerCssClass?.replace?.('layout-sticky-application', ''),
        this.draggable ? 'v-draggable' : '',
        (this.noChildren || this.draggable) ? 'position-relative' : ''
      ];
    },
    isCell() {
      return this.container.template === this.$layoutUtils.cellTemplate;
    },
    sectionType() {
      if (this.$root.isSiteLayout) {
        return this.container.template;
      } else {
        const section = this.$layoutUtils.getSection(this.$root.layout, this.parentId);
        return section?.template || 'page';
      }
    },
    isDraggableCell() {
      return this.container.template === this.$layoutUtils.cellTemplate
        || this.container.template === this.$layoutUtils.bannerTemplate
        || this.container.template === this.$layoutUtils.sidebarCellTemplate;
    },
    dragSelectionClass() {
      return this.isDraggableCell && '.draggable-cell' || '.draggable';
    },
    dragOptions() {
      return this.sectionType && {
        group: `${this.sectionType}`,
        draggable: '.draggable-container-flex',
        animation: 200,
        ghostClass: 'layout-moving-ghost-container',
        chosenClass: 'layout-moving-chosen-container',
        handle: this.dragSelectionClass,
        dataIdAttr: 'data-storage-id',
      };
    },
  },
  watch: {
    hover() {
      this.$emit('hovered', this.hover);
    },
    containerChildren() {
      if (JSON.stringify(this.containerChildren) !== JSON.stringify(this.children)) {
        this.refreshChildren();
      }
    },
    children() {
      if (!this.isCell && JSON.stringify(this.container.children) !== JSON.stringify(this.children)) {
        this.$root.$emit('layout-children-size-updated', this.container, this.children, this.index, this.type);
      }
    },
  },
  created() {
    this.$root.$on('layout-editor-moving-end', this.refreshChildren);
    this.refreshChildren();
  },
  beforeDestroy() {
    this.$root.$off('layout-editor-moving-end', this.refreshChildren);
  },
  methods: {
    refreshChildren() {
      this.children = this.container?.children?.filter(c => !!c) || [];
    },
    hasUnit(length) {
      return Number.isNaN(Number(length));
    },
    moveStart(event, moveType, container) {
      this.$emit('move-start', event, moveType, container);
    },
    moveEnd() {
      this.$emit('move-end');
      this.$root.movingParentId = null;
    },
    startMoving() {
      this.dragged = true;
      this.$root.movingParentId = this.parentId;
      const section = this.$layoutUtils.getContainerById(this.$root.layout, this.parentId);
      this.$root.movingParentDynamic = section?.template === this.$layoutUtils.flexTemplate;
      // Captured here (before the 'draggable' component's v-model reorders
      // this.container.children on its own) so the history entry reflects
      // the order that was in place before the move, not after it.
      this.movingOrder = !this.isCell && this.container.children?.map(c => c.storageId) || null;
    },
    endMoving(event) {
      this.dragged = false;
      this.$root.movingParentId = null;
      const movingOrder = this.movingOrder;
      this.movingOrder = null;

      const fromCell = this.$layoutUtils.getContainerById(this.$root.layout, event.from.getAttribute('data-storage-id'));
      const toCell = this.$layoutUtils.getContainerById(this.$root.layout, event.to.getAttribute('data-storage-id'));
      const application = this.$layoutUtils.getContainerById(this.$root.layout, event.item.getAttribute('data-storage-id'));

      // Section items (Section.vue) don't carry a 'data-storage-id' on their
      // own root element like cells/applications do, so 'application' can't
      // be resolved for a section-level (non-cell) reorder. Order history only
      // needs fromCell/toCell (resolved from the list container itself), so
      // it's handled independently of whether the dragged item itself resolved.
      if (!this.isCell && movingOrder && fromCell && toCell && fromCell.storageId === toCell.storageId) {
        this.$root.$emit('layout-section-order-history-add', fromCell.storageId, movingOrder);
      }

      if (fromCell && toCell && application) {
        if (this.isCell) {
          const section = this.$layoutUtils.getSectionByContainer(this.$root.layout, event.item.getAttribute('data-storage-id'), this.$root.isSiteLayout);
          this.$root.$emit('layout-section-history-add', section?.storageId);
        }

        const fromIndex = fromCell.children.findIndex(c => c?.storageId === application.storageId);
        if (fromIndex >= 0) {
          fromCell.children.splice(fromIndex, 1);
        }
        const toIndex = toCell.children.findIndex(c => c?.storageId === application.storageId);
        if (toIndex < 0) {
          toCell.children.splice(event.newIndex, 0, application);
        }
      }
    },
  },
};
</script>
