<script setup lang="ts">
import { computed } from 'vue'
import type { Paziente, Visita } from '@/api/pazienti'
import type { AndamentoPaziente } from '@/utils/andamento'
import { ETICHETTE_CIRCONFERENZE, formattaNumero } from '@/utils/visita'
import { calcolaEta, formattaDataItalianaEstesa } from '@/utils/data'
import { User, Mail, Phone, Briefcase, Fingerprint, Cake, StickyNote } from '@lucide/vue'
import AndamentoChart from './AndamentoChart.vue'
import ComposizioneChart from './ComposizioneChart.vue'

const ETICHETTE_SESSO: Record<string, string> = {
  M: 'Maschio',
  F: 'Femmina',
  ALTRO: 'Altro',
}

const ETICHETTE_STILE_DI_VITA: Record<string, string> = {
  SEDENTARIO: 'Sedentario',
  POCO_ATTIVO: 'Poco attivo',
  ATTIVO: 'Attivo',
  MOLTO_ATTIVO: 'Molto attivo',
  ESTREMAMENTE_ATTIVO: 'Estremamente attivo',
}

const props = defineProps<{
  paziente: Paziente
  visiteInCaricamento: boolean
  erroreVisite: boolean
  visite: Visita[]
  andamento: AndamentoPaziente
}>()

const eta = computed(() => (props.paziente.dataNascita ? calcolaEta(props.paziente.dataNascita) : null))

const ultimaVisita = computed<Visita | null>(() =>
  props.visite.length > 0 ? props.visite[props.visite.length - 1] : null,
)

const circonferenzeUltimaVisita = computed(() => {
  const visita = ultimaVisita.value
  return (Object.keys(ETICHETTE_CIRCONFERENZE) as Array<keyof Visita['circonferenze']>).map((chiave) => ({
    label: ETICHETTE_CIRCONFERENZE[chiave],
    valore: visita ? visita.circonferenze[chiave] : null,
  }))
})

const haCirconferenze = computed(() => circonferenzeUltimaVisita.value.some((c) => c.valore !== null))

</script>

