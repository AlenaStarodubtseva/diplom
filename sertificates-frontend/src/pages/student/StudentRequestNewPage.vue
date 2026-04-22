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

          <q-input
            v-model="form.purpose"
            label="Куда требуется справка"
            outlined
            color="dark"
            class="form-field"
            :rules="[val => !!val || 'Укажите цель']"
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

          <q-input
            v-model="form.studentFullName"
            label="ФИО"
            outlined
            color="dark"
            class="form-field"
            :rules="[
              val => !!val || 'Укажите ФИО',
              validateFullName
            ]"
          />

          <q-input
            v-model="form.groupName"
            label="Группа"
            outlined
            color="dark"
            class="form-field"
            :rules="[val => !!val || 'Укажите группу']"
          />

          <q-select
            v-model="form.course"
            :options="courseOptions"
            label="Курс"
            outlined
            emit-value
            map-options
            color="dark"
            class="form-field"
            :rules="[val => !!val || 'Выберите курс']"
          />

          <q-select
            v-model="selectedFaculty"
            :options="facultyOptions"
            label="Факультет"
            outlined
            emit-value
            map-options
            color="dark"
            class="form-field"
            :loading="facultiesLoading"
            :rules="[val => !!val || 'Выберите факультет']"
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

const certificateTypeOptions = [
  { label: 'Справка без отметки о стипендии', value: 'NO_STIPEND' },
  { label: 'Справка с отметкой о стипендии', value: 'WITH_STIPEND' }
]

const courseOptions = [
  { label: '1 курс', value: 1 },
  { label: '2 курс', value: 2 },
  { label: '3 курс', value: 3 },
  { label: '4 курс', value: 4 },
  { label: '5 курс', value: 5 }
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
  studentFullName: '',
  groupName: '',
  course: null,
  facultyName: '',
  registrationNumber: null,
  registrationYear: null,
  registeredAt: null,
  issuedAt: null,
  acceptedAt: null,
  completedAt: null,
  archivedAt: null,
  isDeleted: false
})

const facultyOptions = computed(() =>
  faculties.value.map(item => ({
    label: item.name,
    value: item.id
  }))
)

const closedMonthPairs = computed(() => {
  const result = []
  const now = new Date()

  // последний завершённый месяц
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
  form.facultyName = found?.name || ''
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

function validateFullName(val) {
  if (!val) return 'Укажите ФИО'

  const words = val
    .trim()
    .split(/\s+/)
    .filter(Boolean)

  if (words.length < 2 || words.length > 4) {
    return 'ФИО должно содержать 2, 3 или 4 слова'
  }

  return true
}

function validatePeriodOrder() {
  if (form.certificateType !== 'WITH_STIPEND') return true
  if (!periodFromYear.value || !periodFromMonth.value || !periodToYear.value || !periodToMonth.value) return true

  const from = new Date(periodFromYear.value, periodFromMonth.value - 1, 1)
  const to = new Date(periodToYear.value, periodToMonth.value - 1, 1)

  if (from >= to) {
    return 'Период "с" должен быть раньше периода "по"'
  }

  return true
}

function getMonthStart(year, month) {
  return new Date(year, month - 1, 1).toISOString().slice(0, 10)
}

function getMonthEnd(year, month) {
  return new Date(year, month, 0).toISOString().slice(0, 10)
}

async function loadFaculties() {
  facultiesLoading.value = true

  try {
    const { data } = await getFaculties()
    faculties.value = data.filter(item => item.isActive !== false)
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
