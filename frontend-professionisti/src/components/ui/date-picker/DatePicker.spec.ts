import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DatePicker from './DatePicker.vue'

function popover(wrapper: ReturnType<typeof mount>) {
  return wrapper.findComponent({ name: 'Popover' })
}

describe('DatePicker', () => {
  it('parte chiuso e si apre quando il popover lo richiede', async () => {
    const wrapper = mount(DatePicker, { props: { modelValue: '' } })

    expect(popover(wrapper).props('open')).toBe(false)

    await popover(wrapper).vm.$emit('update:open', true)

    expect(popover(wrapper).props('open')).toBe(true)
  })

  it('chiude il popover quando si seleziona una data', async () => {
    const wrapper = mount(DatePicker, { props: { modelValue: '2026-01-01' } })
    await popover(wrapper).vm.$emit('update:open', true)
    expect(popover(wrapper).props('open')).toBe(true)

    await wrapper.setProps({ modelValue: '2026-01-15' })

    expect(popover(wrapper).props('open')).toBe(false)
  })

  it('non chiude il popover se il valore non cambia davvero', async () => {
    const wrapper = mount(DatePicker, { props: { modelValue: '2026-01-01' } })
    await popover(wrapper).vm.$emit('update:open', true)

    await wrapper.setProps({ modelValue: '2026-01-01' })

    expect(popover(wrapper).props('open')).toBe(true)
  })
})
