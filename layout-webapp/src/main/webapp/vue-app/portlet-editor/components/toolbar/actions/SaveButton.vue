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
  <v-btn
    :aria-label="$t('layout.save')"
    class="btn btn-primary d-flex align-center"
    :disabled="disabled"
    elevation="0"
    :loading="loading"
    @click="save">
    <span class="text-none">{{ $t('layout.save') }}</span>
  </v-btn>
</template>
<script>
  export default {
    props: {
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    data: () => ({
      loading: false,
    }),
    methods: {
      async save () {
        this.loading = true;
        try {
          const instance = await eXo.$portletInstanceService.getPortletInstance(this.$root.portletInstanceId);
          instance.preferences = await eXo.$portletInstanceService.getPortletInstancePreferences(this.$root.portletInstanceId);

          await eXo.$portletInstanceService.updatePortletInstance(instance);
          if (!this.$root.portletInstanceEmpty) {
            await this.savePreview();
          }

          if (window?.opener) {
            window?.opener?.dispatchEvent?.(new CustomEvent('portlet-instance-layout-updated', {
              detail: instance,
            }));
            window.close();
          } else {
            this.$root.$emit('alert-message', this.$t('layout.portletInstanceLayoutUpdatedSuccessfully'), 'success');
          }
        } catch (e) {
          console.debug('Error saving portlet instance', e); // eslint-disable-line no-console
          this.$root.$emit('alert-message', this.$t('layout.portletInstanceLayoutUpdatedSuccessfully'), 'success');
        } finally {
          window.setTimeout(() => this.loading = false, 50);
        }
      },
      async savePreview () {
        const previewCanvas = await window.html2canvas(this.$root.portletInstanceElement);
        const previewImage = previewCanvas.toDataURL('image/png');
        const previewBlob = this.convertPreviewToFile(previewImage);
        const uploadId =  await eXo.$uploadService.upload(previewBlob);
        await new Promise((resolve, reject) => {
          const interval = window.setInterval(() => {
            eXo.$uploadService.getUploadProgress(uploadId)
              .then(percent => {
                if (Number(percent) === 100) {
                  window.clearInterval(interval);
                  resolve();
                }
              })
              .catch(e => reject(e));
          }, 200);
        });
        return await eXo.$fileAttachmentService.saveAttachments({
          objectType: 'portletInstance',
          objectId: this.$root.portletInstanceId,
          uploadedFiles: [{ uploadId }],
          attachedFiles: [],
        });
      },
      convertPreviewToFile (previewImage) {
        const imgString = window.atob(previewImage.replace(/^data:image\/\w+;base64,/, ''));
        const bytes = new Uint8Array(imgString.length);
        for (let i = 0; i < imgString.length; i++) {
          bytes[i] = imgString.charCodeAt(i);
        }
        return new Blob([bytes], { type: 'image/png' });
      },
    },
  };
</script>