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
  <v-menu
    v-model="menu"
    :content-class="menuId"
    :left="!$vuetify.rtl"
    offset-y
    :right="$vuetify.rtl">
    <template #activator="{ on, attrs }">
      <v-btn
        :aria-label="$t('sectionTemplates.menu.open')"
        class="mx-auto"
        icon
        :loading="loading"
        small
        v-bind="attrs"
        v-on="on">
        <v-icon class="icon-default-color" size="16">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-hover v-if="menu" @input="hoverMenu = $event">
      <v-list
        class="pa-0"
        dense
        @focusout="menu = false"
        @mouseout="menu = false">
        <v-list-item-group v-model="listItem">
          <v-tooltip bottom :disabled="!sectionTemplate.system">
            <template #activator="{ on, attrs }">
              <div
                v-bind="attrs"
                v-on="on">
                <v-list-item
                  dense
                  :disabled="sectionTemplate.system"
                  :href="!sectionTemplate.system && editLayoutLink"
                  rel="opener"
                  target="_blank">
                  <v-card
                    color="transparent"
                    flat
                    min-width="15">
                    <v-icon
                      :class="sectionTemplate.system && 'disabled--text'"
                      size="13">
                      fa-edit
                    </v-icon>
                  </v-card>
                  <v-list-item-title class="ps-2">
                    {{ $t('sectionTemplates.label.editInstance') }}
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('sectionTemplates.label.system.noEdit') }}</span>
          </v-tooltip>
          <v-list-item
            dense
            @click="$root.$emit('section-template-edit', sectionTemplate)">
            <v-card
              color="transparent"
              flat
              min-width="15">
              <v-icon size="13">
                fa-edit
              </v-icon>
            </v-card>
            <v-list-item-title class="ps-2">
              {{ $t('sectionTemplates.label.editProperties') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="duplicateSectionTemplate">
            <v-card
              color="transparent"
              flat
              min-width="15">
              <v-icon size="13">
                fa-copy
              </v-icon>
            </v-card>
            <v-list-item-title class="ps-2">
              {{ $t('sectionTemplates.label.duplicate') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip bottom :disabled="!sectionTemplate.system">
            <template #activator="{ on, attrs }">
              <div
                v-bind="attrs"
                v-on="on">
                <v-list-item
                  dense
                  @click="$root.$emit('section-template-delete', sectionTemplate)">
                  <v-card
                    color="transparent"
                    flat
                    min-width="15">
                    <v-icon
                      :class="!sectionTemplate.system && 'error--text' || 'disabled--text'"
                      size="13">
                      fa-trash
                    </v-icon>
                  </v-card>
                  <v-list-item-title class="ps-2">
                    <span :class="!sectionTemplate.system && 'error--text' || 'disabled--text'">{{ $t('sectionTemplates.label.delete') }}</span>
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('sectionTemplates.label.system.noDelete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </v-menu>
</template>
<script>
  export default {
    props: {
      sectionTemplate: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      loading: false,
      hoverMenu: false,
      listItem: null,
      menuId: `sectionTemplateMenu${parseInt(Math.random() * 10000)}`,
    }),
    computed: {
      sectionTemplateId () {
        return this.sectionTemplate?.id;
      },
      name () {
        return this.$te(this.sectionTemplate?.name) ? this.$t(this.sectionTemplate?.name) : this.sectionTemplate?.name;
      },
      hasEditMode () {
        return this.sectionTemplate?.supportedModes?.find?.(mode => mode === 'edit');
      },
      editLayoutLink () {
        return `/portal/${eXo.env.portal.portalName}/section-editor?id=${this.sectionTemplateId}`;
      },
    },
    watch: {
      listItem () {
        if (this.menu) {
          this.menu = false;
          this.listItem = null;
        }
      },
      menu () {
        if (this.menu) {
          this.$root.$emit('section-template-menu-opened', this.sectionTemplateId);
        } else {
          this.$root.$emit('section-template-menu-closed', this.sectionTemplateId);
        }
      },
      hoverMenu () {
        if (!this.hoverMenu) {
          window.setTimeout(() => {
            if (!this.hoverMenu) {
              this.menu = false;
            }
          }, 200);
        }
      },
    },
    created () {
      this.$root.$on('section-template-menu-opened', this.checkMenuStatus);
      document.addEventListener('click', this.closeMenuOnClick);
    },
    beforeUnmount () {
      this.$root.$off('section-template-menu-opened', this.checkMenuStatus);
      document.removeEventListener('click', this.closeMenuOnClick);
    },
    methods: {
      closeMenuOnClick (e) {
        if (e.target && !e.target.closest(`.${this.menuId}`)) {
          this.menu = false;
        }
      },
      checkMenuStatus (sectionTemplateId) {
        if (this.menu && sectionTemplateId !== this.sectionTemplate.id) {
          this.menu = false;
        }
      },
      async duplicateSectionTemplate () {
        this.loading = true;
        try {
          const sectionTemplate = JSON.parse(JSON.stringify(this.sectionTemplate));
          sectionTemplate.id = null;
          sectionTemplate.system = false;
          sectionTemplate.category = 'custom';
          const createdSectionTemplate = await eXo.$sectionTemplateService.createSectionTemplate(sectionTemplate);

          const nameLabels = await eXo.$translationService.getTranslations('sectionTemplate', this.sectionTemplate.id, 'title');
          await eXo.$translationService.saveTranslations('sectionTemplate', createdSectionTemplate.id, 'title', nameLabels);

          const descriptionLabels = await eXo.$translationService.getTranslations('sectionTemplate', this.sectionTemplate.id, 'description');
          await eXo.$translationService.saveTranslations('sectionTemplate', createdSectionTemplate.id, 'description', descriptionLabels);

          if (this.sectionTemplate.illustrationId) {
            const illustrationSrc = `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/sectionTemplate/${this.sectionTemplate.id}/${this.sectionTemplate.illustrationId}`;
            const file = await this.getIllustrationFile(illustrationSrc);
            const uploadId = await this.uploadFile(file);
            await eXo.$fileAttachmentService.saveAttachments({
              objectType: 'sectionTemplate',
              objectId: createdSectionTemplate.id,
              uploadedFiles: [{ uploadId }],
              attachedFiles: [],
            });
          }
          this.$root.$emit('section-template-edit', createdSectionTemplate);
        } finally {
          this.loading = false;
        }
      },
      async uploadFile (file) {
        const uploadId =  await eXo.$uploadService.upload(file);
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
      getIllustrationFile (src) {
        return fetch(src, {
          'method': 'GET',
          'credentials': 'include',
        })
          .then(resp => resp.ok && resp.blob());
      },
    },
  };
</script>