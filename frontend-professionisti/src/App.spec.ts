import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import App from './App.vue'

describe('App', () => {
  it('monta senza errori e renderizza la vista della rotta corrente', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', name: 'home', component: { template: '<div>Vista di test</div>' } }],
    })

    const wrapper = mount(App, {
      global: { plugins: [router] },
    })
    await router.isReady()

    expect(wrapper.text()).toContain('Vista di test')
  })

  it('mostra l’errore all’avvio e riapre la stessa rotta dopo Riprova', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{
        path: '/agenda',
        component: { template: '<div>Agenda di test</div>' },
        meta: { requiresAuth: true },
      }],
    })
    await router.push('/agenda')
    const ripristinaSessione = vi.fn()
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true)
    const wrapper = mount(App, {
      props: { ripristinaSessione },
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('Non riusciamo a raggiungere il server')
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(ripristinaSessione).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value.path).toBe('/agenda')
    expect(wrapper.text()).toContain('Agenda di test')
    wrapper.unmount()
  })
})
