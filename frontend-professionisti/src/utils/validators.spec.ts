import { describe, expect, it } from 'vitest'
import { erroreDataFinePiano, erroreDataInizioPiano, erroreGrammiPorzione, erroreObiettivoKcal } from './validators'

// Regressioni del collaudo del 27/11/2026, gruppo 5.
describe('validatori del piano', () => {
  it('bug 10: un obiettivo di 0 kcal non è valido', () => {
    expect(erroreObiettivoKcal('0')).toBe("L'obiettivo deve essere maggiore di 0 kcal.")
    expect(erroreObiettivoKcal('0,0')).toBe("L'obiettivo deve essere maggiore di 0 kcal.")
    expect(erroreObiettivoKcal('')).toBe('Il valore è obbligatorio.')
    expect(erroreObiettivoKcal('2331')).toBeUndefined()
  })

  it('bug 08: una data scritta ma inesistente è "non valida", non "obbligatoria"', () => {
    expect(erroreDataFinePiano('', '2027-01-01', true)).toBe('La data di fine non è valida.')
    expect(erroreDataFinePiano('', '2027-01-01')).toBe('La data di fine è obbligatoria.')
    expect(erroreDataInizioPiano('', true)).toBe('La data di partenza non è valida.')
  })

  it('bug 06: un meno incollato nei grammi è un errore, non una porzione positiva', () => {
    expect(erroreGrammiPorzione('10', true)).toBe('La quantità non può essere negativa.')
    expect(erroreGrammiPorzione('10')).toBeUndefined()
  })

  it('bug 07: una porzione da 0 g o vuota non è valida', () => {
    expect(erroreGrammiPorzione('0')).toBe('La quantità deve essere maggiore di 0 g.')
    expect(erroreGrammiPorzione('0,0')).toBe('La quantità deve essere maggiore di 0 g.')
    expect(erroreGrammiPorzione('')).toBe('Inserisci la quantità.')
    expect(erroreGrammiPorzione('62,5')).toBeUndefined()
  })
})
