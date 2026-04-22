<template>
  <q-page class="q-pa-md page-bg">
    <div class="page-header q-mb-md row items-center justify-between">
      <div>
        <div class="text-h5 text-weight-medium">Справка об обучении №{{ req?.number }}</div>
        <div v-if="req" class="text-caption text-grey-7 q-mt-xs">
          от {{ req.date }}
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

        <div class="top-status row items-center justify-between q-col-gutter-md">
          <div class="col-12 col-md">
            <div class="row items-center q-gutter-sm">
              <span class="text-subtitle1 text-weight-medium">Статус заявки</span>
              <q-badge :color="statusColor(req.status)" text-color="white" class="status-badge">
                {{ req.statusText }}
              </q-badge>
            </div>
            <div class="text-caption text-grey-7 q-mt-xs">
              Актуальное состояние обработки вашей справки
            </div>
          </div>
        </div>

        <q-banner
          v-if="isCancelled"
          rounded
          class="cancel-banner q-mt-md"
        >
          <template #avatar>
            <q-icon name="warning_amber" size="24px" />
          </template>
          Заявка отменена. Отправка новых комментариев недоступна.
        </q-banner>

        <div class="info-grid q-mt-lg">
          <q-card flat bordered class="info-card">
            <q-card-section>
              <div class="section-title">Получатель</div>
              <div class="info-row"><span>ФИО</span><b>{{ req.fio }}</b></div>
              <div class="info-row"><span>Курс</span><b>{{ req.course || '—' }}</b></div>
              <div class="info-row"><span>Группа</span><b>{{ req.group || '—' }}</b></div>
            </q-card-section>
          </q-card>

          <q-card flat bordered class="info-card">
            <q-card-section>
              <div class="section-title">Параметры справки</div>
              <div class="info-row"><span>Куда требуется</span><b>{{ req.purpose }}</b></div>
              <div class="info-row"><span>Электронная копия</span><b>{{ req.needScan ? 'Да' : 'Нет' }}</b></div>
              <div class="info-row"><span>Последний комментарий</span><b>{{ req.studentComment || 'Комментарий отсутствует' }}</b></div>
            </q-card-section>
          </q-card>
        </div>

        <div class="q-mt-lg">
          <div class="section-title q-mb-sm">Ваш комментарий к заявке</div>

          <div class="comment-block">
            <q-input
              outlined
              v-model="newComment"
              type="textarea"
              autogrow
              placeholder="Введите комментарий..."
              :disable="isCancelled"
              class="comment-input"
            />

            <div class="comment-actions">
              <q-btn
                unelevated
                color="primary"
                class="send-btn"
                label="Отправить"
                :loading="savingComment"
                :disable="isCancelled || !newComment.trim()"
                @click="confirmSendComment"
              />
            </div>
          </div>

          <div v-if="!isCancelled" class="cancel-wrap">
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
        </div>

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
  cancelRequest
} from 'src/api/requests'
import { getRequestHistory } from 'src/api/requestHistory'

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

const isCancelled = computed(() => req.value?.status === 'CANCELLED')

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

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU')
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('ru-RU')
}

function mapRequestToView(data) {
  return {
    id: data.id,
    number: data.registrationNumber ?? data.id,
    date: formatDate(data.createdAt),
    createdAt: data.createdAt,
    status: data.status,
    statusText: statusLabel(data.status),
    needScan: !!data.needScan,
    fio: data.studentFullName || '—',
    course: data.course,
    group: data.groupName,
    purpose: data.purpose || '—',
    studentComment: data.studentComment || ''
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
    SECRETARY_COMMENT: item.comment || 'Комментарий секретаря добавлен'
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
    const { data } = await getRequestById(id)
    req.value = mapRequestToView(data)
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
  if (isCancelled.value) return

  $q.dialog({
    title: 'Подтверждение',
    message: 'Вы действительно хотите отменить заявку?',
    cancel: true,
    persistent: true
  }).onOk(() => {
    cancelCurrentRequest()
  })
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
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(24, 39, 75, 0.08);
  background: #ffffff;
}

.status-badge {
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 999px;
}

.cancel-banner {
  background: #fff3e8;
  color: #c24e00;
  border: 1px solid #ffd7b8;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-card {
  border-radius: 16px;
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

.comment-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-actions {
  display: flex;
  justify-content: center;
}

.send-btn {
  border-radius: 12px;
  padding: 10px 28px;
  font-weight: 500;
  background: #8b0015 !important;
}

.send-btn:hover {
  background: #a3001b !important;
}

.cancel-wrap {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.cancel-btn {
  opacity: 0.7;
  transition: 0.2s;
}

.cancel-btn:hover {
  opacity: 1;
  transform: scale(1.05);
}

.comment-input :deep(.q-field__control) {
  border-radius: 14px;
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
  background: #8b0015;
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
