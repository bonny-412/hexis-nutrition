import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import * as pazientiApi from '@/api/pazienti'
import AppHeader from './AppHeader.vue'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

function montaHeader(router: ReturnType<typeof creaRouter>) {
  return mount(AppHeader, { attachTo: document.body, global: { plugins: [router, createTestingPinia()] } })
}

describe('AppHeader', () => {
  it('distingue un errore di ricerca dai risultati vuoti e consente di riprovare', async () => {
    const cerca = vi.spyOn(pazientiApi, 'cerca')
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ contenuto: [], paginaCorrente: 0, dimensionePagina: 5, totaleElementi: 0, totalePagine: 0 })
    const router = creaRouter()
    await router.push('/')
    const wrapper = montaHeader(router)
    try {
      await wrapper.get('[data-test="command-palette-trigger"]').trigger('click')
      const palette = document.querySelector('[role="dialog"][aria-label="Cerca ovunque"]')!
      const input = palette.querySelector<HTMLInputElement>('[role="combobox"]')!
      input.value = 'Verdi'
      input.dispatchEvent(new Event('input', { bubbles: true }))
      await vi.waitFor(() => expect(palette.textContent).toContain('Non è stato possibile cercare i pazienti.'))
      expect(palette.textContent).not.toContain('Nessun risultato')

      Array.from(palette.querySelectorAll('button')).find((button) => button.textContent?.trim() === 'Riprova')?.click()
      await vi.waitFor(() => expect(palette.textContent).toContain('Nessun risultato per «Verdi»'))
      expect(cerca).toHaveBeenCalledTimes(2)
      expect(document.activeElement).toBe(input)
    } finally {
      wrapper.unmount()
      cerca.mockRestore()
    }
  })

  it('naviga i risultati della palette con le frecce e attiva la selezione con Invio', async () => {
    const router = creaRouter()
    await router.push('/')
    const wrapper = montaHeader(router)
    try {
      const trigger = wrapper.get<HTMLButtonElement>('[data-test="command-palette-trigger"]')
      trigger.element.focus()
      await trigger.trigger('click')
      await flushPromises()
      let input = document.querySelector<HTMLInputElement>('[role="combobox"]')!
      expect(document.activeElement).toBe(input)

      input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
      await vi.waitFor(() => expect(document.activeElement).toBe(trigger.element))
      expect(document.querySelector('[role="combobox"]')).toBeNull()

      await trigger.trigger('click')
      await flushPromises()
      input = document.querySelector<HTMLInputElement>('[role="combobox"]')!
      input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true }))
      await flushPromises()
      const selezione = document.getElementById(input.getAttribute('aria-activedescendant') ?? '')
      expect(selezione?.textContent).toContain('Nuovo alimento')
      expect(selezione?.getAttribute('aria-selected')).toBe('true')
      expect(document.activeElement).toBe(input)

      input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))
      await flushPromises()
      expect(router.currentRoute.value.fullPath).toBe('/alimenti?azione=nuovo')
      expect(document.querySelector('[role="combobox"]')).toBeNull()
    } finally {
      wrapper.unmount()
    }
  })

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
