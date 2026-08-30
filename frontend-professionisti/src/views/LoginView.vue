<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
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
const erroreCredenziali = ref(false)
const erroreGenerico = ref(false)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  erroreCredenziali.value = false
  erroreGenerico.value = false
  try {
    await auth.login(email.value, password.value, ricordami.value)
    const destinazione = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(destinazione)
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 401) {
      erroreCredenziali.value = true
    } else {
      erroreGenerico.value = true
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 bg-(--bg) md:grid-cols-2">
    <div class="hidden flex-col justify-between overflow-hidden bg-(--green) p-11 md:flex">
      <div class="flex items-center gap-3.5">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-14 w-14 rounded-2xl bg-white p-1.5" />
        <span class="font-heading text-2xl font-semibold text-white">Hexis Nutrition</span>
      </div>
      <div class="max-w-md">
        <p class="font-heading text-3xl italic text-white">
          Il tuo studio nutrizionale, in un solo posto.
        </p>
        <p class="mt-3.5 text-sm text-white/75">
          Pazienti, piani alimentari e agenda: tutto quello che serve alla tua professione, ogni giorno.
        </p>
      </div>
      <p class="text-xs font-medium text-white/50">© 2026 Hexis Nutrition</p>
    </div>

    <div class="flex items-center justify-center p-10">
      <form class="w-full max-w-90" @submit.prevent="onSubmit">
        <h1 class="font-heading text-2xl italic text-(--fg)">Accedi</h1>
        <p class="mb-6 mt-1.5 text-sm text-(--fg3)">Inserisci le tue credenziali per continuare</p>

        <div
          v-if="erroreCredenziali"
          class="mb-4 rounded-lg border border-(--bd2) bg-(--warn-bg) px-3 py-2.5 text-sm font-semibold text-(--danger)"
        >
          Email o password non corrette.
        </div>

        <div
          v-if="erroreGenerico"
          class="mb-4 rounded-lg border border-(--bd2) bg-(--warn-bg) px-3 py-2.5 text-sm font-semibold text-(--danger)"
        >
          Servizio non raggiungibile, riprova.
        </div>

        <div class="mb-3.5 flex flex-col gap-1.5">
          <Label for="email" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Email</Label>
          <Input id="email" v-model="email" type="email" required autocomplete="username" placeholder="nome@studio.it" />
        </div>

        <div class="mb-2.5 flex flex-col gap-1.5">
          <span class="flex items-center justify-between">
            <Label for="password" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Password</Label>
            <router-link to="/password-dimenticata" class="text-xs font-semibold">Password dimenticata?</router-link>
          </span>
          <span class="flex items-center rounded-lg border border-(--bd2) bg-(--surf) pl-3">
            <input
              id="password"
              v-model="password"
              :type="passwordVisibile ? 'text' : 'password'"
              required
              autocomplete="current-password"
              placeholder="••••••••"
              class="min-w-0 flex-1 border-0 bg-transparent py-2.5 text-sm outline-none"
            />
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              class="m-1.5"
              :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
              @click="passwordVisibile = !passwordVisibile"
            >
              <EyeOff v-if="passwordVisibile" :size="16" />
              <Eye v-else :size="16" />
            </Button>
          </span>
        </div>

        <div class="my-1.5 mb-5 flex items-center gap-2">
          <Checkbox id="ricordami" v-model="ricordami" />
          <Label for="ricordami" class="text-sm font-normal text-(--fg2)">Ricordami su questo dispositivo</Label>
        </div>

        <Button type="submit" :disabled="inCorso" class="w-full">
          {{ inCorso ? 'Accesso in corso…' : 'Accedi' }}
        </Button>
      </form>
    </div>
  </div>
</template>
