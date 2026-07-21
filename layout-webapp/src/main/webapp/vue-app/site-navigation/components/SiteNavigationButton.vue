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
  <v-btn
    id="topBarSiteNavigation"
    :title="$t('siteNavigation.button.tooltip.label')"
    :role="'button'"
    class="ms-5"
    icon
    @click="openSiteNavigationDrawer">
    <v-icon size="20">fa-project-diagram</v-icon>
  </v-btn>
</template>
<script>
export default {
  mounted() {
    this.openSiteNavigationDrawer();
  },
  methods: {
    openSiteNavigationDrawer() {
      const includeGlobal = eXo.env.portal.metaPortalName === eXo.env.portal.siteKeyName && eXo.env.portal.siteKeyType === 'portal';
      const selectedNodeId = eXo.env.portal.selectedNodeId;
      if (!selectedNodeId) {
        this.dispatchOpenDrawer({includeGlobal});
        return;
      }
      // The page currently displayed may be reached via a navigation node
      // inherited from the global site (merged into every site's menu), even
      // while browsing another site. Resolve the node's actual owning site
      // rather than assuming it's the site currently being browsed.
      this.$navigationLayoutService.getNode(selectedNodeId)
        .then(node => this.$siteLayoutService.getSite(node.siteKey.typeName, node.siteKey.name)
          .then(site => this.dispatchOpenDrawer({
            includeGlobal,
            siteId: site.siteId,
            siteType: node.siteKey.typeName,
            siteName: node.siteKey.name,
            siteLabel: site.displayName,
          })))
        .catch(() => this.dispatchOpenDrawer({includeGlobal}));
    },
    dispatchOpenDrawer(detail) {
      document.dispatchEvent(new CustomEvent('open-site-navigation-drawer', {detail}));
    },
  }
};
</script>
