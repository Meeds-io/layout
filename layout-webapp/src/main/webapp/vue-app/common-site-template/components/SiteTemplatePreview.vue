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
  <div class="pt-4">
    <div class="d-flex pb-2">
      <div class="text-header">
        {{ $t('siteTemplates.preview') }}
      </div>
      <v-spacer />
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <div
            v-bind="attrs"
            v-on="on">
            <v-file-input
              id="imageFileInput"
              ref="uploadInput"
              accept="image/*"
              class="file-selector pa-0 ma-0"
              clearable
              dense
              prepend-icon="fas fa-camera z-index-two rounded-circle primary-border-color white py-1 ms-3"
              rounded
              @change="uploadFile" />
          </div>
        </template>
        <span>{{ $t('siteTemplates.uploadPreviewTitle') }}</span>
      </v-tooltip>
    </div>
    <v-img
      class="border-radius"
      eager
      :lazy-src="illustrationSrc"
      :src="illustrationSrc"
      transition="none" />
  </div>
</template>
<script>
  export default {
    props: {
      value: {
        type: String,
        default: null,
      },
      previewImage: {
        type: String,
        default: null,
      },
      siteTemplateId: {
        type: Number,
        default: null,
      },
    },
    data: () => ({
      sendingImage: false,
      avatarData: null,
      attachments: null,
    }),
    computed: {
      illustrationId () {
        return this.attachments?.[0]?.id;
      },
      illustrationSrc () {
        return this.avatarData || this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/siteTemplate/${this.siteTemplateId}/${this.illustrationId}` || '/layout/images/site-templates/defaultSiteTemplatePreview.png';
      },
    },
    watch: {
      sendingImage () {
        this.$emit('sending', this.sendingImage);
      },
      previewImage () {
        this.avatarData = this.previewImage;
      },
    },
    created () {
      this.init();
    },
    methods: {
      async init () {
        if (this.previewImage) {
          this.avatarData = this.previewImage;
          this.attachments = null;
        } else if (this.siteTemplateId) {
          const data = await eXo.$fileAttachmentService.getAttachments('siteTemplate', this.siteTemplateId);
          this.attachments = data?.attachments || [];
          this.avatarData = null;
        }
      },
      uploadFile (file) {
        if (file && file.size) {
          if (file.size > this.maxUploadSize) {
            this.$root.$emit('alert-message', this.$t(eXo.$uploadService.avatarExcceedsLimitError), 'error');
            return;
          }
          this.sendingImage = true;
          const thiss = this;
          return eXo.$uploadService.upload(file)
            .then(uploadId => {
              const reader = new FileReader();
              reader.onload = e => {
                thiss.avatarData = e.target.result;
                thiss.$forceUpdate();
              };
              reader.readAsDataURL(file);
              this.$emit('input', uploadId);
            })
            .then(() => this.$emit('refresh'))
            .catch(() => this.$root.$emit('alert-message', this.$t('layout.errorUploadingPreview'), 'error'))
            .finally(() => this.sendingImage = false);
        }
      },
      async save (siteTemplateId) {
        if (this.value) {
          const report = await eXo.$fileAttachmentService.saveAttachments({
            objectType: 'siteTemplate',
            objectId: this.siteTemplateId || siteTemplateId,
            uploadedFiles: [{ uploadId: this.value }],
            attachedFiles: [],
          });
          if (report?.errorByUploadId?.length) {
            const attachmentHtmlError = Object.values(report.errorByUploadId).join('<br>');
            this.$root.$emit('alert-message-html', attachmentHtmlError, 'error');
          } else {
            await this.init();
            if (this.$refs.uploadInput) {
              this.$refs.uploadInput.reset();
            }
          }
        }
      },
    },
  };
</script>