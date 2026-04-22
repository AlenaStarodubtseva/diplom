<template>
  <q-page class="q-pa-md">
    <div v-if="loading" class="text-grey-7">Загрузка...</div>

    <div v-else-if="error" class="text-negative">
      {{ error }}
    </div>

    <q-card v-else flat bordered class="q-pa-md">
      <div class="text-h6 q-mb-md">
        Заявка №{{ request.id }}
      </div>

      <div class="q-mb-sm"><b>Тип:</b> {{ request.certificateType }}</div>
      <div class="q-mb-sm"><b>Цель:</b> {{ request.purpose }}</div>
      <div class="q-mb-sm"><b>Статус:</b> {{ request.status }}</div>
      <div class="q-mb-sm"><b>ФИО:</b> {{ request.studentFullName }}</div>
      <div class="q-mb-sm"><b>Группа:</b> {{ request.groupName || '—' }}</div>
      <div class="q-mb-sm"><b>Курс:</b> {{ request.course || '—' }}</div>
      <div class="q-mb-sm"><b>Факультет:</b> {{ request.facultyName || '—' }}</div>
      <div class="q-mb-sm"><b>Нужен скан:</b> {{ request.needScan ? 'Да' : 'Нет' }}</div>

      <q-input
        v-model="request.studentComment"
        type="textarea"
        outlined
        autogrow
        label="Комментарий"
        class="q-mt-md"
      />

      <div class="row q-col-gutter-sm q-mt-md">
        <div class="col">
          <q-btn
            flat
            color="grey-8"
            label="Назад"
            class="full-width"
            @click="$router.push('/student')"
          />
        </div>
        <div class="col">
          <q-btn
            unelevated
            color="primary"
            label="Сохранить комментарий"
            class="full-width"
            :loading="saving"
            @click="saveComment"
          />
        </div>
      </div>
    </q-card>
  </q-page>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { getRequestById, updateRequest } from 'src/api/requests'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()

const request = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')

async function loadRequest() {
  loading.value = true
  error.value = ''

  try {
    const response = await getRequestById(route.params.id)
    request.value = response.data
  } catch (err) {
    console.error(err)
    error.value = 'Не удалось загрузить заявку'
  } finally {
    loading.value = false
  }
}

async function saveComment() {
  if (!request.value) return

  saving.value = true

  try {
    await updateRequest(request.value.id, request.value)

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
    saving.value = false
  }
}

onMounted(() => {
  loadRequest()
})
</script>
