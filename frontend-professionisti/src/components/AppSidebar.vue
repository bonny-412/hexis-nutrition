<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import { LayoutGrid, Calendar, Users, MessageSquare, Apple, Lock, PanelLeftOpen, PanelLeftClose, Utensils, Moon, LogOut, ChevronDown } from '@lucide/vue'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'
import { useNotificheStore } from '@/stores/notifiche'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useEsci } from '@/composables/useEsci'
import { AREA_PAZIENTE_ATTIVA } from '@/config/funzioni'

const notifiche = useNotificheStore()
/** `conProfilo`: nel menu mobile, dove l'header non mostra l'avatar, il profilo (tema, uscita) sta qui in fondo. */
const props = withDefaults(defineProps<{ class?: string; conProfilo?: boolean }>(), { class: '', conProfilo: false })
const auth = useAuthStore()
const tema = useThemeStore()
const esci = useEsci()

const isCollapsed = defineModel<boolean>('compressed', { default: false })

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const voci = [
  { nome: 'Dashboard', routeName: 'dashboard', routeNamesAttivi: ['dashboard'], icona: LayoutGrid },
  { nome: 'Agenda', routeName: 'agenda', routeNamesAttivi: ['agenda'], icona: Calendar },
  { nome: 'Pazienti', routeName: 'pazienti', routeNamesAttivi: ['pazienti', 'paziente-nuovo', 'paziente-dettaglio'], icona: Users },
  { nome: 'Piani alimentari', routeName: 'piani-alimentari', routeNamesAttivi: ['piani-alimentari', 'piano-alimentare-nuovo', 'piano-alimentare-modifica'], icona: Utensils },
  // La chat arriva con l'area paziente (decisione 0012). Analytics tolta: non è in programma.
  ...(AREA_PAZIENTE_ATTIVA ? [{ nome: 'Chat', icona: MessageSquare }] : []),
]

const route = useRoute()
</script>

