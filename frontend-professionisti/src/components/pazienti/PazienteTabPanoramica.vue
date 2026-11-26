<script setup lang="ts">
import { computed } from 'vue'
import type { Paziente, Visita } from '@/api/pazienti'
import type { AndamentoPaziente } from '@/utils/andamento'
import { formattaDataItalianaConMese } from '@/utils/data'
import { Stethoscope, ClipboardPlus } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import StatoVuoto from '@/components/StatoVuoto.vue'
import AndamentoChart from './AndamentoChart.vue'
import ComposizioneChart from './ComposizioneChart.vue'
import CirconferenzeCard from './CirconferenzeCard.vue'
import PlicometriaCard from './PlicometriaCard.vue'
import ProfiloPazienteCard from './ProfiloPazienteCard.vue'

const props = defineProps<{
  paziente: Paziente
  visiteInCaricamento: boolean
  erroreVisite: boolean
  visite: Visita[]
  andamento: AndamentoPaziente
}>()

const emit = defineEmits<{ modifica: [] }>()

const ultimaVisita = computed<Visita | null>(() =>
  props.visite.length > 0 ? props.visite[props.visite.length - 1] : null,
)

const didascaliaUltimaVisita = computed(() =>
  ultimaVisita.value ? `Ultima visita, ${formattaDataItalianaConMese(ultimaVisita.value.dataVisita)}` : undefined,
)
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
          <!-- Peso e BMI sono grafici a una serie affiancati: stesso petrolio, il titolo basta a distinguerli. -->
          <AndamentoChart titolo="BMI" unita="" :andamento="andamento.bmi" colore="var(--chart-1)" />

          <ComposizioneChart :grasso="andamento.percentualeGrassoCorporeo" :magra="andamento.massaMagra" />
      </div>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:items-start">

        <PlicometriaCard :visita="ultimaVisita" :didascalia="didascaliaUltimaVisita" />

        <CirconferenzeCard :visita="ultimaVisita" :didascalia="didascaliaUltimaVisita" />
      </div>
      <!-- La BIA non è ancora supportata: una riga, non una card vuota accanto ai dati veri. -->
      <p class="rounded-xl border border-dashed border-(--bd2) px-4 py-2.5 text-xs text-(--fg3)">
        <span class="font-semibold text-(--fg2)">BIA</span> · la bioimpedenziometria non è ancora supportata.
      </p>
    </div>
    </div>

    <!-- Colonna laterale: profilo del paziente, indipendente dalle visite -->
    <ProfiloPazienteCard :paziente="paziente" @modifica="emit('modifica')" />
  </div>
</template>
