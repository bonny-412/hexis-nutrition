import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PianoStampa from './PianoStampa.vue'

// Bug 04 del collaudo (27/11/2026): "Stampa PDF" stampava l'editor, con i comandi di modifica, senza
// intestazione e tagliato al primo foglio. Ora si stampa questo documento, montato fuori da #app.

const riga = (nome: string, grammi: number) => ({ clientId: crypto.randomUUID(), nome, grammi, kcal100g: 100 })

const base = {
  nome: 'Mantenimento fase 1',
  paziente: 'Mario Rossi',
  professionista: 'Anna Bianchi',
  dataInizio: '2026-11-25',
  dataFine: '2026-12-25',
  obiettivoKcal: '2100',
  giorniMacro: [],
  esempi: [],
}

function documento(): HTMLElement {
  const el = document.querySelector<HTMLElement>('[data-test="stampa-piano"]')
  if (!el) throw new Error('documento di stampa non trovato')
  return el
}

afterEach(() => {
  document.body.innerHTML = ''
})

describe('PianoStampa', () => {
  it('ha l\'intestazione con piano, paziente, periodo, obiettivo e professionista, ed è montato sul body', () => {
    mount(PianoStampa, { props: { ...base, modalita: 'PASTI', pasti: [] }, attachTo: document.body })

    const testo = documento().textContent ?? ''
    expect(documento().parentElement).toBe(document.body)
    expect(testo).toContain('Mantenimento fase 1')
    expect(testo).toContain('Mario Rossi')
    expect(testo).toContain('Dal 25 nov 2026 al 25 dic 2026')
    expect(testo).toContain('2100 kcal al giorno')
    expect(testo).toContain('Anna Bianchi')
  })

  it('elenca tutti i giorni con alimenti e grammi, salta i pasti vuoti e non contiene comandi', () => {
    const pasti = [
      { clientId: 'a', giornoSettimana: 'LUNEDI' as const, nome: 'Colazione', nota: null, righe: [riga('Avena', 60)] },
      { clientId: 'b', giornoSettimana: 'LUNEDI' as const, nome: 'Spuntino mattina', nota: null, righe: [] },
      { clientId: 'c', giornoSettimana: 'DOMENICA' as const, nome: 'Cena', nota: 'Senza sale', righe: [riga('Salmone', 140)] },
    ]
    mount(PianoStampa, { props: { ...base, modalita: 'PASTI', pasti }, attachTo: document.body })

    const testo = documento().textContent ?? ''
    expect(testo).toContain('Lunedì')
    expect(testo).toContain('Domenica')
    expect(testo).not.toContain('Martedì')
    expect(testo).toContain('Avena')
    expect(testo).toContain('60 g')
    expect(testo).toContain('140 g')
    expect(testo).toContain('Senza sale')
    expect(testo).not.toContain('Spuntino mattina')
    expect(documento().querySelectorAll('button, input, textarea')).toHaveLength(0)
  })

  it('in modalità macronutrienti stampa la tabella degli obiettivi del giorno', () => {
    const giorniMacro = [{
      giornoSettimana: 'LUNEDI' as const, kcalTarget: '1800', proteineTarget: '120', carboidratiTarget: '',
      grassiTarget: '60', nota: null,
    }]
    mount(PianoStampa, { props: { ...base, modalita: 'MACRO', pasti: [], giorniMacro }, attachTo: document.body })

    const testo = documento().textContent ?? ''
    expect(testo).toContain('1800 kcal')
    expect(testo).toContain('120 g')
    expect(testo).toContain('—')
  })

  it('un piano senza alimenti lo dice invece di stampare un foglio vuoto', () => {
    mount(PianoStampa, { props: { ...base, modalita: 'ESEMPI', pasti: [] }, attachTo: document.body })

    expect(documento().textContent).toContain('Il piano non contiene ancora alimenti.')
  })
})
