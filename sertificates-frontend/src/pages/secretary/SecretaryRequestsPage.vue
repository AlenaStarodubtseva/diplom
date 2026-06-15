<template>
  <q-page class="q-pa-md bg-grey-1">
    <div class="row items-center q-col-gutter-sm q-mb-md">
      <div class="col">
        <div class="text-h5 text-weight-semibold">Заявки секретаря</div>
        <div class="text-grey-7">
          Единый реестр заявок с поиском и фильтрацией
        </div>
      </div>
    </div>

    <q-banner rounded class="bg-blue-1 text-black q-mb-md">
      <div>
        <b>Пользователь:</b> {{ auth.login || '—' }}
      </div>
      <div>
        <b>Доступные факультеты:</b> {{ availableFacultiesLabel }}
      </div>
    </q-banner>

    <q-card class="card">
      <q-card-section>
        <div class="row items-center q-col-gutter-sm q-mb-md">
          <div class="col-12 col-md">
            <q-input
              v-model="search"
              dense
              outlined
              debounce="300"
              placeholder="Поиск: ФИО / № заявки / группа / рег. номер / цель / статус"
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
              <div v-if="props.row.registrationNumbers?.length" class="column q-gutter-xs">
                <q-chip
                  v-for="number in props.row.registrationNumbers"
                  :key="number.id"
                  dense
                  outline
                  color="primary"
                  text-color="primary"
                  class="registration-chip"
                >
                  {{ formatRegistrationNumber(number) }}
                </q-chip>
              </div>

              <span v-else-if="props.row.registrationNumber">
                {{ formatRegistration(props.row) }}
              </span>

              <span v-else class="text-grey-6">Не присвоен</span>
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
                unelevated
                dense
                color="primary"
                icon="open_in_new"
                label="Открыть"
                @click="openRequest(props.row.id)"
              />
            </q-td>
          </template>

          <template #body-cell-faculty="props">
            <q-td :props="props">
              {{ facultyLabel(props.row.facultyId) }}
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
import { getFaculties } from 'src/api/faculties'
import { getAccessAccounts } from 'src/api/accessAccounts'
import { generateCommonRequestDocument } from 'src/api/requestDocuments'
import { getRegistrationNumbersByRequestIds } from 'src/api/requestRegistrationNumbers'

const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()

const requestTab = ref('active')
const search = ref('')
const selected = ref([])

const loading = ref(false)
const error = ref('')

const rows = ref([])
const faculties = ref([])
const accessRows = ref([])

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
  { label: 'Принята', value: 'ACCEPTED' },
  { label: 'В обработке', value: 'IN_WORK' },
  { label: 'Задерживается', value: 'DELAYED' },
  { label: 'Готово', value: 'READY' },
  { label: 'Отклонена', value: 'REJECTED' },
  { label: 'В архиве', value: 'ARCHIVED' },
  { label: 'Отменена', value: 'CANCELLED' }
]

const columns = [
  { name: 'registration', label: 'Рег. номера', field: 'registration', align: 'left' },
  { name: 'id', label: '№ заявки', field: 'id', sortable: true, align: 'left' },
  { name: 'fio', label: 'ФИО', field: 'fio', sortable: true, align: 'left' },
  { name: 'status', label: 'Статус', field: 'status', align: 'left' },
  { name: 'actions', label: 'Действие', field: 'actions', align: 'left' },
  { name: 'faculty', label: 'Факультет', field: 'faculty', align: 'left' },
  { name: 'courseGroup', label: 'Курс/группа', field: 'courseGroup', align: 'left' },
  { name: 'purpose', label: 'Куда нужна справка', field: 'purpose', align: 'left' },
  { name: 'qty', label: 'Кол-во', field: 'qty', sortable: true, align: 'left' },
  { name: 'type', label: 'Тип', field: 'type', align: 'left' },
  { name: 'period', label: 'Период', field: 'period', align: 'left' },
  { name: 'createdAt', label: 'Дата подачи', field: 'createdAt', sortable: true, align: 'left' }
]

const currentAccess = computed(() => {
  if (auth.role === 'ADMIN') {
    return {
      login: auth.login || 'admin',
      role: 'ADMIN',
      facultyIds: faculties.value.map(faculty => faculty.id),
      active: true
    }
  }

  if (auth.role !== 'SECRETARY') {
    return null
  }

  const login = String(auth.login || '').toLowerCase()

  const account = accessRows.value.find(row =>
    String(row.login || '').toLowerCase() === login
  )

  if (account) {
    return account
  }

  return null
})

const availableFacultyIds = computed(() => {
  if (!currentAccess.value) return []

  if (currentAccess.value.role === 'ADMIN') {
    return faculties.value.map(faculty => faculty.id)
  }

  if (currentAccess.value.active === false) {
    return []
  }

  return currentAccess.value.facultyIds || []
})

const availableFacultiesLabel = computed(() => {
  if (!availableFacultyIds.value.length) {
    return 'нет доступных факультетов'
  }

  return availableFacultyIds.value
    .map(id => facultyLabel(id))
    .join(', ')
})

const visibleRows = computed(() => {
  if (auth.role === 'ADMIN') {
    return rows.value
  }

  return rows.value.filter(row =>
    availableFacultyIds.value.includes(row.facultyId)
  )
})

