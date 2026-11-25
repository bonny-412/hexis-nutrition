import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfrontoBarChart from './ConfrontoBarChart.vue'

/** Il grafico è HTML/CSS: si controllano direttamente righe, valori scritti e lunghezze delle barre. */

const RIGHE = [
  { label: '% Grasso corporeo', a: 20.6, b: 16.8, unita: '%' },
  { label: 'Massa magra (kg)', a: 67.1, b: 65.3, unita: 'kg' },
  { label: 'Non confrontabile', a: null, b: 12, unita: 'kg' },
]

function monta(props: Partial<InstanceType<typeof ConfrontoBarChart>['$props']> = {}) {
  return mount(ConfrontoBarChart, {
    props: {
      titolo: 'Plicometria',
      righe: RIGHE,
      etichettaA: '12 gen 2026',
      etichettaB: '18 ago 2026',
      colore: 'var(--chart-1)',
      ...props,
    },
  })
}

/** Le due barre di un gruppo, nell'ordine A poi B. */
function barre(wrapper: ReturnType<typeof monta>, indiceGruppo: number) {
  return wrapper.findAll('[data-test="confronto-gruppo"]')[indiceGruppo].findAll('[data-test="barra"]')
}

describe('ConfrontoBarChart', () => {
  it('mostra il titolo passato', () => {
    expect(monta({ righe: [] }).text()).toContain('Plicometria')
  })

  it('dice che non ci sono misure confrontabili se nessuna riga ha entrambi i valori', () => {
    const wrapper = monta({ righe: [{ label: 'FMI', a: null, b: 4.4, unita: '' }] })

    expect(wrapper.text()).toContain('Nessuna misura presente in entrambe le visite.')
    expect(wrapper.find('[data-test="legenda-confronto"]').exists()).toBe(false)
  })

  it('esclude le righe con un solo lato valorizzato', () => {
    const gruppi = monta().findAll('[data-test="confronto-gruppo"]')

    expect(gruppi).toHaveLength(2)
    expect(gruppi.map((g) => g.text())).toEqual([
      expect.stringContaining('% Grasso corporeo'),
      expect.stringContaining('Massa magra (kg)'),
    ])
  })

  it('in verticale scrive i valori senza unità, che sta già nell\'etichetta del parametro', () => {
    const gruppo = monta().findAll('[data-test="confronto-gruppo"]')[1]

    expect(gruppo.text()).toContain('67,10')
    expect(gruppo.text()).toContain('65,30')
    expect(gruppo.text()).not.toContain('kg 65')
    expect(gruppo.text()).not.toContain('67,10 kg')
  })

  it('in orizzontale scrive i valori con l\'unità', () => {
    const gruppo = monta({ orientamento: 'orizzontale' }).findAll('[data-test="confronto-gruppo"]')[1]

    expect(gruppo.text()).toContain('67,10 kg')
    expect(gruppo.text()).toContain('65,30 kg')
  })

  it('scala ogni coppia sul proprio massimo: il valore più grande occupa l\'altezza piena', () => {
    const wrapper = monta()
    const [a, b] = barre(wrapper, 0) // % grasso: 20,6 → 16,8

    // Altezza piena = 78% della colonna (il resto è per il valore scritto sopra).
    expect(a.attributes('style')).toContain('height: 78%')
    expect(parseFloat(b.attributes('style')!.match(/height: ([\d.]+)%/)![1])).toBeCloseTo((16.8 / 20.6) * 78, 5)
  })

  it('usa per la visita A una tinta chiara del colore passato e per la B il colore pieno', () => {
    const [a, b] = barre(monta({ colore: 'var(--chart-2)' }), 0)

    expect(a.attributes('data-colore')).toBe('color-mix(in srgb, var(--chart-2) 32%, var(--surf))')
    expect(b.attributes('data-colore')).toBe('var(--chart-2)')
  })

  it('usa colonne in verticale di default e barre orizzontali se richiesto', () => {
    expect(monta().find('[data-test="confronto-verticale"]').exists()).toBe(true)

    const orizzontale = monta({ orientamento: 'orizzontale' })
    expect(orizzontale.find('[data-test="confronto-orizzontale"]').exists()).toBe(true)
    expect(orizzontale.find('[data-test="confronto-verticale"]').exists()).toBe(false)
  })

  it('mostra la legenda con le etichette delle due visite quando ci sono dati', () => {
    const legenda = monta().find('[data-test="legenda-confronto"]')
    expect(legenda.text()).toContain('12 gen 2026')
    expect(legenda.text()).toContain('18 ago 2026')
  })
})
