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
  <v-dialog
    v-model="dialog"
    content-class="overflow-y-initial overflow-x-hidden full-height full-width pa-5"
    height="100%"
    max-height="100%"
    max-width="100%"
    overlay-opacity="0.9"
    :persistent="false"
    width="100%">
    <template v-if="dialog">
      <div class="d-flex justify-end">
        <v-btn
          v-if="actionClick"
          :aria-label="actionLabel"
          class="btn btn-primary my-4"
          @click="clickOnAction">
          <v-icon v-if="actionIcon" color="white">{{ actionIcon }}</v-icon>
          <span>{{ actionLabel }}</span>
        </v-btn>
        <v-btn
          :aria-label="$t('pageTemplate.label.close')"
          class="ma-4"
          icon
          @click="dialog = false">
          <v-icon color="white">fa-times</v-icon>
        </v-btn>
      </div>
      <div class="full-width d-flex align-center justify-center">
        <v-card
          class="d-flex flex-column"
          :class="title && 'aspect-ratio-1 px-4 pt-4'"
          :color="!title && 'transparent'"
          flat
          max-height="80vh"
          max-width="100%"
          :min-width="title && 420"
          :width="title && '80%'"
          @click="dialog = false">
          <div
            v-if="title"
            v-sanitized-html="title"
            class="text-start text-title"></div>
          <div
            v-if="title && description"
            v-sanitized-html="description"
            class="text-start"></div>
          <div
            class="d-flex flex-grow-1 flex-shrink-1 align-center justify-center overflow-hidden"
            :class="title && 'py-4'">
            <img
              :alt="title || ''"
              class="full-height width-auto"
              :class="title && 'elevation-2'"
              height="100%"
              :src="illustrationSrc"
              width="100%">
          </div>
        </v-card>
      </div>
    </template>
  </v-dialog>
</template>
<script>
  export default {
    data: () => ({
      dialog: false,
      illustrationSrc: null,
      actionLabel: null,
      actionIcon: null,
      actionClick: null,
      actionCloseOnClick: null,
    }),
    watch: {
      dialog () {
        if (this.dialog) {
          document.dispatchEvent(new CustomEvent('modalOpened'));
        } else {
          document.dispatchEvent(new CustomEvent('modalClosed'));
        }
      },
    },
    created () {
      this.$root.$on('layout-illustration-preview', this.open);
    },
    methods: {
      open (illustrationSrc, action, title, description) {
        this.illustrationSrc = illustrationSrc;
        this.actionIcon = action?.icon;
        this.actionLabel = action?.label;
        this.actionClick = action?.click;
        this.actionCloseOnClick = action?.closeOnClick;
        this.title = title;
        this.description = description;
        this.dialog = true;
      },
      clickOnAction () {
        if (this.actionCloseOnClick) {
          this.dialog = false;
        }
        if (this.actionClick) {
          this.actionClick();
        }
      },
    },
  };
</script>
