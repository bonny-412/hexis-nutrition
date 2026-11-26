<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import {
  cerca, attiva, elimina,
  type PaginaPianiAlimentari, type PianoAlimentareRigaLista, type StatoPiano, type ModalitaPiano, type CriteriRicercaPianiAlimentari,
} from '@/api/pianiAlimentari'
import PianoAlimentareRigaAzioni from '@/components/pianiAlimentari/PianoAlimentareRigaAzioni.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { DatePicker } from '@/components/ui/date-picker'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, FilterX, Plus, SearchX, Utensils } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import ErroreLista from '@/components/liste/ErroreLista.vue'
import CaricamentoLista from '@/components/liste/CaricamentoLista.vue'
import { formattaDataItalianaConMese, isoATimestamp, timestampAIso } from '@/utils/data'
import { inizialiPaziente } from '@/utils/paziente'
import { useVistaLista } from '@/composables/useVistaLista'
import VistaListaToggle from '@/components/liste/VistaListaToggle.vue'
import OrdinamentoSchede from '@/components/liste/OrdinamentoSchede.vue'
import { TONI_STATO } from '@/utils/toniStato'

const { vista, classiTabella, classiSchede } = useVistaLista('piani')

type CampoOrdinamento = NonNullable<CriteriRicercaPianiAlimentari['ordinaPer']>

const CHIP_STATI: { valore: StatoPiano | 'TUTTI'; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BOZZA', etichetta: 'Bozza' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
  { valore: 'CONCLUSO', etichetta: 'Concluso' },
]

const ETICHETTE_STATO: Record<StatoPiano, string> = {
  BOZZA: 'Bozza',
  ATTIVO: 'Attivo',
  CONCLUSO: 'Concluso',
}

const ETICHETTE_MODALITA: Record<ModalitaPiano, string> = {
  PASTI: 'Piano con pasti',
  MACRO: 'Macronutrienti',
  ESEMPI: 'Esempi intercambiabili',
}

// Stesso codice colori della lista pazienti (`toniStato.ts`): la bozza è da completare, il concluso è neutro.
const CLASSI_STATO: Record<StatoPiano, string> = {
  BOZZA: TONI_STATO.daFare,
  ATTIVO: TONI_STATO.attivo,
  CONCLUSO: TONI_STATO.neutro,
}

// Stesso avatar a iniziali della lista pazienti, colorato come il badge di stato del piano.

const router = useRouter()
const route = useRoute()

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoFiltro = ref<StatoPiano | 'TUTTI'>('TUTTI')
const dataInizioDa = ref('')
const dataInizioA = ref('')
const dataFineDa = ref('')
const dataFineA = ref('')
const filtriAvanzatiAperti = ref(false)
const pagina = ref(0)
const ordinaPer = ref<CampoOrdinamento | undefined>(undefined)
const direzione = ref<'asc' | 'desc'>('asc')

const paginaDati = ref<PaginaPianiAlimentari | null>(null)
const caricamentoIniziale = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)

let debounceHandle: ReturnType<typeof setTimeout> | undefined
watch(ricercaInput, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => {
    ricercaEffettiva.value = valore
    pagina.value = 0
  }, 300)
})
onUnmounted(() => clearTimeout(debounceHandle))

const filtriAvanzatiAttivi = computed(() =>
  [dataInizioDa.value, dataInizioA.value, dataFineDa.value, dataFineA.value].filter(Boolean).length,
)

const filtriAttivi = computed(
  () => ricercaEffettiva.value.trim() !== '' || statoFiltro.value !== 'TUTTI' || filtriAvanzatiAttivi.value > 0,
)

const filtriData = { dataInizioDa, dataInizioA, dataFineDa, dataFineA }
function cambiaData(chiave: keyof typeof filtriData, valore: string) {
  filtriData[chiave].value = valore
  pagina.value = 0
}

function selezionaStato(valore: StatoPiano | 'TUTTI') {
  statoFiltro.value = valore
  pagina.value = 0
}

function criteriCorrenti(): CriteriRicercaPianiAlimentari {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ordinaPer: ordinaPer.value,
    // Senza una colonna scelta decide il backend (piano modificato più di recente per primo).
    direzione: ordinaPer.value ? direzione.value : undefined,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    stato: statoFiltro.value === 'TUTTI' ? undefined : statoFiltro.value,
    dataInizioDa: dataInizioDa.value || undefined,
    dataInizioA: dataInizioA.value || undefined,
    dataFineDa: dataFineDa.value || undefined,
    dataFineA: dataFineA.value || undefined,
  }
}

