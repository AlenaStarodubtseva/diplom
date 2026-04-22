<template>
  <q-page class="q-pa-md">
    <div class="text-h6 q-mb-md">Новая заявка</div>

    <q-card flat bordered class="q-pa-md">
      <q-form @submit="submitForm" class="q-gutter-md">
        <q-select
          v-model="form.certificateType"
          :options="certificateTypeOptions"
          label="Тип справки"
          outlined
          emit-value
          map-options
          :rules="[val => !!val || 'Выберите тип справки']"
        />

        <q-input
          v-model="form.purpose"
          label="Куда требуется справка"
          outlined
          :rules="[val => !!val || 'Укажите цель']"
        />

        <q-input
          v-model.number="form.copiesCount"
          type="number"
          label="Количество экземпляров"
          outlined
          min="1"
        />

        <q-toggle
          v-model="form.needScan"
          label="Нужен скан справки"
        />

        <q-input
          v-model="form.studentFullName"
          label="ФИО"
          outlined
          :rules="[val => !!val || 'Укажите ФИО']"
        />

        <q-input
          v-model="form.groupName"
          label="Группа"
          outlined
        />

        <q-input
          v-model.number="form.course"
          type="number"
          label="Курс"
          outlined
        />

        <q-input
          v-model="form.facultyName"
          label="Факультет"
          outlined
        />

        <q-input
          v-model="form.studentComment"
          type="textarea"
          label="Комментарий"
          outlined
          autogrow
        />

        <div class="row q-col-gutter-sm q-mt-md">
          <div class="col">
            <q-btn
              flat
              color="grey-8"
              label="Отмена"
              class="full-width"
              @click="$router.push('/student')"
            />
          </div>
          <div class="col">
            <q-btn
              unelevated
              color="primary"
              label="Отправить заявку"
              type="submit"
              class="full-width"
              :loading="saving"
            />
          </div>
        </div>
      </q-form>
    </q-card>
  </q-page>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { createRequest } from 'src/api/requests'

const router = useRouter()
const $q = useQuasar()

const saving = ref(false)

const certificateTypeOptions = [
  { label: 'Без отметки о стипендии', value: 'NO_STIPEND' },
  { label: 'Со стипендией', value: 'WITH_STIPEND' }
]

const form = reactive({
  facultyId: 1,
  certificateType: 'NO_STIPEND',
  purpose: '',
  copiesCount: 1,
  periodFrom: null,
  periodTo: null,
  needScan: false,
  status: 'NEW',
  studentComment: '',
  secretaryComment: '',
  studentFullName: '',
  groupName: '',
  course: null,
  facultyName: '',
  registrationNumber: null,
  registrationYear: null,
  registeredAt: null,
  issuedAt: null,
  acceptedAt: null,
  completedAt: null,
  archivedAt: null,
  isDeleted: false
})

async function submitForm() {
  saving.value = true

  try {
    await createRequest({ ...form })

    $q.notify({
      type: 'positive',
      message: 'Заявка успешно создана'
    })

    router.push('/student')
  } catch (err) {
    console.error(err)
    $q.notify({
      type: 'negative',
      message: 'Не удалось создать заявку'
    })
  } finally {
    saving.value = false
  }
}
</script>
