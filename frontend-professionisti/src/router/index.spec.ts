import { describe, expect, it } from 'vitest'
import router, { guardiaAutenticazione } from './index'

describe('rotta pagina non trovata', () => {
  it('mostra la 404 per un indirizzo sconosciuto anche senza sessione', () => {
    const rotta = router.resolve('/pazienti/123/indirizzo-inesistente?origine=link')

    expect(rotta).toMatchObject({
      name: 'pagina-non-trovata',
      meta: { requiresAuth: false },
    })
  })

  it('lascia al router le rotte esistenti, anche con parametri', () => {
    expect(router.resolve('/pazienti/123').name).toBe('paziente-dettaglio')
  })
})

describe('guardiaAutenticazione', () => {
  it('rimanda al login se la rotta richiede autenticazione e non c\'è token', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: true }, name: 'pazienti', fullPath: '/pazienti' },
      { token: null },
    )
    expect(risultato).toEqual({ name: 'login', query: { redirect: '/pazienti' } })
  })

  it('lascia proseguire se la rotta richiede autenticazione e il token c\'è', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: true }, name: 'pazienti', fullPath: '/pazienti' },
      { token: 'abc' },
    )
    expect(risultato).toBe(true)
  })

  it('rimanda alla dashboard se si prova ad aprire il login da già autenticati', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: false }, name: 'login', fullPath: '/login' },
      { token: 'abc' },
    )
    expect(risultato).toEqual({ name: 'dashboard' })
  })

  it('lascia proseguire verso il login se non c\'è sessione', () => {
    const risultato = guardiaAutenticazione(
      { meta: { requiresAuth: false }, name: 'login', fullPath: '/login' },
      { token: null },
    )
    expect(risultato).toBe(true)
  })
})