async function carica() {
  if (paginaDati.value === null) {
    caricamentoIniziale.value = true
  } else {
    aggiornamentoInCorso.value = true
  }
  errore.value = false
  try {
    paginaDati.value = await cerca(criteriCorrenti())
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
    aggiornamentoInCorso.value = false
  }
}
function dataDaQuery(valore: unknown) {
  if (typeof valore !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valore)) return ''
  return timestampAIso(isoATimestamp(valore)) === valore ? valore : ''
}

watch(
  [() => route.query.stato, () => route.query.dataFineDa, () => route.query.dataFineA],
  ([stato, da, a]) => {
    statoFiltro.value = CHIP_STATI.find((chip) => chip.valore === stato)?.valore ?? 'TUTTI'
    dataFineDa.value = dataDaQuery(da)
    dataFineA.value = dataDaQuery(a)
    if (dataFineDa.value && dataFineA.value && dataFineDa.value > dataFineA.value) {
      dataFineDa.value = ''
      dataFineA.value = ''
    }
    filtriAvanzatiAperti.value = !!(dataFineDa.value || dataFineA.value)
    ordinaPer.value = filtriAvanzatiAperti.value ? 'dataFine' : undefined
    direzione.value = 'asc'
    pagina.value = 0
  },
  { immediate: true },
)
watch([ricercaEffettiva, statoFiltro, dataInizioDa, dataInizioA, dataFineDa, dataFineA, pagina, ordinaPer, direzione], carica)
onMounted(carica)

function ordina(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) {
    ordinaPer.value = campo
    direzione.value = 'asc'
  } else if (direzione.value === 'asc') {
    direzione.value = 'desc'
  } else {
    ordinaPer.value = undefined
    direzione.value = 'asc'
  }
  pagina.value = 0
}

const OPZIONI_ORDINAMENTO: { valore: CampoOrdinamento; etichetta: string }[] = [
  { valore: 'nome', etichetta: 'Nome del piano' },
  { valore: 'dataInizio', etichetta: 'Inizio' },
  { valore: 'dataFine', etichetta: 'Scadenza' },
]

function impostaOrdinamento(campo: CampoOrdinamento | undefined, nuovaDirezione: 'asc' | 'desc') {
  ordinaPer.value = campo
  direzione.value = nuovaDirezione
  pagina.value = 0
}

function iconaOrdinamento(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) return ArrowUpDown
  return direzione.value === 'asc' ? ArrowUp : ArrowDown
}

function pulisciFiltri() {
  clearTimeout(debounceHandle)
  ricercaInput.value = ''
  ricercaEffettiva.value = ''
  statoFiltro.value = 'TUTTI'
  dataInizioDa.value = ''
  dataInizioA.value = ''
  dataFineDa.value = ''
  dataFineA.value = ''
  pagina.value = 0
  if (route.query.stato || route.query.dataFineDa || route.query.dataFineA) {
    ordinaPer.value = undefined
    direzione.value = 'asc'
    void router.replace({ query: { ...route.query, stato: undefined, dataFineDa: undefined, dataFineA: undefined } })
  }
}

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}
function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}
async function onAttiva(riga: PianoAlimentareRigaLista) {
  try {
    await attiva(riga.id)
    toast.success('Piano attivato.')
    await carica()
  } catch {
    toast.error('Non è stato possibile attivare il piano.')
  }
}

// La copia non si crea qui: "Nuovo piano" si apre precompilato e il piano nasce con "Salva".
function onDuplica(riga: PianoAlimentareRigaLista) {
  router.push(`/piani-alimentari/nuovo?duplicaDa=${riga.id}`)
}

