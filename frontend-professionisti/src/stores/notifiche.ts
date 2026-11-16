import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { elencoNotifiche, segnaAgendaViste as apiSegnaAgendaViste, segnaLette, type Notifica } from '@/api/notifiche'

const INTERVALLO_POLLING_MS = 60_000

export const useNotificheStore = defineStore('notifiche', () => {
  const notifiche = ref<Notifica[]>([])
  const nonLette = ref(0)
  const nonVisteInAgenda = ref(0)
  /** Il professionista ha cliccato il campanello da quando ci sono notifiche non lette: il pulse si ferma. */
  const cliccato = ref(false)
  const listaAperta = ref(false)
  const pulsa = computed(() => nonLette.value > 0 && !cliccato.value)

  // Id delle notifiche presenti all'apertura della lista: solo queste si segnano lette alla chiusura.
  let idsMostrati: string[] = []
  let timer: ReturnType<typeof setInterval> | null = null

  async function carica() {
    try {
      const risposta = await elencoNotifiche()
      notifiche.value = risposta.notifiche
      nonLette.value = risposta.nonLette
      nonVisteInAgenda.value = risposta.nonVisteInAgenda
    } catch {
      // Errore di rete o sessione scaduta (gestita dal client): si riprova al prossimo ciclo.
    }
  }

  function alCambioVisibilita() {
    if (!document.hidden) void carica()
  }

  function avviaPolling() {
    if (timer !== null) return
    void carica()
    timer = setInterval(() => {
      if (!document.hidden) void carica()
    }, INTERVALLO_POLLING_MS)
    document.addEventListener('visibilitychange', alCambioVisibilita)
  }

  function fermaPolling() {
    if (timer !== null) clearInterval(timer)
    timer = null
    document.removeEventListener('visibilitychange', alCambioVisibilita)
  }

  function apriLista() {
    listaAperta.value = true
    cliccato.value = true
    idsMostrati = notifiche.value.map((n) => n.id)
  }

  async function chiudiLista() {
    listaAperta.value = false
    cliccato.value = false
    const ids = idsMostrati
    idsMostrati = []
    try {
      if (ids.length > 0) await segnaLette(ids)
    } catch {
      // Se fallisce restano non lette: il pallino ricompare al prossimo ricarico.
    }
    await carica()
  }

  async function segnaAgendaViste() {
    nonVisteInAgenda.value = 0
    try {
      await apiSegnaAgendaViste()
    } catch {
      // Riproverà al prossimo ciclo se il server le riporta ancora come non viste.
    }
    await carica()
  }

  return { notifiche, nonLette, nonVisteInAgenda, cliccato, listaAperta, pulsa, carica, avviaPolling, fermaPolling, apriLista, chiudiLista, segnaAgendaViste }
})
