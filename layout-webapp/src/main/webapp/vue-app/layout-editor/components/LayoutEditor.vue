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
  <v-app
    class="transparent"
    flat>
    <main class="content-box-sizing">
      <layout-editor-toolbar
        :page="pageContext"
        :node="node"
        :node-labels="nodeLabels" />
      <v-divider />
      <coediting
        v-model="draftNodeId"
        :object-id="nodeId"
        :messages="{
          editingInOtherWindow: 'layout.pageBeingEditedByYouInOther',
          lockConfirmTitle: 'layout.lockConfirmTitle',
          lockConfirmMessage: 'layout.lockConfirmMessage',
          lockConfirmQuestion: 'layout.lockConfirmQuestion',
          lockConfirmOkLabel: 'layout.lockConfirmOkLabel',
          lockConfirmCancelLabel: 'layout.lockConfirmCancelLabel',
          draftConfirmTitle: 'layout.draftConfirmTitle',
          draftConfirmMessage: 'layout.draftConfirmMessage',
          draftConfirmQuestion: 'layout.draftConfirmQuestion',
          draftConfirmOkLabel: 'layout.draftConfirmOkLabel',
          draftConfirmCancelLabel: 'layout.draftConfirmCancelLabel',
        }"
        object-type="navigation"
        @initialized="initDraftPage"
        @locked="stopLoading"
        @draft-detected="stopLoading"
        @canceled="cancelEditPage">
        <layout-editor-content
          :page="pageContext"
          :node="draftNode"
          :layout="draftLayout" />
      </coediting>
      <layout-editor-cells-selection-box />
    </main>
    <layout-analytics
      :application-name="pageTemplateId ? 'pageTemplateManagement' : 'pageLayoutEditor'" />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    node: null,
    pageContext: null,
    draftNodeId: null,
    draftNode: null,
    draftLayout: null,
    nodeLabels: null,
  }),
  computed: {
    pageKey() {
      return this.node?.state?.pageRef;
    },
    pageTemplateId() {
      return this.$layoutUtils.getQueryParam('pageTemplateId');
    },
    pageRef() {
      return this.$layoutUtils.getQueryParam('pageId') || this.pageKey?.ref || (this.pageKey && `${this.pageKey.site.typeName}::${this.pageKey.site.name}::${this.pageKey.name}`);
    },
    nodeId() {
      return this.$layoutUtils.getQueryParam('nodeId') || (this.pageTemplateId && eXo.env.portal.selectedNodeId);
    },
    draftPageKey() {
      return this.draftNode?.state?.pageRef;
    },
    draftPageRef() {
      return this.draftPageKey?.ref || (this.draftPageKey && `${this.draftPageKey.site.typeName}::${this.draftPageKey.site.name}::${this.draftPageKey.name}`);
    },
  },
  watch: {
    pageTemplateId: {
      immediate: true,
      handler() {
        if (this.pageTemplateId) {
          this.$root.pageTemplateId = this.pageTemplateId;
        }
      },
    },
    pageRef: {
      immediate: true,
      handler() {
        if (this.pageRef) {
          this.$root.pageRef = this.pageRef;
          this.$pageLayoutService.getPage(this.pageRef)
            .then(page => {
              this.$root.page = page;
              this.pageContext = page;
            })
            .catch(e => this.handleLoadError(e));
        }
      },
    },
    draftPageRef: {
      immediate: true,
      handler() {
        if (this.draftPageRef) {
          this.$root.draftPageRef = this.draftPageRef;
          if (this.pageTemplateId) {
            this.$pageTemplateService.getPageTemplate(this.pageTemplateId, true)
              .then(pageTemplate => this.$root.pageTemplate = pageTemplate)
              .then(() => this.$pageLayoutService.updatePageLayout(
                this.draftPageRef,
                JSON.parse(this.$root.pageTemplate.content),
                'contentId'))
              .then(draftLayout => this.setDraftLayout(draftLayout))
              .catch(e => this.handleLoadError(e));
          } else {
            this.$pageLayoutService.getPageLayout({
              pageRef: this.draftPageRef,
              expand: 'contentId',
            }).then(draftLayout => this.setDraftLayout(draftLayout))
              .catch(e => this.handleLoadError(e));
          }
        }
      },
    },
    nodeId: {
      immediate: true,
      handler() {
        if (this.nodeId) {
          this.$root.nodeId = this.nodeId;
          if (this.nodeId && !this.nodeLabels) {
            this.$navigationLayoutService.getNodeLabels(this.nodeId)
              .then(nodeLabels => this.nodeLabels = nodeLabels)
              .catch(e => this.handleLoadError(e));
          }
          if (this.nodeId && !this.$root.nodeUri) {
            this.$navigationLayoutService.getNodeUri(this.nodeId)
              .then(uri => {
                uri = uri.replace(/^\/global\//g, `/${eXo.env.portal.portalName}/`);
                return this.$layoutUtils.initPageContext(uri)
                  .finally(() => this.$root.nodeUri = uri);
              })
              .catch(e => this.handleLoadError(e));
          }
        }
      },
    },
    draftNodeId: {
      immediate: true,
      handler() {
        if (this.draftNodeId) {
          this.$root.draftNode = this.draftNode;
          this.$root.draftNodeId = this.draftNodeId;
          if (this.draftNodeId && !this.$root.draftNodeUri) {
            this.$navigationLayoutService.getNodeUri(this.draftNodeId)
              .then(uri => this.$root.draftNodeUri = uri.replace(/^\/global\//g, `/${eXo.env.portal.portalName}/`))
              .catch(e => this.handleLoadError(e));
          }
        }
      },
    },
    layout(newVal, oldVal) {
      if (!oldVal && newVal) {
        this.$root.$applicationLoaded();
      }
    },
  },
  created() {
    this.$root.$on('layout-draft-refresh', this.setDraftLayout);
    if (this.pageTemplateId) {
      this.$root.$on('page-templates-saved', this.deleteDraft);
    } else {
      this.$root.$on('layout-page-saved', this.deleteDraft);
    }
  },
  beforeDestroy() {
    this.$root.$off('layout-draft-refresh', this.setDraftLayout);
    if (this.pageTemplateId) {
      this.$root.$off('page-templates-saved', this.deleteDraft);
    } else {
      this.$root.$off('layout-page-saved', this.deleteDraft);
    }
  },
  methods: {
    initDraftPage() {
      if (this.draftNodeId) {
        this.$navigationLayoutService.getNode(this.nodeId)
          .then(node => this.node = node)
          .then(() => this.$navigationLayoutService.getNode(this.draftNodeId))
          .then(draftNode => this.draftNode = draftNode)
          .catch(e => this.handleLoadError(e));
      } else {
        this.$navigationLayoutService.getNode(this.nodeId)
          .then(node => this.node = node)
          .then(() => this.$navigationLayoutService.createDraftNode(this.node.id))
          .then(draftNode => {
            this.draftNode = draftNode;
            this.draftNodeId = draftNode?.id;
          })
          .catch(e => this.handleLoadError(e));
      }
    },
    cancelEditPage() {
      this.stopLoading();
      window.close();
    },
    stopLoading() {
      document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
    },
    handleLoadError(e) {
      this.$root.$emit('alert-message', this.$te(e?.message) ? this.$t(e.message) : this.$t('layout.pageSavingError'), 'error');
      this.stopLoading();
    },
    setDraftLayout(draftLayout) {
      this.draftLayout = draftLayout;
    },
    deleteDraft() {
      this.$root.$emit('coediting-remove-revision');
    },
  },
};
</script>
