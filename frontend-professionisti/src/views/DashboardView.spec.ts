import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardView from './DashboardView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('DashboardView', () => {
  it('mostra il numero di pazienti attivi calcolato dalla lista reale', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([
      { id: '1', nome: 'A', cognome: 'A', codiceFiscale: 'RSSMRA80A01H501U', email: 'a@a.it', telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, note: null, statoAccount: 'ATTIVO', archiviato: false, obiettivoUltimaVisita: null, dataUltimaVisita: null },
      { id: '2', nome: 'B', cognome: 'B', codiceFiscale: 'RSSMRA80A01H501U', email: 'b@b.it', telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, note: null, statoAccount: 'ATTIVO', archiviato: false, obiettivoUltimaVisita: null, dataUltimaVisita: null },
      { id: '3', nome: 'C', cognome: 'C', codiceFiscale: 'RSSMRA80A01H501U', email: 'c@c.it', telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, note: null, statoAccount: 'MAI_INVITATO', archiviato: false, obiettivoUltimaVisita: null, dataUltimaVisita: null },
    ])
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('Disponibile a breve')
  })

  it('mostra un trattino se il caricamento fallisce', async () => {
    vi.mocked(pazientiApi.lista).mockRejectedValue(new Error('rete'))
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('—')
  })
})
