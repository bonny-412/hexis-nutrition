<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { cerca, type PaginaPianiAlimentari, type StatoPiano } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus } from '@lucide/vue'

const router = useRouter()

const CHIP_STATI: { valore: StatoPiano | 'TUTTI'; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BOZZA', etichetta: 'Bozza' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
  { valore: 'SCADUTO', etichetta: 'Scaduto' },
  { valore: 'TERMINATO', etichetta: 'Terminato' },
]

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoFiltro = ref<StatoPiano | 'TUTTI'>('TUTTI')
const pagina = ref(0)

const paginaDati = ref<PaginaPianiAlimentari | null>(null)
const caricamentoIniziale = ref(true)
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

function selezionaStato(valore: StatoPiano | 'TUTTI') {
  statoFiltro.value = valore
  pagina.value = 0
}

async function carica() {
  caricamentoIniziale.value = paginaDati.value === null
  errore.value = false
  try {
    paginaDati.value = await cerca({
      pagina: pagina.value,
      dimensione: 20,
      ricerca: ricercaEffettiva.value.trim() || undefined,
      stato: statoFiltro.value === 'TUTTI' ? undefined : statoFiltro.value,
    })
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
  }
}
watch([ricercaEffettiva, statoFiltro, pagina], carica)
onMounted(carica)

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}
function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}
function apriPiano(id: string) {
  router.push(`/piani-alimentari/${id}`)
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return ''
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} piani`
})
</script>

<template>
  <AppShell>
    <div class="flex flex-col gap-4 p-6">
      <div class="flex items-end justify-between gap-4">
        <h1 class="font-heading text-2xl italic text-(--fg)">Piani alimentari</h1>
        <Button @click="router.push('/piani-alimentari/nuovo')">
          <Plus :size="14" /> Crea nuovo piano
        </Button>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <Input v-model="ricercaInput" placeholder="Cerca paziente o piano…" class="min-w-60" />
        <button
          v-for="chip in CHIP_STATI" :key="chip.valore" :data-test="`chip-stato-${chip.valore}`"
          class="rounded-full border px-3 py-1 text-xs font-bold"
          :class="statoFiltro === chip.valore ? 'border-(--green) bg-(--green) text-(--on-green)' : 'border-(--bd2) bg-(--surf) text-(--fg2)'"
          @click="selezionaStato(chip.valore)"
        >
          {{ chip.etichetta }}
        </button>
        <span class="ml-auto text-xs text-(--fg3)">{{ conteggioTesto }}</span>
      </div>

      <div v-if="caricamentoIniziale" class="text-sm text-(--fg3)">Caricamento…</div>
      <div v-else-if="errore" class="text-sm text-(--danger)">Non è stato possibile caricare i piani.</div>
      <div v-else class="rounded-2xl border border-(--bd) bg-(--surf)">
        <Table v-if="paginaDati && paginaDati.contenuto.length > 0">
          <TableHeader>
            <TableRow>
              <TableHead>Paziente</TableHead>
              <TableHead>Piano</TableHead>
              <TableHead>Stato</TableHead>
              <TableHead class="text-right">Obiettivo</TableHead>
              <TableHead>Scadenza</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow
              v-for="riga in paginaDati.contenuto" :key="riga.id"
              class="cursor-pointer" @click="apriPiano(riga.id)"
            >
              <TableCell class="font-heading font-semibold">{{ riga.pazienteNomeCompleto }}</TableCell>
              <TableCell>{{ riga.nome }}</TableCell>
              <TableCell>{{ riga.stato }}</TableCell>
              <TableCell class="text-right">{{ riga.obiettivoKcal ?? '—' }} kcal</TableCell>
              <TableCell>{{ riga.dataFine ?? '—' }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <div v-else class="flex flex-col items-center gap-2 p-10 text-center">
          <p class="text-sm font-bold">Nessun piano trovato</p>
          <p class="text-xs text-(--fg3)">Prova a modificare la ricerca o i filtri.</p>
        </div>
      </div>

      <div v-if="paginaDati && paginaDati.totalePagine > 1" class="flex items-center justify-end gap-2">
        <Button variant="outline" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
        <Button variant="outline" size="sm" :disabled="pagina >= paginaDati.totalePagine - 1" @click="paginaSuccessiva">Successivo</Button>
      </div>
    </div>
  </AppShell>
</template>
