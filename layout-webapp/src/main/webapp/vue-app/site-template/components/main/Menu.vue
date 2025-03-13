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
        :aria-label="$t('siteTemplates.menu.open')"
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
          <v-list-item
            dense
            :href="editSiteLayoutLink"
            target="_blank">
            <v-icon size="13">
              fa-window-maximize
            </v-icon>
            <v-list-item-title class="ps-2">
              {{ $t('siteManagement.label.editLayout') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('site-template-navigation-open', siteTemplate)">
            <v-icon size="13">
              fa-columns
            </v-icon>
            <v-list-item-title class="ps-2">
              {{ $t('siteTemplate.label.editNavigation') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('site-template-edit', siteTemplate)">
            <v-card
              color="transparent"
              flat
              min-width="15">
              <v-icon size="13">
                fa-edit
              </v-icon>
            </v-card>
            <v-list-item-title class="ps-2">
              {{ $t('siteTemplates.label.editProperties') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="duplicate">
            <v-card
              color="transparent"
              flat
              min-width="15">
              <v-icon size="13">
                fa-copy
              </v-icon>
            </v-card>
            <v-list-item-title class="ps-2">
              {{ $t('siteTemplates.label.duplicate') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip bottom :disabled="!siteTemplate.system">
            <template #activator="{ on, attrs }">
              <div
                v-bind="attrs"
                v-on="on">
                <v-list-item
                  dense
                  :disabled="siteTemplate.system"
                  @click="$root.$emit('site-template-delete', siteTemplate)">
                  <v-card
                    color="transparent"
                    flat
                    min-width="15">
                    <v-icon
                      :class="!siteTemplate.system && 'error--text' || 'disabled--text'"
                      size="13">
                      fa-trash
                    </v-icon>
                  </v-card>
                  <v-list-item-title class="ps-2">
                    <span :class="!siteTemplate.system && 'error--text' || 'disabled--text'">{{ $t('siteTemplates.label.delete') }}</span>
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('siteTemplates.label.system.noDelete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </v-menu>
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
      menu: false,
      loading: false,
      hoverMenu: false,
      listItem: null,
      menuId: `siteTemplateMenu${parseInt(Math.random() * 10000)}`,
    }),
    computed: {
      siteTemplateId () {
        return this.siteTemplate?.id;
      },
      name () {
        return this.$te(this.siteTemplate?.name) ? this.$t(this.siteTemplate?.name) : this.siteTemplate?.name;
      },
      hasEditMode () {
        return this.siteTemplate?.supportedModes?.find?.(mode => mode === 'edit');
      },
      editSiteLayoutLink () {
        return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/site-layout-editor?siteId=${this.siteTemplateId}`;
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
          this.$root.$emit('site-template-menu-opened', this.siteTemplateId);
        } else {
          this.$root.$emit('site-template-menu-closed', this.siteTemplateId);
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
      this.$root.$on('site-template-menu-opened', this.checkMenuStatus);
      document.addEventListener('click', this.closeMenuOnClick);
    },
    beforeUnmount () {
      this.$root.$off('site-template-menu-opened', this.checkMenuStatus);
      document.removeEventListener('click', this.closeMenuOnClick);
    },
    methods: {
      closeMenuOnClick (e) {
        if (e.target && !e.target.closest(`.${this.menuId}`)) {
          this.menu = false;
        }
      },
      checkMenuStatus (siteTemplateId) {
        if (this.menu && siteTemplateId !== this.siteTemplate.id) {
          this.menu = false;
        }
      },
      async duplicate () {
        const nameTranslations = await eXo.$translationService.getTranslations('siteTemplate', this.siteTemplate.id, 'title');
        const descriptionTranslations = await eXo.$translationService.getTranslations('siteTemplate', this.siteTemplate.id, 'description');

        const bannerBlob = !this.siteTemplate.illustrationId ? null : await fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/siteTemplate/${this.siteTemplate.id}/${this.siteTemplate.illustrationId}`, {
          credentials: 'include',
          method: 'GET',
        }).then(resp => resp?.ok && resp.blob());
        const bannerData = bannerBlob && await eXo.$utils.blobToBase64(bannerBlob);
        const bannerUploadId = bannerBlob && await eXo.$uploadService.upload(bannerBlob);

        this.$root.$emit('site-template-add', {
                           ...this.siteTemplate,
                           id: null,
                           layout: null,
                           illustrationId: null,
                           system: false,
                         },
                         this.siteTemplate.id,
                         nameTranslations?.[eXo.env.portal.defaultLanguage],
                         nameTranslations,
                         descriptionTranslations?.[eXo.env.portal.defaultLanguage],
                         descriptionTranslations,
                         bannerUploadId,
                         bannerData);
      },
    },
  };
</script>