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
  <v-menu
    v-model="menu"
    :content-class="menuId"
    :left="!$vuetify.rtl"
    offset-y
    :right="$vuetify.rtl">
    <template #activator="{ on, attrs }">
      <v-btn
        :aria-label="$t('portlets.menu.open')"
        class="mx-auto"
        icon
        small
        v-bind="attrs"
        v-on="on">
        <v-icon class="icon-default-color" size="16">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-hover v-if="menu" @input="hoverMenu = $event">
      <v-list
        class="pa-0"
        dense
        @focusout="menu = false"
        @mouseout="menu = false">
        <v-list-item-group v-model="listItem">
          <v-list-item
            dense
            :href="editLayoutLink"
            rel="opener"
            target="_blank">
            <v-icon size="13">
              fa-edit
            </v-icon>
            <v-list-item-title class="ps-2">
              {{ $t('portlets.label.editInstance') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('portlet-instance-edit', portletInstance)">
            <v-icon size="13">
              fa-edit
            </v-icon>
            <v-list-item-title class="ps-2">
              {{ $t('portlets.label.editProperties') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip bottom :disabled="!portletInstance.system">
            <template #activator="{ on, attrs }">
              <div
                v-bind="attrs"
                v-on="on">
                <v-list-item
                  dense
                  :disabled="portletInstance.system"
                  @click="$root.$emit('portlet-instance-delete', portletInstance)">
                  <v-icon
                    :class="!portletInstance.system && 'error--text' || 'disabled--text'"
                    size="13">
                    fa-trash
                  </v-icon>
                  <v-list-item-title
                    class="ps-2"
                    :class="!portletInstance.system && 'error--text' || 'disabled--text'">
                    {{ $t('portlets.label.delete') }}
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('portlets.label.system.noDelete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </v-menu>
</template>
<script>
  export default {
    props: {
      portletInstance: {
        type: Object,
        default: null,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
      listItem: null,
      menuId: `PortletInstanceMenu${parseInt(Math.random() * 10000)}`,
    }),
    computed: {
      portletInstanceId () {
        return this.portletInstance?.id;
      },
      name () {
        return this.$te(this.portletInstance?.name) ? this.$t(this.portletInstance?.name) : this.portletInstance?.name;
      },
      hasEditMode () {
        return this.portletInstance?.supportedModes?.find?.(mode => mode === 'edit');
      },
      editLayoutLink () {
        return `/portal/${eXo.env.portal.portalName}/portlet-editor?id=${this.portletInstanceId}&portletMode=${this.hasEditMode && 'edit' || 'view'}`;
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
          this.$root.$emit('portlet-instance-menu-opened', this.portletInstanceId);
        } else {
          this.$root.$emit('portlet-instance-menu-closed', this.portletInstanceId);
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
      this.$root.$on('portlet-instance-menu-opened', this.checkMenuStatus);
      document.addEventListener('click', this.closeMenuOnClick);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-menu-opened', this.checkMenuStatus);
      document.removeEventListener('click', this.closeMenuOnClick);
    },
    methods: {
      closeMenuOnClick (e) {
        if (e.target && !e.target.closest(`.${this.menuId}`)) {
          this.menu = false;
        }
      },
      checkMenuStatus (instanceId) {
        if (this.menu && instanceId !== this.portletInstance.id) {
          this.menu = false;
        }
      },
    },
  };
</script>