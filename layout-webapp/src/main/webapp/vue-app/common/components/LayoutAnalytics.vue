<script>
  export default {
    props: {
      applicationName: {
        type: String,
        default: null,
      },
    },
    created () {
      this.$root.$on('page-templates-created', this.handlePageTemplateCreation);
      this.$root.$on('page-templates-updated', this.handlePageTemplateUpdate);
      this.$root.$on('page-templates-deleted', this.handlePageTemplateDeletion);
      this.$root.$on('page-templates-enabled', this.handlePageTemplateEnabled);
      this.$root.$on('page-templates-disabled', this.handlePageTemplateDisabled);
      this.$root.$on('page-layout-created', this.handlePageCreated);
    },
    beforeUnmount () {
      this.$root.$off('page-templates-created', this.handlePageTemplateCreation);
      this.$root.$off('page-templates-updated', this.handlePageTemplateUpdate);
      this.$root.$off('page-templates-deleted', this.handlePageTemplateDeletion);
      this.$root.$off('page-templates-enabled', this.handlePageTemplateEnabled);
      this.$root.$off('page-templates-disabled', this.handlePageTemplateDisabled);
      this.$root.$off('page-layout-created', this.handlePageCreated);
    },
    methods: {
      handlePageCreated (page, pageTemplate) {
        this.sendAnalytics('pageLayout', 'createPageLayout', {
          pageTemplateId: pageTemplate?.id || null,
          pageTemplateName: pageTemplate?.name || null,
          pageTemplateCategory: pageTemplate?.id && (pageTemplate?.category || 'customized') || null,
          pageTemplateDisabled: pageTemplate?.id && `${pageTemplate.disabled}` || null,
          pageTemplateSystem: pageTemplate?.id && `${pageTemplate.system}` || null,
          pageTemplateIllustrationId: pageTemplate?.id && pageTemplate.illustrationId || null,
          pageName: page?.state?.displayName,
          pageReference: page?.key?.ref,
          pageType: page?.state?.type,
          siteName: page?.key?.site?.name,
          siteType: page?.key?.site?.type,
        });
      },
      handlePageTemplateCreation (pageTemplate, pageRef) {
        this.sendAnalytics('pageTemplate', 'createPageTemplate', {
          pageTemplateId: pageTemplate.id,
          pageTemplateName: pageTemplate.name,
          pageTemplateCategory: pageTemplate.category || 'customized',
          pageTemplateDisabled: `${pageTemplate.disabled}`,
          pageTemplateSystem: `${pageTemplate.system}`,
          pageTemplateIllustrationId: pageTemplate.illustrationId,
          pageTemplatePage: pageRef,
        });
      },
      handlePageTemplateUpdate (pageTemplate) {
        this.sendAnalytics('pageTemplate', 'updatePageTemplate', {
          pageTemplateId: pageTemplate.id,
          pageTemplateName: pageTemplate.name,
          pageTemplateCategory: pageTemplate.category,
          pageTemplateDisabled: `${pageTemplate.disabled}`,
          pageTemplateSystem: `${pageTemplate.system}`,
          pageTemplateIllustrationId: pageTemplate.illustrationId,
        });
      },
      handlePageTemplateDeletion (pageTemplate) {
        this.sendAnalytics('pageTemplate', 'deletePageTemplate', {
          pageTemplateId: pageTemplate.id,
          pageTemplateName: pageTemplate.name,
          pageTemplateCategory: pageTemplate.category,
          pageTemplateDisabled: `${pageTemplate.disabled}`,
          pageTemplateSystem: `${pageTemplate.system}`,
          pageTemplateIllustrationId: pageTemplate.illustrationId,
        });
      },
      handlePageTemplateEnabled (pageTemplate) {
        this.sendAnalytics('pageTemplate', 'enablePageTemplate', {
          pageTemplateId: pageTemplate.id,
          pageTemplateName: pageTemplate.name,
          pageTemplateCategory: pageTemplate.category,
          pageTemplateDisabled: `${pageTemplate.disabled}`,
          pageTemplateSystem: `${pageTemplate.system}`,
          pageTemplateIllustrationId: pageTemplate.illustrationId,
        });
      },
      handlePageTemplateDisabled (pageTemplate) {
        this.sendAnalytics('pageTemplate', 'disablePageTemplate', {
          pageTemplateId: pageTemplate.id,
          pageTemplateName: pageTemplate.name,
          pageTemplateCategory: pageTemplate.category,
          pageTemplateDisabled: `${pageTemplate.disabled}`,
          pageTemplateSystem: `${pageTemplate.system}`,
          pageTemplateIllustrationId: pageTemplate.illustrationId,
        });
      },
      sendAnalytics (subModule, operation, parameters) {
        parameters.applicationName = this.applicationName;
        document.dispatchEvent(new CustomEvent('exo-statistic-message', {
          detail: {
            module: 'layout',
            subModule,
            userId: eXo.env.portal.userIdentityId,
            userName: eXo.env.portal.userName,
            spaceId: eXo.env.portal.spaceId || 0,
            operation,
            timestamp: Date.now(),
            parameters,
          },
        }));
      },
    },
  };
</script>