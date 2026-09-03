import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import { toast } from 'vue-sonner'
import VisitaFormView from './VisitaFormView.vue'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'
import DatiVisitaForm from '@/components/pazienti/DatiVisitaForm.vue'
import * as pazientiApi from '@/api/pazienti'
import { ApiError } from '@/api/client'
import type { Paziente, Visita } from '@/api/pazienti'

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
      { path: '/pazienti/visite/nuova', name: 'visita-nuova-senza-paziente', component: VisitaFormView },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/pazienti/:id/visite/nuova', name: 'visita-nuova', component: VisitaFormView },
      { path: '/pazienti/:id/visite/:visitaId/modifica', name: 'visita-modifica', component: VisitaFormView },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

function pazienteEsempio(overrides: Partial<Paziente> = {}): Paziente {
  return {
    id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
    telefono: null, dataNascita: '1990-01-01', sesso: 'M', lavoro: null, tipoLavoro: null, note: null,
    statoAccount: 'MAI_INVITATO', archiviato: false,
    ...overrides,
  }
}

function visitaEsempio(overrides: Partial<Visita> = {}): Visita {
  return {
    id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
    circonferenze: {
      vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
      polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
    },
    protocolloVita: 'OMS',
    note: null,
    obiettivo: 'MANTENIMENTO',
    plicometria: null,
    ...overrides,
  }
}

async function monta(path: string) {
  const router = creaRouter()
  router.push(path)
  await router.isReady()
  const wrapper = mount(VisitaFormView, { global: { plugins: [router, createTestingPinia()] } })
  await flushPromises()
  return { wrapper, router }
}

describe('VisitaFormView', () => {
  it('con paziente noto dalla route mostra titolo "Nuova visita" e sottotitolo senza ultima visita se non ce ne sono', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([])

    const { wrapper } = await monta('/pazienti/1/visite/nuova')

    expect(wrapper.text()).toContain('Nuova visita')
    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.text()).not.toContain('ultima visita')
  })

  it('mostra "ultima visita" nel sottotitolo quando il paziente ne ha già una', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([visitaEsempio({ dataVisita: '2026-06-01', pesoKg: 82.5 })])

    const { wrapper } = await monta('/pazienti/1/visite/nuova')

    expect(wrapper.text()).toContain('ultima visita 01 giu 2026 con peso 82,5 kg')
  })

  it('in creazione precompila altezza e obiettivo con i valori dell\'ultima visita', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([visitaEsempio({ altezzaCm: 185, obiettivo: 'IPERTROFIA' })])

    const { wrapper } = await monta('/pazienti/1/visite/nuova')

    expect((wrapper.find('#altezza').element as HTMLInputElement).value).toBe('185')
    const form = wrapper.findComponent(DatiVisitaForm).vm as unknown as { ottieniDati(): Record<string, unknown> }
    expect(form.ottieniDati().obiettivo).toBe('IPERTROFIA')
  })

  it('in modalità modifica mostra "Modifica visita" e precompila il form con la visita esistente', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
    vi.mocked(pazientiApi.dettaglioVisita).mockResolvedValue(visitaEsempio({ altezzaCm: 180, pesoKg: 90 }))

    const { wrapper } = await monta('/pazienti/1/visite/v1/modifica')

    expect(pazientiApi.dettaglioVisita).toHaveBeenCalledWith('1', 'v1')
    expect(wrapper.text()).toContain('Modifica visita')
    expect((wrapper.find('#altezza').element as HTMLInputElement).value).toBe('180')
  })

  it('senza paziente nella route mostra il selettore; dopo la selezione mostra il form', async () => {
    vi.mocked(pazientiApi.visite).mockResolvedValue([])

    const { wrapper } = await monta('/pazienti/visite/nuova')

    expect(wrapper.find('[data-test="input-cerca-paziente"]').exists()).toBe(true)
    expect(wrapper.find('#altezza').exists()).toBe(false)

    await wrapper.findComponent(SelezionaPazienteCombobox).vm.$emit('update:modelValue', pazienteEsempio())
    await flushPromises()

    expect(wrapper.find('#altezza').exists()).toBe(true)
    expect(wrapper.text()).toContain('Luca Verdi')
  })

  it('selezionando un paziente dal combobox, precompila altezza suggerita solo dopo il caricamento dell\'ultima visita (nessuna race condition)', async () => {
    vi.mocked(pazientiApi.visite).mockResolvedValue([visitaEsempio({ altezzaCm: 172 })])

    const { wrapper } = await monta('/pazienti/visite/nuova')

    await wrapper.findComponent(SelezionaPazienteCombobox).vm.$emit('update:modelValue', pazienteEsempio())
    await flushPromises()

    expect((wrapper.find('#altezza').element as HTMLInputElement).value).toBe('172')
  })

  it('invia la nuova visita e naviga al dettaglio del paziente', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
    vi.mocked(pazientiApi.creaVisita).mockResolvedValue(visitaEsempio())

    const { wrapper, router } = await monta('/pazienti/1/visite/nuova')

    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.creaVisita).toHaveBeenCalledWith('1', expect.objectContaining({ altezzaCm: 178, pesoKg: 82.5 }))
    expect(toast.success).toHaveBeenCalled()
    expect(router.currentRoute.value.fullPath).toBe('/pazienti/1')
  })

  it('aggiorna la visita esistente e naviga al dettaglio del paziente', async () => {
    vi.mocked(pazientiApi.dettaglio).mockResolvedValue(pazienteEsempio())
    vi.mocked(pazientiApi.visite).mockResolvedValue([])
    vi.mocked(pazientiApi.dettaglioVisita).mockResolvedValue(visitaEsempio())
    vi.mocked(pazientiApi.aggiornaVisita).mockResolvedValue(visitaEsempio())

    const { wrapper, router } = await monta('/pazienti/1/visite/v1/modifica')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.aggiornaVisita).toHaveBeenCalledWith('1', 'v1', expect.any(Object))
    expect(router.currentRoute.value.fullPath).toBe('/pazienti/1')
  })

  it('mostra un errore se il paziente non viene trovato', async () => {
    vi.mocked(pazientiApi.dettaglio).mockRejectedValue(new ApiError(404, 'non trovato'))

    const { wrapper } = await monta('/pazienti/1/visite/nuova')

    expect(wrapper.text()).toContain('Paziente non trovato.')
  })
})
