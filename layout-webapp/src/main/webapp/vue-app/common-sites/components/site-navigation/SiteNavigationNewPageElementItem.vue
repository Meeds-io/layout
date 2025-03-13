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
  <div>
    <v-hover
      v-model="hover">
      <div
        class="d-flex flex-column rounded pa-2 hover-elevation card-selection position-relative"
        :class="selected && 'selected'"
        :style="containerStyle">
        <p
          v-if="title"
          v-sanitized-html="title"
          class="mb-0 font-weight-regular text-truncate-2"></p>
        <div class="d-flex flex-grow-1 align-center">
          <div
            class="overflow-hidden"
            :style="imageContainerStyle">
            <img
              :alt="title"
              class="align-center mx-auto object-fit-cover"
              :src="illustrationSrc"
              :style="imageStyle">
          </div>
        </div>
        <div
          v-if="hover"
          class="position-absolute full-height full-width t-0 l-0 mask-color rounded z-index-drawer d-flex flex-column pa-2 overflow-hidden"
          @click="$root.$emit('layout-illustration-preview', illustrationSrc, illustrationAction)">
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
              {{ $t('siteNavigation.label.use') }}
            </v-btn>
            <v-btn
              class="btn"
              color="white"
              small
              @click="$root.$emit('layout-illustration-preview', illustrationSrc, illustrationAction)">
              {{ $t('siteNavigation.label.templatePreview') }}
            </v-btn>
          </div>
        </div>
      </div>
    </v-hover>
  </div>
</template>
<script>
  export default {
    props: {
      expand: {
        type: Boolean,
        default: false,
      },
      pageTemplate: {
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
        return this.pageTemplate?.name;
      },
      description () {
        return this.pageTemplate?.description;
      },
      illustrationId () {
        return this.pageTemplate?.illustrationId;
      },
      pageTemplateId () {
        return this.pageTemplate?.id;
      },
      illustrationSrc () {
        return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/pageTemplate/${this.pageTemplateId}/${this.illustrationId}` ||  '/layout/images/page-templates/DefaultPreview.webp';
      },
      imageStyle () {
        return {
          'max-width': '100%',
          'min-width': '100%',
          'min-height': '100%',
        };
      },
      imageContainerStyle () {
        return {
          'max-width': '100%',
          'min-width': '100%',
          'min-height': '80px',
          'max-height': '80px',
          'height': '80px',
        };
      },
      containerStyle () {
        return {
          'max-width': '100%',
          'min-width': '100%',
          'min-height': '150px',
          'max-height': '150px',
          'height': '150px',
        };
      },
      illustrationAction () {
        return {
          label: this.$t('siteNavigation.label.templateAdd'),
          closeOnClick: true,
          click: () => this.$emit('select'),
        };
      },
    },
  };


</script>
