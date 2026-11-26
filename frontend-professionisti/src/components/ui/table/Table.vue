<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <!-- Adattamento Hexis: da xl la tabella non ha scroll orizzontale proprio, così l'intestazione (sticky in
    TableHeader) resta in cima mentre scorre la pagina. Sotto xl resta lo scroll orizzontale. -->
  <div data-slot="table-container" class="relative w-full overflow-x-auto xl:overflow-x-visible">
    <table data-slot="table" :class="cn('w-full caption-bottom text-sm', props.class)">
      <slot />
    </table>
  </div>
</template>
