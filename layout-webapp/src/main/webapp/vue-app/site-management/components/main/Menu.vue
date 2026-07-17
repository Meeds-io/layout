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
  <v-menu
    v-if="canEditSite"
    v-model="displayActionMenu"
    transition="slide-x-reverse-transition"
    :right="!$vuetify.rtl"
    offset-x
    offset-y
    class="px-0 pt-0 mx-2 overflow-visible">
    <template #activator="{ on, attrs }">
      <v-btn
        :aria-label="$t('sites.menu.open')"
        :loading="loading"
        icon
        small
        class="mx-auto"
        v-bind="attrs"
        v-on="on">
        <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-list class="pa-0" dense>
      <v-tooltip
        v-if="canEditSite"
        :disabled="canEditSiteLayout"
        bottom>
        <template #activator="{ on, attrs }">
          <div
            v-on="on"
            v-bind="attrs">
            <v-list-item
              :aria-label="$t('siteManagement.label.editLayout')"
              :disabled="!canEditSiteLayout"
              :href="editSiteLayoutLink"
              target="_blank"
              class="px-3">
              <v-card
                color="transparent"
                min-width="15"
                flat>
                <v-icon
                  :class="!canEditSiteLayout && 'disabled--text'"
                  size="13">
                  fa-window-maximize
                </v-icon>
              </v-card>
              <v-list-item-title class="ps-2">
                <span :class="!canEditSiteLayout && 'disabled--text'">{{ $t('siteManagement.label.editLayout') }}</span>
              </v-list-item-title>
            </v-list-item>
          </div>
        </template>
        <span>{{ noLayoutEditTooltip }}</span>
      </v-tooltip>
      <v-list-item
        v-for="extension in extensions"
        :key="extension.id"
        class="px-3"
        @click="handelAction(extension)">
        <v-card
          color="transparent"
          min-width="15"
          class="me-2"
          flat>
          <v-icon size="13">
            {{ extension?.icon }}
          </v-icon>
        </v-card>
        <v-list-item-title
          class="subtitle-2">
          <span class="ps-1">{{ $t(extension.labelKey) }}</span>
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="isPortalSite && !isGlobalSite"
        :aria-label="$t('siteManagement.label.properties')"
        role="button"
        class="px-3"
        @click="openSitePropertiesDrawer">
        <v-card
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fa fa-cog
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('siteManagement.label.properties') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        :aria-label="$t('siteManagement.label.navigation')"
        role="button"
        class="px-3"
        @click="openSiteNavigationDrawer">
        <v-card
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fas fa-project-diagram
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('siteManagement.label.navigation') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="!isGlobalSite"
        :aria-label="$t('siteManagement.label.manageAccess')"
        role="button"
        class="px-3"
        @click="$root.$emit('open-manage-permissions-drawer', site, true, true)">
        <v-card
          class="d-flex align-center justify-center"
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fas fa-shield-alt
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('siteManagement.label.manageAccess') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        class="px-3"
        @click="duplicate">
        <v-card
          class="d-flex align-center justify-center"
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fa-copy
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('sites.duplicate') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        class="px-3"
        @click="saveAsTemplate">
        <v-card
          class="d-flex align-center justify-center"
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fa-columns
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('sites.saveAsSiteTemplate') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="canRestore"
        class="px-3"
        @click="restoreSiteLayout">
        <v-card
          class="d-flex align-center justify-center"
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fa-undo
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('siteManagement.label.restoreSiteLayout') }}
        </v-list-item-title>
      </v-list-item>
      <v-list-item
        v-if="canRestore"
        class="px-3"
        @click="restoreSitePages">
        <v-card
          class="d-flex align-center justify-center"
          color="transparent"
          min-width="15"
          flat>
          <v-icon size="13">
            fa-undo
          </v-icon>
        </v-card>
        <v-list-item-title class="ps-2">
          {{ $t('siteManagement.label.restoreSitePages') }}
        </v-list-item-title>
      </v-list-item>
      <v-tooltip
        v-if="canEditSite"
        :disabled="canDelete"
        bottom>
        <template #activator="{ on, attrs }">
          <div
            v-on="on"
            v-bind="attrs">
            <v-list-item
              :disabled="!canDelete"
              class="px-3"
              @click="$root.$emit('delete-site', site)">
              <v-card
                class="d-flex align-center justify-center"
                color="transparent"
                min-width="15"
                flat>
                <v-icon
                  :class="canDelete && 'error--text' || 'disabled--text'"
                  size="13">
                  fa-trash
                </v-icon>
              </v-card>
              <v-list-item-title class="ps-2">
                <span :class="canDelete && 'error--text' || 'disabled--text'">{{ $t('siteManagement.label.delete') }}</span>
              </v-list-item-title>
            </v-list-item>
          </div>
        </template>
        <span>{{ $t('sites.label.system.noDelete') }}</span>
      </v-tooltip>
    </v-list>
  </v-menu>
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
    displayActionMenu: false,
    loading: false,
  }),
  computed: {
    isGlobalSite() {
      return this.site.name === 'global';
    },
    isPortalSite() {
      return this.site.siteType === 'PORTAL';
    },
    canEditSite() {
      return this.site.canEdit;
    },
    siteId() {
      return this.site.siteId;
    },
    canRestore() {
      return this.site.canRestore;
    },
    canDelete() {
      return this.canEditSite && this.site?.properties?.removable !== 'false';
    },
    editSiteLayoutLink() {
      return this.canEditSite && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/site-layout-editor?siteId=${this.siteId}`;
    },
    canEditSiteLayout() {
      return this.canEditSite
        && !this.isMetaSite
        && !this.isGlobalSite
        && !this.site.displayed;
    },
    noLayoutEditTooltip() {
      return (this.isMetaSite || this.isGlobalSite)
        ? this.$t('sites.label.system.noLayoutEdit')
        : this.$t('sites.label.meta.noLayoutEdit');
    },
    extensions() {
      return this.$root.siteActionExtensions || [];
    },
  },
  watch: {
    displayActionMenu() {
      if (this.displayActionMenu) {
        document.addEventListener('mousedown', this.closeMenu);
      } else {
        document.removeEventListener('mousedown', this.closeMenu);
      }
    },
  },
  beforeDestroy() {
    document.removeEventListener('mousedown', this.closeMenu);
  },
  methods: {
    closeMenu() {
      window.setTimeout(() => {
        this.displayActionMenu = false;
      },200);
    },
    openSiteNavigationDrawer() {
      this.$root.$emit('open-site-navigation-drawer', {
        siteName: this.site.name,
        siteType: this.site.siteType,
        siteId: this.site.siteId,
        siteLabel: this.site.displayName,
        includeGlobal: this.site.name.toLowerCase() === eXo.env.portal.globalPortalName.toLowerCase()
      });
    },
    openSitePropertiesDrawer() {
      this.$root.$emit('open-site-properties-drawer', this.site);
    },
    saveAsTemplate() {
      this.$root.$emit('site-template-add', null, this.site.siteId);
    },
    duplicate() {
      this.$root.$emit('open-site-properties-drawer', {
        ...this.site,
        siteId: null,
        name: null,
      }, this.site.siteId);
    },
    async restoreSitePages() {
      this.loading = true;
      try {
        await this.$siteLayoutService.restoreSite({
          siteName: this.site.name,
          siteType: this.site.siteType,
          importMode: 'MERGE',
          siteLayout: false,
          sitePages: true,
          siteNavigation: true,
        });
        this.$root.$emit('alert-message', this.$t('siteManagement.label.restoreSitePages.success'), 'success');
      } catch (e) {
        this.$root.$emit('alert-message', this.$t('siteManagement.label.restoreSitePages.error'), 'error');
      } finally {
        this.loading = false;
      }
    },
    async restoreSiteLayout() {
      this.loading = true;
      try {
        await this.$siteLayoutService.restoreSite({
          siteName: this.site.name,
          siteType: this.site.siteType,
          importMode: 'MERGE',
          siteLayout: true,
          sitePages: false,
          siteNavigation: false,
        });
        this.$root.$emit('alert-message', this.$t('siteManagement.label.restoreSiteLayout.success'), 'success');
      } catch (e) {
        this.$root.$emit('alert-message', this.$t('siteManagement.label.restoreSiteLayout.error'), 'error');
      } finally {
        this.loading = false;
      }
    },
    handelAction(extension) {
      return extension?.action(this, this.site);
    }
  }
};
</script>