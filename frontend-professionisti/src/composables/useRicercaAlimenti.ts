import { ref, watch, onUnmounted } from 'vue'
import { cerca as cercaAlimenti, type Alimento } from '@/api/alimenti'

/** Ricerca alimenti nel database con debounce di 300 ms sul testo digitato. */
export function useRicercaAlimenti() {
  const query = ref('')
  const risultati = ref<Alimento[]>([])
  let debounceHandle: ReturnType<typeof setTimeout> | undefined
  // Conta le richieste: una risposta che non è l'ultima (o arriva dopo un azzeramento) viene scartata.
  let contatore = 0

  async function eseguiRicerca(testo: string) {
    const ricerca = testo.trim()
    const numero = ++contatore
    if (!ricerca) {
      risultati.value = []
      return
    }
    // Il server ordina per pertinenza: prima i nomi che iniziano col testo, ultimi i match sulla sola categoria.
    const pagina = await cercaAlimenti({ ricerca, ordinaPer: 'pertinenza', dimensione: 30 })
    if (numero !== contatore) return
    risultati.value = pagina.contenuto
  }

  watch(query, (valore) => {
    clearTimeout(debounceHandle)
    debounceHandle = setTimeout(() => eseguiRicerca(valore), 300)
  })
  onUnmounted(() => clearTimeout(debounceHandle))

  /** Svuota testo e risultati e annulla la ricerca in attesa. */
  function azzera() {
    contatore++
    query.value = ''
    risultati.value = []
    clearTimeout(debounceHandle)
  }

  return { query, risultati, azzera }
}
