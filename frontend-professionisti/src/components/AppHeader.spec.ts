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

describe('AppHeader', () => {
  it('il click su "Esci" fa logout e naviga al login', async () => {
    const router = creaRouter()
    router.push('/')
    await router.isReady()
    const wrapper = mount(AppHeader, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(auth.logout).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('login')
  })
})
