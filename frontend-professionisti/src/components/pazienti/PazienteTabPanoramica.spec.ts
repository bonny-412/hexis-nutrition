import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PazienteTabPanoramica from './PazienteTabPanoramica.vue'
import { prepareAndamento } from '@/utils/andamento'
import type { Paziente, Visita } from '@/api/pazienti'

function pazienteEsempio(overrides: Partial<Paziente> = {}): Paziente {
  return {
    id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
    telefono: null, dataNascita: null, sesso: 'M', lavoro: null, cellulare: null, medicoRiferimento: null, trattamenti: null, stileDiVita: null, note: null,
    statoAccount: 'MAI_INVITATO', archiviato: false,
    obiettivoUltimaVisita: null, dataUltimaVisita: null,
    pianoNome: null, pianoStato: null, pianoDataFine: null, dataInizioObiettivo: null, prossimaVisita: null,
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
      polpaccioCm: null, toraceCm: null, braccioContrattoCm: null, braccioRilassatoDominanteCm: null, braccioContrattoDominanteCm: null, cosciaDominanteCm: null, polpaccioDominanteCm: null,
    },
    parteDominante: null,
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

function monta(
  visite: Visita[],
  props: Partial<{ visiteInCaricamento: boolean; erroreVisite: boolean; paziente: Paziente }> = {},
) {
  return mount(PazienteTabPanoramica, {
    props: {
      paziente: pazienteEsempio(),
      visiteInCaricamento: false,
      erroreVisite: false,
      visite,
      andamento: prepareAndamento(visite),
      ...props,
    },
    global: { stubs: { ChartContainer: true } },
  })
}

describe('PazienteTabPanoramica', () => {
  it('mostra sempre il profilo del paziente, anche se lo storico visite non si è caricato', () => {
    const wrapper = monta([], {
      erroreVisite: true,
      paziente: pazienteEsempio({ nome: 'Marco', cognome: 'Bianchi', codiceFiscale: 'BNCMRC80A01H501U' }),
    })
    expect(wrapper.text()).toContain('Profilo')
    expect(wrapper.text()).toContain('BNCMRC80A01H501U')
  })

  it('mostra un trattino per i campi del profilo assenti', () => {
    const wrapper = monta([])
    expect((wrapper.text().match(/—/g) ?? []).length).toBeGreaterThanOrEqual(2)
  })

  it('mostra un errore se lo storico visite non si è caricato', () => {
    const wrapper = monta([], { erroreVisite: true })
    expect(wrapper.text()).toContain('Non è stato possibile caricare lo storico delle visite.')
  })

  it('mostra uno skeleton mentre le visite sono in caricamento', () => {
    const wrapper = monta([], { visiteInCaricamento: true })
    expect(wrapper.findAll('[data-test="andamento-chart-skeleton"]').length).toBe(3)
  })

  it('mostra lo stato vuoto quando non ci sono visite', () => {
    const wrapper = monta([])
    expect(wrapper.text()).toContain('Nessuna visita registrata')
    expect(wrapper.text()).toContain('Peso, composizione corporea e grafici dell\'andamento compaiono qui dalla prima visita.')
    expect(wrapper.find('[data-test="panoramica-prima-visita"]').exists()).toBe(true)
  })

  it('mostra i grafici di peso e bmi, e "Dati non disponibili" nella card massa grassa/magra senza plicometria', () => {
    const wrapper = monta([visita({ dataVisita: '2026-06-01', pesoKg: 80 }), visita({ dataVisita: '2026-08-01', pesoKg: 77.5 })])

    expect(wrapper.text()).toContain('Peso')
    expect(wrapper.text()).toContain('BMI')
    expect(wrapper.text()).toContain('Massa magra e % grasso')
    expect(wrapper.text()).toContain('Dati non disponibili')
    expect(wrapper.text()).not.toContain('% Grasso corporeo')
  })

  it('con una sola visita non disegna i grafici ma mostra i valori della plicometria', () => {
    const wrapper = monta([
      visita({
        dataVisita: '2026-06-01',
        plicometria: {
          protocollo: 'JACKSON_POLLOCK_3', etniaAtleta: null,
          plicaPettoraleMm: null, plicaAscellareMm: null, plicaTricipitaleMm: null, plicaBicipitaleMm: null,
          plicaSottoscapolareMm: null, plicaSoprailiacaMm: null, plicaAddominaleMm: null, plicaCosciaMm: null, plicaPolpaccioMm: null,
          percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1,
        },
      }),
    ])

    expect(wrapper.find('[data-test="andamento-una-visita"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Massa magra e % grasso')
    expect(wrapper.text()).toContain('18,20%')
    expect(wrapper.text()).toContain('63,40 kg')
  })

  it('mostra la nota del paziente, o un messaggio di assenza', () => {
    const senzaNota = monta([visita()], { paziente: pazienteEsempio({ note: null }) })
    expect(senzaNota.text()).toContain('Nessuna storia inserita per questo paziente.')

    const conNota = monta([visita()], { paziente: pazienteEsempio({ note: 'Lieve intolleranza al lattosio.' }) })
    expect(conNota.text()).toContain('Lieve intolleranza al lattosio.')
  })

  it('mostra sempre la BIA come non ancora supportata, in una riga e non in una card', () => {
    const wrapper = monta([visita()])
    expect(wrapper.text()).toContain('BIA')
    expect(wrapper.text()).toContain('la bioimpedenziometria non è ancora supportata')
  })

  it('mostra i dati reali di plicometria dell\'ultima visita quando presenti', () => {
    const wrapper = monta([
      visita({
        plicometria: {
          protocollo: 'JACKSON_POLLOCK_3', etniaAtleta: null,
          plicaPettoraleMm: null, plicaAscellareMm: null, plicaTricipitaleMm: null, plicaBicipitaleMm: null,
          plicaSottoscapolareMm: null, plicaSoprailiacaMm: null, plicaAddominaleMm: null, plicaCosciaMm: null, plicaPolpaccioMm: null,
          percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1,
        },
      }),
    ])

    expect(wrapper.text()).toContain('Plicometria')
    expect(wrapper.text()).toContain('18,20%')
    expect(wrapper.text()).toContain('14,10 kg')
    expect(wrapper.text()).toContain('63,40 kg')
  })

  it('mostra un messaggio se l\'ultima visita non ha plicometria', () => {
    const wrapper = monta([visita({ plicometria: null })])
    expect(wrapper.text()).toContain('Nessuna plicometria registrata per l\'ultima visita.')
  })

  it('mostra le circonferenze valorizzate dell\'ultima visita e un trattino per quelle assenti', () => {
    const wrapper = monta([
      visita({
        circonferenze: {
          vitaCm: 84, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, toraceCm: null, braccioContrattoCm: null, braccioRilassatoDominanteCm: null, braccioContrattoDominanteCm: null, cosciaDominanteCm: null, polpaccioDominanteCm: null,
        },
      }),
    ])

    expect(wrapper.text()).toContain('Vita')
    expect(wrapper.text()).toContain('84,00 cm')
    expect(wrapper.findAll('dd').filter((d) => d.text() === '—').length).toBeGreaterThan(0)
  })

  it('mostra un messaggio se l\'ultima visita non ha nessuna circonferenza registrata', () => {
    const wrapper = monta([visita()])
    expect(wrapper.text()).toContain('Nessuna circonferenza registrata per l\'ultima visita.')
  })
})
