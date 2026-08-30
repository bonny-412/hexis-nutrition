<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { crea } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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
    <h1 class="mb-6 font-heading text-3xl italic text-[var(--fg)]">Nuovo paziente</h1>

    <form class="max-w-md" @submit.prevent="onSubmit">
      <p v-if="errore" class="mb-4 text-sm font-semibold text-[var(--danger)]">{{ errore }}</p>

      <div class="mb-3.5 flex flex-col gap-1.5">
        <Label for="nome" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Nome</Label>
        <Input id="nome" v-model="nome" required />
      </div>

      <div class="mb-3.5 flex flex-col gap-1.5">
        <Label for="cognome" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Cognome</Label>
        <Input id="cognome" v-model="cognome" required />
      </div>

      <div class="mb-3.5 flex flex-col gap-1.5">
        <Label for="email" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Email</Label>
        <Input id="email" v-model="email" type="email" required />
      </div>

      <div class="mb-5 flex flex-col gap-1.5">
        <Label for="telefono" class="text-xs font-bold uppercase tracking-wide text-[var(--fg3)]">Telefono</Label>
        <Input id="telefono" v-model="telefono" />
      </div>

      <Button type="submit" :disabled="inCorso">
        {{ inCorso ? 'Salvataggio…' : 'Crea paziente' }}
      </Button>
    </form>
  </AppShell>
</template>
