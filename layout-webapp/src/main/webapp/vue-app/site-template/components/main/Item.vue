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
          <v-icon size="28">{{ siteTemplate.icon || 'fa-globe' }}</v-icon>
        </v-card>
        <div v-sanitized-html="name" class="text-break"></div>
      </div>
    </td>
    <!-- description -->
    <td
      v-if="!$vuetify.display.lgAndDown.value"
      align="left"
      width="50%">
      <div v-sanitized-html="description" class="text-break"></div>
    </td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <v-switch
        v-model="enabled"
        :aria-label="enabled && $t('siteTemplates.label.disableInstance') || $t('siteTemplates.label.enableInstance')"
        class="ma-auto pa-0 width-fit-content"
        hide-details
        :loading="loading"
        @click="changeStatus" />
    </td>
    <td align="center">
      <site-template-menu :site-template="siteTemplate" />
    </td>
  </tr>
</template>
<script>
  export default {
    props: {
      siteTemplate: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
    }),
    computed: {
      siteTemplateId () {
        return this.siteTemplate?.id;
      },
      enabled () {
        return !this.siteTemplate?.disabled;
      },
      name () {
        return this.$te(this.siteTemplate?.name) ? this.$t(this.siteTemplate?.name) : this.siteTemplate?.name;
      },
      description () {
        return this.$te(this.siteTemplate?.description) ? this.$t(this.siteTemplate?.description) : this.siteTemplate?.description;
      },
      illustrationId () {
        return this.siteTemplate?.illustrationId;
      },
      illustrationSrc () {
        return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/siteTemplate/${this.siteTemplateId}/${this.illustrationId}`;
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
      changeStatus () {
        this.$root.$emit('close-alert-message');
        this.loading = true;
        eXo.$siteTemplateService.getSiteTemplate(this.siteTemplate.id)
          .then(siteTemplate => {
            siteTemplate.disabled = this.enabled;
            return eXo.$siteTemplateService.updateSiteTemplate(siteTemplate)
              .then(() => {
                this.$root.$emit(`site-template-${this.enabled && 'disabled' || 'enabled'}`, siteTemplate);
              });
          })
          .then(() => {
            this.$root.$emit('alert-message', this.enabled && this.$t('siteTemplates.status.disabled.success') || this.$t('siteTemplates.status.enabled.success'), 'success');
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('siteTemplates.status.update.error'), 'error'))
          .finally(() => this.loading = false);
      },
    },
  };
</script>