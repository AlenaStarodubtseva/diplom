<template>
  <q-page class="q-pa-md secretary-page">
    <div class="row items-center q-mb-md">
      <div class="text-h6">Карточка заявки</div>
      <q-space />
      <q-btn flat icon="arrow_back" class="campus-accent" label="Назад" @click="goBack" />
    </div>

    <div v-if="loading" class="text-grey-7 q-mt-md">
      Загрузка...
    </div>

    <div v-else-if="error" class="text-negative q-mt-md">
      {{ error }}
    </div>

    <div v-else-if="request" class="row q-col-gutter-md">
      <div class="col-12 col-lg-7">
        <q-card class="card q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">Основные данные</div>

            <div class="row q-col-gutter-md">
              <div class="col-12 col-md-6">
                <div class="field-label">ФИО</div>
                <div class="field-value">{{ request.fio }}</div>
              </div>

              <div class="col-12 col-md-3">
                <div class="field-label">Курс / группа</div>
                <div class="field-value">{{ request.courseGroup }}</div>
              </div>

              <div class="col-12 col-md-3">
                <div class="field-label">Факультет</div>
                <div class="field-value">{{ request.facultyName || request.facultyId || '—' }}</div>
              </div>

              <div class="col-12">
                <div class="field-label">Куда нужна справка</div>
                <div class="field-value">{{ request.purpose }}</div>
              </div>

              <div class="col-12 col-md-6">
                <div class="field-label">Тип справки</div>
                <div class="field-value">
                  {{ request.type === 'WITH_STIPEND' ? 'С отметкой о стипендии' : 'Без отметки о стипендии' }}
                </div>
              </div>

              <div class="col-12 col-md-6">
                <div class="field-label">Количество</div>
                <div class="field-value">{{ request.qty }}</div>
              </div>

              <div class="col-12" v-if="request.type === 'WITH_STIPEND'">
                <div class="field-label">Период</div>
                <div class="field-value">{{ request.periodFrom || '—' }} — {{ request.periodTo || '—' }}</div>
              </div>

              <div class="col-12 col-md-6">
                <div class="field-label">Дата создания</div>
                <div class="field-value">{{ request.createdAt }}</div>
              </div>

              <div class="col-12 col-md-6">
                <div class="field-label">Текущий статус</div>
                <div class="field-value">
                  <q-chip dense :color="statusColor(request.status)" text-color="white">
                    {{ statusLabel(request.status) }}
                  </q-chip>
                </div>
              </div>

              <div class="col-12">
                <div class="field-label">Комментарий студента</div>
                <div class="field-value">{{ request.studentComment || 'Комментарий отсутствует' }}</div>
              </div>
            </div>
          </q-card-section>
        </q-card>

        <q-card class="card">
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">История обработки</div>

            <q-timeline color="grey-6">
              <q-timeline-entry
                v-for="item in history"
                :key="item.id"
                :title="item.title"
                :subtitle="item.subtitle"
                :body="item.body"
              />
            </q-timeline>

            <div v-if="!history.length" class="text-grey-7">
              История пока отсутствует
            </div>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-lg-5">
        <q-card class="card q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">Регистрация</div>

            <div class="q-mb-md">
              <div class="field-label">Регистрационный номер</div>
              <div class="field-value">
                <template v-if="request.registrationNumber">
                  {{ registrationLabel }}
                </template>
                <template v-else>
                  <span class="text-grey-7">Номер не присвоен</span>
                </template>
              </div>
            </div>

            <div class="q-mb-md">
              <div class="field-label">Дата регистрации</div>
              <div class="field-value">{{ request.registeredAt || '—' }}</div>
            </div>

            <div>
              <div class="field-label">Зарегистрировал</div>
              <div class="field-value">{{ request.registeredBy || '—' }}</div>
            </div>
          </q-card-section>
        </q-card>

        <q-card class="card q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">Действия</div>

            <div class="column q-gutter-sm">
              <q-btn
                v-if="request.status === 'NEW'"
                unelevated
                color="primary"
                label="Принять заявку"
                icon="task_alt"
                @click="openStatusDialog('ACCEPTED')"
              />

              <q-btn
                v-if="request.status === 'ACCEPTED'"
                unelevated
                color="orange-8"
                label="Перевести в работу"
                icon="work_history"
                @click="openStatusDialog('IN_WORK')"
              />

              <q-btn
                v-if="request.status === 'IN_WORK' || request.status === 'DELAYED'"
                unelevated
                color="positive"
                label="Отметить как готово"
                icon="inventory_2"
                @click="openStatusDialog('READY')"
              />

              <q-btn
                v-if="request.status === 'IN_WORK'"
                unelevated
                color="brown-6"
                label="Отметить задержку"
                icon="schedule"
                @click="openStatusDialog('DELAYED')"
              />

              <q-btn
                v-if="request.status !== 'REJECTED' && request.status !== 'READY' && request.status !== 'CANCELLED'"
                outline
                color="negative"
                label="Отклонить заявку"
                icon="cancel"
                @click="openStatusDialog('REJECTED')"
              />

              <q-btn
                v-if="canRollbackStatus"
                outline
                color="primary"
                label="Вернуть предыдущий статус"
                icon="undo"
                @click="openStatusDialog('ROLLBACK')"
              />

              <q-btn
                v-if="!request.archived"
                outline
                class="campus-accent"
                label="В архив"
                icon="archive"
                @click="archiveRequest"
              />

              <q-btn
                v-else
                outline
                class="campus-accent"
                label="Вернуть из архива"
                icon="unarchive"
                @click="unarchiveRequest"
              />
            </div>
          </q-card-section>
        </q-card>

        <q-card class="card">
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">Комментарий секретаря</div>

            <q-input
              v-model="commentText"
              type="textarea"
              outlined
              autogrow
              placeholder="Добавить комментарий..."
              :disable="request.status === 'CANCELLED'"
            />

            <q-btn
              unelevated
              color="primary"
              class="q-mt-md"
              label="Сохранить комментарий"
              :disable="request.status === 'CANCELLED'"
              @click="saveComment"
            />
          </q-card-section>
        </q-card>
      </div>
    </div>

    <q-dialog v-model="statusDialog.open">
      <q-card style="width: 560px; max-width: 95vw;" class="card">
        <q-card-section class="row items-center">
          <div class="text-h6">Подтверждение действия</div>
          <q-space />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section>
          <div class="q-mb-sm">
            {{ dialogMainText }}
          </div>

          <div class="text-body2 text-grey-8 q-mb-sm">
            Текущий статус:
            <b>{{ statusLabel(request.status) }}</b>
          </div>

          <div v-if="statusDialog.action !== 'ROLLBACK'" class="text-body2 text-grey-8 q-mb-sm">
            Новый статус:
            <b>{{ statusLabel(statusDialog.action) }}</b>
          </div>

          <div v-else class="text-body2 text-grey-8 q-mb-sm">
            Предыдущий статус:
            <b>{{ previousStatusLabel }}</b>
          </div>

          <q-banner
            v-if="statusDialog.action === 'ACCEPTED'"
            dense
            rounded
            class="bg-blue-1 text-black q-mt-md"
          >
            При подтверждении заявке будет присвоен регистрационный номер.
          </q-banner>

          <q-input
            v-if="statusDialog.action === 'REJECTED'"
            v-model="statusDialog.reason"
            type="textarea"
            outlined
            autogrow
            class="q-mt-md"
            label="Причина отклонения"
          />

          <div class="text-caption text-grey-7 q-mt-md">
            Действие будет записано в историю обработки заявки.
          </div>
        </q-card-section>

        <q-separator />

        <q-card-actions align="right">
          <q-btn flat label="Отмена" v-close-popup />
          <q-btn unelevated color="primary" label="Подтвердить" @click="confirmStatusAction" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute, useRouter } from 'vue-router'
