<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { Menu, Bell, ChevronDown, LogOut } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu'

defineEmits<{ 'apri-menu': [] }>()

const auth = useAuthStore()
const router = useRouter()

function onLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header
    class="sticky top-0 z-30 flex items-center justify-between bg-(--surf) py-3 px-2 lg:px-4 lg:bg-(--bg-blur) lg:backdrop-blur-[10px]"
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
      <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-9 w-9 rounded-xl bg-(--green)" />
      <span class="font-heading text-xl font-semibold text-(--fg)">Hexis</span>
    </div>

    <div class="flex items-center gap-2.5">
      <Button variant="outline" size="icon" class="relative" aria-label="Notifiche">
        <Bell :size="16" />
        <span class="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-(--green) shadow-[0_0_0_2px_var(--surf)]"></span>
      </Button>

      <div class="mx-1 h-5.5 w-px bg-(--bd)"></div>

      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <button
            type="button"
            aria-label="Menu profilo"
            class="group flex items-center gap-2 rounded-xl lg:border lg:border-(--bd2) lg:bg-(--surf) lg:py-1 lg:pl-1 lg:pr-2.5"
          >
            <Avatar class="bg-(--mint)">
              <AvatarFallback class="bg-(--mint) text-xs font-bold text-(--green)">
                {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
              </AvatarFallback>
            </Avatar>
            <span class="hidden flex-col items-start whitespace-nowrap leading-tight lg:flex">
              <span class="text-[11.5px] font-bold text-(--fg)">
                {{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}
              </span>
              <span class="text-[10px] text-(--fg4)">Professionista</span>
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
            <div class="mt-0.5 text-[11px] font-normal text-(--fg4)">{{ auth.professionista?.email }}</div>
          </DropdownMenuLabel>
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
