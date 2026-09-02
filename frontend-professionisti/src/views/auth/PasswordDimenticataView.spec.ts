import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import PasswordDimenticataView from './PasswordDimenticataView.vue'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/password-dimenticata', name: 'password-dimenticata', component: PasswordDimenticataView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

describe('PasswordDimenticataView', () => {
  it('mostra il messaggio generico dopo l\'invio riuscito', async () => {
    vi.mocked(authApi.richiediResetPassword).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/password-dimenticata')
    await router.isReady()
    const wrapper = mount(PasswordDimenticataView, { global: { plugins: [router] } })

    await wrapper.find('input[type="email"]').setValue('a@b.it')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain("Se l'indirizzo esiste")
  })

  it('mostra un errore di rete se la chiamata fallisce', async () => {
    vi.mocked(authApi.richiediResetPassword).mockRejectedValue(new Error('rete'))
    const router = creaRouter()
    router.push('/password-dimenticata')
    await router.isReady()
    const wrapper = mount(PasswordDimenticataView, { global: { plugins: [router] } })

    await wrapper.find('input[type="email"]').setValue('a@b.it')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).not.toContain("Se l'indirizzo esiste")
    expect(toast.error).toHaveBeenCalledWith('Errore di rete, riprova.')
  })
})
