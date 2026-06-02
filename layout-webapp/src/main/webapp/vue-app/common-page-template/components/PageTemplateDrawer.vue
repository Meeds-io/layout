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
  <exo-drawer
    ref="drawer"
    id="defaultSpacesRegistrationDrawer"
    v-model="drawer"
    :loading="saving"
    right
    disable-pull-to-refresh>
    <template #title>
      {{ duplicate && $t('layout.duplicateTemplateTitle') || templateId && $t('layout.editTemplateTitle') || $t('layout.saveAsTemplateTitle') }}
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <div class="d-flex">
          <translation-text-field
            ref="title"
            id="pageTemplateTitle"
            v-model="titleTranslations"
            :field-value.sync="title"
            :placeholder="$t('layout.templateTitlePlaceholder')"
            :maxlength="maxTitleLength"
            :object-id="templateId"
            :rules="rules.title"
            object-type="pageTemplate"
            field-name="title"
            drawer-title="layout.templateTitleDrawerTitle"
            class="width-auto flex-grow-1 pb-1"
            back-icon
            required>
            <template #title>
              <div class="text-subtitle-1">
                {{ $t('layout.templateTitle') }}
              </div>
            </template>
          </translation-text-field>
        </div>
        <div class="pa-0">
          <translation-text-field
            ref="descriptionTranslation"
            v-model="descriptionTranslations"
            :field-value.sync="description"
            :maxlength="maxDescriptionLength"
            :object-id="templateId"
            :rules="rules.description"
            object-type="pageTemplate"
            field-name="description"
            drawer-title="layout.templateDescriptionDrawerTitle"
            class="ma-1px mt-4"
            back-icon
            rich-editor>
            <template #title>
              <div class="text-subtitle-1">
                {{ $t('layout.templateDescription') }}
              </div>
            </template>
            <rich-editor
              id="pageTemplateDescription"
              ref="pageTemplateDescriptionEditor"
              v-model="descriptionTranslations[lang]"
              :placeholder="$t('layout.templateDescriptionPlaceholder')"
              :max-length="maxDescriptionLength"
              :tag-enabled="false"
              ck-editor-type="pageTemplateDescription"
              disable-suggester />
          </translation-text-field>
        </div>
        <layout-editor-page-template-preview
          ref="pagePreview"
          v-model="illustrationUploadId"
          :duplicate="duplicate"
          :template-id="templateId" />
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
          :disabled="disabled"
          :loading="saving"
          class="btn btn-primary"
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
    saving: false,
    template: null,
    title: null,
    description: null,
    titleTranslations: {},
    descriptionTranslations: {},
    illustration: null,
    maxTitleLength: 50,
    maxDescriptionLength: 100,
    illustrationUploadId: null,
    duplicate: null,
    templateId: null,
    pageLayoutContent: null,
    editLayoutProperties: false,
    lang: eXo.env.portal.defaultLanguage,
  }),
  watch: {
    description() {
      if (this.$refs.descriptionTranslation) {
        this.$refs.descriptionTranslation.setValue(this.description);
      }
      if (this.$refs.pageTemplateDescriptionEditor?.editor && this.description !== this.$refs.pageTemplateDescriptionEditor.inputVal) {
        this.$refs.pageTemplateDescriptionEditor.editor.setData(this.description);
      }
    },
  },
  computed: {
    rules() {
      return {
        title: [
          v => !!v?.length || ' ',
          v => !v?.length || v.length < this.maxTitleLength || this.$t('layout.templateTitle.exceedsMaxLength', {
            0: this.maxTitleLength,
          }),
        ],
        description: [
          v => !v?.length || v.length < this.maxDescriptionLength || this.$t('layout.templateTitle.exceedsMaxLength', {
            0: this.maxDescriptionLength,
          }),
        ],
      };
    },
    disabled() {
      return !this.title?.length || this.title.length > this.maxTitleLength || (this.descriptionTranslations[this.lang]?.length && this.$utils.htmlToText(this.descriptionTranslations[this.lang]).length > this.maxDescriptionLength);
    },
  },
  created() {
    this.$root.$on('layout-page-template-drawer-open', this.open);
    this.descriptionTranslations[this.lang] = '';
  },
  beforeDestroy() {
    this.$root.$off('layout-page-template-drawer-open', this.open);
  },
  methods: {
    async open(template, duplicate, generateIllustration, editLayoutProperties) {
      this.templateId = template.id || this.$root.pageTemplate?.id || null;
      this.editLayoutProperties = editLayoutProperties;
      this.pageLayoutContent = template.content;
      if (this.templateId) {
        const pageTemplate = await this.$pageTemplateService.getPageTemplate(this.templateId, true);
        this.description = pageTemplate?.description || '';
        this.duplicate = duplicate;
        if (this.duplicate) {
          this.pageLayoutContent = pageTemplate.content;
        }
      }
      if (generateIllustration) {
        const parentElement = document.querySelector('.layout-sections-parent .layout-page-body').parentElement;
        parentElement.querySelectorAll('.layout-add-application-button').forEach(el => el.style.display = 'none');
        let previewCanvas;
        try {
          previewCanvas = await window.html2canvas(document.querySelector('.layout-sections-parent').parentElement);
        } finally {
          parentElement.querySelectorAll('.layout-add-application-button').forEach(el => el.style.display = '');
        }

        if (previewCanvas) {
          const previewImage = previewCanvas.toDataURL('image/png');
          const previewBlob = this.convertPreviewToFile(previewImage);
          await this.$nextTick();
          this.$refs.drawer.open();
          await this.$nextTick();
          window.setTimeout(() => this.$refs?.pagePreview?.uploadFile(previewBlob), 200);
        }
      } else {
        await this.$nextTick();
        this.$refs.drawer.open();
      }
    },
    close() {
      this.$refs.drawer.close();
    },
    save() {
      this.saving = true;
      const savePageRequest =
        (!this.duplicate && this.templateId) ?
          this.$pageTemplateService.getPageTemplate(this.templateId, true)
            .then(pageTemplate => {
              const newTemplate = (this.$root.pageTemplate && !this.$root.pageTemplate.name);
              pageTemplate.disabled = newTemplate ? false : pageTemplate.disabled;
              if (!this.editLayoutProperties) {
                pageTemplate.content = this.pageLayoutContent;
              }
              return this.$pageTemplateService.updatePageTemplate(pageTemplate)
                .then(() => {
                  if (newTemplate) {
                    this.$root.$emit('page-templates-created', pageTemplate);
                  } else {
                    this.$root.$emit('page-templates-updated', pageTemplate);
                  }
                });
            })
          : this.$pageTemplateService.createPageTemplate(this.pageLayoutContent)
            .then(pageTemplate => {
              this.$root.$emit('page-templates-created', pageTemplate, this.$root.pageRef);
              return pageTemplate;
            });
      return savePageRequest
        .then(pageTemplate => {
          if (pageTemplate) {
            this.templateId = pageTemplate.id;
            return this.$nextTick();
          }
        })
        .then(() => this.$translationService.saveTranslations('pageTemplate', this.templateId, 'title', this.titleTranslations))
        .then(() => this.$translationService.saveRichTranslations('pageTemplate', this.templateId, 'description', this.descriptionTranslations))
        .then(() => this.$refs?.pagePreview?.save())
        .then(() => {
          if (this.$root.pageTemplate) {
            return this.$pageTemplateService.getPageTemplate(this.templateId, true)
              .then(pageTemplate => this.$root.pageTemplate = pageTemplate);
          }
        })
        .then(() => {
          this.$root.$emit('page-templates-saved');
          this.close();
          this.$root.$emit('alert-message', this.$t('layout.pageTemplateCreatedSuccessfully'), 'success');
        })
        .finally(() => this.saving = false);
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
