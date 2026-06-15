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
          <q-tab-panel name="requests" class="q-pa-none">
            <div class="row items-center q-col-gutter-sm q-mb-md">
              <div class="col-12 col-md">
                <q-input
                  v-model="requestSearch"
                  dense
                  outlined
                  debounce="300"
                  placeholder="Поиск: ФИО / № заявки / группа / рег. номер / цель / статус"
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

            <div v-if="requestsLoading" class="q-pa-md text-grey-7">
              Загрузка...
            </div>

            <div v-else-if="requestsError" class="q-pa-md text-negative">
              {{ requestsError }}
            </div>

            <q-table
              v-else
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
                  <div v-if="props.row.registrationNumbers?.length" class="column q-gutter-xs">
                    <q-chip
                      v-for="number in props.row.registrationNumbers"
                      :key="number.id"
                      dense
                      outline
                      class="registration-chip"
                    >
                      {{ formatRegistrationNumber(number) }}
                    </q-chip>
                  </div>

                  <span v-else-if="props.row.registrationNumber">
                    {{ formatRegistration(props.row) }}
                  </span>

                  <span v-else class="text-grey-6">Не присвоен</span>
                </q-td>
              </template>

              <template #body-cell-status="props">
                <q-td :props="props">
                  <q-chip dense :color="statusColor(props.row.status)" text-color="white">
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
                  >
                    <q-tooltip>
                      Открыть заявку
                    </q-tooltip>
                  </q-btn>
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

              <template #no-data>
                <div class="full-width row flex-center text-grey-7 q-gutter-sm q-pa-lg">
                  <q-icon name="inbox" size="24px" />
                  <span>Заявки не найдены</span>
                </div>
              </template>
            </q-table>
          </q-tab-panel>

          <q-tab-panel name="access" class="q-pa-none">
            <div class="row items-center q-col-gutter-sm q-mb-md">
              <div class="col-12 col-md">
                <q-input
                  v-model="accessSearch"
                  dense
                  outlined
                  debounce="300"
                  placeholder="Поиск по логину / ФИО / роли / факультету"
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

            <q-banner rounded class="bg-blue-1 text-black q-mb-md">
              Доступы загружаются из базы данных. Администратор может назначить секретарю один или несколько факультетов.
            </q-banner>

            <div class="row q-col-gutter-sm q-mb-md">
              <div class="col-12 col-sm-6 col-md-3">
                <q-card flat bordered class="stat-card">
                  <q-card-section>
                    <div class="text-grey-7">Всего доступов</div>
                    <div class="text-h6 text-weight-bold">{{ accessRows.length }}</div>
                  </q-card-section>
                </q-card>
              </div>

              <div class="col-12 col-sm-6 col-md-3">
                <q-card flat bordered class="stat-card">
                  <q-card-section>
                    <div class="text-grey-7">Активных</div>
                    <div class="text-h6 text-weight-bold text-positive">{{ activeAccessCount }}</div>
                  </q-card-section>
                </q-card>
              </div>

              <div class="col-12 col-sm-6 col-md-3">
                <q-card flat bordered class="stat-card">
                  <q-card-section>
                    <div class="text-grey-7">Секретарей</div>
                    <div class="text-h6 text-weight-bold">{{ secretaryAccessCount }}</div>
                  </q-card-section>
                </q-card>
              </div>

              <div class="col-12 col-sm-6 col-md-3">
                <q-card flat bordered class="stat-card">
                  <q-card-section>
                    <div class="text-grey-7">Администраторов</div>
                    <div class="text-h6 text-weight-bold">{{ adminAccessCount }}</div>
                  </q-card-section>
                </q-card>
              </div>
            </div>

            <div v-if="accessLoading" class="q-pa-md text-grey-7">
              Загрузка доступов...
            </div>

            <q-table
              v-else
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

              <template #body-cell-facultyIds="props">
                <q-td :props="props">
                  <span v-if="props.row.role === 'ADMIN'" class="text-grey-7">
                    Все факультеты
                  </span>

                  <div v-else-if="props.row.facultyIds?.length" class="row q-gutter-xs">
                    <q-chip
                      v-for="facultyId in props.row.facultyIds"
                      :key="facultyId"
                      dense
                      outline
                      color="primary"
                      text-color="primary"
                    >
                      {{ facultyLabel(facultyId) }}
                    </q-chip>
                  </div>

                  <span v-else class="text-negative">
                    Не назначено
                  </span>
                </q-td>
              </template>

              <template #body-cell-active="props">
                <q-td :props="props">
                  <q-chip dense :color="props.row.active ? 'green-7' : 'grey-6'" text-color="white">
                    {{ props.row.active ? 'Активен' : 'Отключен' }}
                  </q-chip>
                </q-td>
              </template>

              <template #body-cell-createdAt="props">
                <q-td :props="props">
                  {{ formatDate(props.row.createdAt) }}
                </q-td>
              </template>

              <template #body-cell-updatedAt="props">
                <q-td :props="props">
                  {{ formatDate(props.row.updatedAt) }}
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

                    <q-btn
                      flat
                      dense
                      round
                      icon="delete"
                      color="negative"
                      @click="deleteAccess(props.row)"
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

            <div v-if="facultiesLoading" class="q-pa-md text-grey-7">
              Загрузка...
            </div>

            <div v-else-if="facultiesError" class="q-pa-md text-negative">
              {{ facultiesError }}
            </div>

            <q-table
              v-else
              :rows="filteredFacultyRows"
              :columns="facultyColumns"
              row-key="id"
              flat
              :pagination="{ rowsPerPage: 10 }"
              class="campus-table"
            >
              <template #body-cell-active="props">
                <q-td :props="props">
                  <q-chip dense :color="props.row.active ? 'green-7' : 'grey-6'" text-color="white">
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

    <q-dialog v-model="accessDialog.open" persistent>
      <q-card style="min-width: 560px; max-width: 95vw">
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
            hint="Можно использовать латинские буквы, цифры и символ _"
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
            @update:model-value="onAccessRoleChange"
          />

          <q-select
            v-if="accessDialog.form.role === 'SECRETARY'"
            v-model="accessDialog.form.facultyIds"
            outlined
            dense
            multiple
            use-chips
            use-input
            input-debounce="0"
            clearable
            :options="facultyOptions"
            emit-value
            map-options
            label="Факультеты"
            hint="Можно выбрать один или несколько факультетов"
          />

          <q-banner v-else rounded class="bg-grey-2 text-grey-8">
            Администратор имеет доступ ко всем факультетам.
          </q-banner>

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

    <q-dialog v-model="facultyDialog.open" persistent>
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
            hint="Например: 01, 02, 03"
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
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { getRequests, updateRequestStatus } from 'src/api/requests'
import {
  getFaculties,
  createFaculty,
  updateFaculty,
  toggleFacultyActive
} from 'src/api/faculties'
import {
  getAccessAccounts,
  createAccessAccount,
  updateAccessAccount,
  toggleAccessAccountActive,
  deleteAccessAccount
} from 'src/api/accessAccounts'
import { generateCommonRequestDocument } from 'src/api/requestDocuments'
import { getRegistrationNumbersByRequestIds } from 'src/api/requestRegistrationNumbers'

