<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import {
  cerca, attiva, elimina,
  type PaginaPianiAlimentari, type PianoAlimentareRigaLista, type StatoPiano, type CriteriRicercaPianiAlimentari,
} from '@/api/pianiAlimentari'
import PianoAlimentareRigaAzioni from '@/components/pianiAlimentari/PianoAlimentareRigaAzioni.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus, ArrowUp, ArrowDown, ArrowUpDown } from '@lucide/vue'
import { formattaDataItaliana } from '@/utils/data'

type CampoOrdinamento = NonNullable<CriteriRicercaPianiAlimentari['ordinaPer']>

const CHIP_STATI: { valore: StatoPiano | 'TUTTI'; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BOZZA', etichetta: 'Bozza' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
  { valore: 'SCADUTO', etichetta: 'Scaduto' },
  { valore: 'TERMINATO', etichetta: 'Terminato' },
]

const ETICHETTE_STATO: Record<StatoPiano, string> = {
  BOZZA: 'Bozza',
  ATTIVO: 'Attivo',
  SCADUTO: 'Scaduto',
  TERMINATO: 'Terminato',
}

const CLASSI_STATO: Record<StatoPiano, string> = {
  BOZZA: 'bg-(--hover) text-(--fg4)',
  ATTIVO: 'bg-(--mint) text-(--green)',
  SCADUTO: 'bg-(--danger)/10 text-(--danger)',
  TERMINATO: 'bg-(--warn-bg) text-(--warn-fg)',
}

// Stesso avatar a iniziali della lista pazienti (PazientiListView.vue), colorato però con
// CLASSI_STATO (stato del piano) invece che con lo stato account, che qui non è disponibile.
function inizialiPaziente(nomeCompleto: string): string {
  const parti = nomeCompleto.trim().split(/\s+/)
  const prima = parti[0]?.[0] ?? ''
  const ultima = parti.length > 1 ? parti[parti.length - 1][0] : ''
  return `${prima}${ultima}`.toUpperCase()
}

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoFiltro = ref<StatoPiano | 'TUTTI'>('TUTTI')
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

const filtriAttivi = computed(() => ricercaEffettiva.value.trim() !== '' || statoFiltro.value !== 'TUTTI')

function selezionaStato(valore: StatoPiano | 'TUTTI') {
  statoFiltro.value = valore
  pagina.value = 0
}

function criteriCorrenti(): CriteriRicercaPianiAlimentari {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ordinaPer: ordinaPer.value,
    direzione: direzione.value,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    stato: statoFiltro.value === 'TUTTI' ? undefined : statoFiltro.value,
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
watch([ricercaEffettiva, statoFiltro, pagina, ordinaPer, direzione], carica)
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

function iconaOrdinamento(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) return ArrowUpDown
  return direzione.value === 'asc' ? ArrowUp : ArrowDown
}

function pulisciFiltri() {
  clearTimeout(debounceHandle)
  ricercaInput.value = ''
  ricercaEffettiva.value = ''
  statoFiltro.value = 'TUTTI'
  pagina.value = 0
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
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Piani alimentari</h1>
        <p class="mt-1 text-sm text-(--fg3)">Gestisci i piani alimentari dei tuoi pazienti.</p>
      </div>
      <Button as-child class="hover:bg-primary/80">
        <router-link to="/piani-alimentari/nuovo"><Plus :size="16" /> Nuovo piano</router-link>
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input v-model="ricercaInput" type="search" placeholder="Cerca paziente o piano…" class="min-w-70 flex-1" />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in CHIP_STATI" :key="chip.valore" type="button" :data-test="`chip-stato-${chip.valore}`"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="statoFiltro === chip.valore
              ? 'border-(--sage) bg-(--mint) text-(--green)'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="selezionaStato(chip.valore)"
          >
            {{ chip.etichetta }}
          </button>
        </div>
      </div>
    </section>

    <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)">
      <div v-if="errore" class="flex flex-col items-center gap-3 p-14 text-center">
        <p class="text-(--danger)">Non è stato possibile caricare i piani.</p>
        <Button type="button" variant="outline" @click="carica">Riprova</Button>
      </div>

      <div v-else-if="caricamentoIniziale" class="flex flex-col gap-2 p-4">
        <div v-for="n in 6" :key="n" class="h-9 animate-pulse rounded-lg bg-(--hover)" />
      </div>

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <div class="overflow-x-auto" :class="{ 'pointer-events-none opacity-60': aggiornamentoInCorso }">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="uppercase tracking-wide text-(--fg4)">Paziente</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 uppercase tracking-wide text-(--fg4)" @click="ordina('nome')">
                    Piano
                    <component :is="iconaOrdinamento('nome')" :size="12" :class="ordinaPer === 'nome' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="uppercase tracking-wide text-(--fg4)">Stato</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Obiettivo</TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 uppercase tracking-wide text-(--fg4)" @click="ordina('dataFine')">
                    Scadenza
                    <component :is="iconaOrdinamento('dataFine')" :size="12" :class="ordinaPer === 'dataFine' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="riga in paginaDati.contenuto" :key="riga.id" class="hover:bg-(--soft)">
                <TableCell>
                  <div class="flex items-center gap-2.5">
                    <span class="flex h-9 w-9 flex-none items-center justify-center rounded-full font-heading font-semibold" :class="CLASSI_STATO[riga.stato]">
                      {{ inizialiPaziente(riga.pazienteNomeCompleto) }}
                    </span>
                    <span class="font-heading font-semibold text-(--fg)">{{ riga.pazienteNomeCompleto }}</span>
                  </div>
                </TableCell>
                <TableCell>{{ riga.nome }}</TableCell>
                <TableCell><Badge :class="CLASSI_STATO[riga.stato]">{{ ETICHETTE_STATO[riga.stato] }}</Badge></TableCell>
                <TableCell class="text-right">{{ riga.obiettivoKcal ? `${riga.obiettivoKcal} kcal` : '—' }}</TableCell>
                <TableCell>{{ riga.dataFine ? formattaDataItaliana(riga.dataFine) : '—' }}</TableCell>
                <TableCell class="text-right">
                  <PianoAlimentareRigaAzioni :riga="riga" @attiva="onAttiva" @elimina="onElimina" />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <div v-else class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-heading text-lg italic">Nessun piano trovato</p>
        <p class="text-xs text-muted-foreground max-w-xs">Prova a modificare la ricerca o i filtri.</p>
        <Button v-if="filtriAttivi" type="button" variant="outline" @click="pulisciFiltri">Pulisci filtri</Button>
        <Button v-else as-child><router-link to="/piani-alimentari/nuovo">Nuovo piano</router-link></Button>
      </div>

      <div v-if="paginaDati && !errore" class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <div class="flex gap-2">
          <Button type="button" variant="neutral" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
          <Button
            type="button"
            variant="neutral"
            size="sm"
            :disabled="!paginaDati || pagina >= paginaDati.totalePagine - 1"
            @click="paginaSuccessiva"
          >
            Successivo
          </Button>
        </div>
      </div>
    </section>
  </AppShell>
</template>
