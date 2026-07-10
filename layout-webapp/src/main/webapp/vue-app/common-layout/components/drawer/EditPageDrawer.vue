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
    id="editPagePropertiesDrawer"
    v-model="drawer"
    right
    disable-pull-to-refresh>
    <template #title>
      {{ $root.isSiteLayout && $t('layout.editSitePagesProperties') || $t('layout.editPageProperties') }}
    </template>
    <template v-if="drawer" #content>
      <v-card
        class="pa-4"
        flat>
        <v-card
          :class="fullWindow && 'px-1' || 'px-6'"
          :style="cssStyle"
          class="py-1"
          width="380px"
          flat>
          <div
            v-if="backgroundLayerStyle"
            class="layout-background-layer"
            :style="backgroundLayerStyle"></div>
          <v-img
            :src="pagePreview"
            width="100%"
            height="200"
            class="border-radius mx-auto"
            transition="none"
            eager
            cover />
        </v-card>
        <div class="text-title me-auto mt-4 mb-2">
          {{ $t('layout.siteDesign') }}
        </div>
        <layout-editor-background-input
          v-if="$root.isSiteLayout && parentContainer"
          ref="siteBackgroundInput"
          v-model="parentContainer"
          :default-background-color="defaultBackgroundColor"
          class="mt-2">
          <template #title>
            <div class="text-header me-auto mb-2">
              {{ $t('layout.siteBackground') }}
            </div>
          </template>
        </layout-editor-background-input>

        <div class="d-flex align-center mt-4">
          <div class="text-title me-auto mb-2">
            {{ $t('layout.globalPageDesign') }}
          </div>
        </div>
        <div class="d-flex flex-column mt-4">
          <div class="text-header mb-2">
            {{ $t('layout.updateSitePagesWidth') }}
          </div>
          <v-radio-group
            v-model="width"
            class="ms-0 mt-0 me-auto full-width text-no-wrap"
            mandatory>
            <v-radio
              :value="customWidth"
              class="mx-0">
              <template #label>
                <div class="d-flex full-width align-center">
                  <span class="text-font-size">{{ $t('layout.fixedWidthCustom') }}</span>
                  <number-input
                    v-model="width"
                    v-if="width === customWidth"
                    :label="$t('layout.fixedWidth')"
                    :min="minWidth"
                    :max="maxWidth"
                    :step="10"
                    class="ms-auto my-n2"
                    editable />
                </div>
              </template>
            </v-radio>
            <v-radio
              value="100%"
              class="mx-0">
              <template #label>
                <span class="text-font-size">{{ $t('layout.fullWindow') }}</span>
              </template>
            </v-radio>
          </v-radio-group>
        </div>
        <layout-editor-section-margin-input
          v-model="pageContainer"
          :max="80"
          :min="0"
          left
          right
          top
          bottom>
          <template #title>
            <div class="text-header mt-4">
              {{ $t('layout.pageMargins') }}
            </div>
          </template>
        </layout-editor-section-margin-input>
        <layout-editor-background-input
          v-if="pageContainer"
          ref="backgroundInput"
          v-model="pageContainer"
          :default-background-color="defaultBackgroundColor"
          class="mt-2">
          <template #title>
            <div class="text-header me-auto mb-2">
              {{ $t('layout.globalPageBackground') }}
            </div>
          </template>
        </layout-editor-background-input>

        <div class="d-flex align-center mt-4">
          <div class="text-title me-auto mb-2">
            {{ $t('layout.applicationStyling') }}
          </div>
        </div>
        <layout-editor-border-input
          ref="appBorderInput"
          v-model="pageContainer"
          class="mt-4"
          page-style />
        <layout-editor-border-radius-input
          ref="appBorderRadiusInput"
          v-model="pageContainer"
          class="mt-4"
          page-style />
        <layout-editor-background-input
          ref="appBackgroundInput"
          v-if="appBackgroundProperties"
          v-model="appBackgroundProperties"
          class="mt-4"
          page-style />
        <layout-editor-text-input
          ref="appTextInput"
          v-model="pageContainer"
          class="mt-4"
          page-style />
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
          :loading="saving"
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
    pagePreview: '/layout/images/page-templates/DefaultPreview.webp',
    defaultBackgroundColor: '#F2F2F2FF',
    layout: null,
    originalParentContainer: null,
    originalPageContainer: null,
    parentContainer: null,
    pageContainer: null,
    appBackgroundProperties: null,
    width: 1320,
    defaultWidth: 1320,
    minWidth: 300,
    maxWidth: 5000,
    drawer: false,
    saving: false,
    defaultMarginTop: 20,
    defaultMarginRight: 20,
    defaultMarginBottom: 20,
    defaultMarginLeft: 20,
  }),
  computed: {
    cssStyle() {
      return this.$applicationUtils.getStyle(this.pageContainer, {
        onlyBackgroundStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.$applicationUtils.getBackgroundLayerStyle(this.pageContainer, {});
    },
    customWidth() {
      return this.width === '100%' ? this.defaultWidth : this.width;
    },
    fullWindow() {
      return this.width === '100%';
    },
  },
  watch: {
    pageContainer() {
      if (this.drawer) {
        this.optionsModified = true;
      }
    },
  },
  created() {
    this.$root.$on('layout-page-properties-open', this.open);
    if (document.body.computedStyleMap && document.body.computedStyleMap().get('--allPagesLightGrey')) {
      this.defaultBackgroundColor = document.body.computedStyleMap().get('--allPagesLightGrey')[0] || this.defaultBackgroundColor;
    }
  },
  beforeDestroy() {
    this.$root.$off('layout-page-properties-open', this.open);
  },
  methods: {
    open(pageContainer, parentContainer) {
      this.originalParentContainer = parentContainer;
      this.originalPageContainer = pageContainer;
      this.parentContainer = parentContainer && Object.assign({...this.$layoutUtils.containerModel}, JSON.parse(JSON.stringify(parentContainer)));
      this.pageContainer = Object.assign({...this.$layoutUtils.containerModel}, JSON.parse(JSON.stringify(pageContainer)));
      if (this.pageContainer.marginTop !== 0 && !this.pageContainer.marginTop) {
        this.pageContainer.marginTop = this.defaultMarginTop;
      }
      if (this.pageContainer.marginRight !== 0 && !this.pageContainer.marginRight) {
        this.pageContainer.marginRight = this.defaultMarginRight;
      }
      if (this.pageContainer.marginBottom !== 0 && !this.pageContainer.marginBottom) {
        this.pageContainer.marginBottom = this.defaultMarginBottom;
      }
      if (this.pageContainer.marginLeft !== 0 && !this.pageContainer.marginLeft) {
        this.pageContainer.marginLeft = this.defaultMarginLeft;
      }
      this.width = (this.pageContainer.width === 'fullWindow' ? '100%' : this.pageContainer.width)
        || (this.pageContainer.width === 'singlePageApplication' ? this.defaultWidth : this.pageContainer.width)
        || this.defaultWidth;
      this.appBackgroundProperties = {
        storageId: 0,
        backgroundColor: this.pageContainer.appBackgroundColor || null,
        backgroundImage: this.pageContainer.appBackgroundImage || null,
        backgroundEffect: this.pageContainer.appBackgroundEffect || null,
        backgroundRepeat: this.pageContainer.appBackgroundRepeat || null,
        backgroundSize: this.pageContainer.appBackgroundSize || null,
      };
      this.$refs.drawer.open();
    },
    async apply() {
      this.saving = true;
      try {
        await this.$refs?.siteBackgroundInput?.apply?.();
        await this.$refs.backgroundInput.apply();
        await this.$refs.appBackgroundInput.apply();
        Object.assign(this.originalPageContainer, this.pageContainer);
        this.$set(this.originalPageContainer, 'appBackgroundColor', this.appBackgroundProperties.backgroundColor);
        this.$set(this.originalPageContainer, 'appBackgroundImage', this.appBackgroundProperties.backgroundImage);
        this.$set(this.originalPageContainer, 'appBackgroundEffect', this.appBackgroundProperties.backgroundEffect);
        this.$set(this.originalPageContainer, 'appBackgroundRepeat', this.appBackgroundProperties.backgroundRepeat);
        this.$set(this.originalPageContainer, 'appBackgroundSize', this.appBackgroundProperties.backgroundSize);
        this.$set(this.originalPageContainer, 'width', this.width);

        if (this.pageContainer.marginTop === this.defaultMarginTop) {
          this.pageContainer.marginTop = null;
        }
        if (this.pageContainer.marginRight === this.defaultMarginRight) {
          this.pageContainer.marginRight = null;
        }
        if (this.pageContainer.marginBottom === this.defaultMarginBottom) {
          this.pageContainer.marginBottom = null;
        }
        if (this.pageContainer.marginLeft === this.defaultMarginLeft) {
          this.pageContainer.marginLeft = null;
        }

        this.$layoutUtils.applyContainerStyle(this.originalPageContainer, this.originalPageContainer);
        if (this.parentContainer) {
          Object.assign(this.originalParentContainer, {
            ...this.parentContainer,
            children: this.originalParentContainer.children,
          });
        }
        this.$root.pageFullWindow = this.fullWindow;
        this.$root.$emit('layout-editor-page-design-updated');
        this.close();
      } finally {
        this.saving = false;
      }
    },
    close() {
      this.$refs.drawer.close();
    },
  },
};
</script>