import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
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
})
