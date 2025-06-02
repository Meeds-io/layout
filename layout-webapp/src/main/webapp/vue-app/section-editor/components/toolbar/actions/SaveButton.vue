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
  <v-tooltip v-if="canSave" bottom>
    <template #activator="{on, attrs}">
      <div
        v-on="on"
        v-bind="attrs">
        <v-btn
          :disabled="disabled"
          :loading="loading"
          :aria-label="$t('layout.publish')"
          class="btn btn-primary d-flex align-center"
          elevation="0"
          @click="savePage">
          <span class="text-none">{{ $t('layout.save') }}</span>
        </v-btn>
      </div>
    </template>
    <span>{{ $t('layout.publishTooltip') }}</span>
  </v-tooltip>
</template>
<script>
export default {
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    loading: false,
  }),
  computed: {
    canSave() {
      return eXo.env.portal.selectedNodeId !== this.$root.nodeId;
    },
  },
  methods: {
    async savePage() {
      if (!this.canSave) {
        return;
      }
      this.loading = true;
      const layoutToUpdate = this.$layoutUtils.cleanAttributes(this.$root.layout, false, true);
      try {
        await this.$pageLayoutService.updatePageLayout(this.$root.pageRef, layoutToUpdate, 'contentId', true);
        const sectionContent = await this.$sectionTemplateService.generateSectionTemplateContent(this.$root.sectionTemplateId);
        const sectionTemplate = JSON.parse(JSON.stringify(this.$root.sectionTemplate));
        sectionTemplate.content = JSON.stringify(sectionContent);
        if (!sectionTemplate.category) {
          sectionTemplate.category = 'custom';
        }
        this.$root.$emit('section-template-edit', sectionTemplate, false, document.querySelector('.layout-section'));
      } catch (e) {
        this.$root.$emit('alert-message', this.$te(e.message) ? this.$t(e.message) : this.$t('layout.pageSavingError'), 'error');
      } finally {
        if (this.$root.sectionTemplate?.name) {
          this.loading = false;
        } else {
          window.setTimeout(() => this.loading = false, 2000);
        }
      }
    },
  },
};
</script>