<template>
  <q-card class="q-mb-md card">
    <q-card-section class="row no-wrap items-stretch q-pa-md">
      <StatusStrip :color="statusColor" class="q-mr-md" />

      <div class="col">
        <div class="text-subtitle1 text-weight-medium">
          {{ title }}
        </div>
        <div class="text-caption text-grey-7">
          от {{ dateText }}
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup>
import StatusStrip from 'components/StatusStrip.vue'

const props = defineProps({
  title: { type: String, required: true },
  dateText: { type: String, required: true },
  status: { type: String, default: 'NEW' }
})

const statusColorMap = {
  NEW: '#ffb300',       // жёлтый
  IN_WORK: '#ffb300',   // можно тоже жёлтым
  READY: '#2e7d32',     // зелёный
  REJECTED: '#c62828',  // красный
  ACCEPTED: '#1976d2',  // синий
  DELAYED: '#6d4c41'    // коричневый/серый
}

const statusColor = statusColorMap[props.status] || '#9e9e9e'
</script>

<style scoped>
.card {
  border-radius: 14px;
}
</style>
