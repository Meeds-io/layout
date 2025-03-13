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
    id="portletInstanceDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    :go-back-button="goBackButton"
    :loading="saving"
    right>
    <template #title>
      <span class="text-wrap">{{ isNew && $t('layout.portletInstance.drawerTitie.add') || $t('layout.portletInstance.drawerTitie.edit') }}</span>
    </template>
    <template v-if="drawer" #content>
      <div class="pa-4" flat>
        <translation-text-field
          id="pageTemplateTitle"
          ref="title"
          v-model="titleTranslations"
          v-model:field-value="title"
          back-icon
          class="width-auto flex-grow-1 pb-1"
          drawer-title="layout.portletInstance.nameTranslationDrawerTitle"
          field-name="title"
          :maxlength="maxTitleLength"
          :object-id="instanceId"
          object-type="portletInstance"
          :placeholder="$t('layout.portletInstance.namePlaceholder')"
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
          v-model:field-value="description"
          back-icon
          class="ma-1px mt-4"
          drawer-title="layout.portletInstance.descriptionTranslationDrawerTitle"
          field-name="description"
          :maxlength="maxDescriptionLength"
          :object-id="instanceId"
          object-type="portletInstance"
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
            ck-editor-type="portletInstanceDescription"
            disable-suggester
            :max-length="maxDescriptionLength"
            :placeholder="$t('layout.portletInstance.descriptionTranslationDrawerTitle')"
            :tag-enabled="false"
            @ready="checkCKEdtiorDisplay" />
        </translation-text-field>
        <portlets-instance-category-input
          v-model="categoryId"
          class="mt-4" />
        <portlets-instance-portlet-input
          v-model="contentId"
          class="mt-4"
          :disabled="!isNew || disableSelectedPortlet" />
        <div class="d-flex flex-column mt-4">
          <div class="mb-2">{{ $t('portlets.selectWhoCanAddIt') }}</div>
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <div
                v-bind="attrs"
                v-on="on">
                <v-checkbox
                  v-model="aclAdministrators"
                  :aria-label="$t('portlets.administrators')"
                  class="ma-0 pa-0"
                  disabled
                  :label="$t('portlets.administrators')" />
              </div>
            </template>
            <span>{{ $t('portlets.administratorsMandatorySelectionTooltip') }}</span>
          </v-tooltip>
          <v-checkbox
            v-model="aclContributors"
            :aria-label="$t('portlets.contentManagers')"
            class="ma-0 pa-0"
            :label="$t('portlets.contentManagers')" />
          <v-checkbox
            v-model="aclSpaceHost"
            :aria-label="$t('portlets.spaceHost')"
            class="ma-0 pa-0"
            :label="$t('portlets.spaceHost')" />
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
          class="btn btn-primary"
          :disabled="disabled"
          :loading="saving"
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
      disabled () {
        return !this.categoryId || !this.contentId || !this.title;
      },
    },
    watch: {
      aclContributors () {
        if (!this.aclContributors && this.aclSpaceHost) {
          this.aclSpaceHost = false;
        }
      },
      aclSpaceHost () {
        if (this.aclSpaceHost && !this.aclContributors) {
          this.aclContributors = true;
        }
      },
      description () {
        if (this.$refs.descriptionTranslation) {
          this.$refs.descriptionTranslation.setValue(this.description);
        }
        this.checkCKEdtiorDisplay();
      },
    },
    created () {
      this.$root.$on('portlet-instance-add', this.open);
      this.$root.$on('portlet-instance-edit', this.open);
    },
    beforeUnmount () {
      this.$root.$off('portlet-instance-add', this.open);
      this.$root.$off('portlet-instance-edit', this.open);
    },
    methods: {
      open (instance, goBackButton, contentId) {
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
      close () {
        this.$refs.drawer.close();
      },
      save () {
        this.saving = true;
        let message = null;
        let savedInstance = null;
        const newInstance = !this.instanceId;
        const saveInstanceRequest =
          !newInstance ?
            eXo.$portletInstanceService.getPortletInstance(this.instanceId)
              .then(instance => {
                instance.categoryId = this.categoryId;
                instance.permissions = this.getPermissions();
                return eXo.$portletInstanceService.updatePortletInstance(instance)
                  .then(() => {
                    message = this.$t('layout.portletInstanceUpdatedSuccessfully');
                    savedInstance = instance;
                    return instance;
                  });
              })
            : eXo.$portletInstanceService.createPortletInstance({
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
          .then(() => eXo.$translationService.saveTranslations('portletInstance', this.instanceId, 'title', this.titleTranslations))
          .then(() => eXo.$translationService.saveTranslations('portletInstance', this.instanceId, 'description', this.descriptionTranslations))
          .then(() => this.$refs?.portletInstancePreview?.save())
          .then(() => {
            if (newInstance) {
              if (this.contentId === 'ide/WidgetPortlet') {
                return eXo.$portletInstanceService.getPortletInstance(this.instanceId)
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
                      return eXo.$portletInstanceService.updatePortletInstance(instance);
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
      getPermissions () {
        const permissions = [this.aclSpaceHost && '*:/platform/users' || '*:/platform/administrators'];
        if (this.aclContributors) {
          permissions.push('*:/platform/web-contributors');
        }
        return permissions;
      },
      checkCKEdtiorDisplay () {
        if (this.$refs.portletInstanceDescriptionEditor?.editor
          && this.description !== this.$refs.portletInstanceDescriptionEditor.inputVal) {
          this.$refs.portletInstanceDescriptionEditor.editor.setData(this.description);
        }
      },
    },
  };
</script>