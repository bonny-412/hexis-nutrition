import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import SostituisciAlimentoDialog from './SostituisciAlimentoDialog.vue'
import { cerca, type Alimento, type PaginaAlimenti } from '@/api/alimenti'
import type { RigaAlimento } from '@/utils/sostituzioneAlimento'

vi.mock('@/api/alimenti', () => ({ cerca: vi.fn() }))

// Il contenuto del dialog è teletrasportato in document.body (vedi AggiungiAlimentoDialog.spec.ts).
function el(selettore: string): DOMWrapper<Element> {
  const trovato = document.querySelector(selettore)
  if (!trovato) throw new Error(`Elemento non trovato: ${selettore}`)
  return new DOMWrapper(trovato)
}
const tutti = (selettore: string) => Array.from(document.querySelectorAll(selettore))

function alimento(id: string, nome: string, valori: { kcal: number; p: number; c: number; g: number }): Alimento {
  return {
    id, nome, categoria: 'Categoria', quantitaG: 100, kcal: valori.kcal, proteineG: valori.p, grassiG: valori.g,
    carboidratiG: valori.c, acquaG: null, fibreG: null, zuccheriG: null, ferroMg: null, calcioMg: null, sodioMg: null, bda: true,
  }
}
function pagina(contenuto: Alimento[]): PaginaAlimenti {
  return { contenuto, paginaCorrente: 0, dimensionePagina: 30, totaleElementi: contenuto.length, totalePagine: 1 }
}

const riso = alimento('riso', 'Riso, crudo', { kcal: 350, p: 7, c: 80, g: 0.6 })
const albumeCercato = alimento('albume', 'Albume', { kcal: 43, p: 11, c: 0, g: 0 })

// 50 g di pane: 135 kcal, 4 g prot., 32 g carb., 0,25 g grassi.
const pane: RigaAlimento = {
  alimentoId: 'pane', nome: 'Pane comune', kcal100g: 270, proteine100g: 8, carboidrati100g: 64, grassi100g: 0.5,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null, grammi: 50,
}
const albume: RigaAlimento = {
  alimentoId: 'albume', nome: 'Albume', kcal100g: 43, proteine100g: 11, carboidrati100g: 0, grassi100g: 0,
  zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null, grammi: 100,
}

async function apri(riga: RigaAlimento, altreRigheUguali = 0) {
  // Montato chiuso e poi aperto, come fa l'editor: il popup si prepara all'apertura.
  const wrapper = mount(SostituisciAlimentoDialog, {
    props: { open: false, riga, altreRigheUguali }, attachTo: document.body,
  })
  await wrapper.setProps({ open: true })
  await flushPromises()
  return wrapper
}

async function cercaTesto(testo: string) {
  await el('input[placeholder="Cerca per nome o categoria…"]').setValue(testo)
  await vi.waitFor(() => expect(cerca).toHaveBeenCalledWith(expect.objectContaining({ ricerca: testo })))
  await flushPromises()
}

async function scegliValore(valore: string) {
  await el(`[data-test="mantieni-${valore}"]`).trigger('click')
  await flushPromises()
}

const risultati = () => tutti('[data-test="risultato-sostituzione"]') as HTMLButtonElement[]
const conferma = () => el('[data-test="conferma-sostituzione"]').element as HTMLButtonElement

