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
    <v-data-table
      class="application-body portletsTable px-5"
      :custom-sort="applySortOnItems"
      disable-pagination
      :disable-sort="$root.isMobile"
      :headers="headers"
      hide-default-footer
      :hide-default-header="$root.isMobile"
      :items="filteredPortlets"
      :loading="loading"
      must-sort>
      <template #item="props">
        <portlets-item
          :key="props.item.id"
          :portlet="props.item" />
      </template>
    </v-data-table>
  </div>
</template>
<script>
  export default {
    props: {
      keyword: {
        type: String,
        default: null,
      },
    },
    computed: {
      portlets () {
        return this.$root.portlets;
      },
      loading () {
        return !!this.$root.loading;
      },
      headers () {
        return (this.$root.isMobile && [
          {
            text: this.$t('portlets.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'portlet-name-header',
            width: '100%',
          },
          {
            text: this.$t('portlets.label.instances'),
            value: 'instances',
            align: 'center',
            sortable: false,
            class: 'portlet-instances-header',
            width: '75px',
          },
        ]) || (eXo.vuetify.display.lgAndDown.value && [
          {
            text: '',
            value: 'illustrationId',
            align: 'center',
            sortable: false,
            class: 'portlet-illustration-header',
            width: '75px',
          },
          {
            text: this.$t('portlets.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'portlet-name-header',
            width: 'auto',
          },
          {
            text: this.$t('portlets.label.instances'),
            value: 'instances',
            align: 'center',
            sortable: false,
            class: 'portlet-instances-header',
            width: '75px',
          },
          {
            text: this.$t('portlets.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'portlet-actions-header',
            width: '75px',
          },
        ]) || [
          {
            text: '',
            value: 'illustrationId',
            align: 'center',
            sortable: false,
            class: 'portlet-illustration-header',
            width: '75px',
          },
          {
            text: this.$t('portlets.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'portlet-name-header',
            width: 'auto',
          },
          {
            text: this.$t('portlets.label.description'),
            value: 'description',
            align: 'left',
            sortable: false,
            class: 'portlet-description-header',
            width: 'auto',
          },
          {
            text: this.$t('portlets.label.instances'),
            value: 'instances',
            align: 'center',
            sortable: false,
            class: 'portlet-instances-header text-no-wrap',
            width: '90px',
          },
          {
            text: this.$t('portlets.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'portlet-actions-header text-no-wrap',
            width: '90px',
          },
        ];
      },
      noEmptyPortlets () {
        const portlets = this.portlets?.filter?.(t => t.name) || [];
        portlets.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return portlets;
      },
      filteredPortlets () {
        return this.keyword?.length && this.noEmptyPortlets.filter(t => {
          const name = this.$te(t.name) ? this.$t(t.name) : t.name;
          const description = this.$te(t.description) ? this.$t(t.description) : t.description;
          return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
            || eXo.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
        }) || this.noEmptyPortlets;
      },
    },
    methods: {
      applySortOnItems (portlets, sortFields, sortDescendings) {
        for (let i = 0; i < sortFields.length; i++) {
          portlets = this.applySortOnItemsUsingField(portlets, sortFields[i], sortDescendings[i]);
        }
        return portlets;
      },
      applySortOnItemsUsingField (portlets, field, desc) {
        if (field === 'name') {
          portlets.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        }
        if (desc) {
          portlets.reverse();
        }
        return portlets;
      },
    },
  };
</script>
