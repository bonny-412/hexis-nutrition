<script setup lang="ts" generic="T extends string">
import type { Component } from 'vue'
import { RadioGroupItem, RadioGroupRoot } from 'reka-ui'

const props = defineProps<{
  modelValue: T
  etichetta: string
  opzioni: readonly { valore: T; etichetta: string; icona?: Component }[]
  prefissoTest: string
}>()
const emit = defineEmits<{ 'update:modelValue': [valore: T] }>()

function seleziona(valore: unknown) {
  const opzione = props.opzioni.find((o) => o.valore === valore)
  if (opzione) emit('update:modelValue', opzione.valore)
}
</script>

<template>
  <RadioGroupRoot
    :model-value="modelValue"
    :aria-label="etichetta"
    class="items-center rounded-lg border border-(--bd2) bg-(--surf) p-0.5"
    @update:model-value="seleziona"
  >
    <RadioGroupItem
      v-for="o in opzioni"
      :key="o.valore"
      :value="o.valore"
      :data-test="`${prefissoTest}-${o.valore}`"
      class="flex h-7 items-center gap-1.5 rounded-md px-2.5 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--fg) data-[state=checked]:bg-(--mint) data-[state=checked]:text-(--green-d) focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d)"
    >
      <component :is="o.icona" v-if="o.icona" :size="14" aria-hidden="true" />
      {{ o.etichetta }}
    </RadioGroupItem>
  </RadioGroupRoot>
</template>
