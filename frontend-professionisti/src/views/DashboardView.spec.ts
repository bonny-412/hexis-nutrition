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
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/piani-alimentari', name: 'piani-alimentari', component: { template: '<div/>' } },
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: { template: '<div/>' } },
      { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('DashboardView', () => {
  it('mostra il numero di pazienti attivi restituito dal server', async () => {
    vi.mocked(pazientiApi.conteggioAttivi).mockResolvedValue(2)
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(DashboardView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('Disponibile a breve')
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
