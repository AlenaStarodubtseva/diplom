<template>
  <q-page class="q-pa-md">
    <div class="q-mb-md row items-center justify-between">
      <div class="text-h6">Справка об обучении №{{ req?.number }}</div>

      <q-btn
        flat
        color="grey-8"
        icon="arrow_back"
        label="Назад"
        @click="router.push('/student')"
      />
    </div>

    <q-card class="q-pa-md card" v-if="req">
      <div class="text-h6 q-mb-sm">Справка об обучении №{{ req.number }}</div>
      <div class="text-caption text-grey-7 q-mb-md">от {{ req.date }}</div>

      <div class="q-mb-sm row items-center">
        <b>Статус:</b>
        <q-badge :color="statusColor(req.status)" text-color="white" class="q-ml-sm">
          {{ req.statusText }}
        </q-badge>
      </div>

      <q-banner
        v-if="isCancelled"
        inline-actions
        rounded
        class="bg-orange-1 text-deep-orange q-mb-md"
      >
        Заявка отменена. Отправка новых комментариев недоступна.
      </q-banner>

      <div class="q-mb-sm"><b>Нужна электронная копия:</b> {{ req.needScan ? 'Да' : 'Нет' }}</div>

      <div class="q-mt-md text-subtitle2">Получатель</div>
      <div><b>ФИО:</b> {{ req.fio }}</div>
      <div><b>Курс:</b> {{ req.course || '—' }}</div>
      <div><b>Группа:</b> {{ req.group || '—' }}</div>

      <div class="q-mt-md"><b>Куда требуется справка:</b> {{ req.purpose }}</div>
      <div class="q-mt-sm"><b>Последний комментарий студента:</b> {{ req.studentComment || 'Комментарий отсутствует' }}</div>

      <q-separator class="q-my-md" />

      <div class="text-subtitle2 q-mb-sm">Ваш комментарий к заявке</div>
      <div class="row items-center q-col-gutter-sm">
        <div class="col">
          <q-input
            outlined
            v-model="newComment"
            placeholder="Комментарий..."
            :disable="isCancelled"
          />
        </div>
        <div class="col-auto">
          <q-btn
            outline
            label="Отправить"
            :loading="savingComment"
            :disable="isCancelled || !newComment.trim()"
            @click="confirmSendComment"
          />
        </div>
      </div>

      <div class="q-mt-md" v-if="!isCancelled">
        <q-btn
          color="negative"
          flat
          label="Отменить заявку"
          :loading="savingCancel"
          @click="confirmCancelRequest"
        />
      </div>

      <div class="text-subtitle2 q-mt-lg q-mb-sm">История обработки</div>
      <div v-for="h in history" :key="h.key" class="text-body2 q-mb-xs">
        {{ h.dt }} — {{ h.text }}
      </div>
      <div v-if="!history.length" class="text-body2 text-grey-7">
        История пока отсутствует
      </div>
    </q-card>

    <q-card v-else class="q-pa-md card">
      <div v-if="loading" class="text-grey-7">Загрузка...</div>
      <div v-else-if="error" class="text-negative">{{ error }}</div>
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
    NEW: 'grey',
    ACCEPTED: 'blue',
    IN_WORK: 'orange',
    DELAYED: 'brown',
    READY: 'green',
    REJECTED: 'red',
    ARCHIVED: 'grey-7',
    CANCELLED: 'deep-orange'
  }[status] || 'grey'
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
.card {
  border-radius: 14px;
}
</style>
