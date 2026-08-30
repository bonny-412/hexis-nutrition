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
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('AppShell', () => {
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
