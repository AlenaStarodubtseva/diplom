<template>
  <q-page class="q-pa-md page-bg">
    <div class="page-header q-mb-md row items-center justify-between">
      <div>
        <div class="text-h5 text-weight-medium">
          Ручное создание заявки
        </div>

        <div class="text-caption text-grey-7 q-mt-xs">
          Оформление заявки сотрудником
        </div>
      </div>

      <q-btn
        flat
        color="grey-8"
        icon="arrow_back"
        label="Назад"
        @click="goBack"
      />
    </div>

    <q-card class="main-card" flat>
      <q-card-section class="q-pa-lg">

        <div v-if="loading" class="q-pa-md text-grey-7">
          Загрузка данных...
        </div>

        <q-form
          v-else
          @submit="submitForm"
          class="form-grid"
        >
          <div class="section-title">
            Обучающийся
          </div>

          <q-select
            v-model="selectedStudentId"
            :options="filteredStudentOptions"
            label="Поиск студента"
            outlined
            use-input
            fill-input
            hide-selected
            input-debounce="0"
            emit-value
            map-options
            option-label="label"
            option-value="value"
            :loading="loading"
            @filter="filterStudents"
            :rules="[val => !!val || 'Выберите студента']"
          >
            <template #no-option>
              <q-item>
                <q-item-section class="text-grey">
                  Студенты не найдены
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <div
            v-if="selectedStudentData"
            class="student-preview"
          >
            <div>
              <b>ФИО:</b>
              {{ selectedStudentData.fio }}
            </div>

            <div>
              <b>Факультет:</b>
              {{ facultyLabel(selectedStudentData.facultyId) }}
            </div>

            <div>
              <b>Курс:</b>
              {{ selectedStudentData.course || '—' }}
            </div>

            <div>
              <b>Группа:</b>
              {{ selectedStudentData.groupName || '—' }}
            </div>

            <div class="text-caption text-grey-7 q-mt-xs">
              Данные обучающегося взяты из Кампус БГПУ
            </div>
          </div>

          <q-banner
            v-if="!studentOptions.length"
            rounded
            class="bg-orange-1 text-brown-8"
          >
            Пока нет студентов, доступных для выбора. Студент появится в списке после первой созданной заявки.
          </q-banner>

          <div class="section-title q-mt-md">
            Параметры справки
          </div>

          <q-select
            v-model="form.certificateType"
            :options="certificateTypeOptions"
            label="Тип справки"
            outlined
            emit-value
            map-options
            :rules="[val => !!val || 'Выберите тип справки']"
          />

          <template v-if="form.certificateType === 'WITH_STIPEND'">
            <q-banner rounded class="stipend-banner">
              Для справки со стипендией выберите период по уже завершённым месяцам.
            </q-banner>

            <div class="period-grid">
              <q-select
                v-model="periodFromYear"
                :options="closedYearOptions"
                label="Год с"
                outlined
                emit-value
                map-options
                :rules="[val => !!val || 'Выберите год начала периода']"
              />

              <q-select
                v-model="periodFromMonth"
                :options="availableFromMonthOptions"
                label="Месяц с"
                outlined
                emit-value
                map-options
                :rules="[val => !!val || 'Выберите месяц начала периода']"
              />

              <q-select
                v-model="periodToYear"
                :options="closedYearOptions"
                label="Год по"
                outlined
                emit-value
                map-options
                :rules="[val => !!val || 'Выберите год конца периода']"
              />

              <q-select
                v-model="periodToMonth"
                :options="availableToMonthOptions"
                label="Месяц по"
                outlined
                emit-value
                map-options
                :rules="[
                  val => !!val || 'Выберите месяц конца периода',
                  validatePeriodOrder
                ]"
              />
            </div>
          </template>

          <q-select
            v-model="form.purpose"
            :options="purposeOptions"
            label="Куда требуется справка"
            outlined
            use-input
            fill-input
            hide-selected
            input-debounce="0"
            :rules="[val => !!val || 'Выберите место предоставления справки']"
          />

          <q-input
            v-model.number="form.copiesCount"
            type="number"
            label="Количество экземпляров"
            outlined
            min="1"
            max="10"
            :rules="[
              val => !!val || 'Укажите количество экземпляров',
              val => Number(val) >= 1 || 'Минимум 1 экземпляр',
              val => Number(val) <= 10 || 'Максимум 10 экземпляров'
            ]"
          />

          <q-toggle
            v-model="form.needScan"
            label="Нужен скан"
            color="primary"
          />

          <q-input
            v-model="form.studentComment"
            type="textarea"
            label="Комментарий"
            outlined
            autogrow
          />

          <div class="actions-row">
            <q-btn
              flat
              color="grey-8"
              label="Отмена"
              @click="goBack"
            />

            <q-btn
              unelevated
              color="primary"
              label="Создать заявку"
              type="submit"
              :loading="saving"
            />
          </div>
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'stores/auth'
import { createRequest, getRequests } from 'src/api/requests'
import { getFaculties } from 'src/api/faculties'
import { getAccessAccounts } from 'src/api/accessAccounts'

