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
    ref="drawer"
    id="editSiteSectionsDrawer"
    v-model="drawer"
    right>
    <template #title>
      {{ $t('layout.manageSiteSections') }}
    </template>
    <template v-if="drawer" #content>
      <v-card
        max-width="100%"
        class="ma-4 d-flex flex-column"
        flat>
        <div class="mb-2">
          {{ $t('layout.manageSiteSections.description1') }}
        </div>
        <div class="mb-4">
          {{ $t('layout.manageSiteSections.description2') }}
        </div>
        <div class="text-header mb-2">
          {{ $t('layout.manageSiteSections.label.preview') }}
        </div>
        <v-card
          class="d-flex border-color elevation-2 mx-auto mb-4 pa-1"
          max-width="335px"
          width="100%"
          flat>
          <div class="d-flex full-width page-background">
            <v-card
              :width="leftSidebar && 50"
              class="flex-grow-0 flex-shrink-0 grey-lighten1-background opacity-7 no-border-radius"
              flat
              @click="leftSidebar = !leftSidebar" />
            <v-card
              class="d-flex flex-column flex-grow-1 flex-shrink-1 no-border-radius"
              color="transparent"
              flat>
              <v-card
                :height="topBanner && 20 || 0"
                class="no-border-radius grey-lighten1-background opacity-8"
                flat
                @click="topBanner = !topBanner" />
              <div class="d-flex">
                <v-card
                  :width="internalLeftSidebar && 50 || 0"
                  class="flex-grow-0 flex-shrink-0 no-border-radius grey-lighten1-background opacity-9 me-auto"
                  flat
                  @click="internalLeftSidebar = !internalLeftSidebar" />
                <v-card
                  class="position-relative d-flex flex-column flex-grow-1 flex-shrink-1"
                  style="zoom: 30%"
                  color="transparent"
                  width="100%"
                  height="500px"
                  flat>
                  <v-card
                    class="flex-grow-1 overflow-hidden ma-5"
                    flat>
                    <v-card
                      class="d-flex align-center justify-center text-title d-flex position-absolute z-index-one t-0 fa-rotate-315 ms-n12 mt-12"
                      color="primary"
                      min-height="30"
                      min-width="220"
                      dark
                      flat>
                      {{ $t('layout.editSite.portalPage') }}
                    </v-card>
                  </v-card>
                </v-card>
                <v-card
                  :width="internalRightSidebar && 50 || 0"
                  class="flex-grow-0 flex-shrink-0 no-border-radius grey-lighten1-background opacity-9 ms-auto"
                  flat
                  @click="internalRightSidebar = !internalRightSidebar" />
              </div>
              <v-card
                :height="bottomBanner && 20 || 0"
                class="no-border-radius grey-lighten1-background opacity-8"
                flat
                @click="bottomBanner = !bottomBanner" />
            </v-card>
            <v-card
              :width="rightSidebar && 50"
              class="flex-grow-0 flex-shrink-0 grey-lighten1-background opacity-7 no-border-radius"
              flat
              @click="rightSidebar = !rightSidebar" />
          </div>
        </v-card>
        <div class="text-header mb-4">
          {{ $t('layout.manageSiteSections.label.sectionsChoice') }}
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-9 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.leftSidebar') }}
          </div>
          <v-switch
            v-model="leftSidebar"
            class="ms-auto my-auto me-n2" />
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-7 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.internalLeftSidebar') }}
          </div>
          <v-switch
            v-model="internalLeftSidebar"
            class="ms-auto my-auto me-n2" />
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-8 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.topBanner') }}
          </div>
          <v-switch
            v-model="topBanner"
            class="ms-auto my-auto me-n2" />
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-9 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.rightSidebar') }}
          </div>
          <v-switch
            v-model="rightSidebar"
            class="ms-auto my-auto me-n2" />
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-7 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.internalRightSidebar') }}
          </div>
          <v-switch
            v-model="internalRightSidebar"
            class="ms-auto my-auto me-n2" />
        </div>
        <div class="d-flex align-center mb-2">
          <v-card
            class="flex-grow-0 flex-shrink-0 grey-background rounded-lg grey-lighten1-background opacity-8 me-2 mt-n2"
            height="25"
            width="25" />
          <div class="font-weight-bold me-auto mb-2">
            {{ $t('layout.manageSiteSections.label.bottomBanner') }}
          </div>
          <v-switch
            v-model="bottomBanner"
            class="ms-auto my-auto me-n2" />
        </div>
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
          class="btn btn-primary ms-4"
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
    /* Switch button */
    topBanner: false,
    rightSidebar: false,
    internalRightSidebar: false,
    bottomBanner: false,
    leftSidebar: false,
    internalLeftSidebar: false,
    /* Hover */
    topBannerHover: false,
    rightSidebarHover: false,
    internalRightSidebarHover: false,
    bottomBannerHover: false,
    leftSidebarHover: false,
    internalLeftSidebarHover: false,
  }),
  created() {
    this.$root.$on('layout-site-sections-open', this.open);
  },
  beforeDestroy() {
    this.$root.$off('layout-site-sections-open', this.open);
  },
  methods: {
    open() {
      const options = this.getCurrentOptions();
      this.topBanner = options.top;
      this.rightSidebar = options.right;
      this.internalRightSidebar = options.internalRight;
      this.bottomBanner = options.bottom;
      this.leftSidebar = options.left;
      this.internalLeftSidebar = options.internalLeft;
      this.$refs.drawer.open();
    },
    apply() {
      this.$root.$emit('layout-section-history-add');
      const options = this.getCurrentOptions();
      if (options.top !== this.topBanner) {
        if (this.topBanner) {
          this.$root.middleContainer.children.unshift({
            ...this.$layoutUtils.newContainer(this.$layoutUtils.bannerTemplate),
            children: [
              this.$layoutUtils.newContainer(this.$layoutUtils.bannerCellTemplate),
            ]
          });
        } else {
          this.$root.middleContainer.children.splice(0, this.$root.middleCenterContainerIndex);
        }
      }
      if (options.right !== this.rightSidebar) {
        if (this.rightSidebar) {
          this.$root.layout.children[2] = {
            ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate),
            children: [{
              ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarCellTemplate),
              width: 310
            }]
          };
        } else {
          this.$root.layout.children[2] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
        }
      }
      if (options.internalRight !== this.internalRightSidebar) {
        if (this.internalRightSidebar) {
          this.$root.middleCenterContainer.children[2] = {
            ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate),
            children: [{
              ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarCellTemplate),
              width: 310
            }]
          };
        } else {
          this.$root.middleCenterContainer.children[2] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
        }
      }
      if (options.bottom !== this.bottomBanner) {
        if (this.bottomBanner) {
          this.$root.middleContainer.children.push({
            ...this.$layoutUtils.newContainer(this.$layoutUtils.bannerTemplate),
            children: [
              this.$layoutUtils.newContainer(this.$layoutUtils.bannerCellTemplate),
            ]
          });
        } else {
          this.$root.middleContainer.children.splice(this.$root.middleCenterContainerIndex + 1, this.$root.middleContainer.children.length - this.$root.middleCenterContainerIndex);
        }
      }
      if (options.left !== this.leftSidebar) {
        if (this.leftSidebar) {
          this.$root.layout.children[0] = {
            ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate),
            children: [{
              ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarCellTemplate),
              width: 310
            }]
          };
        } else {
          this.$root.layout.children[0] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
        }
      }
      if (options.internalLeft !== this.internalLeftSidebar) {
        if (this.internalLeftSidebar) {
          this.$root.middleCenterContainer.children[0] = {
            ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate),
            children: [{
              ...this.$layoutUtils.newContainer(this.$layoutUtils.sidebarCellTemplate),
              width: 310
            }]
          };
        } else {
          this.$root.middleCenterContainer.children[0] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
        }
      }
      this.$root.layout.children = this.$root.layout.children.slice();
      this.$root.middleContainer.children = this.$root.middleContainer.children.slice();
      this.$root.middleCenterContainer.children = this.$root.middleCenterContainer.children.slice();
      this.close();
    },
    getCurrentOptions() {
      return {
        top: !!this.$root.middleContainer?.children?.[0]?.children?.length && this.$root.middleContainer?.children?.[0]?.template === this.$layoutUtils.bannerTemplate,
        right: !!this.$root.rightContainer?.children?.length,
        internalRight: !!this.$root.internalRightContainer?.children?.length,
        bottom: !!this.$root.middleContainer?.children?.find?.((c, index) => index > this.$root.middleCenterContainerIndex && c?.children?.length && c.template === this.$layoutUtils.bannerTemplate),
        left: !!this.$root.leftContainer?.children?.length,
        internalLeft: !!this.$root.internalLeftContainer?.children?.length,
      };
    },
    close() {
      this.$refs.drawer.close();
    },
  },
};
</script>