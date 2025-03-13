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
  <v-app
    class="transparent"
    flat>
    <main class="content-box-sizing">
      <section-editor-toolbar
        :node="node"
        :page="pageContext" />
      <v-divider />
      <section-editor-content
        :layout="draftLayout"
        :node="draftNode"
        :page="pageContext" />
      <section-template-drawer />
      <layout-editor-cells-selection-box />
    </main>
    <layout-analytics application-name="sectionEditor" />
  </v-app>
</template>
<script>
  export default {
    data: () => ({
      node: null,
      pageContext: null,
      draftNode: null,
      draftNodeId: null,
      draftLayout: null,
      nodeLabels: null,
    }),
    computed: {
      pageKey () {
        return this.node?.state?.pageRef;
      },
      pageRef () {
        return eXo.$layoutUtils.getQueryParam('pageId') || this.pageKey?.ref || (this.pageKey && `${this.pageKey.site.typeName}::${this.pageKey.site.name}::${this.pageKey.name}`);
      },
      nodeId () {
        return this.$root.nodeId;
      },
      draftPageKey () {
        return this.draftNode?.state?.pageRef;
      },
      draftPageRef () {
        return this.draftPageKey?.ref || (this.draftPageKey && `${this.draftPageKey.site.typeName}::${this.draftPageKey.site.name}::${this.draftPageKey.name}`);
      },
    },
    watch: {
      pageRef: {
        immediate: true,
        handler () {
          if (this.pageRef) {
            this.$root.pageRef = this.pageRef;
            eXo.$pageLayoutService.getPage(this.pageRef)
              .then(page => {
                this.$root.page = page;
                this.pageContext = page;
              });
          }
        },
      },
      draftPageRef: {
        immediate: true,
        handler () {
          if (this.draftPageRef) {
            this.$root.draftPageRef = this.draftPageRef;
            eXo.$pageLayoutService.getPageLayout({
              pageRef: this.draftPageRef,
              expand: 'contentId',
            }).then(draftLayout => this.setDraftLayout(draftLayout));
          }
        },
      },
      nodeId: {
        immediate: true,
        handler () {
          if (this.nodeId) {
            this.$root.nodeId = this.nodeId;
            if (this.nodeId && !this.nodeLabels) {
              eXo.$navigationLayoutService.getNodeLabels(this.nodeId)
                .then(nodeLabels => this.nodeLabels = nodeLabels);
            }
            if (this.nodeId && !this.$root.nodeUri) {
              eXo.$navigationLayoutService.getNodeUri(this.nodeId)
                .then(uri => {
                  uri = uri.replace(/^\/global\//g, `/${eXo.env.portal.portalName}/`);
                  return eXo.$layoutUtils.initPageContext(uri)
                    .finally(() => this.$root.nodeUri = uri);
                });
            }
          }
        },
      },
      draftNodeId: {
        immediate: true,
        handler () {
          if (this.draftNodeId) {
            this.$root.draftNode = this.draftNode;
            this.$root.draftNodeId = this.draftNodeId;
            if (this.draftNodeId && !this.$root.draftNodeUri) {
              eXo.$navigationLayoutService.getNodeUri(this.draftNodeId)
                .then(uri => this.$root.draftNodeUri = uri.replace(/^\/global\//g, `/${eXo.env.portal.portalName}/`));
            }
          }
        },
      },
      layout (newVal, oldVal) {
        if (!oldVal && newVal) {
          this.$root.$applicationLoaded();
        }
      },
    },
    created () {
      this.$root.$on('section-template-saved', this.deleteDraft);
      this.initDraftPage();
    },
    beforeUnmount () {
      this.$root.$off('section-template-saved', this.deleteDraft);
    },
    methods: {
      initDraftPage () {
        eXo.$navigationLayoutService.getNode(this.nodeId)
          .then(node => this.node = node)
          .then(() => eXo.$navigationLayoutService.createDraftNode(this.node.id))
          .then(draftNode => {
            this.draftNode = draftNode;
            this.draftNodeId = draftNode?.id;
          })
          .finally(() => this.stopLoading());
      },
      cancelEditPage () {
        this.stopLoading();
        window.close();
      },
      stopLoading () {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      },
      setDraftLayout (draftLayout) {
        this.draftLayout = draftLayout;
      },
      deleteDraft () {
        this.$root.$emit('coediting-remove-revision');
      },
    },
  };
</script>
