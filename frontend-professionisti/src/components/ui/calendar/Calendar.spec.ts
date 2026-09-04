import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { getLocalTimeZone, today } from '@internationalized/date'
import { Calendar } from '.'

describe('Calendar', () => {
  it('mostra il mese del valore già selezionato, non quello di oggi', () => {
    const oggi = today(getLocalTimeZone())
    const valoreSelezionato = oggi.subtract({ years: 2 })

    const wrapper = mount(Calendar, {
      props: { modelValue: valoreSelezionato },
    })

    const intestazione = wrapper.get('[data-slot="calendar-heading"]').text()
    expect(intestazione).toContain(String(valoreSelezionato.year))
    expect(intestazione).not.toContain(String(oggi.year))
  })
})
