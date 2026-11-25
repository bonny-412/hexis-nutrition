<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue'
import { cerca as cercaAlimenti, type Alimento } from '@/api/alimenti'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { nomeAlimentoLeggibile } from '@/utils/testo'
import { formattaNumero } from '@/utils/visita'
import {
  filtraDecimaleItaliano, numeroItaliano, numeroItalianoOpzionale,
  erroreNomeAlimento, erroreNumeroDecimaleObbligatorio, erroreNumeroDecimale, bloccaTastoNonNumerico,
} from '@/utils/validators'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [valore: boolean]
  aggiunto: [riga: {
    alimentoId: string | null
    nome: string
    kcal100g: number
    proteine100g: number
    carboidrati100g: number
    grassi100g: number
    zuccheri100g: number | null
    fibre100g: number | null
    ferro100mg: number | null
    calcio100mg: number | null
    acqua100g: number | null
    grammi: number
  }]
}>()

const modo = ref<'cerca' | 'manuale'>('cerca')
const query = ref('')
const risultati = ref<Alimento[]>([])
let debounceHandle: ReturnType<typeof setTimeout> | undefined

const inputCerca = ref<{ $el: HTMLInputElement } | null>(null)

function focusRicerca(evento?: Event) {
  evento?.preventDefault()
  nextTick(() => inputCerca.value?.$el?.focus())
}

watch(modo, (valore) => {
  if (valore === 'cerca') focusRicerca()
})

/** Prima i nomi che iniziano con la ricerca, poi quelli che la contengono, infine i match sulla sola categoria. */
function punteggio(alimento: Alimento, testo: string): number {
  const nome = alimento.nome.toLocaleLowerCase('it-IT')
  if (nome.startsWith(testo)) return 0
  if (nome.split(/[\s,(]+/).some((parola) => parola.startsWith(testo))) return 1
  if (nome.includes(testo)) return 2
  return 3
}

async function eseguiRicerca(testo: string) {
  const ricerca = testo.trim()
  if (!ricerca) {
    risultati.value = []
    return
  }
  // Il server ordina per nome e include i match sulla categoria ("pasta per pane e pizza"): chiediamo
  // una pagina ampia e riordiniamo qui per pertinenza. Andrebbe spostato nel backend (ordinamento per rilevanza).
  const pagina = await cercaAlimenti({ ricerca, dimensione: 100 })
  const minuscolo = ricerca.toLocaleLowerCase('it-IT')
  risultati.value = pagina.contenuto
    .map((alimento, indice) => ({ alimento, indice, punti: punteggio(alimento, minuscolo) }))
    .sort((a, b) => a.punti - b.punti || a.indice - b.indice)
    .slice(0, 30)
    .map((r) => r.alimento)
}

function kcalPer100g(alimento: Alimento): string {
  return formattaNumero((alimento.kcal * 100) / alimento.quantitaG, 0)
}

watch(query, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => eseguiRicerca(valore), 300)
})
onUnmounted(() => clearTimeout(debounceHandle))

const manualeNome = ref('')
const manualeGrammi = ref('100')
const manualeKcal = ref('')
const manualeProteine = ref('')
const manualeCarboidrati = ref('')
const manualeGrassi = ref('')
const manualeZuccheri = ref('')
const manualeFibre = ref('')
const manualeFerro = ref('')
const manualeCalcio = ref('')
const manualeAcqua = ref('')
const errori = ref<Record<string, string | undefined>>({})

function resetStato() {
  modo.value = 'cerca'
  query.value = ''
  risultati.value = []
  manualeNome.value = ''
  manualeGrammi.value = '100'
  manualeKcal.value = ''
  manualeProteine.value = ''
  manualeCarboidrati.value = ''
  manualeGrassi.value = ''
  manualeZuccheri.value = ''
  manualeFibre.value = ''
  manualeFerro.value = ''
  manualeCalcio.value = ''
  manualeAcqua.value = ''
  errori.value = {}
}

function chiudi() {
  emit('update:open', false)
  resetStato()
}

function selezionaRisultato(alimento: Alimento) {
  const fattore = 100 / alimento.quantitaG
  emit('aggiunto', {
    alimentoId: alimento.id,
    nome: alimento.nome,
    kcal100g: alimento.kcal * fattore,
    proteine100g: alimento.proteineG * fattore,
    carboidrati100g: alimento.carboidratiG * fattore,
    grassi100g: alimento.grassiG * fattore,
    zuccheri100g: alimento.zuccheriG !== null ? alimento.zuccheriG * fattore : null,
    fibre100g: alimento.fibreG !== null ? alimento.fibreG * fattore : null,
    ferro100mg: alimento.ferroMg !== null ? alimento.ferroMg * fattore : null,
    calcio100mg: alimento.calcioMg !== null ? alimento.calcioMg * fattore : null,
    acqua100g: alimento.acquaG !== null ? alimento.acquaG * fattore : null,
    grammi: 100,
  })
  chiudi()
}

