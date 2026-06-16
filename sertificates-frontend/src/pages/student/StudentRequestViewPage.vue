<template>
  <q-page class="q-pa-md page-bg">
    <div class="page-header q-mb-md row items-center justify-between">
      <div>
        <div class="text-h5 text-weight-medium">
          Заявка №{{ req?.id || '—' }}
        </div>

        <div v-if="req" class="text-caption text-grey-7 q-mt-xs">
          Подана: {{ req.date }}
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

    <q-card class="main-card" flat v-if="req">
      <q-card-section class="q-pa-lg">
        <div class="row items-center q-gutter-sm q-mb-md">
          <div class="text-subtitle1 text-weight-medium">Статус заявки</div>

          <q-chip dense :color="statusColor(req.status)" text-color="white">
            {{ req.statusText }}
          </q-chip>
        </div>

        <q-banner
          v-if="req.status === 'READY'"
          rounded
          dense
          class="bg-green-1 text-black q-mb-md"
        >
          Справка готова. Можно обратиться за получением.
        </q-banner>

        <q-banner
          v-if="isCancelled"
          rounded
          dense
          class="cancel-banner q-mb-md"
        >
          Заявка отменена. Отправка новых комментариев недоступна.
        </q-banner>

        <div class="info-grid">
          <q-card flat bordered class="info-card">
            <q-card-section>
              <div class="section-title">Данные заявки</div>

              <div class="info-row">
                <span>ФИО</span>
                <b>{{ req.fio }}</b>
              </div>

              <div class="info-row">
                <span>Курс / группа</span>
                <b>{{ req.course || '—' }} курс / {{ req.group || '—' }}</b>
              </div>

              <div class="info-row">
                <span>Куда требуется</span>
                <b>{{ req.purpose }}</b>
              </div>

              <div class="info-row">
                <span>Тип справки</span>
                <b>{{ typeLabel(req.type) }}</b>
              </div>

              <div class="info-row">
                <span>Количество экземпляров</span>
                <b>{{ req.copiesCount }}</b>
              </div>

              <div class="info-row">
                <span>Скан справки</span>
                <b>{{ req.needScan ? 'Нужен' : 'Не нужен' }}</b>
              </div>
            </q-card-section>
          </q-card>

          <q-card flat bordered class="info-card">
            <q-card-section>
              <div class="section-title">Регистрация</div>

              <div class="info-row">
                <span>Дата регистрации</span>
                <b>{{ req.registeredAt || '—' }}</b>
              </div>

              <div class="info-row">
                <span>Регистрационные номера</span>

                <div v-if="registrationNumbers.length" class="row q-gutter-xs q-mt-xs">
                  <q-chip
                    v-for="number in registrationNumbers"
                    :key="number.id"
                    dense
                    outline
                    class="registration-chip"
                  >
                    {{ formatRegistrationNumber(number) }}
                  </q-chip>
                </div>

                <q-chip
                  v-else-if="req.registrationNumber"
                  dense
                  outline
                  class="registration-chip"
                >
                  {{ registrationLabel }}
                </q-chip>

                <b v-else>Номер пока не присвоен</b>
              </div>

              <div class="info-row">
                <span>Период</span>
                <b>{{ periodLabel }}</b>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <q-card flat bordered class="info-card q-mt-md">
          <q-card-section>
            <div class="section-title q-mb-md">Готовая справка</div>

            <div v-if="req.scanOriginalFileName" class="scan-box">
              <div class="row items-center no-wrap">
                <q-icon name="attach_file" size="22px" class="campus-accent q-mr-sm" />

                <div class="col">
                  <div class="file-name">{{ req.scanOriginalFileName }}</div>
                  <div class="text-caption text-grey-7">
                    Загружено: {{ req.scanUploadedAt || '—' }}
                  </div>
                </div>
              </div>

              <q-btn
                outline
                class="campus-accent q-mt-md"
                icon="visibility"
                label="Открыть скан"
                @click="openScan"
              />
            </div>

            <div v-else class="text-grey-7">
              Скан справки пока не прикреплён.
            </div>
          </q-card-section>
        </q-card>

        <q-card flat bordered class="info-card q-mt-md">
          <q-card-section>
            <div class="section-title q-mb-md">Комментарии</div>

            <div class="info-row">
              <span>Ваш комментарий</span>
              <b>{{ req.studentComment || 'Комментарий отсутствует' }}</b>
            </div>

            <div class="info-row">
              <span>Комментарий секретаря</span>
              <b>{{ req.secretaryComment || 'Комментарий отсутствует' }}</b>
            </div>

            <div class="comment-block q-mt-md">
              <q-input
                v-model="newComment"
                outlined
                color="dark"
                type="textarea"
                autogrow
                spellcheck="false"
                placeholder="Введите новый комментарий..."
                :disable="isCancelled"
                class="comment-input"
              />

              <div class="comment-actions">
                <q-btn
                  unelevated
                  color="primary"
                  class="send-btn"
                  label="Отправить комментарий"
                  :loading="savingComment"
                  :disable="isCancelled || !newComment.trim()"
                  @click="confirmSendComment"
                />
              </div>
            </div>

            <div v-if="canCancel" class="cancel-wrap">
              <q-btn
                flat
                color="negative"
                icon="close"
                class="cancel-btn"
                label="Отменить заявку"
                :loading="savingCancel"
                @click="confirmCancelRequest"
              />
            </div>
          </q-card-section>
        </q-card>

        <div class="q-mt-xl">
          <div class="section-title q-mb-md">История обработки</div>

          <div v-if="history.length" class="timeline-wrap">
            <div
              v-for="h in history"
              :key="h.key"
              class="timeline-item"
            >
              <div class="timeline-dot"></div>

              <div class="timeline-content">
                <div class="timeline-date">{{ h.dt }}</div>
                <div class="timeline-text">{{ h.text }}</div>
              </div>
            </div>
          </div>

          <div v-else class="text-body2 text-grey-7">
            История пока отсутствует
          </div>
        </div>
      </q-card-section>
    </q-card>

    <q-card v-else class="main-card" flat>
      <q-card-section class="q-pa-lg">
        <div v-if="loading" class="text-grey-7">Загрузка...</div>
        <div v-else-if="error" class="text-negative">{{ error }}</div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import {
  getRequestById,
  updateStudentComment,
  cancelRequest,
  downloadRequestScan
} from 'src/api/requests'
import { getRequestHistory } from 'src/api/requestHistory'
import { getFaculties } from 'src/api/faculties'
import { getRegistrationNumbersByRequestId } from 'src/api/requestRegistrationNumbers'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const id = route.params.id

