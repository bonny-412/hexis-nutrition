<script setup lang="ts">
import { useErroreConnessione } from '@/composables/useErroreConnessione'
import { TONI_STATO } from '@/utils/toniStato'
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { cerca, elimina, type Alimento, type PaginaAlimenti, type CriteriRicercaAlimenti } from '@/api/alimenti'
import AlimentoRigaAzioni from '@/components/alimenti/AlimentoRigaAzioni.vue'
import AlimentoFormDialog from '@/components/alimenti/AlimentoFormDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Apple, ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, FilterX, Plus, SearchX } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import ErroreLista from '@/components/liste/ErroreLista.vue'
import CaricamentoLista from '@/components/liste/CaricamentoLista.vue'
import { nomeAlimentoLeggibile } from '@/utils/testo'
import { formattaNumero } from '@/utils/visita'

type CampoOrdinamento = NonNullable<CriteriRicercaAlimenti['ordinaPer']>

const chipFonte: { valore: NonNullable<CriteriRicercaAlimenti['fonte']>; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'BDA', etichetta: 'BDA' },
  { valore: 'PERSONALIZZATI', etichetta: 'Personalizzati' },
]

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const fonte = ref<NonNullable<CriteriRicercaAlimenti['fonte']>>('TUTTI')
const filtriAttivi = computed(() => ricercaEffettiva.value.trim() !== '' || fonte.value !== 'TUTTI')
const pagina = ref(0)
const ordinaPer = ref<CampoOrdinamento | undefined>(undefined)
const direzione = ref<'asc' | 'desc'>('asc')

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
    ordinaPer: ordinaPer.value,
    direzione: direzione.value,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    fonte: fonte.value,
  }
}

const segnalaErroreConnessione = useErroreConnessione()

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
  } catch (causa) {
    errore.value = true
    segnalaErroreConnessione(causa)
  } finally {
    caricamentoIniziale.value = false
    aggiornamentoInCorso.value = false
  }
}

watch([ricercaEffettiva, fonte, pagina, ordinaPer, direzione], carica)

onMounted(carica)

const route = useRoute()
const router = useRouter()
watch(
  () => route.query.azione,
  (azione) => {
    if (azione === 'nuovo') {
      apriCreazione()
      router.replace({ query: { ...route.query, azione: undefined } })
    }
  },
  { immediate: true },
)

function selezionaFonte(valore: typeof fonte.value) {
  fonte.value = valore
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

function iconaOrdinamento(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) return ArrowUpDown
  return direzione.value === 'asc' ? ArrowUp : ArrowDown
}

