<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'
import { ApiError } from '@/api/client'

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
  <div class="flex min-h-screen items-center justify-center" style="background: var(--bg)">
    <form class="w-full max-w-[360px]" @submit.prevent="onSubmit">
      <h1 class="text-2xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Imposta una nuova password</h1>

      <div
        v-if="errore"
        class="mt-4 rounded-lg border px-3 py-2.5 text-sm font-semibold"
        style="background: var(--warn-bg); border-color: var(--bd2); color: var(--danger)"
      >
        {{ errore }}
        <router-link v-if="errore.includes('richiedine')" to="/password-dimenticata" class="ml-1 underline">
          Richiedi un nuovo link
        </router-link>
      </div>

      <label class="mb-3.5 mt-4 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Nuova password</span>
        <input v-model="nuovaPassword" type="password" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label class="mb-5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Conferma password</span>
        <input v-model="conferma" type="password" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <button
        type="submit"
        :disabled="inCorso"
        class="w-full rounded-lg py-3 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
      >
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </button>
    </form>
  </div>
</template>
