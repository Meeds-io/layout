<template>
  <div>
    <v-data-table
      class="pageTemplatesTable px-5"
      :custom-sort="applySortOnItems"
      disable-pagination
      :disable-sort="$root.isMobile"
      :headers="headers"
      hide-default-footer
      :hide-default-header="$root.isMobile"
      :items="filteredPageTemplates"
      :loading="loading"
      must-sort>
      <template #item="props">
        <page-templates-management-item
          :key="props.item.id"
          :page-template="props.item" />
      </template>
    </v-data-table>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :cancel-label="$t('pageTemplate.label.cancel')"
      :message="$t('pageTemplate.label.confirmDeleteMessage', {0: `<br><strong>${nameToDelete}</strong>`})"
      :ok-label="$t('pageTemplate.label.confirm')"
      :title="$t('pageTemplate.label.confirmDeleteTitle')"
      @closed="pageTemplateToDelete = null"
      @ok="deletePageTemplate(pageTemplateToDelete)" />
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
      pageTemplates: [],
      pageTemplateToDelete: null,
      loading: false,
      creating: false,
      contentLoaded: false,
      collator: new Intl.Collator(eXo.env.portal.language, { numeric: true, sensitivity: 'base' }),
    }),
    computed: {
      headers () {
        return this.$root.isMobile && [
          {
            text: this.$t('pageTemplates.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'page-template-name-header',
            width: '20%',
          },
          {
            text: this.$t('pageTemplates.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'page-template-actions-header',
            width: '50px',
          },
        ] || [
          {
            text: '',
            value: 'illustrationId',
            align: 'center',
            sortable: false,
            class: 'page-template-illustration-header',
            width: '60px',
          },
          {
            text: this.$t('pageTemplates.label.name'),
            value: 'name',
            align: 'left',
            sortable: true,
            class: 'page-template-name-header',
            width: 'auto',
          },
          {
            text: this.$t('pageTemplates.label.description'),
            value: 'description',
            align: 'center',
            sortable: false,
            class: 'page-template-description-header',
            width: 'auto',
          },
          {
            text: this.$t('pageTemplates.label.category'),
            value: 'category',
            align: 'center',
            sortable: true,
            class: 'page-template-category-header',
            width: '120px',
          },
          {
            text: this.$t('pageTemplates.label.status'),
            value: 'disabled',
            align: 'center',
            sortable: true,
            class: 'page-template-category-header text-no-wrap',
            width: '90px',
          },
          {
            text: this.$t('pageTemplates.label.actions'),
            value: 'actions',
            align: 'center',
            sortable: false,
            class: 'page-template-actions-header text-no-wrap',
            width: '90px',
          },
        ];
      },
      noEmptyPageTemplates () {
        const pageTemplates = this.pageTemplates?.filter?.(t => t.name) || [];
        pageTemplates.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return pageTemplates;
      },
      filteredPageTemplates () {
        return this.keyword?.length && this.noEmptyPageTemplates.filter(t => {
          const name = this.$te(t.name) ? this.$t(t.name) : t.name;
          const description = this.$te(t.description) ? this.$t(t.description) : t.description;
          return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
            || eXo.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
        }) || this.noEmptyPageTemplates;
      },
      nameToDelete () {
        return this.pageTemplateToDelete && this.$te(this.pageTemplateToDelete?.name) ? this.$t(this.pageTemplateToDelete?.name) : this.pageTemplateToDelete?.name;
      },
    },
    watch: {
      creating () {
        this.$emit('creating', this.creating);
      },
    },
    created () {
      this.$root.$on('page-templates-deleted', this.refreshPageTemplates);
      this.$root.$on('page-templates-created', this.refreshPageTemplates);
      this.$root.$on('page-templates-updated', this.refreshPageTemplates);
      this.$root.$on('page-templates-enabled', this.refreshPageTemplates);
      this.$root.$on('page-templates-disabled', this.refreshPageTemplates);
      this.$root.$on('page-templates-saved', this.refreshPageTemplates);
      this.$root.$on('page-templates-delete', this.deletePageTemplateConfirm);
      this.$root.$on('page-templates-create', this.createPageTemplate);
      this.refreshPageTemplates();
      this.retrieveColumnsTemplate();
    },
    beforeUnmount () {
      this.$root.$off('page-templates-deleted', this.refreshPageTemplates);
      this.$root.$off('page-templates-created', this.refreshPageTemplates);
      this.$root.$off('page-templates-updated', this.refreshPageTemplates);
      this.$root.$off('page-templates-enabled', this.refreshPageTemplates);
      this.$root.$off('page-templates-disabled', this.refreshPageTemplates);
      this.$root.$off('page-templates-saved', this.refreshPageTemplates);
      this.$root.$off('page-templates-delete', this.deletePageTemplateConfirm);
      this.$root.$off('page-templates-create', this.createPageTemplate);
    },
    methods: {
      applySortOnItems (pageTemplates, sortFields, sortDescendings) {
        for (let i = 0; i < sortFields.length; i++) {
          pageTemplates = this.applySortOnItemsUsingField(pageTemplates, sortFields[i], sortDescendings[i]);
        }
        return pageTemplates;
      },
      applySortOnItemsUsingField (pageTemplates, field, desc) {
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
      deletePageTemplateConfirm (pageTemplate) {
        this.pageTemplateToDelete = pageTemplate;
        if (this.pageTemplateToDelete) {
          this.$refs.deleteConfirmDialog.open();
        }
      },
      refreshPageTemplates () {
        this.loading = true;
        return eXo.$pageTemplateService.getPageTemplates()
          .then(pageTemplates => this.pageTemplates = pageTemplates || [])
          .finally(() => this.loading = false);
      },
      retrieveColumnsTemplate () {
        return eXo.$pageTemplateService.getPageTemplates(true)
          .then(pageTemplates => {
            this.columnsTemplate = pageTemplates?.find?.(t => t.system && t.content.includes('FlexContainer'));
          })
          .finally(() => this.contentLoaded = true);
      },
      deletePageTemplate (pageTemplate) {
        this.loading = true;
        eXo.$pageTemplateService.deletePageTemplate(pageTemplate.id)
          .then(() => {
            this.$root.$emit('page-templates-deleted', pageTemplate);
            this.$root.$emit('alert-message', this.$t('pageTemplate.delete.success'), 'success');
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('pageTemplate.delete.error'), 'error'))
          .finally(() => this.loading = false);
      },
      async createPageTemplate () {
        this.creating = true;
        try {
          const columnsTemplateContent = this.columnsTemplate?.content || '{}';
          const pageTemplate = await eXo.$pageTemplateService.createPageTemplate(columnsTemplateContent, true);
          window.open(`/portal/administration/layout-editor?pageTemplateId=${pageTemplate.id}`, '_blank');
        } finally {
          this.creating = false;
        }
      },
    },
  };
</script>
