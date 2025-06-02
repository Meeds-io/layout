<template>
  <component
    v-model="menu"
    :is="$root.isMobile && 'v-bottom-sheet' || 'v-menu'"
    :left="!$vuetify.rtl"
    :right="$vuetify.rtl"
    :content-class="menuId"
    offset-y>
    <template #activator="{ on, attrs }">
      <v-btn
        :aria-label="$t('pageTemplates.menu.open')"
        icon
        small
        class="mx-auto"
        v-bind="attrs"
        v-on="on">
        <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-hover v-if="menu" @input="hoverMenu = $event">
      <v-list
        class="pa-0"
        dense
        @mouseout="menu = false"
        @focusout="menu = false">
        <v-subheader v-if="$root.isMobile">
          <div class="d-flex full-width">
            <div class="d-flex flex-grow-1 flex-shrink-1 align-center subtitle-1 text-truncate">
              {{ $t('pageTemplate.label.templateMenu', {0: name}) }}
            </div>
            <div class="flex-shrink-0">
              <v-btn
                :aria-label="$t('pageTemplate.label.closeMenu')"
                icon
                @click="menu = false">
                <v-icon>fa-times</v-icon>
              </v-btn>
            </div>
          </div>
        </v-subheader>
        <v-list-item-group v-model="listItem">
          <v-list-item
            v-if="!$root.isMobile"
            :href="editLayoutLink"
            target="_blank"
            rel="opener"
            dense>
            <v-card
              color="transparent"
              min-width="15"
              class="me-2"
              flat>
              <v-icon size="13">
                fa-columns
              </v-icon>
            </v-card>
            <v-list-item-title>
              {{ $t('pageTemplate.label.editLayout') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('layout-page-template-drawer-open', pageTemplate)">
            <v-card
              color="transparent"
              min-width="15"
              class="me-2"
              flat>
              <v-icon size="13">
                fa-edit
              </v-icon>
            </v-card>
            <v-list-item-title>
              {{ $t('pageTemplate.label.editProperties') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('layout-page-template-drawer-open', pageTemplate, true)">
            <v-card
              color="transparent"
              min-width="15"
              class="me-2"
              flat>
              <v-icon size="13">
                fa-copy
              </v-icon>
            </v-card>
            <v-list-item-title>
              {{ $t('pageTemplate.label.duplicate') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('serialize-drawer-open', 'PageTemplate', pageTemplateId)">
            <v-list-item-icon class="me-auto">
              <v-icon size="13">fa-download</v-icon>
            </v-list-item-icon>
            <v-list-item-title>
              {{ $t('pageTemplate.label.export') }}
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            dense
            @click="$root.$emit('page-templates-restore', pageTemplateId)">
            <v-list-item-icon class="me-auto">
              <v-icon size="13">fa-redo</v-icon>
            </v-list-item-icon>
            <v-list-item-title>
              {{ $t('pageTemplate.label.restore') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip :disabled="!pageTemplate.system" bottom>
            <template #activator="{ on, attrs }">
              <div
                v-on="on"
                v-bind="attrs">
                <v-list-item
                  :disabled="pageTemplate.system"
                  dense
                  @click="$root.$emit('page-templates-delete', pageTemplate)">
                  <v-card
                    color="transparent"
                    min-width="15"
                    class="me-2"
                    flat>
                    <v-icon
                      :class="!pageTemplate.system && 'error--text' || 'disabled--text'"
                      size="13">
                      fa-trash
                    </v-icon>
                  </v-card>
                  <v-list-item-title>
                    <span :class="!pageTemplate.system && 'error--text' || 'disabled--text'">{{ $t('pageTemplate.label.delete') }}</span>
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('pageTemplate.label.system.noDelete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </component>
</template>
<script>
export default {
  props: {
    pageTemplate: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    menu: false,
    hoverMenu: false,
    listItem: null,
    menuId: `PageTemplateMenu${parseInt(Math.random() * 10000)}`,
  }),
  computed: {
    pageTemplateId() {
      return this.pageTemplate?.id;
    },
    name() {
      return this.$te(this.pageTemplate?.name) ? this.$t(this.pageTemplate?.name) : this.pageTemplate?.name;
    },
    editLayoutLink() {
      return `/portal/administration/layout-editor?pageTemplateId=${this.pageTemplateId}`;
    },
  },
  watch: {
    listItem() {
      if (this.menu) {
        this.menu = false;
        this.listItem = null;
      }
    },
    menu() {
      if (this.menu) {
        this.$root.$emit('page-management-menu-opened', this.pageTemplateId);
      } else {
        this.$root.$emit('page-management-menu-closed', this.pageTemplateId);
      }
    },
    hoverMenu() {
      if (!this.hoverMenu) {
        window.setTimeout(() => {
          if (!this.hoverMenu) {
            this.menu = false;
          }
        }, 200);
      }
    },
  },
  created() {
    this.$root.$on('page-management-menu-opened', this.checkMenuStatus);
    document.addEventListener('click', this.closeMenuOnClick);
  },
  beforeDestroy() {
    this.$root.$off('page-management-menu-opened', this.checkMenuStatus);
    document.removeEventListener('click', this.closeMenuOnClick);
  },
  methods: {
    closeMenuOnClick(e) {
      if (e.target && !e.target.closest(`.${this.menuId}`)) {
        this.menu = false;
      }
    },
    checkMenuStatus(templateId) {
      if (this.menu && templateId !== this.pageTemplate.id) {
        this.menu = false;
      }
    },
  },
};
</script>