<template>
  <q-page class="q-pa-md">
    <div class="q-mb-md">
      <div class="text-h6">Справки</div>
    </div>

    <RequestCard
      v-for="r in requests"
      :key="r.id"
      :title="`Справка об обучении №${r.number}`"
      :dateText="r.date"
      :status="r.status"
      class="cursor-pointer"
      @click="$router.push(`/student/${r.id}`)"
    />

    <q-btn
      unelevated
      class="full-width q-mt-lg"
      size="lg"
      icon="add"
      label="Создать новую заявку"
      color="grey-4"
      text-color="black"
      @click="$router.push('/student/new')"
    />
  </q-page>
</template>

<script setup>
import RequestCard from 'components/RequestCard.vue'

// пока мок-данные, потом подключим API
const requests = [
  { id: 346, number: 346, date: '08.12.2025', status: 'IN_WORK' },
  { id: 347, number: 347, date: '08.12.2025', status: 'READY' }
]
</script>
