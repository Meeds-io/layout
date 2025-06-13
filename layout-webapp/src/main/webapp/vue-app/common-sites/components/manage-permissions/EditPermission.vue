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
        :value="true"
        class="mt-0 ms-n1">
        <template #label>
          <div class="text-body">
            {{ $t('sites.permission.administrators') }}
          </div>
        </template>
      </v-radio>
      <v-radio
        :value="false"
        class="mt-0 ms-n1">
        <template #label>
          <div class="text-body">
            {{ $t('sites.permission.groupMembers') }}
          </div>
        </template>
      </v-radio>
    </v-radio-group>
    <template v-if="!isAdministrationPermissions">
      <exo-identity-suggester
        v-if="!specificGroupEntry"
        ref="targetPermissions"
        v-model="specificGroup"
        :labels="suggesterLabels"
        :search-options="{filterType: 'all'}"
        name="specificGroupPermission"
        class="mb-n3 mt-n3"
        include-spaces
        include-groups
        all-groups-for-admin />
      <v-list-item
        v-else
        class="pa-1 pb-1"
        dense>
        <v-list-item-action class="pa-0 ma-0">
          <select
            v-model="specificGroupEntry.role"
            aria-label="hidden"
            class="ignore-vuetify-classes width-auto pa-0 ma-0">
            <option
              v-for="role in roles"
              :key="role.value"
              :value="role.value">
              {{ role.text }}
            </option>
          </select>
        </v-list-item-action>
        <v-list-item-content class="d-flex align-center pa-0">
          <v-list-item-title class="d-flex align-center text-truncate">
            <div class="px-2">
              {{ $t('sites.permission.in') }}
            </div>
            <template v-if="specificGroupEntry.providerId === 'group'">
              <v-icon size="28" class="me-2">
                fa-users
              </v-icon>
              <span class="text-truncate">
                {{ specificGroupEntry.displayName }}
              </span>
            </template>
            <space-avatar
              v-else
              :space-id="specificGroupEntry.spaceId"
              class="text-truncate" />
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action class="pa-0 my-auto">
          <v-btn
            :title="$t('siteNavigation.label.deleteCustomGroup')"
            icon
            @click.stop.prevent="deleteSpecificGroup">
            <v-icon color="error" small>fa-trash</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </template>
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
    specificGroup: null,
    defaultRole: 'manager',
  }),
  computed: {
    roles() {
      return [{
        value: '*',
        text: this.$t('sites.permission.everyone'),
      }, {
        value: 'redactor',
        text: this.$t('sites.permission.redactors'),
      }, {
        value: 'publisher',
        text: this.$t('sites.permission.publishers'),
      }, {
        value: 'manager',
        text: this.$t('sites.permission.managers'),
      }];
    },
    isSpecificGroup() {
      return this.specificGroupEntry;
    },
    permission() {
      if (!this.isAdministrationPermissions && this.specificGroupEntry?.groupId) {
        return `${this.specificGroupEntry.role || this.defaultRole}:${this.specificGroupEntry.groupId}`;
      } else {
        return `*:${this.administratorsPermission}`;
      }
    },
    suggesterLabels() {
      return {
        placeholder: this.$t('sites.permissionSuggester.placeholder'),
        noDataLabel: this.$t('sites.permissionSuggester.noData')
      };
    },
  },
  watch: {
    permission() {
      this.$emit('input', this.permission);
    },
    async specificGroup() {
      if (this.specificGroup) {
        this.specificGroupEntry = {
          ...this.specificGroup,
          role: this.defaultRole,
        };
        await this.$nextTick();
        this.specificGroup = null;
      }
    },
    isAdministrationPermissions() {
      if (this.isAdministrationPermissions) {
        this.specificGroupEntry = null;
      }
    },
  },
  async created() {
    const permission = this.value?.includes?.(':') ? this.value?.split?.(':')?.[1] : this.value;
    this.isAdministrationPermissions = permission === this.administratorsPermission;
    if (this.isAdministrationPermissions) {
      this.specificGroupEntry = null;
    } else if (permission) {
      await this.retrieveObject(this.value, '*');
    } else {
      this.isAdministrationPermissions = true;
      this.specificGroupEntry = null;
    }
  },
  methods: {
    deleteSpecificGroup() {
      this.specificGroupEntry = null;
    },
    async retrieveObject(groupId, defaultRole) {
      const role = groupId.includes(':') ? groupId.split(':')[0] : defaultRole || this.defaultRole;
      groupId = groupId.includes(':') ? groupId.split(':')[1] : groupId;
      if (groupId.indexOf('/spaces/') === 0) {
        const space = await this.$spaceService.getSpaceByGroupId(groupId);
        if (space) {
          this.specificGroupEntry = {
            id: `space:${space.prettyName}`,
            remoteId: space.prettyName,
            spaceId: space.id,
            groupId: space.groupId,
            providerId: 'space',
            displayName: space.displayName,
            role,
            profile: {
              fullName: space.displayName,
              originalName: space.shortName,
              avatarUrl: space.avatarUrl ? space.avatarUrl : `/portal/rest/v1/social/spaces/${space.prettyName}/avatar`,
            },
          };
        }
      } else {
        const group = await this.$identityService.getIdentityByProviderIdAndRemoteId('group', groupId);
        if (group) {
          this.specificGroupEntry = {
            id: `group:${group.remoteId}`,
            remoteId: group.remoteId,
            spaceId: groupId,
            groupId: groupId,
            providerId: 'group',
            displayName: group.profile?.fullname,
            role,
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