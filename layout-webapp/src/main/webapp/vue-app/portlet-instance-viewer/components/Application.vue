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
  <div
    ref="content"
    :id="id"
    class="layout-application full-width"></div>
</template>
<script>
export default {
  data: () => ({
    applicationInstalled: false,
    applicationContent: null,
    loading: false,
  }),
  computed: {
    id() {
      return `UIPortlet-${this.$root?.portletInstance?.applicationId}`;
    },
  },
  watch: {
    applicationContent() {
      if (this.applicationContent) {
        this.installApplication();
      }
    },
    loading() {
      this.$emit('loading', this.loading);
    },
  },
  created() {
    this.retrieveData();
  },
  mounted() {
    this.installApplication();
  },
  methods: {
    installApplication() {
      if (this.$refs.content && this.applicationContent && !this.applicationInstalled) {
        this.applicationInstalled = true;
        this.$applicationUtils.handleApplicationContent(this.applicationContent, this.$refs.content);
      }
    },
    retrieveData() {
      this.loading = true;
      fetch(`/portal/${eXo.env.portal.portalName}/portlet-viewer?portletInstanceId=${this.$root.portletInstanceId}&noCache=true&maximizedPortletMode=view&fullRender=true`, {
        credentials: 'include',
        method: 'GET',
        redirect: 'manual'
      })
        .then(resp => {
          if (resp?.status === 200) {
            return resp.text();
          } else {
            throw new Error('Error while retrieving the portlet instance: ', this.$root.portletInstanceId);
          }
        })
        .then(applicationContent => this.applicationContent = applicationContent)
        .finally(() => window.setTimeout(() => this.loading = false, 200));
    },
  }
};
</script>