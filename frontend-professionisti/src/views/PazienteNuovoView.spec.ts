import { describe, expect, it, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteNuovoView from './PazienteNuovoView.vue'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectTrigger } from '@/components/ui/select'
import * as pazientiApi from '@/api/pazienti'

vi.mock('@/api/pazienti')

function creaRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<div/>' } },
      { path: '/pazienti', name: 'pazienti', component: { template: '<div/>' } },
      { path: '/pazienti/nuovo', name: 'paziente-nuovo', component: PazienteNuovoView },
      { path: '/pazienti/:id', name: 'paziente-dettaglio', component: { template: '<div/>' } },
      { path: '/login', name: 'login', component: { template: '<div/>' } },
    ],
  })
}

function oggiIso(): string {
  return new Date().toISOString().slice(0, 10)
}

async function selezionaDataNascita(wrapper: ReturnType<typeof mount>, valore: string) {
  const dataNascitaPicker = wrapper
    .findAllComponents(DatePicker)
    .find((c) => c.props('id') === 'data-nascita')
  await dataNascitaPicker?.vm.$emit('update:modelValue', valore)
  await wrapper.vm.$nextTick()
}

async function selezionaSelect(wrapper: ReturnType<typeof mount>, triggerId: string, valore: string) {
  const select = wrapper.findAllComponents(Select).find((s) => s.findComponent(SelectTrigger).attributes('id') === triggerId)
  await select?.vm.$emit('update:modelValue', valore)
  await wrapper.vm.$nextTick()
}

