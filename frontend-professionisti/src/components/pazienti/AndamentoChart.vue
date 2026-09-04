<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { VisAxis, VisLine, VisScatter, VisXYContainer } from '@unovis/vue'
import { ArrowDown, ArrowUp } from '@lucide/vue'
import type { Andamento } from '@/utils/andamento'
import { formattaDataItaliana, isoATimestamp } from '@/utils/data'

const props = withDefaults(
  defineProps<{
    titolo: string
    unita: string
    andamento: Andamento
    colore: string
    decimali?: number
  }>(),
  { decimali: 1 },
)

interface PuntoGrafico {
  /** Mezzanotte UTC della visita: coordinata numerica dell'asse x. */
  timestamp: number
  /** Etichetta `GG/MM/AA` già formattata per l'asse x. */
  etichetta: string
  valore: number
}

const datiGrafico = computed<PuntoGrafico[]>(() =>
  props.andamento.punti.map((p) => ({
    timestamp: isoATimestamp(p.data),
    etichetta: formattaDataItaliana(p.data),
    valore: p.valore,
  })),
)

/** Un tick per ogni visita: l'asse mostra le date reali, non una griglia arbitraria. */
const tickDate = computed<number[]>(() => datiGrafico.value.map((p) => p.timestamp))

function formattaTick(tick: number | Date): string {
  const timestamp = tick instanceof Date ? tick.getTime() : tick
  const punto = datiGrafico.value.find((p) => p.timestamp === timestamp)
  return punto ? punto.etichetta : formattaDataItaliana(timestamp)
}

const chartConfig = computed<ChartConfig>(() => ({
  valore: { label: props.titolo, color: props.colore },
}))

function formatta(valore: number): string {
  return valore.toLocaleString('it-IT', { minimumFractionDigits: props.decimali, maximumFractionDigits: props.decimali })
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ titolo }}</CardTitle>
    </CardHeader>
    <CardContent>
      <div v-if="andamento.punti.length === 0" class="text-sm text-(--fg4)">Nessun dato disponibile</div>
      <template v-else>
        <div class="flex items-baseline gap-2">
          <span class="font-heading text-3xl font-semibold text-(--fg)">{{ formatta(andamento.ultimo as number) }}</span>
          <span v-if="unita" class="text-sm text-(--fg3)">{{ unita }}</span>
          <span
            v-if="andamento.delta !== null"
            data-test="andamento-delta"
            class="flex items-center gap-0.5 text-sm font-medium"
            :class="andamento.delta < 0 ? 'text-(--green)' : andamento.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
          >
            <ArrowDown v-if="andamento.delta < 0" :size="14" />
            <ArrowUp v-else-if="andamento.delta > 0" :size="14" />
            {{ formatta(Math.abs(andamento.delta)) }}{{ unita ? ' ' + unita : '' }}
          </span>
          <span v-else class="text-sm text-(--fg3)">Prima visita</span>
        </div>
        <ChartContainer :config="chartConfig" class="mt-4 h-40 aspect-auto">
          <VisXYContainer :data="datiGrafico" :margin="{ left: 20, right: 20 }">
            <VisLine :x="(d: PuntoGrafico) => d.timestamp" :y="(d: PuntoGrafico) => d.valore" :color="colore" />
            <VisScatter
              :x="(d: PuntoGrafico) => d.timestamp"
              :y="(d: PuntoGrafico) => d.valore"
              :color="colore"
              :size="8"
            />
            <VisAxis
              type="x"
              :tick-values="tickDate"
              :tick-format="formattaTick"
              :grid-line="false"
              :tick-line="false"
            />
          </VisXYContainer>
        </ChartContainer>
      </template>
    </CardContent>
  </Card>
</template>