import {
  getRequestById,
  updateSecretaryComment,
  updateRequestStatus,
  acceptRequest
} from 'src/api/requests'
import { getRequestHistory } from 'src/api/requestHistory'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()

const requestId = Number(route.params.id)

const loading = ref(false)
const error = ref('')

const request = ref(null)
const history = ref([])
const statusHistory = ref([])
const commentText = ref('')

const statusDialog = reactive({
  open: false,
  action: null,
  reason: ''
})

const registrationLabel = computed(() => {
  if (!request.value?.registrationNumber) return ''
  return `${request.value.facultyId || 'F'}-${request.value.registrationNumber}/${request.value.registrationYear || new Date().getFullYear()}`
})

const canRollbackStatus = computed(() => {
  return request.value?.status !== 'NEW' && statusHistory.value.length > 1
})

const previousStatusLabel = computed(() => {
  if (statusHistory.value.length < 2) return ''
  const prev = statusHistory.value[statusHistory.value.length - 2]
  return statusLabel(prev)
})

const dialogMainText = computed(() => {
  if (statusDialog.action === 'ROLLBACK') {
    return 'Вы уверены, что хотите вернуть предыдущий статус?'
  }
  if (statusDialog.action === 'REJECTED') {
    return 'Вы уверены, что хотите отклонить заявку?'
  }
  return 'Вы уверены, что хотите изменить статус заявки?'
})

