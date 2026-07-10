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
    ref="drawer"
    id="editSectionDrawer"
    v-model="drawer"
    right
    disable-pull-to-refresh>
    <template #title>
      {{ $t('layout.editSectionTitle', {0: index + 1}) }}
    </template>
    <template #titleIcons>
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <div
            v-on="on"
            v-bind="attrs">
            <v-btn
              :aria-label="$t('layout.cloneSection')"
              class="white text-color border-color me-2"
              height="32"
              width="32"
              icon
              @click="$root.$emit('layout-section-clone', section, index)">
              <v-icon class="icon-default-color" size="20">fa-copy</v-icon>
            </v-btn>
          </div>
        </template>
        {{ $t('layout.cloneSection') }}
      </v-tooltip>
      <v-tooltip v-if="$root.isAdministrator" bottom>
        <template #activator="{on, attrs}">
          <div
            v-on="on"
            v-bind="attrs">
            <v-btn
              :aria-label="$t('layout.saveAsSectionTemplate')"
              :loading="savingAsTemplate"
              class="white text-color border-color me-2"
              height="32"
              width="32"
              icon
              @click="saveAsTemplate">
              <v-icon class="icon-default-color" size="20">fa-columns</v-icon>
            </v-btn>
          </div>
        </template>
        {{ $t('layout.saveAsSectionTemplate') }}
      </v-tooltip>
    </template>
    <template v-if="drawer && section" #content>
      <v-card class="pa-4" flat>
        <section-template-grid-editor
          v-if="sectionType === $layoutUtils.gridTemplate"
          :rows-count="section.rowsCount"
          :cols-count="section.colsCount"
          :background-properties="section"
          @rows-updated="rows = $event"
          @cols-updated="cols = $event" />
        <section-template-flex-editor
          v-else-if="sectionType === $layoutUtils.flexTemplate"
          :cols-count="cols"
          :background-properties="section"
          @cols-updated="cols = $event" />
        <layout-editor-section-margin-input
          v-if="section"
          ref="marginInput"
          v-model="section"
          class="mt-4" />
        <layout-editor-background-input
          v-if="section"
          ref="backgroundInput"
          v-model="section"
          class="mt-4" />
        <template v-if="isDynamicSection">
          <div class="d-flex align-center mt-4">
            <div class="text-header mb-2">
              {{ $t('layout.advancedOptions') }}
            </div>
          </div>
          <div class="d-flex align-center mt-2">
            {{ $t('layout.mobileView') }}
          </div>
          <div class="d-flex flex-column justify-center">
            <v-radio-group
              v-model="mobileInColumns"
              class="my-auto text-no-wrap ms-n1 mt-2"
              mandatory>
              <v-radio
                :value="false"
                class="mx-0">
                <template #label>
                  <span class="text-font-size text-color">{{ $t('layout.listAppsInRows') }}</span>
                </template>
              </v-radio>
              <v-radio
                :value="true"
                class="mx-0">
                <template #label>
                  <span class="text-font-size text-color">{{ $t('layout.listAppsInColumns') }}</span>
                </template>
              </v-radio>
            </v-radio-group>
          </div>
          <div class="d-flex align-center mt-2">
            {{ $t('layout.scrollStickyBehavior') }}
          </div>
          <div class="d-flex flex-column justify-center">
            <v-radio-group
              v-model="stickyApplication"
              class="my-auto text-no-wrap ms-n1 mt-2"
              mandatory>
              <v-radio
                :value="false"
                class="mx-0">
                <template #label>
                  <span class="text-font-size text-color">{{ $t('layout.scrollNoSticky') }}</span>
                </template>
              </v-radio>
              <v-radio
                :value="true"
                class="mx-0">
                <template #label>
                  <span class="text-font-size text-color">{{ $t('layout.scrollStickyLastApp') }}</span>
                </template>
              </v-radio>
            </v-radio-group>
          </div>
        </template>
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          v-if="canRemove"
          color="error"
          outlined
          elevation="0"
          class="ignore-vuetify-classes"
          @click="removeSection">
          <span class="text-none">{{ $t('layout.delete') }}</span>
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn"
          @click="close">
          <span class="text-none">{{ $t('layout.cancel') }}</span>
        </v-btn>
        <v-btn
          :disabled="!modified"
          class="btn btn-primary ms-4"
          @click="apply">
          <span class="text-none">{{ $t('layout.apply') }}</span>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    originalSection: null,
    section: null,
    drawer: false,
    index: null,
    rows: 0,
    cols: 0,
    canRemove: false,
    mobileInColumns: false,
    enableBackgroundColor: false,
    enableBackgroundImage: false,
    backgroundColor: '#FFFFFFFF',
    backgroundImageStyle: null,
    backgroundImage: null,
    backgroundEffect: null,
    backgroundRepeat: null,
    backgroundSize: null,
    stickyApplication: false,
    optionsModified: false,
    savingAsTemplate: false,
  }),
  computed: {
    colsCount() {
      return this.section.template === this.$layoutUtils.flexTemplate ? this.section.children.length : this.section?.colsCount;
    },
    modified() {
      return this.optionsModified || this.section?.rowsCount !== this.rows || this.colsCount !== this.cols;
    },
    sectionType() {
      return this.section?.template;
    },
    isDynamicSection() {
      return this.sectionType === this.$layoutUtils.flexTemplate;
    },
  },
  watch: {
    section: {
      deep: true,
      handler() {
        if (this.drawer) {
          this.optionsModified = true;
        }
      }
    },
    stickyApplication() {
      if (this.drawer) {
        if (!this.section.cssClass) {
          this.section.cssClass = '';
        }
        if (this.stickyApplication) {
          this.section.cssClass += ' layout-sticky-application';
        } else {
          this.section.cssClass = this.section.cssClass.replace(/layout-sticky-application/g, '');
        }
        this.optionsModified = true;
      }
    },
    mobileInColumns() {
      if (this.drawer) {
        if (!this.section.cssClass) {
          this.section.cssClass = '';
        }
        if (this.mobileInColumns) {
          this.section.cssClass += ' layout-mobile-columns';
        } else {
          this.section.cssClass = this.section.cssClass.replace(/layout-mobile-columns/g, '');
        }
        this.optionsModified = true;
      }
    },
  },
  methods: {
    open(section, index, length) {
      this.section = JSON.parse(JSON.stringify(section));
      this.section.children = section.children;
      this.stickyApplication = this.section.cssClass?.includes?.('layout-sticky-application');
      this.mobileInColumns = this.section.cssClass?.includes?.('layout-mobile-columns');
      this.optionsModified = false;
      this.originalSection = JSON.parse(JSON.stringify(section));
      this.index = index;
      if (this.section.template === this.$layoutUtils.flexTemplate) {
        this.rows = 1;
        this.cols = this.section.children.length;
      } else {
        this.rows = this.section?.rowsCount;
        this.cols = this.section?.colsCount;
      }
      this.canRemove = length > 1;
      this.$nextTick().then(() => this.$refs.drawer.open());
    },
    removeSection() {
      this.close();
      this.$root.$emit('layout-remove-section', this.index);
    },
    async apply() {
      await this.$refs.backgroundInput.apply();
      const section = JSON.parse(JSON.stringify(this.section));
      section.children = this.section.children;
      if (section.template === this.$layoutUtils.flexTemplate && this.section.children.length !== this.cols) {
        this.$layoutUtils.editDynamicSection(section, this.cols);
      } else if (section.template === this.$layoutUtils.gridTemplate) {
        this.$layoutUtils.editGridSection(section, this.rows, this.cols);
      }
      this.$root.$emit('layout-replace-section', this.index, section);
      this.close();
    },
    close() {
      this.$refs.drawer.close();
      this.section = null;
    },
    async saveAsTemplate() {
      this.savingAsTemplate = true;
      await this.$nextTick();
      window.setTimeout(() => {
        this.$root.$emit('layout-section-save-as-template', this.section);
        this.close();
        this.savingAsTemplate = false;
      }, 200);
    },
  },
};
</script>