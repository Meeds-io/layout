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
  <tr>
    <!-- Illustration -->
    <!-- name -->
    <td
      align="left"
      class="pe-0"
      colspan="2">
      <div class="d-flex align-center text-start">
        <v-card
          class="me-4 d-flex align-center"
          color="transparent"
          flat
          min-width="35">
          <v-icon size="28">{{ site.icon || 'fa-globe' }}</v-icon>
        </v-card>
        <div v-sanitized-html="name" class="text-break"></div>
      </div>
    </td>
    <!-- description -->
    <td
      v-if="!$root.isMobile"
      align="left"
      width="50%">
      <div v-sanitized-html="description" class="text-break"></div>
    </td>
    <td v-if="!$vuetify.display.lgAndDown.value" align="center">
      <v-btn
        :aria-label="$t('sites.label.sitePermissions')"
        icon
        @click="openSiteNavigationDrawer">
        <v-icon size="20">fa-project-diagram</v-icon>
      </v-btn>
    </td>
    <td v-if="!$vuetify.display.lgAndDown.value" align="center">
      <v-btn
        :aria-label="$t('sites.label.siteNavigation')"
        icon
        @click="$root.$emit('open-manage-permissions-drawer', site, true, true)">
        <v-icon size="20">fa-shield-alt</v-icon>
      </v-btn>
    </td>
    <td align="center">
      <site-management-item-menu :site="site" />
    </td>
  </tr>
</template>
<script>
  export default {
    props: {
      site: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
    }),
    computed: {
      siteId () {
        return this.site?.id;
      },
      name () {
        return this.$te(this.site?.displayName) ? this.$t(this.site?.displayName) : this.site?.displayName;
      },
      description () {
        return this.$te(this.site?.description) ? this.$t(this.site?.description) : this.site?.description;
      },
    },
    watch: {
      hoverMenu () {
        if (!this.hoverMenu) {
          window.setTimeout(() => {
            if (!this.hoverMenu) {
              this.menu = false;
            }
          }, 200);
        }
      },
    },
    methods: {
      openSiteNavigationDrawer () {
        this.$root.$emit('open-site-navigation-drawer', {
          siteName: this.site.name,
          siteType: this.site.siteType,
          siteId: this.site.siteId,
          siteLabel: this.site.displayName,
          includeGlobal: this.site.name.toLowerCase() === eXo.env.portal.globalPortalName.toLowerCase(),
        });
      },
    },
  };
</script>