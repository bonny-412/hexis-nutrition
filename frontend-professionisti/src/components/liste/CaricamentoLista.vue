<script setup lang="ts">
withDefaults(defineProps<{ etichetta: string; righe?: number; schede?: boolean }>(), { righe: 6 })
</script>

<template>
  <div role="status" class="flex flex-col" :class="schede ? 'gap-4' : 'gap-2 p-4'">
    <span class="sr-only">{{ etichetta }}</span>
    <div
      v-for="n in righe"
      :key="n"
      aria-hidden="true"
      class="animate-pulse bg-(--hover) motion-reduce:animate-none"
      :class="schede ? 'h-28 rounded-2xl' : 'h-9 rounded-lg'"
    />
  </div>
</template>
