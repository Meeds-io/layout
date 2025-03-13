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
    <div class="d-flex align-center">
      <slot v-if="$slots.title" name="title"></slot>
      <div
        v-else
        class="me-auto"
        :class="textBold && 'font-weight-bold' || 'text-header'">
        {{ $t('layout.background') }}
      </div>
      <v-switch
        v-model="enabled"
        class="ms-auto my-auto me-n2" />
    </div>
    <v-list-item
      v-if="enabled"
      class="pa-0"
      dense>
      <v-list-item-content class="my-auto">
        <v-radio-group
          v-model="choice"
          class="my-auto text-no-wrap flex-grow-1 flex-shrink-0"
          mandatory>
          <v-radio
            class="mx-0"
            value="color">
            <template #label>
              <span>{{ $t('layout.color') }}</span>
            </template>
          </v-radio>
          <v-radio
            class="mx-0"
            value="gradient">
            <template #label>
              <span>{{ $t('layout.gradient') }}</span>
            </template>
          </v-radio>
        </v-radio-group>
      </v-list-item-content>
      <v-list-item-action
        class="me-0 ms-auto"
        :class="choice === 'color' && !scrollColor && 'mb-auto' || 'my-auto'">
        <layout-editor-color-picker
          v-if="choice === 'color' && !scrollColor"
          v-model="container.backgroundColor"
          class="my-auto" />
        <div v-else-if="choice === 'color' && scrollColor">
          <layout-editor-color-picker
            v-model="backgroundScrollTop"
            class="my-auto"
            :label="$t('layout.scrollTopColor')" />
          <layout-editor-color-picker
            v-model="backgroundScrollMiddle"
            class="my-auto"
            :label="$t('layout.scrollMiddleColor')" />
        </div>
        <div v-else>
          <layout-editor-color-picker
            v-model="backgroundGradientFrom"
            class="my-auto"
            :label="$t('layout.gradientFrom')" />
          <layout-editor-color-picker
            v-model="backgroundGradientTo"
            class="my-auto"
            :label="$t('layout.gradientTo')" />
        </div>
      </v-list-item-action>
    </v-list-item>

    <v-list-item
      v-if="enabled"
      class="pa-0"
      dense>
      <v-list-item-content class="my-auto">
        {{ $t('layout.image') }}
      </v-list-item-content>
      <v-list-item-action class="my-auto me-0 ms-auto">
        <layout-editor-background-image-attachment
          ref="backgroundImage"
          v-model="container.backgroundImage"
          class="my-auto"
          :immediate-save="immediateSave"
          :storage-id="objectId" />
      </v-list-item-action>
    </v-list-item>
    <div v-if="container.backgroundImage" class="d-flex">
      <v-radio-group
        v-model="backgroundImageStyle"
        class="my-auto text-no-wrap flex-grow-1 flex-shrink-0"
        mandatory>
        <v-radio
          class="mx-0"
          value="cover">
          <template #label>
            <span>{{ $t('layout.imageSizeCover') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="contain">
          <template #label>
            <span>{{ $t('layout.imageSizeContain') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="repeat">
          <template #label>
            <span>{{ $t('layout.imageRepeat') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="no-repeat">
          <template #label>
            <span>{{ $t('layout.imageNoRepeat') }}</span>
          </template>
        </v-radio>
      </v-radio-group>
      <v-radio-group
        v-model="container.backgroundPosition"
        class="my-auto text-no-wrap flex-grow-1 flex-shrink-0"
        mandatory>
        <v-radio
          class="mx-0"
          value="top left">
          <template #label>
            <span>{{ $t('layout.imagePositionTopLeft') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="top right">
          <template #label>
            <span>{{ $t('layout.imagePositionTopRight') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="bottom left">
          <template #label>
            <span>{{ $t('layout.imagePositionBottomLeft') }}</span>
          </template>
        </v-radio>
        <v-radio
          class="mx-0"
          value="bottom right">
          <template #label>
            <span>{{ $t('layout.imagePositionBottomRight') }}</span>
          </template>
        </v-radio>
      </v-radio-group>
    </div>
  </div>
</template>
<script>
  export default {
    props: {
      value: {
        type: Object,
        default: null,
      },
      defaultBackgroundColor: {
        type: String,
        default: () => '#FFFFFFFF',
      },
      immediateSave: {
        type: Boolean,
        default: false,
      },
      scrollColor: {
        type: Boolean,
        default: false,
      },
      textBold: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      container: null,
      enabled: false,
      choice: null,
      backgroundImageStyle: null,
      backgroundScrollTop: null,
      backgroundScrollMiddle: null,
      backgroundGradientFrom: null,
      backgroundGradientTo: null,
      initialized: false,
    }),
    computed: {
      id () {
        return this.container.storageId || this.container.id;
      },
      objectId () {
        return this.$root.isSiteLayout ? `site_${this.$root.siteId}_${this.id}` : `page_${this.$root.pageId}_${this.id}`;
      },
      backgroundColor () {
        return this.container.backgroundColor;
      },
      backgroundColorChoice () {
        if (!this.enabled) {
          return null;
        } else if (this.choice === 'color'
          && !this.scrollColor) {
          return this.backgroundColor?.includes?.('@') ? this.backgroundColor.split('@')[0] : this.backgroundColor;
        } else if (this.choice === 'color'
          && this.scrollColor) {
          return `${this.backgroundScrollTop}@${this.backgroundScrollMiddle}`;
        } else {
          return '#FFFFFF00';
        }
      },
      backgroundEffect () {
        if (this.choice === 'gradient'
          && this.backgroundGradientFrom
          && this.backgroundGradientTo) {
          return `linear-gradient(${this.backgroundGradientFrom}, ${this.backgroundGradientTo})`;
        } else {
          return null;
        }
      },
    },
    watch: {
      container: {
        deep: true,
        handler () {
          if (this.container) {
            this.$emit('input', this.container);
          }
        },
      },
      scrollColor () {
        if (this.initialized) {
          this.container.backgroundColor = this.enabled && this.defaultBackgroundColor || null;
          this.backgroundScrollTop = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
          this.backgroundScrollMiddle = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
        }
      },
      enabled () {
        if (this.initialized) {
          this.container.backgroundImage = null;
          this.backgroundImageStyle = null;

          this.container.backgroundColor = this.enabled && this.defaultBackgroundColor || null;
          this.backgroundScrollTop = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
          this.backgroundScrollMiddle = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
          this.backgroundGradientFrom = null;
          this.backgroundGradientTo = null;
        }
      },
      backgroundImageStyle () {
        if (this.initialized) {
          if (this.backgroundImageStyle === 'cover' || this.backgroundImageStyle === 'contain') {
            this.container.backgroundSize = this.backgroundImageStyle;
            this.container.backgroundRepeat = null;
          } else {
            this.container.backgroundSize = null;
            this.container.backgroundRepeat = this.backgroundImageStyle;
          }
        }
      },
      backgroundColorChoice: {
        immediate: true,
        handler (newVal, oldVal) {
          if (this.initialized && newVal !== oldVal) {
            this.container.backgroundColor = this.backgroundColorChoice;
          }
        },
      },
      backgroundEffect: {
        immediate: true,
        handler (newVal, oldVal) {
          if (this.initialized && newVal !== oldVal) {
            this.container.backgroundEffect = this.backgroundEffect;
          }
        },
      },
      choice () {
        if (this.initialized) {
          if (this.choice === 'color') {
            this.backgroundGradientFrom = null;
            this.backgroundGradientTo = null;
          } else if (this.choice === 'gradient') {
            this.backgroundGradientFrom = this.defaultBackgroundColor;
            this.backgroundGradientTo = '#999999FF';
          }
          this.container.backgroundColor = this.enabled && this.defaultBackgroundColor || null;
          this.backgroundScrollTop = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
          this.backgroundScrollMiddle = this.enabled && this.scrollColor && this.defaultBackgroundColor || null;
        }
      },
    },
    created () {
      this.container = this.value;
      if (this.container.backgroundSize || this.container.backgroundRepeat) {
        if (this.container.backgroundSize === 'cover'
          || this.container.backgroundSize === 'contain') {
          this.backgroundImageStyle = this.container.backgroundSize;
        } else {
          this.backgroundImageStyle = this.container.backgroundRepeat;
        }
      }
      if (this.container.backgroundEffect) {
        this.choice = 'gradient';
        this.backgroundGradientFrom = this.container.backgroundEffect.replace('linear-gradient(', '').split(',')[0].trim();
        this.backgroundGradientTo = this.container.backgroundEffect.replace('linear-gradient(', '').split(',')[1].replace(/\)$/g, '').trim();
      } else {
        this.choice = 'color';
      }

      this.enabled = !!this.container.backgroundColor || !!this.container.backgroundImage;
      if (this.enabled) {
        if (!this.container.backgroundColor) {
          this.container.backgroundColor = this.defaultBackgroundColor;
        } else if (this.scrollColor) {
          if (this.backgroundColor?.includes?.('@')) {
            this.backgroundScrollTop = this.container.backgroundColor.split('@')[0];
            this.backgroundScrollMiddle = this.container.backgroundColor.split('@')[1];
          } else {
            this.backgroundScrollTop = this.container.backgroundColor;
            this.backgroundScrollMiddle = this.container.backgroundColor;
          }
        } else {
          this.backgroundScrollTop = null;
          this.backgroundScrollMiddle = null;
        }
      }
      this.$nextTick().then(() => this.initialized = true);
    },
    methods: {
      async apply () {
        if (this.enabled && this.$refs.backgroundImage) {
          const backgroundImage = await this.$refs.backgroundImage.save();
          if (backgroundImage) {
            this.container.backgroundImage = backgroundImage;
          }
        } else {
          this.container.backgroundImage = null;
        }
        return this.container;
      },
    },
  };
</script>