import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import AggiungiAlimentoDialog from './AggiungiAlimentoDialog.vue'
import { cerca } from '@/api/alimenti'

vi.mock('@/api/alimenti', () => ({ cerca: vi.fn() }))

// DialogContent è teletrasportato (DialogPortal di reka-ui) fuori dall'albero del
// wrapper, dentro document.body: wrapper.find non lo raggiunge (vedi AlimentoFormDialog.spec.ts,
// che usa lo stesso pattern query-su-document per lo stesso motivo).
function campo(selettore: string): DOMWrapper<Element> {
  const el = document.querySelector(selettore)
  if (!el) throw new Error(`Elemento non trovato: ${selettore}`)
  return new DOMWrapper(el)
}

afterEach(() => {
  document.body.innerHTML = ''
})

describe('AggiungiAlimentoDialog', () => {
  beforeEach(() => {
    vi.mocked(cerca).mockResolvedValue({
      contenuto: [{ id: 'a1', nome: 'Avena in fiocchi', categoria: 'Cereali', quantitaG: 100, kcal: 372,
        proteineG: 12.9, grassiG: 6.5, carboidratiG: 65, acquaG: null, fibreG: null, zuccheriG: 1.1,
        ferroMg: null, calcioMg: null, sodioMg: null, bda: true }],
      paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1,
    })
  })

  it('selezionando un risultato di ricerca emette aggiunto con grammi 100 e chiude il dialog', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true }, attachTo: document.body })
    await flushPromises()
    await campo('input[placeholder="Cerca per nome o categoria…"]').setValue('avena')
    await vi.waitFor(() => expect(cerca).toHaveBeenCalled())
    await nextTick()

    await campo('[data-test="risultato-ricerca"]').trigger('click')

    expect(wrapper.emitted('aggiunto')?.[0]?.[0]).toEqual({
      alimentoId: 'a1', nome: 'Avena in fiocchi', kcal100g: 372, proteine100g: 12.9, carboidrati100g: 65,
      grassi100g: 6.5, zuccheri100g: 1.1, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
      grammi: 100,
    })
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
  })

  it('normalizza a per-100g un risultato con quantitaG diversa da 100 (alimento personalizzato)', async () => {
    vi.mocked(cerca).mockResolvedValue({
      contenuto: [{ id: 'p1', nome: 'Barretta proteica', categoria: 'Snack', quantitaG: 250, kcal: 500,
        proteineG: 50, grassiG: 25, carboidratiG: 75, acquaG: null, fibreG: null, zuccheriG: 25,
        ferroMg: null, calcioMg: null, sodioMg: null, bda: false }],
      paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1,
    })
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true }, attachTo: document.body })
    await flushPromises()
    await campo('input[placeholder="Cerca per nome o categoria…"]').setValue('barretta')
    await vi.waitFor(() => expect(cerca).toHaveBeenCalled())
    await nextTick()

    await campo('[data-test="risultato-ricerca"]').trigger('click')

    expect(wrapper.emitted('aggiunto')?.[0]?.[0]).toEqual({
      alimentoId: 'p1', nome: 'Barretta proteica', kcal100g: 200, proteine100g: 20, carboidrati100g: 30,
      grassi100g: 10, zuccheri100g: 10, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
      grammi: 100,
    })
  })

  it('richiede nome e i 4 macro obbligatori nella modalità manuale', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true }, attachTo: document.body })
    await flushPromises()
    await campo('[data-test="tab-manuale"]').trigger('click')

    await campo('[data-test="aggiungi-manuale"]').trigger('click')

    expect(wrapper.emitted('aggiunto')).toBeUndefined()
    expect(document.body.textContent).toContain('obbligatorio')
  })

  it('in modalità manuale emette aggiunto con i valori inseriti e alimentoId nullo', async () => {
    const wrapper = mount(AggiungiAlimentoDialog, { props: { open: true }, attachTo: document.body })
    await flushPromises()
    await campo('[data-test="tab-manuale"]').trigger('click')
    await campo('[data-test="manuale-nome"]').setValue('Insalata mista')
    await campo('[data-test="manuale-kcal"]').setValue('50')
    await campo('[data-test="manuale-proteine"]').setValue('2')
    await campo('[data-test="manuale-carboidrati"]').setValue('5')
    await campo('[data-test="manuale-grassi"]').setValue('1')

    await campo('[data-test="aggiungi-manuale"]').trigger('click')

    expect(wrapper.emitted('aggiunto')?.[0]?.[0]).toEqual({
      alimentoId: null, nome: 'Insalata mista', kcal100g: 50, proteine100g: 2, carboidrati100g: 5,
      grassi100g: 1, zuccheri100g: null, fibre100g: null, ferro100mg: null, calcio100mg: null, acqua100g: null,
      grammi: 100,
    })
  })
})
