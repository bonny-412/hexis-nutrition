<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useNotificheStore } from '@/stores/notifiche'
import { sincronizzaPush } from '@/utils/push'
import { useEsci } from '@/composables/useEsci'
import { onMounted, onUnmounted } from 'vue'
import { Menu, Bell, ChevronDown, LogOut, Moon } from '@lucide/vue'
import CommandPalette from './CommandPalette.vue'
import NotificheLista from './notifiche/NotificheLista.vue'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
} from '@/components/ui/dropdown-menu'

defineEmits<{ 'apri-menu': [] }>()

const auth = useAuthStore()
const tema = useThemeStore()
const notifiche = useNotificheStore()

onMounted(() => {
  notifiche.avviaPolling()
  void sincronizzaPush()
})
onUnmounted(() => notifiche.fermaPolling())

const onLogout = useEsci()
</script>

<template>
  <!-- Su mobile l'header è una barra marcata (filetto e ombra --elev) che stacca dal contenuto; da desktop resta velato e senza ombra. -->
  <header
    class="sticky top-0 z-30 flex items-center justify-between border-b border-(--bd) bg-(--surf) py-3 px-2 shadow-(--elev) dark:border-(--side-bd) dark:bg-(--side) lg:border-b-0 lg:px-4 lg:bg-(--bg-blur) lg:shadow-none lg:dark:bg-(--bg-blur) lg:backdrop-blur-[10px]"
  >
    <div class="flex items-center gap-2.5">
      <Button variant="ghost" size="icon" class="lg:hidden" aria-label="Apri menu" @click="$emit('apri-menu')">
        <Menu :size="20" />
      </Button>
    </div>

    <div
      data-test="brand-mobile"
      class="absolute left-1/2 top-1/2 flex -translate-x-1/2 -translate-y-1/2 items-center gap-2.5 lg:hidden"
    >
      <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-9 w-9" />
      <span class="font-heading text-xl font-semibold text-(--fg)">Hexis</span>
    </div>

    <div class="flex items-center gap-2.5">
      <CommandPalette />

      <Popover :open="notifiche.listaAperta" @update:open="(aperto) => (aperto ? notifiche.apriLista() : notifiche.chiudiLista())">
        <PopoverTrigger as-child>
          <Button variant="neutral" size="icon" class="relative lg:size-10" aria-label="Notifiche" data-test="notifiche-campanello">
            <Bell :size="18" />
            <span
              v-if="notifiche.nonLette > 0"
              class="absolute right-1.5 top-1.5 size-2 rounded-full border border-(--green-d) bg-(--lime)"
              :class="{ 'animate-pulse': notifiche.pulsa }"
              data-test="notifiche-puntino"
            ></span>
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" class="w-80">
          <NotificheLista />
        </PopoverContent>
      </Popover>

      <div class="mx-1 h-5.5 w-px bg-(--bd) hidden lg:flex"></div>

      <!-- Su mobile il profilo (tema, uscita) sta in fondo al menu laterale: qui l'header resta a ☰ · Hexis · ricerca · notifiche. -->
      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <button
            type="button"
            aria-label="Menu profilo"
            class="group hidden items-center gap-2 rounded-xl lg:flex lg:border lg:border-(--bd2) lg:bg-(--surf) lg:py-1 lg:pl-1 lg:pr-2.5"
          >
            <Avatar class="bg-(--mint)">
              <AvatarFallback class="bg-(--mint) text-xs font-bold text-(--green-d)">
                {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
              </AvatarFallback>
            </Avatar>
            <span class="hidden flex-col items-start whitespace-nowrap leading-tight lg:flex">
              <span class="text-[11.5px] font-bold text-(--fg)">
                {{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}
              </span>
              <span class="text-[10px] text-(--fg3)">Professionista</span>
            </span>
            <ChevronDown
              :size="14"
              class="hidden text-(--fg4) transition-transform duration-200 group-data-[state=open]:rotate-180 lg:block"
            />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="w-56">
          <DropdownMenuLabel class="font-normal">
            <div class="text-xs font-bold text-(--fg)">{{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}</div>
            <div class="mt-0.5 text-[11px] font-normal text-(--fg3)">{{ auth.professionista?.email }}</div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuCheckboxItem
            data-test="toggle-tema"
            class="cursor-pointer"
            :model-value="tema.isDark"
            @update:model-value="tema.toggleTheme"
          >
            <Moon :size="14" />
            Tema scuro
          </DropdownMenuCheckboxItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem data-test="logout" class="cursor-pointer" variant="destructive" @click="onLogout">
            <LogOut :size="14" />
            Esci dall'account
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  </header>
</template>
