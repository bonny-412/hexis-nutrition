<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { CalendarPlus, ChevronDown, Lock, StickyNote } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import AgendaToolbar from '@/components/agenda/AgendaToolbar.vue'
import AgendaGiorno from '@/components/agenda/AgendaGiorno.vue'
import AgendaSettimana from '@/components/agenda/AgendaSettimana.vue'
import AgendaMese from '@/components/agenda/AgendaMese.vue'
import AgendaLegenda from '@/components/agenda/AgendaLegenda.vue'
import AppuntamentoDialog from '@/components/agenda/AppuntamentoDialog.vue'
import AppuntamentoDettaglioDialog from '@/components/agenda/AppuntamentoDettaglioDialog.vue'
import type { AzioneAppuntamento } from '@/components/agenda/AppuntamentoMenu.vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import {
  aggiorna,
  annulla,
  elenco,
  elimina,
  impostaFatto,
  SovrapposizioneError,
  type Appuntamento,
  type AppuntamentoRichiesta,
  type TipoAppuntamento,
} from '@/api/agenda'
import { useNotificheStore } from '@/stores/notifiche'
import { aggiungiGiorni, intervalloVisibile, oggiIso, type FiltroAgenda, type VistaAgenda } from '@/utils/agenda'

const router = useRouter()
const route = useRoute()

const vista = ref<VistaAgenda>('SETTIMANA')
const riferimento = ref(oggiIso())
const filtro = ref<FiltroAgenda>('TUTTI')
const appuntamenti = ref<Appuntamento[]>([])
const caricamento = ref(false)
const errore = ref(false)

// Guardia sulla richiesta più recente: una risposta in ritardo non sovrascrive quella attuale.
let richiestaElenco = 0
async function carica() {
  const richiesta = ++richiestaElenco
  caricamento.value = true
  errore.value = false
  try {
    const { da, a } = intervalloVisibile(vista.value, riferimento.value)
    const risultato = await elenco(da, a)
    if (richiesta === richiestaElenco) appuntamenti.value = risultato
  } catch {
    if (richiesta === richiestaElenco) errore.value = true
  } finally {
    if (richiesta === richiestaElenco) caricamento.value = false
  }
}
watch([vista, riferimento], carica)
onMounted(carica)

// Visite = visite + blocchi; Promemoria = solo promemoria.
const appuntamentiFiltrati = computed(() => {
  if (filtro.value === 'TUTTI') return appuntamenti.value
  if (filtro.value === 'PROMEMORIA') return appuntamenti.value.filter((a) => a.tipo === 'PROMEMORIA')
  return appuntamenti.value.filter((a) => a.tipo !== 'PROMEMORIA')
})

function apriGiorno(iso: string) {
  riferimento.value = iso
  vista.value = 'GIORNO'
}

// Il link è testo libero: si apre solo se http(s).
function apriLinkVideo(link: string) {
  let url: URL | null = null
  try {
    url = new URL(link)
  } catch {
    url = null
  }
  if (!url || (url.protocol !== 'http:' && url.protocol !== 'https:')) {
    toast.error('Link del videoconsulto non valido')
    return
  }
  window.open(url.href, '_blank', 'noopener,noreferrer')
}

// --- Dialog di dettaglio ---
// Si tiene l'id, non l'oggetto: dopo un ricaricamento il dettaglio mostra i dati aggiornati.
const dettaglioId = ref<string | null>(null)
const dettaglioAppuntamento = computed(() => appuntamenti.value.find((a) => a.id === dettaglioId.value) ?? null)
const dettaglioAperto = computed({
  get: () => dettaglioAppuntamento.value !== null,
  set: (v: boolean) => { if (!v) dettaglioId.value = null },
})

function apriDettaglio(a: Appuntamento) {
  dettaglioId.value = a.id
}

// --- Dialog di creazione/modifica ---
const dialogAperto = ref(false)
const dialogTipo = ref<TipoAppuntamento>('VISITA_STUDIO')
const dialogAppuntamento = ref<Appuntamento | null>(null)
const dialogPazienteId = ref<string | null>(null)

