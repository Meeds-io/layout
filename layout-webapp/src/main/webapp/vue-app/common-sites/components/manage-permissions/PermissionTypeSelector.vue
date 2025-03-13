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
  <div class="text-center pe-1">
    <span class="text-capitalize clickable" @click="showMenu = true"> {{ getMembershipTypeLabel(membershipType) }}</span>
    <v-menu
      v-model="showMenu"
      bottom
      :close-on-content-click="false"
      left
      offset-y
      transition="slide-x-reverse-transition">
      <template #activator="{ on, attrs }">
        <v-btn
          icon
          small
          v-bind="attrs"
          v-on="on">
          <v-icon
            :size="16">
            mdi-chevron-down
          </v-icon>
        </v-btn>
      </template>
      <v-list
        class="pa-0">
        <v-list-item
          v-for="(membershipTypeItem, index) in membershipTypes"
          :key="index"
          class="custom-list-item-min-height">
          <v-list-item-title class="clickable text-capitalize" @click="membershipType = membershipTypeItem.name">
            <span class="text-font-size clickable">
              {{ getMembershipTypeLabel(membershipTypeItem.name) }}
            </span>
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script>
  export default {
    props: {
      membershipType: {
        type: String,
        default: '*',
      },
    },
    data: () => ({
      membershipTypes: [],
      showMenu: false,
    }),
    watch: {
      membershipType () {
        this.$emit('membership-type-changed', this.membershipType);
      },
      showMenu () {
        if (this.showMenu) {
          document.addEventListener('mousedown', this.closeMenu);
        } else {
          document.removeEventListener('mousedown', this.closeMenu);
        }
      },
    },
    created () {
      this.getMembershipTypes();
    },
    beforeUnmount () {
      document.removeEventListener('mousedown', this.closeMenu);
    },
    methods: {
      closeMenu () {
        window.setTimeout(() => {
          this.showMenu = false;
        },200);
      },
      getMembershipTypes () {
        return eXo.$siteLayoutService.getMembershipTypes()
          .then(membershipTypes => this.membershipTypes = membershipTypes || []);
      },
      getMembershipTypeLabel (membershipType){
        if (!this.$t(`siteNavigation.label.membershipType.${membershipType}`).includes('siteNavigation.label')) {
          return  this.$t(`siteNavigation.label.membershipType.${membershipType}`);
        } else {
          return membershipType;
        }
      },
    },
  };
</script>