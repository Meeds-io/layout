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
  <v-tooltip bottom>
    <template #activator="{on, bind}">
      <v-btn
        id="topBarSiteNavigation"
        v-on="on"
        v-bind="bind"
        :role="'button'"
        class="ms-5"
        icon
        @click="openSiteNavigationDrawer">
        <v-icon size="20">fa-sitemap</v-icon>
      </v-btn>
    </template>
    <span>{{ $t('siteNavigation.button.tooltip.label') }}</span>
  </v-tooltip>
</template>
<script>
export default {
  methods: {
    openSiteNavigationDrawer() {
      document.dispatchEvent(new CustomEvent('open-site-navigation-drawer',{detail: {
        siteName: this.siteName,
        siteType: this.siteType,
        siteId: this.siteId,
        includeGlobal: eXo.env.portal.metaPortalName === eXo.env.portal.siteKeyName && eXo.env.portal.siteKeyType === 'portal',
      }}));
    }
  }
};
</script>
