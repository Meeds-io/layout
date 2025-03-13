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
  <v-flex id="siteNavigationsSiteSuggesterAutoComplete">
    <v-autocomplete
      ref="selectSiteNavigation"
      v-model="selectedSiteNavigationValue"
      append-icon=""
      attach
      chips
      class="identitySuggester identitySuggesterInputStyle mt-0"
      content-class="identitySuggesterContent"
      dense
      flat
      hide-selected
      item-text="displayName"
      item-value="name"
      :items="sites"
      :loading="loadingSuggestions"
      max-width="100%"
      menu-props="closeOnClick, closeOnContentClick, maxHeight = 100"
      persistent-hint
      :placeholder="suggesterLabels.placeholder"
      required
      return-object
      width="100%"
      @blur="$refs.selectSiteNavigation.isFocused = false"
      @update:search-input="searchTerm = $event">
      <template #no-data>
        <v-list-item class="pa-0">
          <v-list-item-title
            class="px-2">
            {{ suggesterLabels.noData }}
          </v-list-item-title>
        </v-list-item>
      </template>
      <template #selection="{item, selected}">
        <v-chip
          class="identitySuggesterItem"
          :close="true"
          :input-value="selected"
          @click:close="remove()">
          <span class="text-truncate">
            {{ item.displayName }}
          </span>
        </v-chip>
      </template>
      <template #item="data">
        <v-list-item-title class="text-truncate identitySuggestionMenuItemText">
          {{ data.item.displayName }}
        </v-list-item-title>
      </template>
    </v-autocomplete>
    <span v-if="!allSites && !selectedSiteNavigation" class="text-subtitle mt-n3 position-absolute error-color">
      {{ $t('siteNavigation.required.error.message') }}
    </span>
  </v-flex>
</template>

<script>
  export default {
    model: {
      prop: 'selectedSiteNavigation',
      event: 'change',
    },
    props: {
      selectedSiteNavigation: {
        type: Object,
        default: null,
      },
      allSites: {
        type: Boolean,
        default: true,
      },
    },
    data () {
      return {
        sites: [],
        searchTerm: null,
        loadingSuggestions: false,
      };
    },
    computed: {
      suggesterLabels () {
        return {
          placeholder: this.$t('siteNavigation.label.sitesSuggester.searchPlaceholder'),
          noData: this.$t('siteNavigation.label.sitesSuggester.noData'),
        };
      },
      selectedSiteNavigationValue: {
        get () {
          return this.selectedSiteNavigation;
        },
        set (value) {
          this.$emit('change', value);
        },
      },
    },
    watch: {
      selectedSiteNavigation () {
        this.$emit('change', this.selectedSiteNavigation);
      },
    },
    created (){
      this.getSites();
    },
    methods: {
      remove () {
        this.selectedSiteNavigation = null;
        this.$emit('change', this.selectedSiteNavigation);
      },
      getSites () {
        this.loadingSuggestions = true;
        return eXo.$siteService.getSites(null, 'USER', null, true, true, false, false, false, null, true)
          .then(sites => {
            this.sites = sites || [];
          })
          .finally(() => this.loadingSuggestions = false);
      },
    },
  };
</script>
