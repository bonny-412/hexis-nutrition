<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Visita } from '@/api/pazienti'
import { Label } from '@/components/ui/label'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { ETICHETTE_CIRCONFERENZE, ETICHETTE_OBIETTIVO, formattaNumero } from '@/utils/visita'
import { formattaDataItalianaConMese } from '@/utils/data'
import ConfrontoBarChart from './ConfrontoBarChart.vue'
import { Button } from '@/components/ui/button'
import StatoVuoto from '@/components/StatoVuoto.vue'
import { ClipboardPlus, GitCompareArrows } from '@lucide/vue'
import { CLASSE_ESITO, esitoVariazione, type ParametroClinico } from '@/utils/obiettivo'

const props = defineProps<{
  visite: Visita[]
  /** Per il pulsante "Nuova visita" dello stato vuoto: senza (o con paziente archiviato) il pulsante non compare. */
  pazienteId?: string
  archiviato?: boolean
}>()

const idxA = ref(0)
const idxB = ref(Math.max(props.visite.length - 1, 0))

watch(
  () => props.visite.length,
  (lunghezza) => {
    idxA.value = 0
    idxB.value = Math.max(lunghezza - 1, 0)
  },
)

const visitaA = computed<Visita | undefined>(() => props.visite[idxA.value])
const visitaB = computed<Visita | undefined>(() => props.visite[idxB.value])

interface Riga {
  label: string
  parametro: ParametroClinico
  a: number | null
  b: number | null
  unita: string
  /** Cifre decimali per la visualizzazione: default 2 (peso, plicometria, circonferenze), il BMI passa 1. */
  decimali?: number
}

function righeConVariazione(righe: Riga[]) {
  return righe.map((r) => ({
    ...r,
    variazione: r.a !== null && r.b !== null ? +(r.b - r.a).toFixed(r.decimali ?? 2) : null,
  }))
}

const righePeso = computed<Riga[]>(() => [
  { label: 'Peso (kg)', parametro: 'peso', a: visitaA.value?.pesoKg ?? null, b: visitaB.value?.pesoKg ?? null, unita: 'kg' },
  { label: 'BMI', parametro: 'bmi', a: visitaA.value?.bmi ?? null, b: visitaB.value?.bmi ?? null, unita: '', decimali: 1 },
])

const rigaObiettivo = computed(() => ({
  a: visitaA.value ? ETICHETTE_OBIETTIVO[visitaA.value.obiettivo] : '—',
  b: visitaB.value ? ETICHETTE_OBIETTIVO[visitaB.value.obiettivo] : '—',
  invariato: visitaA.value?.obiettivo === visitaB.value?.obiettivo,
}))

const haPlicometria = computed(() => !!visitaA.value?.plicometria || !!visitaB.value?.plicometria)

const righePlicometria = computed<Riga[]>(() => [
  { label: '% Massa grassa', parametro: 'percentualeGrasso', a: visitaA.value?.plicometria?.percentualeGrassoCorporeo ?? null, b: visitaB.value?.plicometria?.percentualeGrassoCorporeo ?? null, unita: '%' },
  { label: 'Massa grassa (kg)', parametro: 'massaGrassa', a: visitaA.value?.plicometria?.massaGrassaKg ?? null, b: visitaB.value?.plicometria?.massaGrassaKg ?? null, unita: 'kg' },
  { label: 'Massa magra (kg)', parametro: 'massaMagra', a: visitaA.value?.plicometria?.massaMagraKg ?? null, b: visitaB.value?.plicometria?.massaMagraKg ?? null, unita: 'kg' },
  { label: 'FMI', parametro: 'fmi', a: visitaA.value?.plicometria?.fmi ?? null, b: visitaB.value?.plicometria?.fmi ?? null, unita: '' },
  { label: 'FFMI', parametro: 'ffmi', a: visitaA.value?.plicometria?.ffmi ?? null, b: visitaB.value?.plicometria?.ffmi ?? null, unita: '' },
])

