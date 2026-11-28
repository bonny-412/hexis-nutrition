<script setup lang="ts">
import { Check, X } from '@lucide/vue'
import { computed, nextTick, ref, watch } from 'vue'
import type { Alimento } from '@/api/alimenti'
import { useRicercaAlimenti } from '@/composables/useRicercaAlimenti'
import {
  VALORI_MANTENIBILI, datiRigaDaAlimento, differenze, grammiEquivalenti, primoValoreDisponibile,
  valoriRiga, valorePer100g,
  type DatiAlimentoRiga, type RigaAlimento, type ValoreMantenuto,
} from '@/utils/sostituzioneAlimento'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import SelettoreSegmentato from '@/components/ui/SelettoreSegmentato.vue'
import { nomeAlimentoLeggibile } from '@/utils/testo'
import { formattaNumero } from '@/utils/visita'

const props = defineProps<{ open: boolean; riga: RigaAlimento | null; altreRigheUguali: number }>()
const emit = defineEmits<{
  'update:open': [valore: boolean]
  sostituito: [sostituto: DatiAlimentoRiga, mantenuto: ValoreMantenuto, ancheAltreRighe: boolean]
}>()

const { query, risultati, azzera } = useRicercaAlimenti()
const inputCerca = ref<{ $el: HTMLInputElement } | null>(null)

const mantenuto = ref<ValoreMantenuto>('kcal')
const selezionato = ref<Alimento | null>(null)
const ancheAltreRighe = ref(false)

// Il campo di ricerca esiste solo se la riga ha un valore da mantenere: altrimenti Reka porta il focus sul primo elemento focalizzabile.
function focusRicerca(evento?: Event) {
  if (primoDisponibile.value === null) return
  evento?.preventDefault()
  nextTick(() => inputCerca.value?.$el?.focus())
}

// Copia della riga e del conteggio al momento dell'apertura: la conferma muta la riga del piano prima che l'animazione di chiusura finisca.
const rigaIniziale = ref<RigaAlimento | null>(null)
const altreRigheIniziali = ref(0)

// Abbreviazioni come nelle colonne della tabella del piano.
const COLONNE: { valore: ValoreMantenuto; intestazione: string; suffisso: string }[] = [
  { valore: 'kcal', intestazione: 'Kcal', suffisso: 'kcal' },
  { valore: 'proteine', intestazione: 'Prot. g', suffisso: 'g prot.' },
  { valore: 'carboidrati', intestazione: 'Carb. g', suffisso: 'g carb.' },
  { valore: 'grassi', intestazione: 'Gras. g', suffisso: 'g grassi' },
]

const valoriPartenza = computed(() => (rigaIniziale.value ? valoriRiga(rigaIniziale.value) : null))
const primoDisponibile = computed(() => (rigaIniziale.value ? primoValoreDisponibile(rigaIniziale.value) : null))
const grammiPartenzaTesto = computed(() => String(rigaIniziale.value?.grammi ?? 0).replace('.', ','))

const opzioniMantenuto = computed(() =>
  VALORI_MANTENIBILI.map((v) => ({
    valore: v.valore,
    etichetta: COLONNE.find((c) => c.valore === v.valore)!.intestazione,
    disabilitata: !valoriPartenza.value || valoriPartenza.value[v.valore] <= 0,
  })),
)

function resetStato() {
  azzera()
  rigaIniziale.value = props.riga ? { ...props.riga } : null
  altreRigheIniziali.value = props.altreRigheUguali
  selezionato.value = null
  ancheAltreRighe.value = false
  mantenuto.value = rigaIniziale.value && valoriPartenza.value && valoriPartenza.value.kcal > 0
    ? 'kcal'
    : (primoDisponibile.value ?? 'kcal')
}

watch(() => props.open, (aperto) => {
  if (aperto) resetStato()
})

// Cambiando il valore da mantenere, il risultato scelto che non permette più il calcolo dei grammi si deseleziona.
watch(mantenuto, () => {
  if (selezionato.value && grammiPer(selezionato.value) === null) selezionato.value = null
})

function grammiPer(alimento: Alimento): number | null {
  if (!valoriPartenza.value) return null
  return grammiEquivalenti(
    valoriPartenza.value[mantenuto.value],
    valorePer100g(datiRigaDaAlimento(alimento), mantenuto.value),
  )
}

// Svuotando il testo i risultati vecchi restano fino al debounce: senza testo non si mostra nulla.
const risultatiVisibili = computed(() => (query.value.trim() ? risultati.value : []))

