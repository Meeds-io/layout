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
  <div class="pt-4">
    <div class="d-flex pb-2">
      <div class="text-subtitle-1">
        {{ $t('layout.templatePreview') }}
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
        <span>{{ $t('layout.uploadPreviewTitle') }}</span>
      </v-tooltip>
    </div>
    <v-img
      class="border-radius"
      contain
      eager
      :lazy-src="illustrationSrc"
      max-height="205"
      max-width="100%"
      position="top"
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
      templateId: {
        type: Number,
        default: null,
      },
      duplicate: {
        type: Boolean,
        default: false,
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
        return this.avatarData || this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/pageTemplate/${this.templateId}/${this.illustrationId}` || '/layout/images/page-templates/DefaultPreview.webp';
      },
    },
    watch: {
      sendingImage () {
        this.$emit('sending', this.sendingImage);
      },
    },
    created () {
      this.init();
    },
    methods: {
      init () {
        if (this.templateId) {
          return eXo.$fileAttachmentService.getAttachments('pageTemplate', this.templateId)
            .then(data => this.attachments = data?.attachments || [])
            .then(() => {
              if (this.duplicate && this.illustrationId) {
                return this.getIllustrationFile().then(this.uploadFile);
              }
            });
        }
      },
      uploadFile (file) {
        if (file?.size) {
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
      save () {
        if (this.value) {
          return eXo.$fileAttachmentService.saveAttachments({
            objectType: 'pageTemplate',
            objectId: this.templateId,
            uploadedFiles: [{ uploadId: this.value }],
            attachedFiles: [],
          }).then(report => {
            if (report?.errorByUploadId?.length) {
              const attachmentHtmlError = Object.values(report.errorByUploadId).join('<br>');
              this.$root.$emit('alert-message-html', attachmentHtmlError, 'error');
            } else {
              return this.init()
                .then(() => {
                  if (this.$refs.uploadInput) {
                    this.$refs.uploadInput.reset();
                  }
                });
            }
          });
        }
      },
      getIllustrationFile () {
        return fetch(this.illustrationSrc, {
          'method': 'GET',
          'credentials': 'include',
        })
          .then(resp => resp.ok && resp.blob());
      },
    },
  };
</script>