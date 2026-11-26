<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { conteggioAttivi, cerca as cercaPazienti, dettaglio as dettaglioPaziente, type Paziente } from '@/api/pazienti'
import { elenco as elencoAgenda, type Appuntamento } from '@/api/agenda'
import { cerca as cercaPiani, type PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { Plus, ChevronDown, UserPlus, ClipboardPlus, CalendarPlus, CalendarDays, CalendarX2, ChevronRight, Play, Utensils, Video, StickyNote, RotateCcw } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import DaFareCard, { type GruppoDaFare } from '@/components/dashboard/DaFareCard.vue'
import SettimanaStrip from '@/components/dashboard/SettimanaStrip.vue'
import PazientiDaRichiamareCard from '@/components/dashboard/PazientiDaRichiamareCard.vue'
import { ETICHETTE_OBIETTIVO } from '@/utils/visita'
import { useAuthStore } from '@/stores/auth'
import { aggiungiGiorni, intervalloVisibile, minutiDaOra, oggiIso } from '@/utils/agenda'
import { eVisitaAttiva, nomePaziente } from '@/utils/agendaStile'
import { formattaDataItalianaConMese } from '@/utils/data'
import { TONI_STATO } from '@/utils/toniStato'
import { riepilogoOggi } from '@/utils/riepilogoOggi'

/** Numero massimo di visite nella card "Prossimi impegni". */
const MAX_PROSSIME_VISITE = 6
/** Righe per gruppo nella card "Da fare": oltre compare "Vedi tutti". */
const MAX_DA_FARE = 3

const auth = useAuthStore()
const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)
const erroreAgenda = ref(false)
const oggi = oggiIso()
const settimanaCorrente = intervalloVisibile('SETTIMANA', oggi)
const fineFinestraScadenze = settimanaCorrente.a
/** Appuntamenti della settimana corrente, da lunedì a domenica. */
const agendaSettimana = ref<Appuntamento[] | null>(null)
const agendaOggi = computed(() => agendaSettimana.value?.filter((a) => a.data === oggi) ?? null)
const promemoriaDaFare = computed(() => agendaSettimana.value?.filter((a) => a.data >= oggi && a.tipo === 'PROMEMORIA' && !a.fatto) ?? [])
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
    .filter((a) => a.data >= oggi && eVisitaAttiva(a) && a.stato === 'PROGRAMMATA')
    // Una visita di oggi resta in lista (con "Avvia visita") finché non è finita, non solo finché non è iniziata:
    // chi apre la Dashboard alle 10:05 deve poter avviare la visita delle 10:00.
    .filter((a) => a.data !== oggi || a.oraInizio === null || minutiDaOra(a.oraInizio) + (a.durataMinuti ?? 60) > minutiAdesso)
    .sort((x, y) => (x.data + (x.oraInizio ?? '')).localeCompare(y.data + (y.oraInizio ?? '')))
    .slice(0, MAX_PROSSIME_VISITE)
})

/** I promemoria da fare condividono la sequenza delle visite, senza incidere sul limite delle visite. */
const gruppiAgenda = computed(() => {
  if (!prossimeVisite.value) return null
  const eventi = [...prossimeVisite.value, ...promemoriaDaFare.value].sort((x, y) =>
    x.data.localeCompare(y.data) || (x.oraInizio ?? '24:00').localeCompare(y.oraInizio ?? '24:00'),
  )
  const gruppi: { data: string; eventi: Appuntamento[] }[] = []
  for (const evento of eventi) {
    const ultimo = gruppi[gruppi.length - 1]
    if (ultimo?.data === evento.data) ultimo.eventi.push(evento)
    else gruppi.push({ data: evento.data, eventi: [evento] })
  }
  return gruppi
})

/** Visite della settimana corrente, le stesse contate nella striscia. */
const visiteSettimana = computed(() => {
  if (!agendaSettimana.value) return null
  return agendaSettimana.value.filter(eVisitaAttiva).length
})

/**
 * Contesto dei pazienti delle prossime visite (obiettivo, piano): quello che serve per prepararsi.
 * Una richiesta per paziente, al massimo `MAX_PROSSIME_VISITE`; se una fallisce la riga resta senza contesto.
 */
const pazientiVisite = ref<Record<string, Paziente>>({})
async function caricaContestoPazienti() {
  const ids = [...new Set((prossimeVisite.value ?? []).map((v) => v.pazienteId).filter((id): id is string => !!id))]
  const esiti = await Promise.allSettled(ids.map((id) => dettaglioPaziente(id)))
  const mappa: Record<string, Paziente> = {}
  esiti.forEach((e, i) => {
    if (e.status === 'fulfilled' && e.value) mappa[ids[i]] = e.value
  })
  pazientiVisite.value = mappa
}

