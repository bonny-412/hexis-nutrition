import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { configureApiClient } from './api/client'

const pinia = createPinia()
const auth = useAuthStore(pinia)
const app = createApp(App, { ripristinaSessione: auth.ripristinaSessione })
app.use(pinia)
app.use(router)

useThemeStore()

configureApiClient({
  getToken: () => auth.token,
  onUnauthorized: () => {
    auth.logout()
    if (router.currentRoute.value.meta.requiresAuth) router.push({ name: 'login' })
  },
})

void router.isReady().then(() => app.mount('#app'))
