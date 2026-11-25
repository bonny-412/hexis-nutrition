import { nextTick } from 'vue'

/**
 * Dopo un salvataggio bloccato dalla validazione porta in vista il primo campo con errore:
 * nei form lunghi "Salva" sta in fondo e l'errore spesso in cima, e il clic sembrava non fare nulla.
 * Cerca, nell'ordine della pagina, un campo `aria-invalid`, un elemento marcato `data-errore`
 * o un messaggio d'errore (`text-(--danger)`), e mette il focus sul campo quando c'è.
 */
export function scorriAlPrimoErrore() {
  nextTick(() => {
    const candidati = [
      ...document.querySelectorAll<HTMLElement>('[aria-invalid="true"], [data-errore], p[class*="text-(--danger)"]'),
    ]
    const primo = candidati[0]
    if (!primo) return
    primo.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (primo.matches('input, textarea, button, [tabindex]')) primo.focus({ preventScroll: true })
  })
}