const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()

const loading = ref(false)
const saving = ref(false)

const requests = ref([])
const faculties = ref([])
const accessRows = ref([])

const selectedStudentId = ref(null)
const studentSearch = ref('')

const periodFromYear = ref(null)
const periodFromMonth = ref(null)
const periodToYear = ref(null)
const periodToMonth = ref(null)

const form = reactive({
  certificateType: 'NO_STIPEND',
  purpose: '',
  copiesCount: 1,
  needScan: false,
  studentComment: '',
  periodFrom: null,
  periodTo: null
})

const certificateTypeOptions = [
  {
    label: 'Без отметки о стипендии',
    value: 'NO_STIPEND'
  },
  {
    label: 'Со стипендией',
    value: 'WITH_STIPEND'
  }
]
const purposeOptions = [
  'В отдел субсидий',
  'В военный комиссариат',
  'В отдел социальной защиты',
  'В фонд пенсионного и социального страхования Российской Федерации',
  'По месту работы родителей',
  'В налоговую инспекцию',
  'По месту работы обучающегося',
  'По месту требования',
  'В суд'
]
const monthNames = [
  { label: 'Январь', value: 1 },
  { label: 'Февраль', value: 2 },
  { label: 'Март', value: 3 },
  { label: 'Апрель', value: 4 },
  { label: 'Май', value: 5 },
  { label: 'Июнь', value: 6 },
  { label: 'Июль', value: 7 },
  { label: 'Август', value: 8 },
  { label: 'Сентябрь', value: 9 },
  { label: 'Октябрь', value: 10 },
  { label: 'Ноябрь', value: 11 },
  { label: 'Декабрь', value: 12 }
]

const closedMonthPairs = computed(() => {
  const result = []
  const now = new Date()

  const lastClosed = new Date(now.getFullYear(), now.getMonth(), 0)

  for (let i = 0; i < 60; i++) {
    const d = new Date(lastClosed.getFullYear(), lastClosed.getMonth() - i, 1)

    result.push({
      year: d.getFullYear(),
      month: d.getMonth() + 1
    })
  }

  return result
})

const closedYearOptions = computed(() => {
  const years = [...new Set(closedMonthPairs.value.map(item => item.year))]

  return years
    .sort((a, b) => b - a)
    .map(year => ({
      label: String(year),
      value: year
    }))
})

const availableFromMonthOptions = computed(() => {
  if (!periodFromYear.value) return []

  const months = closedMonthPairs.value
    .filter(item => item.year === periodFromYear.value)
    .map(item => item.month)

  return monthNames.filter(m => months.includes(m.value))
})

const availableToMonthOptions = computed(() => {
  if (!periodToYear.value) return []

  const months = closedMonthPairs.value
    .filter(item => item.year === periodToYear.value)
    .map(item => item.month)

  return monthNames.filter(m => months.includes(m.value))
})

