<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { conteggioAttivi } from '@/api/pazienti'
import { elenco as elencoAgenda, type Appuntamento } from '@/api/agenda'
import { cerca as cercaPiani, type PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { Plus, ChevronDown, UserPlus, ClipboardPlus, CalendarPlus, CalendarDays, ChevronRight, Utensils, Video } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { aggiungiGiorni, minutiDaOra, oggiIso } from '@/utils/agenda'
import { eVisitaAttiva, nomePaziente } from '@/utils/agendaStile'
import { formattaDataItalianaConMese } from '@/utils/data'
import { riepilogoOggi } from '@/utils/riepilogoOggi'

/** Quanti giorni avanti guarda la lista "Prossime visite" e quante righe mostra al massimo. */
const GIORNI_PROSSIME_VISITE = 7
const MAX_PROSSIME_VISITE = 6

const auth = useAuthStore()
const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)
const oggi = oggiIso()
/** Appuntamenti da oggi ai prossimi 7 giorni: alimentano sia il riepilogo di oggi sia la lista delle prossime visite. */
const agendaSettimana = ref<Appuntamento[] | null>(null)
const agendaOggi = computed(() => agendaSettimana.value?.filter((a) => a.data === oggi) ?? null)
const pianiInBozza = ref<PianoAlimentareRigaLista[] | null>(null)

const prossimeVisite = computed(() => {
  if (!agendaSettimana.value) return null
  const adesso = new Date()
  const minutiAdesso = adesso.getHours() * 60 + adesso.getMinutes()
  return agendaSettimana.value
    .filter((a) => eVisitaAttiva(a) && a.stato === 'PROGRAMMATA')
    .filter((a) => a.data !== oggi || a.oraInizio === null || minutiDaOra(a.oraInizio) >= minutiAdesso)
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

onMounted(async () => {
  try {
    pazientiAttivi.value = await conteggioAttivi()
  } catch {
    erroreCaricamento.value = true
  }
  try {
    agendaSettimana.value = await elencoAgenda(oggi, aggiungiGiorni(oggi, GIORNI_PROSSIME_VISITE))
  } catch {
    // Senza agenda il banner mostra solo il saluto: il resto della dashboard non dipende da questo.
    agendaSettimana.value = null
  }
  try {
    const pagina = await cercaPiani({ stato: 'BOZZA', ordinaPer: 'aggiornatoIl', direzione: 'desc', dimensione: 5 })
    pianiInBozza.value = pagina.contenuto
  } catch {
    pianiInBozza.value = null
  }
})
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
          <Button as-child class="bg-(--lime) text-(--on-lime) hover:bg-(--lime-d)">
            <router-link to="/agenda" data-test="dashboard-apri-agenda">
              <CalendarDays :size="16" />
              Apri l'agenda
            </router-link>
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button class="group border-transparent bg-(--hero-pill) text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring) transition-all hover:bg-(--hero-pill-hover) aria-expanded:bg-(--hero-pill-hover)">
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
        <div v-if="prossimeVisite === null" class="px-5 pb-5 text-sm text-(--fg4)">Non è stato possibile caricare l'agenda.</div>
        <div v-else-if="prossimeVisite.length === 0" class="px-5 pb-5 text-sm text-(--fg3)">
          Nessuna visita nei prossimi {{ GIORNI_PROSSIME_VISITE }} giorni.
          <router-link to="/agenda?nuovo=visita" class="font-semibold text-(--green) transition-colors hover:text-(--green-d)">Fissa un appuntamento</router-link>
        </div>
        <ul v-else class="divide-y divide-(--div) border-t border-(--div)">
          <li v-for="v in prossimeVisite" :key="v.id">
            <router-link
              :to="v.pazienteId ? `/pazienti/${v.pazienteId}` : '/agenda'"
              class="flex items-center gap-4 px-5 py-3 transition-colors hover:bg-(--hover)"
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

        <section class="rounded-2xl border border-(--bd) bg-(--surf)" data-test="dashboard-piani-bozza">
          <header class="flex items-baseline justify-between gap-3 px-5 pt-4 pb-3">
            <h2 class="font-heading text-lg italic text-(--fg)">Piani in bozza</h2>
            <router-link to="/piani-alimentari" class="group inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) transition-colors hover:text-(--green-d)">Tutti i piani <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
          </header>
          <div v-if="pianiInBozza === null" class="px-5 pb-5 text-sm text-(--fg4)">Non è stato possibile caricare i piani.</div>
          <p v-else-if="pianiInBozza.length === 0" class="px-5 pb-5 text-sm text-(--fg3)">Nessun piano da completare.</p>
          <ul v-else class="divide-y divide-(--div) border-t border-(--div)">
            <li v-for="p in pianiInBozza" :key="p.id">
              <router-link :to="`/piani-alimentari/${p.id}`" class="block px-5 py-2.5 transition-colors hover:bg-(--hover)">
                <p class="truncate text-sm font-semibold text-(--fg)">{{ p.pazienteNomeCompleto }}</p>
                <p class="truncate text-xs text-(--fg3)">{{ p.nome }}</p>
              </router-link>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </AppShell>
</template>
