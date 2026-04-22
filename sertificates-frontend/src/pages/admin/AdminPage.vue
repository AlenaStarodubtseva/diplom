<template>
  <q-page class="q-pa-md bg-grey-1">
    <div class="row items-center q-col-gutter-sm q-mb-md">
      <div class="col">
        <div class="text-h5 text-weight-semibold">Панель администратора</div>
        <div class="text-grey-7">
          Управление заявками, доступами и факультетами
        </div>
      </div>
    </div>

    <q-card class="card">
      <q-card-section>
        <q-tabs v-model="tab" dense align="left" class="campus-tabs q-mb-md">
          <q-tab name="requests" label="ЗАЯВКИ" />
          <q-tab name="access" label="ДОСТУПЫ" />
          <q-tab name="faculties" label="ФАКУЛЬТЕТЫ" />
        </q-tabs>

        <q-separator class="q-mb-md" />

        <q-tab-panels v-model="tab" animated>
          <!-- ЗАЯВКИ -->
          <q-tab-panel name="requests" class="q-pa-none">
            <div class="row items-center q-col-gutter-sm q-mb-md">
              <div class="col-12 col-md">
                <q-input
                  v-model="requestSearch"
                  dense
                  outlined
                  debounce="300"
                  placeholder="Поиск: ФИО / № заявки / группа / рег. номер / цель"
                >
                  <template #append>
                    <q-icon name="search" />
                  </template>
                </q-input>
              </div>

              <div class="col-12 col-md-auto">
                <q-btn
                  unelevated
                  color="primary"
                  icon="add"
                  label="Создать заявку"
                  @click="$router.push('/manual-request')"
                />
              </div>
            </div>

            <q-tabs v-model="requestTab" dense align="left" class="campus-tabs q-mb-md">
              <q-tab name="active" label="АКТИВНЫЕ" />
              <q-tab name="archive" label="АРХИВ" />
            </q-tabs>

            <div class="row q-col-gutter-sm items-center q-mb-md">
              <div class="col-12 col-md-3">
                <q-select
                  v-model="requestFilters.facultyId"
                  dense
                  outlined
                  clearable
                  :options="facultyOptions"
                  label="Факультет"
                  emit-value
                  map-options
                />
              </div>

              <div class="col-12 col-md-3">
                <q-select
                  v-model="requestFilters.type"
                  dense
                  outlined
                  clearable
                  :options="typeOptions"
                  label="Тип справки"
                  emit-value
                  map-options
                />
              </div>

              <div class="col-12 col-md-3">
                <q-select
                  v-model="requestFilters.status"
                  dense
                  outlined
                  clearable
                  :options="statusOptions"
                  label="Статус"
                  emit-value
                  map-options
                />
              </div>

              <div class="col-12 col-md-3">
                <q-toggle
                  v-model="requestFilters.onlyRegistered"
                  color="primary"
                  label="Только зарегистрированные"
                />
              </div>
            </div>

            <div class="row q-col-gutter-sm items-center q-mb-md">
              <div class="col-12 col-md-3">
                <q-btn
                  outline
                  class="campus-accent full-width"
                  icon="restart_alt"
                  label="Сбросить фильтры"
                  @click="resetRequestFilters"
                />
              </div>

              <div class="col-12 col-md-9">
                <div class="text-grey-7">
                  Этот раздел используется и как рабочий список заявок, и как журнал учета.
                </div>
              </div>
            </div>

            <div v-if="selectedRequests.length" class="row items-center q-gutter-sm q-mb-md">
              <q-chip dense color="grey-3" text-color="black">
                Выбрано: {{ selectedRequests.length }}
              </q-chip>

              <template v-if="requestTab === 'active'">
                <q-btn
                  unelevated
                  color="primary"
                  icon="archive"
                  label="В архив"
                  @click="bulkArchiveRequests"
                />
                <q-btn
                  unelevated
                  color="positive"
                  icon="description"
                  label="Сформировать общий документ"
                  @click="generateCommonDocument"
                />
              </template>

              <template v-else>
                <q-btn
                  unelevated
                  color="primary"
                  icon="unarchive"
                  label="Вернуть в активные"
                  @click="bulkUnarchiveRequests"
                />
              </template>
            </div>

            <q-table
              :rows="filteredRequests"
              :columns="requestColumns"
              row-key="id"
              flat
              selection="multiple"
              v-model:selected="selectedRequests"
              :pagination="{ rowsPerPage: 10 }"
              class="campus-table"
            >
              <template #body-cell-registration="props">
                <q-td :props="props">
                  <span v-if="props.row.registrationNumber">
                    {{ formatRegistration(props.row) }}
                  </span>
                  <span v-else class="text-grey-6">Не присвоен</span>
                </q-td>
              </template>

              <template #body-cell-facultyId="props">
                <q-td :props="props">
                  {{ facultyLabel(props.row.facultyId) }}
                </q-td>
              </template>

              <template #body-cell-type="props">
                <q-td :props="props">
                  {{ typeLabel(props.row.type) }}
                </q-td>
              </template>

              <template #body-cell-period="props">
                <q-td :props="props">
                  <span v-if="props.row.periodFrom && props.row.periodTo">
                    {{ props.row.periodFrom }} — {{ props.row.periodTo }}
                  </span>
                  <span v-else class="text-grey-6">—</span>
                </q-td>
              </template>

              <template #body-cell-status="props">
                <q-td :props="props">
                  <q-chip
                    dense
                    :color="statusColor(props.row.status)"
                    text-color="white"
                  >
                    {{ statusLabel(props.row.status) }}
                  </q-chip>
                </q-td>
              </template>

              <template #body-cell-actions="props">
                <q-td :props="props">
                  <q-btn
                    flat
                    dense
                    round
                    icon="open_in_new"
                    class="campus-accent"
                    @click="openRequest(props.row.id)"
                  />
                </q-td>
              </template>

              <template #no-data>
                <div class="full-width row flex-center text-grey-7 q-gutter-sm q-pa-lg">
                  <q-icon name="inbox" size="24px" />
                  <span>Заявки не найдены</span>
                </div>
              </template>
            </q-table>
          </q-tab-panel>

          <!-- ДОСТУПЫ -->
          <q-tab-panel name="access" class="q-pa-none">
            <div class="row items-center q-col-gutter-sm q-mb-md">
              <div class="col-12 col-md">
                <q-input
                  v-model="accessSearch"
                  dense
                  outlined
                  debounce="300"
                  placeholder="Поиск по логину / ФИО"
                >
                  <template #append>
                    <q-icon name="search" />
                  </template>
                </q-input>
              </div>

              <div class="col-12 col-md-auto">
                <q-btn
                  unelevated
                  color="primary"
                  icon="person_add"
                  label="Добавить доступ"
                  @click="openCreateAccessDialog"
                />
              </div>
            </div>

            <q-table
              :rows="filteredAccessRows"
              :columns="accessColumns"
              row-key="id"
              flat
              :pagination="{ rowsPerPage: 10 }"
              class="campus-table"
            >
              <template #body-cell-role="props">
                <q-td :props="props">
                  <q-chip dense :color="roleColor(props.row.role)" text-color="white">
                    {{ roleLabel(props.row.role) }}
                  </q-chip>
                </q-td>
              </template>

              <template #body-cell-facultyId="props">
                <q-td :props="props">
                  <span v-if="props.row.facultyId">{{ facultyLabel(props.row.facultyId) }}</span>
                  <span v-else class="text-grey-6">Все факультеты</span>
                </q-td>
              </template>

              <template #body-cell-active="props">
                <q-td :props="props">
                  <q-chip
                    dense
                    :color="props.row.active ? 'green-7' : 'grey-6'"
                    text-color="white"
                  >
                    {{ props.row.active ? 'Активен' : 'Отключен' }}
                  </q-chip>
                </q-td>
              </template>

              <template #body-cell-actions="props">
                <q-td :props="props">
                  <div class="row justify-end q-gutter-xs">
                    <q-btn
                      flat
                      dense
                      round
                      icon="edit"
                      class="campus-accent"
                      @click="openEditAccessDialog(props.row)"
                    />
                    <q-btn
                      flat
                      dense
                      round
                      :icon="props.row.active ? 'block' : 'check_circle'"
                      :color="props.row.active ? 'negative' : 'positive'"
                      @click="toggleAccessStatus(props.row)"
                    />
                  </div>
                </q-td>
              </template>

              <template #no-data>
                <div class="full-width row flex-center text-grey-7 q-gutter-sm q-pa-lg">
                  <q-icon name="manage_accounts" size="24px" />
                  <span>Доступы не найдены</span>
                </div>
              </template>
            </q-table>
          </q-tab-panel>

          <!-- ФАКУЛЬТЕТЫ -->
          <q-tab-panel name="faculties" class="q-pa-none">
            <div class="row items-center q-col-gutter-sm q-mb-md">
              <div class="col-12 col-md">
                <q-input
                  v-model="facultySearch"
                  dense
                  outlined
                  debounce="300"
                  placeholder="Поиск по коду или названию факультета"
                >
                  <template #append>
                    <q-icon name="search" />
                  </template>
                </q-input>
              </div>

              <div class="col-12 col-md-auto">
                <q-btn
                  unelevated
                  color="primary"
                  icon="add_business"
                  label="Добавить факультет"
                  @click="openCreateFacultyDialog"
                />
              </div>
            </div>

            <q-table
              :rows="filteredFacultyRows"
              :columns="facultyColumns"
              row-key="id"
              flat
              :pagination="{ rowsPerPage: 10 }"
              class="campus-table"
            >
              <template #body-cell-active="props">
                <q-td :props="props">
                  <q-chip
                    dense
                    :color="props.row.active ? 'green-7' : 'grey-6'"
                    text-color="white"
                  >
                    {{ props.row.active ? 'Активен' : 'Скрыт' }}
                  </q-chip>
                </q-td>
              </template>

              <template #body-cell-actions="props">
                <q-td :props="props">
                  <div class="row justify-end q-gutter-xs">
                    <q-btn
                      flat
                      dense
                      round
                      icon="edit"
                      class="campus-accent"
                      @click="openEditFacultyDialog(props.row)"
                    />
                    <q-btn
                      flat
                      dense
                      round
                      :icon="props.row.active ? 'visibility_off' : 'visibility'"
                      :color="props.row.active ? 'negative' : 'positive'"
                      @click="toggleFacultyStatus(props.row)"
                    />
                  </div>
                </q-td>
              </template>

              <template #no-data>
                <div class="full-width row flex-center text-grey-7 q-gutter-sm q-pa-lg">
                  <q-icon name="school" size="24px" />
                  <span>Факультеты не найдены</span>
                </div>
              </template>
            </q-table>
          </q-tab-panel>
        </q-tab-panels>
      </q-card-section>
    </q-card>

    <!-- Диалог доступа -->
    <q-dialog v-model="accessDialog.open">
      <q-card style="min-width: 520px; max-width: 95vw">
        <q-card-section class="row items-center">
          <div class="text-h6">
            {{ accessDialog.mode === 'create' ? 'Новый доступ' : 'Редактирование доступа' }}
          </div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section class="q-gutter-md">
          <q-input
            v-model="accessDialog.form.login"
            outlined
            dense
            label="Логин"
          />
          <q-input
            v-model="accessDialog.form.fio"
            outlined
            dense
            label="ФИО"
          />
          <q-select
            v-model="accessDialog.form.role"
            outlined
            dense
            :options="roleOptions"
            emit-value
            map-options
            label="Роль"
          />
          <q-select
            v-model="accessDialog.form.facultyId"
            outlined
            dense
            clearable
            :options="facultyOptions"
            emit-value
            map-options
            label="Факультет"
            hint="Для администратора можно оставить пустым"
          />
          <q-toggle
            v-model="accessDialog.form.active"
            color="primary"
            label="Активен"
          />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Отмена" v-close-popup />
          <q-btn unelevated color="primary" label="Сохранить" @click="saveAccess" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Диалог факультета -->
    <q-dialog v-model="facultyDialog.open">
      <q-card style="min-width: 520px; max-width: 95vw">
        <q-card-section class="row items-center">
          <div class="text-h6">
            {{ facultyDialog.mode === 'create' ? 'Новый факультет' : 'Редактирование факультета' }}
          </div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section class="q-gutter-md">
          <q-input
            v-model="facultyDialog.form.code"
            outlined
            dense
            label="Код факультета"
          />
          <q-input
            v-model="facultyDialog.form.name"
            outlined
            dense
            label="Название факультета"
          />
          <q-input
            v-model.number="facultyDialog.form.nextRegistrationNumber"
            type="number"
            outlined
            dense
            label="Следующий регистрационный номер"
          />
          <q-toggle
            v-model="facultyDialog.form.active"
            color="primary"
            label="Активен"
          />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Отмена" v-close-popup />
          <q-btn unelevated color="primary" label="Сохранить" @click="saveFaculty" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'

