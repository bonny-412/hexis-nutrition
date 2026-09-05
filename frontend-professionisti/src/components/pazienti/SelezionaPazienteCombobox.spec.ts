import { describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import SelezionaPazienteCombobox from './SelezionaPazienteCombobox.vue'
import { cerca, type Paziente } from '@/api/pazienti'

vi.mock('@/api/pazienti', () => ({ cerca: vi.fn() }))

const pazienteEsempio: Paziente = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, stileDiVita: null, note: null, statoAccount: 'MAI_INVITATO',
  archiviato: false,
  obiettivoUltimaVisita: null, dataUltimaVisita: null,
}

describe('SelezionaPazienteCombobox', () => {
  it('senza paziente selezionato mostra il campo di ricerca', () => {
    const wrapper = mount(SelezionaPazienteCombobox, { props: { modelValue: null } })

    expect(wrapper.find('[data-test="input-cerca-paziente"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('digitando cerca il paziente (con debounce) e mostra i risultati', async () => {
    vi.useFakeTimers()
    vi.mocked(cerca).mockResolvedValue({ contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 8, totaleElementi: 1, totalePagine: 1 })
    const wrapper = mount(SelezionaPazienteCombobox, { props: { modelValue: null }, attachTo: document.body })

    await wrapper.find('[data-test="input-cerca-paziente"]').setValue('Luca')
    vi.advanceTimersByTime(300)
    vi.useRealTimers()
    await flushPromises()

    expect(cerca).toHaveBeenCalledWith({ ricerca: 'Luca', dimensione: 8 })
    expect(document.body.textContent).toContain('Luca Verdi')
    wrapper.unmount()
  })

  it('selezionando un risultato emette update:modelValue con il paziente scelto', async () => {
    vi.useFakeTimers()
    vi.mocked(cerca).mockResolvedValue({ contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 8, totaleElementi: 1, totalePagine: 1 })
    const wrapper = mount(SelezionaPazienteCombobox, { props: { modelValue: null }, attachTo: document.body })

    await wrapper.find('[data-test="input-cerca-paziente"]').setValue('Luca')
    vi.advanceTimersByTime(300)
    vi.useRealTimers()
    await flushPromises()

    document.querySelector<HTMLElement>('[data-test="risultato-paziente"]')?.click()
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')).toEqual([[pazienteEsempio]])
    wrapper.unmount()
  })

  it('con paziente selezionato mostra il riepilogo invece del campo di ricerca', () => {
    const wrapper = mount(SelezionaPazienteCombobox, { props: { modelValue: pazienteEsempio } })

    expect(wrapper.text()).toContain('Luca Verdi')
    expect(wrapper.find('[data-test="input-cerca-paziente"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('"Cambia paziente" emette update:modelValue con null', async () => {
    const wrapper = mount(SelezionaPazienteCombobox, { props: { modelValue: pazienteEsempio } })

    await wrapper.find('button').trigger('click')

    expect(wrapper.emitted('update:modelValue')).toEqual([[null]])
    wrapper.unmount()
  })
})