const currentAccess = computed(() => {
  if (auth.role === 'ADMIN') {
    return {
      login: auth.login || 'admin',
      role: 'ADMIN',
      facultyIds: [],
      active: true
    }
  }

  if (auth.role !== 'SECRETARY') {
    return null
  }

  const login = auth.login || ''

  const account = accessRows.value.find(row =>
    row.login?.toLowerCase() === login.toLowerCase()
  )

  if (account) {
    return account
  }

  return {
    login,
    role: 'SECRETARY',
    facultyIds: fallbackFacultyIdsFromAuth(),
    active: true
  }
})

const availableFacultyIds = computed(() => {
  if (!currentAccess.value) return []

  if (currentAccess.value.role === 'ADMIN') {
    return faculties.value.map(faculty => faculty.id)
  }

  return currentAccess.value.facultyIds || []
})

const students = computed(() => {
  const map = new Map()

  requests.value.forEach(request => {
    if (!request.studentFullName) return
    if (!request.facultyId) return

    if (!availableFacultyIds.value.includes(request.facultyId)) return

    const key = [
      request.studentFullName,
      request.groupName || '',
      request.course || '',
      request.facultyId || ''
    ].join('|')

    if (!map.has(key)) {
      map.set(key, {
        id: key,
        fio: request.studentFullName,
        groupName: request.groupName || '',
        course: request.course || null,
        facultyId: request.facultyId
      })
    }
  })

  return Array.from(map.values()).sort((a, b) =>
    a.fio.localeCompare(b.fio)
  )
})

const studentOptions = computed(() =>
  students.value.map(student => ({
    label: `${student.fio} — ${student.groupName || 'без группы'}`,
    value: student.id
  }))
)

const filteredStudentOptions = computed(() => {
  const q = studentSearch.value.trim().toLowerCase()

  if (!q) return studentOptions.value

  return studentOptions.value.filter(option =>
    option.label.toLowerCase().includes(q)
  )
})

const selectedStudentData = computed(() =>
  students.value.find(student => student.id === selectedStudentId.value)
)

watch(
  () => form.certificateType,
  (type) => {
    if (type !== 'WITH_STIPEND') {
      periodFromYear.value = null
      periodFromMonth.value = null
      periodToYear.value = null
      periodToMonth.value = null
      form.periodFrom = null
      form.periodTo = null
    }
  }
)

watch([periodFromYear, periodFromMonth, periodToYear, periodToMonth], () => {
  if (form.certificateType !== 'WITH_STIPEND') {
    form.periodFrom = null
    form.periodTo = null
    return
  }

  if (periodFromYear.value && periodFromMonth.value) {
    form.periodFrom = getMonthStart(periodFromYear.value, periodFromMonth.value)
  } else {
    form.periodFrom = null
  }

  if (periodToYear.value && periodToMonth.value) {
    form.periodTo = getMonthEnd(periodToYear.value, periodToMonth.value)
  } else {
    form.periodTo = null
  }
})

watch(periodFromYear, () => {
  periodFromMonth.value = null
})

watch(periodToYear, () => {
  periodToMonth.value = null
})

function filterStudents(val, update) {
  update(() => {
    studentSearch.value = val
  })
}

function getMonthStart(year, month) {
  return new Date(year, month - 1, 1).toISOString().slice(0, 10)
}

function getMonthEnd(year, month) {
  return new Date(year, month, 0).toISOString().slice(0, 10)
}

function validatePeriodOrder() {
  if (form.certificateType !== 'WITH_STIPEND') return true

  if (!periodFromYear.value || !periodFromMonth.value || !periodToYear.value || !periodToMonth.value) {
    return true
  }

  const from = new Date(periodFromYear.value, periodFromMonth.value - 1, 1)
  const to = new Date(periodToYear.value, periodToMonth.value - 1, 1)

  if (from > to) {
    return 'Период "с" не должен быть позже периода "по"'
  }

  return true
}