const voceMantenuto = computed(() => VALORI_MANTENIBILI.find((v) => v.valore === mantenuto.value)!)
// Solo i grammi: il valore mantenuto è uguale per tutti i risultati e si legge già nella colonna evidenziata.
const righeRisultato = computed(() =>
  risultatiVisibili.value.map((alimento) => ({ alimento, grammi: grammiPer(alimento) })),
)

const sostituto = computed(() => {
  const alimento = selezionato.value
  if (!alimento) return null
  const grammi = grammiPer(alimento)
  if (grammi === null) return null
  const dati = datiRigaDaAlimento(alimento)
  return { dati, grammi, valori: valoriRiga({ ...dati, grammi }) }
})

const differenzeSostituto = computed(() => {
  if (!sostituto.value || !valoriPartenza.value) return []
  return differenze(valoriPartenza.value, sostituto.value.valori, mantenuto.value)
})

function testoDifferenza(d: { chiave: ValoreMantenuto; delta: number }): string {
  const segno = d.delta > 0 ? '+' : '−'
  const suffisso = COLONNE.find((c) => c.valore === d.chiave)!.suffisso
  return `${segno}${formattaNumero(Math.abs(d.delta), 0)} ${suffisso}`
}

const righeConfronto = computed(() => {
  if (!rigaIniziale.value || !valoriPartenza.value) return []
  const elenco = [{
    chiave: 'partenza',
    titolo: 'Da sostituire',
    nome: nomeAlimentoLeggibile(rigaIniziale.value.nome),
    grammi: rigaIniziale.value.grammi,
    valori: valoriPartenza.value,
  }]
  if (sostituto.value) {
    elenco.push({
      chiave: 'sostituto',
      titolo: 'Sostituto',
      nome: nomeAlimentoLeggibile(sostituto.value.dati.nome),
      grammi: sostituto.value.grammi,
      valori: sostituto.value.valori,
    })
  }
  return elenco
})

const testoCasella = computed(() => {
  const nome = rigaIniziale.value ? nomeAlimentoLeggibile(rigaIniziale.value.nome) : ''
  return altreRigheIniziali.value === 1
    ? `Sostituisci anche nell'altra riga con «${nome}»`
    : `Sostituisci anche nelle altre ${altreRigheIniziali.value} righe con «${nome}»`
})

function chiudi() {
  emit('update:open', false)
}

function conferma() {
  if (!sostituto.value) return
  emit('sostituito', sostituto.value.dati, mantenuto.value, altreRigheIniziali.value > 0 && ancheAltreRighe.value)
  chiudi()
}

function classeColonna(valore: ValoreMantenuto): string {
  return valore === mantenuto.value ? 'bg-(--mint) font-bold text-(--green-d)' : 'text-(--fg2)'
}
</script>

