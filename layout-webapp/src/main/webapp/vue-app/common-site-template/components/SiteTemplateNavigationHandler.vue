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
  <div>
    <site-navigation-drawer />
    <manage-permissions-drawer />
    <site-navigation-node-drawer />
    <site-navigation-element-drawer />
  </div>
</template>
<script>
  export default {
    created () {
      document.addEventListener('open-site-navigation-drawer', this.openSiteNavigationDrawerByEvent);
      this.$root.$on('site-template-created', this.openCreatedSiteNavigationDrawer);
      this.$root.$on('site-template-navigation-open', this.openSiteNavigationDrawer);
    },
    beforeUnmount () {
      document.removeEventListener('open-site-navigation-drawer', this.openSiteNavigationDrawerByEvent);
      this.$root.$off('site-template-created', this.openCreatedSiteNavigationDrawer);
      this.$root.$off('site-template-navigation-open', this.openSiteNavigationDrawer);
    },
    methods: {
      openSiteNavigationDrawerByEvent (event) {
        this.openSiteNavigationDrawer(event?.detail);
      },
      openCreatedSiteNavigationDrawer (siteTemplate) {
        this.openSiteNavigationDrawer(siteTemplate, 'siteTemplate.label.editCreatedNavigation.information', true);
      },
      async openSiteNavigationDrawer (siteTemplate, information, displayEditLayout) {
        try {
          const site = await eXo.$siteService.getSite('portal_template', siteTemplate?.layout, {
            expandNavigations: false,
          });
          this.$root.$emit('open-site-navigation-drawer', {
            siteName: site.name,
            siteType: site.siteType,
            siteId: site.siteId,
            siteLabel: siteTemplate?.name,
            information: information || 'siteTemplate.label.editNavigation.information',
            displayEditLayout,
            displayCloseFooter: true,
            includeGlobal: false,
          });
        } finally {
          this.$emit('loading', false);
        }
      },
    },
  };
</script>