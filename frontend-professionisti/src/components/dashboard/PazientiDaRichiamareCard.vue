<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { CalendarPlus, RotateCcw } from '@lucide/vue'
import { cerca, type Paziente } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import CaricamentoLista from '@/components/liste/CaricamentoLista.vue'
import { aggiungiGiorni } from '@/utils/agenda'
import { formattaDataItalianaConMese } from '@/utils/data'

const props = defineProps<{ oggi: string }>()
const SETTIMANE_SOGLIA = 6
const MAX_PAZIENTI = 5

const pazienti = ref<Paziente[]>([])
const caricamento = ref(true)
const errore = ref(false)

async function carica() {
  caricamento.value = true
  errore.value = false
  try {
    const risultato = await cerca({
      archiviato: false,
      senzaProssimaVisita: true,
      dataUltimaVisitaA: aggiungiGiorni(props.oggi, -7 * SETTIMANE_SOGLIA),
      ordinaPer: 'dataUltimaVisita',
      direzione: 'asc',
      dimensione: MAX_PAZIENTI,
    })
    pazienti.value = risultato.contenuto
  } catch {
    errore.value = true
  } finally {
    caricamento.value = false
  }
}

onMounted(carica)
</script>

<template>
  <section class="overflow-clip rounded-2xl border border-(--bd) bg-(--surf)" aria-labelledby="titolo-pazienti-da-richiamare">
    <header class="px-5 pt-4 pb-3">
      <h2 id="titolo-pazienti-da-richiamare" class="font-heading text-lg italic text-(--fg)">Pazienti da richiamare</h2>
      <p class="mt-1 text-xs text-(--fg3)">Ultima visita almeno {{ SETTIMANE_SOGLIA }} settimane fa e nessuna prossima visita.</p>
    </header>
    <CaricamentoLista v-if="caricamento" etichetta="Caricamento dei pazienti da richiamare" :righe="2" />
    <div v-else-if="errore" class="flex flex-wrap items-center justify-between gap-2 px-5 pb-4">
      <p class="text-sm text-(--fg3)" role="status">Non è stato possibile caricare i pazienti da richiamare.</p>
      <Button type="button" variant="neutral" size="sm" @click="carica"><RotateCcw /> Riprova</Button>
    </div>
    <p v-else-if="pazienti.length === 0" class="px-5 pb-4 text-sm text-(--fg3)">Nessun paziente da richiamare.</p>
    <ul v-else class="divide-y divide-(--div) border-t border-(--div)">
      <li v-for="paziente in pazienti" :key="paziente.id" class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 px-5 py-3">
        <div class="min-w-0">
          <p class="wrap-break-word text-sm font-semibold text-(--green-d)">{{ paziente.nome }} {{ paziente.cognome }}</p>
          <p v-if="paziente.dataUltimaVisita" class="text-xs text-(--fg3)">
            Ultima visita <time :datetime="paziente.dataUltimaVisita">{{ formattaDataItalianaConMese(paziente.dataUltimaVisita) }}</time>
          </p>
        </div>
        <Button as-child size="sm" variant="neutral">
          <router-link
            :to="{ name: 'agenda', query: { nuovo: 'visita', pazienteId: paziente.id } }"
            :aria-label="`Nuovo appuntamento per ${paziente.nome} ${paziente.cognome}`"
          ><CalendarPlus aria-hidden="true" /> Nuovo appuntamento</router-link>
        </Button>
      </li>
    </ul>
  </section>
</template>
