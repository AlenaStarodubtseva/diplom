<template>
  <q-page class="q-pa-md">
    <div class="row items-center q-mb-md">
      <div class="text-h6">Создание заявки вручную</div>
      <q-space />
      <q-btn flat icon="arrow_back" class="campus-accent" label="Назад" @click="goBack" />
    </div>

    <q-card class="card q-pa-md">
      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-6">
          <q-input v-model="form.fio" outlined label="ФИО студента" />
        </div>

        <div class="col-12 col-md-3">
          <q-input v-model="form.courseGroup" outlined label="Курс / группа" />
        </div>

        <div class="col-12 col-md-3">
          <q-select
            v-if="auth.role === 'ADMIN'"
            v-model="form.facultyId"
            :options="facultyOptions"
            outlined
            emit-value
            map-options
            label="Факультет"
          />
          <q-input
            v-else
            :model-value="auth.facultyId"
            outlined
            label="Факультет"
            readonly
          />
        </div>

        <div class="col-12 col-md-4">
          <q-input
            v-model="form.admissionDate"
            outlined
            label="Дата зачисления"
            mask="##.##.####"
            placeholder="ДД.ММ.ГГГГ"
            hint="Потом будет подставляться автоматически"
            persistent-hint
          />
        </div>

        <div class="col-12 col-md-4">
          <q-select
            v-model="form.type"
            :options="typeOptions"
            outlined
            emit-value
            map-options
            label="Тип справки"
          />
        </div>

        <div class="col-12 col-md-4">
          <q-select
            v-model="form.purpose"
            :options="purposeOptions"
            outlined
            emit-value
            map-options
            label="Куда нужна справка"
          />
        </div>

        <div class="col-12 col-md-3">
          <q-input
            v-model.number="form.qty"
            type="number"
            min="1"
            max="10"
            outlined
            label="Количество (1–10)"
          />
        </div>

        <div class="col-12" v-if="form.type === 'WITH_STIPEND'">
          <div class="text-subtitle2 q-mb-sm">Период (для справки со стипендией)</div>

          <div class="text-caption text-grey-7 q-mb-xs">С</div>
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-md-3">
              <q-select
                v-model="form.fromMonth"
                :options="monthOptions"
                outlined
                emit-value
                map-options
                label="Месяц"
              />
            </div>
            <div class="col-12 col-md-3">
              <q-select
                v-model="form.fromYear"
                :options="yearOptions"
                outlined
                emit-value
                map-options
                label="Год"
              />
            </div>
            <div class="col-12 col-md-6">
              <q-input v-model="form.dateFrom" outlined label="Дата 'с' (авто)" readonly />
            </div>
          </div>

          <div class="text-caption text-grey-7 q-mt-md q-mb-xs">По</div>
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-md-3">
              <q-select
                v-model="form.toMonth"
                :options="toMonthOptions"
                outlined
                emit-value
                map-options
                label="Месяц"
                :disable="!form.toYear"
                hint="Нельзя выбрать текущий/будущий месяц"
                persistent-hint
              />
            </div>
            <div class="col-12 col-md-3">
              <q-select
                v-model="form.toYear"
                :options="toYearOptions"
                outlined
                emit-value
                map-options
                label="Год"
                hint="До предыдущего месяца включительно"
                persistent-hint
              />
            </div>
            <div class="col-12 col-md-6">
              <q-input v-model="form.dateTo" outlined label="Дата 'по' (авто)" readonly />
            </div>
          </div>
        </div>
      </div>

      <div class="q-mt-lg">
        <q-btn
          unelevated
          color="primary"
          label="Создать заявку"
          @click="submit"
        />
      </div>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()

const facultyOptions = [
  { label: 'F01', value: 'F01' },
  { label: 'F02', value: 'F02' },
  { label: 'F03', value: 'F03' }
]

const typeOptions = [
  { label: 'Без отметки о стипендии', value: 'NO_STIPEND' },
  { label: 'С отметкой о стипендии', value: 'WITH_STIPEND' }
]

const purposeOptions = [
  { label: 'По месту требования', value: 'По месту требования' },
  { label: 'В отдел социальной защиты', value: 'В отдел социальной защиты' },
  { label: 'В пенсионный фонд', value: 'В пенсионный фонд' }
]

