import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AlimentoFormDialog from './AlimentoFormDialog.vue'
import type { Alimento } from '@/api/alimenti'

vi.mock('@/api/alimenti')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

const alimentoBda: Alimento = {
  id: '1', nome: 'Petto di pollo, crudo', categoria: 'Carni', kcal: 165, proteineG: 31, grassiG: 3.6,
  carboidratiG: 0, acquaG: 65, fibreG: null, zuccheriG: null, ferroMg: 1, calcioMg: 15, sodioMg: 74, bda: true,
}

const alimentoPersonalizzato: Alimento = { ...alimentoBda, id: '2', nome: 'Frullato proteico', bda: false }

async function montaDialog(alimento: Alimento | null) {
  const wrapper = mount(AlimentoFormDialog, {
    props: { open: false, alimento },
    attachTo: document.body,
  })
  await wrapper.setProps({ open: true })
  await flushPromises()
  return wrapper
}

function testiBottoni(): string[] {
  return Array.from(document.querySelectorAll('button')).map((b) => b.textContent?.trim() ?? '')
}

afterEach(() => {
  document.body.innerHTML = ''
})

describe('AlimentoFormDialog', () => {
  it('un alimento BDA mostra i campi disabilitati, "Duplica come personalizzato" e nasconde Salva/Elimina', async () => {
    await montaDialog(alimentoBda)

    const input = document.querySelectorAll('input')
    expect(input.length).toBeGreaterThan(0)
    input.forEach((el) => expect((el as HTMLInputElement).disabled).toBe(true))

    const bottoni = testiBottoni()
    expect(bottoni.some((t) => t.includes('Duplica come personalizzato'))).toBe(true)
    expect(bottoni.some((t) => t.includes('Salva'))).toBe(false)
    expect(bottoni.some((t) => t.includes('Elimina'))).toBe(false)
  })

  it('un alimento personalizzato mostra i campi abilitati, "Salva" ed "Elimina", nasconde Duplica', async () => {
    await montaDialog(alimentoPersonalizzato)

    const input = document.querySelectorAll('input')
    expect(input.length).toBeGreaterThan(0)
    input.forEach((el) => expect((el as HTMLInputElement).disabled).toBe(false))

    const bottoni = testiBottoni()
    expect(bottoni.some((t) => t.includes('Salva'))).toBe(true)
    expect(bottoni.some((t) => t.includes('Elimina'))).toBe(true)
    expect(bottoni.some((t) => t.includes('Duplica'))).toBe(false)
  })

  it('la modalità creazione (alimento nullo) mostra "Salva" e nasconde Elimina/Duplica', async () => {
    await montaDialog(null)

    const bottoni = testiBottoni()
    expect(bottoni.some((t) => t.includes('Salva'))).toBe(true)
    expect(bottoni.some((t) => t.includes('Elimina'))).toBe(false)
    expect(bottoni.some((t) => t.includes('Duplica'))).toBe(false)
  })
})
