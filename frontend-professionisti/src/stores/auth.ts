import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { ApiError, eErroreConnessione } from '@/api/client'

const STORAGE_KEY = 'hexis-auth-token'

interface Professionista {
  id: string
  nome: string
  cognome: string
  email: string
  ruolo: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(
    localStorage.getItem(STORAGE_KEY) ?? sessionStorage.getItem(STORAGE_KEY),
  )
  const professionista = ref<Professionista | null>(null)
  // Sessione scaduta mentre si lavora (bug S5 della verifica 3): il token non vale più, ma si resta sulla pagina
  // con i dati inseriti e si chiede la password sopra di essa (SessioneScadutaDialog), invece di portare al login.
  const sessioneScaduta = ref(false)
  let ricordamiSessioneScaduta = true

  function logout() {
    token.value = null
    professionista.value = null
    sessioneScaduta.value = false
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.removeItem(STORAGE_KEY)
  }

  /** Il token è scaduto durante la sessione: lo si toglie, ma si tiene il professionista per chiedergli la password. */
  function segnalaSessioneScaduta() {
    if (sessioneScaduta.value) return
    ricordamiSessioneScaduta = localStorage.getItem(STORAGE_KEY) !== null
    token.value = null
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.removeItem(STORAGE_KEY)
    sessioneScaduta.value = true
  }

  /** Nuovo accesso dalla finestra "Sessione scaduta", con la stessa email e la stessa scelta di "Ricordami". */
  async function riaccedi(password: string) {
    if (!professionista.value) throw new Error('Nessuna sessione da riprendere')
    await login(professionista.value.email, password, ricordamiSessioneScaduta)
    sessioneScaduta.value = false
  }

  function salvaToken(nuovoToken: string, ricordami: boolean) {
    token.value = nuovoToken
    const storage = ricordami ? localStorage : sessionStorage
    storage.setItem(STORAGE_KEY, nuovoToken)
  }

  async function caricaProfilo() {
    professionista.value = await authApi.me()
  }

  async function login(email: string, password: string, ricordami: boolean) {
    const risposta = await authApi.login({ email, password })
    if (risposta.ruolo !== 'PROFESSIONISTA') {
      throw new ApiError(403, 'Accesso riservato ai professionisti')
    }
    salvaToken(risposta.token, ricordami)
    await caricaProfilo()
  }

  async function ripristinaSessione() {
    if (!token.value) return true
    try {
      await caricaProfilo()
    } catch (errore) {
      if (errore instanceof ApiError && errore.status === 401) {
        logout()
      }
      return !eErroreConnessione(errore)
    }
    return true
  }

  return { token, professionista, sessioneScaduta, login, ripristinaSessione, logout, segnalaSessioneScaduta, riaccedi }
})
