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

        <q-table
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth'

const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()

const requestTab = ref('active')
const search = ref('')
const selected = ref([])

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
  { label: 'Отклонено', value: 'REJECTED' }
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

const rows = ref([
  {
    id: 346,
    fio: 'Стародубцева А.К.',
    courseGroup: '4ИС',
    facultyId: 'F01',
    purpose: 'По месту требования',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '08.12.2025',
    status: 'IN_WORK',
    archived: false,
    registrationNumber: 16,
    registrationYear: 2026
  },
  {
    id: 347,
    fio: 'Иванова А.А.',
    courseGroup: '3ИС',
    facultyId: 'F01',
    purpose: 'Соцзащита',
    qty: 2,
    type: 'WITH_STIPEND',
    periodFrom: '01.09.2025',
    periodTo: '31.12.2025',
    createdAt: '09.12.2025',
    status: 'READY',
    archived: false,
    registrationNumber: 17,
    registrationYear: 2026
  },
  {
    id: 348,
    fio: 'Петров П.П.',
    courseGroup: '2МО',
    facultyId: 'F01',
    purpose: 'Военкомат',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '10.12.2025',
    status: 'NEW',
    archived: false,
    registrationNumber: null,
    registrationYear: null
  },
  {
    id: 349,
    fio: 'Смирнова Е.В.',
    courseGroup: '1ПО',
    facultyId: 'F01',
    purpose: 'По месту требования',
    qty: 1,
    type: 'WITH_STIPEND',
    periodFrom: '01.01.2025',
    periodTo: '31.05.2025',
    createdAt: '15.12.2025',
    status: 'ACCEPTED',
    archived: false,
    registrationNumber: 18,
    registrationYear: 2026
  },
  {
    id: 350,
    fio: 'Кузнецова Н.И.',
    courseGroup: '4ИС',
    facultyId: 'F01',
    purpose: 'Посольство',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '01.11.2025',
    status: 'READY',
    archived: true,
    registrationNumber: 11,
    registrationYear: 2025
  },
  {
    id: 351,
    fio: 'Орлов Д.С.',
    courseGroup: '3ИС',
    facultyId: 'F02',
    purpose: 'Соцзащита',
    qty: 1,
    type: 'WITH_STIPEND',
    periodFrom: '01.09.2025',
    periodTo: '31.12.2025',
    createdAt: '12.12.2025',
    status: 'IN_WORK',
    archived: false,
    registrationNumber: 5,
    registrationYear: 2026
  }
])

const visibleRows = computed(() => {
  if (auth.role === 'SECRETARY' && auth.facultyId) {
    return rows.value.filter((r) => r.facultyId === auth.facultyId)
  }

  return []
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

function formatRegistration(row) {
  if (!row.registrationNumber || !row.registrationYear) return ''
  return `${row.facultyId}-${row.registrationNumber}/${row.registrationYear}`
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
    REJECTED: 'red-7'
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

function bulkArchive() {
  if (requestTab.value !== 'active') return

  const ids = new Set(selected.value.map((s) => s.id))
  rows.value = rows.value.map((r) => (
    ids.has(r.id)
      ? { ...r, archived: true }
      : r
  ))

  selected.value = []

  $q.notify({
    type: 'positive',
    message: 'Заявки перемещены в архив.',
    position: 'top'
  })
}

function bulkUnarchive() {
  if (requestTab.value !== 'archive') return

  const ids = new Set(selected.value.map((s) => s.id))
  rows.value = rows.value.map((r) => (
    ids.has(r.id)
      ? { ...r, archived: false }
      : r
  ))

  selected.value = []

  $q.notify({
    type: 'positive',
    message: 'Заявки возвращены в активные.',
    position: 'top'
  })
}

function generateCommonDocument() {
  $q.notify({
    type: 'info',
    message: `Сформирован общий документ по ${selected.value.length} заявк(е/ам) — мок.`,
    position: 'top'
  })
}
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
