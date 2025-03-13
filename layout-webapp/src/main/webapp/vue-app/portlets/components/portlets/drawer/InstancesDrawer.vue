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
  <exo-drawer
    id="portletInstancesDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    right>
    <template #title>
      <span class="text-wrap">{{ $t('portlets.portletInstancesList', {0: name}) }}</span>
    </template>
    <template v-if="drawer" #content>
      <div v-if="applications.length" class="my-4">
        <v-btn
          :aria-label="$t('layout.portletInstance.add')"
          class="btn btn-primary text-truncate mb-6 mx-4"
          :class="$root.isMobile && 'px-0'"
          @click="$root.$emit('portlet-instance-add', null, true, contentId)">
          {{ $t('layout.portletInstance.add') }}
        </v-btn>
        <v-list-item
          v-for="application in applications"
          :key="application.id"
          dense>
          <v-list-item-content>
            <v-list-item-title>
              <v-card
                flat
                v-on="application.illustrationId && {
                  click: () => openIllustration(application)
                }">
                {{ application.name }}
              </v-card>
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action class="my-auto me-4">
            <v-tooltip bottom>
              <template #activator="{on, attrs}">
                <div
                  v-bind="attrs"
                  v-on="on">
                  <v-btn
                    class="transparent d-flex align-center justify-center"
                    :disabled="!application.illustrationId"
                    icon
                    @click="openIllustration(application)">
                    <v-icon class="icon-default-color">fa-eye</v-icon>
                  </v-btn>
                </div>
              </template>
              <span>{{ application.illustrationId && $t('portlets.previewInstance') || $t('portlets.noPreviewAvailable') }}</span>
            </v-tooltip>
          </v-list-item-action>
        </v-list-item>
      </div>
      <div v-else class="d-flex flex-column align-center justify-center pa-4 subtitle-1">
        <v-icon class="icon-default-color" size="40">fa-braille</v-icon>
        <span class="text-sub-title my-4">{{ $t('portlets.noPortletInstancesYet') }}</span>
        <v-btn
          :aria-label="$t('layout.portletInstance.add')"
          class="btn btn-primary text-truncate"
          :class="$root.isMobile && 'px-0'"
          @click="$root.$emit('portlet-instance-add', null, true, contentId)">
          {{ $t('layout.portletInstance.add') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      contentId: null,
      name: null,
    }),
    computed: {
      applications () {
        const applications = this.contentId && this.$root.portletInstances.filter(a => a.contentId === this.contentId) || [];
        applications.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
        return applications;
      },
    },
    created () {
      this.$root.$on('portlet-instance-drawer', this.open);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-drawer', this.open);
    },
    methods: {
      open (contentId, name) {
        this.$root.$emit('close-alert-message');
        this.contentId = contentId;
        this.name = name;
        this.$nextTick().then(() => this.$refs.drawer.open());
      },
      openIllustration (application) {
        const illustrationId = application.illustrationId;
        const illustrationSrc = `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/attachments/portletInstance/${application.id}/${illustrationId}`;
        this.$root.$emit('layout-illustration-preview', illustrationSrc);
      },
      close () {
        this.$refs.drawer.close();
      },
    },
  };
</script>