<script setup lang="ts">
import { computed } from 'vue'
import { Calendar1, CalendarCheck, CalendarDays, CalendarRange, ChevronLeft, ChevronRight } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import SelettoreSegmentato from '@/components/ui/SelettoreSegmentato.vue'
import {
  aggiungiGiorni,
  aggiungiMesi,
  etichettaPeriodo,
  oggiIso,
  type FiltroAgenda,
  type VistaAgenda,
} from '@/utils/agenda'

const props = defineProps<{ vista: VistaAgenda; riferimento: string; filtro: FiltroAgenda }>()

const emit = defineEmits<{
  'update:vista': [vista: VistaAgenda]
  'update:riferimento': [riferimento: string]
  'update:filtro': [filtro: FiltroAgenda]
}>()

// I filtri dicono COSA mostrare e portano il pallino del colore dell'evento, come la legenda e la Dashboard;
// le viste dicono COME mostrarlo e restano il selettore a segmenti: così i due gruppi non si confondono.
const filtri: { valore: FiltroAgenda; etichetta: string; pallino?: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'VISITE', etichetta: 'Visite', pallino: 'bg-(--green)' },
  { valore: 'PROMEMORIA', etichetta: 'Promemoria', pallino: 'bg-(--warn-fg)' },
]

// Con le icone, come l'interruttore Tabella/Schede delle liste: stesso componente, stessa resa.
const viste: { valore: VistaAgenda; etichetta: string; icona: typeof CalendarDays }[] = [
  { valore: 'MESE', etichetta: 'Mese', icona: CalendarDays },
  { valore: 'SETTIMANA', etichetta: 'Settimana', icona: CalendarRange },
  { valore: 'GIORNO', etichetta: 'Giorno', icona: Calendar1 },
]

const etichetta = computed(() => etichettaPeriodo(props.vista, props.riferimento))

function sposta(direzione: 1 | -1) {
  const nuovo =
    props.vista === 'MESE'
      ? aggiungiMesi(props.riferimento, direzione)
      : aggiungiGiorni(props.riferimento, direzione * (props.vista === 'SETTIMANA' ? 7 : 1))
  emit('update:riferimento', nuovo)
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-3">
    <div class="flex items-center gap-1.5">
      <Button type="button" variant="neutral" size="icon" aria-label="Periodo precedente" data-test="agenda-precedente" @click="sposta(-1)">
        <ChevronLeft :size="16" />
      </Button>
      <Button type="button" variant="neutral" data-test="agenda-oggi" @click="emit('update:riferimento', oggiIso())"><CalendarCheck /> Oggi</Button>
      <Button type="button" variant="neutral" size="icon" aria-label="Periodo successivo" data-test="agenda-successivo" @click="sposta(1)">
        <ChevronRight :size="16" />
      </Button>
    </div>

    <h2 class="min-w-44 font-heading text-xl italic text-(--fg)" data-test="agenda-etichetta">{{ etichetta }}</h2>

    <div class="ml-auto flex flex-wrap items-center gap-3">
      <div class="flex gap-1.5" role="group" aria-label="Mostra">
        <Button
          v-for="f in filtri"
          :key="f.valore"
          type="button"
          variant="chip"
          size="chip"
          :aria-pressed="filtro === f.valore"
          :data-test="`agenda-filtro-${f.valore}`"
          @click="emit('update:filtro', f.valore)"
        >
          <span v-if="f.pallino" class="size-1.5 rounded-full" :class="f.pallino" aria-hidden="true" />
          {{ f.etichetta }}
        </Button>
      </div>

      <span class="hidden h-6 w-px bg-(--bd2) sm:block" aria-hidden="true" />

      <SelettoreSegmentato
        :model-value="vista"
        :opzioni="viste"
        etichetta="Vista dell'agenda"
        prefisso-test="agenda-vista"
        class="inline-flex"
        @update:model-value="emit('update:vista', $event)"
      />
    </div>
  </div>
</template>
