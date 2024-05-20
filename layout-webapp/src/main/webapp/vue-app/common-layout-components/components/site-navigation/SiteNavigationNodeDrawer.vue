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
  <div>
    <exo-drawer
      id="siteNavigationAddNodeDrawer"
      ref="siteNavigationAddNodeDrawer"
      :loading="loading"
      right
      allow-expand
      @expand-updated="expanded = $event"
      @closed="close">
      <template slot="title">
        <div class="d-flex">
          <v-icon
            size="16"
            class="clickable"
            @click="close()">
            fas fa-arrow-left
          </v-icon>
          <span> {{ title }} </span>
        </div>
      </template>
      <template slot="content">
        <v-form
          v-model="isValidInputs">
          <v-card-text class="d-flex pb-2">
            <v-label>
              <span class="text-color font-weight-bold text-start text-truncate-2">
                {{ $t('siteNavigation.label.nodeLabel.title') }} *              
              </span>
              <p class="caption">{{ $t('siteNavigation.label.nodeLabel.description') }} </p>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex py-0">
            <v-text-field
              v-model="nodeLabel"
              class="pt-0"
              type="text"
              required="required"
              :rules="[nodeLabelRules.required]"
              outlined
              dense 
              @blur="blurOnNodeLabel">
              <template #append>
                <v-btn
                  class="mt-n2 pt-2px"
                  icon
                  @click="openTranslationDrawer">
                  <v-icon color="primary">fas fa-language</v-icon>
                </v-btn>
              </template>
            </v-text-field>
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 pb-2">
            <v-label>
              <span class="text-color font-weight-bold text-start mr-6 text-truncate-2">
                {{ $t('siteNavigation.label.nodeId.title') }} *              
              </span>
              <p
                v-if="nodeId && nodeId.length"
                class="caption text-break mx-auto text-wrap text-left">
                {{ nodeUrl }}
              </p>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex py-0">
            <v-text-field
              v-model="nodeId"
              class="pt-0"
              type="text"
              required="required"
              :rules="nodeIdRules"
              :disabled="disableNodeId"
              outlined
              dense />
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 pb-2">
            <v-label>
              <span class="text-color font-weight-bold text-start mr-6 text-truncate-2">
                {{ $t('siteNavigation.label.icon.title') }}             
              </span>
              <p class="caption"> {{ $t('siteNavigation.label.icon.description') }}  </p>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex py-0 pb-8">
            <div
              class="d-flex flex-grow-1 full-width">
              <div class="me-2 ms-auto">
                <v-tooltip bottom>
                  <template #activator="{ on, attrs }">
                    <v-btn
                      v-on="on"
                      v-bind="attrs"
                      id="changeIconButton"
                      class="light-black-background border-color"
                      icon
                      outlined
                      dark
                      small
                      @click="openNodeIconPickerDrawer">
                      <v-icon size="13">fas fa-file-image</v-icon>
                    </v-btn>
                  </template>
                  <span>{{ $t('siteNavigation.btn.changeIcon.title') }}</span>
                </v-tooltip>
              </div>
            </div>
          </v-card-text>
          <v-card-text class="flex-grow-1 align-center pb-8">
            <v-icon
              size="32"
              color="black">
              {{ icon }}
            </v-icon>
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 pb-2">
            <v-label>
              <span class="text-color font-weight-bold pt-2">
                {{ $t('siteNavigation.label.visibility.title') }} 
              </span>
            </v-label>
            <v-tooltip bottom>
              <template #activator="{ on, attrs }">
                <v-icon
                  v-on="on"
                  v-bind="attrs"
                  color="black"
                  size="24"
                  class="pl-2">
                  fa-info-circle
                </v-icon>
              </template>
              <span>{{ $t('siteNavigation.label.visibility.info') }}</span>
            </v-tooltip>
          </v-card-text>
          <v-card-text>
            <div class="d-flex flex-row">
              <v-switch      
                v-model="visible"
                class="mt-0 me-1" />
              <span class="caption pt-1">
                {{ $t('siteNavigation.label.visibility.visible') }}
              </span>
            </div>
            <div
              class="d-flex flex-row"
              v-if="visible">
              <v-switch   
                v-model="isScheduled" 
                class="mt-0 me-1" />
              <span class="caption pt-1">
                {{ $t('siteNavigation.label.visibility.scheduleVisibility') }}
              </span>
            </div>
          </v-card-text>
          <v-card-text class="pt-0" v-if="visible && isScheduled">
            <site-navigation-schedule-date-pickers
              :start-schedule-date="startScheduleDate"
              :end-schedule-date="endScheduleDate"
              :start-schedule-time="startScheduleTime"
              :end-schedule-time="endScheduleTime"
              @change="updateDates" />
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 text-no-wrap text-left font-weight-bold pb-2">
            <v-label>
              <span class="text-color font-weight-bold">
                {{ $t('siteNavigation.label.nodeType.title') }}
              </span>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex flex-row">
            <div class="d-flex flex-column">
              <v-radio-group
                v-model="nodeType"
                class="mt-0">
                <v-radio
                  :label="$t('siteNavigation.label.nodeType.group')"
                  value="Group" />
                <p class="caption text-light-color ms-8 me-2 mt-n2">
                  {{ $t('siteNavigation.label.nodeType.group.caption') }}
                </p>
                <div class="d-flex">
                  <v-radio
                    :label="$t('siteNavigation.label.nodeType.pageOrLink')"
                    value="pageOrLink" />
                  <a
                    v-if="this.navigationNode && this.navigationNode.pageKey && this.editMode"
                    class="mx-4"
                    @click="openAddElementDrawer">
                    <v-icon
                      class="pb-1"
                      size="13">
                      fas fa-edit
                    </v-icon>
                    {{ $t('siteNavigation.label.editElement') }}
                  </a>
                </div>
                <p class="caption text-light-color ms-8 me-2 text-wrap text-break text-truncate-2">
                  {{ $t('siteNavigation.label.nodeType.pageOrLink.caption') }}
                </p>
              </v-radio-group>
            </div>
          </v-card-text>
        </v-form>
      </template>
      <template slot="footer">
        <div class="d-flex justify-end">
          <v-btn
            class="btn ms-2"
            @click="close">
            {{ $t('siteNavigation.label.btn.cancel') }}
          </v-btn>
          <v-btn
            v-if="displayNextBtn"
            :disabled="disabled"
            :loading="loading"
            class="btn btn-primary ms-2"
            @click="openAddElementDrawer">
            {{ $t('siteNavigation.label.btn.next') }}
          </v-btn>
          <v-btn
            v-else
            :disabled="disabled"
            :loading="loading"
            @click="saveNode"
            class="btn btn-primary ms-2">
            {{ $t('siteNavigation.label.btn.save') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
    <translation-drawer
      ref="translationDrawer"
      v-model="valuesPerLanguage"
      :default-language="defaultLanguage"
      :supported-languages="supportedLanguages"
      @input="updateNodeLabels" />
    <node-icon-picker-drawer 
      :expanded="expanded" />
  </div>
</template>
<script>
export default {
  data () {
    return {
      expanded: false,
      labels: null,
      supportedLanguages: {},
      valuesPerLanguage: {},
      defaultLanguage: '',
      startScheduleDate: new Date().getTime(),
      endScheduleDate: new Date().getTime(),
      startScheduleTime: new Date(new Date().getTime() + 900000),
      endScheduleTime: new Date(new Date().getTime() + 1800000),
      navigationNode: null,
      loading: false,
      nodeLabel: null,
      nodeId: null,
      visible: true,
      isScheduled: false,
      disableNodeId: false,
      nodeType: 'Group',
      parentNavigationNodeUrl: '',
      editMode: false,
      nodeIcon: null,
      nodeLabelRules: {
        required: value => value == null || !!(value?.length) || this.$t('siteNavigation.required.error.message'),
      },
      isValidInputs: true,
      nodeIdRules: [
        value => {
          const isNodeExisting = this.navigationNode?.children?.find?.(node => node.name === value);
          if (value != null && (/\s+/.test(value) || /[^a-zA-Z0-9_-]/.test(value) || /[\u0300-\u036f]/.test(value.normalize('NFD')))){
            return this.$t('siteNavigation.unauthorizedCharacters.error.message');
          } else if (isNodeExisting) {
            return this.$t('siteNavigation.nodeWithSameNodeIdAlreadyExists.error.message');
          } else {
            return value == null || !!(value?.length) || this.$t('siteNavigation.required.error.message');
          }
        }
      ],
    };
  },
  computed: {
    icon() {
      return this.nodeIcon ? this.nodeIcon : 'fas fa-folder';
    },
    title() {
      return this.editMode ? this.$t('siteNavigation.drawer.editNode.title') : this.$t('siteNavigation.drawer.addNode.title');
    },
    disabled() {
      return !(this.isValidInputs && this.nodeId && this.nodeLabel);
    },
    displayNextBtn() {
      return this.editMode ? this.nodeType === 'pageOrLink' && !this.navigationNode.pageKey : this.nodeType === 'pageOrLink';
    },
    nodeUrl() {
      const nodeuri = `${this.$t('siteNavigation.label.nodeId.description')}${this.parentNavigationNodeUrl}`;
      return this.editMode ? nodeuri : `${nodeuri}/${this.nodeId}` ;
    }
  },
  created() {
    this.$root.$on('open-site-navigation-add-node-drawer', this.open);
    this.$root.$on('open-site-navigation-edit-node-drawer', (navigationNode) => {
      this.editMode = true;
      this.open(navigationNode);
    });
    this.$root.$on('save-node-with-page', this.saveNode);
    this.$root.$on('update-node-icon', (icon) => {
      this.nodeIcon = icon;
    });
  },
  methods: {
    updateDates(startDate, endDate, startTime, endTime) {
      this.startScheduleDate = startDate;
      this.endScheduleDate = endDate;
      this.startScheduleTime = startTime;
      this.endScheduleTime = endTime;
    },
    open(parentNavigationNode) {
      this.navigationNode = parentNavigationNode;
      const siteKey = parentNavigationNode.siteKey;
      this.getNodeLabels();
      if (siteKey.typeName === 'portal') {
        this.parentNavigationNodeUrl = `/portal/${siteKey.name}/${parentNavigationNode.uri}`;
      } else {
        this.parentNavigationNodeUrl = `/portal/g/${siteKey.name.replaceAll('/', ':')}/${parentNavigationNode.uri}`;
      }
      if (this.editMode) {
        this.nodeLabel = parentNavigationNode.label;
        this.nodeId = parentNavigationNode.name;
        this.nodeType = parentNavigationNode.pageKey ? 'pageOrLink' : 'Group';
        this.visible = parentNavigationNode.visibility !== 'HIDDEN';
        this.nodeIcon = parentNavigationNode.icon;
        this.disableNodeId = true;
        if (parentNavigationNode.visibility === 'TEMPORAL') {
          this.isScheduled = true;
          this.startScheduleDate = parentNavigationNode.startPublicationTime;
          this.endScheduleDate = parentNavigationNode.endPublicationTime;
          this.startScheduleTime = new Date(parentNavigationNode.startPublicationTime);
          this.endScheduleTime = new Date(parentNavigationNode.endPublicationTime);
        }
      }
      this.$nextTick().then(() => this.$refs.siteNavigationAddNodeDrawer.open());
    },
    close() {
      this.nodeId = null;
      this.nodeLabel = null;
      this.visible = true;
      this.isScheduled = false;
      this.nodeType = 'Group';
      this.disableNodeId = false;
      this.editMode= false;
      this.startScheduleDate = new Date().getTime();
      this.endScheduleDate = new Date().getTime();
      this.startScheduleTime = new Date(new Date().getTime() + 900000);
      this.endScheduleTime = new Date(new Date().getTime() + 1800000);
      this.valuesPerLanguage = {};
      this.supportedLanguages = {};
      this.labels = null;
      this.nodeIcon = null;
      this.$refs.siteNavigationAddNodeDrawer.close();
    },
    saveNode(pageData) {
      let startScheduleDate = null;
      let endScheduleDate = null;
      if (this.isScheduled) {
        startScheduleDate = new Date(this.startScheduleDate);
        startScheduleDate.setHours(new Date(this.startScheduleTime).getHours());
        startScheduleDate.setMinutes(new Date(this.startScheduleTime).getMinutes());
        startScheduleDate.setSeconds(0);
        endScheduleDate = new Date(this.endScheduleDate);
        endScheduleDate.setHours(new Date(this.endScheduleTime).getHours());
        endScheduleDate.setMinutes(new Date(this.endScheduleTime).getMinutes());
        endScheduleDate.setSeconds(0);
      }
      const nodeChildrenLength = this.navigationNode.children?.length;
      const previousNodeId = nodeChildrenLength ? this.navigationNode.children[nodeChildrenLength -1].id : null;
      if (this.labels == null) {
        this.labels = {
          'en': this.nodeLabel,
        };
        if (this.defaultLanguage !== eXo.env.portal.language) {
          this.labels[eXo.env.portal.language] = this.nodeLabel;
        }
      }
      this.labels[eXo.env.portal.language] = this.nodeLabel;
      const nodeLabels = {
        labels: this.labels
      };
      if (this.editMode) {
        const pageRef = pageData?.pageRef ||  (this.nodeType === 'pageOrLink' ? this.navigationNode.pageKey?.ref || `${ this.navigationNode.pageKey.site.typeName}::${ this.navigationNode.pageKey.site.name}::${this.navigationNode.pageKey?.name}` : '');
        this.loading = true;
        this.$navigationLayoutService.updateNode(this.navigationNode.id, this.nodeLabel, pageRef, this.visible, this.isScheduled, startScheduleDate, endScheduleDate, nodeLabels?.labels, pageData?.nodeTarget || this.navigationNode.target, this.nodeIcon)
          .then(() => {
            this.openTargetPage(pageData, this.navigationNode.id);
            this.$root.$emit('refresh-navigation-nodes');
            this.$root.$emit('close-add-element-drawer');
            this.close();
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('siteNavigation.errorUpdatingNode'), 'error'))
          .finally(() => this.loading = false);
      } else {
        this.loading = true;
        this.$navigationLayoutService.createNode(this.navigationNode.id, previousNodeId, this.nodeLabel, this.nodeId, this.nodeIcon, this.visible, this.isScheduled, startScheduleDate, endScheduleDate, nodeLabels?.labels, pageData?.pageRef, pageData?.pageRef && pageData?.nodeTarget || 'SAME_TAB')
          .then(createdNode => {
            this.openTargetPage(pageData, createdNode.id);
            this.$root.$emit('refresh-navigation-nodes');
            this.$root.$emit('close-add-element-drawer');
            this.close();
          })
          .catch(() => this.$root.$emit('alert-message', this.$t('siteNavigation.errorCreatingNode'), 'error'))
          .finally(() => this.loading = false);
      }
    },
    openAddElementDrawer() {
      this.$root.$emit('open-add-element-drawer', this.nodeId, this.valuesPerLanguage['en'] || this.nodeLabel,  this.navigationNode, this.navigationNode?.pageKey && this.editMode || false);
    },
    conversionRules() {
      return this.nodeLabel.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]/g, '').replace(/\s+/g, '').toLowerCase();
    },
    blurOnNodeLabel() {
      if (this.nodeId == null) {
        this.nodeId = this.conversionRules();
      }
    },
    openTranslationDrawer() {
      this.valuesPerLanguage[eXo.env.portal.language] = this.nodeLabel;
      if (this.defaultLanguage !== eXo.env.portal.language && this.valuesPerLanguage[this.defaultLanguage] == null) {
        this.valuesPerLanguage[this.defaultLanguage] = this.nodeLabel;
      }
      this.$refs.translationDrawer.open();
    },
    getNodeLabels() {
      this.$navigationLayoutService.getNodeLabels(this.navigationNode.id)
        .then(data => {
          if (this.editMode && data.labels != null) {
            this.valuesPerLanguage = data.labels;
          } else {
            this.valuesPerLanguage = {
              'en': null,
            };
          }
          this.defaultLanguage = data.defaultLanguage;
          this.supportedLanguages = data.supportedLanguages;
        });
    },
    updateNodeLabels(translations) {
      this.valuesPerLanguage = translations;
      if (this.valuesPerLanguage[this.defaultLanguage] === '') {
        this.valuesPerLanguage[this.defaultLanguage] = this.nodeLabel;
      }
      this.labels = this.valuesPerLanguage;
      this.nodeLabel = this.valuesPerLanguage[eXo.env.portal.language];
    },
    openTargetPage(pageData, nodeId) {
      if (pageData?.pageRef) {
        if (pageData?.pageType === 'PAGE' && pageData?.pageRef && pageData?.openEditLayout) {
          return this.$pageLayoutService.editPageLayout(nodeId || this.nodeId, pageData?.pageRef);
        } else {
          let targetPageUrl ;
          if (pageData?.pageType === 'LINK' ) {
            targetPageUrl =  this.urlVerify(pageData?.createdPage?.state?.link) ;
          } else {
            targetPageUrl = `/portal${this.navigationNode.siteKey.type === 'GROUP' ? '/g' : ''}/${this.navigationNode.siteKey.name.replaceAll('/', ':')}/${this.navigationNode.uri}`;
            targetPageUrl =  !this.editMode && `${targetPageUrl}/${this.nodeId}` || targetPageUrl;
          }
          window.open(targetPageUrl, pageData?.nodeTarget === 'SAME_TAB' && '_self' || '_blank');
        }
      }
    },
    urlVerify(url) {
      if (!url.match(/^(https?:\/\/|javascript:|\/portal\/)/)) {
        url = `//${url}`;
      }
      return url ;
    },
    openNodeIconPickerDrawer() {
      this.$root.$emit('open-node-icon-picker-drawer');
    }
  }
};
</script>