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
  <v-card
    ref="resizeButton"
    class="position-absolute col-resize-cursor linear-gradient-grey-background grid-gap-width me-n5 z-index-two"
    :class="{
      'l-0': $vuetify.rtl,
      'r-0': !$vuetify.rtl,
      'layout-column-resize': hoverSeparator,
    }"
    flat
    height="100%"
    :title="$t('layout.updateWidth')"
    width="20"
    @focusin="hoverSeparator = true"
    @focusout="hoverSeparator = false"
    @mousedown.prevent.stop="resizeStart"
    @mouseout="hoverSeparator = false"
    @mouseover="hoverSeparator = true" />
</template>
<script>
  export default {
    props: {
      moving: {
        type: Boolean,
        default: null,
      },
    },
    data: () => ({
      hoverSeparator: false,
    }),
    methods: {
      resizeStart (event) {
        this.$emit('move-start', event);
      },
    },
  };
</script>
