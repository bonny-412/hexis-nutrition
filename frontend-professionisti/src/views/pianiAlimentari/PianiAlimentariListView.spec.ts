import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PianiAlimentariListView from './PianiAlimentariListView.vue'
import * as api from '@/api/pianiAlimentari'

vi.mock('@/api/pianiAlimentari')

async function creaRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/piani-alimentari', name: 'piani-alimentari', component: PianiAlimentariListView },
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: { template: '<div/>' } },
      { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
  router.push('/piani-alimentari')
  await router.isReady()
  return router
}

function montaView(router: Awaited<ReturnType<typeof creaRouter>>) {
  return mount(PianiAlimentariListView, { global: { plugins: [router, createTestingPinia()] } })
}

describe('PianiAlimentariListView', () => {
  beforeEach(() => {
    vi.mocked(api.cerca).mockResolvedValue({
      contenuto: [
        { id: '1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Ipertrofia · fase 2', stato: 'ATTIVO', obiettivoKcal: 2800, dataFine: '2026-09-18' },
      ],
      paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1,
    })
  })

  it('carica e mostra i piani alla creazione del componente', async () => {
    const router = await creaRouter()
    const wrapper = montaView(router)
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Mario Bianchi')
    expect(wrapper.text()).toContain('Ipertrofia · fase 2')
  })

  it('il chip di stato filtra la ricerca e riporta la pagina a 0', async () => {
    const router = await creaRouter()
    const wrapper = montaView(router)
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())

    await wrapper.find('[data-test="chip-stato-ATTIVO"]').trigger('click')

    await vi.waitFor(() =>
      expect(api.cerca).toHaveBeenCalledWith(expect.objectContaining({ stato: 'ATTIVO', pagina: 0 })),
    )
  })

  it('mostra lo stato vuoto quando non ci sono risultati', async () => {
    vi.mocked(api.cerca).mockResolvedValue({ contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 })
    const router = await creaRouter()
    const wrapper = montaView(router)
    await vi.waitFor(() => expect(api.cerca).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Nessun piano trovato')
  })
})
