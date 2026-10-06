<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import {
  cerca, duplica,
  type ModalitaPiano, type PaginaPianiAlimentari, type PianoAlimentareRigaLista,
} from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { avanzamentoPiano } from '@/utils/pianoAlimentare'
import { formattaDataItalianaConMese } from '@/utils/data'
import { Copy, Search, Utensils } from '@lucide/vue'

const props = defineProps<{
  pazienteId: string
  archiviato: boolean
}>()

const ETICHETTE_MODALITA: Record<ModalitaPiano, string> = {
  PASTI: 'Piano con pasti',
  MACRO: 'Solo target macro',
  ESEMPI: 'Esempi intercambiabili',
}

const router = useRouter()

const DIMENSIONE_PAGINA = 5

// Un solo piano ATTIVO per paziente (lato backend): un piano oltre la data di fine arriva già
// come SCADUTO, quindi finisce nello storico e non nella card.
const pianoAttivo = ref<PianoAlimentareRigaLista | null>(null)
const pianiPrecedenti = ref<PaginaPianiAlimentari | null>(null)
const pagina = ref(0)
const caricamento = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)
const duplicazioneInCorso = ref(false)

const avanzamento = computed(() =>
  pianoAttivo.value ? avanzamentoPiano(pianoAttivo.value.dataInizio, pianoAttivo.value.dataFine) : null,
)

const totalePiani = computed(() => (pianoAttivo.value ? 1 : 0) + (pianiPrecedenti.value?.totaleElementi ?? 0))

