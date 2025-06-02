<template>
  <div>
    <v-data-table
      v-model="$root.selectedPageTemplates"
      :headers="headers"
      :items="filteredPageTemplates"
      :loading="$root.loading"
      :disable-sort="$root.isMobile"
      :hide-default-header="$root.isMobile"
      :custom-sort="applySortOnItems"
      :show-select="!$root.isMobile"
      must-sort
      disable-pagination
      hide-default-footer
      class="pageTemplatesTable px-5">
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
      <template v-if="$root.selectedPageTemplates.length" slot="body.prepend">
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
        <page-templates-management-item
          :key="props.item.id"
          :page-template="props.item"
          :selected="props.isSelected"
          :select="props.select" />
      </template>
    </v-data-table>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :title="$t('pageTemplate.label.confirmDeleteTitle')"
      :message="$t('pageTemplate.label.confirmDeleteMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('pageTemplate.label.confirm')"
      :cancel-label="$t('pageTemplate.label.cancel')"
      @ok="deletePageTemplate(pageTemplateToDelete)"
      @closed="pageTemplateToDelete = null" />
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
    pageTemplateToDelete: null,
    creating: false,
    contentLoaded: false,
  }),
  computed: {
    headers() {
      return this.$root.isMobile && [
        {
          text: this.$t('pageTemplates.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'page-template-name-header',
          width: '20%'
        },
        {
          text: this.$t('pageTemplates.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'page-template-actions-header',
          width: '50px'
        },
      ] || [
        {
          text: '',
          value: 'illustrationId',
          align: 'center',
          sortable: false,
          class: 'page-template-illustration-header',
          width: '60px'
        },
        {
          text: this.$t('pageTemplates.label.name'),
          value: 'name',
          align: 'left',
          sortable: true,
          class: 'page-template-name-header',
          width: 'auto'
        },
        {
          text: this.$t('pageTemplates.label.description'),
          value: 'description',
          align: 'center',
          sortable: false,
          class: 'page-template-description-header',
          width: 'auto'
        },
        {
          text: this.$t('pageTemplates.label.category'),
          value: 'category',
          align: 'center',
          sortable: true,
          class: 'page-template-category-header',
          width: '120px'
        },
        {
          text: this.$t('pageTemplates.label.status'),
          value: 'disabled',
          align: 'center',
          sortable: true,
          class: 'page-template-category-header text-no-wrap',
          width: '90px'
        },
        {
          text: this.$t('pageTemplates.label.actions'),
          value: 'actions',
          align: 'center',
          sortable: false,
          class: 'page-template-actions-header text-no-wrap',
          width: '90px'
        },
      ];
    },
    noEmptyPageTemplates() {
      const pageTemplates = this.$root.pageTemplates?.filter?.(t => t.name) || [];
      pageTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return pageTemplates;
    },
    filteredPageTemplates() {
      return this.keyword?.length && this.noEmptyPageTemplates.filter(t => {
        const name = this.$te(t.name) ? this.$t(t.name) : t.name;
        const description = this.$te(t.description) ? this.$t(t.description) : t.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || this.noEmptyPageTemplates;
    },
    nameToDelete() {
      return this.pageTemplateToDelete && this.$te(this.pageTemplateToDelete?.name) ? this.$t(this.pageTemplateToDelete?.name) : this.pageTemplateToDelete?.name;
    },
    selectionLabel() {
      if (this.$root.allPageTemplatesSelected) {
        return this.$t('pageTemplate.label.allPageTemplatesSelected', {
          0: `<strong>${this.$root.pageTemplatesSize}</strong>`,
        });
      } else {
        return this.$t('pageTemplate.label.selectedPageTemplatesCount', {
          0: `<strong>${this.$root.selectedPageTemplates.length}</strong>`,
        });
      }
    },
  },
  watch: {
    creating() {
      this.$emit('creating', this.creating);
    },
    keyword() {
      this.$root.allPageTemplatesSelected = false;
      this.$root.selectedPageTemplates = [];
    },
  },
  created() {
    this.$root.$on('page-templates-delete', this.deletePageTemplateConfirm);
    this.$root.$on('page-templates-restore', this.restorePageTemplate);
    this.$root.$on('page-templates-create', this.createPageTemplate);
  },
  beforeDestroy() {
    this.$root.$off('page-templates-delete', this.deletePageTemplateConfirm);
    this.$root.$off('page-templates-restore', this.restorePageTemplate);
    this.$root.$off('page-templates-create', this.createPageTemplate);
  },
  methods: {
    applySortOnItems(pageTemplates, sortFields, sortDescendings) {
      for (let i = 0; i < sortFields.length; i++) {
        pageTemplates = this.applySortOnItemsUsingField(pageTemplates, sortFields[i], sortDescendings[i]);
      }
      return pageTemplates;
    },
    applySortOnItemsUsingField(pageTemplates, field, desc) {
      if (field === 'name') {
        pageTemplates.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      } else if (field === 'category') {
        pageTemplates.sort((a, b) => {
          const categoryA = this.$te(`layout.pageTemplate.category.${a.category || 'customized'}`) ? this.$t(`layout.pageTemplate.category.${a.category || 'customized'}`) : this.pageTemplate.category;
          const categoryB = this.$te(`layout.pageTemplate.category.${b.category || 'customized'}`) ? this.$t(`layout.pageTemplate.category.${b.category || 'customized'}`) : this.pageTemplate.category;
          return this.collator.compare(categoryA.toLowerCase(), categoryB.toLowerCase());
        });
      } else if (field === 'disabled') {
        pageTemplates.sort((a, b) => (a.disabled ? 0 : 1) - (b.disabled ? 0 : 1));
      }
      if (desc) {
        pageTemplates.reverse();
      }
      return pageTemplates;
    },
    deletePageTemplateConfirm(pageTemplate) {
      this.pageTemplateToDelete = pageTemplate;
      if (this.pageTemplateToDelete) {
        this.$refs.deleteConfirmDialog.open();
      }
    },
    deletePageTemplate(pageTemplate) {
      this.loading = true;
      this.$pageTemplateService.deletePageTemplate(pageTemplate.id)
        .then(() => {
          this.$root.$emit('page-templates-deleted', pageTemplate);
          this.$root.$emit('alert-message', this.$t('pageTemplate.delete.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('pageTemplate.delete.error'), 'error'))
        .finally(() => this.loading = false);
    },
    restorePageTemplate(id) {
      this.loading = true;
      this.$pageTemplateService.restorePageTemplate(id)
        .then(() => {
          this.$root.$emit('page-templates-resored', id);
          this.$root.$emit('alert-message', this.$t('pageTemplate.restore.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('pageTemplate.restore.error'), 'error'))
        .finally(() => this.loading = false);
    },
    async createPageTemplate() {
      this.creating = true;
      try {
        const columnsTemplateContent = this.$root.columnsTemplate?.content || '{}';
        const pageTemplate = await this.$pageTemplateService.createPageTemplate(columnsTemplateContent, true);
        window.open(`/portal/administration/layout-editor?pageTemplateId=${pageTemplate.id}`, '_blank');
      } finally {
        this.creating = false;
      }
    },
  },
};
</script>
