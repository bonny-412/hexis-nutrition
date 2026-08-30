<script setup lang="ts">
import { ref } from 'vue'
import { richiediResetPassword } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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
  <div class="flex min-h-screen items-center justify-center bg-[var(--bg)]">
    <div class="w-full max-w-[360px]">
      <h1 class="font-heading text-2xl italic text-[var(--fg)]">Password dimenticata</h1>

      <p v-if="inviato" class="mt-4 text-sm text-[var(--fg2)]">
        Se l'indirizzo esiste, riceverai un'email con le istruzioni per reimpostare la password.
      </p>

      <form v-else class="mt-4" @submit.prevent="onSubmit">
        <p v-if="erroreRete" class="mb-4 text-sm font-semibold text-[var(--danger)]">
          Errore di rete, riprova.
        </p>

        <div class="mb-5 flex flex-col gap-1.5">
          <Label for="email" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Email</Label>
          <Input id="email" v-model="email" type="email" required placeholder="nome@studio.it" />
        </div>

        <Button type="submit" :disabled="inCorso" class="w-full">
          {{ inCorso ? 'Invio in corso…' : 'Invia istruzioni' }}
        </Button>
      </form>

      <router-link to="/login" class="mt-4 inline-block text-sm font-semibold">← Torna al login</router-link>
    </div>
  </div>
</template>
