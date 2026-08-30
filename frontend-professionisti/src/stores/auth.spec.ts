import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import * as authApi from '@/api/auth'

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

  it('ripristinaSessione fa logout se il token salvato non è più valido', async () => {
    localStorage.setItem('hexis-auth-token', 'scaduto')
    vi.mocked(authApi.me).mockRejectedValue(new Error('401'))
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(store.token).toBeNull()
  })

  it('ripristinaSessione non chiama /auth/me se non c\'è un token salvato', async () => {
    const store = useAuthStore()

    await store.ripristinaSessione()

    expect(authApi.me).not.toHaveBeenCalled()
  })
})
