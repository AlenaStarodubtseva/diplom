<template>
  <q-page class="q-pa-md requests-page">
    <div class="q-mb-md">
      <div class="text-h6">Заявки на справки</div>
      <div class="text-caption text-grey-7">
        Список заявок, доступных секретарю
      </div>
    </div>

    <q-card flat class="filter-card q-mb-md">
      <q-card-section class="q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-12 col-md-4">
            <q-select
              v-model="statusFilter"
              :options="statusOptions"
              label="Фильтр по статусу"
              outlined
              emit-value
              map-options
              clearable
              color="dark"
            />
          </div>

          <div class="col-12 col-md-4">
            <q-input
              v-model="searchText"
              label="Поиск по ФИО / группе / цели"
              outlined
              color="dark"
              clearable
            />
          </div>
        </div>
      </q-card-section>
    </q-card>

    <div v-if="loading" class="q-mb-md text-grey-7">
      Загрузка...
    </div>

    <div v-else-if="error" class="q-mb-md text-negative">
      {{ error }}
    </div>

    <template v-else>
      <RequestCard
        v-for="r in filteredRequests"
        :key="r.id"
        :title="requestTitle(r)"
        :dateText="requestSubtitle(r)"
        :status="r.status"
        class="cursor-pointer"
        @click="router.push(`/secretary/${r.id}`)"
      />

      <div v-if="!filteredRequests.length" class="text-grey-7 q-mt-md">
        Подходящих заявок не найдено
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RequestCard from 'components/RequestCard.vue'
import { getRequests } from 'src/api/requests'

const router = useRouter()

const requests = ref([])
const loading = ref(false)
const error = ref('')

const statusFilter = ref(null)
const searchText = ref('')

const statusOptions = [
  { label: 'Новая', value: 'NEW' },
  { label: 'Принята', value: 'ACCEPTED' },
  { label: 'В обработке', value: 'IN_WORK' },
  { label: 'Задерживается', value: 'DELAYED' },
  { label: 'Готова', value: 'READY' },
  { label: 'Отклонена', value: 'REJECTED' },
  { label: 'Архив', value: 'ARCHIVED' },
  { label: 'Отменена', value: 'CANCELLED' }
]

async function loadRequests() {
  loading.value = true
  error.value = ''

  try {
    const { data } = await getRequests()
    requests.value = data
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявки'
  } finally {
    loading.value = false
  }
}

const filteredRequests = computed(() => {
  let result = [...requests.value]

  if (statusFilter.value) {
    result = result.filter(item => item.status === statusFilter.value)
  }

  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase()

    result = result.filter(item =>
      (item.studentFullName || '').toLowerCase().includes(q) ||
      (item.groupName || '').toLowerCase().includes(q) ||
      (item.purpose || '').toLowerCase().includes(q)
    )
  }

  result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return result
})

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU')
}

function requestTitle(r) {
  const num = r.registrationNumber ?? r.id
  return `Справка №${num} — ${r.studentFullName || 'Без ФИО'}`
}

function requestSubtitle(r) {
  return `${formatDate(r.createdAt)} | ${r.groupName || '—'} | ${r.purpose || '—'}`
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

.filter-card {
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(24, 39, 75, 0.06);
}
</style>
