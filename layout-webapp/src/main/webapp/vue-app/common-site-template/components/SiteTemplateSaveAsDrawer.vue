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
    id="siteTemplateDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    :loading="saving"
    right>
    <template #title>
      <span class="text-wrap">{{ $t('layout.siteTemplate.drawerTitie.add') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <translation-text-field
          id="pageTemplateTitle"
          ref="title"
          v-model="titleTranslations"
          v-model:field-value="title"
          back-icon
          class="width-auto flex-grow-1 pb-1"
          drawer-title="layout.siteTemplate.nameTranslationDrawerTitle"
          field-name="title"
          :maxlength="maxTitleLength"
          object-type="siteTemplate"
          :placeholder="$t('layout.siteTemplate.namePlaceholder')"
          required>
          <template #title>
            <div class="text-subtitle-1">
              {{ $t('layout.siteTemplate.name') }}
            </div>
          </template>
        </translation-text-field>
        <translation-text-field
          ref="descriptionTranslation"
          v-model="descriptionTranslations"
          v-model:field-value="description"
          back-icon
          class="ma-1px mt-4"
          drawer-title="layout.siteTemplate.descriptionTranslationDrawerTitle"
          field-name="description"
          :maxlength="maxDescriptionLength"
          object-type="siteTemplate"
          rich-editor>
          <template #title>
            <div class="text-subtitle-1">
              {{ $t('layout.siteTemplate.description') }}
            </div>
          </template>
          <rich-editor
            id="siteTemplateDescription"
            ref="siteTemplateDescriptionEditor"
            v-model="descriptionTranslations[lang]"
            ck-editor-type="siteTemplateDescription"
            disable-suggester
            :max-length="maxDescriptionLength"
            :placeholder="$t('layout.siteTemplate.descriptionTranslationDrawerTitle')"
            :tag-enabled="false"
            @ready="checkCKEdtiorDisplay" />
        </translation-text-field>
        <site-template-preview
          ref="siteTemplatePreview"
          v-model="illustrationUploadId"
          :preview-image="previewImage" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex me-2">
        <v-spacer />
        <v-btn
          class="btn mx-1"
          @click="close">
          {{ $t('layout.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          :disabled="disabled"
          :loading="saving"
          @click="save">
          {{ $t('layout.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      containerId: null,
      title: null,
      titleTranslations: {},
      description: null,
      descriptionTranslations: {},
      maxTitleLength: 250,
      maxDescriptionLength: 1000,
      illustrationUploadId: null,
      lang: eXo.env.portal.language,
      previewImage: null,
      saving: false,
    }),
    computed: {
      disabled () {
        return !this.title?.length;
      },
    },
    watch: {
      description () {
        if (this.$refs.descriptionTranslation) {
          this.$refs.descriptionTranslation.setValue(this.description);
        }
        this.checkCKEdtiorDisplay();
      },
    },
    created () {
      this.$root.$on('site-template-save-as-drawer', this.open);
    },
    beforeUnmount () {
      this.$root.$off('site-template-save-as-drawer', this.open);
    },
    methods: {
      async open (pageRef, containerId, previewElement) {
        this.$root.$emit('close-alert-message');
        this.pageRef = pageRef;
        this.containerId = containerId;
        this.closeOnSave = !!previewElement;
        this.title = null;
        this.description = null;
        this.titleTranslations = {};
        this.descriptionTranslations = {};
        try {
          if (previewElement) {
            this.illustrationUploadId = await this.generatePreview(previewElement);
          } else {
            this.previewImage = null;
          }
        } finally {
          this.$nextTick().then(() => this.$refs.drawer.open());
        }
      },
      async generatePreview (previewElement) {
        const previewCanvas = await window.html2canvas(previewElement);
        this.previewImage = previewCanvas.toDataURL('image/png');
        const previewBlob = this.convertPreviewToFile(this.previewImage);
        const uploadId =  await eXo.$uploadService.upload(previewBlob);
        return await new Promise((resolve, reject) => {
          const interval = window.setInterval(() => {
            eXo.$uploadService.getUploadProgress(uploadId)
              .then(percent => {
                if (Number(percent) === 100) {
                  window.clearInterval(interval);
                  resolve(uploadId);
                }
              })
              .catch(e => reject(e));
          }, 200);
        });
      },
      convertPreviewToFile (previewImage) {
        const imgString = window.atob(previewImage.replace(/^data:image\/\w+;base64,/, ''));
        const bytes = new Uint8Array(imgString.length);
        for (let i = 0; i < imgString.length; i++) {
          bytes[i] = imgString.charCodeAt(i);
        }
        return new Blob([bytes], { type: 'image/png' });
      },
      close () {
        this.$refs.drawer.close();
      },
      async save () {
        this.saving = true;
        try {
          const siteTemplate = await eXo.$siteTemplateService.saveAsSiteTemplate(this.siteId);
          eXo.$translationService.saveTranslations('siteTemplate', siteTemplate.id, 'title', this.titleTranslations);
          eXo.$translationService.saveTranslations('siteTemplate', siteTemplate.id, 'description', this.descriptionTranslations);
          this.$refs?.siteTemplatePreview?.save(siteTemplate.id);
          this.$root.$emit('site-template-created', siteTemplate.id);
          this.$root.$emit('alert-message', this.$t('layout.siteTemplateCreatedSuccessfully'), 'success');
          this.close();
        } finally {
          this.saving = false;
        }
      },
      checkCKEdtiorDisplay () {
        if (this.$refs.siteTemplateDescriptionEditor?.editor
          && this.description !== this.$refs.siteTemplateDescriptionEditor.inputVal) {
          this.$refs.siteTemplateDescriptionEditor.editor.setData(this.description);
        }
      },
    },
  };
</script>