const router = useRouter()
const $q = useQuasar()

const tab = ref('requests')
const requestTab = ref('active')

const requestSearch = ref('')
const accessSearch = ref('')
const facultySearch = ref('')

const selectedRequests = ref([])

const requestsLoading = ref(false)
const requestsError = ref('')
const facultiesLoading = ref(false)
const facultiesError = ref('')
const accessLoading = ref(false)

const faculties = ref([])
const requests = ref([])
const accessRows = ref([])

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
  { label: 'Принята', value: 'ACCEPTED' },
  { label: 'В обработке', value: 'IN_WORK' },
  { label: 'Задерживается', value: 'DELAYED' },
  { label: 'Готово', value: 'READY' },
  { label: 'Отклонена', value: 'REJECTED' },
  { label: 'В архиве', value: 'ARCHIVED' },
  { label: 'Отменена', value: 'CANCELLED' }
]

const roleOptions = [
  { label: 'Администратор', value: 'ADMIN' },
  { label: 'Секретарь', value: 'SECRETARY' }
]

const facultyOptions = computed(() =>
  faculties.value.map((f) => ({
    label: `${facultyCode(f.id)} — ${f.name}`,
    value: f.id
  }))
)

const activeAccessCount = computed(() =>
  accessRows.value.filter((x) => x.active).length
)

const secretaryAccessCount = computed(() =>
  accessRows.value.filter((x) => x.role === 'SECRETARY').length
)

const adminAccessCount = computed(() =>
  accessRows.value.filter((x) => x.role === 'ADMIN').length
)

