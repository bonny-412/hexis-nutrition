<script setup lang="ts">
import { computed } from 'vue'

// Conteggio dei caratteri sotto un campo con `maxlength` (collaudo del 27/11/2026, gruppo 5).
// Sui campi brevi compare solo vicino al limite, da 80% in su: un "12 / 100" sotto ogni nome sarebbe
// rumore. Sui testi lunghi (note, `sempre`) resta visibile, perché lì il limite si raggiunge scrivendo.
const props = withDefaults(defineProps<{ valore: string | null | undefined; max: number; sempre?: boolean }>(), {
  sempre: false,
})

const lunghezza = computed(() => (props.valore ?? '').length)
const visibile = computed(() => props.sempre || lunghezza.value >= props.max * 0.8)
const alLimite = computed(() => lunghezza.value >= props.max)
</script>

<template>
  <p
    v-if="visibile"
    data-test="contatore-caratteri"
    class="self-end text-right text-xs tabular-nums"
    :class="alLimite ? 'font-medium text-(--danger)' : 'text-(--fg3)'"
  >
    {{ alLimite ? `Massimo ${max} caratteri` : `${lunghezza} / ${max}` }}
  </p>
</template>
