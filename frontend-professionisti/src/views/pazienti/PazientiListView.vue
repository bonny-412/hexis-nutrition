<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import {
  cerca,
  invita,
  archivia,
  deArchivia,
  type Paziente,
  type PaginaPazienti,
  type CriteriRicercaPazienti,
} from '@/api/pazienti'
import PazienteRigaAzioni from '@/components/pazienti/PazienteRigaAzioni.vue'
import PazienteStatoAccount from '@/components/pazienti/PazienteStatoAccount.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Archive, ArrowDown, ArrowUp, ArrowUpDown, Check, ChevronLeft, ChevronRight, FilterX, SearchX, UserPlus, Users } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import ErroreLista from '@/components/liste/ErroreLista.vue'
import CaricamentoLista from '@/components/liste/CaricamentoLista.vue'
import { formattaDataItalianaConMese, isoATimestamp } from '@/utils/data'
import { ETICHETTE_OBIETTIVO } from '@/utils/visita'
import { classiAvatarPaziente, inizialiPaziente } from '@/utils/paziente'
import { useVistaLista } from '@/composables/useVistaLista'
import VistaListaToggle from '@/components/liste/VistaListaToggle.vue'
import OrdinamentoSchede from '@/components/liste/OrdinamentoSchede.vue'

const { vista, classiTabella, classiSchede } = useVistaLista('pazienti')

const OPZIONI_ORDINAMENTO: { valore: CampoOrdinamento; etichetta: string }[] = [
  { valore: 'cognome', etichetta: 'Cognome' },
  { valore: 'dataUltimaVisita', etichetta: 'Ultima visita' },
  { valore: 'piano', etichetta: 'Piano alimentare' },
  { valore: 'statoAccount', etichetta: 'Stato account' },
]

type CampoOrdinamento = NonNullable<CriteriRicercaPazienti['ordinaPer']>

const chipStatoAccount: { valore: 'TUTTI' | Paziente['statoAccount']; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'MAI_INVITATO', etichetta: 'Mai invitato' },
  { valore: 'INVITATO', etichetta: 'Invitato' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
]

const CLASSE_TESTO_STATO_PIANO: Record<NonNullable<Paziente['pianoStato']>, string> = {
  BOZZA: 'text-(--fg4)',
  ATTIVO: 'text-(--fg)',
  CONCLUSO: 'text-(--fg4)',
}

function giorniDaOggi(dataIso: string): number {
  const oggi = new Date()
  const oggiIso = `${oggi.getFullYear()}-${String(oggi.getMonth() + 1).padStart(2, '0')}-${String(oggi.getDate()).padStart(2, '0')}`
  return Math.round((isoATimestamp(dataIso) - isoATimestamp(oggiIso)) / 86_400_000)
}

// dataUltimaVisita è sempre una data passata o odierna (è l'ultima visita registrata, non un
// appuntamento futuro: "Appuntamento" è un sotto-progetto non ancora implementato — vedi
// wiki/modello-dati.md, sezione "Entità previste ma non ancora progettate"). Niente ramo per
// una data futura, non può mai verificarsi con questo campo.
function testoUltimaVisita(dataIso: string): string {
  return giorniDaOggi(dataIso) === 0 ? 'Oggi' : formattaDataItalianaConMese(dataIso)
}

function notaUltimaVisita(dataIso: string): string | null {
  const giorni = giorniDaOggi(dataIso)
  if (giorni === 0) return null
  const giorniFa = Math.abs(giorni)
  if (giorniFa < 7) return `${giorniFa} giorn${giorniFa === 1 ? 'o' : 'i'} fa`
  const settimaneFa = Math.floor(giorniFa / 7)
  return `${settimaneFa} settiman${settimaneFa === 1 ? 'a' : 'e'} fa`
}

