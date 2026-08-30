import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { useAuthStore } from './stores/auth'
import { configureApiClient } from './api/client'

const app = createApp(App)
app.use(createPinia())
app.use(router)

const auth = useAuthStore()

configureApiClient({
  getToken: () => auth.token,
  onUnauthorized: () => {
    auth.logout()
    router.push({ name: 'login' })
  },
})

auth.ripristinaSessione().finally(() => {
  app.mount('#app')
})