const loading = ref(false)
const savingComment = ref(false)
const savingCancel = ref(false)
const error = ref('')

const req = ref(null)
const history = ref([])
const newComment = ref('')
const faculties = ref([])
const registrationNumbers = ref([])

const isCancelled = computed(() => req.value?.status === 'CANCELLED')

const canCancel = computed(() => {
  return ['NEW', 'ACCEPTED', 'IN_WORK', 'DELAYED'].includes(req.value?.status)
})

const periodLabel = computed(() => {
  if (!req.value?.periodFrom || !req.value?.periodTo) return '—'
  return `${req.value.periodFrom} — ${req.value.periodTo}`
})

const registrationLabel = computed(() => {
  if (!req.value?.registrationNumber || !req.value?.registrationYear) return ''

  return formatRegistrationNumber({
    facultyId: req.value.facultyId,
    registrationNumber: req.value.registrationNumber,
    registrationYear: req.value.registrationYear
  })
})

function statusLabel(status) {
  return {
    NEW: 'Новая',
    ACCEPTED: 'Принята',
    IN_WORK: 'В обработке',
    DELAYED: 'Задерживается',
    READY: 'Готова',
    REJECTED: 'Отклонена',
    ARCHIVED: 'Архив',
    CANCELLED: 'Отменена'
  }[status] || status || '—'
}