function facultyLabel(facultyId) {
  const faculty = faculties.value.find(f => f.id === facultyId)

  if (!faculty) return '—'

  return faculty.code
    ? `${faculty.code} — ${faculty.name}`
    : faculty.name
}

function fallbackFacultyIdsFromAuth() {
  if (!auth.facultyId) return []

  const authFaculty = String(auth.facultyId).trim().toLowerCase()

  const faculty = faculties.value.find(faculty =>
    String(faculty.id) === authFaculty ||
    String(faculty.code || '').toLowerCase() === authFaculty ||
    `f${String(faculty.id).padStart(2, '0')}`.toLowerCase() === authFaculty
  )

  return faculty ? [faculty.id] : []
}

function goBack() {
  if (auth.role === 'SECRETARY') {
    router.push('/secretary')
    return
  }

  router.push('/admin')
}

async function loadData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      requestsResponse,
      accessAccountsResponse
    ] = await Promise.all([
      getFaculties(),
      getRequests(),
      getAccessAccounts()
    ])

    faculties.value = facultiesResponse.data.filter(item => item.isActive !== false)
    requests.value = requestsResponse.data

    accessRows.value = accessAccountsResponse.data.map(row => ({
      id: row.id,
      login: row.login,
      fio: row.fullName,
      role: row.role,
      facultyIds: row.facultyIds || [],
      active: row.isActive !== false
    }))
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось загрузить данные'
    })
  } finally {
    loading.value = false
  }
}

async function submitForm() {
  if (!selectedStudentData.value) {
    $q.notify({
      type: 'negative',
      message: 'Выберите студента'
    })

    return
  }

  if (auth.role === 'SECRETARY' && !availableFacultyIds.value.includes(selectedStudentData.value.facultyId)) {
    $q.notify({
      type: 'negative',
      message: 'У вас нет доступа к факультету выбранного студента'
    })

    return
  }

  if (form.certificateType === 'WITH_STIPEND') {
    if (!periodFromYear.value || !periodFromMonth.value || !periodToYear.value || !periodToMonth.value) {
      $q.notify({
        type: 'negative',
        message: 'Укажите период для справки со стипендией'
      })

      return
    }

    const periodCheck = validatePeriodOrder()

    if (periodCheck !== true) {
      $q.notify({
        type: 'negative',
        message: periodCheck
      })

      return
    }
  }

  saving.value = true

  try {
    await createRequest({
      facultyId: selectedStudentData.value.facultyId,
      certificateType: form.certificateType,
      purpose: form.purpose,
      copiesCount: form.copiesCount,
      periodFrom: form.periodFrom,
      periodTo: form.periodTo,
      needScan: form.needScan,
      status: 'NEW',
      studentComment: form.studentComment,
      secretaryComment: '',
      studentFullName: selectedStudentData.value.fio,
      groupName: selectedStudentData.value.groupName,
      course: selectedStudentData.value.course,
      registrationNumber: null,
      registrationYear: null,
      registeredAt: null,
      issuedAt: null,
      acceptedAt: null,
      completedAt: null,
      archivedAt: null,
      isDeleted: false
    })

    $q.notify({
      type: 'positive',
      message: 'Заявка успешно создана'
    })

    goBack()
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось создать заявку'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-bg {
  background: #f7f7f8;
  min-height: 100%;
}

.page-header {
  padding: 4px 2px 0;
}

.main-card {
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(24, 39, 75, 0.08);
  background: #ffffff;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.student-preview {
  background: #f4f4f5;
  padding: 16px;
  border-radius: 12px;

  display: flex;
  flex-direction: column;
  gap: 8px;
}

.period-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stipend-banner {
  background: #fff3e8;
  color: #c24e00;
  border: 1px solid #ffd7b8;
}

.actions-row {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}

.actions-row .q-btn:last-child {
  min-width: 220px;
  border-radius: 12px;
  background: #8b0015 !important;
  color: white !important;
}

@media (max-width: 700px) {
  .period-grid {
    grid-template-columns: 1fr;
  }

  .actions-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .actions-row .q-btn {
    width: 100%;
  }
}
</style>
