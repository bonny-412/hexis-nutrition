<script setup lang="ts">
import { CircleAlert, RotateCcw } from '@lucide/vue'
import StatoVuoto from '@/components/StatoVuoto.vue'
import { Button } from '@/components/ui/button'

defineProps<{ messaggio: string; compatto?: boolean }>()
defineEmits<{ riprova: [] }>()
</script>

<template>
  <StatoVuoto :icona="CircleAlert" :titolo="messaggio" :compatto="compatto" livello="h2" role="status">
    <Button type="button" variant="neutral" @click="$emit('riprova')"><RotateCcw /> Riprova</Button>
  </StatoVuoto>
</template>
