<template>
  <q-page class="q-pa-md">
    <q-card class="q-pa-md card">
      <div class="text-h6 q-mb-sm">Справка об обучении №{{ req.number }}</div>
      <div class="text-caption text-grey-7 q-mb-md">от {{ req.date }}</div>

      <div class="q-mb-sm"><b>Статус:</b> {{ req.statusText }}</div>
      <div class="q-mb-sm"><b>Нужна электронная копия:</b> {{ req.needScan ? 'Да' : 'Нет' }}</div>

      <div class="q-mt-md text-subtitle2">Получатель</div>
      <div><b>ФИО:</b> {{ req.fio }}</div>
      <div><b>Курс:</b> {{ req.course }}</div>
      <div><b>Группа:</b> {{ req.group }}</div>

      <div class="q-mt-md"><b>Куда требуется справка:</b> {{ req.purpose }}</div>
      <div class="q-mt-sm"><b>Комментарий студента:</b> {{ req.studentComment || 'Комментарий отсутствует' }}</div>

      <q-separator class="q-my-md" />

      <div class="text-subtitle2 q-mb-sm">Ваш комментарий к заявке</div>
      <div class="row items-center q-col-gutter-sm">
        <div class="col">
          <q-input outlined v-model="newComment" placeholder="Комментарий..." />
        </div>
        <div class="col-auto">
          <q-btn outline label="Отправить" @click="sendComment" />
        </div>
      </div>

      <div class="text-subtitle2 q-mt-lg q-mb-sm">История обработки</div>
      <div v-for="h in history" :key="h.dt" class="text-body2 q-mb-xs">
        {{ h.dt }} — {{ h.text }}
      </div>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const id = route.params.id

// мок-данные (потом из API)
const req = {
  id,
  number: id,
  date: '08.12.2025',
  statusText: 'В обработке',
  needScan: false,
  fio: 'Стародубцева Алёна Константиновна',
  course: 4,
  group: '4ИС',
  purpose: 'По месту требования',
  studentComment: ''
}

const history = ref([
  { dt: '08.12.2025 10:43', text: 'Заявка создана' },
  { dt: '08.12.2025 11:10', text: 'В обработке' }
])

const newComment = ref('')

function sendComment () {
  if (!newComment.value.trim()) return
  // потом: POST в API
  history.value.push({ dt: 'сейчас', text: 'Комментарий студента отправлен' })
  newComment.value = ''
}
</script>

<style scoped>
.card {
  border-radius: 14px;
}
</style>
