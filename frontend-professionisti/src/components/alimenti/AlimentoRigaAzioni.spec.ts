import { afterEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AlimentoRigaAzioni from './AlimentoRigaAzioni.vue'
import type { Alimento } from '@/api/alimenti'

const alimentoBda: Alimento = {
  id: '1', nome: 'Petto di pollo, crudo', categoria: 'Carni', quantitaG: 100, kcal: 165, proteineG: 31, grassiG: 3.6,
  carboidratiG: 0, acquaG: 65, fibreG: null, zuccheriG: null, ferroMg: 1, calcioMg: 15, sodioMg: 74, bda: true,
}

const alimentoPersonalizzato: Alimento = { ...alimentoBda, id: '2', nome: 'Frullato proteico', bda: false }

async function montaComponente(alimento: Alimento) {
  return mount(AlimentoRigaAzioni, {
    props: { alimento },
    attachTo: document.body,
  })
}

async function apriMenu(wrapper: Awaited<ReturnType<typeof montaComponente>>) {
  await wrapper.find('[aria-label="Altre opzioni"]').trigger('click')
  await flushPromises()
}

function testiVociMenu(): string[] {
  return Array.from(document.querySelectorAll('[role="menuitem"]')).map((el) => el.textContent?.trim() ?? '')
}

afterEach(() => {
  document.body.innerHTML = ''
})

describe('AlimentoRigaAzioni', () => {
  it('un alimento BDA mostra "Visualizza" e nessuna opzione Elimina', async () => {
    const wrapper = await montaComponente(alimentoBda)
    await apriMenu(wrapper)

    const voci = testiVociMenu()
    expect(voci.some((t) => t.includes('Visualizza'))).toBe(true)
    expect(voci.some((t) => t.includes('Modifica'))).toBe(false)
    expect(voci.some((t) => t.includes('Elimina'))).toBe(false)
    wrapper.unmount()
  })

  it('un alimento personalizzato mostra "Modifica" e l\'opzione Elimina', async () => {
    const wrapper = await montaComponente(alimentoPersonalizzato)
    await apriMenu(wrapper)

    const voci = testiVociMenu()
    expect(voci.some((t) => t.includes('Modifica'))).toBe(true)
    expect(voci.some((t) => t.includes('Visualizza'))).toBe(false)
    expect(voci.some((t) => t.includes('Elimina'))).toBe(true)
    wrapper.unmount()
  })

  it('emette apri al click della voce principale del menu', async () => {
    const wrapper = await montaComponente(alimentoPersonalizzato)
    await apriMenu(wrapper)

    const voceModifica = Array.from(document.querySelectorAll('[role="menuitem"]')).find((el) => el.textContent?.includes('Modifica'))
    voceModifica?.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await flushPromises()

    expect(wrapper.emitted('apri')).toEqual([[alimentoPersonalizzato]])
    wrapper.unmount()
  })

  it('chiede conferma prima di eliminare ed emette elimina solo dopo la conferma', async () => {
    const wrapper = await montaComponente(alimentoPersonalizzato)
    await apriMenu(wrapper)

    const voceElimina = Array.from(document.querySelectorAll('[role="menuitem"]')).find((el) => el.textContent?.includes('Elimina'))
    voceElimina?.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await flushPromises()

    expect(wrapper.emitted('elimina')).toBeUndefined()
    expect(document.body.textContent).toContain('Eliminare Frullato proteico?')

    const conferma = Array.from(document.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Conferma')
    conferma?.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    await flushPromises()

    expect(wrapper.emitted('elimina')).toEqual([[alimentoPersonalizzato]])
    wrapper.unmount()
  })
})