<template>
  <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_320px] xl:items-start">
    <!-- Colonna principale: andamento clinico, indipendente dai dati anagrafici -->
    <!-- Su schermi piccoli il profilo (colonna laterale) precede queste card: vedi order-* qui e sull'aside. -->
    <div class="order-2 space-y-4 xl:order-1">
      <div v-if="erroreVisite" class="rounded-2xl border border-(--bd) bg-(--surf) p-6 text-xs font-medium text-(--danger) shadow-sm">
        Non è stato possibile caricare lo storico delle visite.
      </div>

      <div v-else-if="visiteInCaricamento" class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div v-for="n in 3" :key="n" data-test="andamento-chart-skeleton" class="h-48 animate-pulse rounded-xl bg-(--hover)" />
      </div>

      <div v-else-if="visite.length === 0" class="rounded-2xl border border-(--bd) bg-(--surf) p-8 text-center text-sm text-(--fg3)">
        <h4 class="font-heading text-lg italic text-(--fg)">Nessuna visita registrata</h4>
        <p class="mx-auto mt-1.5 max-w-sm text-sm text-(--fg3)">
          Nessun dato clinico da mostrare. I dettagli dell'ultima visita appariranno qui non appena registrata.
        </p>
      </div>

      <div v-else class="space-y-4">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <AndamentoChart titolo="Peso" unita="kg" :andamento="andamento.peso" colore="var(--chart-1)" :decimali="2" />
          <AndamentoChart titolo="BMI" unita="" :andamento="andamento.bmi" colore="var(--chart-2)" />

          <ComposizioneChart :grasso="andamento.percentualeGrassoCorporeo" :magra="andamento.massaMagra" />
      </div>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-5 shadow-sm">
          <h4 class="font-heading text-base italic text-(--fg)">BIA</h4>
          <p class="mt-3 text-sm text-(--fg4)">Dati non disponibili: la bioimpedenziometria non è ancora supportata.</p>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-5 shadow-sm">
          <h4 class="font-heading text-base italic text-(--fg)">Plicometria</h4>
          <dl v-if="ultimaVisita?.plicometria" class="mt-3 divide-y divide-(--div) text-sm">
            <div class="flex items-center justify-between py-2 first:pt-0">
              <dt class="text-(--fg3)">% Massa grassa</dt>
              <dd class="font-semibold text-(--fg)">{{ formattaNumero(ultimaVisita.plicometria.percentualeGrassoCorporeo) }}%</dd>
            </div>
            <div class="flex items-center justify-between py-2">
              <dt class="text-(--fg3)">Massa grassa</dt>
              <dd class="font-semibold text-(--fg)">{{ formattaNumero(ultimaVisita.plicometria.massaGrassaKg) }} kg</dd>
            </div>
            <div class="flex items-center justify-between py-2">
              <dt class="text-(--fg3)">Massa magra</dt>
              <dd class="font-semibold text-(--fg)">{{ formattaNumero(ultimaVisita.plicometria.massaMagraKg) }} kg</dd>
            </div>
            <div class="flex items-center justify-between py-2">
              <dt class="text-(--fg3)">FMI</dt>
              <dd class="font-semibold text-(--fg)">{{ formattaNumero(ultimaVisita.plicometria.fmi) }}</dd>
            </div>
            <div class="flex items-center justify-between py-2 last:pb-0">
              <dt class="text-(--fg3)">FFMI</dt>
              <dd class="font-semibold text-(--fg)">{{ formattaNumero(ultimaVisita.plicometria.ffmi) }}</dd>
            </div>
          </dl>
          <p v-else class="mt-3 text-sm text-(--fg4)">Nessuna plicometria registrata per l'ultima visita.</p>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-5 shadow-sm">
          <h4 class="font-heading text-base italic text-(--fg)">Circonferenze</h4>
          <dl v-if="haCirconferenze" class="mt-3 divide-y divide-(--div) text-sm">
            <div v-for="c in circonferenzeUltimaVisita" :key="c.label" class="flex items-center justify-between py-2 first:pt-0 last:pb-0">
              <dt class="text-(--fg3)">{{ c.label }}</dt>
              <dd class="font-semibold text-(--fg)">{{ c.valore !== null ? `${formattaNumero(c.valore)} cm` : '—' }}</dd>
            </div>
          </dl>
          <p v-else class="mt-3 text-sm text-(--fg4)">Nessuna circonferenza registrata per l'ultima visita.</p>
        </div>
      </div>
    </div>
    </div>

    <!-- Colonna laterale: profilo del paziente, indipendente dalle visite -->
    <aside class="order-1 rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm xl:order-2">
      <div class="mb-5 flex items-center gap-3">
        <div>
          <h3 class="font-heading text-lg italic text-(--fg)">Profilo</h3>
          <p class="text-xs text-(--fg3)">Dati anagrafici e contatti</p>
        </div>
      </div>

      <dl class="space-y-3.5 text-sm">
        <div class="flex items-center gap-3">
          <Fingerprint :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Codice Fiscale</dt>
            <dd class="truncate font-mono font-medium uppercase text-(--fg)">{{ paziente.codiceFiscale }}</dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Cake :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Data di Nascita</dt>
            <dd class="font-medium text-(--fg)">
              {{ paziente.dataNascita ? formattaDataItalianaEstesa(paziente.dataNascita) : '—' }}
              <span v-if="eta !== null" class="text-xs text-(--fg3)">({{ eta }} anni)</span>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <User :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Sesso</dt>
            <dd class="font-medium text-(--fg)">{{ ETICHETTE_SESSO[paziente.sesso] ?? paziente.sesso }}</dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Mail :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Email</dt>
            <dd class="truncate">
              <a
                :href="`mailto:${paziente.email}`"
                class="font-medium text-(--fg) underline-offset-4 hover:text-(--green) hover:underline"
              >
                {{ paziente.email }}
              </a>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Phone :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Telefono</dt>
            <dd>
              <a
                v-if="paziente.telefono"
                :href="`tel:${paziente.telefono}`"
                class="font-medium text-(--fg) underline-offset-4 hover:text-(--green) hover:underline"
              >
                {{ paziente.telefono }}
              </a>
              <span v-else class="font-medium text-(--fg3)">—</span>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Briefcase :size="15" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Occupazione</dt>
            <dd class="font-medium text-(--fg)">
              {{ paziente.lavoro ?? '—' }}
              <span v-if="paziente.stileDiVita" class="text-xs text-(--fg3)">· {{ ETICHETTE_STILE_DI_VITA[paziente.stileDiVita] }}</span>
            </dd>
          </div>
        </div>
      </dl>

      <div class="mt-5 flex gap-2.5 rounded-xl bg-(--soft) border-l-2 border-(--sage) p-3.5">
        <StickyNote :size="15" class="mt-0.5 shrink-0 text-(--fg4)" />
        <p class="text-sm" :class="paziente.note ? 'text-(--fg2)' : 'italic text-(--fg4)'">
          {{ paziente.note || 'Nessuna nota per questo paziente.' }}
        </p>
      </div>
    </aside>
  </div>
</template>