// Nota sotto il nome del piano: solo dati realmente disponibili. Nessuna aderenza o data di
// prossimo appuntamento: "Appuntamento" è un sotto-progetto non ancora implementato (vedi
// wiki/modello-dati.md, sezione "Entità previste ma non ancora progettate").
function notaPiano(paziente: Paziente): string {
  if (paziente.pianoStato === 'BOZZA') return 'bozza, non ancora attivato'
  if (!paziente.pianoDataFine) return paziente.pianoStato === 'ATTIVO' ? 'senza scadenza' : paziente.pianoStato === 'CONCLUSO' ? 'concluso' : ''

  const giorni = giorniDaOggi(paziente.pianoDataFine)
  if (paziente.pianoStato === 'CONCLUSO') {
    // Terminato a mano con una data di fine ancora futura: niente "da N giorni".
    if (giorni >= 0) return 'concluso'
    const assoluto = Math.abs(giorni)
    return `concluso da ${assoluto} giorn${assoluto === 1 ? 'o' : 'i'}`
  }
  if (giorni === 0) return 'scade oggi'
  if (giorni === 1) return 'scade domani'
  return `scade tra ${giorni} giorni`
}

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoAccount = ref<'TUTTI' | Paziente['statoAccount']>('TUTTI')
const obiettivo = ref<'TUTTI' | NonNullable<Paziente['obiettivoUltimaVisita']>>('TUTTI')
const dataUltimaVisitaDa = ref('')
const dataUltimaVisitaA = ref('')
const prossimaVisitaEntro = ref<'TUTTE' | '7' | '14' | '21' | '30'>('TUTTE')
const mostraArchiviati = ref(false)
const filtriAvanzatiAperti = ref(false)
const pagina = ref(0)
const ordinaPer = ref<CampoOrdinamento | undefined>(undefined)
const direzione = ref<'asc' | 'desc'>('asc')

const paginaDati = ref<PaginaPazienti | null>(null)
const caricamentoIniziale = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)
const invitiInCorso = reactive(new Set<string>())

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
  [obiettivo.value !== 'TUTTI', !!dataUltimaVisitaDa.value, !!dataUltimaVisitaA.value, prossimaVisitaEntro.value !== 'TUTTE', mostraArchiviati.value].filter(Boolean).length,
)

const filtriAttivi = computed(() =>
  ricercaEffettiva.value.trim() !== '' || statoAccount.value !== 'TUTTI' || filtriAvanzatiAttivi.value > 0,
)

function criteriCorrenti(): CriteriRicercaPazienti {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ordinaPer: ordinaPer.value,
    // Senza una colonna scelta decide il backend (cognome crescente).
    direzione: ordinaPer.value ? direzione.value : undefined,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    statoAccount: statoAccount.value === 'TUTTI' ? undefined : statoAccount.value,
    obiettivo: obiettivo.value === 'TUTTI' ? undefined : obiettivo.value,
    dataUltimaVisitaDa: dataUltimaVisitaDa.value || undefined,
    dataUltimaVisitaA: dataUltimaVisitaA.value || undefined,
    prossimaVisitaEntroGiorni: prossimaVisitaEntro.value === 'TUTTE' ? undefined : (Number(prossimaVisitaEntro.value) as 7 | 14 | 21 | 30),
    archiviato: mostraArchiviati.value,
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
    const risultato = await cerca(criteriCorrenti())
    if (risultato.contenuto.length === 0 && pagina.value > 0 && risultato.totaleElementi > 0) {
      pagina.value -= 1
      return
    }
    paginaDati.value = risultato
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
    aggiornamentoInCorso.value = false
  }
}

const route = useRoute()
const router = useRouter()

watch(() => route.query.statoAccount, (statoQuery) => {
  statoAccount.value = chipStatoAccount.find((chip) => chip.valore === statoQuery)?.valore ?? 'TUTTI'
  mostraArchiviati.value = false
  pagina.value = 0
}, { immediate: true })

watch(
  [ricercaEffettiva, statoAccount, obiettivo, dataUltimaVisitaDa, dataUltimaVisitaA, prossimaVisitaEntro, mostraArchiviati, pagina, ordinaPer, direzione],
  carica,
)

onMounted(carica)

watch(
  () => route.query.ricerca,
  (ricercaQuery) => {
    if (typeof ricercaQuery === 'string' && ricercaQuery !== '') {
      ricercaInput.value = ricercaQuery
      router.replace({ query: { ...route.query, ricerca: undefined } })
    }
  },
  { immediate: true },
)

function selezionaStato(valore: typeof statoAccount.value) {
  statoAccount.value = valore
  pagina.value = 0
}

function onObiettivoChange(valore: AcceptableValue) {
  obiettivo.value = valore as typeof obiettivo.value
  pagina.value = 0
}

function onDataUltimaVisitaDaChange(valore: string) {
  dataUltimaVisitaDa.value = valore
  pagina.value = 0
}

function onDataUltimaVisitaAChange(valore: string) {
  dataUltimaVisitaA.value = valore
  pagina.value = 0
}

