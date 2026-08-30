import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import App from './App.vue'

describe('App', () => {
  it('monta senza errori', () => {
    const wrapper = mount(App)
    expect(wrapper.text()).toContain('Hexis Nutrition')
  })
})
