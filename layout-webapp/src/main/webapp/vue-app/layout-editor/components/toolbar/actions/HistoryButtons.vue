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
  <div class="d-flex align-content justify-content">
    <v-tooltip bottom>
      <template #activator="{on, attrs}">
        <div v-bind="attrs" v-on="on">
          <v-btn
            :aria-label="$t('layout.undoChanges')"
            :disabled="!canUndo"
            icon
            @click="undo">
            <v-icon class="icon-default-color" size="20">fa-undo</v-icon>
          </v-btn>
        </div>
      </template>
      <span>{{ $t('layout.undoChanges') }}</span>
    </v-tooltip>
    <v-tooltip bottom>
      <template #activator="{on, attrs}">
        <div v-bind="attrs" v-on="on">
          <v-btn
            :aria-label="$t('layout.redoChanges')"
            :disabled="!canRedo"
            icon
            @click="redo">
            <v-icon class="icon-default-color" size="20">fa-redo</v-icon>
          </v-btn>
        </div>
      </template>
      <span>{{ $t('layout.redoChanges') }}</span>
    </v-tooltip>
  </div>
</template>
<script>
  export default {
    data: () => ({
      loading: false,
    }),
    computed: {
      canUndo () {
        return !!this.$root.sectionHistory?.length;
      },
      canRedo () {
        return !!this.$root.sectionRedo?.length;
      },
    },
    methods: {
      undo () {
        const section = this.$root.sectionHistory.pop();
        const parentContainer = eXo.$layoutUtils.getParentContainer(this.$root.layout);
        const index = parentContainer.children.findIndex(c => c.storageId === section.storageId);
        if (index >= 0) {
          this.$root.sectionRedo.push(parentContainer.children[index]);
          parentContainer.children.splice(index, 1, section);
        }
      },
      redo () {
        const section = this.$root.sectionRedo.pop();
        const parentContainer = eXo.$layoutUtils.getParentContainer(this.$root.layout);
        const index = parentContainer.children.findIndex(c => c.storageId === section.storageId);
        if (index >= 0) {
          this.$root.sectionHistory.push(parentContainer.children[index]);
          parentContainer.children.splice(index, 1, section);
        }
      },
    },
  };
</script>