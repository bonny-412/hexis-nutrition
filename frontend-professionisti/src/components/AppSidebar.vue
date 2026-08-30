<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'

const voci = [
  { nome: 'Dashboard', routeName: 'dashboard', icona: '▦' },
  { nome: 'Agenda', icona: '📅' },
  { nome: 'Pazienti', routeName: 'pazienti', icona: '👥' },
  { nome: 'Piani alimentari', icona: '📄' },
  { nome: 'Chat', icona: '💬' },
  { nome: 'Analytics', icona: '📊' },
]

const route = useRoute()
</script>

<template>
  <aside class="flex w-64 flex-shrink-0 flex-col justify-between p-4" style="background: var(--green-d)">
    <div>
      <div class="mb-6 flex items-center gap-2 px-2">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-9 w-9 rounded-lg bg-white p-1" />
        <div>
          <div class="font-semibold text-white">Hexis</div>
          <div class="text-[10px] uppercase tracking-wide text-white/60">Professionisti</div>
        </div>
      </div>

      <nav class="flex flex-col gap-1">
        <component
          :is="voce.routeName ? RouterLink : 'span'"
          v-for="voce in voci"
          :key="voce.nome"
          :to="voce.routeName ? { name: voce.routeName } : undefined"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium"
          :class="
            voce.routeName
              ? route.name === voce.routeName
                ? 'bg-white/10 text-white'
                : 'text-white/80 hover:bg-white/5'
              : 'cursor-not-allowed text-white/35'
          "
        >
          <span>{{ voce.icona }}</span>
          <span>{{ voce.nome }}</span>
        </component>
      </nav>
    </div>

    <div class="px-2 text-[10px] uppercase tracking-wide text-white/40">
      Risorse
      <div class="mt-2 flex items-center gap-2.5 text-sm font-medium text-white/35">
        <span>🍎</span><span>Alimenti</span>
      </div>
    </div>
  </aside>
</template>
