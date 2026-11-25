<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { formattaNumero } from '@/utils/visita'

interface Riga {
  label: string
  a: number | null
  b: number | null
  unita: string
  decimali?: number
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
  testoA: string
  testoB: string
  /** Lunghezze in % del massimo della coppia: ogni parametro ha la sua scala (kg, %, cm e indici non si confrontano tra loro). */
  quotaA: number
  quotaB: number
}

function formatta(valore: number, unita: string, decimali?: number): string {
  return `${formattaNumero(valore, decimali)}${unita ? ' ' + unita : ''}`
}

const orizzontale = computed(() => props.orientamento === 'orizzontale')

/** Solo le righe con entrambi i valori: un confronto non ha senso se manca un lato. */
const punti = computed<PuntoConfronto[]>(() =>
  props.righe
    .filter((r): r is Riga & { a: number; b: number } => r.a !== null && r.b !== null)
    .map((r) => {
      const massimo = Math.max(Math.abs(r.a), Math.abs(r.b)) || 1
      // In verticale le colonne sono strette: l'unità sta già nell'etichetta del parametro ("Massa grassa (kg)").
      const unita = orizzontale.value ? r.unita : ''
      return {
        label: r.label,
        testoA: formatta(r.a, unita, r.decimali),
        testoB: formatta(r.b, unita, r.decimali),
        quotaA: (Math.abs(r.a) / massimo) * 100,
        quotaB: (Math.abs(r.b) / massimo) * 100,
      }
    }),
)

/** La visita A è una tinta chiara del colore di B: si legge "prima → dopo", non due serie diverse. */
const coloreA = computed(() => `color-mix(in srgb, ${props.colore} 32%, var(--surf))`)
</script>

<template>
  <Card>
    <CardHeader>
      <div class="flex flex-wrap items-center justify-between gap-2">
        <CardTitle class="text-sm font-semibold text-(--fg2)">{{ titolo }}</CardTitle>
        <div v-if="punti.length > 0" class="flex items-center gap-3 text-[11px] text-(--fg3)" data-test="legenda-confronto">
          <span class="flex items-center gap-1.5"><span class="h-2 w-3 rounded-sm" :style="{ background: coloreA }" />{{ etichettaA }}</span>
          <span class="flex items-center gap-1.5"><span class="h-2 w-3 rounded-sm" :style="{ background: colore }" />{{ etichettaB }}</span>
        </div>
      </div>
    </CardHeader>
    <CardContent>
      <p v-if="punti.length === 0" class="text-sm text-(--fg3)">Nessuna misura presente in entrambe le visite.</p>

      <!-- Verticale: una coppia di colonne per parametro, valore sopra ciascuna. -->
      <div v-else-if="!orizzontale" class="flex items-stretch gap-2 sm:gap-4" data-test="confronto-verticale">
        <div v-for="p in punti" :key="p.label" class="flex min-w-0 flex-1 flex-col" data-test="confronto-gruppo">
          <div class="flex h-40 items-end justify-center gap-1 border-b border-(--bd)">
            <div class="flex h-full w-full max-w-7 flex-col items-center justify-end">
              <span class="mb-1 text-[10px] tabular-nums text-(--fg3) whitespace-nowrap">{{ p.testoA }}</span>
              <div data-test="barra" :data-colore="coloreA" class="w-full rounded-t-md" :style="{ height: `${p.quotaA * 0.78}%`, background: coloreA }" />
            </div>
            <div class="flex h-full w-full max-w-7 flex-col items-center justify-end">
              <span class="mb-1 text-[10px] font-semibold tabular-nums text-(--fg) whitespace-nowrap">{{ p.testoB }}</span>
              <div data-test="barra" :data-colore="colore" class="w-full rounded-t-md" :style="{ height: `${p.quotaB * 0.78}%`, background: colore }" />
            </div>
          </div>
          <span class="mt-2 text-center text-xs leading-tight text-(--fg3)">{{ p.label }}</span>
        </div>
      </div>

      <!-- Orizzontale: una riga per parametro, A sopra e B sotto, valore in fondo a ciascuna barra. -->
      <ul v-else class="divide-y divide-(--div)" data-test="confronto-orizzontale">
        <li
          v-for="p in punti"
          :key="p.label"
          class="grid grid-cols-1 gap-1.5 py-2.5 first:pt-0 last:pb-0 sm:grid-cols-[minmax(0,14rem)_1fr] sm:items-center sm:gap-4"
          data-test="confronto-gruppo"
        >
          <span class="truncate text-sm text-(--fg2)" :title="p.label">{{ p.label }}</span>
          <div class="space-y-1">
            <div class="flex items-center gap-2">
              <div data-test="barra" :data-colore="coloreA" class="h-2 rounded-full" :style="{ width: `calc(${p.quotaA}% - 4.5rem)`, minWidth: '2px', background: coloreA }" />
              <span class="text-[11px] tabular-nums text-(--fg3) whitespace-nowrap">{{ p.testoA }}</span>
            </div>
            <div class="flex items-center gap-2">
              <div data-test="barra" :data-colore="colore" class="h-2 rounded-full" :style="{ width: `calc(${p.quotaB}% - 4.5rem)`, minWidth: '2px', background: colore }" />
              <span class="text-[11px] font-semibold tabular-nums text-(--fg) whitespace-nowrap">{{ p.testoB }}</span>
            </div>
          </div>
        </li>
      </ul>
    </CardContent>
  </Card>
</template>
