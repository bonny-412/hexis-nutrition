<script setup lang="ts">
import { LayoutGrid, Rows3 } from '@lucide/vue'
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
  <div role="radiogroup" aria-label="Vista della lista" class="hidden items-center rounded-lg border border-(--bd2) bg-(--surf) p-0.5 md:inline-flex">
    <button
      v-for="o in opzioni"
      :key="o.valore"
      type="button"
      role="radio"
      :aria-checked="modelValue === o.valore"
      :data-test="`vista-${o.valore}`"
      class="flex h-7 items-center gap-1.5 rounded-md px-2.5 text-xs font-semibold transition-colors"
      :class="modelValue === o.valore ? 'bg-(--mint) text-(--green-d)' : 'text-(--fg3) hover:text-(--fg)'"
      @click="emit('update:modelValue', o.valore)"
    >
      <component :is="o.icona" :size="14" />
      {{ o.etichetta }}
    </button>
  </div>
</template>
