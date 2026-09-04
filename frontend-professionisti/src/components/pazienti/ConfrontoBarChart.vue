<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { VisAxis, VisGroupedBar, VisXYContainer } from '@unovis/vue'

interface Riga {
  label: string
  a: number | null
  b: number | null
  unita: string
}

const props = withDefaults(
  defineProps<{
    titolo: string
    righe: Riga[]
    etichettaA: string
    etichettaB: string
    colore?: string
    orientamento?: 'verticale' | 'orizzontale'
  }>(),
  { colore: 'var(--chart-1)', orientamento: 'verticale' },
)

interface PuntoConfronto {
  label: string
  unita: string
  a: number
  b: number
}

/** Solo le righe con entrambi i valori: un confronto non ha senso se manca un lato. */
const punti = computed<PuntoConfronto[]>(() =>
  props.righe
    .filter((r): r is Riga & { a: number, b: number } => r.a !== null && r.b !== null)
    .map((r) => ({ label: r.label, unita: r.unita, a: r.a, b: r.b })),
)

const orizzontale = computed(() => props.orientamento === 'orizzontale')

/** Il grafico orizzontale cresce con le righe (fino a 10 circonferenze); quello verticale ha altezza fissa. */
const altezzaPx = computed(() => (orizzontale.value ? Math.max(punti.value.length * 34 + 16, 90) : 180))

function formattaTick(indice: number): string {
  return punti.value[Math.round(indice)]?.label ?? ''
}

function coloreBarra(_d: PuntoConfronto, indiceSerie: number): string {
  return indiceSerie === 0 ? 'var(--fg4)' : props.colore
}

const chartConfig = computed<ChartConfig>(() => ({
  a: { label: props.etichettaA, color: 'var(--fg4)' },
  b: { label: props.etichettaB, color: props.colore },
}))
</script>

<template>
  <Card>
    <CardHeader>
      <div class="flex flex-wrap items-center justify-between gap-2">
        <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ titolo }}</CardTitle>
        <div v-if="punti.length > 0" class="flex items-center gap-3 text-[11px] text-(--fg3)">
          <span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-sm bg-(--fg4)" />{{ etichettaA }}</span>
          <span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-sm" :style="{ background: colore }" />{{ etichettaB }}</span>
        </div>
      </div>
    </CardHeader>
    <CardContent>
      <div v-if="punti.length === 0" class="text-sm text-(--fg4)">Dati non disponibili</div>
      <ChartContainer v-else :config="chartConfig" class="aspect-auto" :style="{ height: `${altezzaPx}px` }">
        <VisXYContainer :data="punti" :margin="orizzontale ? { left: 96, right: 12 } : { left: 8, right: 8 }">
          <VisGroupedBar
            :x="(_d: PuntoConfronto, i: number) => i"
            :y="[(d: PuntoConfronto) => d.a, (d: PuntoConfronto) => d.b]"
            :color="coloreBarra"
            :orientation="orizzontale ? 'horizontal' : 'vertical'"
            :rounded-corners="4"
          />
          <VisAxis
            :type="orizzontale ? 'y' : 'x'"
            :tick-values="punti.map((_p, i) => i)"
            :tick-format="formattaTick"
            :grid-line="false"
            :tick-line="false"
          />
        </VisXYContainer>
      </ChartContainer>
    </CardContent>
  </Card>
</template>
