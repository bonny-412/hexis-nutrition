import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TimePicker from './TimePicker.vue'

// Bug 01 del collaudo (27/11/2026): un orario non valido cancellava quello salvato senza avviso.
describe('TimePicker', () => {
  it('un orario inesistente lascia il modello vuoto ma segnala il testo non valido', async () => {
    const wrapper = mount(TimePicker, { props: { modelValue: '17:42' } })

    await wrapper.find('input').setValue('2599')

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([''])
    expect(wrapper.emitted('update:nonValido')?.at(-1)).toEqual([true])
  })

  it('un orario valido o il campo svuotato non sono "non validi"', async () => {
    const wrapper = mount(TimePicker, { props: { modelValue: '' } })

    await wrapper.find('input').setValue('0930')
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['09:30'])
    // Resta false: defineModel non emette se il valore non cambia.
    expect(wrapper.emitted('update:nonValido')).toBeUndefined()

    await wrapper.find('input').setValue('')
    expect(wrapper.emitted('update:nonValido')).toBeUndefined()
  })

  it('correggendo un orario non valido il segnale torna falso', async () => {
    const wrapper = mount(TimePicker, { props: { modelValue: '' } })

    await wrapper.find('input').setValue('2599')
    await wrapper.find('input').setValue('1742')

    expect(wrapper.emitted('update:nonValido')?.at(-1)).toEqual([false])
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['17:42'])
  })
})
