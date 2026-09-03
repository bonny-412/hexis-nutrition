<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Visita } from '@/api/pazienti'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ETICHETTE_CIRCONFERENZE, ETICHETTE_OBIETTIVO, categoriaBmi, formattaNumero } from '@/utils/visita'
import { formattaDataItalianaConMese } from '@/utils/data'
import { ChevronDown } from '@lucide/vue'

const props = defineProps<{
  visiteInCaricamento: boolean
  erroreVisite: boolean
  visite: Visita[]
}>()

const idAperto = ref<string | null>(null)

// Alla prima visita disponibile si apre di default la più recente; da lì in poi
// resta sotto il controllo dell'utente (vedi toggle).
let apertoDiDefault = false
watch(
  () => props.visite,
  (visite) => {
    if (!apertoDiDefault && visite.length > 0) {
      idAperto.value = visite[visite.length - 1].id
      apertoDiDefault = true
    }
  },
  { immediate: true },
)

function toggle(id: string) {
  idAperto.value = idAperto.value === id ? null : id
}

interface RigaStorico {
  visita: Visita
  rel: string
  deltaPeso: number | null
  deltaMg: number | null
  categoriaBmi: string | null
  circonferenze: Array<{ label: string; valore: number | null }>
  haCirconferenze: boolean
}

const righe = computed<RigaStorico[]>(() => {
  const decrescente = [...props.visite].reverse()
  return decrescente.map((visita, indice) => {
    const precedente = decrescente[indice + 1]
    const circonferenze = (Object.keys(ETICHETTE_CIRCONFERENZE) as Array<keyof Visita['circonferenze']>).map((chiave) => ({
      label: ETICHETTE_CIRCONFERENZE[chiave],
      valore: visita.circonferenze[chiave],
    }))
    return {
      visita,
      rel: indice === 0 ? 'Più recente' : indice === 1 ? '1 visita fa' : `${indice} visite fa`,
      deltaPeso: precedente ? +(visita.pesoKg - precedente.pesoKg).toFixed(1) : null,
      deltaMg:
        precedente && visita.plicometria && precedente.plicometria
          ? +(visita.plicometria.percentualeGrassoCorporeo - precedente.plicometria.percentualeGrassoCorporeo).toFixed(1)
          : null,
      categoriaBmi: categoriaBmi(visita.bmi),
      circonferenze,
      haCirconferenze: circonferenze.some((c) => c.valore !== null),
    }
  })
})
</script>

