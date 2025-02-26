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
  <div class="application-layout-style">
    <v-data-table
      v-model="$root.selectedPortletInstances"
      :headers="headers"
      :items="filteredPortletInstances"
      :loading="loading"
      :disable-sort="$root.isMobile"
      :hide-default-header="$root.isMobile"
      :custom-sort="applySortOnItems"
      :show-select="!$root.isMobile"
      must-sort
      disable-pagination
      hide-default-footer
      class="application-body portletInstancesTable px-5">
      <template slot="header.data-table-select" slot-scope="{on, props}">
        <v-checkbox
          v-on="on"
          v-bind="props"
          on-icon="fas fa-check-square fa-lg primary--text"
          indeterminate-icon="fas fa-minus-square fa-lg"
          off-icon="far fa-square fa-lg"
          class="my-auto pt-2"
          @change="on.input" />
      </template>
      <template v-if="$root.selectedPortletInstances.length" slot="body.prepend">
        <tr>
          <td :colspan="headers.length + 1" class="px-0">
            <v-alert
              :icon="false"
              class="ma-0 ps-5 no-border-radius"
              border="left"
              type="info"
              colored-border>
              <div v-html="selectionLabel"></div>
            </v-alert>
          </td>
        </tr>
      </template>
      <template slot="item" slot-scope="props">
        <portlets-instance-item
          :key="props.item.id"
          :portlet-instance="props.item"
          :selected="props.isSelected"
          :select="props.select" />
      </template>
    </v-data-table>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :title="$t('portlets.label.confirmDeleteTitle')"
      :message="$t('portlets.label.confirmDeleteMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('portlets.label.confirm')"
      :cancel-label="$t('portlets.label.cancel')"
      @ok="deletePortletInstance(portletInstanceToDelete)"
      @closed="portletInstanceToDelete = null" />
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
  data: () => ({
    portletInstanceToDelete: null,
    categoryId: 0,
  }),
  computed: {
    loading() {
      return !!this.$root.loading;
    },
    headers() {
      return (this.$root.isMobile && [
        {
          text: this.$t('portlets.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'portlet-instance-name-header',
          width: '100%'
        },
      ]) || (this.$vuetify.breakpoint.lgAndDown && [
        {
          text: '',
          value: 'illustrationId',
          align: 'center',
          sortable: false,
          class: 'portlet-instance-illustration-header',
          width: '75px'
        },
        {
          text: this.$t('portlets.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'portlet-instance-name-header',
          width: 'auto'
        },
        {
          text: this.$t('portlets.label.status'),
          value: 'disabled',
          align: 'center',
          sortable: true,
          class: 'portlet-instance-status-header',
          width: '75px'
        },
        {
          text: this.$t('portlets.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'portlet-instance-actions-header',
          width: '75px'
        },
      ]) || [
        {
          text: '',
          value: 'illustrationId',
          align: 'center',
          sortable: false,
          class: 'portlet-instance-illustration-header',
          width: '75px'
        },
        {
          text: this.$t('portlets.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'portlet-instance-name-header',
          width: 'auto'
        },
        {
          text: this.$t('portlets.label.description'),
          value: 'description',
          align: 'left',
          sortable: false,
          class: 'portlet-instance-description-header',
          width: 'auto'
        },
        {
          text: this.$t('portlets.label.status'),
          value: 'disabled',
          align: 'center',
          sortable: true,
          class: 'portlet-instance-status-header text-no-wrap',
          width: '90px'
        },
        {
          text: this.$t('portlets.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'portlet-instance-actions-header text-no-wrap',
          width: '90px'
        },
      ];
    },
    portletInstances() {
      return this.categoryId && this.$root.portletInstances.filter(a => a.categoryId === this.categoryId) || this.$root.portletInstances;
    },
    noEmptyPortletInstances() {
      const portletInstances = this.portletInstances?.filter?.(t => t.name) || [];
      portletInstances.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return portletInstances;
    },
    filteredPortletInstances() {
      return this.keyword?.length && this.noEmptyPortletInstances.filter(t => {
        const name = this.$te(t.name) ? this.$t(t.name) : t.name;
        const description = this.$te(t.description) ? this.$t(t.description) : t.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || this.noEmptyPortletInstances;
    },
    nameToDelete() {
      return this.portletInstanceToDelete && this.$te(this.portletInstanceToDelete?.name) ? this.$t(this.portletInstanceToDelete?.name) : this.portletInstanceToDelete?.name;
    },
    selectionLabel() {
      if (this.$root.allPortletInstancesSelected) {
        return this.$t('portletInstance.label.allPortletInstancesSelected', {
          0: `<strong>${this.$root.portletInstancesSize}</strong>`,
        });
      } else {
        return this.$t('portletInstance.label.selectedPortletInstancesCount', {
          0: `<strong>${this.$root.selectedPortletInstances.length}</strong>`,
        });
      }
    },
    selectedPortletInstances() {
      return this.$root.selectedPortletInstances;
    },
  },
  watch: {
    keyword() {
      this.$root.allPortletInstancesSelected = false;
      this.$root.selectedPortletInstances = [];
    },
  },
  created() {
    this.$root.$on('portlet-instance-delete', this.deletePortletInstanceConfirm);
    this.$root.$on('portlet-instance-category-selected', this.selectCategoryId);
  },
  beforeDestroy() {
    this.$root.$off('portlet-instance-delete', this.deletePortletInstanceConfirm);
    this.$root.$off('portlet-instance-category-selected', this.selectCategoryId);
  },
  methods: {
    applySortOnItems(portletInstances, sortFields, sortDescendings) {
      for (let i = 0; i < sortFields.length; i++) {
        portletInstances = this.applySortOnItemsUsingField(portletInstances, sortFields[i], sortDescendings[i]);
      }
      return portletInstances;
    },
    applySortOnItemsUsingField(portletInstances, field, desc) {
      if (field === 'name') {
        portletInstances.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      } else if (field === 'disabled') {
        portletInstances.sort((a, b) => (a.disabled ? 0 : 1) - (b.disabled ? 0 : 1));
      }
      if (desc) {
        portletInstances.reverse();
      }
      return portletInstances;
    },
    deletePortletInstanceConfirm(portletInstance) {
      this.portletInstanceToDelete = portletInstance;
      if (this.portletInstanceToDelete) {
        this.$refs.deleteConfirmDialog.open();
      }
    },
    selectCategoryId(id) {
      if (this.categoryId !== id) {
        this.categoryId = id;
        this.$root.allPortletInstancesSelected = false;
        this.$root.selectedPortletInstances = [];
      }
    },
    deletePortletInstance(portletInstance) {
      this.loading = true;
      this.$portletInstanceService.deletePortletInstance(portletInstance.id)
        .then(() => {
          this.$root.$emit('portlet-instance-deleted', portletInstance);
          this.$root.$emit('alert-message', this.$t('portlets.delete.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('portlets.delete.error'), 'error'))
        .finally(() => this.loading = false);
    },
  },
};
</script>
