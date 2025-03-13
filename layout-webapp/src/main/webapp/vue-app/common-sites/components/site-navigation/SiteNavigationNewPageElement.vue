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
    <span class="d-block text-start text-header mb-5">{{ $t('siteNavigation.label.pageTemplate') }}</span>
    <site-navigation-new-page-element-item-list
      v-if="blankTemplates?.length"
      v-model="selectedTemplateId"
      :category-name="$t('siteNavigation.label.blankTemplate')"
      :template-items="blankTemplates"
      @input="selectTemplate($event)" />
    <site-navigation-new-page-element-item-list
      v-if="defaultTemplates?.length"
      v-model="selectedTemplateId"
      :category-name="$t('siteNavigation.label.defaultTemplate')"
      :template-items="defaultTemplates"
      @input="selectTemplate($event)" />
    <site-navigation-new-page-element-item-list
      v-if="customizedTemplates?.length"
      v-model="selectedTemplateId"
      :category-name="$t('siteNavigation.label.customizedTemplate')"
      :template-items="customizedTemplates"
      @input="selectTemplate($event)" />
  </div>
</template>
<script>
  export default {
    props: {
      pageTemplates: {
        type: Array,
        default: null,
      },
    },
    data () {
      return {
        collator: new Intl.Collator(eXo.env.portal.language, { numeric: true, sensitivity: 'base' }),
        selectedTemplateId: null,
      };
    },
    computed: {
      blankTemplates () {
        const items = this.pageTemplates.filter(item => item.category === 'blank');
        items.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return items;
      },
      defaultTemplates () {
        const items = this.pageTemplates.filter(item => item.category === 'default');
        items.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return items;
      },
      customizedTemplates () {
        const items = this.pageTemplates.filter(item => item.category !== 'blank' && item.category !== 'default');
        items.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return items;
      },
    },
    created () {
      this.selectedTemplateId = this.value;
    },
    methods: {
      selectTemplate (value) {
        this.selectedTemplateId = value;
        this.$emit('input', this.selectedTemplateId);
      },
    },
  };
</script>