function statusColor(status) {
  return {
    NEW: 'grey-7',
    ACCEPTED: 'blue-7',
    IN_WORK: 'orange-8',
    DELAYED: 'brown-6',
    READY: 'green-7',
    REJECTED: 'red-7',
    ARCHIVED: 'blue-grey-6',
    CANCELLED: 'deep-orange-6'
  }[status] || 'grey-7'
}

function typeLabel(type) {
  return {
    NO_STIPEND: 'Без отметки',
    WITH_STIPEND: 'Со стипендией'
  }[type] || type || '—'
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

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleDateString('ru-RU')
}

function formatDateTime(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleString('ru-RU')
}

function mapRequestToView(data) {
  return {
    id: data.id,
    date: formatDate(data.createdAt),
    createdAt: data.createdAt,
    status: data.status,
    statusText: statusLabel(data.status),
    facultyId: data.facultyId,
    type: data.certificateType,
    needScan: !!data.needScan,
    fio: data.studentFullName || '—',
    course: data.course,
    group: data.groupName,
    purpose: data.purpose || '—',
    copiesCount: data.copiesCount || 1,
    periodFrom: formatDate(data.periodFrom),
    periodTo: formatDate(data.periodTo),
    studentComment: data.studentComment || '',
    secretaryComment: data.secretaryComment || '',
    registrationNumber: data.registrationNumber,
    registrationYear: data.registrationYear,
    registeredAt: data.registeredAt ? formatDateTime(data.registeredAt) : null,
    scanOriginalFileName: data.scanOriginalFileName || '',
    scanContentType: data.scanContentType || '',
    scanUploadedAt: data.scanUploadedAt ? formatDateTime(data.scanUploadedAt) : null
  }
}

function mapHistoryItem(item) {
  const statusText = item.newStatus ? statusLabel(item.newStatus) : ''

  const textMap = {
    CREATE: 'Заявка создана',
    EDIT: 'Заявка изменена',
    STATUS_CHANGE: `Статус изменён: ${statusText}`,
    REGISTER: 'Заявка зарегистрирована',
    ARCHIVE: 'Заявка архивирована',
    CANCEL: 'Заявка отменена студентом',
    STUDENT_COMMENT: item.comment || 'Комментарий студента добавлен',
    SECRETARY_COMMENT: item.comment || 'Комментарий секретаря добавлен',
    SCAN_UPLOAD: 'Скан справки прикреплён',
    SCAN_DELETE: 'Скан справки удалён'
  }

  return {
    key: `${item.id ?? item.createdAt}_${item.actionType}_${item.comment ?? ''}`,
    dt: formatDateTime(item.createdAt),
    text: textMap[item.actionType] || item.comment || item.actionType || 'Изменение',
    actionType: item.actionType
  }
}

