<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function onLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="flex items-center justify-end gap-3 px-6 py-3" style="background: var(--surf)">
    <div class="flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm" style="border-color: var(--bd2)">
      <span
        class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold text-white"
        style="background: var(--green)"
      >
        {{ auth.professionista?.nome?.[0] }}{{ auth.professionista?.cognome?.[0] }}
      </span>
      <span class="font-semibold" style="color: var(--fg)">
        {{ auth.professionista?.nome }} {{ auth.professionista?.cognome }}
      </span>
      <button type="button" class="ml-2 text-xs font-semibold" style="color: var(--fg3)" @click="onLogout">
        Esci
      </button>
    </div>
  </header>
</template>
