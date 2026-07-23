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
  <div class="d-contents">
    <tr>
      <td>
        <v-hover :disabled="$root.mobileDisplayMode">
          <v-row
            slot-scope="{ hover }"
            class="d-flex pt-2 px-0 text-truncate v-list-item v-list-item--dense d-flex flex-nowrap"
            :class="extraClass">
            <v-col
              :cols="cols"
              class="my-0 py-0 px-0">
              <v-icon
                v-if="hasChildren && !hideChildren"
                size="23"
                class="align-center px-0"
                :class="!$vuetify.rtl ? 'pull-right' : 'pull-left'"
                @click="displayChildren = !displayChildren">
                {{ icon }}
              </v-icon>
              <div v-else class="ms-3 me-2"></div>
            </v-col>
            <v-col
              class="my-0 py-0 text-truncate">
              <v-list-item class="px-0 width-min-content">
                <v-list-item-content>
                  <v-tooltip bottom>
                    <template #activator="{ on, attrs }">
                      <v-list-item-title
                        v-on="on"
                        v-bind="attrs"
                        class="font-weight-bold text-truncate">
                        {{ navigationNode.label }}
                      </v-list-item-title>
                    </template>
                    <span>{{ navigationNode.label }}</span>
                  </v-tooltip>
                  <v-tooltip bottom>
                    <template #activator="{ on, attrs }">
                      <v-list-item-subtitle
                        v-on="on"
                        v-bind="attrs"
                        class="text-truncate">
                        {{ navigationNodeUri }}
                      </v-list-item-subtitle>
                    </template>
                    <span>{{ navigationNodeUri }}</span>
                  </v-tooltip>
                </v-list-item-content>
              </v-list-item>
            </v-col>
            <v-col
              cols="2"
              class="mb-1 mt-0 py-0 d-flex align-center justify-end flex-nowrap">
              <v-btn
                v-show="hover && canMoveUp"
                :title="$t('siteNavigation.label.moveUp')"
                :aria-label="$t('siteNavigation.label.moveUp')"
                icon
                @click="moveUpNode">
                <v-icon size="16" class="icon-default-color">fas fa-arrow-up</v-icon>
              </v-btn>
              <v-btn
                v-show="hover && canMoveDown"
                :title="$t('siteNavigation.label.moveDown')"
                :aria-label="$t('siteNavigation.label.moveDown')"
                icon
                @click="moveDownNode">
                <v-icon size="16" class="icon-default-color">fas fa-arrow-down</v-icon>
              </v-btn>
              <site-navigation-node-item-menu
                :navigation-node="navigationNode"
                :hover="hover"
                :can-delete="canDelete"
                :node-to-paste="nodeToPaste"
                :paste-mode="pasteMode" />
            </v-col>
          </v-row>
        </v-hover>
      </td>
      <td v-if="expanded" class="align-center text-light-color font-weight-bold">
        <span>
          {{ $t(`siteNavigation.label.${navigationNodeType}`) }}
        </span>
      </td>
      <td v-if="expanded" class="align-center">
        <span>
          {{ updatedDate }}
        </span>
      </td>
      <td v-if="expanded" class="align-center">
        <v-tooltip bottom>
          <template #activator="{ on, attrs }">
            <v-icon
              v-on="on"
              v-bind="attrs"
              color="grey"
              dark
              size="20"
              class="px-2">
              {{ visibilityIcon.icon }}
            </v-icon>
          </template>
          <span>{{ visibilityIcon.title }}</span>
        </v-tooltip>
      </td>
      <td v-if="expanded" class="align-center">
        <v-tooltip bottom>
          <template #activator="{ on, attrs }">
            <v-icon
              v-on="on"
              v-bind="attrs"
              color="grey"
              dark
              size="20"
              class="px-2">
              {{ accessIcon.icon }}
            </v-icon>
          </template>
          <span>{{ accessIcon.title }}</span>
        </v-tooltip>
      </td>
    </tr>
    <template v-if="displayChildren && !hideChildren">
      <site-navigation-node-item
        v-for="(child, index) in navigationNode.children"
        :key="child.id"
        :navigation-node="child"
        :can-move-up="index > 0"
        :can-move-down="index < (navigationNode.children.length - 1)"
        :cols="cols + 1"
        :hide-children="hideChildren"
        :expanded="expanded"
        can-delete />
    </template>
  </div>
</template>