async function loadRequest() {
  loading.value = true
  error.value = ''

  try {
    const [
      requestResponse,
      facultiesResponse,
      registrationNumbersResponse
    ] = await Promise.all([
      getRequestById(id),
      getFaculties(),
      getRegistrationNumbersByRequestId(id)
    ])

    faculties.value = facultiesResponse.data.map(faculty => ({
      id: faculty.id,
      code: faculty.code,
      name: faculty.name,
      active: faculty.isActive !== false
    }))

    req.value = mapRequestToView(requestResponse.data)
    registrationNumbers.value = registrationNumbersResponse.data
    newComment.value = ''
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявку'
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  try {
    const { data } = await getRequestHistory()

    const filtered = data
      .filter(item => String(item.requestId) === String(id))
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

    let mapped = filtered.map(mapHistoryItem)

    const hasCreate = filtered.some(item => item.actionType === 'CREATE')

    if (!hasCreate && req.value?.createdAt) {
      mapped = [
        {
          key: `created_${req.value.id}`,
          dt: formatDateTime(req.value.createdAt),
          text: 'Заявка создана',
          actionType: 'CREATE'
        },
        ...mapped
      ]
    }

    history.value = mapped
  } catch (err) {
    console.error(err)

    history.value = req.value?.createdAt
      ? [
          {
            key: `created_${req.value.id}`,
            dt: formatDateTime(req.value.createdAt),
            text: 'Заявка создана',
            actionType: 'CREATE'
          }
        ]
      : []
  }
}

async function sendComment() {
  if (!newComment.value.trim() || isCancelled.value) return

  savingComment.value = true

  try {
    const { data } = await updateStudentComment(id, newComment.value)
    req.value = mapRequestToView(data)
    newComment.value = ''

    await loadHistory()

    $q.notify({
      type: 'positive',
      message: 'Комментарий сохранён'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось сохранить комментарий'
    })
  } finally {
    savingComment.value = false
  }
}

function confirmSendComment() {
  if (!newComment.value.trim() || isCancelled.value) return

  $q.dialog({
    title: 'Подтверждение',
    message: 'Отправить комментарий к заявке?',
    cancel: true,
    persistent: true
  }).onOk(() => {
    sendComment()
  })
}

async function cancelCurrentRequest() {
  savingCancel.value = true

  try {
    const { data } = await cancelRequest(id)
    req.value = mapRequestToView(data)

    await loadHistory()

    $q.notify({
      type: 'positive',
      message: 'Заявка отменена'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось отменить заявку'
    })
  } finally {
    savingCancel.value = false
  }
}

function confirmCancelRequest() {
  if (!canCancel.value) return

  $q.dialog({
    title: 'Подтверждение',
    message: 'Вы действительно хотите отменить заявку?',
    cancel: true,
    persistent: true
  }).onOk(() => {
    cancelCurrentRequest()
  })
}

async function openScan() {
  try {
    const response = await downloadRequestScan(id)

    const blob = new Blob([response.data], {
      type: req.value.scanContentType || response.data.type || 'application/octet-stream'
    })

    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')

    setTimeout(() => {
      URL.revokeObjectURL(url)
    }, 10000)
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось открыть скан справки'
    })
  }
}

onMounted(async () => {
  await loadRequest()
  await loadHistory()
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
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.info-card {
  border-radius: 14px;
  background: #fafafa;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
}

.info-row span {
  color: #6b7280;
  font-size: 13px;
}

.info-row b {
  font-size: 14px;
  color: #111827;
}

.registration-chip {
  width: fit-content;
  color: #7a0019;
  border-color: #7a0019;
  font-weight: 500;
}

.scan-box {
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #eeeeee;
  padding: 12px;
}

.file-name {
  font-size: 15px;
  font-weight: 500;
}

.campus-accent {
  color: #7a0019;
  border-color: #7a0019;
}

.cancel-banner {
  background: #fff3e8;
  color: #c24e00;
  border: 1px solid #ffd7b8;
}

.comment-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-actions {
  display: flex;
  justify-content: flex-start;
}

.send-btn {
  border-radius: 12px;
  padding: 10px 22px;
  font-weight: 500;
  background: #7a0019 !important;
}

.cancel-wrap {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
}

.cancel-btn {
  opacity: 0.75;
}

.cancel-btn:hover {
  opacity: 1;
}

.comment-input :deep(.q-field__control) {
  border-radius: 14px;
}

.comment-input :deep(.q-field__control:before) {
  border: 1px solid #d1d5db !important;
}

.comment-input :deep(.q-field__control:hover:before) {
  border-color: #7a0019 !important;
}

.comment-input :deep(.q-field--focused .q-field__control:before) {
  border: 2px solid #7a0019 !important;
}

.comment-input :deep(.q-field--focused .q-field__control:after) {
  border: 2px solid #7a0019 !important;
}

.comment-input :deep(textarea:focus),
.comment-input :deep(.q-field__native:focus) {
  outline: none !important;
  box-shadow: none !important;
}

.comment-input :deep(.q-field--focused .q-field__control) {
  box-shadow: 0 0 0 2px rgba(122, 0, 25, 0.12) !important;
}

.comment-input :deep(textarea),
.comment-input :deep(.q-field__native) {
  caret-color: #7a0019;
}

.timeline-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #7a0019;
  margin-top: 6px;
  flex-shrink: 0;
}

.timeline-content {
  background: #fafafa;
  border-radius: 14px;
  padding: 10px 14px;
  width: 100%;
}

.timeline-date {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.timeline-text {
  font-size: 14px;
  color: #111827;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
