import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazientiListView from './PazientiListView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: PazientiListView },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO' as const,
}

describe('PazientiListView', () => {
  it('mostra i pazienti caricati dal backend', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([pazienteEsempio])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Invita')
  })

  it('filtra i pazienti in base al testo di ricerca', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([
      pazienteEsempio,
      { ...pazienteEsempio, id: '2', nome: 'Marco', cognome: 'Bianchi', email: 'marco@example.com' },
    ])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    await wrapper.find('input[type="search"]').setValue('marco')

    expect(wrapper.text()).toContain('Marco Bianchi')
    expect(wrapper.text()).not.toContain('Luca Verdi')
  })

  it('invita un paziente e ne aggiorna lo stato in tabella', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([pazienteEsempio])
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('INVITATO')
  })

  it('mostra uno stato vuoto se non ci sono pazienti', async () => {
    vi.mocked(pazientiApi.lista).mockResolvedValue([])
    const router = creaRouter()
    router.push('/pazienti')
    await router.isReady()
    const wrapper = mount(PazientiListView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Nessun paziente')
  })
})
