import { describe, expect, it } from 'vitest'
import { ETICHETTE_OBIETTIVO, categoriaBmi, formattaNumero } from './visita'

describe('ETICHETTE_OBIETTIVO', () => {
  it('associa un\'etichetta leggibile a ogni valore di obiettivo', () => {
    expect(ETICHETTE_OBIETTIVO.DIMAGRIMENTO).toBe('Dimagrimento')
    expect(ETICHETTE_OBIETTIVO.AUMENTO_PESO).toBe('Aumento peso')
    expect(ETICHETTE_OBIETTIVO.IPERTROFIA).toBe('Ipertrofia')
    expect(ETICHETTE_OBIETTIVO.RICOMPOSIZIONE).toBe('Ricomposizione')
    expect(ETICHETTE_OBIETTIVO.MANTENIMENTO).toBe('Mantenimento')
    expect(ETICHETTE_OBIETTIVO.EDUCATIVO).toBe('Educativo')
    expect(ETICHETTE_OBIETTIVO.PREPARAZIONE_SPORTIVA).toBe('Preparazione sportiva')
  })
})

describe('categoriaBmi', () => {
  it('restituisce null se il bmi non è disponibile', () => {
    expect(categoriaBmi(null)).toBeNull()
  })

  it('classifica correttamente le fasce OMS', () => {
    expect(categoriaBmi(17)).toBe('sottopeso')
    expect(categoriaBmi(22)).toBe('normopeso')
    expect(categoriaBmi(27)).toBe('sovrappeso')
    expect(categoriaBmi(32)).toBe('obesità')
  })

  it('tratta i valori di confine come appartenenti alla fascia superiore', () => {
    expect(categoriaBmi(18.5)).toBe('normopeso')
    expect(categoriaBmi(25)).toBe('sovrappeso')
    expect(categoriaBmi(30)).toBe('obesità')
  })
})

describe('formattaNumero', () => {
  it('formatta con la notazione italiana e due cifre decimali di default', () => {
    expect(formattaNumero(77.5)).toBe('77,50')
    expect(formattaNumero(80)).toBe('80,00')
  })

  it('rispetta il numero di decimali richiesto', () => {
    expect(formattaNumero(18.25, 2)).toBe('18,25')
    expect(formattaNumero(80, 0)).toBe('80')
  })
})
