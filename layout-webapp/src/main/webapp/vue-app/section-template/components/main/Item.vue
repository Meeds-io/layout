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
    <td
      v-if="!$root.isMobile"
      align="center"
      class="px-0">
      <layout-image-illustration
        object-type="sectionTemplate"
        :value="sectionTemplate" />
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
      v-if="!$vuetify.display.lgAndDown.value"
      v-sanitized-html="description"
      align="left"
      class="text-break"></td>
    <td
      v-if="!$vuetify.display.lgAndDown.value"
      align="left">
      {{ $t(`sectionTemplates.label.category.${sectionTemplate.category}`) }}
    </td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <v-switch
        v-model="enabled"
        :aria-label="enabled && $t('sectionTemplates.label.disableInstance') || $t('sectionTemplates.label.enableInstance')"
        class="ma-auto pa-0 width-fit-content"
        hide-details
        :loading="loading"
        @click="changeStatus" />
    </td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <section-template-menu :section-template="sectionTemplate" />
    </td>
  </tr>
</template>
<script>
  export default {
    props: {
      sectionTemplate: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
    }),
    computed: {
      sectionTemplateId () {
        return this.sectionTemplate?.id;
      },
      enabled () {
        return !this.sectionTemplate?.disabled;
      },
      name () {
        return this.$te(this.sectionTemplate?.name) ? this.$t(this.sectionTemplate?.name) : this.sectionTemplate?.name;
      },
      description () {
        return this.$te(this.sectionTemplate?.description) ? this.$t(this.sectionTemplate?.description) : this.sectionTemplate?.description;
      },
      illustrationId () {
        return this.sectionTemplate?.illustrationId;
      },
      illustrationSrc () {
        return this.illustrationId && `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/sectionTemplate/${this.sectionTemplateId}/${this.illustrationId}`;
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
        eXo.$sectionTemplateService.getSectionTemplate(this.sectionTemplate.id)
          .then(sectionTemplate => {
            sectionTemplate.disabled = this.enabled;
            return eXo.$sectionTemplateService.updateSectionTemplate(sectionTemplate)
              .then(() => {
                this.$root.$emit(`section-template-${this.enabled && 'disabled' || 'enabled'}`, sectionTemplate);
              });
          })
          .then(() => {
            this.$root.$emit('alert-message', this.enabled && this.$t('sectionTemplates.status.disabled.success') || this.$t('sectionTemplates.status.enabled.success'), 'success');
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('sectionTemplates.status.update.error'), 'error'))
          .finally(() => this.loading = false);
      },
    },
  };
</script>