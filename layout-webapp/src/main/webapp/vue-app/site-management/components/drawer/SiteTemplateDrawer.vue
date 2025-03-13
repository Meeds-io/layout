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
    id="siteManagementSiteTemplateDrawer"
    ref="siteTemplateDrawer"
    eager
    :right="!$vuetify.rtl"
    @closed="close">
    <template #title>
      <div class="d-flex">
        <v-icon
          class="clickable"
          size="16"
          @click="close">
          fas fa-arrow-left
        </v-icon>
        <span class="ms-2"> {{ $t('siteManagement.label.siteTemplate.drawer.title') }} </span>
      </div>
    </template>
    <template #content>
      <v-card class="mx-4 my-4 px-2 py-2 elevation-0">
        <span class="font-weight-bold text-start text-color body-2">{{ $t('siteManagement.label.siteTemplate') }}</span>
        <v-select
          v-model="template"
          class="caption pt-1 mb-5"
          dense
          item-text="text"
          item-value="value"
          :items="templates"
          outlined />

        <v-img
          class="align-center mx-auto"
          max-height="350"
          max-width="500"
          :src="siteTemplateSkeleton" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex justify-end">
        <v-btn
          class="btn ms-2"
          @click="close">
          {{ $t('siteManagement.label.btn.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary ms-2"
          :loading="loading"
          @click="saveSite">
          {{ $t('siteManagement.label.btn.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data () {
      return {
        template: 'navigationWebsite',
        loading: false,
        isValidForm: true,
      };
    },
    computed: {
      templates () {
        return [
          {
            text: this.$t('siteManagement.label.navigationWebsite'),
            value: 'navigationWebsite',
          },
        ];
      },
      siteTemplateSkeleton () {
        return `/layout/images/site-templates/${this.template}.png`;
      },
    },
    created () {
      this.$root.$on('open-site-template-drawer', this.open);
      this.$root.$on('close-site-template-drawer', this.close);
    },
    methods: {
      open () {
        this.$refs.siteTemplateDrawer.open();
      },
      close () {
        this.loading = false;
        this.$refs.siteTemplateDrawer.close();
        this.$refs.siteTemplateDrawer.endLoading();
      },
      saveSite () {
        this.loading = true;
        this.$refs.siteTemplateDrawer.startLoading();
        this.$root.$emit('create-site', this.template);
      },
    },
  };
</script>