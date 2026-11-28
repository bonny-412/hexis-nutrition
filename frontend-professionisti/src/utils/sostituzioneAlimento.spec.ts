import { describe, expect, it } from 'vitest'
import type { Alimento } from '@/api/alimenti'
import {
  applicaSostituzione, datiRigaDaAlimento, differenze, grammiEquivalenti, primoValoreDisponibile,
  stessoAlimento, valoriRiga, type DatiAlimentoRiga, type RigaAlimento,
} from './sostituzioneAlimento'

// Valori per 100 g, arrotondati per avere conti leggibili.
const pane: DatiAlimentoRiga = {
  alimentoId: 'pane', nome: 'Pane comune', kcal100g: 270, proteine100g: 8, carboidrati100g: 64, grassi100g: 0.5,
  zuccheri100g: 2, fibre100g: 3, ferro100mg: 1, calcio100mg: 20, acqua100g: 30,
}
const riso: DatiAlimentoRiga = {
  alimentoId: 'riso', nome: 'Riso, crudo', kcal100g: 350, proteine100g: 7, carboidrati100g: 80, grassi100g: 0.6,
  zuccheri100g: null, fibre100g: 1, ferro100mg: null, calcio100mg: 10, acqua100g: 12,
}
const albume: DatiAlimentoRiga = {
  alimentoId: 'albume', nome: 'Albume', kcal100g: 43, proteine100g: 11, carboidrati100g: 0, grassi100g: 0,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: 88,
}

function riga(dati: DatiAlimentoRiga, grammi: number): RigaAlimento & { clientId: string } {
  return { clientId: `c-${dati.nome}-${grammi}`, ...dati, grammi }
}

describe('grammiEquivalenti', () => {
  it('calcola i grammi che danno lo stesso valore, arrotondati al grammo', () => {
    // 50 g di pane = 32 g di carboidrati → riso a 80 g/100 g → 40 g
    expect(grammiEquivalenti(32, 80)).toBe(40)
    expect(grammiEquivalenti(135, 350)).toBe(39) // 38,57 → 39
  })

  it('non scende mai sotto 1 g', () => {
    expect(grammiEquivalenti(0.1, 900)).toBe(1)
  })

  it('restituisce null se il sostituto non ha quel valore o il valore da mantenere è zero', () => {
    expect(grammiEquivalenti(32, 0)).toBeNull()
    expect(grammiEquivalenti(0, 80)).toBeNull()
    expect(grammiEquivalenti(-5, 80)).toBeNull()
    expect(grammiEquivalenti(32, Number.POSITIVE_INFINITY)).toBeNull()
    expect(grammiEquivalenti(Number.NaN, 80)).toBeNull()
  })
})

describe('datiRigaDaAlimento', () => {
  it('porta i valori a 100 g e lascia null i facoltativi mancanti', () => {
    const barretta: Alimento = {
      id: 'b1', nome: 'Barretta', categoria: 'Snack', quantitaG: 50, kcal: 200, proteineG: 10, grassiG: 5,
      carboidratiG: 25, acquaG: null, fibreG: 2, zuccheriG: null, ferroMg: null, calcioMg: 40, sodioMg: null, bda: false,
    }
    expect(datiRigaDaAlimento(barretta)).toEqual({
      alimentoId: 'b1', nome: 'Barretta', kcal100g: 400, proteine100g: 20, carboidrati100g: 50, grassi100g: 10,
      zuccheri100g: null, fibre100g: 4, ferro100mg: null, calcio100mg: 80, acqua100g: null,
    })
  })
})

describe('primoValoreDisponibile', () => {
  it('parte dalle kcal e salta i valori a zero', () => {
    expect(primoValoreDisponibile(riga(pane, 50))).toBe('kcal')
    expect(primoValoreDisponibile(riga({ ...albume, kcal100g: 0 }, 100))).toBe('proteine')
  })

  it('restituisce null se non c\'è nessun valore da mantenere, anche per una riga a 0 g', () => {
    expect(primoValoreDisponibile(riga({ ...albume, kcal100g: 0, proteine100g: 0 }, 100))).toBeNull()
    expect(primoValoreDisponibile(riga(pane, 0))).toBeNull()
  })
})

describe('differenze', () => {
  it('confronta solo i valori non mantenuti e nasconde quelle che arrotondate fanno zero', () => {
    const partenza = valoriRiga(riga(pane, 50)) // 135 kcal, 4 prot, 32 carb, 0,25 grassi
    const sostituto = valoriRiga(riga(riso, 40)) // 140 kcal, 2,8 prot, 32 carb, 0,24 grassi
    expect(differenze(partenza, sostituto, 'carboidrati')).toEqual([
      { chiave: 'kcal', delta: 5 },
      { chiave: 'proteine', delta: -1 },
    ])
  })
})

describe('stessoAlimento', () => {
  it('confronta l\'id per gli alimenti del database e il nome per quelli manuali', () => {
    expect(stessoAlimento(pane, { ...pane, nome: 'Altro nome' })).toBe(true)
    expect(stessoAlimento(pane, riso)).toBe(false)
    const manuale = { ...pane, alimentoId: null }
    expect(stessoAlimento(manuale, { ...manuale })).toBe(true)
    expect(stessoAlimento(manuale, { ...manuale, nome: 'Pane di casa' })).toBe(false)
  })

  it('non confonde un alimento manuale con uno del database dallo stesso nome', () => {
    expect(stessoAlimento({ ...pane, alimentoId: null }, pane)).toBe(false)
  })
})

describe('applicaSostituzione', () => {
  it('cambia la riga sul posto: stesso clientId, dati del sostituto e grammi ricalcolati', () => {
    const r = riga(pane, 50)
    expect(applicaSostituzione(r, riso, 'carboidrati')).toBe(true)
    expect(r).toEqual({ clientId: 'c-Pane comune-50', ...riso, grammi: 40 })
  })

  it('lascia la riga com\'è se il valore da mantenere non si può calcolare', () => {
    const r = riga(pane, 50)
    const prima = { ...r }
    expect(applicaSostituzione(r, albume, 'carboidrati')).toBe(false)
    expect(r).toEqual(prima)
  })
})