function obiettivoPaziente(id: string | null): string | null {
  const p = id ? pazientiVisite.value[id] : undefined
  return p?.obiettivoUltimaVisita ? ETICHETTE_OBIETTIVO[p.obiettivoUltimaVisita] : null
}

/** "Ultima visita 25 nov", oppure "Prima visita" se il paziente non ne ha ancora: dice a che punto è il percorso. */
function ultimaVisitaPaziente(id: string | null): string | null {
  const p = id ? pazientiVisite.value[id] : undefined
  if (!p) return null
  return p.dataUltimaVisita ? `Ultima visita ${formattaDataItalianaConMese(p.dataUltimaVisita).slice(0, 6)}` : 'Prima visita'
}

/** Stato del piano più recente, nei toni di stato comuni: bozza da completare, attivo, oppure nessun piano. */
function pianoPaziente(id: string | null): { testo: string; classi: string } | null {
  const p = id ? pazientiVisite.value[id] : undefined
  if (!p) return null
  if (p.pianoStato === 'BOZZA') return { testo: 'Piano in bozza', classi: TONI_STATO.daFare }
  if (p.pianoStato === 'ATTIVO') {
    return { testo: p.pianoDataFine ? `Piano fino al ${formattaDataItalianaConMese(p.pianoDataFine).slice(0, 6)}` : 'Piano attivo', classi: TONI_STATO.attivo }
  }
  return { testo: 'Nessun piano attivo', classi: TONI_STATO.neutro }
}

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
  erroreCaricamento.value = false
  try {
    pazientiAttivi.value = await conteggioAttivi()
  } catch {
    erroreCaricamento.value = true
  }
}

async function caricaAgenda() {
  erroreAgenda.value = false
  try {
    agendaSettimana.value = await elencoAgenda(settimanaCorrente.da, settimanaCorrente.a)
    await caricaContestoPazienti()
  } catch {
    // Senza agenda il banner mostra solo il saluto: il resto della dashboard non dipende da questo.
    agendaSettimana.value = null
    erroreAgenda.value = true
  }
}

