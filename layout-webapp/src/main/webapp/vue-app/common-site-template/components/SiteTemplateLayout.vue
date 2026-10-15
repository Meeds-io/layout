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
  <div>
    <div class="text-header mb-2">
      {{ $t('siteTemplates.preview') }}
    </div>
    <v-card
      ref="preview"
      class="d-flex border-color elevation-2 mx-auto pa-1 mb-4"
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
                  v-if="displayPreviewTitle"
                  ref="previewTitle"
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
    <div class="text-header mb-2">
      {{ $t('layout.adjustPortalPageWidth') }}
    </div>
    <v-radio-group
      v-model="width"
      class="ma-0 me-auto full-width text-no-wrap"
      mandatory>
      <v-radio
        :value="customWidth"
        class="mx-0">
        <template #label>
          <div class="d-flex full-width align-center">
            <span class="text-font-size">{{ $t('layout.fixedWidthCustom') }}</span>
            <number-input
              v-if="width === customWidth"
              v-model="width"
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
    <div class="text-header mb-2">
      {{ $t('layout.chooseWhatToDisplay') }}
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
  </div>
</template>
<script>
export default {
  props: {
    siteTemplate: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    displayPreviewTitle: true,
    topBanner: true,
    rightSidebar: true,
    internalRightSidebar: true,
    bottomBanner: true,
    leftSidebar: true,
    internalLeftSidebar: true,
    width: 1320,
    minWidth: 300,
    maxWidth: 5000,
  }),
  computed: {
    customWidth() {
      return this.width === '100%' ? 0 : this.width;
    },
  },
  methods: {
    async save() {
      const layout = {
        width: this.width,
      };
      this.$layoutUtils.newSite(layout);
      const middleContainer = layout?.children?.find(c => c.template === this.$layoutUtils.siteBodyMiddleTemplate);
      const middleCenterContainer = middleContainer?.children?.find(c => c.template === this.$layoutUtils.siteBodyMiddleCenterTemplate);
      const middleCenterContainerIndex = middleContainer?.children?.findIndex(c => c.template === this.$layoutUtils.siteBodyMiddleCenterTemplate);
      if (!this.topBanner) {
        middleContainer.children.splice(0, middleCenterContainerIndex);
      }
      if (!this.rightSidebar) {
        layout.children[2] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
      }
      if (!this.internalRightSidebar) {
        middleCenterContainer.children[2] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
      }
      if (!this.bottomBanner) {
        middleContainer.children.splice(middleCenterContainerIndex + 1, middleContainer.children.length - middleCenterContainerIndex);
      }
      if (!this.leftSidebar) {
        layout.children[0] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
      }
      if (!this.internalLeftSidebar) {
        middleCenterContainer.children[0] = this.$layoutUtils.newContainer(this.$layoutUtils.sidebarTemplate);
      }
      await this.$siteLayoutService.updateSiteLayout('portal_template', this.siteTemplate.layout, layout);
      await this.uploadIllustration();
    },
    async uploadIllustration() {
      this.displayPreviewTitle = false;
      await this.$nextTick();
      const previewCanvas = await window.html2canvas(this.$refs.preview.$el);
      const previewImage = previewCanvas.toDataURL('image/png');
      window.setTimeout(() => this.displayPreviewTitle = true, 200);

      const previewBlob = this.convertPreviewToFile(previewImage);
      const uploadId = await this.uploadFile(previewBlob);
      const report = await this.$fileAttachmentService.saveAttachments({
        objectType: 'siteTemplate',
        objectId: this.siteTemplate?.id,
        uploadedFiles: [{uploadId}],
        attachedFiles: [],
      });
      if (report?.errorByUploadId?.length) {
        const attachmentHtmlError = Object.values(report.errorByUploadId).join('<br>');
        this.$root.$emit('alert-message-html', attachmentHtmlError, 'error');
      }
    },
    async uploadFile(file) {
      if (file?.size) {
        if (file.size > this.maxUploadSize) {
          this.$root.$emit('alert-message', this.$t(this.$uploadService.avatarExcceedsLimitError), 'error');
          return;
        } else {
          this.sendingImage = true;
          return await this.$uploadService.upload(file);
        }
      }
    },
    convertPreviewToFile(previewImage) {
      const imgString = window.atob(previewImage.replace(/^data:image\/\w+;base64,/, ''));
      const bytes = new Uint8Array(imgString.length);
      for (let i = 0; i < imgString.length; i++) {
        bytes[i] = imgString.charCodeAt(i);
      }
      return new Blob([bytes], {type: 'image/png'});
    },
  },
};
</script>