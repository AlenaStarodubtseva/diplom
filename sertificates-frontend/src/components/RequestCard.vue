<template>
  <q-card flat bordered class="request-card q-mb-md" @click="$emit('click')">
    <q-card-section class="row items-center no-wrap">
      <div class="status-line" :class="statusClass"></div>

      <div class="col q-ml-md">
        <div class="text-subtitle1">{{ title }}</div>
        <div class="text-caption text-grey-7">от {{ dateText }}</div>
      </div>

      <div class="col-auto">
        <q-badge :color="statusColor" text-color="white">
          {{ statusText }}
        </q-badge>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  dateText: { type: String, default: '' },
  status: { type: String, default: 'NEW' }
})

defineEmits(['click'])

const statusText = computed(() => {
  return {
    NEW: 'Новая',
    ACCEPTED: 'Принята',
    IN_WORK: 'В обработке',
    DELAYED: 'Задерживается',
    READY: 'Готова',
    REJECTED: 'Отклонена',
    ARCHIVED: 'Архив',
    CANCELLED: 'Отменена'
  }[props.status] || props.status
})

const statusColor = computed(() => {
  return {
    NEW: 'grey',
    ACCEPTED: 'blue',
    IN_WORK: 'orange',
    DELAYED: 'brown',
    READY: 'green',
    REJECTED: 'red',
    ARCHIVED: 'grey-7',
    CANCELLED: 'deep-orange'
  }[props.status] || 'grey'
})

const statusClass = computed(() => {
  return {
    NEW: 'line-grey',
    ACCEPTED: 'line-blue',
    IN_WORK: 'line-orange',
    DELAYED: 'line-brown',
    READY: 'line-green',
    REJECTED: 'line-red',
    ARCHIVED: 'line-grey-dark',
    CANCELLED: 'line-deep-orange'
  }[props.status] || 'line-grey'
})
</script>

<style scoped>
.request-card {
  border-radius: 18px;
  cursor: pointer;
  transition: 0.2s ease;
}

.request-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.status-line {
  width: 8px;
  height: 60px;
  border-radius: 8px;
}

.line-grey {
  background: #9e9e9e;
}

.line-blue {
  background: #1e88e5;
}

.line-orange {
  background: #fb8c00;
}

.line-brown {
  background: #8d6e63;
}

.line-green {
  background: #43a047;
}

.line-red {
  background: #e53935;
}

.line-grey-dark {
  background: #616161;
}

.line-deep-orange {
  background: #f4511e;
}
</style>