function onProssimaVisitaEntroChange(valore: AcceptableValue) {
  prossimaVisitaEntro.value = valore as typeof prossimaVisitaEntro.value
  pagina.value = 0
}

function onMostraArchiviatiChange(valore: boolean) {
  mostraArchiviati.value = valore
  pagina.value = 0
}

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
  statoAccount.value = 'TUTTI'
  obiettivo.value = 'TUTTI'
  dataUltimaVisitaDa.value = ''
  dataUltimaVisitaA.value = ''
  prossimaVisitaEntro.value = 'TUTTE'
  mostraArchiviati.value = false
  pagina.value = 0
  if (route.query.statoAccount) {
    void router.replace({ query: { ...route.query, statoAccount: undefined } })
  }
}

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}

function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}

async function onInvita(paziente: Paziente) {
  if (invitiInCorso.has(paziente.id)) return
  invitiInCorso.add(paziente.id)
  try {
    await invita(paziente.id)
    paziente.statoAccount = 'INVITATO'
    toast.success('Invito inviato.')
  } catch {
    toast.error('Non è stato possibile inviare l\'invito.')
  } finally {
    invitiInCorso.delete(paziente.id)
  }
}

async function onArchivia(paziente: Paziente) {
  try {
    await archivia(paziente.id)
    await carica()
    toast.success('Paziente archiviato.')
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  }
}

