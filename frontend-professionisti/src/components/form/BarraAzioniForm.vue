<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Save, X } from '@lucide/vue'

// Barra "Annulla / Salva" dei form lunghi (nuovo paziente, visita, piano): resta in fondo all'area
// che scorre (sticky dentro il <main> di AppShell) e a fine pagina si appoggia sotto l'ultimo contenuto.
// È una card flottante: sticky si ferma al bordo del padding di 16 px del <main>, quindi resta staccata
// dal fondo come dai lati. Ha l'ombra perché sta sopra la pagina (ADR 0009, "Ombre").
// Lo stato "modificato" e la logica di uscita restano nelle view: qui c'è solo la presentazione.
withDefaults(
  defineProps<{
    modificato: boolean
    inCorso: boolean
    etichettaSalva: string
    salvaDisabilitato?: boolean
    tipoSalva?: 'submit' | 'button'
    dataTestAnnulla?: string
  }>(),
  { salvaDisabilitato: false, tipoSalva: 'submit', dataTestAnnulla: 'btn-annulla' },
)

const emit = defineEmits<{ annulla: []; salva: [] }>()
</script>

<template>
  <div
    data-test="barra-azioni-form"
    class="sticky bottom-0 z-20 flex flex-col gap-2 rounded-2xl border border-(--bd) bg-(--surf) p-2.5 shadow-lg sm:ml-auto sm:w-fit sm:flex-row sm:items-center sm:gap-4 print:hidden"
  >
    <p class="flex min-h-4 items-center gap-2 px-1.5 text-xs text-(--fg3) empty:hidden sm:pl-2.5 sm:pr-0" aria-live="polite">
      <template v-if="modificato">
        <span class="size-2 shrink-0 rounded-full bg-(--lime) ring-1 ring-(--green)/30" aria-hidden="true" />
        Modifiche non salvate
      </template>
    </p>
    <div class="flex items-center gap-2">
      <Button
        type="button"
        variant="neutral"
        size="lg"
        class="flex-1 rounded-xl sm:flex-none"
        :data-test="dataTestAnnulla"
        :disabled="inCorso"
        @click="emit('annulla')"
      >
        <X :size="16" />
        Annulla
      </Button>
      <Button
        :type="tipoSalva"
        size="lg"
        class="flex-1 rounded-xl sm:flex-none"
        data-test="btn-salva"
        :disabled="inCorso || salvaDisabilitato"
        @click="tipoSalva === 'button' && emit('salva')"
      >
        <Save :size="16" />
        {{ inCorso ? 'Salvataggio…' : etichettaSalva }}
      </Button>
    </div>
  </div>
</template>
