<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

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
    :id="anchorId"
    :style="anchorStyle"
    class="flex-shrink-0 flex-grow-0"></div>
</template>
<script>
export default {
  props: {
    side: {
      type: String,
      default: 'right',
    },
  },
  data: () => ({
    // re-synced from the non-reactive global on each placement change, so the
    // anchor releases or reserves its width without a page reload
    placements: window.eXo?.env?.portal?.appPlacements || null,
    panelHeight: 0,
  }),
  computed: {
    anchorId() {
      return `pageBody${this.side === 'left' && 'Left' || 'Right'}Panel`;
    },
    stuckAllowed() {
      return (this.$vuetify?.breakpoint?.width || 0) >= (this.$vuetify?.breakpoint?.thresholds?.lg || 1264);
    },
    placedApplication() {
      return this.side === 'left' && this.placements?.left || this.side === 'right' && this.placements?.right || null;
    },
    anchorStyle() {
      // the anchor reserves the panel width from the server-injected payload
      // at its own first render, and sticks to the scroll viewport so the
      // page scrolls on its own below while the panel stays fully displayed
      if (this.placedApplication && this.placements?.siteEligible && this.stuckAllowed) {
        return {
          position: 'sticky',
          top: '0',
          alignSelf: 'flex-start',
          height: this.panelHeight && `${this.panelHeight}px` || 'calc(var(--100vh, 100vh) - 56px)',
          flex: '0 0 420px',
          zIndex: '0',
        };
      }
      return null;
    },
  },
  created() {
    document.addEventListener('app-placement-changed', this.refreshPlacements);
    window.addEventListener('resize', this.computePanelHeight);
  },
  mounted() {
    this.computePanelHeight();
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refreshPlacements);
    window.removeEventListener('resize', this.computePanelHeight);
  },
  methods: {
    refreshPlacements() {
      this.placements = window.eXo?.env?.portal?.appPlacements || null;
      this.$nextTick(this.computePanelHeight);
    },
    computePanelHeight() {
      // the panel spans the scroll viewport: the element BodyScrollListener
      // promotes to the site scroller, whose client height already excludes
      // whatever renders above it
      const scroller = document.querySelector('.site-scroll-parent')
        || document.querySelector('#UIPageBody')
        || document.querySelector('#UISiteBody');
      this.panelHeight = scroller?.clientHeight || 0;
    },
  },
};
</script>
