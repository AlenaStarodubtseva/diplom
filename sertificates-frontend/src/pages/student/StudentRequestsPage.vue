<template>
  <q-page class="q-pa-md requests-page">
    <div class="row items-center q-mb-md page-head">
      <div>
        <div class="text-h6">Справки</div>
        <div class="text-grey-7 text-body2">
          Ваши заявки на получение справок
        </div>
      </div>

      <q-space />

      <q-btn
        unelevated
        class="create-btn"
        icon="add"
        label="Новая заявка"
        @click="$router.push('/student/new')"
      />
    </div>

    <div v-if="loading" class="q-mb-md text-grey-7">
      Загрузка...
    </div>

    <div v-else-if="error" class="q-mb-md text-negative">
      {{ error }}
    </div>

    <template v-else>
      <q-card
        v-for="request in requests"
        :key="request.id"
        flat
        bordered
        class="request-card q-mb-md"
        @click="$router.push(`/student/${request.id}`)"
      >
        <q-card-section class="request-section">
          <div class="status-line" :class="statusClass(request.status)"></div>

          <div class="request-content">
            <div class="row items-center q-gutter-sm">
              <div class="text-subtitle1 text-weight-medium">
                Заявка №{{ request.id }}
              </div>

              <q-chip
                dense
                :color="statusColor(request.status)"
                text-color="white"
              >
                {{ statusLabel(request.status) }}
              </q-chip>
            </div>

            <div class="text-caption text-grey-7 q-mt-xs">
              Подана: {{ formatDate(request.createdAt) }}
            </div>

            <div class="row q-col-gutter-sm q-mt-sm items-center">
              <div class="col-12 col-md-auto">
                <span class="field-label">Количество:</span>
                <span class="field-value">{{ request.copiesCount || 1 }}</span>
              </div>

              <div class="col-12 col-md-auto">
                <span class="field-label">Тип:</span>
                <span class="field-value">{{ typeLabel(request.certificateType) }}</span>
              </div>

              <div class="col-12 col-md-auto">
                <span class="field-label">Скан:</span>
                <span class="field-value">{{ request.needScan ? 'нужен' : 'не нужен' }}</span>
              </div>
            </div>

            <div class="q-mt-sm">
              <div v-if="request.registrationNumbers?.length" class="row q-gutter-xs">
                <q-chip
                  v-for="number in request.registrationNumbers"
                  :key="number.id"
                  dense
                  outline
                  class="registration-chip"
                >
                  {{ formatRegistrationNumber(number) }}
                </q-chip>
              </div>

              <q-chip
                v-else-if="request.registrationNumber"
                dense
                outline
                class="registration-chip"
              >
                {{ formatLegacyRegistration(request) }}
              </q-chip>

              <span v-else class="text-caption text-grey-6">
                Регистрационный номер пока не присвоен
              </span>
            </div>

            <div v-if="request.scanOriginalFileName" class="q-mt-sm">
              <q-chip dense outline class="scan-chip" icon="attach_file">
                Скан прикреплён
              </q-chip>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <div v-if="!requests.length" class="empty-state q-mt-md">
        <q-icon name="description" size="32px" class="text-grey-6" />
        <div class="text-grey-7 q-mt-sm">
          У вас пока нет заявок
        </div>
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getRequests } from 'src/api/requests'
import { getFaculties } from 'src/api/faculties'
import { getRegistrationNumbersByRequestIds } from 'src/api/requestRegistrationNumbers'

const requests = ref([])
const faculties = ref([])

const loading = ref(false)
const error = ref('')


const CURRENT_STUDENT_FULL_NAME = 'Стародубцева Алёна Константиновна'

function normalizeFullName(value) {
  return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ')
}


async function loadRequests() {
  loading.value = true
  error.value = ''

  try {
    const [
      requestsResponse,
      facultiesResponse
    ] = await Promise.all([
      getRequests(),
      getFaculties()
    ])

    faculties.value = facultiesResponse.data.map(faculty => ({
      id: faculty.id,
      code: faculty.code,
      name: faculty.name,
      active: faculty.isActive !== false
    }))

    const normalizedRequests = requestsResponse.data
      .filter(request =>
        normalizeFullName(request.studentFullName) === normalizeFullName(CURRENT_STUDENT_FULL_NAME)
      )
      .map(request => ({
        ...request,
        registrationNumbers: []
      }))

    const requestIds = normalizedRequests.map(request => request.id)

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

    requests.value = normalizedRequests.map(request => ({
      ...request,
      registrationNumbers: registrationNumbersByRequestId[request.id] || []
    }))
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявки'
  } finally {
    loading.value = false
  }
}