function statusLabel(s) {
  const map = {
    NEW: 'Новая',
    ACCEPTED: 'Принята',
    IN_WORK: 'В обработке',
    DELAYED: 'Задерживается',
    READY: 'Готово',
    REJECTED: 'Отклонена',
    ARCHIVED: 'В архиве',
    CANCELLED: 'Отменена'
  }
  return map[s] || s
}

function statusColor(s) {
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
  return map[s] || 'grey-7'
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU')
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('ru-RU')
}

function normalizeRequest(data) {
  const courseGroup = [data.course, data.groupName].filter(Boolean).join(' / ')

  return {
    id: data.id,
    fio: data.studentFullName || '—',
    courseGroup: courseGroup || '—',
    facultyId: data.facultyId,
    facultyName: data.facultyName,
    purpose: data.purpose || '—',
    qty: data.copiesCount || 1,
    type: data.certificateType,
    periodFrom: formatDate(data.periodFrom),
    periodTo: formatDate(data.periodTo),
    createdAt: formatDateTime(data.createdAt),
    status: data.status,
    archived: data.status === 'ARCHIVED',
    registrationNumber: data.registrationNumber,
    registrationYear: data.registrationYear,
    registeredAt: data.registeredAt ? formatDateTime(data.registeredAt) : null,
    registeredBy: 'sec_f01',
    studentComment: data.studentComment || '',
    secretaryComment: data.secretaryComment || ''
  }
}

function buildStatusHistory(items) {
  const statuses = []

  items
    .filter(item => item.actionType === 'CREATE' || item.actionType === 'STATUS_CHANGE')
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .forEach(item => {
      const status = item.newStatus || (item.actionType === 'CREATE' ? 'NEW' : null)
      if (status) statuses.push(status)
    })

  return statuses.length ? statuses : ['NEW']
}

function normalizeHistory(items) {
  return items
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map(item => {
      let title = 'Изменение'
      let body = item.comment || ''

      if (item.actionType === 'CREATE') {
        title = 'Заявка создана'
        body = item.comment || 'Студент создал заявку на справку.'
      } else if (item.actionType === 'STATUS_CHANGE') {
        title = 'Статус изменён'
        body = item.comment || `Новый статус: ${statusLabel(item.newStatus)}.`
      } else if (item.actionType === 'STUDENT_COMMENT') {
        title = 'Комментарий студента'
      } else if (item.actionType === 'SECRETARY_COMMENT') {
        title = 'Комментарий секретаря'
      } else if (item.actionType === 'ARCHIVE') {
        title = 'Архивация'
      }

      return {
        id: item.id,
        title,
        subtitle: `${formatDateTime(item.createdAt)} • ${item.actorLogin || item.actorRole || 'system'}`,
        body
      }
    })
}

function goBack() {
  router.push('/secretary')
}

function openStatusDialog(action) {
  statusDialog.action = action
  statusDialog.reason = ''
  statusDialog.open = true
}