async function onDeArchivia(paziente: Paziente) {
  try {
    await deArchivia(paziente.id)
    await carica()
    toast.success('Paziente ripristinato.')
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  }
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return mostraArchiviati.value ? 'Nessun paziente archiviato' : 'Nessun paziente'
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} pazienti`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Pazienti</h1>
        <p class="mt-1 text-sm text-(--fg3)">Cartelle cliniche, inviti all'app e prossime visite.</p>
      </div>
      <Button as-child>
        <router-link to="/pazienti/nuovo"><UserPlus :size="16" /> Nuovo paziente</router-link>
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input
          v-model="ricercaInput"
          type="search"
          aria-label="Cerca per nome, email o codice fiscale"
          autocomplete="off" placeholder="Cerca per nome, email o codice fiscale…"
          class="min-w-70 flex-1"
        />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in chipStatoAccount"
            :key="chip.valore"
            type="button"
            :aria-pressed="statoAccount === chip.valore"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="statoAccount === chip.valore
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
        class="flex items-center gap-2 text-xs font-bold text-(--fg2) hover:text-(--fg)"
        @click="filtriAvanzatiAperti = !filtriAvanzatiAperti"
      >
        <ChevronRight :size="14" class="transition-transform" :class="{ 'rotate-90': filtriAvanzatiAperti }" />
        Filtri avanzati
        <Badge :variant="filtriAvanzatiAttivi > 0 ? 'secondary' : 'outline'">
          {{ filtriAvanzatiAttivi > 0 ? `${filtriAvanzatiAttivi} attivi` : 'Nessuno attivo' }}
        </Badge>
      </button>

      <div v-if="filtriAvanzatiAperti" class="mt-3 grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] items-end gap-2.5">
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Obiettivo</Label>
          <Select :model-value="obiettivo" @update:model-value="onObiettivoChange">
            <SelectTrigger class="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="TUTTI">Tutti</SelectItem>
              <SelectItem v-for="(etichetta, valore) in ETICHETTE_OBIETTIVO" :key="valore" :value="valore">{{ etichetta }}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data ultima visita da</Label>
          <DatePicker id="data-ultima-visita-da" :model-value="dataUltimaVisitaDa" :max="dataUltimaVisitaA" @update:model-value="onDataUltimaVisitaDaChange" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Data ultima visita a</Label>
          <DatePicker id="data-ultima-visita-a" :model-value="dataUltimaVisitaA" :min="dataUltimaVisitaDa" @update:model-value="onDataUltimaVisitaAChange" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-xs font-medium text-(--fg3)">Prossima visita</Label>
          <Select :model-value="prossimaVisitaEntro" @update:model-value="onProssimaVisitaEntroChange">
            <SelectTrigger id="filtro-prossima-visita" class="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="TUTTE">Tutte</SelectItem>
              <SelectItem value="7">Entro 1 settimana</SelectItem>
              <SelectItem value="14">Entro 2 settimane</SelectItem>
              <SelectItem value="21">Entro 3 settimane</SelectItem>
              <SelectItem value="30">Entro 1 mese</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="flex items-end pb-px">
          <button
            id="filtro-mostra-archiviati"
            type="button"
            role="switch"
            :aria-checked="mostraArchiviati"
            class="inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-bold whitespace-nowrap transition-colors"
            :class="mostraArchiviati
              ? 'border-(--sage) bg-(--mint) text-(--green-d)'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="onMostraArchiviatiChange(!mostraArchiviati)"
          >
            <!-- Stesso stato attivo dei chip di stato, più la spunta: acceso/spento si legge anche senza il colore. -->
            <Check v-if="mostraArchiviati" :size="14" aria-hidden="true" />
            <Archive v-else :size="14" aria-hidden="true" />
            Mostra pazienti archiviati
          </button>
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
      <ErroreLista v-if="errore" messaggio="Non è stato possibile caricare i pazienti." @riprova="carica" />
      <CaricamentoLista v-else-if="caricamentoIniziale" etichetta="Caricamento i pazienti" />

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <!-- Schede: sempre su mobile, su desktop se scelte con l'interruttore. -->
        <ul
          data-test="schede-pazienti"
          class="grid grid-cols-1 gap-3 bg-(--bg) p-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4"
          :class="[classiSchede(), { 'pointer-events-none opacity-60': aggiornamentoInCorso }]"
        >
          <li
            v-for="paziente in paginaDati.contenuto"
            :key="paziente.id"
            class="relative flex flex-col gap-3 rounded-xl border border-(--bd) bg-(--surf) p-4 transition-colors hover:border-(--sage)"
          >
            <div class="flex items-start gap-3">
              <span
                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
                :class="classiAvatarPaziente(paziente)"
              >
                {{ inizialiPaziente(paziente.nome, paziente.cognome) }}
              </span>
              <div class="min-w-0 flex-1">
              <!-- Il link copre tutta la scheda; i controlli con z-10 restano cliccabili. -->
                <router-link
                  :to="`/pazienti/${paziente.id}`"
                  class="block truncate font-semibold text-(--fg) after:absolute after:inset-0 after:rounded-xl hover:text-(--green)"
                >
                  {{ paziente.nome }} {{ paziente.cognome }}
                </router-link>
                <div class="truncate text-xs text-(--fg3)">{{ paziente.email }}</div>
              </div>
            </div>
            <div class="flex items-center justify-between gap-2">
              <PazienteStatoAccount
                class="[&>button]:z-10"
                :paziente="paziente"
                :sola-lettura="mostraArchiviati"
                :invito-in-corso="invitiInCorso.has(paziente.id)"
                @invita="onInvita(paziente)"
              />
              <div class="relative z-10 -my-1 -mr-1">
                <PazienteRigaAzioni
                  :paziente="paziente"
                  :mostra-archiviati="mostraArchiviati"
                  senza-apri-cartella
                  @archivia="onArchivia"
                  @de-archivia="onDeArchivia"
                />
              </div>
            </div>
            <dl class="grid grid-cols-2 gap-x-3 gap-y-2 border-t border-(--div) pt-3 text-xs">
              <div>
                <dt class="text-(--fg3)">Prossima visita</dt>
                <dd class="font-semibold text-(--fg)">
                  <template v-if="paziente.prossimaVisita">
                    {{ formattaDataItalianaConMese(paziente.prossimaVisita.slice(0, 10)) }}, {{ paziente.prossimaVisita.slice(11, 16) }}
                  </template>
                  <span v-else class="font-normal text-(--fg3)">—</span>
                </dd>
              </div>
              <div>
                <dt class="text-(--fg3)">Ultima visita</dt>
                <dd class="font-semibold text-(--fg)">
                  <template v-if="paziente.dataUltimaVisita">{{ testoUltimaVisita(paziente.dataUltimaVisita) }}</template>
                  <span v-else class="font-normal text-(--fg3)">—</span>
                </dd>
              </div>
              <div>
                <dt class="text-(--fg3)">Obiettivo</dt>
                <dd class="font-semibold text-(--fg)">
                  <template v-if="paziente.obiettivoUltimaVisita">{{ ETICHETTE_OBIETTIVO[paziente.obiettivoUltimaVisita] }}</template>
                  <span v-else class="font-normal text-(--fg3)">—</span>
                </dd>
              </div>
              <div>
                <dt class="text-(--fg3)">Piano</dt>
                <dd class="truncate font-semibold" :class="paziente.pianoStato ? CLASSE_TESTO_STATO_PIANO[paziente.pianoStato] : ''">
                  <template v-if="paziente.pianoNome">{{ paziente.pianoNome }}</template>
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
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('cognome')">
                    Paziente
                    <component :is="iconaOrdinamento('cognome')" :size="12" :class="ordinaPer === 'cognome' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-(--fg3)">Obiettivo</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('dataUltimaVisita')">
                    Ultima visita
                    <component :is="iconaOrdinamento('dataUltimaVisita')" :size="12" :class="ordinaPer === 'dataUltimaVisita' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-(--fg3)">Prossima visita</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('piano')">
                    Piano alimentare
                    <component :is="iconaOrdinamento('piano')" :size="12" :class="ordinaPer === 'piano' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 text-(--fg3)" @click="ordina('statoAccount')">
                    Stato account
                    <component :is="iconaOrdinamento('statoAccount')" :size="12" :class="ordinaPer === 'statoAccount' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-right text-(--fg3)">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="paziente in paginaDati.contenuto" :key="paziente.id" class="hover:bg-(--soft)">
                <TableCell>
                  <div class="flex items-center gap-2.5">
                    <span
                      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[13px] font-semibold"
                      :class="classiAvatarPaziente(paziente)"
                    >
                      {{ inizialiPaziente(paziente.nome, paziente.cognome) }}
                    </span>
                    <div>
                      <!-- Nome non cliccabile: la scheda si apre dall'icona cartella, come nella tabella dei piani. -->
                      <div class="font-semibold text-(--fg)">{{ paziente.nome }} {{ paziente.cognome }}</div>
                      <!-- Solo l'email: col codice fiscale la tabella non entrava a 1280 px (serve per l'intestazione fissa).
                        Il CF resta cercabile e compare passando il mouse. -->
                      <div class="text-xs text-(--fg3)" :title="`Codice fiscale: ${paziente.codiceFiscale}`">{{ paziente.email }}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <template v-if="paziente.obiettivoUltimaVisita">
                    <div class="font-semibold text-(--fg)">{{ ETICHETTE_OBIETTIVO[paziente.obiettivoUltimaVisita] }}</div>
                    <div v-if="paziente.dataInizioObiettivo" class="text-xs text-(--fg3)">dal {{ formattaDataItalianaConMese(paziente.dataInizioObiettivo) }}</div>
                  </template>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell>
                  <template v-if="paziente.dataUltimaVisita">
                    <div class="font-semibold text-(--fg)">{{ testoUltimaVisita(paziente.dataUltimaVisita) }}</div>
                    <div v-if="notaUltimaVisita(paziente.dataUltimaVisita)" class="text-xs text-(--fg3)">
                      {{ notaUltimaVisita(paziente.dataUltimaVisita) }}
                    </div>
                  </template>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell>
                  <template v-if="paziente.prossimaVisita">
                    <div class="font-semibold text-(--fg)">{{ formattaDataItalianaConMese(paziente.prossimaVisita.slice(0, 10)) }}</div>
                    <div class="text-xs text-(--fg3)">{{ paziente.prossimaVisita.slice(11, 16) }}</div>
                  </template>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell>
                  <template v-if="paziente.pianoNome">
                    <div class="font-semibold" :class="CLASSE_TESTO_STATO_PIANO[paziente.pianoStato!]">{{ paziente.pianoNome }}</div>
                    <div class="text-xs text-(--fg3)">{{ notaPiano(paziente) }}</div>
                  </template>
                  <span v-else class="text-(--fg3)">—</span>
                </TableCell>
                <TableCell>
                  <PazienteStatoAccount
                    :paziente="paziente"
                    :sola-lettura="mostraArchiviati"
                    :invito-in-corso="invitiInCorso.has(paziente.id)"
                    @invita="onInvita(paziente)"
                  />
                </TableCell>
                <TableCell class="text-right">
                  <PazienteRigaAzioni
                    :paziente="paziente"
                    :mostra-archiviati="mostraArchiviati"
                    @archivia="onArchivia"
                    @de-archivia="onDeArchivia"
                  />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <StatoVuoto v-else-if="!filtriAttivi" :icona="Users" titolo="Nessun paziente presente" testo="Inizia aggiungendo il primo paziente per consultare la sua scheda o invitarlo." livello="h2">
        <Button as-child class="active:not-aria-[haspopup]:translate-y-0.5"><UserPlus /> <router-link to="/pazienti/nuovo">Nuovo paziente</router-link></Button>
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
