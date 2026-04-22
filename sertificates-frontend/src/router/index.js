import { defineRouter } from '#q-app/wrappers'
import {
  createRouter,
  createMemoryHistory,
  createWebHistory,
  createWebHashHistory,
} from 'vue-router'
import routes from './routes'
import { useAuthStore } from 'src/stores/auth'

export default defineRouter(function () {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : process.env.VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  })

  Router.beforeEach((to) => {
    const auth = useAuthStore()

    if (!to.meta?.requiresAuth) return true
    if (!auth.login) return { path: '/' }

    const allowedRoles = to.meta?.roles || []
    if (allowedRoles.length && !allowedRoles.includes(auth.role)) {
      return { path: '/' }
    }

    return true
  })

  return Router
})
