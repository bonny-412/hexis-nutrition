<script setup lang="ts">
import { computed } from 'vue'
import type { Paziente, Visita } from '@/api/pazienti'
import type { AndamentoPaziente } from '@/utils/andamento'
import { ETICHETTE_CIRCONFERENZE, formattaNumero } from '@/utils/visita'
import { calcolaEta, formattaDataItalianaEstesa } from '@/utils/data'
import { User, Mail, Phone, Smartphone, Briefcase, Stethoscope, Fingerprint, Cake, StickyNote, Pill, ClipboardPlus } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import StatoVuoto from '@/components/StatoVuoto.vue'
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
    <!-- I dati clinici vengono prima del profilo a ogni larghezza: da mobile il profilo scende in fondo. -->
    <div class="space-y-4">
      <div v-if="erroreVisite" class="rounded-2xl border border-(--bd) bg-(--surf) p-6 text-xs font-medium text-(--danger)">
        Non è stato possibile caricare lo storico delle visite.
      </div>

      <div v-else-if="visiteInCaricamento" class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div v-for="n in 3" :key="n" data-test="andamento-chart-skeleton" class="h-48 animate-pulse rounded-xl bg-(--hover)" />
      </div>

      <div v-else-if="visite.length === 0" class="rounded-2xl border border-(--bd) bg-(--surf)">
        <StatoVuoto
      livello="h4"
          :icona="Stethoscope"
          titolo="Nessuna visita registrata"
          testo="Peso, composizione corporea e grafici dell'andamento compaiono qui dalla prima visita."
        >
          <Button v-if="!paziente.archiviato" as-child>
            <router-link :to="`/pazienti/${paziente.id}/visite/nuova`" data-test="panoramica-prima-visita">
              <ClipboardPlus :size="16" /> Registra la prima visita
            </router-link>
          </Button>
        </StatoVuoto>
      </div>

      <div v-else class="space-y-4">
        <!-- Con una sola visita un grafico è un punto isolato: i valori sono già nella riga di indicatori in alto. -->
        <p
          v-if="visite.length === 1"
          data-test="andamento-una-visita"
          class="rounded-2xl border border-dashed border-(--bd2) px-5 py-3 text-sm text-(--fg3)"
        >
          I grafici dell'andamento compaiono dalla seconda visita.
        </p>
        <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <AndamentoChart titolo="Peso" unita="kg" :andamento="andamento.peso" colore="var(--chart-1)" :decimali="2" />
          <AndamentoChart titolo="BMI" unita="" :andamento="andamento.bmi" colore="var(--chart-2)" />

          <ComposizioneChart :grasso="andamento.percentualeGrassoCorporeo" :magra="andamento.massaMagra" />
      </div>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-5">
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
          <p v-else class="mt-3 text-sm text-(--fg3)">Nessuna plicometria registrata per l'ultima visita.</p>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-5">
          <h4 class="font-heading text-base italic text-(--fg)">Circonferenze</h4>
          <dl v-if="haCirconferenze" class="mt-3 divide-y divide-(--div) text-sm">
            <div v-for="c in circonferenzeUltimaVisita" :key="c.label" class="flex items-center justify-between py-2 first:pt-0 last:pb-0">
              <dt class="text-(--fg3)">{{ c.label }}</dt>
              <dd class="font-semibold text-(--fg)">{{ c.valore !== null ? `${formattaNumero(c.valore)} cm` : '—' }}</dd>
            </div>
          </dl>
          <p v-else class="mt-3 text-sm text-(--fg3)">Nessuna circonferenza registrata per l'ultima visita.</p>
        </div>
      </div>
      <!-- La BIA non è ancora supportata: una riga, non una card vuota accanto ai dati veri. -->
      <p class="rounded-xl border border-dashed border-(--bd2) px-4 py-2.5 text-xs text-(--fg3)">
        <span class="font-semibold text-(--fg2)">BIA</span> · la bioimpedenziometria non è ancora supportata.
      </p>
    </div>
    </div>

    <!-- Colonna laterale: profilo del paziente, indipendente dalle visite -->
    <aside class="rounded-2xl border border-(--bd) bg-(--surf) p-6">
      <div class="mb-5 flex items-center gap-3">
        <div>
          <h3 class="font-heading text-lg italic text-(--fg)">Profilo</h3>
          <p class="text-xs text-(--fg3)">Dati anagrafici e contatti</p>
        </div>
      </div>

      <dl class="space-y-3.5 text-sm">
        <div class="flex items-center gap-3">
          <Fingerprint :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Codice Fiscale</dt>
            <dd class="truncate font-mono font-medium uppercase text-(--fg)">{{ paziente.codiceFiscale }}</dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Cake :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Data di Nascita</dt>
            <dd class="font-medium text-(--fg)">
              {{ paziente.dataNascita ? formattaDataItalianaEstesa(paziente.dataNascita) : '—' }}
              <span v-if="eta !== null" class="text-xs text-(--fg3)">({{ eta }} anni)</span>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <User :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Sesso</dt>
            <dd class="font-medium text-(--fg)">{{ ETICHETTE_SESSO[paziente.sesso] ?? paziente.sesso }}</dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Mail :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Email</dt>
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
          <Phone :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Telefono</dt>
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
          <Smartphone :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Cellulare</dt>
            <dd>
              <a
                v-if="paziente.cellulare"
                :href="`tel:${paziente.cellulare}`"
                class="font-medium text-(--fg) underline-offset-4 hover:text-(--green) hover:underline"
              >
                {{ paziente.cellulare }}
              </a>
              <span v-else class="font-medium text-(--fg3)">—</span>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Briefcase :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Occupazione</dt>
            <dd class="font-medium text-(--fg)">
              {{ paziente.lavoro ?? '—' }}
              <span v-if="paziente.stileDiVita" class="text-xs text-(--fg3)">· {{ ETICHETTE_STILE_DI_VITA[paziente.stileDiVita] }}</span>
            </dd>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Stethoscope :size="16" class="shrink-0 text-(--fg4)" />
          <div class="min-w-0 flex-1">
            <dt class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">Medico di riferimento</dt>
            <dd class="font-medium text-(--fg)">
              {{ paziente.medicoRiferimento ?? '—' }}
            </dd>
          </div>
        </div>
      </dl>

      <div class="mt-5 flex gap-2.5 rounded-xl bg-(--soft) border-l-2 border-(--sage) p-3.5">
        <StickyNote :size="16" class="mt-0.5 shrink-0 text-(--fg4)" />
        <p class="text-sm" :class="paziente.note ? 'text-(--fg2)' : 'italic text-(--fg3)'">
          {{ paziente.note || 'Nessuna storia inserita per questo paziente.' }}
        </p>
      </div>

      <div class="mt-3 flex gap-2.5 rounded-xl bg-(--soft) border-l-2 border-(--sage) p-3.5">
        <Pill :size="16" class="mt-0.5 shrink-0 text-(--fg4)" />
        <p class="text-sm" :class="paziente.trattamenti ? 'text-(--fg2)' : 'italic text-(--fg3)'">
          {{ paziente.trattamenti || 'Nessun trattamento o terapia inserito per questo paziente.' }}
        </p>
      </div>
    </aside>
  </div>
</template>
