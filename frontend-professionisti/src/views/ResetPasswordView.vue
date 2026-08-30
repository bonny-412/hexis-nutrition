<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const route = useRoute()
const router = useRouter()
const token = typeof route.query.token === 'string' ? route.query.token : ''

const nuovaPassword = ref('')
const conferma = ref('')
const inCorso = ref(false)
const errore = ref('')

async function onSubmit() {
  errore.value = ''
  if (nuovaPassword.value.length < 8) {
    errore.value = 'La password deve avere almeno 8 caratteri.'
    return
  }
  if (nuovaPassword.value !== conferma.value) {
    errore.value = 'Le due password non coincidono.'
    return
  }
  inCorso.value = true
  try {
    await resetPassword(token, nuovaPassword.value)
    router.push({ name: 'login' })
  } catch (e) {
    errore.value = e instanceof ApiError && e.status === 400
      ? 'Il link non è più valido: richiedine uno nuovo.'
      : 'Errore imprevisto, riprova.'
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--bg)]">
    <form class="w-full max-w-[360px]" @submit.prevent="onSubmit">
      <h1 class="font-heading text-2xl italic text-[var(--fg)]">Imposta una nuova password</h1>

      <div
        v-if="errore"
        class="mt-4 rounded-lg border border-[var(--bd2)] bg-[var(--warn-bg)] px-3 py-2.5 text-sm font-semibold text-[var(--danger)]"
      >
        {{ errore }}
        <router-link v-if="errore.includes('richiedine')" to="/password-dimenticata" class="ml-1 underline">
          Richiedi un nuovo link
        </router-link>
      </div>

      <div class="mb-3.5 mt-4 flex flex-col gap-1.5">
        <Label for="nuova-password" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Nuova password</Label>
        <Input id="nuova-password" v-model="nuovaPassword" type="password" required />
      </div>

      <div class="mb-5 flex flex-col gap-1.5">
        <Label for="conferma-password" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Conferma password</Label>
        <Input id="conferma-password" v-model="conferma" type="password" required />
      </div>

      <Button type="submit" :disabled="inCorso" class="w-full">
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </Button>
    </form>
  </div>
</template>
