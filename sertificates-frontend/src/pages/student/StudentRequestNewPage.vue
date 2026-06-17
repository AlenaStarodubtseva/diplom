<template>
  <q-page class="q-pa-md page-bg">
    <div class="page-header q-mb-md row items-center justify-between">
      <div>
        <div class="text-h5 text-weight-medium">Новая заявка</div>
        <div class="text-caption text-grey-7 q-mt-xs">
          Заполните форму для оформления справки
        </div>
      </div>

      <q-btn
        flat
        color="grey-8"
        icon="arrow_back"
        label="Назад"
        @click="router.push('/student')"
      />
    </div>

    <q-card flat bordered class="student-data-card q-mb-md">
      <q-card-section>
        <div class="text-subtitle1 text-weight-bold q-mb-md">
          Данные обучающегося
        </div>

        <div class="student-data-grid">
          <div>
            <div class="student-field-label">ФИО</div>
            <div class="student-field-value">
              {{ studentMock.fullName }}
            </div>
          </div>

          <div>
            <div class="student-field-label">Курс / группа</div>
            <div class="student-field-value">
              {{ studentMock.course }} курс / {{ studentMock.groupName }}
            </div>
          </div>

          <div>
            <div class="student-field-label">Факультет</div>
            <div class="student-field-value">
              {{ studentMock.facultyShortName }}
            </div>
          </div>
        </div>

        <div class="text-caption text-grey-6 q-mt-md">
          Данные автоматически получены из Кампус БГПУ
        </div>
      </q-card-section>
    </q-card>

    <q-card class="main-card" flat>
      <q-card-section class="q-pa-lg">
        <q-form @submit="submitForm" class="form-grid">
          <q-select
            v-model="form.certificateType"
            :options="certificateTypeOptions"
            label="Тип справки"
            outlined
            emit-value
            map-options
            color="dark"
            class="form-field"
            :rules="[val => !!val || 'Выберите тип справки']"
          />

          <q-select
            v-model="form.purpose"
            :options="purposeOptions"
            label="Куда требуется справка"
            outlined
            color="dark"
            class="form-field"
            :rules="[val => !!val || 'Выберите место предоставления']"
          />

          <q-input
            v-model.number="form.copiesCount"
            type="number"
            label="Количество экземпляров"
            outlined
            color="dark"
            class="form-field"
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
            label="Нужен скан справки"
            color="primary"
            class="q-mb-sm"
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
                color="dark"
                class="form-field"
                :rules="[val => !!val || 'Выберите год начала периода']"
              />

              <q-select
                v-model="periodFromMonth"
                :options="availableFromMonthOptions"
                label="Месяц с"
                outlined
                emit-value
                map-options
                color="dark"
                class="form-field"
                :rules="[val => !!val || 'Выберите месяц начала периода']"
              />

              <q-select
                v-model="periodToYear"
                :options="closedYearOptions"
                label="Год по"
                outlined
                emit-value
                map-options
                color="dark"
                class="form-field"
                :rules="[val => !!val || 'Выберите год конца периода']"
              />

              <q-select
                v-model="periodToMonth"
                :options="availableToMonthOptions"
                label="Месяц по"
                outlined
                emit-value
                map-options
                color="dark"
                class="form-field"
                :rules="[
                  val => !!val || 'Выберите месяц конца периода',
                  validatePeriodOrder
                ]"
              />
            </div>

            <div
              v-if="form.periodFrom && form.periodTo"
              class="period-preview"
            >
              Период будет указан так:
              <b>{{ formatDisplayDate(form.periodFrom) }} — {{ formatDisplayDate(form.periodTo) }}</b>
            </div>
          </template>

          <q-input
            v-model="form.studentComment"
            type="textarea"
            label="Комментарий"
            outlined
            color="dark"
            autogrow
            class="form-field comment-field"
          />

          <div class="actions-row">
            <q-btn
              flat
              color="grey-8"
              label="Отмена"
              class="cancel-form-btn"
              @click="router.push('/student')"
            />

            <q-btn
              unelevated
              color="primary"
              label="Отправить заявку"
              type="submit"
              class="submit-form-btn"
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
import { createRequest } from 'src/api/requests'
import { getFaculties } from 'src/api/faculties'

const router = useRouter()
const $q = useQuasar()

const saving = ref(false)
const facultiesLoading = ref(false)

const selectedFaculty = ref(null)
const faculties = ref([])

const periodFromYear = ref(null)
const periodFromMonth = ref(null)
const periodToYear = ref(null)
const periodToMonth = ref(null)

const studentMock = {
  fullName: 'Стародубцева Алёна Константиновна',
  course: 4,
  groupName: '4ИС',
  facultyShortName: 'ФФМОиТ'
}

