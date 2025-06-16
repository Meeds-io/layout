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
  <exo-drawer
    id="sitePropertiesDrawer"
    ref="drawer"
    v-model="drawer"
    :loading="loading"
    :right="!$vuetify.rtl"
    :allow-expand="!$root.isMobile"
    :go-back-button="isNew && step && !initialStep"
    @expand-updated="expanded = $event"
    @closed="close"
    @go-back="step = 0">
    <template #title>
      <span>{{ isNew && $t('siteManagement.drawer.addSite.title') || $t('siteManagement.drawer.properties.title') }}</span>
    </template>
    <template v-if="drawer" #content>
      <v-expand-transition>
        <div v-if="!step" class="my-4 ms-4">
          <div class="text-header me-4">
            {{ $t('sites.selectTemplate') }}
          </div>
          <div class="d-flex flex-wrap align-center">
            <site-management-template
              v-for="t in sortedTemplates"
              :key="t.id"
              :site-template="t"
              :selected="t.id === templateId"
              class="col-auto ps-0 pe-4"
              @select="templateId = t.id" />
          </div>
          <div class="d-flex justify-center my-4">
            <v-btn
              :disabled="!templateId"
              class="btn btn-primary"
              @click="step = 1">
              {{ $t('sites.stepper.start') }}
            </v-btn>
          </div>
          <layout-image-illustration-preview />
        </div>
        <v-stepper
          v-else
          v-model="step"
          :class="{
            'pe-3' : $root.isMobile,
            'mt-5' : !isNew,
          }"
          class="ma-0 py-0"
          vertical
          flat>
          <v-stepper-step
            v-if="isNew"
            :step="1"
            class="ma-4 pa-0"
            editable>
            {{ $t('sites.stepper.properties') }}
          </v-stepper-step>
          <v-stepper-content :step="1" class="pa-0 ma-0 no-border">
            <form
              v-if="step === 1"
              ref="form1"
              class="px-4 pb-4"
              @submit.stop.prevent="0">
              <translation-text-field
                ref="siteTitleTranslation"
                v-model="siteTitleTranslations"
                v-bind="{
                  autofocus: true,
                }"
                :placeholder="$t('siteManagement.label.siteLabel.placeholder')"
                :field-value.sync="siteLabel"
                :maxlength="maxTitleLength"
                :no-expand-icon="!expanded"
                :supported-languages="supportedLanguages"
                :default-language="defaultLanguage"
                :object-id="siteId"
                drawer-title="siteManagement.label.translateTitle"
                object-type="site"
                field-name="label"
                name="siteTitle"
                class="width-auto flex-grow-1"
                back-icon
                required>
                <template #title>
                  {{ $t('siteManagement.label.siteName.title') }}
                </template>
              </translation-text-field>
              <v-list-item class="pa-0" dense>
                <v-list-item-content>
                  <v-list-item-title />
                  {{ $t('sites.urlSlug.label') }}
                  <v-list-item-subtitle>
                    {{ $t('sites.urlSlug.caption') }}
                  </v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
              <div class="d-flex align-center">
                <div class="me-4">/portal/</div>
                <v-text-field
                  v-model="siteName"
                  :aria-label="$t('sites.urlSlug.label')"
                  :placeholder="$t('sites.urlSlug.placeholder')"
                  :disabled="!isNew"
                  :rules="isNew && rules"
                  class="pt-0"
                  type="text"
                  required="required"
                  outlined
                  dense
                  @keyup="customSiteName = true" />
              </div>
              <translation-text-field
                ref="siteDescriptionTranslation"
                v-model="siteDescriptionTranslations"
                :placeholder="$t('siteManagement.label.siteDescription.placeholder')"
                :field-value.sync="siteDescription"
                :object-id="siteId"
                :max-length="maxDescriptionLength"
                :no-expand-icon="!expanded"
                :supported-languages="supportedLanguages"
                :default-language="defaultLanguage"
                drawer-title="siteManagement.label.translateDescription"
                object-type="site"
                field-name="description"
                class="width-auto flex-grow-1 pt-4"
                back-icon
                rich-editor>
                <template #title>
                  <span>
                    {{ $t('siteManagement.label.siteDescription.title') }}           
                  </span>
                </template>
                <rich-editor
                  id="siteDescription"
                  ref="siteDescriptionEditor"
                  v-model="siteDescription"
                  :placeholder="$t('siteManagement.label.siteDescription.placeholder')"
                  :max-length="maxDescriptionLength"
                  :tag-enabled="false"
                  disable-suggester
                  ck-editor-type="site" />
              </translation-text-field>
              <font-icon-input
                v-model="siteIcon"
                class="my-4" />
              <site-management-banner
                ref="siteBanner"
                v-model="bannerUploadId"
                :banner-url="siteBannerUrl"
                :banner-data="bannerData" />
            </form>
          </v-stepper-content>
          <template v-if="isNew">
            <v-stepper-step
              :step="2"
              :editable="!saveDisabled"
              class="ma-4 pa-0">
              {{ $t('sites.stepper.permissions') }}
            </v-stepper-step>
            <v-stepper-content :step="2" class="pa-0 ma-0 no-border">
              <form
                v-if="step === 2"
                class="px-4"
                @submit.stop.prevent="0">
                <manage-permissions-edit
                  v-model="editPermission"
                  is-site />
                <manage-permissions-access
                  v-model="accessPermissions"
                  class="mt-4"
                  is-site />
              </form>
            </v-stepper-content>
          </template>
        </v-stepper>
      </v-expand-transition>
    </template>
    <template v-if="step > 0" slot="footer">
      <div class="d-flex">
        <v-btn
          v-if="isNew && step > initialStep"
          class="btn me-2"
          @click="step--">
          {{ $t('siteManagement.label.btn.previous') }}
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn ms-2"
          @click="close">
          {{ $t('siteManagement.label.btn.cancel') }}
        </v-btn>
        <v-btn
          v-if="isNew && step < 2"
          :disabled="saveDisabled"
          :loading="loading"
          @click="step++"
          class="btn btn-primary ms-2">
          {{ $t('siteManagement.label.btn.next') }}
        </v-btn>
        <v-btn
          v-else
          :disabled="saveDisabled"
          :loading="loading"
          @click="saveSite"
          class="btn btn-primary ms-2">
          {{ $t('siteManagement.label.btn.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    sites: {
      type: Array,
      default: null,
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    templates: null,
    templateId: null,
    step: 0,
    initialStep: 0,
    site: null,
    siteId: null,
    siteName: '',
    siteLabel: '',
    siteDescription: '',
    siteIcon: '',
    bannerId: null,
    maxTitleLength: 200,
    maxDescriptionLength: 1300,
    siteTitleTranslations: {},
    siteDescriptionTranslations: {},
    defaultLanguage: eXo.env.portal.defaultLanguage,
    supportedLanguages: null,
    isNew: false,
    customSiteName: false,
    expanded: false,
    siteBannerUrl: null,
    bannerData: null,
    bannerUploadId: null,
    accessPermissions: null,
    editPermission: null,
  }),
  computed: {
    rules() {
      return [
        (v) => !this.sites?.find?.(s => s.name === v) || this.$t('sites.urlSlug.alreadyExists'),
        (v) => (/^[a-zA-Z0-9_-]*$/).test(v) || this.$t('sites.urlSlug.badFormat'),
      ];
    },
    saveDisabled() {
      return !this.siteLabel
        || !this.siteName
        || (this.isNew && this.sites?.find?.(s => s.name === this.siteName))
        || !(/^[a-zA-Z0-9_-]*$/).test(this.siteName);
    },
    sortedTemplates() {
      const siteTemplates = this.templates?.filter?.(t => t.name) || [];
      siteTemplates.sort((a, b) => this.$root.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
      return siteTemplates;
    },
    siteTemplate() {
      return this.templates?.find?.(t => t.id === this.templateId);
    },
  },
  watch: {
    siteDescription() {
      if (this.$refs.siteDescriptionTranslation && this.drawer && this.step) {
        this.$refs.siteDescriptionTranslation.setValue(this.siteDescription);
      }
    },
    siteLabel() {
      if (this.isNew && !this.customSiteName) {
        this.siteName = this.normalizeText(this.siteLabel);
      }
    },
  },
  created() {
    this.$root.$on('open-site-properties-drawer', this.open);
  },
  beforeDestroy() {
    this.$root.$off('open-site-properties-drawer', this.open);
  },
  methods: {
    async open(site, templateId) {
      this.isNew = !site?.siteId;
      this.templateId = templateId;
      this.site = site || null;
      this.siteName = !this.isNew && site?.name || null;
      this.siteId = site?.siteId;
      this.siteIcon = site?.icon || 'fa-globe';
      this.siteLabel = site?.displayName || site?.name ;
      this.siteDescription = site?.description || '';
      this.siteBannerUrl = !this.isNew && site?.bannerUrl || null;
      this.accessPermissions = site?.accessPermissions;
      this.editPermission = site?.editPermission;
      this.step = (!this.isNew || templateId) ? 1 : 0;
      this.initialStep = this.step;
      this.customSiteName = !this.isNew;
      await this.getSiteLabels(site?.siteId || templateId);
      await this.getSiteDescriptions(site?.siteId || templateId);
      if (this.isNew && !this.templates?.length) {
        this.templates = await this.$siteTemplateService.getSiteTemplates();
      }
      this.bannerUploadId = null;
      this.bannerData = null;
      if (this.isNew && site?.bannerUrl) {
        const bannerBlob = await fetch(site.bannerUrl, {
          credentials: 'include',
          method: 'GET',
        }).then(resp => resp?.ok && resp.blob()).catch();
        if (bannerBlob) {
          this.bannerData = bannerBlob && await this.$utils.blobToBase64(bannerBlob);
          this.bannerUploadId = bannerBlob && await this.$uploadService.upload(bannerBlob);
        }
      }
      if (!this.supportedLanguages) {
        const translationConfiguration = await this.$translationService.getTranslationConfiguration();
        this.supportedLanguages = translationConfiguration?.supportedLanguages;
      }
      this.$refs.drawer.open();
    },
    getSiteLabels(siteId) {
      return this.$siteLayoutService.getSiteLabels(siteId)
        .then(data => this.siteTitleTranslations = data?.labels || {'en': null})
        .catch(() => this.siteTitleTranslations = {en: null});
    },
    getSiteDescriptions(siteId) {
      return this.$siteLayoutService.getSiteDescriptions(siteId)
        .then(data => this.siteDescriptionTranslations = data?.labels || {'en': null})
        .catch(() => this.siteDescriptionTranslations = {en: null});
    },
    close() {
      this.site = null;
      this.reset();
      this.$refs.drawer.close();
    },
    updateBannerUploadId(bannerUploadId) {
      this.bannerUploadId = bannerUploadId;
    },
    reset() {
      this.siteId = null;
      this.siteName = '';
      this.siteLabel = '';
      this.siteDescription = '';
      this.siteTitleTranslations = {};
      this.siteDescriptionTranslations = {};
      this.drawer = false;
      this.siteBannerUrl = null;
      this.$refs.siteBannerSelector?.reset?.();
    },
    async saveSite() {
      if (this.isNew) {
        await this.createSite();
      } else {
        await this.updateSite();
      }
    },
    async createSite() {
      this.siteName = this.normalizeText(this.siteName);
      this.loading = true;
      try {
        const site = await this.$siteLayoutService.createSite(this.siteName, this.templateId, this.siteLabel, this.siteDescription, false, 0, this.bannerUploadId !== '0' && this.bannerUploadId || null, this.siteIcon, this.accessPermissions, this.editPermission);
        await this.$translationService.saveTranslations('site',  site.siteId, 'label', this.siteTitleTranslations);
        await this.$translationService.saveTranslations('site', site.siteId, 'description', this.siteDescriptionTranslations);
        this.$root.$emit('alert-message', this.$t('siteManagement.label.createSite.success'), 'success');
        this.$root.$emit('site-created', site);
        this.close();
      } catch (e) {
        this.$root.$emit('alert-message', this.$t('siteManagement.label.createSite.error'), 'error');
      } finally {
        this.loading = false;
      }
    },
    async updateSite() {
      this.loading = true;
      try {
        await this.$siteLayoutService.updateSite(this.site.name, this.site.siteType, this.siteLabel, this.siteDescription, this.site.metaSite || this.site.displayed, this.site.displayed && this.site.displayOrder || 0, this.bannerUploadId !== '0' && this.bannerUploadId || null, this.bannerUploadId === '0', this.siteIcon);
        await this.$translationService.saveTranslations('site', this.siteId, 'label', this.siteTitleTranslations);
        await this.$translationService.saveTranslations('site', this.siteId, 'description', this.siteDescriptionTranslations);
        this.$root.$emit('alert-message', this.$t('siteManagement.label.updateSite.success'), 'success');
        this.$root.$emit('site-updated', this.site);
        this.close();
      } catch (e) {
        const message = e.message ==='401' &&  this.$t('siteManagement.label.updateSite.unauthorized') || this.$t('siteManagement.label.updateSite.error');
        this.$root.$emit('alert-message', message, 'error');
      } finally {
        this.loading = false;
      }
    },
    normalizeText(text) {
      return text && text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]/g, '').replace(/\s+/g, '').toLowerCase() || '';
    },
  }
};
</script>
