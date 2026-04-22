<template>
  <q-page class="q-pa-md bg-grey-1">
    <div class="row items-center q-col-gutter-sm q-mb-md">
      <div class="col">
        <div class="text-h5 text-weight-semibold">Заявки секретаря</div>
        <div class="text-grey-7">
          Единый реестр заявок с поиском и фильтрацией
        </div>
      </div>

      <div class="col-auto" v-if="auth.facultyId">
        <q-chip outline color="primary" text-color="primary">
          Факультет: {{ auth.facultyId }}
        </q-chip>
      </div>
    </div>

    <q-card class="card">
      <q-card-section>
        <div class="row items-center q-col-gutter-sm q-mb-md">
          <div class="col-12 col-md">
            <q-input
              v-model="search"
              dense
              outlined
              debounce="300"
              placeholder="Поиск: ФИО / № заявки / группа / рег. номер / цель"
            >
              <template #append>
                <q-icon name="search" />
              </template>
            </q-input>
          </div>

          <div class="col-12 col-md-auto">
            <q-btn
              unelevated
              color="primary"
              icon="add"
              label="Создать заявку"
              @click="$router.push('/manual-request')"
            />
          </div>
        </div>

        <q-tabs v-model="requestTab" dense align="left" class="campus-tabs q-mb-md">
          <q-tab name="active" label="АКТИВНЫЕ" />
          <q-tab name="archive" label="АРХИВ" />
        </q-tabs>

        <div class="row q-col-gutter-sm items-center q-mb-md">
          <div class="col-12 col-md-3">
            <q-select
              v-model="filters.type"
              dense
              outlined
              clearable
              :options="typeOptions"
              label="Тип справки"
              emit-value
              map-options
            />
          </div>

          <div class="col-12 col-md-3">
            <q-select
              v-model="filters.status"
              dense
              outlined
              clearable
              :options="statusOptions"
              label="Статус"
              emit-value
              map-options
            />
          </div>

          <div class="col-12 col-md-3">
            <q-toggle
              v-model="filters.onlyRegistered"
              color="primary"
              label="Только зарегистрированные"
            />
          </div>

          <div class="col-12 col-md-3">
            <q-btn
              outline
              class="campus-accent full-width"
              icon="restart_alt"
              label="Сбросить фильтры"
              @click="resetFilters"
            />
          </div>
        </div>

        <div v-if="selected.length" class="row items-center q-gutter-sm q-mb-md">
          <q-chip dense color="grey-3" text-color="black">
            Выбрано: {{ selected.length }}
          </q-chip>

          <template v-if="requestTab === 'active'">
            <q-btn
              unelevated
              color="primary"
              icon="archive"
              label="В архив"
              @click="bulkArchive"
            />
            <q-btn
              unelevated
              color="positive"
              icon="description"
              label="Сформировать общий документ"
              @click="generateCommonDocument"
            />
          </template>

          <template v-else>
            <q-btn
              unelevated
              color="primary"
              icon="unarchive"
              label="Вернуть в активные"
              @click="bulkUnarchive"
            />
          </template>
        </div>

        <div v-if="loading" class="q-pa-md text-grey-7">
          Загрузка...
        </div>

        <div v-else-if="error" class="q-pa-md text-negative">
          {{ error }}
        </div>

        <q-table
          v-else
          :rows="filteredRows"
          :columns="columns"
          row-key="id"
          flat
          selection="multiple"
          v-model:selected="selected"
          :pagination="{ rowsPerPage: 10 }"
          class="campus-table"
        >
          <template #body-cell-registration="props">
            <q-td :props="props">
              <span v-if="props.row.registrationNumber">
                {{ formatRegistration(props.row) }}
              </span>
              <span v-else class="text-grey-6">Не присвоен</span>
            </q-td>
          </template>

          <template #body-cell-type="props">
            <q-td :props="props">
              {{ typeLabel(props.row.type) }}
            </q-td>
          </template>

          <template #body-cell-period="props">
            <q-td :props="props">
              <span v-if="props.row.periodFrom && props.row.periodTo">
                {{ props.row.periodFrom }} — {{ props.row.periodTo }}
              </span>
              <span v-else class="text-grey-6">—</span>
            </q-td>
          </template>

          <template #body-cell-status="props">
            <q-td :props="props">
              <q-chip
                dense
                :color="statusColor(props.row.status)"
                text-color="white"
              >
                {{ statusLabel(props.row.status) }}
              </q-chip>
            </q-td>
          </template>

          <template #body-cell-actions="props">
            <q-td :props="props">
              <q-btn
                flat
                dense
                round
                icon="open_in_new"
                class="campus-accent"
                @click="openRequest(props.row.id)"
              />
            </q-td>
          </template>

          <template #no-data>
            <div class="full-width row flex-center text-grey-7 q-gutter-sm q-pa-lg">
              <q-icon name="inbox" size="24px" />
              <span>Ничего не найдено по текущим фильтрам</span>
            </div>
          </template>
        </q-table>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth'
import { getRequests, updateRequestStatus } from 'src/api/requests'

const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()

const requestTab = ref('active')
const search = ref('')
const selected = ref([])

const loading = ref(false)
const error = ref('')

const filters = ref({
  type: null,
  status: null,
  onlyRegistered: false
})

const typeOptions = [
  { label: 'Без отметки', value: 'NO_STIPEND' },
  { label: 'Со стипендией', value: 'WITH_STIPEND' }
]