<script>
export default {
  props: {
    navigationNode: {
      type: Object,
      default: null,
    },
    rootNodeId: {
      type: String,
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
    cols: {
      type: Number,
      default: () => 1,
    },
    hideChildren: {
      type: Boolean,
      default: true
    },
    expanded: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      displayChildren: false,
      nodeToPaste: null,
      pasteMode: null,
      lang: eXo.env.portal.language || 'en',
      fullDateFormat: {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      },
    };
  },
  computed: {
    highlightNode() {
      return this.navigationNode.uri === eXo.env.portal.selectedNodeUri;
    },
    icon() {
      return this.displayChildren && 'mdi-menu-down' || 'mdi-menu-right';
    },
    hasChildren() {
      return this.navigationNode?.children?.length;
    },
    navigationNodeUri() {
      return `/${this.navigationNode.uri}`;
    },
    navigationNodeType() {
      return !this.navigationNode.pageKey && 'group' || this.navigationNode.pageLink && 'link' ||  this.navigationNode.pageKey && 'page';
    },
    isSystemVisibility() {
      return this.navigationNode?.visibility === 'SYSTEM';
    },
    canDelete() {
      return Number(this.navigationNode.id) !== Number(this.rootNodeId) && !this.isSystemVisibility;
    },
    visibilityIcon() {
      switch (this.navigationNode?.visibility) {
      case 'TEMPORAL':
        return {
          icon: 'fas fa-calendar-alt',
          title: this.$t('siteNavigation.label.visibility.scheduled'),
        };
      case 'HIDDEN':
        return {
          icon: 'fas fa-eye-slash',
          title: this.$t('siteNavigation.label.visibility.hidden'),
        };
      default:
        return {
          icon: 'fas fa-eye',
          title: this.$t('siteNavigation.label.visibility.displayed'),
        };
      }
    },
    accessIcon() {
      if (this.navigationNode?.pageAccessPermissions?.[0] === 'Everyone') {
        return {
          icon: 'fas fa-layer-group',
          title: this.$t('siteNavigation.label.access.all'),
        };
      } else {
        return {
          icon: 'fas fa-user-lock',
          title: this.$t('siteNavigation.label.access.specific'),
        };
      }
    },
    updatedDate() {
      return this.formatDate(this.navigationNode.updatedDate);
    },
    extraClass() {
      return `${this.highlightNode ? 'light-grey-background ' : ' ' } ${this.expanded ? ' ' : ' ms-4 me-1 '} `;
    },
  },
  created() {
    this.displayCurrentNodeParentTree();
    this.$root.$on('delete-node', this.deleteChildNode);
    this.$root.$on('moveup-node', this.moveUpChildNode);
    this.$root.$on('movedown-node', this.moveDownChildNode);
    this.$root.$on('cut-node', this.cutNode);
    this.$root.$on('copy-node', this.copyNode);
    this.$root.$on('site-navigation-hide-nodes-tree', () => {
      this.displayChildren = false;
    });
    this.$root.$on('site-navigation-drawer-opened', () => {
      this.displayCurrentNodeParentTree();
    });
  },
  methods: {
    moveUpNode() {
      this.$root.$emit('moveup-node', this.navigationNode.id);
    },
    moveDownNode() {
      this.$root.$emit('movedown-node', this.navigationNode.id);
    },
    moveUpChildNode(navigationNodeId) {
      if (this.navigationNode.children.length) {
        const index = this.navigationNode?.children?.findIndex?.(navigationNode => navigationNode.id === navigationNodeId);
        if (index !== -1) {
          const previousNodeId = index > 1 ? this.navigationNode.children[index - 2].id : null;
          this.$navigationLayoutService.moveNode(navigationNodeId, null, previousNodeId).then(() => {
            this.$root.$emit('refresh-navigation-nodes');
          });
        }

      }
    },
    moveDownChildNode(navigationNodeId) {
      if (this.navigationNode.children.length) {
        const index = this.navigationNode?.children?.findIndex?.(navigationNode => navigationNode.id === navigationNodeId);
        if (index !== -1) {
          const previousNodeId = this.navigationNode.children[index + 1].id;
          this.$navigationLayoutService.moveNode(navigationNodeId, null, previousNodeId).then(() => {
            this.$root.$emit('refresh-navigation-nodes');
          });
        }
      }
    },
    deleteChildNode(navigationNodeId) {
      if (this.navigationNode.children.length) {
        const index = this.navigationNode?.children?.findIndex?.(child => child.id === navigationNodeId);
        if (index >= 0) {
          this.navigationNode.children.splice(index, 1);
        }
      }
    },
    displayCurrentNodeParentTree() {
      const currentUri = eXo.env.portal.selectedNodeUri;
      const splittedCurrentUri = currentUri.split('/');
      let nodeUri = '';
      const currentNodeParentTree = splittedCurrentUri.map(subPath => {
        nodeUri += `${subPath}/`;
        return nodeUri.slice(0, -1); // Remove trailing slash
      }).slice(0, -1); // Remove last element
      this.displayChildren = currentNodeParentTree.includes(this.navigationNode.uri);
    },
    cutNode(navigationNode) {
      this.pasteMode = 'Cut';
      this.nodeToPaste = navigationNode;
    },
    copyNode(navigationNode) {
      this.pasteMode = 'Copy';
      this.nodeToPaste = navigationNode;
    },
    formatDate(time) {
      return !time && '--' || this.$dateUtil.formatDateObjectToDisplay(new Date(time),this.fullDateFormat, this.lang);
    },
  }
};
</script>