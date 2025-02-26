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
  <tr>
    <td align="center">
      <v-checkbox
        :value="selected || $root.allPortletInstancesSelected"
        on-icon="fas fa-check-square fa-lg primary--text"
        off-icon="far fa-square fa-lg"
        class="my-auto pt-2"
        @change="changeCheckboxStatus" />
    </td>
    <!-- Illustration -->
    <td
      v-if="!$root.isMobile"
      align="center">
      <layout-image-illustration
        :value="portletInstance"
        object-type="portletInstance"
        default-src="/layout/images/portlets/DefaultPortlet.png" />
    </td>
    <!-- name -->
    <td align="left">
      <v-card
        v-sanitized-html="name"
        class="transparent text-break"
        flat
        @click="$root.$emit('layout-illustration-preview', illustrationSrc)" />
    </td>
    <!-- description -->
    <td
      v-if="!$vuetify.breakpoint.lgAndDown"
      align="left"
      class="text-break"
      v-sanitized-html="description"></td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <v-switch
        v-model="enabled"
        :loading="loading"
        :aria-label="enabled && $t('portlets.label.disableInstance') || $t('portlets.label.enableInstance')"
        class="mt-0 mx-auto width-fit-content"
        @click="changeStatus" />
    </td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <portlets-instance-menu :portlet-instance="portletInstance" />
    </td>
  </tr>
</template>
<script>
export default {
  props: {
    portletInstance: {
      type: Object,
      default: null,
    },
    selected: {
      type: Boolean,
      default: false,
    },
    select: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    menu: false,
    hoverMenu: false,
  }),
  computed: {
    portletInstanceId() {
      return this.portletInstance?.id;
    },
    enabled() {
      return !this.portletInstance?.disabled;
    },
    name() {
      return this.$te(this.portletInstance?.name) ? this.$t(this.portletInstance?.name) : this.portletInstance?.name;
    },
    description() {
      return this.$te(this.portletInstance?.description) ? this.$t(this.portletInstance?.description) : this.portletInstance?.description;
    },
    illustrationId() {
      return this.portletInstance?.illustrationId;
    },
    illustrationSrc() {
      return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/portletInstance/${this.portletInstanceId}/${this.illustrationId}` || '/layout/images/portlets/DefaultPortlet.png';
    },
  },
  watch: {
    hoverMenu() {
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
    changeStatus() {
      this.$root.$emit('close-alert-message');
      this.loading = true;
      this.$portletInstanceService.getPortletInstance(this.portletInstance.id)
        .then(portletInstance => {
          portletInstance.disabled = this.enabled;
          return this.$portletInstanceService.updatePortletInstance(portletInstance)
            .then(() => {
              this.$root.$emit(`portlet-instance-${this.enabled && 'disabled' || 'enabled'}`, portletInstance);
            });
        })
        .then(() => {
          this.$root.$emit('alert-message', this.enabled && this.$t('portlets.status.disabled.success') || this.$t('portlets.status.enabled.success'), 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('portlets.status.update.error'), 'error'))
        .finally(() => this.loading = false);
    },
    changeCheckboxStatus(status) {
      this.select(status);
    }
  },
};
</script>