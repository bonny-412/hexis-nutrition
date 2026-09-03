import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import PazientiListView from './PazientiListView.vue'
import { DatePicker } from '@/components/ui/date-picker'
import * as pazientiApi from '@/api/pazienti'
import type { PaginaPazienti } from '@/api/pazienti'

vi.mock('@/api/pazienti')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: PazientiListView },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: '3331234567', dataNascita: '1990-01-01', sesso: 'M', lavoro: 'Impiegato', tipoLavoro: 'ATTIVO' as const, note: null,
  statoAccount: 'MAI_INVITATO' as const, archiviato: false,
}

function paginaCon(contenuto: typeof pazienteEsempio[]): PaginaPazienti {
  return { contenuto, paginaCorrente: 0, dimensionePagina: 20, totaleElementi: contenuto.length, totalePagine: 1 }
}

async function montaView(attachToBody = false) {
  const router = creaRouter()
  router.push('/pazienti')
  await router.isReady()
  const wrapper = mount(PazientiListView, {
    attachTo: attachToBody ? document.body : undefined,
    global: { plugins: [router, createTestingPinia()] },
  })
  await flushPromises()
  return wrapper
}

afterEach(() => {
  vi.useRealTimers()
  vi.mocked(toast.error).mockClear()
  vi.mocked(toast.success).mockClear()
})

