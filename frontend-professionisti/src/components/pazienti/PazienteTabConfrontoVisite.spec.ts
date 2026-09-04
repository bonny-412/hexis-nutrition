import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PazienteTabConfrontoVisite from './PazienteTabConfrontoVisite.vue'
import type { Visita } from '@/api/pazienti'

/** ChartContainer stubbato: evita di montare i grafici Unovis reali, che in jsdom falliscono (`getBBox` non esiste). */
function monta(visite: Visita[]) {
  return mount(PazienteTabConfrontoVisite, {
    props: { visite },
    global: { stubs: { ChartContainer: true } },
  })
}

function plicometriaEsempio(overrides: Partial<NonNullable<Visita['plicometria']>> = {}): NonNullable<Visita['plicometria']> {
  return {
    protocollo: 'JACKSON_POLLOCK_3', etniaAtleta: null,
    plicaPettoraleMm: null, plicaAscellareMm: null, plicaTricipitaleMm: null, plicaBicipitaleMm: null,
    plicaSottoscapolareMm: null, plicaSoprailiacaMm: null, plicaAddominaleMm: null, plicaCosciaMm: null, plicaPolpaccioMm: null,
    percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1,
    ...overrides,
  }
}

function circonferenzeEsempio(overrides: Partial<Visita['circonferenze']> = {}): Visita['circonferenze'] {
  return {
    vitaCm: 84, fianchiCm: 98, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
    polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
    ...overrides,
  }
}

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
    protocolloVita: 'OMS',
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

describe('PazienteTabConfrontoVisite', () => {
  it('mostra un messaggio se ci sono meno di due visite', () => {
    const wrapper = monta([visita()])
    expect(wrapper.text()).toContain('Servono almeno due visite')
  })

  it('confronta di default la prima e l\'ultima visita, con la tabella divisa per sezioni', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', pesoKg: 84.5, bmi: 26.0 }),
      visita({ id: 'v2', dataVisita: '2026-06-01', pesoKg: 80.9, bmi: 24.9 }),
      visita({ id: 'v3', dataVisita: '2026-08-01', pesoKg: 78.4, bmi: 24.1 }),
    ])

    expect(wrapper.text()).toContain('Peso')
    expect(wrapper.text()).toContain('84,5')
    expect(wrapper.text()).toContain('78,4')
    expect(wrapper.text()).toContain('-6,1')
    expect(wrapper.text()).toContain('BIA')
    expect(wrapper.text()).toContain('Dati non disponibili')
  })

  it('nasconde del tutto la sezione Plicometria (tabella e card) quando nessuna delle due visite la ha registrata', () => {
    const wrapper = monta([visita({ id: 'v1' }), visita({ id: 'v2', dataVisita: '2026-02-01' })])
    expect(wrapper.text()).not.toContain('Plicometria')
  })

  it('nasconde del tutto la sezione Circonferenze (tabella e card) quando nessuna delle due visite ha valori', () => {
    const wrapper = monta([visita({ id: 'v1' }), visita({ id: 'v2', dataVisita: '2026-02-01' })])
    expect(wrapper.text()).not.toContain('Circonferenze')
  })

  it('mostra i dati di plicometria quando almeno una visita li ha registrati', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', plicometria: null }),
      visita({ id: 'v2', dataVisita: '2026-02-01', plicometria: plicometriaEsempio() }),
    ])

    expect(wrapper.text()).toContain('% Grasso corporeo')
    expect(wrapper.text()).toContain('18,2')
  })

  it('mostra i dati di circonferenze quando almeno una visita ha valori', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01' }),
      visita({ id: 'v2', dataVisita: '2026-02-01', circonferenze: circonferenzeEsempio() }),
    ])

    expect(wrapper.text()).toContain('Circonferenze')
    expect(wrapper.text()).toContain('Vita')
    expect(wrapper.text()).toContain('84,0')
  })

  it('aggiorna il confronto quando si cambia la visita selezionata', async () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', pesoKg: 84.5 }),
      visita({ id: 'v2', dataVisita: '2026-06-01', pesoKg: 80.9 }),
      visita({ id: 'v3', dataVisita: '2026-08-01', pesoKg: 78.4 }),
    ])

    const selects = wrapper.findAll('select')
    await selects[1].setValue('1')

    expect(wrapper.text()).toContain('80,9')
    expect(wrapper.text()).not.toContain('78,4')
  })

  it('colora ogni variazione col colore di avviso, senza distinguere tra miglioramento e peggioramento', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', pesoKg: 80, bmi: 25 }),
      visita({ id: 'v2', dataVisita: '2026-02-01', pesoKg: 82, bmi: 25 }),
    ])
    const righe = wrapper.findAll('tbody tr').filter((r) => r.text().includes('Peso (kg)') || r.text().includes('BMI'))
    const rigaPeso = righe.find((r) => r.text().includes('Peso (kg)'))!
    const rigaBmi = righe.find((r) => r.text().includes('BMI'))!

    // Il peso aumenta (+2 kg): colore di avviso, non rosso "peggioramento".
    const celleVariazionePeso = rigaPeso.findAll('td')
    expect(celleVariazionePeso[3].classes()).toContain('text-(--warn-fg)')
    expect(celleVariazionePeso[3].classes()).not.toContain('text-(--danger)')

    // Il BMI non cambia: nessun colore di avviso.
    const celleVariazioneBmi = rigaBmi.findAll('td')
    expect(celleVariazioneBmi[3].classes()).toContain('text-(--fg4)')
    expect(celleVariazioneBmi[3].classes()).not.toContain('text-(--warn-fg)')
  })

  it('mostra "—" per l\'obiettivo in riga se non è cambiato tra le due visite', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', obiettivo: 'DIMAGRIMENTO' }),
      visita({ id: 'v2', dataVisita: '2026-02-01', obiettivo: 'DIMAGRIMENTO' }),
    ])

    expect(wrapper.text()).toContain('Obiettivo')
    expect(wrapper.text()).toContain('Dimagrimento')
    expect(wrapper.text()).not.toContain('Cambiato')
  })

  it('segnala l\'obiettivo cambiato tra le due visite', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01', obiettivo: 'DIMAGRIMENTO' }),
      visita({ id: 'v2', dataVisita: '2026-02-01', obiettivo: 'IPERTROFIA' }),
    ])

    expect(wrapper.text()).toContain('Dimagrimento')
    expect(wrapper.text()).toContain('Ipertrofia')
    expect(wrapper.text()).toContain('Cambiato')
  })

  it('mostra le card grafiche di Plicometria e Circonferenze sopra la tabella quando ci sono dati comparabili', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-01-01' }),
      visita({ id: 'v2', dataVisita: '2026-02-01', plicometria: plicometriaEsempio(), circonferenze: circonferenzeEsempio() }),
    ])
    const titoli = wrapper.findAll('[data-slot="card-title"]').map((t) => t.text())

    expect(titoli).toContain('Plicometria')
    expect(titoli).toContain('Circonferenze')
  })

  it('non mostra nessuna card grafica se non ci sono dati né di plicometria né di circonferenze', () => {
    const wrapper = monta([visita({ id: 'v1' }), visita({ id: 'v2', dataVisita: '2026-02-01' })])
    expect(wrapper.findAll('[data-slot="card-title"]')).toHaveLength(0)
  })
})
