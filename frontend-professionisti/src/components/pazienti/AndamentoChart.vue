<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { VisAxis, VisLine, VisScatter, VisXYContainer } from '@unovis/vue'
import { MAX_PUNTI_GRAFICO, type Andamento } from '@/utils/andamento'
import { formattaDataItaliana, isoATimestamp } from '@/utils/data'
import { formattaNumero } from '@/utils/visita'

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

/** Solo le ultime visite: `ultimo` e `delta` restano calcolati sull'intero storico. */
const datiGrafico = computed<PuntoGrafico[]>(() =>
  props.andamento.punti.slice(-MAX_PUNTI_GRAFICO).map((p) => ({
    timestamp: isoATimestamp(p.data),
    etichetta: formattaDataItaliana(p.data),
    valore: p.valore,
  })),
)

const etichettaIntervallo = computed(() => {
  const n = datiGrafico.value.length
  return n === 1 ? 'Ultima visita' : `Ultime ${n} visite`
})

/** Un tick per ogni visita: l'asse mostra le date reali, non una griglia arbitraria. */
const tickDate = computed<number[]>(() => datiGrafico.value.map((p) => p.timestamp))

function formattaTick(tick: number | Date): string {
  const timestamp = tick instanceof Date ? tick.getTime() : tick
  const punto = datiGrafico.value.find((p) => p.timestamp === timestamp)
  return punto ? punto.etichetta : formattaDataItaliana(timestamp)
}

/** Il valore più recente è quello che si cerca: più grande, con l'unità, nel colore del testo principale. */
// Confronto per data e non per identità: Unovis passa agli accessor copie dei dati.
function eUltimo(d: PuntoGrafico): boolean {
  return d.timestamp === datiGrafico.value[datiGrafico.value.length - 1]?.timestamp
}
function etichettaPunto(d: PuntoGrafico): string {
  const valore = formattaNumero(d.valore, props.decimali)
  return eUltimo(d) && props.unita ? `${valore} ${props.unita}` : valore
}
const colorePunto = (d: PuntoGrafico) => (eUltimo(d) ? 'var(--fg)' : 'var(--fg3)')
const dimensionePunto = (d: PuntoGrafico) => (eUltimo(d) ? 10 : 7)

const chartConfig = computed<ChartConfig>(() => ({
  valore: { label: props.titolo, color: props.colore },
}))

</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="flex items-baseline justify-between gap-2 text-sm font-semibold text-(--fg2)">
        {{ titolo }}
        <span v-if="andamento.punti.length > 0" class="text-[10px] font-medium normal-case tracking-normal text-(--fg4)">
          {{ etichettaIntervallo }}
        </span>
      </CardTitle>
    </CardHeader>
    <CardContent>
      <div v-if="andamento.punti.length === 0" class="text-sm text-(--fg3)">Nessun dato disponibile</div>
      <template v-else>
        <!-- Un valore su ogni punto, l'ultimo in evidenza: la variazione rispetto alla visita precedente è negli indicatori in alto. -->
        <ChartContainer
          :config="chartConfig"
          class="h-40 aspect-auto [--vis-scatter-point-label-text-font-size:11px] [--vis-scatter-point-label-text-font-weight:600]"
        >
          <VisXYContainer :data="datiGrafico" :margin="{ left: 28, right: 28, top: 22 }">
            <VisLine :x="(d: PuntoGrafico) => d.timestamp" :y="(d: PuntoGrafico) => d.valore" :color="colore" />
            <VisScatter
              :x="(d: PuntoGrafico) => d.timestamp"
              :y="(d: PuntoGrafico) => d.valore"
              :color="colore"
              :size="dimensionePunto"
              :label="etichettaPunto"
              :label-color="colorePunto"
              label-position="top"
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
