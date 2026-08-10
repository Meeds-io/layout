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
  <v-hover v-model="hover">
    <v-card
      :id="`PortletInstance_${application.id}`"
      :title="$t('layout.preview')"
      :elevation="hover && 2 || 0"
      :aria-label="$t('layout.preview')"
      :height="height"
      :max-height="height"
      :width="width"
      :max-width="width"
      class="border-color card-border-radius overflow-hidden position-relative"
      v-on="illustrationId && {
        click: () => preview()
      }"
      @click="preview">
      <v-btn
        v-show="hover"
        :aria-label="$t('layout.add')"
        :class="$vuetify.rtl && 'l-0' || 'r-0'"
        class="position-absolute t-0 mt-4 me-4 z-index-one"
        icon
        @click.stop="$emit('add')">
        <v-icon size="24" color="primary">fa-plus</v-icon>
      </v-btn>
      <div class="d-flex flex-column border-box-sizing full-height full-width pa-5">
        <div
          :title="name"
          class="flex-grow-0 flex-shrink-0 subtitle-1 text-color">
          <span class="text-truncate-2">{{ name }}</span>
        </div>
        <div
          v-if="description"
          :title="$utils.htmlToText(description)"
          class="flex-grow-0 flex-shrink-0 pt-0 text-subtitle">
          <span
            v-sanitized-html="description"
            class="text-truncate-2"></span>
        </div>
        <div class="d-flex flex-grow-1 flex-shrink-1 justify-center mt-2 overflow-hidden">
          <layout-image-illustration
            ref="illustration"
            :value="application"
            :preview-action="illustrationAction"
            :max-height="maxImageHeight"
            :max-width="maxImageWidth"
            object-type="portletInstance"
            no-hover />
        </div>
      </div>
    </v-card>
  </v-hover>
</template>
<script>
export default {
  props: {
    application: {
      type: Object,
      default: null,
    },
    height: {
      type: String,
      default: () => 'auto',
    },
    maxImageHeight: {
      type: String,
      default: () => '100%',
    },
    width: {
      type: String,
      default: () => '100%',
    },
    maxImageWidth: {
      type: String,
      default: () => '100%',
    },
  },
  data: () => ({
    defaultImageSrc: '/sites/images/application/DefaultPortlet.png',
    hover: false,
  }),
  computed: {
    name() {
      return this.application?.name;
    },
    description() {
      return this.application?.description;
    },
    instanceId() {
      return this.application?.id;
    },
    illustrationId() {
      return this.application?.illustrationId;
    },
    illustrationSrc() {
      return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/portletInstance/${this.instanceId}/${this.illustrationId}` || this.defaultImageSrc;
    },
    illustrationAction() {
      return {
        label: this.$t('layout.add'),
        closeOnClick: true,
        click: () => this.$emit('add'),
      };
    },
  },
  methods: {
    preview() {
      this.$refs.illustration.openIllustration();
    },
  },
};
</script>

