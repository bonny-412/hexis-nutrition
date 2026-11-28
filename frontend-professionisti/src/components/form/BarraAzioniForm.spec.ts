import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BarraAzioniForm from './BarraAzioniForm.vue'

// Opzioni aggiunte il 28/11/2026 per il piano alimentare ("Salvato alle", solo "Chiudi" senza modifiche).
// Visita e nuovo paziente non le usano: la barra di default deve restare com'era.

const base = { inCorso: false, etichettaSalva: 'Salva', tipoSalva: 'button' as const }

describe('BarraAzioniForm', () => {
  it('di default mostra sempre "Annulla" e "Salva", anche senza modifiche', () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: false } })

    expect(wrapper.find('[data-test="btn-annulla"]').text()).toBe('Annulla')
    expect(wrapper.find('[data-test="btn-salva"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="salvato-alle"]').exists()).toBe(false)
  })

  it('con le modifiche in sospeso dice "Modifiche non salvate" e non l\'ora dell\'ultimo salvataggio', () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: true, ultimoSalvataggio: 'Salvato alle 11:42' } })

    expect(wrapper.text()).toContain('Modifiche non salvate')
    expect(wrapper.find('[data-test="salvato-alle"]').exists()).toBe(false)
  })

  it('senza modifiche mostra la frase dell\'ultimo salvataggio', () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: false, ultimoSalvataggio: 'Salvato alle 11:42' } })

    expect(wrapper.find('[data-test="salvato-alle"]').text()).toBe('Salvato alle 11:42')
  })

  it('con soloChiudiSeNonModificato e niente da salvare mostra solo "Chiudi"', () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: false, soloChiudiSeNonModificato: true } })

    expect(wrapper.find('[data-test="btn-annulla"]').text()).toBe('Chiudi')
    expect(wrapper.find('[data-test="btn-salva"]').exists()).toBe(false)
  })

  it('con soloChiudiSeNonModificato e modifiche in sospeso torna a "Annulla" e "Salva"', () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: true, soloChiudiSeNonModificato: true } })

    expect(wrapper.find('[data-test="btn-annulla"]').text()).toBe('Annulla')
    expect(wrapper.find('[data-test="btn-salva"]').exists()).toBe(true)
  })

  it('"Chiudi" emette annulla, come "Annulla"', async () => {
    const wrapper = mount(BarraAzioniForm, { props: { ...base, modificato: false, soloChiudiSeNonModificato: true } })

    await wrapper.find('[data-test="btn-annulla"]').trigger('click')

    expect(wrapper.emitted('annulla')).toHaveLength(1)
  })
})
