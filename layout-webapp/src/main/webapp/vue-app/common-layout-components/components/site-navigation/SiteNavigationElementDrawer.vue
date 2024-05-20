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
    ref="siteNavigationAddElementDrawer"
    id="siteNavigationAddElementDrawer"
    :loading="loading"
    right
    eager
    allow-expand
    @closed="close">
    <template slot="title">
      <div class="d-flex">
        <v-icon
          size="16"
          class="clickable"
          @click="back">
          fas fa-arrow-left
        </v-icon>
        <span class="ms-2"> {{ drawerTitle }}</span>
      </div>
    </template>
    <template slot="content">
      <v-card class="mx-4 my-4 px-2 py-2 elevation-0">
        <v-form
          v-model="isValidForm">
          <template>
            <span class="font-weight-bold text-start text-truncate-2 text-color body-2">{{ $t('siteNavigation.label.selectElementType') }}</span>
            <v-select
              v-model="elementType"
              :items="elementTypes"
              item-text="text"
              item-value="value"
              dense
              class="caption pt-1 mb-5"
              outlined />
          </template>
          <template>
            <span class="font-weight-bold text-start text-truncate-2 text-color body-2 mt-8">{{ $t('siteNavigation.label.selectOpenType') }}</span>
            <v-select
              v-model="target"
              :items="targetTypes"
              item-text="text"
              item-value="value"
              dense
              class="caption pt-1 mb-5"
              outlined />
          </template>
          <template v-if="isLinkElement">
            <span class="font-weight-bold text-start text-color body-2 mt-8">{{ $t('siteNavigation.label.link') }}</span>
            <v-text-field
              v-model="link"
              :placeholder="$t('siteNavigation.label.enterUrl') "
              :rules="linkRules"
              class="pt-0"
              type="text"
              required
              outlined
              dense />
          </template>
          <site-navigation-page-element
            v-else
            :element-type="elementType"
            :selected-page="selectedPage" />
        </v-form>
      </v-card>
    </template>
    <template slot="footer">
      <div class="d-flex justify-end">
        <v-btn
          :disabled="loading"
          class="btn ms-2"
          @click="close">
          {{ $t('siteNavigation.label.btn.cancel') }}
        </v-btn>
        <v-btn
          :loading="loading"
          :disabled="disabled"
          class="btn btn-primary ms-2"
          @click="saveElement">
          {{ $t('siteNavigation.label.btn.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
export default {

  data() {
    return {
      elementType: 'PAGE',
      target: 'SAME_TAB',
      link: '',
      linkRules: [url => !!(url?.match(/^((https?:\/\/)?(www\.)?[a-zA-Z0-9:._\\/+=-]+\.[^\s]{2,})|(javascript:)|(\/portal\/)/))
              || ( !url?.length && this.$t('siteNavigation.required.error.message') || this.$t('siteNavigation.label.invalidLink'))],
      navigationNode: null,
      elementName: null,
      elementTitle: null,
      pageTemplate: 'empty',
      selectedPage: null,
      loading: false,
      resetDrawer: true,
      editMode: false,
      pageToEdit: false,
      isValidForm: true,
    };
  },
  computed: {
    elementTypes() {
      return [
        {
          text: this.$t('siteNavigation.label.newPage'),
          value: 'PAGE',
        },
        {
          text: this.$t('siteNavigation.label.existingPage'),
          value: 'existingPage',
        },
        {
          text: this.$t('siteNavigation.label.link'),
          value: 'LINK',
        },
      ];
    },
    targetTypes() {
      return [
        {
          text: this.$t('siteNavigation.label.sameTab'),
          value: 'SAME_TAB',
        },
        {
          text: this.$t('siteNavigation.label.newTab'),
          value: 'NEW_TAB',
        },
      ];
    },
    isLinkElement() {
      return this.elementType === 'LINK';
    },
    drawerTitle() {
      return this.editMode && this.$t('siteNavigation.label.editElement') || this.$t('siteNavigation.addElementDrawer.title');
    },
    disabled() {
      return !this.isValidForm || this.isLinkElement && !this.link || this.elementType === 'existingPage' && !this.selectedPage || false;
    },
  },
  created() {
    this.$root.$on('open-add-element-drawer', this.open);
    this.$root.$on('close-add-element-drawer', this.close);
    this.$root.$on('page-template-changed', this.changePageTemplate);
    this.$root.$on('existing-page-selected', this.changeSelectedPage);
  },
  methods: {
    open(elementName, elementTitle, navigationNode, editMode) {
      this.resetDrawer = true;
      this.elementName = elementName;
      this.elementTitle = elementTitle;
      this.navigationNode = navigationNode;
      this.target = navigationNode?.target;
      this.editMode = editMode;
      if (editMode && this.navigationNode?.pageKey) {
        const pageRef = this.navigationNode.pageKey.ref ||`${ this.navigationNode.pageKey.site.typeName}::${ this.navigationNode.pageKey.site.name}::${this.navigationNode.pageKey.name}`;
        this.$pageLayoutService.getPage(pageRef)
          .then((page) => {
            this.selectedPage = {
              pageRef,
              displayName: page.state.displayName || page.key.name,
            };
            this.pageToEdit = page;
            this.elementType = page.state?.type === 'LINK' && 'LINK' || 'existingPage';
            this.link = page?.state?.link;
            this.$nextTick().then(() => {
              this.$refs.siteNavigationAddElementDrawer.open();
              this.$nextTick()
                .then(() => {
                  this.$root.$emit('set-selected-page', page.state);
                });
            });
          });
      } else {
        this.$refs.siteNavigationAddElementDrawer.open();
      }
    },
    close() {
      if (this.resetDrawer) {
        this.reset();
      }
      this.$refs.siteNavigationAddElementDrawer.close();
    },
    back() {
      this.resetDrawer = false;
      this.$refs.siteNavigationAddElementDrawer.close();
    },
    reset() {
      this.selectedPage = null;
      this.pageToEdit = null;
      this.elementType = 'PAGE';
      this.link = '';
      this.target = 'SAME_TAB';
      this.$root.$emit('reset-element-drawer');
    },
    changePageTemplate(pageTemplate) {
      this.pageTemplate = pageTemplate;
    },
    changeSelectedPage(selectedPage) {
      this.selectedPage = selectedPage;
    },
    saveElement() {
      if (this.editMode) {
        this.updateElement();
      } else {
        this.createElement();
      }
    },
    createElement() {
      if (this.elementType === 'existingPage') {
        const pageRef = this.selectedPage?.pageRef;
        this.$root.$emit('save-node-with-page', {
          'pageRef': pageRef,
          'nodeTarget': this.target,
          'pageType': this.elementType
        });
        this.loading = false;
      } else {
        this.loading = true;
        this.$pageLayoutService.createPage(
          this.elementName,
          this.elementTitle,
          this.navigationNode.siteKey.name,
          this.navigationNode.siteKey.type,
          this.elementType, this.elementType === 'LINK' && this.link || null,
          this.elementType === 'PAGE' && this.pageTemplate?.id || null)
          .then((createdPage) => {
            const pageRef = createdPage?.key?.ref || `${createdPage?.key.site.typeName}::${createdPage?.key.site.name}::${createdPage?.pageContext?.key.name}`;
            this.$root.$emit('save-node-with-page', {
              'pageRef': pageRef,
              'nodeTarget': this.target,
              'pageType': this.elementType,
              'createdPage': createdPage,
              'openEditLayout': this.elementType === 'PAGE',
            });
          }).catch(() => {
            this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageCreation.error'), 'error');
          }).finally(() => this.loading = false);
      }
    },
    updateElement() {
      if (this.elementType === 'LINK') {
        if (this.pageToEdit?.type === 'LINK') {
          this.updatePageLink();
        } else {
          this.createElement();
        }
      } else if (this.elementType === 'PAGE') {
        this.createElement();
      } else if (this.elementType === 'existingPage') {
        const pageRef = this.selectedPage?.pageRef;
        this.$root.$emit('save-node-with-page', {
          'pageRef': pageRef,
          'nodeTarget': this.target,
          'pageType': this.elementType
        });
      }
    },
    updatePageLink() {
      const pageRef = this.pageToEdit?.key?.ref || `${this.pageToEdit?.key.site.typeName}::${this.pageToEdit?.key.site.name}::${this.pageToEdit?.key.name}`;
      this.$pageLayoutService.updatePageLink(pageRef, this.link)
        .then(() => {
          this.$root.$emit('save-node-with-page', {
            'pageRef': pageRef,
            'nodeTarget': this.target,
            'pageType': this.elementType
          });
        }).catch(() => {
          this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageUpdate.error'), 'error');
        });
    }
  }
};
</script>