const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),

    children: [
      {
        path: '',
        component: () => import('pages/IndexPage.vue')
      },

      // СТУДЕНТ
      {
        path: 'student',
        component: () => import('pages/student/StudentRequestsPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['STUDENT']
        }
      },

      {
        path: 'student/new',
        component: () => import('pages/student/StudentRequestNewPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['STUDENT']
        }
      },

      {
        path: 'student/:id',
        component: () => import('pages/student/StudentRequestViewPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['STUDENT']
        }
      },

      // СЕКРЕТАРЬ
      {
        path: 'secretary',
        component: () => import('pages/secretary/SecretaryRequestsPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['SECRETARY']
        }
      },

      {
        path: 'secretary/:id',
        component: () => import('pages/secretary/SecretaryRequestViewPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['SECRETARY', 'ADMIN']
        }
      },

      // АДМИН
      {
        path: 'admin',
        component: () => import('pages/admin/AdminPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN']
        }
      },

      // РУЧНОЕ СОЗДАНИЕ ЗАЯВКИ
      {
        path: 'manual-request',
        component: () => import('pages/staff/ManualRequestPage.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'SECRETARY']
        }
      }
    ]
  },

  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
