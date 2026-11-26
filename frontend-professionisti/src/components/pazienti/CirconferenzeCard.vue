<script setup lang="ts">
import { computed } from 'vue'
import type { Visita } from '@/api/pazienti'
import { ETICHETTE_CIRCONFERENZE, formattaNumero } from '@/utils/visita'

type ChiaveCirconferenza = keyof Visita['circonferenze']

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
  { didascalia: undefined, testoVuoto: "Nessuna circonferenza registrata per l'ultima visita.", annidata: false },
)

const ultima = computed<Visita | null>(() => props.visita)

const TRONCO: ChiaveCirconferenza[] = ['vitaCm', 'fianchiCm', 'addomeCm', 'toraceCm']

// Ogni distretto degli arti in una riga: lato non dominante e dominante affiancati, per vedere l'asimmetria.
const ARTI: Array<{ etichetta: string; nonDominante: ChiaveCirconferenza; dominante: ChiaveCirconferenza }> = [
  { etichetta: 'Braccio rilassato', nonDominante: 'braccioRilassatoCm', dominante: 'braccioRilassatoDominanteCm' },
  { etichetta: 'Braccio contratto', nonDominante: 'braccioContrattoCm', dominante: 'braccioContrattoDominanteCm' },
  { etichetta: 'Coscia', nonDominante: 'cosciaCm', dominante: 'cosciaDominanteCm' },
  { etichetta: 'Polpaccio', nonDominante: 'polpaccioCm', dominante: 'polpaccioDominanteCm' },
]

const NOMI_BREVI: Record<ChiaveCirconferenza, string> = {
  vitaCm: 'vita',
  fianchiCm: 'fianchi',
  addomeCm: 'addome',
  toraceCm: 'torace',
  braccioRilassatoCm: 'braccio rilassato (non dom.)',
  braccioContrattoCm: 'braccio contratto (non dom.)',
  cosciaCm: 'coscia (non dom.)',
  polpaccioCm: 'polpaccio (non dom.)',
  braccioRilassatoDominanteCm: 'braccio rilassato (dom.)',
  braccioContrattoDominanteCm: 'braccio contratto (dom.)',
  cosciaDominanteCm: 'coscia (dom.)',
  polpaccioDominanteCm: 'polpaccio (dom.)',
}

function valore(visita: Visita | null, chiave: ChiaveCirconferenza): number | null {
  return visita ? visita.circonferenze[chiave] : null
}

const haCirconferenze = computed(() =>
  (Object.keys(ETICHETTE_CIRCONFERENZE) as ChiaveCirconferenza[]).some((c) => valore(ultima.value, c) !== null),
)

const righeTronco = computed(() =>
  TRONCO.filter((chiave) => valore(ultima.value, chiave) !== null).map((chiave) => ({
    chiave,
    etichetta: ETICHETTE_CIRCONFERENZE[chiave],
    valore: valore(ultima.value, chiave)!,
  })),
)

const righeArti = computed(() =>
  ARTI.map((riga) => ({
    ...riga,
    valoreNonDominante: valore(ultima.value, riga.nonDominante),
    valoreDominante: valore(ultima.value, riga.dominante),
  })).filter((riga) => riga.valoreNonDominante !== null || riga.valoreDominante !== null),
)

// Le misure mancanti in una riga sola: se manca un intero gruppo di arti si dice "arti", non le otto voci.
const nonRilevate = computed(() => {
  const mancanti = (Object.keys(ETICHETTE_CIRCONFERENZE) as ChiaveCirconferenza[])
    .filter((chiave) => valore(ultima.value, chiave) === null)
  const chiaviArti = ARTI.flatMap((riga) => [riga.nonDominante, riga.dominante])
  const artiTuttiMancanti = chiaviArti.every((chiave) => mancanti.includes(chiave))
  const voci = mancanti
    .filter((chiave) => !(artiTuttiMancanti && chiaviArti.includes(chiave)))
    .map((chiave) => NOMI_BREVI[chiave])
  if (artiTuttiMancanti) voci.push('arti')
  return voci
})

function elenco(voci: string[]): string {
  return voci.length > 1 ? `${voci.slice(0, -1).join(', ')} e ${voci.at(-1)}` : voci[0]
}

