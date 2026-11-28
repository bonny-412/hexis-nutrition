import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createTestingPinia } from '@pinia/testing'
import { toast } from 'vue-sonner'
import PianoAlimentareFormView from './PianoAlimentareFormView.vue'
import * as api from '@/api/pianiAlimentari'
import * as apiPazienti from '@/api/pazienti'
import { ErroreRete } from '@/api/client'
import { DatePicker } from '@/components/ui/date-picker'

vi.mock('@/api/pianiAlimentari')
vi.mock('@/api/pazienti')
vi.mock('vue-sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

// Verifica 3 prima del rilascio (28/11/2026): errori durante il salvataggio del piano.
// S1 piano duplicato, S2 falso conflitto dopo una risposta persa, S3 modifiche fatte durante il salvataggio.

const piano = {
  id: 'piano1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', pazienteNome: 'Mario', pazienteCognome: 'Bianchi',
  nome: 'Ipertrofia · fase 1', modalita: 'PASTI', stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: '2026-12-12',
  obiettivoKcal: 2800, obiettivoKcalSuggerito: 2800, bmrCalcolato: 1806.45, tdeeCalcolato: 2800,
  formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false, versione: 3, aggiornatoIl: '2026-11-20T12:00:00Z',
  pasti: [{ id: 'pasto1', giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, ordine: 0, righe: [] }],
  giorniMacroTarget: [], esempi: [],
}

const paziente = {
  id: 'p1', nome: 'Mario', cognome: 'Bianchi', email: 'mario@example.com', sesso: 'M', dataNascita: '1990-01-01',
}

const erroreRete = () => new ErroreRete(new TypeError('Failed to fetch'))

async function creaRouter(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/agenda', name: 'agenda', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/piani-alimentari', name: 'piani-alimentari', component: { template: '<div/>' } },
      { path: '/piani-alimentari/nuovo', name: 'piano-alimentare-nuovo', component: PianoAlimentareFormView },
      { path: '/piani-alimentari/:id', name: 'piano-alimentare-modifica', component: PianoAlimentareFormView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
  router.push(path)
  await router.isReady()
  return router
}

async function monta(path: string) {
  const router = await creaRouter(path)
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] }, attachTo: document.body })
  await flushPromises()
  return { wrapper, router }
}

type Wrapper = Awaited<ReturnType<typeof monta>>['wrapper']

const barra = (wrapper: Wrapper) => wrapper.find('[data-test="barra-azioni-form"]')

async function salva(wrapper: Wrapper) {
  await barra(wrapper).find('[data-test="btn-salva"]').trigger('click')
  await flushPromises()
}

async function impostaKcal(wrapper: Wrapper, valore: string) {
  await wrapper.find('[data-test="obiettivo-kcal-input"]').setValue(valore)
}

