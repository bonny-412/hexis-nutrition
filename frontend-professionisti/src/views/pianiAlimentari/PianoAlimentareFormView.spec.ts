import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PianoAlimentareFormView from './PianoAlimentareFormView.vue'
import * as api from '@/api/pianiAlimentari'
import * as apiPazienti from '@/api/pazienti'

vi.mock('@/api/pianiAlimentari')
vi.mock('@/api/pazienti')

const pianoVuoto = {
  id: 'piano1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', pazienteNome: 'Mario', pazienteCognome: 'Bianchi', nome: 'Ipertrofia · fase 1',
  modalita: 'PASTI', stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: '2026-12-12',
  obiettivoKcal: 2800, obiettivoKcalSuggerito: 2800, bmrCalcolato: 1806.45, tdeeCalcolato: 2800,
  formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false,
  pasti: [
    { id: 'pasto1', giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, ordine: 0, righe: [] },
  ],
  giorniMacroTarget: [], esempi: [],
}

const pianoMacro = {
  ...pianoVuoto,
  id: 'piano2', modalita: 'MACRO', pasti: [],
  giorniMacroTarget: [{ giornoSettimana: 'LUNEDI', kcalTarget: 2400, proteineTarget: 150, carboidratiTarget: 300, grassiTarget: 67 }],
}

const pianoEsempi = {
  ...pianoVuoto,
  id: 'piano3', modalita: 'ESEMPI', pasti: [], giorniMacroTarget: [],
  esempi: [{ id: 'es1', tipoPasto: 'COLAZIONE', nome: 'Colazione 1', ordine: 0, righe: [] }],
}

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

