import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { login, me, richiediResetPassword, resetPassword } from './auth'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

describe('api/auth', () => {
  it('login chiama POST /auth/login con email e password', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ token: 't', ruolo: 'PROFESSIONISTA' })

    const risultato = await login({ email: 'a@b.it', password: 'segreta123' })

    expect(apiRequest).toHaveBeenCalledWith('/auth/login', {
      method: 'POST',
      body: { email: 'a@b.it', password: 'segreta123' },
    })
    expect(risultato.token).toBe('t')
  })

  it('me chiama GET /auth/me', async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'a@b.it', ruolo: 'PROFESSIONISTA',
    })

    await me()

    expect(apiRequest).toHaveBeenCalledWith('/auth/me')
  })

  it('richiediResetPassword chiama POST /auth/password-dimenticata con l\'email', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await richiediResetPassword('a@b.it')

    expect(apiRequest).toHaveBeenCalledWith('/auth/password-dimenticata', {
      method: 'POST',
      body: { email: 'a@b.it' },
    })
  })

  it('resetPassword chiama POST /auth/reset-password con token e nuova password', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await resetPassword('il-token', 'nuovaPassword1')

    expect(apiRequest).toHaveBeenCalledWith('/auth/reset-password', {
      method: 'POST',
      body: { token: 'il-token', nuovaPassword: 'nuovaPassword1' },
    })
  })
})
