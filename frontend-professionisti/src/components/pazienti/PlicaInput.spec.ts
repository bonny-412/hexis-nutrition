import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PlicaInput from './PlicaInput.vue'
import { Checkbox } from '@/components/ui/checkbox'

async function attivaTripla(wrapper: ReturnType<typeof mount>, attiva: boolean) {
  await wrapper.findComponent(Checkbox).vm.$emit('update:modelValue', attiva)
  await wrapper.vm.$nextTick()
}

describe('PlicaInput', () => {
  it('in modalità singola emette il valore filtrato al variare dell\'input', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await wrapper.find('#plica-test').setValue('12,50')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['12,50'])
  })

  it('attivando la tripla misurazione mostra 3 campi e calcola la media', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await attivaTripla(wrapper, true)
    await wrapper.find('#plica-test-m1').setValue('12,5')
    await wrapper.find('#plica-test-m2').setValue('13,0')
    await wrapper.find('#plica-test-m3').setValue('12,2')

    expect(wrapper.text()).toContain('Media calcolata: 12,57 mm')
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['12,57'])
  })

  it('disattivando la tripla misurazione svuota le sotto-misurazioni', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await attivaTripla(wrapper, true)
    await wrapper.find('#plica-test-m1').setValue('12,5')
    await attivaTripla(wrapper, false)
    await attivaTripla(wrapper, true)

    expect((wrapper.find('#plica-test-m1').element as HTMLInputElement).value).toBe('')
  })

  it('svuota il valore emesso se una misura viene cancellata dopo aver completato la tripla misurazione', async () => {
    const wrapper = mount(PlicaInput, { props: { id: 'plica-test', label: 'Plica test', modelValue: '' } })

    await attivaTripla(wrapper, true)
    await wrapper.find('#plica-test-m1').setValue('12,5')
    await wrapper.find('#plica-test-m2').setValue('13,0')
    await wrapper.find('#plica-test-m3').setValue('12,2')
    await wrapper.find('#plica-test-m3').setValue('')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([''])
  })

  it('evidenzia di rosso le tre misurazioni quando è presente un errore', async () => {
    const wrapper = mount(PlicaInput, {
      props: { id: 'plica-test', label: 'Plica test', modelValue: '', errore: 'Questa plica è obbligatoria per il protocollo scelto.' },
    })

    await attivaTripla(wrapper, true)

    expect(wrapper.find('#plica-test-m1').attributes('aria-invalid')).toBe('true')
    expect(wrapper.find('#plica-test-m2').attributes('aria-invalid')).toBe('true')
    expect(wrapper.find('#plica-test-m3').attributes('aria-invalid')).toBe('true')
  })
})