/** Le tre liste di "Da fare" si caricano insieme; una che fallisce non blocca le altre. */
async function caricaDaFare() {
  const [scadenza, bozze, inviti] = await Promise.allSettled([
    cercaPiani({ stato: 'ATTIVO', dataFineDa: oggi, dataFineA: fineFinestraScadenze, ordinaPer: 'dataFine', direzione: 'asc', dimensione: MAX_DA_FARE }),
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
                Nuovo
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
                  Nuovo piano
                </router-link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </section>

    <!-- La settimana a colpo d'occhio, subito sotto il saluto: ogni giorno apre l'agenda su quella data. -->
    <SettimanaStrip class="mt-4" :oggi="oggi" :appuntamenti="agendaSettimana" :errore="erroreAgenda" />

    <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
      <div class="min-w-0 space-y-4">
        <section class="overflow-clip rounded-2xl border border-(--bd) bg-(--surf)" data-test="dashboard-prossime-visite">
          <header class="flex items-baseline justify-between gap-3 px-5 pt-4 pb-3">
            <h2 class="font-heading text-lg italic text-(--fg)">Prossimi impegni</h2>
            <Button as-child variant="ghost-primary" size="sm" class="group font-semibold">
              <router-link to="/agenda">Vedi l'agenda <ChevronRight aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
            </Button>
          </header>
          <div v-if="erroreAgenda" class="flex flex-wrap items-center justify-between gap-2 px-5 pb-5">
            <p class="text-sm text-(--fg3)" role="status">Non è stato possibile caricare l'agenda.</p>
            <Button type="button" variant="neutral" size="sm" aria-label="Riprova a caricare gli impegni" @click="caricaAgenda"><RotateCcw /> Riprova</Button>
          </div>
          <p v-else-if="gruppiAgenda === null" class="px-5 pb-5 text-sm text-(--fg3)" role="status">Caricamento degli impegni…</p>
          <StatoVuoto
            v-else-if="gruppiAgenda.length === 0"
            compatto
            :icona="CalendarX2"
            titolo="Nessun altro impegno questa settimana"
            testo="Le visite e i promemoria da fare in agenda compaiono qui."
          >
            <Button as-child size="sm">
              <router-link to="/agenda?nuovo=visita" data-test="dashboard-fissa-appuntamento"><CalendarPlus :size="14" /> Nuovo appuntamento</router-link>
            </Button>
          </StatoVuoto>
          <!-- Raggruppate per giorno; ogni riga dice anche obiettivo e stato del piano, per prepararsi alla visita. -->
          <div v-else class="border-t border-(--div)">
            <section v-for="g in gruppiAgenda" :key="g.data" :aria-label="etichettaGiorno(g.data)">
              <h3 class="border-b border-(--div) bg-(--soft) px-5 py-1.5 text-xs font-semibold text-(--fg2)">{{ etichettaGiorno(g.data) }}</h3>
              <ul class="divide-y divide-(--div)">
                <li v-for="v in g.eventi" :key="v.id" class="group flex items-center transition-colors hover:bg-(--hover)">
                  <router-link
                    :to="{ path: '/agenda', query: { evento: v.id, data: v.data } }"
                    class="flex min-w-0 flex-1 items-center gap-4 py-3 pl-5"
                    :class="v.tipo !== 'PROMEMORIA' && v.data === oggi && v.pazienteId ? 'pr-2' : 'pr-5'"
                  >
                    <span class="w-12 shrink-0 text-sm font-semibold tabular-nums text-(--green-d)">{{ v.oraInizio ?? '—' }}</span>
                    <div v-if="v.tipo === 'PROMEMORIA'" class="min-w-0 flex-1">
                      <p class="wrap-break-word text-sm font-semibold text-(--green-d)">{{ v.testo || 'Promemoria' }}</p>
                      <p class="flex items-center gap-1 text-xs text-(--fg3)">
                        <StickyNote :size="14" aria-hidden="true" /> {{ v.oraInizio ? 'Promemoria' : 'Promemoria senza orario' }}
                      </p>
                    </div>
                    <div v-else class="min-w-0 flex-1">
                      <p class="truncate text-sm font-semibold text-(--green-d)">{{ nomePaziente(v) || 'Visita' }}</p>
                      <p class="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-xs text-(--fg3)">
                        <span v-if="obiettivoPaziente(v.pazienteId)">{{ obiettivoPaziente(v.pazienteId) }}</span>
                        <span v-if="ultimaVisitaPaziente(v.pazienteId)">{{ ultimaVisitaPaziente(v.pazienteId) }}</span>
                        <span
                          v-if="pianoPaziente(v.pazienteId)"
                          class="inline-flex rounded-full px-2 py-px text-[11px] font-bold"
                          :class="pianoPaziente(v.pazienteId)!.classi"
                          data-test="dashboard-visita-piano"
                        >{{ pianoPaziente(v.pazienteId)!.testo }}</span>
                        <span v-if="v.testo" class="truncate">{{ v.testo }}</span>
                      </p>
                    </div>
                    <span v-if="v.tipo === 'VIDEOCONSULTO'" class="inline-flex shrink-0 items-center gap-1 text-xs text-(--fg3)">
                      <Video :size="14" /> Videoconsulto
                    </span>
                    <ChevronRight :size="16" aria-hidden="true" class="shrink-0 text-(--fg4) transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" />
                  </router-link>
                  <!-- Le visite di oggi si avviano da qui: stesso flusso di "Avvia visita" in agenda (l'appuntamento si conclude al salvataggio). -->
                  <Button v-if="v.tipo !== 'PROMEMORIA' && v.data === oggi && v.pazienteId" as-child size="sm" class="mr-5 shrink-0">
                    <router-link :to="`/pazienti/${v.pazienteId}/visite/nuova?appuntamentoId=${v.id}`" data-test="dashboard-avvia-visita">
                      <Play :size="14" /> Avvia visita
                    </router-link>
                  </Button>
                </li>
              </ul>
            </section>
          </div>
        </section>

        <PazientiDaRichiamareCard :oggi="oggi" />
      </div>

      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
            <p class="text-xs font-semibold text-(--fg3)">Pazienti attivi</p>
            <p class="mt-1 text-3xl font-semibold text-(--green-d) tabular-nums">
              {{ erroreCaricamento ? '—' : (pazientiAttivi ?? '…') }}
            </p>
            <div v-if="erroreCaricamento" class="mt-2 space-y-2">
              <p class="text-xs text-(--fg3)" role="status">Conteggio non disponibile.</p>
              <Button type="button" variant="neutral" size="sm" aria-label="Riprova a caricare il conteggio dei pazienti" @click="caricaConteggio"><RotateCcw /> Riprova</Button>
            </div>
          </div>
          <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
            <p class="text-xs font-semibold text-(--fg3)">Visite Lun-Dom</p>
            <p class="mt-1 text-3xl font-semibold text-(--green-d) tabular-nums" data-test="dashboard-visite-settimana">
              {{ erroreAgenda ? '—' : (visiteSettimana ?? '…') }}
            </p>
            <p class="mt-1 text-xs text-(--fg3)">Programmate e concluse</p>
          </div>
        </div>

        <div v-if="caricamentoDaFare" class="h-40 animate-pulse rounded-2xl bg-(--hover)" data-test="dashboard-da-fare-skeleton" />
        <DaFareCard v-else :in-scadenza="pianiInScadenza" :bozze="pianiInBozza" :mai-invitati="maiInvitati" :oggi="oggi" :fine-finestra-scadenze="fineFinestraScadenze" @invitato="onInvitato" />
      </div>
    </div>
  </AppShell>
</template>
