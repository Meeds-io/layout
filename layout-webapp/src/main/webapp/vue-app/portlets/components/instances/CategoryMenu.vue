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
            @click="$root.$emit('portlet-instance-category-edit', category)">
            <v-icon size="13">
              fa-edit
            </v-icon>
            <v-list-item-title class="ps-2">
              {{ $t('portlets.label.edit') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip bottom :disabled="!category.system">
            <template #activator="{ on, attrs }">
              <div
                v-bind="attrs"
                v-on="on">
                <v-list-item
                  dense
                  :disabled="category.system"
                  @click="$root.$emit('portlet-instance-category-delete', category)">
                  <v-icon
                    :class="!category.system && 'error--text' || 'disabled--text'"
                    size="13">
                    fa-trash
                  </v-icon>
                  <v-list-item-title
                    class="ps-2"
                    :class="!category.system && 'error--text' || 'disabled--text'">
                    {{ $t('portlets.label.delete') }}
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('portlets.label.category.system.noDelete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </v-menu>
</template>
<script>
  export default {
    props: {
      category: {
        type: Object,
        default: null,
      },
      value: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      menu: false,
      hoverMenu: false,
      listItem: null,
      menuId: `PortletInstanceCategoryMenu${parseInt(Math.random() * 10000)}`,
    }),
    computed: {
      categoryId () {
        return this.category?.id;
      },
      name () {
        return this.$te(this.category?.name) ? this.$t(this.category?.name) : this.category?.name;
      },
      editLayoutLink () {
        return `/portal/administration/layout-editor?categoryId=${this.categoryId}`;
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
          this.$root.$emit('portlet-instance-category-menu-opened', this.categoryId);
        } else {
          this.$root.$emit('portlet-instance-category-menu-closed', this.categoryId);
        }
        this.$emit('input', this.menu);
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
      this.$root.$on('portlet-instance-category-menu-opened', this.checkMenuStatus);
      document.addEventListener('click', this.closeMenuOnClick);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-category-menu-opened', this.checkMenuStatus);
      document.removeEventListener('click', this.closeMenuOnClick);
    },
    methods: {
      closeMenuOnClick (e) {
        if (e.target && !e.target.closest(`.${this.menuId}`)) {
          this.menu = false;
        }
      },
      checkMenuStatus (instanceId) {
        if (this.menu && instanceId !== this.category.id) {
          this.menu = false;
        }
      },
    },
  };
</script>