// Solo le circonferenze misurate in almeno una delle due visite: una riga vuota in entrambe non dice nulla.
// Un decimale, come nelle card Circonferenze della scheda.
const righeCirconferenze = computed<Riga[]>(() =>
  (Object.keys(ETICHETTE_CIRCONFERENZE) as Array<keyof Visita['circonferenze']>)
    .map((chiave) => ({
      label: ETICHETTE_CIRCONFERENZE[chiave],
      parametro: chiave,
      a: visitaA.value?.circonferenze[chiave] ?? null,
      b: visitaB.value?.circonferenze[chiave] ?? null,
      unita: 'cm',
      decimali: 1,
    }))
    .filter((r) => r.a !== null || r.b !== null),
)

const haCirconferenze = computed(() => righeCirconferenze.value.length > 0)

// I grafici confrontano solo misure presenti in entrambe le visite: senza nessuna, il grafico non compare
// (i valori di una sola visita restano nella tabella sotto).
const haCoppia = (righe: Riga[]) => righe.some((r) => r.a !== null && r.b !== null)
const graficoPlicometria = computed(() => haCoppia(righePlicometria.value))
const graficoCirconferenze = computed(() => haCoppia(righeCirconferenze.value))

/** Colore della variazione in tabella: verde/rosso secondo l'obiettivo della visita B, grigio se indifferente. */
function classeVariazione(r: Riga & { variazione: number | null }): string {
  return CLASSE_ESITO[esitoVariazione(visitaB.value?.obiettivo, r.parametro, r.variazione)]
}

function formattaCella(valore: number | null, unita: string, decimali?: number): string {
  return valore === null ? '—' : `${formattaNumero(valore, decimali)}${unita ? ' ' + unita : ''}`
}

function formattaVariazione(variazione: number | null, unita: string, decimali?: number): string {
  if (variazione === null) return '—'
  const segno = variazione > 0 ? '+' : ''
  return `${segno}${formattaNumero(variazione, decimali)}${unita ? ' ' + unita : ''}`
}
</script>