function pulisciFiltri() {
  clearTimeout(debounceHandle)
  ricercaInput.value = ''
  ricercaEffettiva.value = ''
  fonte.value = 'TUTTI'
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
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} alimenti`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Alimenti</h1>
        <p class="mt-1 text-sm text-(--fg3)">Banca Dati Alimenti in sola lettura e i tuoi alimenti personalizzati, in un'unica tabella.</p>
      </div>
      <Button @click="apriCreazione">
        <Plus :size="16" /> Nuovo alimento
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input v-model="ricercaInput" type="search" autocomplete="off" aria-label="Cerca alimento o categoria" placeholder="Cerca alimento o categoria…" class="min-w-70 flex-1" />
        <div class="flex flex-wrap gap-1.5">
          <Button
            v-for="chip in chipFonte"
            :key="chip.valore"
            type="button"
            variant="chip"
            size="chip"
            :aria-pressed="fonte === chip.valore"
            @click="selezionaFonte(chip.valore)"
          >
            {{ chip.etichetta }}
          </Button>
        </div>
      </div>
    </section>

    <section class="overflow-clip rounded-2xl border border-(--bd) bg-(--surf)">
      <ErroreLista v-if="errore" messaggio="Non è stato possibile caricare gli alimenti." @riprova="carica" />
      <CaricamentoLista v-else-if="caricamentoIniziale" etichetta="Caricamento gli alimenti" />

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <!-- Mobile: righe compatte con kcal in evidenza; su desktop la tabella, adatta a confrontare numeri in colonna. -->
        <ul
          data-test="righe-alimenti-mobile"
          class="divide-y divide-(--div) md:hidden"
          :class="{ 'pointer-events-none opacity-60': aggiornamentoInCorso }"
        >
          <li v-for="alimento in paginaDati.contenuto" :key="alimento.id" class="relative flex items-start gap-3 px-4 py-3">
            <div class="min-w-0 flex-1">
              <button
                type="button"
                class="block w-full truncate text-left font-semibold text-(--fg) after:absolute after:inset-0"
                @click="apriDettaglio(alimento)"
              >
                {{ nomeAlimentoLeggibile(alimento.nome) }}
              </button>
              <div class="truncate text-xs text-(--fg3)">
                <span
                  class="mr-1 rounded px-1.5 py-px text-[10.5px] font-bold"
                  :class="alimento.bda ? TONI_STATO.neutro : TONI_STATO.attivo"
                >{{ alimento.bda ? 'BDA' : 'Personalizzato' }}</span>
                {{ alimento.categoria }}
              </div>
              <div class="mt-1 flex gap-3 text-xs tabular-nums text-(--fg2)">
                <span>Prot. {{ formattaNumero(alimento.proteineG, 1) }} g</span>
                <span>Carb. {{ formattaNumero(alimento.carboidratiG, 1) }} g</span>
                <span>Grassi {{ formattaNumero(alimento.grassiG, 1) }} g</span>
              </div>
            </div>
            <div class="text-right">
              <div class="font-semibold tabular-nums text-(--fg)">{{ formattaNumero(alimento.kcal, 0) }}</div>
              <div class="text-[11px] text-(--fg3)">kcal<template v-if="!alimento.bda"> / {{ alimento.quantitaG.toFixed(0) }} g</template></div>
            </div>
            <div class="relative z-10 -mr-2">
              <AlimentoRigaAzioni :alimento="alimento" @apri="apriDettaglio" @elimina="onEliminato" />
            </div>
          </li>
        </ul>

        <div class="hidden overflow-x-auto md:block xl:overflow-x-visible" :class="{ 'pointer-events-none opacity-60': aggiornamentoInCorso }">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <Button type="button" variant="ghost" size="xs" class="-ml-2 text-[length:inherit] font-medium text-(--fg3)" @click="ordina('nome')">
                    Alimento
                    <component :is="iconaOrdinamento('nome')" :size="12" :class="ordinaPer === 'nome' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </Button>
                </TableHead>
                <TableHead class="text-(--fg3)">Fonte</TableHead>
                <TableHead class="text-right text-(--fg3)">kcal</TableHead>
                <TableHead class="text-right text-(--fg3)">Prot. g</TableHead>
                <TableHead class="text-right text-(--fg3)">Carb. g</TableHead>
                <TableHead class="text-right text-(--fg3)">Grassi g</TableHead>
                <TableHead class="text-right text-(--fg3)">Azioni</TableHead>
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
                    <span class="font-semibold text-(--fg)">{{ nomeAlimentoLeggibile(alimento.nome) }}</span>
                    <span class="text-xs text-(--fg3)">
                      {{ alimento.categoria }}<template v-if="!alimento.bda"> · {{ alimento.quantitaG.toFixed(0) }} g</template>
                    </span>
                  </div>
                </TableCell>
                <TableCell>
                  <span
                    class="rounded px-2 py-0.5 text-[10.5px] font-bold tracking-wide"
                    :class="alimento.bda ? TONI_STATO.neutro : TONI_STATO.attivo"
                  >
                    {{ alimento.bda ? 'BDA' : 'PERSONALIZZATO' }}
                  </span>
                </TableCell>
                <TableCell class="text-right font-semibold tabular-nums">{{ formattaNumero(alimento.kcal, 0) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ formattaNumero(alimento.proteineG, 1) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ formattaNumero(alimento.carboidratiG, 1) }}</TableCell>
                <TableCell class="text-right text-(--fg2) tabular-nums">{{ formattaNumero(alimento.grassiG, 1) }}</TableCell>
                <TableCell class="text-right" @click.stop>
                  <AlimentoRigaAzioni :alimento="alimento" @apri="apriDettaglio" @elimina="onEliminato" />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <StatoVuoto v-else-if="filtriAttivi" :icona="SearchX" titolo="Nessun alimento trovato" testo="Prova a modificare la ricerca o i filtri." livello="h2">
        <Button type="button" variant="neutral" @click="pulisciFiltri"><FilterX /> Pulisci filtri</Button>
      </StatoVuoto>
      <StatoVuoto v-else :icona="Apple" titolo="Nessun alimento trovato" testo="Puoi aggiungere un alimento personalizzato." livello="h2">
        <Button type="button" @click="apriCreazione"><Plus /> Nuovo alimento</Button>
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

    <AlimentoFormDialog
      v-model:open="dialogAperto"
      :alimento="alimentoSelezionato"
      @salvato="onSalvato"
      @duplicato="onDuplicato"
    />
  </AppShell>
</template>
