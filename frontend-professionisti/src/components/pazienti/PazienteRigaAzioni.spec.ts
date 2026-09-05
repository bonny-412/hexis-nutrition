import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteRigaAzioni from './PazienteRigaAzioni.vue'
import type { Paziente } from '@/api/pazienti'

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/pazienti/:id/visite/nuova', name: 'visita-nuova', component: { template: '<div/>' } },
    ],
  })
}

const pazienteEsempio: Paziente = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, stileDiVita: null, note: null,
  statoAccount: 'MAI_INVITATO', archiviato: false,
  obiettivoUltimaVisita: null, dataUltimaVisita: null,
}

async function montaComponente(paziente: Paziente, mostraArchiviati = false) {
  const router = creaRouter()
  router.push('/')
  await router.isReady()
  return mount(PazienteRigaAzioni, {
    props: { paziente, mostraArchiviati },
    attachTo: document.body,
    global: { plugins: [router] },
  })
}

async function apriMenu(wrapper: Awaited<ReturnType<typeof montaComponente>>) {
  await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
  await flushPromises()
}

describe('PazienteRigaAzioni', () => {
  it('mostra il bottone Invita per un paziente mai invitato', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    expect(wrapper.text()).toContain('Invita')
    wrapper.unmount()
  })

  it('emette invita al click del bottone', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    const pulsante = wrapper.findAll('button').find((b) => b.text() === 'Invita')
    await pulsante?.trigger('click')
    expect(wrapper.emitted('invita')).toEqual([[pazienteEsempio]])
    wrapper.unmount()
  })

  it('non mostra il bottone Invita in vista archiviati', async () => {
    const wrapper = await montaComponente({ ...pazienteEsempio, archiviato: true }, true)
    expect(wrapper.text()).not.toContain('Invita')
    wrapper.unmount()
  })

  it('apre il menu e collega "Nuova visita" alla pagina di creazione per il paziente', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)

    const link = document.querySelector<HTMLAnchorElement>('[data-test="menu-nuova-visita"]')
    expect(link?.textContent).toContain('Nuova visita')
    expect(link?.getAttribute('href')).toBe('/pazienti/1/visite/nuova')
    wrapper.unmount()
  })

  it('chiede conferma prima di archiviare e non emette nulla finché non si conferma', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('archivia')).toBeUndefined()
    expect(document.body.textContent).toContain('Archiviare Luca Verdi?')
    wrapper.unmount()
  })

  it('emette archivia dopo la conferma', async () => {
    const wrapper = await montaComponente(pazienteEsempio)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('archivia')).toEqual([[pazienteEsempio]])
    wrapper.unmount()
  })

  it('mostra "De-archivia paziente" ed emette deArchivia in vista archiviati', async () => {
    const pazienteArchiviato = { ...pazienteEsempio, archiviato: true }
    const wrapper = await montaComponente(pazienteArchiviato, true)
    await apriMenu(wrapper)
    document.querySelector<HTMLElement>('[data-test="menu-de-archivia"]')?.click()
    await flushPromises()
    document.querySelector<HTMLElement>('[data-test="conferma-conferma"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('deArchivia')).toEqual([[pazienteArchiviato]])
    wrapper.unmount()
  })
})
