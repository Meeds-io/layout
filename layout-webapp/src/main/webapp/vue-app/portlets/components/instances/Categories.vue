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
  <v-list
    class="application-layout-style"
    dense
    :loading="loading"
    min-width="225"
    nav
    :role="null">
    <v-list-item class="pe-0" :role="null">
      <v-list-item-content>
        <v-list-item-title class="text-header pb-2">{{ $t('layout.portletInstance.category') }}</v-list-item-title>
      </v-list-item-content>
      <v-list-item-action class="my-auto ms-4">
        <v-btn
          class="mb-2"
          icon
          small
          :title="$t('layout.add')"
          @click="$root.$emit('portlet-instance-category-add')">
          <v-icon class="primary--text" size="16">fa-plus</v-icon>
        </v-btn>
      </v-list-item-action>
    </v-list-item>
    <v-list-item-group
      v-model="selectedCategory"
      color="primary"
      mandatory
      role="list">
      <v-list-item
        :aria-label="$t('layout.portletInstance.category.all.name')"
        :value="0">
        <v-list-item-icon class="my-auto me-4">
          <v-card
            class="transparent d-flex align-center justify-center"
            flat
            width="30">
            <v-icon class="icon-default-color">fa-braille</v-icon>
          </v-card>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>{{ $t('layout.portletInstance.category.all.name') }}</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <portlets-instance-category
        v-for="category in filteredCategories"
        :key="category.id"
        :category="category" />
    </v-list-item-group>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :cancel-label="$t('portlets.label.cancel')"
      :message="$t('portlets.label.confirmDeleteCategoryMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('portlets.label.confirm')"
      :title="$t('portlets.label.confirmDeleteCategoryTitle')"
      @closed="portletInstanceCategoryToDelete = null"
      @ok="deletePortletInstanceCategory(portletInstanceCategoryToDelete)" />
  </v-list>
</template>
<script>
  export default {
    data: () => ({
      loading: false,
      selectedCategory: null,
      portletInstanceCategoryToDelete: null,
    }),
    computed: {
      nameToDelete () {
        return this.portletInstanceCategoryToDelete?.name;
      },
      categories () {
        return this.$root.portletInstanceCategories;
      },
      filteredCategories () {
        const categories = this.categories?.filter?.(c => c.name) || [];
        categories.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return categories;
      },
    },
    watch: {
      selectedCategory () {
        this.$root.$emit('portlet-instance-category-selected', this.selectedCategory);
      },
    },
    created () {
      this.$root.$on('portlet-instance-category-delete', this.deletePortletInstanceCategoryConfirm);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-category-delete', this.deletePortletInstanceCategoryConfirm);
    },
    methods: {
      deletePortletInstanceCategoryConfirm (portletInstanceCatgory) {
        this.portletInstanceCategoryToDelete = portletInstanceCatgory;
        if (this.portletInstanceCategoryToDelete) {
          this.$refs.deleteConfirmDialog.open();
        }
      },
      deletePortletInstanceCategory (portletInstanceCategory) {
        this.loading = true;
        eXo.$portletInstanceCategoryService.deletePortletInstanceCategory(portletInstanceCategory.id)
          .then(() => {
            this.$root.$emit('portlet-instance-category-deleted', portletInstanceCategory);
            this.$root.$emit('alert-message', this.$t('portlets.category.delete.success'), 'success');
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('portlets.category.delete.error'), 'error'))
          .finally(() => this.loading = false);
      },
    },
  };
</script>