function apriCreazione(tipo: TipoAppuntamento, pazienteId: string | null = null) {
  dialogTipo.value = tipo
  dialogAppuntamento.value = null
  dialogPazienteId.value = pazienteId
  dialogAperto.value = true
}

function apriModifica(a: Appuntamento) {
  dialogTipo.value = a.tipo
  dialogAppuntamento.value = a
  dialogPazienteId.value = null
  dialogAperto.value = true
}

const REGEX_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const TIPI_DA_QUERY: Record<string, TipoAppuntamento> = {
  visita: 'VISITA_STUDIO',
  promemoria: 'PROMEMORIA',
  blocco: 'BLOCCO',
}

// `?nuovo=visita|promemoria|blocco[&pazienteId=<uuid>]`: apre il dialog e poi rimuove la query.
watch(
  () => route.query.nuovo,
  (nuovo) => {
    const tipo = typeof nuovo === 'string' ? TIPI_DA_QUERY[nuovo] : undefined
    if (!tipo) return
    const id = typeof route.query.pazienteId === 'string' && REGEX_UUID.test(route.query.pazienteId) ? route.query.pazienteId : null
    apriCreazione(tipo, id)
    router.replace({ query: { ...route.query, nuovo: undefined, pazienteId: undefined } })
  },
  { immediate: true },
)

// Le notifiche si considerano "viste in agenda" appena si entra qui, anche se ne arrivano altre a pagina aperta.
const notificheStore = useNotificheStore()
watch(
  () => notificheStore.nonVisteInAgenda,
  (n) => { if (n > 0) void notificheStore.segnaAgendaViste() },
  { immediate: true },
)

// `?evento=<id>&data=YYYY-MM-DD` (dal clic su una notifica): mostra quel giorno e apre il dettaglio dell'evento.
const REGEX_DATA = /^\d{4}-\d{2}-\d{2}$/
const eventoDaAprire = ref<string | null>(null)
watch(
  () => route.query.evento,
  (evento) => {
    if (typeof evento !== 'string' || !REGEX_UUID.test(evento)) return
    const dataEvento = typeof route.query.data === 'string' && REGEX_DATA.test(route.query.data) ? route.query.data : null
    eventoDaAprire.value = evento
    if (dataEvento) riferimento.value = dataEvento
    router.replace({ query: { ...route.query, evento: undefined, data: undefined } })
    // Se era già in elenco (stesso intervallo) il watch su `appuntamenti` non scatta: controllo subito.
    apriEventoInAttesa()
  },
  { immediate: true },
)

function apriEventoInAttesa() {
  if (!eventoDaAprire.value) return
  const trovato = appuntamenti.value.find((a) => a.id === eventoDaAprire.value)
  if (trovato) {
    apriDettaglio(trovato)
    eventoDaAprire.value = null
  }
}
watch(appuntamenti, apriEventoInAttesa)

async function onSalvato() {
  await carica()
}

// --- Conferme ---
const daAnnullare = ref<Appuntamento | null>(null)
const daEliminare = ref<Appuntamento | null>(null)
const annullaAperto = computed({
  get: () => daAnnullare.value !== null,
  set: (v: boolean) => { if (!v) daAnnullare.value = null },
})
const eliminaAperto = computed({
  get: () => daEliminare.value !== null,
  set: (v: boolean) => { if (!v) daEliminare.value = null },
})

// --- Sovrapposizione su "Sposta a domani" ---
const spostamento = ref<{ appuntamento: Appuntamento; conflitti: Appuntamento[] } | null>(null)
const spostamentoAperto = computed({
  get: () => spostamento.value !== null,
  set: (v: boolean) => { if (!v) spostamento.value = null },
})

