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
  <v-hover v-model="hover">
    <div
      ref="content"
      :id="id"
      :class="`${cssClass}${isDynamicSection && (hover || !hasContent) && ' position-relative' || ''}${displayNoContent && ' z-index-one' || ''}`"
      :style="cssStyle"
      :data-storage-id="storageId"
      class="layout-application">
      <v-hover v-model="hoverMenu">
        <layout-editor-application-menu
          ref="menu"
          :container="container"
          :section="section"
          :parent-id="parentId"
          :application-title="applicationTitle"
          :application-category-title="applicationCategoryTitle"
          @move-start="moveStart"
          @move-end="moveEnd" />
      </v-hover>
      <div v-if="displayNoContent" class="full-width text-no-wrap border-color-grey-lighten ms-1">
        <div class="layout-no-content absolute-vertical-center d-flex full-width ms-n1">
          <div class="light-black-background white--text px-2">
            {{ applicationTitle }}
          </div>
          <v-icon size="35" class="layout-no-content-caret icon-default-color my-n2">{{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}</v-icon>
        </div>
      </div>
      <div
        v-else-if="!editablePortlet && !hoverMenu"
        v-show="hover"
        class="full-width full-height position-absolute z-index-two">
        <v-expand-transition>
          <v-card
            v-if="hover"
            :class="isDynamicSection && 'mb-5'"
            :height="isDynamicSection && 'calc(100% - 20px)' || '100%'"
            class="d-flex align-center justify-center full-width transition-fast-in-fast-out mask-color darken-2 v-card--reveal white--text">
            <v-icon size="22" class="white--text me-2 mt-1">fab fa-readme</v-icon>
            <span>{{ $t('layout.readonlyPortletContent') }}</span>
          </v-card>
        </v-expand-transition>
      </div>
    </div>
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
  },
  data: () => ({
    applicationInstalled: false,
    installing: false,
    section: null,
    cssStyle: null,
    cssClass: null,
    hover: false,
    hoverMenu: false,
    hoverGridCell: false,
    hasContentCheckCount: 0,
    hasContent: true,
  }),
  computed: {
    storageId() {
      return this.container?.storageId;
    },
    hoveredParentId() {
      return this.$root.hoveredParentId;
    },
    applicationId() {
      return this.container?.children?.[0]?.storageId || this.container?.storageId;
    },
    nodeId() {
      return this.$root.draftNodeId;
    },
    nodeUri() {
      return this.$root.draftNodeUri;
    },
    id() {
      return `UIPortlet-${this.container?.id || this.storageId || parseInt(Math.random() * 10000)}`;
    },
    isDynamicSection() {
      return this.section?.template === this.$layoutUtils.flexTemplate;
    },
    applicationTitle() {
      return this.$root.portletInstances?.find?.(a => a?.contentId === this.container?.contentId)?.name || this.container?.title || '';
    },
    applicationCategory() {
      return this.applicationTitle && this.$root.portletInstanceCategories?.find?.(c => c?.applications?.find?.(a => a?.name === this.applicationTitle));
    },
    applicationCategoryTitle() {
      return this.applicationCategory?.name || '';
    },
    hoverApp() {
      return this.hoverMenu || this.hover || this.hoverGridCell;
    },
    displayNoContent() {
      return this.isDynamicSection && !this.hasContent && this.$root.desktopDisplayMode;
    },
    portletInstance() {
      return this.$root.portletInstances?.find?.(p => p.contentId === this.container?.contentId);
    },
    editablePortlet() {
      return this.portletInstance?.editable || false;
    },
  },
  watch: {
    applicationInstalled() {
      this.$emit('initialized');
      this.computeHasContent();
    },
    storageId(newVal, oldVal) {
      if (!oldVal && newVal) {
        this.installApplication();
      }
    },
    nodeUri(newVal, oldVal) {
      if (!oldVal && newVal) {
        this.installApplication();
      }
    },
    hoveredParentId() {
      if (!this.isDynamicSection) {
        this.hoverGridCell = this.hoveredParentId === this.parentId;
      }
    },
    hoverApp() {
      if (this.hoverApp) {
        this.$refs.menu.displayMenu();
      } else {
        this.$refs.menu.hideMenu();
      }
    },
  },
  created() {
    this.$root.$on('layout-section-application-update-style', this.updateStyle);
    this.$root.$on('layout-editor-portlet-properties-updated', this.updateApplication);
    this.initStyle();
    this.section = this.$layoutUtils.getSectionByContainer(this.$root.layout, this.parentId);
  },
  mounted() {
    this.installApplication();
  },
  updated() {
    this.installApplication();
  },
  beforeDestroy() {
    this.$root.$off('layout-section-application-update-style', this.updateStyle);
    this.$root.$off('layout-editor-portlet-properties-updated', this.updateApplication);
  },
  methods: {
    updateApplication(applicationId) {
      if (applicationId !== this.applicationId) {
        return;
      }
      this.applicationInstalled = false;
      this.installApplication();
    },
    installApplication() {
      if (!this.applicationInstalled
          && !this.installing
          && this.$refs.content
          && this.nodeUri
          && this.storageId) {
        this.$applicationUtils.installApplication(this.nodeUri, this.storageId, this.$refs.content)
          .then(() => window.setTimeout(() => this.applicationInstalled = true, 200));
      }
    },
    updateStyle(container) {
      if (this.container.storageId === container.storageId) {
        this.initStyle();
      }
    },
    initStyle() {
      this.cssClass = this.container.cssClass || '';
      this.cssStyle = this.$applicationUtils.getStyle(this.container, {
        isApplicationBackground: true,
        isApplicationStyle: true,
        isApplicationScroll: true,
      });
    },
    moveEnd() {
      this.$emit('move-end');
    },
    moveStart(event, moveType) {
      this.$emit('move-start', event, moveType, this.container);
    },
    hasUnit(length) {
      return Number.isNaN(Number(length));
    },
    computeHasContent() {
      if (!this.isDynamicSection) {
        return;
      }
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
        if (this.$refs.content) {
          this.hasContent = this.$refs.content.getBoundingClientRect().height > 30;
          if (!this.hasContent) {
            this.computeHasContentAsync();
          }
        }
      }, 200);
    },
  },
};
</script>