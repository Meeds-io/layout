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
      class="application-body siteTemplatesTable px-5"
      :custom-sort="applySortOnItems"
      disable-pagination
      :disable-sort="$root.isMobile"
      :headers="headers"
      hide-default-footer
      :hide-default-header="$root.isMobile"
      :items="filteredSiteTemplates"
      :loading="loading"
      must-sort>
      <template #item="props">
        <site-template-item
          :key="props.item.id"
          :site-template="props.item" />
      </template>
    </v-data-table>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :cancel-label="$t('siteTemplates.label.cancel')"
      :message="$t('siteTemplates.label.confirmDeleteMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('siteTemplates.label.confirm')"
      :title="$t('siteTemplates.label.confirmDeleteTitle')"
      @closed="siteTemplateToDelete = null"
      @ok="deleteSiteTemplate(siteTemplateToDelete)" />
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
      siteTemplateToDelete: null,
    }),
    computed: {
      loading () {
        return !!this.$root.loading;
      },
      headers () {
        return (this.$root.isMobile && [
          {
            text: this.$t('siteTemplates.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'site-template-name-header',
            width: '100%',
          },
          {
            text: this.$t('sites.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-template-actions-header',
            width: '75px',
          },
        ]) || (eXo.vuetify.display.lgAndDown.value && [
          {
            text: '',
            value: 'icon',
            align: 'left',
            sortable: false,
            class: 'site-template-illustration-header',
            width: '35px',
          },
          {
            text: this.$t('siteTemplates.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'site-template-name-header ps-0',
            width: 'auto',
          },
          {
            text: this.$t('siteTemplates.label.status'),
            value: 'disabled',
            align: 'center',
            sortable: true,
            class: 'site-template-status-header',
            width: '75px',
          },
          {
            text: this.$t('siteTemplates.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-template-actions-header',
            width: '75px',
          },
        ]) || [
          {
            text: '',
            value: 'illustrationId',
            align: 'center',
            sortable: false,
            class: 'site-template-illustration-header',
            width: '35px',
          },
          {
            text: this.$t('siteTemplates.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'site-template-name-header ps-0',
            width: 'auto',
          },
          {
            text: this.$t('siteTemplates.label.description'),
            value: 'description',
            align: 'left',
            sortable: false,
            class: 'site-template-description-header',
            width: '50%',
          },
          {
            text: this.$t('siteTemplates.label.status'),
            value: 'disabled',
            align: 'center',
            sortable: true,
            class: 'site-template-status-header text-no-wrap',
            width: '90px',
          },
          {
            text: this.$t('siteTemplates.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'site-template-actions-header text-no-wrap',
            width: '90px',
          },
        ];
      },
      siteTemplates () {
        return this.$root.siteTemplates;
      },
      noEmptySiteTemplates () {
        const siteTemplates = this.siteTemplates?.filter?.(t => t.name) || [];
        siteTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return siteTemplates;
      },
      filteredSiteTemplates () {
        return this.keyword?.length && this.noEmptySiteTemplates.filter(t => {
          const name = this.$te(t.name) ? this.$t(t.name) : t.name;
          const description = this.$te(t.description) ? this.$t(t.description) : t.description;
          return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
            || eXo.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
        }) || this.noEmptySiteTemplates;
      },
      nameToDelete () {
        return this.siteTemplateToDelete && this.$te(this.siteTemplateToDelete?.name) ? this.$t(this.siteTemplateToDelete?.name) : this.siteTemplateToDelete?.name;
      },
    },
    created () {
      this.$root.$on('site-template-delete', this.deleteSiteTemplateConfirm);
    },
    beforeUnmount () {
      this.$root.$off('site-template-delete', this.deleteSiteTemplateConfirm);
    },
    methods: {
      applySortOnItems (siteTemplates, sortFields, sortDescendings) {
        for (let i = 0; i < sortFields.length; i++) {
          siteTemplates = this.applySortOnItemsUsingField(siteTemplates, sortFields[i], sortDescendings[i]);
        }
        return siteTemplates;
      },
      applySortOnItemsUsingField (siteTemplates, field, desc) {
        if (field === 'name') {
          siteTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        } else if (field === 'disabled') {
          siteTemplates.sort((a, b) => (a.disabled ? 0 : 1) - (b.disabled ? 0 : 1));
        }
        if (desc) {
          siteTemplates.reverse();
        }
        return siteTemplates;
      },
      deleteSiteTemplateConfirm (siteTemplate) {
        this.siteTemplateToDelete = siteTemplate;
        if (this.siteTemplateToDelete) {
          this.$refs.deleteConfirmDialog.open();
        }
      },
      deleteSiteTemplate (siteTemplate) {
        this.loading = true;
        eXo.$siteTemplateService.deleteSiteTemplate(siteTemplate.id)
          .then(() => {
            this.$root.$emit('site-template-deleted', siteTemplate);
            this.$root.$emit('alert-message', this.$t('siteTemplates.delete.success'), 'success');
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('siteTemplates.delete.error'), 'error'))
          .finally(() => this.loading = false);
      },
    },
  };
</script>
