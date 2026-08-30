<script setup lang="ts">
import { ref } from 'vue'
import { richiediResetPassword } from '@/api/auth'

const email = ref('')
const inviato = ref(false)
const erroreRete = ref(false)
const inCorso = ref(false)

async function onSubmit() {
  if (inCorso.value) return
  inCorso.value = true
  erroreRete.value = false
  try {
    await richiediResetPassword(email.value)
    inviato.value = true
  } catch {
    erroreRete.value = true
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center" style="background: var(--bg)">
    <div class="w-full max-w-[360px]">
      <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Password dimenticata</h1>

      <p v-if="inviato" class="mt-4 text-sm" style="color: var(--fg2)">
        Se l'indirizzo esiste, riceverai un'email con le istruzioni per reimpostare la password.
      </p>

      <form v-else class="mt-4" @submit.prevent="onSubmit">
        <p v-if="erroreRete" class="mb-4 text-sm font-semibold" style="color: var(--danger)">
          Errore di rete, riprova.
        </p>

        <label class="mb-5 flex flex-col gap-1.5">
          <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
          <input
            v-model="email"
            type="email"
            required
            placeholder="nome@studio.it"
            class="rounded-lg border px-3 py-2.5 text-sm"
            style="border-color: var(--bd2); background: var(--surf)"
          />
        </label>

        <button
          type="submit"
          :disabled="inCorso"
          class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
          style="background: var(--green)"
        >
          {{ inCorso ? 'Invio in corso…' : 'Invia istruzioni' }}
        </button>
      </form>

      <router-link to="/login" class="mt-4 inline-block text-sm font-semibold">← Torna al login</router-link>
    </div>
  </div>
</template>
