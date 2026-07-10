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
    <div class="d-flex align-center mb-2">
      <div
        :class="textBold && 'font-weight-bold'"
        class="me-auto">
        {{ $t('layout.borderRadius') }}
      </div>
      <v-switch
        v-model="enabled"
        class="ms-auto my-auto me-n2" />
    </div>
    <div
      v-if="enabled"
      :class="choice === 'same' && 'flex-row' || 'flex-column'"
      class="d-flex">
      <v-radio-group v-model="choice" class="my-auto text-no-wrap ms-n1">
        <v-radio
          :label="$t('layout.sameForAllCorners')"
          value="same"
          class="mx-0" />
        <v-radio
          :label="$t('layout.differentForEachCorner')"
          value="different"
          class="mx-0" />
      </v-radio-group>
      <v-list-item class="pe-0 ps-7 py-0" dense>
        <v-list-item-content v-if="choice === 'different'" class="my-auto">
          {{ $t('layout.topRight') }}
        </v-list-item-content>
        <number-input
          v-model="radiusTopRight"
          :max="max"
          :min="min"
          :class="choice === 'different' && 'my-auto' || 'mb-auto ms-auto'"
          class="me-n3" />
      </v-list-item>
      <v-list-item
        v-if="choice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.topLeft') }}
        </v-list-item-content>
        <number-input
          v-model="radiusTopLeft"
          :max="max"
          :min="min"
          class="my-auto me-n3" />
      </v-list-item>
      <v-list-item
        v-if="choice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.bottomRight') }}
        </v-list-item-content>
        <number-input
          v-model="radiusBottomRight"
          :max="max"
          :min="min"
          class="my-auto me-n3" />
      </v-list-item>
      <v-list-item
        v-if="choice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.bottomLeft') }}
        </v-list-item-content>
        <number-input
          v-model="radiusBottomLeft"
          :max="max"
          :min="min"
          class="my-auto me-n3" />
      </v-list-item>
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
    max: {
      type: Number,
      default: () => 120,
    },
    min: {
      type: Number,
      default: () => 0,
    },
    textBold: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    container: null,
    initialized: false,
    enabled: false,
    choice: 'same',
    radiusTopRight: 0,
    radiusTopLeft: 0,
    radiusBottomRight: 0,
    radiusBottomLeft: 0,
  }),
  watch: {
    radiusTopRight() {
      if (this.initialized) {
        if (this.choice === 'same') {
          this.radiusTopLeft = this.radiusTopRight;
          this.radiusBottomRight = this.radiusTopRight;
          this.radiusBottomLeft = this.radiusTopRight;
        }
        this.save();
      }
    },
    radiusTopLeft() {
      if (this.initialized) {
        this.save();
      }
    },
    radiusBottomRight() {
      if (this.initialized) {
        this.save();
      }
    },
    radiusBottomLeft() {
      if (this.initialized) {
        this.save();
      }
    },
    enabled() {
      if (this.initialized) {
        this.choice = 'same';
        this.radiusTopRight = this.enabled ? 0 : null;
        this.radiusTopLeft = this.enabled ? 0 : null;
        this.radiusBottomRight = this.enabled ? 0 : null;
        this.radiusBottomLeft = this.enabled ? 0 : null;
        this.save();
      }
    },
    choice() {
      if (this.initialized && this.choice === 'same') {
        this.radiusTopLeft = this.radiusTopRight;
        this.radiusBottomRight = this.radiusTopRight;
        this.radiusBottomLeft = this.radiusTopRight;
      }
    },
  },
  created() {
    this.container = this.value;
    const values = this.$applicationUtils.parseBackgroundLayerValues(this.container.cssClass) || {};
    this.radiusTopRight = values.radiusTopRight || 0;
    this.radiusTopLeft = values.radiusTopLeft || 0;
    this.radiusBottomRight = values.radiusBottomRight || 0;
    this.radiusBottomLeft = values.radiusBottomLeft || 0;
    this.choice =
      this.radiusTopRight === this.radiusTopLeft
      && this.radiusBottomRight === this.radiusTopLeft
      && this.radiusTopLeft === this.radiusBottomLeft ? 'same' : 'different';
    this.enabled = this.choice !== 'same' || this.radiusTopRight !== 0;
    this.$nextTick().then(() => this.initialized = true);
  },
  methods: {
    save() {
      this.$applicationUtils.setBackgroundLayerValues(this.container, {
        radiusTopRight: this.enabled ? (this.radiusTopRight || 0) : null,
        radiusTopLeft: this.enabled ? (this.radiusTopLeft || 0) : null,
        radiusBottomRight: this.enabled ? (this.radiusBottomRight || 0) : null,
        radiusBottomLeft: this.enabled ? (this.radiusBottomLeft || 0) : null,
      });
      this.$emit('refresh');
    },
  },
};
</script>
