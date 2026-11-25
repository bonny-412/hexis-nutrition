<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { CurveType } from '@unovis/ts'
import { VisAxis, VisLine, VisScatter, VisXYContainer } from '@unovis/vue'
import { MAX_PUNTI_GRAFICO, type Andamento } from '@/utils/andamento'
import { formattaDataItaliana, isoATimestamp } from '@/utils/data'

const props = defineProps<{
  /** Andamento della % di grasso corporeo (asse destro). */
  grasso: Andamento
  /** Andamento della massa magra in kg (asse sinistro). */
  magra: Andamento
}>()

/** Una riga per visita: le due serie condividono la X; i valori sono la variazione % rispetto alla prima visita mostrata. */
interface RigaGrafico {
  timestamp: number
  magra: number | null
  grasso: number | null
}

const COLORE_GRASSO = 'var(--chart-3)'
const COLORE_MAGRA = 'var(--chart-4)'

/**
 * Due grandezze con unità diverse (kg e %) non stanno sulla stessa scala: ciascuna è espressa come
 * variazione percentuale rispetto alla prima visita mostrata. Le linee condividono così lo stesso
 * asse, partono dallo stesso punto e mostrano di quanto cambia ogni grandezza, senza coprirsi.
 * I valori reali sono nella riga di indicatori della scheda paziente.
 */
function variazioniUltimi(andamento: Andamento): Map<number, number> {
  const punti = andamento.punti.slice(-MAX_PUNTI_GRAFICO)
  const base = punti.length > 0 ? punti[0].valore : 0
  return new Map(punti.map((p) => [isoATimestamp(p.data), base === 0 ? 0 : ((p.valore - base) / base) * 100]))
}
const righe = computed<RigaGrafico[]>(() => {
  const magra = variazioniUltimi(props.magra)
  const grasso = variazioniUltimi(props.grasso)
  const date = [...new Set([...magra.keys(), ...grasso.keys()])].sort((a, b) => a - b)
  return date.map((timestamp) => ({
    timestamp,
    magra: magra.get(timestamp) ?? null,
    grasso: grasso.get(timestamp) ?? null,
  }))
})

/** Un tick per ogni visita disegnata. */
const tickDate = computed<number[]>(() => righe.value.map((r) => r.timestamp))
function formattaTickData(tick: number | Date): string {
  return formattaDataItaliana(tick instanceof Date ? tick.getTime() : tick)
}
const chartConfig: ChartConfig = {
  magra: { label: 'Massa magra', color: COLORE_MAGRA },
  grasso: { label: '% Grasso corporeo', color: COLORE_GRASSO },
}

</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="flex items-baseline justify-between gap-2 text-sm font-semibold text-(--fg2)">
        Massa magra e % grasso
        <span v-if="tickDate.length > 0" class="text-[10px] font-medium normal-case tracking-normal text-(--fg4)">
          {{ tickDate.length === 1 ? 'Ultima visita' : `Ultime ${tickDate.length} visite` }}
        </span>
      </CardTitle>
    </CardHeader>
    <CardContent>
      <div v-if="tickDate.length === 0" class="text-sm text-(--fg3)">Dati non disponibili</div>
      <template v-else>
        <!-- Valori attuali e variazioni stanno nella riga di indicatori in cima alla scheda paziente. -->
        <p class="text-xs text-(--fg3)">Variazione % rispetto alla prima visita mostrata</p>
        <div class="mt-1.5 flex items-center gap-4 text-xs text-(--fg3)">
          <span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" :style="{ background: COLORE_MAGRA }" />Massa magra (kg)</span>
          <span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" :style="{ background: COLORE_GRASSO }" />% Grasso</span>
        </div>

        <ChartContainer :config="chartConfig" class="mt-4 h-40 aspect-auto">
          <VisXYContainer :data="righe" :margin="{ left: 20, right: 20 }">
            <VisLine :x="(d: RigaGrafico) => d.timestamp" :y="(d: RigaGrafico) => d.magra" :color="COLORE_MAGRA" :curve-type="CurveType.Linear" />
            <VisScatter :x="(d: RigaGrafico) => d.timestamp" :y="(d: RigaGrafico) => d.magra" :color="COLORE_MAGRA" :size="8" />
            <VisLine :x="(d: RigaGrafico) => d.timestamp" :y="(d: RigaGrafico) => d.grasso" :color="COLORE_GRASSO" :curve-type="CurveType.Linear" />
            <VisScatter :x="(d: RigaGrafico) => d.timestamp" :y="(d: RigaGrafico) => d.grasso" :color="COLORE_GRASSO" :size="8" />
            <VisAxis
              type="x"
              :tick-values="tickDate"
              :tick-format="formattaTickData"
              :grid-line="false"
              :tick-line="false"
            />
          </VisXYContainer>
        </ChartContainer>
      </template>
    </CardContent>
  </Card>
</template>
