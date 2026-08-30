import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/client'

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

  function logout() {
    token.value = null
    professionista.value = null
    localStorage.removeItem(STORAGE_KEY)
    sessionStorage.removeItem(STORAGE_KEY)
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
    if (!token.value) return
    try {
      await caricaProfilo()
    } catch (errore) {
      if (errore instanceof ApiError && errore.status === 401) {
        logout()
      }
    }
  }

  return { token, professionista, login, ripristinaSessione, logout }
})
