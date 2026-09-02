import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import PazienteDettaglioView from './PazienteDettaglioView.vue'
import * as pazientiApi from '@/api/pazienti'
import { ApiError } from '@/api/client'

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

describe('PazienteDettaglioView', () => {
  beforeEach(() => {
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
  })

  it('mostra i dati del paziente caricato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('luca@example.com')
    expect(wrapper.text()).toContain('RSSMRA80A01H501U')
  })

  it('mostra i campi anagrafici aggiuntivi (telefono, età, sesso, lavoro) quando presenti', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: '3331234567', dataNascita: '1990-05-20', sesso: 'M', lavoro: 'Impiegato', tipoLavoro: 'ATTIVO',
      statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('3331234567')
    expect(wrapper.text()).toContain('20/05/1990')
    expect(wrapper.text()).toContain('Maschio')
    expect(wrapper.text()).toContain('Impiegato')
    expect(wrapper.text()).toContain('Attivo')
  })

  it('mostra un trattino per i campi anagrafici assenti', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.findAll('span').filter((s) => s.text() === '—').length).toBeGreaterThanOrEqual(2)
  })

  it('invita il paziente e ne aggiorna lo stato mostrato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.invita).mockResolvedValue(undefined)
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(pazientiApi.invita).toHaveBeenCalledWith('1')
    expect(wrapper.text()).toContain('Invitato')
    expect(toast.success).toHaveBeenCalledWith('Invito inviato con successo.')
  })

  it('mostra la striscia di statistiche rapide con peso, BMI e ultima visita', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
      {
        id: 'v2', dataVisita: '2026-08-01', altezzaCm: 178, pesoKg: 77.5, bmi: 24.4, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Peso attuale')
    expect(wrapper.text()).toContain('77,5 kg')
    expect(wrapper.text()).toContain('2,5 kg dalla visita precedente')
    expect(wrapper.text()).toContain('BMI')
    expect(wrapper.text()).toContain('24,4')
    expect(wrapper.text()).toContain('Ultima visita')
    expect(wrapper.text()).toContain('01 ago 2026')
    expect(wrapper.text()).toContain('Prossima visita')
    expect(wrapper.text()).toContain('Piano alimentare')
    expect(wrapper.findAll('p').filter((p) => p.text() === 'Presto disponibile').length).toBe(2)
  })

  it('mostra la card Visite con l\'elenco dalla più recente, etichettando la più vecchia come "Prima visita"', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
      {
        id: 'v2', dataVisita: '2026-08-15', altezzaCm: 178, pesoKg: 77.5, bmi: 24.4, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Visite')
    expect(wrapper.text()).toContain('15AGO 2026')
    expect(wrapper.text()).toContain('01GIU 2026')
    expect(wrapper.text()).toContain('Prima visita')
    expect(wrapper.text()).toContain('Visita di controllo')
    expect(wrapper.findAll('.bg-\\(--mint\\)').filter((b) => b.text() === 'Completata').length).toBe(2)

    // La più recente (15 ago) deve comparire prima della più vecchia (01 giu) nel markup.
    const testoPagina = wrapper.text()
    expect(testoPagina.indexOf('15AGO 2026')).toBeLessThan(testoPagina.indexOf('01GIU 2026'))
  })

  it('mostra "Nessuna visita registrata" nella card Visite quando non ci sono visite', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Visite')
    expect(wrapper.text()).toContain('Nessuna visita registrata.')
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

  it('mostra un errore se l\'invito fallisce e non aggiorna lo stato del paziente (nessun optimistic update)', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.invita).mockRejectedValue(new Error('409'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    const pulsanteInvita = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsanteInvita?.trigger('click')
    await flushPromises()

    expect(toast.error).toHaveBeenCalledWith('Non è stato possibile inviare l\'invito.')
    expect(wrapper.findAll('button').some((b) => b.text() === 'Invita')).toBe(true)
    expect(wrapper.text()).not.toContain('Reinvia invito')
  })

  it('mostra la sezione Andamento con i dati delle visite', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
      {
        id: 'v2', dataVisita: '2026-08-01', altezzaCm: 178, pesoKg: 77.5, bmi: 24.4, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Andamento')
    expect(wrapper.text()).toContain('Peso')
    expect(wrapper.text()).toContain('77,5')
    expect(wrapper.text()).toContain('BMI')
  })

  it('nasconde la card % Grasso corporeo se nessuna visita ha la plicometria', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('Grasso corporeo')
  })

  it('mostra la card % Grasso corporeo quando almeno una visita ha la plicometria', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockResolvedValue([
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: { percentualeGrassoCorporeo: 18.2, massaGrassaKg: 14.1, massaMagraKg: 63.4, fmi: 4.4, ffmi: 20.1 },
      },
    ])
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Grasso corporeo')
  })

  it('non mostra lo stato vuoto dell\'andamento finché lo storico visite è in caricamento', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    let risolviVisite: (visite: pazientiApi.Visita[]) => void = () => {}
    vi.mocked(pazientiApi.visite).mockReturnValue(
      new Promise<pazientiApi.Visita[]>((resolve) => {
        risolviVisite = resolve
      }),
    )
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, {
      global: { plugins: [router, createTestingPinia()], stubs: { ChartContainer: true } },
    })
    await flushPromises()

    // L'anagrafica c'è già, ma la sezione Andamento non deve mentire dicendo che non ci sono visite.
    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Andamento')
    expect(wrapper.text()).not.toContain('Nessuna visita registrata')
    expect(wrapper.findAll('[data-test="andamento-chart-skeleton"]').length).toBe(3)

    risolviVisite([])
    await flushPromises()

    // A caricamento finito, e solo allora, lo stato vuoto è quello vero.
    expect(wrapper.findAll('[data-test="andamento-chart-skeleton"]').length).toBe(0)
    expect(wrapper.text()).toContain('Nessuna visita registrata')
  })

  it('mostra un errore se lo storico visite non si carica, senza bloccare l\'anagrafica', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue({
      id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO', archiviato: false,
    })
    vi.mocked(pazientiApi.visite).mockRejectedValue(new Error('500'))
    const router = creaRouter()
    router.push('/pazienti/1')
    await router.isReady()
    const wrapper = mount(PazienteDettaglioView, { global: { plugins: [router, createTestingPinia()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).toContain('Non è stato possibile caricare lo storico delle visite.')
    expect(wrapper.text()).toContain('Non è stato possibile caricare i dati clinici del paziente.')
    expect(wrapper.text()).not.toContain('Peso attuale')
    expect(wrapper.text()).toContain('Non è stato possibile caricare l\'elenco delle visite.')
  })
})
