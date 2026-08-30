import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteNuovoView from './PazienteNuovoView.vue'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: PazienteNuovoView },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PazienteNuovoView', () => {
  it('crea il paziente e naviga al suo dettaglio', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '42', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, altezzaCm: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com', telefono: undefined,
    })
    expect(router.currentRoute.value.path).toBe('/pazienti/42')
  })

  it('mostra un errore se la creazione fallisce', async () => {
    vi.mocked(pazientiApi.crea).mockRejectedValue(new Error('email duplicata'))
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile creare il paziente')
  })
})
