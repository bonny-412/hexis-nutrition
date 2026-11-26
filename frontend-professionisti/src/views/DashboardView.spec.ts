import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardView from './DashboardView.vue'
import DaFareCard from '@/components/dashboard/DaFareCard.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/agenda', name: 'agenda', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/piani-alimentari', name: 'piani-alimentari', component: { template: '<div/>' } },
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: { template: '<div/>' } },
      { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('DashboardView', () => {
  it('mostra conteggi e un solo pannello Da fare e consente di cambiare tab da tastiera', async () => {
    const router = creaRouter()
    await router.push('/')
    const wrapper = mount(DaFareCard, {
      props: {
        oggi: '2026-11-25',
        fineFinestraScadenze: '2026-12-02',
        inScadenza: { voci: [], totale: 0 },
        bozze: { voci: [{
          id: 'bozza-1', nome: 'Piano di prova', pazienteNomeCompleto: 'Paziente di prova',
          pazienteNome: 'Paziente', pazienteCognome: 'di prova', pazienteEmail: null, stato: 'BOZZA', obiettivoKcal: null,
          modalita: 'PASTI', dataInizio: '2026-11-25', dataFine: null,
        }], totale: 2 },
        maiInvitati: { voci: [], totale: 0 },
      },
      global: { plugins: [router] },
    })
    const [scadenze, bozze, inviti] = wrapper.findAll('[role="tab"]')
    expect(bozze?.text()).toContain('2')
    expect(bozze?.attributes('aria-selected')).toBe('true')
    expect(wrapper.text()).toContain('Piano di prova')
    expect(wrapper.get('a[href="/piani-alimentari?stato=BOZZA"]').text()).toContain('Vedi tutti')

    await inviti?.trigger('keydown', { key: 'Enter' })
    expect(inviti?.attributes('aria-selected')).toBe('true')
    expect(scadenze?.attributes('aria-selected')).toBe('false')
    expect(wrapper.text()).toContain('Tutti i pazienti sono stati invitati.')
    expect(wrapper.text()).not.toContain('Piano di prova')
    wrapper.unmount()
  })

  it('mostra il numero di pazienti attivi restituito dal server', async () => {
    vi.mocked(pazientiApi.conteggioAttivi).mockResolvedValue(2)
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).not.toContain('Disponibile a breve')
  })

  it('mostra un trattino se il caricamento fallisce', async () => {
    vi.mocked(pazientiApi.conteggioAttivi).mockRejectedValue(new Error('rete'))
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('—')
  })
})