const conteggioTesto = computed(() => {
  if (!pianiPrecedenti.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = pianiPrecedenti.value
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} piani`
})

async function carica() {
  if (pianiPrecedenti.value) aggiornamentoInCorso.value = true
  else caricamento.value = true
  errore.value = false
  try {
    const [attivo, precedenti] = await Promise.all([
      cerca({ pazienteId: props.pazienteId, stato: 'ATTIVO', dimensione: 1 }),
      cerca({
        pazienteId: props.pazienteId, escludiAttivo: true, escludiBozze: true, pagina: pagina.value, dimensione: DIMENSIONE_PAGINA,
        ordinaPer: 'dataInizio', direzione: 'desc',
      }),
    ])
    pianoAttivo.value = attivo.contenuto[0] ?? null
    pianiPrecedenti.value = precedenti
  } catch {
    errore.value = true
  } finally {
    caricamento.value = false
    aggiornamentoInCorso.value = false
  }
}
onMounted(carica)
watch(pagina, carica)

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}
function paginaSuccessiva() {
  if (pianiPrecedenti.value && pagina.value < pianiPrecedenti.value.totalePagine - 1) pagina.value += 1
}

function periodo(piano: PianoAlimentareRigaLista): string {
  const inizio = formattaDataItalianaConMese(piano.dataInizio)
  return piano.dataFine ? `${inizio} → ${formattaDataItalianaConMese(piano.dataFine)}` : `Dal ${inizio}`
}

function target(piano: PianoAlimentareRigaLista): string {
  return piano.obiettivoKcal != null ? `${Math.round(piano.obiettivoKcal)} kcal` : '—'
}

function linkPiano(piano: PianoAlimentareRigaLista): string {
  return `/piani-alimentari/${piano.id}?pazienteId=${props.pazienteId}`
}

async function onDuplica(piano: PianoAlimentareRigaLista) {
  duplicazioneInCorso.value = true
  try {
    const copia = await duplica(piano.id)
    toast.success('Piano duplicato come bozza.')
    await router.push(`/piani-alimentari/${copia.id}?pazienteId=${props.pazienteId}`)
  } catch {
    toast.error('Non è stato possibile duplicare il piano.')
  } finally {
    duplicazioneInCorso.value = false
  }
}
</script>

<template>
  <div v-if="caricamento" class="flex flex-col gap-4">
    <div v-for="n in 2" :key="n" data-test="piani-skeleton" class="h-28 animate-pulse rounded-2xl bg-(--hover)" />
  </div>

  <div v-else-if="errore" class="rounded-2xl border border-(--bd) bg-(--surf) p-8 text-center shadow-sm">
    <p class="text-sm text-(--fg3)">Non è stato possibile caricare i piani alimentari.</p>
    <Button variant="neutral" class="mt-3" @click="carica">Riprova</Button>
  </div>

  <div v-else class="flex flex-col gap-4">
    <div class="rounded-2xl border border-(--bd) bg-(--surf) px-5 py-4 shadow-sm">
      <template v-if="pianoAttivo && avanzamento">
        <div class="flex flex-wrap items-start gap-3.5">
          <div class="min-w-55 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-[10px] font-bold uppercase tracking-widest text-(--fg3)">Piano attivo</span>
              <Badge variant="secondary" class="bg-(--mint) text-(--green)">{{ ETICHETTE_MODALITA[pianoAttivo.modalita] }}</Badge>
            </div>
            <div class="font-heading mt-1 text-[21px] font-medium italic text-(--fg)">{{ pianoAttivo.nome }}</div>
            <div class="mt-0.5 text-[12.5px] text-(--fg3)">
              {{ periodo(pianoAttivo) }}<template v-if="pianoAttivo.obiettivoKcal != null"> · {{ target(pianoAttivo) }}/giorno</template>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button v-if="!archiviato" variant="neutral" :disabled="duplicazioneInCorso" @click="onDuplica(pianoAttivo)">
              <Copy :size="15" />
              <span>Duplica</span>
            </Button>
            <Button as-child>
              <router-link :to="linkPiano(pianoAttivo)">Apri piano</router-link>
            </Button>
          </div>
        </div>

        <div v-if="avanzamento.percentuale !== null" class="mt-3.5">
          <div class="mb-1 flex justify-between text-[11.5px] text-(--fg3)">
            <span>{{ avanzamento.durataLabel }}</span>
            <span
              class="font-bold"
              :class="avanzamento.scadenzaInGiornata || avanzamento.scadenzaScaduta ? 'text-(--warn-fg)' : ''"
            >{{ avanzamento.scadenzaLabel }}</span>
          </div>
          <div class="h-1.5 overflow-hidden rounded bg-(--div)">
            <div data-test="piano-avanzamento" class="h-full rounded bg-(--green)" :style="{ width: `${avanzamento.percentuale}%` }" />
          </div>
        </div>
      </template>

      <div v-else class="flex flex-wrap items-center gap-3.5">
        <div class="min-w-55 flex-1">
          <span class="text-[10px] font-bold uppercase tracking-widest text-(--fg3)">Piano attivo</span>
          <div class="font-heading mt-1 text-lg italic text-(--fg)">Nessun piano attivo</div>
          <p class="mt-0.5 text-[12.5px] text-(--fg3)">
            {{ totalePiani > 0 ? 'Apri un piano dallo storico per attivarlo, oppure creane uno nuovo.' : 'Questo paziente non ha ancora nessun piano alimentare.' }}
          </p>
        </div>
        <Button v-if="!archiviato" as-child>
          <router-link :to="`/piani-alimentari/nuovo?pazienteId=${pazienteId}`">
            <Utensils :size="15" />
            <span>Nuovo piano</span>
          </router-link>
        </Button>
      </div>
    </div>

    <div v-if="pianiPrecedenti && pianiPrecedenti.contenuto.length > 0" class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-sm">
      <div class="font-heading border-b border-(--div) px-4.5 py-3.5 text-[15px] font-semibold">Piani precedenti</div>
      <Table class="min-w-150" :class="aggiornamentoInCorso ? 'opacity-60' : ''">
        <TableHeader>
          <TableRow class="bg-(--soft)">
            <TableHead class="uppercase tracking-wide text-(--fg4)">Piano</TableHead>
            <TableHead class="uppercase tracking-wide text-(--fg4)">Tipo</TableHead>
            <TableHead class="uppercase tracking-wide text-(--fg4)">Periodo</TableHead>
            <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Target</TableHead>
            <TableHead class="w-20" />
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="piano in pianiPrecedenti.contenuto" :key="piano.id" data-test="piano-precedente">
            <TableCell class="font-semibold text-(--fg)">{{ piano.nome }}</TableCell>
            <TableCell class="text-(--fg2)">{{ ETICHETTE_MODALITA[piano.modalita] }}</TableCell>
            <TableCell class="text-(--fg2)">{{ periodo(piano) }}</TableCell>
            <TableCell class="text-right tabular-nums text-(--fg2)">{{ target(piano) }}</TableCell>
            <TableCell class="text-right">
              <Button variant="neutral" size="icon-sm" as-child>
                <router-link :to="linkPiano(piano)" aria-label="Apri piano" title="Apri piano"><Search :size="15" /></router-link>
              </Button>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
      <div class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <div class="flex gap-2">
          <Button type="button" variant="neutral" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
          <Button type="button" variant="neutral" size="sm" :disabled="pagina >= pianiPrecedenti.totalePagine - 1" @click="paginaSuccessiva">Successivo</Button>
        </div>
      </div>
    </div>
  </div>
</template>
