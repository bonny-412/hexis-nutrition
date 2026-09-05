import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import AlimentiListView from './AlimentiListView.vue'
import * as alimentiApi from '@/api/alimenti'
import type { Alimento, PaginaAlimenti } from '@/api/alimenti'

vi.mock('@/api/alimenti')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: AlimentiListView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

const alimentoEsempio: Alimento = {
  id: '1', nome: 'Petto di pollo, crudo', categoria: 'Carni', kcal: 165, proteineG: 31, grassiG: 3.6,
  carboidratiG: 0, acquaG: 65, fibreG: null, zuccheriG: null, ferroMg: 1, calcioMg: 15, sodioMg: 74, bda: true,
}

function paginaCon(contenuto: Alimento[]): PaginaAlimenti {
  return { contenuto, paginaCorrente: 0, dimensionePagina: 20, totaleElementi: contenuto.length, totalePagine: 1 }
}

async function montaView() {
  const router = creaRouter()
  router.push('/alimenti')
  await router.isReady()
  const wrapper = mount(AlimentiListView, {
    global: { plugins: [router, createTestingPinia()] },
  })
  await flushPromises()
  return wrapper
}

describe('AlimentiListView', () => {
  it('mostra uno stato di caricamento e poi la tabella popolata dopo la ricerca', async () => {
    let risolvi!: (valore: PaginaAlimenti) => void
    vi.mocked(alimentiApi.cerca).mockReturnValue(new Promise((resolve) => { risolvi = resolve }))

    const router = creaRouter()
    router.push('/alimenti')
    await router.isReady()
    const wrapper = mount(AlimentiListView, {
      global: { plugins: [router, createTestingPinia()] },
    })
    await flushPromises()

    expect(wrapper.findAll('tbody tr').length).toBe(0)
    expect(wrapper.findAll('.animate-pulse').length).toBeGreaterThan(0)

    risolvi(paginaCon([alimentoEsempio]))
    await flushPromises()

    expect(wrapper.findAll('.animate-pulse').length).toBe(0)
    expect(wrapper.findAll('tbody tr').length).toBe(1)
    expect(wrapper.text()).toContain('Petto di pollo, crudo')
    expect(wrapper.text()).toContain('BDA')
  })

  it('chiama cerca() al montaggio con fonte TUTTI', async () => {
    vi.mocked(alimentiApi.cerca).mockResolvedValue(paginaCon([alimentoEsempio]))
    await montaView()

    expect(alimentiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ fonte: 'TUTTI', pagina: 0 }))
  })

  it('il click su un chip di fonte cambia lo stato attivo del chip e richiama cerca() con il filtro', async () => {
    vi.mocked(alimentiApi.cerca).mockResolvedValue(paginaCon([alimentoEsempio]))
    const wrapper = await montaView()
    vi.mocked(alimentiApi.cerca).mockClear()

    const chipTutti = wrapper.findAll('button').find((b) => b.text() === 'Tutti')
    const chipBda = wrapper.findAll('button').find((b) => b.text() === 'BDA')
    expect(chipTutti?.classes()).toContain('border-(--sage)')
    expect(chipBda?.classes()).not.toContain('border-(--sage)')

    await chipBda?.trigger('click')
    await flushPromises()

    expect(chipBda?.classes()).toContain('border-(--sage)')
    expect(chipTutti?.classes()).not.toContain('border-(--sage)')
    expect(alimentiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ fonte: 'BDA', pagina: 0 }))
  })

  it('mostra un errore con bottone Riprova se il caricamento fallisce, e Riprova richiama cerca()', async () => {
    vi.mocked(alimentiApi.cerca).mockRejectedValue(new Error('500'))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Non è stato possibile caricare gli alimenti.')

    vi.mocked(alimentiApi.cerca).mockResolvedValueOnce(paginaCon([alimentoEsempio]))
    const riprova = wrapper.findAll('button').find((b) => b.text() === 'Riprova')
    await riprova?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Petto di pollo, crudo')
  })

  it('mostra lo stato vuoto se non ci sono alimenti', async () => {
    vi.mocked(alimentiApi.cerca).mockResolvedValue(paginaCon([]))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Nessun alimento trovato')
  })
})