async function onElimina(riga: PianoAlimentareRigaLista) {
  try {
    await elimina(riga.id)
    toast.success('Piano eliminato.')
    await carica()
  } catch {
    toast.error('Non è stato possibile eliminare il piano.')
  }
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return 'Nessun piano'
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} piani`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Piani alimentari</h1>
        <p class="mt-1 text-sm text-(--fg3)">Bozze, piani attivi e conclusi di tutti i pazienti.</p>
      </div>
      <Button as-child>
        <router-link to="/piani-alimentari/nuovo"><Plus :size="16" /> Nuovo piano</router-link>
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input v-model="ricercaInput" type="search" autocomplete="off" aria-label="Cerca paziente o piano" placeholder="Cerca paziente o piano…" class="min-w-70 flex-1" />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in CHIP_STATI" :key="chip.valore" type="button" :data-test="`chip-stato-${chip.valore}`"
            :aria-pressed="statoFiltro === chip.valore"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="statoFiltro === chip.valore
              ? 'border-(--sage) bg-(--mint) text-(--green-d)'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="selezionaStato(chip.valore)"
          >
            {{ chip.etichetta }}
          </button>
        </div>
      </div>

      <div class="my-3 h-px bg-(--div)" />

      <button
        type="button"
        data-test="filtri-avanzati-toggle"
        class="flex items-center gap-2 text-xs font-bold text-(--fg2) hover:text-(--fg)"
        @click="filtriAvanzatiAperti = !filtriAvanzatiAperti"
      >
        <ChevronRight :size="14" class="transition-transform" :class="{ 'rotate-90': filtriAvanzatiAperti }" />
        Filtri avanzati
        <Badge :variant="filtriAvanzatiAttivi > 0 ? 'secondary' : 'outline'">
          {{ filtriAvanzatiAttivi > 0 ? ` attivi` : 'Nessuno attivo' }}
        </Badge>
      </button>

      <div v-if="filtriAvanzatiAperti" class="mt-3 grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] items-end gap-2.5">
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data inizio da</Label>
          <DatePicker id="data-inizio-da" :model-value="dataInizioDa" :max="dataInizioA" @update:model-value="(v) => cambiaData('dataInizioDa', v)" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data inizio a</Label>
          <DatePicker id="data-inizio-a" :model-value="dataInizioA" :min="dataInizioDa" @update:model-value="(v) => cambiaData('dataInizioA', v)" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data scadenza da</Label>
          <DatePicker id="data-fine-da" :model-value="dataFineDa" :max="dataFineA" @update:model-value="(v) => cambiaData('dataFineDa', v)" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data scadenza a</Label>
          <DatePicker id="data-fine-a" :model-value="dataFineA" :min="dataFineDa" @update:model-value="(v) => cambiaData('dataFineA', v)" />
        </div>
        <button
          type="button"
          class="rounded-lg border border-dashed border-(--dash) px-3 py-2 text-xs font-bold text-(--fg3) hover:border-(--fg4) hover:text-(--fg)"
          @click="pulisciFiltri"
        >
          Pulisci filtri
        </button>
      </div>
    </section>

    <div v-if="paginaDati && paginaDati.contenuto.length > 0" class="mb-3 flex items-center justify-end gap-2">
      <OrdinamentoSchede
        :class="classiSchede()"
        :opzioni="OPZIONI_ORDINAMENTO"
        :ordina-per="ordinaPer"
        :direzione="direzione"
        @cambia="impostaOrdinamento"
      />
      <VistaListaToggle v-model="vista" />
    </div>

    <section class="overflow-clip rounded-2xl border border-(--bd) bg-(--surf)">
      <ErroreLista v-if="errore" messaggio="Non è stato possibile caricare i piani alimentari." @riprova="carica" />
      <CaricamentoLista v-else-if="caricamentoIniziale" etichetta="Caricamento i piani alimentari" />

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <!-- Schede: sempre su mobile, su desktop se scelte con l'interruttore. -->
        <ul
          data-test="schede-piani"
          class="grid grid-cols-1 gap-3 bg-(--bg) p-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4"
          :class="[classiSchede(), { 'pointer-events-none opacity-60': aggiornamentoInCorso }]"
        >
          <li
            v-for="riga in paginaDati.contenuto"
            :key="riga.id"
            class="relative flex flex-col gap-3 rounded-xl border border-(--bd) bg-(--surf) p-4 transition-colors hover:border-(--sage)"
          >
            <div class="flex items-start gap-3">
              <span class="flex h-10 w-10 flex-none items-center justify-center rounded-full text-sm font-semibold" :class="CLASSI_STATO[riga.stato]">
                {{ inizialiPaziente(riga.pazienteNome ?? '', riga.pazienteCognome ?? '') || '—' }}
              </span>
              <div class="min-w-0 flex-1">
                <!-- Il link copre tutta la scheda; il menu azioni sta sopra (z-10) e resta cliccabile. -->
                <router-link
                  :to="`/piani-alimentari/${riga.id}`"
                  class="block truncate font-semibold text-(--fg) after:absolute after:inset-0 after:rounded-xl hover:text-(--green)"
                >
                  {{ riga.nome }}
                </router-link>
                <div class="truncate text-xs text-(--fg3)">{{ riga.pazienteNomeCompleto }}</div>
              </div>
              <div class="relative z-10 -mr-1 -mt-1">
                <PianoAlimentareRigaAzioni :riga="riga" senza-apri-piano @attiva="onAttiva" @duplica="onDuplica" @elimina="onElimina" />
              </div>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <Badge :class="CLASSI_STATO[riga.stato]">{{ ETICHETTE_STATO[riga.stato] }}</Badge>
              <span class="text-xs text-(--fg3)">{{ ETICHETTE_MODALITA[riga.modalita] }}</span>
            </div>
            <dl class="grid grid-cols-3 gap-x-3 border-t border-(--div) pt-3 text-xs">
              <div>
                <dt class="text-(--fg3)">Obiettivo</dt>
                <dd class="font-semibold text-(--fg)">
                  <template v-if="riga.obiettivoKcal">{{ riga.obiettivoKcal }} kcal</template>
                  <span v-else class="font-normal text-(--fg3)">—</span>
                </dd>
              </div>
              <div>
                <dt class="text-(--fg3)">Inizio</dt>
                <dd class="font-semibold text-(--fg)">{{ formattaDataItalianaConMese(riga.dataInizio) }}</dd>
              </div>
              <div>
                <dt class="text-(--fg3)">Scadenza</dt>
                <dd class="font-semibold text-(--fg)">
                  <template v-if="riga.dataFine">{{ formattaDataItalianaConMese(riga.dataFine) }}</template>
                  <span v-else class="font-normal text-(--fg3)">—</span>
                </dd>
              </div>
            </dl>
          </li>
        </ul>

        <div class="overflow-x-auto xl:overflow-x-visible" :class="[classiTabella(), { 'pointer-events-none opacity-60': aggiornamentoInCorso }]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="text-(--fg3)">Paziente</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('nome')">
                    Piano
                    <component :is="iconaOrdinamento('nome')" :size="12" :class="ordinaPer === 'nome' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-(--fg3)">Stato</TableHead>
                <TableHead class="text-(--fg3)">Obiettivo</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('dataInizio')">
                    Inizio
                    <component :is="iconaOrdinamento('dataInizio')" :size="12" :class="ordinaPer === 'dataInizio' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('dataFine')">
                    Scadenza
                    <component :is="iconaOrdinamento('dataFine')" :size="12" :class="ordinaPer === 'dataFine' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-right text-(--fg3)">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="riga in paginaDati.contenuto" :key="riga.id" class="hover:bg-(--soft)">
                <TableCell>
                  <div class="flex items-center gap-2.5">
                    <span class="flex h-9 w-9 flex-none items-center justify-center rounded-full text-[13px] font-semibold" :class="CLASSI_STATO[riga.stato]">
                      {{ inizialiPaziente(riga.pazienteNome ?? '', riga.pazienteCognome ?? '') || '—' }}
                    </span>
                    <!-- Nome ed email come nella lista pazienti. -->
                    <div class="min-w-0">
                      <div class="font-semibold text-(--fg)">{{ riga.pazienteNomeCompleto }}</div>
                      <div v-if="riga.pazienteEmail" class="text-xs text-(--fg3)">{{ riga.pazienteEmail }}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <div class="font-semibold text-(--fg)">{{ riga.nome }}</div>
                  <div class="text-xs text-(--fg3)">{{ ETICHETTE_MODALITA[riga.modalita] }}</div>
                </TableCell>
                <TableCell><Badge :class="CLASSI_STATO[riga.stato]">{{ ETICHETTE_STATO[riga.stato] }}</Badge></TableCell>
                <TableCell>
                  <span v-if="riga.obiettivoKcal" class="font-semibold text-(--fg)">{{ riga.obiettivoKcal }} kcal</span>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell class="font-semibold text-(--fg)">{{ formattaDataItalianaConMese(riga.dataInizio) }}</TableCell>
                <TableCell>
                  <span v-if="riga.dataFine" class="font-semibold text-(--fg)">{{ formattaDataItalianaConMese(riga.dataFine) }}</span>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell class="text-right">
                  <PianoAlimentareRigaAzioni :riga="riga" @attiva="onAttiva" @duplica="onDuplica" @elimina="onElimina" />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <StatoVuoto v-else-if="!filtriAttivi" :icona="Utensils" titolo="Nessun piano presente" testo="Inizia creando il primo piano alimentare per un tuo paziente." livello="h2">
        <Button as-child class="active:not-aria-[haspopup]:translate-y-0.5"><Plus /> <router-link to="/piani-alimentari/nuovo">Nuovo piano</router-link></Button>
      </StatoVuoto>
      <StatoVuoto v-else :icona="SearchX" titolo="Nessun risultato trovato" testo="Prova a modificare o resettare i filtri applicati." livello="h2">
        <Button type="button" variant="neutral" @click="pulisciFiltri"><FilterX /> Pulisci filtri</Button>
      </StatoVuoto>

      <div v-if="paginaDati && !errore" class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <!-- Con una pagina sola i pulsanti sarebbero entrambi disattivati: resta solo il conteggio. -->
        <div v-if="paginaDati.totalePagine > 1" class="flex gap-2">
          <Button type="button" variant="neutral" size="sm" :disabled="pagina === 0" @click="paginaPrecedente"><ChevronLeft /> Precedente</Button>
          <Button
            type="button"
            variant="neutral"
            size="sm"
            :disabled="!paginaDati || pagina >= paginaDati.totalePagine - 1"
            @click="paginaSuccessiva"
          >
            Successivo <ChevronRight />
          </Button>
        </div>
      </div>
    </section>
  </AppShell>
</template>
