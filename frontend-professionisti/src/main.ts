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
    // Durante il lavoro si chiede la password sopra la pagina, così i dati non salvati restano (bug S5 della
    // verifica 3). All'avvio, senza un profilo caricato, si va al login come prima.
    if (auth.professionista && router.currentRoute.value.meta.requiresAuth) {
      auth.segnalaSessioneScaduta()
      return
    }
    auth.logout()
    if (router.currentRoute.value.meta.requiresAuth) router.push({ name: 'login' })
  },
})

void router.isReady().then(() => app.mount('#app'))