const filteredRows = computed(() => {
  const q = search.value.trim().toLowerCase()

  return visibleRows.value
    .filter((r) => (requestTab.value === 'archive' ? r.archived : !r.archived))
    .filter((r) => {
      if (filters.value.type && r.type !== filters.value.type) return false
      if (filters.value.status && r.status !== filters.value.status) return false

      if (filters.value.onlyRegistered) {
        const hasRegistrationNumbers = r.registrationNumbers?.length || r.registrationNumber

        if (!hasRegistrationNumbers) return false
      }

      if (q) {
        const registrationText = r.registrationNumbers?.length
          ? r.registrationNumbers.map(formatRegistrationNumber).join(' ')
          : r.registrationNumber
            ? formatRegistration(r)
            : ''

        const haystack = [
          r.id,
          r.fio,
          facultyLabel(r.facultyId),
          r.courseGroup,
          r.purpose,
          statusLabel(r.status),
          registrationText
        ]
          .join(' ')
          .toLowerCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
})

async function loadData() {
  loading.value = true
  error.value = ''

  try {
    const [
      requestsResponse,
      facultiesResponse,
      accessAccountsResponse
    ] = await Promise.all([
      getRequests(),
      getFaculties(),
      getAccessAccounts()
    ])

    faculties.value = facultiesResponse.data.map(faculty => ({
      id: faculty.id,
      code: faculty.code,
      name: faculty.name,
      active: faculty.isActive !== false
    }))

    accessRows.value = accessAccountsResponse.data.map(account => ({
      id: account.id,
      login: account.login,
      fio: account.fullName,
      role: account.role,
      facultyIds: account.facultyIds || [],
      active: account.isActive !== false
    }))

    const normalizedRows = requestsResponse.data.map(normalizeRow)
    const requestIds = normalizedRows.map(row => row.id)

    let registrationNumbersByRequestId = {}

    if (requestIds.length) {
      const registrationNumbersResponse = await getRegistrationNumbersByRequestIds(requestIds)

      registrationNumbersByRequestId = registrationNumbersResponse.data.reduce((acc, number) => {
        const key = Number(number.requestId)

        if (!acc[key]) {
          acc[key] = []
        }

        acc[key].push(number)

        return acc
      }, {})
    }

    rows.value = normalizedRows.map(row => ({
      ...row,
      registrationNumbers: registrationNumbersByRequestId[row.id] || []
    }))
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
    facultyId: r.facultyId,
    purpose: r.purpose || '—',
    qty: r.copiesCount || 1,
    type: r.certificateType,
    periodFrom: formatDate(r.periodFrom),
    periodTo: formatDate(r.periodTo),
    createdAt: formatDate(r.createdAt),
    status: r.status,
    archived: r.status === 'ARCHIVED',
    registrationNumber: r.registrationNumber,
    registrationYear: r.registrationYear,
    registrationNumbers: []
  }
}

function facultyLabel(facultyId) {
  const faculty = faculties.value.find(f => f.id === facultyId)

  if (!faculty) return '—'

  return faculty.code
    ? `${facultyCode(facultyId)} — ${faculty.name}`
    : faculty.name
}

function facultyCode(facultyId) {
  const faculty = faculties.value.find(f => f.id === facultyId)

  if (faculty?.code && /^\d+$/.test(String(faculty.code))) {
    return String(faculty.code).padStart(2, '0')
  }

  return String(facultyId).padStart(2, '0')
}

function formatDate(value) {
  if (!value) return null

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return null

  return date.toLocaleDateString('ru-RU')
}

function formatRegistration(row) {
  if (!row.registrationNumber || !row.registrationYear) return ''

  return formatRegistrationNumber({
    facultyId: row.facultyId,
    registrationNumber: row.registrationNumber,
    registrationYear: row.registrationYear
  })
}

function formatRegistrationNumber(number) {
  if (!number?.registrationNumber || !number?.registrationYear) return ''

  const facultyCodeValue = facultyCode(number.facultyId)
  const regNumber = String(number.registrationNumber).padStart(4, '0')
  const year = String(number.registrationYear).slice(-2)

  return `${facultyCodeValue}-${regNumber}/${year}`
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
      await updateRequestStatus(row.id, 'ARCHIVED', 'Заявка перемещена в архив.')
    }

    await loadData()
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
      await updateRequestStatus(row.id, 'ACCEPTED', 'Заявка возвращена из архива.')
    }

    await loadData()
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

async function generateCommonDocument() {
  if (!selected.value.length) {
    $q.notify({
      type: 'negative',
      message: 'Выберите хотя бы одну заявку.',
      position: 'top'
    })
    return
  }

  const hasNoStipendRequests = selected.value.some(
    (request) => request.type === 'NO_STIPEND'
  )

  if (hasNoStipendRequests) {
    $q.notify({
      type: 'negative',
      message: 'Общий документ можно сформировать только для справок с отметкой о стипендии.',
      position: 'top'
    })
    return
  }

  try {
    const requestIds = selected.value.map((request) => request.id)

    const response = await generateCommonRequestDocument(requestIds)

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    })

    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = 'Общий_документ.docx'
    link.click()

    URL.revokeObjectURL(url)

    $q.notify({
      type: 'positive',
      message: 'Общий документ сформирован.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: err.response?.data?.message || 'Не удалось сформировать документ.',
      position: 'top'
    })
  }
}

onMounted(() => {
  loadData()
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

.registration-chip {
  width: fit-content;
}
</style>
