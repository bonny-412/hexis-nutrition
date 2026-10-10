<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { GroupedBar } from '@unovis/ts'
import { VisAxis, VisGroupedBar, VisTooltip, VisXYContainer } from '@unovis/vue'
import { formattaNumero } from '@/utils/visita'

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

/** Colore della prima visita (A): verde chiaro, da affiancare al verde scuro della seconda (B). */
const COLORE_A = 'var(--chart-2)'

/** Larghezza massima (px) di una coppia di barre nel grafico verticale: su schermi larghi le colonne restano snelle. */
const LARGHEZZA_MAX_GRUPPO = 80

const orizzontale = computed(() => props.orientamento === 'orizzontale')

/** Il grafico orizzontale cresce con le righe (fino a 10 circonferenze); quello verticale ha altezza fissa. */
const altezzaPx = computed(() => (orizzontale.value ? Math.max(punti.value.length * 40 + 16, 90) : 180))

function formattaTick(indice: number): string {
  return punti.value[Math.round(indice)]?.label ?? ''
}

function coloreBarra(_d: PuntoConfronto, indiceSerie: number): string {
  return indiceSerie === 0 ? COLORE_A : props.colore
}

function escapeHtml(testo: string): string {
  return testo.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function rigaTooltip(colore: string, etichetta: string, valore: number, unita: string): string {
  const testo = `${formattaNumero(valore)}${unita ? ' ' + escapeHtml(unita) : ''}`
  return `<div style="display:flex;align-items:center;justify-content:space-between;gap:16px"><span style="display:flex;align-items:center;gap:6px;color:var(--fg3)"><span style="width:8px;height:8px;border-radius:2px;background:${colore}"></span>${escapeHtml(etichetta)}</span><span style="font-weight:600">${testo}</span></div>`
}

/** ChartContainer rende il tooltip di Unovis senza sfondo né bordo: il riquadro va stilato qui. */
function contenutoTooltip(d: PuntoConfronto): string {
  return `<div style="min-width:150px;padding:8px 10px;border-radius:8px;border:1px solid var(--bd);background:var(--surf);color:var(--fg);box-shadow:0 4px 12px rgba(0,0,0,.12);font-size:12px;display:grid;gap:4px"><div style="font-weight:700">${escapeHtml(d.label)}</div>${rigaTooltip(COLORE_A, props.etichettaA, d.a, d.unita)}${rigaTooltip(props.colore, props.etichettaB, d.b, d.unita)}</div>`
}

const triggers = { [GroupedBar.selectors.barGroup]: contenutoTooltip }

const chartConfig = computed<ChartConfig>(() => ({
  a: { label: props.etichettaA, color: COLORE_A },
  b: { label: props.etichettaB, color: props.colore },
}))
</script>

<template>
  <Card>
    <CardHeader>
      <div class="flex flex-wrap items-center justify-between gap-2">
        <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ titolo }}</CardTitle>
        <div v-if="punti.length > 0" class="flex items-center gap-3 text-[11px] text-(--fg3)">
          <span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-sm" :style="{ background: COLORE_A }" />{{ etichettaA }}</span>
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
            :group-max-width="orizzontale ? undefined : LARGHEZZA_MAX_GRUPPO"
            :bar-padding="orizzontale ? 0.2 : 0"
          />
          <VisTooltip :triggers="triggers" />
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