describe('SostituisciAlimentoDialog', () => {
  beforeEach(() => {
    vi.mocked(cerca).mockReset()
    vi.mocked(cerca).mockResolvedValue(pagina([riso, albumeCercato]))
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('parte dalle kcal e disattiva i valori che la riga di partenza ha a zero', async () => {
    await apri(albume)

    expect(el('[data-test="mantieni-kcal"]').attributes('data-state')).toBe('checked')
    expect(el('[data-test="mantieni-proteine"]').attributes('data-disabled')).toBeUndefined()
    expect(el('[data-test="mantieni-carboidrati"]').attributes('data-disabled')).toBeDefined()
    expect(el('[data-test="mantieni-grassi"]').attributes('data-disabled')).toBeDefined()
  })

  it('mostra i grammi equivalenti per il valore scelto e disattiva chi non ha quel valore', async () => {
    await apri(pane)
    await cercaTesto('ri')
    await scegliValore('carboidrati')

    const [rigaRiso, rigaAlbume] = risultati()
    expect(rigaRiso.textContent).toContain('40 g') // 32 g carb. ÷ 80 g/100 g
    expect(rigaRiso.disabled).toBe(false)
    expect(rigaAlbume.disabled).toBe(true)
    expect(rigaAlbume.textContent).toContain('non ha carboidrati')
  })

  it('scelto un risultato mostra il sostituto con le differenze, e conferma emette la sostituzione', async () => {
    const wrapper = await apri(pane)
    await cercaTesto('ri')
    await scegliValore('carboidrati')
    expect(conferma().disabled).toBe(true)

    await new DOMWrapper(risultati()[0]).trigger('click')

    expect(el('[data-test="riga-sostituto"]').text()).toContain('Riso, crudo')
    expect(el('[data-test="riga-sostituto"]').text()).toContain('40 g')
    // 140 kcal contro 135, 2,8 g di proteine contro 4.
    expect(el('[data-test="differenze"]').text()).toBe('+5 kcal · −1 g prot.')

    await el('[data-test="conferma-sostituzione"]').trigger('click')

    const [dati, mantenuto, ancheAltreRighe] = wrapper.emitted('sostituito')![0] as [unknown, string, boolean]
    expect(dati).toEqual({
      alimentoId: 'riso', nome: 'Riso, crudo', kcal100g: 350, proteine100g: 7, carboidrati100g: 80, grassi100g: 0.6,
      zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
    })
    expect(mantenuto).toBe('carboidrati')
    expect(ancheAltreRighe).toBe(false)
    expect(wrapper.emitted('update:open')?.at(-1)).toEqual([false])
  })

  it('cambiando valore, un sostituto che non si può più calcolare viene tolto e non torna da solo', async () => {
    await apri(pane)
    await cercaTesto('al')
    await new DOMWrapper(risultati()[1]).trigger('click') // albume, a kcal
    expect(document.querySelector('[data-test="riga-sostituto"]')).not.toBeNull()

    await scegliValore('carboidrati')
    expect(document.querySelector('[data-test="riga-sostituto"]')).toBeNull()
    expect(conferma().disabled).toBe(true)

    await scegliValore('kcal')
    expect(document.querySelector('[data-test="riga-sostituto"]')).toBeNull()
  })

  it('la casella per le altre righe compare solo se l\'alimento c\'è altrove, ed è spenta', async () => {
    await apri(pane, 0)
    expect(document.querySelector('[data-test="anche-altre-righe"]')).toBeNull()
    document.body.innerHTML = ''

    const wrapper = await apri(pane, 1)
    expect(document.body.textContent).toContain('Sostituisci anche nell\'altra riga con «Pane comune»')
    expect(el('[data-test="anche-altre-righe"]').attributes('data-state')).toBe('unchecked')

    await cercaTesto('ri')
    await new DOMWrapper(risultati()[0]).trigger('click')
    await el('[data-test="anche-altre-righe"]').trigger('click')
    await el('[data-test="conferma-sostituzione"]').trigger('click')

    expect(wrapper.emitted('sostituito')![0][2]).toBe(true)
  })

  it('una riga a 0 g non si può sostituire e il messaggio lo dice', async () => {
    await apri({ ...pane, grammi: 0 })

    expect(el('[data-test="nessun-valore"]').text()).toBe('La riga ha 0 g: inserisci i grammi prima di sostituire.')
    expect(document.querySelector('input[placeholder="Cerca per nome o categoria…"]')).toBeNull()
  })

  it('una risposta lenta di una ricerca precedente non sostituisce i risultati di quella nuova', async () => {
    let rispondiVecchia: (p: PaginaAlimenti) => void = () => {}
    vi.mocked(cerca)
      .mockImplementationOnce(() => new Promise((r) => { rispondiVecchia = r }))
      .mockResolvedValueOnce(pagina([riso]))
    await apri(pane)

    await el('input[placeholder="Cerca per nome o categoria…"]').setValue('al')
    await vi.waitFor(() => expect(cerca).toHaveBeenCalledTimes(1))
    await cercaTesto('riso')
    rispondiVecchia(pagina([albumeCercato]))
    await flushPromises()

    expect(risultati().map((b) => b.textContent)).toEqual([expect.stringContaining('Riso, crudo')])
  })

  it('riaprendo su un\'altra riga riparte da capo', async () => {
    const wrapper = await apri(pane, 2)
    await cercaTesto('ri')
    await scegliValore('carboidrati')
    await new DOMWrapper(risultati()[0]).trigger('click')
    await wrapper.setProps({ open: false })
    await flushPromises()

    await wrapper.setProps({ riga: albume, altreRigheUguali: 0, open: true })
    await flushPromises()

    expect(el('[data-test="riga-partenza"]').text()).toContain('Albume')
    expect(el('[data-test="mantieni-kcal"]').attributes('data-state')).toBe('checked')
    expect((el('input[placeholder="Cerca per nome o categoria…"]').element as HTMLInputElement).value).toBe('')
    expect(document.querySelector('[data-test="riga-sostituto"]')).toBeNull()
    expect(document.querySelector('[data-test="anche-altre-righe"]')).toBeNull()
  })
})
