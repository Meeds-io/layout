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
    <div class="mb-2">{{ $t('layout.portlet') }}</div>
    <v-radio-group
      v-if="!disabled && ideEnabled"
      v-model="portletType"
      class="my-auto text-no-wrap ms-n1">
      <v-radio
        class="mx-0"
        :label="$t('layout.newPortletChoice')"
        value="new" />
      <v-radio
        class="mx-0"
        :label="$t('layout.existingPortletChoice')"
        value="existing" />
    </v-radio-group>
    <v-autocomplete
      v-if="portletType === 'existing' || disabled"
      ref="autocomplete"
      v-model="contentId"
      chips
      class="portlet-instance-autocomplete ma-0 pa-0"
      dense
      :disabled="disabled"
      item-text="name"
      item-value="contentId"
      :items="sortedPortlets"
      outlined
      :placeholder="$t('layout.portlet.placeholder')" />
  </div>
</template>
<script>
  export default {
    props: {
      value: {
        type: String,
        default: null,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      contentId: null,
      portletType: null,
      ideEnabled: eXo.env.portal.ideEnabled,
    }),
    computed: {
      sortedPortlets () {
        const portlets = this.$root.portlets?.filter?.(c => c.name) || [];
        portlets.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return portlets;
      },
    },
    watch: {
      value: {
        immediate: true,
        handler () {
          this.contentId = this.value;
        },
      },
      portletType () {
        if (this.portletType === 'new') {
          this.contentId = 'ide/WidgetPortlet';
        }
      },
      contentId () {
        if (this.contentId !== this.value) {
          this.$emit('input', this.contentId);
        }
      },
    },
    created () {
      if (this.disabled || !this.ideEnabled) {
        this.portletType = 'existing';
      } else {
        this.portletType = 'new';
      }
    },
    beforeUnmount () {
      document.removeEventListener('click', this.closeMenu);
    },
    methods: {
      closeMenu (event) {
        if (this?.$refs?.autocomplete
          && !event?.target?.closest?.('.portlet-instance-autocomplete')) {
          this.$refs.autocomplete.blur();
        }
      },
    },
  };
</script>