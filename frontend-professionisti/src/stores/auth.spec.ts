import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/client'

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
    vi.mocked(authApi.me).mockRejectedValue(new Error('network error'))
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(store.token).toBe('valido')
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