describe('PazientiListView', () => {
  it('mostra i pazienti caricati dal backend con le colonne reali', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com · RSSMRA80A01H501U')
    expect(wrapper.text()).toContain('3331234567')
    expect(wrapper.text()).toContain('Invita')
  })

  it('chiama cerca() invece di lista() e non richiede lista completa', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    await montaView()

    expect(pazientiApi.cerca).toHaveBeenCalled()
    expect(pazientiApi.lista).not.toHaveBeenCalled()
  })

  it('la ricerca testuale è debounced e richiama cerca() con il testo digitato', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    await wrapper.find('input[type="search"]').setValue('marco')
    expect(pazientiApi.cerca).not.toHaveBeenCalled()

    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ ricerca: 'marco', pagina: 0 }))
  })

  it('il click su un chip di stato richiama cerca() con statoAccount filtrato', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    const chipAttivo = wrapper.findAll('button').find((b) => b.text() === 'Attivo')
    await chipAttivo?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ statoAccount: 'ATTIVO', pagina: 0 }))
  })

  it('i filtri avanzati (sesso, intervallo date) richiamano cerca() con i parametri', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()
    vi.mocked(pazientiApi.cerca).mockClear()

    const pickerDataDa = wrapper.findAllComponents(DatePicker).find((c) => c.props('id') === 'data-nascita-da')
    await pickerDataDa?.vm.$emit('update:modelValue', '1990-01-01')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ dataNascitaDa: '1990-01-01', pagina: 0 }))
  })

  it('il toggle "Mostra pazienti archiviati" richiama cerca() con archiviato:true e nasconde Invita', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([{ ...pazienteEsempio, archiviato: true }]))
    await wrapper.find('#filtro-mostra-archiviati').trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ archiviato: true, pagina: 0 }))
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(false)
  })

  it('"Pulisci filtri" resetta ricerca/filtri e richiama cerca() senza criteri attivi', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    await wrapper.find('input[type="search"]').setValue('marco')
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()

    const pulisci = wrapper.findAll('button').find((b) => b.text() === 'Pulisci filtri')
    await pulisci?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({
      ricerca: undefined, statoAccount: undefined, sesso: undefined,
      dataNascitaDa: undefined, dataNascitaA: undefined, archiviato: false, pagina: 0,
    }))
  })

  it('il click su un header ordinabile cicla asc → desc → nessun ordinamento su tre click', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()

    const headerPaziente = wrapper.findAll('th button').find((b) => b.text().includes('Paziente'))
    await headerPaziente?.trigger('click')
    await flushPromises()
    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ ordinaPer: 'nome', direzione: 'asc' }))

    await headerPaziente?.trigger('click')
    await flushPromises()
    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ ordinaPer: 'nome', direzione: 'desc' }))

    await headerPaziente?.trigger('click')
    await flushPromises()
    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ ordinaPer: undefined, direzione: 'asc' }))
  })

  it('mostra lo stato vuoto "primo paziente" se non ci sono pazienti e nessun filtro attivo', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([]))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Nessun paziente presente')
  })

  it('mostra lo stato vuoto "per filtro" se la ricerca non trova risultati', async () => {
    vi.useFakeTimers()
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const wrapper = await montaView()

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([]))
    await wrapper.find('input[type="search"]').setValue('nessuna-corrispondenza-xyz')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.text()).toContain('Nessun risultato trovato')
  })

  it('mostra un errore con bottone Riprova se il caricamento fallisce, e Riprova richiama cerca()', async () => {
    vi.mocked(pazientiApi.cerca).mockRejectedValue(new Error('500'))
    const wrapper = await montaView()

    expect(wrapper.text()).toContain('Non è stato possibile caricare i pazienti.')

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce(paginaCon([pazienteEsempio]))
    const riprova = wrapper.findAll('button').find((b) => b.text() === 'Riprova')
    await riprova?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
  })

  it('invita un paziente e ne aggiorna lo stato in tabella', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const wrapper = await montaView()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('Invitato')
    expect(toast.success).toHaveBeenCalledWith('Invito inviato.')
  })

  it('mostra un errore se l\'invito fallisce e non aggiorna lo stato del paziente', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([{ ...pazienteEsempio, statoAccount: 'MAI_INVITATO' }]))
    vi.mocked(pazientiApi.invita).mockRejectedValue(new Error('409'))
    const wrapper = await montaView()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('Non è stato possibile inviare l\'invito.')
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(true)
    expect(wrapper.text()).not.toContain('Reinvia invito')
  })

  it('archivia un paziente dal menu riga e ricarica la lista', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    vi.mocked(pazientiApi.archivia).mockResolvedValue(undefined)
    const wrapper = await montaView(true)

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.archivia).toHaveBeenCalledWith('1')
    expect(pazientiApi.cerca).toHaveBeenCalledTimes(2)
    expect(toast.success).toHaveBeenCalledWith('Paziente archiviato.')
    wrapper.unmount()
  })

  it('mostra un errore se l\'archiviazione fallisce', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([pazienteEsempio]))
    vi.mocked(pazientiApi.archivia).mockRejectedValue(new Error('500'))
    const wrapper = await montaView(true)

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.archivia).toHaveBeenCalledWith('1')
    expect(toast.error).toHaveBeenCalledWith('Non è stato possibile completare l\'operazione.')
    wrapper.unmount()
  })

  it('de-archivia un paziente dal menu riga in vista archiviati e ricarica la lista', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([{ ...pazienteEsempio, archiviato: true }]))
    vi.mocked(pazientiApi.deArchivia).mockResolvedValue(undefined)
    const wrapper = await montaView(true)
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()
    await wrapper.find('#filtro-mostra-archiviati').trigger('click')
    await flushPromises()

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-de-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.deArchivia).toHaveBeenCalledWith('1')
    expect(toast.success).toHaveBeenCalledWith('Paziente ripristinato.')
    wrapper.unmount()
  })

  it('mostra un errore se la de-archiviazione fallisce', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue(paginaCon([{ ...pazienteEsempio, archiviato: true }]))
    vi.mocked(pazientiApi.deArchivia).mockRejectedValue(new Error('500'))
    const wrapper = await montaView(true)
    const toggleFiltri = wrapper.findAll('button').find((b) => b.text().includes('Filtri avanzati'))
    await toggleFiltri?.trigger('click')
    await flushPromises()
    await wrapper.find('#filtro-mostra-archiviati').trigger('click')
    await flushPromises()

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-de-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.deArchivia).toHaveBeenCalledWith('1')
    expect(toast.error).toHaveBeenCalledWith('Non è stato possibile completare l\'operazione.')
    wrapper.unmount()
  })

  it('i bottoni di paginazione richiamano cerca() con la pagina aggiornata', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValue({
      contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 40, totalePagine: 2,
    })
    const wrapper = await montaView()
    vi.mocked(pazientiApi.cerca).mockClear()
    vi.mocked(pazientiApi.cerca).mockResolvedValue({
      contenuto: [pazienteEsempio], paginaCorrente: 1, dimensionePagina: 20, totaleElementi: 40, totalePagine: 2,
    })

    const successivo = wrapper.findAll('button').find((b) => b.text() === 'Successivo')
    await successivo?.trigger('click')
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenCalledWith(expect.objectContaining({ pagina: 1 }))
  })

  it('se la pagina corrente torna vuota (es. dopo aver archiviato l\'ultimo paziente), torna alla pagina precedente', async () => {
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce({
      contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 21, totalePagine: 2,
    })
    const wrapper = await montaView(true)

    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce({
      contenuto: [pazienteEsempio], paginaCorrente: 1, dimensionePagina: 20, totaleElementi: 21, totalePagine: 2,
    })
    const successivo = wrapper.findAll('button').find((b) => b.text() === 'Successivo')
    await successivo?.trigger('click')
    await flushPromises()

    vi.mocked(pazientiApi.archivia).mockResolvedValue(undefined)
    vi.mocked(pazientiApi.cerca).mockClear()
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce({
      contenuto: [], paginaCorrente: 1, dimensionePagina: 20, totaleElementi: 20, totalePagine: 1,
    })
    vi.mocked(pazientiApi.cerca).mockResolvedValueOnce({
      contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 20, totalePagine: 1,
    })

    await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(pazientiApi.cerca).toHaveBeenLastCalledWith(expect.objectContaining({ pagina: 0 }))
    expect(wrapper.text()).toContain('Luca Verdi')
    wrapper.unmount()
  })
})
