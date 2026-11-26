<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { LayoutDashboard, LogIn } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <main class="grid min-h-dvh place-items-center bg-background px-6 py-12 text-center text-foreground">
    <div class="w-full">
      <div class="mb-10 flex items-center justify-center gap-3">
        <img src="@/assets/hexis-logo.svg" alt="" class="size-10 rounded-xl bg-(--side) p-1.5" />
        <span class="font-heading text-xl font-semibold">Hexis Nutrition</span>
      </div>

      <p class="font-heading text-7xl leading-none text-primary sm:text-8xl">404</p>
      <h1 class="mt-4 font-heading text-3xl italic sm:text-4xl">Pagina non trovata</h1>
      <p class="mt-3 text-sm leading-relaxed text-muted-foreground">
        Controlla l’indirizzo oppure usa il link qui sotto per continuare.
      </p>

      <Button as-child size="lg" class="mt-8 rounded-xl">
        <RouterLink :to="{ name: auth.token ? 'dashboard' : 'login' }">
          <LayoutDashboard v-if="auth.token" aria-hidden="true" />
          <LogIn v-else aria-hidden="true" />
          {{ auth.token ? 'Torna alla dashboard' : 'Vai al login' }}
        </RouterLink>
      </Button>
    </div>
  </main>
</template>
