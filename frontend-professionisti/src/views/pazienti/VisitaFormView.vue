<script setup lang="ts">
import { useConfermaUscita } from '@/composables/useConfermaUscita'
import CaricamentoLista from '@/components/liste/CaricamentoLista.vue'
import { scorriAlPrimoErrore } from '@/utils/form'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import DatiVisitaForm from '@/components/pazienti/DatiVisitaForm.vue'
import RiepilogoVisitaPrecedente from '@/components/pazienti/RiepilogoVisitaPrecedente.vue'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'
import {
  dettaglio,
  dettaglioVisita,
  visite as caricaVisite,
  creaVisita,
  aggiornaVisita,
  type Paziente,
  type Visita,
} from '@/api/pazienti'
import { cerca as cercaPiani, noteDelPiano } from '@/api/pianiAlimentari'
import { pianiNellIntervallo, type PianoConNote } from '@/utils/pianoAlimentare'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import BarraAzioniForm from '@/components/form/BarraAzioniForm.vue'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/alert-dialog'
import { ArrowLeft, History, LogOut, Pencil } from '@lucide/vue'
import { formattaDataItalianaConMese } from '@/utils/data'
import { formattaNumero } from '@/utils/visita'

const route = useRoute()
const router = useRouter()

const pazienteIdRoute = route.params.id as string | undefined
const visitaId = route.params.visitaId as string | undefined
// Presente quando si arriva da "Avvia visita" dell'agenda: l'appuntamento viene concluso alla registrazione.
const appuntamentoId = typeof route.query.appuntamentoId === 'string' ? route.query.appuntamentoId : undefined
const modalitaModifica = computed(() => !!visitaId)

const paziente = ref<Paziente | null>(null)
const visitaEsistente = ref<Visita | null>(null)
const ultimaVisita = ref<Visita | null>(null)
const caricamento = ref(true)
const erroreCaricamento = ref<string | null>(null)
const inCorso = ref(false)

const datiVisitaForm = ref<InstanceType<typeof DatiVisitaForm>>()

// Storico visite del paziente, dalla più vecchia: serve a ricavare la visita precedente per il riepilogo.
const visiteDelPaziente = ref<Visita[]>([])

async function caricaUltimaVisita(pazienteId: string) {
  try {
    const lista = await caricaVisite(pazienteId)
    visiteDelPaziente.value = lista
    ultimaVisita.value = lista.length > 0 ? lista[lista.length - 1] : null
  } catch {
    visiteDelPaziente.value = []
    ultimaVisita.value = null
  }
}

// "Ultima visita" del riepilogo: in modifica è la visita che precede quella aperta (non la più recente
// in assoluto), in inserimento l'ultima registrata. La lista arriva dal backend già in ordine cronologico
// (data, poi inserimento): si usa la posizione, così vale anche per due visite nello stesso giorno.
const visitaPrecedente = computed<Visita | null>(() => {
  const corrente = visitaEsistente.value
  const lista = visiteDelPaziente.value
  const fine = corrente ? lista.findIndex((v) => v.id === corrente.id) : lista.length
  const candidate = fine >= 0 ? lista.slice(0, fine) : lista.filter((v) => v.dataVisita < corrente!.dataVisita)
  return candidate.length > 0 ? candidate[candidate.length - 1] : null
})

const riepilogoAperto = ref(false)
const pianiRiepilogo = ref<PianoConNote[]>([])
const noteInCaricamento = ref(false)

// Visita e piano non sono collegati da un vincolo: il piano parte dopo una visita e dura fino alla successiva.
// Il riepilogo mostra quindi le note dei piani in corso tra la visita precedente e questa (in modifica fino
// alla data della visita aperta, in inserimento fino a oggi). Le bozze non hanno note e restano fuori.
// Si carica quando il pannello è aperto (non a ogni apertura della pagina) e se cambia la visita di riferimento.
let richiestaRiepilogo = 0
async function caricaPianiEdNote() {
  const richiesta = ++richiestaRiepilogo
  pianiRiepilogo.value = []
  const visita = visitaPrecedente.value
  const pazienteId = paziente.value?.id
  if (!riepilogoAperto.value || !visita || !pazienteId) return
  noteInCaricamento.value = true
  try {
    const pagina = await cercaPiani({ pazienteId, escludiBozze: true, dimensione: 100 })
    const piani = pianiNellIntervallo(pagina.contenuto, visita.dataVisita, visitaEsistente.value?.dataVisita ?? null)
      .reverse() // dal più recente
    const conNote = await Promise.all(piani.map(async (piano) => ({ piano, note: await noteDelPiano(piano.id) })))
    if (richiesta === richiestaRiepilogo) pianiRiepilogo.value = conNote
  } catch {
    // Il riepilogo resta utilizzabile con i soli dati della visita.
  } finally {
    if (richiesta === richiestaRiepilogo) noteInCaricamento.value = false
  }
}
watch([riepilogoAperto, visitaPrecedente], caricaPianiEdNote)