function aggiungiManuale() {
  const nuoviErrori: Record<string, string | undefined> = {
    nome: erroreNomeAlimento(manualeNome.value),
    grammi: erroreNumeroDecimaleObbligatorio(manualeGrammi.value),
    kcal: erroreNumeroDecimaleObbligatorio(manualeKcal.value),
    proteine: erroreNumeroDecimaleObbligatorio(manualeProteine.value),
    carboidrati: erroreNumeroDecimaleObbligatorio(manualeCarboidrati.value),
    grassi: erroreNumeroDecimaleObbligatorio(manualeGrassi.value),
    zuccheri: erroreNumeroDecimale(manualeZuccheri.value),
    fibre: erroreNumeroDecimale(manualeFibre.value),
    ferro: erroreNumeroDecimale(manualeFerro.value),
    calcio: erroreNumeroDecimale(manualeCalcio.value),
    acqua: erroreNumeroDecimale(manualeAcqua.value),
  }
  errori.value = nuoviErrori
  if (Object.values(nuoviErrori).some((e) => e !== undefined)) return

  emit('aggiunto', {
    alimentoId: null,
    nome: manualeNome.value.trim(),
    kcal100g: numeroItaliano(manualeKcal.value),
    proteine100g: numeroItaliano(manualeProteine.value),
    carboidrati100g: numeroItaliano(manualeCarboidrati.value),
    grassi100g: numeroItaliano(manualeGrassi.value),
    zuccheri100g: numeroItalianoOpzionale(manualeZuccheri.value) ?? null,
    fibre100g: numeroItalianoOpzionale(manualeFibre.value) ?? null,
    ferro100mg: numeroItalianoOpzionale(manualeFerro.value) ?? null,
    calcio100mg: numeroItalianoOpzionale(manualeCalcio.value) ?? null,
    acqua100g: numeroItalianoOpzionale(manualeAcqua.value) ?? null,
    grammi: numeroItalianoOpzionale(manualeGrammi.value) ?? 100,
  })
  chiudi()
}
</script>

