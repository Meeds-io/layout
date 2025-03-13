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
  <div class="d-flex flex-column">
    <div v-if="isSite" class="font-weight-bold mb-2">
      {{ $t('sites.permission.whoCanView') }}
    </div>
    <div v-else class="font-weight-bold mb-2">
      {{ $t('pages.permission.whoCanView') }}
    </div>
    <v-checkbox
      v-model="isAdministrationPermissions"
      class="mt-0 ms-n1"
      disabled
      off-icon="far fa-square"
      on-icon="fa-check-square">
      <template #label>
        <div class="text-body">
          {{ $t('sites.permission.administrators') }}
        </div>
      </template>
    </v-checkbox>
    <v-checkbox
      v-model="isUserPermissions"
      class="mt-0 ms-n1"
      :disabled="isAnyPermissions"
      off-icon="far fa-square"
      on-icon="fa-check-square">
      <template #label>
        <div class="text-body">
          {{ $t('sites.permission.users') }}
        </div>
      </template>
    </v-checkbox>
    <v-checkbox
      v-model="isGuestPermissions"
      class="mt-0 ms-n1"
      :disabled="isAnyPermissions"
      off-icon="far fa-square"
      on-icon="fa-check-square">
      <template #label>
        <div class="text-body">
          {{ $t('sites.permission.guests') }}
        </div>
      </template>
    </v-checkbox>
    <v-checkbox
      v-model="isAnyPermissions"
      class="mt-0 ms-n1"
      off-icon="far fa-square"
      on-icon="fa-check-square">
      <template #label>
        <div class="text-body">
          {{ $t('sites.permission.everyone') }}
        </div>
      </template>
    </v-checkbox>
    <v-checkbox
      v-model="isCustomPermissions"
      class="mt-0 ms-n1"
      :disabled="isAnyPermissions"
      off-icon="far fa-square"
      on-icon="fa-check-square"
      @click="specificGroupEntries = null">
      <template #label>
        <div class="text-body">
          {{ $t('sites.permission.groupMembers') }}
        </div>
      </template>
    </v-checkbox>
    <exo-identity-suggester
      v-if="isCustomPermissions"
      ref="targetPermissions"
      v-model="specificGroupEntries"
      all-groups-for-admin
      class="mb-n3"
      include-groups
      include-spaces
      :labels="suggesterLabels"
      multiple
      name="specificGroupPermissions"
      required
      :search-options="{filterType: 'all'}" />
  </div>
</template>
<script>
  export default {
    props: {
      value: {
        type: String,
        default: null,
      },
      isSite: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      administratorsPermission: '/platform/administrators',
      usersPermission: '/platform/users',
      externalsPermission: '/platform/externals',
      everyonePermission: 'Everyone',
      isAdministrationPermissions: true,
      isUserPermissions: false,
      isGuestPermissions: false,
      isAnyPermissions: false,
      isCustomPermissions: false,
      specificGroupEntries: null,
    }),
    computed: {
      isSpecificGroup () {
        return !!this.specificGroupEntries?.length;
      },
      permissions () {
        const permissions = [];
        if (this.isAnyPermissions) {
          permissions.push(this.everyonePermission);
        } else {
          if (this.isUserPermissions) {
            permissions.push(`*:${this.usersPermission}`);
          }
          if (this.isGuestPermissions) {
            permissions.push(`*:${this.externalsPermission}`);
          }
          if (this.specificGroupEntries?.length) {
            const specificGroupEntries = this.specificGroupEntries?.map?.(g => g.groupId)?.filter?.(g => g) || [];
            permissions.push(...specificGroupEntries.map(g => `*:${g}`));
          }
          if (!permissions.length) {
            permissions.push(`*:${this.administratorsPermission}`);
          }
        }
        return permissions;
      },
      suggesterLabels () {
        return {
          placeholder: this.$t('sites.permissionSuggester.placeholder'),
          noDataLabel: this.$t('sites.permissionSuggester.noData'),
        };
      },
    },
    watch: {
      permissions () {
        this.$emit('input', this.permissions);
      },
      isAnyPermissions () {
        if (this.isAnyPermissions) {
          this.isAdministrationPermissions = true;
          this.isUserPermissions = true;
          this.isGuestPermissions = true;
          this.isCustomPermissions = false;
        }
      },
    },
    created () {
      const permissions = this.value?.slice?.();
      this.isAnyPermissions = permissions?.find?.(p => p === this.everyonePermission) && true || false;
      this.isUserPermissions = permissions?.find?.(p => (p.includes(':') ? p.split(':')[1] : p) === this.usersPermission) && true || false;
      this.isGuestPermissions = permissions?.find?.(p => (p.includes(':') ? p.split(':')[1] : p) === this.externalsPermission) && true || false;
      this.specificGroupEntries = [];

      const specificGroupEntries = permissions?.filter?.(p => p)?.filter?.(p => {
        const g = p.includes(':') ? p.split(':')[1] : p;
        return g !== this.administratorsPermission
          && g !== this.usersPermission
          && g !== this.externalsPermission
          && g !== this.everyonePermission;
      }) || null;
      this.isCustomPermissions = !!specificGroupEntries?.length;
      if (specificGroupEntries?.length) {
        specificGroupEntries.forEach(this.retrieveObject);
      }
    },
    methods: {
      async retrieveObject (groupId) {
        try {
          groupId = groupId.includes(':') ? groupId.split(':')[1] : groupId;
          if (groupId.indexOf('/spaces/') === 0) {
            const space = await eXo.$spaceService.getSpaceByGroupId(groupId);
            if (space) {
              this.specificGroupEntries.push({
                id: `space:${space.prettyName}`,
                remoteId: space.prettyName,
                spaceId: space.id,
                groupId: space.groupId,
                providerId: 'space',
                displayName: space.displayName,
                profile: {
                  fullName: space.displayName,
                  originalName: space.shortName,
                  avatarUrl: space.avatarUrl ? space.avatarUrl : `/portal/rest/v1/social/spaces/${space.prettyName}/avatar`,
                },
              });
            }
          } else {
            const group = await eXo.$identityService.getIdentityByProviderIdAndRemoteId('group', groupId);
            if (group) {
              this.specificGroupEntries.push({
                id: `group:${group.remoteId}`,
                remoteId: group.remoteId,
                spaceId: groupId,
                groupId,
                providerId: 'group',
                displayName: group.profile?.fullname,
                profile: {
                  fullName: group.profile?.fullname,
                  originalName: group.profile?.fullname,
                },
              });
            }
          }
        } catch (e) {
          console.error('Error retrieving group details with id', groupId, e);
        }
      },
    },
  };
</script>