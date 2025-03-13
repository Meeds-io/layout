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
  <v-btn
    :aria-label="$t('portlets.label.createInstance')"
    :disabled="disabled"
    icon
    :title="$t('portlets.label.createInstance')"
    @click="$root.$emit('portlet-instance-add', null, false, portlet.contentId)">
    <v-icon class="icon-default-icon" size="20">
      fa-plus
    </v-icon>
  </v-btn>
</template>
<script>
  export default {
    props: {
      portlet: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
      listItem: null,
      menuId: `PortletMenu${parseInt(Math.random() * 10000)}`,
    }),
    computed: {
      portletId () {
        return this.portlet?.id;
      },
      name () {
        return this.$te(this.portlet?.name) ? this.$t(this.portlet?.name) : this.portlet?.name;
      },
      editLayoutLink () {
        return `/portal/administration/layout-editor?portletContentId=${this.portletId}`;
      },
    },
    watch: {
      listItem () {
        if (this.menu) {
          this.menu = false;
          this.listItem = null;
        }
      },
      menu () {
        if (this.menu) {
          this.$root.$emit('portlet-menu-opened', this.portletId);
        } else {
          this.$root.$emit('portlet-menu-closed', this.portletId);
        }
      },
      hoverMenu () {
        if (!this.hoverMenu) {
          window.setTimeout(() => {
            if (!this.hoverMenu) {
              this.menu = false;
            }
          }, 200);
        }
      },
    },
    created () {
      this.$root.$on('portlet-menu-opened', this.checkMenuStatus);
      document.addEventListener('click', this.closeMenuOnClick);
    },
    beforeUnmount () {
      this.$root.$off('portlet-menu-opened', this.checkMenuStatus);
      document.removeEventListener('click', this.closeMenuOnClick);
    },
    methods: {
      closeMenuOnClick (e) {
        if (e.target && !e.target.closest(`.${this.menuId}`)) {
          this.menu = false;
        }
      },
      checkMenuStatus (portletId) {
        if (this.menu && portletId !== this.portlet.id) {
          this.menu = false;
        }
      },
    },
  };
</script>