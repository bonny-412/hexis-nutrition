import { describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { ArrowDown, ArrowUp } from '@lucide/vue'
import AndamentoChart from './AndamentoChart.vue'
import type { Andamento } from '@/utils/andamento'
import { isoATimestamp } from '@/utils/data'

/**
 * Unovis disegna SVG reali e in jsdom fallisce (`getBBox` non esiste), quindi
 * i suoi componenti sono sostituiti da stub che dichiarano le stesse props:
 * così si può ispezionare *cosa* il componente passa al grafico (accessor x/y,
 * tick, marker) senza dipendere dal rendering SVG.
 */
vi.mock('@unovis/vue', async () => {
  const { defineComponent, h } = await import('vue')
  const stub = (name: string) =>
    defineComponent({
      name,
      props: {
        data: { type: Array, default: undefined },
        margin: { default: undefined },
        x: { default: undefined },
        y: { default: undefined },
        color: { default: undefined },
        size: { default: undefined },
        type: { default: undefined },
        tickValues: { default: undefined },
        tickFormat: { default: undefined },
        gridLine: { default: undefined },
        tickLine: { default: undefined },
      },
      setup(_props, { slots }) {
        return () => h('div', { class: `stub-${name}` }, slots.default?.())
      },
    })

  return {
    VisXYContainer: stub('VisXYContainer'),
    VisLine: stub('VisLine'),
    VisScatter: stub('VisScatter'),
    VisAxis: stub('VisAxis'),
  }
})

/** Recupera lo stub Unovis montato, per ispezionare le props che il componente gli passa. */
function vis(wrapper: VueWrapper, nome: string) {
  return wrapper.findComponent({ name: nome })
}

// `@types/node` non è installato: leggiamo l'ambiente dal globale in modo tipizzato.
const ambiente = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process.env

function montaConAndamento(andamento: Andamento) {
  return mount(AndamentoChart, {
    props: { titolo: 'Peso', unita: 'kg', andamento, colore: 'var(--chart-1)' },
    global: { stubs: { ChartContainer: true } },
  })
}

/** Come `montaConAndamento`, ma lascia passare lo slot di `ChartContainer` per arrivare al grafico. */
function montaGrafico(andamento: Andamento) {
  return mount(AndamentoChart, {
    props: { titolo: 'Peso', unita: 'kg', andamento, colore: 'var(--chart-1)' },
    global: { stubs: { ChartContainer: { template: '<div><slot /></div>' } } },
  })
}

const UNA_VISITA: Andamento = { punti: [{ data: '2026-06-01', valore: 80 }], ultimo: 80, delta: null }

const DUE_VISITE: Andamento = {
  punti: [{ data: '2026-06-01', valore: 80 }, { data: '2026-08-01', valore: 77.5 }],
  ultimo: 77.5,
  delta: -2.5,
}

describe('AndamentoChart', () => {
  it('mostra il titolo passato', () => {
    const wrapper = montaConAndamento({ punti: [], ultimo: null, delta: null })

    expect(wrapper.text()).toContain('Peso')
  })

  it('mostra "Nessun dato disponibile" senza punti', () => {
    const wrapper = montaConAndamento({ punti: [], ultimo: null, delta: null })

    expect(wrapper.text()).toContain('Nessun dato disponibile')
  })

  it('mostra l\'ultimo valore e "Prima visita" quando il delta è null', () => {
    const wrapper = montaConAndamento({ punti: [{ data: '2026-01-01', valore: 80 }], ultimo: 80, delta: null })

    expect(wrapper.text()).toContain('80,0')
    expect(wrapper.text()).toContain('Prima visita')
  })

  it('mostra il delta negativo con il segno e senza "Prima visita"', () => {
    const wrapper = montaConAndamento(DUE_VISITE)

    expect(wrapper.text()).toContain('77,5')
    expect(wrapper.text()).toContain('2,5')
    expect(wrapper.text()).not.toContain('Prima visita')
  })

  it('mostra il delta negativo con la freccia in giù in verde', () => {
    const wrapper = montaConAndamento(DUE_VISITE)
    const delta = wrapper.get('[data-test="andamento-delta"]')

    expect(delta.findComponent(ArrowDown).exists()).toBe(true)
    expect(delta.findComponent(ArrowUp).exists()).toBe(false)
    expect(delta.classes()).toContain('text-(--green)')
  })

  it('mostra il delta positivo con la freccia in su in rosso, in valore assoluto', () => {
    const wrapper = montaConAndamento({
      punti: [{ data: '2026-06-01', valore: 77.5 }, { data: '2026-08-01', valore: 80 }],
      ultimo: 80,
      delta: 2.5,
    })
    const delta = wrapper.get('[data-test="andamento-delta"]')

    expect(delta.findComponent(ArrowUp).exists()).toBe(true)
    expect(delta.findComponent(ArrowDown).exists()).toBe(false)
    expect(delta.classes()).toContain('text-(--danger)')
    expect(delta.text()).toContain('2,5')
    expect(delta.text()).not.toContain('-')
    expect(wrapper.text()).not.toContain('Prima visita')
  })

  it('mostra il delta nullo senza frecce e in colore neutro', () => {
    const wrapper = montaConAndamento({
      punti: [{ data: '2026-06-01', valore: 80 }, { data: '2026-08-01', valore: 80 }],
      ultimo: 80,
      delta: 0,
    })
    const delta = wrapper.get('[data-test="andamento-delta"]')

    expect(delta.findComponent(ArrowUp).exists()).toBe(false)
    expect(delta.findComponent(ArrowDown).exists()).toBe(false)
    expect(delta.classes()).toContain('text-(--fg3)')
    expect(delta.text()).toContain('0,0')
    expect(wrapper.text()).not.toContain('Prima visita')
  })

  it('disegna i marker dei punti oltre alla linea, così una sola visita resta visibile', () => {
    const wrapper = montaGrafico(UNA_VISITA)

    expect(vis(wrapper, 'VisLine').exists()).toBe(true)
    expect(vis(wrapper, 'VisScatter').exists()).toBe(true)
    expect(vis(wrapper, 'VisScatter').props('size')).toBe(8)
    expect(vis(wrapper, 'VisXYContainer').props('data')).toHaveLength(1)
  })

  it('posiziona i punti sull\'asse x in base alla data reale della visita', () => {
    const wrapper = montaGrafico(DUE_VISITE)
    const dati = vis(wrapper, 'VisXYContainer').props('data') as { timestamp: number; valore: number }[]

    expect(dati.map((d) => d.timestamp)).toEqual([isoATimestamp('2026-06-01'), isoATimestamp('2026-08-01')])
    expect(dati.map((d) => d.valore)).toEqual([80, 77.5])

    // La distanza fra i due punti riflette i due mesi reali, non un indice.
    expect(dati[1].timestamp - dati[0].timestamp).toBe(61 * 24 * 60 * 60 * 1000)
  })

  it('passa alla linea e ai marker gli stessi accessor x/y', () => {
    const wrapper = montaGrafico(DUE_VISITE)
    const punto = { timestamp: 123, valore: 45 }

    for (const componente of [vis(wrapper, 'VisLine'), vis(wrapper, 'VisScatter')]) {
      const x = componente.props('x') as (d: typeof punto) => number
      const y = componente.props('y') as (d: typeof punto) => number

      expect(x(punto)).toBe(123)
      expect(y(punto)).toBe(45)
    }
  })

  it('mette un tick per visita e lo etichetta con la data italiana', () => {
    const wrapper = montaGrafico(DUE_VISITE)
    const asse = vis(wrapper, 'VisAxis')

    const tickValues = asse.props('tickValues') as number[]
    const tickFormat = asse.props('tickFormat') as (tick: number | Date) => string

    expect(tickValues).toEqual([isoATimestamp('2026-06-01'), isoATimestamp('2026-08-01')])
    expect(tickValues.map((t) => tickFormat(t))).toEqual(['01/06/26', '01/08/26'])
  })

  it('etichetta correttamente anche un tick che non coincide con una visita', () => {
    const wrapper = montaGrafico(DUE_VISITE)
    const tickFormat = vis(wrapper, 'VisAxis').props('tickFormat') as (tick: number | Date) => string

    expect(tickFormat(isoATimestamp('2026-07-15'))).toBe('15/07/26')
    expect(tickFormat(new Date(isoATimestamp('2026-07-15')))).toBe('15/07/26')
  })

  it('non usa il fuso orario del browser per le etichette dell\'asse', () => {
    const wrapper = montaGrafico({ punti: [{ data: '2026-01-01', valore: 80 }], ultimo: 80, delta: null })
    const asse = vis(wrapper, 'VisAxis')
    const tickValues = asse.props('tickValues') as number[]
    const tickFormat = asse.props('tickFormat') as (tick: number | Date) => string

    const precedente = ambiente.TZ
    try {
      // A New York la mezzanotte UTC del 01/01/2026 è il 31/12/2025: con una
      // formattazione locale l'etichetta mostrerebbe l'anno sbagliato. Il tick
      // di un punto dati usa l'etichetta pre-calcolata dalla stringa ISO...
      ambiente.TZ = 'America/New_York'
      expect(tickFormat(tickValues[0])).toBe('01/01/26')
      // ...e un tick generato da d3, che non coincide con nessuna visita, passa
      // dalla conversione timestamp → giorno UTC.
      expect(tickFormat(isoATimestamp('2027-01-01'))).toBe('01/01/27')
      expect(tickFormat(new Date(isoATimestamp('2027-01-01')))).toBe('01/01/27')
      ambiente.TZ = 'Pacific/Kiritimati'
      expect(tickFormat(tickValues[0])).toBe('01/01/26')
      expect(tickFormat(isoATimestamp('2025-12-31'))).toBe('31/12/25')
    } finally {
      ambiente.TZ = precedente
    }
  })
})