const router = useRouter()
const $q = useQuasar()

const tab = ref('requests')
const requestTab = ref('active')

const requestSearch = ref('')
const accessSearch = ref('')
const facultySearch = ref('')

const selectedRequests = ref([])

const faculties = ref([
  {
    id: 1,
    code: 'F01',
    name: 'ФФМОиТ',
    nextRegistrationNumber: 19,
    active: true
  },
  {
    id: 2,
    code: 'F02',
    name: 'ФПП',
    nextRegistrationNumber: 6,
    active: true
  },
  {
    id: 3,
    code: 'F03',
    name: 'Историко-филологический факультет',
    nextRegistrationNumber: 14,
    active: false
  }
])

const requests = ref([
  {
    id: 346,
    fio: 'Стародубцева А.К.',
    courseGroup: '4ИС',
    facultyId: 'F01',
    purpose: 'По месту требования',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '08.12.2025',
    status: 'IN_WORK',
    archived: false,
    registrationNumber: 16,
    registrationYear: 2026
  },
  {
    id: 347,
    fio: 'Иванова А.А.',
    courseGroup: '3ИС',
    facultyId: 'F01',
    purpose: 'Соцзащита',
    qty: 2,
    type: 'WITH_STIPEND',
    periodFrom: '01.09.2025',
    periodTo: '31.12.2025',
    createdAt: '09.12.2025',
    status: 'READY',
    archived: false,
    registrationNumber: 17,
    registrationYear: 2026
  },
  {
    id: 348,
    fio: 'Петров П.П.',
    courseGroup: '2МО',
    facultyId: 'F01',
    purpose: 'Военкомат',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '10.12.2025',
    status: 'NEW',
    archived: false,
    registrationNumber: null,
    registrationYear: null
  },
  {
    id: 349,
    fio: 'Смирнова Е.В.',
    courseGroup: '1ПО',
    facultyId: 'F01',
    purpose: 'По месту требования',
    qty: 1,
    type: 'WITH_STIPEND',
    periodFrom: '01.01.2025',
    periodTo: '31.05.2025',
    createdAt: '15.12.2025',
    status: 'ACCEPTED',
    archived: false,
    registrationNumber: 18,
    registrationYear: 2026
  },
  {
    id: 350,
    fio: 'Кузнецова Н.И.',
    courseGroup: '4ИС',
    facultyId: 'F01',
    purpose: 'Посольство',
    qty: 1,
    type: 'NO_STIPEND',
    periodFrom: null,
    periodTo: null,
    createdAt: '01.11.2025',
    status: 'READY',
    archived: true,
    registrationNumber: 11,
    registrationYear: 2025
  },
  {
    id: 351,
    fio: 'Орлов Д.С.',
    courseGroup: '3ИС',
    facultyId: 'F02',
    purpose: 'Соцзащита',
    qty: 1,
    type: 'WITH_STIPEND',
    periodFrom: '01.09.2025',
    periodTo: '31.12.2025',
    createdAt: '12.12.2025',
    status: 'IN_WORK',
    archived: false,
    registrationNumber: 5,
    registrationYear: 2026
  }
])

