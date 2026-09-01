import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PlicometriaForm from './PlicometriaForm.vue'
import { Select, SelectTrigger } from '@/components/ui/select'

interface PlicometriaFormExposed {
  valida(): boolean
  ottieniDati(): Record<string, unknown> | undefined
}

function esposti(wrapper: ReturnType<typeof mount>): PlicometriaFormExposed {
  return wrapper.vm as unknown as PlicometriaFormExposed
}

async function selezionaSelect(wrapper: ReturnType<typeof mount>, triggerId: string, valore: string) {
  const select = wrapper.findAllComponents(Select).find((s) => s.findComponent(SelectTrigger).attributes('id') === triggerId)
  await select?.vm.$emit('update:modelValue', valore)
  await wrapper.vm.$nextTick()
}

describe('PlicometriaForm', () => {
  it('è disabilitato per sesso ALTRO: valida() torna true e ottieniDati() torna undefined', () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'ALTRO' } })

    expect(wrapper.text()).toContain('Non disponibile per sesso "Altro"')
    expect(esposti(wrapper).valida()).toBe(true)
    expect(esposti(wrapper).ottieniDati()).toBeUndefined()
  })

  it('senza protocollo selezionato, ottieniDati() torna undefined', () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    expect(esposti(wrapper).ottieniDati()).toBeUndefined()
  })

  it('Jackson-Pollock 3 mostra pettorale/addominale/coscia per sesso M', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'JACKSON_POLLOCK_3')

    expect(wrapper.find('#plica-pettorale').exists()).toBe(true)
    expect(wrapper.find('#plica-addominale').exists()).toBe(true)
    expect(wrapper.find('#plica-coscia').exists()).toBe(true)
    expect(wrapper.find('#plica-tricipitale').exists()).toBe(false)
  })

  it('Jackson-Pollock 3 mostra tricipitale/soprailiaca/coscia per sesso F', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'F' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'JACKSON_POLLOCK_3')

    expect(wrapper.find('#plica-tricipitale').exists()).toBe(true)
    expect(wrapper.find('#plica-soprailiaca').exists()).toBe(true)
    expect(wrapper.find('#plica-coscia').exists()).toBe(true)
    expect(wrapper.find('#plica-pettorale').exists()).toBe(false)
  })

  it('Evans mostra anche il campo etnia', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'EVANS_ATLETI')

    expect(wrapper.find('#etnia-atleta').exists()).toBe(true)
  })

  it('valida() fallisce e mostra un errore se manca una plica obbligatoria', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'FAULKNER_4')
    await wrapper.find('#plica-tricipitale').setValue('10,00')

    const valido = esposti(wrapper).valida()
    await wrapper.vm.$nextTick()

    expect(valido).toBe(false)
    expect(wrapper.text()).toContain('Questa plica è obbligatoria per il protocollo scelto.')
  })

  it('ottieniDati() restituisce protocollo, etnia e pliche compilate', async () => {
    const wrapper = mount(PlicometriaForm, { props: { sesso: 'M' } })

    await selezionaSelect(wrapper, 'protocollo-plico', 'EVANS_ATLETI')
    await selezionaSelect(wrapper, 'etnia-atleta', 'AFROAMERICANO')
    await wrapper.find('#plica-tricipitale').setValue('10,00')
    await wrapper.find('#plica-addominale').setValue('10,00')
    await wrapper.find('#plica-coscia').setValue('10,00')

    expect(esposti(wrapper).ottieniDati()).toMatchObject({
      protocollo: 'EVANS_ATLETI',
      etniaAtleta: 'AFROAMERICANO',
      plicaTricipitaleMm: 10,
      plicaAddominaleMm: 10,
      plicaCosciaMm: 10,
    })
  })
})
