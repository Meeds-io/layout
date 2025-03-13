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
  <div class="ms-8">
    <div class="d-flex align-center justify-space-between flex-grow-1">
      <span>{{ $t('siteNavigation.label.choosePage') }}</span>
      <div class="d-flex align-center">
        <span>{{ $t('siteNavigation.label.chooseSite') }}</span>
        <v-checkbox
          v-model="allSites"
          class="mt-1 me-0 ms-1" />
      </div>
    </div>
    <site-navigation-site-suggester
      v-if="!allSites"
      v-model="selectedSiteNavigation"
      :all-sites="allSites"
      :class="!allSites && 'mb-8'" />
    <site-navigation-page-suggester
      :all-sites="allSites"
      :page="selectedPage"
      :site-name="selectedSiteNavigation?.name"
      :site-type="selectedSiteNavigation?.siteType" />
  </div>
</template>

<script>
  export default {
    props: {
      selectedPage: {
        type: Object,
        default: null,
      },
    },
    data () {
      return {
        allSites: true,
        selectedSiteNavigation: null,
      };
    },
    computed: {
      allSitesChipClass () {
        return this.allSites && 'primary' || '';
      },
      selectSiteChipClass () {
        return !this.allSites &&'primary' || '';
      },
    },
    watch: {
      allSites (){
        this.selectedSiteNavigation = null;
      },
    },
  };
</script>
