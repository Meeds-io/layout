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
  <div
    :id="id"
    ref="content"
    class="layout-application no-applications-spacing full-width"
    :class="$root.portletMode === 'view' && 'light-grey-background-color pa-5 ma-n5' || 'pa-5 border-box-sizing application-body'"
    :style="cssStyle"></div>
</template>
<script>
  export default {
    data: () => ({
      applicationInstalled: false,
      applicationContent: null,
      contentRetrieved: 0,
    }),
    computed: {
      id () {
        return this.$root.portletMode === 'view' ? `UIPortlet-${this.$root?.portletInstance?.applicationId}` : this.$root?.portletInstance?.applicationId;
      },
      portletMode () {
        return this.$root.portletMode;
      },
      portletInstanceId () {
        return this.$root.portletInstanceId;
      },
      isEmpty () {
        return this.contentRetrieved
          && !this.applicationContent
          && !this.$el?.querySelector?.('.PORTLET-FRAGMENT')?.offsetHeight;
      },
      cssStyle () {
        return this.isEmpty && {
          'min-height': '100px',
          'display': 'block !important',
        } || null;
      },
    },
    watch: {
      applicationContent () {
        if (this.applicationContent) {
          this.installApplication();
        }
      },
      portletMode () {
        this.retrieveData();
      },
      isEmpty: {
        immediate: true,
        handler () {
          this.$emit('empty', this.isEmpty);
        },
      },
      applicationInstalled () {
        this.$emit('initialized');
      },
    },
    created () {
      document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      this.retrieveData();
    },
    mounted () {
      this.installApplication();
      this.$root.portletInstanceElement = this.$el;
    },
    methods: {
      installApplication () {
        if (this.$refs.content && this.applicationContent) {
          eXo.$applicationUtils.handleApplicationContent(this.applicationContent, this.$refs.content);
          // Cleanup JS memory
          this.applicationContent = null;
          // Wait for some seconds for application to be displayed
          this.contentRetrieved++;
          const interval = window.setInterval(() => {
            if (this.contentRetrieved >= 100 || !this.isEmpty) {
              document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
              this.applicationInstalled = true;
              window.clearInterval(interval);
            } else if (this.isEmpty) {
              this.contentRetrieved++;
              if (this.contentRetrieved === 30) {
                document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
                this.$emit('empty-message');
              }
            }
          }, 50);
        }
      },
      retrieveData () {
        this.contentRetrieved = 0;
        fetch(`/portal/${eXo.env.portal.portalName}/portlet-viewer?portletInstanceId=${this.portletInstanceId}&noCache=true&maximizedPortletMode=${this.portletMode}&fullRender=true`, {
          credentials: 'include',
          method: 'GET',
          redirect: 'manual',
        })
          .then(resp => {
            if (resp?.status === 200) {
              return resp.text();
            } else {
              throw new Error('The retrieved page is not a portal page');
            }
          })
          .then(applicationContent => this.applicationContent = applicationContent);
      },
    },
  };
</script>