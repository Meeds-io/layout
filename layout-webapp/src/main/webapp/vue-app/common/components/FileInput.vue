<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <v-file-input
    v-if="!resetInput"
    ref="fileInput"
    :multiple="false"
    accept=".zip"
    class="d-none position-absolute"
    hide-input
    @change="handleFileChange" />
</template>

<script>
export default {
  data() {
    return {
      selectedFile: null,
      uploadId: null,
      resetInput: false
    };
  },
  computed: {
    fileName() {
      return this.selectedFile?.name;
    }
  },
  created() {
    this.$root.$on('reset-uploaded-file', this.reset);
  },
  methods: {
    openFileExplorer() {
      this.$refs.fileInput.$el.querySelector('input').click();
    },
    handleFileChange(files) {
      if (files) {
        this.selectedFile = files[0];
        this.uploadFile();
      }
    },
    uploadFile() {
      this.$root.$emit('close-alert-message');
      if (this.selectedFile && this.selectedFile.size) {
        this.sending = true;
        const self = this;
        return this.$uploadService.upload(this.selectedFile)
          .then(uploadId => {
            if (uploadId) {
              const reader = new FileReader();
              reader.onload = (e) => {
                self.$emit('src', e.target.result);
                self.$forceUpdate();
              };
              reader.readAsDataURL(this.selectedFile);
              this.$emit('input', uploadId);
              this.uploadId = uploadId;
              this.$emit('uploaded', this.uploadId, this.fileName);
            }
          })
          .catch(error => this.$root.$emit('alert-message', this.$t(String(error)), 'error'))
          .finally(() => this.sending = false);
      }
    },
    reset() {
      this.selectedFile = null;
      this.uploadId = null;
      this.resetInput = true;
      this.$nextTick().then(() => this.resetInput = false);
    },
  }
};
</script>