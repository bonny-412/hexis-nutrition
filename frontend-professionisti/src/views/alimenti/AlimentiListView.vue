<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { cerca, elimina, type Alimento, type PaginaAlimenti, type CriteriRicercaAlimenti } from '@/api/alimenti'
import AlimentoRigaAzioni from '@/components/alimenti/AlimentoRigaAzioni.vue'
import AlimentoFormDialog from '@/components/alimenti/AlimentoFormDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus } from '@lucide/vue'

const chipFonte: { valore: NonNullable<CriteriRicercaAlimenti['fonte']>; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BDA', etichetta: 'BDA' },
  { valore: 'PERSONALIZZATI', etichetta: 'Personalizzati' },
]

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const fonte = ref<NonNullable<CriteriRicercaAlimenti['fonte']>>('TUTTI')
const pagina = ref(0)

const paginaDati = ref<PaginaAlimenti | null>(null)
const caricamentoIniziale = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)

const dialogAperto = ref(false)
const alimentoSelezionato = ref<Alimento | null>(null)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

watch(ricercaInput, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => {
    ricercaEffettiva.value = valore
    pagina.value = 0
  }, 300)
})

onUnmounted(() => clearTimeout(debounceHandle))

function criteriCorrenti(): CriteriRicercaAlimenti {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    fonte: fonte.value,
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

watch([ricercaEffettiva, fonte, pagina], carica)

onMounted(carica)

function selezionaFonte(valore: typeof fonte.value) {
  fonte.value = valore
  pagina.value = 0
}

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}

function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}

function apriCreazione() {
  alimentoSelezionato.value = null
  dialogAperto.value = true
}

function apriDettaglio(alimento: Alimento) {
  alimentoSelezionato.value = alimento
  dialogAperto.value = true
}

async function onSalvato() {
  dialogAperto.value = false
  await carica()
}

async function onDuplicato() {
  await carica()
}

async function onEliminato(alimento: Alimento) {
  try {
    await elimina(alimento.id)
    await carica()
    toast.success('Alimento eliminato.')
  } catch {
    toast.error("Non è stato possibile eliminare l'alimento.")
  }
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return 'Nessun alimento trovato'
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} alimenti · valori per 100 g`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Alimenti</h1>
        <p class="mt-1 text-sm text-(--fg3)">Banca Dati Alimenti in sola lettura e i tuoi alimenti personalizzati, in un'unica tabella.</p>
      </div>
      <Button class="hover:bg-primary/80" @click="apriCreazione">
        <Plus :size="16" /> Nuovo alimento
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input v-model="ricercaInput" type="search" placeholder="Cerca alimento o categoria…" class="min-w-70 flex-1" />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in chipFonte"
            :key="chip.valore"
            type="button"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="fonte === chip.valore
              ? 'border-(--sage) bg-(--mint) text-(--green)'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="selezionaFonte(chip.valore)"
          >
            {{ chip.etichetta }}
          </button>
        </div>
      </div>
    </section>

    <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)">
      <div v-if="errore" class="flex flex-col items-center gap-3 p-14 text-center">
        <p class="text-(--danger)">Non è stato possibile caricare gli alimenti.</p>
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
                <TableHead class="uppercase tracking-wide text-(--fg4)">Alimento</TableHead>
                <TableHead class="uppercase tracking-wide text-(--fg4)">Fonte</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">kcal</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Prot.</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Carb.</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Grassi</TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow
                v-for="alimento in paginaDati.contenuto"
                :key="alimento.id"
                class="cursor-pointer hover:bg-(--soft)"
                @click="apriDettaglio(alimento)"
              >
                <TableCell>
                  <div class="flex flex-col gap-0.5">
                    <span class="font-heading font-semibold text-(--fg)">{{ alimento.nome }}</span>
                    <span class="text-xs text-(--fg3)">{{ alimento.categoria }}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <span
                    class="rounded px-2 py-0.5 text-[10.5px] font-bold tracking-wide"
                    :class="alimento.bda ? 'bg-(--mint) text-(--green)' : 'border border-(--green) text-(--green)'"
                  >
                    {{ alimento.bda ? 'BDA' : 'Personalizzato' }}
                  </span>
                </TableCell>
                <TableCell class="text-right font-semibold tabular-nums">{{ alimento.kcal.toFixed(0) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ alimento.proteineG.toFixed(1) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ alimento.carboidratiG.toFixed(1) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ alimento.grassiG.toFixed(1) }}</TableCell>
                <TableCell class="text-right" @click.stop>
                  <AlimentoRigaAzioni :alimento="alimento" @apri="apriDettaglio" @elimina="onEliminato" />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <div v-else class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-bold">Nessun alimento trovato</p>
        <p class="max-w-xs text-xs text-muted-foreground">Prova a modificare la ricerca o i filtri.</p>
      </div>

      <div v-if="paginaDati && !errore" class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <div class="flex gap-2">
          <Button type="button" variant="outline" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            :disabled="!paginaDati || pagina >= paginaDati.totalePagine - 1"
            @click="paginaSuccessiva"
          >
            Successivo
          </Button>
        </div>
      </div>
    </section>

    <AlimentoFormDialog
      v-model:open="dialogAperto"
      :alimento="alimentoSelezionato"
      @salvato="onSalvato"
      @duplicato="onDuplicato"
    />
  </AppShell>
</template>
