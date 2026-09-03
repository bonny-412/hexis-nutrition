<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, visite as caricaVisite, type Paziente, type Visita } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import AndamentoChart from '@/components/pazienti/AndamentoChart.vue'
import { prepareAndamento } from '@/utils/andamento'
import { calcolaEta, formattaDataItalianaConMese, formattaDataItalianaEstesa } from '@/utils/data'
import {
  ArrowLeft,
  ArrowUp,
  ArrowDown,
  Pencil,
  AlertCircle,
  Plus,
  Scale,
  Ruler,
  Calendar,
  FileText,
  ChevronRight,
} from '@lucide/vue'

const ETICHETTE_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'Mai invitato',
  INVITATO: 'Invitato',
  ATTIVO: 'Attivo',
}

const CLASSI_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'bg-(--hover) text-(--fg4)',
  INVITATO: 'bg-(--warn-bg) text-(--warn-fg)',
  ATTIVO: 'bg-(--mint) text-(--green)',
}

const ETICHETTE_SESSO: Record<string, string> = {
  M: 'Maschio',
  F: 'Femmina',
  ALTRO: 'Altro',
}

const ETICHETTE_TIPO_LAVORO: Record<string, string> = {
  SEDENTARIO: 'Sedentario',
  POCO_ATTIVO: 'Poco attivo',
  ATTIVO: 'Attivo',
  MOLTO_ATTIVO: 'Molto attivo',
}

const route = useRoute()

// Stato Paziente
const caricamento = ref(true)
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)

// Stato Visite e Andamento
const visite = ref<Visita[]>([])
const erroreVisite = ref(false)
const visiteInCaricamento = ref(true)

const andamento = computed(() => prepareAndamento(visite.value))
const eta = computed(() =>
  paziente.value?.dataNascita ? calcolaEta(paziente.value.dataNascita) : null
)
const ultimaVisita = computed(() => (visite.value.length > 0 ? visite.value[visite.value.length - 1] : null))
// Le visite arrivano dal backend in ordine cronologico crescente; per l'elenco le vogliamo dalla più recente.
const visiteOrdineDecrescente = computed(() => [...visite.value].reverse())

