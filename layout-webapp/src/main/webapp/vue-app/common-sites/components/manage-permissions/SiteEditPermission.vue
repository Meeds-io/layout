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
      {{ $t('sites.permission.whoCanEdit') }}
    </div>
    <div v-else class="font-weight-bold mb-2">
      {{ $t('pages.permission.whoCanEdit') }}
    </div>
    <v-radio-group v-model="isAdministrationPermissions" class="mt-0">
      <v-radio
        class="mt-0 ms-n1"
        :value="true">
        <template #label>
          <div class="text-body">
            {{ $t('sites.permission.administrators') }}
          </div>
        </template>
      </v-radio>
      <v-radio
        class="mt-0 ms-n1"
        :value="false">
        <template #label>
          <div class="text-body">
            {{ $t('sites.permission.groupMembers') }}
          </div>
        </template>
      </v-radio>
    </v-radio-group>
    <exo-identity-suggester
      v-if="!isAdministrationPermissions"
      ref="targetPermissions"
      v-model="specificGroupEntry"
      all-groups-for-admin
      class="mb-n3 mt-n3"
      include-groups
      include-spaces
      :labels="suggesterLabels"
      name="specificGroupPermission"
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
      isAdministrationPermissions: true,
      specificGroupEntry: null,
    }),
    computed: {
      isSpecificGroup () {
        return this.specificGroupEntry;
      },
      permission () {
        if (!this.isAdministrationPermissions && this.specificGroupEntry?.groupId) {
          return `*:${this.specificGroupEntry.groupId}`;
        } else {
          return `*:${this.administratorsPermission}`;
        }
      },
      suggesterLabels () {
        return {
          placeholder: this.$t('sites.permissionSuggester.placeholder'),
          noDataLabel: this.$t('sites.permissionSuggester.noData'),
        };
      },
    },
    watch: {
      permission () {
        this.$emit('input', this.permission);
      },
      isAdministrationPermissions () {
        if (this.isAdministrationPermissions) {
          this.specificGroupEntry = null;
        }
      },
    },
    async created () {
      const permission = this.value?.includes?.(':') ? this.value?.split?.(':')?.[1] : this.value;
      this.isAdministrationPermissions = permission === this.administratorsPermission;
      if (this.isAdministrationPermissions) {
        this.specificGroupEntry = null;
      } else if (permission) {
        await this.retrieveObject(permission);
      } else {
        this.isAdministrationPermissions = true;
        this.specificGroupEntry = null;
      }
    },
    methods: {
      async retrieveObject (groupId) {
        groupId = groupId.includes(':') ? groupId.split(':')[1] : groupId;
        if (groupId.indexOf('/spaces/') === 0) {
          const space = await eXo.$spaceService.getSpaceByGroupId(groupId);
          if (space) {
            this.specificGroupEntry = {
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
            };
          }
        } else {
          const group = await eXo.$identityService.getIdentityByProviderIdAndRemoteId('group', groupId);
          if (group) {
            this.specificGroupEntry = {
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
            };
          }
        }
      },
    },
  };
</script>