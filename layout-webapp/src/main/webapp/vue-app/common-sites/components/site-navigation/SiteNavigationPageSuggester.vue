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
  <v-flex id="siteNavigationsPagesSuggesterAutoComplete">
    <v-autocomplete
      ref="selectPage"
      v-model="selectedPage"
      append-icon=""
      attach
      chips
      class="identitySuggester identitySuggesterInputStyle mt-0"
      content-class="identitySuggesterContent"
      dense
      flat
      hide-no-data
      hide-selected
      item-text="displayName"
      :items="items"
      :loading="loadingSuggestions"
      max-width="100%"
      menu-props="closeOnClick, closeOnContentClick, maxHeight = 100"
      persistent-hint
      :placeholder="suggesterLabels.placeholder"
      required
      return-object
      width="100%"
      @blur="$refs.selectPage.isFocused = false"
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
    <span v-if="!page" class="text-subtitle mt-n3 position-absolute error-color">
      {{ $t('siteNavigation.required.error.message') }}
    </span>
  </v-flex>
</template>
<script>
  export default {
    props: {
      page: {
        type: Object,
        default: null,
      },
      siteName: {
        type: String,
        default: null,
      },
      siteType: {
        type: String,
        default: null,
      },
      allSites: {
        type: Boolean,
        default: true,
      },
    },
    data () {
      return {
        pages: [],
        selectedPage: null,
        searchTerm: null,
        loadingSuggestions: false,
        startSearchAfterInMilliseconds: 300,
        endTypingKeywordTimeout: 50,
        startTypingKeywordTimeout: 0,
        typing: false,
      };
    },
    computed: {
      suggesterLabels () {
        return {
          placeholder: this.$t('siteNavigation.label.pagesSuggester.searchPlaceholder'),
          noData: this.$t('siteNavigation.label.pagesSuggester.noData'),
        };
      },
      items () {
        return this.pages.slice().map(page => ({
          displayName: page?.state?.displayName || page?.state?.name,
          pageRef: `${page?.key?.site?.typeName}::${page?.key?.site?.name}::${page?.key?.name}`,
        }));
      },
    },
    watch: {
      searchTerm () {
        if (this.searchTerm) {
          this.startTypingKeywordTimeout = Date.now() + this.startSearchAfterInMilliseconds;
          if (!this.typing) {
            this.typing = true;
            this.waitForEndTyping();
          }
        }
      },
      page: {
        immediate: true,
        handler () {
          if (this.page?.pageRef && this.selectedPage?.pageRef !== this.page?.pageRef) {
            this.selectedPage = this.page;
          } else if (!this.page && this.selectedPage) {
            this.selectedPage = null;
          }
        },
      },
      selectedPage () {
        this.$root.$emit('existing-page-selected', this.selectedPage);
      },
      siteType () {
        this.selectedPage = null;
      },
      allSites () {
        this.selectedPage = null;
        this.searchTerm = ' ';
      },
      siteName () {
        this.selectedPage = null;
        this.pages = [];
        this.searchTerm = ' ';
      },
    },
    created () {
      this.$root.$on('set-selected-page', this.emitSelectedValue);
    },
    methods: {
      remove () {
        this.selectedPage = null;
      },
      searchPages () {
        if (this.allSites || (!this.allSites && this.siteType && this.siteName)) {
          this.loadingSuggestions = true;
          this.pages = [];
          eXo.$pageLayoutService.getPages(this.siteType, this.siteName, this.searchTerm)
            .then(pages => this.pages = pages)
            .finally(() => this.loadingSuggestions = false);
        }
      },
      waitForEndTyping () {
        window.setTimeout(() => {
          if (Date.now() - this.startTypingKeywordTimeout > this.startSearchAfterInMilliseconds) {
            this.typing = false;
            this.searchPages();
          } else {
            this.waitForEndTyping();
          }
        }, this.endTypingKeywordTimeout);
      },
      emitSelectedValue (value) {
        this.selectedPage = {
          pageRef: `${ value?.key?.site?.typeName}::${ value?.key?.site?.name}::${value?.key?.name}`,
          displayName: value?.state?.displayName || value?.key.name,
        };
        this.pages.push(value);
      },
    },
  };
</script>
