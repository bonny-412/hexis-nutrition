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
    class="sticky top-0 z-30 flex items-center justify-between bg-[var(--surf)] p-[14px_28px] lg:bg-[var(--bg-blur)] lg:backdrop-blur-[10px]"
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
      <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-9 w-9 rounded-xl bg-[var(--green)]" />
      <span class="font-heading text-xl font-semibold text-[var(--fg)]">Hexis</span>
    </div>

    <div class="flex items-center gap-2.5">
      <Button variant="outline" size="icon" class="relative" aria-label="Notifiche">
        <Bell :size="16" />
        <span class="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-[var(--green)] shadow-[0_0_0_2px_var(--surf)]"></span>
      </Button>

      <div class="mx-1 h-[22px] w-px bg-[var(--bd)]"></div>

      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <button
            type="button"
            aria-label="Menu profilo"
            class="group flex items-center gap-2 rounded-xl lg:border lg:border-[var(--bd2)] lg:bg-[var(--surf)] lg:py-1 lg:pl-1 lg:pr-2.5"
          >
            <Avatar class="bg-[var(--mint)]">
              <AvatarFallback class="bg-[var(--mint)] text-xs font-bold text-[var(--green)]">
                {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
              </AvatarFallback>
            </Avatar>
            <span class="hidden flex-col items-start whitespace-nowrap leading-[1.25] lg:flex">
              <span class="text-[11.5px] font-bold text-[var(--fg)]">
                {{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}
              </span>
              <span class="text-[10px] text-[var(--fg4)]">Professionista</span>
            </span>
            <ChevronDown
              :size="14"
              class="hidden text-[var(--fg4)] transition-transform duration-200 group-data-[state=open]:rotate-180 lg:block"
            />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="w-56">
          <DropdownMenuLabel class="font-normal">
            <div class="text-xs font-bold text-[var(--fg)]">{{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}</div>
            <div class="mt-0.5 text-[11px] font-normal text-[var(--fg4)]">{{ auth.professionista?.email }}</div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem data-test="logout" variant="destructive" @click="onLogout">
            <LogOut :size="14" />
            Esci dall'account
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  </header>
</template>
