import { describe, expect, it } from 'vitest'
import { guardiaAutenticazione } from './index'

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
