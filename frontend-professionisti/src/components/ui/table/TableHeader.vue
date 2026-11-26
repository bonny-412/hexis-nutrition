<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <!-- Adattamento Hexis: intestazione fissa da xl mentre scorre la pagina. -top-4 compensa il py-4 del <main>
    (AppShell), altrimenti nella fessura sopra l'intestazione si vedrebbero passare le righe. -->
  <thead
    data-slot="table-header"
    :class="cn('[&_tr]:border-b xl:sticky xl:-top-4 xl:z-10 xl:bg-(--surf) xl:shadow-[0_1px_0_var(--bd)]', props.class)"
  >
    <slot />
  </thead>
</template>
