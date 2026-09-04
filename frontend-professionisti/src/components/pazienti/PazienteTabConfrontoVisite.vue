<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Visita } from '@/api/pazienti'
import { Label } from '@/components/ui/label'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { ETICHETTE_CIRCONFERENZE, ETICHETTE_OBIETTIVO, formattaNumero } from '@/utils/visita'
import { formattaDataItalianaConMese } from '@/utils/data'
import ConfrontoBarChart from './ConfrontoBarChart.vue'

const props = defineProps<{ visite: Visita[] }>()

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
  { label: 'Peso (kg)', a: visitaA.value?.pesoKg ?? null, b: visitaB.value?.pesoKg ?? null, unita: 'kg' },
  { label: 'BMI', a: visitaA.value?.bmi ?? null, b: visitaB.value?.bmi ?? null, unita: '', decimali: 1 },
])

const rigaObiettivo = computed(() => ({
  a: visitaA.value ? ETICHETTE_OBIETTIVO[visitaA.value.obiettivo] : '—',
  b: visitaB.value ? ETICHETTE_OBIETTIVO[visitaB.value.obiettivo] : '—',
  invariato: visitaA.value?.obiettivo === visitaB.value?.obiettivo,
}))

const haPlicometria = computed(() => !!visitaA.value?.plicometria || !!visitaB.value?.plicometria)

const righePlicometria = computed<Riga[]>(() => [
  { label: '% Grasso corporeo', a: visitaA.value?.plicometria?.percentualeGrassoCorporeo ?? null, b: visitaB.value?.plicometria?.percentualeGrassoCorporeo ?? null, unita: '%' },
  { label: 'Massa grassa (kg)', a: visitaA.value?.plicometria?.massaGrassaKg ?? null, b: visitaB.value?.plicometria?.massaGrassaKg ?? null, unita: 'kg' },
  { label: 'Massa magra (kg)', a: visitaA.value?.plicometria?.massaMagraKg ?? null, b: visitaB.value?.plicometria?.massaMagraKg ?? null, unita: 'kg' },
  { label: 'FMI', a: visitaA.value?.plicometria?.fmi ?? null, b: visitaB.value?.plicometria?.fmi ?? null, unita: '' },
  { label: 'FFMI', a: visitaA.value?.plicometria?.ffmi ?? null, b: visitaB.value?.plicometria?.ffmi ?? null, unita: '' },
])

const righeCirconferenze = computed<Riga[]>(() =>
  (Object.keys(ETICHETTE_CIRCONFERENZE) as Array<keyof Visita['circonferenze']>).map((chiave) => ({
    label: ETICHETTE_CIRCONFERENZE[chiave],
    a: visitaA.value?.circonferenze[chiave] ?? null,
    b: visitaB.value?.circonferenze[chiave] ?? null,
    unita: 'cm',
  })),
)

const haCirconferenze = computed(() => righeCirconferenze.value.some((r) => r.a !== null || r.b !== null))

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
  <div v-if="visite.length < 2" class="rounded-2xl border border-(--bd) bg-(--surf) p-8 text-center text-sm text-(--fg3)">
    <h4 class="font-heading text-lg italic text-(--fg)">Nessun confronto disponibile</h4>
    <p class="mx-auto mt-1.5 max-w-sm text-sm text-(--fg3)">
      Servono almeno due visite per confrontare i progressi.
    </p>
  </div>

  <div v-else class="space-y-5">
    <div class="flex flex-wrap items-end gap-4">
      <label class="flex flex-col gap-1.5">
        <Label class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Visita A</Label>
        <NativeSelect v-model="idxA" class="[&_select]:bg-(--surf)">
          <NativeSelectOption v-for="(v, i) in visite" :key="v.id" :value="i">{{ formattaDataItalianaConMese(v.dataVisita) }}</NativeSelectOption>
        </NativeSelect>
      </label>
      <span class="mb-1.5 text-(--fg4)">→</span>
      <label class="flex flex-col gap-1.5">
        <Label class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Visita B</Label>
        <NativeSelect v-model="idxB" class="[&_select]:bg-(--surf)">
          <NativeSelectOption v-for="(v, i) in visite" :key="v.id" :value="i">{{ formattaDataItalianaConMese(v.dataVisita) }}</NativeSelectOption>
        </NativeSelect>
      </label>
    </div>

    <div
      v-if="haPlicometria || haCirconferenze"
      class="grid grid-cols-1 gap-4"
      :class="{ 'xl:grid-cols-2': haPlicometria && haCirconferenze }"
    >
      <ConfrontoBarChart
        v-if="haPlicometria"
        titolo="Plicometria"
        :righe="righePlicometria"
        :etichetta-a="formattaDataItalianaConMese(visitaA!.dataVisita)"
        :etichetta-b="formattaDataItalianaConMese(visitaB!.dataVisita)"
        colore="var(--chart-1)"
      />
      <ConfrontoBarChart
        v-if="haCirconferenze"
        titolo="Circonferenze"
        :righe="righeCirconferenze"
        :etichetta-a="formattaDataItalianaConMese(visitaA!.dataVisita)"
        :etichetta-b="formattaDataItalianaConMese(visitaB!.dataVisita)"
        colore="var(--chart-2)"
        orientamento="orizzontale"
      />
    </div>

    <div class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full min-w-140 border-collapse text-sm">
          <thead>
            <tr class="bg-(--soft) text-xs font-bold uppercase tracking-wide text-(--fg3)">
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
              <td class="px-5 py-2.5 text-right font-bold" :class="rigaObiettivo.invariato ? 'text-(--fg4)' : 'text-(--warn-fg)'">
                {{ rigaObiettivo.invariato ? '—' : 'Cambiato' }}
              </td>
            </tr>
            <tr v-for="r in righeConVariazione(righePeso)" :key="r.label" class="border-t">
              <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
              <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
              <td
                class="px-5 py-2.5 text-right font-bold"
                :class="!r.variazione ? 'text-(--fg4)' : 'text-(--warn-fg)'"
              >
                {{ formattaVariazione(r.variazione, r.unita, r.decimali) }}
              </td>
            </tr>

            <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-bold uppercase tracking-wide text-(--fg3)">BIA</td></tr>
            <tr class="border-t">
              <td colspan="4" class="px-5 py-2.5 text-(--fg4)">Dati non disponibili: la bioimpedenziometria non è ancora supportata.</td>
            </tr>

            <template v-if="haPlicometria">
              <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-bold uppercase tracking-wide text-(--fg3)">Plicometria</td></tr>
              <tr v-for="r in righeConVariazione(righePlicometria)" :key="r.label" class="border-t">
                <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
                <td
                  class="px-5 py-2.5 text-right font-bold"
                  :class="!r.variazione ? 'text-(--fg4)' : 'text-(--warn-fg)'"
                >
                  {{ formattaVariazione(r.variazione, r.unita, r.decimali) }}
                </td>
              </tr>
            </template>

            <template v-if="haCirconferenze">
              <tr><td colspan="4" class="border-t border-(--div) bg-(--soft)/50 px-5 py-2 text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenze</td></tr>
              <tr v-for="r in righeConVariazione(righeCirconferenze)" :key="r.label" class="border-t">
                <td class="px-5 py-2.5 font-semibold text-(--fg2)">{{ r.label }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.a, r.unita, r.decimali) }}</td>
                <td class="px-3 py-2.5 text-right tabular-nums">{{ formattaCella(r.b, r.unita, r.decimali) }}</td>
                <td
                  class="px-5 py-2.5 text-right font-bold"
                  :class="!r.variazione ? 'text-(--fg4)' : 'text-(--warn-fg)'"
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