describe('PianoAlimentareFormView — errori durante il salvataggio', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(api.dettaglio).mockResolvedValue(piano as never)
    vi.mocked(api.aggiorna).mockResolvedValue(piano as never)
    vi.mocked(api.noteDelPiano).mockResolvedValue([])
    vi.mocked(apiPazienti.visite).mockResolvedValue([])
    vi.mocked(apiPazienti.dettaglio).mockResolvedValue(paziente as never)
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('S1: se il primo salvataggio si ferma dopo la creazione, il nuovo tentativo aggiorna lo stesso piano', async () => {
    vi.mocked(api.crea).mockResolvedValue({ ...piano, id: 'nuovo', versione: 0, pasti: [] } as never)
    vi.mocked(api.dettaglio).mockResolvedValue({ ...piano, id: 'nuovo', versione: 0, pasti: [] } as never)
    vi.mocked(api.aggiorna)
      .mockRejectedValueOnce(erroreRete())
      .mockResolvedValueOnce({ ...piano, id: 'nuovo', versione: 1 } as never)
    const { wrapper, router } = await monta('/piani-alimentari/nuovo?pazienteId=p1')
    await impostaKcal(wrapper, '2500')
    wrapper.findAllComponents(DatePicker).find((c) => c.props('id') === 'data-fine')!
      .vm.$emit('update:modelValue', '2026-12-31')
    await flushPromises()

    await salva(wrapper)
    expect(api.crea).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value.path).toBe('/piani-alimentari/nuovo')
    expect(toast.error).toHaveBeenCalledWith('Connessione al server non riuscita. Le modifiche sono ancora qui: riprova tra poco.')

    await salva(wrapper)

    expect(api.crea).toHaveBeenCalledTimes(1)
    expect(api.aggiorna).toHaveBeenCalledTimes(2)
    expect(vi.mocked(api.aggiorna).mock.calls[1][0]).toBe('nuovo')
    expect(toast.success).toHaveBeenCalledWith('Piano salvato.')
  })

  it('S4: la creazione del piano manda una chiave di idempotenza', async () => {
    vi.mocked(api.crea).mockResolvedValue({ ...piano, id: 'nuovo', versione: 0, pasti: [] } as never)
    const { wrapper } = await monta('/piani-alimentari/nuovo?pazienteId=p1')
    await impostaKcal(wrapper, '2500')
    wrapper.findAllComponents(DatePicker).find((c) => c.props('id') === 'data-fine')!
      .vm.$emit('update:modelValue', '2026-12-31')
    await flushPromises()

    await salva(wrapper)

    expect(vi.mocked(api.crea).mock.calls[0][1]).toEqual(expect.any(String))
    expect(vi.mocked(api.crea).mock.calls[0][1]!.length).toBeGreaterThan(0)
  })

  it('S2: dopo una risposta persa riparte dalla versione salvata sul server, senza falso conflitto', async () => {
    const { wrapper } = await monta('/piani-alimentari/piano1')
    vi.mocked(api.aggiorna)
      .mockRejectedValueOnce(erroreRete())
      .mockResolvedValueOnce({ ...piano, obiettivoKcal: 2500, versione: 5 } as never)
    // Il server aveva applicato il salvataggio di cui si è persa la risposta: versione 3 → 4.
    vi.mocked(api.dettaglio).mockResolvedValue({ ...piano, obiettivoKcal: 2500, versione: 4 } as never)
    await impostaKcal(wrapper, '2500')

    await salva(wrapper)
    expect(vi.mocked(api.aggiorna).mock.calls[0][1].versione).toBe(3)

    await salva(wrapper)

    expect(api.dettaglio).toHaveBeenLastCalledWith('piano1')
    expect(vi.mocked(api.aggiorna).mock.calls[1][1].versione).toBe(4)
    expect(toast.success).toHaveBeenCalledWith('Piano salvato.')
  })

  it('S2: se nel frattempo il piano è avanzato di più versioni (altra scheda) manda la sua versione e lascia il conflitto', async () => {
    const { wrapper } = await monta('/piani-alimentari/piano1')
    vi.mocked(api.aggiorna).mockRejectedValueOnce(erroreRete())
    vi.mocked(api.dettaglio).mockResolvedValue({ ...piano, versione: 6 } as never)
    await impostaKcal(wrapper, '2500')

    await salva(wrapper)
    await salva(wrapper)

    expect(vi.mocked(api.aggiorna).mock.calls[1][1].versione).toBe(3)
  })

  it('senza errori di rete non rilegge il piano prima di salvare', async () => {
    const { wrapper } = await monta('/piani-alimentari/piano1')
    vi.mocked(api.dettaglio).mockClear()
    await impostaKcal(wrapper, '2500')

    await salva(wrapper)

    expect(api.dettaglio).not.toHaveBeenCalled()
  })

  it('S3: le modifiche fatte durante il salvataggio restano e risultano non salvate', async () => {
    const { wrapper } = await monta('/piani-alimentari/piano1')
    let rispondi: (valore: unknown) => void = () => {}
    vi.mocked(api.aggiorna)
      .mockImplementationOnce(() => new Promise((risolvi) => { rispondi = risolvi }) as never)
      .mockResolvedValueOnce({ ...piano, obiettivoKcal: 2400, versione: 5 } as never)
    await impostaKcal(wrapper, '2250')

    await barra(wrapper).find('[data-test="btn-salva"]').trigger('click')
    await impostaKcal(wrapper, '2400')
    rispondi({ ...piano, obiettivoKcal: 2250, versione: 4, aggiornatoIl: new Date().toISOString() })
    await flushPromises()

    expect((wrapper.find('[data-test="obiettivo-kcal-input"]').element as HTMLInputElement).value).toBe('2400')
    expect(barra(wrapper).text()).toContain('Modifiche non salvate')

    // Il salvataggio successivo parte dalla versione appena ricevuta.
    await salva(wrapper)
    expect(vi.mocked(api.aggiorna).mock.calls[1][1].versione).toBe(4)
    expect(vi.mocked(api.aggiorna).mock.calls[1][1].obiettivoKcal).toBe(2400)
  })

  it('S3: senza modifiche durante il salvataggio i campi prendono quanto salvato dal server', async () => {
    const { wrapper } = await monta('/piani-alimentari/piano1')
    vi.mocked(api.aggiorna).mockResolvedValueOnce({ ...piano, obiettivoKcal: 2250, versione: 4, aggiornatoIl: new Date().toISOString() } as never)
    await impostaKcal(wrapper, '2250')

    await salva(wrapper)

    expect(barra(wrapper).text()).not.toContain('Modifiche non salvate')
    expect(barra(wrapper).find('[data-test="salvato-alle"]').exists()).toBe(true)
  })

  it('un errore di rete sull\'attivazione, ad attivazione avvenuta, dà "Piano attivato." invece dell\'errore', async () => {
    const pianoPieno = {
      ...piano,
      pasti: [{
        ...piano.pasti[0],
        righe: [{
          alimentoId: 'a1', nome: 'Avena', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
          grassi100g: 6.5, zuccheri100g: 1.1, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null, grammi: 60,
        }],
      }],
    }
    vi.mocked(api.dettaglio).mockResolvedValue(pianoPieno as never)
    vi.mocked(api.aggiorna).mockResolvedValue(pianoPieno as never)
    vi.mocked(api.cerca).mockResolvedValue({ contenuto: [], paginaCorrente: 0, dimensionePagina: 5, totaleElementi: 0, totalePagine: 1 } as never)
    const { wrapper } = await monta('/piani-alimentari/piano1')
    vi.mocked(api.attiva).mockRejectedValueOnce(erroreRete())
    vi.mocked(api.dettaglio).mockResolvedValue({ ...pianoPieno, stato: 'ATTIVO', versione: 5 } as never)

    await wrapper.find('[data-test="opzioni-piano"]').trigger('click')
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()
    await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))
    await flushPromises()

    expect(toast.success).toHaveBeenCalledWith('Piano attivato.')
    expect(toast.error).not.toHaveBeenCalled()
  })
})
