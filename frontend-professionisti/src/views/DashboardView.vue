<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { conteggioAttivi, cerca as cercaPazienti, type Paziente } from '@/api/pazienti'
import { elenco as elencoAgenda, type Appuntamento } from '@/api/agenda'
import { cerca as cercaPiani, type PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { Plus, ChevronDown, UserPlus, ClipboardPlus, CalendarPlus, CalendarDays, CalendarX2, ChevronRight, Play, Utensils, Video } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import DaFareCard, { type GruppoDaFare } from '@/components/dashboard/DaFareCard.vue'
import { useAuthStore } from '@/stores/auth'
import { aggiungiGiorni, minutiDaOra, oggiIso } from '@/utils/agenda'
import { eVisitaAttiva, nomePaziente } from '@/utils/agendaStile'
import { formattaDataItalianaConMese } from '@/utils/data'
import { riepilogoOggi } from '@/utils/riepilogoOggi'

/** Quanti giorni avanti guarda la lista "Prossime visite" e quante righe mostra al massimo. */
const GIORNI_PROSSIME_VISITE = 7
const MAX_PROSSIME_VISITE = 6
/** Righe per gruppo nella card "Da fare": oltre compare "Vedi tutti". */
const MAX_DA_FARE = 3

const auth = useAuthStore()
const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)
const oggi = oggiIso()
/** Appuntamenti da oggi ai prossimi 7 giorni: alimentano sia il riepilogo di oggi sia la lista delle prossime visite. */
const agendaSettimana = ref<Appuntamento[] | null>(null)
const agendaOggi = computed(() => agendaSettimana.value?.filter((a) => a.data === oggi) ?? null)
const pianiInBozza = ref<GruppoDaFare<PianoAlimentareRigaLista>>({ voci: [], totale: 0 })
const pianiInScadenza = ref<GruppoDaFare<PianoAlimentareRigaLista>>({ voci: [], totale: 0 })
const maiInvitati = ref<GruppoDaFare<Paziente>>({ voci: [], totale: 0 })
const caricamentoDaFare = ref(true)

/** Dopo "Invita" il paziente esce dal gruppo senza ricaricare la pagina. */
function onInvitato(id: string) {
  const g = maiInvitati.value
  if (!g.voci) return
  maiInvitati.value = { voci: g.voci.filter((p) => p.id !== id), totale: Math.max(g.totale - 1, 0) }
}

const prossimeVisite = computed(() => {
  if (!agendaSettimana.value) return null
  const adesso = new Date()
  const minutiAdesso = adesso.getHours() * 60 + adesso.getMinutes()
  return agendaSettimana.value
    .filter((a) => eVisitaAttiva(a) && a.stato === 'PROGRAMMATA')
    // Una visita di oggi resta in lista (con "Avvia visita") finché non è finita, non solo finché non è iniziata:
    // chi apre la Dashboard alle 10:05 deve poter avviare la visita delle 10:00.
    .filter((a) => a.data !== oggi || a.oraInizio === null || minutiDaOra(a.oraInizio) + (a.durataMinuti ?? 60) > minutiAdesso)
    .sort((x, y) => (x.data + (x.oraInizio ?? '')).localeCompare(y.data + (y.oraInizio ?? '')))
    .slice(0, MAX_PROSSIME_VISITE)
})

function etichettaGiorno(iso: string): string {
  if (iso === oggi) return 'Oggi'
  if (iso === aggiungiGiorni(oggi, 1)) return 'Domani'
  const giorno = new Intl.DateTimeFormat('it-IT', { weekday: 'short', timeZone: 'UTC' }).format(new Date(`${iso}T12:00:00Z`))
  return `${giorno.replace(/^./, (c) => c.toUpperCase())} ${formattaDataItalianaConMese(iso).slice(0, 6)}`
}

const dataOggi = new Intl.DateTimeFormat('it-IT', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})
  .format(new Date())
  .replace(/^./, (c) => c.toUpperCase())

const riepilogo = computed(() => (agendaOggi.value ? riepilogoOggi(agendaOggi.value) : null))

