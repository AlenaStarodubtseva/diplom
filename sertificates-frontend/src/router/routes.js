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
        component: () => import('pages/student/StudentRequestsPage.vue')
      },

      {
        path: 'student/new',
        component: () => import('pages/student/StudentRequestNewPage.vue')
      },

      {
        path: 'student/:id',
        component: () => import('pages/student/StudentRequestViewPage.vue')
      },

      // СЕКРЕТАРЬ

      {
        path: 'secretary',
        component: () => import('pages/secretary/SecretaryRequestsPage.vue')
      },

      {
        path: 'secretary/:id',
        component: () => import('pages/secretary/SecretaryRequestViewPage.vue')
      },

      // АДМИН

      {
        path: 'admin',
        component: () => import('pages/admin/AdminPage.vue')
      },

      // РУЧНОЕ СОЗДАНИЕ ЗАЯВКИ

      {
        path: 'manual-request',
        component: () => import('pages/staff/ManualRequestPage.vue')
      }
    ]
  },

  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
