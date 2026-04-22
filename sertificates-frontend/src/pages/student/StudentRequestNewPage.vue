<template>
  <q-page class="q-pa-md">
    <div class="text-h6 q-mb-md">Заказ справки</div>

    <q-card class="q-pa-md card">
      <!-- 1) Тип справки -->
      <q-select
        v-model="form.certificateType"
        :options="certificateTypeOptions"
        label="Тип справки"
        outlined
        emit-value
        map-options
        class="q-mb-md"
      />

      <!-- 3) Куда нужна справка -->
      <q-select
        v-model="form.purposeId"
        :options="purposeOptions"
        label="Куда нужна справка"
        outlined
        emit-value
        map-options
        class="q-mb-md"
      />

      <!-- 2) Период (если с отметкой) -->
      <div v-if="isStipend" class="q-mb-md">
        <div class="text-subtitle2 q-mb-sm">Период (для справки со стипендией)</div>

        <!-- Выбор "С" -->
        <div class="text-caption text-grey-7 q-mb-xs">С</div>
        <div class="row q-col-gutter-sm">
          <div class="col-12 col-sm-6">
            <q-select
              v-model="form.fromMonth"
              :options="monthOptions"
              label="Месяц"
              outlined
              emit-value
              map-options
            />
          </div>
          <div class="col-12 col-sm-6">
            <q-select
              v-model="form.fromYear"
              :options="yearOptions"
              label="Год"
              outlined
              emit-value
              map-options
            />
          </div>
        </div>

        <div class="row q-col-gutter-sm q-mt-sm q-mb-md">
          <div class="col-12">
            <q-input v-model="form.dateFrom" label="Дата 'с' (авто)" outlined readonly />
          </div>
        </div>

        <!-- Выбор "ПО" -->
        <div class="text-caption text-grey-7 q-mb-xs">По</div>
        <div class="row q-col-gutter-sm">
          <div class="col-12 col-sm-6">
            <q-select
              v-model="form.toMonth"
              :options="toMonthOptions"
              label="Месяц"
              outlined
              emit-value
              map-options
              :disable="!form.toYear"
              hint="Нельзя выбрать текущий/будущий месяц"
              persistent-hint
            />
          </div>
          <div class="col-12 col-sm-6">
            <q-select
              v-model="form.toYear"
              :options="toYearOptions"
              label="Год"
              outlined
              emit-value
              map-options
              hint="До предыдущего месяца включительно"
              persistent-hint
            />
          </div>
        </div>

        <div class="row q-col-gutter-sm q-mt-sm">
          <div class="col-12">
            <q-input v-model="form.dateTo" label="Дата 'по' (авто)" outlined readonly />
          </div>
        </div>

        <div class="text-caption text-grey-7 q-mt-sm">
          Даты подставляются автоматически: «с» — 1-е число выбранного месяца, «по» — последний день выбранного месяца.
        </div>
      </div>

      <!-- 4) Количество -->
      <q-input
        v-model.number="form.quantity"
        type="number"
        label="Количество справок (1–10)"
        outlined
        min="1"
        max="10"
        class="q-mb-lg"
      />

      <q-btn
        unelevated
        color="positive"
        class="full-width"
        size="lg"
        label="Отправить заявку"
        :loading="sending"
        @click="submit"
      />
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'

const $q = useQuasar()
const router = useRouter()

// Дата зачисления студента.
// Пока мок. Потом будет подтягиваться из Кампуса / API.
const admissionDate = '01.09.2024'

// --- Справочники (пока моки, потом заменим на API) ---
const certificateTypeOptions = [
  { label: 'Без отметки о стипендии', value: 'NO_STIPEND' },
  { label: 'С отметкой о стипендии', value: 'WITH_STIPEND' }
]