const certificateTypeOptions = [
  { label: 'Справка без отметки о стипендии', value: 'NO_STIPEND' },
  { label: 'Справка с отметкой о стипендии', value: 'WITH_STIPEND' }
]
const purposeOptions = [
  'В отдел субсидий',
  'В военный комиссариат',
  'В отдел социальной защиты',
  'В Фонд пенсионного и социального страхования Российской Федерации',
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

const form = reactive({
  facultyId: null,
  certificateType: 'NO_STIPEND',
  purpose: '',
  copiesCount: 1,
  periodFrom: null,
  periodTo: null,
  needScan: false,
  status: 'NEW',
  studentComment: '',
  secretaryComment: '',
  studentFullName: studentMock.fullName,
  groupName: studentMock.groupName,
  course: studentMock.course,
  facultyName: studentMock.facultyShortName,
  registrationNumber: null,
  registrationYear: null,
  registeredAt: null,
  issuedAt: null,
  acceptedAt: null,
  completedAt: null,
  archivedAt: null,
  isDeleted: false
})

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

watch(selectedFaculty, (facultyId) => {
  const found = faculties.value.find(f => f.id === facultyId)

  form.facultyId = facultyId || null
  form.facultyName = found?.name || studentMock.facultyShortName
})

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

function validatePeriodOrder() {
  if (form.certificateType !== 'WITH_STIPEND') return true
  if (!periodFromYear.value || !periodFromMonth.value || !periodToYear.value || !periodToMonth.value) return true

  const from = new Date(periodFromYear.value, periodFromMonth.value - 1, 1)
  const to = new Date(periodToYear.value, periodToMonth.value - 1, 1)

  if (from > to) {
    return 'Период "с" не может быть позже периода "по"'
  }

  return true
}

function getMonthStart(year, month) {
  return `${year}-${pad(month)}-01`
}

function getMonthEnd(year, month) {
  const lastDay = new Date(year, month, 0).getDate()

  return `${year}-${pad(month)}-${pad(lastDay)}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDisplayDate(value) {
  if (!value) return '—'

  const [year, month, day] = value.split('-')

  return `${day}.${month}.${year}`
}

async function loadFaculties() {
  facultiesLoading.value = true

  try {
    const { data } = await getFaculties()

    faculties.value = data.filter(item => item.isActive !== false)

    const foundFaculty = faculties.value.find(item => {
      const code = String(item.code || '').toLowerCase()
      const name = String(item.name || '').toLowerCase()

      return (
        code === 'f01' ||
        code === '01' ||
        name.includes('ффмоит') ||
        name.includes('физико-математ')
      )
    })

    if (foundFaculty) {
      selectedFaculty.value = foundFaculty.id
      form.facultyId = foundFaculty.id
      form.facultyName = foundFaculty.name
    }
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось загрузить факультеты'
    })
  } finally {
    facultiesLoading.value = false
  }
}

async function submitForm() {
  form.studentFullName = studentMock.fullName
  form.groupName = studentMock.groupName
  form.course = studentMock.course

  if (!form.facultyId) {
    $q.notify({
      type: 'negative',
      message: 'Не удалось определить факультет обучающегося'
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
    await createRequest({ ...form })

    $q.notify({
      type: 'positive',
      message: 'Заявка успешно создана'
    })

    router.push('/student')
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
  loadFaculties()
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

.student-data-card {
  border-radius: 20px;
  background: #f4f4f5;
  border: 1px solid #dddddd;
}

.student-data-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  gap: 24px;
}

.student-field-label {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 8px;
}

.student-field-value {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
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

.period-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.period-preview {
  background: #f4f4f5;
  border: 1px solid #dddddd;
  border-radius: 14px;
  padding: 12px 14px;
  color: #374151;
}

.form-field :deep(.q-field__control) {
  border-radius: 14px;
}

.form-field :deep(.q-field__control:before) {
  border: 1px solid #d1d5db !important;
}

.form-field :deep(.q-field__control:hover:before) {
  border-color: #8b0015 !important;
}

.form-field :deep(.q-field--focused .q-field__control:before) {
  border: 2px solid #8b0015 !important;
}

.form-field :deep(.q-field--focused .q-field__control:after) {
  border: 2px solid #8b0015 !important;
}

.form-field :deep(.q-field--focused .q-field__control) {
  box-shadow: 0 0 0 2px rgba(139, 0, 21, 0.12) !important;
}

.form-field :deep(textarea:focus),
.form-field :deep(.q-field__native:focus) {
  outline: none !important;
  box-shadow: none !important;
}

.form-field :deep(textarea),
.form-field :deep(.q-field__native) {
  caret-color: #8b0015;
}

.comment-field :deep(.q-field__control) {
  min-height: 110px;
}

.stipend-banner {
  background: #fff3e8;
  color: #c24e00;
  border: 1px solid #ffd7b8;
}

.actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.cancel-form-btn {
  min-width: 140px;
  border-radius: 12px;
}

.submit-form-btn {
  min-width: 240px;
  border-radius: 14px;
  font-weight: 600;
  letter-spacing: 0.2px;
  background: #8b0015 !important;
  color: #ffffff !important;
}

.submit-form-btn:hover {
  background: #a3001b !important;
}

@media (max-width: 900px) {
  .student-data-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .period-grid {
    grid-template-columns: 1fr;
  }

  .actions-row {
    flex-direction: column;
    align-items: stretch;
  }

  .cancel-form-btn,
  .submit-form-btn {
    width: 100%;
  }
}
</style>