function typeLabel(type) {
  const map = {
    NO_STIPEND: 'Без отметки',
    WITH_STIPEND: 'Со стипендией'
  }

  return map[type] || type || '—'
}

function statusLabel(status) {
  const map = {
    NEW: 'Новая',
    ACCEPTED: 'Принята',
    IN_WORK: 'В обработке',
    DELAYED: 'Задерживается',
    READY: 'Готова',
    REJECTED: 'Отклонена',
    ARCHIVED: 'Архив',
    CANCELLED: 'Отменена'
  }

  return map[status] || status || '—'
}

function statusColor(status) {
  const map = {
    NEW: 'grey-7',
    ACCEPTED: 'blue-7',
    IN_WORK: 'orange-8',
    DELAYED: 'brown-6',
    READY: 'green-7',
    REJECTED: 'red-7',
    ARCHIVED: 'blue-grey-6',
    CANCELLED: 'deep-orange-6'
  }

  return map[status] || 'grey-7'
}

function statusClass(status) {
  const map = {
    NEW: 'line-grey',
    ACCEPTED: 'line-blue',
    IN_WORK: 'line-orange',
    DELAYED: 'line-brown',
    READY: 'line-green',
    REJECTED: 'line-red',
    ARCHIVED: 'line-grey-dark',
    CANCELLED: 'line-deep-orange'
  }

  return map[status] || 'line-grey'
}

function facultyCode(facultyId) {
  const faculty = faculties.value.find(item => item.id === facultyId)

  if (faculty?.code && /^\d+$/.test(String(faculty.code))) {
    return String(faculty.code).padStart(2, '0')
  }

  return String(facultyId).padStart(2, '0')
}

function formatRegistrationNumber(number) {
  if (!number?.registrationNumber || !number?.registrationYear) return ''

  const code = facultyCode(number.facultyId)
  const regNumber = String(number.registrationNumber).padStart(4, '0')
  const year = String(number.registrationYear).slice(-2)

  return `${code}-${regNumber}/${year}`
}

function formatLegacyRegistration(request) {
  if (!request?.registrationNumber || !request?.registrationYear) return ''

  return formatRegistrationNumber({
    facultyId: request.facultyId,
    registrationNumber: request.registrationNumber,
    registrationYear: request.registrationYear
  })
}

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleDateString('ru-RU')
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

.page-head {
  gap: 12px;
}

.create-btn {
  border-radius: 12px;
  background: #7a0019 !important;
  color: #ffffff !important;
  font-weight: 600;
}

.request-card {
  position: relative;
  border-radius: 16px;
  cursor: pointer;
  transition: 0.2s ease;
  background: #ffffff;
  overflow: hidden;
}

.request-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.request-section {
  position: relative;
  padding: 24px 24px 24px 32px;
}

.status-line {
  position: absolute;
  left: 16px;
  top: 16px;
  bottom: 16px;
  width: 7px;
  border-radius: 8px;
}

.request-content {
  margin-left: 16px;
}

.line-grey {
  background: #9e9e9e;
}

.line-blue {
  background: #1e88e5;
}

.line-orange {
  background: #fb8c00;
}

.line-brown {
  background: #8d6e63;
}

.line-green {
  background: #43a047;
}

.line-red {
  background: #e53935;
}

.line-grey-dark {
  background: #616161;
}

.line-deep-orange {
  background: #f4511e;
}

.field-label {
  color: #777;
  font-size: 13px;
  margin-right: 4px;
}

.field-value {
  font-size: 13px;
  font-weight: 500;
}

.registration-chip {
  width: fit-content;
  color: #7a0019;
  border-color: #7a0019;
  font-weight: 500;
}

.scan-chip {
  width: fit-content;
  color: #7a0019;
  border-color: #7a0019;
  font-weight: 500;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  padding: 32px;
  background: #ffffff;
  border: 1px solid #eeeeee;
}

@media (max-width: 600px) {
  .page-head {
    align-items: flex-start;
  }

  .create-btn {
    width: 100%;
    margin-top: 12px;
  }

  .request-section {
    padding: 20px 18px 20px 30px;
  }
}
</style>
