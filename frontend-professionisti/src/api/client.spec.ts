import { describe, expect, it, vi, beforeEach } from 'vitest'
import { apiRequest, ApiError, configureApiClient } from './client'

describe('apiRequest', () => {
  beforeEach(() => {
    configureApiClient({ getToken: () => null, onUnauthorized: () => {} })
    vi.stubGlobal('fetch', vi.fn())
  })

  it('invia il token come header Authorization quando presente', async () => {
    configureApiClient({ getToken: () => 'il-token', onUnauthorized: () => {} })
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))

    await apiRequest('/qualcosa')

    const init = vi.mocked(fetch).mock.calls[0][1]
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer il-token')
  })

  it('restituisce undefined su risposta 204', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    const risultato = await apiRequest('/qualcosa')

    expect(risultato).toBeUndefined()
  })

  it('lancia ApiError e notifica onUnauthorized su risposta 401', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('credenziali non valide', { status: 401 }))
    const onUnauthorized = vi.fn()
    configureApiClient({ getToken: () => 'token-scaduto', onUnauthorized })

    await expect(apiRequest('/qualcosa')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('lancia ApiError con lo status su altre risposte non ok', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('errore', { status: 500 }))

    const errore = await apiRequest('/qualcosa').catch((e) => e)

    expect(errore).toBeInstanceOf(ApiError)
    expect((errore as ApiError).status).toBe(500)
  })
})