<template>
  <div v-if="visite.length < 2" class="rounded-2xl border border-(--bd) bg-(--surf)">
    <StatoVuoto
      livello="h4"
      :icona="GitCompareArrows"
      titolo="Nessun confronto disponibile"
      testo="Servono almeno due visite per confrontare i progressi."
    >
      <Button v-if="pazienteId && !archiviato" as-child>
        <router-link :to="`/pazienti/${pazienteId}/visite/nuova`" data-test="confronto-nuova-visita">
          <ClipboardPlus :size="16" /> Nuova visita
        </router-link>
      </Button>
    </StatoVuoto>
  </div>

  <div v-else class="space-y-5">
    <div class="flex flex-wrap items-end gap-4">
      <label class="flex flex-col gap-1.5">
        <Label class="text-[13px] font-medium text-(--fg2)">Visita A</Label>
        <NativeSelect v-model="idxA" class="[&_select]:bg-(--surf)">
          <NativeSelectOption v-for="(v, i) in visite" :key="v.id" :value="i">{{ formattaDataItalianaConMese(v.dataVisita) }}</NativeSelectOption>
        </NativeSelect>
      </label>
      <span class="mb-1.5 text-(--fg3)">→</span>
      <label class="flex flex-col gap-1.5">
        <Label class="text-[13px] font-medium text-(--fg2)">Visita B</Label>
        <NativeSelect v-model="idxB" class="[&_select]:bg-(--surf)">
          <NativeSelectOption v-for="(v, i) in visite" :key="v.id" :value="i">{{ formattaDataItalianaConMese(v.dataVisita) }}</NativeSelectOption>
        </NativeSelect>
      </label>
    </div>

    <div
      v-if="graficoPlicometria || graficoCirconferenze"
      class="grid grid-cols-1 gap-4"
      :class="{ 'xl:grid-cols-2': graficoPlicometria && graficoCirconferenze }"
    >
      <ConfrontoBarChart
        v-if="graficoPlicometria"
        titolo="Plicometria"
        :righe="righePlicometria"
        :etichetta-a="formattaDataItalianaConMese(visitaA!.dataVisita)"
        :etichetta-b="formattaDataItalianaConMese(visitaB!.dataVisita)"
        colore="var(--chart-1)"
      />
      <ConfrontoBarChart
        v-if="graficoCirconferenze"
        titolo="Circonferenze"
        :righe="righeCirconferenze"
        :etichetta-a="formattaDataItalianaConMese(visitaA!.dataVisita)"
        :etichetta-b="formattaDataItalianaConMese(visitaB!.dataVisita)"
        colore="var(--chart-1)"
        orientamento="orizzontale"
      />
    </div>

    <div class="overflow-clip rounded-2xl border border-(--bd) bg-(--surf)">
      <div class="overflow-x-auto xl:overflow-x-visible">
        <table class="w-full min-w-140 border-collapse text-sm">
          <!-- Intestazione fissa durante lo scroll da xl, come in components/ui/table. -->
          <thead class="xl:sticky xl:-top-4 xl:z-10 xl:shadow-[0_1px_0_var(--bd)]">
            <tr class="bg-(--soft) text-xs font-semibold text-(--fg3)">
              <th class="px-5 py-3 text-left">Parametro</th>
              <th class="px-3 py-3 text-right">{{ formattaDataItalianaConMese(visitaA!.dataVisita) }}</th>
              <th class="px-3 py-3 text-right">{{ formattaDataItalianaConMese(visitaB!.dataVisita) }}</th>
              <th class="px-5 py-3 text-right">Variazione</th>
            </tr>
          </thead>
          <tbody>
            <tr class="border-t">
              <td class="px-5 py-2.5 font-semibold text-(--fg2)">Obiettivo</td>
              <td class="px-3 py-2.5 text-right">{{ rigaObiettivo.a }}</td>
              <td class="px-3 py-2.5 text-right">{{ rigaObiettivo.b }}</td>
              <td class="px-5 py-2.5 text-right font-bold" :class="rigaObiettivo.invariato ? 'text-(--fg3)' : 'text-(--warn-fg)'">
                {{ rigaObiettivo.invariato ? '—' : 'Cambiato' }}
              </td>
            </tr>
            <tr v-for="r in righeConVariazione(righePeso)" :key="r.label" class="border-t">
              <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
              <td
                class="px-5 py-2.5 text-right font-bold"
                :class="classeVariazione(r)"
              >
                {{ formattaVariazione(r.variazione, r.unita, r.decimali) }}
              </td>
            </tr>

            <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-semibold text-(--fg3)">BIA</td></tr>
            <tr class="border-t">
              <td colspan="4" class="px-5 py-2.5 text-(--fg3)">Dati non disponibili: la bioimpedenziometria non è ancora supportata.</td>
            </tr>

            <template v-if="haPlicometria">
              <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-semibold text-(--fg3)">Plicometria</td></tr>
              <tr v-for="r in righeConVariazione(righePlicometria)" :key="r.label" class="border-t">
                <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
                <td
                  class="px-5 py-2.5 text-right font-bold"
                  :class="classeVariazione(r)"
                >
                  {{ formattaVariazione(r.variazione, r.unita, r.decimali) }}
                </td>
              </tr>
            </template>

            <template v-if="haCirconferenze">
              <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-semibold text-(--fg3)">Circonferenze</td></tr>
              <tr v-for="r in righeConVariazione(righeCirconferenze)" :key="r.label" class="border-t">
                <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
                <td
                  class="px-5 py-2.5 text-right font-bold"
                  :class="classeVariazione(r)"
                >
                  {{ formattaVariazione(r.variazione, r.unita, r.decimali) }}
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
