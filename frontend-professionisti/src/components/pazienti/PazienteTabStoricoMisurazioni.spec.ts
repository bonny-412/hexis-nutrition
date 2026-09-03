import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PazienteTabStoricoMisurazioni from './PazienteTabStoricoMisurazioni.vue'
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

function monta(visite: Visita[], props: Partial<{ visiteInCaricamento: boolean; erroreVisite: boolean }> = {}) {
  return mount(PazienteTabStoricoMisurazioni, {
    props: { visiteInCaricamento: false, erroreVisite: false, visite, ...props },
  })
}

describe('PazienteTabStoricoMisurazioni', () => {
  it('mostra un errore se lo storico non si è caricato', () => {
    const wrapper = monta([], { erroreVisite: true })
    expect(wrapper.text()).toContain('Non è stato possibile caricare l\'elenco delle visite.')
  })

  it('mostra uno skeleton mentre le visite sono in caricamento', () => {
    const wrapper = monta([], { visiteInCaricamento: true })
    expect(wrapper.findAll('[data-test="storico-skeleton"]').length).toBeGreaterThan(0)
  })

  it('mostra un messaggio quando non ci sono visite', () => {
    const wrapper = monta([])
    expect(wrapper.text()).toContain('Nessuna visita registrata.')
  })

  it('elenca le visite dalla più recente, con obiettivo e delta rispetto alla precedente', () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-06-01', pesoKg: 80, bmi: 25.2, obiettivo: 'DIMAGRIMENTO' }),
      visita({ id: 'v2', dataVisita: '2026-08-01', pesoKg: 77.5, bmi: 24.4, obiettivo: 'MANTENIMENTO' }),
    ])

    const testo = wrapper.text()
    expect(testo.indexOf('01 ago 2026')).toBeLessThan(testo.indexOf('01 giu 2026'))
    expect(testo).toContain('Più recente')
    expect(testo).toContain('Mantenimento')
    expect(testo).toContain('77,5')
    expect(testo).toContain('2,5 kg')
  })

  it('apre di default la visita più recente, mostrando generali, BIA, plicometria e circonferenze', async () => {
    const wrapper = monta([
      visita({
        id: 'v1',
        note: 'Prima visita, nessuna allergia nota.',
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
        circonferenze: {
          vitaCm: 84, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
      }),
    ])

    const dettaglio = wrapper.get('[data-test="storico-dettaglio"]')
    expect(dettaglio.text()).toContain('Generali')
    expect(dettaglio.text()).toContain('Prima visita, nessuna allergia nota.')
    expect(dettaglio.text()).toContain('BIA')
    expect(dettaglio.text()).toContain('Dati non disponibili')
    expect(dettaglio.text()).toContain('Plicometria')
    expect(dettaglio.text()).toContain('18,2%')
    expect(dettaglio.text()).toContain('Circonferenze')
    expect(dettaglio.text()).toContain('84,0 cm')

    await wrapper.get('[data-test="storico-riga"]').trigger('click')
    expect(wrapper.find('[data-test="storico-dettaglio"]').exists()).toBe(false)

    await wrapper.get('[data-test="storico-riga"]').trigger('click')
    expect(wrapper.find('[data-test="storico-dettaglio"]').exists()).toBe(true)
  })

  it('apre di default la visita più recente tra più visite, e chiude quella aperta quando se ne apre un\'altra', async () => {
    const wrapper = monta([
      visita({ id: 'v1', dataVisita: '2026-06-01' }),
      visita({ id: 'v2', dataVisita: '2026-08-01' }),
    ])

    const righe = wrapper.findAll('[data-test="storico-riga"]')
    expect(righe).toHaveLength(2)
    expect(wrapper.findAll('[data-test="storico-dettaglio"]')).toHaveLength(1)
    expect(righe[0].element.parentElement?.querySelector('[data-test="storico-dettaglio"]')).not.toBeNull()

    await righe[1].trigger('click')

    expect(wrapper.findAll('[data-test="storico-dettaglio"]')).toHaveLength(1)
    expect(righe[1].element.parentElement?.querySelector('[data-test="storico-dettaglio"]')).not.toBeNull()
  })

  it('mostra un messaggio se la visita non ha nessuna circonferenza registrata', () => {
    const wrapper = monta([visita()])
    const dettaglio = wrapper.get('[data-test="storico-dettaglio"]')
    expect(dettaglio.text()).toContain('Nessuna circonferenza registrata.')
  })

  it('disabilita le azioni di modifica ed eliminazione visita', () => {
    const wrapper = monta([visita()])

    const bottoni = wrapper.findAll('button').filter((b) => b.text().includes('visita'))
    expect(bottoni.some((b) => b.text() === 'Modifica visita' && b.attributes('disabled') !== undefined)).toBe(true)
    expect(bottoni.some((b) => b.text() === 'Elimina visita' && b.attributes('disabled') !== undefined)).toBe(true)
  })

  it('mostra un bordo evidenziato al passaggio del mouse sulla card', () => {
    const wrapper = monta([visita()])
    const card = wrapper.get('[data-test="storico-riga"]').element.parentElement
    const haBordoHover = [...(card?.classList ?? [])].some((c) => c.startsWith('hover:border-'))
    expect(haBordoHover).toBe(true)
  })
})