// "Куда нужна справка" (purpose). Сейчас моки.
// Позже: GET /api/purposes
const purposeOptions = [
  { label: 'По месту требования', value: 1 },
  { label: 'В отдел социальной защиты', value: 2 },
  { label: 'В пенсионный фонд', value: 3 }
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

// --- Ограничение "по": нельзя текущий/будущий месяц ---
// максимум допустимый месяц = предыдущий относительно текущей даты
const now = new Date()
const maxTo = computed(() => {
  const y = now.getFullYear()
  const m = now.getMonth() + 1 // 1..12
  // предыдущий месяц
  if (m === 1) return { year: y - 1, month: 12 }
  return { year: y, month: m - 1 }
})

function pad2 (n) {
  return String(n).padStart(2, '0')
}

function lastDayOfMonth (year, month) {
  // month: 1..12; day 0 следующего месяца = последний день текущего
  return new Date(year, month, 0).getDate()
}

function monthIndex (year, month) {
  // удобная “линейная” шкала, чтобы сравнивать
  return year * 12 + (month - 1)
}

function parseRuDate (dateStr) {
  const [dd, mm, yyyy] = dateStr.split('.').map(Number)
  return new Date(yyyy, mm - 1, dd)
}

// --- Опции годов ---
// диапазон “вокруг” (можно расширить)
const currentYear = now.getFullYear()
const yearOptions = Array.from({ length: 12 }, (_, i) => {
  const y = currentYear - 6 + i
  return { label: String(y), value: y }
})

// Для "ПО" ограничим годы до maxTo.year
const toYearOptions = computed(() =>
  yearOptions.filter(o => o.value <= maxTo.value.year)
)

// --- Форма ---
const form = ref({
  certificateType: 'NO_STIPEND',
  purposeId: null,

  // период (месяц/год) отдельно для "с" и "по"
  fromMonth: null,
  fromYear: null,
  toMonth: null,
  toYear: null,

  // авто-даты
  dateFrom: '',
  dateTo: '',

  quantity: 1
})

const isStipend = computed(() => form.value.certificateType === 'WITH_STIPEND')

// Для "ПО" ограничим месяцы, если год == maxTo.year
const toMonthOptions = computed(() => {
  const y = form.value.toYear
  if (!y) return monthOptions

  if (y < maxTo.value.year) return monthOptions
  // y == maxTo.year: разрешаем только до maxTo.month
  return monthOptions.filter(m => m.value <= maxTo.value.month)
})

// Автоподстановка dateFrom
watch(
  () => [form.value.fromMonth, form.value.fromYear, form.value.certificateType],
  () => {
    if (!isStipend.value) {
      form.value.fromMonth = null
      form.value.fromYear = null
      form.value.dateFrom = ''
      return
    }

    const m = form.value.fromMonth
    const y = form.value.fromYear
    if (!m || !y) {
      form.value.dateFrom = ''
      return
    }

    form.value.dateFrom = `01.${pad2(m)}.${y}`
  },
  { immediate: true }
)

// Автоподстановка dateTo + принудительное “срезание” запрещённых значений
watch(
  () => [form.value.toMonth, form.value.toYear, form.value.certificateType],
  () => {
    if (!isStipend.value) {
      form.value.toMonth = null
      form.value.toYear = null
      form.value.dateTo = ''
      return
    }

    const m = form.value.toMonth
    const y = form.value.toYear
    if (!m || !y) {
      form.value.dateTo = ''
      return
    }

    // если выбрали запрещённый "по" (текущий/будущий) — откатываем
    const chosen = monthIndex(y, m)
    const maxAllowed = monthIndex(maxTo.value.year, maxTo.value.month)
    if (chosen > maxAllowed) {
      // сбросим месяц (попросим выбрать корректный)
      form.value.toMonth = null
      form.value.dateTo = ''
      return
    }

    const last = lastDayOfMonth(y, m)
    form.value.dateTo = `${pad2(last)}.${pad2(m)}.${y}`
  },
  { immediate: true }
)

// Если пользователь поменял toYear, а текущий toMonth стал недопустим — сбрасываем month
watch(
  () => form.value.toYear,
  () => {
    if (!form.value.toYear || !form.value.toMonth) return
    const allowedMonths = toMonthOptions.value.map(o => o.value)
    if (!allowedMonths.includes(form.value.toMonth)) {
      form.value.toMonth = null
    }
  }
)

const sending = ref(false)

function notifyError (message) {
  $q.notify({
    type: 'negative',
    message,
    position: 'top',
    timeout: 2500
  })
}

function notifyOk (message) {
  $q.notify({
    type: 'positive',
    message,
    position: 'top',
    timeout: 2000
  })
}

async function submit () {
  // куда нужна справка
  if (!form.value.purposeId) {
    notifyError('Выберите, куда нужна справка.')
    return
  }

  // количество 1..10
  if (!Number.isFinite(form.value.quantity) || form.value.quantity < 1 || form.value.quantity > 10) {
    notifyError('Количество справок должно быть от 1 до 10.')
    return
  }

  // период обязателен только для справки со стипендией
  if (isStipend.value) {
    if (!form.value.fromMonth || !form.value.fromYear) {
      notifyError('Выберите месяц и год для даты "с".')
      return
    }
    if (!form.value.toMonth || !form.value.toYear) {
      notifyError('Выберите месяц и год для даты "по".')
      return
    }

    const fromIdx = monthIndex(form.value.fromYear, form.value.fromMonth)
    const toIdx = monthIndex(form.value.toYear, form.value.toMonth)
    if (fromIdx > toIdx) {
      notifyError('Период задан неверно: "с" не может быть позже "по".')
      return
    }

    // доп. защита на "по" (на всякий)
    const maxAllowed = monthIndex(maxTo.value.year, maxTo.value.month)
    if (toIdx > maxAllowed) {
      notifyError('Нельзя выбрать месяц "по", который ещё не закончился.')
      return
    }

    // НОВОЕ ПРАВИЛО:
    // дата "с" должна быть позже или в день зачисления
    const fromDate = parseRuDate(form.value.dateFrom)
    const studentAdmissionDate = parseRuDate(admissionDate)

    if (fromDate < studentAdmissionDate) {
      notifyError(`Дата "с" не может быть раньше даты зачисления (${admissionDate}).`)
      return
    }
  }

  sending.value = true
  try {
    // Пока мок отправки (потом будет POST /api/requests)
    await new Promise((r) => setTimeout(r, 500))

    notifyOk('Заявка успешно отправлена!')
    router.push('/student')
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.card {
  border-radius: 14px;
}
</style>
