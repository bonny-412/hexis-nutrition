import { describe, expect, it } from 'vitest'
import { prepareAndamento } from './andamento'
import type { Visita } from '@/api/pazienti'

function visita(overrides: Partial<Visita> = {}): Visita {
  return {
    id: '1',
    dataVisita: '2026-01-01',
    altezzaCm: 178,
    pesoKg: 80,
    bmi: 25.2,
    whr: null,
    whtr: null,
    mamcCm: null,
    circonferenze: {
      vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
      polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
    },
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

describe('prepareAndamento', () => {
  it('con nessuna visita restituisce punti vuoti e ultimo/delta null per tutte le metriche', () => {
    const risultato = prepareAndamento([])

    expect(risultato.peso).toEqual({ punti: [], ultimo: null, delta: null })
    expect(risultato.bmi).toEqual({ punti: [], ultimo: null, delta: null })
    expect(risultato.percentualeGrassoCorporeo).toEqual({ punti: [], ultimo: null, delta: null })
  })

  it('con una sola visita valorizza ultimo ma non il delta', () => {
    const risultato = prepareAndamento([visita({ pesoKg: 80, bmi: 25.2 })])

    expect(risultato.peso).toEqual({ punti: [{ data: '2026-01-01', valore: 80 }], ultimo: 80, delta: null })
    expect(risultato.bmi.ultimo).toBe(25.2)
    expect(risultato.bmi.delta).toBeNull()
  })

  it('con più visite calcola il delta tra le ultime due (in ordine di arrivo)', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-06-01', pesoKg: 80 }),
      visita({ dataVisita: '2026-08-01', pesoKg: 77.5 }),
    ])

    expect(risultato.peso.ultimo).toBe(77.5)
    expect(risultato.peso.delta).toBeCloseTo(-2.5)
  })

  it('percentualeGrassoCorporeo ignora le visite senza plicometria', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-06-01', plicometria: null }),
      visita({
        dataVisita: '2026-08-01',
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
      }),
    ])

    expect(risultato.percentualeGrassoCorporeo.punti).toEqual([{ data: '2026-08-01', valore: 18.2 }])
    expect(risultato.percentualeGrassoCorporeo.ultimo).toBe(18.2)
    expect(risultato.percentualeGrassoCorporeo.delta).toBeNull()
  })

  it('percentualeGrassoCorporeo è vuoto se nessuna visita ha la plicometria', () => {
    const risultato = prepareAndamento([visita({ plicometria: null }), visita({ dataVisita: '2026-02-01', plicometria: null })])

    expect(risultato.percentualeGrassoCorporeo).toEqual({ punti: [], ultimo: null, delta: null })
  })

  it('ignora visite senza bmi valorizzato nel calcolo dei punti per BMI', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-01-01', bmi: null }),
      visita({ dataVisita: '2026-02-01', bmi: 24.0 }),
    ])

    expect(risultato.bmi.punti).toEqual([{ data: '2026-02-01', valore: 24.0 }])
    expect(risultato.bmi.delta).toBeNull()
  })

  it('massaMagra ignora le visite senza plicometria', () => {
    const risultato = prepareAndamento([
      visita({ dataVisita: '2026-06-01', plicometria: null }),
      visita({
        dataVisita: '2026-08-01',
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
      }),
    ])

    expect(risultato.massaMagra.punti).toEqual([{ data: '2026-08-01', valore: 63.4 }])
    expect(risultato.massaMagra.ultimo).toBe(63.4)
    expect(risultato.massaMagra.delta).toBeNull()
  })

  it('massaMagra è vuoto se nessuna visita ha la plicometria', () => {
    const risultato = prepareAndamento([visita({ plicometria: null })])

    expect(risultato.massaMagra).toEqual({ punti: [], ultimo: null, delta: null })
  })
})
