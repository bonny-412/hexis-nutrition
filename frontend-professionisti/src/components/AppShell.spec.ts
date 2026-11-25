import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import AppShell from './AppShell.vue'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
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

describe('AppShell', () => {
  it('mantiene la compressione desktop al rimontaggio e conserva i nomi dei link', async () => {
    const router = creaRouter()
    await router.push('/alimenti')
    const options = { global: { plugins: [router, createTestingPinia()] } }
    const wrapper = mount(AppShell, options)
    await wrapper.get('[aria-label="Comprimi sidebar"]').trigger('click')
    wrapper.unmount()

    await router.push('/agenda')
    const rimontata = mount(AppShell, options)
    try {
      expect(rimontata.find('[aria-label="Espandi sidebar"]').exists()).toBe(true)
      expect(rimontata.get('a[href="/agenda"]').attributes('aria-label')).toBe('Agenda')
      expect(rimontata.get('a[href="/alimenti"]').attributes('aria-label')).toBe('Alimenti')
    } finally {
      rimontata.unmount()
      sessionStorage.removeItem('hexis-sidebar-compressa')
    }
  })

  it('apre il cassetto della sidebar cliccando l\'hamburger nell\'header', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(AppShell, { attachTo: document.body, global: { plugins: [router, createTestingPinia()] } })

    // Il contenuto della sidebar mobile (dentro lo Sheet) non è nel DOM finché non si apre.
    expect(document.body.querySelectorAll('nav').length).toBe(1)

    await wrapper.find('[aria-label="Apri menu"]').trigger('click')
    await flushPromises()

    expect(document.body.querySelectorAll('nav').length).toBe(2)

    wrapper.unmount()
  })
})
