<script setup lang="ts" generic="T extends string">
import type { AcceptableValue } from 'reka-ui'
import { ArrowDown, ArrowUp } from '@lucide/vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

// Nella vista a schede non ci sono intestazioni di colonna: l'ordinamento passa da qui.
// Stessi campi e stessa logica della tabella; "Ordine predefinito" corrisponde a nessun ordinamento esplicito.
const props = defineProps<{
  opzioni: { valore: T; etichetta: string }[]
  ordinaPer: T | undefined
  direzione: 'asc' | 'desc'
}>()
const emit = defineEmits<{ cambia: [ordinaPer: T | undefined, direzione: 'asc' | 'desc'] }>()

const PREDEFINITO = '__predefinito'

function onCampo(valore: AcceptableValue) {
  emit('cambia', valore === PREDEFINITO ? undefined : (valore as T), 'asc')
}

function invertiDirezione() {
  emit('cambia', props.ordinaPer, props.direzione === 'asc' ? 'desc' : 'asc')
}
</script>

<template>
  <div class="flex items-center gap-1.5">
    <Select :model-value="ordinaPer ?? PREDEFINITO" @update:model-value="onCampo">
      <SelectTrigger id="ordinamento-schede" aria-label="Ordina per" class="w-48 bg-(--surf)"><SelectValue /></SelectTrigger>
      <SelectContent>
        <SelectItem :value="PREDEFINITO">Ordine predefinito</SelectItem>
        <SelectItem v-for="o in opzioni" :key="o.valore" :value="o.valore">{{ o.etichetta }}</SelectItem>
      </SelectContent>
    </Select>
    <Button
      type="button"
      variant="outline"
      size="icon"
      :disabled="!ordinaPer"
      :aria-label="direzione === 'asc' ? 'Ordine crescente, passa a decrescente' : 'Ordine decrescente, passa a crescente'"
      class="text-(--fg3) disabled:opacity-40"
      @click="invertiDirezione"
    >
      <component :is="direzione === 'asc' ? ArrowUp : ArrowDown" :size="14" />
    </Button>
  </div>
</template>