function formattaValoreMetrica(valore: number): string {
  return valore.toLocaleString('it-IT', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

function scomponiDataVisita(dataIso: string): { giorno: string; meseAnno: string } {
  const [giorno, ...resto] = formattaDataItalianaConMese(dataIso).split(' ')
  return { giorno, meseAnno: resto.join(' ').toUpperCase() }
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

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
    toast.success('Invito inviato con successo.')
  } catch {
    toast.error("Non è stato possibile inviare l'invito.")
  } finally {
    invitoInCorso.value = false
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
      <router-link to="/pazienti"
        class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)"
      >
        <ArrowLeft :size="16" />
        <span>Torna alla lista pazienti</span>
      </router-link>
    </div>

    <!-- Stato Errore Caricamento -->
    <div
      v-if="erroreCaricamento"
      class="flex flex-col items-center justify-center rounded-2xl border border-(--bd) bg-(--surf) p-12 text-center shadow-sm"
    >
      <AlertCircle :size="40" class="text-(--danger) mb-3" />
      <h2 class="font-heading text-xl italic text-(--fg)">Errore di caricamento</h2>
      <p class="mt-1 text-sm text-(--fg3)">{{ erroreCaricamento }}</p>
      <Button variant="outline" size="sm" class="mt-4" @click="carica">Riprova</Button>
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
    <div v-else-if="paziente" class="space-y-6">
      <!-- 1. TITOLO E SOTTOTITOLO -->
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Scheda Paziente</h1>
        <p class="mt-1 text-sm text-(--fg3)">
          Consulta le informazioni personali, i dati anagrafici e l'andamento dei parametri clinici.
        </p>
      </div>

      <!-- 2. CARD UTENTE (Profilo principale con azioni) -->
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <div class="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
          <!-- Avatar + Nome + Stato -->
          <div class="flex items-center gap-4">
            <span
              class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full font-heading text-2xl font-semibold select-none"
              :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]"
            >
              {{ paziente.nome[0] }}{{ paziente.cognome[0] }}
            </span>
            <div class="space-y-1">
              <div class="flex flex-col">
                <h2 class="font-heading text-2xl italic text-(--fg)">
                  {{ paziente.nome }} {{ paziente.cognome }}
                </h2>
                <div class="flex items-center gap-2">
                  <Badge :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]">
                    {{ ETICHETTE_STATO_ACCOUNT[paziente.statoAccount] }}
                  </Badge>
                  <button
                    v-if="paziente.statoAccount !== 'ATTIVO'"
                    type="button"
                    :disabled="invitoInCorso"
                    class="text-xs font-semibold text-(--green) underline-offset-4 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                    @click="onInvita"
                  >
                    {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Azioni Utente: le azioni sempre visibili, disabilitate quelle senza flusso costruito -->
          <div class="flex flex-wrap items-center gap-2">
            <Button variant="outline" size="sm" disabled>
              <Pencil :size="15" />
              <span>Modifica</span>
            </Button>

            <Button variant="secondary" size="sm" disabled>
              <Plus :size="15" />
              <span>Nuova visita</span>
            </Button>

            <Button size="sm" class="hover:bg-primary/80" disabled>
              <Plus :size="15" />
              <span>Nuovo piano alimentare</span>
            </Button>
          </div>
        </div>

        <!-- Statistiche rapide -->
        <p v-if="erroreVisite" class="mt-6 border-t border-(--div) pt-6 text-xs font-medium text-(--danger)">
          Non è stato possibile caricare i dati clinici del paziente.
        </p>
        <div v-else class="mt-6 grid grid-cols-2 gap-x-4 gap-y-5 border-t border-(--div) pt-6 sm:grid-cols-3 lg:grid-cols-5">
          <div class="flex items-start gap-2">
            <Scale :size="16" class="shrink-0 text-primary" />
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Peso attuale</p>
              <div v-if="visiteInCaricamento" class="mt-1.5 h-5 w-16 animate-pulse rounded bg-(--hover)" />
              <template v-else>
                <p class="text-lg font-semibold text-(--fg)">
                  {{ andamento.peso.ultimo !== null ? `${formattaValoreMetrica(andamento.peso.ultimo)} kg` : '—' }}
                </p>
                <p
                  v-if="andamento.peso.delta !== null"
                  class="flex items-center gap-0.5 text-xs font-medium"
                  :class="andamento.peso.delta < 0 ? 'text-(--green)' : andamento.peso.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
                >
                  <ArrowDown v-if="andamento.peso.delta < 0" :size="11" />
                  <ArrowUp v-else-if="andamento.peso.delta > 0" :size="11" />
                  {{ formattaValoreMetrica(Math.abs(andamento.peso.delta)) }} kg dalla visita precedente
                </p>
              </template>
            </div>
          </div>

          <div class="flex items-start gap-2">
            <Ruler :size="16" class="shrink-0 text-primary" />
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">BMI</p>
              <div v-if="visiteInCaricamento" class="mt-1.5 h-5 w-12 animate-pulse rounded bg-(--hover)" />
              <template v-else>
                <p class="text-lg font-semibold text-(--fg)">
                  {{ andamento.bmi.ultimo !== null ? formattaValoreMetrica(andamento.bmi.ultimo) : '—' }}
                </p>
                <p
                  v-if="andamento.bmi.delta !== null"
                  class="flex items-center gap-0.5 text-xs font-medium"
                  :class="andamento.bmi.delta < 0 ? 'text-(--green)' : andamento.bmi.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
                >
                  <ArrowDown v-if="andamento.bmi.delta < 0" :size="11" />
                  <ArrowUp v-else-if="andamento.bmi.delta > 0" :size="11" />
                  {{ formattaValoreMetrica(Math.abs(andamento.bmi.delta)) }} dalla visita precedente
                </p>
              </template>
            </div>
          </div>

          <div class="flex items-start gap-2">
            <Calendar :size="16" class="shrink-0 text-primary" />
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Ultima visita</p>
              <div v-if="visiteInCaricamento" class="mt-1.5 h-5 w-20 animate-pulse rounded bg-(--hover)" />
              <p v-else class="text-lg font-semibold text-(--fg)">
                {{ ultimaVisita ? formattaDataItalianaConMese(ultimaVisita.dataVisita) : '—' }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-2 opacity-50">
            <Calendar :size="16" class="shrink-0 text-primary" />
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Prossima visita</p>
              <p class="text-sm italic text-(--fg4)">Presto disponibile</p>
            </div>
          </div>

          <div class="flex items-start gap-2 opacity-50">
            <FileText :size="16" class="shrink-0 text-primary" />
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Piano alimentare</p>
              <p class="text-sm italic text-(--fg4)">Presto disponibile</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. CARDS CON DATI ANAGRAFICI E CONTATTI -->
      <div class="grid gap-6 md:grid-cols-2">
        <!-- Card Dati Anagrafici -->
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm">
          <div class="mb-5 flex items-center gap-2 border-b border-(--bd) pb-3">
            <h3 class="font-heading text-xl italic text-(--fg)">Dati Anagrafici</h3>
          </div>

          <div class="space-y-4">
            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Codice Fiscale</span>
              <span class="font-mono font-medium uppercase text-(--fg)">
                {{ paziente.codiceFiscale }}
              </span>
            </div>

            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Sesso</span>
              <span class="font-medium text-(--fg)">
                {{ ETICHETTE_SESSO[paziente.sesso] ?? paziente.sesso }}
              </span>
            </div>

            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Data di Nascita</span>
              <span class="font-medium text-(--fg)">
                {{ paziente.dataNascita ? formattaDataItalianaEstesa(paziente.dataNascita) : '—' }}
                <span v-if="eta !== null" class="text-xs text-(--fg3)">({{ eta }} anni)</span>
              </span>
            </div>

            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Occupazione</span>
              <span class="font-medium text-(--fg)">
                {{ paziente.lavoro ?? '—' }}
              </span>
            </div>

            <div v-if="paziente.tipoLavoro" class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Stile di Vita / Lavoro</span>
              <span class="font-medium text-(--fg)">
                {{ ETICHETTE_TIPO_LAVORO[paziente.tipoLavoro] }}
              </span>
            </div>

            <div v-if="paziente.note" class="text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Note</span>
              <p class="mt-1 whitespace-pre-line font-medium text-(--fg)">{{ paziente.note }}</p>
            </div>
          </div>
        </div>

        <!-- Card Contatti -->
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm">
          <div class="mb-5 flex items-center gap-2 border-b border-(--bd) pb-3">
            <h3 class="font-heading text-xl italic text-(--fg)">Contatti e Recapiti</h3>
          </div>

          <div class="space-y-4">
            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Email</span>
              <a
                :href="`mailto:${paziente.email}`"
                class="font-medium text-(--fg) underline-offset-4 hover:text-(--green) hover:underline"
              >
                {{ paziente.email }}
              </a>
            </div>

            <div class="flex items-center justify-between text-sm">
              <span class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Telefono</span>
              <a
                v-if="paziente.telefono"
                :href="`tel:${paziente.telefono}`"
                class="font-medium text-(--fg) underline-offset-4 hover:text-(--green) hover:underline"
              >
                {{ paziente.telefono }}
              </a>
              <span v-else class="font-medium text-(--fg3)">—</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. CARD ANDAMENTO CLINICO CON GRAFICI -->
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <div class="mb-6 flex items-center gap-2 border-b border-(--bd) pb-4">
          <div>
            <h3 class="font-heading text-xl italic text-(--fg)">Andamento Clinico</h3>
            <p class="text-xs text-(--fg3)">Storico dei parametri e misurazioni registrate durante le visite.</p>
          </div>
        </div>

        <div
          v-if="erroreVisite"
          class="p-4 text-xs font-medium text-(--danger)"
        >
          Non è stato possibile caricare lo storico delle visite.
        </div>

        <div v-else-if="visiteInCaricamento" class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div v-for="n in 3" :key="n" data-test="andamento-chart-skeleton" class="h-48 animate-pulse rounded-xl bg-(--hover)" />
        </div>

        <div
          v-else-if="visite.length === 0"
          class="rounded-xl border border-(--bd) bg-(--hover)/40 p-8 text-center text-sm text-(--fg3)"
        >
          Nessuna visita registrata. L'andamento apparirà dopo la prima visita.
        </div>

        <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <AndamentoChart
            titolo="Peso"
            unita="kg"
            :andamento="andamento.peso"
            colore="var(--chart-1)"
          />
          <AndamentoChart
            titolo="BMI"
            unita=""
            :andamento="andamento.bmi"
            colore="var(--chart-2)"
          />
          <AndamentoChart
            v-if="andamento.percentualeGrassoCorporeo.punti.length > 0"
            titolo="% Grasso corporeo"
            unita="%"
            :andamento="andamento.percentualeGrassoCorporeo"
            colore="var(--chart-3)"
          />
        </div>
      </div>

      <!-- 5. CARD VISITE -->
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <div class="mb-5 flex items-center justify-between gap-2 border-b border-(--bd) pb-4">
          <div class="flex items-center gap-2">
            <Calendar :size="18" class="text-(--green)" />
            <h3 class="font-heading text-xl italic text-(--fg)">Visite</h3>
          </div>
          <Button size="sm" class="hover:bg-primary/80" disabled>
            <Plus :size="15" />
            <span>Nuova visita</span>
          </Button>
        </div>

        <div v-if="erroreVisite" class="text-xs font-medium text-(--danger)">
          Non è stato possibile caricare l'elenco delle visite.
        </div>

        <div v-else-if="visiteInCaricamento" class="space-y-3">
          <div v-for="n in 3" :key="n" class="h-14 animate-pulse rounded-xl bg-(--hover)" />
        </div>

        <div
          v-else-if="visite.length === 0"
          class="rounded-xl border border-(--bd) bg-(--hover)/40 p-8 text-center text-sm text-(--fg3)"
        >
          Nessuna visita registrata.
        </div>

        <ul v-else class="divide-y divide-(--div)">
          <li v-for="(visitaRiga, indice) in visiteOrdineDecrescente" :key="visitaRiga.id" class="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
            <div class="flex w-14 shrink-0 flex-col items-center justify-center rounded-lg bg-(--hover) py-1.5">
              <span class="text-lg font-bold leading-none text-(--fg)">{{ scomponiDataVisita(visitaRiga.dataVisita).giorno }}</span>
              <span class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">{{ scomponiDataVisita(visitaRiga.dataVisita).meseAnno }}</span>
            </div>
            <p class="min-w-0 flex-1 font-medium text-(--fg)">
              {{ indice === visiteOrdineDecrescente.length - 1 ? 'Prima visita' : 'Visita di controllo' }}
            </p>
            <Badge class="bg-(--mint) text-(--green)">Completata</Badge>
            <ChevronRight :size="16" class="shrink-0 text-(--fg4)" />
          </li>
        </ul>
      </div>
    </div>
  </AppShell>
</template>