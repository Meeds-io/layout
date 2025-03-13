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
  <v-hover v-model="hover" :disabled="noHover || !illustrationId">
    <div
      :aria-label="$t(`${objectType}.label.preview`, {0: name})"
      class="position-relative full-height full-width d-flex align-center justify-center">
      <v-expand-transition>
        <v-card
          v-if="hover"
          :aria-label="$t(`${objectType}.label.openIllustrationPreview`)"
          class="d-flex absolute-full-size z-index-one align-center justify-center transition-fast-in-fast-out grey opacity-5"
          flat
          @click="openIllustration">
          <v-icon color="white">fa-search</v-icon>
        </v-card>
      </v-expand-transition>
      <v-img
        contain
        :max-height="maxHeight"
        :max-width="maxWidth"
        :src="illustrationSrc" />
    </div>
  </v-hover>
</template>
<script>
  export default {
    props: {
      value: {
        type: Object,
        default: null,
      },
      objectType: {
        type: String,
        default: null,
      },
      defaultSrc: {
        type: String,
        default: null,
      },
      noHover: {
        type: Boolean,
        default: false,
      },
      maxHeight: {
        type: String,
        default: () => '30',
      },
      maxWidth: {
        type: String,
        default: () => '60',
      },
      previewAction: {
        type: String,
        default: null,
      },
    },
    data: () => ({
      hover: false,
    }),
    computed: {
      id () {
        return this.value?.id;
      },
      name () {
        return this.$te(this.value?.name) ? this.$t(this.value?.name) : this.value?.name;
      },
      illustrationId () {
        return this.value?.illustrationId;
      },
      illustrationSrc () {
        return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/${this.objectType}/${this.id}/${this.illustrationId}` || this.defaultSrc;
      },
    },
    methods: {
      openIllustration () {
        this.$root.$emit('layout-illustration-preview', this.illustrationSrc, this.previewAction);
      },
    },
  };
</script>