async function caricaConPazienteNoto(pazienteId: string) {
  caricamento.value = true
  try {
    paziente.value = await dettaglio(pazienteId)
    if (visitaId) {
      visitaEsistente.value = await dettaglioVisita(pazienteId, visitaId)
    }
    await caricaUltimaVisita(pazienteId)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = modalitaModifica.value ? 'Visita non trovata.' : 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare i dati.'
    }
  } finally {
    caricamento.value = false
  }
}

async function onPazienteCambiato(selezionato: Paziente | null) {
  ultimaVisita.value = null
  visiteDelPaziente.value = []
  riepilogoAperto.value = false
  if (!selezionato) {
    paziente.value = null
    return
  }
  // Attende il caricamento dell'ultima visita prima di mostrare il form, così
  // altezza/obiettivo suggeriti sono già disponibili al primo render di DatiVisitaForm.
  await caricaUltimaVisita(selezionato.id)
  paziente.value = selezionato
}

onMounted(() => {
  if (pazienteIdRoute) {
    caricaConPazienteNoto(pazienteIdRoute)
  } else {
    caricamento.value = false
  }
})

const sottotitolo = computed(() => {
  if (!paziente.value) return pazienteIdRoute ? '' : 'Seleziona il paziente per cui registrare la nuova visita.'
  const base = `${paziente.value.nome} ${paziente.value.cognome}`
  // In modifica la visita di riferimento è quella aperta: "ultima visita" la confondeva con il riepilogo della precedente.
  if (modalitaModifica.value) {
    return visitaEsistente.value ? `${base} · visita del ${formattaDataItalianaConMese(visitaEsistente.value.dataVisita)}` : base
  }
  if (!ultimaVisita.value) return base
  return `${base} · ultima visita ${formattaDataItalianaConMese(ultimaVisita.value.dataVisita)} con peso ${formattaNumero(ultimaVisita.value.pesoKg)} kg`
})

// Da `/pazienti/:id/visite/...` (creazione o modifica avviata dal dettaglio paziente, o dalla
// riga paziente in lista) il paziente è già noto dalla route: si torna sempre lì.
// Da `/pazienti/visite/nuova` (nessun id in route) invece il paziente si scopre solo dopo aver
// scelto dal combobox (vedi onPazienteCambiato) — prima di allora non c'è un paziente noto.
// Questa route è raggiungibile non solo dal link "Nuova visita" della dashboard ma anche dalla
// Command Palette (Ctrl+K), disponibile da qualunque pagina: un fallback fisso alla dashboard
// sbaglierebbe la pagina chiamante in tutti quei casi, quindi si preferisce router.back() (la
// vera pagina precedente in cronologia) quando disponibile, e solo in mancanza di cronologia si
// ripiega sulla dashboard. Stessa identica logica sia per il link in alto sia per "Annulla".
const pazienteIdConosciuto = computed(() => pazienteIdRoute ?? paziente.value?.id)
// Quando si andrà indietro con router.back() (nessun paziente noto ma c'è cronologia), la
// destinazione reale non è la dashboard ma la pagina chiamante: l'etichetta deve dirlo in modo
// generico ("Torna indietro"), altrimenti mentirebbe su dove porta il click.
const haCronologia = !!router.options.history.state.back
const linkIndietro = computed(() => (pazienteIdConosciuto.value ? `/pazienti/${pazienteIdConosciuto.value}` : '/'))
const testoLinkIndietro = computed(() => {
  if (pazienteIdConosciuto.value) return 'Torna al paziente'
  return haCronologia ? 'Torna indietro' : 'Torna alla dashboard'
})

function tornaIndietro() {
  if (pazienteIdConosciuto.value) {
    router.push(`/pazienti/${pazienteIdConosciuto.value}`)
  } else if (router.options.history.state.back) {
    router.back()
  } else {
    router.push('/')
  }
}


// "Uscire senza salvare?" su ogni uscita dalla pagina, sidebar compresa (bug 13 del collaudo).
const {
  aperta: mostraConfermaAnnulla,
  chiedi: chiediUscita,
  esciSenzaSalvare,
  consentiUscita,
} = useConfermaUscita(() => datiVisitaForm.value?.modificato() ?? false)

