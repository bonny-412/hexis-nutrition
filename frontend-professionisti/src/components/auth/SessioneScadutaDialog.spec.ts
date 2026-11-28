import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import SessioneScadutaDialog from './SessioneScadutaDialog.vue'
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'
import { ApiError, ErroreRete } from '@/api/client'

vi.mock('@/api/auth')

// Verifica 3 prima del rilascio (28/11/2026), bug S5: a sessione scaduta la password si chiede sopra la pagina,
// senza perdere i dati non salvati.
describe('SessioneScadutaDialog', () => {
  const PROFILO = { id: '1', nome: 'Anna', cognome: 'Bianchi', email: 'anna@studio.it', ruolo: 'PROFESSIONISTA' }

  async function monta() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/piani-alimentari/:id', component: { template: '<div>Piano</div>' } },
        { path: '/login', name: 'login', component: { template: '<div>Login</div>' } },
      ],
    })
    await router.push('/piani-alimentari/piano1')
    await router.isReady()
    vi.mocked(authApi.login).mockResolvedValue({ token: 'primo', ruolo: 'PROFESSIONISTA' })
    vi.mocked(authApi.me).mockResolvedValue(PROFILO)
    const auth = useAuthStore()
    await auth.login(PROFILO.email, 'password', true)
    const wrapper = mount(SessioneScadutaDialog, { global: { plugins: [pinia, router] }, attachTo: document.body })
    return { wrapper, auth, router }
  }

  const dialog = () => document.querySelector('[data-test="sessione-scaduta"]')
  const campoPassword = () => document.querySelector<HTMLInputElement>('#sessione-scaduta-password')!
  const accedi = () => document.querySelector<HTMLButtonElement>('[data-test="sessione-scaduta-accedi"]')!

  async function scriviPassword(valore: string) {
    campoPassword().value = valore
    campoPassword().dispatchEvent(new Event('input'))
    await flushPromises()
  }

  beforeEach(() => {
    vi.mocked(authApi.login).mockReset()
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('compare solo quando la sessione scade, con l\'email del professionista', async () => {
    const { auth } = await monta()
    expect(dialog()).toBeNull()

    auth.segnalaSessioneScaduta()
    await flushPromises()

    expect(dialog()).not.toBeNull()
    expect(dialog()!.textContent).toContain('Password di anna@studio.it')
  })

  it('con la password giusta accede di nuovo con la stessa email, si chiude e resta sulla pagina', async () => {
    const { auth, router } = await monta()
    auth.segnalaSessioneScaduta()
    await flushPromises()
    vi.mocked(authApi.login).mockResolvedValue({ token: 'nuovo', ruolo: 'PROFESSIONISTA' })

    await scriviPassword('password')
    accedi().click()
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({ email: 'anna@studio.it', password: 'password' })
    expect(auth.token).toBe('nuovo')
    expect(auth.sessioneScaduta).toBe(false)
    expect(router.currentRoute.value.path).toBe('/piani-alimentari/piano1')
  })

  it('con la password sbagliata resta aperta e lo dice', async () => {
    const { auth } = await monta()
    auth.segnalaSessioneScaduta()
    await flushPromises()
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'credenziali non valide'))

    await scriviPassword('sbagliata')
    accedi().click()
    await flushPromises()

    expect(dialog()).not.toBeNull()
    expect(document.querySelector('#sessione-scaduta-errore')?.textContent).toBe('Password non corretta.')
  })

  it('senza rete lo dice invece di parlare della password', async () => {
    const { auth } = await monta()
    auth.segnalaSessioneScaduta()
    await flushPromises()
    vi.mocked(authApi.login).mockRejectedValue(new ErroreRete(new TypeError('Failed to fetch')))

    await scriviPassword('password')
    accedi().click()
    await flushPromises()

    expect(document.querySelector('#sessione-scaduta-errore')?.textContent).toBe('Server non raggiungibile. Riprova tra poco.')
  })

  it('senza password non prova ad accedere', async () => {
    const { auth } = await monta()
    auth.segnalaSessioneScaduta()
    await flushPromises()
    vi.mocked(authApi.login).mockClear()

    accedi().click()
    await flushPromises()

    expect(authApi.login).not.toHaveBeenCalled()
    expect(document.querySelector('#sessione-scaduta-errore')?.textContent).toBe('La password è obbligatoria.')
  })

  it('"Esci" porta al login con il ritorno alla pagina', async () => {
    const { auth, router } = await monta()
    auth.segnalaSessioneScaduta()
    await flushPromises()

    document.querySelector<HTMLButtonElement>('[data-test="sessione-scaduta-esci"]')!.click()
    await flushPromises()

    expect(auth.sessioneScaduta).toBe(false)
    expect(auth.professionista).toBeNull()
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/piani-alimentari/piano1')
  })
})
