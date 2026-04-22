import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    login: null,
    role: null,
    facultyId: null
  }),

  getters: {
    isAuthed: (s) => Boolean(s.role)
  },

  actions: {
    // ✅ универсальный вход (его ждёт LoginPage.vue)
    loginAs ({ login, role, facultyId = null }) {
      this.login = login ?? null
      this.role = role ?? null
      this.facultyId = facultyId ?? null
    },

    // ✅ на всякий: если где-то в проекте используются старые моки
    mockLoginAsAdmin () {
      this.loginAs({ login: 'admin', role: 'ADMIN' })
    },
    mockLoginAsSecretary (facultyId) {
      this.loginAs({ login: `sec_${String(facultyId).toLowerCase()}`, role: 'SECRETARY', facultyId })
    },
    mockLoginAsStudent (login, facultyId) {
      this.loginAs({ login, role: 'STUDENT', facultyId })
    },

    logout () {
      this.login = null
      this.role = null
      this.facultyId = null

      try {
        localStorage.removeItem('certificates-auth')
      } catch {
        // ignore
      }
    }
  },

  persist: {
    key: 'certificates-auth',
    paths: ['login', 'role', 'facultyId']
  }
})
