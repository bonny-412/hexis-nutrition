<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <div
    data-slot="alert-dialog-footer"
    :class="
      cn(
        'bg-muted/50 -mx-4 -mb-4 flex shrink-0 flex-wrap items-center justify-end gap-2 rounded-b-xl border-t p-4',
        props.class,
      )
    "
  >
    <slot />
  </div>
</template>