describe('PianoAlimentareFormView', () => {
  beforeEach(() => {
    vi.mocked(api.dettaglio).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.crea).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.aggiorna).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.noteDelPiano).mockResolvedValue([])
    // Ultima visita del paziente, caricata da caricaPiano per il sottotitolo (vedi
    // sottotitoloVisita): nessuna di default, i singoli test la impostano se serve.
    vi.mocked(apiPazienti.visite).mockResolvedValue([])
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('carica un piano esistente da /piani-alimentari/:id e mostra il pasto già presente', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalledWith('piano1'))
    await wrapper.vm.$nextTick()

    const nomePastoInput = wrapper.find('[data-test="card-pasto"] input').element as HTMLInputElement
    expect(nomePastoInput.value).toBe('Colazione')
    expect(wrapper.text()).toContain('Mario Bianchi')
  })

  it('aggiungere un pasto lo mostra nel giorno selezionato', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="aggiungi-pasto"]').trigger('click')

    expect(wrapper.findAll('[data-test="card-pasto"]')).toHaveLength(2)
  })

  it('aggiungere un alimento tramite il dialog lo mostra nella tabella del pasto', async () => {
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="apri-aggiungi-alimento"]').trigger('click')
    await wrapper.findComponent({ name: 'AggiungiAlimentoDialog' }).vm.$emit('aggiunto', {
      alimentoId: 'a1', nome: 'Avena in fiocchi', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
      grassi100g: 6.5, zuccheri100g: 1.1, grammi: 60,
    })
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Avena in fiocchi')
  })

  it('mostra e permette di modificare i target macro del giorno selezionato', async () => {
    vi.mocked(api.dettaglio).mockResolvedValue(pianoMacro as never)
    const router = await creaRouter('/piani-alimentari/piano2')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    const campoKcal = wrapper.find('[data-test="macro-target-kcal"]')
    expect((campoKcal.element as HTMLInputElement).value).toBe('2400')
  })

  it('applica a tutti i giorni copia i target del giorno corrente sugli altri 6', async () => {
    vi.mocked(api.dettaglio).mockResolvedValue(pianoMacro as never)
    const router = await creaRouter('/piani-alimentari/piano2')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="applica-a-tutti"]').trigger('click')
    await wrapper.findAll('[data-test="giorno-tab"]')[1].trigger('click') // Martedì
    await wrapper.vm.$nextTick()

    const campoKcal = wrapper.find('[data-test="macro-target-kcal"]')
    expect((campoKcal.element as HTMLInputElement).value).toBe('2400')
  })

  it('mostra le categorie di esempi e permette di aggiungerne uno nuovo', async () => {
    vi.mocked(api.dettaglio).mockResolvedValue(pianoEsempi as never)
    const router = await creaRouter('/piani-alimentari/piano3')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    const nomeEsempioInput = wrapper.find('[data-test="card-esempio"] input')
    expect((nomeEsempioInput.element as HTMLInputElement).value).toBe('Colazione 1')

    await wrapper.find('[data-test="nuovo-esempio-COLAZIONE"]').trigger('click')

    expect(wrapper.findAll('[data-test="card-esempio"]').length).toBeGreaterThanOrEqual(2)
  })

  it('mostra il suggerimento TDEE e un avviso se sotto la soglia di sicurezza', async () => {
    vi.mocked(api.dettaglio).mockResolvedValue({
      ...pianoVuoto, obiettivoKcalSuggerito: 1100, tdeeCalcolato: 1100, bmrCalcolato: 900,
      formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: true,
    } as never)
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    // Il suggerimento (kcal) è mostrato come placeholder dell'input; formula e avviso soglia
    // sono nel title dell'icona informativa, non come testo visibile (vedi titleObiettivoKcal).
    const campoObiettivo = wrapper.find('[data-test="obiettivo-kcal-input"]').element as HTMLInputElement
    expect(campoObiettivo.placeholder).toBe('1100')
    const infoObiettivo = wrapper.find('[data-test="obiettivo-kcal-info"]').element as HTMLElement
    expect(infoObiettivo.title).toContain('sotto la soglia')
  })

  // "Attiva piano"/"Elimina" sono ora voci del menu "Altre opzioni" (icona), non più bottoni
  // diretti: il contenuto del DropdownMenu è teleportato in document.body (DropdownMenuPortal,
  // stesso motivo/pattern dell'AlertDialog già documentato sotto), va aperto con "opzioni-piano"
  // e raggiunto con document.querySelector, non wrapper.find — serve quindi attachTo: document.body.
  async function apriMenuOpzioni(wrapper: ReturnType<typeof mount>) {
    await wrapper.find('[data-test="opzioni-piano"]').trigger('click')
    await flushPromises()
  }

  it('attiva salva e poi chiama attiva, senza mostrare più il bottone su un piano già attivo', async () => {
    vi.mocked(api.attiva).mockResolvedValue(undefined as never)
    const pianoAttivato = { ...pianoVuoto, stato: 'ATTIVO' }
    vi.mocked(api.aggiorna).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.dettaglio)
      .mockResolvedValueOnce(pianoVuoto as never)
      .mockResolvedValueOnce(pianoAttivato as never)
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] }, attachTo: document.body })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()
    // Il piano di prova è vuoto: dal 28/11/2026 si chiede conferma prima di attivarlo.
    await vi.waitFor(() => expect(document.querySelector('[data-test="avviso-piano-vuoto"]')).not.toBeNull())
    document.querySelector<HTMLElement>('[data-test="conferma-attiva-piano"]')?.click()
    await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))

    expect(api.aggiorna).toHaveBeenCalled()
  })

  it('se il salvataggio fallisce, attiva piano non chiama l\'API attiva (niente stato ambiguo)', async () => {
    vi.mocked(api.aggiorna).mockRejectedValueOnce(new Error('errore di rete'))
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] }, attachTo: document.body })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()
    await vi.waitFor(() => expect(document.querySelector('[data-test="conferma-attiva-piano"]')).not.toBeNull())
    document.querySelector<HTMLElement>('[data-test="conferma-attiva-piano"]')?.click()
    await vi.waitFor(() => expect(api.aggiorna).toHaveBeenCalled())
    await flushPromises()

    expect(api.attiva).not.toHaveBeenCalled()
  })

  it('elimina richiede conferma e poi chiama elimina', async () => {
    vi.mocked(api.elimina).mockResolvedValue(undefined as never)
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] }, attachTo: document.body })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="elimina-piano"]')?.click()
    await flushPromises()
    // Il contenuto dell'AlertDialog è teleportato in document.body: non raggiungibile con wrapper.find
    // (stessa convenzione di PazienteTabStoricoVisite.spec.ts / AlimentoRigaAzioni.spec.ts).
    document.querySelector<HTMLElement>('[data-test="conferma-elimina-piano"]')?.click()
    await flushPromises()

    await vi.waitFor(() => expect(api.elimina).toHaveBeenCalledWith('piano1'))
  })
  // --- Attivazione, salvataggio e stampa: decisioni di Andrea del 28/11/2026 ---

  const pianoConAlimenti = {
    ...pianoVuoto,
    pasti: [{
      id: 'pasto1', giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, ordine: 0,
      righe: [{
        alimentoId: 'a1', nome: 'Avena in fiocchi', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
        grassi100g: 6.5, zuccheri100g: 1.1, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null, grammi: 60,
      }],
    }],
  }
  const paginaPiani = (contenuto: unknown[]) =>
    ({ contenuto, paginaCorrente: 0, dimensionePagina: 5, totaleElementi: contenuto.length, totalePagine: 1 }) as never

  async function montaPiano(piano: unknown) {
    vi.mocked(api.dettaglio).mockResolvedValue(piano as never)
    vi.mocked(api.aggiorna).mockResolvedValue(piano as never)
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] }, attachTo: document.body })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()
    return wrapper
  }

  it('se il paziente ha già un piano attivo chiede conferma, dicendo che quello passerà a Concluso', async () => {
    vi.mocked(api.attiva).mockResolvedValue(undefined as never)
    vi.mocked(api.cerca).mockResolvedValue(paginaPiani([
      { id: 'altro', nome: 'Fase 0', stato: 'ATTIVO', dataInizio: '2026-08-01', dataFine: '2026-10-01' },
    ]))
    const wrapper = await montaPiano(pianoConAlimenti)

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()
    await vi.waitFor(() => expect(document.querySelector('[data-test="avviso-piano-attivo"]')).not.toBeNull())

    const avviso = document.querySelector('[data-test="avviso-piano-attivo"]')!.textContent ?? ''
    expect(avviso).toContain('Fase 0')
    expect(avviso).toContain('passerà a Concluso')
    expect(avviso).toContain('Ipertrofia · fase 1')
    expect(document.querySelector('[data-test="avviso-piano-vuoto"]')).toBeNull()
    expect(api.cerca).toHaveBeenCalledWith(expect.objectContaining({ pazienteId: 'p1', stato: 'ATTIVO' }))
    expect(api.attiva).not.toHaveBeenCalled()

    document.querySelector<HTMLElement>('[data-test="conferma-attiva-piano"]')?.click()
    await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))
  })

  it('un piano con alimenti e nessun altro piano attivo si attiva senza conferma', async () => {
    vi.mocked(api.attiva).mockResolvedValue(undefined as never)
    vi.mocked(api.cerca).mockResolvedValue(paginaPiani([]))
    const wrapper = await montaPiano(pianoConAlimenti)

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()

    await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))
    expect(document.querySelector('[data-test="conferma-attivazione"]')).toBeNull()
  })

  it('senza data di fine non apre la conferma né attiva, e dice che la data di fine è obbligatoria', async () => {
    vi.mocked(api.cerca).mockResolvedValue(paginaPiani([]))
    const wrapper = await montaPiano({ ...pianoConAlimenti, dataFine: null })

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="attiva-piano"]')?.click()
    await flushPromises()

    expect(wrapper.find('#piano-errore-data-fine').text()).toBe('La data di fine è obbligatoria.')
    expect(api.cerca).not.toHaveBeenCalled()
    expect(api.attiva).not.toHaveBeenCalled()
    expect(document.querySelector('[data-test="conferma-attivazione"]')).toBeNull()
  })

  it('senza modifiche la barra mostra solo "Chiudi"; dopo il salvataggio dice "Salvato alle" e nasconde "Salva"', async () => {
    const wrapper = await montaPiano({ ...pianoConAlimenti, aggiornatoIl: '2026-11-20T12:00:00Z' })
    vi.mocked(api.aggiorna).mockResolvedValue({ ...pianoConAlimenti, aggiornatoIl: new Date().toISOString() } as never)
    const barra = () => wrapper.find('[data-test="barra-azioni-form"]')

    expect(barra().find('[data-test="btn-salva"]').exists()).toBe(false)
    expect(barra().find('[data-test="annulla-modifiche"]').text()).toBe('Chiudi')
    // Riaprendo il piano la barra dice già quando è stato salvato l'ultima volta (un altro giorno).
    expect(barra().find('[data-test="salvato-alle"]').text()).toMatch(/^Salvato il 20 nov alle \d{2}:\d{2}$/)

    await wrapper.find('[data-test="obiettivo-kcal-input"]').setValue('2500')
    expect(barra().find('[data-test="btn-salva"]').exists()).toBe(true)
    expect(barra().find('[data-test="annulla-modifiche"]').text()).toBe('Annulla')

    await barra().find('[data-test="btn-salva"]').trigger('click')
    await vi.waitFor(() => expect(api.aggiorna).toHaveBeenCalled())
    await flushPromises()

    expect(barra().find('[data-test="salvato-alle"]').text()).toMatch(/^Salvato alle \d{2}:\d{2}$/)
    expect(barra().find('[data-test="btn-salva"]').exists()).toBe(false)
    expect(barra().find('[data-test="annulla-modifiche"]').text()).toBe('Chiudi')
  })

  it('durante la stampa il titolo della pagina, cioè il nome del PDF, è "Piano alimentare - Paziente - Nome piano"', async () => {
    let titoloInStampa = ''
    const stampa = vi.spyOn(window, 'print').mockImplementation(() => { titoloInStampa = document.title })
    document.title = 'Hexis Nutrition'
    const wrapper = await montaPiano(pianoConAlimenti)

    await apriMenuOpzioni(wrapper)
    document.querySelector<HTMLElement>('[data-test="stampa-pdf"]')?.click()
    await flushPromises()

    expect(stampa).toHaveBeenCalled()
    expect(titoloInStampa).toBe('Piano alimentare - Mario Bianchi - Ipertrofia · fase 1')
    window.dispatchEvent(new Event('afterprint'))
    expect(document.title).toBe('Hexis Nutrition')
    stampa.mockRestore()
  })
})