const statusOptions = [
  { label: 'Новая', value: 'NEW' },
  { label: 'Принято', value: 'ACCEPTED' },
  { label: 'В обработке', value: 'IN_WORK' },
  { label: 'Отложено', value: 'DELAYED' },
  { label: 'Готово', value: 'READY' },
  { label: 'Отклонено', value: 'REJECTED' },
  { label: 'В архиве', value: 'ARCHIVED' },
  { label: 'Отменена', value: 'CANCELLED' }
]

const columns = [
  { name: 'registration', label: 'Рег. номер', field: 'registration', align: 'left' },
  { name: 'id', label: '№ заявки', field: 'id', sortable: true, align: 'left' },
  { name: 'fio', label: 'ФИО', field: 'fio', sortable: true, align: 'left' },
  { name: 'courseGroup', label: 'Курс/группа', field: 'courseGroup', align: 'left' },
  { name: 'purpose', label: 'Куда нужна справка', field: 'purpose', align: 'left' },
  { name: 'qty', label: 'Кол-во', field: 'qty', sortable: true, align: 'left' },
  { name: 'type', label: 'Тип', field: 'type', align: 'left' },
  { name: 'period', label: 'Период', field: 'period', align: 'left' },
  { name: 'createdAt', label: 'Дата подачи', field: 'createdAt', sortable: true, align: 'left' },
  { name: 'status', label: 'Статус', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const rows = ref([])

async function loadRequests() {
  loading.value = true
  error.value = ''

  try {
    const { data } = await getRequests()
    rows.value = data.map(normalizeRow)
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявки'
  } finally {
    loading.value = false
  }
}

function normalizeRow(r) {
  return {
    id: r.id,
    fio: r.studentFullName || '—',
    courseGroup: [r.course ? `${r.course} курс` : null, r.groupName].filter(Boolean).join(' / ') || '—',
    facultyId: r.facultyId ? `F${String(r.facultyId).padStart(2, '0')}` : null,
    purpose: r.purpose || '—',
    qty: r.copiesCount || 1,
    type: r.certificateType,
    periodFrom: formatDate(r.periodFrom),
    periodTo: formatDate(r.periodTo),
    createdAt: formatDate(r.createdAt),
    status: r.status,
    archived: r.status === 'ARCHIVED',
    registrationNumber: r.registrationNumber,
    registrationYear: r.registrationYear
  }
}

const visibleRows = computed(() => {
  if (auth.role === 'SECRETARY' && auth.facultyId) {
    return rows.value.filter((r) => r.facultyId === auth.facultyId)
  }

  return rows.value
})

const filteredRows = computed(() => {
  const q = search.value.trim().toLowerCase()

  return visibleRows.value
    .filter((r) => (requestTab.value === 'archive' ? r.archived : !r.archived))
    .filter((r) => {
      if (filters.value.type && r.type !== filters.value.type) return false
      if (filters.value.status && r.status !== filters.value.status) return false
      if (filters.value.onlyRegistered && !r.registrationNumber) return false

      if (q) {
        const haystack = [
          r.id,
          r.fio,
          r.courseGroup,
          r.purpose,
          r.registrationNumber ? formatRegistration(r) : ''
        ]
          .join(' ')
          .toLowerCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
})

function formatDate(value) {
  if (!value) return null
  return new Date(value).toLocaleDateString('ru-RU')
}

function formatRegistration(row) {
  if (!row.registrationNumber || !row.registrationYear) return ''
  return `${row.facultyId}-${String(row.registrationNumber).padStart(4, '0')}/${row.registrationYear}`
}

function typeLabel(type) {
  const map = {
    NO_STIPEND: 'Без отметки',
    WITH_STIPEND: 'Со стипендией'
  }
  return map[type] || type
}

function statusLabel(status) {
  return statusOptions.find((x) => x.value === status)?.label || status
}

function statusColor(status) {
  const map = {
    NEW: 'grey-8',
    ACCEPTED: 'blue-7',
    IN_WORK: 'orange-8',
    DELAYED: 'brown-6',
    READY: 'green-7',
    REJECTED: 'red-7',
    ARCHIVED: 'blue-grey-7',
    CANCELLED: 'deep-orange-6'
  }

  return map[status] || 'grey-7'
}

function resetFilters() {
  filters.value.type = null
  filters.value.status = null
  filters.value.onlyRegistered = false
}

function openRequest(id) {
  router.push(`/secretary/${id}`)
}

async function bulkArchive() {
  if (requestTab.value !== 'active') return

  try {
    for (const row of selected.value) {
      await updateRequestStatus(row.id, 'ARCHIVED')
    }

    await loadRequests()
    selected.value = []

    $q.notify({
      type: 'positive',
      message: 'Заявки перемещены в архив.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось архивировать выбранные заявки.',
      position: 'top'
    })
  }
}

async function bulkUnarchive() {
  if (requestTab.value !== 'archive') return

  try {
    for (const row of selected.value) {
      await updateRequestStatus(row.id, 'ACCEPTED')
    }

    await loadRequests()
    selected.value = []

    $q.notify({
      type: 'positive',
      message: 'Заявки возвращены в активные.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось вернуть заявки из архива.',
      position: 'top'
    })
  }
}

function generateCommonDocument() {
  $q.notify({
    type: 'info',
    message: `Сформирован общий документ по ${selected.value.length} заявк(е/ам) — пока мок.`,
    position: 'top'
  })
}

onMounted(() => {
  loadRequests()
})
</script>

<style scoped>
.card {
  border-radius: 14px;
}

.campus-accent {
  color: #7a0019;
  border-color: #7a0019;
}

.campus-tabs :deep(.q-tab--active) {
  color: #7a0019;
}

.campus-table :deep(.q-table__bottom) {
  border-top: 1px solid #eee;
}
</style>
