const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/auth/LoginPage.vue') },

      { path: 'student', component: () => import('pages/student/StudentRequestsPage.vue') },
      { path: 'student/new', component: () => import('pages/student/StudentRequestNewPage.vue') },
      { path: 'student/:id', component: () => import('pages/student/StudentRequestViewPage.vue') },

      { path: 'secretary', component: () => import('pages/secretary/SecretaryRequestsPage.vue') },
      { path: 'secretary/:id', component: () => import('pages/secretary/SecretaryRequestViewPage.vue') },

      { path: 'admin', component: () => import('pages/admin/AdminPage.vue') },

      { path: 'manual-request', component: () => import('pages/shared/ManualRequestCreatePage.vue') }
    ]
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }

]

export default routes
