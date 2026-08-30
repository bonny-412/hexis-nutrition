// src/views/ResetPasswordView.spec.ts
import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ResetPasswordView from './ResetPasswordView.vue'
import * as authApi from '@/api/auth'
import { ApiError } from '@/api/client'

vi.mock('@/api/auth')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/reset-password', name: 'reset-password', component: ResetPasswordView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
      { path: '/password-dimenticata', name: 'password-dimenticata', component: { template: '<div/>' } },
    ],
  })
}

describe('ResetPasswordView', () => {
  it('segnala se le due password non coincidono, senza chiamare l\'API', async () => {
    const router = creaRouter()
    router.push('/reset-password?token=abc')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('altra1234')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('non coincidono')
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('su successo naviga al login', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/reset-password?token=abc')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(authApi.resetPassword).toHaveBeenCalledWith('abc', 'password123')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('mostra un messaggio dedicato se il token non è più valido', async () => {
    vi.mocked(authApi.resetPassword).mockRejectedValue(new ApiError(400, 'Token non valido'))
    const router = creaRouter()
    router.push('/reset-password?token=scaduto')
    await router.isReady()
    const wrapper = mount(ResetPasswordView, { global: { plugins: [router] } })

    await wrapper.findAll('input[type="password"]')[0].setValue('password123')
    await wrapper.findAll('input[type="password"]')[1].setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('non è più valido')
  })
})