<template>
  <div v-if="erroreVisite" class="text-xs font-medium text-(--danger)">
    Non è stato possibile caricare l'elenco delle visite.
  </div>

  <div v-else-if="visiteInCaricamento" class="space-y-3">
    <div v-for="n in 3" :key="n" data-test="storico-skeleton" class="h-16 animate-pulse rounded-2xl bg-(--hover)" />
  </div>

  <div
    v-else-if="visite.length === 0"
    class="rounded-2xl border border-(--bd) bg-(--hover)/40 p-8 text-center text-sm text-(--fg3)"
  >
    Nessuna visita registrata.
  </div>

  <div v-else class="relative lg:pl-2">
    <div class="absolute bottom-2 left-7.25 top-2 hidden w-0.5 bg-(--bd2) lg:block"></div>

    <div v-for="(riga, indice) in righe" :key="riga.visita.id" class="relative mb-3.5 flex gap-4 last:mb-0">
      <div class="relative hidden w-11 shrink-0 justify-center pt-4 lg:flex">
        <span
          class="h-3.25 w-3.25 rounded-full"
          :style="{ background: indice === 0 ? 'var(--green)' : 'var(--sage)', boxShadow: '0 0 0 4px var(--bg), 0 0 0 5px var(--bd2)' }"
        />
      </div>

      <div class="min-w-0 flex-1 overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-sm transition-colors hover:border-(--green)">
        <button
          type="button"
          data-test="storico-riga"
          class="flex w-full flex-wrap items-center gap-4 px-5 py-4 text-left transition-colors hover:bg-(--soft)"
          @click="toggle(riga.visita.id)"
        >
          <div class="flex min-w-24 flex-col gap-0.5">
            <span class="text-sm font-bold text-(--fg)">{{ formattaDataItalianaConMese(riga.visita.dataVisita) }}</span>
            <span class="text-xs text-(--fg4)">{{ riga.rel }}</span>
          </div>

          <Badge class="bg-(--mint) text-(--green)">{{ ETICHETTE_OBIETTIVO[riga.visita.obiettivo] }}</Badge>

          <div class="flex flex-1 flex-wrap gap-6">
            <div class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.pesoKg) }} <span class="text-xs font-sans text-(--fg4)">kg</span></span>
              <span
                v-if="riga.deltaPeso !== null"
                class="text-xs font-bold"
                :class="riga.deltaPeso <= 0 ? 'text-(--green)' : 'text-(--danger)'"
              >
                {{ riga.deltaPeso > 0 ? '+' : '' }}{{ formattaNumero(riga.deltaPeso) }} kg
              </span>
              <span v-else class="text-xs text-(--fg4)">Prima visita</span>
            </div>

            <div v-if="riga.visita.bmi !== null" class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.bmi) }} <span class="text-xs font-sans text-(--fg4)">BMI</span></span>
              <span class="text-xs text-(--fg4)">{{ riga.categoriaBmi }}</span>
            </div>

            <div v-if="riga.visita.plicometria" class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.plicometria.percentualeGrassoCorporeo) }}% <span class="text-xs font-sans text-(--fg4)">massa grassa</span></span>
              <span
                v-if="riga.deltaMg !== null"
                class="text-xs font-bold"
                :class="riga.deltaMg <= 0 ? 'text-(--green)' : 'text-(--danger)'"
              >
                {{ riga.deltaMg > 0 ? '+' : '' }}{{ formattaNumero(riga.deltaMg) }} pt
              </span>
            </div>
          </div>

          <ChevronDown :size="16" class="shrink-0 text-(--fg3) transition-transform" :class="{ 'rotate-180': idAperto === riga.visita.id }" />
        </button>

        <div v-if="idAperto === riga.visita.id" data-test="storico-dettaglio" class="border-t border-(--div) px-5 pb-5 pt-4">
          <div class="mb-4 flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled title="Presto disponibile">Modifica visita</Button>
            <Button variant="outline" size="sm" disabled title="Presto disponibile">Elimina visita</Button>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Generali</h5>
              <dl class="mt-2 space-y-1 text-sm">
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Peso</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.pesoKg) }} kg</dd></div>
                <div v-if="riga.visita.bmi !== null" class="flex items-center justify-between"><dt class="text-(--fg2)">BMI</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.bmi) }}</dd></div>
              </dl>
              <p v-if="riga.visita.note" class="mt-2 border-t border-(--div2) pt-2 text-xs leading-relaxed text-(--fg3)">{{ riga.visita.note }}</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">BIA</h5>
              <p class="mt-2 text-xs text-(--fg4)">Dati non disponibili.</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Plicometria</h5>
              <dl v-if="riga.visita.plicometria" class="mt-2 space-y-1 text-sm">
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">% Grasso</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.percentualeGrassoCorporeo) }}%</dd></div>
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Massa grassa</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.massaGrassaKg) }} kg</dd></div>
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Massa magra</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.massaMagraKg) }} kg</dd></div>
              </dl>
              <p v-else class="mt-2 text-xs text-(--fg4)">Nessuna plicometria registrata.</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenze</h5>
              <dl v-if="riga.haCirconferenze" class="mt-2 space-y-1 text-sm">
                <div v-for="c in riga.circonferenze" :key="c.label" class="flex items-center justify-between">
                  <dt class="text-(--fg2)">{{ c.label }}</dt>
                  <dd class="font-semibold text-(--fg)">{{ c.valore !== null ? `${formattaNumero(c.valore)} cm` : '—' }}</dd>
                </div>
              </dl>
              <p v-else class="mt-2 text-xs text-(--fg4)">Nessuna circonferenza registrata.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