/** Pagina della ricerca → gruppo di "Da fare"; una risposta mancante o senza contenuto vale come errore del gruppo. */
function gruppo<T>(esito: PromiseSettledResult<{ contenuto: T[]; totaleElementi: number } | undefined>): GruppoDaFare<T> {
  if (esito.status !== 'fulfilled' || !esito.value?.contenuto) return { voci: null, totale: 0 }
  return { voci: esito.value.contenuto, totale: esito.value.totaleElementi }
}

async function caricaConteggio() {
  try {
    pazientiAttivi.value = await conteggioAttivi()
  } catch {
    erroreCaricamento.value = true
  }
}

async function caricaAgenda() {
  try {
    agendaSettimana.value = await elencoAgenda(oggi, aggiungiGiorni(oggi, GIORNI_PROSSIME_VISITE))
  } catch {
    // Senza agenda il banner mostra solo il saluto: il resto della dashboard non dipende da questo.
    agendaSettimana.value = null
  }
}

/** Le tre liste di "Da fare" si caricano insieme; una che fallisce non blocca le altre. */
async function caricaDaFare() {
  const [scadenza, bozze, inviti] = await Promise.allSettled([
    cercaPiani({ stato: 'ATTIVO', dataFineDa: oggi, dataFineA: aggiungiGiorni(oggi, 7), ordinaPer: 'dataFine', direzione: 'asc', dimensione: MAX_DA_FARE }),
    cercaPiani({ stato: 'BOZZA', ordinaPer: 'aggiornatoIl', direzione: 'desc', dimensione: MAX_DA_FARE }),
    cercaPazienti({ statoAccount: 'MAI_INVITATO', archiviato: false, ordinaPer: 'cognome', direzione: 'asc', dimensione: MAX_DA_FARE }),
  ])
  pianiInScadenza.value = gruppo(scadenza)
  pianiInBozza.value = gruppo(bozze)
  maiInvitati.value = gruppo(inviti)
  caricamentoDaFare.value = false
}

// I tre blocchi sono indipendenti: si caricano in parallelo, non uno dopo l'altro.
onMounted(() => Promise.all([caricaConteggio(), caricaAgenda(), caricaDaFare()]))
</script>

