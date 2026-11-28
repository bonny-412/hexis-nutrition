import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  apiRequest, ApiError, configureApiClient, eErroreConnessione, ErroreRete, ErroreTimeout, TIMEOUT_MS,
} from './client'

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

  // Verifica 3 prima del rilascio (28/11/2026), bug S5: il 401 del login sono credenziali sbagliate, non una
  // sessione scaduta, e non deve aprire la finestra "Sessione scaduta".
  it('su un 401 di una richiesta senza token non notifica onUnauthorized', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('credenziali non valide', { status: 401 }))
    const onUnauthorized = vi.fn()
    configureApiClient({ getToken: () => null, onUnauthorized })

    await expect(apiRequest('/auth/login', { method: 'POST', body: {} })).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  // Bug S4: la chiave di idempotenza arriva al backend come header.
  it('manda la chiave di idempotenza nell\'header Idempotency-Key, e senza chiave non manda l\'header', async () => {
    vi.mocked(fetch).mockImplementation(async () => new Response(JSON.stringify({}), { status: 201 }))

    await apiRequest('/pazienti', { method: 'POST', body: {}, chiaveIdempotenza: 'chiave-1' })
    await apiRequest('/pazienti', { method: 'POST', body: {} })

    const intestazioni = vi.mocked(fetch).mock.calls.map((c) => c[1]?.headers as Record<string, string>)
    expect(intestazioni[0]['Idempotency-Key']).toBe('chiave-1')
    expect(intestazioni[1]).not.toHaveProperty('Idempotency-Key')
  })

  // Bug S6: senza un limite, "Salvataggio…" restava acceso per sempre se il server non rispondeva.
  it('annulla la richiesta dopo TIMEOUT_MS e lancia ErroreTimeout, che è un errore di rete', async () => {
    vi.useFakeTimers()
    try {
      vi.mocked(fetch).mockImplementation((_url, init) => new Promise((_risolvi, rifiuta) => {
        init?.signal?.addEventListener('abort', () => rifiuta(new DOMException('aborted', 'AbortError')))
      }))

      const richiesta = apiRequest('/qualcosa').catch((e) => e)
      await vi.advanceTimersByTimeAsync(TIMEOUT_MS - 1)
      expect(vi.mocked(fetch).mock.calls[0][1]?.signal?.aborted).toBe(false)
      await vi.advanceTimersByTimeAsync(1)
      const errore = await richiesta

      expect(errore).toBeInstanceOf(ErroreTimeout)
      expect(errore).toBeInstanceOf(ErroreRete)
      expect(eErroreConnessione(errore)).toBe(true)
    } finally {
      vi.useRealTimers()
    }
  })

  it('un errore di rete che non è il timeout resta ErroreRete', async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    const errore = await apiRequest('/qualcosa').catch((e) => e)

    expect(errore).toBeInstanceOf(ErroreRete)
    expect(errore).not.toBeInstanceOf(ErroreTimeout)
  })

  it('lancia ApiError con lo status su altre risposte non ok', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('errore', { status: 500 }))

    const errore = await apiRequest('/qualcosa').catch((e) => e)

    expect(errore).toBeInstanceOf(ApiError)
    expect((errore as ApiError).status).toBe(500)
  })
})
