import { describe, expect, it, vi, afterEach } from 'vitest'
import { calcolaEta } from './data'

describe('calcolaEta', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('calcola gli anni compiuti quando il compleanno di quest\'anno è già passato', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-01-15')).toBe(26)
  })

  it('non conta l\'anno in corso se il compleanno non è ancora arrivato', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-12-25')).toBe(25)
  })

  it('conta correttamente se il compleanno è esattamente oggi', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-09-01')).toBe(26)
  })

  it('restituisce null se la data di nascita è vuota', () => {
    expect(calcolaEta('')).toBeNull()
  })

  it('restituisce null se la data di nascita è nel futuro', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2030-01-01')).toBeNull()
  })
})
