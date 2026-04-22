<template>
  <q-page class="q-pa-md">
    <div class="row items-center q-mb-md">
      <div class="text-h6">Карточка заявки</div>
      <q-space />
      <q-btn flat icon="arrow_back" class="campus-accent" label="Назад" @click="goBack" />
    </div>

    <div class="row q-col-gutter-md">
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
                <div class="field-value">{{ request.facultyId }}</div>
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
                <div class="field-value">{{ request.periodFrom }} — {{ request.periodTo }}</div>
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
                v-if="request.status !== 'REJECTED' && request.status !== 'READY'"
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
            />

            <q-btn
              unelevated
              color="primary"
              class="q-mt-md"
              label="Сохранить комментарий"
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
import { computed, reactive, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute, useRouter } from 'vue-router'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()

const requestId = Number(route.params.id)

const request = ref({
  id: requestId,
  regNumber: requestId,
  fio: 'Стародубцева А.К.',
  courseGroup: '4ИС',
  facultyId: 'F01',
  purpose: 'По месту требования',
  qty: 2,
  type: 'WITH_STIPEND',
  periodFrom: '01.01.2025',
  periodTo: '31.01.2026',
  createdAt: '08.12.2025',
  status: 'NEW',
  archived: false,
  registrationNumber: null,
  registrationYear: null,
  registeredAt: null,
  registeredBy: null
})

const history = ref([
  {
    id: 1,
    title: 'Заявка создана',
    subtitle: '08.12.2025 10:43 • student_2014',
    body: 'Студент создал заявку на справку.'
  }
])

const statusHistory = ref(['NEW'])
const commentText = ref('')

const statusDialog = reactive({
  open: false,
  action: null,
  reason: ''
})

const registrationLabel = computed(() => {
  if (!request.value.registrationNumber) return ''
  return `${request.value.facultyId}-${request.value.registrationNumber}/${request.value.registrationYear}`
})

const canRollbackStatus = computed(() => {
  return request.value.status !== 'NEW' && statusHistory.value.length > 1
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

function statusLabel (s) {
  const map = {
    NEW: 'Новая',
    ACCEPTED: 'Принята',
    IN_WORK: 'В обработке',
    DELAYED: 'Задерживается',
    READY: 'Готово',
    REJECTED: 'Отклонена'
  }
  return map[s] || s
}

function statusColor (s) {
  const map = {
    NEW: 'grey-8',
    ACCEPTED: 'blue-7',
    IN_WORK: 'orange-8',
    DELAYED: 'brown-6',
    READY: 'green-7',
    REJECTED: 'red-7'
  }
  return map[s] || 'grey-7'
}

function nowText () {
  return new Date().toLocaleString('ru-RU')
}

function addHistory (title, body, actor = 'sec_f01') {
  history.value.unshift({
    id: Date.now() + Math.random(),
    title,
    subtitle: `${nowText()} • ${actor}`,
    body
  })
}

function pushStatusHistory (newStatus) {
  const last = statusHistory.value[statusHistory.value.length - 1]
  if (last !== newStatus) {
    statusHistory.value.push(newStatus)
  }
}

function goBack () {
  router.push('/secretary')
}

function openStatusDialog (action) {
  statusDialog.action = action
  statusDialog.reason = ''
  statusDialog.open = true
}

function confirmStatusAction () {
  if (statusDialog.action === 'REJECTED' && !statusDialog.reason.trim()) {
    $q.notify({
      type: 'negative',
      message: 'Укажите причину отклонения.',
      position: 'top'
    })
    return
  }

  if (statusDialog.action === 'ROLLBACK') {
    confirmRollbackStatus()
    return
  }

  if (statusDialog.action === 'ACCEPTED') {
    request.value.status = 'ACCEPTED'
    pushStatusHistory('ACCEPTED')
    request.value.registrationNumber = 16
    request.value.registrationYear = 2026
    request.value.registeredAt = nowText()
    request.value.registeredBy = 'sec_f01'

    addHistory(
      'Заявка принята',
      `Присвоен регистрационный номер ${registrationLabel.value}.`
    )

    $q.notify({
      type: 'positive',
      message: 'Заявка принята, номер присвоен.',
      position: 'top'
    })
  }

  if (statusDialog.action === 'IN_WORK') {
    request.value.status = 'IN_WORK'
    pushStatusHistory('IN_WORK')

    addHistory('Статус изменён', 'Заявка переведена в обработку.')

    $q.notify({
      type: 'positive',
      message: 'Заявка переведена в обработку.',
      position: 'top'
    })
  }

  if (statusDialog.action === 'DELAYED') {
    request.value.status = 'DELAYED'
    pushStatusHistory('DELAYED')

    addHistory('Статус изменён', 'По заявке зафиксирована задержка.')

    $q.notify({
      type: 'warning',
      message: 'Для заявки отмечена задержка.',
      position: 'top'
    })
  }

  if (statusDialog.action === 'READY') {
    request.value.status = 'READY'
    pushStatusHistory('READY')

    addHistory('Статус изменён', 'Справка подготовлена и готова к выдаче.')

    $q.notify({
      type: 'positive',
      message: 'Справка отмечена как готовая.',
      position: 'top'
    })
  }

  if (statusDialog.action === 'REJECTED') {
    request.value.status = 'REJECTED'
    pushStatusHistory('REJECTED')

    addHistory('Заявка отклонена', statusDialog.reason.trim())
    $q.notify({
      type: 'negative',
      message: 'Заявка отклонена.',
      position: 'top'
    })
  }

  statusDialog.open = false
}

function confirmRollbackStatus () {
  if (statusHistory.value.length < 2 || request.value.status === 'NEW') {
    $q.notify({
      type: 'negative',
      message: 'Нельзя вернуть предыдущий статус.',
      position: 'top'
    })
    statusDialog.open = false
    return
  }

  const currentStatus = statusHistory.value.pop()
  const previousStatus = statusHistory.value[statusHistory.value.length - 1]

  request.value.status = previousStatus

  addHistory(
    'Откат статуса',
    `Статус изменён обратно: ${statusLabel(currentStatus)} → ${statusLabel(previousStatus)}.`
  )

  statusDialog.open = false

  $q.notify({
    type: 'warning',
    message: `Статус возвращён: ${statusLabel(previousStatus)}.`,
    position: 'top'
  })
}

function archiveRequest () {
  request.value.archived = true
  addHistory('Архивация', 'Заявка перемещена в архив.')

  $q.notify({
    type: 'positive',
    message: 'Заявка перемещена в архив.',
    position: 'top'
  })
}

function unarchiveRequest () {
  request.value.archived = false
  addHistory('Восстановление', 'Заявка возвращена из архива.')

  $q.notify({
    type: 'positive',
    message: 'Заявка возвращена из архива.',
    position: 'top'
  })
}

function saveComment () {
  if (!commentText.value.trim()) {
    $q.notify({
      type: 'negative',
      message: 'Введите комментарий.',
      position: 'top'
    })
    return
  }

  addHistory('Комментарий', commentText.value.trim())
  commentText.value = ''

  $q.notify({
    type: 'positive',
    message: 'Комментарий сохранён.',
    position: 'top'
  })
}
</script>

<style scoped>
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
