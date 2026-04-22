<template>
  <q-layout view="lHh Lpr lFf">

    <q-header elevated class="campus-header">
      <q-toolbar class="campus-toolbar">

        <div class="header-title">
          <q-icon name="school" size="28px" class="header-icon" />

          <div class="title-wrap">
            <div class="title-text full-title">Модуль «Справки»</div>
            <div class="title-text short-title">Справки</div>
          </div>
        </div>

        <q-space />

        <q-btn
          v-if="auth.isAuthed"
          flat
          class="campus-accent exit-btn"
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

function resetMock() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.campus-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e0e0e0;
}

.campus-toolbar {
  min-height: 64px;
  gap: 12px;
}

.campus-accent {
  color: #7a0019;
}

.header-title {
  display: flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  gap: 10px;
}

.header-icon {
  color: #7a0019;
  flex-shrink: 0;
}

.title-wrap {
  min-width: 0;
}

.title-text {
  font-weight: 700;
  color: #7a0019;
  line-height: 1.1;
  white-space: nowrap;
}

.full-title {
  font-size: 18px;
  display: block;
}

.short-title {
  display: none;
  font-size: 18px;
}

.exit-btn {
  flex-shrink: 0;
}

@media (max-width: 700px) {
  .campus-toolbar {
    min-height: 60px;
    padding-left: 12px;
    padding-right: 8px;
  }

  .full-title {
    display: none;
  }

  .short-title {
    display: block;
  }

  .exit-btn {
    min-width: auto;
    padding-left: 8px;
    padding-right: 8px;
  }

  .exit-btn :deep(.q-btn__content) {
    gap: 4px;
  }

  .exit-btn :deep(.q-btn__label) {
    font-size: 12px;
    white-space: nowrap;
  }
}

@media (max-width: 460px) {
  .short-title {
    font-size: 16px;
  }

  .header-icon {
    font-size: 24px !important;
  }

  .exit-btn :deep(.q-btn__label) {
    font-size: 11px;
  }
}
</style>
