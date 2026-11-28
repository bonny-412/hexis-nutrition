import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import * as authApi from '@/api/auth'
import { ApiError, ErroreRete } from '@/api/client'

vi.mock('@/api/auth')

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('dopo il login salva il token in localStorage se ricordami è true', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()

    await store.login('a@b.it', 'password123', true)

    expect(store.token).toBe('abc')
    expect(localStorage.getItem('hexis-auth-token')).toBe('abc')
    expect(sessionStorage.getItem('hexis-auth-token')).toBeNull()
    expect(store.professionista?.nome).toBe('Anna')
  })

  it('dopo il login salva il token in sessionStorage se ricordami è false', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()

    await store.login('a@b.it', 'password123', false)

    expect(sessionStorage.getItem('hexis-auth-token')).toBe('abc')
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
  })

  it('logout pulisce token, profilo ed entrambe le storage', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()
    await store.login('a@b.it', 'password123', true)

    store.logout()

    expect(store.token).toBeNull()
    expect(store.professionista).toBeNull()
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
  })

  it('ripristinaSessione fa logout se il token salvato non è più valido (401)', async () => {
    localStorage.setItem('hexis-auth-token', 'scaduto')
    vi.mocked(authApi.me).mockRejectedValue(new ApiError(401, 'Sessione scaduta'))
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(store.token).toBeNull()
  })

  it('ripristinaSessione non fa logout per un errore non-401 (es. rete assente)', async () => {
    localStorage.setItem('hexis-auth-token', 'valido')
    vi.mocked(authApi.me).mockRejectedValue(new ErroreRete(new TypeError('Failed to fetch')))
    const store = useAuthStore()

    await expect(store.ripristinaSessione()).resolves.toBe(false)

    expect(store.token).toBe('valido')
  })

  // Verifica 3 prima del rilascio (28/11/2026), bug S5: a sessione scaduta si chiede la password sopra la pagina.
  it('segnalaSessioneScaduta toglie il token ma tiene il professionista, per chiedergli la password', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()
    await store.login('a@b.it', 'password123', true)

    store.segnalaSessioneScaduta()

    expect(store.sessioneScaduta).toBe(true)
    expect(store.token).toBeNull()
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
    expect(store.professionista?.email).toBe('a@b.it')
  })

  it('riaccedi usa la stessa email e la stessa scelta di "Ricordami", poi chiude la sessione scaduta', async () => {
    vi.mocked(authApi.login)
      .mockResolvedValueOnce({ token: 'primo', ruolo: 'PROFESSIONISTA' })
      .mockResolvedValueOnce({ token: 'secondo', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()
    await store.login('a@b.it', 'password123', false)
    store.segnalaSessioneScaduta()

    await store.riaccedi('password123')

    expect(authApi.login).toHaveBeenLastCalledWith({ email: 'a@b.it', password: 'password123' })
    expect(store.sessioneScaduta).toBe(false)
    expect(store.token).toBe('secondo')
    expect(sessionStorage.getItem('hexis-auth-token')).toBe('secondo')
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
  })

  it('se riaccedi fallisce la sessione resta scaduta', async () => {
    vi.mocked(authApi.login)
      .mockResolvedValueOnce({ token: 'primo', ruolo: 'PROFESSIONISTA' })
      .mockRejectedValueOnce(new ApiError(401, 'credenziali non valide'))
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA' })
    const store = useAuthStore()
    await store.login('a@b.it', 'password123', true)
    store.segnalaSessioneScaduta()

    await expect(store.riaccedi('sbagliata')).rejects.toBeInstanceOf(ApiError)

    expect(store.sessioneScaduta).toBe(true)
    expect(store.token).toBeNull()
  })

  it('ripristinaSessione non chiama /auth/me se non c\'è un token salvato', async () => {
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(authApi.me).not.toHaveBeenCalled()
  })

  it('rifiuta il login se il ruolo restituito non è PROFESSIONISTA e non salva il token', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'abc', ruolo: 'PAZIENTE' })
    const store = useAuthStore()

    await expect(store.login('a@b.it', 'password123', true)).rejects.toThrow(ApiError)

    expect(store.token).toBeNull()
    expect(localStorage.getItem('hexis-auth-token')).toBeNull()
    expect(sessionStorage.getItem('hexis-auth-token')).toBeNull()
  })
})
