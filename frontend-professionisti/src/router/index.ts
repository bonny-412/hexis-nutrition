import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth: boolean
  }
}

export function guardiaAutenticazione(
  to: Pick<RouteLocationNormalized, 'meta' | 'name' | 'fullPath'>,
  auth: { token: string | null },
) {
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'dashboard' }
  }
  return true as const
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { requiresAuth: false } },
    { path: '/password-dimenticata', name: 'password-dimenticata', component: () => import('@/views/auth/PasswordDimenticataView.vue'), meta: { requiresAuth: false } },
    { path: '/reset-password', name: 'reset-password', component: () => import('@/views/auth/ResetPasswordView.vue'), meta: { requiresAuth: false } },
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti', name: 'pazienti', component: () => import('@/views/pazienti/PazientiListView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: () => import('@/views/pazienti/PazienteNuovoView.vue'), meta: { requiresAuth: true } },
    { path: '/pazienti/:id', name: 'paziente-dettaglio', component: () => import('@/views/pazienti/PazienteDettaglioView.vue'), meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => guardiaAutenticazione(to, useAuthStore()))

export default router
