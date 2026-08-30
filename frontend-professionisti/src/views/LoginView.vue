<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'

const email = ref('')
const password = ref('')
const ricordami = ref(true)
const passwordVisibile = ref(false)
const inCorso = ref(false)
const erroreCredenziali = ref(false)

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  erroreCredenziali.value = false
  try {
    await auth.login(email.value, password.value, ricordami.value)
    const destinazione = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(destinazione)
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 401) {
      erroreCredenziali.value = true
    } else {
      throw errore
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 md:grid-cols-2" style="background: var(--bg)">
    <div class="hidden flex-col justify-between overflow-hidden p-11 md:flex" style="background: var(--green)">
      <div class="flex items-center gap-3.5">
        <img src="@/assets/hexis-logo.svg" alt="Hexis" class="h-14 w-14 rounded-2xl bg-white p-1.5" />
        <span class="text-2xl font-semibold text-white" style="font-family: Fraunces, serif">Hexis Nutrition</span>
      </div>
      <div class="max-w-md">
        <p class="text-3xl italic text-white" style="font-family: Fraunces, serif">
          Il tuo studio nutrizionale, in un solo posto.
        </p>
        <p class="mt-3.5 text-sm text-white/75">
          Pazienti, piani alimentari e agenda: tutto quello che serve alla tua professione, ogni giorno.
        </p>
      </div>
      <p class="text-xs font-medium text-white/50">© 2026 Hexis Nutrition</p>
    </div>

    <div class="flex items-center justify-center p-10">
      <form class="w-full max-w-[360px]" @submit.prevent="onSubmit">
        <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Accedi</h1>
        <p class="mb-6 mt-1.5 text-sm" style="color: var(--fg3)">Inserisci le tue credenziali per continuare</p>

        <div
          v-if="erroreCredenziali"
          class="mb-4 rounded-lg border px-3 py-2.5 text-sm font-semibold"
          style="background: var(--warn-bg); border-color: var(--bd2); color: var(--danger)"
        >
          Email o password non corrette.
        </div>

        <label class="mb-3.5 flex flex-col gap-1.5">
          <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="username"
            placeholder="nome@studio.it"
            class="rounded-lg border px-3 py-2.5 text-sm"
            style="border-color: var(--bd2); background: var(--surf)"
          />
        </label>

        <label class="mb-2.5 flex flex-col gap-1.5">
          <span class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Password</span>
            <router-link to="/password-dimenticata" class="text-xs font-semibold">Password dimenticata?</router-link>
          </span>
          <span class="flex items-center rounded-lg border pl-3" style="border-color: var(--bd2); background: var(--surf)">
            <input
              v-model="password"
              :type="passwordVisibile ? 'text' : 'password'"
              required
              autocomplete="current-password"
              placeholder="••••••••"
              class="min-w-0 flex-1 border-0 bg-transparent py-2.5 text-sm outline-none"
            />
            <button
              type="button"
              class="m-1.5 flex h-[30px] w-[30px] items-center justify-center rounded-md"
              :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
              @click="passwordVisibile = !passwordVisibile"
            >
              <svg v-if="!passwordVisibile" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1.5 8s2.3-4.5 6.5-4.5S14.5 8 14.5 8s-2.3 4.5-6.5 4.5S1.5 8 1.5 8z"></path><circle cx="8" cy="8" r="1.8"></circle></svg>
              <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1.5 8s2.3-4.5 6.5-4.5S14.5 8 14.5 8s-2.3 4.5-6.5 4.5S1.5 8 1.5 8z"></path><circle cx="8" cy="8" r="1.8"></circle><path d="M2 2l12 12"></path></svg>
            </button>
          </span>
        </label>

        <label class="my-1.5 mb-5 flex items-center gap-2">
          <input v-model="ricordami" type="checkbox" />
          <span class="text-sm" style="color: var(--fg2)">Ricordami su questo dispositivo</span>
        </label>

        <button
          type="submit"
          :disabled="inCorso"
          class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
          style="background: var(--green)"
        >
          {{ inCorso ? 'Accesso in corso…' : 'Accedi' }}
        </button>
      </form>
    </div>
  </div>
</template>
