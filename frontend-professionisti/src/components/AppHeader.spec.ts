import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from './AppHeader.vue'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

function montaHeader(router: ReturnType<typeof creaRouter>) {
  return mount(AppHeader, { attachTo: document.body, global: { plugins: [router, createTestingPinia()] } })
}

describe('AppHeader', () => {
  it('il click sul chip profilo apre il menu con "Esci dall\'account"', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = montaHeader(router)

    expect(wrapper.text()).not.toContain("Esci dall'account")

    await wrapper.find('[aria-label="Menu profilo"]').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain("Esci dall'account")
    wrapper.unmount()
  })

  it('il click su "Esci dall\'account" nel menu fa logout e naviga al login', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = montaHeader(router)
    const auth = useAuthStore()

    await wrapper.find('[aria-label="Menu profilo"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="logout"]')?.click()
    await flushPromises()

    expect(auth.logout).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('login')
    wrapper.unmount()
  })

  it('il click sull\'hamburger emette "apri-menu"', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = montaHeader(router)

    await wrapper.find('[aria-label="Apri menu"]').trigger('click')

    expect(wrapper.emitted('apri-menu')).toHaveLength(1)
    wrapper.unmount()
  })

  it('mostra il marchio Hexis accanto all\'hamburger', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = montaHeader(router)

    expect(wrapper.find('[data-test="brand-mobile"]').text()).toContain('Hexis')
    wrapper.unmount()
  })
})
