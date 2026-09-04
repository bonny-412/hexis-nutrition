import { describe, expect, it, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import ConfrontoBarChart from './ConfrontoBarChart.vue'

/**
 * Unovis disegna SVG reali e in jsdom fallisce (`getBBox` non esiste), quindi
 * i suoi componenti sono sostituiti da stub che dichiarano le stesse props:
 * così si può ispezionare *cosa* il componente passa al grafico (accessor x/y,
 * colore, orientamento) senza dipendere dal rendering SVG.
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
        orientation: { default: undefined },
        roundedCorners: { default: undefined },
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
    VisGroupedBar: stub('VisGroupedBar'),
    VisAxis: stub('VisAxis'),
  }
})

function vis(wrapper: VueWrapper, nome: string) {
  return wrapper.findComponent({ name: nome })
}

const RIGHE = [
  { label: '% Grasso corporeo', a: 20.6, b: 16.8, unita: '%' },
  { label: 'Massa magra (kg)', a: 67.1, b: 65.3, unita: 'kg' },
  { label: 'Non confrontabile', a: null, b: 12, unita: 'kg' },
]

function montaConDati(righe: typeof RIGHE) {
  return mount(ConfrontoBarChart, {
    props: { titolo: 'Plicometria', righe, etichettaA: '12 gen 2026', etichettaB: '18 ago 2026', colore: 'var(--chart-1)' },
    global: { stubs: { ChartContainer: { template: '<div><slot /></div>' } } },
  })
}

describe('ConfrontoBarChart', () => {
  it('mostra il titolo passato', () => {
    const wrapper = mount(ConfrontoBarChart, {
      props: { titolo: 'Plicometria', righe: [], etichettaA: 'A', etichettaB: 'B' },
      global: { stubs: { ChartContainer: true } },
    })
    expect(wrapper.text()).toContain('Plicometria')
  })

  it('mostra "Dati non disponibili" se nessuna riga ha entrambi i valori', () => {
    const wrapper = mount(ConfrontoBarChart, {
      props: { titolo: 'Plicometria', righe: [{ label: 'FMI', a: null, b: 4.4, unita: '' }], etichettaA: 'A', etichettaB: 'B' },
      global: { stubs: { ChartContainer: true } },
    })
    expect(wrapper.text()).toContain('Dati non disponibili')
  })

  it('esclude dal grafico le righe con un solo lato valorizzato', () => {
    const wrapper = montaConDati(RIGHE)
    const dati = vis(wrapper, 'VisXYContainer').props('data') as { label: string }[]

    expect(dati).toHaveLength(2)
    expect(dati.map((d) => d.label)).toEqual(['% Grasso corporeo', 'Massa magra (kg)'])
  })

  it('passa a VisGroupedBar gli accessor x/y corretti', () => {
    const wrapper = montaConDati(RIGHE)
    const bar = vis(wrapper, 'VisGroupedBar')
    const x = bar.props('x') as (d: unknown, i: number) => number
    const y = bar.props('y') as ((d: { a: number, b: number }) => number)[]

    expect(x({}, 1)).toBe(1)
    expect(y).toHaveLength(2)
    expect(y[0]({ a: 20.6, b: 16.8 })).toBe(20.6)
    expect(y[1]({ a: 20.6, b: 16.8 })).toBe(16.8)
  })

  it('colora la serie A in neutro e la serie B con il colore passato', () => {
    const wrapper = montaConDati(RIGHE)
    const colore = vis(wrapper, 'VisGroupedBar').props('color') as (d: unknown, i: number) => string

    expect(colore({}, 0)).toBe('var(--fg4)')
    expect(colore({}, 1)).toBe('var(--chart-1)')
  })

  it('usa l\'orientamento verticale di default e quello orizzontale se richiesto', () => {
    const verticale = montaConDati(RIGHE)
    expect(vis(verticale, 'VisGroupedBar').props('orientation')).toBe('vertical')
    expect(vis(verticale, 'VisAxis').props('type')).toBe('x')

    const orizzontale = mount(ConfrontoBarChart, {
      props: {
        titolo: 'Circonferenze', righe: RIGHE, etichettaA: 'A', etichettaB: 'B',
        colore: 'var(--chart-2)', orientamento: 'orizzontale',
      },
      global: { stubs: { ChartContainer: { template: '<div><slot /></div>' } } },
    })
    expect(vis(orizzontale, 'VisGroupedBar').props('orientation')).toBe('horizontal')
    expect(vis(orizzontale, 'VisAxis').props('type')).toBe('y')
  })

  it('etichetta i tick dell\'asse con il label della riga corrispondente', () => {
    const wrapper = montaConDati(RIGHE)
    const tickFormat = vis(wrapper, 'VisAxis').props('tickFormat') as (i: number) => string

    expect(tickFormat(0)).toBe('% Grasso corporeo')
    expect(tickFormat(1)).toBe('Massa magra (kg)')
  })

  it('mostra la legenda con le etichette delle due visite quando ci sono dati', () => {
    const wrapper = montaConDati(RIGHE)
    expect(wrapper.text()).toContain('12 gen 2026')
    expect(wrapper.text()).toContain('18 ago 2026')
  })
})