function richiestaDa(a: Appuntamento, data: string): AppuntamentoRichiesta {
  return {
    tipo: a.tipo,
    pazienteId: a.pazienteId,
    data,
    oraInizio: a.oraInizio,
    durataMinuti: a.durataMinuti,
    testo: a.testo,
    linkVideo: a.tipo === 'VIDEOCONSULTO' && a.linkVideo ? a.linkVideo : null,
    notifica: a.notifica,
    anticipoNotificaMinuti: a.anticipoNotificaMinuti,
  }
}

async function spostaDomani(a: Appuntamento, conferma = false) {
  try {
    await aggiorna(a.id, richiestaDa(a, aggiungiGiorni(a.data, 1)), conferma)
    toast.success('Promemoria spostato a domani.')
    await carica()
  } catch (errore) {
    if (errore instanceof SovrapposizioneError) {
      spostamento.value = { appuntamento: a, conflitti: errore.conflitti }
    } else {
      toast.error('Non è stato possibile spostare il promemoria.')
    }
  }
}

async function onAzione(a: Appuntamento, azione: AzioneAppuntamento) {
  try {
    if (azione === 'FATTO' || azione === 'DA_FARE') {
      await impostaFatto(a.id, azione === 'FATTO')
      toast.success(azione === 'FATTO' ? 'Promemoria segnato come fatto.' : 'Promemoria riportato da fare.')
      await carica()
    } else if (azione === 'APRI_CARTELLA' && a.pazienteId) {
      await router.push(`/pazienti/${a.pazienteId}`)
    } else if (azione === 'APRI_VIDEO' && a.linkVideo) {
      apriLinkVideo(a.linkVideo)
    } else if (azione === 'AVVIA_VISITA' && a.pazienteId) {
      await router.push(`/pazienti/${a.pazienteId}/visite/nuova?appuntamentoId=${a.id}`)
    } else if (azione === 'MODIFICA') {
      apriModifica(a)
    } else if (azione === 'ANNULLA') {
      daAnnullare.value = a
    } else if (azione === 'ELIMINA') {
      daEliminare.value = a
    } else if (azione === 'SPOSTA_DOMANI') {
      await spostaDomani(a)
    }
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  }
}

async function confermaAnnulla() {
  const a = daAnnullare.value
  daAnnullare.value = null
  if (!a) return
  try {
    await annulla(a.id)
    toast.success('Appuntamento annullato.')
    await carica()
  } catch {
    toast.error('Non è stato possibile annullare l\'appuntamento.')
  }
}

async function confermaElimina() {
  const a = daEliminare.value
  daEliminare.value = null
  if (!a) return
  try {
    await elimina(a.id)
    toast.success('Elemento eliminato.')
    await carica()
  } catch {
    toast.error('Non è stato possibile eliminare l\'elemento.')
  }
}

async function confermaSpostamento() {
  const s = spostamento.value
  spostamento.value = null
  if (s) await spostaDomani(s.appuntamento, true)
}

function etichettaConflitto(c: Appuntamento): string {
  const nome = [c.pazienteNome, c.pazienteCognome].filter(Boolean).join(' ')
  const descrizione = nome || c.testo || (c.tipo === 'BLOCCO' ? 'Fascia bloccata' : 'Appuntamento')
  return c.oraInizio ? `${c.oraInizio} · ${descrizione}` : descrizione
}

