<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import { LayoutGrid, Calendar, Users, FileText, MessageSquare, BarChart3, Apple, PanelLeftOpen, PanelLeftClose } from '@lucide/vue'
import { ref } from 'vue';

const props = withDefaults(defineProps<{ class?: string }>(), { class: '' })

const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const voci = [
  { nome: 'Dashboard', routeName: 'dashboard', routeNamesAttivi: ['dashboard'], icona: LayoutGrid },
  { nome: 'Agenda', icona: Calendar },
  { nome: 'Pazienti', routeName: 'pazienti', routeNamesAttivi: ['pazienti', 'paziente-nuovo', 'paziente-dettaglio'], icona: Users },
  { nome: 'Piani alimentari', icona: FileText },
  { nome: 'Chat', icona: MessageSquare },
  { nome: 'Analytics', icona: BarChart3 },
]

const route = useRoute()
</script>

<template>
  <div
    :class="['flex shrink-0 flex-col justify-between bg-(--side) p-[18px_14px_14px] text-(--side-fg)', isCollapsed ? 'w-16 px-2' : 'w-61.5', props.class]"
  >
    <div>
      <div class="mb-5 flex items-center gap-2.5 px-1.5" :class="{ 'justify-center px-0': isCollapsed }">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-8.5 w-8.5 flex-none rounded-[10px] bg-white" />
        <div v-if="!isCollapsed" class="flex flex-col gap-px overflow-hidden whitespace-nowrap">
          <span class="font-heading text-base font-semibold leading-tight text-white">Hexis</span>
          <span class="text-[10px] uppercase tracking-[0.14em] text-(--side-fg2)">Professionisti</span>
        </div>
      </div>

      <nav class="flex flex-col gap-0.5">
        <component
          :is="voce.routeName ? RouterLink : 'span'"
          v-for="voce in voci"
          :key="voce.nome"
          :to="voce.routeName ? { name: voce.routeName } : undefined"
          class="flex items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium transition-colors"
          :class="[
            isCollapsed ? 'justify-center px-0' : '',
            voce.routeNamesAttivi?.includes(route.name as string)
              ? 'bg-(--side-act-bg) text-white'
              : voce.routeName
                ? 'text-(--side-fg) hover:bg-(--side-act-bg)/50'
                : 'cursor-not-allowed text-(--side-fg2)',
          ]"
        >
          <component :is="voce.icona" :size="16" :stroke-width="1.8" />
          <span v-if="!isCollapsed" class="truncate">{{ voce.nome }}</span>
        </component>
      </nav>
    </div>

    <div class="flex flex-col gap-2">
      <div>
        <div v-if="!isCollapsed" class="mb-1.5 px-2.5 text-[10px] uppercase tracking-[0.14em] text-(--side-fg2)">Risorse</div>
        <span
            :title="isCollapsed ? 'Alimenti' : undefined"
            class="flex cursor-not-allowed items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium text-(--side-fg2)"
            :class="{ 'justify-center px-0': isCollapsed }"
          >
            <Apple :size="16" :stroke-width="1.8" class="shrink-0" />
            <span v-if="!isCollapsed" class="truncate">Alimenti</span>
          </span>
      </div>

      <!-- Bottone Toggle Collapse -->
      <button
        type="button"
        @click="toggleSidebar"
        :title="isCollapsed ? 'Espandi sidebar' : 'Rimpicciolisci sidebar'"
        class="hidden items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium text-(--side-fg2) transition-colors hover:bg-(--side-act-bg) hover:text-white lg:flex"
        :class="{ 'lg:justify-center lg:px-0': isCollapsed }"
      >
        <component
          :is="isCollapsed ? PanelLeftOpen : PanelLeftClose"
          :size="16"
          :stroke-width="1.8"
          class="shrink-0"
        />
        <span class="truncate" :class="{ 'lg:hidden': isCollapsed }">Comprimi</span>
      </button>
    </div>
  </div>
</template>