/** "Annulla": esce subito se non è stato cambiato nulla, altrimenti chiede conferma. */
function onAnnullaClick() {
  chiediUscita(tornaIndietro)
}

async function onSubmit() {
  if (!paziente.value) return
  const valido = datiVisitaForm.value?.valida() ?? false
  if (!valido) {
    scorriAlPrimoErrore()
    return
  }

  inCorso.value = true
  try {
    const dati = datiVisitaForm.value!.ottieniDati()
    if (modalitaModifica.value && visitaId) {
      await aggiornaVisita(paziente.value.id, visitaId, dati)
      toast.success('Visita aggiornata.')
    } else {
      await creaVisita(paziente.value.id, dati, appuntamentoId)
      toast.success('Visita registrata.')
    }
    consentiUscita()
    router.push(`/pazienti/${paziente.value.id}`)
  } catch (errore) {
    if (appuntamentoId && errore instanceof ApiError && (errore.status === 400 || errore.status === 404)) {
      toast.error("L'appuntamento non è più valido: la visita non è stata salvata. Riapri la visita dall'agenda o dalla scheda paziente.")
    } else {
      toast.error('Non è stato possibile salvare la visita. Controlla i dati e riprova.')
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
      <Button as-child variant="ghost-primary" size="sm" class="group font-semibold">
        <router-link :to="linkIndietro" data-test="link-indietro" @click.capture.prevent="onAnnullaClick">
          <ArrowLeft aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
          {{ testoLinkIndietro }}
        </router-link>
      </Button>
      <h1 class="font-heading text-3xl italic text-(--fg)">{{ modalitaModifica ? 'Modifica visita' : 'Nuova visita' }}</h1>
      <p v-if="sottotitolo" class="mt-1 text-sm text-(--fg3)">{{ sottotitolo }}</p>
      </div>

      <!-- Senza visite precedenti (prima visita) non c'è niente da riepilogare: il pulsante non compare. -->
      <Button
        v-if="paziente && visitaPrecedente"
        type="button"
        variant="neutral"
        data-test="apri-riepilogo"
        :aria-pressed="riepilogoAperto"
        :class="riepilogoAperto && 'border-(--green) bg-(--mint) text-(--green-d) hover:bg-(--mint)'"
        @click="riepilogoAperto = !riepilogoAperto"
      >
        <History :size="16" />
        Riepilogo precedente
      </Button>
    </div>

    <div class="flex items-start gap-6">
    <div class="min-w-0 flex-1">
    <div
      v-if="erroreCaricamento"
      class="flex flex-col items-center justify-center rounded-2xl border border-(--bd) bg-(--surf) p-12 text-center"
    >
      <p class="text-sm text-(--fg3)">{{ erroreCaricamento }}</p>
    </div>

    <CaricamentoLista v-else-if="caricamento" schede etichetta="Caricamento dei dati della visita" :righe="2" />

    <template v-else>
      <SelezionaPazienteCombobox
        v-if="!pazienteIdRoute"
        class="mb-6"
        :model-value="paziente"
        @update:model-value="onPazienteCambiato"
      />

      <form v-if="paziente" class="space-y-6" @submit.prevent="onSubmit">
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 sm:p-8">
          <DatiVisitaForm
            ref="datiVisitaForm"
            :sesso="paziente.sesso"
            :dati-iniziali="visitaEsistente"
            :altezza-suggerita-cm="ultimaVisita?.altezzaCm"
            :obiettivo-suggerito="ultimaVisita?.obiettivo"
            :parte-dominante-suggerita="ultimaVisita?.parteDominante"
          />
        </div>

        <BarraAzioniForm
          :modificato="datiVisitaForm?.modificato() ?? false"
          :in-corso="inCorso"
          etichetta-salva="Salva visita"
          @annulla="onAnnullaClick"
        />
      </form>
    </template>
    </div>

    <RiepilogoVisitaPrecedente
      v-if="riepilogoAperto && paziente && visitaPrecedente"
      :visita="visitaPrecedente"
      :stile-di-vita="paziente.stileDiVita"
      :piani="pianiRiepilogo"
      :note-in-caricamento="noteInCaricamento"
      @chiudi="riepilogoAperto = false"
    />
    </div>

    <AlertDialog v-model:open="mostraConfermaAnnulla">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Uscire senza salvare?</AlertDialogTitle>
          <AlertDialogDescription>
            Hai delle modifiche non salvate. Se esci ora andranno perse.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel data-test="conferma-annulla-continua"><Pencil /> Continua a compilare</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-annulla-esci" @click="esciSenzaSalvare"><LogOut /> Esci senza salvare</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </AppShell>
</template>
