import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DatiVisitaForm from './DatiVisitaForm.vue'

function oggiIso(): string {
  return new Date().toISOString().slice(0, 10)
}

interface DatiVisitaFormExposed {
  valida(): boolean
  ottieniDati(): Record<string, unknown>
}

function esposti(wrapper: ReturnType<typeof mount>): DatiVisitaFormExposed {
  return wrapper.vm as unknown as DatiVisitaFormExposed
}

describe('DatiVisitaForm', () => {
  it('valida() restituisce false e mostra gli errori se altezza e peso sono vuoti', async () => {
    const wrapper = mount(DatiVisitaForm, { props: { sesso: 'M' } })

    const valido = esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(valido).toBe(false)
    expect(wrapper.text()).toContain("L'altezza è obbligatoria.")
    expect(wrapper.text()).toContain('Il peso è obbligatorio.')
  })

  it('ottieniDati() restituisce il payload della visita con la data di oggi e nessun protocollo vita selezionato', async () => {
    const wrapper = mount(DatiVisitaForm, { props: { sesso: 'M' } })

    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    expect(esposti(wrapper).valida()).toBe(true)
    expect(esposti(wrapper).ottieniDati()).toEqual({
      dataVisita: oggiIso(),
      altezzaCm: 178,
      pesoKg: 82.5,
      circonferenzaVitaCm: undefined,
      circonferenzaFianchiCm: undefined,
      circonferenzaAddomeCm: undefined,
      circonferenzaBraccioRilassatoCm: undefined,
      circonferenzaCosciaCm: undefined,
      circonferenzaPolpaccioCm: undefined,
      circonferenzaColloCm: undefined,
      circonferenzaToraceCm: undefined,
      circonferenzaBraccioContrattoCm: undefined,
      circonferenzaAvambraccioCm: undefined,
      circonferenzaCavigliaCm: undefined,
      protocolloVita: undefined,
      note: undefined,
      obiettivo: 'MANTENIMENTO',
      plicometria: undefined,
    })
  })

  it('ottieniDati() include le misurazioni compilate nell\'accordion circonferenze', async () => {
    const wrapper = mount(DatiVisitaForm, { props: { sesso: 'M' } })

    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#circonferenza-vita').setValue('90,10')
    await wrapper.find('#circonferenza-caviglia').setValue('22,90')

    const dati = esposti(wrapper).ottieniDati()
    expect(dati.circonferenzaVitaCm).toBe(90.1)
    expect(dati.circonferenzaCavigliaCm).toBe(22.9)
  })

  it('mostra un errore di formato per una circonferenza non valida', async () => {
    const wrapper = mount(DatiVisitaForm, { props: { sesso: 'M' } })

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await wrapper.vm.$nextTick()

    await wrapper.find('#circonferenza-vita').setValue(',50')
    esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Inserisci un numero valido (es. 95,50).')
  })

  it('con datiIniziali precompila i campi principali e le circonferenze', async () => {
    const wrapper = mount(DatiVisitaForm, {
      props: {
        sesso: 'M',
        datiIniziali: {
          id: 'v1', dataVisita: '2026-03-01', altezzaCm: 178, pesoKg: 82.5,
          bmi: 26.05, whr: null, whtr: null, mamcCm: null,
          circonferenze: {
            vitaCm: 90.1, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
            polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
          },
          protocolloVita: 'OMBELICALE',
          note: 'Nota precedente',
          obiettivo: 'DIMAGRIMENTO',
          plicometria: null,
        },
      },
    })

    expect(esposti(wrapper).ottieniDati()).toMatchObject({
      dataVisita: '2026-03-01',
      altezzaCm: 178,
      pesoKg: 82.5,
      circonferenzaVitaCm: 90.1,
      protocolloVita: 'OMBELICALE',
      note: 'Nota precedente',
      obiettivo: 'DIMAGRIMENTO',
    })
  })

  it('senza datiIniziali, precompila altezza e obiettivo con i valori suggeriti dalla visita precedente', () => {
    const wrapper = mount(DatiVisitaForm, {
      props: { sesso: 'M', altezzaSuggeritaCm: 180, obiettivoSuggerito: 'IPERTROFIA' },
    })

    const dati = esposti(wrapper).ottieniDati()
    expect(dati.altezzaCm).toBe(180)
    expect(dati.obiettivo).toBe('IPERTROFIA')
    expect((wrapper.find('#peso').element as HTMLInputElement).value).toBe('')
  })

  it('con datiIniziali, ignora i valori suggeriti e usa quelli della visita da modificare', () => {
    const wrapper = mount(DatiVisitaForm, {
      props: {
        sesso: 'M',
        altezzaSuggeritaCm: 999,
        obiettivoSuggerito: 'EDUCATIVO',
        datiIniziali: {
          id: 'v1', dataVisita: '2026-03-01', altezzaCm: 178, pesoKg: 82.5,
          bmi: 26.05, whr: null, whtr: null, mamcCm: null,
          circonferenze: {
            vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
            polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
          },
          protocolloVita: 'OMS',
          note: null,
          obiettivo: 'DIMAGRIMENTO',
          plicometria: null,
        },
      },
    })

    const dati = esposti(wrapper).ottieniDati()
    expect(dati.altezzaCm).toBe(178)
    expect(dati.obiettivo).toBe('DIMAGRIMENTO')
  })
})
