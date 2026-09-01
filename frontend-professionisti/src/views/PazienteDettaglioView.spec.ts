import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteDettaglioView from './PazienteDettaglioView.vue'
import * as pazientiApi from '@/api/pazienti'
import { ApiError } from '@/api/client'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: PazienteDettaglioView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PazienteDettaglioView', () => {
  it('mostra i dati del paziente caricato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com')
  })

  it('invita il paziente e ne aggiorna lo stato mostrato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('INVITATO')
  })

  it('mostra un messaggio se il paziente non è stato trovato (404)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new ApiError(404, 'Non trovato'))
    const router = creaRouter()
    router.push('/pazienti/999')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Paziente non trovato')
  })

  it('mostra un messaggio generico per errori diversi dal 404 (es. 500 o rete)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new ApiError(500, 'Errore interno'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile caricare il paziente')
  })

  it('mostra un errore se l\'invito fallisce e non aggiorna lo stato del paziente (nessun optimistic update)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    vi.mocked(pazientiApi.invita).mockRejectedValue(new Error('409'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile inviare l\'invito')
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(true)
    expect(wrapper.text()).not.toContain('Reinvia invito')
  })
})
