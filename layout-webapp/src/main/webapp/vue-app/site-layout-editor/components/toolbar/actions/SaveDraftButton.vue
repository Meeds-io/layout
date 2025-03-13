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
  <v-tooltip bottom>
    <template #activator="{on, attrs}">
      <v-btn
        v-bind="attrs"
        :aria-label="$t('layout.saveDraft')"
        class="btn me-3"
        :loading="loading"
        v-on="on"
        @click="saveDraft">
        {{ $t('layout.saveDraft') }}
      </v-btn>
    </template>
    <span>{{ $t('layout.saveSiteDraftTooltip') }}</span>
  </v-tooltip>
</template>
<script>
  export default {
    data: () => ({
      loading: false,
    }),
    methods: {
      saveDraft () {
        this.loading = true;
        this.$root.$on('layout-draft-saved', this.stopLoadingSuccess);
        this.$root.$on('layout-draft-save-error', this.stopLoadingError);
        this.$root.$emit('layout-save-draft');
      },
      stopLoadingSuccess () {
        this.stopLoading(true);
      },
      stopLoadingError () {
        this.stopLoading();
      },
      stopLoading (success) {
        if (this.loading && success) {
          this.$root.$emit('alert-message', this.$t('layout.siteDraftSavedSuccessfully'), 'success');
        }
        this.$root.$off('layout-draft-saved', this.stopLoadingSuccess);
        this.$root.$off('layout-draft-save-error', this.stopLoadingError);
        this.loading = false;
      },
    },
  };
</script>