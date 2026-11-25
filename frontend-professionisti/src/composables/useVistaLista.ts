import { ref, watch } from 'vue'

export type VistaLista = 'tabella' | 'schede'

/**
 * Vista scelta su desktop per una lista (tabella o schede), ricordata nel browser per lista.
 * Su mobile le liste mostrano sempre le schede: questa scelta conta solo da 768 px in su.
 * L'accesso a localStorage può fallire (finestra privata, dati bloccati): si ricade sulla tabella.
 */
export function useVistaLista(chiave: string) {
  const chiaveStorage = `hexis.vista.${chiave}`
  let iniziale: VistaLista = 'tabella'
  try {
    if (localStorage.getItem(chiaveStorage) === 'schede') iniziale = 'schede'
  } catch {
    // nessuna preferenza salvata leggibile
  }
  const vista = ref<VistaLista>(iniziale)
  watch(vista, (valore) => {
    try {
      localStorage.setItem(chiaveStorage, valore)
    } catch {
      // la preferenza resta solo per questa visita
    }
  })

  /** Classi del contenitore tabella: mai su mobile, su desktop solo in vista tabella. */
  const classiTabella = () => (vista.value === 'tabella' ? 'hidden md:block' : 'hidden')
  /** Classi del contenitore schede: sempre su mobile, su desktop solo in vista schede. */
  const classiSchede = () => (vista.value === 'schede' ? '' : 'md:hidden')

  return { vista, classiTabella, classiSchede }
}
