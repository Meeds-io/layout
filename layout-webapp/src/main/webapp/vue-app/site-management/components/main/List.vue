<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <v-data-table
    class="application-body sitesTable px-5"
    :custom-sort="!$root.isMobile && applySortOnItems"
    disable-pagination
    :disable-sort="$root.isMobile"
    :headers="headers"
    hide-default-footer
    :hide-default-header="$root.isMobile"
    :items="filteredSites"
    :loading="loading"
    :must-sort="!$root.isMobile">
    <template #item="props">
      <site-management-item
        :key="props.item.id"
        :site="props.item" />
    </template>
  </v-data-table>
</template>
<script>
  export default {
    props: {
      sites: {
        type: Array,
        default: () => [],
      },
      keyword: {
        type: String,
        default: null,
      },
      loading: {
        type: Boolean,
        default: false,
      },
    },
    computed: {
      headers () {
        return (this.$root.isMobile && [
          {
            text: '',
            value: 'icon',
            align: 'left',
            sortable: false,
            class: 'site-icon-header',
            width: '75px',
          },
          {
            text: this.$t('sites.label.name'),
            value: 'name',
            align: 'left',
            sortable: false,
            class: 'site-name-header',
            width: 'auto',
          },
          {
            text: this.$t('sites.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-actions-header',
            width: '75px',
          },
        ]) || (eXo.vuetify.display.lgAndDown.value && [
          {
            text: '',
            value: 'icon',
            align: 'left',
            sortable: false,
            class: 'site-icon-header',
            width: '35px',
          },
          {
            text: this.$t('sites.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'site-name-header',
            width: 'auto',
          },
          {
            text: this.$t('sites.label.description'),
            value: 'description',
            align: 'left',
            sortable: false,
            class: 'site-description-header',
            width: '50%',
          },
          {
            text: this.$t('sites.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-actions-header',
            width: '75px',
          },
        ]) || [
          {
            text: '',
            value: 'icon',
            align: 'left',
            sortable: false,
            class: 'site-icon-header ps-0',
            width: '35px',
          },
          {
            text: this.$t('sites.label.name'),
            value: 'displayName',
            align: 'left',
            sortable: true,
            class: 'site-name-header',
            width: 'auto',
          },
          {
            text: this.$t('sites.label.description'),
            value: 'description',
            align: 'left',
            sortable: false,
            class: 'site-description-header',
            width: '50%',
          },
          {
            text: this.$t('sites.label.navigation'),
            value: 'navigation',
            align: 'center',
            sortable: false,
            class: 'site-navigation-header',
            width: '75px',
          },
          {
            text: this.$t('sites.label.permission'),
            value: 'permission',
            align: 'center',
            sortable: false,
            class: 'site-permission-header',
            width: '75px',
          },
          {
            text: this.$t('sites.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-actions-header',
            width: '75px',
          },
        ];
      },
      filteredSites () {
        return this.keyword?.length && this.sites.filter(s => {
          const name = this.$te(s.name) ? this.$t(s.name) : s.name;
          const description = this.$te(s.description) ? this.$t(s.description) : s.description;
          return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
            || eXo.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
        }) || this.sites;
      },
    },
    methods: {
      applySortOnItems (sites, sortFields, sortDescendings) {
        for (let i = 0; i < sortFields.length; i++) {
          sites = this.applySortOnItemsUsingField(sites, sortFields[i], sortDescendings[i]);
        }
        return sites;
      },
      applySortOnItemsUsingField (sites, field, desc) {
        if (field === 'name') {
          sites.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        }
        if (desc) {
          sites.reverse();
        }
        return sites;
      },
    },
  };
</script>
