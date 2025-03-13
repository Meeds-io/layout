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
  <div class="mb-5">
    <div class="d-flex align-center justify-space-between">
      <span class="font-weight-bold">{{ categoryName }}</span>
      <v-btn
        v-if="seeMore"
        :aria-label="$t('siteNavigation.label.seeMore')"
        color="primary"
        plain
        text
        @click="displayItems(maxItemsToDisplay+8)">
        <span>{{ $t('siteNavigation.label.seeMore') }}</span>
      </v-btn>
    </div>
    <v-row class="mx-0 d-flex flex-row flex-grow-1 flex-shrink-1">
      <site-navigation-new-page-element-item
        v-for="template in templatesTodisplay"
        :key="template.id"
        class="ps-0 clickable"
        :class="!expanded && 'col-6' || 'col-2'"
        :expand="expanded"
        :page-template="template"
        :selected="selectedTemplateId === template.id"
        @select="selectedTemplateId = template.id" />
    </v-row>
  </div>
</template>

<script>
  export default {
    props: {
      value: {
        type: String,
        default: null,
      },
      templateItems: {
        type: Object,
        default: null,
      },
      categoryName: {
        type: String, 
        default: '',
      },
      selectedList: {
        type: Boolean,
        default: false,
      },
    },
    data () {
      return {
        maxItemsToDisplay: 4,
        templates: null,
        expanded: false,
        collator: new Intl.Collator(eXo.env.portal.language, { numeric: true, sensitivity: 'base' }),
        selectedTemplateId: null,
      };
    },
    computed: {
      templatesTodisplay () {
        return this.templates;
      },
      seeMore () {
        return this.templateItems?.length && this.templateItems?.length >= this.maxItemsToDisplay;
      },
    },
    watch: {
      selectedTemplateId () {
        this.$emit('input', this.selectedTemplateId);
      },
      value () {
        this.selectedTemplateId = this.value;
      },
    },
    created () {
      this.templates = this.templateItems.slice(0, this.maxItemsToDisplay);
      this.$root.$on('toggle-expand', this.toggleExpand);
      this.$root.$on('reset-element-drawer', this.resetTemplateList);
      this.selectedTemplateId = this.value;
    
    },
    beforeUnmount () {
      this.$root.$off('toggle-expand', this.toggleExpand);
      this.$root.$off('reset-element-drawer', this.toggleExpand);
    
    },
    methods: {
      displayItems () {
        this.maxItemsToDisplay = this.maxItemsToDisplay + 8;
        if (this.templateItems?.length && this.templateItems?.length > this.maxItemsToDisplay) {
          this.templates = this.templateItems.slice(0, this.maxItemsToDisplay);
        } else {
          this.templates = this.templateItems;
        }
      },
      toggleExpand (event) {
        this.expanded = event;
        if (this.maxItemsToDisplay < 12) {
          this.maxItemsToDisplay = 12;
        }
        this.templates = this.templateItems.slice(0, this.maxItemsToDisplay);
      },
      resetTemplateList () {
        this.maxItemsToDisplay = 4;
        this.templates = this.templateItems.slice(0, this.maxItemsToDisplay);
      },
    },
  };
</script>
