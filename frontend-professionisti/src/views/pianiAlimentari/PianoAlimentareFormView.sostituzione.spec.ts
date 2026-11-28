import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createTestingPinia } from '@pinia/testing'
import PianoAlimentareFormView from './PianoAlimentareFormView.vue'
import SostituisciAlimentoDialog from '@/components/pianiAlimentari/SostituisciAlimentoDialog.vue'
import * as api from '@/api/pianiAlimentari'
import * as apiPazienti from '@/api/pazienti'
import type { DatiAlimentoRiga } from '@/utils/sostituzioneAlimento'

vi.mock('@/api/pianiAlimentari')
vi.mock('@/api/pazienti')
vi.mock('@/api/alimenti', () => ({ cerca: vi.fn() }))
vi.mock('vue-sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

// "Sostituisci alimento" (richiesta di Edoardo, 28/11/2026): collegamento del popup all'editor del piano.

const datiPane = {
  alimentoId: 'pane', nome: 'Pane comune', kcal100g: 270, proteine100g: 8, carboidrati100g: 64, grassi100g: 0.5,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
}
const datiMela = {
  alimentoId: 'mela', nome: 'Mela', kcal100g: 52, proteine100g: 0.3, carboidrati100g: 14, grassi100g: 0.2,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
}
const riso: DatiAlimentoRiga = {
  alimentoId: 'riso', nome: 'Riso, crudo', kcal100g: 350, proteine100g: 7, carboidrati100g: 80, grassi100g: 0.6,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
}

const pasto = (id: string, giorno: string, righe: object[]) => ({
  id, giornoSettimana: giorno, nome: 'Colazione', tipo: 'COLAZIONE', nota: null, ordine: 0, righe,
})

const piano = {
  id: 'piano1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', pazienteNome: 'Mario', pazienteCognome: 'Bianchi',
  nome: 'Mantenimento', modalita: 'PASTI', stato: 'ATTIVO', dataInizio: '2026-11-01', dataFine: '2026-12-31',
  obiettivoKcal: 2000, obiettivoKcalSuggerito: 2000, bmrCalcolato: 1500, tdeeCalcolato: 2000,
  formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false, versione: 1, aggiornatoIl: '2026-11-20T12:00:00Z',
  pasti: [
    pasto('lun', 'LUNEDI', [{ id: 'r1', ...datiPane, grammi: 50 }, { id: 'r2', ...datiMela, grammi: 150 }]),
    pasto('mar', 'MARTEDI', [{ id: 'r3', ...datiPane, grammi: 80 }]),
  ],
  giorniMacroTarget: [], esempi: [],
}

async function monta() {
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
  router.push('/piani-alimentari/piano1')
  await router.isReady()
  const wrapper = mount(PianoAlimentareFormView, { global: { plugins: [router, createTestingPinia()] }, attachTo: document.body })
  await flushPromises()
  return wrapper
}

type Wrapper = Awaited<ReturnType<typeof monta>>

async function salvaERighe(wrapper: Wrapper) {
  await wrapper.find('[data-test="barra-azioni-form"] [data-test="btn-salva"]').trigger('click')
  await flushPromises()
  const richiesta = vi.mocked(api.aggiorna).mock.calls[0][1]
  return richiesta.pasti!.map((p) => ({
    giorno: p.giornoSettimana,
    righe: p.righe.map((r) => ({ alimentoId: r.alimentoId, nome: r.nome, grammi: r.grammi })),
  }))
}

const campoGrammi = (wrapper: Wrapper, nome: string) =>
  wrapper.find(`input[aria-label="Grammi di ${nome}"]`).element as HTMLInputElement

describe('PianoAlimentareFormView — sostituisci alimento', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(api.dettaglio).mockResolvedValue(piano as never)
    vi.mocked(api.aggiorna).mockResolvedValue(piano as never)
    vi.mocked(api.noteDelPiano).mockResolvedValue([])
    vi.mocked(apiPazienti.visite).mockResolvedValue([])
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('apre il popup sulla riga scelta e conta le altre righe con lo stesso alimento', async () => {
    const wrapper = await monta()

    await wrapper.find('[aria-label="Sostituisci Pane comune"]').trigger('click')

    const dialog = wrapper.findComponent(SostituisciAlimentoDialog)
    expect(dialog.props('open')).toBe(true)
    expect(dialog.props('riga')).toMatchObject({ alimentoId: 'pane', grammi: 50 })
    expect(dialog.props('altreRigheUguali')).toBe(1)
  })

  it('sostituisce solo la riga scelta, al suo posto, e mostra i grammi nuovi nel campo', async () => {
    const wrapper = await monta()
    // Un valore appena digitato (anche decimale) resta nel campo finché non lo si sostituisce.
    await wrapper.find('input[aria-label="Grammi di Pane comune"]').setValue('62,5')
    await wrapper.find('[aria-label="Sostituisci Pane comune"]').trigger('click')

    wrapper.findComponent(SostituisciAlimentoDialog).vm.$emit('sostituito', riso, 'carboidrati', false)
    await flushPromises()

    expect(campoGrammi(wrapper, 'Riso, crudo').value).toBe('50') // 62,5 g di pane = 40 g carb. ÷ 80 g/100 g
    expect(await salvaERighe(wrapper)).toEqual([
      { giorno: 'LUNEDI', righe: [
        { alimentoId: 'riso', nome: 'Riso, crudo', grammi: 50 },
        { alimentoId: 'mela', nome: 'Mela', grammi: 150 },
      ] },
      { giorno: 'MARTEDI', righe: [{ alimentoId: 'pane', nome: 'Pane comune', grammi: 80 }] },
    ])
  })

  it('con la casella accesa sostituisce lo stesso alimento in tutti i giorni, ricalcolando i grammi riga per riga', async () => {
    const wrapper = await monta()
    await wrapper.find('[aria-label="Sostituisci Pane comune"]').trigger('click')

    wrapper.findComponent(SostituisciAlimentoDialog).vm.$emit('sostituito', riso, 'carboidrati', true)
    await flushPromises()

    expect(await salvaERighe(wrapper)).toEqual([
      { giorno: 'LUNEDI', righe: [
        { alimentoId: 'riso', nome: 'Riso, crudo', grammi: 40 },
        { alimentoId: 'mela', nome: 'Mela', grammi: 150 },
      ] },
      // 80 g di pane = 51,2 g carb. → 64 g di riso
      { giorno: 'MARTEDI', righe: [{ alimentoId: 'riso', nome: 'Riso, crudo', grammi: 64 }] },
    ])
  })

  it('su un piano concluso il pulsante non c\'è', async () => {
    vi.mocked(api.dettaglio).mockResolvedValue({ ...piano, stato: 'CONCLUSO' } as never)
    const wrapper = await monta()

    expect(wrapper.find('[data-test="sostituisci-alimento"]').exists()).toBe(false)
  })
})
