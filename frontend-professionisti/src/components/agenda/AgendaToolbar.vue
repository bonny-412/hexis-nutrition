<script setup lang="ts">
import { computed } from 'vue'
import { CalendarCheck, ChevronLeft, ChevronRight } from '@lucide/vue'
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

const filtri: { valore: FiltroAgenda; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'VISITE', etichetta: 'Visite' },
  { valore: 'PROMEMORIA', etichetta: 'Promemoria' },
]

const viste: { valore: VistaAgenda; etichetta: string }[] = [
  { valore: 'MESE', etichetta: 'Mese' },
  { valore: 'SETTIMANA', etichetta: 'Settimana' },
  { valore: 'GIORNO', etichetta: 'Giorno' },
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
      <div class="flex gap-1.5">
        <button
          v-for="f in filtri"
          :key="f.valore"
          type="button"
          class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
          :class="filtro === f.valore
            ? 'border-(--sage) bg-(--mint) text-(--green-d)'
            : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
          :aria-pressed="filtro === f.valore"
          :data-test="`agenda-filtro-${f.valore}`"
          @click="emit('update:filtro', f.valore)"
        >
          {{ f.etichetta }}
        </button>
      </div>

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