const accessRows = ref([
  {
    id: 1,
    login: 'admin',
    fio: 'Системный администратор',
    role: 'ADMIN',
    facultyId: null,
    active: true
  },
  {
    id: 2,
    login: 'secretary_f01',
    fio: 'Секретарь ФФМОиТ',
    role: 'SECRETARY',
    facultyId: 'F01',
    active: true
  },
  {
    id: 3,
    login: 'secretary_f02',
    fio: 'Секретарь ФПП',
    role: 'SECRETARY',
    facultyId: 'F02',
    active: true
  }
])

const requestFilters = ref({
  facultyId: null,
  type: null,
  status: null,
  onlyRegistered: false
})

const typeOptions = [
  { label: 'Без отметки', value: 'NO_STIPEND' },
  { label: 'Со стипендией', value: 'WITH_STIPEND' }
]

const statusOptions = [
  { label: 'Новая', value: 'NEW' },
  { label: 'Принято', value: 'ACCEPTED' },
  { label: 'В обработке', value: 'IN_WORK' },
  { label: 'Отложено', value: 'DELAYED' },
  { label: 'Готово', value: 'READY' },
  { label: 'Отклонено', value: 'REJECTED' }
]

const roleOptions = [
  { label: 'Администратор', value: 'ADMIN' },
  { label: 'Секретарь', value: 'SECRETARY' }
]