<template>
  <Dialog :open="props.open" @update:open="(v) => !v && chiudi()">
    <!-- Ancorato in alto invece che centrato: con i risultati che arrivano il dialog non deve saltare. -->
    <DialogContent class="top-[10vh] translate-y-0 sm:max-w-lg" @open-auto-focus="focusRicerca">
      <DialogHeader>
        <DialogTitle>Aggiungi alimento</DialogTitle>
      </DialogHeader>

      <div class="flex gap-1 border-b border-(--div) px-1">
        <button
          data-test="tab-cerca"
          class="border-b-2 px-1 py-2 text-sm font-bold"
          :class="modo === 'cerca' ? 'border-(--green) text-(--green)' : 'border-transparent text-(--fg3)'"
          @click="modo = 'cerca'"
        >
          Cerca dal database
        </button>
        <button
          data-test="tab-manuale"
          class="border-b-2 px-1 py-2 text-sm font-bold"
          :class="modo === 'manuale' ? 'border-(--green) text-(--green)' : 'border-transparent text-(--fg3)'"
          @click="modo = 'manuale'"
        >
          Alimento manuale
        </button>
      </div>

      <div v-if="modo === 'cerca'" class="flex flex-col gap-3">
        <Input ref="inputCerca" v-model="query" autocomplete="off" placeholder="Cerca per nome o categoria…" />
        <div class="flex h-80 flex-col gap-1 overflow-y-auto">
          <p v-if="!query.trim()" class="p-4 text-center text-sm text-(--fg3)">
            Scrivi il nome di un alimento, per esempio «pasta» o «yogurt».
          </p>
          <button
            v-for="risultato in risultati"
            :key="risultato.id"
            data-test="risultato-ricerca"
            class="flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-left hover:bg-(--soft)"
            @click="selezionaRisultato(risultato)"
          >
            <span class="flex flex-col">
              <span class="text-sm font-semibold text-(--fg)">{{ nomeAlimentoLeggibile(risultato.nome) }}</span>
              <span class="text-xs text-(--fg3)">{{ risultato.categoria }}</span>
            </span>
            <span class="whitespace-nowrap text-xs font-bold tabular-nums text-(--fg3)">{{ kcalPer100g(risultato) }} kcal/100 g</span>
          </button>
          <p v-if="query.trim() && risultati.length === 0" class="p-4 text-center text-sm text-(--fg3)">
            Nessun alimento trovato.
          </p>
        </div>
      </div>

      <div v-else class="flex flex-col gap-3">
        <label class="flex flex-col gap-1">
          <span class="text-[13px] font-medium text-(--fg2)">Nome alimento</span>
          <Input data-test="manuale-nome" v-model="manualeNome" placeholder="Es. Insalata mista condita" :aria-invalid="!!errori.nome" />
          <span v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</span>
        </label>
        <div class="grid grid-cols-2 gap-3">
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Grammi</span>
            <Input
              data-test="manuale-grammi" type="text" inputmode="decimal" placeholder="Es. 100"
              :model-value="manualeGrammi" :aria-invalid="!!errori.grammi" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeGrammi = filtraDecimaleItaliano(String(v)); errori.grammi = undefined }"
            />
            <span v-if="errori.grammi" class="text-xs font-medium text-(--danger)">{{ errori.grammi }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Kcal</span>
            <Input
              data-test="manuale-kcal" type="text" inputmode="decimal" placeholder="Es. 372"
              :model-value="manualeKcal" :aria-invalid="!!errori.kcal" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeKcal = filtraDecimaleItaliano(String(v)); errori.kcal = undefined }"
            />
            <span v-if="errori.kcal" class="text-xs font-medium text-(--danger)">{{ errori.kcal }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Proteine (g)</span>
            <Input
              data-test="manuale-proteine" type="text" inputmode="decimal" placeholder="Es. 12,90"
              :model-value="manualeProteine" :aria-invalid="!!errori.proteine" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeProteine = filtraDecimaleItaliano(String(v)); errori.proteine = undefined }"
            />
            <span v-if="errori.proteine" class="text-xs font-medium text-(--danger)">{{ errori.proteine }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Carboidrati (g)</span>
            <Input
              data-test="manuale-carboidrati" type="text" inputmode="decimal" placeholder="Es. 65"
              :model-value="manualeCarboidrati" :aria-invalid="!!errori.carboidrati" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeCarboidrati = filtraDecimaleItaliano(String(v)); errori.carboidrati = undefined }"
            />
            <span v-if="errori.carboidrati" class="text-xs font-medium text-(--danger)">{{ errori.carboidrati }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Grassi (g)</span>
            <Input
              data-test="manuale-grassi" type="text" inputmode="decimal" placeholder="Es. 6,50"
              :model-value="manualeGrassi" :aria-invalid="!!errori.grassi" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeGrassi = filtraDecimaleItaliano(String(v)); errori.grassi = undefined }"
            />
            <span v-if="errori.grassi" class="text-xs font-medium text-(--danger)">{{ errori.grassi }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Zuccheri (g)</span>
            <Input
              type="text" inputmode="decimal" placeholder="Es. 1,10"
              :model-value="manualeZuccheri" :aria-invalid="!!errori.zuccheri" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeZuccheri = filtraDecimaleItaliano(String(v)); errori.zuccheri = undefined }"
            />
            <span v-if="errori.zuccheri" class="text-xs font-medium text-(--danger)">{{ errori.zuccheri }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Fibre (g)</span>
            <Input
              type="text" inputmode="decimal" placeholder="Es. 2"
              :model-value="manualeFibre" :aria-invalid="!!errori.fibre" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeFibre = filtraDecimaleItaliano(String(v)); errori.fibre = undefined }"
            />
            <span v-if="errori.fibre" class="text-xs font-medium text-(--danger)">{{ errori.fibre }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Ferro (mg)</span>
            <Input
              type="text" inputmode="decimal" placeholder="Es. 3,50"
              :model-value="manualeFerro" :aria-invalid="!!errori.ferro" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeFerro = filtraDecimaleItaliano(String(v)); errori.ferro = undefined }"
            />
            <span v-if="errori.ferro" class="text-xs font-medium text-(--danger)">{{ errori.ferro }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Calcio (mg)</span>
            <Input
              type="text" inputmode="decimal" placeholder="Es. 50"
              :model-value="manualeCalcio" :aria-invalid="!!errori.calcio" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeCalcio = filtraDecimaleItaliano(String(v)); errori.calcio = undefined }"
            />
            <span v-if="errori.calcio" class="text-xs font-medium text-(--danger)">{{ errori.calcio }}</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-(--fg2)">Acqua (g)</span>
            <Input
              type="text" inputmode="decimal" placeholder="Es. 60"
              :model-value="manualeAcqua" :aria-invalid="!!errori.acqua" @keydown="bloccaTastoNonNumerico"
              @update:model-value="(v) => { manualeAcqua = filtraDecimaleItaliano(String(v)); errori.acqua = undefined }"
            />
            <span v-if="errori.acqua" class="text-xs font-medium text-(--danger)">{{ errori.acqua }}</span>
          </label>
        </div>
        <p class="text-xs text-(--fg3)">Valori per 100 g.</p>
      </div>

      <DialogFooter v-if="modo === 'manuale'" class="flex items-center gap-2.5">
        <Button type="button" variant="neutral" @click="chiudi">Annulla</Button>
        <Button data-test="aggiungi-manuale" @click="aggiungiManuale">Aggiungi al pasto</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
