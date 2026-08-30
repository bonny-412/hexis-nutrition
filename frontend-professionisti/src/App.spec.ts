import { describe, expect, it, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from './App.vue'
import router from './router'

describe('App', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('monta senza errori e renderizza la vista della rotta corrente', async () => {
    router.push('/login')
    await router.isReady()

    const wrapper = mount(App, {
      global: { plugins: [router] },
    })
    await router.isReady()

    expect(wrapper.text()).toContain('Login')
  })
})
