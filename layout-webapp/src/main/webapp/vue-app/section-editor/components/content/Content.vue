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
  <v-card
    :class="parentClass"
    class="transparent layout-sections-parent page-content mx-auto"
    flat>
    <layout-editor-container-extension
      :container="pageLayout"
      class="layout-page-body no-border-radius" />
    <layout-editor-section-edit-drawer
      ref="sectionEditDrawer" />
    <layout-editor-application-edit-drawer
      ref="applicationPropertiesDrawer" />
    <layout-editor-application-category-select-drawer
      ref="applicationCategoryDrawer" />
    <layout-editor-application-add-drawer
      ref="applicationDrawer" />
    <layout-editor-portlet-edit-dialog />
    <layout-image-illustration-preview />
  </v-card>
</template>
<script>
export default {
  props: {
    page: {
      type: Object,
      default: null,
    },
    node: {
      type: Object,
      default: null,
    },
    layout: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    layoutToEdit: null,
    loading: 1,
  }),
  computed: {
    pageLayout() {
      if (this.layoutToEdit?.children?.[0]?.children?.[0]?.template === 'system:/groovy/portal/webui/container/UIPageLayout.gtmpl') {
        return this.layoutToEdit?.children?.[0]?.children?.[0];
      } else {
        return this.layoutToEdit?.children?.[0];
      }
    },
    mobileDisplayMode() {
      return this.$root.mobileDisplayMode;
    },
    parentClass() {
      return this.mobileDisplayMode && 'layout-mobile-view elevation-3 mt-3' || 'layout-desktop-view';
    },
  },
  watch: {
    layoutToEdit: {
      immediate: true,
      handler() {
        this.$root.layout = this.layoutToEdit;
      },
    },
    loading(newVal, oldVal) {
      if (newVal - oldVal > 0) {
        document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      } else if (newVal - oldVal < 0) {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }
    },
    layout: {
      immediate: true,
      handler() {
        if (this.layout) {
          const layout = JSON.parse(JSON.stringify(this.layout));
          this.setLayout(layout);
        }
      },
    },
    mobileDisplayMode() {
      this.switchDisplayMode();
    },
  },
  created() {
    this.$root.$on('layout-edit-section-drawer', this.editSection);
    this.$root.$on('layout-add-application-category-drawer', this.openApplicationCategoryDrawer);
    this.$root.$on('layout-replace-section', this.handleReplaceSection);
    this.$root.$on('layout-children-size-updated', this.handleSectionUpdated);
    this.$root.$on('layout-cell-resize', this.handleCellResize);
    this.$root.$on('layout-cell-drag', this.handleCellMove);
    this.$root.$on('layout-cells-select', this.addApplicationOnCells);
    this.$root.$on('layout-add-application', this.handleAddApplication);
    this.$root.$on('layout-edit-application', this.handleEditApplication);
    this.$root.$on('layout-delete-application', this.handleDeleteApplication);
    this.$root.$on('layout-application-category-drawer-closed', this.resetCellsSelection);
    this.$root.$on('layout-section-history-add', this.addSectionVersion);
    this.$root.$on('layout-section-order-history-add', this.addOrderVersion);
    this.$root.$on('layout-page-saved', this.handlePageSaved);
    this.$root.$on('layout-apply-grid-style', this.handleApplyGridStyle);
    this.$root.$on('layout-save-draft', this.saveDraft);
    document.addEventListener('keydown', this.restoreSectionVersion);
  },
  methods: {
    setLayout(layout) {
      this.initContainer(layout);
      if (this.mobileDisplayMode) {
        // Breakpoint CSS classes are stripped while previewing mobile
        // (see switchDisplayMode/applyMobileStyle) so that the desktop grid
        // doesn't leak into the shrunk preview box. parseSections re-derives
        // colsCount/rowsCount from those classes, so restore them first or
        // it corrupts every section's layout, not just the one being edited.
        this.$layoutUtils.applyDesktopStyle(layout);
      }
      this.$layoutUtils.parseSections(layout);
      if (this.mobileDisplayMode) {
        this.$layoutUtils.applyMobileStyle(layout);
      }
      if (this.layoutToEdit) {
        Object.assign(this.layoutToEdit, layout);
      } else {
        this.layoutToEdit = layout;
      }
    },
    switchDisplayMode() {
      if (this.$root.displayMode === 'mobile') {
        this.$layoutUtils.applyMobileStyle(this.layoutToEdit);
      } else {
        this.$layoutUtils.applyDesktopStyle(this.layoutToEdit);
      }
    },
    initContainer(container) {
      if (container.children) {
        container.children.forEach(this.initContainer);
      } else {
        container.children = [];
      }
    },
    editSection(index) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      if (parentContainer) {
        this.$refs.sectionEditDrawer.open(parentContainer.children[index], index, parentContainer.children.length);
      }
    },
    openApplicationCategoryDrawer(sectionId, container) {
      this.$root.selectedSectionId = sectionId;
      this.$root.selectedCells = [container];
      this.$refs.applicationCategoryDrawer.open();
    },
    resetCellsSelection() {
      window.setTimeout(() => {
        this.$root.resetMoving();
        this.$root.initCellsSelection();
      }, 300);
    },
    addApplicationOnCells(selection) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      const section = parentContainer.children.find(c => c.storageId === this.$root.selectedSectionId);
      if (section) {
        this.$root.selectedCells = section.children?.filter?.(c =>
          this.$layoutUtils.isBetween(c.colIndex, selection.fromColIndex, selection.toColIndex)
          && this.$layoutUtils.isBetween(c.rowIndex, selection.fromRowIndex, selection.toRowIndex)
        ) || [];
        this.$refs.applicationCategoryDrawer.open();
      } else {
        console.warn(`Can't find section with id ${this.$root.selectedSectionId}`); // eslint-disable-line no-console
      }
    },
    handlePageSaved() {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertLink: `/portal${this.$root.nodeUri}`,
        alertMessage: this.$t('layout.pageSavedSuccessfully'),
        alertLinkText: this.$t('layout.view'),
        alertLinkTarget: '_blank',
        alertType: 'success',
      }}));
    },
    handleApplyGridStyle() {
      this.$layoutUtils.applyGridStyle(this.layoutToEdit);
    },
    handleAddApplication(application) {
      const selectedCells = this.$root.selectedCells.slice();
      const selectedSectionId = this.$root.selectedSectionId;
      const firstCellRowIndex = Math.min(...selectedCells.map(c => c.rowIndex));
      const firstCellColIndex = Math.min(...selectedCells.map(c => c.colIndex));
      const lastCellRowIndex = Math.max(...selectedCells.map(c => c.rowIndex));
      const lastCellColIndex = Math.max(...selectedCells.map(c => c.colIndex));

      const singleCell = selectedCells?.length === 1;
      const section = this.$layoutUtils.getSection(this.layoutToEdit, selectedSectionId);
      const isDynamicSection = section.template === this.$layoutUtils.flexTemplate;
      try {
        const firstCell = singleCell && selectedCells[0] || selectedCells.find(c => c.rowIndex === firstCellRowIndex && c.colIndex === firstCellColIndex);
        const lastCell = singleCell && selectedCells[0] || selectedCells.find(c => c.rowIndex === lastCellRowIndex && c.colIndex === lastCellColIndex);
        if (!firstCell) {
          console.error('Can not find the first cell to add an application into it', selectedCells, firstCellRowIndex, firstCellColIndex); // eslint-lint-disable no-console
          return;
        } else if (!lastCell) {
          console.error('Can not find the last cell to add an application into it', selectedCells, lastCellRowIndex, lastCellColIndex); // eslint-lint-disable no-console
          return;
        } else {
          this.addSectionVersion(section.storageId);
          if (selectedCells.length > 1) {
            this.mergeCell(selectedSectionId, firstCell, lastCell.rowIndex, lastCell.colIndex);
          }
        }
        const cell = this.$layoutUtils.getCell(this.layoutToEdit, firstCell.storageId);
        this.$layoutUtils.newApplication(cell, application, isDynamicSection);
        this.saveDraft();
      } finally {
        this.$root.initCellsSelection();
      }
    },
    handleCellResize(event) {
      this.mergeCell(
        event.sectionId,
        event.cell,
        event.rowIndex + event.rowsCount - 1,
        event.colIndex + event.colsCount - 1
      );
    },
    handleCellMove(event) {
      this.moveCell(
        event.sectionId,
        event.cell,
        event.rowIndex,
        event.colIndex);
    },
    handleCellMerge(sectionId, container, targetCellRowIndex, targetCellColIndex) {
      this.mergeCell(sectionId, container, targetCellRowIndex, targetCellColIndex);
    },
    handleDeleteApplication(sectionId, container) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      const section = parentContainer.children.find(c => c.storageId === sectionId);
      if (section) {
        this.addSectionVersion(sectionId);
        this.$layoutUtils.deleteCell(section, container);
      } else {
        console.warn(`Can't find section with id ${sectionId}`); // eslint-disable-line no-console
      }
    },
    handleEditApplication(sectionId, container, applicationTitle) {
      const section = this.$layoutUtils.getSection(this.layoutToEdit, sectionId);
      const containerToEdit = this.$layoutUtils.getContainerById(this.layoutToEdit, container.id);
      this.$refs.applicationPropertiesDrawer.open(section, containerToEdit, applicationTitle);
    },
    mergeCell(sectionId, container, targetCellRowIndex, targetCellColIndex) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      const section = parentContainer.children.find(c => c.storageId === sectionId);
      if (section) {
        this.addSectionVersion(sectionId);
        this.$layoutUtils.resizeCell(section, container, targetCellRowIndex, targetCellColIndex);
      } else {
        console.warn(`Can't find section with id ${sectionId}`); // eslint-disable-line no-console
      }
    },
    moveCell(sectionId, container, targetCellRowIndex, targetCellColIndex) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      const section = parentContainer.children.find(c => c.storageId === sectionId);
      if (section) {
        this.addSectionVersion(sectionId);
        this.$layoutUtils.moveCell(section, container, targetCellRowIndex, targetCellColIndex);
      } else {
        console.warn(`Can't find section with id ${sectionId}`); // eslint-disable-line no-console
      }
    },
    handleSectionUpdated(container, children) {
      container.children = children?.filter(c => !!c) || [];
    },
    handleReplaceSection(index, section) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      this.addSectionVersion(section.storageId);
      parentContainer.children.splice(index, 1, section);
      this.setLayout(this.layoutToEdit);
    },
    addSectionVersion(sectionId) {
      const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
      const section = parentContainer.children.find(c => c.storageId === sectionId);
      if (section) {
        if (!this.$root.sectionHistory) {
          this.$root.sectionHistory = [JSON.parse(JSON.stringify(section))];
        } else {
          this.$root.sectionHistory.push(JSON.parse(JSON.stringify(section)));
        }
        this.$root.sectionRedo = [];
      }
    },
    addOrderVersion(containerId, order) {
      if (containerId && order) {
        const entry = {orderContainerId: containerId, order};
        if (!this.$root.sectionHistory) {
          this.$root.sectionHistory = [entry];
        } else {
          this.$root.sectionHistory.push(entry);
        }
        this.$root.sectionRedo = [];
      }
    },
    restoreSectionVersion(event) {
      if (event.ctrlKey) {
        if (event.keyCode === 90) {
          this.undoSectionVersion();
        } else if (event.keyCode === 89) {
          this.redoSectionVersion();
        }
      }
    },
    undoSectionVersion() {
      if (this.$root.sectionHistory?.length) {
        const entry = this.$root.sectionHistory.pop();
        if (entry.orderContainerId) {
          const container = this.$layoutUtils.getContainerById(this.layoutToEdit, entry.orderContainerId);
          if (container) {
            this.$root.sectionRedo.push({orderContainerId: entry.orderContainerId, order: container.children.map(c => c.storageId)});
            this.$layoutUtils.reorderChildren(container, entry.order);
          }
        } else {
          const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
          const index = parentContainer.children.findIndex(c => c.storageId === entry.storageId);
          if (index >= 0) {
            this.$root.sectionRedo.push(parentContainer.children[index]);
            parentContainer.children.splice(index, 1, entry);
          }
        }
      }
    },
    redoSectionVersion() {
      if (this.$root.sectionRedo?.length) {
        const entry = this.$root.sectionRedo.pop();
        if (entry.orderContainerId) {
          const container = this.$layoutUtils.getContainerById(this.layoutToEdit, entry.orderContainerId);
          if (container) {
            this.$root.sectionHistory.push({orderContainerId: entry.orderContainerId, order: container.children.map(c => c.storageId)});
            this.$layoutUtils.reorderChildren(container, entry.order);
          }
        } else {
          const parentContainer = this.$layoutUtils.getParentContainer(this.layoutToEdit);
          const index = parentContainer.children.findIndex(c => c.storageId === entry.storageId);
          if (index >= 0) {
            this.$root.sectionHistory.push(parentContainer.children[index]);
            parentContainer.children.splice(index, 1, entry);
          }
        }
      }
    },
    resetSectionHistory() {
      this.$root.sectionHistory = [];
      this.$root.sectionRedo = [];
    },
    saveDraft(layout) {
      this.resetSectionHistory();

      this.loading++;
      const layoutToUpdate = this.$layoutUtils.cleanAttributes(layout || this.layoutToEdit, false, true);
      return this.$pageLayoutService.updatePageLayout(this.$root.draftPageRef, layoutToUpdate, 'contentId')
        .then(layout => {
          this.setLayout(layout);
          this.$root.$emit('layout-draft-saved');
        })
        .catch(e => {
          this.$root.$emit('alert-message', this.$te(e.message) ? this.$t(e.message) : this.$t('layout.pageSavingError'), 'error');
          this.$root.$emit('layout-draft-save-error');
        })
        .finally(() => {
          window.setTimeout(() => this.loading--, 200);
          this.$root.$emit('coediting-set-lock');
        });
    },
  },
};
</script>