// Indici calcolati dal backend sulla visita: solo il valore, senza soglie di letteratura,
// che sarebbero un'interpretazione e non un dato del paziente.
const indici = computed(() => {
  const visita = ultima.value
  if (!visita) return []
  return [
    { etichetta: 'Vita / fianchi', valore: visita.whr },
    { etichetta: 'Vita / altezza', valore: visita.whtr },
  ].filter((v): v is { etichetta: string; valore: number } => v.valore !== null)
})

</script>

<template>
  <div
    :class="annidata ? 'rounded-xl bg-(--soft) p-4' : haCirconferenze ? 'rounded-2xl border border-(--bd) bg-(--surf) p-5' : 'rounded-2xl border border-dashed border-(--bd2) px-5 py-3.5'"
    data-test="card-circonferenze">
    <div class="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
      <h3 class="font-heading text-base italic text-(--fg)">Circonferenze</h3>
      <p v-if="didascalia && haCirconferenze" class="text-xs text-(--fg3)">{{ didascalia }}</p>
    </div>

    <template v-if="haCirconferenze">
      <!-- Indici derivati in testa: riassumono vita, fianchi e altezza in un numero. -->
      <div v-if="indici.length" class="mt-3 grid grid-cols-2 gap-2">
        <div
          v-for="indice in indici"
          :key="indice.etichetta"
          class="rounded-xl bg-(--mint) px-3 py-2.5"
          data-test="indice-circonferenze"
        >
          <p class="text-xs text-(--fg2)">{{ indice.etichetta }}</p>
          <p class="mt-0.5 text-lg font-semibold tabular-nums text-(--green-d)">{{ formattaNumero(indice.valore) }}</p>
        </div>
      </div>

      <section v-if="righeTronco.length" class="mt-4">
        <h4 class="mb-1 text-xs text-(--fg3)">Tronco</h4>
        <dl class="divide-y divide-(--div) text-sm">
          <div v-for="riga in righeTronco" :key="riga.chiave" class="flex items-center justify-between gap-3 py-2">
            <dt class="text-(--fg3)">{{ riga.etichetta }}</dt>
            <dd class="font-semibold tabular-nums text-(--fg)">{{ formattaNumero(riga.valore, 1) }} cm</dd>
          </div>
        </dl>
      </section>

      <section v-if="righeArti.length" class="mt-4">
        <table class="w-full table-fixed text-sm">
          <caption class="sr-only">Circonferenze degli arti in centimetri, lato non dominante e dominante</caption>
          <thead>
            <tr class="text-xs text-(--fg3)">
              <th scope="col" class="pb-1 text-left font-normal">Arti</th>
              <th scope="col" class="w-20 pb-1 text-right font-normal">Non dom.</th>
              <th scope="col" class="w-20 pb-1 text-right font-normal">Dom.</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-(--div)">
            <tr v-for="riga in righeArti" :key="riga.etichetta">
              <th scope="row" class="py-2 text-left font-normal text-(--fg3)">{{ riga.etichetta }}</th>
              <td class="py-2 text-right font-semibold tabular-nums text-(--fg)">
                <template v-if="riga.valoreNonDominante !== null">{{ formattaNumero(riga.valoreNonDominante, 1) }}</template>
                <span v-else class="font-normal text-(--fg3)">—</span>
              </td>
              <td class="py-2 text-right font-semibold tabular-nums text-(--fg)">
                <template v-if="riga.valoreDominante !== null">{{ formattaNumero(riga.valoreDominante, 1) }}</template>
                <span v-else class="font-normal text-(--fg3)">—</span>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="mt-1 text-right text-[11px] text-(--fg3)">valori in cm</p>
      </section>

      <p
        v-if="nonRilevate.length"
        class="mt-3 border-t border-dashed border-(--bd2) pt-2.5 text-xs text-(--fg3)"
        data-test="circonferenze-non-rilevate"
      >
        {{ nonRilevate.length === 1 && nonRilevate[0] !== 'arti' ? 'Non rilevata' : 'Non rilevate' }}: {{ elenco(nonRilevate) }}
      </p>
    </template>

    <p v-else class="mt-3 text-sm text-(--fg3)">{{ testoVuoto }}</p>
  </div>
</template>
