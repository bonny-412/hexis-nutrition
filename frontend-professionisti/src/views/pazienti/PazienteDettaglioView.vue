<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, deArchivia, visite as caricaVisite, type Paziente, type Visita } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
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
import type { AcceptableValue } from 'reka-ui'
import { TabsContent, TabsList, TabsRoot, TabsTrigger } from 'reka-ui'
import PazienteTabPanoramica from '@/components/pazienti/PazienteTabPanoramica.vue'
import PazienteTabStoricoVisite from '@/components/pazienti/PazienteTabStoricoVisite.vue'
import PazienteTabConfrontoVisite from '@/components/pazienti/PazienteTabConfrontoVisite.vue'
import PazienteTabPianiAlimentari from '@/components/pazienti/PazienteTabPianiAlimentari.vue'
import PazienteStatoAccount from '@/components/pazienti/PazienteStatoAccount.vue'
import ModificaPazienteDialog from '@/components/pazienti/ModificaPazienteDialog.vue'
import { prepareAndamento } from '@/utils/andamento'
import { formattaDataItalianaConMese } from '@/utils/data'
import { ETICHETTE_OBIETTIVO, formattaNumero } from '@/utils/visita'
import { CLASSE_ESITO, esitoVariazione, type ParametroClinico } from '@/utils/obiettivo'
import { inizialiPaziente } from '@/utils/paziente'
import { AlertCircle, Archive, ArchiveRestore, ArrowDown, ArrowLeft, ArrowUp, CalendarCheck, CalendarPlus, CalendarX2, ClipboardPlus, Dumbbell, MoreHorizontal, Pencil, Percent, RotateCcw, Ruler, Scale, Utensils, X } from '@lucide/vue'

const TAB_DEFS = [
  { id: 'panoramica', label: 'Panoramica' },
  { id: 'storico', label: 'Storico visite' },
  { id: 'confronto', label: 'Confronto visite' },
  { id: 'piani', label: 'Piani alimentari' },
] as const

type TabId = (typeof TAB_DEFS)[number]['id']

const route = useRoute()

// Stato Paziente
const caricamento = ref(true)
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)
const mostraModificaAnagrafica = ref(false)
const confermaDeArchiviaAperta = ref(false)
const deArchiviazioneInCorso = ref(false)

// Stato Visite e Andamento
const visite = ref<Visita[]>([])
const erroreVisite = ref(false)
const visiteInCaricamento = ref(true)

// Tab attivo nella sezione clinica
const tabAttivo = ref<TabId>('panoramica')

function onCambiaTab(valore: AcceptableValue) {
  tabAttivo.value = valore as TabId
}

const andamento = computed(() => prepareAndamento(visite.value))
const primaVisita = computed(() => (visite.value.length > 0 ? visite.value[0] : null))
const ultimaVisita = computed(() => (visite.value.length > 0 ? visite.value[visite.value.length - 1] : null))

/** Colore della variazione "vs prec.": verde o rosso solo se l'obiettivo dell'ultima visita dà una direzione. */
function classeDelta(parametro: ParametroClinico, delta: number | null): string {
  return CLASSE_ESITO[esitoVariazione(ultimaVisita.value?.obiettivo, parametro, delta)]
}

async function carica() {
  caricamento.value = true
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare il paziente.'
    }
  } finally {
    caricamento.value = false
  }
}

async function caricaAndamento() {
  try {
    visite.value = await caricaVisite(route.params.id as string)
  } catch {
    erroreVisite.value = true
  } finally {
    visiteInCaricamento.value = false
  }
}

function onPazienteAggiornato(aggiornato: Paziente) {
  paziente.value = aggiornato
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
    toast.success('Invito inviato.')
  } catch {
    toast.error("Non è stato possibile inviare l'invito.")
  } finally {
    invitoInCorso.value = false
  }
}

async function onDeArchivia() {
  if (!paziente.value) return
  deArchiviazioneInCorso.value = true
  try {
    await deArchivia(paziente.value.id)
    paziente.value.archiviato = false
    toast.success('Paziente ripristinato.')
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  } finally {
    deArchiviazioneInCorso.value = false
    confermaDeArchiviaAperta.value = false
  }
}

onMounted(() => {
  carica()
  caricaAndamento()
})
</script>

