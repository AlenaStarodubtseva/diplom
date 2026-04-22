<template>
  <q-page class="q-pa-md">
    <div class="q-mb-md">
      <div class="text-h6">Справки</div>
    </div>

    <div v-if="loading" class="q-mb-md text-grey-7">
      Загрузка...
    </div>

    <div v-else-if="error" class="q-mb-md text-negative">
      {{ error }}
    </div>

    <template v-else>
      <RequestCard
        v-for="r in requests"
        :key="r.id"
        :title="`Справка об обучении №${r.id}`"
        :dateText="formatDate(r.createdAt)"
        :status="r.status"
        class="cursor-pointer"
        @click="$router.push(`/student/${r.id}`)"
      />

      <div v-if="!requests.length" class="text-grey-7 q-mt-md">
        У вас пока нет заявок
      </div>
    </template>

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
import { onMounted, ref } from 'vue'
import RequestCard from 'components/RequestCard.vue'
import { getRequests } from 'src/api/requests'

const requests = ref([])
const loading = ref(false)
const error = ref('')

async function loadRequests() {
  loading.value = true
  error.value = ''

  try {
    const response = await getRequests()
    requests.value = response.data
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявки'
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU')
}

onMounted(() => {
  loadRequests()
})
</script>
