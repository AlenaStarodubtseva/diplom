<template>
  <q-page class="column flex-center q-pa-lg">
    <q-card style="width: 420px; max-width: 90vw;">
      <q-card-section>
        <div class="text-h6">
          Вход в модуль «Справки»
        </div>
        <div class="text-caption text-grey-7">
          Макет SSO: Кампус БГПУ позже будет передавать логин и роль пользователя
        </div>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-gutter-sm">
        <q-btn
          color="primary"
          class="full-width"
          label="Войти как admin"
          @click="loginAdmin"
        />

        <q-btn
          color="secondary"
          class="full-width"
          label="Войти как secretary_f01"
          @click="loginSecretary"
        />

        <q-btn
          outline
          class="full-width"
          label="Войти как student_2014"
          @click="loginStudent"
        />
      </q-card-section>

      <q-separator />

      <q-card-section>
        <div class="text-subtitle2 q-mb-sm">
          Текущее состояние SSO
        </div>
        <pre class="bg-grey-2 q-pa-sm rounded-borders">
{{ stateDump }}
        </pre>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const stateDump = computed(() => ({
  login: auth.login,
  role: auth.role,
  facultyId: auth.facultyId
}))

function loginAdmin () {
  auth.loginAs({
    login: 'admin',
    role: 'ADMIN',
    facultyId: null
  })

  router.push('/admin')
}

function loginSecretary () {
  auth.loginAs({
    login: 'secretary_f01',
    role: 'SECRETARY',
    facultyId: null
  })

  router.push('/secretary')
}

function loginStudent () {
  auth.loginAs({
    login: 'student_2014',
    role: 'STUDENT',
    facultyId: 'F01'
  })

  router.push('/student')
}
</script>
