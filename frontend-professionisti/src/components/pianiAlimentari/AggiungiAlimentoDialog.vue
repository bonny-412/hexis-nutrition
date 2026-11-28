<script setup lang="ts">
import { Plus, X } from '@lucide/vue'
import { ref, watch, nextTick } from 'vue'
import type { Alimento } from '@/api/alimenti'
import { useRicercaAlimenti } from '@/composables/useRicercaAlimenti'
import { datiRigaDaAlimento } from '@/utils/sostituzioneAlimento'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
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

type Modo = 'cerca' | 'manuale'
const LINGUETTE: { valore: Modo; etichetta: string }[] = [
  { valore: 'cerca', etichetta: 'Cerca dal database' },
  { valore: 'manuale', etichetta: 'Alimento manuale' },
]
const modo = ref<Modo>('cerca')
const { query, risultati, azzera } = useRicercaAlimenti()

const inputCerca = ref<{ $el: HTMLInputElement } | null>(null)

function focusRicerca(evento?: Event) {
  evento?.preventDefault()
  nextTick(() => inputCerca.value?.$el?.focus())
}

watch(modo, (valore) => {
  if (valore === 'cerca') focusRicerca()
})

function kcalPer100g(alimento: Alimento): string {
  return formattaNumero((alimento.kcal * 100) / alimento.quantitaG, 0)
}

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
  azzera()
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
  emit('aggiunto', { ...datiRigaDaAlimento(alimento), grammi: 100 })
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
    <DialogContent class="sm:top-[10dvh] sm:max-h-[calc(90dvh-1rem)] sm:translate-y-0 sm:max-w-lg" @open-auto-focus="focusRicerca">
      <DialogHeader class="shrink-0">
        <DialogTitle>Aggiungi alimento</DialogTitle>
      </DialogHeader>

      <!-- Le Tabs reggono solo le linguette: i due contenuti sotto restano legati a `modo`. -->
      <Tabs :model-value="modo" class="shrink-0" @update:model-value="(v) => (modo = v as Modo)">
        <TabsList variant="line" class="h-auto w-full justify-start gap-1 border-b border-(--div) px-1 py-0">
          <TabsTrigger
            v-for="t in LINGUETTE"
            :key="t.valore"
            :value="t.valore"
            :data-test="`tab-${t.valore}`"
            class="-mb-px h-auto flex-none rounded-none border-0 border-b-2 after:hidden border-transparent px-1 py-2 font-bold text-(color:--fg3) data-active:border-(--green) data-active:text-(color:--green) data-active:hover:text-(color:--green) dark:text-(color:--fg3) dark:data-active:hover:text-(color:--green) dark:group-data-[variant=line]/tabs-list:data-active:border-(--green) dark:data-active:text-(color:--green)"
          >
            {{ t.etichetta }}
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <div v-if="modo === 'cerca'" class="flex min-h-0 flex-1 flex-col gap-3">
        <Input ref="inputCerca" v-model="query" class="shrink-0" autocomplete="off" placeholder="Cerca per nome o categoria…" />
        <div class="flex h-80 min-h-0 flex-col gap-1 overflow-y-auto overscroll-contain">
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

      <div v-else class="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto overscroll-contain px-1 py-1">
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
        <Button type="button" variant="neutral" @click="chiudi"><X /> Annulla</Button>
        <Button data-test="aggiungi-manuale" @click="aggiungiManuale"><Plus /> Aggiungi al pasto</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
