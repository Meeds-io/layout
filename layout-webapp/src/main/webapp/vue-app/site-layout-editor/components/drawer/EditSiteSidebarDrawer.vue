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
  <exo-drawer
    id="editSiteSectionsDrawer"
    ref="drawer"
    v-model="drawer"
    right>
    <template #title>
      {{ $t('layout.editSiteSidebarSection') }}
    </template>
    <template v-if="drawer" #content>
      <v-card
        class="ma-4 d-flex flex-column"
        flat
        max-width="100%">
        <div class="text-header mb-2">
          {{ $t('layout.editSiteSidebarSection.label.setDisplay') }}
        </div>
        <div class="d-flex align-center mb-2">
          <div class="me-auto">
            {{ $t('layout.editSiteSidebarSection.label.sectionMaxWidth') }}
          </div>
          <number-input
            v-model="width"
            class="ms-auto my-n2"
            editable
            :max="1000"
            :min="50"
            :step="10" />
        </div>
        <div class="d-flex align-center ms-n1 mb-4">
          <v-checkbox
            v-model="hiddenOnMobile"
            class="my-0 ml-n2px"
            :label="$t('layout.sectionHiddenOnMobile')"
            off-icon="far fa-square"
            on-icon="fa-check-square" />
        </div>
        <div class="text-header mb-4">
          {{ $t('layout.editSiteSidebarSection.label.updateStyle') }}
        </div>
        <div class="d-flex align-center mb-2">
          <div class="me-auto mb-2">
            {{ $t('layout.fixPositionWhenScrolling') }}
          </div>
          <v-switch
            v-model="stickySection"
            class="ms-auto my-auto me-n2" />
        </div>
        <layout-editor-background-input
          ref="backgroundInput"
          v-model="container"
          class="mb-4"
          :scroll-color="stickySection"
          text-bold />
        <layout-editor-text-input
          ref="textInput"
          v-model="container"
          class="mb-4"
          text-bold />
        <layout-editor-section-margin-input
          ref="marginInput"
          v-model="container"
          :bottom="false"
          class="mb-2"
          left
          :max="60"
          :min="-60"
          right
          text-bold
          :top="false" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          color="error"
          :disabled="saving"
          outlined
          :title="$t('layout.deleteSection')"
          @click="removeSection">
          {{ $t('layout.delete') }}
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn"
          :disabled="saving"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
          class="btn btn-primary ms-4"
          :loading="saving"
          @click="apply">
          <span class="text-none">{{ $t('layout.apply') }}</span>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      stickySection: false,
      hiddenOnMobile: false,
      saving: false,
      width: null,
      container: null,
      parentId: null,
    }),
    computed: {
      sidebarContainer () {
        return this.parentId && eXo.$layoutUtils.getContainerById(this.$root.layout, this.parentId);
      },
    },
    created () {
      this.$root.$on('layout-site-sidebar-section-open', this.open);
    },
    beforeUnmount () {
      this.$root.$off('layout-site-sidebar-section-open', this.open);
    },
    methods: {
      open (container, parentId) {
        this.container = JSON.parse(JSON.stringify(container));
        this.parentId = parentId;
        this.width = this.container.width || 310;
        this.hiddenOnMobile = this.container.cssClass?.includes?.('hidden-sm-and-down');
        this.stickySection = this.sidebarContainer?.cssClass?.includes?.('layout-sticky-section');
        this.$refs.drawer.open();
      },
      removeSection () {
        this.$root.$emit('layout-section-history-add');
        this.sidebarContainer.cssClass = null;
        this.sidebarContainer.children = [];
        this.close();
      },
      async apply () {
        this.saving = true;
        try {
          this.$root.$emit('layout-section-history-add');
          this.$set(this.container, 'width', this.width);
          await this.$refs.backgroundInput.apply();
          const container = eXo.$layoutUtils.getContainerById(this.$root.layout, this.container.storageId);
          Object.assign(container, this.container);
          this.container.hiddenOnMobile = this.hiddenOnMobile;
          eXo.$layoutUtils.applyContainerStyle(container, this.container);
          this.$set(this.container, 'cssClass', this.container?.cssClass?.trim() || '');
          if (this.hiddenOnMobile && !this.container.cssClass?.includes?.('hidden-sm-and-down')) {
            this.$set(this.container, 'cssClass', this.container.cssClass ? `${this.container.cssClass} hidden-sm-and-down` : 'hidden-sm-and-down');
          } else if (!this.hiddenOnMobile && this.container.cssClass?.includes?.('hidden-sm-and-down')) {
            this.$set(this.container, 'cssClass', this.container.cssClass.replace('hidden-sm-and-down', ''));
          }
          if (this.stickySection && !this.sidebarContainer.cssClass?.includes?.('layout-sticky-section')) {
            this.sidebarContainer.cssClass = this.sidebarContainer.cssClass ? `${this.sidebarContainer.cssClass} layout-sticky-section` : 'layout-sticky-section';
          } else if (!this.stickySection && this.sidebarContainer.cssClass?.includes?.('layout-sticky-section')) {
            this.sidebarContainer.cssClass = this.sidebarContainer.cssClass.replace('layout-sticky-section', '');
          }
          this.close();
        } finally {
          this.saving = false;
        }
      },
      close () {
        this.$refs.drawer.close();
      },
    },
  };
</script>