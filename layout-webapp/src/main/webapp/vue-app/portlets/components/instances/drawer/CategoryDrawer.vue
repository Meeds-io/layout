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
  <exo-drawer
    id="portletInstanceCategoryDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    :loading="saving"
    right>
    <template #title>
      <span class="text-wrap">{{ isNew && $t('layout.portletInstance.category.drawerTitie.add') || $t('layout.portletInstance.category.drawerTitie.edit') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <translation-text-field
          id="pageTemplateTitle"
          ref="title"
          v-model="titleTranslations"
          v-model:field-value="title"
          back-icon
          class="width-auto flex-grow-1 pb-1"
          drawer-title="layout.portletInstance.category.nameTranslationDrawerTitle"
          field-name="title"
          :maxlength="maxTitleLength"
          :object-id="categoryId"
          object-type="portletInstanceCategory"
          :placeholder="$t('layout.portletInstance.category.namePlaceholder')"
          required>
          <template #title>
            <div class="text-subtitle-1">
              {{ $t('layout.portletInstance.category.name') }}
            </div>
          </template>
        </translation-text-field>
        <font-icon-input
          v-model="categoryIcon"
          class="mt-4" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex me-2">
        <v-spacer />
        <v-btn
          class="btn mx-1"
          @click="close">
          {{ $t('layout.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          :loading="saving"
          @click="save">
          {{ $t('layout.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      categoryIcon: null,
      categoryId: null,
      title: null,
      titleTranslations: {},
      maxTitleLength: 100,
      saving: false,
      isNew: false,
    }),
    created () {
      this.$root.$on('portlet-instance-category-add', this.open);
      this.$root.$on('portlet-instance-category-edit', this.open);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-category-add', this.open);
      this.$root.$off('portlet-instance-category-edit', this.open);
    },
    methods: {
      open (category) {
        this.$root.$emit('close-alert-message');
        this.isNew = !category;
        this.categoryId = category?.id || null;
        this.title = null;
        this.titleTranslations = {};
        this.categoryIcon = category?.icon || 'fa-braille';
        this.$nextTick().then(() => this.$refs.drawer.open());
      },
      close () {
        this.$refs.drawer.close();
      },
      save () {
        this.saving = true;
        let message = null;
        const saveCategoryRequest =
          this.categoryId ?
            eXo.$portletInstanceCategoryService.getPortletInstanceCategory(this.categoryId)
              .then(category => {
                category.icon = this.categoryIcon;
                return eXo.$portletInstanceCategoryService.updatePortletInstanceCategory(category)
                  .then(() => {
                    this.$root.$emit('portlet-instance-category-updated', category);
                    message = this.$t('layout.portletInstanceCategoryUpdatedSuccessfully');
                    return category;
                  });
              })
            : eXo.$portletInstanceCategoryService.createPortletInstanceCategory({
              icon: this.categoryIcon,
            })
              .then(category => {
                this.$root.$emit('portlet-instance-category-created', category);
                message = this.$t('layout.portletInstanceCategoryCreatedSuccessfully');
                return category;
              });
        return saveCategoryRequest
          .then(category => {
            if (category) {
              this.categoryId = category.id;
              return this.$nextTick();
            }
          })
          .then(() => eXo.$translationService.saveTranslations('portletInstanceCategory', this.categoryId, 'title', this.titleTranslations))
          .then(() => {
            this.$root.$emit('portlet-instance-category-saved', this.categoryId);
            this.$root.$emit('alert-message', message, 'success');
            this.close();
          })
          .finally(() => this.saving = false);
      },
    },
  };
</script>