const monthOptions = [
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

const now = new Date()
const maxTo = computed(() => {
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  if (m === 1) return { year: y - 1, month: 12 }
  return { year: y, month: m - 1 }
})

const currentYear = now.getFullYear()
const yearOptions = Array.from({ length: 12 }, (_, i) => {
  const y = currentYear - 6 + i
  return { label: String(y), value: y }
})

const toYearOptions = computed(() =>
  yearOptions.filter(o => o.value <= maxTo.value.year)
)

const form = reactive({
  fio: '',
  courseGroup: '',
  facultyId: auth.role === 'SECRETARY' ? auth.facultyId : null,
  admissionDate: '',
  type: 'NO_STIPEND',
  purpose: null,
  qty: 1,

  fromMonth: null,
  fromYear: null,
  toMonth: null,
  toYear: null,
  dateFrom: '',
  dateTo: ''
})

const toMonthOptions = computed(() => {
  const y = form.toYear
  if (!y) return monthOptions
  if (y < maxTo.value.year) return monthOptions
  return monthOptions.filter(m => m.value <= maxTo.value.month)
})

watch(
  () => [form.fromMonth, form.fromYear, form.type],
  () => {
    if (form.type !== 'WITH_STIPEND') {
      form.fromMonth = null
      form.fromYear = null
      form.dateFrom = ''
      return
    }

    const m = form.fromMonth
    const y = form.fromYear
    if (!m || !y) {
      form.dateFrom = ''
      return
    }

    form.dateFrom = `01.${pad2(m)}.${y}`
  },
  { immediate: true }
)

watch(
  () => [form.toMonth, form.toYear, form.type],
  () => {
    if (form.type !== 'WITH_STIPEND') {
      form.toMonth = null
      form.toYear = null
      form.dateTo = ''
      return
    }

    const m = form.toMonth
    const y = form.toYear
    if (!m || !y) {
      form.dateTo = ''
      return
    }

    const chosen = monthIndex(y, m)
    const maxAllowed = monthIndex(maxTo.value.year, maxTo.value.month)

    if (chosen > maxAllowed) {
      form.toMonth = null
      form.dateTo = ''
      return
    }

    const last = lastDayOfMonth(y, m)
    form.dateTo = `${pad2(last)}.${pad2(m)}.${y}`
  },
  { immediate: true }
)

watch(
  () => form.toYear,
  () => {
    if (!form.toYear || !form.toMonth) return
    const allowedMonths = toMonthOptions.value.map(o => o.value)
    if (!allowedMonths.includes(form.toMonth)) {
      form.toMonth = null
    }
  }
)

function pad2 (n) {
  return String(n).padStart(2, '0')
}

function lastDayOfMonth (year, month) {
  return new Date(year, month, 0).getDate()
}

function monthIndex (year, month) {
  return year * 12 + (month - 1)
}

function parseRuDate (dateStr) {
  const [dd, mm, yyyy] = dateStr.split('.').map(Number)
  return new Date(yyyy, mm - 1, dd)
}

function isValidRuDate (dateStr) {
  if (!dateStr || dateStr.length !== 10) return false
  const parts = dateStr.split('.')
  if (parts.length !== 3) return false
  const [dd, mm, yyyy] = parts.map(Number)
  if (!dd || !mm || !yyyy) return false
  if (dd < 1 || dd > 31) return false
  if (mm < 1 || mm > 12) return false
  if (yyyy < 1900 || yyyy > 2100) return false
  return true
}

function goBack () {
  if (auth.role === 'ADMIN') {
    router.push('/admin')
  } else {
    router.push('/secretary')
  }
}

function notifyError (message) {
  $q.notify({
    type: 'negative',
    message,
    position: 'top',
    timeout: 2500
  })
}

function submit () {
  if (!form.fio.trim()) {
    notifyError('Введите ФИО.')
    return
  }

  if (!form.courseGroup.trim()) {
    notifyError('Введите курс / группу.')
    return
  }

  if (!form.facultyId) {
    notifyError('Выберите факультет.')
    return
  }

  if (!isValidRuDate(form.admissionDate)) {
    notifyError('Укажите корректную дату зачисления в формате ДД.ММ.ГГГГ.')
    return
  }

  if (!form.purpose) {
    notifyError('Выберите, куда нужна справка.')
    return
  }

  if (!Number.isFinite(form.qty) || form.qty < 1 || form.qty > 10) {
    notifyError('Количество должно быть от 1 до 10.')
    return
  }

  if (form.type === 'WITH_STIPEND') {
    if (!form.fromMonth || !form.fromYear) {
      notifyError('Выберите месяц и год для даты "с".')
      return
    }

    if (!form.toMonth || !form.toYear) {
      notifyError('Выберите месяц и год для даты "по".')
      return
    }

    const fromIdx = monthIndex(form.fromYear, form.fromMonth)
    const toIdx = monthIndex(form.toYear, form.toMonth)
    const maxAllowed = monthIndex(maxTo.value.year, maxTo.value.month)

    if (fromIdx > toIdx) {
      notifyError('Период задан неверно: дата "с" не может быть позже даты "по".')
      return
    }

    if (toIdx > maxAllowed) {
      notifyError('Нельзя выбрать месяц "по", который ещё не закончился.')
      return
    }

    const fromDate = parseRuDate(form.dateFrom)
    const admissionDate = parseRuDate(form.admissionDate)

    if (fromDate < admissionDate) {
      notifyError(`Дата "с" не может быть раньше даты зачисления (${form.admissionDate}).`)
      return
    }
  }

  $q.notify({
    type: 'positive',
    message: 'Заявка создана вручную (мок).',
    position: 'top'
  })

  goBack()
}
</script>

<style scoped>
.card {
  border-radius: 14px;
}

.campus-accent {
  color: #7a0019;
}
</style>
