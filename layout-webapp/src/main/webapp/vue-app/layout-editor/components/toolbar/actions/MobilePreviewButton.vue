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
    <v-tooltip bottom>
      <template #activator="{on, attrs}">
        <div v-bind="attrs" v-on="on">
          <v-btn
            :aria-label="label"
            icon
            @click="switchMode">
            <v-icon class="icon-default-color" size="20">{{ mobileDisplayMode ? 'fa-desktop' :'fa-mobile-alt' }}</v-icon>
          </v-btn>
        </div>
      </template>
      <span>{{ label }}</span>
    </v-tooltip>
  </div>
</template>
<script>
  export default {
    computed: {
      mobileDisplayMode () {
        return this.$root.displayMode === 'mobile';
      },
      label () {
        return this.mobileDisplayMode ? this.$t('layout.switchToDesktop') : this.$t('layout.switchToMobile');
      },
    },
    methods: {
      switchMode () {
        this.$root.displayMode = this.mobileDisplayMode ? 'desktop' : 'mobile';
      },
    },
  };
</script>