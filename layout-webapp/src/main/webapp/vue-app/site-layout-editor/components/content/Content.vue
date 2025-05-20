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
    v-if="initialized"
    :class="parentClass"
    class="transparent layout-sections-parent full-width mx-auto"
    flat>
    <layout-editor-container-extension
      :container="$root.layout"
      class="layout-page-body d-flex no-border-radius" />
    <site-layout-editor-application-edit-drawer
      ref="applicationPropertiesDrawer" />
    <layout-editor-application-category-select-drawer
      ref="applicationCategoryDrawer" />
    <layout-editor-application-add-drawer
      ref="applicationDrawer" />
    <layout-editor-portlet-edit-dialog />
    <layout-image-illustration-preview />
    <changes-reminder
      ref="changesReminder"
      :reminder="reminder"
      @opened="changesReminderOpened = true">
      <div>{{ $t('layout.site.reminder.description.part1') }}</div>
      <div>{{ $t('layout.site.reminder.description.part2') }}</div>
      <div>{{ $t('layout.site.reminder.description.part3') }}</div>
      <div>{{ $t('layout.site.reminder.description.part4') }}</div>
    </changes-reminder>
  </v-card>
</template>
<script>
export default {
  props: {
    layout: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    isCompatible: true,
    initialized: true,
    changesReminderOpened: false,
    loading: 1,
  }),
  computed: {
    mobileDisplayMode() {
      return this.$root.mobileDisplayMode;
    },
    parentClass() {
      return this.mobileDisplayMode && 'layout-mobile-view elevation-3 mt-3' || 'layout-desktop-view';
    },
    reminder() {
      return {
        name: 'LayoutEditorChangesAnnounced' ,
        title: this.$t('layout.reminder.title'),
        img: '/layout/images/EditLayout.gif',
      };
    },
  },
  watch: {
    loading(newVal, oldVal) {
      if (newVal - oldVal > 0) {
        document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      } else if (newVal - oldVal < 0) {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }
    },
    layout: {
      immediate: true,
      handler(newVal, oldVal) {
        if (newVal && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
          this.setLayout(JSON.parse(JSON.stringify(newVal)));
        }
        if (!oldVal) {
          window.setTimeout(() => document.dispatchEvent(new CustomEvent('hideTopBarLoading')), 200);
          if (newVal) {
            this.$root.$applicationLoaded();
          }
        }
      }
    },
    mobileDisplayMode() {
      this.switchDisplayMode();
    },
    isCompatible() {
      if (!this.isCompatible) {
        this.openChangesReminder();
      }
    },
  },
  created() {
    this.$root.$on('layout-add-application-category-drawer', this.openApplicationCategoryDrawer);
    this.$root.$on('layout-add-application', this.handleAddApplication);
    this.$root.$on('layout-edit-application', this.handleEditApplication);
    this.$root.$on('layout-delete-application', this.handleDeleteApplication);
    this.$root.$on('layout-section-history-add', this.addHistory);
    this.$root.$on('layout-section-history-undo', this.undoFromHistory);
    this.$root.$on('layout-section-history-redo', this.redoFromHistory);
    this.$root.$on('layout-site-saved', this.handleSiteSaved);
    this.$root.$on('layout-save-draft', this.saveDraft);
    document.addEventListener('keydown', this.restoreHistory);
    this.resetHistory();
  },
  mounted() {
    this.openChangesReminder();
  },
  methods: {
    openChangesReminder() {
      if (!this.isCompatible && !this.changesReminderOpened && this.$refs.changesReminder) {
        this.$refs.changesReminder.open();
      }
    },
    switchDisplayMode() {
      if (this.$root.displayMode === 'mobile') {
        this.$layoutUtils.applyMobileStyle(this.$root.layout);
      } else {
        this.$layoutUtils.applyDesktopStyle(this.$root.layout);
      }
    },
    async setLayout(layout) {
      if (!layout) {
        return;
      }
      this.initContainer(layout);
      const compatible = !!this.$root.layout || this.$layoutUtils.parseSite(layout);
      if (this.$root.layout) {
        Object.assign(this.$root.layout, layout);
      } else {
        this.$root.layout = layout;
      }
      if (!this.initialized) {
        this.initialized = true;
        await this.$nextTick();
        this.isCompatible = compatible;
      }
    },
    initContainer(container) {
      if (container.children) {
        container.children.filter(c => c).forEach(this.initContainer);
      } else {
        container.children = [];
      }
    },
    openApplicationCategoryDrawer(sectionId) {
      this.$root.selectedSectionId = sectionId;
      this.$refs.applicationCategoryDrawer.open();
    },
    handleAddApplication(application) {
      try {
        this.addHistory();
        const container = this.$layoutUtils.getContainerById(this.$root.layout, this.$root.selectedSectionId);
        this.$layoutUtils.newApplication(container, application, true);
        this.saveDraft();
      } finally {
        this.$root.initCellsSelection();
      }
    },
    handleDeleteApplication(sectionId, container) {
      const section = this.$layoutUtils.getContainerById(this.$root.layout, sectionId);
      if (section) {
        this.addHistory();
        const index = section.children.findIndex(c => c.storageId === container.storageId);
        if (index >= 0) {
          section.children.splice(index, 1);
        }
      } else {
        console.warn(`Can't find section with id ${sectionId}`); // eslint-disable-line no-console
      }
    },
    handleEditApplication(sectionId, container, applicationTitle) {
      const section = this.$layoutUtils.getContainerById(this.$root.layout, sectionId);
      const containerToEdit = section.template === this.$layoutUtils.bannerCellTemplate ? section : container;
      this.$refs.applicationPropertiesDrawer.open(section, containerToEdit, applicationTitle);
    },
    handleSiteSaved() {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertLink: this.$root.siteUri,
        alertMessage: this.$t('layout.siteSavedSuccessfully'),
        alertLinkText: this.$t('layout.view'),
        alertLinkTarget: '_blank',
        alertType: 'success',
      }}));
    },
    addHistory() {
      if (!this.$root.sectionHistory) {
        this.$root.sectionHistory = [JSON.parse(JSON.stringify(this.$root.layout))];
      } else {
        this.$root.sectionHistory.push(JSON.parse(JSON.stringify(this.$root.layout)));
      }
      this.$root.sectionRedo = [];
    },
    restoreHistory(event) {
      if (event.ctrlKey) {
        if (event.keyCode === 90) {
          this.undoFromHistory();
        } else if (event.keyCode === 89) {
          this.redoFromHistory();
        }
      }
    },
    undoFromHistory() {
      if (this.$root.sectionHistory?.length) {
        const layout = this.$root.sectionHistory.pop();
        this.$root.sectionRedo.push(JSON.parse(JSON.stringify(this.$root.layout)));
        this.$root.layout = layout;
      }
    },
    redoFromHistory() {
      if (this.$root.sectionRedo?.length) {
        const layout = this.$root.sectionRedo.pop();
        this.$root.sectionHistory.push(JSON.parse(JSON.stringify(this.$root.layout)));
        this.$root.layout = layout;
      }
    },
    resetHistory() {
      this.$root.sectionHistory = [];
      this.$root.sectionRedo = [];
    },
    saveDraft(layout) {
      this.resetHistory();

      this.loading++;
      const layoutToUpdate = this.$layoutUtils.cleanAttributes(layout || JSON.parse(JSON.stringify(this.$root.layout)), false, true);
      return this.$siteLayoutService.updateSiteLayout(this.$root.draftSiteType, this.$root.draftSiteName, layoutToUpdate, 'contentId')
        .then(layout => {
          this.setLayout(layout);
          this.$root.$emit('layout-draft-saved');
        })
        .catch(e => {
          this.$root.$emit('alert-message', this.$te(e.message) ? this.$t(e.message) : this.$t('layout.siteSavingError'), 'error');
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
