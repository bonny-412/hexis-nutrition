<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import { toast } from 'vue-sonner'
import { Eye, EyeOff } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'

const email = ref('')
const password = ref('')
const ricordami = ref(true)
const passwordVisibile = ref(false)
const inCorso = ref(false)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  try {
    await auth.login(email.value, password.value, ricordami.value)
    const destinazione = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(destinazione)
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 401) {
      toast.error('Email o password non corrette. Riprova.')
    } else {
      toast.error('Servizio temporaneamente non raggiungibile. Riprova più tardi.')
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 bg-(--bg) md:grid-cols-12">
    <!-- HERO / LEFT PANEL (7 Colonne) -->
    <div class="relative hidden flex-col justify-between overflow-hidden bg-(--green) p-12 text-white md:col-span-6 md:flex lg:col-span-7 xl:p-16">
      <!-- Glow Decorativo di Sfondo -->
      <div class="pointer-events-none absolute -left-20 -top-20 h-96 w-96 rounded-full bg-(--sage)/20 blur-3xl" />
      <div class="pointer-events-none absolute -bottom-32 -right-32 h-120 w-120 rounded-full bg-(--mint)/10 blur-3xl" />

      <!-- Top: Logo & Brand -->
      <div class="relative z-10 flex items-center gap-3">
        <div class="flex size-11 items-center justify-center rounded-2xl bg-white/10 p-2 backdrop-blur-md ring-1 ring-white/20">
          <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-full w-full object-contain" />
        </div>
        <span class="font-heading text-xl font-semibold tracking-tight text-white">Hexis Nutrition</span>
      </div>

      <!-- Center: Copy Editorial -->
      <div class="relative z-10 my-auto max-w-lg py-12">
        <div class="mb-6 inline-flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1.5 text-xs font-medium text-(--mint) backdrop-blur-md ring-1 ring-white/15">
          <ShieldCheck :size="14" />
          <span>Piattaforma clinica avanzata</span>
        </div>
        <h2 class="font-heading text-4xl font-normal italic leading-tight text-white xl:text-5xl">
          Il tuo studio nutrizionale, in un unico posto.
        </h2>
        <p class="mt-4 text-base leading-relaxed text-(--side-fg)">
          Pazienti, piani alimentari e agenda: una suite integrata pensata per valorizzare la tua professione ogni giorno.
        </p>
      </div>

      <!-- Bottom: Footer -->
      <div class="relative z-10 flex items-center justify-between text-xs text-white/50">
        <span>© 2026 Hexis Nutrition</span>
        <span class="hover:underline cursor-pointer">Privacy Policy</span>
      </div>
    </div>

    <!-- FORM PANEL (5 Colonne) -->
    <div class="flex flex-col items-center justify-center p-6 md:col-span-6 md:p-12 lg:col-span-5">
      <div class="w-full max-w-sm">
        <!-- Logo Mobile -->
        <div class="mb-8 flex items-center gap-3 md:hidden">
          <img src="@/assets/hexis-logo.svg" alt="Hexis" class="size-10 rounded-xl bg-(--green) p-2" />
          <span class="font-heading text-lg font-semibold text-(--fg)">Hexis Nutrition</span>
        </div>

        <div class="mb-8">
          <h1 class="font-heading text-3xl italic text-(--fg)">Accedi</h1>
          <p class="mt-2 text-sm text-(--fg3)">Inserisci le tue credenziali per entrare nel tuo studio</p>
        </div>

        <form class="space-y-4" @submit.prevent="onSubmit">
          <!-- Campo Email -->
          <div class="space-y-1.5">
            <Label for="email" class="text-xs font-semibold uppercase tracking-wider text-(--fg3)">Email</Label>
            <Input
              id="email"
              v-model="email"
              type="email"
              required
              autocomplete="username"
              placeholder="nome@studio.it"
              class="h-11 rounded-xl border-(--bd) bg-(--surf) px-3.5 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)"
            />
          </div>

          <!-- Campo Password -->
          <div class="space-y-1.5">
            <div class="flex items-center justify-between">
              <Label for="password" class="text-xs font-semibold uppercase tracking-wider text-(--fg3)">Password</Label>
              <router-link to="/password-dimenticata" class="text-xs font-semibold text-(--green) hover:underline">
                Password dimenticata?
              </router-link>
            </div>
            <div class="relative flex items-center">
              <Input
                id="password"
                v-model="password"
                :type="passwordVisibile ? 'text' : 'password'"
                required
                autocomplete="current-password"
                placeholder="••••••••"
                class="h-11 w-full rounded-xl border-(--bd) bg-(--surf) pl-3.5 pr-10 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)"
              />
              <button
                type="button"
                class="absolute right-3 text-(--fg3) transition-colors hover:text-(--fg)"
                :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
                @click="passwordVisibile = !passwordVisibile"
              >
                <EyeOff v-if="passwordVisibile" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
          </div>

          <!-- Remember Me -->
          <div class="flex items-center gap-2 pt-1">
            <Checkbox id="ricordami" v-model="ricordami" class="rounded-md border-(--bd) data-[state=checked]:bg-(--green)" />
            <Label for="ricordami" class="cursor-pointer text-xs font-medium text-(--fg2)">
              Ricordami su questo dispositivo
            </Label>
          </div>

          <!-- Submit Button -->
          <Button
            type="submit"
            :disabled="inCorso"
            class="mt-2 h-11 w-full rounded-xl bg-(--green) text-sm font-semibold text-white shadow-sm transition-all hover:bg-primary/80 active:not-aria-[haspopup]:translate-y-0.5"
          >
            <Loader2 v-if="inCorso" :size="18" class="mr-2 animate-spin" />
            <span>{{ inCorso ? 'Accesso in corso…' : 'Accedi' }}</span>
          </Button>
        </form>
      </div>
    </div>
  </div>
</template>
