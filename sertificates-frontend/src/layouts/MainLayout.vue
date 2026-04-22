<template>
  <q-layout view="lHh Lpr lFf">

    <!-- Верхняя панель -->
    <q-header elevated class="campus-header">
      <q-toolbar>

        <!-- Бордовая иконка -->
        <q-icon name="school" size="md" class="q-mr-sm campus-accent" />

        <!-- Бордовый заголовок -->
        <q-toolbar-title class="campus-title">
          Модуль «Справки»
        </q-toolbar-title>

        <q-space />

        <!-- Кнопка выхода -->
        <q-btn
          v-if="auth.isAuthed"
          flat
          class="campus-accent"
          icon="logout"
          :label="exitLabel"
          @click="resetMock"
        />
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>

  </q-layout>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth'

const router = useRouter()
const auth = useAuthStore()

const exitLabel = computed(() => {
  if (!auth.role) return ''
  if (auth.facultyId) {
    return `Выход (${auth.role} ${auth.facultyId})`
  }
  return `Выход (${auth.role})`
})

function resetMock () {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
/* Белая верхняя панель */
.campus-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e0e0e0;
}

/* Бордовый цвет кампуса */
.campus-accent {
  color: #7a0019;
}

/* Бордовый заголовок */
.campus-title {
  color: #7a0019;
  font-weight: 600;
}
</style>
