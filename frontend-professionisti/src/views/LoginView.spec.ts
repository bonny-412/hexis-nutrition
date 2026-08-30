import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import LoginView from './LoginView.vue'

function creaRouterDiTest() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: LoginView },
      { path: '/', name: 'dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/password-dimenticata', name: 'password-dimenticata', component: { template: '<div />' } },
    ],
  })
}

describe('LoginView', () => {
  let router: ReturnType<typeof creaRouterDiTest>

  beforeEach(async () => {
    router = creaRouterDiTest()
    router.push('/login')
    await router.isReady()
  })

  it('dopo un login riuscito naviga alla dashboard', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockResolvedValue(undefined)

    await wrapper.find('input[type="email"]').setValue('anna@studio.it')
    await wrapper.find('input[type="password"]').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(auth.login).toHaveBeenCalledWith('anna@studio.it', 'password123', true)
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('mostra il banner di errore su credenziali non valide', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockRejectedValue(new ApiError(401, 'Credenziali non valide'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Email o password non corrette')
  })

  it('il link "password dimenticata" porta alla rotta dedicata', () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })

    const link = wrapper.find('a[href="/password-dimenticata"]')

    expect(link.exists()).toBe(true)
  })

  it('mostra un banner generico se il login fallisce per un errore non gestito (es. servizio non raggiungibile)', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockRejectedValue(new Error('network down'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Servizio non raggiungibile')
  })

  it('mostra il banner generico anche per un ApiError diverso da 401 (es. ruolo non professionista)', async () => {
    const wrapper = mount(LoginView, { global: { plugins: [router, createTestingPinia()] } })
    const auth = useAuthStore()
    vi.mocked(auth.login).mockRejectedValue(new ApiError(403, 'Accesso riservato ai professionisti'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Servizio non raggiungibile')
  })
})
