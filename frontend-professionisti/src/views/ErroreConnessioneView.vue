<script setup lang="ts">
import { RotateCw, WifiOff } from '@lucide/vue'
import { useOnline } from '@vueuse/core'
import { Button } from '@/components/ui/button'

defineProps<{ inCorso: boolean }>()
defineEmits<{ riprova: [] }>()

const online = useOnline()
</script>

<template>
  <main class="grid min-h-dvh place-items-center bg-background px-6 py-12 text-center text-foreground">
    <div class="w-full">
      <div class="mb-10 flex items-center justify-center gap-3">
        <img src="@/assets/hexis-logo.svg" alt="" class="size-11 rounded-lg bg-(--side) p-0.5" />
        <span class="font-heading text-xl font-semibold">Hexis Nutrition</span>
      </div>

      <WifiOff aria-hidden="true" class="mx-auto size-14 text-primary" />
      <div role="status">
        <h1 class="mt-4 font-heading text-3xl italic sm:text-4xl">
          {{ online ? 'Non riusciamo a raggiungere il server' : 'Sei offline' }}
        </h1>
        <p class="mt-3 text-sm leading-relaxed text-muted-foreground">
          {{ online ? 'Riprova tra poco per caricare la pagina.' : 'Riconnettiti a Internet e riprova.' }}
        </p>
      </div>

      <Button size="lg" class="mt-8 rounded-xl" :disabled="inCorso || !online" @click="$emit('riprova')">
        <RotateCw aria-hidden="true" :class="{ 'motion-safe:animate-spin': inCorso }" />
        {{ inCorso ? 'Tentativo in corso…' : 'Riprova' }}
      </Button>
    </div>
  </main>
</template>
