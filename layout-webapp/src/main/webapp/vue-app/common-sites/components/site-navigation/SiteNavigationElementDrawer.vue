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
  <exo-drawer
    id="siteNavigationAddElementDrawer"
    ref="siteNavigationAddElementDrawer"
    v-model="drawer"
    allow-expand
    eager
    :loading="loading"
    right
    @closed="close"
    @expand-updated="$root.$emit('toggle-expand',$event)">
    <template #title>
      <div class="d-flex">
        <v-icon
          class="clickable"
          size="16"
          @click="back">
          fas fa-arrow-left
        </v-icon>
        <span class="ms-2"> {{ drawerTitle }}</span>
      </div>
    </template>
    <template v-if="drawer" #content>
      <v-card class="mx-4 my-4 px-2 py-2 elevation-0">
        <v-form
          v-model="isValidForm">
          <site-navigation-page-element 
            v-model="pageTempalateId" />
        </v-form>
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex justify-end">
        <v-btn
          class="btn ms-2"
          :disabled="loading"
          @click="close">
          {{ $t('siteNavigation.label.btn.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary ms-2"
          :disabled="disabled"
          :loading="loading"
          @click="createElement">
          {{ $t('siteNavigation.label.btn.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
  export default {
    data () {
      return {
        elementType: 'PAGE',
        target: 'SAME_TAB',
        drawer: false,
        loading: false,
        navigationNode: null,
        elementName: null,
        elementTitle: null,
        pageTempalateId: null,
        selectedPage: null,
        resetDrawer: true,
        isValidForm: true,
        pageTemplates: null,
      };
    },
    computed: {
      drawerTitle () {
        return this.$t('siteNavigation.addElementDrawer.title');
      },
      disabled () {
        return !this.isValidForm || !this.pageTemplate || false;
      },
      pageTemplate () {
        return this.pageTemplates?.find?.(item => item.id === this.pageTempalateId);
      },
    },
    created () {
      this.$root.$on('open-add-element-drawer', this.open);
      this.$root.$on('close-add-element-drawer', this.close);
      this.$root.$on('existing-page-selected', this.changeSelectedPage);
      this.$root.$on('page-templates-loaded', this.updatePageTemplates);
      this.pageTemplates = this.$root.pageTemplates;
    },
    beforeUnmount () {
      this.$root.$off('open-add-element-drawer', this.open);
      this.$root.$off('close-add-element-drawer', this.close);
      this.$root.$off('existing-page-selected', this.changeSelectedPage);
      this.$root.$off('page-templates-loaded', this.updatePageTemplates);
    },
    methods: {
      open (elementName, elementTitle, navigationNode) {
        this.resetDrawer = true;
        this.elementName = elementName;
        this.elementTitle = elementTitle;
        this.navigationNode = navigationNode;
        this.$refs.siteNavigationAddElementDrawer.open();
      },
      close () {
        if (this.resetDrawer) {
          this.reset();
        }
        this.$refs.siteNavigationAddElementDrawer.close();
      },
      back () {
        this.resetDrawer = false;
        this.$refs.siteNavigationAddElementDrawer.close();
      },
      reset () {
        this.selectedPage = null;
        this.$root.$emit('reset-element-drawer');
      },
      changeSelectedPage (selectedPage) {
        this.selectedPage = selectedPage;
      },
      updatePageTemplates (pageTemplates) {
        this.pageTemplates = pageTemplates;
      },
      createElement () {
        this.loading = true;
        eXo.$pageLayoutService.createPage( 
          this.elementName, 
          this.elementTitle, 
          this.navigationNode.siteKey.name, 
          this.navigationNode.siteKey.type, 
          this.elementType, 
          '', 
          this.pageTemplate?.id || null
        ).then(createdPage => {
          const pageRef = createdPage?.key?.ref || `${createdPage?.key.site.typeName}::${createdPage?.key.site.name}::${createdPage?.pageContext?.key.name}`;
          this.$root.$emit('save-node-with-page', {
            pageRef,
            'nodeTarget': this.target,
            'pageType': this.elementType,
            createdPage,
            'openEditLayout': this.elementType === 'PAGE',
          });
          return createdPage;
        }).then(page => {
          this.$root.$emit('page-layout-created', page, this.pageTemplate);
        }).catch(() => {
          this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageCreation.error'), 'error');
        }).finally(() => this.loading = false);
      },
    },
  };
</script>