const requestColumns = [
  { name: 'registration', label: 'Рег. номера', field: 'registration', align: 'left' },
  { name: 'id', label: '№ заявки', field: 'id', sortable: true, align: 'left' },
  { name: 'fio', label: 'ФИО', field: 'fio', sortable: true, align: 'left' },
  { name: 'status', label: 'Статус', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'center' },
  { name: 'facultyId', label: 'Факультет', field: 'facultyId', align: 'left' },
  { name: 'courseGroup', label: 'Курс/группа', field: 'courseGroup', align: 'left' },
  { name: 'purpose', label: 'Куда нужна справка', field: 'purpose', align: 'left' },
  { name: 'qty', label: 'Кол-во', field: 'qty', sortable: true, align: 'left' },
  { name: 'type', label: 'Тип', field: 'type', align: 'left' },
  { name: 'period', label: 'Период', field: 'period', align: 'left' },
  { name: 'createdAt', label: 'Дата подачи', field: 'createdAt', sortable: true, align: 'left' }
]

const accessColumns = [
  { name: 'login', label: 'Логин', field: 'login', align: 'left', sortable: true },
  { name: 'fio', label: 'ФИО', field: 'fio', align: 'left', sortable: true },
  { name: 'role', label: 'Роль', field: 'role', align: 'left' },
  { name: 'facultyIds', label: 'Факультеты', field: 'facultyIds', align: 'left' },
  { name: 'active', label: 'Статус', field: 'active', align: 'left' },
  { name: 'createdAt', label: 'Создан', field: 'createdAt', align: 'left', sortable: true },
  { name: 'updatedAt', label: 'Изменён', field: 'updatedAt', align: 'left', sortable: true },
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

      if (requestFilters.value.onlyRegistered) {
        const hasRegistrationNumbers = r.registrationNumbers?.length || r.registrationNumber
        if (!hasRegistrationNumbers) return false
      }

      if (q) {
        const registrationText = r.registrationNumbers?.length
          ? r.registrationNumbers.map(formatRegistrationNumber).join(' ')
          : r.registrationNumber
            ? formatRegistration(r)
            : ''

        const haystack = [
          r.id,
          r.fio,
          facultyLabel(r.facultyId),
          r.courseGroup,
          r.purpose,
          statusLabel(r.status),
          registrationText
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

    const facultyText = row.role === 'ADMIN'
      ? 'все факультеты'
      : (row.facultyIds || []).map((facultyId) => facultyLabel(facultyId)).join(' ')

    return [
      row.login,
      row.fio,
      roleLabel(row.role),
      facultyText,
      row.active ? 'активен' : 'отключен'
    ]
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
    facultyIds: [],
    active: true,
    createdAt: null,
    updatedAt: null
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

async function loadRequests() {
  requestsLoading.value = true
  requestsError.value = ''

  try {
    const { data } = await getRequests()
    const normalizedRows = data.map(normalizeRequestRow)
    const requestIds = normalizedRows.map(row => row.id)

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

    requests.value = normalizedRows.map(row => ({
      ...row,
      registrationNumbers: registrationNumbersByRequestId[row.id] || []
    }))
  } catch (err) {
    console.error(err)
    requestsError.value = 'Не удалось загрузить заявки'
  } finally {
    requestsLoading.value = false
  }
}

async function loadFaculties() {
  facultiesLoading.value = true
  facultiesError.value = ''

  try {
    const { data } = await getFaculties()
    faculties.value = data.map((f) => ({
      id: f.id,
      code: f.code,
      name: f.name,
      nextRegistrationNumber: f.nextRegistrationNumber,
      active: !!f.isActive
    }))
  } catch (err) {
    console.error(err)
    facultiesError.value = 'Не удалось загрузить факультеты'
  } finally {
    facultiesLoading.value = false
  }
}

async function loadAccessRows() {
  accessLoading.value = true

  try {
    const { data } = await getAccessAccounts()

    accessRows.value = data.map((row) => ({
      id: row.id,
      login: row.login,
      fio: row.fullName,
      role: row.role,
      facultyIds: row.facultyIds || [],
      active: !!row.isActive,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    }))

    sortAccessRows()
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось загрузить доступы.',
      position: 'top'
    })
  } finally {
    accessLoading.value = false
  }
}

function sortAccessRows() {
  accessRows.value.sort((a, b) => {
    if (a.role === 'ADMIN' && b.role !== 'ADMIN') return -1
    if (a.role !== 'ADMIN' && b.role === 'ADMIN') return 1

    return a.login.localeCompare(b.login)
  })
}

function normalizeRequestRow(r) {
  return {
    id: r.id,
    fio: r.studentFullName || '—',
    courseGroup: [r.course ? `${r.course} курс` : null, r.groupName].filter(Boolean).join(' / ') || '—',
    facultyId: r.facultyId,
    purpose: r.purpose || '—',
    qty: r.copiesCount || 1,
    type: r.certificateType,
    periodFrom: formatDate(r.periodFrom),
    periodTo: formatDate(r.periodTo),
    createdAt: formatDate(r.createdAt),
    status: r.status,
    archived: r.status === 'ARCHIVED',
    registrationNumber: r.registrationNumber,
    registrationYear: r.registrationYear,
    registrationNumbers: []
  }
}

function facultyLabel(facultyId) {
  if (!facultyId) return '—'

  const faculty = faculties.value.find((f) => f.id === facultyId)
  return faculty ? `${facultyCode(facultyId)} — ${faculty.name}` : '—'
}

function facultyCode(facultyId) {
  const faculty = faculties.value.find((f) => f.id === facultyId)

  if (faculty?.code && /^\d+$/.test(String(faculty.code))) {
    return String(faculty.code).padStart(2, '0')
  }

  return String(facultyId).padStart(2, '0')
}

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return date.toLocaleDateString('ru-RU')
}

