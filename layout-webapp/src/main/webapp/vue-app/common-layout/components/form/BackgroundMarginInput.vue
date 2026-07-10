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
        {{ $t('layout.margins') }}
      </div>
      <v-switch
        v-model="enabled"
        class="ms-auto my-auto me-n2" />
    </div>
    <div
      v-if="enabled"
      :class="marginChoice === 'same' && 'flex-row' || 'flex-column'"
      class="d-flex">
      <v-radio-group v-model="marginChoice" class="my-auto text-no-wrap ms-n1">
        <v-radio
          :label="$t('layout.sameForAllSides')"
          value="same"
          class="mx-0" />
        <v-radio
          :label="$t('layout.differentForEachSide')"
          value="different"
          class="mx-0" />
      </v-radio-group>
      <v-list-item class="pe-0 ps-7 py-0" dense>
        <v-list-item-content
          v-if="marginChoice === 'different'"
          class="my-auto">
          {{ $t('layout.top') }}
        </v-list-item-content>
        <number-input
          v-model="marginTop"
          :max="max"
          :min="min"
          :class="marginChoice === 'different' && 'my-auto' || 'mb-auto ms-auto'"
          class="me-n3" />
      </v-list-item>
      <v-list-item
        v-if="marginChoice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.right') }}
        </v-list-item-content>
        <number-input
          v-model="marginRight"
          :max="max"
          :min="min"
          class="my-auto me-n3" />
      </v-list-item>
      <v-list-item
        v-if="marginChoice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.bottom') }}
        </v-list-item-content>
        <number-input
          v-model="marginBottom"
          :max="max"
          :min="min"
          class="my-auto me-n3" />
      </v-list-item>
      <v-list-item
        v-if="marginChoice === 'different'"
        class="pe-0 ps-7 py-0"
        dense>
        <v-list-item-content class="my-auto">
          {{ $t('layout.left') }}
        </v-list-item-content>
        <number-input
          v-model="marginLeft"
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
      default: () => 400,
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
    marginChoice: 'same',
    marginTop: 0,
    marginRight: 0,
    marginBottom: 0,
    marginLeft: 0,
  }),
  watch: {
    marginTop() {
      if (this.initialized) {
        if (this.marginChoice === 'same') {
          this.marginRight = this.marginTop;
          this.marginBottom = this.marginTop;
          this.marginLeft = this.marginTop;
        }
        this.save();
      }
    },
    marginRight() {
      if (this.initialized) {
        this.save();
      }
    },
    marginBottom() {
      if (this.initialized) {
        this.save();
      }
    },
    marginLeft() {
      if (this.initialized) {
        this.save();
      }
    },
    enabled() {
      if (this.initialized) {
        this.marginChoice = 'same';
        this.marginTop = this.enabled ? 0 : null;
        this.marginRight = this.enabled ? 0 : null;
        this.marginBottom = this.enabled ? 0 : null;
        this.marginLeft = this.enabled ? 0 : null;
        this.save();
      }
    },
    marginChoice() {
      if (this.initialized && this.marginChoice === 'same') {
        this.marginRight = this.marginTop;
        this.marginBottom = this.marginTop;
        this.marginLeft = this.marginTop;
      }
    },
  },
  created() {
    this.container = this.value;
    const values = this.$applicationUtils.parseBackgroundLayerValues(this.container.cssClass) || {};
    this.marginTop = values.marginTop || 0;
    this.marginRight = values.marginRight || 0;
    this.marginBottom = values.marginBottom || 0;
    this.marginLeft = values.marginLeft || 0;
    this.marginChoice =
      this.marginTop === this.marginRight
      && this.marginRight === this.marginLeft
      && this.marginLeft === this.marginBottom ? 'same' : 'different';
    this.enabled = this.marginChoice !== 'same' || this.marginTop !== 0;
    this.$nextTick().then(() => this.initialized = true);
  },
  methods: {
    save() {
      this.$applicationUtils.setBackgroundLayerValues(this.container, {
        marginTop: this.enabled ? (this.marginTop || 0) : null,
        marginRight: this.enabled ? (this.marginRight || 0) : null,
        marginBottom: this.enabled ? (this.marginBottom || 0) : null,
        marginLeft: this.enabled ? (this.marginLeft || 0) : null,
      });
      this.$emit('refresh');
    },
  },
};
</script>
