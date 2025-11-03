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
    id="portletInstanceDrawer"
    v-model="drawer"
    :loading="saving"
    :go-back-button="goBackButton"
    allow-expand
    right>
    <template #title>
      <span class="text-wrap">{{ isNew && $t('layout.portletInstance.drawerTitie.add') || $t('layout.portletInstance.drawerTitie.edit') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <translation-text-field
          ref="title"
          id="pageTemplateTitle"
          v-model="titleTranslations"
          :field-value.sync="title"
          :placeholder="$t('layout.portletInstance.namePlaceholder')"
          :maxlength="maxTitleLength"
          :object-id="instanceId"
          object-type="portletInstance"
          field-name="title"
          drawer-title="layout.portletInstance.nameTranslationDrawerTitle"
          class="width-auto flex-grow-1 pb-1"
          back-icon
          required>
          <template #title>
            <div class="text-subtitle-1">
              {{ $t('layout.portletInstance.name') }}
            </div>
          </template>
        </translation-text-field>
        <translation-text-field
          ref="descriptionTranslation"
          v-model="descriptionTranslations"
          :field-value.sync="description"
          :maxlength="maxDescriptionLength"
          :object-id="instanceId"
          object-type="portletInstance"
          field-name="description"
          drawer-title="layout.portletInstance.descriptionTranslationDrawerTitle"
          class="ma-1px mt-4"
          back-icon
          rich-editor>
          <template #title>
            <div class="text-subtitle-1">
              {{ $t('layout.portletInstance.description') }}
            </div>
          </template>
          <rich-editor
            id="portletInstanceDescription"
            ref="portletInstanceDescriptionEditor"
            v-model="descriptionTranslations[lang]"
            :placeholder="$t('layout.portletInstance.descriptionTranslationDrawerTitle')"
            :max-length="maxDescriptionLength"
            :tag-enabled="false"
            ck-editor-type="portletInstanceDescription"
            disable-suggester
            @ready="checkCKEdtiorDisplay" />
        </translation-text-field>
        <div class="mb-2 mt-4">{{ $t('layout.category') }}</div>
        <portlets-instance-category-input
          v-model="categoryId"
          class="mt-4" />
        <portlets-instance-portlet-input
          v-model="contentId"
          :disabled="!isNew || disableSelectedPortlet"
          class="mt-4" />
        <div class="d-flex flex-column mt-4">
          <div class="mb-2">{{ $t('portlets.selectWhoCanAddIt') }}</div>
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <div
                v-on="on"
                v-bind="attrs">
                <v-checkbox
                  v-model="aclAdministrators"
                  :label="$t('portlets.administrators')"
                  :aria-label="$t('portlets.administrators')"
                  class="ma-0 pa-0"
                  disabled />
              </div>
            </template>
            <span>{{ $t('portlets.administratorsMandatorySelectionTooltip') }}</span>
          </v-tooltip>
          <v-checkbox
            v-model="aclContributors"
            :label="$t('portlets.contentManagers')"
            :aria-label="$t('portlets.contentManagers')"
            class="ma-0 pa-0" />
          <v-checkbox
            v-model="aclSpaceHost"
            :label="$t('portlets.spaceHost')"
            :aria-label="$t('portlets.spaceHost')"
            class="ma-0 pa-0" />
        </div>
        <portlets-instance-preview
          ref="portletInstancePreview"
          v-model="illustrationUploadId"
          :instance-id="instanceId" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex me-2">
        <v-spacer />
        <v-btn
          class="btn mx-1"
          @click="close">
          {{ $t('layout.cancel') }}
        </v-btn>
        <v-btn
          :disabled="disabled"
          :loading="saving"
          class="btn btn-primary"
          @click="save">
          {{ $t('layout.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    instance: null,
    instanceId: null,
    categoryId: null,
    contentId: null,
    title: null,
    titleTranslations: {},
    description: null,
    descriptionTranslations: {},
    maxTitleLength: 250,
    maxDescriptionLength: 1000,
    illustrationUploadId: null,
    lang: eXo.env.portal.language,
    goBackButton: false,
    disableSelectedPortlet: false,
    saving: false,
    isNew: false,
    aclSpaceHost: false,
    aclContributors: false,
    aclAdministrators: true,
  }),
  computed: {
    disabled() {
      return !this.categoryId || !this.contentId || !this.title;
    },
  },
  watch: {
    aclContributors() {
      if (!this.aclContributors && this.aclSpaceHost) {
        this.aclSpaceHost = false;
      }
    },
    aclSpaceHost() {
      if (this.aclSpaceHost && !this.aclContributors) {
        this.aclContributors = true;
      }
    },
    description() {
      if (this.$refs.descriptionTranslation) {
        this.$refs.descriptionTranslation.setValue(this.description);
      }
      this.checkCKEdtiorDisplay();
    },
  },
  created() {
    this.$root.$on('portlet-instance-add', this.open);
    this.$root.$on('portlet-instance-edit', this.open);
  },
  beforeDestroy() {
    this.$root.$off('portlet-instance-add', this.open);
    this.$root.$off('portlet-instance-edit', this.open);
  },
  methods: {
    open(instance, goBackButton, contentId) {
      this.$root.$emit('close-alert-message');
      this.isNew = !instance;
      this.goBackButton = goBackButton;
      this.disableSelectedPortlet = !!contentId;
      this.instance = instance || {};
      this.instanceId = instance?.id || null;
      if (instance) {
        this.categoryId = instance.categoryId || null;
        this.aclSpaceHost = !instance.permissions?.length
          || !!instance.permissions?.find?.(p => p.includes('Everyone'))
          || !!instance.permissions?.find?.(p => p.includes('/platform/users'));
        this.aclContributors = this.aclSpaceHost || !!instance.permissions?.find?.(p => p.includes('/platform/web-contributors'));
      } else {
        this.categoryId = this.$root?.selectedCategoryId;
        this.aclSpaceHost = true;
        this.aclContributors = true;
      }
      this.contentId = instance?.contentId || contentId;
      this.title = instance?.name || null;
      this.titleTranslations = {};
      this.descriptionTranslations = {};
      this.description = instance?.description || null;
      this.$nextTick().then(() => this.$refs.drawer.open());
    },
    close() {
      this.$refs.drawer.close();
    },
    save() {
      this.saving = true;
      let message = null;
      let savedInstance = null;
      const newInstance = !this.instanceId;
      const saveInstanceRequest =
        !newInstance ?
          this.$portletInstanceService.getPortletInstance(this.instanceId)
            .then(instance => {
              instance.categoryId = this.categoryId;
              instance.permissions = this.getPermissions();
              return this.$portletInstanceService.updatePortletInstance(instance)
                .then(() => {
                  message = this.$t('layout.portletInstanceUpdatedSuccessfully');
                  savedInstance = instance;
                  return instance;
                });
            })
          : this.$portletInstanceService.createPortletInstance({
            categoryId: this.categoryId,
            contentId: this.contentId,
            permissions: this.getPermissions(),
          })
            .then(instance => {
              message = this.$t('layout.portletInstanceCreatedSuccessfully');
              savedInstance = instance;
              return instance;
            });
      return saveInstanceRequest
        .then(instance => {
          if (instance) {
            this.instanceId = instance.id;
            return this.$nextTick();
          }
        })
        .then(() => this.$translationService.saveTranslations('portletInstance', this.instanceId, 'title', this.titleTranslations))
        .then(() => this.$translationService.saveRichTranslations('portletInstance', this.instanceId, 'description', this.descriptionTranslations))
        .then(() => this.$refs?.portletInstancePreview?.save())
        .then(() => {
          if (newInstance) {
            if (this.contentId === 'ide/WidgetPortlet') {
              return this.$portletInstanceService.getPortletInstance(this.instanceId)
                .then(instance => {
                  if (!instance?.preferences?.portletInstanceId) {
                    if (!instance.preferences) {
                      instance.preferences = [];
                    }
                    instance.preferences.push({
                      name: 'portletInstanceId',
                      value: this.instanceId,
                    });
                    savedInstance = instance;
                    return this.$portletInstanceService.updatePortletInstance(instance);
                  }
                })
                .then(() => this.$root.$emit('portlet-instance-created', savedInstance, true));
            } else {
              this.$root.$emit('portlet-instance-created', savedInstance);
            }
          } else {
            this.$root.$emit('portlet-instance-updated', savedInstance);
          }
        })
        .then(() => {
          this.$root.$emit('portlet-instance-saved', this.instanceId);
          this.$root.$emit('alert-message', message, 'success');
          this.close();
        })
        .finally(() => this.saving = false);
    },
    getPermissions() {
      const permissions = [this.aclSpaceHost && '*:/platform/users' || '*:/platform/administrators'];
      if (this.aclContributors) {
        permissions.push('*:/platform/web-contributors');
      }
      return permissions;
    },
    checkCKEdtiorDisplay() {
      if (this.$refs.portletInstanceDescriptionEditor?.editor
          && this.description !== this.$refs.portletInstanceDescriptionEditor.inputVal) {
        this.$refs.portletInstanceDescriptionEditor.editor.setData(this.description);
      }
    },
  },
};
</script>