function formatRegistration(row) {
  if (!row.registrationNumber || !row.registrationYear) return ''

  return formatRegistrationNumber({
    facultyId: row.facultyId,
    registrationNumber: row.registrationNumber,
    registrationYear: row.registrationYear
  })
}

function formatRegistrationNumber(number) {
  if (!number?.registrationNumber || !number?.registrationYear) return ''

  const code = facultyCode(number.facultyId)
  const regNumber = String(number.registrationNumber).padStart(4, '0')
  const year = String(number.registrationYear).slice(-2)

  return `${code}-${regNumber}/${year}`
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
    REJECTED: 'red-7',
    ARCHIVED: 'blue-grey-7',
    CANCELLED: 'deep-orange-6'
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

function resetRequestFilters() {
  requestFilters.value.facultyId = null
  requestFilters.value.type = null
  requestFilters.value.status = null
  requestFilters.value.onlyRegistered = false
}

function openRequest(id) {
  router.push(`/secretary/${id}`)
}

async function bulkArchiveRequests() {
  if (requestTab.value !== 'active') return

  try {
    for (const row of selectedRequests.value) {
      await updateRequestStatus(row.id, 'ARCHIVED', 'Заявка перемещена в архив.')
    }

    selectedRequests.value = []
    await loadRequests()

    $q.notify({
      type: 'positive',
      message: 'Заявки перенесены в архив.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось перенести заявки в архив.',
      position: 'top'
    })
  }
}

async function bulkUnarchiveRequests() {
  if (requestTab.value !== 'archive') return

  try {
    for (const row of selectedRequests.value) {
      await updateRequestStatus(row.id, 'ACCEPTED', 'Заявка возвращена из архива.')
    }

    selectedRequests.value = []
    await loadRequests()

    $q.notify({
      type: 'positive',
      message: 'Заявки возвращены в активные.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось вернуть заявки в активные.',
      position: 'top'
    })
  }
}

async function generateCommonDocument() {
  if (!selectedRequests.value.length) {
    $q.notify({
      type: 'negative',
      message: 'Выберите хотя бы одну заявку.',
      position: 'top'
    })
    return
  }

  const hasNoStipendRequests = selectedRequests.value.some(
    (request) => request.type === 'NO_STIPEND'
  )

  if (hasNoStipendRequests) {
    $q.notify({
      type: 'negative',
      message: 'Общий документ можно сформировать только для справок с отметкой о стипендии.',
      position: 'top'
    })
    return
  }

  try {
    const requestIds = selectedRequests.value.map((request) => request.id)

    const response = await generateCommonRequestDocument(requestIds)

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    })

    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = 'Общий_документ.docx'
    link.click()

    URL.revokeObjectURL(url)

    $q.notify({
      type: 'positive',
      message: 'Общий документ сформирован.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: err.response?.data?.message || 'Не удалось сформировать документ.',
      position: 'top'
    })
  }
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
      facultyIds: [],
      active: true,
      createdAt: null,
      updatedAt: null
    }
  }
}

function openEditAccessDialog(row) {
  accessDialog.value = {
    open: true,
    mode: 'edit',
    form: {
      id: row.id,
      login: row.login,
      fio: row.fio,
      role: row.role,
      facultyIds: [...(row.facultyIds || [])],
      active: row.active,
      createdAt: row.createdAt || null,
      updatedAt: row.updatedAt || null
    }
  }
}

