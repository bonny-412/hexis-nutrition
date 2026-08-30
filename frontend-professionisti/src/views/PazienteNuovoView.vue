<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { crea } from '@/api/pazienti'

const nome = ref('')
const cognome = ref('')
const email = ref('')
const telefono = ref('')
const inCorso = ref(false)
const errore = ref('')

const router = useRouter()

async function onSubmit() {
  inCorso.value = true
  errore.value = ''
  try {
    const paziente = await crea({
      nome: nome.value,
      cognome: cognome.value,
      email: email.value,
      telefono: telefono.value || undefined,
    })
    router.push(`/pazienti/${paziente.id}`)
  } catch {
    errore.value = 'Non è stato possibile creare il paziente. Controlla i dati e riprova.'
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <h1 class="mb-6 text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Nuovo paziente</h1>

    <form class="max-w-md" @submit.prevent="onSubmit">
      <p v-if="errore" class="mb-4 text-sm font-semibold" style="color: var(--danger)">{{ errore }}</p>

      <label for="nome" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Nome</span>
        <input id="nome" v-model="nome" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="cognome" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Cognome</span>
        <input id="cognome" v-model="cognome" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="email" class="mb-3.5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Email</span>
        <input id="email" v-model="email" type="email" required class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <label for="telefono" class="mb-5 flex flex-col gap-1.5">
        <span class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Telefono</span>
        <input id="telefono" v-model="telefono" class="rounded-lg border px-3 py-2.5 text-sm" style="border-color: var(--bd2); background: var(--surf)" />
      </label>

      <button
        type="submit"
        :disabled="inCorso"
        class="rounded-lg px-4 py-2.5 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
      >
        {{ inCorso ? 'Salvataggio…' : 'Crea paziente' }}
      </button>
    </form>
  </AppShell>
</template>