<template>
  <AppShell>
    <!-- Banner d'apertura: l'unico blocco pieno della pagina, con il lime solo sull'azione principale. -->
    <section
      class="rounded-2xl border border-(--hero-d) bg-(--hero) px-6 py-5 text-(--on-hero)"
      data-test="dashboard-hero"
    >
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="min-w-60 flex-1">
          <h1 class="font-heading text-3xl italic">Ciao, {{ auth.professionista?.nome }}</h1>
          <p class="mt-1 text-sm text-(--on-hero-2)">{{ dataOggi }}</p>
          <p v-if="riepilogo" class="mt-3 text-[15px] font-medium" data-test="dashboard-riepilogo">{{ riepilogo.frase }}</p>
          <p v-if="riepilogo?.prossima" class="mt-0.5 text-sm text-(--on-hero-2)" data-test="dashboard-prossima">
            Prossima visita alle {{ riepilogo.prossima.oraInizio }} · {{ nomePaziente(riepilogo.prossima) }}
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <Button as-child variant="accent">
            <router-link to="/agenda" data-test="dashboard-apri-agenda">
              <CalendarDays :size="16" />
              Apri l'agenda
            </router-link>
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="hero" class="group">
                <Plus :size="16" />
                Crea nuovo
                <ChevronDown :size="14" class="transition-transform duration-200 group-data-[state=open]:rotate-180" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-64 p-1.5">
              <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5 cursor-pointer">
                <router-link to="/pazienti/nuovo">
                  <UserPlus :size="16" />
                  Nuovo paziente
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5 cursor-pointer">
                <router-link to="/agenda?nuovo=visita">
                  <CalendarPlus :size="16" />
                  Nuovo appuntamento
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5 cursor-pointer">
                <router-link to="/pazienti/visite/nuova">
                  <ClipboardPlus :size="16" />
                  Nuova visita
                </router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5 cursor-pointer">
                <router-link to="/piani-alimentari/nuovo">
                  <Utensils :size="16" />
                  Nuovo piano alimentare
                </router-link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </section>

    <div class="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
      <!-- Prossime visite: il blocco principale, la cosa che il professionista guarda per prima -->
      <section class="rounded-2xl border border-(--bd) bg-(--surf)" data-test="dashboard-prossime-visite">
        <header class="flex items-baseline justify-between gap-3 px-5 pt-4 pb-3">
          <h2 class="font-heading text-lg italic text-(--fg)">Prossime visite</h2>
          <router-link to="/agenda" class="group inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) transition-colors hover:text-(--green-d)">Vedi l'agenda <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
        </header>
        <div v-if="prossimeVisite === null" class="px-5 pb-5 text-sm text-(--fg3)">Non è stato possibile caricare l'agenda.</div>
        <StatoVuoto
          v-else-if="prossimeVisite.length === 0"
          compatto
          :icona="CalendarX2"
          :titolo="`Nessuna visita nei prossimi ${GIORNI_PROSSIME_VISITE} giorni`"
          testo="Le visite fissate in agenda compaiono qui."
        >
          <Button as-child size="sm">
            <router-link to="/agenda?nuovo=visita" data-test="dashboard-fissa-appuntamento"><CalendarPlus :size="14" /> Fissa un appuntamento</router-link>
          </Button>
        </StatoVuoto>
        <ul v-else class="divide-y divide-(--div) border-t border-(--div)">
          <li v-for="v in prossimeVisite" :key="v.id" class="flex items-center transition-colors hover:bg-(--hover)">
            <router-link
              :to="v.pazienteId ? `/pazienti/${v.pazienteId}` : '/agenda'"
              class="flex min-w-0 flex-1 items-center gap-4 py-3 pl-5"
              :class="v.data === oggi && v.pazienteId ? 'pr-2' : 'pr-5'"
            >
              <div class="w-24 shrink-0">
                <p class="text-sm font-semibold text-(--fg)">{{ etichettaGiorno(v.data) }}</p>
                <p class="text-xs text-(--fg3) tabular-nums">{{ v.oraInizio ?? 'Senza orario' }}</p>
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-semibold text-(--fg)">{{ nomePaziente(v) || 'Visita' }}</p>
                <p v-if="v.testo" class="truncate text-xs text-(--fg3)">{{ v.testo }}</p>
              </div>
              <span v-if="v.tipo === 'VIDEOCONSULTO'" class="inline-flex shrink-0 items-center gap-1 text-xs text-(--fg3)">
                <Video :size="14" /> Videoconsulto
              </span>
            </router-link>
            <!-- Le visite di oggi si avviano da qui: stesso flusso di "Avvia visita" in agenda (l'appuntamento si conclude al salvataggio). -->
            <Button v-if="v.data === oggi && v.pazienteId" as-child size="sm" class="mr-5 shrink-0">
              <router-link :to="`/pazienti/${v.pazienteId}/visite/nuova?appuntamentoId=${v.id}`" data-test="dashboard-avvia-visita">
                <Play :size="14" /> Avvia visita
              </router-link>
            </Button>
          </li>
        </ul>
      </section>

      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
            <p class="text-xs font-semibold text-(--fg3)">Pazienti attivi</p>
            <p class="mt-1 text-3xl font-semibold text-(--fg) tabular-nums">
              {{ erroreCaricamento ? '—' : (pazientiAttivi ?? '…') }}
            </p>
          </div>
          <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
            <p class="text-xs font-semibold text-(--fg3)">Visite oggi</p>
            <p class="mt-1 text-3xl font-semibold text-(--fg) tabular-nums" data-test="dashboard-visite-oggi">
              {{ riepilogo ? riepilogo.visite : '—' }}
            </p>
          </div>
        </div>

        <div v-if="caricamentoDaFare" class="h-40 animate-pulse rounded-2xl bg-(--hover)" data-test="dashboard-da-fare-skeleton" />
        <DaFareCard v-else :in-scadenza="pianiInScadenza" :bozze="pianiInBozza" :mai-invitati="maiInvitati" @invitato="onInvitato" />
      </div>
    </div>
  </AppShell>
</template>
