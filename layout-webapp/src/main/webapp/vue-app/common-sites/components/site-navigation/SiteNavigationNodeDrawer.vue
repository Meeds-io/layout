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
      v-model="drawer"
      :loading="loading"
      go-back-button
      allow-expand
      right
      @expand-updated="expanded = $event"
      @closed="close">
      <template #title>{{ title }}</template>
      <template v-if="drawer" #content>
        <v-form v-model="isValidInputs">
          <v-card-text
            class="d-flex pb-2">
            <span class="font-weight-bold text-start text-truncate-2">
              {{ $t('siteNavigation.label.nodeLabel.title') }}
            </span>
          </v-card-text>
          <v-card-text class="d-flex py-0">
            <v-text-field
              v-model="nodeLabel"
              class="pt-0"
              type="text"
              required="required"
              :rules="[nodeLabelRules.required]"
              :placeholder="$t('siteNavigation.label.nodeLabel.placeholder')"
              outlined
              dense
              autofocus
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
          <v-card-text class="py-2">
            <div class="d-flex align-center justify-space-between">
              <div>
                <span class="font-weight-bold text-start mr-6 text-truncate-2">
                  {{ $t('siteNavigation.label.nodeId.title') }}
                </span>
              </div>
              <v-switch v-model="displayNodeName" class="mt-0 me-0" />
            </div>
          </v-card-text>
          <v-card-text v-if="displayNodeName" class="d-flex pt-0">
            <v-text-field
              v-model="nodeId"
              class="pt-0"
              type="text"
              required="required"
              :rules="nodeIdRules"
              :disabled="disableNodeId"
              :placeholder="$t('siteNavigation.label.nodeId.placeholder')"
              outlined
              dense />
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 text-no-wrap text-left font-weight-bold pb-2">
            <v-label>
              <span class="font-weight-bold">
                {{ $t('siteNavigation.label.nodeType.title') }}
              </span>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex flex-row pt-0">
            <div class="d-flex flex-column flex-grow-1">
              <v-radio-group v-model="elementType" class="mt-0">
                <v-radio
                  :label="$t('siteNavigation.label.newPage')"
                  value="PAGE" />
                <v-radio
                  :label="$t('siteNavigation.label.existingPage')"
                  value="existingPage" />
                <template v-if="elementType === 'existingPage'">
                  <site-navigation-existing-page-element
                    v-if="!isNewPageElement"
                    :selected-page="selectedPage" />
                  <div class="d-flex align-center justify-space-between flex-row ms-8 pb-2 mt-2">
                    <span>{{ $t('siteNavigation.label.openSameTab') }}</span>
                    <v-switch
                      v-model="nodeTarget"
                      true-value="SAME_TAB"
                      false-value="NEW_TAB"
                      class="mt-0 me-0" />
                  </div>
                </template>
                <v-radio :label="$t('siteNavigation.label.link')" value="LINK" />
                <template v-if="isLinkElement">
                  <v-text-field
                    v-model="link"
                    :placeholder="$t('siteNavigation.label.enterUrl') "
                    :rules="linkRules"
                    class="pt-0 mb-0 ms-8"
                    type="text"
                    required
                    outlined
                    dense />
                  <div class="d-flex align-center justify-space-between flex-row pb-2 ms-8">
                    <span>{{ $t('siteNavigation.label.openSameTab') }}</span>
                    <v-switch
                      v-model="nodeTarget"
                      true-value="SAME_TAB"
                      false-value="NEW_TAB"
                      class="mt-0 me-0" />
                  </div>
                </template>
                <v-radio
                  :label="$t('siteNavigation.label.nodeType.group')"
                  value="Group" />
              </v-radio-group>
            </div>
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 pb-2 pt-0">
            <v-label>
              <span class="font-weight-bold text-start mr-6 text-truncate-2">
                {{ $t('siteNavigation.label.icon.title') }}
              </span>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex pt-2">
            <div class="d-flex flex-grow-1 full-width">
              <site-navigation-icon-input
                ref="nodeIcon"
                v-model="nodeIcon"
                :site-id="siteId" />
            </div>
          </v-card-text>
          <v-card-text class="d-flex flex-grow-1 pb-0">
            <span class="font-weight-bold pt-2">
              {{ $t('siteNavigation.label.visibility.title') }}
            </span>
          </v-card-text> <v-card-text
            class="pt-2">
            <div class="d-flex align-center justify-space-between flex-row">
              <span>
                {{ $t('siteNavigation.label.visibility.visible') }}
              </span>
              <v-switch v-model="visible" class="mt-0 me-0" />
            </div>
            <div
              v-if="visible"
              class="d-flex align-center justify-space-between flex-row">
              <span class="pt-1">
                {{ $t('siteNavigation.label.visibility.scheduleVisibility') }}
              </span>
              <v-switch v-model="isScheduled" class="mt-0 me-0" />
            </div>
          </v-card-text>
          <v-card-text
            v-if="visible && isScheduled"
            class="pt-0">
            <site-navigation-schedule-date-pickers
              :start-schedule-date="startScheduleDate"
              :end-schedule-date="endScheduleDate"
              :start-schedule-time="startScheduleTime"
              :end-schedule-time="endScheduleTime"
              @change="updateDates" />
          </v-card-text>
        </v-form>
      </template>
      <template slot="footer">
        <div class="d-flex justify-end">
          <v-btn class="btn ms-2" @click="close">
            {{ $t('siteNavigation.label.btn.cancel') }}
          </v-btn>
          <v-btn
            v-if="isNewPageElement"
            :disabled="disableNextBtn"
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
      drawer: false,
      loading: false,
      nodeLabel: null,
      nodeId: null,
      visible: true,
      isScheduled: false,
      disableNodeId: false,
      displayNodeName: false,
      elementType: 'PAGE',
      siteId: null,
      allSites: true,
      nodeTarget: 'SAME_TAB',
      parentNavigationNodeUrl: '',
      editMode: false,
      nodeIcon: null,
      selectedPage: null,
      pageToEdit: false,
      link: '',
      linkRules: [url => !!(url?.match(/^((https?:\/\/)?(www\.)?[a-zA-Z0-9:._\\/+=-]+\.[^\s]{2,})|(javascript:)|(\/portal)/))
          || ( !url?.length && this.$t('siteNavigation.required.error.message') || this.$t('siteNavigation.label.invalidLink'))],
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
      return this.nodeIcon ? this.nodeIcon : 'fas fa-project-diagram';
    },
    title() {
      return this.editMode ? this.$t('siteNavigation.drawer.editNode.title') : this.$t('siteNavigation.drawer.addNode.title');
    },
    disabled() {
      return !(this.isValidInputs && this.nodeId && this.nodeLabel) || this.isLinkElement && !this.link;
    },
    disableNextBtn() {
      return !(this.elementType === 'PAGE' && this.nodeLabel?.length);
    },
    isLinkElement() {
      return this.elementType === 'LINK';
    },
    isNewPageElement() {
      return this.elementType === 'PAGE';
    },
  },
  created() {
    this.$root.$on('open-site-navigation-add-node-drawer', this.open);
    this.$root.$on('open-site-navigation-edit-node-drawer', (navigationNode) => {
      this.editMode = true;
      this.open(navigationNode);
    });
    this.$root.$on('save-node-with-page', this.saveNode);
    this.$root.$on('existing-page-selected', this.changeSelectedPage);
  },
  methods: {
    updateDates(startDate, endDate, startTime, endTime) {
      this.startScheduleDate = startDate;
      this.endScheduleDate = endDate;
      this.startScheduleTime = startTime;
      this.endScheduleTime = endTime;
    },
    async open(parentNavigationNode) {
      this.navigationNode = parentNavigationNode;
      const siteKey = parentNavigationNode.siteKey;
      const site = await this.$siteService.getSite(siteKey.type, siteKey.name);
      this.siteId = site?.siteId;
      await this.getNodeLabels();
      if (siteKey.typeName === 'portal') {
        this.parentNavigationNodeUrl = `/portal/${siteKey.name}/${parentNavigationNode.uri}`;
      } else {
        this.parentNavigationNodeUrl = `/portal/g/${siteKey.name.replaceAll('/', ':')}/${parentNavigationNode.uri}`;
      }
      if (this.editMode) {
        this.nodeLabel = parentNavigationNode.label;
        this.nodeId = parentNavigationNode.name;
        this.nodeTarget = parentNavigationNode.target;
        if (this.navigationNode?.pageKey) {
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
              this.$nextTick()
                .then(() => {
                  this.$root.$emit('set-selected-page', page);
                });
            });
        } else {
          this.elementType = 'Group';
        }
        this.elementType = parentNavigationNode.pageKey ? 'pageOrLink' : 'Group';
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
      this.displayNodeName = false;
      this.elementType = 'PAGE';
      this.nodeTarget = 'SAME_TAB';
      this.disableNodeId = false;
      this.editMode= false;
      this.pageToEdit = null;
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
    async saveNode(pageData) {
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
      this.loading = true;
      if (this.$refs?.nodeIcon) {
        try {
          await this.$refs.nodeIcon?.save?.();
        } catch (e) {
          this.loading = false;
          this.$root.$emit('alert-message', this.$t('siteNavigation.errorUpdatingNode'), 'error');
          throw e;
        }
      }
      if (this.editMode) {
        const pageRef = pageData?.pageRef ||  (this.elementType !== 'Group' ? this.navigationNode.pageKey?.ref || `${ this.navigationNode.pageKey.site.typeName}::${ this.navigationNode.pageKey.site.name}::${this.navigationNode.pageKey?.name}` : '');
        if (this.elementType === 'existingPage') {
          const pageRef = this.selectedPage?.pageRef;
          pageData = {
            'pageRef': pageRef,
            'nodeTarget': this.nodeTarget,
            'pageType': this.elementType
          };
          this.updateNode(pageData, pageRef, startScheduleDate, endScheduleDate, nodeLabels);
        } else if (this.elementType === 'LINK') {
          if (this.pageToEdit?.type === 'LINK') {
            this.updatePageLink(startScheduleDate, endScheduleDate, nodeLabels);
          } else {
            this.$pageLayoutService.createPage(
              this.nodeId,
              this.valuesPerLanguage['en'] || this.nodeLabel,
              this.navigationNode.siteKey.name,
              this.navigationNode.siteKey.type,
              this.elementType, this.elementType === 'LINK' && this.link || null,
            ).then((createdPage) => {
              const pageRef = createdPage?.key?.ref || `${createdPage?.key.site.typeName}::${createdPage?.key.site.name}::${createdPage?.pageContext?.key.name}`;
              pageData = {
                'pageRef': pageRef,
                'nodeTarget': this.nodeTarget,
                'pageType': this.elementType,
                'createdPage': createdPage,
              };
              this.updateNode(pageData,pageRef, startScheduleDate, endScheduleDate, nodeLabels);
            }).then(page => {
              this.$root.$emit('page-layout-created', page, this.pageTemplate);
            }).catch(() => {
              this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageCreation.error'), 'error');
            });
          }
        } else {
          this.updateNode(pageData, pageRef, startScheduleDate, endScheduleDate, nodeLabels);
        }
      } else {
        if (this.elementType === 'existingPage') {
          const pageRef = this.selectedPage?.pageRef;
          pageData = {
            'pageRef': pageRef,
            'nodeTarget': this.nodeTarget,
            'pageType': this.elementType
          };
          this.createNode(previousNodeId, pageData, startScheduleDate, endScheduleDate, nodeLabels);
        } else if (this.elementType === 'LINK') {
          this.$pageLayoutService.createPage(
            this.nodeId,
            this.valuesPerLanguage['en'] || this.nodeLabel,
            this.navigationNode.siteKey.name,
            this.navigationNode.siteKey.type,
            this.elementType, this.elementType === 'LINK' && this.link || null,
          ).then((createdPage) => {
            const pageRef = createdPage?.key?.ref || `${createdPage?.key.site.typeName}::${createdPage?.key.site.name}::${createdPage?.pageContext?.key.name}`;
            pageData = {
              'pageRef': pageRef,
              'nodeTarget': this.nodeTarget,
              'pageType': this.elementType,
              'createdPage': createdPage,
            };
            this.createNode(previousNodeId, pageData, startScheduleDate, endScheduleDate, nodeLabels);
          }).then(page => {
            this.$root.$emit('page-layout-created', page, this.pageTemplate);
          }).catch(() => {
            this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageCreation.error'), 'error');
          });
        } else {
          this.createNode(previousNodeId, pageData, startScheduleDate, endScheduleDate, nodeLabels);
        }
      }
    },
    createNode(previousNodeId, pageData, startScheduleDate, endScheduleDate, nodeLabels) {
      this.$navigationLayoutService.createNode(this.navigationNode.id, previousNodeId, this.nodeLabel, this.nodeId, this.nodeIcon, this.visible, this.isScheduled, startScheduleDate, endScheduleDate, nodeLabels?.labels, pageData?.pageRef, pageData?.pageRef && pageData?.nodeTarget || 'SAME_TAB')
        .then(createdNode => {
          this.openTargetPage(pageData, createdNode.id);
          this.$root.$emit('refresh-navigation-nodes');
          this.$root.$emit('close-add-element-drawer');
          this.close();
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('siteNavigation.errorCreatingNode'), 'error'))
        .finally(() => this.loading = false);
    },
    updateNode(pageData, pageRef, startScheduleDate, endScheduleDate, nodeLabels) {
      this.$navigationLayoutService.updateNode(this.navigationNode.id, this.nodeLabel, pageRef, this.visible, this.isScheduled, startScheduleDate, endScheduleDate, nodeLabels?.labels, pageData?.nodeTarget || this.navigationNode.target, this.nodeIcon)
        .then(() => {
          this.$root.$emit('refresh-navigation-nodes');
          this.$root.$emit('close-add-element-drawer');
          this.close();
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('siteNavigation.errorUpdatingNode'), 'error'))
        .finally(() => this.loading = false);
    },
    updatePageLink(startScheduleDate, endScheduleDate, nodeLabels) {
      const pageRef = this.pageToEdit?.key?.ref || `${this.pageToEdit?.key.site.typeName}::${this.pageToEdit?.key.site.name}::${this.pageToEdit?.key.name}`;
      this.$pageLayoutService.updatePageLink(pageRef, this.link)
        .then(() => {
          const pageData = {
            'pageRef': pageRef,
            'nodeTarget': this.nodeTarget,
            'pageType': this.elementType
          };
          this.updateNode(pageData, pageRef, startScheduleDate, endScheduleDate, nodeLabels);
        }).catch(() => {
          this.$root.$emit('alert-message', this.$t('siteNavigation.label.pageUpdate.error'), 'error');
        });
    },
    openAddElementDrawer() {
      this.$root.$emit('open-add-element-drawer', this.nodeId, this.valuesPerLanguage['en'] || this.nodeLabel,  this.navigationNode, this.navigationNode?.pageKey && this.editMode || false);
    },
    changeSelectedPage(selectedPage) {
      this.selectedPage = selectedPage;
    },
    blurOnNodeLabel() {
      if (this.nodeId == null) {
        this.nodeId = this.nodeLabel && this.nodeLabel.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9_-]/g, '').replace(/\s+/g, '').toLowerCase();
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
      return this.$navigationLayoutService.getNodeLabels(this.navigationNode.id)
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
        if (pageData?.pageType === 'PAGE' && pageData?.pageRef) {
          return this.$pageLayoutService.editPageLayout(nodeId || this.nodeId, pageData?.pageRef);
        } else if (pageData?.pageType === 'LINK' ) {
          const targetPageUrl =  this.urlVerify(pageData?.createdPage?.state?.link) ;
          window.open(targetPageUrl, pageData?.nodeTarget === 'SAME_TAB' && '_self' || '_blank');
        }
      }
    },
    urlVerify(url) {
      if (!url.match(/^(https?:\/\/|javascript:|\/portal)/)) {
        url = `//${url}`;
      }
      return url ;
    },
  }
};
</script>