describe('PazienteNuovoView', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('calcola e mostra l\'età in automatico quando si seleziona la data di nascita, come campo non modificabile', async () => {
    vi.setSystemTime(new Date('2026-09-01'))
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await selezionaDataNascita(wrapper, '2000-01-15')

    const etaInput = wrapper.find('#eta').element as HTMLInputElement
    expect(etaInput.value).toBe('26')
    expect(etaInput.disabled).toBe(true)
  })

  it('crea il paziente con i dati anagrafici e della visita, poi naviga al suo dettaglio', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '42', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await selezionaSelect(wrapper, 'sesso', 'M')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: '1990-05-20', sesso: 'M', lavoro: undefined, tipoLavoro: undefined,
      visita: {
        dataVisita: oggiIso(), altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: undefined, circonferenzaFianchiCm: undefined, circonferenzaAddomeCm: undefined,
        circonferenzaBraccioRilassatoCm: undefined, circonferenzaCosciaCm: undefined, circonferenzaPolpaccioCm: undefined,
        circonferenzaColloCm: undefined, circonferenzaToraceCm: undefined, circonferenzaBraccioContrattoCm: undefined,
        circonferenzaAvambraccioCm: undefined, circonferenzaCavigliaCm: undefined, protocolloVita: undefined,
      },
    })
    expect(router.currentRoute.value.path).toBe('/pazienti/42')
  })

  it('invia tutte le circonferenze della visita con i valori corretti nei rispettivi campi', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '43', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await selezionaSelect(wrapper, 'sesso', 'M')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await flushPromises()

    await wrapper.find('#circonferenza-vita').setValue('90,10')
    await wrapper.find('#circonferenza-fianchi').setValue('91,2')
    await wrapper.find('#circonferenza-addome').setValue('92,3')
    await wrapper.find('#circonferenza-braccio-rilassato').setValue('93,4')
    await wrapper.find('#circonferenza-coscia').setValue('94,5')
    await wrapper.find('#circonferenza-polpaccio').setValue('95,6')
    await wrapper.find('#circonferenza-collo').setValue('96,7')
    await wrapper.find('#circonferenza-torace').setValue('97,8')
    await wrapper.find('#circonferenza-braccio-contratto').setValue('98,9')
    await wrapper.find('#circonferenza-avambraccio').setValue('99,0')
    await wrapper.find('#circonferenza-caviglia').setValue('100,1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: '1990-05-20', sesso: 'M', lavoro: undefined, tipoLavoro: undefined,
      visita: {
        dataVisita: oggiIso(), altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: 90.1, circonferenzaFianchiCm: 91.2, circonferenzaAddomeCm: 92.3,
        circonferenzaBraccioRilassatoCm: 93.4, circonferenzaCosciaCm: 94.5, circonferenzaPolpaccioCm: 95.6,
        circonferenzaColloCm: 96.7, circonferenzaToraceCm: 97.8, circonferenzaBraccioContrattoCm: 98.9,
        circonferenzaAvambraccioCm: 99.0, circonferenzaCavigliaCm: 100.1, protocolloVita: undefined,
      },
    })
  })

  it('mostra un errore se la creazione fallisce', async () => {
    vi.mocked(pazientiApi.crea).mockRejectedValue(new Error('email duplicata'))
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await selezionaSelect(wrapper, 'sesso', 'M')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Non è stato possibile creare il paziente')
  })

  it('non invia la richiesta e mostra gli errori sotto i campi obbligatori vuoti', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Il nome è obbligatorio.')
    expect(wrapper.text()).toContain('Il cognome è obbligatorio.')
    expect(wrapper.text()).toContain("L'email è obbligatoria.")
    expect(wrapper.text()).toContain('La data di nascita è obbligatoria.')
    expect(wrapper.text()).toContain("L'altezza è obbligatoria.")
    expect(wrapper.text()).toContain('Il peso è obbligatorio.')
    expect(wrapper.text()).toContain('Il sesso è obbligatorio.')
  })

  it('non invia la richiesta se manca la data di nascita, e l\'errore sparisce non appena viene selezionata', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario')
    await wrapper.find('#cognome').setValue('Rossi')
    await wrapper.find('#email').setValue('mario@example.com')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('La data di nascita è obbligatoria.')

    await selezionaDataNascita(wrapper, '1990-05-20')

    expect(wrapper.text()).not.toContain('La data di nascita è obbligatoria.')
  })

  it('non invia la richiesta se manca il sesso, e l\'errore sparisce non appena viene selezionato', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario')
    await wrapper.find('#cognome').setValue('Rossi')
    await wrapper.find('#email').setValue('mario@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Il sesso è obbligatorio.')

    await selezionaSelect(wrapper, 'sesso', 'ALTRO')

    expect(wrapper.text()).not.toContain('Il sesso è obbligatorio.')
  })

  it('blocca a video i caratteri non ammessi mentre si digita (nome, telefono, peso)', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario1//asd')
    await wrapper.find('#telefono').setValue('1212121212121212121266//////asjkajskjaks')
    await wrapper.find('#peso').setValue('82abc,5xy')

    expect((wrapper.find('#nome').element as HTMLInputElement).value).toBe('Marioasd')
    expect((wrapper.find('#telefono').element as HTMLInputElement).value).toBe('1212121212')
    expect((wrapper.find('#peso').element as HTMLInputElement).value).toBe('82,5')
  })

  it('blocca a video un carattere non ammesso anche digitandolo un tasto alla volta', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    const nome = wrapper.find('#nome')
    const nomeEl = nome.element as HTMLInputElement

    nomeEl.value = '1'
    await nome.trigger('input')
    expect(nomeEl.value).toBe('')

    nomeEl.value = nomeEl.value + '2'
    await nome.trigger('input')
    expect(nomeEl.value).toBe('')

    nomeEl.value = nomeEl.value + 'M'
    await nome.trigger('input')
    expect(nomeEl.value).toBe('M')

    const peso = wrapper.find('#peso')
    const pesoEl = peso.element as HTMLInputElement
    pesoEl.value = 'x'
    await peso.trigger('input')
    expect(pesoEl.value).toBe('')
  })

  it('non invia la richiesta e mostra l\'errore se il telefono ha meno di 10 cifre', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario')
    await wrapper.find('#cognome').setValue('Rossi')
    await wrapper.find('#email').setValue('mario@example.com')
    await wrapper.find('#telefono').setValue('123')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Il telefono deve contenere 10 cifre numeriche.')
  })

  it('mette in maiuscolo la prima lettera di nome, cognome e lavoro mentre si digita', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('mario')
    await wrapper.find('#cognome').setValue('rossi')
    await wrapper.find('#lavoro').setValue('impiegato')

    expect((wrapper.find('#nome').element as HTMLInputElement).value).toBe('Mario')
    expect((wrapper.find('#cognome').element as HTMLInputElement).value).toBe('Rossi')
    expect((wrapper.find('#lavoro').element as HTMLInputElement).value).toBe('Impiegato')
  })

  it('fa sparire l\'errore di un campo obbligatorio non appena viene compilato correttamente', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Il nome è obbligatorio.')

    await wrapper.find('#nome').setValue('Mario')

    expect(wrapper.text()).not.toContain('Il nome è obbligatorio.')
  })

  it('include la plicometria nel payload quando protocollo e pliche sono compilati', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '44', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await selezionaDataNascita(wrapper, '1990-05-20')
    await selezionaSelect(wrapper, 'sesso', 'M')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    const accordionPlicometria = wrapper.findAll('button').find((b) => b.text().includes('Plicometria'))
    await accordionPlicometria?.trigger('click')
    await flushPromises()

    await selezionaSelect(wrapper, 'protocollo-plico', 'FAULKNER_4')
    await wrapper.find('#plica-tricipitale').setValue('10,00')
    await wrapper.find('#plica-sottoscapolare').setValue('10,00')
    await wrapper.find('#plica-soprailiaca').setValue('10,00')
    await wrapper.find('#plica-addominale').setValue('10,00')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalled()
    const richiesta = vi.mocked(pazientiApi.crea).mock.calls[0][0]
    expect(richiesta.visita.plicometria).toMatchObject({
      protocollo: 'FAULKNER_4',
      plicaTricipitaleMm: 10,
      plicaSottoscapolareMm: 10,
      plicaSoprailiacaMm: 10,
      plicaAddominaleMm: 10,
    })
  })

  it('fa sparire l\'errore di formato di un campo non appena viene corretto', async () => {
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Mario')
    await wrapper.find('#cognome').setValue('Rossi')
    await wrapper.find('#email').setValue('mario@example.com')
    await wrapper.find('#telefono').setValue('123')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Il telefono deve contenere 10 cifre numeriche.')

    await wrapper.find('#telefono').setValue('1234567890')

    expect(wrapper.text()).not.toContain('Il telefono deve contenere 10 cifre numeriche.')
  })
})
