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
    id="editApplicationDrawer"
    v-model="drawer"
    allow-expand
    right
    @closed="reset">
    <template #title>
      <span :title="drawerTitle" class="text-truncate">
        {{ drawerTitle }}
      </span>
    </template>
    <template v-if="canEditPortletProperties" #titleIcons>
      <v-btn
        icon
        @click="$root.$emit('layout-editor-portlet-edit', applicationId, applicationTitle)">
        <v-icon size="20">fa-edit</v-icon>
      </v-btn>
    </template>
    <template v-if="drawer" #content>
      <v-card
        max-width="100%"
        class="ma-4"
        flat>
        <div class="d-flex align-center">
          <div class="text-title mb-2">
            {{ $t('layout.applicationStyling') }}
          </div>
        </div>
        <layout-editor-margin-input
          v-if="initialized"
          ref="marginInput"
          v-model="container"
          :max="60"
          :min="-60"
          :diff="0"
          class="mt-4"
          @refresh="refresh++" />
        <layout-editor-border-input
          v-if="initialized"
          ref="borderInput"
          v-model="container"
          class="mt-4"
          @refresh="refresh++" />
        <layout-editor-border-radius-input
          v-if="initialized"
          ref="borderRadiusInput"
          v-model="container"
          class="mt-4"
          @refresh="refresh++" />
        <layout-editor-background-input
          v-if="initialized"
          ref="backgroundInput"
          v-model="backgroundProperties"
          immediate-save
          class="mt-4"
          @refresh="refresh++" />
        <layout-editor-text-input
          v-if="initialized"
          ref="textInput"
          v-model="container"
          class="mt-4"
          @refresh="refresh++" />
        <div class="d-flex align-center mt-4">
          <div class="text-title mb-2">
            {{ $t('layout.advancedOptions') }}
          </div>
        </div>
        <template v-if="isDynamicSection">
          <div class="d-flex align-center ms-n1">
            <v-checkbox
              v-model="fixedHeight"
              :label="$t('layout.fixedHeight')"
              on-icon="fa-check-square"
              off-icon="far fa-square"
              class="my-0 ml-n2px" />
          </div>
          <div v-if="fixedHeight" class="d-flex flex-column justify-center">
            <v-radio-group
              v-model="height"
              class="my-auto text-no-wrap ms-7"
              mandatory>
              <v-radio
                value="150px"
                class="mx-0">
                <template #label>
                  <span class="text-font-size">{{ $t('layout.fixedHeight150') }}</span>
                </template>
              </v-radio>
              <v-radio
                value="300px"
                class="mx-0">
                <template #label>
                  <span class="text-font-size">{{ $t('layout.fixedHeight300') }}</span>
                </template>
              </v-radio>
              <v-radio
                value="500px"
                class="mx-0">
                <template #label>
                  <span class="text-font-size">{{ $t('layout.fixedHeight500') }}</span>
                </template>
              </v-radio>
              <v-radio
                :value="customHeight"
                class="mx-0">
                <template #label>
                  <div class="d-flex full-width align-center">
                    <span class="text-font-size">{{ $t('layout.fixedHeightCustom') }}</span>
                    <number-input
                      v-model="height"
                      v-if="height === customHeight"
                      :label="$t('layout.fixedHeight')"
                      :min="minHeight"
                      :max="maxHeight"
                      :step="10"
                      class="ms-auto my-n2"
                      editable
                      @valid="invalidCustomHeight = !$event" />
                  </div>
                </template>
              </v-radio>
            </v-radio-group>
            <span v-if="invalidCustomHeight" class="error-color mt-n1 ms-8">{{ $t('layout.invalidFixedHeight', {
              0: minHeightFormatted,
              1: maxHeightFormatted,
            }) }}</span>
          </div>
        </template>
        <template v-else>
          <div class="d-flex align-center ms-n1">
            <v-checkbox
              v-model="fixedWidth"
              :label="$t('layout.fixedWidth')"
              on-icon="fa-check-square"
              off-icon="far fa-square"
              class="my-0 ml-n2px" />
          </div>
          <div v-if="fixedWidth" class="d-flex flex-column justify-center">
            <v-radio-group
              v-model="width"
              class="my-auto text-no-wrap ms-7"
              mandatory>
              <v-radio
                value="fit-content"
                class="mx-0">
                <template #label>
                  <span class="text-font-size">{{ $t('layout.fixedWidthAutoFill') }}</span>
                </template>
              </v-radio>
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
                      editable
                      @valid="invalidCustomWidth = !$event" />
                  </div>
                </template>
              </v-radio>
            </v-radio-group>
            <span v-if="invalidCustomWidth" class="error-color mt-n1 ms-8">{{ $t('layout.invalidFixedWidth', {
              0: minWidthFormatted,
              1: maxWidthFormatted,
            }) }}</span>
          </div>
        </template>
        <div class="d-flex align-center mt-2">
          {{ $t('layout.viewOptions') }}
        </div>
        <div class="d-flex flex-column justify-center mb-4">
          <v-radio-group
            v-model="viewOption"
            class="my-auto text-no-wrap ms-n1 mt-2"
            mandatory>
            <v-radio value="both" class="mx-0">
              <template #label>
                <span class="text-font-size text-color">{{ $t('layout.appDisplayBoth') }}</span>
              </template>
            </v-radio>
            <v-radio value="hideDesktop" class="mx-0">
              <template #label>
                <span class="text-font-size text-color">{{ $t('layout.appHiddenOnDesktop') }}</span>
              </template>
            </v-radio>
            <v-radio value="hideMobile" class="mx-0">
              <template #label>
                <span class="text-font-size text-color">{{ $t('layout.hiddenOnMobile') }}</span>
              </template>
            </v-radio>
          </v-radio-group>
        </div>
        <template v-if="!isDynamicSection">
          <div class="text-header mb-2">
            {{ $t('layout.alignApp') }}
          </div>
          <div class="d-flex">
            <div class="col-6 pa-0">
              <v-radio-group v-model="hAlign" class="ma-0">
                <v-radio
                  value="START"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignLeft') }}</span>
                  </template>
                </v-radio>
                <v-radio
                  value="CENTER"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignCenter') }}</span>
                  </template>
                </v-radio>
                <v-radio
                  value="END"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignRight') }}</span>
                  </template>
                </v-radio>
              </v-radio-group>
            </div>
            <div class="col-6 pa-0">
              <v-radio-group v-model="vAlign" class="ma-0">
                <v-radio
                  value="START"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignTop') }}</span>
                  </template>
                </v-radio>
                <v-radio
                  value="CENTER"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignMiddle') }}</span>
                  </template>
                </v-radio>
                <v-radio
                  value="END"
                  class="ma-0 pa-0">
                  <template #label>
                    <span class="text-body">{{ $t('layout.alignBottom') }}</span>
                  </template>
                </v-radio>
              </v-radio-group>
            </div>
          </div>
        </template>
      </v-card>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    initialized: false,
    fixedHeight: false,
    fixedWidth: false,
    customHeightValue: false,
    customWidthValue: false,
    hAlign: 'CENTER',
    vAlign: 'CENTER',
    height: null,
    minHeight: 100,
    maxHeight: 1000,
    width: null,
    minWidth: 0,
    maxWidth: 1000,
    invalidCustomHeight: false,
    invalidCustomWidth: false,
    viewOption: 'both',
    section: null,
    container: null,
    backgroundProperties: null,
    applicationTitle: null,
    refresh: 1,
  }),
  computed: {
    applicationId() {
      return this.container?.children?.[0]?.storageId || this.container?.storageId;
    },
    applicationContentId() {
      return this.container?.children?.[0]?.contentId || this.container?.contentId;
    },
    application() {
      return this.$root.portletInstances?.find?.(a => a?.contentId === this.applicationContentId);
    },
    supportedModes() {
      return this.application?.supportedModes || [];
    },
    canEditPortletProperties() {
      return this.supportedModes.find(m => m === 'edit');
    },
    sectionId() {
      return this.section?.storageId;
    },
    isDynamicSection() {
      return this.section?.template === this.$layoutUtils.sidebarCellTemplate;
    },
    customHeight() {
      return (!this.height || this.height === '150px' || this.height === '300px' || this.height === '500px') ? 400 : this.height;
    },
    customWidth() {
      return (!this.width || this.width === 'fit-content') ? 400 : this.width;
    },
    drawerTitle() {
      return this.$t('layout.editApplicationTitleNoCategory', {
        0: this.applicationTitle,
      });
    },
    minHeightFormatted() {
      if (this.minHeight === 0 || this.minHeight) {
        return new Intl.NumberFormat(eXo.env.portal.language, {
          style: 'decimal',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(this.minHeight);
      }
      return null;
    },
    maxHeightFormatted() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.maxHeight);
    },
    minWidthFormatted() {
      if (this.minWidth === 0 || this.minWidth) {
        return new Intl.NumberFormat(eXo.env.portal.language, {
          style: 'decimal',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(this.minWidth);
      }
      return null;
    },
    maxWidthFormatted() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.maxWidth);
    },
    styleClasses() {
      return this.drawer && this.refresh > 0 && this.sectionId && {
        marginTop: this.container?.marginTop,
        marginRight: this.container?.marginRight,
        marginBottom: this.container?.marginBottom,
        marginLeft: this.container?.marginLeft,
        radiusTopRight: this.container?.radiusTopRight,
        radiusTopLeft: this.container?.radiusTopLeft,
        radiusBottomRight: this.container?.radiusBottomRight,
        radiusBottomLeft: this.container?.radiusBottomLeft,
        borderColor: this.container?.borderColor,
        borderSize: this.container?.borderColor && this.container?.borderSize || null,
        boxShadow: this.container?.boxShadow && 'true' || null,
        textTitleColor: this.container?.textTitleColor || null,
        textTitleFontSize: this.container?.textTitleFontSize || null,
        textTitleFontWeight: this.container?.textTitleFontWeight || null,
        textTitleFontStyle: this.container?.textTitleFontStyle || null,
        textHeaderColor: this.container?.textHeaderColor || null,
        textHeaderFontSize: this.container?.textHeaderFontSize || null,
        textHeaderFontWeight: this.container?.textHeaderFontWeight || null,
        textHeaderFontStyle: this.container?.textHeaderFontStyle || null,
        textColor: this.container?.textColor || null,
        textFontSize: this.container?.textFontSize || null,
        textFontWeight: this.container?.textFontWeight || null,
        textFontStyle: this.container?.textFontStyle || null,
        textSubtitleColor: this.container?.textSubtitleColor || null,
        textSubtitleFontSize: this.container?.textSubtitleFontSize || null,
        textSubtitleFontWeight: this.container?.textSubtitleFontWeight || null,
        textSubtitleFontStyle: this.container?.textSubtitleFontStyle || null,
        height: this.container?.height || null,
        width: this.container?.width || null,
        ...this.backgroundProperties,
        hiddenOnMobile: this.viewOption === 'hideMobile',
        hiddenOnDesktop: this.viewOption === 'hideDesktop',
      } || null;
    },
  },
  watch: {
    drawer(value, oldVal) {
      if (value && !oldVal && this.styleClasses) {
        this.$layoutUtils.applyContainerStyle(this.container, this.styleClasses);
      }
    },
    styleClasses(value, oldVal) {
      if (value && oldVal) {
        this.$root.$emit('layout-section-history-add', this.sectionId);
        this.$layoutUtils.applyContainerStyle(this.container, this.styleClasses);
        this.$root.$emit('layout-section-application-update-style', this.container);
      } else if (value && !oldVal) {
        this.initialized = true;
      }
    },
    fixedHeight(value) {
      if (value) {
        this.height = this.height || '150px';
      } else {
        this.height = null;
      }
    },
    fixedWidth(value) {
      if (value) {
        this.width = this.width || 'fit-content';
      } else {
        this.width = null;
      }
    },
    height(value) {
      if (this.initialized) {
        this.container.height = value;
        this.refresh++;
      }
    },
    width(value) {
      if (this.initialized) {
        this.container.width = value;
        this.refresh++;
      }
    },
    vAlign() {
      if (this.drawer && this.vAlign) {
        this.container.cssClass = this.container.cssClass?.replace?.('application-align-v-end', '') || '';
        this.container.cssClass = this.container.cssClass.replace('application-align-v-center', '');
        this.$set(this.container, 'cssClass', this.container.cssClass.trim());
        if (this.vAlign === 'END') {
          this.$set(this.container, 'cssClass', `${this.container.cssClass} application-align-v-end`);
        } else if (this.vAlign === 'CENTER') {
          this.$set(this.container, 'cssClass', `${this.container.cssClass} application-align-v-center`);
        }
        this.refresh++;
      }
    },
    hAlign() {
      if (this.drawer && this.hAlign) {
        this.container.cssClass = this.container.cssClass?.replace?.('application-align-h-end', '') || '';
        this.container.cssClass = this.container.cssClass.replace('application-align-h-center', '');
        this.$set(this.container, 'cssClass', this.container.cssClass.trim());
        if (this.hAlign === 'END') {
          this.$set(this.container, 'cssClass', `${this.container.cssClass} application-align-h-end`);
        } else if (this.hAlign === 'CENTER') {
          this.$set(this.container, 'cssClass', `${this.container.cssClass} application-align-h-center`);
        }
        this.refresh++;
      }
    },
  },
  methods: {
    open(section, container, applicationTitle) {
      this.initialized = false;
      this.$layoutUtils.parseContainerStyle(container);
      this.section = section;
      this.container = container;
      this.height = container.height;
      this.width = container.width;
      this.$set(container, 'cssClass', container?.cssClass?.trim?.() || '');
      if (container.cssClass?.includes?.('hidden-sm-and-down')) {
        this.viewOption = 'hideMobile';
      } else if (container.cssClass?.includes?.('hidden-md-and-up')) {
        this.viewOption = 'hideDesktop';
      } else {
        this.viewOption = 'both';
      }
      if (this.container.cssClass?.includes?.('application-align-v-end')) {
        this.vAlign = 'END';
      } else if (this.container.cssClass?.includes?.('application-align-v-center')) {
        this.vAlign = 'CENTER';
      } else {
        this.vAlign = 'START';
      }
      if (this.container.cssClass?.includes?.('application-align-h-end')) {
        this.hAlign = 'END';
      } else if (this.container.cssClass?.includes?.('application-align-h-center')) {
        this.hAlign = 'CENTER';
      } else {
        this.hAlign = 'START';
      }
      this.fixedHeight = !!this.height;
      this.fixedWidth = !!this.width;
      this.applicationTitle = applicationTitle;

      this.backgroundProperties = {
        storageId: this.container.storageId,
        cssClass: this.container.cssClass || '',
        backgroundColor: this.container.backgroundColor || null,
        backgroundImage: this.container.backgroundImage || null,
        backgroundEffect: this.container.backgroundEffect || null,
        backgroundRepeat: this.container.backgroundRepeat || null,
        backgroundSize: this.container.backgroundSize || null,
      };

      this.$nextTick(() => this.$refs.drawer.open());
    },
    reset() {
      window.setTimeout(() => {
        this.initialized = false;
        this.fixedHeight = false;
        this.fixedWidth = false;
        this.customHeightValue = false;
        this.customWidthValue = false;
        this.height = null;
        this.width = null;
        this.minHeight = 100;
        this.maxHeight = 1000;
        this.minWidth = 0;
        this.maxWidth = 1000;
        this.invalidCustomHeight = false;
        this.invalidCustomWidth = false;
        this.viewOption = 'both';
        this.section = null;
        this.container = null;
        this.backgroundProperties = null;
        this.applicationTitle = null;
        this.refresh = 1;
      }, 200);
    },
    close() {
      this.$refs.drawer.close();
    },
  },
};
</script>