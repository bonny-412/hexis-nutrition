import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

/**
 * "Uscire senza salvare?" per le pagine con modifiche non salvate, su **qualunque** uscita: pulsanti
 * della pagina, sidebar, header, Ctrl+K, indietro del browser (bug 13 del collaudo, 27/11/2026). Chiudere
 * o ricaricare la scheda mostra invece l'avviso nativo del browser.
 *
 * È una guardia globale (`router.beforeEach`) attiva finché la pagina è montata, non `onBeforeRouteLeave`:
 * l'editor dei piani passa da /piani-alimentari/nuovo a /piani-alimentari/:id restando la stessa istanza,
 * e una guardia legata al record di partenza smetterebbe di valere dopo il primo salvataggio.
 *
 * Il dialog resta nel template della pagina: `v-model:open="aperta"` e `@click="esciSenzaSalvare"`
 * sull'AlertDialogAction. L'uscita in sospeso è tenuta a parte da `aperta`, perché AlertDialogAction chiude
 * il dialog prima di eseguire il click (vedi il bug delle conferme dell'agenda).
 */
export function useConfermaUscita(modificato: () => boolean) {
  const router = useRouter()
  const aperta = ref(false)
  let uscitaInSospeso: (() => void) | null = null
  let uscitaConsentita = false

  /** Esegue l'uscita se non ci sono modifiche, altrimenti chiede conferma e la esegue solo su "Esci". */
  function chiedi(uscita: () => void) {
    if (!modificato()) {
      uscita()
      return
    }
    uscitaInSospeso = uscita
    aperta.value = true
  }

  function esciSenzaSalvare() {
    aperta.value = false
    const uscita = uscitaInSospeso
    uscitaInSospeso = null
    uscitaConsentita = true
    uscita?.()
  }

  /** Lascia passare la prossima navigazione senza chiedere: da chiamare dopo un salvataggio riuscito. */
  function consentiUscita() {
    uscitaConsentita = true
  }

  let rimuoviGuardia: (() => void) | null = null
  const avvisoChiusura = (evento: BeforeUnloadEvent) => {
    if (modificato()) evento.preventDefault()
  }

  onMounted(() => {
    rimuoviGuardia = router.beforeEach((to, from) => {
      if (uscitaConsentita) {
        uscitaConsentita = false
        return true
      }
      if (to.fullPath === from.fullPath || !modificato()) return true
      chiedi(() => router.push(to.fullPath))
      return false
    })
    window.addEventListener('beforeunload', avvisoChiusura)
  })

  onBeforeUnmount(() => {
    rimuoviGuardia?.()
    window.removeEventListener('beforeunload', avvisoChiusura)
  })

  return { aperta, chiedi, esciSenzaSalvare, consentiUscita }
}