function titoloEliminazione(a: Appuntamento | null): string {
  return a?.tipo === 'PROMEMORIA' ? 'Eliminare il promemoria?' : 'Eliminare la fascia bloccata?'
}
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Agenda</h1>
        <p class="mt-1 text-sm text-(--fg3)">Visite, videoconsulti, promemoria e fasce bloccate dello studio.</p>
      </div>
      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <Button type="button" class="hover:bg-primary/80" data-test="agenda-aggiungi">
            <CalendarPlus :size="16" /> Aggiungi <ChevronDown :size="14" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="w-52">
          <DropdownMenuItem class="cursor-pointer" data-test="aggiungi-appuntamento" @select="apriCreazione('VISITA_STUDIO')"><CalendarPlus /> Nuovo appuntamento</DropdownMenuItem>
          <DropdownMenuItem class="cursor-pointer" data-test="aggiungi-promemoria" @select="apriCreazione('PROMEMORIA')"><StickyNote /> Nuovo promemoria</DropdownMenuItem>
          <DropdownMenuItem class="cursor-pointer" data-test="aggiungi-blocco" @select="apriCreazione('BLOCCO')"><Lock /> Blocca fascia oraria</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <AgendaToolbar
        :vista="vista"
        :riferimento="riferimento"
        :filtro="filtro"
        @update:vista="vista = $event"
        @update:riferimento="riferimento = $event"
        @update:filtro="filtro = $event"
      />
    </section>

    <div v-if="errore" class="flex flex-col items-center gap-3 rounded-2xl border border-(--bd) bg-(--surf) p-14 text-center">
      <p class="text-(--danger)">Non è stato possibile caricare l'agenda.</p>
      <Button type="button" variant="outline" @click="carica">Riprova</Button>
    </div>

    <div v-else-if="caricamento && appuntamenti.length === 0" class="rounded-2xl border border-(--bd) bg-(--surf) p-14 text-center text-(--fg3)">
      Caricamento…
    </div>

    <template v-else>
      <AgendaGiorno
        v-if="vista === 'GIORNO'"
        :giorno="riferimento"
        :appuntamenti="appuntamentiFiltrati"
        @azione="onAzione"
        @dettaglio="apriDettaglio"
      />
      <AgendaSettimana
        v-else-if="vista === 'SETTIMANA'"
        :riferimento="riferimento"
        :appuntamenti="appuntamentiFiltrati"
        @azione="onAzione"
        @dettaglio="apriDettaglio"
      />
      <AgendaMese
        v-else
        :riferimento="riferimento"
        :appuntamenti="appuntamentiFiltrati"
        @apri-giorno="apriGiorno"
        @dettaglio="apriDettaglio"
      />
    </template>

    <div class="mt-3.5">
      <AgendaLegenda />
    </div>

    <AppuntamentoDettaglioDialog v-model:aperto="dettaglioAperto" :appuntamento="dettaglioAppuntamento" @azione="onAzione" />

    <AppuntamentoDialog
      v-model:aperto="dialogAperto"
      :tipo-iniziale="dialogTipo"
      :appuntamento="dialogAppuntamento"
      :paziente-id-iniziale="dialogPazienteId"
      :data-iniziale="riferimento"
      @salvato="onSalvato"
    />

    <AlertDialog v-model:open="annullaAperto">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Annullare l'appuntamento?</AlertDialogTitle>
          <AlertDialogDescription>L'appuntamento resterà in agenda come annullato e non occuperà più la fascia oraria.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel variant="neutral">Indietro</AlertDialogCancel>
          <AlertDialogAction class="bg-(--danger) hover:bg-(--danger)/80" data-test="conferma-annulla-appuntamento" @click="confermaAnnulla">
            Annulla appuntamento
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog v-model:open="eliminaAperto">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{{ titoloEliminazione(daEliminare) }}</AlertDialogTitle>
          <AlertDialogDescription>L'operazione non è reversibile.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel variant="neutral">Annulla</AlertDialogCancel>
          <AlertDialogAction class="bg-(--danger) hover:bg-(--danger)/80" data-test="conferma-elimina-appuntamento" @click="confermaElimina">
            Elimina
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog v-model:open="spostamentoAperto">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Sovrapposizione</AlertDialogTitle>
          <AlertDialogDescription>Domani l'orario si sovrappone ad altri impegni.</AlertDialogDescription>
        </AlertDialogHeader>
        <ul v-if="spostamento" class="space-y-1 text-sm text-(--fg2)">
          <li v-for="c in spostamento.conflitti" :key="c.id">{{ etichettaConflitto(c) }}</li>
        </ul>
        <AlertDialogFooter>
          <AlertDialogCancel variant="neutral">Cambia orario</AlertDialogCancel>
          <AlertDialogAction class="hover:bg-primary/80" @click="confermaSpostamento">Conferma comunque</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </AppShell>
</template>