const facultyOptions = computed(() =>
  faculties.value.map((f) => ({
    label: `${f.code} — ${f.name}`,
    value: f.code
  }))
)

const requestColumns = [
  { name: 'registration', label: 'Рег. номер', field: 'registration', align: 'left' },
  { name: 'id', label: '№ заявки', field: 'id', sortable: true, align: 'left' },
  { name: 'fio', label: 'ФИО', field: 'fio', sortable: true, align: 'left' },
  { name: 'facultyId', label: 'Факультет', field: 'facultyId', align: 'left' },
  { name: 'courseGroup', label: 'Курс/группа', field: 'courseGroup', align: 'left' },
  { name: 'purpose', label: 'Куда нужна справка', field: 'purpose', align: 'left' },
  { name: 'qty', label: 'Кол-во', field: 'qty', sortable: true, align: 'left' },
  { name: 'type', label: 'Тип', field: 'type', align: 'left' },
  { name: 'period', label: 'Период', field: 'period', align: 'left' },
  { name: 'createdAt', label: 'Дата подачи', field: 'createdAt', sortable: true, align: 'left' },
  { name: 'status', label: 'Статус', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const accessColumns = [
  { name: 'login', label: 'Логин', field: 'login', align: 'left', sortable: true },
  { name: 'fio', label: 'ФИО', field: 'fio', align: 'left', sortable: true },
  { name: 'role', label: 'Роль', field: 'role', align: 'left' },
  { name: 'facultyId', label: 'Факультет', field: 'facultyId', align: 'left' },
  { name: 'active', label: 'Статус', field: 'active', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const facultyColumns = [
  { name: 'code', label: 'Код', field: 'code', align: 'left', sortable: true },
  { name: 'name', label: 'Название факультета', field: 'name', align: 'left', sortable: true },
  { name: 'nextRegistrationNumber', label: 'Следующий рег. номер', field: 'nextRegistrationNumber', align: 'left', sortable: true },
  { name: 'active', label: 'Статус', field: 'active', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const filteredRequests = computed(() => {
  const q = requestSearch.value.trim().toLowerCase()

  return requests.value
    .filter((r) => (requestTab.value === 'archive' ? r.archived : !r.archived))
    .filter((r) => {
      if (requestFilters.value.facultyId && r.facultyId !== requestFilters.value.facultyId) return false
      if (requestFilters.value.type && r.type !== requestFilters.value.type) return false
      if (requestFilters.value.status && r.status !== requestFilters.value.status) return false
      if (requestFilters.value.onlyRegistered && !r.registrationNumber) return false

      if (q) {
        const haystack = [
          r.id,
          r.fio,
          r.facultyId,
          r.courseGroup,
          r.purpose,
          r.registrationNumber ? formatRegistration(r) : ''
        ]
          .join(' ')
          .toLowerCase()

        if (!haystack.includes(q)) return false
      }

      return true
    })
})

const filteredAccessRows = computed(() => {
  const q = accessSearch.value.trim().toLowerCase()

  return accessRows.value.filter((row) => {
    if (!q) return true

    return [row.login, row.fio, roleLabel(row.role), facultyLabel(row.facultyId || '')]
      .join(' ')
      .toLowerCase()
      .includes(q)
  })
})

const filteredFacultyRows = computed(() => {
  const q = facultySearch.value.trim().toLowerCase()

  return faculties.value.filter((row) => {
    if (!q) return true

    return [row.code, row.name].join(' ').toLowerCase().includes(q)
  })
})

const accessDialog = ref({
  open: false,
  mode: 'create',
  form: {
    id: null,
    login: '',
    fio: '',
    role: 'SECRETARY',
    facultyId: null,
    active: true
  }
})

const facultyDialog = ref({
  open: false,
  mode: 'create',
  form: {
    id: null,
    code: '',
    name: '',
    nextRegistrationNumber: 1,
    active: true
  }
})

function formatRegistration(row) {
  if (!row.registrationNumber || !row.registrationYear) return ''
  return `${row.facultyId}-${row.registrationNumber}/${row.registrationYear}`
}

function typeLabel(type) {
  const map = {
    NO_STIPEND: 'Без отметки',
    WITH_STIPEND: 'Со стипендией'
  }
  return map[type] || type
}

function statusLabel(status) {
  return statusOptions.find((x) => x.value === status)?.label || status
}

function statusColor(status) {
  const map = {
    NEW: 'grey-8',
    ACCEPTED: 'blue-7',
    IN_WORK: 'orange-8',
    DELAYED: 'brown-6',
    READY: 'green-7',
    REJECTED: 'red-7'
  }

  return map[status] || 'grey-7'
}

function roleLabel(role) {
  const map = {
    ADMIN: 'Администратор',
    SECRETARY: 'Секретарь'
  }
  return map[role] || role
}

function roleColor(role) {
  const map = {
    ADMIN: 'deep-purple-7',
    SECRETARY: 'indigo-7'
  }
  return map[role] || 'grey-7'
}

function facultyLabel(code) {
  if (!code) return ''
  const faculty = faculties.value.find((f) => f.code === code)
  return faculty ? `${faculty.code} — ${faculty.name}` : code
}

function resetRequestFilters() {
  requestFilters.value.facultyId = null
  requestFilters.value.type = null
  requestFilters.value.status = null
  requestFilters.value.onlyRegistered = false
}

function openRequest(id) {
  router.push(`/secretary/${id}`)
}

function bulkArchiveRequests() {
  if (requestTab.value !== 'active') return

  const ids = new Set(selectedRequests.value.map((s) => s.id))
  requests.value = requests.value.map((r) => (
    ids.has(r.id)
      ? { ...r, archived: true }
      : r
  ))

  selectedRequests.value = []

  $q.notify({
    type: 'positive',
    message: 'Заявки перенесены в архив.',
    position: 'top'
  })
}

function bulkUnarchiveRequests() {
  if (requestTab.value !== 'archive') return

  const ids = new Set(selectedRequests.value.map((s) => s.id))
  requests.value = requests.value.map((r) => (
    ids.has(r.id)
      ? { ...r, archived: false }
      : r
  ))

  selectedRequests.value = []

  $q.notify({
    type: 'positive',
    message: 'Заявки возвращены в активные.',
    position: 'top'
  })
}

function generateCommonDocument() {
  $q.notify({
    type: 'info',
    message: `Сформирован общий документ по ${selectedRequests.value.length} заявк(е/ам) — мок.`,
    position: 'top'
  })
}

function openCreateAccessDialog() {
  accessDialog.value = {
    open: true,
    mode: 'create',
    form: {
      id: null,
      login: '',
      fio: '',
      role: 'SECRETARY',
      facultyId: null,
      active: true
    }
  }
}

function openEditAccessDialog(row) {
  accessDialog.value = {
    open: true,
    mode: 'edit',
    form: { ...row }
  }
}

function saveAccess() {
  const form = accessDialog.value.form

  if (!form.login?.trim() || !form.fio?.trim() || !form.role) {
    $q.notify({
      type: 'negative',
      message: 'Заполни логин, ФИО и роль.',
      position: 'top'
    })
    return
  }

  if (form.role === 'SECRETARY' && !form.facultyId) {
    $q.notify({
      type: 'negative',
      message: 'Для секретаря нужно выбрать факультет.',
      position: 'top'
    })
    return
  }

  if (accessDialog.value.mode === 'create') {
    accessRows.value.unshift({
      ...form,
      id: Date.now()
    })
  } else {
    accessRows.value = accessRows.value.map((row) =>
      row.id === form.id ? { ...form } : row
    )
  }

  accessDialog.value.open = false

  $q.notify({
    type: 'positive',
    message: 'Доступ сохранён.',
    position: 'top'
  })
}

function toggleAccessStatus(row) {
  accessRows.value = accessRows.value.map((item) =>
    item.id === row.id
      ? { ...item, active: !item.active }
      : item
  )

  $q.notify({
    type: 'positive',
    message: row.active ? 'Доступ отключён.' : 'Доступ активирован.',
    position: 'top'
  })
}

function openCreateFacultyDialog() {
  facultyDialog.value = {
    open: true,
    mode: 'create',
    form: {
      id: null,
      code: '',
      name: '',
      nextRegistrationNumber: 1,
      active: true
    }
  }
}

function openEditFacultyDialog(row) {
  facultyDialog.value = {
    open: true,
    mode: 'edit',
    form: { ...row }
  }
}

function saveFaculty() {
  const form = facultyDialog.value.form

  if (!form.code?.trim() || !form.name?.trim()) {
    $q.notify({
      type: 'negative',
      message: 'Заполни код и название факультета.',
      position: 'top'
    })
    return
  }

  const normalizedCode = form.code.trim().toUpperCase()

  const duplicate = faculties.value.find((f) =>
    f.code === normalizedCode && f.id !== form.id
  )

  if (duplicate) {
    $q.notify({
      type: 'negative',
      message: 'Факультет с таким кодом уже существует.',
      position: 'top'
    })
    return
  }

  if (facultyDialog.value.mode === 'create') {
    faculties.value.unshift({
      ...form,
      id: Date.now(),
      code: normalizedCode
    })
  } else {
    faculties.value = faculties.value.map((row) =>
      row.id === form.id
        ? { ...form, code: normalizedCode }
        : row
    )
  }

  facultyDialog.value.open = false

  $q.notify({
    type: 'positive',
    message: 'Факультет сохранён.',
    position: 'top'
  })
}

function toggleFacultyStatus(row) {
  faculties.value = faculties.value.map((item) =>
    item.id === row.id
      ? { ...item, active: !item.active }
      : item
  )

  $q.notify({
    type: 'positive',
    message: row.active ? 'Факультет скрыт.' : 'Факультет активирован.',
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

.campus-tabs :deep(.q-tab--active) {
  color: #7a0019;
}

.campus-table :deep(.q-table__bottom) {
  border-top: 1px solid #eee;
}
</style>