<template>
  <AppShell>
    <!-- Link Torna Indietro -->
    <div>
      <Button as-child variant="ghost-primary" size="sm" class="group font-semibold">
        <router-link to="/pazienti">
          <ArrowLeft aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
          Torna alla lista pazienti
        </router-link>
      </Button>
    </div>

    <!-- Stato Errore Caricamento -->
    <div
      v-if="erroreCaricamento"
      class="flex flex-col items-center justify-center rounded-2xl border border-(--bd) bg-(--surf) p-12 text-center"
    >
      <AlertCircle :size="40" class="text-(--danger) mb-3" />
      <h2 class="font-heading text-xl italic text-(--fg)">Errore di caricamento</h2>
      <p class="mt-1 text-sm text-(--fg3)">{{ erroreCaricamento }}</p>
      <Button variant="neutral" size="sm" class="mt-4" @click="carica"><RotateCcw /> Riprova</Button>
    </div>

    <!-- Skeleton Loading -->
    <div v-else-if="caricamento" class="space-y-6">
      <div class="space-y-2">
        <div class="h-8 w-64 animate-pulse rounded bg-(--hover)" />
        <div class="h-4 w-96 animate-pulse rounded bg-(--hover)" />
      </div>
      <div class="h-40 animate-pulse rounded-2xl bg-(--hover)" />
      <div class="grid gap-6 md:grid-cols-2">
        <div class="h-48 animate-pulse rounded-2xl bg-(--hover)" />
        <div class="h-48 animate-pulse rounded-2xl bg-(--hover)" />
      </div>
      <div class="h-64 animate-pulse rounded-2xl bg-(--hover)" />
    </div>

    <!-- Contenuto Principale -->
    <div v-else-if="paziente" class="space-y-6 mt-4">
      <!-- 1. INTESTAZIONE PAZIENTE -->
      <!-- Da lg i pulsanti stanno sempre a destra: prima andavano a capo o no secondo la lunghezza delle pillole. -->
      <div class="flex flex-col gap-4 rounded-2xl border border-(--hero-d) bg-(--hero) px-5 py-4 text-(--on-hero) sm:flex-row sm:flex-wrap sm:items-start sm:justify-between lg:flex-nowrap" data-test="paziente-hero">
        <div class="flex min-w-0 items-start gap-4">
          <span
            class="flex size-10 shrink-0 items-center justify-center rounded-xl font-heading text-lg font-semibold select-none sm:size-14 sm:rounded-2xl sm:text-2xl"
            :class="paziente.archiviato ? 'bg-(--hero-pill) text-(--fg3) ring-1 ring-(--hero-pill-ring)' : 'bg-(--hero-pill) text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring)'"
          >
            {{ inizialiPaziente(paziente.nome, paziente.cognome) }}
          </span>
          <div>
            <div class="flex flex-wrap items-center gap-2.5">
              <h1 class="font-heading text-xl italic text-(--on-hero) sm:text-2xl">{{ paziente.nome }} {{ paziente.cognome }}</h1>
              <Button
                v-if="!paziente.archiviato"
                variant="ghost"
                size="icon"
                class="text-(--on-hero) hover:bg-white/15 hover:text-(--on-hero)"
                title="Modifica dati anagrafici"
                aria-label="Modifica dati anagrafici"
                @click="mostraModificaAnagrafica = true"
              >
                <Pencil :size="14" />
              </Button>
            </div>
            <div class="mt-1 text-sm text-(--on-hero-2) flex w-full lg:flex-wrap lg:flex-row flex-col lg:items-center empty:hidden">
              <!-- Da mobile resta solo l'obiettivo: email e data d'inizio sono nel profilo, la prossima visita nel badge sotto. -->
              <span class="hidden sm:inline">{{ paziente.email }}</span>
              <span class="hidden lg:flex mx-1"> · </span>
              <span v-if="ultimaVisita">Obiettivo attuale: {{ ETICHETTE_OBIETTIVO[ultimaVisita.obiettivo] }}</span>
              <span class="hidden lg:flex mx-1"> · </span>
              <span v-if="primaVisita" class="hidden sm:inline">paziente dal {{ formattaDataItalianaConMese(primaVisita.dataVisita) }}</span>
            </div>
            <div class="mt-2.5 flex flex-wrap items-center gap-2">
              <PazienteStatoAccount :paziente="paziente" hero :invito-in-corso="invitoInCorso" @invita="onInvita" />
              <span
                v-if="paziente.archiviato"
                class="inline-flex items-center gap-1.5 rounded-full bg-(--hero-pill) px-2.5 py-1 text-xs font-bold text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring)"
              >
                <Archive :size="16" />
                Paziente archiviato
              </span>
              <!-- Pillole cliccabili: la prossima visita apre quel giorno in Agenda; senza visita, il nuovo appuntamento. -->
              <router-link
                v-else-if="paziente.prossimaVisita"
                :to="{ name: 'agenda', query: { data: paziente.prossimaVisita.slice(0, 10) } }"
                data-test="pillola-prossima-visita"
                class="inline-flex items-center gap-1.5 rounded-full bg-(--hero-pill) px-2.5 py-1 text-xs font-bold text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring) transition-colors hover:bg-(--hero-pill-hover)"
              >
                <CalendarCheck :size="16" />
                Prossima visita: {{ formattaDataItalianaConMese(paziente.prossimaVisita.slice(0, 10)) }} · {{ paziente.prossimaVisita.slice(11, 16) }}
              </router-link>
              <router-link
                v-else
                :to="`/agenda?nuovo=visita&pazienteId=${paziente.id}`"
                data-test="pillola-nessuna-visita"
                class="inline-flex items-center gap-1.5 rounded-full bg-(--warn-bg) px-2.5 py-1 text-xs font-bold text-(--warn-fg) transition-[filter] hover:brightness-95"
                title="Nuovo appuntamento"
              >
                <CalendarX2 :size="16" />
                Nessuna visita programmata
              </router-link>
            </div>
          </div>
        </div>

        <div v-if="!paziente.archiviato" class="flex items-center gap-2 sm:flex-wrap lg:flex-none lg:flex-nowrap">
          <!-- Da mobile un'azione principale e le altre nel menu; da 640 px i tre pulsanti. -->
          <Button as-child variant="hero" class="hidden sm:inline-flex">
            <router-link :to="`/piani-alimentari/nuovo?pazienteId=${paziente.id}`">
              <Utensils :size="16" />
              <span>Nuovo piano</span>
            </router-link>
          </Button>
          <Button as-child variant="hero" class="hidden sm:inline-flex">
            <router-link :to="`/agenda?nuovo=visita&pazienteId=${paziente.id}`">
              <CalendarPlus :size="16" />
              <span>Nuovo appuntamento</span>
            </router-link>
          </Button>
          <Button as-child variant="accent" class="flex-1 sm:flex-none">
            <router-link :to="`/pazienti/${paziente.id}/visite/nuova`">
              <ClipboardPlus :size="16" />
              <span>Nuova visita</span>
            </router-link>
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="hero" size="icon" class="sm:hidden" aria-label="Altre azioni" data-test="paziente-altre-azioni">
                <MoreHorizontal :size="16" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-56 p-1.5">
              <DropdownMenuItem as-child class="cursor-pointer gap-2 px-2.5 py-1.5">
                <router-link :to="`/piani-alimentari/nuovo?pazienteId=${paziente.id}`"><Utensils :size="16" /> Nuovo piano</router-link>
              </DropdownMenuItem>
              <DropdownMenuItem as-child class="cursor-pointer gap-2 px-2.5 py-1.5">
                <router-link :to="`/agenda?nuovo=visita&pazienteId=${paziente.id}`"><CalendarPlus :size="16" /> Nuovo appuntamento</router-link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div v-else class="flex flex-wrap items-center gap-2">
          <Button type="button" variant="hero" @click="confermaDeArchiviaAperta = true">
            <ArchiveRestore :size="16" />
            <span>Ripristina paziente</span>
          </Button>
        </div>
      </div>

      <!-- 2. STATISTICHE RAPIDE -->
      <p v-if="erroreVisite" class="text-xs font-medium text-(--danger)">
        Non è stato possibile caricare i dati clinici del paziente.
      </p>
      <div v-else class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="flex items-center gap-1.5">
            <Scale :size="14" class="text-primary" />
            <p class="text-xs font-semibold text-(--fg3)">Peso attuale</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-16 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.peso.ultimo !== null ? `${formattaNumero(andamento.peso.ultimo)} kg` : '—' }}
            </p>
            <p
              v-if="andamento.peso.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="classeDelta('peso', andamento.peso.delta)"
            >
              <ArrowDown v-if="andamento.peso.delta < 0" :size="12" />
              <ArrowUp v-else-if="andamento.peso.delta > 0" :size="12" />
              {{ formattaNumero(Math.abs(andamento.peso.delta)) }} kg vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="flex items-center gap-1.5">
            <Ruler :size="14" class="text-primary" />
            <p class="text-xs font-semibold text-(--fg3)">BMI</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-12 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.bmi.ultimo !== null ? formattaNumero(andamento.bmi.ultimo, 1) : '—' }}
            </p>
            <p
              v-if="andamento.bmi.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="classeDelta('bmi', andamento.bmi.delta)"
            >
              <ArrowDown v-if="andamento.bmi.delta < 0" :size="12" />
              <ArrowUp v-else-if="andamento.bmi.delta > 0" :size="12" />
              {{ formattaNumero(Math.abs(andamento.bmi.delta), 1) }} vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="flex items-center gap-1.5">
            <Percent :size="14" class="text-primary" />
            <p class="text-xs font-semibold text-(--fg3)">Massa grassa</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-12 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.percentualeGrassoCorporeo.ultimo !== null ? `${formattaNumero(andamento.percentualeGrassoCorporeo.ultimo)}%` : '—' }}
            </p>
            <p
              v-if="andamento.percentualeGrassoCorporeo.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="classeDelta('percentualeGrasso', andamento.percentualeGrassoCorporeo.delta)"
            >
              <ArrowDown v-if="andamento.percentualeGrassoCorporeo.delta < 0" :size="12" />
              <ArrowUp v-else-if="andamento.percentualeGrassoCorporeo.delta > 0" :size="12" />
              {{ formattaNumero(Math.abs(andamento.percentualeGrassoCorporeo.delta)) }} pt vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="flex items-center gap-1.5">
            <Dumbbell :size="14" class="text-primary" />
            <p class="text-xs font-semibold text-(--fg3)">Massa magra</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-16 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.massaMagra.ultimo !== null ? `${formattaNumero(andamento.massaMagra.ultimo)} kg` : '—' }}
            </p>
            <p
              v-if="andamento.massaMagra.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="classeDelta('massaMagra', andamento.massaMagra.delta)"
            >
              <ArrowDown v-if="andamento.massaMagra.delta < 0" :size="12" />
              <ArrowUp v-else-if="andamento.massaMagra.delta > 0" :size="12" />
              {{ formattaNumero(Math.abs(andamento.massaMagra.delta)) }} kg vs prec.
            </p>
          </template>
        </div>
      </div>

      <!-- 3. SEZIONE CLINICA A TAB -->
      <TabsRoot :model-value="tabAttivo" @update:model-value="onCambiaTab">
        <!-- Selettore per schermi piccoli -->
        <Select :model-value="tabAttivo" @update:model-value="onCambiaTab">
          <SelectTrigger id="sezione-clinica-tab" aria-label="Sezione clinica" class="w-full lg:hidden">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="tab in TAB_DEFS" :key="tab.id" :value="tab.id">{{ tab.label }}</SelectItem>
          </SelectContent>
        </Select>

        <!-- Barra a tab per schermi grandi -->
        <TabsList aria-label="Sezioni cliniche" class="hidden gap-1 overflow-x-auto border-b border-(--bd) lg:flex">
          <TabsTrigger
            v-for="tab in TAB_DEFS"
            :key="tab.id"
            :value="tab.id"
            class="mr-5 shrink-0 border-b-2 border-transparent px-1 py-2.5 text-sm font-bold text-(--fg3) transition-colors hover:text-(--fg) focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d) data-[state=active]:border-(--green-d) data-[state=active]:text-(--green-d)"
          >
            {{ tab.label }}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="panoramica" class="mt-5 focus-visible:outline-2 focus-visible:outline-(--green-d)">
          <PazienteTabPanoramica
            :paziente="paziente"
            :visite-in-caricamento="visiteInCaricamento"
            :errore-visite="erroreVisite"
            :visite="visite"
            :andamento="andamento"
            @modifica="mostraModificaAnagrafica = true"
          />
        </TabsContent>

        <TabsContent value="storico" class="mt-5 focus-visible:outline-2 focus-visible:outline-(--green-d)">
          <PazienteTabStoricoVisite
            :paziente-id="paziente.id"
            :archiviato="paziente.archiviato"
            @eliminata="caricaAndamento"
          />
        </TabsContent>

        <TabsContent value="confronto" class="mt-5 focus-visible:outline-2 focus-visible:outline-(--green-d)">
          <PazienteTabConfrontoVisite
            :visite="visite"
            :paziente-id="paziente.id"
            :archiviato="paziente.archiviato"
          />
        </TabsContent>

        <TabsContent value="piani" class="mt-5 focus-visible:outline-2 focus-visible:outline-(--green-d)">
          <PazienteTabPianiAlimentari
            :paziente-id="paziente.id"
            :archiviato="paziente.archiviato"
          />
        </TabsContent>
      </TabsRoot>
    </div>
  </AppShell>

  <ModificaPazienteDialog
    v-if="paziente"
    v-model:open="mostraModificaAnagrafica"
    :paziente="paziente"
    @aggiornato="onPazienteAggiornato"
  />

  <AlertDialog v-if="paziente" v-model:open="confermaDeArchiviaAperta">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Ripristinare {{ paziente.nome }} {{ paziente.cognome }}?</AlertDialogTitle>
        <AlertDialogDescription>Il paziente tornerà visibile nella lista pazienti attivi.</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel variant="neutral" :disabled="deArchiviazioneInCorso"><X /> Annulla</AlertDialogCancel>
        <AlertDialogAction :disabled="deArchiviazioneInCorso" @click="onDeArchivia">
          <ArchiveRestore />
          {{ deArchiviazioneInCorso ? 'Ripristino…' : 'Ripristina paziente' }}
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