async function loadRequestCard() {
  loading.value = true
  error.value = ''

  try {
    const [{ data: requestData }, { data: historyData }] = await Promise.all([
      getRequestById(requestId),
      getRequestHistory()
    ])

    const relatedHistory = historyData.filter(item => Number(item.requestId) === requestId)

    request.value = normalizeRequest(requestData)
    history.value = normalizeHistory(relatedHistory)
    statusHistory.value = buildStatusHistory(relatedHistory)
    commentText.value = requestData.secretaryComment || ''
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить карточку заявки'
  } finally {
    loading.value = false
  }
}

async function applyStatus(newStatus, comment = null) {
  await updateRequestStatus(requestId, newStatus)
  await loadRequestCard()

  if (comment) {
    $q.notify({
      type: 'positive',
      message: comment,
      position: 'top'
    })
  }
}

async function confirmStatusAction() {
  if (statusDialog.action === 'REJECTED' && !statusDialog.reason.trim()) {
    $q.notify({
      type: 'negative',
      message: 'Укажите причину отклонения.',
      position: 'top'
    })
    return
  }

  if (statusDialog.action === 'ROLLBACK') {
    await confirmRollbackStatus()
    return
  }

  try {
    if (statusDialog.action === 'ACCEPTED') {
      await acceptRequest(requestId)
      await loadRequestCard()

      $q.notify({
        type: 'positive',
        message: 'Заявка принята и зарегистрирована.',
        position: 'top'
      })

      statusDialog.open = false
      return
    }

    if (statusDialog.action === 'IN_WORK') {
      await applyStatus('IN_WORK', 'Заявка переведена в обработку.')
    }

    if (statusDialog.action === 'DELAYED') {
      await applyStatus('DELAYED', 'Для заявки отмечена задержка.')
    }

    if (statusDialog.action === 'READY') {
      await applyStatus('READY', 'Справка отмечена как готовая.')
    }

    if (statusDialog.action === 'REJECTED') {
      await applyStatus('REJECTED', 'Заявка отклонена.')
      if (statusDialog.reason.trim()) {
        await updateSecretaryComment(requestId, `Причина отклонения: ${statusDialog.reason.trim()}`)
        await loadRequestCard()
      }
    }

    statusDialog.open = false
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось изменить статус заявки.',
      position: 'top'
    })
  }
}

async function confirmRollbackStatus() {
  if (statusHistory.value.length < 2 || request.value.status === 'NEW') {
    $q.notify({
      type: 'negative',
      message: 'Нельзя вернуть предыдущий статус.',
      position: 'top'
    })
    statusDialog.open = false
    return
  }

  const previousStatus = statusHistory.value[statusHistory.value.length - 2]

  try {
    await applyStatus(previousStatus, `Статус возвращён: ${statusLabel(previousStatus)}.`)
    statusDialog.open = false
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось вернуть предыдущий статус.',
      position: 'top'
    })
  }
}

async function archiveRequest() {
  try {
    await applyStatus('ARCHIVED', 'Заявка перемещена в архив.')
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось архивировать заявку.',
      position: 'top'
    })
  }
}

async function unarchiveRequest() {
  try {
    await applyStatus('ACCEPTED', 'Заявка возвращена из архива.')
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось вернуть заявку из архива.',
      position: 'top'
    })
  }
}

async function saveComment() {
  if (!commentText.value.trim()) {
    $q.notify({
      type: 'negative',
      message: 'Введите комментарий.',
      position: 'top'
    })
    return
  }

  try {
    await updateSecretaryComment(requestId, commentText.value.trim())
    await loadRequestCard()

    $q.notify({
      type: 'positive',
      message: 'Комментарий сохранён.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось сохранить комментарий.',
      position: 'top'
    })
  }
}

onMounted(() => {
  loadRequestCard()
})
</script>

<style scoped>
.secretary-page {
  background: #f7f7f8;
  min-height: 100%;
}

.card {
  border-radius: 14px;
}

.campus-accent {
  color: #7a0019;
  border-color: #7a0019;
}

.field-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.field-value {
  font-size: 15px;
  font-weight: 500;
}
</style>