<template>
  <Dialog :open="props.open" @update:open="(v) => !v && chiudi()">
    <!-- Ancorato in alto invece che centrato: con i risultati che arrivano il dialog non deve saltare. -->
    <DialogContent class="sm:top-[10dvh] sm:max-h-[calc(90dvh-1rem)] sm:translate-y-0 sm:max-w-xl" @open-auto-focus="focusRicerca">
      <DialogHeader class="shrink-0">
        <DialogTitle>Sostituisci alimento</DialogTitle>
      </DialogHeader>

      <div v-if="rigaIniziale" class="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto overscroll-contain px-1 py-1">
        <!-- Confronto: stesse colonne per la riga di partenza e per il sostituto, il valore mantenuto evidenziato in entrambe.
             Le intestazioni delle colonne sono il selettore "Mantieni uguale": la colonna scelta è quella che resta uguale. -->
        <div class="flex flex-col gap-1 rounded-xl border border-(--div) bg-(--surf) p-2" data-test="confronto">
          <div class="flex flex-col gap-1.5 px-2 sm:flex-row sm:items-center sm:gap-3">
            <span class="flex-1 text-[13px] font-medium text-(--fg2)">
              {{ primoDisponibile !== null ? 'Mantieni uguale' : '' }}
            </span>
            <!-- Stesso controllo di "Tabella/Schede" nelle liste, a quattro colonne allineate ai valori sotto. -->
            <SelettoreSegmentato
              v-model="mantenuto"
              etichetta="Mantieni uguale"
              :opzioni="opzioniMantenuto"
              prefisso-test="mantieni"
              class="grid w-full grid-cols-4 gap-0.5 sm:w-72"
            />
          </div>

          <div
            v-for="r in righeConfronto"
            :key="r.chiave"
            :data-test="`riga-${r.chiave}`"
            class="flex flex-col gap-1.5 rounded-lg px-2 py-2 sm:flex-row sm:items-center sm:gap-3"
            :class="r.chiave === 'sostituto' ? 'border border-(--green)/40 bg-(--bg)' : ''"
          >
            <div class="flex min-w-0 flex-1 flex-col">
              <span class="text-xs text-(--fg3)">{{ r.titolo }}</span>
              <span class="truncate text-sm font-semibold text-(--fg)">{{ r.nome }}</span>
              <span class="text-xs font-medium tabular-nums text-(--fg2)">{{ r.chiave === 'partenza' ? grammiPartenzaTesto : formattaNumero(r.grammi, 0) }} g</span>
            </div>
            <div class="grid w-full grid-cols-4 gap-1 px-0.5 text-center text-sm tabular-nums sm:w-72">
              <span v-for="c in COLONNE" :key="c.valore" class="rounded-md px-2 py-1" :class="classeColonna(c.valore)">
                {{ formattaNumero(r.valori[c.valore], 0) }}
              </span>
            </div>
          </div>

          <p v-if="sostituto" class="px-2 pb-1 pt-0.5 text-xs font-medium tabular-nums text-(--fg2)" data-test="differenze">
            <template v-if="differenzeSostituto.length">
              {{ differenzeSostituto.map(testoDifferenza).join(' · ') }}
            </template>
            <template v-else>Nessuna differenza sugli altri valori.</template>
          </p>
        </div>

        <p v-if="primoDisponibile === null" class="rounded-lg bg-(--soft) p-3 text-sm text-(--fg2)" data-test="nessun-valore">
          <template v-if="rigaIniziale.grammi <= 0">La riga ha 0 g: inserisci i grammi prima di sostituire.</template>
          <template v-else>Questo alimento non ha kcal né macronutrienti: non c'è un valore da mantenere.</template>
        </p>

        <template v-else>
          <div class="flex min-h-0 flex-col gap-2">
            <Input ref="inputCerca" v-model="query" class="shrink-0" autocomplete="off" placeholder="Cerca per nome o categoria…" />
            <div class="flex h-56 min-h-0 flex-col gap-1 overflow-y-auto overscroll-contain">
              <p v-if="!query.trim()" class="p-4 text-center text-sm text-(--fg3)">
                Scrivi il nome di un alimento, per esempio «pasta» o «yogurt».
              </p>
              <button
                v-for="r in righeRisultato"
                :key="r.alimento.id"
                type="button"
                data-test="risultato-sostituzione"
                :disabled="r.grammi === null"
                :aria-pressed="selezionato?.id === r.alimento.id"
                class="flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-left hover:bg-(--soft) disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent"
                :class="selezionato?.id === r.alimento.id ? 'bg-(--mint)' : ''"
                @click="selezionato = r.alimento"
              >
                <span class="flex min-w-0 flex-col">
                  <span class="truncate text-sm font-semibold text-(--fg)">{{ nomeAlimentoLeggibile(r.alimento.nome) }}</span>
                  <span class="truncate text-xs text-(--fg3)">{{ r.alimento.categoria }}</span>
                </span>
                <span
                  v-if="r.grammi !== null"
                  class="whitespace-nowrap text-xs font-bold tabular-nums"
                  :class="selezionato?.id === r.alimento.id ? 'text-(--green-d)' : 'text-(--fg3)'"
                >
                  {{ formattaNumero(r.grammi, 0) }} g
                </span>
                <span v-else class="whitespace-nowrap text-xs font-medium text-(--fg3)">non ha {{ voceMantenuto.nomeAssente }}</span>
              </button>
              <p v-if="query.trim() && risultatiVisibili.length === 0" class="p-4 text-center text-sm text-(--fg3)">
                Nessun alimento trovato.
              </p>
            </div>
          </div>
        </template>

        <div v-if="altreRigheIniziali > 0" class="flex items-start gap-2.5">
          <Checkbox id="sostituisci-altre-righe" v-model="ancheAltreRighe" data-test="anche-altre-righe" class="mt-0.5" />
          <Label for="sostituisci-altre-righe" class="items-start text-sm leading-snug text-(--fg2)">{{ testoCasella }}</Label>
        </div>
      </div>

      <DialogFooter class="flex items-center gap-2.5">
        <Button type="button" variant="neutral" @click="chiudi"><X /> Annulla</Button>
        <Button type="button" data-test="conferma-sostituzione" :disabled="!sostituto" @click="conferma"><Check /> Sostituisci</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
