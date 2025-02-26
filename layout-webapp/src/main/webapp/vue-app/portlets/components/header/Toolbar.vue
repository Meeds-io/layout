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
  <application-toolbar
    id="portletsApplication"
    :center-button-toggle="{
      selected: tabName,
      buttons: [{
        value: 'instances',
        text: $t('portlets.instances'),
        icon: 'fa-braille',
      }, {
        value: 'portlets',
        text: $t('portlets'),
        icon: 'fa-grip-horizontal',
      }]
    }"
    :right-text-filter="{
      minCharacters: 1,
      placeholder: $t('portlets.filter.placeholder'),
      tooltip: $t('portlets.filter.placeholder'),
    }"
    class="border-box-sizing px-1"
    @toggle-select="$emit('select-tab', $event)"
    @filter-text-input="$emit('portlet-instance-filter', $event)">
    <template v-if="!$root.isMobile && tabName === 'instances'" #left>
      <div v-if="$root.selectedPortletInstances?.length && !$root.isMobile">
        <v-btn
          color="primary"
          elevation="0"
          outlined
          @click="$root.$emit('serialize-drawer-open', 'PortletInstance', selectedPortletInstancesIds)">
          <v-icon size="16" class="me-2">fa-download</v-icon>
          {{ $t('portletInstance.label.export') }}
        </v-btn>
      </div>
      <v-btn
        v-else
        id="applicationToolbarLeftButton"
        :aria-label="$t('layout.portletInstance.add')"
        :class="$root.isMobile && 'px-0'"
        class="btn btn-primary text-truncate"
        @click="$root.$emit('portlet-instance-add')">
        <v-icon
          size="18">
          fa-plus
        </v-icon>
        <span
          v-if="!$root.isMobile"
          class="text-truncate text-none ms-2">
          {{ $t('layout.portletInstance.add') }}
        </span>
      </v-btn>
    </template>
  </application-toolbar>
</template>
<script>
export default {
  props: {
    tabName: {
      type: String,
      default: null,
    }
  },
  computed: {
    selectedPortletInstancesIds() {
      return this.$root.selectedPortletInstances.map(item => item.id);
    }
  }
};
</script>
