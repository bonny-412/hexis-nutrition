<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import { LayoutGrid, Calendar, Users, FileText, MessageSquare, BarChart3, Apple } from '@lucide/vue'

const props = withDefaults(defineProps<{ class?: string }>(), { class: '' })

const voci = [
  { nome: 'Dashboard', routeName: 'dashboard', icona: LayoutGrid },
  { nome: 'Agenda', icona: Calendar },
  { nome: 'Pazienti', routeName: 'pazienti', icona: Users },
  { nome: 'Piani alimentari', icona: FileText },
  { nome: 'Chat', icona: MessageSquare },
  { nome: 'Analytics', icona: BarChart3 },
]

const route = useRoute()
</script>

<template>
  <div
    :class="['flex w-[246px] flex-shrink-0 flex-col justify-between bg-[var(--side)] p-[18px_14px_14px] text-[var(--side-fg)]', props.class]"
  >
    <div>
      <div class="mb-5 flex items-center gap-2.5 px-1.5">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-[34px] w-[34px] flex-none rounded-[10px] bg-white" />
        <div class="flex flex-col gap-px">
          <span class="font-heading text-base font-semibold leading-tight text-white">Hexis</span>
          <span class="text-[10px] uppercase tracking-[0.14em] text-[var(--side-fg2)]">Professionisti</span>
        </div>
      </div>

      <nav class="flex flex-col gap-0.5">
        <component
          :is="voce.routeName ? RouterLink : 'span'"
          v-for="voce in voci"
          :key="voce.nome"
          :to="voce.routeName ? { name: voce.routeName } : undefined"
          class="flex items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium"
          :class="
            voce.routeName && route.name === voce.routeName
              ? 'bg-[var(--side-act-bg)] text-white'
              : voce.routeName
                ? 'text-[var(--side-fg)]'
                : 'cursor-not-allowed text-[var(--side-fg2)]'
          "
        >
          <component :is="voce.icona" :size="16" :stroke-width="1.8" />
          <span>{{ voce.nome }}</span>
        </component>
      </nav>
    </div>

    <div>
      <div class="mb-1.5 px-2.5 text-[10px] uppercase tracking-[0.14em] text-[var(--side-fg2)]">Risorse</div>
      <span class="flex cursor-not-allowed items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium text-[var(--side-fg2)]">
        <Apple :size="16" :stroke-width="1.8" />
        <span>Alimenti</span>
      </span>
    </div>
  </div>
</template>
