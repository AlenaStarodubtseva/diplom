<template>
  <q-page class="q-pa-md requests-page">
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

    <div class="create-btn-wrap">
      <q-btn
        unelevated
        class="create-btn"
        size="lg"
        icon="add"
        label="Создать новую заявку"
        @click="$router.push('/student/new')"
      />
    </div>
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

<style scoped>
.requests-page {
  background: #f7f7f8;
  min-height: 100%;
}

.create-btn-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.create-btn {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
  background: #8b0015 !important;
  color: #ffffff !important;
  font-weight: 600;
  letter-spacing: 0.2px;
  padding: 12px 20px;
}

.create-btn:hover {
  background: #a3001b !important;
}

@media (max-width: 600px) {
  .create-btn {
    max-width: 100%;
    font-size: 18px;
  }
}
</style>
