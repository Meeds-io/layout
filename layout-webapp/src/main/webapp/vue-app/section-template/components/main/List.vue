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
  <div class="application-layout-style">
    <v-data-table
      v-model="$root.selectedSectionTemplates"
      :headers="headers"
      :items="filteredSectionTemplates"
      :loading="loading"
      :disable-sort="$root.isMobile"
      :hide-default-header="$root.isMobile"
      :custom-sort="applySortOnItems"
      :show-select="!$root.isMobile"
      must-sort
      disable-pagination
      hide-default-footer
      class="application-body sectionTemplatesTable px-5">
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
      <template v-if="$root.selectedSectionTemplates.length" slot="body.prepend">
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
        <section-template-item
          :key="props.item.id"
          :section-template="props.item"
          :selected="props.isSelected"
          :select="props.select" />
      </template>
    </v-data-table>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :title="$t('sectionTemplates.label.confirmDeleteTitle')"
      :message="$t('sectionTemplates.label.confirmDeleteMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('sectionTemplates.label.confirm')"
      :cancel-label="$t('sectionTemplates.label.cancel')"
      @ok="deleteSectionTemplate(sectionTemplateToDelete)"
      @closed="sectionTemplateToDelete = null" />
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
    sectionTemplateToDelete: null,
  }),
  computed: {
    loading() {
      return !!this.$root.loading;
    },
    headers() {
      return (this.$root.isMobile && [
        {
          text: this.$t('sectionTemplates.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'section-template-name-header',
          width: '100%'
        },
      ]) || (this.$vuetify.breakpoint.lgAndDown && [
        {
          text: '',
          value: 'illustrationId',
          align: 'center',
          sortable: false,
          class: 'section-template-illustration-header',
          width: '75px'
        },
        {
          text: this.$t('sectionTemplates.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'section-template-name-header',
          width: 'auto'
        },
        {
          text: this.$t('sectionTemplates.label.status'),
          value: 'disabled',
          align: 'center',
          sortable: true,
          class: 'section-template-status-header',
          width: '75px'
        },
        {
          text: this.$t('sectionTemplates.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'section-template-actions-header',
          width: '75px'
        },
      ]) || [
        {
          text: '',
          value: 'illustrationId',
          align: 'center',
          sortable: false,
          class: 'section-template-illustration-header',
          width: '75px'
        },
        {
          text: this.$t('sectionTemplates.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'section-template-name-header',
          width: 'auto'
        },
        {
          text: this.$t('sectionTemplates.label.description'),
          value: 'description',
          align: 'left',
          sortable: false,
          class: 'section-template-description-header',
          width: 'auto'
        },
        {
          text: this.$t('sectionTemplates.label.category'),
          value: 'category',
          align: 'center',
          sortable: true,
          class: 'section-template-category-header text-no-wrap',
          width: '90px'
        },
        {
          text: this.$t('sectionTemplates.label.status'),
          value: 'disabled',
          align: 'center',
          sortable: true,
          class: 'section-template-status-header text-no-wrap',
          width: '90px'
        },
        {
          text: this.$t('sectionTemplates.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'section-template-actions-header text-no-wrap',
          width: '90px'
        },
      ];
    },
    sectionTemplates() {
      return this.$root.sectionTemplates;
    },
    noEmptySectionTemplates() {
      const sectionTemplates = this.sectionTemplates?.filter?.(t => t.name) || [];
      sectionTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return sectionTemplates;
    },
    filteredSectionTemplates() {
      return this.keyword?.length && this.noEmptySectionTemplates.filter(t => {
        const name = this.$te(t.name) ? this.$t(t.name) : t.name;
        const description = this.$te(t.description) ? this.$t(t.description) : t.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || this.noEmptySectionTemplates;
    },
    nameToDelete() {
      return this.sectionTemplateToDelete && this.$te(this.sectionTemplateToDelete?.name) ? this.$t(this.sectionTemplateToDelete?.name) : this.sectionTemplateToDelete?.name;
    },
    selectionLabel() {
      if (this.$root.allSectionTemplatesSelected) {
        return this.$t('sectionTemplates.label.allSectionTemplatesSelected', {
          0: `<strong>${this.$root.sectionTemplatesSize}</strong>`,
        });
      } else {
        return this.$t('sectionTemplates.label.selectedSectionTemplatesCount', {
          0: `<strong>${this.$root.selectedSectionTemplates.length}</strong>`,
        });
      }
    },
  },
  watch: {
    keyword() {
      this.$root.allSectionTemplatesSelected = false;
      this.$root.selectedSectionTemplates = [];
    },
  },
  created() {
    this.$root.$on('section-template-delete', this.deleteSectionTemplateConfirm);
    this.$root.$on('section-template-restore', this.restoreSectionTemplate);
  },
  beforeDestroy() {
    this.$root.$off('section-template-delete', this.deleteSectionTemplateConfirm);
    this.$root.$off('section-template-restore', this.restoreSectionTemplate);
  },
  methods: {
    applySortOnItems(sectionTemplates, sortFields, sortDescendings) {
      for (let i = 0; i < sortFields.length; i++) {
        sectionTemplates = this.applySortOnItemsUsingField(sectionTemplates, sortFields[i], sortDescendings[i]);
      }
      return sectionTemplates;
    },
    applySortOnItemsUsingField(sectionTemplates, field, desc) {
      if (field === 'name') {
        sectionTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      } else if (field === 'disabled') {
        sectionTemplates.sort((a, b) => (a.disabled ? 0 : 1) - (b.disabled ? 0 : 1));
      }
      if (desc) {
        sectionTemplates.reverse();
      }
      return sectionTemplates;
    },
    deleteSectionTemplateConfirm(sectionTemplate) {
      this.sectionTemplateToDelete = sectionTemplate;
      if (this.sectionTemplateToDelete) {
        this.$refs.deleteConfirmDialog.open();
      }
    },
    deleteSectionTemplate(sectionTemplate) {
      this.loading = true;
      this.$sectionTemplateService.deleteSectionTemplate(sectionTemplate.id)
        .then(() => {
          this.$root.$emit('section-template-deleted', sectionTemplate);
          this.$root.$emit('alert-message', this.$t('sectionTemplates.delete.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('sectionTemplates.delete.error'), 'error'))
        .finally(() => this.loading = false);
    },
    restoreSectionTemplate(id) {
      this.loading = true;
      this.$sectionTemplateService.restoreSectionTemplate(id)
        .then(() => {
          this.$root.$emit('section-template-restored', id);
          this.$root.$emit('alert-message', this.$t('sectionTemplates.restore.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('sectionTemplates.restore.error'), 'error'))
        .finally(() => this.loading = false);
    },
  },
};
</script>
