<script setup lang="ts">
import type { Component } from 'vue'

// Stato vuoto condiviso: dice cosa manca e, nello slot, offre il pulsante per farlo.
// `compatto` per le card piccole (Dashboard), la versione piena per le tab e le pagine.
withDefaults(
  defineProps<{
    icona: Component
    titolo: string
    testo?: string
    compatto?: boolean
    /** Livello del titolo nella pagina, per chi naviga per titoli con uno screen reader. */
    livello?: 'h2' | 'h3' | 'h4'
    /** `avviso` su sfondi ambra (giorno festivo in Agenda): il disco neutro risalterebbe come una toppa bianca. */
    tono?: 'neutro' | 'avviso'
  }>(),
  { compatto: false, livello: 'h3', tono: 'neutro' },
)
</script>

<template>
  <div
    data-test="stato-vuoto"
    class="flex flex-col items-center text-center"
    :class="compatto ? 'gap-2 px-5 py-6' : 'gap-3 px-6 py-10'"
  >
    <span
      class="flex items-center justify-center rounded-full"
      :class="[compatto ? 'size-10' : 'size-16', tono === 'avviso' ? 'bg-(--warn-fg)/10 text-(--warn-fg)/70' : 'bg-(--soft) text-(--fg4)']"
      aria-hidden="true"
    >
      <component :is="icona" :size="compatto ? 20 : 40" :stroke-width="1.5" />
    </span>
    <div>
      <component :is="livello" class="font-heading italic text-(--fg)" :class="compatto ? 'text-base' : 'text-lg'">{{ titolo }}</component>
      <p v-if="testo" class="mx-auto mt-1 max-w-sm text-sm text-(--fg3)">{{ testo }}</p>
    </div>
    <div v-if="$slots.default" class="mt-1 flex flex-wrap items-center justify-center gap-2">
      <slot />
    </div>
  </div>
</template>
