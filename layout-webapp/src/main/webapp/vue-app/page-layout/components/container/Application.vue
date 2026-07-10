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
    ref="content"
    :id="id"
    :class="cssClass"
    :style="cssStyle"
    class="layout-application">
    <div
      v-if="backgroundLayerStyle"
      class="layout-background-layer"
      :style="backgroundLayerStyle"></div>
  </div>
</template>
<script>
export default {
  props: {
    container: {
      type: Object,
      default: null,
    },
    parentId: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    applicationContent: null,
    contentRetrieved: false,
  }),
  computed: {
    nodeUri() {
      return this.$root.nodeUri;
    },
    storageId() {
      return this.container?.storageId;
    },
    portletId() {
      return this.storageId || this.container?.id;
    },
    contentId() {
      return this.container?.contentId;
    },
    id() {
      return `UIPortlet-${this.container?.id || this.storageId || `${this.parentId}-${parseInt(Math.random() * 10000)}`}`;
    },
    cssStyle() {
      return this.$applicationUtils.getStyle(this.container, {
        isApplicationBackground: true,
        isApplicationScroll: true,
        appStyle: true,
      });
    },
    backgroundLayerStyle() {
      return this.$applicationUtils.getBackgroundLayerStyle(this.container, {});
    },
    cssClass() {
      return this.container.cssClass || '';
    },
  },
  watch: {
    portletId() {
      this.retrieveData();
    },
    nodeUri() {
      this.retrieveData();
    },
    applicationContent() {
      if (this.applicationContent) {
        this.installApplication();
      }
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
      if (this.$refs.content && this.nodeUri) {
        if (!this.portletId) {
          console.warn(`Application '${this.contentId}' doesn't have a storageId neither and id`); // eslint-disable-line no-console
        } else if (this.applicationContent) {
          this.$applicationUtils.handleApplicationContent(this.applicationContent, this.$refs.content);
          this.applicationContent = null;
        }
      }
    },
    retrieveData() {
      if (this.portletId && this.nodeUri && !this.contentRetrieved) {
        this.contentRetrieved = true;
        this.$applicationUtils.getApplicationContent(this.nodeUri, this.portletId, 'VIEW', this.$root.siteId)
          .then(applicationContent => this.applicationContent = applicationContent);
      }
    },
    hasUnit(length) {
      return Number.isNaN(Number(length));
    },
  }
};
</script>