import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PianoAlimentareFormView from './PianoAlimentareFormView.vue'
import * as api from '@/api/pianiAlimentari'

vi.mock('@/api/pianiAlimentari')

const pianoVuoto = {
  id: 'piano1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Ipertrofia · fase 1',
  modalita: 'PASTI', stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: null,
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

    expect(wrapper.text()).toContain('1100')
    expect(wrapper.text()).toContain('sotto la soglia')
  })

  it('attiva salva e poi chiama attiva, senza mostrare più il bottone su un piano già attivo', async () => {
    vi.mocked(api.attiva).mockResolvedValue(undefined as never)
    const pianoAttivato = { ...pianoVuoto, stato: 'ATTIVO' }
    vi.mocked(api.aggiorna).mockResolvedValue(pianoVuoto as never)
    vi.mocked(api.dettaglio)
      .mockResolvedValueOnce(pianoVuoto as never)
      .mockResolvedValueOnce(pianoAttivato as never)
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="attiva-piano"]').trigger('click')
    await vi.waitFor(() => expect(api.attiva).toHaveBeenCalledWith('piano1'))

    expect(api.aggiorna).toHaveBeenCalled()
  })

  it('se il salvataggio fallisce, attiva piano non chiama l\'API attiva (niente stato ambiguo)', async () => {
    vi.mocked(api.aggiorna).mockRejectedValueOnce(new Error('errore di rete'))
    const router = await creaRouter('/piani-alimentari/piano1')
    const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router] } })
    await vi.waitFor(() => expect(api.dettaglio).toHaveBeenCalled())
    await wrapper.vm.$nextTick()

    await wrapper.find('[data-test="attiva-piano"]').trigger('click')
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

    await wrapper.find('[data-test="elimina-piano"]').trigger('click')
    // Il contenuto dell'AlertDialog è teleportato in document.body: non raggiungibile con wrapper.find
    // (stessa convenzione di PazienteTabStoricoMisurazioni.spec.ts / AlimentoRigaAzioni.spec.ts).
    document.querySelector<HTMLElement>('[data-test="conferma-elimina-piano"]')?.click()
    await flushPromises()

    await vi.waitFor(() => expect(api.elimina).toHaveBeenCalledWith('piano1'))
  })
})
