<script setup lang="ts">
import { LayoutGrid, Rows3 } from '@lucide/vue'
import SelettoreSegmentato from '@/components/ui/SelettoreSegmentato.vue'
import type { VistaLista } from '@/composables/useVistaLista'

// Interruttore Tabella / Schede, solo da desktop: su mobile le liste sono sempre a schede.
defineProps<{ modelValue: VistaLista }>()
const emit = defineEmits<{ 'update:modelValue': [vista: VistaLista] }>()

const opzioni: { valore: VistaLista; etichetta: string; icona: typeof Rows3 }[] = [
  { valore: 'tabella', etichetta: 'Tabella', icona: Rows3 },
  { valore: 'schede', etichetta: 'Schede', icona: LayoutGrid },
]
</script>

<template>
  <SelettoreSegmentato
    :model-value="modelValue"
    :opzioni="opzioni"
    etichetta="Vista della lista"
    prefisso-test="vista"
    class="hidden md:inline-flex"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>
