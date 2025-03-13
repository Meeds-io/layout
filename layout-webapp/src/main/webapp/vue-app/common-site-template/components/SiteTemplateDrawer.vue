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
      <span class="text-wrap">{{ isNew && $t('layout.siteTemplate.drawerTitie.add') || $t('layout.siteTemplate.drawerTitie.edit') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <translation-text-field
          id="siteTemplateTitle"
          ref="title"
          v-model="titleTranslations"
          v-model:field-value="title"
          back-icon
          class="width-auto flex-grow-1 pb-1"
          drawer-title="layout.siteTemplate.nameTranslationDrawerTitle"
          field-name="title"
          :maxlength="maxTitleLength"
          :object-id="siteTemplateId"
          object-type="siteTemplate"
          :placeholder="$t('layout.siteTemplate.namePlaceholder')"
          required>
          <template #title>
            <div class="text-header">
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
          :object-id="siteTemplateId"
          object-type="siteTemplate"
          rich-editor>
          <template #title>
            <div class="text-header">
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
        <font-icon-input
          v-model="siteTemplate.icon"
          class="text-header mt-4" />
        <site-template-layout
          v-if="isNewNoDuplication"
          ref="siteTemplateLayout"
          class="mt-4"
          :site-template="siteTemplate" />
        <site-template-preview
          v-else
          ref="siteTemplatePreview"
          v-model="illustrationUploadId"
          :preview-image="illustrationData"
          :site-template-id="siteTemplateId" />
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
          {{ $t(isNew && 'layout.create' || 'layout.update') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      siteTemplate: null,
      siteTemplateId: null,
      title: null,
      titleTranslations: {},
      description: null,
      descriptionTranslations: {},
      maxTitleLength: 250,
      maxDescriptionLength: 1000,
      illustrationUploadId: null,
      illustrationData: null,
      lang: eXo.env.portal.language,
      saving: false,
      isNew: false,
    }),
    computed: {
      disabled () {
        return !this.title?.length;
      },
      isNewNoDuplication () {
        return this.isNew && !this.sourceSiteId;
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
      this.$root.$on('site-template-add', this.open);
      this.$root.$on('site-template-edit', this.open);
    },
    beforeUnmount () {
      this.$root.$off('site-template-add', this.open);
      this.$root.$off('site-template-edit', this.open);
    },
    methods: {
      open (siteTemplate, sourceSiteId, name, nameTranslations, description, descriptionTranslations, illustrationUploadId, illustrationData) {
        this.$root.$emit('close-alert-message');
        this.isNew = !siteTemplate?.id;
        this.siteTemplate = siteTemplate && JSON.parse(JSON.stringify(siteTemplate)) || {
          icon: 'fa-globe',
        };
        this.siteTemplateId = siteTemplate?.id || null;
        this.sourceSiteId = sourceSiteId;
        this.title = name || siteTemplate?.name || null;
        this.titleTranslations = nameTranslations || {};
        this.descriptionTranslations = descriptionTranslations || {};
        this.description = description || siteTemplate?.description || null;
        this.illustrationUploadId = illustrationUploadId;
        this.illustrationData = illustrationData || null;
        this.$refs.drawer.open();
      },
      close () {
        this.$refs.drawer.close();
      },
      async save () {
        this.saving = true;
        try {
          let siteTemplate;
          if (this.isNew) {
            this.siteTemplate.layout = this.generateLayoutName(this.titleTranslations[eXo.env.portal.defaultLanguage]);
            if (this.sourceSiteId) {
              siteTemplate = await eXo.$siteTemplateService.saveAsSiteTemplate(this.siteTemplate, this.sourceSiteId);
            } else {
              siteTemplate = await eXo.$siteTemplateService.createSiteTemplate(this.siteTemplate);
            }
            this.siteTemplate.id = siteTemplate.id;
          } else {
            siteTemplate = await eXo.$siteTemplateService.updateSiteTemplate(this.siteTemplate);
          }
          this.siteTemplateId = siteTemplate.id;
          siteTemplate.name = this.title;
          siteTemplate.description = this.description;
          await this.$nextTick();

          await eXo.$translationService.saveTranslations('siteTemplate', siteTemplate.id, 'title', this.titleTranslations);
          await eXo.$translationService.saveTranslations('siteTemplate', siteTemplate.id, 'description', this.descriptionTranslations);
          if (this.isNewNoDuplication) {
            await this.$refs?.siteTemplateLayout?.save();
            this.$root.$emit('alert-message', this.$t('layout.siteTemplateCreatedSuccessfully'), 'success');
            this.$root.$emit('site-template-created', siteTemplate);
          } else {
            await this.$refs?.siteTemplatePreview?.save();
            this.$root.$emit('alert-message', this.$t('layout.siteTemplateUpdatedSuccessfully'), 'success');
            this.$root.$emit('site-template-updated', siteTemplate);
          }
          this.close();
        } finally {
          this.saving = false;
        }
      },
      generateLayoutName (name) {
        return `${name.toLowerCase().split('').map(a => a.charCodeAt(0) % 25 + 97).map(a => String.fromCharCode(a)).join('')}${parseInt(Math.random() * 1000)}`;
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