<template>
  <div
    :class="['flex shrink-0 flex-col justify-between bg-(--side) p-[18px_14px_14px] text-(--side-fg)', isCollapsed ? 'w-16 px-2' : 'w-61.5', props.class]"
  >
    <div>
      <div class="mb-5 flex items-center gap-2.5 px-1.5" :class="{ 'justify-center px-0': isCollapsed }">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-10 w-10 flex-none rounded-[6px]" />
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
          :aria-label="voce.nome === 'Agenda' && notifiche.nonVisteInAgenda > 0
            ? 'Agenda, nuove notifiche'
            : !voce.routeName ? `${voce.nome}, in arrivo`
            : isCollapsed ? voce.nome : undefined"
          :title="voce.routeName ? (isCollapsed ? voce.nome : undefined) : `${voce.nome}: in arrivo`"
          :aria-disabled="voce.routeName ? undefined : 'true'"
          :role="voce.routeName ? undefined : 'link'"
          class="relative flex items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium transition-colors"
          :class="[
            isCollapsed ? 'justify-center px-0' : '',
            voce.routeNamesAttivi?.includes(route.name as string)
              ? 'bg-(--side-act-bg) text-white before:absolute before:inset-y-1.5 before:left-0 before:w-[3px] before:rounded-full before:bg-(--lime)'
              : voce.routeName
                ? 'text-(--side-fg) hover:bg-(--side-act-bg)/50'
                : 'cursor-not-allowed text-(--side-fg2)',
          ]"
        >
          <component :is="voce.icona" :size="16" :stroke-width="1.8" />
          <span v-if="!isCollapsed" class="truncate">{{ voce.nome }}</span>
          <!-- Funzioni non ancora disponibili: visibili per far sapere che arriveranno, col lucchetto e non cliccabili. -->
          <Lock
            v-if="!voce.routeName"
            :size="12"
            :stroke-width="2"
            class="flex-none"
            :class="isCollapsed ? 'absolute bottom-1 right-1.5' : 'ml-auto'"
            aria-hidden="true"
            data-test="sidebar-voce-bloccata"
          />
          <span
            v-if="voce.nome === 'Agenda' && notifiche.nonVisteInAgenda > 0"
            class="size-2 flex-none rounded-full bg-(--lime)"
            :class="isCollapsed ? 'absolute right-2 top-1.5' : 'ml-auto'"
            :aria-hidden="true"
            data-test="sidebar-agenda-puntino"
          />
        </component>
      </nav>
    </div>

    <div class="flex flex-col gap-2">
      <div>
        <div v-if="!isCollapsed" class="mb-1.5 px-2.5 text-[10px] uppercase tracking-[0.14em] text-(--side-fg2)">Risorse</div>
        <RouterLink
            :to="{ name: 'alimenti' }"
            :title="isCollapsed ? 'Alimenti' : undefined"
            :aria-label="isCollapsed ? 'Alimenti' : undefined"
            class="relative flex items-center gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium transition-colors"
            :class="[
              isCollapsed ? 'justify-center px-0' : '',
              route.name === 'alimenti' ? 'bg-(--side-act-bg) text-white before:absolute before:inset-y-1.5 before:left-0 before:w-[3px] before:rounded-full before:bg-(--lime)' : 'text-(--side-fg) hover:bg-(--side-act-bg)/50',
            ]"
          >
            <Apple :size="16" :stroke-width="1.8" class="shrink-0" />
            <span v-if="!isCollapsed" class="truncate">Alimenti</span>
          </RouterLink>
      </div>

      <!-- Profilo come nel menu dell'header da desktop: un pulsante che apre tema e uscita, verso l'alto. -->
      <div v-if="conProfilo" class="border-t border-(--side-bd) pt-3" data-test="sidebar-profilo">
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <button
              type="button"
              aria-label="Menu profilo"
              class="group flex w-full items-center gap-2.5 rounded-[9px] px-1.5 py-1.5 text-left transition-colors hover:bg-(--side-act-bg)/50"
            >
              <span class="flex size-8 flex-none items-center justify-center rounded-full bg-(--mint) text-xs font-bold text-(--green-d)" aria-hidden="true">
                {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
              </span>
              <span class="min-w-0 flex-1 leading-tight">
                <span class="block truncate text-[13px] font-semibold text-white">{{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}</span>
                <span class="block truncate text-[11px] text-(--side-fg2)">Professionista</span>
              </span>
              <!-- Come il menu profilo dell'header da desktop: verso il basso da chiuso, ruota da aperto. -->
              <ChevronDown :size="14" class="flex-none text-(--side-fg2) transition-transform duration-200 group-data-[state=open]:rotate-180" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="start" class="w-(--reka-dropdown-menu-trigger-width) min-w-56">
            <DropdownMenuLabel class="font-normal">
              <div class="text-xs font-bold text-(--fg)">{{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}</div>
              <div class="mt-0.5 text-[11px] font-normal text-(--fg3)">{{ auth.professionista?.email }}</div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuCheckboxItem class="cursor-pointer" data-test="sidebar-tema" :model-value="tema.isDark" @update:model-value="tema.toggleTheme">
              <Moon :size="14" />
              Tema scuro
            </DropdownMenuCheckboxItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem class="cursor-pointer" variant="destructive" data-test="sidebar-esci" @click="esci">
              <LogOut :size="14" />
              Esci dall'account
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <!-- Bottone Toggle Collapse -->
      <Button
        type="button"
        variant="ghost"
        @click="toggleSidebar"
        :title="isCollapsed ? 'Espandi sidebar' : 'Rimpicciolisci sidebar'"
        :aria-label="isCollapsed ? 'Espandi sidebar' : 'Comprimi sidebar'"
        class="hidden h-auto justify-start gap-2.5 rounded-[9px] px-2.5 py-2 text-[13.5px] font-medium text-(--side-fg2) hover:bg-(--side-act-bg) hover:text-white active:translate-y-0 lg:flex dark:hover:bg-(--side-act-bg)"
        :class="{ 'lg:justify-center lg:px-0': isCollapsed }"
      >
        <component
          :is="isCollapsed ? PanelLeftOpen : PanelLeftClose"
          :size="16"
          :stroke-width="1.8"
          class="shrink-0"
        />
        <span class="truncate" :class="{ 'lg:hidden': isCollapsed }">Comprimi</span>
      </Button>
    </div>
  </div>
</template>
