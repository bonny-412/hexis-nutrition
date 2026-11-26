<script setup lang="ts">
import { computed } from 'vue'
import type { Visita } from '@/api/pazienti'
import { ETICHETTE_PLICHE, ETICHETTE_PROTOCOLLO_PLICOMETRIA, formattaNumero, type ChiavePlica } from '@/utils/visita'

// Stessa struttura di `CirconferenzeCard`: indici in testa, poi i risultati, poi le misure rilevate.
// Usata nella Panoramica (ultima visita), nello Storico visite e nel riepilogo accanto al form di visita.
const props = withDefaults(
  defineProps<{
    visita: Visita | null
    /** Testo accanto al titolo, es. "Ultima visita, 25 nov 2026". */
    didascalia?: string
    testoVuoto?: string
    /** Dentro un'altra card: fondo tenue invece di bordo, come gli altri blocchi annidati. */
    annidata?: boolean
  }>(),
  { didascalia: undefined, testoVuoto: "Nessuna plicometria registrata per l'ultima visita.", annidata: false },
)

const ultima = computed<Visita | null>(() => props.visita)
const plicometria = computed(() => ultima.value?.plicometria ?? null)

const composizione = computed(() => {
  const p = plicometria.value
  if (!p) return []
  return [
    { etichetta: '% Massa grassa', valore: `${formattaNumero(p.percentualeGrassoCorporeo)}%` },
    { etichetta: 'Massa grassa', valore: `${formattaNumero(p.massaGrassaKg)} kg` },
    { etichetta: 'Massa magra', valore: `${formattaNumero(p.massaMagraKg)} kg` },
  ]
})

const indici = computed(() => {
  const p = plicometria.value
  if (!p) return []
  return [
    { etichetta: 'FMI', descrizione: 'Indice di massa grassa', valore: p.fmi },
    { etichetta: 'FFMI', descrizione: 'Indice di massa magra', valore: p.ffmi },
  ]
})

// Solo le pliche del protocollo usato sono valorizzate: le altre non si mostrano.
const pliche = computed(() => {
  const p = plicometria.value
  if (!p) return []
  return (Object.keys(ETICHETTE_PLICHE) as ChiavePlica[])
    .filter((chiave) => p[chiave] !== null)
    .map((chiave) => ({ chiave, etichetta: ETICHETTE_PLICHE[chiave], valore: p[chiave] as number }))
})
</script>

<template>
  <div
    :class="annidata ? 'rounded-xl bg-(--soft) p-4' : 'rounded-2xl border border-(--bd) bg-(--surf) p-5'"
    data-test="card-plicometria">
    <div class="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
      <h3 class="font-heading text-base italic text-(--fg)">Plicometria</h3>
      <p v-if="didascalia && plicometria" class="text-xs text-(--fg3)">{{ didascalia }}</p>
    </div>

    <template v-if="plicometria">
      <div class="mt-3 grid grid-cols-2 gap-2">
        <div
          v-for="indice in indici"
          :key="indice.etichetta"
          class="rounded-xl bg-(--mint) px-3 py-2.5"
          data-test="indice-plicometria"
        >
          <p class="text-xs text-(--fg2)" :title="indice.descrizione">
            {{ indice.etichetta }}<span class="sr-only">, {{ indice.descrizione }}</span>
          </p>
          <p class="mt-0.5 text-lg font-semibold tabular-nums text-(--green-d)">{{ formattaNumero(indice.valore) }}</p>
        </div>
      </div>

      <section class="mt-4">
        <h4 class="mb-1 text-xs text-(--fg3)">Composizione</h4>
        <dl class="divide-y divide-(--div) text-sm">
          <div v-for="riga in composizione" :key="riga.etichetta" class="flex items-center justify-between gap-3 py-2">
            <dt class="text-(--fg3)">{{ riga.etichetta }}</dt>
            <dd class="font-semibold tabular-nums text-(--fg)">{{ riga.valore }}</dd>
          </div>
        </dl>
      </section>

      <section v-if="pliche.length" class="mt-4">
        <h4 class="mb-1 text-xs text-(--fg3)">Pliche</h4>
        <dl class="divide-y divide-(--div) text-sm">
          <div v-for="riga in pliche" :key="riga.chiave" class="flex items-center justify-between gap-3 py-2">
            <dt class="text-(--fg3)">{{ riga.etichetta }}</dt>
            <dd class="font-semibold tabular-nums text-(--fg)">{{ formattaNumero(riga.valore, 1) }} mm</dd>
          </div>
        </dl>
      </section>

      <p class="mt-3 border-t border-dashed border-(--bd2) pt-2.5 text-xs text-(--fg3)" data-test="plicometria-protocollo">
        Protocollo {{ ETICHETTE_PROTOCOLLO_PLICOMETRIA[plicometria.protocollo] }}
      </p>
    </template>

    <p v-else class="mt-3 text-sm text-(--fg3)">{{ testoVuoto }}</p>
  </div>
</template>
