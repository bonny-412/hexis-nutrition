import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import AlimentoFormDialog from './AlimentoFormDialog.vue'
import * as alimentiApi from '@/api/alimenti'
import type { Alimento } from '@/api/alimenti'

vi.mock('@/api/alimenti')
vi.mock('vue-sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}))

const alimentoBda: Alimento = {
  id: '1', nome: 'Petto di pollo, crudo', categoria: 'Carni', quantitaG: 100, kcal: 165, proteineG: 31, grassiG: 3.6,
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

function campo(selettore: string): DOMWrapper<Element> {
  const el = document.querySelector(selettore)
  if (!el) throw new Error(`Elemento non trovato: ${selettore}`)
  return new DOMWrapper(el)
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

  it('la modalità creazione precompila la quantità di riferimento a 100', async () => {
    await montaDialog(null)

    const quantita = document.querySelector('#alimento-quantita') as HTMLInputElement
    expect(quantita.value).toBe('100')
  })

  it('un alimento con quantità diversa da 100 mostra il valore corretto nel campo e nel titolo', async () => {
    const snack: Alimento = { ...alimentoPersonalizzato, id: '3', quantitaG: 30 }
    await montaDialog(snack)

    const quantita = document.querySelector('#alimento-quantita') as HTMLInputElement
    expect(quantita.value).toBe('30')
    expect(document.body.textContent).toContain('Valori nutrizionali · per 30 g')
  })

  it('non invia la richiesta e mostra gli errori sotto i campi obbligatori vuoti', async () => {
    await montaDialog(null)

    await campo('form').trigger('submit')
    await flushPromises()

    expect(alimentiApi.crea).not.toHaveBeenCalled()
    expect(document.body.textContent).toContain('Il nome è obbligatorio.')
    expect(document.body.textContent).toContain('La categoria è obbligatoria.')
    expect(document.body.textContent).toContain('Il valore è obbligatorio.')
  })

  it('filtra i caratteri non numerici nei campi decimali e accetta la virgola italiana', async () => {
    await montaDialog(null)

    const quantita = campo('#alimento-quantita')
    await quantita.setValue('abc12,5xyz')

    expect((quantita.element as HTMLInputElement).value).toBe('12,5')
  })

  it('invia la richiesta con i numeri decimali italiani convertiti correttamente', async () => {
    vi.mocked(alimentiApi.crea).mockResolvedValue({ ...alimentoPersonalizzato, id: '9' })
    await montaDialog(null)

    await campo('#alimento-nome').setValue('Snack in busta')
    await campo('#alimento-categoria').setValue('Snack')
    await campo('#alimento-quantita').setValue('30')
    await campo('#alimento-kcal').setValue('140')
    await campo('#alimento-proteine').setValue('2,5')
    await campo('#alimento-grassi').setValue('7')
    await campo('#alimento-carboidrati').setValue('18,25')

    await campo('form').trigger('submit')
    await flushPromises()

    expect(alimentiApi.crea).toHaveBeenCalledWith(expect.objectContaining({
      nome: 'Snack in busta',
      categoria: 'Snack',
      quantitaG: 30,
      kcal: 140,
      proteineG: 2.5,
      grassiG: 7,
      carboidratiG: 18.25,
    }))
  })

  it('l\'errore di un campo obbligatorio sparisce non appena diventa valido', async () => {
    await montaDialog(null)

    await campo('form').trigger('submit')
    await flushPromises()
    expect(document.body.textContent).toContain('Il nome è obbligatorio.')

    await campo('#alimento-nome').setValue('Snack in busta')
    await flushPromises()

    expect(document.body.textContent).not.toContain('Il nome è obbligatorio.')
  })
})