function onAccessRoleChange(role) {
  if (role === 'ADMIN') {
    accessDialog.value.form.facultyIds = []
  }
}

async function saveAccess() {
  const form = accessDialog.value.form
  const loginRegex = /^[a-zA-Z0-9_]+$/

  if (!form.login?.trim() || !form.fio?.trim() || !form.role) {
    $q.notify({
      type: 'negative',
      message: 'Заполни логин, ФИО и роль.',
      position: 'top'
    })
    return
  }

  const normalizedLogin = form.login.trim()

  if (!loginRegex.test(normalizedLogin)) {
    $q.notify({
      type: 'negative',
      message: 'Логин может содержать только латинские буквы, цифры и символ _.',
      position: 'top'
    })
    return
  }

  if (form.role === 'SECRETARY' && (!form.facultyIds || !form.facultyIds.length)) {
    $q.notify({
      type: 'negative',
      message: 'Для секретаря нужно выбрать хотя бы один факультет.',
      position: 'top'
    })
    return
  }

  const duplicate = accessRows.value.find((row) =>
    row.login.toLowerCase() === normalizedLogin.toLowerCase() &&
    row.id !== form.id
  )

  if (duplicate) {
    $q.notify({
      type: 'negative',
      message: 'Пользователь с таким логином уже есть.',
      position: 'top'
    })
    return
  }

  const payload = {
    login: normalizedLogin,
    fullName: form.fio.trim(),
    role: form.role,
    isActive: form.active,
    facultyIds: form.role === 'ADMIN' ? [] : [...form.facultyIds]
  }

  try {
    if (accessDialog.value.mode === 'create') {
      await createAccessAccount(payload)
    } else {
      await updateAccessAccount(form.id, payload)
    }

    accessDialog.value.open = false
    await loadAccessRows()

    $q.notify({
      type: 'positive',
      message: 'Доступ сохранён.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: err.response?.data?.message || 'Не удалось сохранить доступ.',
      position: 'top'
    })
  }
}

async function toggleAccessStatus(row) {
  try {
    await toggleAccessAccountActive(row.id)
    await loadAccessRows()

    $q.notify({
      type: 'positive',
      message: row.active ? 'Доступ отключён.' : 'Доступ активирован.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: err.response?.data?.message || 'Не удалось изменить статус доступа.',
      position: 'top'
    })
  }
}

function deleteAccess(row) {
  $q.dialog({
    title: 'Удаление доступа',
    message: `Удалить доступ для пользователя ${row.login}?`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      await deleteAccessAccount(row.id)
      await loadAccessRows()

      $q.notify({
        type: 'positive',
        message: 'Доступ удалён.',
        position: 'top'
      })
    } catch (err) {
      console.error(err)

      $q.notify({
        type: 'negative',
        message: err.response?.data?.message || 'Не удалось удалить доступ.',
        position: 'top'
      })
    }
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

async function saveFaculty() {
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

  try {
    if (facultyDialog.value.mode === 'create') {
      await createFaculty({
        code: normalizedCode,
        name: form.name,
        nextRegistrationNumber: form.nextRegistrationNumber,
        isActive: form.active
      })
    } else {
      await updateFaculty(form.id, {
        code: normalizedCode,
        name: form.name,
        nextRegistrationNumber: form.nextRegistrationNumber,
        isActive: form.active
      })
    }

    facultyDialog.value.open = false
    await loadFaculties()
    await loadRequests()
    await loadAccessRows()

    $q.notify({
      type: 'positive',
      message: 'Факультет сохранён.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось сохранить факультет.',
      position: 'top'
    })
  }
}

async function toggleFacultyStatus(row) {
  try {
    await toggleFacultyActive(row.id)
    await loadFaculties()
    await loadRequests()
    await loadAccessRows()

    $q.notify({
      type: 'positive',
      message: row.active ? 'Факультет скрыт.' : 'Факультет активирован.',
      position: 'top'
    })
  } catch (err) {
    console.error(err)

    $q.notify({
      type: 'negative',
      message: 'Не удалось изменить статус факультета.',
      position: 'top'
    })
  }
}

onMounted(async () => {
  await loadFaculties()
  await loadAccessRows()
  await loadRequests()
})
</script>

<style scoped>
.card {
  border-radius: 14px;
}

.stat-card {
  border-radius: 12px;
  background: #ffffff;
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

.registration-chip {
  width: fit-content;
  color: #7a0019;
  border-color: #7a0019;
  font-weight: 500;
}
</style>
