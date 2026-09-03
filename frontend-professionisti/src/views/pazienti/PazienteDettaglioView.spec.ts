import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import PazienteDettaglioView from './PazienteDettaglioView.vue'
import * as pazientiApi from '@/api/pazienti'
import { ApiError } from '@/api/client'
import type { Visita } from '@/api/pazienti'
import { Select, SelectTrigger } from '@/components/ui/select'

vi.mock('@/api/pazienti')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: PazienteDettaglioView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

function visita(overrides: Partial<Visita> = {}): Visita {
  return {
    id: '1',
    dataVisita: '2026-01-01',
    altezzaCm: 178,
    pesoKg: 80,
    bmi: 25.2,
    whr: null,
    whtr: null,
    mamcCm: null,
    circonferenze: {
      vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
      polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
    },
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

async function montaConPaziente(pazienteOverrides: Partial<pazientiApi.Paziente> = {}) {
  vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
    id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
    telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, note: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    ...pazienteOverrides,
  })
  const router = creaRouter()
  router.push('/pazienti/1')
  await router.isReady()
  const wrapper = mount(PazienteDettaglioView, {
    global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
  })
  await flushPromises()
  return wrapper
}

describe('PazienteDettaglioView', () => {
  beforeEach(() => {
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
  })

  it('mostra i dati del paziente caricato', async () => {
    const wrapper = await montaConPaziente()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com')
    expect(wrapper.text()).toContain('RSSMRA80A01H501U')
  })

  it('mostra i campi anagrafici aggiuntivi (telefono, età, sesso, lavoro) quando presenti', async () => {
    const wrapper = await montaConPaziente({
      telefono: '3331234567', dataNascita: '1990-05-20', sesso: 'M', lavoro: 'Impiegato', tipoLavoro: 'ATTIVO',
    })

    expect(wrapper.text()).toContain('3331234567')
    expect(wrapper.text()).toContain('20/05/1990')
    expect(wrapper.text()).toContain('Maschio')
    expect(wrapper.text()).toContain('Impiegato')
    expect(wrapper.text()).toContain('Attivo')
  })

  it('mostra un trattino per i campi anagrafici assenti', async () => {
    const wrapper = await montaConPaziente()
    expect((wrapper.text().match(/—/g) ?? []).length).toBeGreaterThanOrEqual(2)
  })

  it('invita il paziente e ne aggiorna lo stato mostrato', async () => {
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const wrapper = await montaConPaziente()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('Invitato')
    expect(toast.success).toHaveBeenCalledWith('Invito inviato con successo.')
  })

  it('mostra un errore se l\'invito fallisce e non aggiorna lo stato del paziente (nessun optimistic update)', async () => {
    vi.mocked(pazientiApi.invita).mockRejectedValue(new Error('409'))
    const wrapper = await montaConPaziente()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('Non è stato possibile inviare l\'invito.')
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(true)
    expect(wrapper.text()).not.toContain('Reinvia invito')
  })

  it('mostra un messaggio se il paziente non è stato trovato (404)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new ApiError(404, 'Non trovato'))
    const router = creaRouter()
    router.push('/pazienti/999')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Paziente non trovato')
  })

  it('mostra un messaggio generico per errori diversi dal 404 (es. 500 o rete)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new ApiError(500, 'Errore interno'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile caricare il paziente')
  })

  it('mostra "Nessuna visita programmata" perché l\'agenda non è ancora disponibile', async () => {
    const wrapper = await montaConPaziente()
    expect(wrapper.text()).toContain('Nessuna visita programmata')
  })

  describe('statistiche rapide', () => {
    beforeEach(() => {
      vi.mocked(pazientiApi.visite).mockResolvedValue([
        visita({ id: 'v1', dataVisita: '2026-06-01', pesoKg: 80, bmi: 25.2 }),
        visita({
          id: 'v2', dataVisita: '2026-08-01', pesoKg: 77.5, bmi: 24.4,
          plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
        }),
      ])
    })

    it('mostra peso, bmi, massa grassa e massa magra dell\'ultima visita con il delta rispetto alla precedente', async () => {
      const wrapper = await montaConPaziente()

      expect(wrapper.text()).toContain('Peso attuale')
      expect(wrapper.text()).toContain('77,5 kg')
      expect(wrapper.text()).toContain('2,5 kg vs prec.')
      expect(wrapper.text()).toContain('BMI')
      expect(wrapper.text()).toContain('24,4')
      expect(wrapper.text()).toContain('Massa grassa')
      expect(wrapper.text()).toContain('18,2%')
      expect(wrapper.text()).toContain('Massa magra')
      expect(wrapper.text()).toContain('63,4 kg')
      expect(wrapper.text()).toContain('Aderenza piano')
      expect(wrapper.text()).toContain('Presto disponibile')
    })

    it('mostra l\'obiettivo dell\'ultima visita e la data della prima visita nel sottotitolo', async () => {
      const wrapper = await montaConPaziente()
      expect(wrapper.text()).toContain('Obiettivo attuale: Mantenimento')
      expect(wrapper.text()).toContain('paziente dal 01 giu 2026')
    })
  })

  it('mostra un errore per le statistiche se lo storico visite non si carica, senza bloccare l\'anagrafica', async () => {
    vi.mocked(pazientiApi.visite).mockRejectedValue(new Error('500'))
    const wrapper = await montaConPaziente()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Non è stato possibile caricare i dati clinici del paziente.')
    expect(wrapper.text()).not.toContain('Peso attuale')
  })

  describe('sezione clinica a tab', () => {
    const dueVisite = () => [
      visita({ id: 'v1', dataVisita: '2026-06-01', pesoKg: 80, bmi: 25.2 }),
      visita({ id: 'v2', dataVisita: '2026-08-15', pesoKg: 77.5, bmi: 24.4 }),
    ]

    it('mostra di default il tab Panoramica con l\'andamento', async () => {
      vi.mocked(pazientiApi.visite).mockResolvedValue(dueVisite())
      const wrapper = await montaConPaziente()

      expect(wrapper.text()).toContain('Peso')
      expect(wrapper.text()).toContain('BMI')
      expect(wrapper.text()).not.toContain('Nessuna visita registrata.')
    })

    it('passa al tab Storico misurazioni e mostra le visite dalla più recente', async () => {
      vi.mocked(pazientiApi.visite).mockResolvedValue(dueVisite())
      const wrapper = await montaConPaziente()

      const tabStorico = wrapper.findAll('button').find((b) => b.text() === 'Storico misurazioni')
      await tabStorico?.trigger('click')

      const righe = wrapper.findAll('[data-test="storico-riga"]')
      expect(righe).toHaveLength(2)
      expect(righe[0].text()).toContain('15 ago 2026')
      expect(righe[1].text()).toContain('01 giu 2026')
    })

    it('passa al tab Confronto visite e mostra la tabella comparativa', async () => {
      vi.mocked(pazientiApi.visite).mockResolvedValue(dueVisite())
      const wrapper = await montaConPaziente()

      const tabConfronto = wrapper.findAll('button').find((b) => b.text() === 'Confronto visite')
      await tabConfronto?.trigger('click')

      expect(wrapper.text()).toContain('Circonferenze')
      expect(wrapper.text()).toContain('Variazione')
    })

    it('passa al tab Piani alimentari e mostra il placeholder', async () => {
      const wrapper = await montaConPaziente()

      const tabPiani = wrapper.findAll('button').find((b) => b.text() === 'Piani alimentari')
      await tabPiani?.trigger('click')

      expect(wrapper.text()).toContain('Nessun piano collegato')
    })

    it('cambia tab anche tramite la select pensata per gli schermi piccoli', async () => {
      vi.mocked(pazientiApi.visite).mockResolvedValue(dueVisite())
      const wrapper = await montaConPaziente()

      const select = wrapper.findAllComponents(Select).find((s) => s.findComponent(SelectTrigger).attributes('id') === 'sezione-clinica-tab')
      await select?.vm.$emit('update:modelValue', 'storico')
      await wrapper.vm.$nextTick()

      expect(wrapper.findAll('[data-test="storico-riga"]')).toHaveLength(2)
    })
  })
})
