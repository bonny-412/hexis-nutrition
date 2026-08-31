import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createRouter, createMemoryHistory } from 'vue-router'
import PazienteNuovoView from './PazienteNuovoView.vue'
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

describe('PazienteNuovoView', () => {
  it('crea il paziente con i dati anagrafici e della visita, poi naviga al suo dettaglio', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '42', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: undefined, sesso: undefined, lavoro: undefined, tipoLavoro: undefined,
      visita: {
        dataVisita: oggiIso(), altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: undefined, circonferenzaOmbelicoCm: undefined, circonferenzaFianchiCm: undefined,
        circonferenzaPettoCm: undefined, circonferenzaCosciaDxCm: undefined, circonferenzaCosciaSxCm: undefined,
        circonferenzaPolpaccioDxCm: undefined, circonferenzaPolpaccioSxCm: undefined,
        larghezzaSpalleCm: undefined, circonferenzaSpalleCm: undefined,
        circonferenzaBicipiteDxCm: undefined, circonferenzaBicipiteSxCm: undefined,
      },
    })
    expect(router.currentRoute.value.path).toBe('/pazienti/42')
  })

  it('invia tutte le 14 misurazioni della visita con i valori corretti nei rispettivi campi', async () => {
    vi.mocked(pazientiApi.crea).mockResolvedValue({
      id: '43', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: null, dataNascita: null, sesso: null, lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
    })
    const router = creaRouter()
    router.push('/pazienti/nuovo')
    await router.isReady()
    const wrapper = mount(PazienteNuovoView, { global: { plugins: [router, createTestingPinia()] } })

    await wrapper.find('#nome').setValue('Luca')
    await wrapper.find('#cognome').setValue('Verdi')
    await wrapper.find('#email').setValue('luca@example.com')
    await wrapper.find('#altezza').setValue('178')
    await wrapper.find('#peso').setValue('82,5')

    const accordionCirconferenze = wrapper.findAll('button').find((b) => b.text().includes('Circonferenze'))
    await accordionCirconferenze?.trigger('click')
    await flushPromises()

    await wrapper.find('#circonferenza-vita').setValue('90,10')
    await wrapper.find('#circonferenza-ombelico').setValue('91,2')
    await wrapper.find('#circonferenza-fianchi').setValue('92,3')
    await wrapper.find('#circonferenza-petto').setValue('93,4')
    await wrapper.find('#circonferenza-coscia-dx').setValue('94,5')
    await wrapper.find('#circonferenza-coscia-sx').setValue('95,6')
    await wrapper.find('#circonferenza-polpaccio-dx').setValue('96,7')
    await wrapper.find('#circonferenza-polpaccio-sx').setValue('97,8')
    await wrapper.find('#larghezza-spalle').setValue('98,9')
    await wrapper.find('#circonferenza-spalle').setValue('99,0')
    await wrapper.find('#circonferenza-bicipite-dx').setValue('100,1')
    await wrapper.find('#circonferenza-bicipite-sx').setValue('101,2')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(pazientiApi.crea).toHaveBeenCalledWith({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
      telefono: undefined, dataNascita: undefined, sesso: undefined, lavoro: undefined, tipoLavoro: undefined,
      visita: {
        dataVisita: oggiIso(), altezzaCm: 178, pesoKg: 82.5,
        circonferenzaVitaCm: 90.1, circonferenzaOmbelicoCm: 91.2, circonferenzaFianchiCm: 92.3,
        circonferenzaPettoCm: 93.4, circonferenzaCosciaDxCm: 94.5, circonferenzaCosciaSxCm: 95.6,
        circonferenzaPolpaccioDxCm: 96.7, circonferenzaPolpaccioSxCm: 97.8,
        larghezzaSpalleCm: 98.9, circonferenzaSpalleCm: 99.0,
        circonferenzaBicipiteDxCm: 100.1, circonferenzaBicipiteSxCm: 101.2,
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
    expect(wrapper.text()).toContain("L'altezza è obbligatoria.")
    expect(wrapper.text()).toContain('Il peso è obbligatorio.')
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
