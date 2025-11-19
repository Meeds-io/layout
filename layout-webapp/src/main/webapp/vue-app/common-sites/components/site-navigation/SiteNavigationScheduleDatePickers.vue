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
  <v-card
    class="d-flex"
    max-width="420"
    width="100%"
    flat>
    <div class="flex-grow-0 flex-shrink-0 me-4 mt-2">
      <v-icon size="24">far fa-clock</v-icon>
    </div>
    <div class="flex-grow-1 flex-shrink-1">
      <div class="d-flex full-width overflow-hidden">
        <date-picker
          :attach="false"
          top
          v-model="startScheduleDate"
          :min-value="minimumStartDate"
          class="scheduleStartDatePicker flex-grow-1 flex-shrink-1 input-width-full pa-0" />
        <time-picker
          v-model="startScheduleTime"
          :min="minimumStartTime"
          class="flex-grow-0 flex-shrink-0 mb-3" />
      </div>
      <div class="d-flex full-width overflow-hidden">
        <date-picker
          :attach="false"
          top
          v-model="endScheduleDate"
          :min-value="minimumEndDate"
          class="scheduleEndDatePicker flex-grow-1 flex-shrink-1 input-width-full pa-0" />
        <time-picker
          v-model="endScheduleTime"
          :min="minimumEndTime"
          class="flex-grow-0 flex-shrink-0 mb-3" />
      </div>
    </div>
  </v-card>
</template>

<script>
export default {
  props: {
    startScheduleDate: {
      type: Number,
      default: 0,
    },
    endScheduleDate: {
      type: Number,
      default: 0,
    },
    startScheduleTime: {
      type: Number,
      default: 0,
    },
    endScheduleTime: {
      type: Number,
      default: 0,
    }
  },
  computed: {
    minimumStartDate() {
      return new Date();
    },
    minimumStartTime() {
      if (!this.startScheduleDate || !this.checkDatesOnSameDay(this.startScheduleDate, new Date().getTime())) {
        return null;
      }
      return new Date();
    },
    minimumEndDate() {
      if (!this.startScheduleDate) {
        return null;
      }
      return new Date(this.startScheduleDate);
    },
    minimumEndTime() {
      this.$emit('change', this.startScheduleDate, this.endScheduleDate, this.startScheduleTime, this.endScheduleTime);
      if (!this.startScheduleDate || !this.endScheduleDate || !this.startScheduleTime || !this.endScheduleTime || !this.checkDatesOnSameDay(this.startScheduleDate, new Date().getTime())) {
        return null;
      }
      if (this.checkDatesOnSameDay(this.startScheduleDate, this.endScheduleDate)){
        return new Date(this.startScheduleTime.getTime() + 900000);
      }
      return null;
    },
  },
  watch: {
    startScheduleDate(newVal, oldVal) {
      if (!newVal || !oldVal || this.checkDatesOnSameDay(newVal, oldVal)) {
        return;
      }
      const newDate = new Date(newVal);
      this.endScheduleDate = new Date(newDate.getTime());
      if (this.checkDatesOnSameDay(newVal, new Date().getTime())) {
        this.startScheduleTime = new Date(new Date().getTime() + 900000);
      }
    },
    startScheduleTime(newVal, oldVal) {
      if (!newVal || !oldVal || new Date(newVal).getTime() === new Date(oldVal).getTime()) {
        return;
      }
      const newDate = new Date(newVal);
      this.endScheduleTime = new Date(newDate.getTime() + 900000);
    },
  },
  methods: {
    checkDatesOnSameDay(firstDate, secondDate) {
      return new Date(firstDate).getFullYear() === new Date(secondDate).getFullYear() &&
            new Date(firstDate).getMonth() === new Date(secondDate).getMonth() &&
            new Date(firstDate).getDate() === new Date(secondDate).getDate();
    },
  }
};
</script>