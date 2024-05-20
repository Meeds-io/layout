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
  <v-menu
    v-model="displayActionMenu"
    transition="slide-x-reverse-transition"
    :right="!$vuetify.rtl"
    class="px-0 mx-2 overflow-visible"
    offset-x
    offset-y>
    <template #activator="{ on, attrs }">
      <v-btn
        v-show="hover"
        v-bind="attrs"
        icon
        v-on="on">
        <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-list class="pa-0" dense>
      <v-list-item
        v-if="canEditPageLayout"
        class="subtitle-2" 
        @click="editLayout">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-table
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.editLayout') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-divider />
      <v-list-item
        @click="$root.$emit('open-site-navigation-add-node-drawer', navigationNode)">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-plus
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.drawer.addNode.title') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        @click="$root.$emit('open-site-navigation-edit-node-drawer', navigationNode)">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-edit
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.drawer.editNode.title') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        @click="deleteNode()">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-trash
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.delete') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-divider />
      <v-list-item
        class="subtitle-2" 
        @click="cutNode">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-cut
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.cutNode') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        class="subtitle-2" 
        @click="copyNode">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-copy
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.copyNode') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="pasteMode"
        class="subtitle-2" 
        @click="pasteNode">
        <v-icon
          size="16"
          class="pe-1">
          fas fa-paste
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.pasteNode') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-divider />
      <v-list-item
        @click="moveUpNode()"
        v-if="canMoveUp">
        <v-icon
          size="21"
          class="pe-1">
          mdi-mouse-move-up
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.moveUp') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="canMoveDown"
        @click="moveDownNode()">
        <v-icon
          size="21"
          class="pe-1">
          mdi-mouse-move-down
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.moveDown') }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-divider />
      <v-list-item
        v-if="canEditPage"
        @click="openManagePermissionsDrawer">
        <v-icon
          size="13"
          class="pe-1">
          fas fa-shield-alt
        </v-icon>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t('siteNavigation.label.manageAccess') }}</span>
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script>
export default {
  props: {
    hover: {
      type: Boolean,
      default: () => false,
    },
    navigationNode: {
      type: Object,
      default: null,
    },
    canMoveUp: {
      type: Boolean,
      default: () => false,
    },
    canMoveDown: {
      type: Boolean,
      default: () => false,
    },
    nodeToPaste: {
      type: Object,
      default: null,
    },
    pasteMode: {
      type: String,
      default: null,
    }
  },
  data: () => ({
    displayActionMenu: false,
    nodeLabels: {}
  }),
  computed: {
    pageRef() {
      if (this.navigationNode?.pageKey) {
        return this.navigationNode.pageKey.ref || `${this.navigationNode.pageKey.site.typeName}::${this.navigationNode.pageKey.site.name}::${this.navigationNode.pageKey.name}`;
      }
      return null;
    },
    pageName() {
      return this.navigationNode?.pageKey?.name;
    },
    pageSiteType() {
      return this.navigationNode?.pageKey?.site?.typeName;
    },
    pageSiteName() {
      return this.navigationNode?.pageKey?.site?.name;
    },
    nodeUri() {
      return this.navigationNode?.uri;
    },
    nodeId() {
      return this.navigationNode?.id;
    },
    nodeSiteType() {
      return this.navigationNode?.siteKey?.typeName;
    },
    nodeSiteName() {
      return this.navigationNode?.siteKey?.name;
    },
    canEditPage() {
      return this.navigationNode?.canEditPage && this.pageRef;
    },
    pageLink() {
      return this.navigationNode?.pageLink;
    },
    canEditPageLayout() {
      return this.canEditPage && !this.pageLink;
    },
  },
  watch: {
    displayActionMenu() {
      if (this.displayActionMenu) {
        document.addEventListener('mousedown', this.closeMenu);
      } else {
        document.removeEventListener('mousedown', this.closeMenu);
      }
    },
  },
  beforeDestroy() {
    document.removeEventListener('mousedown', this.closeMenu);
  },
  methods: {
    closeMenu() {
      window.setTimeout(() => {
        this.displayActionMenu = false;
      },200);
    },
    moveUpNode() {
      this.$root.$emit('moveup-node', this.navigationNode.id);
    },
    moveDownNode() {
      this.$root.$emit('movedown-node', this.navigationNode.id);
    },
    deleteNode() {
      this.$root.$emit('delete-node', this.navigationNode.id);
      const deleteDelay = 6;
      const message = this.$t('siteNavigation.label.deleteSuccess');
      const undoMessage = this.$t('siteNavigation.label.undoDelete');
      const undoMessageSuccess = this.$t('siteNavigation.deleteCanceled');
      this.$navigationLayoutService.deleteNode(this.navigationNode.id, deleteDelay)
        .then(() => {
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
            alertType: 'success',
            alertMessage: message ,
            alertLinkText: undoMessage ,
            alertLinkCallback: () => this.undoDeleteNode(this.navigationNode.id, undoMessageSuccess),
          }}));
        });
      const redirectionTime = 6100;
      setTimeout(() => {
        const deletedNode = localStorage.getItem('deletedNode');
        if (deletedNode != null) {
          this.$root.$emit('navigation-node-deleted');
        }
      }, redirectionTime);
    },
    editLayout() {
      return this.$pageLayoutService.editPageLayout(this.nodeId, this.pageRef);
    },
    openManagePermissionsDrawer(){
      this.$root.$emit('open-manage-permissions-drawer', JSON.parse(JSON.stringify(this.navigationNode)));
    },
    cutNode() {
      this.$root.$emit('cut-node', this.navigationNode);
    },
    copyNode() {
      this.$root.$emit('copy-node', this.navigationNode);
    },
    pasteNode() {
      if (this.navigationNode.children.length) {
        const index = this?.navigationNode?.children?.findIndex?.(navNode => navNode.name === this.nodeToPaste.name);
        if (index !== -1) {
          this.$root.$emit('alert-message', this.$t('siteNavigation.label.pasteNode.error'), 'error');
          return;
        } 
      }
      if (this.pasteMode === 'Cut') {
        this.$navigationLayoutService.moveNode(this.nodeToPaste.id, this.navigationNode.id, null).then(() => {
          this.$root.$emit('refresh-navigation-nodes');
        });
      } else if (this.pasteMode === 'Copy') {
        this.pasteCopiedNode(this.navigationNode.id, this.nodeToPaste);
      }
    },
    pasteCopiedNode(navigationNodeId, nodeToPaste) {
      let pageRef = null;
      if (nodeToPaste.pageKey) {
        pageRef = nodeToPaste.pageKey?.ref ? nodeToPaste.pageKey?.ref : `${ nodeToPaste.pageKey.site.typeName}::${ nodeToPaste.pageKey.site.name}::${nodeToPaste.pageKey?.name}`;
      }
      const visible = nodeToPaste.visibility !== 'HIDDEN';
      const isScheduled = nodeToPaste.visibility === 'TEMPORAL';
      const startScheduleDate = nodeToPaste.startPublicationTime !== -1 ? new Date(nodeToPaste.startPublicationTime) : null;
      const endScheduleDate = nodeToPaste.endPublicationTime !== -1 ? new Date(nodeToPaste.endPublicationTime) : null;
      const isPasteMode = true;
      this.$navigationLayoutService.getNodeLabels(nodeToPaste.id)
        .then(data => {
          this.nodeLabels = {
            labels: data.labels
          };
        })
        .then(() => {
          this.$navigationLayoutService.createNode(navigationNodeId, null, nodeToPaste.label, nodeToPaste.name, nodeToPaste.icon, visible, isScheduled, startScheduleDate, endScheduleDate, this.nodeLabels?.labels, pageRef, nodeToPaste.target, isPasteMode)
            .then(createdNode => {
              if (nodeToPaste.children.length > 0) {
                nodeToPaste.children.forEach(children => this.pasteCopiedNode(createdNode.id, children));
              }
            })
            .finally(() => {
              this.$root.$emit('refresh-navigation-nodes');
            });
        });

    },
    undoDeleteNode(nodeId, successMsg) {
      return this.$navigationLayoutService.undoDeleteNode(nodeId)
        .then(() => {
          this.$root.$emit('refresh-navigation-nodes');
          this.$root.$emit('close-alert-message');
          this.$root.$emit('alert-message', successMsg, 'success');
        });
    }
  }
};
</script>