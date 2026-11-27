import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import AgendaView from './AgendaView.vue'
import AgendaGiorno from '@/components/agenda/AgendaGiorno.vue'
import * as agendaApi from '@/api/agenda'
import type { Appuntamento } from '@/api/agenda'

vi.mock('@/api/agenda')

const GIORNO = '2026-11-27'

function appuntamento(parziale: Partial<Appuntamento>): Appuntamento {
  return {
    id: 'id', tipo: 'BLOCCO', stato: null, data: GIORNO, oraInizio: '19:00', durataMinuti: null,
    dataFine: GIORNO, oraFine: '20:00', giornataIntera: false,
    pazienteId: null, pazienteNome: null, pazienteCognome: null, testo: 'Dentista', linkVideo: null,
    fatto: false, visitaId: null, notifica: false, anticipoNotificaMinuti: 45,
    ...parziale,
  }
}

const blocco = appuntamento({ id: 'blocco-1' })
const visita = appuntamento({
  id: 'visita-1', tipo: 'VISITA_STUDIO', stato: 'PROGRAMMATA', oraInizio: '10:00', durataMinuti: 30,
  dataFine: null, oraFine: null, pazienteId: 'paz-1', pazienteNome: 'Mario', pazienteCognome: 'Rossi', testo: null,
})

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/agenda', name: 'agenda', component: AgendaView },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/alimenti', name: 'alimenti', component: { template: '<div/>' } },
      { path: '/piani-alimentari', name: 'piani-alimentari', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

async function montaAgenda() {
  const router = creaRouter()
  await router.push(`/agenda?data=${GIORNO}&vista=giorno`)
  await router.isReady()
  const wrapper = mount(AgendaView, { attachTo: document.body, global: { plugins: [router, createTestingPinia()] } })
  await flushPromises()
  return wrapper
}

function clicca(testo: string) {
  const pulsante = Array.from(document.querySelectorAll('button')).find((b) => b.textContent?.trim() === testo)
  expect(pulsante, `pulsante "${testo}"`).toBeDefined()
  pulsante?.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
}

beforeEach(() => {
  vi.mocked(agendaApi.elenco).mockResolvedValue([blocco, visita])
  vi.mocked(agendaApi.elimina).mockResolvedValue()
  vi.mocked(agendaApi.annulla).mockResolvedValue()
})

afterEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
})

// Regressione: AlertDialogAction chiude il dialog (v-model:open = false) prima di eseguire il @click.
// Se la chiusura azzera l'elemento scelto, la conferma non chiama mai il backend.
describe('AgendaView — conferme', () => {
  it('conferma l\'eliminazione di una fascia bloccata e chiama il backend con il suo id', async () => {
    const wrapper = await montaAgenda()

    wrapper.getComponent(AgendaGiorno).vm.$emit('azione', blocco, 'ELIMINA')
    await flushPromises()
    clicca('Elimina fascia bloccata')
    await flushPromises()

    expect(agendaApi.elimina).toHaveBeenCalledWith('blocco-1')
    wrapper.unmount()
  })

  it('conferma l\'annullamento di una visita e chiama il backend con il suo id', async () => {
    const wrapper = await montaAgenda()

    wrapper.getComponent(AgendaGiorno).vm.$emit('azione', visita, 'ANNULLA')
    await flushPromises()
    clicca('Annulla appuntamento')
    await flushPromises()

    expect(agendaApi.annulla).toHaveBeenCalledWith('visita-1')
    wrapper.unmount()
  })
})
