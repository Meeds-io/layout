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
    id="managePermissionsDrawer"
    ref="drawer"
    v-model="drawer"
    :allow-expand="isSite"
    eager
    :go-back-button="goBackButton"
    :loading="loading"
    :right="!$vuetify.rtl"
    @closed="close">
    <template #title>
      {{ drawerTitle }}
    </template>
    <template v-if="drawer" #content>
      <v-card class="mx-4 my-4 px-2 py-2 elevation-0">
        <site-edit-permission
          v-model="editPermission"
          :is-site="isSite" />
        <site-access-permissions
          v-model="accessPermissions"
          class="mt-4"
          :is-site="isSite" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex justify-end">
        <v-btn
          class="btn ms-2"
          @click="close">
          {{ $t('siteNavigation.label.btn.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary ms-2"
          :disabled="!enableSave"
          :loading="loading"
          @click="save">
          {{ $t('siteNavigation.label.btn.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data: () => ({
      drawer: false,
      loading: false,
      navigationNode: null,
      editPermission: null,
      accessPermissions: [],
      site: null,
      isSite: false,
      accessPermissionChanged: false,
      editPermissionChanged: false,
      goBackButton: false,
    }),
    computed: {
      drawerTitle () {
        return this.isSite && this.$t('siteManagement.label.managePermissions') || this.$t('siteNavigation.managePermissionsDrawer.title');
      },
      enableSave () {
        return this.accessPermissions?.length && this.editPermission;
      },
    },
    created () {
      this.$root.$on('open-manage-permissions-drawer', this.open);
    },
    beforeUnmount () {
      this.$root.$off('open-manage-permissions-drawer', this.open);
    },
    methods: {
      open (object, isSite, noGoBackButton) {
        this.isSite = isSite || false;
        this.goBackButton = !noGoBackButton;
        if (this.isSite) {
          this.site = JSON.parse(JSON.stringify(object));
          this.editPermission = this.site?.editPermission;
          this.accessPermissions = this.site?.accessPermissions || [];
        } else {
          this.navigationNode = JSON.parse(JSON.stringify(object));
          this.editPermission = this.navigationNode?.pageEditPermission;
          this.accessPermissions = this.navigationNode?.pageAccessPermissions || [];
        }
        this.$refs.drawer.open();
      },
      close () {
        this.$refs.drawer.close();
      },
      async save () {
        if (this.isSite) {
          await this.saveSitePermission();
        } else {
          await this.saveNavigationNodePermission();
        }
      },
      saveNavigationNodePermission () {
        this.loading = true;
        const pageRef = this.navigationNode.pageKey.ref ||`${ this.navigationNode.pageKey.site.typeName}::${ this.navigationNode.pageKey.site.name}::${this.navigationNode.pageKey.name}`;
        return eXo.$pageLayoutService.updatePagePermissions(pageRef, this.editPermission, this.accessPermissions)
          .then(() => {
            this.$root.$emit('alert-message', this.$t('siteNavigation.label.updatePermission.success'), 'success');
            this.$root.$emit('refresh-navigation-nodes');
            this.close();
          }).catch(e => {
            const message = e.message ==='401' &&  this.$t('siteNavigation.label.updatePermission.unauthorized') || this.$t('siteNavigation.label.updatePermission.error');
            this.$root.$emit('alert-message', message, 'error');
          })
          .finally(() => this.loading = false);
      },
      saveSitePermission () {
        this.loading = true;
        return eXo.$siteLayoutService.updateSitePermissions(this.site.siteType, this.site.name, this.editPermission, this.accessPermissions)
          .then(() => {
            this.$root.$emit('alert-message', this.$t('siteManagement.label.updatePermission.success'), 'success');
            this.$root.$emit('site-updated');
            this.close();
          }).catch(e => {
            const message = e.message ==='401' &&  this.$t('siteManagement.label.updatePermission.unauthorized') || this.$t('siteManagement.label.updatePermission.error');
            this.$root.$emit('alert-message', message, 'error');
          })
          .finally(() => this.loading = false);
      },
    },
  };
</script>