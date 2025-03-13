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
    <v-hover
      v-model="hover">
      <v-card
        class="d-flex flex-column pa-2 hover-elevation overflow-hidden position-relative"
        :class="{
          'primary-border-color': selected,
          'border-color': !selected,
        }"
        flat
        max-height="150"
        max-width="170"
        min-height="150"
        min-width="170"
        @click="$root.$emit('layout-illustration-preview', illustrationSrc, illustrationAction, title, description)">
        <div class="d-flex flex-column flex-grow-1 align-center justify-center">
          <v-icon class="py-4" size="32">{{ siteTemplate.icon || 'fa-globe' }}</v-icon>
          <p
            v-if="title"
            v-sanitized-html="title"
            class="mb-0 font-weight-regular text-truncate-2"></p>
        </div>
        <div
          v-if="hover"
          class="position-absolute full-height full-width t-0 l-0 mask-color z-index-drawer d-flex flex-column pa-2 overflow-hidden text-start">
          <p
            v-if="title"
            v-sanitized-html="title"
            class="white--text mb-1 font-weight-regular text-truncate-2"></p>
          <p
            v-if="description"
            v-sanitized-html="description"
            class="white--text mb-0 text-subtitle text-truncate-3"></p>
          <div class="d-flex flex-grow-1 align-end justify-end">
            <v-btn
              class="btn btn-primary me-2"
              small
              @click.prevent.stop="$emit('select')">
              {{ $t('layout.use') }}
            </v-btn>
            <v-btn
              class="btn"
              color="white"
              small
              @click="$root.$emit('layout-illustration-preview', illustrationSrc, illustrationAction)">
              {{ $t('layout.preview') }}
            </v-btn>
          </div>
        </div>
      </v-card>
    </v-hover>
  </div>
</template>
<script>
  export default {
    props: {
      siteTemplate: {
        type: Object,
        default: null,
      },
      selected: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      hover: false,
    }),
    computed: {
      title () {
        return this.siteTemplate?.name;
      },
      description () {
        return this.siteTemplate?.description;
      },
      illustrationId () {
        return this.siteTemplate?.illustrationId;
      },
      siteTemplateId () {
        return this.siteTemplate?.id;
      },
      illustrationSrc () {
        return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/siteTemplate/${this.siteTemplateId}/${this.illustrationId}` ||  '/layout/images/page-templates/DefaultPreview.webp';
      },
      illustrationAction () {
        return {
          label: this.$t('layout.use'),
          closeOnClick: true,
          click: () => this.$emit('select'),
        };
      },
    },
  };
</script>