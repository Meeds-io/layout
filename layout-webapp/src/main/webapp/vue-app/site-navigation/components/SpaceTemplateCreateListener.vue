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
<script>
  export default {
    created () {
      this.$root.$on('space-templates-created', this.openSiteNavigationDrawer);
    },
    beforeUnmount () {
      this.$root.$off('space-templates-created', this.openSiteNavigationDrawer);
    },
    methods: {
      async openSiteNavigationDrawer (spaceTemplate) {
        try {
          const site = await eXo.$siteService.getSite('group_template', spaceTemplate?.layout, {
            expandNavigations: false,
          });
          document.dispatchEvent(new CustomEvent('open-site-navigation-drawer',{ detail: {
            siteName: site.name,
            siteType: site.siteType,
            siteId: site.siteId,
            siteLabel: spaceTemplate?.name,
            information: 'spaceTemplate.label.editNavigation.information',
            displayCloseFooter: true,
            includeGlobal: false,
          } }));
        } finally {
          this.$emit('loading', false);
        }
      },
    },
  };
</script>